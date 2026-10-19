package com.wolffentp.stockstreamlocal.ui.viewmodel

import android.content.Context
import com.wolffentp.stockstreamlocal.csv.*
import com.wolffentp.stockstreamlocal.data.repository.PortfolioRepository
import com.wolffentp.stockstreamlocal.data.repository.TickerRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CsvImportViewModelTest {
    private val portfolio = mockk<PortfolioRepository>(relaxed = true)
    private val tickers = mockk<TickerRepository>(relaxed = true)
    private val parser = CsvParser()
    private lateinit var viewModel: CsvImportViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = CsvImportViewModel(
            mockk<Context>(), parser, FidelityCsvMapper(), portfolio, tickers, CoinbaseCsvMapper(),
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Coinbase confirmation routes mapped balances to source scoped reconciliation`() = runBlocking {
        val result = parse("Transaction Type,Asset,Quantity Transacted\nBuy,BTC,2")

        viewModel.confirmImport(result, "coinbase.csv")
        val state = awaitCompletion()

        assertTrue(state is ImportState.Imported)
        assertEquals(1, (state as ImportState.Imported).importedCount)
        coVerify {
            portfolio.importLots(
                match { it.single().symbol == "BTC" && it.single().quantity == 2.0 },
                false,
                COINBASE_CSV_SOURCE,
            )
        }
    }

    @Test
    fun `closed Coinbase balances still clear the prior source without clearing other accounts`() = runBlocking {
        val result = parse("Transaction Type,Asset,Quantity Transacted\nBuy,BTC,2\nSell,BTC,2")

        viewModel.confirmImport(result, "coinbase.csv")
        val state = awaitCompletion()

        assertEquals(0, (state as ImportState.Imported).importedCount)
        coVerify { portfolio.importLots(emptyList(), false, COINBASE_CSV_SOURCE) }
    }

    @Test
    fun `Fidelity all account confirmation retains snapshot replacement`() = runBlocking {
        val result = parse("Positions: All Accounts\nSymbol,Quantity\nAAPL,2")

        viewModel.confirmImport(result, "fidelity.csv")
        awaitCompletion()

        coVerify { portfolio.importLots(match { it.single().symbol == "AAPL" }, true, null) }
    }

    @Test
    fun `invalid import reports an error instead of success`() = runBlocking {
        coEvery { portfolio.importLots(any(), any(), any()) } throws IllegalArgumentException("Invalid holdings")

        viewModel.confirmImport(parse("Symbol,Quantity\nAAPL,2"), "positions.csv")

        assertEquals(ImportState.Error("Invalid holdings"), awaitCompletion())
    }

    private fun parse(csv: String) = parser.parse(csv.byteInputStream()) as CsvImportResult.Success

    private suspend fun awaitCompletion(): ImportState = withTimeout(5_000) {
        viewModel.importState.first { it is ImportState.Imported || it is ImportState.Error }
    }
}
