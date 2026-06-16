package com.parhar.noor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.parhar.noor.data.local.SyncStatus

@Entity(
    tableName = "categories",
    indices = [Index(value = ["category_key"])],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category_key")     val categoryKey: String,
    @ColumnInfo(name = "category_name") val categoryName: String,
    val title: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
