package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDefinitionDao {

    @Query("SELECT * FROM task_definitions ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<TaskDefinitionEntity>>

    @Query("SELECT * FROM task_definitions ORDER BY position ASC, id ASC")
    suspend fun getAll(): List<TaskDefinitionEntity>

    @Query("SELECT * FROM task_definitions WHERE category = :category ORDER BY position ASC, id ASC")
    suspend fun getByCategory(category: String): List<TaskDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskDefinitionEntity>)

    @Transaction
    suspend fun upsertAllInTransaction(tasks: List<TaskDefinitionEntity>) {
        if (tasks.isNotEmpty()) {
            upsertAll(tasks)
        }
    }

    @Query("SELECT * FROM task_definitions WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<TaskDefinitionEntity>

    @Query("DELETE FROM task_definitions WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM task_definitions WHERE category = :category")
    suspend fun deleteByCategory(category: String)
}
