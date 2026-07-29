package com.wolffentp.stockstreamlocal.data.repository

import com.wolffentp.stockstreamlocal.data.db.dao.PortfolioLotDao
import com.wolffentp.stockstreamlocal.data.model.PortfolioLotEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepository @Inject constructor(private val dao: PortfolioLotDao) {

    fun observeAll(): Flow<List<PortfolioLotEntity>> = dao.observeAll()
    fun observeBySymbol(symbol: String): Flow<List<PortfolioLotEntity>> = dao.observeBySymbol(symbol)
    fun observeByAccount(account: String): Flow<List<PortfolioLotEntity>> = dao.observeByAccount(account)

    suspend fun importLots(lots: List<PortfolioLotEntity>) = dao.insertAll(lots)
    suspend fun addOrUpdateLot(lot: PortfolioLotEntity) = dao.insert(lot)
    suspend fun deleteLot(id: Long) = dao.deleteById(id)
    suspend fun clearAll() = dao.deleteAll()
    suspend fun getDistinctAccounts(): List<String> = dao.getDistinctAccounts()

    /**
     * Aggregates all lots for a symbol into totals.
     * Returns (totalQuantity, totalCostBasis, allLots).
     */
    suspend fun getSymbolTotals(symbol: String): Triple<Double, Double, List<PortfolioLotEntity>> {
        val lots = dao.getBySymbol(symbol)
        val totalQty = lots.mapNotNull { it.quantity }.sum()
        val totalCost = lots.sumOf { lot ->
            val q = lot.quantity ?: 0.0
            val p = lot.purchasePrice ?: 0.0
            q * p
        }
        return Triple(totalQty, totalCost, lots)
    }
}
