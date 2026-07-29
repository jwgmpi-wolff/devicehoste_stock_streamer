package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.clickable
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
import com.wolffentp.stockstreamlocal.columns.AllColumns
import com.wolffentp.stockstreamlocal.market.provider.RefreshState
import com.wolffentp.stockstreamlocal.ui.components.AddHoldingsDialog
import com.wolffentp.stockstreamlocal.ui.components.QuoteRow
import com.wolffentp.stockstreamlocal.ui.components.StatusBanner
import com.wolffentp.stockstreamlocal.ui.viewmodel.QuoteViewModel
import com.wolffentp.stockstreamlocal.ui.viewmodel.RotatingViewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWatchlist: () -> Unit,
    onNavigateToCsvImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFullScreen: () -> Unit,
    onNavigateToLegend: () -> Unit,
    quoteVm: QuoteViewModel = hiltViewModel(),
    rotationVm: RotatingViewViewModel = hiltViewModel(),
) {
    val quotes by quoteVm.quotes.collectAsStateWithLifecycle()
    val sortedQuotes by quoteVm.sortedQuotes.collectAsStateWithLifecycle()
    val sortColumn by quoteVm.sortColumn.collectAsStateWithLifecycle()
    val sortAscending by quoteVm.sortAscending.collectAsStateWithLifecycle()
    val refreshState by quoteVm.refreshState.collectAsStateWithLifecycle()
    val isOnline by quoteVm.isOnline.collectAsStateWithLifecycle()
    val currentView by rotationVm.currentView.collectAsStateWithLifecycle()
    val views by rotationVm.views.collectAsStateWithLifecycle()
    val currentIndex by rotationVm.currentIndex.collectAsStateWithLifecycle()

    var showAddHoldingsDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { quoteVm.startPolling() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentView?.displayName ?: "StockStream Local")
                        if (views.size > 1) {
                            Text(
                                "${currentIndex + 1} / ${views.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { quoteVm.refreshNow() }) {
                        Icon(Icons.Default.Refresh, "Refresh now")
                    }
                    IconButton(onClick = onNavigateToFullScreen) {
                        Icon(Icons.Default.Fullscreen, "Full screen")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToWatchlist,
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text("Watchlist") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCsvImport,
                    icon = { Icon(Icons.Default.Upload, null) },
                    label = { Text("Import") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToLegend,
                    icon = { Icon(Icons.Default.Info, null) },
                    label = { Text("Legend") },
                )
            }
        },
        floatingActionButton = {
            if (views.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(onClick = { rotationVm.previous() }) {
                        Icon(Icons.Default.NavigateBefore, "Previous view")
                    }
                    SmallFloatingActionButton(onClick = { rotationVm.next() }) {
                        Icon(Icons.Default.NavigateNext, "Next view")
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StatusBanner(refreshState = refreshState, isOnline = isOnline)

            val visibleColumns = currentView?.let { view ->
                AllColumns.definitions.filter {
                    it.name in view.columnNames && it.name !in view.hiddenColumnNames
                }
            } ?: AllColumns.definitions.filter { it.defaultVisible }

            // Sortable column headers
            if (quotes.isNotEmpty() && visibleColumns.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    visibleColumns.forEach { col ->
                        val isActive = sortColumn == col.name
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { quoteVm.setSort(col.name) }
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                col.displayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isActive) {
                                Icon(
                                    imageVector = if (sortAscending) Icons.Default.ArrowUpward
                                                  else Icons.Default.ArrowDownward,
                                    contentDescription = if (sortAscending) "Sorted ascending"
                                                         else "Sorted descending",
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            if (quotes.isEmpty() && refreshState is RefreshState.Idle) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No tickers in watchlist", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onNavigateToWatchlist) { Text("Add Tickers") }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sortedQuotes, key = { it.symbol }) { quote ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            QuoteRow(
                                quote = quote,
                                visibleColumns = visibleColumns,
                                onClick = {},
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { showAddHoldingsDialog = quote.symbol },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.Default.Edit, "Add/edit holdings", Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // Holdings dialog
    showAddHoldingsDialog?.let { symbol ->
        AddHoldingsDialog(
            symbol = symbol,
            onConfirm = { qty, price ->
                quoteVm.addHolding(symbol, qty, price)
            },
            onDismiss = { showAddHoldingsDialog = null },
        )
    }
}
