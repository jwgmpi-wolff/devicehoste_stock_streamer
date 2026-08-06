package com.wolffentp.stockstreamlocal.ui.viewmodel

import com.wolffentp.stockstreamlocal.market.model.QuoteResult

object PortfolioSummary {
    fun totalGain(quotes: Collection<QuoteResult>): Double? {
        val gains = quotes.mapNotNull { quote ->
            val quantity = quote.quantity ?: return@mapNotNull null
            val purchasePrice = quote.purchasePrice ?: return@mapNotNull null
            val lastPrice = quote.last ?: return@mapNotNull null
            (lastPrice - purchasePrice) * quantity
        }

        return gains.takeIf { it.isNotEmpty() }?.sum()
    }
}