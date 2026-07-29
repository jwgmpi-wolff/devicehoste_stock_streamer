package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.ui.components.DraggableColumnRow
import com.wolffentp.stockstreamlocal.ui.viewmodel.ColumnLayoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnLayoutEditorScreen(
    viewId: String,
    onNavigateBack: () -> Unit,
    viewModel: ColumnLayoutViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewId) { viewModel.setViewId(viewId) }
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Column Layout") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = { viewModel.resetToDefault() }) { Text("Reset") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        if (layout == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator() }
            return@Scaffold
        }
        val l = layout!!
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Tap the eye icon to show/hide columns. Drag the handle to reorder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            LazyColumn {
                itemsIndexed(l.orderedColumns, key = { _, col -> col.name }) { _, col ->
                    DraggableColumnRow(
                        column = col,
                        isHidden = col.name in l.hiddenColumnNames,
                        onToggleVisibility = { viewModel.toggleColumnVisibility(col.name) },
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
