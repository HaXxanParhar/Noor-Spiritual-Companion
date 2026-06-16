package com.parhar.noor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.parhar.noor.data.local.SyncStatus

@Entity(
    tableName = "friends",
    primaryKeys = ["user_uid", "friend_uid"],
)
data class FriendEntity(
    @ColumnInfo(name = "user_uid") val userUid: String,
    @ColumnInfo(name = "friend_uid") val friendUid: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
