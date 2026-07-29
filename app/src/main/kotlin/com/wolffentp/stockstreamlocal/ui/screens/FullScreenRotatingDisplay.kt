package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.columns.AllColumns
import com.wolffentp.stockstreamlocal.ui.components.QuoteRow
import com.wolffentp.stockstreamlocal.ui.components.StatusBanner
import com.wolffentp.stockstreamlocal.ui.viewmodel.QuoteViewModel
import com.wolffentp.stockstreamlocal.ui.viewmodel.RotatingViewViewModel

/**
 * Full-screen rotating display with always-on support and burn-in mitigation.
 */
@Composable
fun FullScreenRotatingDisplay(
    onExit: () -> Unit,
    quoteVm: QuoteViewModel = hiltViewModel(),
    rotationVm: RotatingViewViewModel = hiltViewModel(),
) {
    val quotes by quoteVm.quotes.collectAsStateWithLifecycle()
    val refreshState by quoteVm.refreshState.collectAsStateWithLifecycle()
    val isOnline by quoteVm.isOnline.collectAsStateWithLifecycle()
    val currentView by rotationVm.currentView.collectAsStateWithLifecycle()
    val views by rotationVm.views.collectAsStateWithLifecycle()

    // Subtle burn-in mitigation: shift content position every minute
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }
    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            kotlinx.coroutines.delay(60_000)
            tick++
            offsetX = if (tick % 2 == 0) 0.dp else 2.dp
            offsetY = if (tick % 3 == 0) 0.dp else 2.dp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = offsetX, y = offsetY),
        ) {
            // Header bar with view name and controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currentView?.displayName ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (views.size > 1) {
                    IconButton(onClick = { rotationVm.previous() }) { Icon(Icons.Default.NavigateBefore, "Previous") }
                    IconButton(onClick = { rotationVm.next() }) { Icon(Icons.Default.NavigateNext, "Next") }
                }
                IconButton(onClick = { rotationVm.pause() }) { Icon(Icons.Default.Pause, "Pause rotation") }
                IconButton(onClick = onExit) { Icon(Icons.Default.FullscreenExit, "Exit") }
            }

            StatusBanner(refreshState = refreshState, isOnline = isOnline)

            val visibleColumns = currentView?.let { v ->
                AllColumns.definitions.filter { it.name in v.columnNames && it.name !in v.hiddenColumnNames }
            } ?: AllColumns.definitions.filter { it.defaultVisible }

            // Column headers
            if (visibleColumns.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    visibleColumns.forEach { col ->
                        Text(col.displayLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    }
                }
                HorizontalDivider()
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(quotes.values.toList(), key = { it.symbol }) { quote ->
                    QuoteRow(quote = quote, visibleColumns = visibleColumns, onClick = {})
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
