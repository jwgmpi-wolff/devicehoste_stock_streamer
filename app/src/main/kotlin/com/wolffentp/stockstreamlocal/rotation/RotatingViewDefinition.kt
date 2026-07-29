package com.wolffentp.stockstreamlocal.rotation

enum class ViewType(val displayName: String) {
    QUOTE("Quote View"),
    GAIN_LOSS("Gain / Loss View"),
    VOLUME("Volume View"),
    PORTFOLIO_VALUE("Portfolio Value View"),
    EARNINGS_DIVIDEND("Earnings / Dividend View"),
    CUSTOM("Custom View"),
}

data class RotatingViewDefinition(
    val id: String,
    val displayName: String,
    val viewType: ViewType,
    val columnNames: List<String>,
    val hiddenColumnNames: Set<String>,
    val sortColumnName: String? = null,
    val sortAscending: Boolean = true,
    val rotationIntervalSeconds: Int = 30,
    val refreshIntervalOverrideSeconds: Int? = null,
    val displayOrder: Int = 0,
    val isEnabled: Boolean = true,
    val isFullScreen: Boolean = false,
)

/** Default built-in views provisioned on first launch. */
object DefaultViews {
    fun build(): List<RotatingViewDefinition> = listOf(
        RotatingViewDefinition(
            id = "quote",
            displayName = "Quote View",
            viewType = ViewType.QUOTE,
            columnNames = listOf("Symbol", "Last", "Chg", "% Tdy G/L", "Volume"),
            hiddenColumnNames = emptySet(),
            displayOrder = 0,
        ),
        RotatingViewDefinition(
            id = "gain_loss",
            displayName = "Gain / Loss View",
            viewType = ViewType.GAIN_LOSS,
            columnNames = listOf("Symbol", "Last", "Tdy G/L", "% Tdy G/L", "G/L", "% G/L"),
            hiddenColumnNames = emptySet(),
            displayOrder = 1,
        ),
        RotatingViewDefinition(
            id = "volume",
            displayName = "Volume View",
            viewType = ViewType.VOLUME,
            columnNames = listOf("Symbol", "Last", "Volume", "Day Range", "Chg"),
            hiddenColumnNames = emptySet(),
            displayOrder = 2,
        ),
        RotatingViewDefinition(
            id = "portfolio",
            displayName = "Portfolio Value View",
            viewType = ViewType.PORTFOLIO_VALUE,
            columnNames = listOf("Symbol", "Quantity", "Last", "Value", "G/L", "% G/L", "Account"),
            hiddenColumnNames = emptySet(),
            displayOrder = 3,
        ),
        RotatingViewDefinition(
            id = "earnings_div",
            displayName = "Earnings / Dividend View",
            viewType = ViewType.EARNINGS_DIVIDEND,
            columnNames = listOf("Symbol", "Last", "Earnings Date", "Div Date", "Prev Close"),
            hiddenColumnNames = emptySet(),
            displayOrder = 4,
        ),
    )
}
