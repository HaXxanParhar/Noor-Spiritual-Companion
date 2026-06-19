package com.parhar.noor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.parhar.noor.data.local.SyncStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String = "",
    val name: String = "",
    val gender: String = "",
    @ColumnInfo(name = "avatar_text") val avatarText: String = "",
    @ColumnInfo(name = "avatar_bg") val avatarBg: String = "",
    @ColumnInfo(name = "avatar_border") val avatarBorder: String = "",
    @ColumnInfo(name = "avatar_style") val avatarStyle: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "privacy_tasks_today") val privacyTasksToday: String = "private",
    @ColumnInfo(name = "privacy_tasks_history") val privacyTasksHistory: String = "private",
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
