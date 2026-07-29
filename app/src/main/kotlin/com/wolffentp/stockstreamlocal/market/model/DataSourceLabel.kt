package com.wolffentp.stockstreamlocal.market.model

/**
 * Indicates the origin of a data field so the UI can display the correct badge.
 * Every quote field in [QuoteResult] carries one of these labels.
 */
enum class DataSourceLabel(val displayText: String) {
    /** Field value came from a live, real-time provider response during trading hours. */
    LIVE("LIVE"),

    /** Provider confirmed this data is delayed (typically 15–20 minutes). */
    DELAYED("DELAYED"),

    /** Data came from a previous successful provider response that is now older than the
     *  configured staleness threshold, or the provider did not respond on this refresh cycle. */
    STALE("STALE"),

    /** Value was imported from a CSV file and represents a historical/baseline snapshot.
     *  Must never be relabeled as live or current market data. */
    IMPORTED_BASELINE("IMPORTED"),

    /** Value was derived from two or more fields using local arithmetic. Input field sources
     *  are tracked separately. Must not be shown if inputs are missing. */
    CALCULATED("CALCULATED"),

    /** The provider does not return this field for the given symbol or in its free tier.
     *  The field is not guessed, inferred, or synthesized. */
    NOT_PROVIDED_BY_SOURCE("NOT PROVIDED"),

    /** The symbol is not recognized or not tradable via the configured provider. */
    UNSUPPORTED_BY_PROVIDER("UNSUPPORTED"),

    /** A transient or persistent error prevented field retrieval on this refresh cycle. */
    ERROR("ERROR"),

    /** Field has never been populated — no import, no provider response, no calculation. */
    UNAVAILABLE("UNAVAILABLE");

    /** Returns true when the label represents any form of live or delayed real-time data. */
    val isFromProvider: Boolean
        get() = this == LIVE || this == DELAYED || this == STALE

    /** Returns true when showing this label implies a caution color in the UI. */
    val isWarning: Boolean
        get() = this == STALE || this == DELAYED || this == ERROR
}
