package com.yourname.simplenotes.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY `order` ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY `order` ASC, name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)

    /** Moves a folder to a new parent (or to root when newParentId is null). */
    @Query("UPDATE categories SET parent_id = :parentId WHERE id = :id")
    suspend fun updateParent(id: String, parentId: String?)

    /** Updates the sort order of a folder within its parent. */
    @Query("UPDATE categories SET `order` = :order WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int)
}
