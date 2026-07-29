package com.wolffentp.stockstreamlocal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wolffentp.stockstreamlocal.auth.AuthState
import com.wolffentp.stockstreamlocal.auth.BiometricHelper
import com.wolffentp.stockstreamlocal.ui.viewmodel.AuthViewModel

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.initialize() }

    LaunchedEffect(authState) {
        if (authState == AuthState.Authenticated || authState == AuthState.NoPinConfigured) {
            onUnlocked()
        }
    }

    var pin by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text("StockStream Local", style = MaterialTheme.typography.headlineMedium)
            Text("Enter PIN to unlock", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 12) pin = it.filter { c -> c.isDigit() } },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            val pinState = authState as? AuthState.PinEntry
            if (pinState?.errorMessage != null) {
                Text(pinState.errorMessage, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall)
            }

            Button(
                onClick = {
                    viewModel.submitPin(pin.toCharArray())
                    pin = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.isNotBlank(),
            ) { Text("Unlock") }

            if (prefs.isBiometricEnabled && BiometricHelper.isBiometricAvailable(context)) {
                OutlinedButton(
                    onClick = {
                        BiometricHelper.showPrompt(
                            activity = context as FragmentActivity,
                            title = "Biometric Unlock",
                            subtitle = "Use your biometric to unlock StockStream Local",
                            cancelText = "Use PIN",
                            onSuccess = { viewModel.onBiometricSuccess() },
                            onError = { _, _ -> },
                            onFailed = {},
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Use Biometric") }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "All data is stored locally on this device only.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
