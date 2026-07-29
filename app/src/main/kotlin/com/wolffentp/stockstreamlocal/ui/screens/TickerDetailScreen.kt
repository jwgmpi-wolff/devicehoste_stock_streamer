package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.ui.components.DataSourceBadge
import com.wolffentp.stockstreamlocal.ui.components.StatusBanner
import com.wolffentp.stockstreamlocal.ui.viewmodel.QuoteViewModel
import com.wolffentp.stockstreamlocal.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickerDetailScreen(
    symbol: String,
    onNavigateBack: () -> Unit,
    quoteVm: QuoteViewModel = hiltViewModel(),
) {
    val quotes by quoteVm.quotes.collectAsStateWithLifecycle()
    val refreshState by quoteVm.refreshState.collectAsStateWithLifecycle()
    val isOnline by quoteVm.isOnline.collectAsStateWithLifecycle()
    val quote = quotes[symbol]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(symbol) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            StatusBanner(refreshState = refreshState, isOnline = isOnline)

            if (quote == null) {
                Text("No quote data available for $symbol.", modifier = Modifier.padding(16.dp))
                return@Column
            }

            @Composable
            fun DetailRow(label: String, value: String, badge: com.wolffentp.stockstreamlocal.market.model.DataSourceLabel) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                        DataSourceBadge(badge)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }

            Spacer(Modifier.height(8.dp))
            DetailRow("Last",          Formatters.currency(quote.last),                       quote.lastLabel)
            DetailRow("Bid",           Formatters.currency(quote.bid),                        quote.bidLabel)
            DetailRow("Ask",           Formatters.currency(quote.ask),                        quote.askLabel)
            DetailRow("Change",        Formatters.changeWithSign(quote.chg),                  quote.chgLabel)
            DetailRow("% Change",      Formatters.percentWithSign(quote.pctTdyGainLoss),       quote.pctTdyGainLossLabel)
            DetailRow("Prev Close",    Formatters.currency(quote.prevClose),                   quote.prevCloseLabel)
            DetailRow("Volume",        Formatters.volume(quote.volume),                        quote.volumeLabel)
            DetailRow("Day Range",     Formatters.range(quote.dayRangeLow, quote.dayRangeHigh), quote.dayRangeLabel)
            DetailRow("52 Wk Range",   Formatters.range(quote.weekRange52Low, quote.weekRange52High), quote.weekRange52Label)
            DetailRow("Earnings Date", quote.earningsDate ?: "—",                              quote.earningsDateLabel)
            DetailRow("Div Date",      quote.divDate ?: "—",                                   quote.divDateLabel)

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Data Source", style = MaterialTheme.typography.labelLarge)
                    Text("Provider: ${quote.providerName}", style = MaterialTheme.typography.bodySmall)
                    Text("Retrieved: ${Formatters.timestamp(quote.retrievedAtUtc)}", style = MaterialTheme.typography.bodySmall)
                    Text("Market: ${quote.marketStatus.name}", style = MaterialTheme.typography.bodySmall)
                    Text("Freshness: ${quote.freshnessStatus.name}", style = MaterialTheme.typography.bodySmall)
                    if (quote.errorMessage != null) Text("Error: ${quote.errorMessage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
