package com.wolffentp.stockstreamlocal.data.db.dao

import androidx.room.*
import com.wolffentp.stockstreamlocal.data.model.TickerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TickerDao {
    @Query("SELECT * FROM tickers ORDER BY symbol ASC")
    fun observeAll(): Flow<List<TickerEntity>>

    @Query("SELECT * FROM tickers WHERE isInWatchlist = 1 ORDER BY symbol ASC")
    fun observeWatchlist(): Flow<List<TickerEntity>>

    @Query("SELECT * FROM tickers WHERE symbol = :symbol")
    suspend fun getBySymbol(symbol: String): TickerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ticker: TickerEntity): Long

    @Update
    suspend fun update(ticker: TickerEntity)

    @Query("DELETE FROM tickers WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("SELECT symbol FROM tickers WHERE isInWatchlist = 1")
    suspend fun getWatchlistSymbols(): List<String>

    @Query("DELETE FROM tickers")
    suspend fun deleteAll()
}
