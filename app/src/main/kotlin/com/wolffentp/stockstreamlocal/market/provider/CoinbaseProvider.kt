package com.wolffentp.stockstreamlocal.market.provider

import android.util.Log
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatusResult
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.model.SecurityDetails
import com.wolffentp.stockstreamlocal.market.model.SymbolValidationResult
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val TAG = "CoinbaseProvider"
private const val EXCHANGE_BASE_URL = "https://api.exchange.coinbase.com"
private const val RETAIL_BASE_URL = "https://api.coinbase.com"

/** Public Coinbase market data for USD crypto pairs. Account credentials are never transmitted. */
class CoinbaseProvider(
    private val okHttpClient: OkHttpClient = defaultClient(),
    private val exchangeBaseUrl: String = EXCHANGE_BASE_URL,
    private val retailBaseUrl: String = RETAIL_BASE_URL,
) : MarketDataProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override val capabilities = ProviderCapabilities(
        providerName = PROVIDER_NAME,
        supportsRealtime = false,
        supportsDelayed = false,
        supportsBidAsk = true,
        supportsVolume = true,
        supportsDayRange = true,
        supports52WeekRange = false,
        supportsEarningsDate = false,
        supportsDividendDate = false,
        supportsMarketStatus = true,
        requiresApiKey = false,
        minimumPollIntervalSeconds = 1,
        rateLimitDescription = "Live REST quotes; public Coinbase market-data rate limits apply.",
    )

    override suspend fun validateSymbol(symbol: String): SymbolValidationResult {
        val normalized = normalizeSymbol(symbol)
            ?: return SymbolValidationResult.Invalid(symbol, "Coinbase symbols must use a USD crypto pair")
        val quote = getQuote(normalized)
        return when (quote.freshnessStatus) {
            FreshnessStatus.LIVE, FreshnessStatus.DELAYED ->
                SymbolValidationResult.Valid(normalized, normalized.substringBefore('-'))
            FreshnessStatus.UNSUPPORTED -> SymbolValidationResult.Unsupported(symbol)
            else -> SymbolValidationResult.ProviderError(symbol, quote.errorMessage ?: "Coinbase lookup failed")
        }
    }

    override suspend fun getQuote(symbol: String): QuoteResult {
        val normalized = normalizeSymbol(symbol) ?: return QuoteResult.unsupported(symbol, PROVIDER_NAME)
        return try {
            val tickerResponse = executeGet("$exchangeBaseUrl/products/$normalized/ticker")
            when {
                tickerResponse.code == 429 -> QuoteResult.throttled(symbol, PROVIDER_NAME)
                tickerResponse.code == 404 -> getSpotQuote(symbol, normalized)
                tickerResponse.code !in 200..299 -> QuoteResult.error(
                    symbol,
                    PROVIDER_NAME,
                    "Coinbase returned HTTP ${tickerResponse.code}",
                )
                else -> parseExchangeQuote(symbol, normalized, tickerResponse.body)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Quote fetch network error")
            QuoteResult.error(symbol, PROVIDER_NAME, "Network error: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Quote parse error")
            QuoteResult.error(symbol, PROVIDER_NAME, "Parse error: ${e.message}")
        }
    }

    override suspend fun getQuotes(symbols: List<String>): List<QuoteResult> = symbols.mapIndexed { index, symbol ->
        if (index > 0) delay(1_000)
        getQuote(symbol)
    }

    override suspend fun getSecurityDetails(symbol: String): SecurityDetails = SecurityDetails(
        symbol = normalizeSymbol(symbol) ?: symbol,
        assetType = "Cryptocurrency",
        exchange = "Coinbase",
        providerName = PROVIDER_NAME,
        warning = "Company profiles and news are not provided for Coinbase crypto pairs.",
    )

    override suspend fun getMarketStatus(symbol: String): MarketStatusResult =
        if (normalizeSymbol(symbol) == null) MarketStatusResult.NotSupported
        else MarketStatusResult.Known(MarketStatus.OPEN)

    private fun parseExchangeQuote(symbol: String, normalized: String, body: String): QuoteResult {
        val ticker = json.decodeFromString<CoinbaseTickerResponse>(body)
        val last = ticker.price.toPositiveDouble()
            ?: return QuoteResult.unsupported(symbol, PROVIDER_NAME)
        val statsResponse = executeGet("$exchangeBaseUrl/products/$normalized/stats")
        val stats = if (statsResponse.code in 200..299) {
            json.decodeFromString<CoinbaseStatsResponse>(statsResponse.body)
        } else null
        val open = stats?.open.toPositiveDouble()
        val change = open?.let { last - it }
        val percentChange = open?.takeIf { it != 0.0 }?.let { (last - it) / it * 100.0 }

        return QuoteResult(
            symbol = symbol,
            providerName = PROVIDER_NAME,
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.OPEN,
            freshnessStatus = FreshnessStatus.LIVE,
            isLive = true,
            isDelayed = false,
            isStale = false,
            last = last,
            lastLabel = DataSourceLabel.LIVE,
            bid = ticker.bid.toPositiveDouble(),
            bidLabel = ticker.bid.toPositiveDouble().liveOrUnavailable(),
            ask = ticker.ask.toPositiveDouble(),
            askLabel = ticker.ask.toPositiveDouble().liveOrUnavailable(),
            prevClose = open,
            prevCloseLabel = open.liveOrUnavailable(),
            chg = change,
            chgLabel = change.liveOrUnavailable(),
            pctTdyGainLoss = percentChange,
            pctTdyGainLossLabel = percentChange.liveOrUnavailable(),
            volume = stats?.volume.toPositiveDouble()?.toLong(),
            volumeLabel = stats?.volume.toPositiveDouble().liveOrUnavailable(),
            dayRangeLow = stats?.low.toPositiveDouble(),
            dayRangeHigh = stats?.high.toPositiveDouble(),
            dayRangeLabel = if (stats?.low.toPositiveDouble() != null && stats?.high.toPositiveDouble() != null) {
                DataSourceLabel.LIVE
            } else DataSourceLabel.UNAVAILABLE,
        )
    }

    private fun getSpotQuote(symbol: String, normalized: String): QuoteResult {
        val response = executeGet("$retailBaseUrl/v2/prices/$normalized/spot")
        if (response.code == 429) return QuoteResult.throttled(symbol, PROVIDER_NAME)
        if (response.code == 404) return QuoteResult.unsupported(symbol, PROVIDER_NAME)
        if (response.code !in 200..299) {
            return QuoteResult.error(symbol, PROVIDER_NAME, "Coinbase returned HTTP ${response.code}")
        }
        val spot = json.decodeFromString<CoinbaseSpotResponse>(response.body)
        val last = spot.data.amount.toPositiveDouble() ?: return QuoteResult.unsupported(symbol, PROVIDER_NAME)
        return QuoteResult(
            symbol = symbol,
            providerName = PROVIDER_NAME,
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.OPEN,
            freshnessStatus = FreshnessStatus.LIVE,
            isLive = true,
            isDelayed = false,
            isStale = false,
            last = last,
            lastLabel = DataSourceLabel.LIVE,
        )
    }

    private fun executeGet(url: String): HttpResponse {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "StockStreamLocal")
            .get()
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            HttpResponse(response.code, response.body?.string().orEmpty())
        }
    }

    private fun normalizeSymbol(symbol: String): String? {
        val normalized = symbol.trim().uppercase().replace('_', '-').replace('/', '-')
        val pair = if ('-' in normalized) normalized else "$normalized-USD"
        val parts = pair.split('-')
        return pair.takeIf { parts.size == 2 && parts[0].isNotBlank() && parts[1] == "USD" }
    }

    private fun String?.toPositiveDouble(): Double? = this?.toDoubleOrNull()?.takeIf { it > 0.0 }

    private fun Number?.liveOrUnavailable(): DataSourceLabel =
        if (this != null) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE

    private data class HttpResponse(val code: Int, val body: String)

    companion object {
        const val PROVIDER_NAME = "Coinbase"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

@Serializable
private data class CoinbaseTickerResponse(
    val price: String = "",
    val bid: String = "",
    val ask: String = "",
)

@Serializable
private data class CoinbaseStatsResponse(
    val open: String = "",
    val high: String = "",
    val low: String = "",
    val volume: String = "",
)

@Serializable
private data class CoinbaseSpotResponse(val data: CoinbaseSpotData = CoinbaseSpotData())

@Serializable
private data class CoinbaseSpotData(
    val amount: String = "",
    val base: String = "",
    val currency: String = "",
)