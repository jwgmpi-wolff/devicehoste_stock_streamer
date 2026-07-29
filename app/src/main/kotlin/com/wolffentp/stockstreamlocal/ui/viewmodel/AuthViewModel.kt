package com.wolffentp.stockstreamlocal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wolffentp.stockstreamlocal.auth.AuthManager
import com.wolffentp.stockstreamlocal.auth.AuthState
import com.wolffentp.stockstreamlocal.auth.BiometricHelper
import com.wolffentp.stockstreamlocal.data.datastore.AppPreferences
import com.wolffentp.stockstreamlocal.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authManager.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Locked)

    val preferences: StateFlow<AppPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    fun initialize() {
        viewModelScope.launch {
            val prefs = settingsRepository.getCurrentPreferences()
            authManager.initialize(prefs)
        }
    }

    fun submitPin(pin: CharArray) = authManager.submitPin(pin)

    fun onBiometricSuccess() = authManager.onBiometricSuccess()

    fun startPinEntry() = authManager.startPinEntry()
}
