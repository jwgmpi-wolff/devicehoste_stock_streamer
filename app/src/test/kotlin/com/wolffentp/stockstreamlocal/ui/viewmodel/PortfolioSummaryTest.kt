package com.wolffentp.stockstreamlocal.ui.viewmodel

import com.wolffentp.stockstreamlocal.data.model.PortfolioLotEntity
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioSummaryTest {
    @Test
    fun `total gain sums positive negative and break-even holdings`() {
        val quotes = listOf(
            holding("AAPL", quantity = 2.0, purchasePrice = 100.0, lastPrice = 110.0),
            holding("MSFT", quantity = 4.0, purchasePrice = 50.0, lastPrice = 45.0),
            holding("GOOG", quantity = 1.0, purchasePrice = 150.0, lastPrice = 150.0),
        )

        assertEquals(0.0, PortfolioSummary.totalGain(quotes)!!, 0.001)
    }

    @Test
    fun `total gain ignores incomplete holdings`() {
        val quotes = listOf(
            holding("AAPL", quantity = 2.0, purchasePrice = 100.0, lastPrice = 110.0),
            holding("MSFT", quantity = 4.0, purchasePrice = null, lastPrice = 45.0),
            holding("GOOG", quantity = 1.0, purchasePrice = 150.0, lastPrice = null),
        )

        assertEquals(20.0, PortfolioSummary.totalGain(quotes)!!, 0.001)
    }

    @Test
    fun `total gain is absent when no holding can be calculated`() {
        val quotes = listOf(
            holding("MSFT", quantity = 4.0, purchasePrice = null, lastPrice = 45.0),
            QuoteResult.notConfigured("GOOG"),
        )

        assertNull(PortfolioSummary.totalGain(quotes))
    }

    @Test
    fun `live quote calculates gain from imported Fidelity holdings and retains provenance`() {
        val quote = QuoteResult.notConfigured("ARM").copy(last = 289.08)
        val lots = listOf(
            PortfolioLotEntity(symbol = "ARM", account = "Individual", quantity = 13.0, purchasePrice = 64.02),
            PortfolioLotEntity(symbol = "ARM", account = "Cash Management", quantity = 10.0, purchasePrice = 95.14),
        )

        val enriched = enrichQuoteWithPortfolio(quote, lots)

        assertEquals(23.0, enriched.quantity!!, 0.001)
        assertEquals((13.0 * 64.02 + 10.0 * 95.14) / 23.0, enriched.purchasePrice!!, 0.001)
        assertEquals(23.0 * 289.08 - (13.0 * 64.02 + 10.0 * 95.14), enriched.gainLoss!!, 0.001)
        assertEquals(DataSourceLabel.IMPORTED_BASELINE, enriched.quantityLabel)
        assertEquals(DataSourceLabel.IMPORTED_BASELINE, enriched.purchasePriceLabel)
        assertEquals(DataSourceLabel.CALCULATED, enriched.gainLossLabel)
        assertEquals("Individual, Cash Management", enriched.account)
    }

    private fun holding(
        symbol: String,
        quantity: Double?,
        purchasePrice: Double?,
        lastPrice: Double?,
    ) = QuoteResult.notConfigured(symbol).copy(
        quantity = quantity,
        purchasePrice = purchasePrice,
        last = lastPrice,
    )
}