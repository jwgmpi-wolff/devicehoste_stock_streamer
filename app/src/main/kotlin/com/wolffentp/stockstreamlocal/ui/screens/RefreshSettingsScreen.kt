package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wolffentp.stockstreamlocal.settings.SettingsViewModel

/** Thin screen — full interval UI is embedded in SettingsScreen; this is a standalone route. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refresh Settings") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Configure quote polling and view rotation intervals in Settings → Refresh Intervals.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Open Settings") }
        }
    }
}
