package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.FriendDao
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.entity.FriendEntity
import com.parhar.noor.data.local.mapper.EntityMappers.toEntity
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.AddFriendPayload
import com.parhar.noor.data.remote.dto.RemoteUserProfile
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.data.sync.SyncCoordinator
import kotlinx.coroutines.flow.Flow

class FriendsRepositoryImpl(
    private val friendDao: FriendDao,
    private val userDao: UserDao,
    private val userRemote: UserRemoteDataSource,
    private val syncCoordinator: SyncCoordinator,
    private val connectivityMonitor: ConnectivityMonitor,
) : FriendsRepository {

    override fun observeFriendIds(userUid: String): Flow<List<String>> {
        return friendDao.observeFriendIds(userUid)
    }

    override suspend fun getFriendIds(userUid: String): List<String> {
        return friendDao.getFriendIds(userUid)
    }

    override suspend fun addFriend(currentUid: String, friendUid: String) {
        val now = System.currentTimeMillis()
        friendDao.upsert(
            FriendEntity(
                userUid = currentUid,
                friendUid = friendUid,
                createdAt = now,
                syncStatus = SyncStatus.PENDING_PUSH,
            ),
        )
        if (connectivityMonitor.checkOnline()) {
            runCatching {
                userRemote.fetchUserProfile(friendUid)
            }.getOrNull()?.let { profile ->
                userDao.upsert(
                    com.parhar.noor.domain.model.UserProfile(
                        uid = profile.uid,
                        email = profile.email,
                        name = profile.name,
                        gender = profile.gender,
                    ).toEntity(now, SyncStatus.SYNCED),
                )
            }
        }
        syncCoordinator.enqueueOutbox(
            opType = SyncOpType.ADD_FRIEND,
            entityId = "$currentUid|$friendUid",
            payload = AddFriendPayload(currentUid = currentUid, friendUid = friendUid),
        )
    }
}
