package com.parhar.noor.data.local

import com.parhar.noor.data.local.dao.CategoryDao
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.FavoriteBannerDao
import com.parhar.noor.data.local.dao.FriendDao
import com.parhar.noor.data.local.dao.SyncMetadataDao
import com.parhar.noor.data.local.dao.TaskDefinitionDao
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.entity.CategoryEntity
import com.parhar.noor.data.local.entity.DailyTaskEntryEntity
import com.parhar.noor.data.local.entity.FavoriteBannerEntity
import com.parhar.noor.data.local.entity.FriendEntity
import com.parhar.noor.data.local.entity.SyncMetadataEntity
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import com.parhar.noor.data.local.entity.UserEntity
import com.parhar.noor.data.local.mapper.DailyTaskSerializer
import com.parhar.noor.data.remote.dto.RemoteCategory
import com.parhar.noor.data.remote.dto.RemoteFavoriteInfo
import com.parhar.noor.data.remote.dto.RemoteTaskDefinition
import com.parhar.noor.data.remote.dto.RemoteUserProfile

class LocalDataStore(
    private val userDao: UserDao,
    private val friendDao: FriendDao,
    private val categoryDao: CategoryDao,
    private val taskDefinitionDao: TaskDefinitionDao,
    private val dailyTaskEntryDao: DailyTaskEntryDao,
    private val favoriteBannerDao: FavoriteBannerDao,
    private val syncMetadataDao: SyncMetadataDao,
) {

    suspend fun upsertRemoteUser(profile: RemoteUserProfile, remoteUpdatedAt: Long) {
        val existing = userDao.getUser(profile.uid)
        if (shouldSkipRemoteOverwrite(existing?.syncStatus, existing?.updatedAt, remoteUpdatedAt)) {
            return
        }
        userDao.upsert(profile.toEntity(remoteUpdatedAt))
    }

    suspend fun forceUpsertRemoteUser(profile: RemoteUserProfile, remoteUpdatedAt: Long) {
        userDao.upsert(profile.toEntity(remoteUpdatedAt))
    }

    suspend fun upsertRemoteCategories(categories: List<RemoteCategory>, remoteUpdatedAt: Long) {
        categoryDao.upsertAll(
            categories.map { category ->
                CategoryEntity(
                    id = category.id,
                    categoryKey = category.category.lowercase(),
                    categoryName = category.category,
                    title = category.title.ifBlank { category.category },
                    updatedAt = remoteUpdatedAt,
                    syncStatus = SyncStatus.SYNCED,
                )
            },
        )
        syncMetadataDao.upsert(SyncMetadataEntity("categories", remoteUpdatedAt))
    }

    suspend fun upsertRemoteTaskDefinitions(tasks: List<RemoteTaskDefinition>, remoteUpdatedAt: Long) {
        taskDefinitionDao.upsertAll(
            tasks.map { task ->
                TaskDefinitionEntity(
                    id = task.id,
                    category = task.category,
                    name = task.name,
                    points = task.points,
                    sortOrder = task.id.toLongOrNull() ?: Long.MAX_VALUE,
                    updatedAt = remoteUpdatedAt,
                    syncStatus = SyncStatus.SYNCED,
                )
            },
        )
        syncMetadataDao.upsert(SyncMetadataEntity("tasks_catalog", remoteUpdatedAt))
    }

    suspend fun upsertRemoteDailyTasks(
        userUid: String,
        dateKey: String,
        rawValue: String?,
        remoteUpdatedAt: Long,
    ) {
        val pending = dailyTaskEntryDao.getPendingForDate(userUid, dateKey)
        if (pending.isNotEmpty()) {
            val pendingUpdatedAt = pending.maxOf { it.updatedAt }
            if (pendingUpdatedAt >= remoteUpdatedAt) return
        }

        val existing = dailyTaskEntryDao.getForDate(userUid, dateKey)
        val existingUpdatedAt = existing.maxOfOrNull { it.updatedAt } ?: 0L
        if (existing.isNotEmpty() && existingUpdatedAt > remoteUpdatedAt) return

        replaceDailyTasks(userUid, dateKey, rawValue, remoteUpdatedAt)
    }

    suspend fun forceUpsertRemoteDailyTasks(
        userUid: String,
        dateKey: String,
        rawValue: String?,
        remoteUpdatedAt: Long,
    ) {
        replaceDailyTasks(userUid, dateKey, rawValue, remoteUpdatedAt)
    }

    suspend fun upsertRemoteUserTaskHistory(
        userUid: String,
        history: Map<String, String>,
        remoteUpdatedAt: Long,
    ) {
        history.forEach { (dateKey, rawValue) ->
            upsertRemoteDailyTasks(userUid, dateKey, rawValue, remoteUpdatedAt)
        }
    }

    suspend fun forceUpsertRemoteUserTaskHistory(
        userUid: String,
        history: Map<String, String>,
        remoteUpdatedAt: Long,
    ) {
        history.forEach { (dateKey, rawValue) ->
            forceUpsertRemoteDailyTasks(userUid, dateKey, rawValue, remoteUpdatedAt)
        }
    }

    suspend fun upsertRemoteFriends(userUid: String, friendIds: List<String>, remoteUpdatedAt: Long) {
        friendDao.upsertAll(
            friendIds.map { friendUid ->
                FriendEntity(
                    userUid = userUid,
                    friendUid = friendUid,
                    createdAt = remoteUpdatedAt,
                    syncStatus = SyncStatus.SYNCED,
                )
            },
        )
        syncMetadataDao.upsert(SyncMetadataEntity("friends_$userUid", remoteUpdatedAt))
    }

    suspend fun upsertRemoteFavorite(info: RemoteFavoriteInfo?, remoteUpdatedAt: Long) {
        if (info == null) return
        favoriteBannerDao.upsert(
            FavoriteBannerEntity(
                email = info.email,
                emoji = info.emoji,
                message = info.top,
                updatedAt = remoteUpdatedAt,
            ),
        )
        syncMetadataDao.upsert(SyncMetadataEntity("favorite_banner", remoteUpdatedAt))
    }

    private suspend fun replaceDailyTasks(
        userUid: String,
        dateKey: String,
        rawValue: String?,
        remoteUpdatedAt: Long,
    ) {
        dailyTaskEntryDao.deleteForDate(userUid, dateKey)
        val entries = DailyTaskSerializer.parse(userUid, dateKey, rawValue, remoteUpdatedAt)
        if (entries.isNotEmpty()) {
            dailyTaskEntryDao.upsertAll(entries)
        }
    }

    private fun RemoteUserProfile.toEntity(remoteUpdatedAt: Long) = UserEntity(
        uid = uid,
        email = email,
        name = name,
        gender = gender,
        updatedAt = remoteUpdatedAt,
        syncStatus = SyncStatus.SYNCED,
    )

    private fun shouldSkipRemoteOverwrite(
        localStatus: SyncStatus?,
        localUpdatedAt: Long?,
        remoteUpdatedAt: Long,
    ): Boolean {
        if (localStatus == SyncStatus.PENDING_PUSH) {
            return (localUpdatedAt ?: 0L) >= remoteUpdatedAt
        }
        return false
    }
}
