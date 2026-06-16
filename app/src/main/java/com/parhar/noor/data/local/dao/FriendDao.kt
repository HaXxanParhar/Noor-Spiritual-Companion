package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT friend_uid FROM friends WHERE user_uid = :userUid")
    fun observeFriendIds(userUid: String): Flow<List<String>>

    @Query("SELECT friend_uid FROM friends WHERE user_uid = :userUid")
    suspend fun getFriendIds(userUid: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(friends: List<FriendEntity>)

    @Query("SELECT * FROM friends WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<FriendEntity>

    @Query("SELECT COUNT(*) FROM friends WHERE user_uid = :userUid AND friend_uid = :friendUid")
    suspend fun exists(userUid: String, friendUid: String): Int
}
