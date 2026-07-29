package com.wolffentp.stockstreamlocal.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wolffentp.stockstreamlocal.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE = "stockstream_secure_prefs"
private const val KEY_API_KEY = "provider_api_key"
private const val KEY_PROVIDER_TYPE = "provider_type"
private const val KEY_PIN_HASH = "pin_hash"
private const val KEY_PIN_SALT = "pin_salt"
private const val KEY_IS_PIN_SET = "is_pin_set"

/**
 * Wraps Android Keystore-backed [EncryptedSharedPreferences] for all sensitive local values.
 *
 * What is stored:
 * - Market data provider API key (encrypted AES-256-GCM via Android Keystore)
 * - Provider type selection
 * - PIN hash + salt (PBKDF2-SHA256; stored encrypted)
 * - PIN set flag
 *
 * What is NOT stored here:
 * - Raw PIN digits (never stored anywhere)
 * - Portfolio values
 * - Account names
 * - Biometric credentials (managed by Android BiometricPrompt / system)
 *
 * Backup behavior: android:allowBackup=false in manifest prevents cloud backup of these values.
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // ─── API Key ───────────────────────────────────────────────────────────────

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)
        ?: BuildConfig.DEFAULT_PROVIDER_API_KEY.ifBlank { null }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    // ─── Provider type ─────────────────────────────────────────────────────────

    fun saveProviderType(type: String) {
        prefs.edit().putString(KEY_PROVIDER_TYPE, type).apply()
    }

    fun getProviderType(): String? = prefs.getString(KEY_PROVIDER_TYPE, null)

    // ─── PIN ───────────────────────────────────────────────────────────────────

    fun savePinCredentials(hash: ByteArray, salt: ByteArray) {
        prefs.edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putBoolean(KEY_IS_PIN_SET, true)
            .apply()
    }

    fun getPinHash(): ByteArray? {
        val encoded = prefs.getString(KEY_PIN_HASH, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    fun getPinSalt(): ByteArray? {
        val encoded = prefs.getString(KEY_PIN_SALT, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    fun isPinSet(): Boolean = prefs.getBoolean(KEY_IS_PIN_SET, false)

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_IS_PIN_SET, false)
            .apply()
    }

    // ─── Reset all ─────────────────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
