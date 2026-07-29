package com.wolffentp.stockstreamlocal.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wolffentp.stockstreamlocal.auth.AuthManager
import com.wolffentp.stockstreamlocal.auth.AuthState
import com.wolffentp.stockstreamlocal.data.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Settings screen.
 *
 * Exposes four primary actions:
 *  1. [lockApp]          — Lock (log out) the local app session.
 *  2. [unlockApp]        — Unlock (log in) via PIN submission.
 *  3. [clearContext]     — Wipe all local data and reset app to factory state.
 *  4. [setRefreshInterval] — Configure the quote-polling cadence.
 *
 * Every action is asynchronous; [uiState] reflects loading / success / error.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    /** Live application preferences observed from DataStore. */
    val preferences: StateFlow<AppPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    /** Auth state forwarded from [AuthManager]. */
    val authState: StateFlow<AuthState> = authManager.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.NoPinConfigured)

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ─── Log in / Log out ─────────────────────────────────────────────────────

    /**
     * Lock the app immediately.
     * The NavHost should observe [authState] and navigate to the lock screen when Locked.
     */
    fun lockApp() {
        settingsRepository.lockApp()
    }

    /**
     * Attempt to unlock the app with a PIN.
     * @param pin The digits entered by the user (cleared from memory after use).
     */
    fun unlockApp(pin: CharArray) {
        val success = authManager.submitPin(pin)
        if (!success) {
            _uiState.value = SettingsUiState.Error("Incorrect PIN — try again.")
        }
    }

    /** Enable PIN protection with a new PIN. */
    fun enablePin(pin: CharArray) {
        viewModelScope.launch {
            runCatching { settingsRepository.enablePin(pin) }
                .onSuccess { _uiState.value = SettingsUiState.Success("PIN enabled.") }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Failed to set PIN.") }
        }
    }

    /** Disable PIN and biometric protection. */
    fun disablePin() {
        viewModelScope.launch {
            runCatching { settingsRepository.disablePin() }
                .onSuccess { _uiState.value = SettingsUiState.Success("PIN disabled.") }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Failed to disable PIN.") }
        }
    }

    /** Enable or disable biometric unlock (requires PIN to be enabled). */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    // ─── Refresh interval ─────────────────────────────────────────────────────

    /**
     * Set the quote-polling interval.
     * Values outside [[MIN_REFRESH_INTERVAL_SECONDS], [MAX_REFRESH_INTERVAL_SECONDS]] are
     * automatically clamped and the clamped value is surfaced in the UI state.
     */
    fun setRefreshInterval(seconds: Int) {
        val clamped = seconds.coerceIn(MIN_REFRESH_INTERVAL_SECONDS, MAX_REFRESH_INTERVAL_SECONDS)
        viewModelScope.launch {
            runCatching { settingsRepository.setRefreshInterval(clamped) }
                .onSuccess {
                    val msg = if (clamped != seconds)
                        "Refresh interval set to ${clamped}s (clamped to provider-safe range)."
                    else
                        "Refresh interval set to ${clamped}s."
                    _uiState.value = SettingsUiState.Success(msg)
                }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Failed to save interval.") }
        }
    }

    /** Set how long each rotating view displays before advancing. */
    fun setRotationInterval(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setRotationInterval(seconds)
        }
    }

    // ─── Clear context ────────────────────────────────────────────────────────

    /**
     * Wipe all locally stored application data and reset to factory state.
     * Requires explicit user confirmation via [confirmed] = true to prevent accidental calls.
     */
    fun clearAllContext(confirmed: Boolean) {
        if (!confirmed) return
        _uiState.value = SettingsUiState.Loading("Clearing all local data…")
        viewModelScope.launch {
            runCatching { settingsRepository.clearAllContext() }
                .onSuccess { _uiState.value = SettingsUiState.ContextCleared }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Clear failed.") }
        }
    }

    /**
     * Clear only live quote snapshots and imported portfolio data.
     * Keeps tickers, layouts, and provider config intact.
     */
    fun clearMarketData(confirmed: Boolean) {
        if (!confirmed) return
        viewModelScope.launch {
            runCatching { settingsRepository.clearMarketData() }
                .onSuccess { _uiState.value = SettingsUiState.Success("Market data cleared.") }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Clear failed.") }
        }
    }

    /**
     * Clear watchlist tickers and all imported portfolio lots.
     */
    fun clearWatchlistAndPortfolio(confirmed: Boolean) {
        if (!confirmed) return
        viewModelScope.launch {
            runCatching { settingsRepository.clearWatchlistAndPortfolio() }
                .onSuccess { _uiState.value = SettingsUiState.Success("Watchlist and portfolio cleared.") }
                .onFailure { _uiState.value = SettingsUiState.Error(it.message ?: "Clear failed.") }
        }
    }

    // ─── Other preferences ────────────────────────────────────────────────────

    fun setThemeMode(mode: String) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDebugMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDebugMode(enabled) }
    fun setShowImportedBaseline(show: Boolean) = viewModelScope.launch { settingsRepository.setShowImportedBaseline(show) }
    fun setPortfolioViewMode(mode: String) = viewModelScope.launch { settingsRepository.setPortfolioViewMode(mode) }
    fun setAlwaysOn(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAlwaysOn(enabled) }

    fun dismissUiState() {
        _uiState.value = SettingsUiState.Idle
    }
}

/** UI state emitted by [SettingsViewModel] to drive snackbars, loading overlays, and navigation. */
sealed class SettingsUiState {
    object Idle : SettingsUiState()
    data class Loading(val message: String) : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
    /** Emitted after [SettingsViewModel.clearAllContext] succeeds — triggers nav to onboarding/lock. */
    object ContextCleared : SettingsUiState()
}
