package com.wolffentp.stockstreamlocal.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.wolffentp.stockstreamlocal.auth.AuthManager
import com.wolffentp.stockstreamlocal.auth.PinManager
import com.wolffentp.stockstreamlocal.data.datastore.AppPreferences
import com.wolffentp.stockstreamlocal.data.datastore.AppPreferencesSerializer
import com.wolffentp.stockstreamlocal.data.db.AppDatabase
import com.wolffentp.stockstreamlocal.security.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

val Context.appPreferencesStore: DataStore<AppPreferences> by dataStore(
    fileName = "app_preferences.json",
    serializer = AppPreferencesSerializer,
)

/** Minimum allowed quote-refresh polling interval in seconds (provider rate-limit safety). */
const val MIN_REFRESH_INTERVAL_SECONDS = 15

/** Maximum allowed quote-refresh polling interval in seconds (1 hour). */
const val MAX_REFRESH_INTERVAL_SECONDS = 3600

/**
 * Orchestrates all user-facing settings operations:
 *
 *  1. **Log in / Log out** — lock/unlock the local app via PIN or biometric.
 *  2. **Clear context** — wipe all local data (tickers, portfolio, quotes, layouts, provider
 *     config, secure prefs) and reset to fresh-install state.
 *  3. **Set refresh interval** — configure the quote-polling cadence with rate-limit clamping.
 *  4. **General preferences** — theme, debug mode, rotation, always-on display, etc.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val pinManager: PinManager,
    private val secureStorage: SecureStorage,
    private val db: AppDatabase,
) {
    val preferencesFlow: Flow<AppPreferences> = context.appPreferencesStore.data
        .catch { emit(AppPreferences()) }

    // ─── Log in / Log out (local PIN auth) ────────────────────────────────────

    /**
     * Lock the app. Navigates the user to the lock/PIN screen.
     * This is the "log out" action in a local-only app — no server session is terminated.
     */
    fun lockApp() = authManager.lock()

    /**
     * Set and enable a new PIN for local app access.
     * The raw [pin] array is hashed in [PinManager] and then zeroed from memory.
     */
    suspend fun enablePin(pin: CharArray) {
        pinManager.setPin(pin)
        updatePrefs { it.copy(isPinEnabled = true) }
    }

    /**
     * Disable PIN protection and clear all stored PIN credentials.
     */
    suspend fun disablePin() {
        pinManager.clearPin()
        updatePrefs { it.copy(isPinEnabled = false, isBiometricEnabled = false) }
    }

    /** Enable or disable biometric unlock alongside PIN. */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        updatePrefs { it.copy(isBiometricEnabled = enabled) }
    }

    // ─── Set refresh interval ──────────────────────────────────────────────────

    /**
     * Set the quote-polling interval in seconds.
     * Automatically clamped to [[MIN_REFRESH_INTERVAL_SECONDS], [MAX_REFRESH_INTERVAL_SECONDS]]
     * to prevent provider rate-limit violations.
     *
     * @param seconds Desired interval. Values below 15 are raised to 15.
     */
    suspend fun setRefreshInterval(seconds: Int) {
        val clamped = seconds.coerceIn(MIN_REFRESH_INTERVAL_SECONDS, MAX_REFRESH_INTERVAL_SECONDS)
        updatePrefs { it.copy(quoteRefreshIntervalSeconds = clamped) }
    }

    /** Set how long each rotating view is displayed before advancing. Minimum 5 seconds. */
    suspend fun setRotationInterval(seconds: Int) {
        updatePrefs { it.copy(rotationIntervalSeconds = seconds.coerceAtLeast(5)) }
    }

    // ─── Clear context ─────────────────────────────────────────────────────────

    /**
     * Full context wipe — equivalent to a factory reset of the app's local state:
     *  - All Room tables cleared (tickers, lots, snapshots, layouts, views, provider config)
     *  - Encrypted SharedPreferences cleared (API key, PIN hash/salt)
     *  - DataStore preferences reset to defaults
     *  - App moved to [AuthState.NoPinConfigured] (lock screen skipped on next launch)
     *
     * The app binary and Android system data are untouched.
     */
    suspend fun clearAllContext() {
        // 1. Room — clear every table
        db.quoteSnapshotDao().deleteAll()
        db.portfolioLotDao().deleteAll()
        db.tickerDao().deleteAll()
        db.columnLayoutDao().deleteAll()
        db.rotatingViewDao().deleteAll()
        db.providerConfigDao().deleteAll()

        // 2. Secure storage — API key, PIN hash, provider type
        secureStorage.clearAll()

        // 3. DataStore preferences — reset to defaults
        context.appPreferencesStore.updateData { AppPreferences() }

        // 4. Auth — PIN cleared so initialize skips lock screen
        authManager.initialize(AppPreferences())
    }

    /**
     * Clear only live/cached market data (quote snapshots and portfolio lots).
     * Retains tickers, layouts, provider configuration, and auth settings.
     * Useful for a clean re-fetch without full reset.
     */
    suspend fun clearMarketData() {
        db.quoteSnapshotDao().deleteAll()
        db.portfolioLotDao().deleteAll()
    }

    /**
     * Clear watchlist tickers and imported portfolio data while keeping
     * layouts, provider config, and auth settings intact.
     */
    suspend fun clearWatchlistAndPortfolio() {
        db.quoteSnapshotDao().deleteAll()
        db.portfolioLotDao().deleteAll()
        db.tickerDao().deleteAll()
    }

    // ─── General preferences ──────────────────────────────────────────────────

    suspend fun setThemeMode(mode: String) = updatePrefs { it.copy(themeMode = mode) }
    suspend fun setDebugMode(enabled: Boolean) = updatePrefs { it.copy(isDebugModeEnabled = enabled) }
    suspend fun setShowImportedBaseline(show: Boolean) = updatePrefs { it.copy(showImportedBaseline = show) }
    suspend fun setPortfolioViewMode(mode: String) = updatePrefs { it.copy(portfolioViewMode = mode) }
    suspend fun setRotationPaused(paused: Boolean) = updatePrefs { it.copy(isRotationPaused = paused) }
    suspend fun setAlwaysOn(enabled: Boolean) = updatePrefs { it.copy(isAlwaysOnEnabled = enabled) }

    suspend fun getCurrentPreferences(): AppPreferences =
        context.appPreferencesStore.data.first()

    // ─── Private ──────────────────────────────────────────────────────────────

    private suspend fun updatePrefs(transform: (AppPreferences) -> AppPreferences) {
        context.appPreferencesStore.updateData(transform)
    }
}
