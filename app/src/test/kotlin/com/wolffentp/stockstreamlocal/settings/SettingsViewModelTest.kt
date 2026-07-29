package com.wolffentp.stockstreamlocal.settings

import com.wolffentp.stockstreamlocal.auth.AuthManager
import com.wolffentp.stockstreamlocal.auth.AuthState
import com.wolffentp.stockstreamlocal.data.datastore.AppPreferences
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var authManager: AuthManager
    private lateinit var viewModel: SettingsViewModel

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.NoPinConfigured)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        authManager = mockk(relaxed = true)

        every { settingsRepository.preferencesFlow } returns flowOf(AppPreferences())
        every { authManager.authState } returns authStateFlow

        viewModel = SettingsViewModel(settingsRepository, authManager)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── Log out (lock) ────────────────────────────────────────────────────────

    @Test
    fun `lockApp delegates to settingsRepository`() {
        viewModel.lockApp()
        verify(exactly = 1) { settingsRepository.lockApp() }
    }

    @Test
    fun `lockApp does not call any data-clearing operation`() {
        viewModel.lockApp()
        coVerify(exactly = 0) { settingsRepository.clearAllContext() }
        coVerify(exactly = 0) { settingsRepository.clearMarketData() }
    }

    // ── Enable / disable PIN ──────────────────────────────────────────────────

    @Test
    fun `enablePin calls repository and emits Success`() = runTest {
        val pin = "1234".toCharArray()
        coEvery { settingsRepository.enablePin(pin) } just runs

        viewModel.enablePin(pin)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success but got $state", state is SettingsUiState.Success)
    }

    @Test
    fun `enablePin emits Error on repository failure`() = runTest {
        val pin = "1234".toCharArray()
        coEvery { settingsRepository.enablePin(pin) } throws RuntimeException("Keystore error")

        viewModel.enablePin(pin)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is SettingsUiState.Error)
        assertTrue((state as SettingsUiState.Error).message.contains("Keystore error"))
    }

    @Test
    fun `disablePin calls repository and emits Success`() = runTest {
        coEvery { settingsRepository.disablePin() } just runs

        viewModel.disablePin()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
    }

    // ── Set refresh interval ──────────────────────────────────────────────────

    @Test
    fun `setRefreshInterval with valid value calls repository`() = runTest {
        coEvery { settingsRepository.setRefreshInterval(30) } just runs

        viewModel.setRefreshInterval(30)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setRefreshInterval(30) }
        assertTrue(viewModel.uiState.value is SettingsUiState.Success)
    }

    @Test
    fun `setRefreshInterval below minimum is clamped to 15`() = runTest {
        coEvery { settingsRepository.setRefreshInterval(any()) } just runs

        viewModel.setRefreshInterval(5) // below MIN_REFRESH_INTERVAL_SECONDS (15)
        advanceUntilIdle()

        // ViewModel clamps then passes clamped value to repository
        coVerify { settingsRepository.setRefreshInterval(MIN_REFRESH_INTERVAL_SECONDS) }
        val state = viewModel.uiState.value as? SettingsUiState.Success
        assertNotNull("Expected Success state", state)
        assertTrue(
            "Success message should mention clamping",
            state!!.message.contains("clamped", ignoreCase = true),
        )
    }

    @Test
    fun `setRefreshInterval above maximum is clamped to 3600`() = runTest {
        coEvery { settingsRepository.setRefreshInterval(any()) } just runs

        viewModel.setRefreshInterval(9999)
        advanceUntilIdle()

        coVerify { settingsRepository.setRefreshInterval(MAX_REFRESH_INTERVAL_SECONDS) }
    }

    @Test
    fun `setRefreshInterval emits Error on repository failure`() = runTest {
        coEvery { settingsRepository.setRefreshInterval(any()) } throws RuntimeException("DataStore error")

        viewModel.setRefreshInterval(60)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SettingsUiState.Error)
    }

    // ── Clear context ─────────────────────────────────────────────────────────

    @Test
    fun `clearAllContext with confirmed=false does nothing`() = runTest {
        viewModel.clearAllContext(confirmed = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { settingsRepository.clearAllContext() }
        assertTrue(viewModel.uiState.value is SettingsUiState.Idle)
    }

    @Test
    fun `clearAllContext with confirmed=true calls repository and emits ContextCleared`() = runTest {
        coEvery { settingsRepository.clearAllContext() } just runs

        viewModel.clearAllContext(confirmed = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.clearAllContext() }
        assertTrue("Expected ContextCleared", viewModel.uiState.value is SettingsUiState.ContextCleared)
    }

    @Test
    fun `clearAllContext emits Error on repository failure`() = runTest {
        coEvery { settingsRepository.clearAllContext() } throws RuntimeException("DB error")

        viewModel.clearAllContext(confirmed = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SettingsUiState.Error)
    }

    @Test
    fun `clearMarketData with confirmed=true calls repository`() = runTest {
        coEvery { settingsRepository.clearMarketData() } just runs

        viewModel.clearMarketData(confirmed = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.clearMarketData() }
        assertTrue(viewModel.uiState.value is SettingsUiState.Success)
    }

    @Test
    fun `clearMarketData with confirmed=false does nothing`() = runTest {
        viewModel.clearMarketData(confirmed = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { settingsRepository.clearMarketData() }
    }

    // ── dismissUiState ────────────────────────────────────────────────────────

    @Test
    fun `dismissUiState resets to Idle`() = runTest {
        coEvery { settingsRepository.clearMarketData() } just runs
        viewModel.clearMarketData(confirmed = true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value is SettingsUiState.Idle)
        viewModel.dismissUiState()
        assertTrue(viewModel.uiState.value is SettingsUiState.Idle)
    }
}
