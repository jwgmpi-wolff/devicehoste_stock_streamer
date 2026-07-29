package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.ui.components.DataSourceBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataQualityLegendScreen(onNavigateBack: () -> Unit) {
    val entries = listOf(
        DataSourceLabel.LIVE to "Data confirmed current by provider during active trading hours.",
        DataSourceLabel.DELAYED to "Provider indicates data is delayed (typically 15–20 minutes).",
        DataSourceLabel.STALE to "Data is from a prior successful refresh; provider did not respond this cycle.",
        DataSourceLabel.IMPORTED_BASELINE to "Value imported from CSV file. Historical/baseline only — not live.",
        DataSourceLabel.CALCULATED to "Value derived arithmetically from other fields. All inputs must be present.",
        DataSourceLabel.NOT_PROVIDED_BY_SOURCE to "This provider does not return this field. No value is inferred.",
        DataSourceLabel.UNSUPPORTED_BY_PROVIDER to "Symbol not recognized by the configured provider.",
        DataSourceLabel.ERROR to "A retrieval error occurred. Prior value not shown to prevent stale labeling.",
        DataSourceLabel.UNAVAILABLE to "No data has ever been received for this field.",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Source Legend") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Text(
                    "Every numeric field in a quote row carries one of these badges indicating where the value came from.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            items(entries) { (label, description) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DataSourceBadge(label = label, modifier = Modifier.alignByBaseline())
                    Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).alignByBaseline())
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
