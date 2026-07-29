package com.wolffentp.stockstreamlocal.data.db.dao

import androidx.room.*
import com.wolffentp.stockstreamlocal.data.model.PortfolioLotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioLotDao {
    @Query("SELECT * FROM portfolio_lots ORDER BY symbol ASC, account ASC")
    fun observeAll(): Flow<List<PortfolioLotEntity>>

    @Query("SELECT * FROM portfolio_lots WHERE symbol = :symbol ORDER BY account ASC")
    fun observeBySymbol(symbol: String): Flow<List<PortfolioLotEntity>>

    @Query("SELECT * FROM portfolio_lots WHERE symbol = :symbol")
    suspend fun getBySymbol(symbol: String): List<PortfolioLotEntity>

    @Query("SELECT * FROM portfolio_lots WHERE account = :account ORDER BY symbol ASC")
    fun observeByAccount(account: String): Flow<List<PortfolioLotEntity>>

    @Query("SELECT DISTINCT account FROM portfolio_lots ORDER BY account ASC")
    suspend fun getDistinctAccounts(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lots: List<PortfolioLotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lot: PortfolioLotEntity)

    @Query("DELETE FROM portfolio_lots WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM portfolio_lots")
    suspend fun deleteAll()

    @Query("SELECT * FROM portfolio_lots WHERE symbol = :symbol AND account = :account")
    suspend fun getBySymbolAndAccount(symbol: String, account: String): PortfolioLotEntity?
}
