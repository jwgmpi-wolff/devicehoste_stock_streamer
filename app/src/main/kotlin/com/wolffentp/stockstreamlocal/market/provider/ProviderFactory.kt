package com.wolffentp.stockstreamlocal.market.provider

import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.model.SymbolValidationResult
import com.wolffentp.stockstreamlocal.market.model.MarketStatusResult
import com.wolffentp.stockstreamlocal.security.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates and vends the configured [MarketDataProvider] instance.
 * If no provider is configured or the API key is absent, returns a [NullMarketDataProvider]
 * that safely returns "not configured" results for every call.
 *
 * Note: A Null provider is NOT the same as a fake/demo provider. It returns structured
 * "not configured" results — no prices, no synthesized data.
 */
@Singleton
class ProviderFactory @Inject constructor(
    private val secureStorage: SecureStorage,
) {
    fun buildProvider(): MarketDataProvider {
        val providerTypeStr = secureStorage.getProviderType() ?: return NullMarketDataProvider()
        val apiKey = secureStorage.getApiKey() ?: return NullMarketDataProvider()

        val providerType = try {
            ProviderType.valueOf(providerTypeStr)
        } catch (e: IllegalArgumentException) {
            return NullMarketDataProvider()
        }

        return when (providerType) {
            ProviderType.ALPHA_VANTAGE -> AlphaVantageProvider(apiKey = apiKey)
            ProviderType.FINNHUB -> FinnhubProvider(apiKey = apiKey)
            ProviderType.NONE -> NullMarketDataProvider()
        }
    }
}

enum class ProviderType(val displayName: String) {
    NONE("None (not configured)"),
    ALPHA_VANTAGE("Alpha Vantage"),
    FINNHUB("Finnhub (Free Real-time)"),
}

/**
 * A safe no-op provider returned when no provider is configured.
 * Every method returns a structured "not configured" result. No data is invented.
 */
class NullMarketDataProvider : MarketDataProvider {
    override val capabilities = ProviderCapabilities(
        providerName = "None",
        supportsRealtime = false,
        supportsDelayed = false,
        supportsBidAsk = false,
        supportsVolume = false,
        supportsDayRange = false,
        supports52WeekRange = false,
        supportsEarningsDate = false,
        supportsDividendDate = false,
        supportsMarketStatus = false,
        requiresApiKey = false,
        minimumPollIntervalSeconds = Int.MAX_VALUE,
        rateLimitDescription = "No provider configured.",
    )

    override suspend fun validateSymbol(symbol: String): SymbolValidationResult =
        SymbolValidationResult.ProviderNotConfigured

    override suspend fun getQuote(symbol: String): QuoteResult =
        QuoteResult.notConfigured(symbol)

    override suspend fun getQuotes(symbols: List<String>): List<QuoteResult> =
        symbols.map { QuoteResult.notConfigured(it) }

    override suspend fun getMarketStatus(symbol: String): MarketStatusResult =
        MarketStatusResult.NotSupported
}
