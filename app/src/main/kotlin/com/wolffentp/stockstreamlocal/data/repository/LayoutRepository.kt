package com.wolffentp.stockstreamlocal.data.repository

import com.wolffentp.stockstreamlocal.data.db.dao.ColumnLayoutDao
import com.wolffentp.stockstreamlocal.data.db.dao.RotatingViewDao
import com.wolffentp.stockstreamlocal.data.model.RotatingViewEntity
import com.wolffentp.stockstreamlocal.rotation.DefaultViews
import com.wolffentp.stockstreamlocal.rotation.RotatingViewDefinition
import com.wolffentp.stockstreamlocal.rotation.ViewType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LayoutRepository @Inject constructor(
    private val rotatingViewDao: RotatingViewDao,
    private val columnLayoutDao: ColumnLayoutDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeViews(): Flow<List<RotatingViewEntity>> = rotatingViewDao.observeAll()
    fun observeEnabledViews(): Flow<List<RotatingViewEntity>> = rotatingViewDao.observeEnabled()

    suspend fun getOrSeedDefaultViews(): List<RotatingViewDefinition> {
        val existing = rotatingViewDao.observeAll().first()
        if (existing.isEmpty()) {
            DefaultViews.build().forEach { v ->
                rotatingViewDao.upsert(v.toEntity())
            }
        }
        return rotatingViewDao.observeEnabled().first().map { it.toDomain() }
    }

    suspend fun saveView(view: RotatingViewDefinition) = rotatingViewDao.upsert(view.toEntity())

    suspend fun deleteView(id: String) {
        rotatingViewDao.deleteById(id)
        columnLayoutDao.delete(id)
    }

    suspend fun createCustomView(name: String): RotatingViewDefinition {
        val id = UUID.randomUUID().toString()
        val view = RotatingViewDefinition(
            id = id,
            displayName = name,
            viewType = ViewType.CUSTOM,
            columnNames = listOf("Symbol", "Last", "Chg", "% Tdy G/L"),
            hiddenColumnNames = emptySet(),
        )
        rotatingViewDao.upsert(view.toEntity())
        return view
    }

    private fun RotatingViewDefinition.toEntity() = RotatingViewEntity(
        id = id,
        displayName = displayName,
        viewType = viewType.name,
        columnOrderJson = json.encodeToString(columnNames),
        hiddenColumnsJson = json.encodeToString(hiddenColumnNames.toList()),
        sortColumnName = sortColumnName,
        sortAscending = sortAscending,
        rotationIntervalSeconds = rotationIntervalSeconds,
        refreshIntervalOverrideSeconds = refreshIntervalOverrideSeconds,
        displayOrder = displayOrder,
        isEnabled = isEnabled,
        isFullScreen = isFullScreen,
        updatedAtUtc = System.currentTimeMillis(),
    )

    private fun RotatingViewEntity.toDomain() = RotatingViewDefinition(
        id = id,
        displayName = displayName,
        viewType = runCatching { ViewType.valueOf(viewType) }.getOrElse { ViewType.CUSTOM },
        columnNames = runCatching { json.decodeFromString<List<String>>(columnOrderJson) }.getOrElse { emptyList() },
        hiddenColumnNames = runCatching { json.decodeFromString<List<String>>(hiddenColumnsJson).toSet() }.getOrElse { emptySet() },
        sortColumnName = sortColumnName,
        sortAscending = sortAscending,
        rotationIntervalSeconds = rotationIntervalSeconds,
        refreshIntervalOverrideSeconds = refreshIntervalOverrideSeconds,
        displayOrder = displayOrder,
        isEnabled = isEnabled,
        isFullScreen = isFullScreen,
    )
}
