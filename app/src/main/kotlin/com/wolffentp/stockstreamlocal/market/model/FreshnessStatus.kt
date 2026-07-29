package com.wolffentp.stockstreamlocal.market.model

/**
 * Describes how fresh the data returned from a provider call is.
 * The UI uses this to decide badge color and warning icons.
 */
enum class FreshnessStatus {
    /** Provider confirmed the data is current as of the latest trading moment. */
    LIVE,

    /** Provider indicates data is intentionally delayed (e.g., 15 minutes). */
    DELAYED,

    /** Data is from a prior successful refresh and has exceeded the configured
     *  staleness window, or the provider returned a throttle/error response. */
    STALE,

    /** No provider data was ever received; the value comes from a CSV import only. */
    IMPORTED_ONLY,

    /** Provider returned an error and no prior snapshot exists for fallback. */
    ERROR,

    /** Device is offline; no attempt was made to contact the provider. */
    OFFLINE,

    /** Provider is configured but declined the request (rate limit, invalid key, etc.). */
    THROTTLED,

    /** Market is currently closed; data shown is from last trading session. */
    MARKET_CLOSED,

    /** No provider is configured. */
    NOT_CONFIGURED,

    /** Symbol is not supported by the configured provider. */
    UNSUPPORTED;
}
