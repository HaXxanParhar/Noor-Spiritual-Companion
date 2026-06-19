package com.parhar.noor.ui.admin

import com.parhar.noor.data.repository.TaskPositionDirection
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem

internal object TaskSectionOptimisticReorder {

    fun apply(
        sections: List<HomeTaskSection>,
        taskId: String,
        category: String,
        direction: TaskPositionDirection,
    ): List<HomeTaskSection>? {
        val sectionIndex = sections.indexOfFirst { it.category.equals(category, ignoreCase = true) }
        if (sectionIndex < 0) return null

        val section = sections[sectionIndex]
        val taskIndex = section.tasks.indexOfFirst { it.id == taskId }
        if (taskIndex < 0) return null

        return when (direction) {
            TaskPositionDirection.UP -> when {
                taskIndex > 0 -> sections.swapTasksInSection(sectionIndex, taskIndex, taskIndex - 1)
                sectionIndex > 0 -> sections.moveTaskToPreviousSection(sectionIndex, taskIndex)
                else -> null
            }
            TaskPositionDirection.DOWN -> when {
                taskIndex < section.tasks.lastIndex -> sections.swapTasksInSection(sectionIndex, taskIndex, taskIndex + 1)
                sectionIndex < sections.lastIndex -> sections.moveTaskToNextSection(sectionIndex, taskIndex)
                else -> null
            }
        }
    }

    private fun List<HomeTaskSection>.swapTasksInSection(
        sectionIndex: Int,
        fromIndex: Int,
        toIndex: Int,
    ): List<HomeTaskSection> {
        val section = this[sectionIndex]
        val tasks = section.tasks.toMutableList()
        val temp = tasks[fromIndex]
        tasks[fromIndex] = tasks[toIndex]
        tasks[toIndex] = temp
        return toMutableList().apply {
            this[sectionIndex] = section.copy(tasks = tasks)
        }
    }

    private fun List<HomeTaskSection>.moveTaskToNextSection(
        sectionIndex: Int,
        taskIndex: Int,
    ): List<HomeTaskSection> {
        val sourceSection = this[sectionIndex]
        val targetSection = this[sectionIndex + 1]
        val taskItem = sourceSection.tasks[taskIndex]
        val movedTask = taskItem.withCategoryAtPosition(
            category = targetSection.category,
            position = 0,
        )

        val newSourceTasks = sourceSection.tasks.toMutableList().apply { removeAt(taskIndex) }
        val newTargetTasks = buildList {
            add(movedTask)
            targetSection.tasks.forEach { item ->
                add(item.withPosition(item.task.position + 1))
            }
        }

        return toMutableList().apply {
            this[sectionIndex] = sourceSection.copy(tasks = newSourceTasks)
            this[sectionIndex + 1] = targetSection.copy(tasks = newTargetTasks)
        }
    }

    private fun List<HomeTaskSection>.moveTaskToPreviousSection(
        sectionIndex: Int,
        taskIndex: Int,
    ): List<HomeTaskSection> {
        val sourceSection = this[sectionIndex]
        val targetSection = this[sectionIndex - 1]
        val taskItem = sourceSection.tasks[taskIndex]
        val bottomPosition = (targetSection.tasks.maxOfOrNull { it.task.position } ?: -1) + 1
        val movedTask = taskItem.withCategoryAtPosition(
            category = targetSection.category,
            position = bottomPosition,
        )

        val newSourceTasks = sourceSection.tasks.toMutableList().apply { removeAt(taskIndex) }
        val newTargetTasks = targetSection.tasks + movedTask

        return toMutableList().apply {
            this[sectionIndex] = sourceSection.copy(tasks = newSourceTasks)
            this[sectionIndex - 1] = targetSection.copy(tasks = newTargetTasks)
        }
    }

    private fun TaskItem.withCategoryAtPosition(category: String, position: Int): TaskItem {
        return copy(task = task.copy(category = category, position = position))
    }

    private fun TaskItem.withPosition(position: Int): TaskItem {
        return copy(task = task.copy(position = position))
    }
}
