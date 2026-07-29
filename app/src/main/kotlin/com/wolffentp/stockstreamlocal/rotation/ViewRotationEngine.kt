package com.wolffentp.stockstreamlocal.rotation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the automatic rotation between [RotatingViewDefinition]s.
 *
 * - Rotation only advances when [isPaused] is false.
 * - Manual navigation is always allowed.
 * - The rotation loop is tied to the caller's [CoroutineScope] (ViewModel scope).
 *   It stops automatically when the ViewModel is cleared, preventing background battery drain.
 */
@Singleton
class ViewRotationEngine @Inject constructor() {

    private val _currentView = MutableStateFlow<RotatingViewDefinition?>(null)
    val currentView: StateFlow<RotatingViewDefinition?> = _currentView.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var views: List<RotatingViewDefinition> = emptyList()
    private var rotationJob: Job? = null
    private var isPaused = false

    fun setViews(viewList: List<RotatingViewDefinition>) {
        views = viewList.filter { it.isEnabled }
        val idx = _currentIndex.value.coerceIn(0, (views.size - 1).coerceAtLeast(0))
        _currentIndex.value = idx
        _currentView.value = views.getOrNull(idx)
    }

    fun startRotation(scope: CoroutineScope) {
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (isActive) {
                val view = _currentView.value
                val intervalMs = ((view?.rotationIntervalSeconds ?: 30).coerceAtLeast(5)) * 1000L
                delay(intervalMs)
                if (!isPaused && views.size > 1) advance()
            }
        }
    }

    fun stopRotation() {
        rotationJob?.cancel()
        rotationJob = null
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun next() {
        if (views.isEmpty()) return
        val next = (_currentIndex.value + 1) % views.size
        _currentIndex.value = next
        _currentView.value = views[next]
    }

    fun previous() {
        if (views.isEmpty()) return
        val prev = ((_currentIndex.value - 1) + views.size) % views.size
        _currentIndex.value = prev
        _currentView.value = views[prev]
    }

    fun jumpTo(index: Int) {
        if (index !in views.indices) return
        _currentIndex.value = index
        _currentView.value = views[index]
    }

    private fun advance() = next()
}
