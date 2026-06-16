package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {

    @Query("SELECT * FROM user_preferences WHERE user_uid = :userUid LIMIT 1")
    fun observe(userUid: String): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE user_uid = :userUid LIMIT 1")
    suspend fun get(userUid: String): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preferences: UserPreferencesEntity)
}
