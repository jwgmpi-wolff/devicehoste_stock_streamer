package com.wolffentp.stockstreamlocal.data.db.dao

import androidx.room.*
import com.wolffentp.stockstreamlocal.data.model.ColumnLayoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColumnLayoutDao {
    @Query("SELECT * FROM column_layouts WHERE viewId = :viewId")
    fun observe(viewId: String): Flow<ColumnLayoutEntity?>

    @Query("SELECT * FROM column_layouts WHERE viewId = :viewId")
    suspend fun get(viewId: String): ColumnLayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(layout: ColumnLayoutEntity)

    @Query("DELETE FROM column_layouts WHERE viewId = :viewId")
    suspend fun delete(viewId: String)

    @Query("DELETE FROM column_layouts")
    suspend fun deleteAll()
}
