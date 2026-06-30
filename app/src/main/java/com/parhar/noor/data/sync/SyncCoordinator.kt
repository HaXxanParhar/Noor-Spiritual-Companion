package com.parhar.noor.data.sync

import com.google.gson.Gson
import com.parhar.noor.data.local.LocalDataStore
import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.FriendDao
import com.parhar.noor.data.local.dao.SyncOutboxDao
import com.parhar.noor.data.local.dao.TaskDefinitionDao
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.entity.FriendEntity
import com.parhar.noor.data.local.entity.SyncOutboxEntity
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import com.parhar.noor.data.local.entity.UserEntity
import com.parhar.noor.data.local.mapper.DailyTaskSerializer
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toEntity
import com.parhar.noor.data.remote.CatalogRemoteDataSource
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.AddFriendPayload
import com.parhar.noor.data.remote.dto.AddTaskDefPayload
import com.parhar.noor.data.remote.dto.DailyTasksPayload
import com.parhar.noor.data.remote.dto.RemoteCategory
import com.parhar.noor.data.remote.dto.RemoteTaskDefinition
import com.parhar.noor.data.remote.dto.RemoteUserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class SyncCoordinator(
    private val catalogRemote: CatalogRemoteDataSource,
    private val userRemote: UserRemoteDataSource,
    private val localDataStore: LocalDataStore,
    private val userDao: UserDao,
    private val friendDao: FriendDao,
    private val taskDefinitionDao: TaskDefinitionDao,
    private val dailyTaskEntryDao: DailyTaskEntryDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val gson: Gson = Gson(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = CopyOnWriteArrayList<AutoCloseable>()
    private val friendTaskListeners = ConcurrentHashMap<String, AutoCloseable>()
    private var favoriteListener: AutoCloseable? = null
    private var activeUid: String? = null

    init {
        scope.launch {
            connectivityMonitor.isOnline.collectLatest { online ->
                if (online) {
                    processOutbox()
                }
            }
        }
    }

    suspend fun bootstrapFromRemote(uid: String?): Result<Unit> {
        if (!connectivityMonitor.checkOnline()) {
            return Result.failure(IllegalStateException("No internet connection."))
        }

        return runCatching {
            processOutbox()
            val now = System.currentTimeMillis()

            val categories = catalogRemote.fetchCategories()
            localDataStore.upsertRemoteCategories(categories, now)

            val tasks = catalogRemote.fetchTaskDefinitions()
            localDataStore.upsertRemoteTaskDefinitions(tasks, now)

            userRemote.fetchFavoriteInfo()?.let { fav ->
                localDataStore.upsertRemoteFavorite(fav, now)
            }

            if (!uid.isNullOrBlank()) {
                userRemote.fetchUserProfile(uid)?.let { profile ->
                    localDataStore.forceUpsertRemoteUser(profile, now)
                }
                val history = userRemote.fetchUserTaskHistory(uid)
                localDataStore.forceUpsertRemoteUserTaskHistory(uid, history, now)

                val friends = userRemote.fetchFriendIds(uid)
                localDataStore.upsertRemoteFriends(uid, friends, now)
                friends.forEach { friendUid ->
                    userRemote.fetchUserProfile(friendUid)?.let { profile ->
                        localDataStore.forceUpsertRemoteUser(profile, now)
                    }
                    val friendHistory = userRemote.fetchUserTaskHistory(friendUid)
                    localDataStore.forceUpsertRemoteUserTaskHistory(friendUid, friendHistory, now)
                }
            }
        }
    }

    fun startLeaderboardSync(uid: String) {
        if (activeUid == uid && listeners.isNotEmpty()) return
        stopLeaderboardListeners()
        activeUid = uid
        scope.launch {
            if (connectivityMonitor.checkOnline()) {
                processOutbox()
            }
            startLeaderboardListeners(uid)
        }
    }

    fun startFavoriteSync() {
        if (favoriteListener != null) return
        favoriteListener = userRemote.observeFavoriteInfo(
            onChanged = { info ->
                scope.launch {
                    localDataStore.upsertRemoteFavorite(info, System.currentTimeMillis())
                }
            },
            onError = {},
        )
    }

    fun stopFavoriteSync() {
        stopFavoriteListener()
    }

    fun stopSync() {
        stopLeaderboardListeners()
        stopFavoriteListener()
        activeUid = null
    }

    private fun stopFavoriteListener() {
        favoriteListener?.close()
        favoriteListener = null
    }

    suspend fun processOutbox() {
        if (!connectivityMonitor.checkOnline()) return
        val pending = syncOutboxDao.getAll()
        pending.forEach { entry ->
            runCatching {
                when (entry.opType) {
                    SyncOpType.SAVE_DAILY_TASKS -> pushDailyTasks(entry)
                    SyncOpType.ADD_FRIEND -> pushFriend(entry)
                    SyncOpType.SAVE_PROFILE -> pushProfile(entry)
                    SyncOpType.ADD_TASK_DEF -> pushTaskDef(entry)
                }
                syncOutboxDao.delete(entry.id)
            }.onFailure { error ->
                syncOutboxDao.markFailed(entry.id, error.message.orEmpty())
            }
        }
    }

    suspend fun enqueueOutbox(
        opType: SyncOpType,
        entityId: String,
        payload: Any,
    ) {
        syncOutboxDao.insert(
            SyncOutboxEntity(
                opType = opType,
                entityId = entityId,
                payloadJson = gson.toJson(payload),
                createdAt = System.currentTimeMillis(),
            ),
        )
        if (connectivityMonitor.checkOnline()) {
            processOutbox()
        }
    }

    suspend fun pushTaskDefinitionNow(
        task: RemoteTaskDefinition,
        originalCategory: String? = null,
    ) {
        if (!connectivityMonitor.checkOnline()) {
            throw IllegalStateException("No internet connection.")
        }
        originalCategory?.let { oldCategory ->
            catalogRemote.deleteTaskDefinition(oldCategory, task.id)
        }
        catalogRemote.pushTaskDefinition(task)
    }

    fun pushTaskPositionUpdatesInBackground(
        updates: List<RemoteTaskDefinition>,
        categoryMove: Pair<RemoteTaskDefinition, String>? = null,
    ) {
        scope.launch {
            runCatching {
                if (!connectivityMonitor.checkOnline()) {
                    throw IllegalStateException("No internet connection.")
                }
                updates.forEach { task ->
                    catalogRemote.pushTaskDefinition(task)
                }
                categoryMove?.let { (task, originalCategory) ->
                    catalogRemote.deleteTaskDefinition(originalCategory, task.id)
                    catalogRemote.pushTaskDefinition(task)
                }
            }
        }
    }

    suspend fun pushCategoryNow(category: RemoteCategory) {
        if (!connectivityMonitor.checkOnline()) {
            throw IllegalStateException("No internet connection.")
        }
        catalogRemote.pushCategory(category)
    }

    fun pushCategoryPositionUpdatesInBackground(categories: List<RemoteCategory>) {
        scope.launch {
            runCatching {
                if (!connectivityMonitor.checkOnline()) {
                    throw IllegalStateException("No internet connection.")
                }
                categories.forEach { category ->
                    catalogRemote.pushCategory(category)
                }
            }
        }
    }

    suspend fun deleteTasksForCategoryNow(categoryName: String) {
        if (!connectivityMonitor.checkOnline()) {
            throw IllegalStateException("No internet connection.")
        }
        catalogRemote.deleteTasksForCategory(categoryName)
    }

    suspend fun renameCategoryTasksNow(
        originalCategoryName: String,
        newCategoryName: String,
        tasks: List<RemoteTaskDefinition>,
    ) {
        if (!connectivityMonitor.checkOnline()) {
            throw IllegalStateException("No internet connection.")
        }
        tasks.forEach { task ->
            catalogRemote.pushTaskDefinition(task)
        }
        catalogRemote.deleteTasksForCategory(originalCategoryName)
    }

    suspend fun deleteCategoryWithTasksNow(
        categoryId: String,
        categoryName: String,
        taskIds: List<String>,
    ) {
        if (!connectivityMonitor.checkOnline()) {
            throw IllegalStateException("No internet connection.")
        }
        purgeTaskIdsFromUsers(taskIds)
        catalogRemote.deleteTasksForCategory(categoryName)
        catalogRemote.deleteCategory(categoryId)
    }

    private suspend fun purgeTaskIdsFromUsers(taskIds: List<String>) {
        if (taskIds.isEmpty()) return
        val taskIdSet = taskIds.toSet()
        val now = System.currentTimeMillis()

        val userUids = buildSet {
            dailyTaskEntryDao.getDistinctUserUids().forEach { add(it) }
            userDao.getAll().forEach { add(it.uid) }
        }

        userUids.forEach { userUid ->
            val history = runCatching { userRemote.fetchUserTaskHistory(userUid) }.getOrNull()
                ?: return@forEach
            history.forEach { (dateKey, rawValue) ->
                val originalEntries = DailyTaskSerializer.parse(userUid, dateKey, rawValue, now)
                val entries = originalEntries.filter { entry -> entry.taskId !in taskIdSet }
                if (entries.size != originalEntries.size) {
                    val serialized = DailyTaskSerializer.serialize(entries)
                    userRemote.pushDailyTaskString(userUid, dateKey, serialized)
                    dailyTaskEntryDao.upsertAll(
                        entries.map { entry ->
                            entry.copy(updatedAt = now, syncStatus = SyncStatus.SYNCED)
                        },
                    )
                }
            }
        }

        dailyTaskEntryDao.deleteByTaskIds(taskIds)
    }

    suspend fun deleteTaskDefinitionNow(category: String, taskId: String) {
        if (!connectivityMonitor.checkOnline()) {
            throw IllegalStateException("No internet connection.")
        }
        catalogRemote.deleteTaskDefinition(category, taskId)
    }

    private suspend fun pushDailyTasks(entry: SyncOutboxEntity) {
        val payload = gson.fromJson(entry.payloadJson, DailyTasksPayload::class.java)
        val entries = dailyTaskEntryDao.getForDate(payload.userUid, payload.dateKey)
        val serialized = DailyTaskSerializer.serialize(entries)
        userRemote.pushDailyTaskString(payload.userUid, payload.dateKey, serialized)
        val now = System.currentTimeMillis()
        dailyTaskEntryDao.upsertAll(
            entries.map { item ->
                item.copy(updatedAt = now, syncStatus = SyncStatus.SYNCED)
            },
        )
    }

    private suspend fun pushFriend(entry: SyncOutboxEntity) {
        val payload = gson.fromJson(entry.payloadJson, AddFriendPayload::class.java)
        userRemote.pushFriendship(payload.currentUid, payload.friendUid)
        val now = System.currentTimeMillis()
        friendDao.upsert(
            FriendEntity(
                userUid = payload.currentUid,
                friendUid = payload.friendUid,
                createdAt = now,
                syncStatus = SyncStatus.SYNCED,
            ),
        )
        friendDao.upsert(
            FriendEntity(
                userUid = payload.friendUid,
                friendUid = payload.currentUid,
                createdAt = now,
                syncStatus = SyncStatus.SYNCED,
            ),
        )
    }

    private suspend fun pushProfile(entry: SyncOutboxEntity) {
        val profile = gson.fromJson(entry.payloadJson, RemoteUserProfile::class.java)
        userRemote.pushUserProfile(profile)
        userDao.upsert(
            profile.toDomain().toEntity(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED,
            ),
        )
    }

    private suspend fun pushTaskDef(entry: SyncOutboxEntity) {
        val payload = gson.fromJson(entry.payloadJson, AddTaskDefPayload::class.java)
        val remote = RemoteTaskDefinition(
            id = payload.taskId,
            category = payload.category,
            name = payload.name,
            points = payload.points,
            position = payload.position,
            emoji = payload.emoji,
            shortDescription = payload.shortDescription,
            detailedDescription = payload.detailedDescription,
            arabic = payload.arabic,
            visible = payload.visible,
        )
        catalogRemote.pushTaskDefinition(remote)
        taskDefinitionDao.upsert(
            TaskDefinitionEntity(
                id = payload.taskId,
                category = payload.category,
                name = payload.name,
                points = payload.points,
                position = payload.position,
                emoji = payload.emoji,
                shortDescription = payload.shortDescription,
                detailedDescription = payload.detailedDescription,
                arabic = payload.arabic,
                visible = payload.visible,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED,
            ),
        )
    }

    private fun startLeaderboardListeners(uid: String) {
        val nowProvider = { System.currentTimeMillis() }

        listeners.add(
            userRemote.observeFriends(
                uid = uid,
                onChanged = { friendIds ->
                    scope.launch {
                        localDataStore.upsertRemoteFriends(uid, friendIds, nowProvider())
                        syncFriendTaskListeners(friendIds)
                        if (connectivityMonitor.checkOnline()) {
                            friendIds.forEach { friendUid ->
                                userRemote.fetchUserProfile(friendUid)?.let { profile ->
                                    localDataStore.forceUpsertRemoteUser(profile, nowProvider())
                                }
                            }
                        }
                    }
                },
                onError = {},
            ),
        )

        scope.launch {
            val friendIds = userRemote.fetchFriendIds(uid)
            syncFriendTaskListeners(friendIds)
        }
    }

    private fun syncFriendTaskListeners(friendIds: List<String>) {
        val activeIds = friendIds.toSet()
        friendTaskListeners.keys.filter { it !in activeIds }.forEach { friendUid ->
            friendTaskListeners.remove(friendUid)?.close()
        }

        friendIds.forEach { friendUid ->
            if (friendTaskListeners.containsKey(friendUid)) return@forEach
            friendTaskListeners[friendUid] = userRemote.observeUserTaskHistory(
                uid = friendUid,
                onChanged = { history ->
                    scope.launch {
                        localDataStore.upsertRemoteUserTaskHistory(
                            userUid = friendUid,
                            history = history,
                            remoteUpdatedAt = System.currentTimeMillis(),
                        )
                    }
                },
                onError = {},
            )
        }
    }

    private fun stopLeaderboardListeners() {
        listeners.forEach { runCatching { it.close() } }
        listeners.clear()
        friendTaskListeners.values.forEach { runCatching { it.close() } }
        friendTaskListeners.clear()
    }
}
