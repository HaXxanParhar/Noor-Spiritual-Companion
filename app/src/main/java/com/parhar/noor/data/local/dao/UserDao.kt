package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeUser(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid IN (:uids)")
    suspend fun getUsers(uids: List<String>): List<UserEntity>

    @Query("SELECT * FROM users")
    fun observeAll(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<UserEntity>

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserEntity>

    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun delete(uid: String)
}
