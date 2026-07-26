package com.yourname.simplenotes.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.simplenotes.data.remote.DriveDataSource
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.ui.settings.SettingsPrefs
import java.time.Instant

/**
 * Two-direction sync worker:
 *  1. Pull Drive index → download notes newer than local
 *  2. Upload dirty local notes → mark clean in Room
 *  3. Update Drive index
 *
 * Conflict resolution: last-write-wins via Drive modifiedTime vs Room updatedAt.
 * On failure, WorkManager retries with exponential backoff.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: NoteRepository,
    private val driveDataSource: DriveDataSource,
    private val categoryRepository: CategoryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { sync() }
            .fold(
                onSuccess = { Log.i(TAG, "Sync completed successfully"); Result.success() },
                onFailure = { e -> Log.e(TAG, "Sync failed, will retry: ${e.message}", e); Result.retry() }
            )

    private suspend fun sync() {
        Log.i(TAG, "Starting sync…")

        // Step 1: fetch Drive index {noteId → modifiedTimeIso}
        val driveIndex = driveDataSource.fetchIndex()
        Log.i(TAG, "Drive index has ${driveIndex.size} entries")

        // Fetched once upfront: IDs this device permanently deleted locally (via Empty Trash)
        // whose Drive-side tombstone (file + index entry) hasn't been pushed yet this pass —
        // see Step 5 below.
        val pendingDeletedIds = repository.getPendingDeletedIds()

        // Persistent note-deletion tombstones (mirrors categories.json's deletedIds): unlike
        // index.json — where a purged note just disappears with no trace — this list never
        // shrinks, so a device that missed the intermediate soft-delete (e.g. was offline while
        // another device both deleted the note AND emptied the trash) still learns it's gone
        // instead of keeping a ghost copy forever.
        val remoteDeletedNoteIds = driveDataSource.downloadDeletedNoteIds()
        val allDeletedNoteIds = remoteDeletedNoteIds + pendingDeletedIds
        if (allDeletedNoteIds.isNotEmpty()) {
            repository.purgeRemotelyDeleted(allDeletedNoteIds.toList())
        }

        // A note that's already fully synced (not pending upload) and sitting in this device's
        // trash, but whose ID is no longer in the Drive index, was permanently deleted on
        // another device — mirror that deletion here instead of leaving a ghost copy forever.
        // (Belt-and-suspenders alongside the tombstone list above, which is the authoritative
        // signal going forward.)
        val remotelyPurgedIds = repository.getCleanDeletedNoteIds().filter { it !in driveIndex.keys }
        if (remotelyPurgedIds.isNotEmpty()) {
            Log.i(TAG, "Purging ${remotelyPurgedIds.size} notes permanently deleted on another device")
            repository.purgeRemotelyDeleted(remotelyPurgedIds)
        }

        // Collect dirty local notes upfront
        val dirtyNotes = repository.getDirtyNotes()
        Log.i(TAG, "Dirty local notes: ${dirtyNotes.size}")

        // Step 2: determine which Drive notes are newer than local copies.
        val notesToDownload = driveIndex.keys.filter { noteId ->
            val driveTime = parseIso(driveIndex[noteId] ?: return@filter false)
            val local = repository.getByIdIncludeDeleted(noteId)
            // Never resurrect a note this device just permanently deleted — its Drive tombstone
            // (Step 5) hasn't landed yet this pass, so it's still sitting in driveIndex right now.
            if (noteId in pendingDeletedIds) return@filter false
            // Never resurrect a note tombstoned by any device (persistent deletedIds list).
            if (noteId in allDeletedNoteIds) return@filter false
            (local == null || driveTime > local.updatedAt).also { needsDownload ->
                if (needsDownload) Log.d(TAG, "Will download $noteId (driveTime=$driveTime local=${local?.updatedAt})")
            }
        }
        Log.i(TAG, "Notes to download: ${notesToDownload.size}")

        if (notesToDownload.isNotEmpty()) {
            val remoteNotes = driveDataSource.downloadNotes(notesToDownload.toSet())
            Log.i(TAG, "Downloaded ${remoteNotes.size} notes from Drive")
            repository.upsertFromRemote(remoteNotes)
        }

        // Step 3: upload dirty local notes (skip those where Drive won the conflict)
        val newIndex = driveIndex.toMutableMap()
        for (note in dirtyNotes) {
            if (note.id in notesToDownload) { Log.d(TAG, "Skipping upload of ${note.id} — Drive won"); continue }
            if (note.id in allDeletedNoteIds) { Log.d(TAG, "Skipping upload of ${note.id} — tombstoned"); continue }
            val modifiedTime = driveDataSource.uploadNote(note)
            if (modifiedTime == null) { Log.w(TAG, "Upload failed for ${note.id}"); continue }
            repository.markClean(note.id)
            newIndex[note.id] = modifiedTime
            Log.d(TAG, "Uploaded note ${note.id}")
        }

        // Step 4: permanently remove notes that have been in the recycle bin for more than 30
        // days. Runs after the download step above so a note already queued for download this
        // cycle can't be purged and then immediately re-downloaded in the same pass.
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        repository.purgeOldDeleted(cutoff)

        // Step 5: propagate every pending local permanent-deletion (from purge above, or from
        // the recycle bin's manual "delete forever") to Drive, so it doesn't get re-downloaded
        // on a future sync. Must happen before the index upload below. Re-fetched since Step 4's
        // 30-day auto-purge may have added new entries since the pendingDeletedIds read at the top.
        val allPendingDeletedIds = repository.getPendingDeletedIds()
        if (allPendingDeletedIds.isNotEmpty()) {
            Log.i(TAG, "Deleting ${allPendingDeletedIds.size} notes from Drive")
            allPendingDeletedIds.forEach { id ->
                driveDataSource.deleteNote(id)
                newIndex.remove(id)
            }
        }
        // Persist the merged tombstone list (union, never shrinks) so a device that missed the
        // soft-delete step still learns about the deletion on some future sync.
        val finalDeletedNoteIds = remoteDeletedNoteIds + allPendingDeletedIds
        if (finalDeletedNoteIds.isNotEmpty()) {
            driveDataSource.uploadDeletedNoteIds(finalDeletedNoteIds)
        }
        repository.clearPendingDeletedIds()

        // Step 6: push updated index
        driveDataSource.uploadIndex(newIndex)
        Log.i(TAG, "Index uploaded with ${newIndex.size} entries")

        syncCategories()
        syncSettings()
    }

    private suspend fun syncCategories() {
        val (remoteCategories, remoteDeletedIds) = driveDataSource.downloadCategories()
        val localCategories = categoryRepository.getAll()
        val pendingDeletedIds = categoryRepository.getPendingDeletedIds()

        if (remoteCategories.isEmpty() && remoteDeletedIds.isEmpty()) {
            // First sync from this device (or signed out / offline) — upload local categories
            if (localCategories.isNotEmpty() || pendingDeletedIds.isNotEmpty()) {
                driveDataSource.uploadCategories(localCategories, pendingDeletedIds)
                categoryRepository.clearPendingDeletedIds()
            }
            // Still worth healing against what's locally authoritative — a note left pointing at
            // a folder deleted via a path that skipped reassignment shouldn't wait for a real
            // remote category payload to show up before its folder ref gets cleared.
            repository.clearOrphanedFolderRefs(localCategories.map { it.id }.toSet())
            return
        }

        // Apply remote deletions to local DB (without re-adding to pendingDeletedIds)
        if (remoteDeletedIds.isNotEmpty()) {
            categoryRepository.applyRemoteDeletions(remoteDeletedIds)
        }

        // Merge: all deleted IDs = remote + local pending
        val allDeletedIds = remoteDeletedIds + pendingDeletedIds

        // Last-write-wins per category ID (mirrors note sync's updatedAt comparison), so a
        // local edit (e.g. folder color change) isn't clobbered by a stale remote copy that
        // hasn't caught up yet.
        val remoteById = remoteCategories.associateBy { it.id }
        val localById = localCategories.associateBy { it.id }
        val merged = (remoteById.keys + localById.keys)
            .filter { it !in allDeletedIds }
            .map { id ->
                val remote = remoteById[id]
                val local = localById[id]
                when {
                    remote == null -> local!!
                    local == null -> remote
                    local.updatedAt > remote.updatedAt -> local
                    else -> remote
                }
            }

        categoryRepository.upsertAll(merged)
        driveDataSource.uploadCategories(merged, allDeletedIds)
        categoryRepository.clearPendingDeletedIds()

        // Defensive healing pass (see NoteDao.clearOrphanedFolderRefs): a note whose folder was
        // deleted on another device before this device's copy of that note got reassigned to
        // root ends up invisible everywhere yet still counted in the total. Using the just-merged
        // category set catches that regardless of which device's edit arrived first.
        repository.clearOrphanedFolderRefs(merged.map { it.id }.toSet())
    }

    /**
     * Settings are a single object (not a per-item collection like notes/categories), so the
     * merge is just last-write-wins by [com.yourname.simplenotes.domain.model.SettingsSnapshot.updatedAt]
     * between the one local copy and the one Drive copy.
     */
    private suspend fun syncSettings() {
        val local = SettingsPrefs(applicationContext)
        val remote = driveDataSource.downloadSettings()
        when {
            remote == null -> driveDataSource.uploadSettings(local.toSnapshot())
            remote.updatedAt > local.updatedAt -> local.applySnapshot(remote)
            local.updatedAt > remote.updatedAt -> driveDataSource.uploadSettings(local.toSnapshot())
            // else: already in sync
        }
    }

    private fun parseIso(isoString: String): Long =
        runCatching { Instant.parse(isoString).toEpochMilli() }.getOrDefault(0L)

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_IMMEDIATE = "sync_immediate"
        private const val TAG = "SyncWorker"
    }
}
