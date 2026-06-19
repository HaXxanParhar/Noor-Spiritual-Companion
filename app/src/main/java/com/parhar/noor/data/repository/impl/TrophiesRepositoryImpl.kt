package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.remote.TrophiesRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteTrophy
import com.parhar.noor.data.repository.TrophiesRepository
import com.parhar.noor.domain.model.Trophy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TrophiesRepositoryImpl(
    private val trophiesRemote: TrophiesRemoteDataSource,
) : TrophiesRepository {

    override fun observeTrophies(): Flow<List<Trophy>> = callbackFlow {
        val closeable = trophiesRemote.observeTrophies(
            onChanged = { remoteTrophies ->
                val trophies = remoteTrophies
                    .map { it.toDomain() }
                    .sortedByDescending { it.requirement }
                trySend(trophies)
            },
            onError = { message ->
                close(IllegalStateException(message))
            },
        )
        awaitClose { closeable.close() }
    }

    override suspend fun addTrophy(name: String, icon: String, requirement: Int): String {
        val trophyId = System.currentTimeMillis().toString()
        trophiesRemote.pushTrophy(
            RemoteTrophy(
                id = trophyId,
                name = name.trim(),
                icon = icon,
                requirement = requirement,
            ),
        )
        return trophyId
    }

    override suspend fun updateTrophy(trophyId: String, name: String, icon: String, requirement: Int) {
        trophiesRemote.pushTrophy(
            RemoteTrophy(
                id = trophyId,
                name = name.trim(),
                icon = icon,
                requirement = requirement,
            ),
        )
    }

    override suspend fun deleteTrophy(trophyId: String) {
        trophiesRemote.deleteTrophy(trophyId)
    }

    private fun RemoteTrophy.toDomain(): Trophy = Trophy(
        id = id,
        name = name,
        icon = icon,
        requirement = requirement,
    )
}
