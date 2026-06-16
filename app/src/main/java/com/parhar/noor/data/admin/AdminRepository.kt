package com.parhar.noor.data.admin

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class AdminRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
) {

    suspend fun getCategories(): List<CategoryItem> {
        val snapshot = database.reference.child(CATEGORIES_NODE).get().await()
        return snapshot.children.mapNotNull { child ->
            child.toCategoryItem()
        }.sortedBy { it.category.lowercase() }
    }

    suspend fun addTask(task: Task): String {
        val taskId = System.currentTimeMillis().toString()
        database.reference
            .child(TASKS_NODE)
            .child(task.category)
            .child(taskId)
            .setValue(task)
            .await()
        return taskId
    }

    fun observeTasks(
        onTasksChanged: (Map<String, List<TaskItem>>) -> Unit,
        onError: (String) -> Unit,
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupedTasks = linkedMapOf<String, List<TaskItem>>()
                snapshot.children.forEach { categorySnapshot ->
                    val categoryName = categorySnapshot.key.orEmpty()
                    val tasks = categorySnapshot.children.mapNotNull { taskSnapshot ->
                        taskSnapshot.toTaskItem(categoryName)
                    }.sortedWith(
                        compareBy<TaskItem> { it.id.toLongOrNull() ?: Long.MAX_VALUE }
                            .thenBy { it.id },
                    )

                    if (categoryName.isNotBlank() && tasks.isNotEmpty()) {
                        groupedTasks[categoryName] = tasks
                    }
                }
                onTasksChanged(groupedTasks)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        database.reference.child(TASKS_NODE).addValueEventListener(listener)
        return listener
    }

    fun removeTasksObserver(listener: ValueEventListener) {
        database.reference.child(TASKS_NODE).removeEventListener(listener)
    }

    private fun DataSnapshot.toCategoryItem(): CategoryItem? {
        val category = child(CATEGORY_FIELD).getValue(String::class.java).orEmpty()
        if (category.isBlank()) return null

        return CategoryItem(
            id = key.orEmpty(),
            category = category,
            title = child(TITLE_FIELD).getValue(String::class.java).orEmpty(),
        )
    }

    private fun DataSnapshot.toTaskItem(categoryName: String): TaskItem? {
        val name = child(NAME_FIELD).getValue(String::class.java).orEmpty()
        if (name.isBlank()) return null

        return TaskItem(
            id = key.orEmpty(),
            task = Task(
                category = child(CATEGORY_FIELD).getValue(String::class.java).orEmpty()
                    .ifBlank { categoryName },
                name = name,
                points = child(POINTS_FIELD).getValue(Int::class.java) ?: 0,
            ),
        )
    }

    private companion object {
        private const val CATEGORIES_NODE = "categories"
        private const val TASKS_NODE = "tasks"
        private const val CATEGORY_FIELD = "category"
        private const val NAME_FIELD = "name"
        private const val POINTS_FIELD = "points"
        private const val TITLE_FIELD = "title"
    }
}
