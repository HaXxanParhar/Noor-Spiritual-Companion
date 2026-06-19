package com.parhar.noor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_uid")
    val userUid: String,
    @ColumnInfo(name = "primary_task_ids") val primaryTaskIds: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
