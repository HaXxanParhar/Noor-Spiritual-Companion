package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.DailyTaskState
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.domain.model.UserTaskStats
import kotlinx.coroutines.flow.Flow

interface UserTaskRepository {
    fun observeHomeSections(): Flow<List<HomeTaskSection>>
    fun observeDailyTaskState(userUid: String, dateKey: String): Flow<DailyTaskState>
    fun observeTaskStats(userUid: String, dateKey: String): Flow<UserTaskStats>
    suspend fun toggleTask(userUid: String, dateKey: String, taskId: String, points: Int, checked: Boolean)
    suspend fun toggleCannotOfferForDate(
        userUid: String,
        dateKey: String,
        checked: Boolean,
        primaryTasks: List<TaskItem>,
    )
    suspend fun bulkSetPrimaryPrayersChecked(
        userUid: String,
        dateKey: String,
        tasks: List<TaskItem>,
        checked: Boolean,
        enqueueSync: Boolean = true,
    ): Boolean
    suspend fun savePrimaryTaskIds(userUid: String, taskIds: Set<String>)
    suspend fun getPrimaryTaskIds(userUid: String): Set<String>
    suspend fun refreshDailyTasksFromRemote(userUid: String, dateKey: String)
}
