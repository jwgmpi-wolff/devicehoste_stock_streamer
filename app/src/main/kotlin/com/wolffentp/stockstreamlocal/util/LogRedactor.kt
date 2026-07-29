package com.wolffentp.stockstreamlocal.util

/**
 * Redacts sensitive values from log messages.
 * Prevents accidental logging of API keys, account names, portfolio values, or PINs.
 */
object LogRedactor {
    private val API_KEY_PATTERN = Regex("apikey=[^&\\s\"]+", RegexOption.IGNORE_CASE)
    private val DOLLAR_AMOUNT_PATTERN = Regex("\\$[0-9,]+\\.?[0-9]*")

    fun redactUrl(url: String): String =
        url.replace(API_KEY_PATTERN, "apikey=[REDACTED]")

    fun redactMessage(message: String): String =
        message.replace(API_KEY_PATTERN, "apikey=[REDACTED]")
               .replace(DOLLAR_AMOUNT_PATTERN, "\$[REDACTED]")

    /** Returns a safe symbol descriptor without any associated financial values. */
    fun safeSymbol(symbol: String): String =
        if (symbol.matches(Regex("[A-Za-z0-9.^-]{1,10}"))) symbol else "[REDACTED_SYMBOL]"
}
