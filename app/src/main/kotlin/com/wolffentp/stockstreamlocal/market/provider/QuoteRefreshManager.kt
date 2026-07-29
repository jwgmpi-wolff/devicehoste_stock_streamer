package com.wolffentp.stockstreamlocal.market.provider

import android.util.Log
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.policy.NoHallucinatedDataPolicy
import com.wolffentp.stockstreamlocal.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

private const val TAG = "QuoteRefreshManager"
private const val MIN_INTERVAL_SECONDS = 15L

/**
 * Manages the coroutine-based polling loop for live quote refreshes.
 *
 * When the configured provider is Finnhub, a WebSocket connection is opened alongside
 * the polling loop to deliver real-time last-price updates between REST polls.
 *
 * Rules enforced:
 * - Polling only runs while the app is actively in the foreground (caller manages scope lifecycle).
 * - If the device is offline, no provider call is made; [RefreshState.Offline] is emitted.
 * - Every [QuoteResult] from REST passes through [NoHallucinatedDataPolicy] before being emitted.
 * - Minimum interval of [MIN_INTERVAL_SECONDS] is enforced regardless of user setting.
 * - Pause/resume is supported via [pause] and [resume].
 */
@Singleton
class QuoteRefreshManager @Inject constructor(
    private val providerFactory: ProviderFactory,
    private val networkMonitor: NetworkMonitor,
    private val policy: NoHallucinatedDataPolicy,
    private val wsManager: FinnhubWebSocketManager,
) {
    private val _quotes = MutableStateFlow<Map<String, QuoteResult>>(emptyMap())
    val quotes: StateFlow<Map<String, QuoteResult>> = _quotes.asStateFlow()

    private val _state = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val state: StateFlow<RefreshState> = _state.asStateFlow()

    private var pollingJob: Job? = null
    private var wsCollectJob: Job? = null
    private var wsStateJob: Job? = null
    private var paused = false
    private var symbols: List<String> = emptyList()
    private var intervalSeconds: Long = 60L

    /** Start or restart polling (and WebSocket if Finnhub) for the given symbol list. */
    fun start(
        scope: CoroutineScope,
        symbolList: List<String>,
        intervalSec: Long = 60L,
    ) {
        pollingJob?.cancel()
        wsCollectJob?.cancel()
        wsStateJob?.cancel()

        symbols = symbolList
        intervalSeconds = max(intervalSec, MIN_INTERVAL_SECONDS)
        paused = false

        // Start WebSocket streaming if provider supports real-time
        val provider = providerFactory.buildProvider()
        if (provider.capabilities.supportsRealtime && symbols.isNotEmpty() && networkMonitor.isOnline()) {
            wsManager.connect(symbols)
            startWsCollection(scope)
        } else {
            wsManager.disconnect()
        }

        // Polling loop for full quotes (refreshes all fields including chg, range, etc.)
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (!paused) refresh()
                delay(intervalSeconds * 1000L)
            }
        }
        Log.d(TAG, "Started; interval=${intervalSeconds}s; symbols=${symbols.size}; ws=${provider.capabilities.supportsRealtime}")
    }

    /**
     * Collects individual trade events from the WebSocket and updates [_quotes] last price
     * in real time. Also transitions [_state] to [RefreshState.Streaming].
     */
    private fun startWsCollection(scope: CoroutineScope) {
        wsCollectJob = scope.launch {
            wsManager.tradeFlow.collect { (symbol, price) ->
                val current = _quotes.value[symbol] ?: return@collect
                _quotes.value = _quotes.value + (symbol to current.copy(
                    last = price,
                    lastLabel = DataSourceLabel.LIVE,
                    freshnessStatus = FreshnessStatus.LIVE,
                    isLive = true,
                    retrievedAtUtc = Instant.now(),
                ))
                // Only upgrade to Streaming state — don't downgrade from Success/Streaming
                _state.value = RefreshState.Streaming(Instant.now())
            }
        }

        wsStateJob = scope.launch {
            wsManager.connectionState.collect { wsState ->
                when (wsState) {
                    is WsConnectionState.Error -> {
                        if (_state.value is RefreshState.Streaming) {
                            _state.value = RefreshState.Error("Stream disconnected: ${wsState.message}")
                        }
                    }
                    WsConnectionState.Disconnected -> {
                        if (_state.value is RefreshState.Streaming) {
                            _state.value = RefreshState.Idle
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        wsCollectJob?.cancel()
        wsStateJob?.cancel()
        wsManager.disconnect()
        pollingJob = null
        wsCollectJob = null
        wsStateJob = null
        _state.value = RefreshState.Idle
    }

    fun pause() {
        paused = true
        _state.value = RefreshState.Paused
    }

    fun resume() {
        paused = false
    }

    /** Trigger an immediate REST refresh outside the polling interval. */
    suspend fun refreshNow() = refresh()

    private suspend fun refresh() {
        if (symbols.isEmpty()) {
            _state.value = RefreshState.Idle
            return
        }
        val online = networkMonitor.isOnline()
        if (!online) {
            wsManager.disconnect()
            _state.value = RefreshState.Offline
            val offlineMap = _quotes.value.mapValues { (sym, _) ->
                QuoteResult.offline(sym, providerFactory.buildProvider().capabilities.providerName)
            }
            _quotes.value = offlineMap
            return
        }

        // Only set Refreshing if we are not actively streaming (don't flicker the banner)
        if (_state.value !is RefreshState.Streaming) {
            _state.value = RefreshState.Refreshing
        }
        try {
            val provider = providerFactory.buildProvider()

            // Re-connect WebSocket if it dropped since last poll
            if (provider.capabilities.supportsRealtime &&
                wsManager.connectionState.value == WsConnectionState.Disconnected
            ) {
                wsManager.connect(symbols)
            }

            val results = provider.getQuotes(symbols)
            val validated = results.map { policy.validate(it) }

            // Merge REST results with any WebSocket-sourced last price already in the map
            val merged = validated.associate { restQuote ->
                val existing = _quotes.value[restQuote.symbol]
                val merged = if (existing != null && existing.isLive && existing.last != null) {
                    // Preserve the more-recent WebSocket last price
                    restQuote.copy(
                        last = existing.last,
                        lastLabel = DataSourceLabel.LIVE,
                        freshnessStatus = FreshnessStatus.LIVE,
                        isLive = true,
                    )
                } else restQuote
                restQuote.symbol to merged
            }
            _quotes.value = merged

            if (_state.value !is RefreshState.Streaming) {
                _state.value = RefreshState.Success(Instant.now())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Quote refresh error: ${e.javaClass.simpleName}")
            if (_state.value !is RefreshState.Streaming) {
                _state.value = RefreshState.Error("Refresh failed: ${e.message}")
            }
        }
    }
}

sealed class RefreshState {
    object Idle : RefreshState()
    object Paused : RefreshState()
    object Refreshing : RefreshState()
    object Offline : RefreshState()
    data class Success(val at: Instant) : RefreshState()
    data class Error(val message: String) : RefreshState()
    /** WebSocket is active and pushing real-time trade prices. [at] = last trade received. */
    data class Streaming(val at: Instant) : RefreshState()
}
