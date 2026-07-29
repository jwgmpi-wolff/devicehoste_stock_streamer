package com.wolffentp.stockstreamlocal.rotation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewRotationTest {

    private lateinit var engine: ViewRotationEngine

    private val v1 = RotatingViewDefinition("v1", "Quote", ViewType.QUOTE, listOf("Symbol", "Last"), emptySet(), rotationIntervalSeconds = 5)
    private val v2 = RotatingViewDefinition("v2", "G/L", ViewType.GAIN_LOSS, listOf("Symbol", "G/L"), emptySet(), rotationIntervalSeconds = 5)
    private val v3 = RotatingViewDefinition("v3", "Volume", ViewType.VOLUME, listOf("Symbol", "Volume"), emptySet(), rotationIntervalSeconds = 5)

    @Before
    fun setup() {
        engine = ViewRotationEngine()
        engine.setViews(listOf(v1, v2, v3))
    }

    @Test
    fun `setViews selects first view by default`() {
        assertEquals(v1.id, engine.currentView.value?.id)
        assertEquals(0, engine.currentIndex.value)
    }

    @Test
    fun `next advances to second view`() {
        engine.next()
        assertEquals(v2.id, engine.currentView.value?.id)
        assertEquals(1, engine.currentIndex.value)
    }

    @Test
    fun `next wraps around from last to first`() {
        engine.jumpTo(2)
        engine.next()
        assertEquals(v1.id, engine.currentView.value?.id)
        assertEquals(0, engine.currentIndex.value)
    }

    @Test
    fun `previous wraps from first to last`() {
        engine.previous()
        assertEquals(v3.id, engine.currentView.value?.id)
        assertEquals(2, engine.currentIndex.value)
    }

    @Test
    fun `jumpTo navigates to correct index`() {
        engine.jumpTo(2)
        assertEquals(v3.id, engine.currentView.value?.id)
    }

    @Test
    fun `jumpTo out of bounds does nothing`() {
        engine.jumpTo(99)
        // Should still be at first view
        assertEquals(v1.id, engine.currentView.value?.id)
    }

    @Test
    fun `pause prevents automatic rotation`() = runTest {
        engine.startRotation(this)
        engine.pause()
        val before = engine.currentIndex.value
        advanceTimeBy(10_000) // advance past rotation interval
        assertEquals("Index should not change while paused", before, engine.currentIndex.value)
        engine.stopRotation()
    }

    @Test
    fun `disabled views are filtered out`() {
        val disabledV3 = v3.copy(isEnabled = false)
        engine.setViews(listOf(v1, v2, disabledV3))
        // Jump past end — only 2 views
        engine.jumpTo(2)
        // Should still be on last valid index (1)
        assertTrue(engine.currentIndex.value <= 1)
    }

    @Test
    fun `rotation interval from view definition is respected`() = runTest {
        val slowView = v1.copy(rotationIntervalSeconds = 60)
        engine.setViews(listOf(slowView, v2))
        engine.startRotation(this)
        advanceTimeBy(30_000)
        assertEquals("Should not have rotated yet (60s interval)", 0, engine.currentIndex.value)
        advanceTimeBy(35_000)
        assertEquals("Should have rotated after 65s total", 1, engine.currentIndex.value)
        engine.stopRotation()
    }
}
