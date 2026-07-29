package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.auth.AuthState
import com.wolffentp.stockstreamlocal.settings.MIN_REFRESH_INTERVAL_SECONDS
import com.wolffentp.stockstreamlocal.settings.MAX_REFRESH_INTERVAL_SECONDS
import com.wolffentp.stockstreamlocal.settings.SettingsUiState
import com.wolffentp.stockstreamlocal.settings.SettingsViewModel

/**
 * Settings screen exposing primary functions:
 *  - Log in / Log out (local PIN session)
 *  - Market data provider configuration
 *  - Clear context  (factory reset of all local data)
 *  - Set quote-refresh interval
 *  - General display / rotation preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onContextCleared: () -> Unit,  // navigate to lock/onboard after full clear
    onNavigateToProviderSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialogs
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showClearMarketDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }

    // React to UI state
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is SettingsUiState.ContextCleared -> {
                viewModel.dismissUiState()
                onContextCleared()
            }
            is SettingsUiState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.dismissUiState()
            }
            is SettingsUiState.Error -> {
                snackbarHostState.showSnackbar("Error: ${s.message}")
                viewModel.dismissUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Section: App Access (Log in / Log out) ───────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.Lock,
                title = "App Access",
            )

            // Log out (lock) — only shown when authenticated
            if (authState == AuthState.Authenticated || authState == AuthState.NoPinConfigured) {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = "Lock App",
                    subtitle = "End current session and return to lock screen.",
                    onClick = { viewModel.lockApp() },
                )
            }

            val isPinEnabled = prefs.isPinEnabled
            if (isPinEnabled) {
                SettingsItem(
                    icon = Icons.Default.PinEnd,
                    title = "Disable PIN",
                    subtitle = "Remove PIN protection. App will open without authentication.",
                    onClick = { showDisablePinDialog = true },
                )
            } else {
                SettingsItem(
                    icon = Icons.Default.Pin,
                    title = "Set PIN",
                    subtitle = "Enable PIN lock for local app access.",
                    onClick = { showSetPinDialog = true },
                )
            }

            if (isPinEnabled) {
                SettingsSwitchItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face unlock instead of PIN.",
                    checked = prefs.isBiometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section: Market Data ─────────────────────────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.Tune,
                title = "Market Data",
            )

            SettingsItem(
                icon = Icons.Default.Cloud,
                title = "Provider Settings",
                subtitle = "Configure API key, select data provider (Finnhub, Alpha Vantage).",
                onClick = onNavigateToProviderSettings,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section: Refresh Interval ────────────────────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.Timer,
                title = "Refresh Intervals",
            )

            SettingsItem(
                icon = Icons.Default.Refresh,
                title = "Quote Refresh Interval",
                subtitle = buildString {
                    append("Current: ${prefs.quoteRefreshIntervalSeconds}s  ")
                    append("(min ${MIN_REFRESH_INTERVAL_SECONDS}s – max ${MAX_REFRESH_INTERVAL_SECONDS / 60}min)")
                },
                onClick = { showRefreshDialog = true },
            )

            SettingsItem(
                icon = Icons.Default.SlowMotionVideo,
                title = "View Rotation Interval",
                subtitle = "Current: ${prefs.rotationIntervalSeconds}s per view",
                onClick = { showRefreshDialog = true /* reuse same dialog with a flag */ },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section: Clear Context ───────────────────────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.DeleteSweep,
                title = "Clear Local Data",
            )

            SettingsItem(
                icon = Icons.Default.CloudOff,
                title = "Clear Market Data",
                subtitle = "Remove cached quote snapshots and imported portfolio lots. Keeps tickers and settings.",
                tintError = false,
                onClick = { showClearMarketDialog = true },
            )

            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Clear All Context",
                subtitle = "Factory reset — removes all tickers, portfolio data, settings, and PIN. Cannot be undone.",
                tintError = true,
                onClick = { showClearAllDialog = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section: Display ─────────────────────────────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = "Display",
            )

            SettingsSwitchItem(
                icon = Icons.Default.TableRows,
                title = "Show Imported Baseline",
                subtitle = "Display CSV-imported values with IMPORTED badge alongside live quotes.",
                checked = prefs.showImportedBaseline,
                onCheckedChange = { viewModel.setShowImportedBaseline(it) },
            )

            SettingsSwitchItem(
                icon = Icons.Default.BrightnessHigh,
                title = "Always-On Display",
                subtitle = "Keep screen on during rotating display. Layout shifts periodically to reduce burn-in.",
                checked = prefs.isAlwaysOnEnabled,
                onCheckedChange = { viewModel.setAlwaysOn(it) },
            )

            SettingsSwitchItem(
                icon = Icons.Default.BugReport,
                title = "Debug Mode",
                subtitle = "Show additional data source diagnostics in quote rows. Does not log sensitive values.",
                checked = prefs.isDebugModeEnabled,
                onCheckedChange = { viewModel.setDebugMode(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Local-only notice
            Text(
                text = "All data is stored locally on this device. No cloud services are used.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onConfirm = { pin ->
                viewModel.enablePin(pin)
                showSetPinDialog = false
            },
        )
    }

    if (showDisablePinDialog) {
        ConfirmDialog(
            title = "Disable PIN",
            body = "This will remove PIN protection. Anyone with physical access to the device will be able to open the app.",
            confirmLabel = "Disable PIN",
            onDismiss = { showDisablePinDialog = false },
            onConfirm = {
                viewModel.disablePin()
                showDisablePinDialog = false
            },
        )
    }

    if (showRefreshDialog) {
        RefreshIntervalDialog(
            currentQuoteInterval = prefs.quoteRefreshIntervalSeconds,
            currentRotationInterval = prefs.rotationIntervalSeconds,
            onDismiss = { showRefreshDialog = false },
            onConfirm = { quoteSeconds, rotationSeconds ->
                viewModel.setRefreshInterval(quoteSeconds)
                viewModel.setRotationInterval(rotationSeconds)
                showRefreshDialog = false
            },
        )
    }

    if (showClearMarketDialog) {
        ConfirmDialog(
            title = "Clear Market Data",
            body = "This removes all cached quote snapshots and imported portfolio lots. Tickers and settings are kept. This cannot be undone.",
            confirmLabel = "Clear Market Data",
            onDismiss = { showClearMarketDialog = false },
            onConfirm = {
                viewModel.clearMarketData(confirmed = true)
                showClearMarketDialog = false
            },
        )
    }

    if (showClearAllDialog) {
        ConfirmDialog(
            title = "Clear All Local Data",
            body = "This performs a factory reset of the app:\n\n• All tickers removed\n• All portfolio data removed\n• All quote history removed\n• Provider API key removed\n• PIN removed\n• All settings reset\n\nThis cannot be undone.",
            confirmLabel = "Clear Everything",
            isDestructive = true,
            onDismiss = { showClearAllDialog = false },
            onConfirm = {
                viewModel.clearAllContext(confirmed = true)
                showClearAllDialog = false
            },
        )
    }

    if (uiState is SettingsUiState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp, end = 16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tintError: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (tintError) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (tintError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            if (isDestructive) Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(title) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SetPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set App PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set a local PIN to protect app access. The PIN is stored securely on this device only.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 12) pin = it.filter { c -> c.isDigit() } },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 12) confirmPin = it.filter { c -> c.isDigit() } },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    pin.length < 4 -> error = "PIN must be at least 4 digits."
                    pin != confirmPin -> error = "PINs do not match."
                    else -> {
                        onConfirm(pin.toCharArray())
                        pin = ""
                        confirmPin = ""
                    }
                }
            }) { Text("Save PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RefreshIntervalDialog(
    currentQuoteInterval: Int,
    currentRotationInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (quoteSeconds: Int, rotationSeconds: Int) -> Unit,
) {
    var quoteInput by remember { mutableStateOf(currentQuoteInterval.toString()) }
    var rotationInput by remember { mutableStateOf(currentRotationInterval.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Refresh Intervals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Quote refresh
                Text("Quote Refresh", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Minimum: ${MIN_REFRESH_INTERVAL_SECONDS}s (provider rate-limit safety)\nMaximum: ${MAX_REFRESH_INTERVAL_SECONDS / 60} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = quoteInput,
                    onValueChange = { quoteInput = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Interval (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("s") },
                )

                HorizontalDivider()

                // View rotation
                Text("View Rotation", style = MaterialTheme.typography.labelLarge)
                Text("Minimum: 5s per view", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = rotationInput,
                    onValueChange = { rotationInput = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Interval (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("s") },
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val quoteSeconds = quoteInput.toIntOrNull()
                val rotationSeconds = rotationInput.toIntOrNull()
                when {
                    quoteSeconds == null || quoteSeconds < 1 -> error = "Enter a valid quote interval."
                    rotationSeconds == null || rotationSeconds < 1 -> error = "Enter a valid rotation interval."
                    else -> {
                        error = null
                        onConfirm(quoteSeconds, rotationSeconds)
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
