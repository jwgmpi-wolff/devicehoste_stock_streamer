package com.wolffentp.stockstreamlocal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.model.SecurityDetails
import com.wolffentp.stockstreamlocal.ui.viewmodel.SecurityDetailState
import com.wolffentp.stockstreamlocal.util.Formatters

@Composable
fun SecurityDetailDialog(
    state: SecurityDetailState,
    quote: QuoteResult?,
    onDismiss: () -> Unit,
    onRetry: (String) -> Unit,
) {
    val symbol = when (state) {
        SecurityDetailState.Closed -> return
        is SecurityDetailState.Loading -> state.symbol
        is SecurityDetailState.Loaded -> state.details.symbol
        is SecurityDetailState.Error -> state.symbol
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(symbol, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (state is SecurityDetailState.Loaded && !state.details.name.isNullOrBlank()) {
                            Text(
                                state.details.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close details") }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuoteSummary(quote)
                    when (state) {
                        is SecurityDetailState.Loading -> {
                            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is SecurityDetailState.Error -> {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { onRetry(symbol) }) { Text("Retry") }
                        }
                        is SecurityDetailState.Loaded -> DetailContent(state.details)
                        SecurityDetailState.Closed -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteSummary(quote: QuoteResult?) {
    Text("Quote", style = MaterialTheme.typography.titleMedium)
    if (quote == null) {
        Text("Quote data is not available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SummaryValue("Last", Formatters.currency(quote.last))
        SummaryValue("Change", Formatters.changeWithSign(quote.chg))
        SummaryValue("Volume", Formatters.volume(quote.volume))
    }
    Text(
        "${quote.providerName} · ${Formatters.timestamp(quote.retrievedAtUtc)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider()
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailContent(details: SecurityDetails) {
    val fields = listOfNotNull(
        details.assetType?.let { "Type" to it },
        details.exchange?.let { "Exchange" to it },
        details.industry?.let { "Industry" to it },
        details.marketCapitalization?.let { "Market cap" to Formatters.currency(it) },
    )
    if (fields.isNotEmpty()) {
        Text("Details", style = MaterialTheme.typography.titleMedium)
        fields.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, modifier = Modifier.padding(start = 16.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    details.description?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    details.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    HorizontalDivider()
    Text("Recent news", style = MaterialTheme.typography.titleMedium)
    if (details.news.isEmpty()) {
        Text("No recent news was returned for this ticker.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        val uriHandler = LocalUriHandler.current
        details.news.forEach { article ->
            Column(
                modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(article.url) }.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(article.headline, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.OpenInNew, "Open article", modifier = Modifier.padding(start = 8.dp).size(18.dp))
                }
                Text(
                    listOfNotNull(article.source, article.publishedAtUtc?.let(Formatters::timestamp)).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                article.summary?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}