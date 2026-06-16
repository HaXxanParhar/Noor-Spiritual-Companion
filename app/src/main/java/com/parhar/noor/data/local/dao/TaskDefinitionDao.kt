package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDefinitionDao {

    @Query("SELECT * FROM task_definitions ORDER BY category ASC, sort_order ASC, id ASC")
    fun observeAll(): Flow<List<TaskDefinitionEntity>>

    @Query("SELECT * FROM task_definitions ORDER BY category ASC, sort_order ASC, id ASC")
    suspend fun getAll(): List<TaskDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskDefinitionEntity>)

    @Query("SELECT * FROM task_definitions WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<TaskDefinitionEntity>
}
