package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.dao.CategoryDao
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.TaskDefinitionDao
import com.parhar.noor.data.local.entity.CategoryEntity
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import com.parhar.noor.data.remote.dto.RemoteCategory
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskItem
import com.parhar.noor.data.remote.dto.AddTaskDefPayload
import com.parhar.noor.data.remote.dto.RemoteTaskDefinition
import com.parhar.noor.data.repository.CategoryPositionDirection
import com.parhar.noor.data.repository.CatalogRepository
import com.parhar.noor.data.repository.TaskPositionDirection
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.domain.model.Category
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.domain.usecase.CatalogSectionBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CatalogRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDefinitionDao: TaskDefinitionDao,
    private val dailyTaskEntryDao: DailyTaskEntryDao,
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

    override fun observeTaskSections(includeEmptyCategories: Boolean): Flow<List<HomeTaskSection>> {
        return combine(observeCategories(), observeTaskItems()) { categories, taskItems ->
            CatalogSectionBuilder.buildSections(
                categories = categories,
                tasks = taskItems.map { it.task },
                includeEmptyCategories = includeEmptyCategories,
            )
        }
    }

    override suspend fun getCategories(): List<Category> {
        return categoryDao.getAll().map { it.toDomain() }
    }

    override suspend fun isCategoryNameTaken(categoryName: String, excludeCategoryId: String?): Boolean {
        val normalizedKey = categoryName.trim().lowercase()
        return categoryDao.getAll().any { category ->
            category.categoryKey == normalizedKey && category.id != excludeCategoryId
        }
    }

    override suspend fun addCategory(title: String, categoryName: String, description: String): String {
        val trimmedName = categoryName.trim()
        if (isCategoryNameTaken(trimmedName)) {
            throw IllegalArgumentException("Category name already exists.")
        }
        val categoryId = System.currentTimeMillis().toString()
        val now = System.currentTimeMillis()
        val position = nextCategoryPosition()
        val entity = CategoryEntity(
            id = categoryId,
            categoryKey = trimmedName.lowercase(),
            categoryName = trimmedName,
            title = title,
            description = description,
            position = position,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )
        categoryDao.upsert(entity)
        syncCoordinator.pushCategoryNow(entity.toRemote())
        return categoryId
    }

    override suspend fun updateCategory(
        categoryId: String,
        originalCategoryName: String,
        title: String,
        categoryName: String,
        description: String,
    ) {
        val trimmedName = categoryName.trim()
        if (isCategoryNameTaken(trimmedName, excludeCategoryId = categoryId)) {
            throw IllegalArgumentException("Category name already exists.")
        }
        val existing = categoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("Category not found.")
        val now = System.currentTimeMillis()
        val renamed = originalCategoryName != trimmedName
        val updated = existing.copy(
            categoryKey = trimmedName.lowercase(),
            categoryName = trimmedName,
            title = title,
            description = description,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )
        categoryDao.upsert(updated)
        syncCoordinator.pushCategoryNow(updated.toRemote())

        if (renamed) {
            val tasks = taskDefinitionDao.getByCategory(originalCategoryName)
            if (tasks.isNotEmpty()) {
                val movedTasks = tasks.map { task ->
                    task.copy(
                        category = trimmedName,
                        updatedAt = now,
                        syncStatus = SyncStatus.SYNCED,
                    )
                }
                taskDefinitionDao.upsertAllInTransaction(movedTasks)
                syncCoordinator.renameCategoryTasksNow(
                    originalCategoryName = originalCategoryName,
                    newCategoryName = trimmedName,
                    tasks = movedTasks.map { it.toRemote() },
                )
            } else {
                syncCoordinator.deleteTasksForCategoryNow(originalCategoryName)
            }
        }
    }

    override suspend fun deleteCategory(categoryId: String, categoryName: String) {
        val tasks = taskDefinitionDao.getByCategory(categoryName)
        val taskIds = tasks.map { it.id }
        syncCoordinator.deleteCategoryWithTasksNow(
            categoryId = categoryId,
            categoryName = categoryName,
            taskIds = taskIds,
        )
        if (taskIds.isNotEmpty()) {
            dailyTaskEntryDao.deleteByTaskIds(taskIds)
            taskDefinitionDao.deleteByCategory(categoryName)
        }
        categoryDao.deleteById(categoryId)
    }

    override suspend fun swapCategoryPosition(
        categoryId: String,
        direction: CategoryPositionDirection,
    ) {
        val sorted = categoryDao.getAll().sortedWith(
            compareBy<CategoryEntity> { it.position }.thenBy { it.categoryKey },
        )
        if (sorted.size < 2) return

        val currentIndex = sorted.indexOfFirst { it.id == categoryId }
        if (currentIndex < 0) return

        val neighborIndex = when (direction) {
            CategoryPositionDirection.UP -> currentIndex - 1
            CategoryPositionDirection.DOWN -> currentIndex + 1
        }
        if (neighborIndex !in sorted.indices) return

        val now = System.currentTimeMillis()
        val current = sorted[currentIndex]
        val neighbor = sorted[neighborIndex]
        val updatedCurrent = current.copy(
            position = neighbor.position,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )
        val updatedNeighbor = neighbor.copy(
            position = current.position,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )
        categoryDao.upsertAllInTransaction(listOf(updatedCurrent, updatedNeighbor))
        syncCoordinator.pushCategoryPositionUpdatesInBackground(
            listOf(updatedCurrent.toRemote(), updatedNeighbor.toRemote()),
        )
    }

    override suspend fun addTask(category: String, name: String, points: Int, emoji: String): String {
        val taskId = System.currentTimeMillis().toString()
        val now = System.currentTimeMillis()
        val position = nextPositionInCategory(category)
        taskDefinitionDao.upsert(
            TaskDefinitionEntity(
                id = taskId,
                category = category,
                name = name,
                points = points,
                position = position,
                emoji = emoji,
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
                position = position,
                emoji = emoji,
            ),
        )
        return taskId
    }

    override suspend fun updateTask(
        taskId: String,
        originalCategory: String,
        category: String,
        name: String,
        points: Int,
        emoji: String,
    ) {
        val now = System.currentTimeMillis()
        val existing = taskDefinitionDao.getAll().firstOrNull { it.id == taskId }
        val categoryChanged = !originalCategory.equals(category, ignoreCase = true)
        val position = if (categoryChanged) {
            nextPositionInCategory(category)
        } else {
            existing?.position ?: 0
        }

        syncCoordinator.pushTaskDefinitionNow(
            RemoteTaskDefinition(
                id = taskId,
                category = category,
                name = name,
                points = points,
                position = position,
                emoji = emoji,
            ),
            originalCategory = originalCategory.takeIf { categoryChanged },
        )
        taskDefinitionDao.upsert(
            TaskDefinitionEntity(
                id = taskId,
                category = category,
                name = name,
                points = points,
                position = position,
                emoji = emoji,
                updatedAt = now,
                syncStatus = SyncStatus.SYNCED,
            ),
        )
    }

    override suspend fun deleteTask(taskId: String, category: String) {
        syncCoordinator.deleteTaskDefinitionNow(category, taskId)
        taskDefinitionDao.deleteById(taskId)
    }

    override suspend fun swapTaskPosition(
        taskId: String,
        category: String,
        direction: TaskPositionDirection,
    ) {
        val categories = getCategories().sortedWith(
            compareBy<Category> { it.position }.thenBy { it.category.lowercase() },
        )
        val categoryIndex = categories.indexOfFirst { it.category.equals(category, ignoreCase = true) }
        if (categoryIndex < 0) return

        val tasksInCategory = taskDefinitionDao.getByCategory(category)
        if (tasksInCategory.isEmpty()) return

        val sorted = tasksInCategory.sortedWith(
            compareBy<TaskDefinitionEntity> { it.position }.thenBy { it.id },
        )
        val currentIndex = sorted.indexOfFirst { it.id == taskId }
        if (currentIndex < 0) return

        when (direction) {
            TaskPositionDirection.UP -> {
                if (currentIndex > 0) {
                    swapPositions(sorted[currentIndex], sorted[currentIndex - 1])
                } else if (categoryIndex > 0) {
                    moveTaskToPreviousCategoryBottom(
                        task = sorted[currentIndex],
                        targetCategory = categories[categoryIndex - 1].category,
                    )
                }
            }
            TaskPositionDirection.DOWN -> {
                if (currentIndex < sorted.lastIndex) {
                    swapPositions(sorted[currentIndex], sorted[currentIndex + 1])
                } else if (categoryIndex < categories.lastIndex) {
                    moveTaskToNextCategoryTop(
                        task = sorted[currentIndex],
                        targetCategory = categories[categoryIndex + 1].category,
                    )
                }
            }
        }
    }

    private suspend fun swapPositions(first: TaskDefinitionEntity, second: TaskDefinitionEntity) {
        val now = System.currentTimeMillis()
        val updatedFirst = first.copy(
            position = second.position,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )
        val updatedSecond = second.copy(
            position = first.position,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )

        applyLocalTaskUpdates(listOf(updatedFirst, updatedSecond))
        syncCoordinator.pushTaskPositionUpdatesInBackground(
            updates = listOf(updatedFirst.toRemote(), updatedSecond.toRemote()),
        )
    }

    private suspend fun moveTaskToNextCategoryTop(
        task: TaskDefinitionEntity,
        targetCategory: String,
    ) {
        val now = System.currentTimeMillis()
        val originalCategory = task.category
        val targetTasks = taskDefinitionDao.getByCategory(targetCategory)
            .sortedWith(compareBy<TaskDefinitionEntity> { it.position }.thenBy { it.id })

        val shiftedTargets = targetTasks.map { targetTask ->
            targetTask.copy(
                position = targetTask.position + 1,
                updatedAt = now,
                syncStatus = SyncStatus.SYNCED,
            )
        }
        val movedTask = task.copy(
            category = targetCategory,
            position = 0,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )

        applyLocalTaskUpdates(shiftedTargets + movedTask)
        syncCoordinator.pushTaskPositionUpdatesInBackground(
            updates = shiftedTargets.map { it.toRemote() },
            categoryMove = movedTask.toRemote() to originalCategory,
        )
    }

    private suspend fun moveTaskToPreviousCategoryBottom(
        task: TaskDefinitionEntity,
        targetCategory: String,
    ) {
        val now = System.currentTimeMillis()
        val originalCategory = task.category
        val targetTasks = taskDefinitionDao.getByCategory(targetCategory)
        val bottomPosition = (targetTasks.maxOfOrNull { it.position } ?: -1) + 1

        val movedTask = task.copy(
            category = targetCategory,
            position = bottomPosition,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED,
        )
        val shiftedSources = taskDefinitionDao.getByCategory(originalCategory)
            .filter { it.id != task.id && it.position > task.position }
            .map { sourceTask ->
                sourceTask.copy(
                    position = sourceTask.position - 1,
                    updatedAt = now,
                    syncStatus = SyncStatus.SYNCED,
                )
            }

        applyLocalTaskUpdates(listOf(movedTask) + shiftedSources)
        syncCoordinator.pushTaskPositionUpdatesInBackground(
            updates = shiftedSources.map { it.toRemote() },
            categoryMove = movedTask.toRemote() to originalCategory,
        )
    }

    private suspend fun applyLocalTaskUpdates(updates: List<TaskDefinitionEntity>) {
        taskDefinitionDao.upsertAllInTransaction(updates)
    }

    private suspend fun nextCategoryPosition(): Int {
        val categories = categoryDao.getAll()
        return categories.size
    }

    private suspend fun nextPositionInCategory(category: String): Int {
        val tasksInCategory = taskDefinitionDao.getByCategory(category)
        return (tasksInCategory.maxOfOrNull { it.position } ?: -1) + 1
    }

    private fun CategoryEntity.toRemote(): RemoteCategory {
        return RemoteCategory(
            id = id,
            category = categoryName,
            title = title,
            description = description,
            position = position,
        )
    }

    private fun TaskDefinitionEntity.toRemote(): RemoteTaskDefinition {
        return RemoteTaskDefinition(
            id = id,
            category = category,
            name = name,
            points = points,
            position = position,
            emoji = emoji,
        )
    }
}
