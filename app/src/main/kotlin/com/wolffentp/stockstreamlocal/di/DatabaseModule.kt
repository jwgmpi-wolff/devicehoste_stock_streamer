package com.wolffentp.stockstreamlocal.di

import android.content.Context
import androidx.room.Room
import com.wolffentp.stockstreamlocal.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides fun provideTickerDao(db: AppDatabase) = db.tickerDao()
    @Provides fun providePortfolioLotDao(db: AppDatabase) = db.portfolioLotDao()
    @Provides fun provideQuoteSnapshotDao(db: AppDatabase) = db.quoteSnapshotDao()
    @Provides fun provideColumnLayoutDao(db: AppDatabase) = db.columnLayoutDao()
    @Provides fun provideRotatingViewDao(db: AppDatabase) = db.rotatingViewDao()
    @Provides fun provideProviderConfigDao(db: AppDatabase) = db.providerConfigDao()
}
