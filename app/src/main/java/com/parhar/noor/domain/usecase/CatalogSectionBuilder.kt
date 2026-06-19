package com.parhar.noor.domain.usecase

import com.parhar.noor.domain.model.Category
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskDefinition
import com.parhar.noor.domain.model.TaskItem

object CatalogSectionBuilder {

    fun buildSections(
        categories: List<Category>,
        tasks: List<TaskDefinition>,
        includeEmptyCategories: Boolean = false,
    ): List<HomeTaskSection> {
        val tasksByCategory = tasks.groupBy { it.category }
        val sortedCategories = categories.sortedWith(
            compareBy<Category> { it.position }.thenBy { it.category.lowercase() },
        )

        return sortedCategories.mapNotNull { category ->
            val sectionTasks = tasksByCategory[category.category]
                .orEmpty()
                .sortedWith(compareBy<TaskDefinition> { it.position }.thenBy { it.id })
                .map { task -> TaskItem(id = task.id, task = task) }

            if (!includeEmptyCategories && sectionTasks.isEmpty()) {
                null
            } else {
                HomeTaskSection(
                    category = category.category,
                    title = category.title.ifBlank { category.category },
                    description = category.description,
                    tasks = sectionTasks,
                )
            }
        }
    }
}
