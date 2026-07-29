package com.wolffentp.stockstreamlocal.market.policy

import android.util.Log
import com.wolffentp.stockstreamlocal.market.model.*
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class NoHallucinatedDataPolicyTest {

    private val policy = NoHallucinatedDataPolicy()

    @Before
    fun stubAndroidLogging() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    private fun liveQuote(symbol: String = "AAPL", last: Double? = 150.0) = QuoteResult(
        symbol = symbol,
        providerName = "AlphaVantage",
        retrievedAtUtc = Instant.now(),
        marketStatus = MarketStatus.OPEN,
        freshnessStatus = FreshnessStatus.LIVE,
        isLive = true,
        isDelayed = false,
        isStale = false,
        last = last,
        lastLabel = DataSourceLabel.LIVE,
    )

    // ── isLive invariant ─────────────────────────────────────────────────────

    @Test
    fun `validate corrects isLive=true when freshness is not LIVE`() {
        val bad = liveQuote().copy(freshnessStatus = FreshnessStatus.STALE, isLive = true)
        val fixed = policy.validate(bad)
        assertFalse("isLive must be false for STALE freshness", fixed.isLive)
    }

    @Test
    fun `validate preserves isLive=true when freshness is LIVE`() {
        val good = liveQuote()
        val result = policy.validate(good)
        assertTrue(result.isLive)
    }

    @Test
    fun `validate allows isLive=false for DELAYED freshness`() {
        val delayed = liveQuote().copy(freshnessStatus = FreshnessStatus.DELAYED, isLive = false)
        val result = policy.validate(delayed)
        assertFalse(result.isLive)
    }

    // ── NOT_CONFIGURED clears price ───────────────────────────────────────────

    @Test
    fun `validate clears last price for NOT_CONFIGURED freshness`() {
        val bad = QuoteResult.notConfigured("AAPL").copy(last = 999.0)
        val fixed = policy.validate(bad)
        assertNull("last must be null for NOT_CONFIGURED result", fixed.last)
    }

    // ── NOT_PROVIDED fields must be null ──────────────────────────────────────

    @Test
    fun `validate nulls bid when label is NOT_PROVIDED_BY_SOURCE`() {
        val r = liveQuote().copy(bid = 149.0, bidLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE)
        val fixed = policy.validate(r)
        assertNull("bid must be null when label is NOT_PROVIDED_BY_SOURCE", fixed.bid)
    }

    @Test
    fun `validate nulls weekRange when label is NOT_PROVIDED_BY_SOURCE`() {
        val r = liveQuote().copy(
            weekRange52Low = 100.0,
            weekRange52High = 200.0,
            weekRange52Label = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,
        )
        val fixed = policy.validate(r)
        assertNull(fixed.weekRange52Low)
        assertNull(fixed.weekRange52High)
    }

    // ── Provider error results render unavailable, not fake values ────────────

    @Test
    fun `error result has null last price`() {
        val err = QuoteResult.error("AAPL", "TestProvider", "timeout")
        assertEquals(DataSourceLabel.ERROR, err.lastLabel)
        assertNull(err.last)
    }

    @Test
    fun `throttled result has null last price`() {
        val t = QuoteResult.throttled("AAPL", "TestProvider")
        assertNull(t.last)
        assertEquals(FreshnessStatus.THROTTLED, t.freshnessStatus)
    }

    @Test
    fun `offline result has null last price`() {
        val o = QuoteResult.offline("AAPL", "TestProvider")
        assertNull(o.last)
        assertEquals(FreshnessStatus.OFFLINE, o.freshnessStatus)
        assertFalse(o.isLive)
    }

    @Test
    fun `unsupported result has null last price`() {
        val u = QuoteResult.unsupported("JVTSF", "TestProvider")
        assertNull(u.last)
        assertEquals(FreshnessStatus.UNSUPPORTED, u.freshnessStatus)
    }

    // ── Fake provider detection ───────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `assertNotFake throws for fake provider sentinel`() {
        val fake = liveQuote().copy(providerName = NoHallucinatedDataPolicy.FAKE_PROVIDER_SENTINEL)
        policy.assertNotFake(fake)
    }

    @Test
    fun `assertNotFake passes for real provider name`() {
        policy.assertNotFake(liveQuote()) // must not throw
    }

    // ── Missing calculation inputs stay unavailable ───────────────────────────

    @Test
    fun `imported baseline result has IMPORTED_BASELINE labels`() {
        // Simulate what the QuoteRepository would produce for an imported-only symbol
        val imported = liveQuote().copy(
            freshnessStatus = FreshnessStatus.IMPORTED_ONLY,
            isLive = false,
            last = 20.0,
            lastLabel = DataSourceLabel.IMPORTED_BASELINE,
        )
        val result = policy.validate(imported)
        assertEquals(DataSourceLabel.IMPORTED_BASELINE, result.lastLabel)
        // isLive should be false — NOT_CONFIGURED doesn't apply here, IMPORTED_ONLY is valid non-live
        assertFalse(result.isLive)
    }
}
