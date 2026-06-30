package com.parhar.noor.data.remote

import com.parhar.noor.data.remote.dto.RemoteAyat

interface AyatsRemoteDataSource {
    suspend fun fetchAyats(): List<RemoteAyat>
    suspend fun pushAyat(ayat: RemoteAyat)
    suspend fun deleteAyat(ayatId: String)
    fun observeAyats(
        onChanged: (List<RemoteAyat>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable
}
