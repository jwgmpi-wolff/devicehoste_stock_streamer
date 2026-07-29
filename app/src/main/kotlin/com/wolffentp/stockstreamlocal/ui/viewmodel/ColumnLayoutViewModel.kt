package com.wolffentp.stockstreamlocal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wolffentp.stockstreamlocal.columns.ColumnDefinition
import com.wolffentp.stockstreamlocal.columns.ColumnLayoutManager
import com.wolffentp.stockstreamlocal.columns.ResolvedColumnLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ColumnLayoutViewModel @Inject constructor(
    private val layoutManager: ColumnLayoutManager,
) : ViewModel() {

    private val _viewId = MutableStateFlow("default")

    val layout: StateFlow<ResolvedColumnLayout?> = _viewId
        .flatMapLatest { layoutManager.observeLayout(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setViewId(id: String) { _viewId.value = id }

    fun moveColumn(from: Int, to: Int) {
        viewModelScope.launch { layoutManager.moveColumn(_viewId.value, from, to) }
    }

    fun toggleColumnVisibility(columnName: String) {
        viewModelScope.launch { layoutManager.toggleColumnVisibility(_viewId.value, columnName) }
    }

    fun resetToDefault() {
        viewModelScope.launch { layoutManager.resetToDefault(_viewId.value) }
    }

    fun saveLayout(layout: ResolvedColumnLayout) {
        viewModelScope.launch { layoutManager.saveLayout(layout) }
    }
}
