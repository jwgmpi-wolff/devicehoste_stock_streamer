package com.wolffentp.stockstreamlocal.csv

import org.junit.Assert.*
import org.junit.Test

class CsvParserTest {

    private val parser = CsvParser()

    // ── Header detection ─────────────────────────────────────────────────────

    @Test
    fun `detects header row with Symbol column`() {
        val csv = "Symbol,Last,Bid\nAAPL,150.00,149.90"
        val result = parser.parse(csv.byteInputStream())
        assertTrue(result is CsvImportResult.Success)
        assertEquals(1, (result as CsvImportResult.Success).validRows)
    }

    @Test
    fun `skips Fidelity preamble rows before header`() {
        val csv = "Account Summary Report\nGenerated: 2024-01-01\nSymbol,Last,Bid\nNVDA,450.00,449.50"
        val result = parser.parse(csv.byteInputStream()) as CsvImportResult.Success
        assertEquals(1, result.validRows)
        assertEquals("NVDA", result.rows.first()["Symbol"])
    }

    @Test
    fun `returns HeaderError when Symbol column is absent`() {
        val csv = "Ticker,Price\nAAPL,150.00"
        val result = parser.parse(csv.byteInputStream())
        assertTrue("Expected HeaderError", result is CsvImportResult.HeaderError)
        assertTrue((result as CsvImportResult.HeaderError).missingColumns.contains("Symbol"))
    }

    @Test
    fun `returns EmptyFile for empty input`() {
        val result = parser.parse("".byteInputStream())
        assertTrue(result is CsvImportResult.EmptyFile)
    }

    // ── Quoted fields and commas ─────────────────────────────────────────────

    @Test
    fun `handles quoted fields containing commas`() {
        val row = parser.parseRow("""Symbol,"1,234.56",Bid""")
        assertEquals("Symbol", row[0])
        assertEquals("1,234.56", row[1])
        assertEquals("Bid", row[2])
    }

    @Test
    fun `handles escaped double-quotes inside quoted field`() {
        val row = parser.parseRow("Symbol,\"He said \"\"hello\"\"\",Last")
        assertEquals("He said \"hello\"", row[1])
    }

    @Test
    fun `handles empty quoted fields`() {
        val row = parser.parseRow("AAPL,\"\",\"\"")
        assertEquals("", row[1])
    }

    @Test
    fun `strips BOM character from header`() {
        val csv = "\uFEFFSymbol,Last\nAAPL,150"
        val result = parser.parse(csv.byteInputStream())
        assertTrue("Expected Success but got $result", result is CsvImportResult.Success)
    }

    // ── Missing column validation ─────────────────────────────────────────────

    @Test
    fun `reports missing optional columns without failing`() {
        val csv = "Symbol,Last\nAAPL,150.00"
        val result = parser.parse(csv.byteInputStream()) as CsvImportResult.Success
        assertTrue(result.missingOptionalColumns.contains("Bid"))
        assertTrue(result.missingOptionalColumns.contains("Ask"))
    }

    @Test
    fun `parses all 20 Fidelity columns when present`() {
        val header = CsvColumns.ALL_COLUMNS.joinToString(",")
        val row = CsvColumns.ALL_COLUMNS.joinToString(",") { if (it == "Symbol") "AAPL" else "0" }
        val result = parser.parse("$header\n$row".byteInputStream()) as CsvImportResult.Success
        assertEquals(1, result.validRows)
        assertEquals(0, result.missingOptionalColumns.size)
    }

    // ── Row validation ────────────────────────────────────────────────────────

    @Test
    fun `skips Fidelity subtotal rows`() {
        val csv = "Symbol,Last\nAAPL,150\n--Subtotal,0\nAccount Total,0"
        val result = parser.parse(csv.byteInputStream()) as CsvImportResult.Success
        assertEquals(1, result.validRows)
        assertEquals("AAPL", result.rows.first()["Symbol"])
    }

    @Test
    fun `records row error for missing symbol`() {
        val csv = "Symbol,Last\n,150.00\nNVDA,450"
        val result = parser.parse(csv.byteInputStream()) as CsvImportResult.Success
        assertEquals(1, result.validRows)
        assertEquals(1, result.rowErrors.size)
    }

    // ── Duplicate symbols across accounts ────────────────────────────────────

    @Test
    fun `allows duplicate symbols across different accounts`() {
        val csv = "Symbol,Last,Account\nGME,20.00,Account1\nGME,20.00,Account2"
        val result = parser.parse(csv.byteInputStream()) as CsvImportResult.Success
        assertEquals(2, result.validRows)
    }
}
