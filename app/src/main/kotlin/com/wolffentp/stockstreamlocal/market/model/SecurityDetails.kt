package com.wolffentp.stockstreamlocal.market.model

import java.time.Instant

data class SecurityDetails(
    val symbol: String,
    val name: String? = null,
    val assetType: String? = null,
    val exchange: String? = null,
    val industry: String? = null,
    val description: String? = null,
    val marketCapitalization: Double? = null,
    val websiteUrl: String? = null,
    val news: List<NewsArticle> = emptyList(),
    val providerName: String,
    val retrievedAtUtc: Instant = Instant.now(),
    val warning: String? = null,
)

data class NewsArticle(
    val headline: String,
    val summary: String? = null,
    val source: String? = null,
    val url: String,
    val publishedAtUtc: Instant? = null,
)