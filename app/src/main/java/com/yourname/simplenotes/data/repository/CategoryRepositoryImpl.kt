package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.data.local.CategoryDao
import com.yourname.simplenotes.data.local.CategoryEntity
import com.yourname.simplenotes.data.local.CategorySyncPrefs
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
    private val syncScheduler: SyncScheduler,
    private val syncPrefs: CategorySyncPrefs
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeHierarchy(): Flow<List<Category>> =
        dao.observeAll().map { entities ->
            buildTree(entities.map { it.toDomain() }, null)
        }

    override fun observeChildren(parentId: String?): Flow<List<Category>> =
        dao.observeAll().map { entities ->
            entities.filter { it.parentId == parentId }
                .sortedBy { it.order }
                .map { it.toDomain() }
        }

    override suspend fun getAll(): List<Category> = dao.getAll().map { it.toDomain() }

    override suspend fun upsertAll(categories: List<Category>) =
        dao.upsertAll(categories.map { it.toEntity() })

    override suspend fun getById(id: String): Category? = dao.getById(id)?.toDomain()

    override suspend fun save(category: Category) {
        dao.upsert(category.copy(updatedAt = System.currentTimeMillis()).toEntity())
        syncScheduler.triggerImmediateSync()  // Bug 5 fix
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
        syncPrefs.addDeletedId(id)            // Bug 4 fix: track for Drive propagation
        syncScheduler.triggerImmediateSync()  // Bug 5 fix
    }

    override suspend fun moveFolder(folderId: String, newParentId: String?) =
        dao.updateParent(folderId, newParentId)

    override suspend fun reorderFolders(parentId: String?, folderIds: List<String>) {
        dao.updateOrders(folderIds)
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun getPendingDeletedIds(): Set<String> = syncPrefs.pendingDeletedIds

    override suspend fun clearPendingDeletedIds() = syncPrefs.clear()

    override suspend fun applyRemoteDeletions(ids: Set<String>) {
        ids.forEach { dao.delete(it) }
        // Do NOT add to syncPrefs — these deletions came from Drive, not local user action
    }
}

private fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    colorArgb = colorArgb,
    parentId = parentId,
    order = order,
    updatedAt = updatedAt
)

/** Builds a nested folder tree from a flat list, sorted by order within each level. */
private fun buildTree(flat: List<Category>, parentId: String?): List<Category> =
    flat.filter { it.parentId == parentId }
        .sortedBy { it.order }
        .map { cat -> cat.copy(subFolders = buildTree(flat, cat.id)) }

private fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    colorArgb = colorArgb,
    parentId = parentId,
    order = order,
    updatedAt = updatedAt
)
