package com.wolffentp.stockstreamlocal.data.db.dao

import androidx.room.*
import com.wolffentp.stockstreamlocal.data.model.RotatingViewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RotatingViewDao {
    @Query("SELECT * FROM rotating_views ORDER BY displayOrder ASC, displayName ASC")
    fun observeAll(): Flow<List<RotatingViewEntity>>

    @Query("SELECT * FROM rotating_views WHERE isEnabled = 1 ORDER BY displayOrder ASC")
    fun observeEnabled(): Flow<List<RotatingViewEntity>>

    @Query("SELECT * FROM rotating_views WHERE id = :id")
    suspend fun getById(id: String): RotatingViewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(view: RotatingViewEntity)

    @Query("DELETE FROM rotating_views WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM rotating_views")
    suspend fun deleteAll()
}
