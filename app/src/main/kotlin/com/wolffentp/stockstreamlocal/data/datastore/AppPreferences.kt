package com.wolffentp.stockstreamlocal.data.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Application preferences stored in DataStore.
 * Sensitive values (API key, PIN hash) are stored in [com.wolffentp.stockstreamlocal.security.SecureStorage],
 * not here.
 */
@Serializable
data class AppPreferences(
    val quoteRefreshIntervalSeconds: Int = 60,
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val defaultViewId: String = "quote",
    val rotationIntervalSeconds: Int = 30,
    val isRotationPaused: Boolean = false,
    val isAlwaysOnEnabled: Boolean = false,
    val isDebugModeEnabled: Boolean = false,
    val showImportedBaseline: Boolean = true,
    val portfolioViewMode: String = "AGGREGATE", // "AGGREGATE" or "PER_ACCOUNT"
    val lastProviderType: String = "NONE",
    val themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
)

object AppPreferencesSerializer : Serializer<AppPreferences> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: AppPreferences = AppPreferences()

    override suspend fun readFrom(input: InputStream): AppPreferences {
        return try {
            json.decodeFromString(AppPreferences.serializer(), input.readBytes().decodeToString())
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppPreferences, output: OutputStream) {
        output.write(json.encodeToString(AppPreferences.serializer(), t).encodeToByteArray())
    }
}
