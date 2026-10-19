package com.wolffentp.stockstreamlocal.data.repository

import com.wolffentp.stockstreamlocal.data.db.dao.ProviderConfigDao
import com.wolffentp.stockstreamlocal.data.model.ProviderConfigEntity
import com.wolffentp.stockstreamlocal.market.provider.ProviderType
import com.wolffentp.stockstreamlocal.security.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val dao: ProviderConfigDao,
    private val secureStorage: SecureStorage,
) {
    fun observeProviderType(): Flow<ProviderType> = dao.observe().map { entity ->
        entity?.let { runCatching { ProviderType.valueOf(it.providerType) }.getOrElse { ProviderType.NONE } }
            ?: ProviderType.NONE
    }

    fun getCustomProviderName(): String = secureStorage.getCustomProviderName().orEmpty()

    suspend fun saveProviderConfig(
        type: ProviderType,
        apiKey: String,
        apiKeyName: String = "",
        customProviderName: String = "",
    ) {
        if (type == ProviderType.NONE) {
            clearProviderConfig()
            return
        }
        val name = customProviderName.trim()
        val sameProvider = secureStorage.getProviderType() == type.name &&
            (type != ProviderType.CUSTOM || name == getCustomProviderName())
        require(type != ProviderType.CUSTOM || name.isNotBlank()) {
            "Enter a custom provider name."
        }
        require(apiKey.isNotBlank() || (sameProvider && !secureStorage.getApiKey().isNullOrBlank())) {
            "Enter an API key for the selected provider."
        }
        require(type != ProviderType.COINBASE || apiKeyName.isNotBlank() ||
            (sameProvider && !secureStorage.getApiKeyName().isNullOrBlank())) {
            "Enter the Coinbase API key name."
        }
        if (!sameProvider) {
            secureStorage.clearApiKey()
            secureStorage.clearApiKeyName()
        }
        secureStorage.saveProviderType(type.name)
        if (apiKey.isNotBlank()) secureStorage.saveApiKey(apiKey)
        if (apiKeyName.isNotBlank()) secureStorage.saveApiKeyName(apiKeyName)
        if (type == ProviderType.CUSTOM) {
            secureStorage.saveCustomProviderName(name)
        } else {
            secureStorage.clearCustomProviderName()
        }
        dao.upsert(ProviderConfigEntity(providerType = type.name))
    }

    suspend fun clearProviderConfig() {
        dao.deleteAll()
        secureStorage.clearApiKey()
        secureStorage.clearApiKeyName()
        secureStorage.clearCustomProviderName()
        secureStorage.saveProviderType(ProviderType.NONE.name)
    }

    fun getApiKeyMasked(): String {
        val key = secureStorage.getApiKey() ?: return "(not set)"
        return if (key.length <= 8) "****" else "****${key.takeLast(4)}"
    }

    fun getApiKeyNameMasked(): String {
        val name = secureStorage.getApiKeyName() ?: return "(not set)"
        return if (name.length <= 8) "****" else "****${name.takeLast(4)}"
    }

    fun isProviderConfigured(): Boolean {
        val type = runCatching {
            ProviderType.valueOf(secureStorage.getProviderType() ?: return false)
        }.getOrElse { return false }
        return type != ProviderType.NONE && type != ProviderType.CUSTOM &&
            !secureStorage.getApiKey().isNullOrBlank() &&
            (type != ProviderType.COINBASE || secureStorage.getApiKeyName() != null)
    }
}
