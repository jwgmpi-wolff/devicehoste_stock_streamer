package com.wolffentp.stockstreamlocal.market.provider

import android.util.Log
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatus
import com.wolffentp.stockstreamlocal.market.model.MarketStatusResult
import com.wolffentp.stockstreamlocal.market.model.NewsArticle
import com.wolffentp.stockstreamlocal.market.model.QuoteResult
import com.wolffentp.stockstreamlocal.market.model.SecurityDetails
import com.wolffentp.stockstreamlocal.market.model.SymbolValidationResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

private const val TAG = "FinnhubProvider"
private const val BASE_URL = "https://finnhub.io/api/v1"

/**
 * Concrete [MarketDataProvider] for Finnhub (https://finnhub.io).
 *
 * Free tier limits: 60 API calls per minute.
 * Provides real-time quotes, no attribution, no fake data.
 *
 * Fields NOT provided by Finnhub:
 * - Bid/Ask spreads (quote endpoint returns bid/ask)
 * - 52 Week Range, Earnings Date, Dividend Date (requires different endpoints)
 */
class FinnhubProvider(
    private val apiKey: String,
    private val okHttpClient: OkHttpClient = defaultClient(),
) : MarketDataProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override val capabilities = ProviderCapabilities(
        providerName = PROVIDER_NAME,
        supportsRealtime = true,
        supportsDelayed = false,
        supportsBidAsk = true,
        supportsVolume = true,
        supportsDayRange = true,
        supports52WeekRange = false,
        supportsEarningsDate = false,
        supportsDividendDate = false,
        supportsMarketStatus = true,
        requiresApiKey = true,
        minimumPollIntervalSeconds = 2, // 60 req/min = 1 per second max, but be conservative
        rateLimitDescription = "Free tier: 60 API calls per minute.",
    )

    override suspend fun validateSymbol(symbol: String): SymbolValidationResult {
        if (apiKey.isBlank()) return SymbolValidationResult.ProviderNotConfigured
        val url = "$BASE_URL/search?q=${symbol.trim()}&token=$apiKey"
        return try {
            val responseBody = executeGet(url)
            val response = json.decodeFromString<FinnhubSearchResponse>(responseBody)
            when {
                response.result.isNullOrEmpty() -> SymbolValidationResult.Invalid(symbol, "Symbol not found")
                else -> {
                    val match = response.result.firstOrNull {
                        it.symbol.equals(symbol.trim(), ignoreCase = true)
                    }
                    if (match == null) SymbolValidationResult.Invalid(symbol, "Exact symbol not found")
                    else SymbolValidationResult.Valid(symbol = match.symbol, name = match.description)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Symbol validation network error")
            SymbolValidationResult.ProviderError(symbol, "Network error: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Symbol validation parse error")
            SymbolValidationResult.ProviderError(symbol, "Parse error: ${e.message}")
        }
    }

    override suspend fun getQuote(symbol: String): QuoteResult {
        if (apiKey.isBlank()) return QuoteResult.notConfigured(symbol)
        val url = "$BASE_URL/quote?symbol=${symbol.trim()}&token=$apiKey"
        return try {
            val responseBody = executeGet(url)
            val quote = parseQuoteResponse(symbol, responseBody)
            if (quote.last == null) quote else {
                val volume = fetchLatestDailyVolume(symbol)
                quote.copy(
                    volume = volume,
                    volumeLabel = if (volume != null) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,
                )
            }
        } catch (e: IOException) {
            Log.w(TAG, "Quote fetch network error")
            QuoteResult.error(symbol, PROVIDER_NAME, "Network error: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Quote parse error")
            QuoteResult.error(symbol, PROVIDER_NAME, "Parse error: ${e.message}")
        }
    }

    override suspend fun getQuotes(symbols: List<String>): List<QuoteResult> {
        return symbols.map { symbol ->
            val result = getQuote(symbol)
            // Respect rate limit: 60 req/min = 1 per second
            kotlinx.coroutines.delay(1100)
            result
        }
    }

    override suspend fun getSecurityDetails(symbol: String): SecurityDetails {
        if (apiKey.isBlank()) {
            return SecurityDetails(symbol, providerName = PROVIDER_NAME, warning = "Provider is not configured.")
        }
        val normalized = symbol.trim().uppercase()
        var profile: FinnhubProfile? = null
        var news = emptyList<NewsArticle>()
        val warnings = mutableListOf<String>()

        try {
            profile = json.decodeFromString<FinnhubProfile>(
                executeGet("$BASE_URL/stock/profile2?symbol=$normalized&token=$apiKey"),
            )
        } catch (e: Exception) {
            warnings += "Company or fund profile is unavailable."
        }

        try {
            val today = LocalDate.now(ZoneOffset.UTC)
            val from = today.minusDays(14)
            news = json.decodeFromString<List<FinnhubNewsArticle>>(
                executeGet("$BASE_URL/company-news?symbol=$normalized&from=$from&to=$today&token=$apiKey"),
            ).mapNotNull { article ->
                val headline = article.headline.trim()
                val url = article.url.trim()
                if (headline.isBlank() || url.isBlank()) null else NewsArticle(
                    headline = headline,
                    summary = article.summary.takeIf { it.isNotBlank() },
                    source = article.source.takeIf { it.isNotBlank() },
                    url = url,
                    publishedAtUtc = article.datetime.takeIf { it > 0 }?.let(Instant::ofEpochSecond),
                )
            }.sortedByDescending { it.publishedAtUtc }.take(10)
        } catch (e: Exception) {
            warnings += "Recent news is unavailable."
        }

        return SecurityDetails(
            symbol = normalized,
            name = profile?.name?.takeIf { it.isNotBlank() },
            assetType = profile?.finnhubIndustry?.takeIf { it.isNotBlank() }?.let { "Equity" },
            exchange = profile?.exchange?.takeIf { it.isNotBlank() },
            industry = profile?.finnhubIndustry?.takeIf { it.isNotBlank() },
            marketCapitalization = profile?.marketCapitalization?.takeIf { it > 0 },
            websiteUrl = profile?.weburl?.takeIf { it.isNotBlank() },
            news = news,
            providerName = PROVIDER_NAME,
            warning = warnings.takeIf { it.isNotEmpty() }?.joinToString(" "),
        )
    }

    override suspend fun getMarketStatus(symbol: String): MarketStatusResult {
        // Finnhub doesn't directly provide market status; infer from time
        val now = Instant.now()
        val isOpen = isUsMarketOpen(now)
        return MarketStatusResult.Known(if (isOpen) MarketStatus.OPEN else MarketStatus.CLOSED)
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            return response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    private fun fetchLatestDailyVolume(symbol: String): Long? {
        return try {
            val to = Instant.now().epochSecond
            val from = to - TimeUnit.DAYS.toSeconds(7)
            val body = executeGet("$BASE_URL/stock/candle?symbol=${symbol.trim()}&resolution=D&from=$from&to=$to&token=$apiKey")
            val response = json.decodeFromString<FinnhubCandleResponse>(body)
            response.volume?.lastOrNull()?.takeIf { it > 0 }
        } catch (e: Exception) {
            Log.w(TAG, "Daily volume unavailable")
            null
        }
    }

    private fun parseQuoteResponse(symbol: String, body: String): QuoteResult {
        val response = json.decodeFromString<FinnhubQuoteResponse>(body)

        // Finnhub returns zeros for missing data; check if we got any price
        if (response.c == 0.0) {
            return QuoteResult.unsupported(symbol, PROVIDER_NAME)
        }

        val last = response.c
        val prevClose = response.pc
        val chg = response.d
        val pctChg = response.dp
        val high = response.h
        val low = response.l
        val open = response.o

        val isLive = response.t != null // timestamp present = live quote
        val freshness = if (isLive) FreshnessStatus.LIVE else FreshnessStatus.DELAYED

        return QuoteResult(
            symbol = symbol,
            providerName = PROVIDER_NAME,
            retrievedAtUtc = Instant.now(),
            marketStatus = MarketStatus.OPEN,
            freshnessStatus = freshness,
            isLive = isLive,
            isDelayed = !isLive,
            isStale = false,

            last = if (last > 0) last else null,
            lastLabel = if (last > 0) (if (isLive) DataSourceLabel.LIVE else DataSourceLabel.DELAYED) else DataSourceLabel.ERROR,

            bid = response.bp?.takeIf { it > 0 },
            bidLabel = response.bp?.takeIf { it > 0 }?.let { DataSourceLabel.LIVE } ?: DataSourceLabel.UNAVAILABLE,

            ask = response.ap?.takeIf { it > 0 },
            askLabel = response.ap?.takeIf { it > 0 }?.let { DataSourceLabel.LIVE } ?: DataSourceLabel.UNAVAILABLE,

            prevClose = if (prevClose > 0) prevClose else null,
            prevCloseLabel = if (prevClose > 0) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            chg = if (chg != 0.0) chg else null,
            chgLabel = if (chg != 0.0) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            pctTdyGainLoss = if (pctChg != 0.0) pctChg else null,
            pctTdyGainLossLabel = if (pctChg != 0.0) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,

            volume = null,
            volumeLabel = DataSourceLabel.UNAVAILABLE,

            dayRangeLow = if (low > 0) low else null,
            dayRangeHigh = if (high > 0) high else null,
            dayRangeLabel = if (low > 0 && high > 0) DataSourceLabel.LIVE else DataSourceLabel.UNAVAILABLE,
        )
    }

    private fun isUsMarketOpen(instant: Instant): Boolean {
        val et = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.of("America/New_York"))
        val day = et.dayOfWeek
        val time = et.toLocalTime()
        return day != java.time.DayOfWeek.SATURDAY &&
               day != java.time.DayOfWeek.SUNDAY &&
               time >= java.time.LocalTime.of(9, 30) &&
               time < java.time.LocalTime.of(16, 0)
    }

    companion object {
        const val PROVIDER_NAME = "Finnhub"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

// ─── Finnhub JSON models ──────────────────────────────────────────────

@Serializable
private data class FinnhubQuoteResponse(
    @SerialName("c") val c: Double = 0.0,           // current price
    @SerialName("h") val h: Double = 0.0,           // day high
    @SerialName("l") val l: Double = 0.0,           // day low
    @SerialName("o") val o: Double = 0.0,           // open price
    @SerialName("pc") val pc: Double = 0.0,         // previous close
    @SerialName("d") val d: Double = 0.0,           // change
    @SerialName("dp") val dp: Double = 0.0,         // change percent
    @SerialName("t") val t: Long? = null,           // timestamp (null if delayed)
    @SerialName("bid") val bp: Double? = null,      // retained for provider plan variants
    @SerialName("ask") val ap: Double? = null,      // retained for provider plan variants
)

@Serializable
private data class FinnhubCandleResponse(
    @SerialName("v") val volume: List<Long>? = null,
)

@Serializable
private data class FinnhubProfile(
    val name: String = "",
    val exchange: String = "",
    val weburl: String = "",
    val marketCapitalization: Double? = null,
    val finnhubIndustry: String = "",
)

@Serializable
private data class FinnhubNewsArticle(
    val datetime: Long = 0,
    val headline: String = "",
    val source: String = "",
    val summary: String = "",
    val url: String = "",
)

@Serializable
private data class FinnhubSearchResponse(
    @SerialName("result") val result: List<FinnhubSearchMatch>? = null,
)

@Serializable
private data class FinnhubSearchMatch(
    @SerialName("symbol") val symbol: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("type") val type: String = "",
)
