package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.ui.viewmodel.AddTickerState
import com.wolffentp.stockstreamlocal.ui.viewmodel.WatchlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTickerScreen(
    onNavigateBack: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    var symbol by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(addState) {
        when (val s = addState) {
            is AddTickerState.Added -> { snackbarHost.showSnackbar("${s.symbol} added to watchlist."); viewModel.resetAddState(); onNavigateBack() }
            is AddTickerState.Warning -> { snackbarHost.showSnackbar(s.message); viewModel.resetAddState(); onNavigateBack() }
            is AddTickerState.Error -> { snackbarHost.showSnackbar(s.message); viewModel.resetAddState() }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Ticker") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '^' }.take(10) },
                label = { Text("Symbol (e.g. AAPL)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name (optional)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "The symbol will be validated against the configured market data provider.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { viewModel.addTicker(symbol, displayName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = symbol.isNotBlank() && addState !is AddTickerState.Validating,
            ) {
                if (addState is AddTickerState.Validating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Add to Watchlist")
            }
        }
    }
}
