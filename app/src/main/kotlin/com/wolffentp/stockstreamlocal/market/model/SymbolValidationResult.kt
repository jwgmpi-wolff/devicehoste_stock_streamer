package com.wolffentp.stockstreamlocal.market.model

sealed class SymbolValidationResult {
    data class Valid(val symbol: String, val name: String?) : SymbolValidationResult()
    data class Invalid(val symbol: String, val reason: String) : SymbolValidationResult()
    data class Unsupported(val symbol: String) : SymbolValidationResult()
    data class ProviderError(val symbol: String, val message: String) : SymbolValidationResult()
    object ProviderNotConfigured : SymbolValidationResult()
    object Offline : SymbolValidationResult()
}

sealed class MarketStatusResult {
    data class Known(val status: MarketStatus) : MarketStatusResult()
    data class Error(val message: String) : MarketStatusResult()
    object NotSupported : MarketStatusResult()
}
