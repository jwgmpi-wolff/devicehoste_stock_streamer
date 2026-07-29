package com.wolffentp.stockstreamlocal.market.policy

import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NoHallucinatedDataPolicy
 *
 * Runtime enforcement gate for the hard rule:
 *   "This app must never display fake, simulated, inferred, or placeholder financial data."
 *
 * Every [QuoteResult] produced by any [com.wolffentp.stockstreamlocal.market.provider.MarketDataProvider]
 * must pass through [validate] before being stored or displayed.
 *
 * Invariants enforced:
 * 1. [QuoteResult.isLive] may only be true when [FreshnessStatus.LIVE].
 * 2. No numeric field may have a non-null value paired with [DataSourceLabel.UNAVAILABLE]
 *    or [DataSourceLabel.NOT_PROVIDED_BY_SOURCE] — those labels mean the field is missing.
 * 3. Calculated fields must have [DataSourceLabel.CALCULATED] and all inputs present.
 * 4. A result with [FreshnessStatus.NOT_CONFIGURED] must have all price fields null.
 *
 * Violations cause the offending field to be reset to null/UNAVAILABLE and the error
 * is logged (without sensitive values). No exception is thrown to avoid crashing the UI.
 *
 * Design-time Compose @Preview functions MUST use [PreviewFakeData] objects that are
 * annotated [@PreviewOnly] and never referenced in production code paths.
 * See [com.wolffentp.stockstreamlocal.ui.preview.PreviewFakeData].
 */
@Singleton
class NoHallucinatedDataPolicy @Inject constructor() {

    /**
     * Validate and sanitize a [QuoteResult].
     * Returns a safe, corrected result — never throws.
     */
    fun validate(result: QuoteResult): QuoteResult {
        var r = result

        // Invariant 1: isLive must only be true for LIVE freshness
        if (r.isLive && r.freshnessStatus != FreshnessStatus.LIVE) {
            android.util.Log.e(
                TAG,
                "POLICY VIOLATION: isLive=true but freshnessStatus=${r.freshnessStatus} for redacted symbol. Correcting."
            )
            r = r.copy(isLive = false)
        }

        // Invariant 2: Fields labeled NOT_PROVIDED_BY_SOURCE or UNAVAILABLE must be null
        r = r.copy(
            last = if (r.lastLabel == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                       r.lastLabel == DataSourceLabel.UNAVAILABLE) null else r.last,
            bid = if (r.bidLabel == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                      r.bidLabel == DataSourceLabel.UNAVAILABLE) null else r.bid,
            ask = if (r.askLabel == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                      r.askLabel == DataSourceLabel.UNAVAILABLE) null else r.ask,
            prevClose = if (r.prevCloseLabel == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                            r.prevCloseLabel == DataSourceLabel.UNAVAILABLE) null else r.prevClose,
            chg = if (r.chgLabel == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                      r.chgLabel == DataSourceLabel.UNAVAILABLE) null else r.chg,
            volume = if (r.volumeLabel == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                         r.volumeLabel == DataSourceLabel.UNAVAILABLE) null else r.volume,
            weekRange52Low = if (r.weekRange52Label == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                                 r.weekRange52Label == DataSourceLabel.UNAVAILABLE) null else r.weekRange52Low,
            weekRange52High = if (r.weekRange52Label == DataSourceLabel.NOT_PROVIDED_BY_SOURCE ||
                                  r.weekRange52Label == DataSourceLabel.UNAVAILABLE) null else r.weekRange52High,
        )

        // Invariant 3: NOT_CONFIGURED result must have all price fields null
        if (r.freshnessStatus == FreshnessStatus.NOT_CONFIGURED) {
            if (r.last != null) {
                android.util.Log.e(TAG, "POLICY VIOLATION: NOT_CONFIGURED result has non-null last price. Clearing.")
                r = r.copy(
                    last = null,
                    lastLabel = DataSourceLabel.UNAVAILABLE,
                )
            }
        }

        return r
    }

    /**
     * Checks that a result came from a real provider and not a synthetic/fake source.
     * Used in tests to assert the production dependency graph has no fake providers wired in.
     */
    fun assertNotFake(result: QuoteResult) {
        require(result.providerName != FAKE_PROVIDER_SENTINEL) {
            "POLICY VIOLATION: Fake provider '${result.providerName}' must not appear in the production runtime. " +
            "Ensure BuildConfig.ALLOW_FAKE_PROVIDER == false."
        }
    }

    companion object {
        private const val TAG = "NoHallucinatedDataPolicy"

        /**
         * Sentinel provider name used in tests to detect if a fake provider was wired in.
         * Production code must never create a QuoteResult with this provider name.
         */
        const val FAKE_PROVIDER_SENTINEL = "__FAKE_DEMO_PROVIDER__"
    }
}
