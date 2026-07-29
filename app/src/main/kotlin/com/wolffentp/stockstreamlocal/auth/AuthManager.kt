package com.wolffentp.stockstreamlocal.auth

import com.wolffentp.stockstreamlocal.data.datastore.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central auth state manager.
 * Determines initial [AuthState] on app launch and handles PIN/biometric unlock.
 * Local-only — no network calls, no identity service.
 */
@Singleton
class AuthManager @Inject constructor(
    private val pinManager: PinManager,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Locked)
    val authState = _authState.asStateFlow()

    fun initialize(prefs: AppPreferences) {
        _authState.value = when {
            prefs.isPinEnabled && pinManager.isPinSet() -> AuthState.Locked
            else -> AuthState.NoPinConfigured
        }
    }

    fun startPinEntry() {
        _authState.value = AuthState.PinEntry()
    }

    fun submitPin(pin: CharArray): Boolean {
        val correct = pinManager.verifyPin(pin)
        _authState.value = if (correct) {
            AuthState.Authenticated
        } else {
            val current = _authState.value as? AuthState.PinEntry
            AuthState.PinEntry(
                attemptCount = (current?.attemptCount ?: 0) + 1,
                errorMessage = "Incorrect PIN. Try again.",
            )
        }
        return correct
    }

    fun onBiometricSuccess() {
        _authState.value = AuthState.Authenticated
    }

    fun lock() {
        _authState.value = AuthState.Locked
    }

    val isAuthenticated: Boolean
        get() = _authState.value == AuthState.Authenticated || _authState.value == AuthState.NoPinConfigured
}
