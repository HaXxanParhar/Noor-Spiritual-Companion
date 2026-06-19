package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.Category
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow

enum class TaskPositionDirection {
    UP,
    DOWN,
}

interface CatalogRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeTaskItems(): Flow<List<TaskItem>>
    fun observeTaskSections(includeEmptyCategories: Boolean = false): Flow<List<HomeTaskSection>>
    suspend fun getCategories(): List<Category>
    suspend fun isCategoryNameTaken(categoryName: String, excludeCategoryId: String? = null): Boolean
    suspend fun addCategory(title: String, categoryName: String, description: String): String
    suspend fun updateCategory(
        categoryId: String,
        originalCategoryName: String,
        title: String,
        categoryName: String,
        description: String,
    )
    suspend fun deleteCategory(categoryId: String, categoryName: String)
    suspend fun swapCategoryPosition(categoryId: String, direction: CategoryPositionDirection)
    suspend fun addTask(category: String, name: String, points: Int, emoji: String = ""): String
    suspend fun updateTask(
        taskId: String,
        originalCategory: String,
        category: String,
        name: String,
        points: Int,
        emoji: String = "",
    )
    suspend fun deleteTask(taskId: String, category: String)
    suspend fun swapTaskPosition(
        taskId: String,
        category: String,
        direction: TaskPositionDirection,
    )
}
