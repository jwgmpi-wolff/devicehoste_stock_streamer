package com.wolffentp.stockstreamlocal.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wolffentp.stockstreamlocal.data.db.dao.*
import com.wolffentp.stockstreamlocal.data.model.*

@Database(
    entities = [
        TickerEntity::class,
        PortfolioLotEntity::class,
        QuoteSnapshotEntity::class,
        ColumnLayoutEntity::class,
        RotatingViewEntity::class,
        ProviderConfigEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tickerDao(): TickerDao
    abstract fun portfolioLotDao(): PortfolioLotDao
    abstract fun quoteSnapshotDao(): QuoteSnapshotDao
    abstract fun columnLayoutDao(): ColumnLayoutDao
    abstract fun rotatingViewDao(): RotatingViewDao
    abstract fun providerConfigDao(): ProviderConfigDao

    companion object {
        const val DATABASE_NAME = "stockstream_local.db"
    }
}
