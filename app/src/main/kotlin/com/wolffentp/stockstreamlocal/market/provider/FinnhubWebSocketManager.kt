package com.wolffentp.stockstreamlocal.market.provider

import android.util.Log
import com.wolffentp.stockstreamlocal.security.SecureStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FinnhubWebSocket"
private const val WS_BASE = "wss://ws.finnhub.io"

sealed class WsConnectionState {
    object Disconnected : WsConnectionState()
    object Connecting : WsConnectionState()
    object Connected : WsConnectionState()
    data class Error(val message: String) : WsConnectionState()
}

/**
 * Manages a persistent Finnhub WebSocket connection for real-time trade price streaming.
 *
 * Usage:
 * - Call [connect] with the symbols to subscribe to.
 * - Collect [tradeFlow] for individual (symbol, price) trade events.
 * - Observe [connectionState] for the socket lifecycle.
 * - Call [disconnect] when done.
 */
@Singleton
class FinnhubWebSocketManager @Inject constructor(
    private val secureStorage: SecureStorage,
    private val okHttpClient: OkHttpClient,
) {
    private val _connectionState = MutableStateFlow<WsConnectionState>(WsConnectionState.Disconnected)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    /** Emits individual (symbol, price) pairs as trades arrive from the WebSocket. */
    private val _tradeFlow = MutableSharedFlow<Pair<String, Double>>(extraBufferCapacity = 128)
    val tradeFlow: SharedFlow<Pair<String, Double>> = _tradeFlow.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var subscribedSymbols: Set<String> = emptySet()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /**
     * Opens the WebSocket and subscribes to [symbols].
     * Safe to call repeatedly — it reconnects if already connected.
     */
    fun connect(symbols: List<String>) {
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isNullOrBlank() || symbols.isEmpty()) return

        disconnect()
        _connectionState.value = WsConnectionState.Connecting

        val request = Request.Builder()
            .url("$WS_BASE?token=$apiKey")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connectionState.value = WsConnectionState.Connected
                symbols.forEach { symbol ->
                    ws.send("""{"type":"subscribe","symbol":"${symbol.trim()}"}""")
                }
                subscribedSymbols = symbols.toSet()
                Log.d(TAG, "WebSocket connected; subscribed to ${symbols.size} symbol(s)")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<FinnhubWsMessage>(text)
                    if (msg.type == "trade") {
                        msg.data?.forEach { trade ->
                            if (trade.p > 0.0 && trade.s.isNotBlank()) {
                                _tradeFlow.tryEmit(trade.s to trade.p)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "WebSocket parse error: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = WsConnectionState.Error(t.message ?: "Connection failure")
                Log.w(TAG, "WebSocket failure: ${t.message}")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = WsConnectionState.Disconnected
                Log.d(TAG, "WebSocket closed (code=$code): $reason")
            }
        })
    }

    /**
     * Subscribes to additional [newSymbols] and unsubscribes from symbols no longer needed.
     * Requires an active connection.
     */
    fun updateSymbols(newSymbols: List<String>) {
        val ws = webSocket ?: return
        if (_connectionState.value !is WsConnectionState.Connected) {
            connect(newSymbols)
            return
        }
        val newSet = newSymbols.toSet()
        (subscribedSymbols - newSet).forEach { sym ->
            ws.send("""{"type":"unsubscribe","symbol":"${sym.trim()}"}""")
        }
        (newSet - subscribedSymbols).forEach { sym ->
            ws.send("""{"type":"subscribe","symbol":"${sym.trim()}"}""")
        }
        subscribedSymbols = newSet
    }

    /** Closes the WebSocket connection gracefully. */
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        subscribedSymbols = emptySet()
        _connectionState.value = WsConnectionState.Disconnected
    }
}

// ─── Finnhub WebSocket JSON models ───────────────────────────────────────────

@Serializable
private data class FinnhubWsMessage(
    @SerialName("type") val type: String = "",
    @SerialName("data") val data: List<FinnhubWsTrade>? = null,
)

@Serializable
private data class FinnhubWsTrade(
    @SerialName("p") val p: Double = 0.0,   // trade price
    @SerialName("s") val s: String = "",    // symbol
    @SerialName("t") val t: Long = 0L,      // timestamp ms
    @SerialName("v") val v: Double = 0.0,   // volume
)
