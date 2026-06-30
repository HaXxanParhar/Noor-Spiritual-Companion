package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.entity.AyatEntity

@Dao
interface AyatDao {

    @Query("SELECT * FROM cached_ayats ORDER BY id ASC")
    suspend fun getAll(): List<AyatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ayats: List<AyatEntity>)

    @Query("DELETE FROM cached_ayats")
    suspend fun deleteAll()
}
