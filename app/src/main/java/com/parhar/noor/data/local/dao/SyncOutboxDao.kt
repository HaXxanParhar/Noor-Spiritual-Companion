package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {

    @Query("SELECT * FROM sync_outbox ORDER BY created_at ASC")
    suspend fun getAll(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SyncOutboxEntity): Long

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        "UPDATE sync_outbox SET retry_count = retry_count + 1, last_error = :error WHERE id = :id",
    )
    suspend fun markFailed(id: Long, error: String)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Int
}
