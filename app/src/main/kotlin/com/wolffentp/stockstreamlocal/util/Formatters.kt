package com.wolffentp.stockstreamlocal.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val currencyFmt = NumberFormat.getCurrencyInstance(Locale.US)
    private val numberFmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 4 }
    private val volumeFmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    private val pctFmt = NumberFormat.getPercentInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    private val timestampFmt = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault())

    fun currency(value: Double?): String = value?.let { currencyFmt.format(it) } ?: "—"
    fun number(value: Double?): String = value?.let { numberFmt.format(it) } ?: "—"
    fun volume(value: Long?): String = value?.let { volumeFmt.format(it) } ?: "—"
    fun percent(value: Double?): String = value?.let { pctFmt.format(it / 100.0) } ?: "—"
    fun timestamp(instant: Instant?): String = instant?.let { timestampFmt.format(it) } ?: "Never"

    fun range(low: Double?, high: Double?): String {
        return if (low != null && high != null) "${currency(low)} – ${currency(high)}" else "—"
    }

    fun changeWithSign(value: Double?): String {
        if (value == null) return "—"
        return if (value >= 0) "+${currency(value)}" else currency(value)
    }

    fun percentWithSign(value: Double?): String {
        if (value == null) return "—"
        val formatted = percent(value)
        return if (value >= 0) "+$formatted" else formatted
    }
}
