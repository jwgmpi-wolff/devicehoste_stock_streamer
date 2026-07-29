package com.wolffentp.stockstreamlocal.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wolffentp.stockstreamlocal.csv.CsvImportResult
import com.wolffentp.stockstreamlocal.csv.CsvParser
import com.wolffentp.stockstreamlocal.csv.FidelityCsvMapper
import com.wolffentp.stockstreamlocal.data.repository.PortfolioRepository
import com.wolffentp.stockstreamlocal.data.repository.TickerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val csvParser: CsvParser,
    private val csvMapper: FidelityCsvMapper,
    private val portfolioRepository: PortfolioRepository,
    private val tickerRepository: TickerRepository,
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun parseFile(uri: Uri) {
        _importState.value = ImportState.Parsing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: return@launch run { _importState.value = ImportState.Error("Cannot open file.") }
                val fileName = uri.lastPathSegment ?: "unknown.csv"
                val result = csvParser.parse(stream)
                withContext(Dispatchers.Main) {
                    _importState.value = ImportState.Parsed(result, fileName)
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Parse failed: ${e.message}")
            }
        }
    }

    fun confirmImport(result: CsvImportResult.Success, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lots = csvMapper.mapRows(result.rows, fileName)
            portfolioRepository.importLots(lots)
            // Offer watchlist addition for valid symbols
            val newSymbols = lots.map { it.symbol }.distinct()
            val symbols = newSymbols.filter { tickerRepository.getBySymbol(it) == null }
            withContext(Dispatchers.Main) {
                _importState.value = ImportState.Imported(
                    importedCount = lots.size,
                    newSymbols = symbols,
                )
            }
        }
    }

    fun addSymbolsToWatchlist(symbols: List<String>) {
        viewModelScope.launch {
            symbols.forEach { tickerRepository.addTicker(it) }
        }
    }

    fun reset() { _importState.value = ImportState.Idle }
}

sealed class ImportState {
    object Idle : ImportState()
    object Parsing : ImportState()
    data class Parsed(val result: CsvImportResult, val fileName: String) : ImportState()
    data class Imported(val importedCount: Int, val newSymbols: List<String>) : ImportState()
    data class Error(val message: String) : ImportState()
}
