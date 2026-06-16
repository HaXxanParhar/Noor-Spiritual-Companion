package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.CategoryDao
import com.parhar.noor.data.local.dao.TaskDefinitionDao
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskItem
import com.parhar.noor.data.repository.CatalogRepository
import com.parhar.noor.data.remote.dto.AddTaskDefPayload
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.domain.model.Category
import com.parhar.noor.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDefinitionDao: TaskDefinitionDao,
    private val syncCoordinator: SyncCoordinator,
) : CatalogRepository {

    override fun observeCategories(): Flow<List<Category>> {
        return categoryDao.observeAll().map { categories ->
            categories.map { it.toDomain() }
        }
    }

    override fun observeTaskItems(): Flow<List<TaskItem>> {
        return taskDefinitionDao.observeAll().map { tasks ->
            tasks.map { it.toTaskItem() }
        }
    }

    override suspend fun getCategories(): List<Category> {
        return categoryDao.getAll().map { it.toDomain() }
    }

    override suspend fun addTask(category: String, name: String, points: Int): String {
        val taskId = System.currentTimeMillis().toString()
        val now = System.currentTimeMillis()
        taskDefinitionDao.upsert(
            TaskDefinitionEntity(
                id = taskId,
                category = category,
                name = name,
                points = points,
                sortOrder = taskId.toLongOrNull() ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_PUSH,
            ),
        )
        syncCoordinator.enqueueOutbox(
            opType = SyncOpType.ADD_TASK_DEF,
            entityId = taskId,
            payload = AddTaskDefPayload(
                taskId = taskId,
                category = category,
                name = name,
                points = points,
            ),
        )
        return taskId
    }
}
