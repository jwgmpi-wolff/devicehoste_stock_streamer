package com.wolffentp.stockstreamlocal.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteSortingTest {

    @Test
    fun `numeric values precede missing values in both directions`() {
        val values = listOf<Double?>(null, 2.0, 10.0)

        val ascending = values.sortedWith { first, second ->
            compareSortValues(first, second, ascending = true)
        }
        val descending = values.sortedWith { first, second ->
            compareSortValues(first, second, ascending = false)
        }

        assertEquals(listOf(2.0, 10.0, null), ascending)
        assertEquals(listOf(10.0, 2.0, null), descending)
    }
}