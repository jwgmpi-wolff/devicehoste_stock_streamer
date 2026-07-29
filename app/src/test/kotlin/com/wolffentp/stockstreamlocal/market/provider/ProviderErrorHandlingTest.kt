package com.wolffentp.stockstreamlocal.market.provider

import com.wolffentp.stockstreamlocal.market.model.*
import org.junit.Assert.*
import org.junit.Test

class ProviderErrorHandlingTest {

    private val nullProvider = NullMarketDataProvider()

    // ── Provider not configured ───────────────────────────────────────────────

    @Test
    fun `NullMarketDataProvider getQuote returns NOT_CONFIGURED`() {
        kotlinx.coroutines.runBlocking {
            val result = nullProvider.getQuote("AAPL")
            assertEquals(FreshnessStatus.NOT_CONFIGURED, result.freshnessStatus)
            assertNull("last price must be null when not configured", result.last)
            assertFalse(result.isLive)
        }
    }

    @Test
    fun `NullMarketDataProvider validateSymbol returns ProviderNotConfigured`() {
        kotlinx.coroutines.runBlocking {
            val result = nullProvider.validateSymbol("AAPL")
            assertTrue(result is SymbolValidationResult.ProviderNotConfigured)
        }
    }

    @Test
    fun `NullMarketDataProvider getQuotes returns NOT_CONFIGURED for all symbols`() {
        kotlinx.coroutines.runBlocking {
            val results = nullProvider.getQuotes(listOf("AAPL", "NVDA", "GME"))
            results.forEach { result ->
                assertEquals(FreshnessStatus.NOT_CONFIGURED, result.freshnessStatus)
                assertNull(result.last)
            }
        }
    }

    @Test
    fun `NullMarketDataProvider getMarketStatus returns NotSupported`() {
        kotlinx.coroutines.runBlocking {
            val result = nullProvider.getMarketStatus("AAPL")
            assertTrue(result is MarketStatusResult.NotSupported)
        }
    }

    // ── Static factory methods produce correct error shapes ───────────────────

    @Test
    fun `QuoteResult throttled has null price and THROTTLED freshness`() {
        val r = QuoteResult.throttled("GME", "TestProvider")
        assertEquals(FreshnessStatus.THROTTLED, r.freshnessStatus)
        assertNull(r.last)
        assertFalse(r.isLive)
        assertEquals(DataSourceLabel.ERROR, r.lastLabel)
    }

    @Test
    fun `QuoteResult offline has null price and OFFLINE freshness`() {
        val r = QuoteResult.offline("AMC", "TestProvider")
        assertEquals(FreshnessStatus.OFFLINE, r.freshnessStatus)
        assertNull(r.last)
        assertFalse(r.isLive)
    }

    @Test
    fun `QuoteResult error has null price and ERROR freshness`() {
        val r = QuoteResult.error("NVDA", "TestProvider", "HTTP 429")
        assertEquals(FreshnessStatus.ERROR, r.freshnessStatus)
        assertNull(r.last)
        assertEquals("HTTP 429", r.errorMessage)
    }

    @Test
    fun `QuoteResult unsupported has UNSUPPORTED freshness and badge`() {
        val r = QuoteResult.unsupported("HCMC", "TestProvider")
        assertEquals(FreshnessStatus.UNSUPPORTED, r.freshnessStatus)
        assertEquals(DataSourceLabel.UNSUPPORTED_BY_PROVIDER, r.lastLabel)
        assertNull(r.last)
    }

    // ── Capabilities reflect what provider does NOT support ───────────────────

    @Test
    fun `NullMarketDataProvider capabilities reflect no support`() {
        val caps = nullProvider.capabilities
        assertFalse(caps.supportsRealtime)
        assertFalse(caps.supportsBidAsk)
        assertFalse(caps.supports52WeekRange)
        assertFalse(caps.supportsEarningsDate)
    }

    @Test
    fun `AlphaVantageProvider capabilities reflect correct support flags`() {
        val provider = AlphaVantageProvider(apiKey = "demo")
        val caps = provider.capabilities
        assertFalse("AlphaVantage free tier does not support Bid/Ask", caps.supportsBidAsk)
        assertFalse("AlphaVantage free tier does not support 52-week range", caps.supports52WeekRange)
        assertFalse("AlphaVantage free tier does not support earnings date", caps.supportsEarningsDate)
        assertTrue(caps.supportsVolume)
        assertTrue(caps.supportsDayRange)
        assertTrue(caps.requiresApiKey)
    }
}
