package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** Flat list of all categories sorted by order, then name. */
    fun observeAll(): Flow<List<Category>>

    /** Hierarchical tree starting from root folders (parentId = null). */
    fun observeHierarchy(): Flow<List<Category>>

    /** Immediate children of a given parent (null = root level). */
    fun observeChildren(parentId: String?): Flow<List<Category>>

    suspend fun getAll(): List<Category>
    suspend fun upsertAll(categories: List<Category>)
    suspend fun getById(id: String): Category?
    suspend fun save(category: Category)
    suspend fun delete(id: String)
    suspend fun moveFolder(folderId: String, newParentId: String?)
    suspend fun reorderFolders(parentId: String?, folderIds: List<String>)

    /** IDs deleted locally but not yet acknowledged by Drive sync. */
    suspend fun getPendingDeletedIds(): Set<String>

    /** Clears pending deleted IDs after a successful sync upload. */
    suspend fun clearPendingDeletedIds()

    /** Deletes categories from local DB without triggering sync or adding to pendingDeletedIds.
     *  Used by SyncWorker to apply remote deletions. */
    suspend fun applyRemoteDeletions(ids: Set<String>)
}
