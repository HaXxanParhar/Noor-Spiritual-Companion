package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.CategoryDao
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.TaskDefinitionDao
import com.parhar.noor.data.local.dao.UserPreferencesDao
import com.parhar.noor.data.local.entity.DailyTaskEntryEntity
import com.parhar.noor.data.local.entity.UserPreferencesEntity
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskHistory
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskItem
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskPointsMap
import com.parhar.noor.data.remote.dto.DailyTasksPayload
import com.parhar.noor.data.repository.UserTaskRepository
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.domain.model.DailyTaskState
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.UserTaskStats
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
) : UserTaskRepository {

    override fun observeHomeSections(): Flow<List<HomeTaskSection>> {
        return combine(
            categoryDao.observeAll(),
            taskDefinitionDao.observeAll(),
        ) { categories, tasks ->
            val titleMap = categories.associate { it.categoryKey to it.title }
            val grouped = tasks.groupBy { it.category }
            val ordered = mutableListOf<HomeTaskSection>()
            FIXED_SECTION_ORDER.forEach { sectionName ->
                grouped.entries.firstOrNull { (category, _) ->
                    category.equals(sectionName, ignoreCase = true)
                }?.let { (category, sectionTasks) ->
                    ordered.add(
                        HomeTaskSection(
                            category = category,
                            title = titleMap[category.lowercase()].orEmpty().ifBlank { category },
                            tasks = sectionTasks.map { it.toTaskItem() },
                        ),
                    )
                }
            }
            grouped.filterKeys { category ->
                FIXED_SECTION_ORDER.none { it.equals(category, ignoreCase = true) }
            }.forEach { (category, sectionTasks) ->
                ordered.add(
                    HomeTaskSection(
                        category = category,
                        title = titleMap[category.lowercase()].orEmpty().ifBlank { category },
                        tasks = sectionTasks.map { it.toTaskItem() },
                    ),
                )
            }
            ordered
        }
    }

    override fun observeDailyTaskState(userUid: String, dateKey: String): Flow<DailyTaskState> {
        return dailyTaskEntryDao.observeForDate(userUid, dateKey).map { entries ->
            val taskPoints = entries.toTaskPointsMap()
            DailyTaskState(
                taskPoints = taskPoints,
                checkedTaskIds = taskPoints.filterValues { it > 0 }.keys,
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
            val checkedIds = todayPoints.filterValues { it > 0 }.keys
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
        syncCoordinator.enqueueOutbox(
            opType = SyncOpType.SAVE_DAILY_TASKS,
            entityId = "$userUid|$dateKey",
            payload = DailyTasksPayload(userUid = userUid, dateKey = dateKey),
        )
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

    private companion object {
        private val FIXED_SECTION_ORDER = listOf("Primary", "Secondary", "Bonus", "Event")
    }
}
