package com.wolffentp.stockstreamlocal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores provider configuration such as the selected provider type.
 * The actual API key is stored separately in Keystore-backed EncryptedSharedPreferences
 * via [com.wolffentp.stockstreamlocal.security.SecureStorage] — NOT in this entity.
 */
@Entity(tableName = "provider_config")
data class ProviderConfigEntity(
    @PrimaryKey val id: Int = 1,   // singleton row
    val providerType: String,      // ProviderType.name()
    val updatedAtUtc: Long = System.currentTimeMillis(),
)
