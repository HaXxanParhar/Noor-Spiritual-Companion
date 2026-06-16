package com.parhar.noor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_banner")
data class FavoriteBannerEntity(
    @PrimaryKey val id: Int = 1,
    val email: String = "",
    val emoji: String = "",
    val message: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
