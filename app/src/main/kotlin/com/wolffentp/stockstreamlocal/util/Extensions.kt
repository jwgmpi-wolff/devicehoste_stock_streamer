package com.wolffentp.stockstreamlocal.util

fun String.truncate(maxLen: Int): String =
    if (length <= maxLen) this else take(maxLen) + "…"

fun <T> List<T>.safeSubList(from: Int, to: Int): List<T> =
    subList(from.coerceIn(0, size), to.coerceIn(0, size))

/** Moves an element at [from] to [to] in a new list; safe for out-of-bounds indices. */
fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
