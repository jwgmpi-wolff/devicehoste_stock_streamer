package com.wolffentp.stockstreamlocal.ui.viewmodel

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