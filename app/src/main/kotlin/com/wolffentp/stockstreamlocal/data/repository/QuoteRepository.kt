package com.wolffentp.stockstreamlocal.data.repository

import com.wolffentp.stockstreamlocal.data.db.dao.QuoteSnapshotDao
import com.wolffentp.stockstreamlocal.data.model.QuoteSnapshotEntity
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatus
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepository @Inject constructor(private val dao: QuoteSnapshotDao) {

    fun observeAll(): Flow<List<QuoteSnapshotEntity>> = dao.observeAll()

    suspend fun saveQuoteResult(result: QuoteResult) = dao.upsert(result.toEntity())

    suspend fun saveQuoteResults(results: List<QuoteResult>) =
        dao.upsertAll(results.map { it.toEntity() })

    suspend fun getSnapshot(symbol: String): QuoteSnapshotEntity? = dao.getBySymbol(symbol)

    suspend fun deleteSnapshot(symbol: String) = dao.deleteBySymbol(symbol)

    suspend fun clearAll() = dao.deleteAll()

    private fun QuoteResult.toEntity() = QuoteSnapshotEntity(
        symbol = symbol,
        providerName = providerName,
        retrievedAtUtc = retrievedAtUtc.toEpochMilli(),
        marketStatus = marketStatus.name,
        freshnessStatus = freshnessStatus.name,
        isLive = isLive,
        isDelayed = isDelayed,
        errorMessage = errorMessage,
        last = last, lastLabel = lastLabel.name,
        bid = bid, bidLabel = bidLabel.name,
        ask = ask, askLabel = askLabel.name,
        prevClose = prevClose, prevCloseLabel = prevCloseLabel.name,
        chg = chg, chgLabel = chgLabel.name,
        pctTdyGainLoss = pctTdyGainLoss, pctTdyGainLossLabel = pctTdyGainLossLabel.name,
        volume = volume, volumeLabel = volumeLabel.name,
        dayRangeLow = dayRangeLow, dayRangeHigh = dayRangeHigh, dayRangeLabel = dayRangeLabel.name,
        weekRange52Low = weekRange52Low, weekRange52High = weekRange52High, weekRange52Label = weekRange52Label.name,
        earningsDate = earningsDate, earningsDateLabel = earningsDateLabel.name,
        divDate = divDate, divDateLabel = divDateLabel.name,
    )

    /** Re-hydrate a stored snapshot as a [QuoteResult] with STALE freshness. */
    fun QuoteSnapshotEntity.toStaleQuoteResult(): QuoteResult = QuoteResult(
        symbol = symbol,
        providerName = providerName,
        retrievedAtUtc = Instant.ofEpochMilli(retrievedAtUtc),
        marketStatus = runCatching { MarketStatus.valueOf(marketStatus) }.getOrElse { MarketStatus.UNKNOWN },
        freshnessStatus = FreshnessStatus.STALE,
        isLive = false,
        isDelayed = false,
        isStale = true,
        errorMessage = errorMessage,
        last = last,
        lastLabel = runCatching { DataSourceLabel.valueOf(lastLabel) }.getOrElse { DataSourceLabel.STALE },
    )
}
