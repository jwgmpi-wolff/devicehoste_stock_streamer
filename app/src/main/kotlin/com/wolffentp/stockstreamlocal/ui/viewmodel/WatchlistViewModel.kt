package com.wolffentp.stockstreamlocal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wolffentp.stockstreamlocal.data.model.TickerEntity
import com.wolffentp.stockstreamlocal.data.repository.TickerRepository
import com.wolffentp.stockstreamlocal.market.model.SymbolValidationResult
import com.wolffentp.stockstreamlocal.market.provider.ProviderFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val tickerRepository: TickerRepository,
    private val providerFactory: ProviderFactory,
) : ViewModel() {

    val watchlist: StateFlow<List<TickerEntity>> = tickerRepository.observeWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _addState = MutableStateFlow<AddTickerState>(AddTickerState.Idle)
    val addState: StateFlow<AddTickerState> = _addState.asStateFlow()

    fun addTicker(symbol: String, displayName: String = symbol) {
        val upper = symbol.trim().uppercase()
        if (!tickerRepository.validateSymbolFormat(upper)) {
            _addState.value = AddTickerState.Error("Invalid symbol format: $upper")
            return
        }
        _addState.value = AddTickerState.Validating
        viewModelScope.launch {
            val result = providerFactory.buildProvider().validateSymbol(upper)
            when (result) {
                is SymbolValidationResult.Valid -> {
                    try {
                        tickerRepository.addTicker(upper, displayName.ifBlank { result.name ?: upper })
                        _addState.value = AddTickerState.Added(upper)
                    } catch (e: Exception) {
                        _addState.value = AddTickerState.Error("Failed to save ticker: ${e.message}")
                    }
                }
                is SymbolValidationResult.Invalid ->
                    _addState.value = AddTickerState.Error("Symbol not found: $upper")
                is SymbolValidationResult.Unsupported -> {
                    try {
                        tickerRepository.addTicker(upper)
                        _addState.value = AddTickerState.Warning("$upper added but unsupported by provider — no live quotes.")
                    } catch (e: Exception) {
                        _addState.value = AddTickerState.Error("Failed to save ticker: ${e.message}")
                    }
                }
                is SymbolValidationResult.ProviderNotConfigured -> {
                    try {
                        tickerRepository.addTicker(upper)
                        _addState.value = AddTickerState.Warning("$upper added. Configure a provider in Settings for live quotes.")
                    } catch (e: Exception) {
                        _addState.value = AddTickerState.Error("Failed to save ticker: ${e.message}")
                    }
                }
                is SymbolValidationResult.Offline -> {
                    try {
                        tickerRepository.addTicker(upper)
                        _addState.value = AddTickerState.Warning("$upper added. Symbol not validated — device offline.")
                    } catch (e: Exception) {
                        _addState.value = AddTickerState.Error("Failed to save ticker: ${e.message}")
                    }
                }
                is SymbolValidationResult.ProviderError -> {
                    try {
                        tickerRepository.addTicker(upper)
                        _addState.value = AddTickerState.Warning("$upper added. Provider error during validation.")
                    } catch (e: Exception) {
                        _addState.value = AddTickerState.Error("Failed to save ticker: ${e.message}")
                    }
                }
            }
        }
    }

    fun removeTicker(symbol: String) {
        viewModelScope.launch { tickerRepository.removeTicker(symbol) }
    }

    fun updateNotes(symbol: String, notes: String) {
        viewModelScope.launch {
            val existing = tickerRepository.getBySymbol(symbol) ?: return@launch
            tickerRepository.updateTicker(existing.copy(notes = notes))
        }
    }

    fun resetAddState() { _addState.value = AddTickerState.Idle }
}

sealed class AddTickerState {
    object Idle : AddTickerState()
    object Validating : AddTickerState()
    data class Added(val symbol: String) : AddTickerState()
    data class Warning(val message: String) : AddTickerState()
    data class Error(val message: String) : AddTickerState()
}
