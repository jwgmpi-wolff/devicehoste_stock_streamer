package com.wolffentp.stockstreamlocal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.csv.CsvImportResult
import com.wolffentp.stockstreamlocal.ui.viewmodel.CsvImportViewModel
import com.wolffentp.stockstreamlocal.ui.viewmodel.ImportState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    onNavigateToValidation: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CsvImportViewModel = hiltViewModel(),
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    var awaitingFidelityExport by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentAwaitingFidelityExport by rememberUpdatedState(awaitingFidelityExport)

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::parseFile)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && currentAwaitingFidelityExport) {
                awaitingFidelityExport = false
                filePicker.launch("text/*")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Parsed -> if (s.result is CsvImportResult.Success) onNavigateToValidation()
                else when (val result = s.result) {
                    is CsvImportResult.HeaderError -> snackbarHost.showSnackbar("Missing required columns: ${result.missingColumns.joinToString()}")
                    is CsvImportResult.ParseError -> snackbarHost.showSnackbar(result.message)
                    is CsvImportResult.EmptyFile -> snackbarHost.showSnackbar(result.message)
                    else -> {}
                }
            is ImportState.Error -> snackbarHost.showSnackbar(s.message)
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Portfolio") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Logging in alone does not transfer positions. Fidelity must export a CSV for StockStream to reconcile.", style = MaterialTheme.typography.bodyMedium)

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How to sync", style = MaterialTheme.typography.titleMedium)
                    Text("1. Tap Open Fidelity & Export.")
                    Text("2. Sign in and open the Positions page.")
                    Text("3. Use Fidelity's Download or Export control to save the positions CSV.")
                    Text("4. Return to StockStream and select the downloaded CSV when prompted.")
                    Text("5. Review the holdings, then confirm reconciliation.")
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.OpenInBrowser, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Fidelity login opens in your system browser.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("StockStream never receives or stores your Fidelity credentials.", style = MaterialTheme.typography.labelSmall)
                    Text("Imported values are treated as historical baseline only.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("They will NOT be labeled as live market data.", style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(
                onClick = {
                    awaitingFidelityExport = true
                    try {
                        uriHandler.openUri(FIDELITY_POSITIONS_URL)
                    } catch (exception: IllegalArgumentException) {
                        awaitingFidelityExport = false
                        coroutineScope.launch {
                            snackbarHost.showSnackbar("No browser is available to open Fidelity.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.OpenInBrowser, null)
                Spacer(Modifier.width(8.dp))
                Text("Open Fidelity & Export")
            }

            OutlinedButton(
                onClick = { filePicker.launch("text/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = importState !is ImportState.Parsing,
            ) {
                if (importState is ImportState.Parsing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Parsing…")
                } else {
                    Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose CSV Manually")
                }
            }

            Text("The manual picker supports Fidelity and common broker positions, plus Coinbase transaction-history CSV exports. Coinbase requires complete history to estimate remaining holdings and cost basis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val FIDELITY_POSITIONS_URL = "https://digital.fidelity.com/ftgw/digital/portfolio/positions"
