package com.wolffentp.stockstreamlocal.csv

import com.wolffentp.stockstreamlocal.data.model.PortfolioLotEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps parsed CSV rows to [PortfolioLotEntity] objects.
 *
 * All mapped values are tagged as IMPORTED BASELINE.
 * No live quote data is ever assigned from CSV rows.
 * Dollar signs, percent signs, commas in numbers, and parentheses for negatives
 * are all handled during parsing.
 */
@Singleton
class FidelityCsvMapper @Inject constructor() {

    fun mapRows(rows: List<Map<String, String>>, sourceFileName: String): List<PortfolioLotEntity> {
        val mappedRows = rows.mapNotNull { row ->
            val symbol = row.ci(CsvColumns.SYMBOL)?.trim()?.uppercase() ?: return@mapNotNull null
            if (symbol.isBlank()) return@mapNotNull null

            val quantity = row.ci(CsvColumns.QUANTITY)?.cleanNumber()
            val purchasePrice = row.ci(CsvColumns.PURCHASE_PRICE)?.cleanNumber()
                ?: row.ci(CsvColumns.COST_BASIS)?.cleanNumber()?.let { totalCost ->
                    quantity?.takeIf { it != 0.0 }?.let { totalCost / it }
                }

            val dayRange = row.ci(CsvColumns.DAY_RANGE)
            val (dayLow, dayHigh) = parseRange(dayRange)

            val weekRange = row.ci(CsvColumns.WEEK_RANGE_52)
            val (weekLow, weekHigh) = parseRange(weekRange)

            PortfolioLotEntity(
                symbol = symbol,
                account = row.ci(CsvColumns.ACCOUNT) ?: "",
                importedLast = row.ci(CsvColumns.LAST)?.cleanNumber(),
                importedBid = row.ci(CsvColumns.BID)?.cleanNumber(),
                importedAsk = row.ci(CsvColumns.ASK)?.cleanNumber(),
                importedChg = row.ci(CsvColumns.CHG)?.cleanNumber(),
                importedTdyGainLoss = row.ci(CsvColumns.TDY_GAIN_LOSS)?.cleanNumber(),
                quantity = quantity,
                importedVolume = row.ci(CsvColumns.VOLUME)?.cleanNumber()?.toLong(),
                importedDayRangeLow = dayLow,
                importedDayRangeHigh = dayHigh,
                importedWeekRange52Low = weekLow,
                importedWeekRange52High = weekHigh,
                purchasePrice = purchasePrice,
                importedValue = row.ci(CsvColumns.VALUE)?.cleanNumber(),
                importedPctTdyGainLoss = row.ci(CsvColumns.PCT_TDY_GAIN_LOSS)?.cleanPercent(),
                importedGainLoss = row.ci(CsvColumns.GAIN_LOSS)?.cleanNumber(),
                importedPctGainLoss = row.ci(CsvColumns.PCT_GAIN_LOSS)?.cleanPercent(),
                importedCloseValue = row.ci(CsvColumns.CLOSE_VALUE)?.cleanNumber(),
                earningsDate = row.ci(CsvColumns.EARNINGS_DATE)?.takeIf { it.isNotBlank() },
                divDate = row.ci(CsvColumns.DIV_DATE)?.takeIf { it.isNotBlank() },
                importedPrevClose = row.ci(CsvColumns.PREV_CLOSE)?.cleanNumber(),
                importedAtUtc = System.currentTimeMillis(),
                sourceFileName = sourceFileName,
            )
        }

        return mappedRows
            .groupBy { it.symbol to it.account }
            .values
            .map(::aggregateLots)
    }

    private fun aggregateLots(lots: List<PortfolioLotEntity>): PortfolioLotEntity {
        val latest = lots.last()
        val totalQuantity = lots.mapNotNull { it.quantity }.sum()
        val totalCost = lots.sumOf { (it.quantity ?: 0.0) * (it.purchasePrice ?: 0.0) }
        return latest.copy(
            id = 0,
            quantity = totalQuantity.takeIf { it != 0.0 },
            purchasePrice = if (totalQuantity != 0.0 && totalCost != 0.0) totalCost / totalQuantity else latest.purchasePrice,
            importedTdyGainLoss = lots.mapNotNull { it.importedTdyGainLoss }.takeIf { it.isNotEmpty() }?.sum(),
            importedValue = lots.mapNotNull { it.importedValue }.takeIf { it.isNotEmpty() }?.sum(),
            importedGainLoss = lots.mapNotNull { it.importedGainLoss }.takeIf { it.isNotEmpty() }?.sum(),
            importedCloseValue = lots.mapNotNull { it.importedCloseValue }.takeIf { it.isNotEmpty() }?.sum(),
        )
    }

    /** Case-insensitive key lookup in the row map. */
    private fun Map<String, String>.ci(key: String): String? =
        entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value

    /** Remove $, commas, spaces; handle (x) as negative. */
    private fun String.cleanNumber(): Double? {
        val s = this.trim()
            .replace("$", "")
            .replace(",", "")
            .replace(" ", "")
        return when {
            s.isBlank() || s == "--" || s == "N/A" -> null
            s.startsWith("(") && s.endsWith(")") -> s.removePrefix("(").removeSuffix(")").toDoubleOrNull()?.unaryMinus()
            else -> s.toDoubleOrNull()
        }
    }

    /** Remove %, handle (x%) as negative. */
    private fun String.cleanPercent(): Double? {
        return this.replace("%", "").cleanNumber()
    }

    /** Parse "12.50 - 13.75" range format. */
    private fun parseRange(raw: String?): Pair<Double?, Double?> {
        if (raw.isNullOrBlank() || raw == "--" || raw == "N/A") return null to null
        val parts = raw.split(" - ", " – ", "-").map { it.trim().replace(",", "").replace("$", "") }
        if (parts.size < 2) return null to null
        return parts[0].toDoubleOrNull() to parts[1].toDoubleOrNull()
    }
}
