package com.wolffentp.stockstreamlocal.columns

import com.wolffentp.stockstreamlocal.data.db.dao.ColumnLayoutDao
import com.wolffentp.stockstreamlocal.data.model.ColumnLayoutEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedColumnLayout(
    val viewId: String,
    val orderedColumns: List<ColumnDefinition>,
    val hiddenColumnNames: Set<String>,
    val visibleColumns: List<ColumnDefinition>,
    val sortColumnName: String?,
    val sortAscending: Boolean,
)

@Singleton
class ColumnLayoutManager @Inject constructor(
    private val dao: ColumnLayoutDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeLayout(viewId: String): Flow<ResolvedColumnLayout> =
        dao.observe(viewId).map { entity -> entity?.resolve() ?: defaultLayout(viewId) }

    suspend fun getLayout(viewId: String): ResolvedColumnLayout =
        dao.get(viewId)?.resolve() ?: defaultLayout(viewId)

    suspend fun saveLayout(layout: ResolvedColumnLayout) {
        dao.upsert(
            ColumnLayoutEntity(
                viewId = layout.viewId,
                columnOrderJson = json.encodeToString(layout.orderedColumns.map { it.name }),
                hiddenColumnsJson = json.encodeToString(layout.hiddenColumnNames.toList()),
                sortColumnName = layout.sortColumnName,
                sortAscending = layout.sortAscending,
                updatedAtUtc = System.currentTimeMillis(),
            )
        )
    }

    suspend fun resetToDefault(viewId: String) {
        dao.delete(viewId)
    }

    suspend fun moveColumn(viewId: String, from: Int, to: Int) {
        val layout = getLayout(viewId)
        val reordered = layout.orderedColumns.toMutableList().apply { add(to, removeAt(from)) }
        saveLayout(layout.copy(orderedColumns = reordered))
    }

    suspend fun toggleColumnVisibility(viewId: String, columnName: String) {
        val layout = getLayout(viewId)
        val hidden = layout.hiddenColumnNames.toMutableSet()
        if (columnName in hidden) hidden.remove(columnName) else hidden.add(columnName)
        saveLayout(layout.copy(hiddenColumnNames = hidden))
    }

    private fun ColumnLayoutEntity.resolve(): ResolvedColumnLayout {
        val orderNames: List<String> = runCatching {
            json.decodeFromString<List<String>>(columnOrderJson)
        }.getOrElse { AllColumns.defaultVisibleNames }

        val hiddenNames: Set<String> = runCatching {
            json.decodeFromString<List<String>>(hiddenColumnsJson).toSet()
        }.getOrElse { emptySet() }

        val ordered = orderNames.mapNotNull { AllColumns.byName[it] }
            .ifEmpty { AllColumns.definitions.filter { it.defaultVisible } }

        return ResolvedColumnLayout(
            viewId = viewId,
            orderedColumns = ordered,
            hiddenColumnNames = hiddenNames,
            visibleColumns = ordered.filter { it.name !in hiddenNames },
            sortColumnName = sortColumnName,
            sortAscending = sortAscending,
        )
    }

    private fun defaultLayout(viewId: String) = ResolvedColumnLayout(
        viewId = viewId,
        orderedColumns = AllColumns.definitions.filter { it.defaultVisible },
        hiddenColumnNames = emptySet(),
        visibleColumns = AllColumns.definitions.filter { it.defaultVisible },
        sortColumnName = null,
        sortAscending = true,
    )
}
