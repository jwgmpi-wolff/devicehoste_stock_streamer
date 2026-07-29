package com.wolffentp.stockstreamlocal.columns

import com.wolffentp.stockstreamlocal.csv.CsvColumns

/** Represents a single displayable column in the quote/portfolio table. */
data class ColumnDefinition(
    val name: String,          // Matches CsvColumns constant
    val displayLabel: String,  // Short UI label
    val isNumeric: Boolean = true,
    val isCurrency: Boolean = false,
    val isPercent: Boolean = false,
    val isDate: Boolean = false,
    val isText: Boolean = false,
    val defaultVisible: Boolean = true,
    val minWidthDp: Int = 70,
)

/**
 * The canonical ordered list of all supported columns.
 * Column display names match the Fidelity CSV column names exactly for consistency.
 */
object AllColumns {
    val definitions = listOf(
        ColumnDefinition(CsvColumns.SYMBOL,           "Symbol",        isNumeric = false, isText = true, minWidthDp = 80),
        ColumnDefinition(CsvColumns.LAST,             "Last",          isCurrency = true),
        ColumnDefinition(CsvColumns.BID,              "Bid",           isCurrency = true, defaultVisible = false),
        ColumnDefinition(CsvColumns.CHG,              "Chg",           isCurrency = true),
        ColumnDefinition(CsvColumns.ASK,              "Ask",           isCurrency = true, defaultVisible = false),
        ColumnDefinition(CsvColumns.TDY_GAIN_LOSS,    "Tdy G/L",       isCurrency = true),
        ColumnDefinition(CsvColumns.QUANTITY,         "Quantity",      isNumeric = true),
        ColumnDefinition(CsvColumns.VOLUME,           "Volume",        isNumeric = true),
        ColumnDefinition(CsvColumns.DAY_RANGE,        "Day Range",     isNumeric = true, minWidthDp = 110),
        ColumnDefinition(CsvColumns.WEEK_RANGE_52,    "52 Wk Range",   isNumeric = true, minWidthDp = 120, defaultVisible = false),
        ColumnDefinition(CsvColumns.PURCHASE_PRICE,   "Purchase Price",isCurrency = true, defaultVisible = false),
        ColumnDefinition(CsvColumns.VALUE,            "Value",         isCurrency = true),
        ColumnDefinition(CsvColumns.PCT_TDY_GAIN_LOSS,"% Tdy G/L",    isPercent = true),
        ColumnDefinition(CsvColumns.GAIN_LOSS,        "G/L",           isCurrency = true),
        ColumnDefinition(CsvColumns.PCT_GAIN_LOSS,    "% G/L",         isPercent = true),
        ColumnDefinition(CsvColumns.ACCOUNT,          "Account",       isNumeric = false, isText = true, defaultVisible = false),
        ColumnDefinition(CsvColumns.CLOSE_VALUE,      "Close Value",   isCurrency = true, defaultVisible = false),
        ColumnDefinition(CsvColumns.EARNINGS_DATE,    "Earnings Date", isDate = true, isNumeric = false, defaultVisible = false),
        ColumnDefinition(CsvColumns.DIV_DATE,         "Div Date",      isDate = true, isNumeric = false, defaultVisible = false),
        ColumnDefinition(CsvColumns.PREV_CLOSE,       "Prev Close",    isCurrency = true, defaultVisible = false),
    )

    val byName: Map<String, ColumnDefinition> = definitions.associateBy { it.name }

    val defaultVisibleNames: List<String> = definitions
        .filter { it.defaultVisible }
        .map { it.name }
}
