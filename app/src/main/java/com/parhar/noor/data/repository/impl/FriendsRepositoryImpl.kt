package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.FriendDao
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.entity.FriendEntity
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toEntity
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.AddFriendPayload
import com.parhar.noor.data.remote.dto.RemoteUserProfile
import com.parhar.noor.data.repository.RemindResult
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.repository.RemindersRepository
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.Flow

class FriendsRepositoryImpl(
    private val friendDao: FriendDao,
    private val userDao: UserDao,
    private val userRemote: UserRemoteDataSource,
    private val syncCoordinator: SyncCoordinator,
    private val connectivityMonitor: ConnectivityMonitor,
    private val sessionManager: SessionManager,
    private val remindersRepository: RemindersRepository,
) : FriendsRepository {

    override fun observeFriendIds(userUid: String): Flow<List<String>> {
        return friendDao.observeFriendIds(userUid)
    }

    override suspend fun getFriendIds(userUid: String): List<String> {
        return friendDao.getFriendIds(userUid)
    }

    override suspend fun isFriend(currentUid: String, friendUid: String): Boolean {
        return friendDao.getFriendIds(currentUid).contains(friendUid)
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
                userDao.upsert(profile.toDomain().toEntity(now, SyncStatus.SYNCED))
            }
        }
        syncCoordinator.enqueueOutbox(
            opType = SyncOpType.ADD_FRIEND,
            entityId = "$currentUid|$friendUid",
            payload = AddFriendPayload(currentUid = currentUid, friendUid = friendUid),
        )
        refreshCachedFriendCount(currentUid)
    }

    override suspend fun removeFriend(currentUid: String, friendUid: String) {
        friendDao.delete(currentUid, friendUid)
        friendDao.delete(friendUid, currentUid)
        if (connectivityMonitor.checkOnline()) {
            runCatching {
                userRemote.removeFriendship(currentUid, friendUid)
            }
        }
        refreshCachedFriendCount(currentUid)
    }

    private suspend fun refreshCachedFriendCount(userUid: String) {
        val count = friendDao.getFriendIds(userUid).size
        sessionManager.saveFriendCount(count)
        if (count > 0) {
            syncCoordinator.startLeaderboardSync(userUid)
        }
    }

    override suspend fun sendRemind(
        currentUid: String,
        friendUid: String,
        senderName: String,
        message: String,
    ): RemindResult {
        return remindersRepository.sendReminder(
            targetUid = friendUid,
            senderUid = currentUid,
            senderName = senderName,
            message = message,
            isOnline = connectivityMonitor.checkOnline(),
        )
    }

    override suspend fun canSendReminder(currentUid: String, friendUid: String): Boolean {
        return remindersRepository.canSendReminderTo(friendUid, currentUid)
    }
}
