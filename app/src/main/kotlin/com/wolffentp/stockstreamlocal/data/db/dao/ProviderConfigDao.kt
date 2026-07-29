package com.wolffentp.stockstreamlocal.data.db.dao

import androidx.room.*
import com.wolffentp.stockstreamlocal.data.model.ProviderConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderConfigDao {
    @Query("SELECT * FROM provider_config WHERE id = 1")
    fun observe(): Flow<ProviderConfigEntity?>

    @Query("SELECT * FROM provider_config WHERE id = 1")
    suspend fun get(): ProviderConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: ProviderConfigEntity)

    @Query("DELETE FROM provider_config")
    suspend fun deleteAll()
}
