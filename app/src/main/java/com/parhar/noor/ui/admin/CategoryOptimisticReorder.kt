package com.parhar.noor.ui.admin

import com.parhar.noor.data.repository.CategoryPositionDirection
import com.parhar.noor.domain.model.Category

internal object CategoryOptimisticReorder {

    fun apply(
        categories: List<Category>,
        categoryId: String,
        direction: CategoryPositionDirection,
    ): List<Category>? {
        val sorted = categories.sortedWith(
            compareBy<Category> { it.position }.thenBy { it.category },
        )
        val currentIndex = sorted.indexOfFirst { it.id == categoryId }
        if (currentIndex < 0) return null

        val neighborIndex = when (direction) {
            CategoryPositionDirection.UP -> currentIndex - 1
            CategoryPositionDirection.DOWN -> currentIndex + 1
        }
        if (neighborIndex !in sorted.indices) return null

        val current = sorted[currentIndex]
        val neighbor = sorted[neighborIndex]
        return sorted.toMutableList().apply {
            this[currentIndex] = current.copy(position = neighbor.position)
            this[neighborIndex] = neighbor.copy(position = current.position)
        }
    }
}
