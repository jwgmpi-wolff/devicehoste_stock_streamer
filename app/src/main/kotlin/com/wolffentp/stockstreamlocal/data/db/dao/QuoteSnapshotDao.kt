package com.wolffentp.stockstreamlocal.data.db.dao

import androidx.room.*
import com.wolffentp.stockstreamlocal.data.model.QuoteSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteSnapshotDao {
    @Query("SELECT * FROM quote_snapshots ORDER BY symbol ASC")
    fun observeAll(): Flow<List<QuoteSnapshotEntity>>

    @Query("SELECT * FROM quote_snapshots WHERE symbol = :symbol")
    suspend fun getBySymbol(symbol: String): QuoteSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: QuoteSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(snapshots: List<QuoteSnapshotEntity>)

    @Query("DELETE FROM quote_snapshots WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("DELETE FROM quote_snapshots")
    suspend fun deleteAll()
}
