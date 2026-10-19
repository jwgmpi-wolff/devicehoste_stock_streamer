package com.wolffentp.stockstreamlocal.csv

import org.junit.Assert.*
import org.junit.Test

class CoinbaseCsvMapperTest {
    private val parser = CsvParser()
    private val mapper = CoinbaseCsvMapper()

    @Test
    fun `partial sale preserves average cost and computes remaining historical gain`() {
        val lots = map(
            "2026-01-01T00:00:00Z,Buy,BTC,2,USD,100,200",
            "2026-01-02T00:00:00Z,Sell,BTC,1,USD,150,150",
        )

        val lot = lots.single()
        assertEquals("BTC", lot.symbol)
        assertEquals("Coinbase", lot.account)
        assertEquals(1.0, lot.quantity!!, 0.001)
        assertEquals(100.0, lot.purchasePrice!!, 0.001)
        assertEquals(150.0, lot.importedLast!!, 0.001)
        assertEquals(150.0, lot.importedValue!!, 0.001)
        assertEquals(50.0, lot.importedGainLoss!!, 0.001)
        assertEquals(COINBASE_CSV_SOURCE, lot.sourceFileName)
    }

    @Test
    fun `reverse chronological report is sorted before computing balance`() {
        val lot = map(
            "2026-01-02T00:00:00Z,Sell,BTC,1,USD,150,150",
            "2026-01-01T00:00:00Z,Buy,BTC,2,USD,100,200",
        ).single()

        assertEquals(1.0, lot.quantity!!, 0.001)
        assertEquals(150.0, lot.importedLast!!, 0.001)
    }

    @Test
    fun `fully disposed assets produce no holdings`() {
        assertTrue(map(
            "2026-01-01T00:00:00Z,Buy,BTC,2,USD,100,200",
            "2026-01-02T00:00:00Z,Send,BTC,2,USD,150,300",
        ).isEmpty())
    }

    @Test
    fun `missing historical price leaves valuation and gain unavailable`() {
        val lot = map("2026-01-01T00:00:00Z,Receive,BTC,2,USD,,").single()

        assertNull(lot.importedLast)
        assertNull(lot.importedValue)
        assertNull(lot.importedGainLoss)
        assertNull(lot.purchasePrice)
    }

    private fun map(vararg rows: String) = mapper.mapRows(
        (parser.parse(
            ("Timestamp,Transaction Type,Asset,Quantity Transacted,Spot Price Currency,Spot Price at Transaction,Total (inclusive of fees and/or spread)\n" +
                rows.joinToString("\n")).byteInputStream(),
        ) as CsvImportResult.Success).rows,
    )
}
