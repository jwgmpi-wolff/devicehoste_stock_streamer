package com.wolffentp.stockstreamlocal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "column_layouts")
data class ColumnLayoutEntity(
    @PrimaryKey val viewId: String,   // matches RotatingViewEntity.id or "default"
    val columnOrderJson: String,      // JSON array of column names in display order
    val hiddenColumnsJson: String,    // JSON array of hidden column names
    val sortColumnName: String? = null,
    val sortAscending: Boolean = true,
    val updatedAtUtc: Long = System.currentTimeMillis(),
)
