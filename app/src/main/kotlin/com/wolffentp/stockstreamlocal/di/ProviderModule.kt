package com.wolffentp.stockstreamlocal.di

import com.wolffentp.stockstreamlocal.market.policy.NoHallucinatedDataPolicy
import com.wolffentp.stockstreamlocal.market.provider.FinnhubWebSocketManager
import com.wolffentp.stockstreamlocal.market.provider.ProviderFactory
import com.wolffentp.stockstreamlocal.market.provider.QuoteRefreshManager
import com.wolffentp.stockstreamlocal.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)  // Keep WebSocket alive
        .build()

    @Provides
    @Singleton
    fun provideNoHallucinatedDataPolicy(): NoHallucinatedDataPolicy = NoHallucinatedDataPolicy()

    @Provides
    @Singleton
    fun provideQuoteRefreshManager(
        providerFactory: ProviderFactory,
        networkMonitor: NetworkMonitor,
        policy: NoHallucinatedDataPolicy,
        wsManager: FinnhubWebSocketManager,
    ): QuoteRefreshManager = QuoteRefreshManager(providerFactory, networkMonitor, policy, wsManager)
}
