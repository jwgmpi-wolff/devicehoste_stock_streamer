package com.wolffentp.stockstreamlocal.csv

/**
 * Exact column names from the Fidelity-style CSV format.
 * These must match the CSV header row exactly (case-insensitive trimmed match).
 */
object CsvColumns {
    const val SYMBOL = "Symbol"
    const val LAST = "Last"
    const val BID = "Bid"
    const val CHG = "Chg"
    const val ASK = "Ask"
    const val TDY_GAIN_LOSS = "Tdy G/L"
    const val QUANTITY = "Quantity"
    const val VOLUME = "Volume"
    const val DAY_RANGE = "Day Range"
    const val WEEK_RANGE_52 = "52 Wk Range"
    const val PURCHASE_PRICE = "Purchase Price"
    const val VALUE = "Value"
    const val PCT_TDY_GAIN_LOSS = "% Tdy G/L"
    const val GAIN_LOSS = "G/L"
    const val PCT_GAIN_LOSS = "% G/L"
    const val ACCOUNT = "Account"
    const val CLOSE_VALUE = "Close Value"
    const val EARNINGS_DATE = "Earnings Date"
    const val DIV_DATE = "Div Date"
    const val PREV_CLOSE = "Prev Close"

    val REQUIRED_COLUMNS = setOf(SYMBOL)

    val ALL_COLUMNS = setOf(
        SYMBOL, LAST, BID, CHG, ASK, TDY_GAIN_LOSS, QUANTITY, VOLUME,
        DAY_RANGE, WEEK_RANGE_52, PURCHASE_PRICE, VALUE, PCT_TDY_GAIN_LOSS,
        GAIN_LOSS, PCT_GAIN_LOSS, ACCOUNT, CLOSE_VALUE, EARNINGS_DATE,
        DIV_DATE, PREV_CLOSE,
    )
}
