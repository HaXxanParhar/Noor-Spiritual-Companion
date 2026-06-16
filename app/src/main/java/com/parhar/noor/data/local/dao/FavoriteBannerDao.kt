package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.entity.FavoriteBannerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteBannerDao {

    @Query("SELECT * FROM favorite_banner WHERE id = 1 LIMIT 1")
    fun observeBanner(): Flow<FavoriteBannerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(banner: FavoriteBannerEntity)
}
