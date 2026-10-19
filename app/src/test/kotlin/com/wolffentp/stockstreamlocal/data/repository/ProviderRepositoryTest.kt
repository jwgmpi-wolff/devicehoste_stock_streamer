package com.wolffentp.stockstreamlocal.data.repository

import com.wolffentp.stockstreamlocal.data.db.dao.ProviderConfigDao
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.provider.ProviderFactory
import com.wolffentp.stockstreamlocal.market.provider.ProviderType
import com.wolffentp.stockstreamlocal.security.SecureStorage
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ProviderRepositoryTest {
    private val dao = mockk<ProviderConfigDao>(relaxed = true)
    private val storage = mockk<SecureStorage>(relaxed = true)
    private val repository = ProviderRepository(dao, storage)

    @Test
    fun `custom name is trimmed and credentials are stored outside Room`() = runTest {
        repository.saveProviderConfig(ProviderType.CUSTOM, "custom-key", customProviderName = " Example ")

        verify { storage.saveCustomProviderName("Example") }
        verify { storage.saveApiKey("custom-key") }
        coVerify { dao.upsert(match { it.providerType == ProviderType.CUSTOM.name }) }
    }

    @Test
    fun `saved custom provider is restored from configuration`() = runTest {
        every { dao.observe() } returns flowOf(
            com.wolffentp.stockstreamlocal.data.model.ProviderConfigEntity(providerType = "CUSTOM"),
        )
        every { storage.getCustomProviderName() } returns "Example"

        assertEquals(ProviderType.CUSTOM, repository.observeProviderType().first())
        assertEquals("Example", repository.getCustomProviderName())
    }

    @Test
    fun `custom provider requires a name before writing anything`() = runTest {
        assertRejected { repository.saveProviderConfig(ProviderType.CUSTOM, "key", customProviderName = " ") }
        verify(exactly = 0) { storage.saveApiKey(any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `switching to custom provider cannot reuse built in credentials`() = runTest {
        every { storage.getProviderType() } returns ProviderType.FINNHUB.name
        every { storage.getApiKey() } returns "finnhub-key"

        assertRejected {
            repository.saveProviderConfig(ProviderType.CUSTOM, "", customProviderName = "Example")
        }
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `same custom provider can retain its existing key`() = runTest {
        existingCustomProvider()

        repository.saveProviderConfig(ProviderType.CUSTOM, "", customProviderName = "Example")

        verify(exactly = 0) { storage.clearApiKey() }
        verify(exactly = 0) { storage.saveApiKey(any()) }
        coVerify { dao.upsert(match { it.providerType == "CUSTOM" }) }
    }

    @Test
    fun `renaming custom provider requires a replacement key`() = runTest {
        existingCustomProvider()

        assertRejected {
            repository.saveProviderConfig(ProviderType.CUSTOM, "", customProviderName = "Other")
        }
        verify(exactly = 0) { storage.saveCustomProviderName(any()) }
    }

    @Test
    fun `custom credentials are not treated as a working quote integration`() = runTest {
        existingCustomProvider()

        assertFalse(repository.isProviderConfigured())
        val quote = ProviderFactory(storage).buildProvider().getQuote("AAPL")
        assertEquals(FreshnessStatus.NOT_CONFIGURED, quote.freshnessStatus)
        assertNull(quote.last)
    }

    @Test
    fun `switching back to built in provider replaces credentials and removes custom name`() = runTest {
        existingCustomProvider()

        repository.saveProviderConfig(ProviderType.FINNHUB, "finnhub-key")

        verify { storage.clearApiKey() }
        verify { storage.clearApiKeyName() }
        verify { storage.clearCustomProviderName() }
        verify { storage.saveApiKey("finnhub-key") }
        verify { storage.saveProviderType(ProviderType.FINNHUB.name) }
    }

    @Test
    fun `same built in provider retains key when input is blank`() = runTest {
        every { storage.getProviderType() } returns ProviderType.FINNHUB.name
        every { storage.getApiKey() } returns "finnhub-key"

        repository.saveProviderConfig(ProviderType.FINNHUB, "")

        verify(exactly = 0) { storage.clearApiKey() }
        assertTrue(repository.isProviderConfigured())
    }

    @Test
    fun `clear removes custom name and all provider credentials`() = runTest {
        repository.clearProviderConfig()

        coVerify { dao.deleteAll() }
        verify { storage.clearApiKey() }
        verify { storage.clearApiKeyName() }
        verify { storage.clearCustomProviderName() }
        verify { storage.saveProviderType(ProviderType.NONE.name) }
    }

    @Test
    fun `selecting none clears provider credentials`() = runTest {
        repository.saveProviderConfig(ProviderType.NONE, "")

        coVerify { dao.deleteAll() }
        verify { storage.clearApiKey() }
        verify { storage.clearCustomProviderName() }
    }

    private fun existingCustomProvider() {
        every { storage.getProviderType() } returns ProviderType.CUSTOM.name
        every { storage.getCustomProviderName() } returns "Example"
        every { storage.getApiKey() } returns "custom-key"
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected invalid provider configuration to be rejected")
        } catch (e: IllegalArgumentException) {
            assertFalse(e.message.isNullOrBlank())
        }
    }
}
