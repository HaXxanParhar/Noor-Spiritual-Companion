package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.Trophy
import kotlinx.coroutines.flow.Flow

interface TrophiesRepository {
    fun observeTrophies(): Flow<List<Trophy>>
    suspend fun addTrophy(name: String, icon: String, requirement: Int): String
    suspend fun updateTrophy(trophyId: String, name: String, icon: String, requirement: Int)
    suspend fun deleteTrophy(trophyId: String)
}
