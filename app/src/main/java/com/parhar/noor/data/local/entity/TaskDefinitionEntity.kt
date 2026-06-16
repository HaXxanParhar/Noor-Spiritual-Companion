package com.parhar.noor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.parhar.noor.data.local.SyncStatus

@Entity(
    tableName = "task_definitions",
    indices = [Index(value = ["category"])],
)
data class TaskDefinitionEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val points: Int = 0,
    @ColumnInfo(name = "sort_order") val sortOrder: Long = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
