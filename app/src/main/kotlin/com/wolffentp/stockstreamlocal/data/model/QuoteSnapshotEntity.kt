package com.wolffentp.stockstreamlocal.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatus

/**
 * Persisted snapshot of the most recent quote for a symbol.
 * Stored to allow display of last-known values with a STALE label when the app resumes.
 * NEVER relabeled as live; always shown with retrievedAtUtc timestamp.
 */
@Entity(
    tableName = "quote_snapshots",
    indices = [Index("symbol")],
)
data class QuoteSnapshotEntity(
    @PrimaryKey val symbol: String,
    val providerName: String,
    val retrievedAtUtc: Long,
    val marketStatus: String, // MarketStatus.name()
    val freshnessStatus: String, // FreshnessStatus.name()
    val isLive: Boolean,
    val isDelayed: Boolean,
    val errorMessage: String? = null,

    val last: Double? = null,
    val lastLabel: String = DataSourceLabel.UNAVAILABLE.name,

    val bid: Double? = null,
    val bidLabel: String = DataSourceLabel.NOT_PROVIDED_BY_SOURCE.name,

    val ask: Double? = null,
    val askLabel: String = DataSourceLabel.NOT_PROVIDED_BY_SOURCE.name,

    val prevClose: Double? = null,
    val prevCloseLabel: String = DataSourceLabel.UNAVAILABLE.name,

    val chg: Double? = null,
    val chgLabel: String = DataSourceLabel.UNAVAILABLE.name,

    val pctTdyGainLoss: Double? = null,
    val pctTdyGainLossLabel: String = DataSourceLabel.UNAVAILABLE.name,

    val volume: Long? = null,
    val volumeLabel: String = DataSourceLabel.UNAVAILABLE.name,

    val dayRangeLow: Double? = null,
    val dayRangeHigh: Double? = null,
    val dayRangeLabel: String = DataSourceLabel.UNAVAILABLE.name,

    val weekRange52Low: Double? = null,
    val weekRange52High: Double? = null,
    val weekRange52Label: String = DataSourceLabel.NOT_PROVIDED_BY_SOURCE.name,

    val earningsDate: String? = null,
    val earningsDateLabel: String = DataSourceLabel.NOT_PROVIDED_BY_SOURCE.name,

    val divDate: String? = null,
    val divDateLabel: String = DataSourceLabel.NOT_PROVIDED_BY_SOURCE.name,
)
