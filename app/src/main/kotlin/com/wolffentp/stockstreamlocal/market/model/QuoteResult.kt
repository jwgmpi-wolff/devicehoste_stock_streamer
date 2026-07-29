package com.wolffentp.stockstreamlocal.market.model

import java.time.Instant

/**
 * A fully-attributed quote result from a provider call.
 *
 * Every field is individually labeled with a [DataSourceLabel] so the UI can render
 * the correct badge per cell. Fields that the provider does not return are null with
 * label [DataSourceLabel.NOT_PROVIDED_BY_SOURCE]. Fields that errored are null with
 * label [DataSourceLabel.ERROR]. No field is ever guessed or synthesized.
 *
 * INVARIANT: [isLive] may only be true when [freshnessStatus] == [FreshnessStatus.LIVE]
 * AND the provider explicitly confirmed the data is current during trading hours.
 */
data class QuoteResult(
    val symbol: String,
    val providerName: String,
    val retrievedAtUtc: Instant,
    val marketStatus: MarketStatus,
    val freshnessStatus: FreshnessStatus,
    val isLive: Boolean,
    val isDelayed: Boolean,
    val isStale: Boolean,
    val errorMessage: String? = null,

    // --- Price fields ---
    val last: Double? = null,
    val lastLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val bid: Double? = null,
    val bidLabel: DataSourceLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,

    val ask: Double? = null,
    val askLabel: DataSourceLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,

    val prevClose: Double? = null,
    val prevCloseLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    // --- Change fields ---
    val chg: Double? = null,
    val chgLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val tdyGainLoss: Double? = null,
    val tdyGainLossLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val pctTdyGainLoss: Double? = null,
    val pctTdyGainLossLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    // --- Volume / Range ---
    val volume: Long? = null,
    val volumeLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val dayRangeLow: Double? = null,
    val dayRangeHigh: Double? = null,
    val dayRangeLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val weekRange52Low: Double? = null,
    val weekRange52High: Double? = null,
    val weekRange52Label: DataSourceLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,

    // --- Portfolio fields (populated from CSV import, never from live quotes) ---
    val quantity: Double? = null,
    val quantityLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val purchasePrice: Double? = null,
    val purchasePriceLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val value: Double? = null,
    val valueLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val gainLoss: Double? = null,
    val gainLossLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val pctGainLoss: Double? = null,
    val pctGainLossLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val closeValue: Double? = null,
    val closeValueLabel: DataSourceLabel = DataSourceLabel.UNAVAILABLE,

    val account: String? = null,

    // --- Corporate action dates ---
    val earningsDate: String? = null,
    val earningsDateLabel: DataSourceLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,

    val divDate: String? = null,
    val divDateLabel: DataSourceLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,
) {
    /**
     * Safety assertion used by [com.wolffentp.stockstreamlocal.market.policy.NoHallucinatedDataPolicy].
     * Returns true only when the result originated from a real provider response (not fake/demo data).
     */
    val isProviderSourced: Boolean
        get() = freshnessStatus != FreshnessStatus.NOT_CONFIGURED

    companion object {
        /** Creates a result representing a symbol that is unsupported by the configured provider. */
        fun unsupported(symbol: String, providerName: String): QuoteResult = QuoteResult(
            symbol = symbol,
            providerName = providerName,
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.UNKNOWN,
            freshnessStatus = FreshnessStatus.UNSUPPORTED,
            isLive = false,
            isDelayed = false,
            isStale = false,
            errorMessage = "Unsupported by provider",
            lastLabel = DataSourceLabel.UNSUPPORTED_BY_PROVIDER,
        )

        /** Creates a result representing a provider that is not yet configured. */
        fun notConfigured(symbol: String): QuoteResult = QuoteResult(
            symbol = symbol,
            providerName = "None",
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.UNKNOWN,
            freshnessStatus = FreshnessStatus.NOT_CONFIGURED,
            isLive = false,
            isDelayed = false,
            isStale = false,
            errorMessage = "Provider not configured",
            lastLabel = DataSourceLabel.UNAVAILABLE,
        )

        /** Creates a result representing a device-offline condition. */
        fun offline(symbol: String, providerName: String): QuoteResult = QuoteResult(
            symbol = symbol,
            providerName = providerName,
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.UNKNOWN,
            freshnessStatus = FreshnessStatus.OFFLINE,
            isLive = false,
            isDelayed = false,
            isStale = false,
            errorMessage = "Device offline",
            lastLabel = DataSourceLabel.UNAVAILABLE,
        )

        /** Creates a result representing a provider throttle response. */
        fun throttled(symbol: String, providerName: String): QuoteResult = QuoteResult(
            symbol = symbol,
            providerName = providerName,
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.UNKNOWN,
            freshnessStatus = FreshnessStatus.THROTTLED,
            isLive = false,
            isDelayed = false,
            isStale = false,
            errorMessage = "Provider rate limit reached",
            lastLabel = DataSourceLabel.ERROR,
        )

        /** Creates an error result for a provider exception. */
        fun error(symbol: String, providerName: String, message: String): QuoteResult = QuoteResult(
            symbol = symbol,
            providerName = providerName,
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.UNKNOWN,
            freshnessStatus = FreshnessStatus.ERROR,
            isLive = false,
            isDelayed = false,
            isStale = false,
            errorMessage = message,
            lastLabel = DataSourceLabel.ERROR,
        )
    }
}
