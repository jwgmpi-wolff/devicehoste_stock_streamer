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

    suspend fun saveProviderConfig(type: ProviderType, apiKey: String) {
        dao.upsert(ProviderConfigEntity(providerType = type.name))
        secureStorage.saveProviderType(type.name)
        if (apiKey.isNotBlank()) secureStorage.saveApiKey(apiKey)
    }

    suspend fun clearProviderConfig() {
        dao.deleteAll()
        secureStorage.clearApiKey()
        secureStorage.saveProviderType(ProviderType.NONE.name)
    }

    fun getApiKeyMasked(): String {
        val key = secureStorage.getApiKey() ?: return "(not set)"
        return if (key.length <= 8) "****" else "****${key.takeLast(4)}"
    }

    fun isProviderConfigured(): Boolean =
        secureStorage.getProviderType() != null && secureStorage.getApiKey() != null
}
