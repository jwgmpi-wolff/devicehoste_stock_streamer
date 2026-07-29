package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.csv.CsvImportResult
import com.wolffentp.stockstreamlocal.ui.viewmodel.CsvImportViewModel
import com.wolffentp.stockstreamlocal.ui.viewmodel.ImportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportValidationScreen(
    onNavigateBack: () -> Unit,
    viewModel: CsvImportViewModel = hiltViewModel(),
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showAddSymbolsDialog by remember { mutableStateOf(false) }
    var newSymbols by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Imported -> {
                if (s.newSymbols.isNotEmpty()) { newSymbols = s.newSymbols; showAddSymbolsDialog = true }
                else snackbarHost.showSnackbar("Imported ${s.importedCount} rows.")
            }
            else -> {}
        }
    }

    val result = (importState as? ImportState.Parsed)?.result
    val fileName = (importState as? ImportState.Parsed)?.fileName ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Validation") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when (result) {
                is CsvImportResult.Success -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF1B5E20)); Spacer(Modifier.width(8.dp)); Text("Parse successful", style = MaterialTheme.typography.titleMedium) }
                                Text("File: $fileName", style = MaterialTheme.typography.bodySmall)
                                Text("Valid rows: ${result.validRows} / ${result.totalRows}", style = MaterialTheme.typography.bodySmall)
                                Text("Headers found: ${result.detectedHeaders.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                if (result.missingOptionalColumns.isNotEmpty())
                                    Text("Optional columns not found: ${result.missingOptionalColumns.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (result.rowErrors.isNotEmpty()) {
                        item { Text("Row errors (${result.rowErrors.size})", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp)) }
                        items(result.rowErrors) { err ->
                            ListItem(
                                headlineContent = { Text("Row ${err.rowIndex}: ${err.reason}", style = MaterialTheme.typography.bodySmall) },
                                leadingContent = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("⚠ All imported values are labeled IMPORTED BASELINE — not live market data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.confirmImport(result, fileName) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Import ${result.validRows} Rows") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.reset(); onNavigateBack() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                        Spacer(Modifier.height(24.dp))
                    }
                }
                is CsvImportResult.HeaderError -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Missing required columns", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.height(4.dp))
                                Text("Missing: ${result.missingColumns.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                Text("Found: ${result.foundHeaders.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                else -> { item { Text("No validation data available.", modifier = Modifier.padding(16.dp)) } }
            }
        }
    }

    if (showAddSymbolsDialog) {
        AlertDialog(
            onDismissRequest = { showAddSymbolsDialog = false; onNavigateBack() },
            title = { Text("Add to Watchlist?") },
            text = { Text("${newSymbols.size} new symbol(s) were found in the import:\n${newSymbols.joinToString(", ")}\n\nAdd them to your watchlist?") },
            confirmButton = {
                Button(onClick = { viewModel.addSymbolsToWatchlist(newSymbols); showAddSymbolsDialog = false; onNavigateBack() }) { Text("Add All") }
            },
            dismissButton = { TextButton(onClick = { showAddSymbolsDialog = false; onNavigateBack() }) { Text("Skip") } },
        )
    }
}
