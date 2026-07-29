package com.wolffentp.stockstreamlocal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.csv.CsvImportResult
import com.wolffentp.stockstreamlocal.ui.viewmodel.CsvImportViewModel
import com.wolffentp.stockstreamlocal.ui.viewmodel.ImportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    onNavigateToValidation: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CsvImportViewModel = hiltViewModel(),
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri = it; viewModel.parseFile(it) }
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Parsed -> if (s.result is CsvImportResult.Success) onNavigateToValidation()
            is ImportState.Error -> snackbarHost.showSnackbar(s.message)
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Portfolio CSV") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Select a Fidelity-style CSV file from this device to import your portfolio holdings.", style = MaterialTheme.typography.bodyMedium)
            Text("Supported columns: Symbol, Last, Bid, Chg, Ask, Tdy G/L, Quantity, Volume, Day Range, 52 Wk Range, Purchase Price, Value, % Tdy G/L, G/L, % G/L, Account, Close Value, Earnings Date, Div Date, Prev Close", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Imported values are treated as historical baseline only.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("They will NOT be labeled as live market data.", style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(
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
                    Text("Select CSV File")
                }
            }
        }
    }
}
