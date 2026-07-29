package com.wolffentp.stockstreamlocal.columns

import org.junit.Assert.*
import org.junit.Test

class ColumnLayoutTest {

    @Test
    fun `AllColumns contains all 20 CSV column names`() {
        val names = AllColumns.definitions.map { it.name }.toSet()
        assertEquals(20, names.size)
        listOf(
            "Symbol", "Last", "Bid", "Chg", "Ask", "Tdy G/L", "Quantity", "Volume",
            "Day Range", "52 Wk Range", "Purchase Price", "Value", "% Tdy G/L",
            "G/L", "% G/L", "Account", "Close Value", "Earnings Date", "Div Date", "Prev Close",
        ).forEach { col ->
            assertTrue("Missing column: $col", col in names)
        }
    }

    @Test
    fun `defaultVisibleNames excludes columns marked defaultVisible=false`() {
        val visible = AllColumns.defaultVisibleNames.toSet()
        // Bid and Ask are defaultVisible=false
        assertFalse("Bid should not be visible by default", "Bid" in visible)
        assertFalse("Ask should not be visible by default", "Ask" in visible)
    }

    @Test
    fun `byName lookup is case-sensitive and returns correct definition`() {
        val def = AllColumns.byName["Symbol"]
        assertNotNull(def)
        assertEquals("Symbol", def!!.name)
        assertTrue(def.isText)
        assertFalse(def.isNumeric)
    }

    @Test
    fun `currency columns are flagged correctly`() {
        val last = AllColumns.byName["Last"]!!
        assertTrue(last.isCurrency)
        assertFalse(last.isPercent)
    }

    @Test
    fun `percent columns are flagged correctly`() {
        val pctGL = AllColumns.byName["% G/L"]!!
        assertTrue(pctGL.isPercent)
        assertFalse(pctGL.isCurrency)
    }

    @Test
    fun `date columns are flagged correctly`() {
        val earnings = AllColumns.byName["Earnings Date"]!!
        assertTrue(earnings.isDate)
        assertFalse(earnings.isNumeric)
    }
}
