package com.wolffentp.stockstreamlocal.csv

sealed class CsvImportResult {
    data class Success(
        val rows: List<Map<String, String>>,
        val totalRows: Int,
        val validRows: Int,
        val rowErrors: List<RowError>,
        val detectedHeaders: List<String>,
        val missingOptionalColumns: List<String>,
    ) : CsvImportResult()

    data class HeaderError(
        val missingColumns: List<String>,
        val foundHeaders: List<String>,
    ) : CsvImportResult()

    data class ParseError(val message: String) : CsvImportResult()
    data class EmptyFile(val message: String = "The selected file is empty.") : CsvImportResult()
}

data class RowError(
    val rowIndex: Int,
    val rawLine: String? = null, // NOT logged — may contain PII
    val reason: String,
)
