package com.wolffentp.stockstreamlocal.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class FidelityCsvMapperTest {

    private val mapper = FidelityCsvMapper()

    @Test
    fun `aggregates tax lots using weighted unit cost per symbol and account`() {
        val rows = listOf(
            mapOf("Symbol" to "AAPL", "Account" to "Brokerage", "Quantity" to "10", "Purchase Price" to "100", "Value" to "1500"),
            mapOf("Symbol" to "AAPL", "Account" to "Brokerage", "Quantity" to "5", "Purchase Price" to "130", "Value" to "750"),
        )

        val lot = mapper.mapRows(rows, "positions.csv").single()

        assertEquals(15.0, lot.quantity!!, 0.001)
        assertEquals(110.0, lot.purchasePrice!!, 0.001)
        assertEquals(2250.0, lot.importedValue!!, 0.001)
    }

    @Test
    fun `keeps the same symbol in separate accounts separate`() {
        val rows = listOf(
            mapOf("Symbol" to "AAPL", "Account" to "Brokerage", "Quantity" to "10", "Purchase Price" to "100"),
            mapOf("Symbol" to "AAPL", "Account" to "IRA", "Quantity" to "5", "Purchase Price" to "130"),
        )

        val lots = mapper.mapRows(rows, "positions.csv")

        assertEquals(2, lots.size)
    }

    @Test
    fun `derives unit cost from total cost basis when average cost is absent`() {
        val rows = listOf(
            mapOf("Symbol" to "AAPL", "Quantity" to "10", "Cost Basis" to "1250"),
        )

        val lot = mapper.mapRows(rows, "positions.csv").single()

        assertEquals(125.0, lot.purchasePrice!!, 0.001)
    }
}