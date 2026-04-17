package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.data.local.CategoryDao
import com.yourname.simplenotes.data.local.CategoryEntity
import com.yourname.simplenotes.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(private val dao: CategoryDao) : CategoryRepository {

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

    override suspend fun getById(id: String): Category? = dao.getById(id)?.toDomain()

    override suspend fun save(category: Category) =
        dao.upsert(
            CategoryEntity(
                id = category.id,
                name = category.name,
                colorArgb = category.colorArgb,
                parentId = category.parentId,
                order = category.order
            )
        )

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun moveFolder(folderId: String, newParentId: String?) =
        dao.updateParent(folderId, newParentId)

    override suspend fun reorderFolders(parentId: String?, folderIds: List<String>) {
        folderIds.forEachIndexed { index, id -> dao.updateOrder(id, index) }
    }
}

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
    order = order
)
