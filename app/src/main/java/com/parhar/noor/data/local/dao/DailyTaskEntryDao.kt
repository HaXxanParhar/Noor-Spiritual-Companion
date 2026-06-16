package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.entity.DailyTaskEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskEntryDao {

    @Query(
        "SELECT * FROM daily_task_entries WHERE user_uid = :userUid AND date_key = :dateKey",
    )
    fun observeForDate(userUid: String, dateKey: String): Flow<List<DailyTaskEntryEntity>>

    @Query(
        "SELECT * FROM daily_task_entries WHERE user_uid = :userUid AND date_key = :dateKey",
    )
    suspend fun getForDate(userUid: String, dateKey: String): List<DailyTaskEntryEntity>

    @Query("SELECT * FROM daily_task_entries WHERE user_uid = :userUid")
    fun observeForUser(userUid: String): Flow<List<DailyTaskEntryEntity>>

    @Query("SELECT * FROM daily_task_entries WHERE user_uid = :userUid")
    suspend fun getForUser(userUid: String): List<DailyTaskEntryEntity>

    @Query("SELECT * FROM daily_task_entries")
    fun observeAll(): Flow<List<DailyTaskEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DailyTaskEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<DailyTaskEntryEntity>)

    @Query(
        "SELECT * FROM daily_task_entries WHERE user_uid = :userUid AND date_key = :dateKey AND sync_status = :status",
    )
    suspend fun getPendingForDate(
        userUid: String,
        dateKey: String,
        status: SyncStatus = SyncStatus.PENDING_PUSH,
    ): List<DailyTaskEntryEntity>

    @Query("SELECT DISTINCT date_key FROM daily_task_entries WHERE user_uid = :userUid AND sync_status = :status")
    suspend fun getPendingDateKeys(userUid: String, status: SyncStatus = SyncStatus.PENDING_PUSH): List<String>

    @Query("DELETE FROM daily_task_entries WHERE user_uid = :userUid AND date_key = :dateKey")
    suspend fun deleteForDate(userUid: String, dateKey: String)
}
