package com.wolffentp.stockstreamlocal.data.repository

import com.wolffentp.stockstreamlocal.data.db.dao.TickerDao
import com.wolffentp.stockstreamlocal.data.model.TickerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TickerRepository @Inject constructor(private val dao: TickerDao) {

    fun observeWatchlist(): Flow<List<TickerEntity>> = dao.observeWatchlist()
    fun observeAll(): Flow<List<TickerEntity>> = dao.observeAll()

    suspend fun addTicker(symbol: String, displayName: String = symbol, notes: String = "") {
        dao.insert(TickerEntity(symbol = symbol.trim().uppercase(), displayName = displayName, notes = notes))
    }

    suspend fun removeTicker(symbol: String) = dao.deleteBySymbol(symbol)

    suspend fun updateTicker(ticker: TickerEntity) = dao.update(ticker)

    suspend fun getBySymbol(symbol: String): TickerEntity? = dao.getBySymbol(symbol)

    suspend fun getWatchlistSymbols(): List<String> = dao.getWatchlistSymbols()

    /** Basic local format validation — symbol must be 1–10 uppercase alphanumeric chars. */
    fun validateSymbolFormat(symbol: String): Boolean =
        symbol.trim().matches(Regex("^[A-Z0-9.^]{1,10}$"))
}
