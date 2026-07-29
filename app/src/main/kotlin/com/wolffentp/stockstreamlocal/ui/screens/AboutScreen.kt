package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("StockStream Local", style = MaterialTheme.typography.headlineSmall)
            Text("Version 1.0.0 · Local-only Android application", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Section("Local-Only Architecture") {
                Text("This app runs entirely on your Android device. No Azure, no backend server, no cloud service, no developer-controlled infrastructure is used.\n\nAll data — tickers, portfolio lots, quote snapshots, column layouts, rotating view definitions, preferences, and authentication metadata — is stored in local Room database and DataStore files on this device only.")
            }
            Section("Authentication") {
                Text("App access is protected by a local PIN hashed with PBKDF2-SHA256 (200,000 iterations) and stored in Android Keystore-backed EncryptedSharedPreferences. No password, PIN, or biometric state is transmitted anywhere. No Microsoft Entra ID, MSAL, OAuth, or cloud identity is used.")
            }
            Section("Market Data") {
                Text("Live quotes are fetched directly from the configured market data provider (e.g. Alpha Vantage) using an API key stored locally. See Provider Settings for configuration.\n\n⚠ API keys stored on mobile devices can potentially be extracted. Use a key with minimal read-only permissions.")
            }
            Section("Data Labels") {
                Text("Every quote field displays a badge: LIVE, DELAYED, STALE, IMPORTED, CALCULATED, NOT PROVIDED, or UNAVAILABLE. No field is ever guessed or synthesized. If the provider does not return a value, the field is blank with NOT PROVIDED badge.")
            }
            Section("Privacy") {
                Text("No telemetry, analytics, or usage data is collected or transmitted. No portfolio values, account names, or financial data leave this device. Cloud backup is disabled.")
            }
            Section("Known Limitations") {
                listOf(
                    "Direct provider API access from a mobile device may expose the API key to extraction.",
                    "Some providers do not permit client-side mobile usage — check your provider's terms.",
                    "True passkey / server-backed authentication is not included (requires a backend).",
                    "Microsoft Entra / MSAL login is excluded — cloud identity is not local-only.",
                    "AlphaVantage free tier: 25 requests/day, 5 requests/minute. Bid/Ask and 52-week range not available.",
                ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
    HorizontalDivider()
}
