package com.wolffentp.stockstreamlocal.csv

object CoinbaseCsvColumns {
    const val TIMESTAMP = "Timestamp"
    const val TRANSACTION_TYPE = "Transaction Type"
    const val ASSET = "Asset"
    const val QUANTITY = "Quantity Transacted"
    const val SPOT_PRICE_CURRENCY = "Spot Price Currency"
    const val SPOT_PRICE = "Spot Price at Transaction"
    const val SUBTOTAL = "Subtotal"
    const val TOTAL = "Total (inclusive of fees and/or spread)"
    const val FEES = "Fees and/or Spread"

    val REQUIRED_COLUMNS = listOf(TRANSACTION_TYPE, ASSET, QUANTITY)
    val OPTIONAL_COLUMNS = listOf(
        TIMESTAMP,
        SPOT_PRICE_CURRENCY,
        SPOT_PRICE,
        SUBTOTAL,
        TOTAL,
        FEES,
    )
}
