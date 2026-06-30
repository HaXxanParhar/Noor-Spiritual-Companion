package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.Ayat
import kotlinx.coroutines.flow.Flow

interface AyatsRepository {
    fun observeAyats(): Flow<List<Ayat>>
    suspend fun fetchAyats(): List<Ayat>
    suspend fun getSplashAyats(): List<Ayat>
    suspend fun syncAyatsFromRemote()
    suspend fun addAyat(ayat: String, english: String, urdu: String, reference: String): String
    suspend fun updateAyat(ayatId: String, ayat: String, english: String, urdu: String, reference: String)
    suspend fun deleteAyat(ayatId: String)
}
