package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.dao.AyatDao
import com.parhar.noor.data.local.mapper.toDomain
import com.parhar.noor.data.local.mapper.toEntity
import com.parhar.noor.data.remote.AyatsRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteAyat
import com.parhar.noor.data.repository.AyatsRepository
import com.parhar.noor.data.repository.LegacyAyatRepository
import com.parhar.noor.domain.model.Ayat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AyatsRepositoryImpl(
    private val ayatsRemote: AyatsRemoteDataSource,
    private val ayatDao: AyatDao,
) : AyatsRepository {

    override fun observeAyats(): Flow<List<Ayat>> = callbackFlow {
        val closeable = ayatsRemote.observeAyats(
            onChanged = { remoteAyats ->
                trySend(remoteAyats.map { it.toDomain() }.sortedBy { it.id })
            },
            onError = { message ->
                close(IllegalStateException(message))
            },
        )
        awaitClose { closeable.close() }
    }

    override suspend fun fetchAyats(): List<Ayat> {
        return ayatsRemote.fetchAyats()
            .map { it.toDomain() }
            .sortedBy { it.id }
    }

    override suspend fun getSplashAyats(): List<Ayat> {
        val cachedAyats = ayatDao.getAll().map { it.toDomain() }
        if (cachedAyats.isNotEmpty()) {
            return cachedAyats
        }
        return LegacyAyatRepository.ayats
    }

    override suspend fun syncAyatsFromRemote() {
        val remoteAyats = runCatching { fetchAyats() }.getOrDefault(emptyList())
        if (remoteAyats.isEmpty()) return
        ayatDao.deleteAll()
        ayatDao.insertAll(remoteAyats.map { it.toEntity() })
    }

    override suspend fun addAyat(
        ayat: String,
        english: String,
        urdu: String,
        reference: String,
    ): String {
        val ayatId = System.currentTimeMillis().toString()
        ayatsRemote.pushAyat(
            RemoteAyat(
                id = ayatId,
                ayat = ayat.trim(),
                english = english.trim(),
                urdu = urdu.trim(),
                reference = reference.trim(),
            ),
        )
        return ayatId
    }

    override suspend fun updateAyat(
        ayatId: String,
        ayat: String,
        english: String,
        urdu: String,
        reference: String,
    ) {
        ayatsRemote.pushAyat(
            RemoteAyat(
                id = ayatId,
                ayat = ayat.trim(),
                english = english.trim(),
                urdu = urdu.trim(),
                reference = reference.trim(),
            ),
        )
    }

    override suspend fun deleteAyat(ayatId: String) {
        ayatsRemote.deleteAyat(ayatId)
    }

    private fun RemoteAyat.toDomain(): Ayat = Ayat(
        id = id,
        ayat = ayat,
        english = english,
        urdu = urdu,
        reference = reference,
    )
}
