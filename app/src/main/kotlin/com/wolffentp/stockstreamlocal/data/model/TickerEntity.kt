package com.wolffentp.stockstreamlocal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickers")
data class TickerEntity(
    @PrimaryKey val symbol: String,
    val displayName: String = symbol,
    val notes: String = "",
    val addedAtUtc: Long = System.currentTimeMillis(),
    val isInWatchlist: Boolean = true,
)
