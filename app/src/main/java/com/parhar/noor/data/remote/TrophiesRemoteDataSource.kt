package com.parhar.noor.data.remote

import com.parhar.noor.data.remote.dto.RemoteTrophy

interface TrophiesRemoteDataSource {
    suspend fun fetchTrophies(): List<RemoteTrophy>
    suspend fun pushTrophy(trophy: RemoteTrophy)
    suspend fun deleteTrophy(trophyId: String)
    fun observeTrophies(
        onChanged: (List<RemoteTrophy>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable
}
