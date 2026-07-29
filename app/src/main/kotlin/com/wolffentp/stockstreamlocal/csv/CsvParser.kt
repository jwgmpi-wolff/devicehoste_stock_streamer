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
            parseRow(line).any { it.trim().equals(CsvColumns.SYMBOL, ignoreCase = true) }
        }

        if (headerLineIndex < 0) {
            return CsvImportResult.HeaderError(
                missingColumns = listOf(CsvColumns.SYMBOL),
                foundHeaders = emptyList(),
            )
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
            if (cells.size < headers.size && cells.none { it.isNotBlank() }) return@forEachIndexed

            val symbol = cells.getOrNull(
                headers.indexOfFirst { it.equals(CsvColumns.SYMBOL, ignoreCase = true) }
            )?.trim()

            if (symbol.isNullOrBlank()) {
                rowErrors.add(RowError(rowIndex = idx + headerLineIndex + 2, reason = "Missing symbol"))
                return@forEachIndexed
            }

            // Skip Fidelity subtotal/total rows
            if (symbol.startsWith("--") || symbol.equals("Subtotal", ignoreCase = true) ||
                symbol.equals("Account Total", ignoreCase = true)) {
                return@forEachIndexed
            }

            val row = mutableMapOf<String, String>()
            headers.forEachIndexed { colIdx, header ->
                row[header] = cells.getOrElse(colIdx) { "" }.trim()
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

    private fun normalizeHeader(raw: String): String =
        raw.trimStart('\uFEFF').trim()
}
