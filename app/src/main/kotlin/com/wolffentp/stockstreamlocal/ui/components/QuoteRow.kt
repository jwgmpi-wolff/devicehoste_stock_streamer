package com.wolffentp.stockstreamlocal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wolffentp.stockstreamlocal.columns.ColumnDefinition
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.ui.theme.BadgeError
import com.wolffentp.stockstreamlocal.util.Formatters

/**
 * A single row in the quote table, rendering only [visibleColumns] from [quote].
 * Each cell includes a [DataSourceBadge] so the data origin is always visible.
 */
@Composable
fun QuoteRow(
    quote: QuoteResult,
    visibleColumns: List<ColumnDefinition>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visibleColumns.forEach { col ->
            val (text, label) = resolveCell(quote, col)
            QuoteCell(
                text = text,
                label = label,
                badgeColor = if ((col.name == "Chg" || col.name == "Symbol") && quote.chg?.let { it < 0 } == true) BadgeError else null,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun QuoteCell(
    text: String,
    label: DataSourceLabel,
    badgeColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
        DataSourceBadge(label = label, backgroundColor = badgeColor)
    }
}

private fun resolveCell(q: QuoteResult, col: ColumnDefinition): Pair<String, DataSourceLabel> = when (col.name) {
    "Symbol"        -> q.symbol to DataSourceLabel.LIVE
    "Last"          -> Formatters.currency(q.last) to q.lastLabel
    "Bid"           -> Formatters.currency(q.bid) to q.bidLabel
    "Ask"           -> Formatters.currency(q.ask) to q.askLabel
    "Chg"           -> Formatters.changeWithSign(q.chg) to q.chgLabel
    "Tdy G/L"       -> Formatters.currency(q.tdyGainLoss) to q.tdyGainLossLabel
    "% Tdy G/L"     -> Formatters.percentWithSign(q.pctTdyGainLoss) to q.pctTdyGainLossLabel
    "Volume"        -> Formatters.volume(q.volume) to q.volumeLabel
    "Day Range"     -> Formatters.range(q.dayRangeLow, q.dayRangeHigh) to q.dayRangeLabel
    "52 Wk Range"   -> Formatters.range(q.weekRange52Low, q.weekRange52High) to q.weekRange52Label
    "Purchase Price"-> Formatters.currency(q.purchasePrice) to q.purchasePriceLabel
    "Quantity"      -> Formatters.number(q.quantity) to q.quantityLabel
    "Value"         -> Formatters.currency(q.value) to q.valueLabel
    "G/L"           -> Formatters.currency(q.gainLoss) to q.gainLossLabel
    "% G/L"         -> Formatters.percentWithSign(q.pctGainLoss) to q.pctGainLossLabel
    "Account"       -> (q.account ?: "—") to DataSourceLabel.IMPORTED_BASELINE
    "Close Value"   -> Formatters.currency(q.closeValue) to q.closeValueLabel
    "Earnings Date" -> (q.earningsDate ?: "—") to q.earningsDateLabel
    "Div Date"      -> (q.divDate ?: "—") to q.divDateLabel
    "Prev Close"    -> Formatters.currency(q.prevClose) to q.prevCloseLabel
    else            -> "—" to DataSourceLabel.UNAVAILABLE
}
