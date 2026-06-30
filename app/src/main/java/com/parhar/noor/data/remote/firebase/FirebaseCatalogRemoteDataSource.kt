package com.parhar.noor.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.parhar.noor.data.remote.CatalogRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteCategory
import com.parhar.noor.data.remote.dto.RemoteTaskDefinition
import kotlinx.coroutines.tasks.await
import java.io.Closeable

class FirebaseCatalogRemoteDataSource(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
) : CatalogRemoteDataSource {

    override suspend fun fetchCategories(): List<RemoteCategory> {
        val snapshot = database.reference.child(CATEGORIES_NODE).get().await()
        return snapshot.children.mapNotNull { child -> child.toRemoteCategory() }
    }

    override suspend fun fetchTaskDefinitions(): List<RemoteTaskDefinition> {
        val snapshot = database.reference.child(TASKS_NODE).get().await()
        return snapshot.children.flatMap { categorySnapshot ->
            val categoryName = categorySnapshot.key.orEmpty()
            categorySnapshot.children.mapNotNull { taskSnapshot ->
                taskSnapshot.toRemoteTaskDefinition(categoryName)
            }
        }
    }

    override suspend fun pushTaskDefinition(task: RemoteTaskDefinition) {
        database.reference
            .child(TASKS_NODE)
            .child(task.category)
            .child(task.id)
            .setValue(
                mapOf(
                    CATEGORY_FIELD to task.category,
                    NAME_FIELD to task.name,
                    POINTS_FIELD to task.points,
                    POSITION_FIELD to task.position,
                    EMOJI_FIELD to task.emoji,
                    SHORT_DESCRIPTION_FIELD to task.shortDescription,
                    DETAILED_DESCRIPTION_FIELD to task.detailedDescription,
                    ARABIC_FIELD to task.arabic,
                    VISIBLE_FIELD to task.visible,
                ),
            )
            .await()
    }

    override suspend fun deleteTaskDefinition(category: String, taskId: String) {
        database.reference
            .child(TASKS_NODE)
            .child(category)
            .child(taskId)
            .removeValue()
            .await()
    }

    override suspend fun pushCategory(category: RemoteCategory) {
        database.reference
            .child(CATEGORIES_NODE)
            .child(category.id)
            .setValue(
                mapOf(
                    CATEGORY_FIELD to category.category,
                    TITLE_FIELD to category.title,
                    DESCRIPTION_FIELD to category.description,
                    POSITION_FIELD to category.position,
                ),
            )
            .await()
    }

    override suspend fun deleteCategory(categoryId: String) {
        database.reference
            .child(CATEGORIES_NODE)
            .child(categoryId)
            .removeValue()
            .await()
    }

    override suspend fun deleteTasksForCategory(category: String) {
        database.reference
            .child(TASKS_NODE)
            .child(category)
            .removeValue()
            .await()
    }

    override fun observeTaskDefinitions(
        onChanged: (List<RemoteTaskDefinition>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = snapshot.children.flatMap { categorySnapshot ->
                    val categoryName = categorySnapshot.key.orEmpty()
                    categorySnapshot.children.mapNotNull { taskSnapshot ->
                        taskSnapshot.toRemoteTaskDefinition(categoryName)
                    }
                }
                onChanged(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference.child(TASKS_NODE).addValueEventListener(listener)
        return CloseableListener {
            database.reference.child(TASKS_NODE).removeEventListener(listener)
        }
    }

    override fun observeCategories(
        onChanged: (List<RemoteCategory>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.children.mapNotNull { child -> child.toRemoteCategory() })
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference.child(CATEGORIES_NODE).addValueEventListener(listener)
        return CloseableListener {
            database.reference.child(CATEGORIES_NODE).removeEventListener(listener)
        }
    }

    private fun DataSnapshot.toRemoteCategory(): RemoteCategory? {
        val category = child(CATEGORY_FIELD).getValue(String::class.java).orEmpty()
        if (category.isBlank()) return null
        return RemoteCategory(
            id = key.orEmpty(),
            category = category,
            title = child(TITLE_FIELD).getValue(String::class.java).orEmpty(),
            description = child(DESCRIPTION_FIELD).getValue(String::class.java).orEmpty(),
            position = child(POSITION_FIELD).getValue(Int::class.java) ?: 0,
        )
    }

    private fun DataSnapshot.toRemoteTaskDefinition(categoryName: String): RemoteTaskDefinition? {
        val name = child(NAME_FIELD).getValue(String::class.java).orEmpty()
        if (name.isBlank()) return null
        val id = key.orEmpty()
        return RemoteTaskDefinition(
            id = id,
            category = child(CATEGORY_FIELD).getValue(String::class.java).orEmpty()
                .ifBlank { categoryName },
            name = name,
            points = child(POINTS_FIELD).getValue(Int::class.java) ?: 0,
            position = child(POSITION_FIELD).getValue(Int::class.java) ?: 0,
            emoji = child(EMOJI_FIELD).getValue(String::class.java).orEmpty(),
            shortDescription = child(SHORT_DESCRIPTION_FIELD).getValue(String::class.java).orEmpty(),
            detailedDescription = child(DETAILED_DESCRIPTION_FIELD).getValue(String::class.java).orEmpty(),
            arabic = child(ARABIC_FIELD).getValue(String::class.java).orEmpty(),
            visible = child(VISIBLE_FIELD).getValue(Boolean::class.java) ?: true,
        )
    }

    private class CloseableListener(private val onClose: () -> Unit) : Closeable {
        override fun close() = onClose()
    }

    private companion object {
        private const val CATEGORIES_NODE = "categories"
        private const val TASKS_NODE = "tasks"
        private const val CATEGORY_FIELD = "category"
        private const val NAME_FIELD = "name"
        private const val POINTS_FIELD = "points"
        private const val POSITION_FIELD = "position"
        private const val EMOJI_FIELD = "emoji"
        private const val SHORT_DESCRIPTION_FIELD = "shortDescription"
        private const val DETAILED_DESCRIPTION_FIELD = "detailedDescription"
        private const val ARABIC_FIELD = "arabic"
        private const val VISIBLE_FIELD = "visible"
        private const val TITLE_FIELD = "title"
        private const val DESCRIPTION_FIELD = "description"
    }
}
