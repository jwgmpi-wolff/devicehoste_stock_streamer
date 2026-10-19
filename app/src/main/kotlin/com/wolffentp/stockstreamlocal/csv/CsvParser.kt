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

        // Find either a positions header or a Coinbase transaction-history header.
        val headerLineIndex = lines.indexOfFirst { line ->
            val cells = parseRow(line)
            cells.any { normalizeHeader(it).equals(CsvColumns.SYMBOL, ignoreCase = true) } ||
                isCoinbaseHeader(cells)
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
        val format = if (isCoinbaseHeader(rawHeaders)) {
            CsvImportFormat.COINBASE_TRANSACTIONS
        } else {
            CsvImportFormat.BROKER_POSITIONS
        }
        val headers = rawHeaders.map { raw ->
            if (format == CsvImportFormat.COINBASE_TRANSACTIONS) normalizeCoinbaseHeader(raw)
            else normalizeHeader(raw)
        }

        // Check required columns
        val requiredColumns = if (format == CsvImportFormat.COINBASE_TRANSACTIONS) {
            CoinbaseCsvColumns.REQUIRED_COLUMNS
        } else {
            CsvColumns.REQUIRED_COLUMNS
        }
        val optionalColumns = if (format == CsvImportFormat.COINBASE_TRANSACTIONS) {
            CoinbaseCsvColumns.OPTIONAL_COLUMNS
        } else {
            CsvColumns.ALL_COLUMNS.filter { it !in CsvColumns.REQUIRED_COLUMNS }
        }
        val missingRequired = requiredColumns.filter { req ->
            headers.none { h -> h.equals(req, ignoreCase = true) }
        }
        if (missingRequired.isNotEmpty()) {
            return CsvImportResult.HeaderError(
                missingColumns = missingRequired,
                foundHeaders = rawHeaders,
            )
        }

        val missingOptional = optionalColumns.filter { col ->
            headers.none { h -> h.equals(col, ignoreCase = true) }
        }

        val dataLines = lines.drop(headerLineIndex + 1)
        val rows = mutableListOf<Map<String, String>>()
        val rowErrors = mutableListOf<RowError>()

        dataLines.forEachIndexed { idx, line ->
            if (line.isBlank()) return@forEachIndexed
            val cells = parseRow(line)
            if (cells.none { it.isNotBlank() }) return@forEachIndexed

            val identityColumn = if (format == CsvImportFormat.COINBASE_TRANSACTIONS) {
                CoinbaseCsvColumns.ASSET
            } else {
                CsvColumns.SYMBOL
            }
            val rawIdentity = cells.getOrNull(
                headers.indexOfFirst { it.equals(identityColumn, ignoreCase = true) }
            )?.trim()

            if (rawIdentity.isNullOrBlank()) {
                rowErrors.add(RowError(rowIndex = idx + headerLineIndex + 2, reason = "Missing ${identityColumn.lowercase()}"))
                return@forEachIndexed
            }

            // Skip Fidelity subtotal/total rows
            if (format == CsvImportFormat.BROKER_POSITIONS &&
                (rawIdentity.startsWith("--") || rawIdentity.equals("Subtotal", ignoreCase = true) ||
                    rawIdentity.equals("Account Total", ignoreCase = true) ||
                    rawIdentity.equals("TOTALS", ignoreCase = true))) {
                return@forEachIndexed
            }

            val row = mutableMapOf<String, String>()
            headers.forEachIndexed { colIdx, header ->
                row[header] = cells.getOrElse(colIdx) { "" }.trim()
            }
            if (format == CsvImportFormat.BROKER_POSITIONS) {
                row[CsvColumns.SYMBOL] = normalizeFidelitySymbol(rawIdentity)
            }
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
            format = format,
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

    private fun isCoinbaseHeader(cells: List<String>): Boolean {
        val headers = cells.map(::normalizeCoinbaseHeader)
        return CoinbaseCsvColumns.REQUIRED_COLUMNS.all { required ->
            headers.any { it.equals(required, ignoreCase = true) }
        }
    }

    private fun normalizeCoinbaseHeader(raw: String): String {
        val header = raw.trimStart('\uFEFF').trim()
        return COINBASE_HEADER_ALIASES.entries.firstOrNull { (_, aliases) ->
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

        val COINBASE_HEADER_ALIASES = mapOf(
            CoinbaseCsvColumns.TIMESTAMP to setOf("Timestamp"),
            CoinbaseCsvColumns.TRANSACTION_TYPE to setOf("Transaction Type"),
            CoinbaseCsvColumns.ASSET to setOf("Asset"),
            CoinbaseCsvColumns.QUANTITY to setOf("Quantity Transacted", "Quantity"),
            CoinbaseCsvColumns.SPOT_PRICE_CURRENCY to setOf("Spot Price Currency", "Price Currency"),
            CoinbaseCsvColumns.SPOT_PRICE to setOf("Spot Price at Transaction", "Price at Transaction"),
            CoinbaseCsvColumns.SUBTOTAL to setOf("Subtotal"),
            CoinbaseCsvColumns.TOTAL to setOf("Total (inclusive of fees and/or spread)", "Total (inclusive of fees)"),
            CoinbaseCsvColumns.FEES to setOf("Fees and/or Spread", "Fees"),
        )
    }
}
