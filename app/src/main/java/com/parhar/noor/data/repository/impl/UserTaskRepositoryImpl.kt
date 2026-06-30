package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.CategoryDao
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.TaskDefinitionDao
import com.parhar.noor.data.local.dao.UserPreferencesDao
import com.parhar.noor.data.local.entity.DailyTaskEntryEntity
import com.parhar.noor.data.local.entity.UserPreferencesEntity
import com.parhar.noor.data.local.mapper.DailyTaskKeys
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskHistory
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskItem
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskPointsMap
import com.parhar.noor.data.remote.dto.DailyTasksPayload
import com.parhar.noor.data.repository.UserTaskRepository
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.domain.model.DailyTaskState
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.domain.model.UserTaskStats
import com.parhar.noor.domain.usecase.CatalogSectionBuilder
import com.parhar.noor.domain.usecase.TaskStatsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class UserTaskRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDefinitionDao: TaskDefinitionDao,
    private val dailyTaskEntryDao: DailyTaskEntryDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val syncCoordinator: SyncCoordinator,
    private val taskStatsUseCase: TaskStatsUseCase,
    private val userRemote: com.parhar.noor.data.remote.UserRemoteDataSource,
    private val localDataStore: com.parhar.noor.data.local.LocalDataStore,
) : UserTaskRepository {

    override fun observeHomeSections(): Flow<List<HomeTaskSection>> {
        return combine(
            categoryDao.observeAll(),
            taskDefinitionDao.observeAll(),
        ) { categories, tasks ->
            CatalogSectionBuilder.buildSections(
                categories = categories.map { it.toDomain() },
                tasks = tasks.map { it.toDomain() },
                includeEmptyCategories = false,
                includeHiddenTasks = false,
            )
        }
    }

    override fun observeDailyTaskState(userUid: String, dateKey: String): Flow<DailyTaskState> {
        return dailyTaskEntryDao.observeForDate(userUid, dateKey).map { entries ->
            val taskPoints = entries.toTaskPointsMap()
            DailyTaskState(
                taskPoints = taskPoints,
                checkedTaskIds = taskPoints
                    .filter { (taskId, points) ->
                        points > 0 && !DailyTaskKeys.isCannotOfferKey(taskId)
                    }
                    .keys,
            )
        }
    }

    override fun observeTaskStats(userUid: String, dateKey: String): Flow<UserTaskStats> {
        return combine(
            dailyTaskEntryDao.observeForUser(userUid),
            dailyTaskEntryDao.observeForDate(userUid, dateKey),
            taskDefinitionDao.observeAll(),
            userPreferencesDao.observe(userUid),
        ) { historyEntries, todayEntries, taskDefinitions, preferences ->
            val history = historyEntries.toTaskHistory()
            val todayPoints = todayEntries.toTaskPointsMap()
            val checkedIds = todayPoints
                .filter { (taskId, points) -> points > 0 && !DailyTaskKeys.isCannotOfferKey(taskId) }
                .keys
            val primaryIds = preferences?.primaryTaskIds
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                .orEmpty()
            val pointLookup = taskDefinitions.associate { it.id to it.points }
            taskStatsUseCase.calculateStats(
                taskHistory = history,
                primaryTaskIds = primaryIds,
                todayKey = dateKey,
                todayTaskPoints = todayPoints,
                checkedTaskIds = checkedIds,
                taskPointLookup = pointLookup,
            )
        }
    }

    override suspend fun toggleCannotOfferForDate(
        userUid: String,
        dateKey: String,
        checked: Boolean,
        primaryTasks: List<TaskItem>,
    ) {
        val now = System.currentTimeMillis()
        var changed = false

        if (checked) {
            val existingCannotOffer = dailyTaskEntryDao.getForDate(userUid, dateKey)
                .firstOrNull { DailyTaskKeys.isCannotOfferKey(it.taskId) }
            if (existingCannotOffer?.points != 1) {
                dailyTaskEntryDao.upsert(
                    DailyTaskEntryEntity(
                        userUid = userUid,
                        dateKey = dateKey,
                        taskId = DailyTaskKeys.CANNOT_OFFER_TASK_ID,
                        points = 1,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING_PUSH,
                    ),
                )
                changed = true
            }
            changed = bulkSetPrimaryPrayersChecked(
                userUid = userUid,
                dateKey = dateKey,
                tasks = primaryTasks,
                checked = true,
                enqueueSync = false,
            ) || changed
        } else {
            val hadCannotOffer = dailyTaskEntryDao.getForDate(userUid, dateKey)
                .any { DailyTaskKeys.isCannotOfferKey(it.taskId) }
            if (hadCannotOffer) {
                dailyTaskEntryDao.deleteEntry(
                    userUid = userUid,
                    dateKey = dateKey,
                    taskId = DailyTaskKeys.CANNOT_OFFER_TASK_ID,
                )
                changed = true
            }
            changed = bulkSetPrimaryPrayersChecked(
                userUid = userUid,
                dateKey = dateKey,
                tasks = primaryTasks,
                checked = false,
                enqueueSync = false,
            ) || changed
        }

        if (changed) {
            enqueueDailyTasksSync(userUid, dateKey)
        }
    }

    override suspend fun bulkSetPrimaryPrayersChecked(
        userUid: String,
        dateKey: String,
        tasks: List<TaskItem>,
        checked: Boolean,
        enqueueSync: Boolean,
    ): Boolean {
        if (tasks.isEmpty()) return false

        val now = System.currentTimeMillis()
        val existingEntries = dailyTaskEntryDao.getForDate(userUid, dateKey).associateBy { it.taskId }
        var changed = false

        tasks.forEach { taskItem ->
            val targetPoints = if (checked) taskItem.task.points.coerceAtLeast(0) else 0
            val currentPoints = existingEntries[taskItem.id]?.points ?: 0
            if (currentPoints == targetPoints) return@forEach

            changed = true
            dailyTaskEntryDao.upsert(
                DailyTaskEntryEntity(
                    userUid = userUid,
                    dateKey = dateKey,
                    taskId = taskItem.id,
                    points = targetPoints,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING_PUSH,
                ),
            )
        }

        if (changed && enqueueSync) {
            enqueueDailyTasksSync(userUid, dateKey)
        }
        return changed
    }

    override suspend fun toggleTask(
        userUid: String,
        dateKey: String,
        taskId: String,
        points: Int,
        checked: Boolean,
    ) {
        val now = System.currentTimeMillis()
        dailyTaskEntryDao.upsert(
            DailyTaskEntryEntity(
                userUid = userUid,
                dateKey = dateKey,
                taskId = taskId,
                points = if (checked) points.coerceAtLeast(0) else 0,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_PUSH,
            ),
        )
        enqueueDailyTasksSync(userUid, dateKey)
    }

    override suspend fun savePrimaryTaskIds(userUid: String, taskIds: Set<String>) {
        userPreferencesDao.upsert(
            UserPreferencesEntity(
                userUid = userUid,
                primaryTaskIds = taskIds.joinToString(","),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun getPrimaryTaskIds(userUid: String): Set<String> {
        return userPreferencesDao.get(userUid)
            ?.primaryTaskIds
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
    }

    override suspend fun refreshDailyTasksFromRemote(userUid: String, dateKey: String) {
        val raw = userRemote.fetchDailyTaskString(userUid, dateKey)
        localDataStore.forceUpsertRemoteDailyTasks(
            userUid = userUid,
            dateKey = dateKey,
            rawValue = raw,
            remoteUpdatedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun enqueueDailyTasksSync(userUid: String, dateKey: String) {
        syncCoordinator.enqueueOutbox(
            opType = SyncOpType.SAVE_DAILY_TASKS,
            entityId = "$userUid|$dateKey",
            payload = DailyTasksPayload(userUid = userUid, dateKey = dateKey),
        )
    }
}