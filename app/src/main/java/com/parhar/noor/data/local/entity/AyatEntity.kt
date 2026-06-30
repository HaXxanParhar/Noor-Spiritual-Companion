package com.parhar.noor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_ayats")
data class AyatEntity(
    @PrimaryKey val id: String,
    val ayat: String,
    val english: String,
    val urdu: String,
    val reference: String,
)
