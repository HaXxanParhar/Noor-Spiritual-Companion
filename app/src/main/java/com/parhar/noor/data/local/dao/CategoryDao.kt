package com.parhar.noor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.parhar.noor.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY position ASC, category_key ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position ASC, category_key ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getById(categoryId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE category_key = :categoryKey LIMIT 1")
    suspend fun getByCategoryKey(categoryKey: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Transaction
    suspend fun upsertAllInTransaction(categories: List<CategoryEntity>) {
        if (categories.isNotEmpty()) {
            upsertAll(categories)
        }
    }

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: String)
}
