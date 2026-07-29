package com.wolffentp.stockstreamlocal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rotating_views")
data class RotatingViewEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val viewType: String,        // e.g. "QUOTE", "GAIN_LOSS", "VOLUME", "PORTFOLIO", "EARNINGS", "CUSTOM"
    val columnOrderJson: String, // JSON array of column names
    val hiddenColumnsJson: String,
    val sortColumnName: String? = null,
    val sortAscending: Boolean = true,
    val filtersJson: String = "{}",
    val rotationIntervalSeconds: Int = 30,
    val refreshIntervalOverrideSeconds: Int? = null,
    val displayOrder: Int = 0,
    val isEnabled: Boolean = true,
    val isFullScreen: Boolean = false,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis(),
)
