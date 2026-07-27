package com.yourname.simplenotes.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    /**
     * Moves a folder to a new parent (or to root when newParentId is null). Also bumps
     * [CategoryEntity.updatedAt] — see [updateOrder] for why that matters for sync.
     */
    @Query("UPDATE categories SET parent_id = :parentId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateParent(id: String, parentId: String?, updatedAt: Long = System.currentTimeMillis())

    /**
     * Updates the sort order of a folder within its parent. Must also bump [CategoryEntity.updatedAt]:
     * SyncWorker.syncCategories() merges local vs. remote categories last-write-wins by that field,
     * so a reorder that leaves it untouched looks like a no-op edit and a stale remote copy —
     * still carrying the old order — wins the merge, silently reverting the drag a moment later.
     */
    @Query("UPDATE categories SET `order` = :order, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int, updatedAt: Long)

    /**
     * Updates the sort order of multiple folders atomically. Without @Transaction, each
     * [updateOrder] call commits (and invalidates observeAll()'s Flow) separately, so
     * collectors would briefly see a partially-reordered list mid-batch before the final,
     * fully-correct order lands — visible as a folder snapping back after a drag-reorder.
     */
    @Transaction
    suspend fun updateOrders(orderedIds: List<String>) {
        val now = System.currentTimeMillis()
        orderedIds.forEachIndexed { index, id -> updateOrder(id, index, now) }
    }
}
