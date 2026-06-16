package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toEntity
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteUserProfile
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepositoryImpl(
    private val userDao: UserDao,
    private val userRemote: UserRemoteDataSource,
    private val syncCoordinator: SyncCoordinator,
    private val connectivityMonitor: ConnectivityMonitor,
) : UserProfileRepository {

    override fun observeUser(uid: String): Flow<UserProfile?> {
        return userDao.observeUser(uid).map { entity -> entity?.toDomain() }
    }

    override suspend fun getUser(uid: String): UserProfile? {
        return userDao.getUser(uid)?.toDomain()
    }

    override suspend fun saveProfile(profile: UserProfile) {
        val now = System.currentTimeMillis()
        userDao.upsert(profile.toEntity(now, SyncStatus.PENDING_PUSH))
        syncCoordinator.enqueueOutbox(
            opType = SyncOpType.SAVE_PROFILE,
            entityId = profile.uid,
            payload = RemoteUserProfile(
                uid = profile.uid,
                email = profile.email,
                name = profile.name,
                gender = profile.gender,
            ),
        )
    }

    override suspend fun userExists(uid: String): Boolean {
        val local = userDao.getUser(uid)
        if (local != null) return true
        if (!connectivityMonitor.checkOnline()) return false
        return userRemote.userExists(uid)
    }
}
