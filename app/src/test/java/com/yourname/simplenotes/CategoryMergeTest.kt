package com.yourname.simplenotes

import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.sync.mergeCategories
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Simulates the two-device round trip for a folder drag-reorder:
 *  1. Device 1 reorders locally (bumping updatedAt) and syncs — its sync worker merges its
 *     newly-reordered local copy against the (still old) copy already on Drive, then re-uploads
 *     the merge result.
 *  2. Device 2 syncs later — its worker merges its own (stale) local copy against what device 1
 *     just uploaded, and must adopt device 1's new order.
 *
 * If either merge picks the wrong side, the reorder either never reaches Drive or never reaches
 * device 2 — exactly the "kéo folder xong không sync qua thiết bị khác" symptom.
 */
class CategoryMergeTest {

    @Test
    fun `device 1's local reorder wins over the stale copy already on Drive`() {
        val driveCopy = listOf(
            Category(id = "a", name = "Bank", order = 0, updatedAt = 1000L),
            Category(id = "b", name = "Acc", order = 1, updatedAt = 1000L)
        )
        // Device 1 just dragged "b" above "a" — CategoryDao.updateOrders bumps updatedAt for both.
        val device1LocalAfterDrag = listOf(
            Category(id = "b", name = "Acc", order = 0, updatedAt = 2000L),
            Category(id = "a", name = "Bank", order = 1, updatedAt = 2000L)
        )

        val merged = mergeCategories(local = device1LocalAfterDrag, remote = driveCopy, deletedIds = emptySet())

        assertEquals(0, merged.first { it.id == "b" }.order)
        assertEquals(1, merged.first { it.id == "a" }.order)
    }

    @Test
    fun `device 2 adopts device 1's newly-uploaded order over its own stale local copy`() {
        // What device 1 uploaded to Drive after the drag (from the test above).
        val driveCopyAfterDevice1Sync = listOf(
            Category(id = "b", name = "Acc", order = 0, updatedAt = 2000L),
            Category(id = "a", name = "Bank", order = 1, updatedAt = 2000L)
        )
        // Device 2 never touched its folders — still has the original order/timestamp.
        val device2StaleLocal = listOf(
            Category(id = "a", name = "Bank", order = 0, updatedAt = 1000L),
            Category(id = "b", name = "Acc", order = 1, updatedAt = 1000L)
        )

        val merged = mergeCategories(local = device2StaleLocal, remote = driveCopyAfterDevice1Sync, deletedIds = emptySet())

        assertEquals(0, merged.first { it.id == "b" }.order)
        assertEquals(1, merged.first { it.id == "a" }.order)
    }
}
