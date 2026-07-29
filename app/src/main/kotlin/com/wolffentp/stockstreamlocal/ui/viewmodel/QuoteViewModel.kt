package com.wolffentp.stockstreamlocal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wolffentp.stockstreamlocal.data.model.PortfolioLotEntity
import com.wolffentp.stockstreamlocal.data.repository.PortfolioRepository
import com.wolffentp.stockstreamlocal.data.repository.QuoteRepository
import com.wolffentp.stockstreamlocal.data.repository.TickerRepository
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.provider.QuoteRefreshManager
import com.wolffentp.stockstreamlocal.market.provider.RefreshState
import com.wolffentp.stockstreamlocal.settings.SettingsRepository
import com.wolffentp.stockstreamlocal.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val refreshManager: QuoteRefreshManager,
    private val tickerRepository: TickerRepository,
    private val quoteRepository: QuoteRepository,
    private val portfolioRepository: PortfolioRepository,
    private val settingsRepository: SettingsRepository,
    val networkMonitor: NetworkMonitor,
) : ViewModel() {

    // Raw quotes from refresh manager
    private val _rawQuotes = refreshManager.quotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Portfolio lots per symbol
    private val _portfolioLots = portfolioRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Quotes enriched with portfolio data (quantity, gain/loss, etc.).
     * Merges live quotes with calculated portfolio fields.
     */
    val quotes: StateFlow<Map<String, QuoteResult>> = combine(
        _rawQuotes, _portfolioLots,
    ) { rawQuotes, lots ->
        val lotsPerSymbol = lots.groupBy { it.symbol }
        rawQuotes.mapValues { (symbol, quote) ->
            val symbolLots = lotsPerSymbol[symbol] ?: emptyList()
            enrichQuoteWithPortfolio(quote, symbolLots)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val refreshState: StateFlow<RefreshState> = refreshManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RefreshState.Idle)

    val isOnline: StateFlow<Boolean> = networkMonitor.onlineState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ─── Sorting ──────────────────────────────────────────────────────────────

    private val _sortColumn = MutableStateFlow<String?>(null)
    val sortColumn: StateFlow<String?> = _sortColumn.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    /** Quotes list ordered by the current sort column/direction. Stable insertion order when unsorted. */
    val sortedQuotes: StateFlow<List<QuoteResult>> = combine(
        quotes, _sortColumn, _sortAscending,
    ) { quotesMap, col, asc ->
        val list = quotesMap.values.toList()
        if (col == null) return@combine list
        list.sortedWith { a, b ->
            val av = sortKey(a, col)
            val bv = sortKey(b, col)
            compareNullable(av, bv).let { if (asc) it else -it }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Toggle sort on [column]: first click = ascending, second = descending, third = clear. */
    fun setSort(column: String) {
        when {
            _sortColumn.value != column -> {
                _sortColumn.value = column
                _sortAscending.value = true
            }
            _sortAscending.value -> _sortAscending.value = false
            else -> _sortColumn.value = null      // third click clears sort
        }
    }

    // ─── Holdings ─────────────────────────────────────────────────────────────

    /**
     * Add or update a holding for a symbol.
     * Creates a new portfolio lot with the provided quantity and purchase price.
     */
    fun addHolding(symbol: String, quantity: Double, purchasePrice: Double, account: String = "Default") {
        viewModelScope.launch {
            val lot = PortfolioLotEntity(
                symbol = symbol.uppercase().trim(),
                account = account,
                quantity = quantity,
                purchasePrice = purchasePrice,
                sourceFileName = "Manual Entry",
            )
            portfolioRepository.addOrUpdateLot(lot)
        }
    }

    /**
     * Delete a portfolio lot by ID.
     */
    fun deleteHolding(lotId: Long) {
        viewModelScope.launch {
            portfolioRepository.deleteLot(lotId)
        }
    }

    // ─── Polling ──────────────────────────────────────────────────────────────

    /**
     * Observes the watchlist as a Flow so polling automatically restarts whenever
     * tickers are added or removed — no manual refresh required.
     */
    fun startPolling() {
        viewModelScope.launch {
            tickerRepository.observeWatchlist()
                .map { tickers -> tickers.map { it.symbol } }
                .distinctUntilChanged()
                .collect { symbols ->
                    val prefs = settingsRepository.getCurrentPreferences()
                    refreshManager.start(
                        scope = viewModelScope,
                        symbolList = symbols,
                        intervalSec = prefs.quoteRefreshIntervalSeconds.toLong(),
                    )
                }
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            val prefs = settingsRepository.getCurrentPreferences()
            val symbols = tickerRepository.getWatchlistSymbols()
            refreshManager.start(viewModelScope, symbols, prefs.quoteRefreshIntervalSeconds.toLong())
            refreshManager.refreshNow()
        }
    }

    fun pause() = refreshManager.pause()
    fun resume() = refreshManager.resume()

    override fun onCleared() {
        super.onCleared()
        refreshManager.stop()
    }
}

// ─── Portfolio enrichment ──────────────────────────────────────────────────────

/**
 * Enriches a live quote with aggregated portfolio data (quantity, gain/loss).
 * Calculates derived fields based on current last price and imported cost basis.
 */
private fun enrichQuoteWithPortfolio(quote: QuoteResult, lots: List<PortfolioLotEntity>): QuoteResult {
    if (lots.isEmpty()) return quote

    val totalQty = lots.mapNotNull { it.quantity }.sum()
    val totalCostBasis = lots.sumOf { (it.quantity ?: 0.0) * (it.purchasePrice ?: 0.0) }

    val last = quote.last ?: return quote.copy(quantity = totalQty)
    val currentValue = last * totalQty
    val gainLoss = currentValue - totalCostBasis
    val pctGainLoss = if (totalCostBasis > 0.0) (gainLoss / totalCostBasis) * 100.0 else 0.0

    return quote.copy(
        quantity = if (totalQty > 0.0) totalQty else null,
        purchasePrice = if (totalCostBasis > 0.0) (totalCostBasis / totalQty) else null,
        value = currentValue,
        valueLabel = DataSourceLabel.CALCULATED,
        gainLoss = gainLoss.takeIf { it != 0.0 },
        gainLossLabel = DataSourceLabel.CALCULATED,
        pctGainLoss = pctGainLoss.takeIf { it != 0.0 },
        pctGainLossLabel = DataSourceLabel.CALCULATED,
    )
}

// ─── Sort helpers ─────────────────────────────────────────────────────────────

private fun sortKey(q: QuoteResult, column: String): Comparable<*>? = when (column) {
    "Symbol"        -> q.symbol
    "Last"          -> q.last
    "Bid"           -> q.bid
    "Ask"           -> q.ask
    "Chg"           -> q.chg
    "Tdy G/L"       -> q.tdyGainLoss
    "% Tdy G/L"     -> q.pctTdyGainLoss
    "Volume"        -> q.volume?.toDouble()
    "Value"         -> q.value
    "G/L"           -> q.gainLoss
    "% G/L"         -> q.pctGainLoss
    "Prev Close"    -> q.prevClose
    "Purchase Price"-> q.purchasePrice
    "Quantity"      -> q.quantity
    "Close Value"   -> q.closeValue
    else            -> null
}

@Suppress("UNCHECKED_CAST")
private fun compareNullable(a: Comparable<*>?, b: Comparable<*>?): Int = when {
    a == null && b == null -> 0
    a == null -> 1      // nulls sort last
    b == null -> -1
    else -> (a as Comparable<Any>).compareTo(b)
}
