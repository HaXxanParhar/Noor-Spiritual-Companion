package com.parhar.noor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.parhar.noor.data.local.SyncStatus

@Entity(
    tableName = "daily_task_entries",
    primaryKeys = ["user_uid", "date_key", "task_id"],
    indices = [Index(value = ["user_uid", "date_key"])],
)
data class DailyTaskEntryEntity(
    @ColumnInfo(name = "user_uid") val userUid: String,
    @ColumnInfo(name = "date_key") val dateKey: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    val points: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
