package com.wolffentp.stockstreamlocal.market.provider

import android.util.Log
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatusResult
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.model.SymbolValidationResult
import com.wolffentp.stockstreamlocal.util.LogRedactor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val TAG = "AlphaVantageProvider"
private const val BASE_URL = "https://www.alphavantage.co/query"

/**
 * Concrete [MarketDataProvider] for AlphaVantage (https://www.alphavantage.co).
 *
 * Free tier limits: 25 requests/day, 5 requests/minute.
 * The minimum poll interval must be set to at least 13 seconds to stay within 5/minute.
 *
 * Fields NOT provided by AlphaVantage GLOBAL_QUOTE (free tier):
 * - Bid / Ask (not in GLOBAL_QUOTE)
 * - 52 Week Range (requires OVERVIEW endpoint; premium)
 * - Earnings Date (requires EARNINGS endpoint)
 * - Dividend Date (requires OVERVIEW endpoint)
 *
 * These fields are returned as null with [DataSourceLabel.NOT_PROVIDED_BY_SOURCE].
 * They are NEVER synthesized or guessed.
 */
class AlphaVantageProvider(
    private val apiKey: String,
    private val okHttpClient: OkHttpClient = defaultClient(),
) : MarketDataProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override val capabilities = ProviderCapabilities(
        providerName = PROVIDER_NAME,
        supportsRealtime = true,   // Intraday data; latency varies by plan
        supportsDelayed = true,
        supportsBidAsk = false,    // Not in GLOBAL_QUOTE
        supportsVolume = true,
        supportsDayRange = true,
        supports52WeekRange = false, // Premium only
        supportsEarningsDate = false,
        supportsDividendDate = false,
        supportsMarketStatus = false, // Not directly provided; we infer from time
        requiresApiKey = true,
        minimumPollIntervalSeconds = 15, // Conservative: 5 requests/min max
        rateLimitDescription = "Free tier: 25 requests/day, 5 requests/minute.",
    )

    override suspend fun validateSymbol(symbol: String): SymbolValidationResult {
        if (apiKey.isBlank()) return SymbolValidationResult.ProviderNotConfigured
        val url = "$BASE_URL?function=SYMBOL_SEARCH&keywords=${symbol.trim()}&apikey=$apiKey"
        return try {
            val responseBody = executeGet(url)
            val response = json.decodeFromString<AlphaVantageSearchResponse>(responseBody)
            when {
                response.note != null -> SymbolValidationResult.ProviderError(symbol, "Rate limit: ${response.note}")
                response.information != null -> SymbolValidationResult.ProviderError(symbol, response.information)
                response.bestMatches.isNullOrEmpty() -> SymbolValidationResult.Invalid(symbol, "Symbol not found")
                else -> {
                    val match = response.bestMatches.firstOrNull {
                        it.symbol.equals(symbol.trim(), ignoreCase = true)
                    } ?: response.bestMatches.first()
                    SymbolValidationResult.Valid(symbol = match.symbol, name = match.name)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Symbol validation network error") // No symbol logged
            SymbolValidationResult.ProviderError(symbol, "Network error: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Symbol validation parse error")
            SymbolValidationResult.ProviderError(symbol, "Parse error: ${e.message}")
        }
    }

    override suspend fun getQuote(symbol: String): QuoteResult {
        if (apiKey.isBlank()) return QuoteResult.notConfigured(symbol)
        val url = "$BASE_URL?function=GLOBAL_QUOTE&symbol=${symbol.trim()}&apikey=$apiKey"
        return try {
            val responseBody = executeGet(url)
            parseGlobalQuoteResponse(symbol, responseBody)
        } catch (e: IOException) {
            Log.w(TAG, "Quote fetch network error for redacted symbol")
            QuoteResult.error(symbol, PROVIDER_NAME, "Network error: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Quote parse error")
            QuoteResult.error(symbol, PROVIDER_NAME, "Parse error: ${e.message}")
        }
    }

    override suspend fun getQuotes(symbols: List<String>): List<QuoteResult> {
        return symbols.map { symbol ->
            val result = getQuote(symbol)
            // Respect rate limit between calls: minimum 200ms gap
            kotlinx.coroutines.delay(200)
            result
        }
    }

    override suspend fun getMarketStatus(symbol: String): MarketStatusResult {
        // AlphaVantage does not provide a direct market-status endpoint on the free tier.
        // We infer US market status from wall-clock time in Eastern Time.
        return MarketStatusResult.Known(inferUsMarketStatus())
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private fun executeGet(url: String): String {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            return response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    private fun parseGlobalQuoteResponse(symbol: String, body: String): QuoteResult {
        val response = json.decodeFromString<AlphaVantageGlobalQuoteResponse>(body)

        // Rate-limit note — no price data available
        if (response.note != null) {
            Log.w(TAG, "AlphaVantage rate limit reached") // Not logging note content (may contain key info)
            return QuoteResult.throttled(symbol, PROVIDER_NAME)
        }

        // Premium/info message — treat as configuration error
        if (response.information != null) {
            return QuoteResult.error(symbol, PROVIDER_NAME, "Provider information: see provider documentation")
        }

        // Error message from provider
        if (response.errorMessage != null) {
            return QuoteResult.error(symbol, PROVIDER_NAME, "Provider error")
        }

        val q = response.globalQuote
        // Empty global quote object means symbol not found
        if (q == null || q.symbol.isBlank()) {
            return QuoteResult.unsupported(symbol, PROVIDER_NAME)
        }

        val marketStatus = inferUsMarketStatus()
        val isLive = marketStatus == MarketStatus.OPEN
        val isDelayed = !isLive // AlphaVantage free tier may have data latency
        val freshness = if (isLive) FreshnessStatus.LIVE else FreshnessStatus.MARKET_CLOSED

        val last = q.price.toDoubleOrNull()
        val prevClose = q.previousClose.toDoubleOrNull()
        val chg = q.change.toDoubleOrNull()
        val pctChg = q.changePercent.removeSuffix("%").toDoubleOrNull()
        val volume = q.volume.toLongOrNull()
        val dayLow = q.low.toDoubleOrNull()
        val dayHigh = q.high.toDoubleOrNull()

        return QuoteResult(
            symbol = q.symbol,
            providerName = PROVIDER_NAME,
            retrievedAtUtc = Instant.now(),
            marketStatus = marketStatus,
            freshnessStatus = freshness,
            isLive = isLive,
            isDelayed = isDelayed,
            isStale = false,

            last = last,
            lastLabel = if (last != null) (if (isLive) DataSourceLabel.LIVE else DataSourceLabel.DELAYED)
                        else DataSourceLabel.ERROR,

            // Bid/Ask NOT provided by AlphaVantage GLOBAL_QUOTE
            bid = null,
            bidLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,
            ask = null,
            askLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,

            prevClose = prevClose,
            prevCloseLabel = if (prevClose != null) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            chg = chg,
            chgLabel = if (chg != null) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            pctTdyGainLoss = pctChg,
            pctTdyGainLossLabel = if (pctChg != null) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            volume = volume,
            volumeLabel = if (volume != null) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            dayRangeLow = dayLow,
            dayRangeHigh = dayHigh,
            dayRangeLabel = if (dayLow != null && dayHigh != null) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            // 52 Week Range NOT in GLOBAL_QUOTE free tier
            weekRange52Low = null,
            weekRange52High = null,
            weekRange52Label = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,

            // Earnings / Dividend dates NOT in GLOBAL_QUOTE
            earningsDate = null,
            earningsDateLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,
            divDate = null,
            divDateLabel = DataSourceLabel.NOT_PROVIDED_BY_SOURCE,
        )
    }

    private fun inferUsMarketStatus(): MarketStatus {
        val et = ZonedDateTime.now(ZoneId.of("America/New_York"))
        val dayOfWeek = et.dayOfWeek
        val time = et.toLocalTime()
        val marketOpen = LocalTime.of(9, 30)
        val marketClose = LocalTime.of(16, 0)
        val preMarketOpen = LocalTime.of(4, 0)
        val afterHoursClose = LocalTime.of(20, 0)
        return when {
            dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY -> MarketStatus.CLOSED
            time >= marketOpen && time < marketClose -> MarketStatus.OPEN
            time >= preMarketOpen && time < marketOpen -> MarketStatus.PRE_MARKET
            time >= marketClose && time < afterHoursClose -> MarketStatus.AFTER_HOURS
            else -> MarketStatus.CLOSED
        }
    }

    companion object {
        const val PROVIDER_NAME = "AlphaVantage"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

// ─── AlphaVantage JSON models ──────────────────────────────────────────────

@Serializable
private data class AlphaVantageGlobalQuoteResponse(
    @SerialName("Global Quote") val globalQuote: AlphaVantageGlobalQuote? = null,
    @SerialName("Note") val note: String? = null,
    @SerialName("Information") val information: String? = null,
    @SerialName("Error Message") val errorMessage: String? = null,
)

@Serializable
private data class AlphaVantageGlobalQuote(
    @SerialName("01. symbol") val symbol: String = "",
    @SerialName("02. open") val open: String = "",
    @SerialName("03. high") val high: String = "",
    @SerialName("04. low") val low: String = "",
    @SerialName("05. price") val price: String = "",
    @SerialName("06. volume") val volume: String = "",
    @SerialName("07. latest trading day") val latestTradingDay: String = "",
    @SerialName("08. previous close") val previousClose: String = "",
    @SerialName("09. change") val change: String = "",
    @SerialName("10. change percent") val changePercent: String = "",
)

@Serializable
private data class AlphaVantageSearchResponse(
    @SerialName("bestMatches") val bestMatches: List<AlphaVantageSearchMatch>? = null,
    @SerialName("Note") val note: String? = null,
    @SerialName("Information") val information: String? = null,
)

@Serializable
private data class AlphaVantageSearchMatch(
    @SerialName("1. symbol") val symbol: String = "",
    @SerialName("2. name") val name: String = "",
    @SerialName("3. type") val type: String = "",
    @SerialName("4. region") val region: String = "",
    @SerialName("9. matchScore") val matchScore: String = "",
)
