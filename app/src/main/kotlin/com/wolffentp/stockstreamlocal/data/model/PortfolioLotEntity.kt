package com.wolffentp.stockstreamlocal.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents one imported CSV row (one portfolio lot).
 * Multiple lots for the same symbol are allowed (one per account).
 *
 * All values stored here are IMPORTED BASELINE — never live market data.
 * The UI must label these fields with DataSourceLabel.IMPORTED_BASELINE.
 */
@Entity(
    tableName = "portfolio_lots",
    indices = [Index("symbol"), Index("account")],
)
data class PortfolioLotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val account: String = "",

    // Baseline values from CSV — all are imported, never live
    val importedLast: Double? = null,
    val importedBid: Double? = null,
    val importedAsk: Double? = null,
    val importedChg: Double? = null,
    val importedTdyGainLoss: Double? = null,
    val quantity: Double? = null,
    val importedVolume: Long? = null,
    val importedDayRangeLow: Double? = null,
    val importedDayRangeHigh: Double? = null,
    val importedWeekRange52Low: Double? = null,
    val importedWeekRange52High: Double? = null,
    val purchasePrice: Double? = null,
    val importedValue: Double? = null,
    val importedPctTdyGainLoss: Double? = null,
    val importedGainLoss: Double? = null,
    val importedPctGainLoss: Double? = null,
    val importedCloseValue: Double? = null,
    val earningsDate: String? = null,
    val divDate: String? = null,
    val importedPrevClose: Double? = null,
    val importedAtUtc: Long = System.currentTimeMillis(),
    val sourceFileName: String = "",
)
