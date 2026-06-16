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
                ),
            )
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
        private const val TITLE_FIELD = "title"
    }
}
