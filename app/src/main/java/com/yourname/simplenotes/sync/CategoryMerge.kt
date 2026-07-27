package com.yourname.simplenotes.sync

import com.yourname.simplenotes.domain.model.Category

/**
 * Last-write-wins merge of local vs. remote categories by [Category.updatedAt], excluding
 * anything tombstoned in [deletedIds]. Used by [SyncWorker.syncCategories] for both directions:
 * a local edit (e.g. drag-reorder, rename, color change) that hasn't reached Drive yet must win
 * over a stale remote copy, and a remote edit made on another device must win over a local copy
 * that hasn't caught up.
 */
fun mergeCategories(
    local: List<Category>,
    remote: List<Category>,
    deletedIds: Set<String>
): List<Category> {
    val remoteById = remote.associateBy { it.id }
    val localById = local.associateBy { it.id }
    return (remoteById.keys + localById.keys)
        .filter { it !in deletedIds }
        .map { id ->
            val r = remoteById[id]
            val l = localById[id]
            when {
                r == null -> l!!
                l == null -> r
                l.updatedAt > r.updatedAt -> l
                else -> r
            }
        }
}
