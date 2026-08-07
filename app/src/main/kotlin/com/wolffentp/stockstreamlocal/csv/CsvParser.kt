package com.wolffentp.stockstreamlocal.csv

import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses a Fidelity-style CSV with:
 * - Quoted fields (handles commas inside quotes)
 * - Fidelity preamble rows (skips rows before the header)
 * - BOM marker removal
 * - Trims whitespace and currency symbols from values
 *
 * DOES NOT log raw row content (may contain account names or financial data).
 */
@Singleton
class CsvParser @Inject constructor() {

    /**
     * Parse the CSV from [inputStream]. Detects the header row by looking for [CsvColumns.SYMBOL].
     */
    fun parse(inputStream: InputStream): CsvImportResult {
        val lines = try {
            inputStream.bufferedReader(Charsets.UTF_8).readLines()
        } catch (e: Exception) {
            return CsvImportResult.ParseError("Failed to read file: ${e.message}")
        }

        if (lines.isEmpty()) return CsvImportResult.EmptyFile()

        // Find header row (first row that contains the SYMBOL column)
        val headerLineIndex = lines.indexOfFirst { line ->
            parseRow(line).any { normalizeHeader(it).equals(CsvColumns.SYMBOL, ignoreCase = true) }
        }

        if (headerLineIndex < 0) {
            return CsvImportResult.HeaderError(
                missingColumns = listOf(CsvColumns.SYMBOL),
                foundHeaders = emptyList(),
            )
        }

        val isAllAccountsSnapshot = lines.take(headerLineIndex).any { line ->
            line.trim().equals("Positions: All Accounts", ignoreCase = true)
        }

        val rawHeaders = parseRow(lines[headerLineIndex]).map { it.trim() }
        val headers = rawHeaders.map { normalizeHeader(it) }

        // Check required columns
        val missingRequired = CsvColumns.REQUIRED_COLUMNS.filter { req ->
            headers.none { h -> h.equals(req, ignoreCase = true) }
        }
        if (missingRequired.isNotEmpty()) {
            return CsvImportResult.HeaderError(
                missingColumns = missingRequired,
                foundHeaders = rawHeaders,
            )
        }

        val missingOptional = CsvColumns.ALL_COLUMNS.filter { col ->
            col !in CsvColumns.REQUIRED_COLUMNS &&
                    headers.none { h -> h.equals(col, ignoreCase = true) }
        }

        val dataLines = lines.drop(headerLineIndex + 1)
        val rows = mutableListOf<Map<String, String>>()
        val rowErrors = mutableListOf<RowError>()

        dataLines.forEachIndexed { idx, line ->
            if (line.isBlank()) return@forEachIndexed
            val cells = parseRow(line)
            if (cells.none { it.isNotBlank() }) return@forEachIndexed

            val rawSymbol = cells.getOrNull(
                headers.indexOfFirst { it.equals(CsvColumns.SYMBOL, ignoreCase = true) }
            )?.trim()

            if (rawSymbol.isNullOrBlank()) {
                rowErrors.add(RowError(rowIndex = idx + headerLineIndex + 2, reason = "Missing symbol"))
                return@forEachIndexed
            }

            // Skip Fidelity subtotal/total rows
            if (rawSymbol.startsWith("--") || rawSymbol.equals("Subtotal", ignoreCase = true) ||
                rawSymbol.equals("Account Total", ignoreCase = true) ||
                rawSymbol.equals("TOTALS", ignoreCase = true)) {
                return@forEachIndexed
            }

            val row = mutableMapOf<String, String>()
            headers.forEachIndexed { colIdx, header ->
                row[header] = cells.getOrElse(colIdx) { "" }.trim()
            }
            row[CsvColumns.SYMBOL] = normalizeFidelitySymbol(rawSymbol)
            rows.add(row)
        }

        return CsvImportResult.Success(
            rows = rows,
            totalRows = dataLines.count { it.isNotBlank() },
            validRows = rows.size,
            rowErrors = rowErrors,
            detectedHeaders = rawHeaders,
            missingOptionalColumns = missingOptional,
            isAllAccountsSnapshot = isAllAccountsSnapshot,
        )
    }

    /**
     * RFC 4180-compliant CSV row parser with support for quoted fields containing commas and newlines.
     */
    fun parseRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        val cleaned = line.trimStart('\uFEFF') // Remove BOM
        while (i < cleaned.length) {
            val c = cleaned[i]
            when {
                c == '"' && !inQuotes -> {
                    inQuotes = true
                }
                c == '"' && inQuotes -> {
                    if (i + 1 < cleaned.length && cleaned[i + 1] == '"') {
                        sb.append('"')
                        i++ // skip escaped quote
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun normalizeHeader(raw: String): String {
        val header = raw.trimStart('\uFEFF').trim()
        return HEADER_ALIASES.entries.firstOrNull { (_, aliases) ->
            aliases.any { it.equals(header, ignoreCase = true) }
        }?.key ?: header
    }

    private fun normalizeFidelitySymbol(raw: String): String =
        raw.replace(Regex("\\s*\\(Core account\\)\\s*$", RegexOption.IGNORE_CASE), "").trim()

    private companion object {
        val HEADER_ALIASES = mapOf(
            CsvColumns.SYMBOL to setOf("Symbol", "Ticker", "Ticker Symbol", "Security Symbol"),
            CsvColumns.LAST to setOf("Last", "Price", "Last Price", "Current Price", "Market Price"),
            CsvColumns.QUANTITY to setOf("Quantity", "Qty", "Shares", "Units"),
            CsvColumns.PURCHASE_PRICE to setOf(
                "Purchase Price", "Average Cost", "Avg Cost", "Average Price", "Cost Per Share", "Unit Cost",
            ),
            CsvColumns.COST_BASIS to setOf("Cost Basis", "Total Cost", "Total Cost Basis", "Book Cost"),
            CsvColumns.VALUE to setOf("Value", "Market Value", "Current Value", "Position Value"),
            CsvColumns.GAIN_LOSS to setOf("G/L", "Gain/Loss", "Unrealized Gain/Loss", "Unrealized G/L", "Total Gain/Loss"),
            CsvColumns.PCT_GAIN_LOSS to setOf("% G/L", "Gain/Loss %", "Unrealized Gain/Loss %", "Total Gain/Loss %"),
            CsvColumns.TDY_GAIN_LOSS to setOf("Tdy G/L", "Day Gain/Loss", "Today's Gain/Loss", "Daily Gain/Loss"),
            CsvColumns.PCT_TDY_GAIN_LOSS to setOf("% Tdy G/L", "Day Gain/Loss %", "Today's Gain/Loss %", "Daily Gain/Loss %"),
            CsvColumns.ACCOUNT to setOf("Account", "Account Name", "Account Number", "Portfolio"),
            CsvColumns.PREV_CLOSE to setOf("Prev Close", "Previous Close"),
        )
    }
}
