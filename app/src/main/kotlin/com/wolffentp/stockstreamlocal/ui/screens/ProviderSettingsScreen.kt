package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.data.repository.ProviderRepository
import com.wolffentp.stockstreamlocal.market.provider.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderSettingsViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
) : ViewModel() {
    val providerType = providerRepository.observeProviderType()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProviderType.NONE)
    val maskedApiKey get() = providerRepository.getApiKeyMasked()
    val isConfigured get() = providerRepository.isProviderConfigured()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun save(type: ProviderType, apiKey: String) {
        viewModelScope.launch {
            providerRepository.saveProviderConfig(type, apiKey)
            _saved.value = true
        }
    }
    fun clear() { viewModelScope.launch { providerRepository.clearProviderConfig() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProviderSettingsViewModel = hiltViewModel(),
) {
    val currentType by viewModel.providerType.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    var selectedType by remember(currentType) { mutableStateOf(currentType) }
    var apiKey by remember { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(saved) { if (saved) { snackbarHost.showSnackbar("Provider saved."); onNavigateBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Settings") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Configure the market data provider used for live quotes.", style = MaterialTheme.typography.bodyMedium)

            // Provider picker
            Text("Provider", style = MaterialTheme.typography.labelLarge)
            ProviderType.values().forEach { type ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                    Spacer(Modifier.width(8.dp))
                    Text(type.displayName)
                }
            }

            if (selectedType != ProviderType.NONE) {
                Text("Current key: ${viewModel.maskedApiKey}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Leave blank to keep existing key.") },
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "⚠ API keys stored on a mobile device can potentially be extracted by a determined attacker. Because this app is local-only, no backend proxy is available. Use a provider key with read-only permissions and minimum required scopes.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Button(
                onClick = { viewModel.save(selectedType, apiKey) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }

            if (viewModel.isConfigured) {
                OutlinedButton(
                    onClick = { viewModel.clear() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Clear Provider Config") }
            }
        }
    }
}
