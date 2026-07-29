package com.wolffentp.stockstreamlocal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wolffentp.stockstreamlocal.data.repository.LayoutRepository
import com.wolffentp.stockstreamlocal.rotation.RotatingViewDefinition
import com.wolffentp.stockstreamlocal.rotation.ViewRotationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RotatingViewViewModel @Inject constructor(
    private val layoutRepository: LayoutRepository,
    private val rotationEngine: ViewRotationEngine,
) : ViewModel() {

    val currentView: StateFlow<RotatingViewDefinition?> = rotationEngine.currentView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentIndex: StateFlow<Int> = rotationEngine.currentIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _views = MutableStateFlow<List<RotatingViewDefinition>>(emptyList())
    val views: StateFlow<List<RotatingViewDefinition>> = _views.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = layoutRepository.getOrSeedDefaultViews()
            _views.value = loaded
            rotationEngine.setViews(loaded)
            rotationEngine.startRotation(viewModelScope)
        }
    }

    fun next() = rotationEngine.next()
    fun previous() = rotationEngine.previous()
    fun jumpTo(index: Int) = rotationEngine.jumpTo(index)
    fun pause() = rotationEngine.pause()
    fun resume() = rotationEngine.resume()

    fun saveView(view: RotatingViewDefinition) {
        viewModelScope.launch {
            layoutRepository.saveView(view)
            reload()
        }
    }

    fun deleteView(id: String) {
        viewModelScope.launch {
            layoutRepository.deleteView(id)
            reload()
        }
    }

    fun createCustomView(name: String) {
        viewModelScope.launch {
            layoutRepository.createCustomView(name)
            reload()
        }
    }

    private suspend fun reload() {
        val updated = layoutRepository.getOrSeedDefaultViews()
        _views.value = updated
        rotationEngine.setViews(updated)
    }

    override fun onCleared() {
        super.onCleared()
        rotationEngine.stopRotation()
    }
}
