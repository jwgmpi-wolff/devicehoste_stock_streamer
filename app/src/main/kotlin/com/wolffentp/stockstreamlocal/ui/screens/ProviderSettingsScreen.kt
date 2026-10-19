package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    val maskedApiKeyName get() = providerRepository.getApiKeyNameMasked()
    val customProviderName get() = providerRepository.getCustomProviderName()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun save(type: ProviderType, apiKey: String, apiKeyName: String = "", customProviderName: String = "") {
        viewModelScope.launch {
            _error.value = null
            try {
                providerRepository.saveProviderConfig(type, apiKey, apiKeyName, customProviderName)
                _saved.value = true
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            } catch (e: java.io.IOException) {
                _error.value = "Unable to save provider credentials. Please try again."
            } catch (e: java.security.GeneralSecurityException) {
                _error.value = "Unable to encrypt provider credentials."
            } catch (e: android.database.sqlite.SQLiteException) {
                _error.value = "Unable to save provider settings. Please try again."
            }
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
    val error by viewModel.error.collectAsStateWithLifecycle()
    var selectedType by remember(currentType) { mutableStateOf(currentType) }
    var apiKey by remember(selectedType) { mutableStateOf("") }
    var apiKeyName by remember(selectedType) { mutableStateOf("") }
    var customProviderName by remember(currentType) { mutableStateOf(viewModel.customProviderName) }
    val keepingProvider = selectedType == currentType &&
        (selectedType != ProviderType.CUSTOM || customProviderName.trim() == viewModel.customProviderName)
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
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
                if (selectedType == ProviderType.CUSTOM) {
                    OutlinedTextField(
                        value = customProviderName,
                        onValueChange = { customProviderName = it },
                        label = { Text("Provider Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Custom credentials are saved securely on this device. Quotes and streaming are not available until an API integration is added for this provider.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (selectedType == ProviderType.COINBASE) {
                    Text("Current API key name: ${if (keepingProvider) viewModel.maskedApiKeyName else "(not set)"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = apiKeyName,
                        onValueChange = { apiKeyName = it },
                        label = { Text("API Key Name") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text(if (keepingProvider) "Leave blank to keep the existing API key name." else "Enter the API key name for this provider.") },
                    )
                }
                Text("Current key: ${if (keepingProvider) viewModel.maskedApiKey else "(not set)"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (selectedType == ProviderType.COINBASE) "API Private Key" else "API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = selectedType != ProviderType.COINBASE,
                    maxLines = if (selectedType == ProviderType.COINBASE) 6 else 1,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(if (keepingProvider) "Leave blank to keep existing key." else "Enter an API key for this provider.") },
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "⚠ API keys stored on a mobile device can potentially be extracted by a determined attacker. Because this app is local-only, no backend proxy is available. Use a provider key with read-only permissions and minimum required scopes.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { viewModel.save(selectedType, apiKey, apiKeyName, customProviderName) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }

            if (currentType != ProviderType.NONE) {
                OutlinedButton(
                    onClick = { viewModel.clear() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Clear Provider Config") }
            }
        }
    }
}
