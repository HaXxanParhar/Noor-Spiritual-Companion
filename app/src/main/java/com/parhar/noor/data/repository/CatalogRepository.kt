package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.Category
import com.parhar.noor.domain.model.TaskDefinition
import com.parhar.noor.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeTaskItems(): Flow<List<TaskItem>>
    suspend fun getCategories(): List<Category>
    suspend fun addTask(category: String, name: String, points: Int): String
}
