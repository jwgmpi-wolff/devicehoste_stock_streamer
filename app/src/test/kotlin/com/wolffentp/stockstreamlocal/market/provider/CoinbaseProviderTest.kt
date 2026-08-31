package com.wolffentp.stockstreamlocal.market.provider

import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.market.model.FreshnessStatus
import com.wolffentp.stockstreamlocal.security.SecureStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinbaseProviderTest {

    @Test
    fun `factory builds Coinbase provider when Coinbase credentials are configured`() {
        val secureStorage = mockk<SecureStorage>()
        every { secureStorage.getProviderType() } returns ProviderType.COINBASE.name
        every { secureStorage.getApiKey() } returns "stored-private-key"

        val provider = ProviderFactory(secureStorage).buildProvider()

        assertTrue(provider is CoinbaseProvider)
    }

    @Test
    fun `capabilities do not start the Finnhub WebSocket for Coinbase credentials`() {
        val capabilities = CoinbaseProvider().capabilities

        assertEquals(false, capabilities.supportsRealtime)
        assertEquals(false, capabilities.requiresApiKey)
    }

    @Test
    fun `exchange pair returns current quote and 24 hour stats`() = runBlocking {
        val provider = providerWithResponses(
            "/products/BTC-USD/ticker" to ResponseSpec(
                200,
                """{"ask":"101.5","bid":"101.0","volume":"12.3","price":"101.25"}""",
            ),
            "/products/BTC-USD/stats" to ResponseSpec(
                200,
                """{"open":"100","high":"105","low":"98","last":"101.25","volume":"1234.9"}""",
            ),
        )

        val quote = provider.getQuote("btc-usd")

        assertEquals(CoinbaseProvider.PROVIDER_NAME, quote.providerName)
        assertEquals(FreshnessStatus.LIVE, quote.freshnessStatus)
        assertEquals(101.25, quote.last!!, 0.001)
        assertEquals(101.0, quote.bid!!, 0.001)
        assertEquals(101.5, quote.ask!!, 0.001)
        assertEquals(1.25, quote.chg!!, 0.001)
        assertEquals(1.25, quote.pctTdyGainLoss!!, 0.001)
        assertEquals(1234L, quote.volume)
        assertEquals(98.0, quote.dayRangeLow!!, 0.001)
        assertEquals(105.0, quote.dayRangeHigh!!, 0.001)
    }

    @Test
    fun `asset without exchange order book falls back to Coinbase spot price`() = runBlocking {
        val provider = providerWithResponses(
            "/products/USDC-USD/ticker" to ResponseSpec(404, """{"message":"NotFound"}"""),
            "/v2/prices/USDC-USD/spot" to ResponseSpec(
                200,
                """{"data":{"amount":"1","base":"USDC","currency":"USD"}}""",
            ),
        )

        val quote = provider.getQuote("USDC-USD")

        assertEquals(FreshnessStatus.LIVE, quote.freshnessStatus)
        assertEquals(1.0, quote.last!!, 0.0)
        assertEquals(DataSourceLabel.LIVE, quote.lastLabel)
        assertNull(quote.volume)
        assertEquals(DataSourceLabel.UNAVAILABLE, quote.volumeLabel)
    }

    private fun providerWithResponses(vararg responses: Pair<String, ResponseSpec>): CoinbaseProvider {
        val responsesByPath = responses.toMap()
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val request = chain.request()
            val response = responsesByPath[request.url.encodedPath] ?: ResponseSpec(404, "")
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(response.code)
                .message("Test response")
                .body(response.body.toResponseBody("application/json".toMediaType()))
                .build()
        }).build()
        return CoinbaseProvider(
            okHttpClient = client,
            exchangeBaseUrl = "https://exchange.test",
            retailBaseUrl = "https://retail.test",
        )
    }

    private data class ResponseSpec(val code: Int, val body: String)
}