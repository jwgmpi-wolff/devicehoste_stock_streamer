package com.wolffentp.stockstreamlocal.market.provider

/**
 * Capability flags for a concrete [MarketDataProvider] implementation.
 * The UI uses these to decide which columns to attempt a live fetch for
 * and which to pre-label as [com.wolffentp.stockstreamlocal.market.model.DataSourceLabel.NOT_PROVIDED_BY_SOURCE].
 */
data class ProviderCapabilities(
    val providerName: String,
    val supportsRealtime: Boolean,
    val supportsDelayed: Boolean,
    val supportsBidAsk: Boolean,
    val supportsVolume: Boolean,
    val supportsDayRange: Boolean,
    val supports52WeekRange: Boolean,
    val supportsEarningsDate: Boolean,
    val supportsDividendDate: Boolean,
    val supportsMarketStatus: Boolean,
    val requiresApiKey: Boolean,
    val minimumPollIntervalSeconds: Int,
    val rateLimitDescription: String,
)
