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
import com.wolffentp.stockstreamlocal.rotation.RotatingViewDefinition
import com.wolffentp.stockstreamlocal.ui.viewmodel.RotatingViewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotatingViewsEditorScreen(
    onEditLayout: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RotatingViewViewModel = hiltViewModel(),
) {
    val views by viewModel.views.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rotating Views") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, "Create view") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(views, key = { it.id }) { view ->
                RotatingViewRow(
                    view = view,
                    onEditLayout = { onEditLayout(view.id) },
                    onDelete = { viewModel.deleteView(view.id) },
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Custom View") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("View name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = { if (name.isNotBlank()) { viewModel.createCustomView(name); showCreateDialog = false } }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RotatingViewRow(
    view: RotatingViewDefinition,
    onEditLayout: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(view.displayName) },
        supportingContent = {
            Text("${view.viewType.displayName} · ${view.rotationIntervalSeconds}s · ${view.columnNames.size} columns",
                style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEditLayout) { Icon(Icons.Default.Edit, "Edit layout") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        },
        leadingContent = { Icon(Icons.Default.ViewList, null) },
    )
}
