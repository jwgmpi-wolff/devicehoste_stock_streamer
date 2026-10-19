package com.wolffentp.stockstreamlocal.csv

import com.wolffentp.stockstreamlocal.data.model.PortfolioLotEntity
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

const val COINBASE_CSV_SOURCE = "Coinbase Transaction History"

@Singleton
class CoinbaseCsvMapper @Inject constructor() {

    fun mapRows(rows: List<Map<String, String>>): List<PortfolioLotEntity> {
        val balances = linkedMapOf<String, EstimatedBalance>()
        val orderedRows = rows.mapIndexed { index, row ->
            IndexedTransaction(index, parseTimestamp(row.value(CoinbaseCsvColumns.TIMESTAMP)), row)
        }.sortedWith(compareBy<IndexedTransaction> { it.timestamp ?: Instant.MAX }.thenBy { it.index })

        orderedRows.forEach { transaction ->
            val row = transaction.row
            val asset = row.value(CoinbaseCsvColumns.ASSET)?.trim()?.uppercase().orEmpty()
            if (asset.isBlank() || asset == "USD") return@forEach

            val signedQuantity = row.value(CoinbaseCsvColumns.QUANTITY).toNumber() ?: return@forEach
            if (signedQuantity == 0.0) return@forEach

            val quantity = abs(signedQuantity)
            val type = row.value(CoinbaseCsvColumns.TRANSACTION_TYPE)?.trim()?.lowercase().orEmpty()
            val spotPrice = row.value(CoinbaseCsvColumns.SPOT_PRICE).toNumber()?.takeIf { it > 0.0 }
            val currency = row.value(CoinbaseCsvColumns.SPOT_PRICE_CURRENCY)?.trim()?.uppercase()
            val state = balances.getOrPut(asset) { EstimatedBalance() }
            if (spotPrice != null && (currency == null || currency == "USD" || currency == "USDC")) {
                state.latestUsdPrice = spotPrice
            }

            if (isDisposal(type, signedQuantity)) {
                val disposed = quantity.coerceAtMost(state.quantity)
                val averageCost = if (state.quantity > 0.0) state.costBasis / state.quantity else 0.0
                state.quantity -= disposed
                state.costBasis = (state.costBasis - averageCost * disposed).coerceAtLeast(0.0)
                if (state.quantity < BALANCE_EPSILON) {
                    state.quantity = 0.0
                    state.costBasis = 0.0
                }
            } else {
                val total = row.value(CoinbaseCsvColumns.TOTAL).toNumber()?.let(::abs)?.takeIf { it > 0.0 }
                val subtotal = row.value(CoinbaseCsvColumns.SUBTOTAL).toNumber()?.let(::abs)?.takeIf { it > 0.0 }
                val fees = row.value(CoinbaseCsvColumns.FEES).toNumber()?.let(::abs) ?: 0.0
                val estimatedCost = when {
                    isPurchase(type) && total != null -> total
                    subtotal != null -> subtotal + fees
                    spotPrice != null -> quantity * spotPrice + fees
                    else -> 0.0
                }
                state.quantity += quantity
                state.costBasis += estimatedCost
            }
        }

        val importedAt = System.currentTimeMillis()
        return balances.mapNotNull { (asset, balance) ->
            val quantity = balance.quantity.takeIf { it > BALANCE_EPSILON } ?: return@mapNotNull null
            val averageCost = (balance.costBasis / quantity).takeIf { balance.costBasis > 0.0 }
            val latestUsdPrice = balance.latestUsdPrice
            PortfolioLotEntity(
                symbol = asset,
                account = COINBASE_ACCOUNT,
                importedLast = latestUsdPrice,
                quantity = quantity,
                purchasePrice = averageCost,
                importedValue = latestUsdPrice?.times(quantity),
                importedGainLoss = if (averageCost != null && latestUsdPrice != null) {
                    (latestUsdPrice - averageCost) * quantity
                } else {
                    null
                },
                importedAtUtc = importedAt,
                sourceFileName = COINBASE_CSV_SOURCE,
            )
        }.sortedBy { it.symbol }
    }

    private fun isPurchase(type: String): Boolean =
        type.contains("buy") || type.contains("purchase")

    private fun isDisposal(type: String, signedQuantity: Double): Boolean =
        signedQuantity < 0.0 || DISPOSAL_TYPES.any(type::contains)

    private fun parseTimestamp(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw.trim()) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(raw.trim()).toInstant() }.getOrNull()
    }

    private fun Map<String, String>.value(key: String): String? =
        entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value

    private fun String?.toNumber(): Double? {
        val value = this?.trim().orEmpty()
            .replace("$", "")
            .replace(",", "")
            .replace(" ", "")
        return when {
            value.isBlank() || value == "--" -> null
            value.startsWith("(") && value.endsWith(")") ->
                value.removePrefix("(").removeSuffix(")").toDoubleOrNull()?.unaryMinus()
            else -> value.toDoubleOrNull()
        }
    }

    private data class IndexedTransaction(
        val index: Int,
        val timestamp: Instant?,
        val row: Map<String, String>,
    )

    private data class EstimatedBalance(
        var quantity: Double = 0.0,
        var costBasis: Double = 0.0,
        var latestUsdPrice: Double? = null,
    )

    private companion object {
        const val COINBASE_ACCOUNT = "Coinbase"
        const val BALANCE_EPSILON = 0.000000000001
        val DISPOSAL_TYPES = listOf("sell", "send", "withdraw")
    }
}
