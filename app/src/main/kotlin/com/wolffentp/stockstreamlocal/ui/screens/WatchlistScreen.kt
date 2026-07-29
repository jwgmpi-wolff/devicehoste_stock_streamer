package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.data.model.TickerEntity
import com.wolffentp.stockstreamlocal.ui.viewmodel.WatchlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onAddTicker: () -> Unit,
    onTickerDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<TickerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTicker) {
                Icon(Icons.Default.Add, "Add ticker")
            }
        },
    ) { padding ->
        if (watchlist.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No tickers yet", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onAddTicker) { Text("Add Ticker") }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(watchlist, key = { it.symbol }) { ticker ->
                    ListItem(
                        headlineContent = { Text(ticker.symbol, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            Column {
                                if (ticker.displayName != ticker.symbol) Text(ticker.displayName)
                                if (ticker.notes.isNotBlank()) Text(ticker.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onTickerDetail(ticker.symbol) }) {
                                    Icon(Icons.Default.OpenInNew, "Detail")
                                }
                                IconButton(onClick = { showDeleteDialog = ticker }) {
                                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }

    showDeleteDialog?.let { ticker ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Remove ${ticker.symbol}?") },
            text = { Text("This removes the ticker from your watchlist. Imported portfolio lots for this symbol are retained.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.removeTicker(ticker.symbol); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } },
        )
    }
}
