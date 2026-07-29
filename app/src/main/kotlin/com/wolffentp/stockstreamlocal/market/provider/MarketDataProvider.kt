package com.wolffentp.stockstreamlocal.market.provider

import com.wolffentp.stockstreamlocal.market.model.MarketStatusResult
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.model.SymbolValidationResult

/**
 * Contract for all market data provider implementations.
 *
 * RULES:
 * - Implementations must never generate, synthesize, or randomize field values.
 * - If the provider does not return a field, map it to null with the correct
 *   [com.wolffentp.stockstreamlocal.market.model.DataSourceLabel].
 * - If the provider returns a throttle/rate-limit response, return [QuoteResult.throttled].
 * - If the API key is missing or invalid, return [QuoteResult.notConfigured] or [QuoteResult.error].
 * - If the symbol is not recognized, return [QuoteResult.unsupported].
 * - [isLive] in the returned [QuoteResult] must only be true when the provider
 *   explicitly confirms the data is real-time during active trading hours.
 */
interface MarketDataProvider {
    val capabilities: ProviderCapabilities

    /**
     * Validate whether the given symbol is recognized by this provider.
     * Does not return quote data.
     */
    suspend fun validateSymbol(symbol: String): SymbolValidationResult

    /**
     * Fetch a single quote for the given symbol.
     * Returns [QuoteResult.error] on network failure.
     * Returns [QuoteResult.throttled] on rate limit.
     * Returns [QuoteResult.unsupported] if the symbol is rejected by the provider.
     */
    suspend fun getQuote(symbol: String): QuoteResult

    /**
     * Fetch quotes for multiple symbols in a single batch or sequential calls.
     * Implementations should respect rate limits between calls.
     */
    suspend fun getQuotes(symbols: List<String>): List<QuoteResult>

    /**
     * Return current market status for the given symbol's exchange.
     * Returns [MarketStatusResult.NotSupported] if the provider cannot report market status.
     */
    suspend fun getMarketStatus(symbol: String): MarketStatusResult
}
