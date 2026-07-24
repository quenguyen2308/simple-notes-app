package com.yourname.simplenotes.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.simplenotes.data.remote.DriveDataSource
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
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

        // Collect dirty local notes upfront
        val dirtyNotes = repository.getDirtyNotes()
        Log.i(TAG, "Dirty local notes: ${dirtyNotes.size}")

        // Step 2: determine which Drive notes are newer than local copies.
        val notesToDownload = driveIndex.keys.filter { noteId ->
            val driveTime = parseIso(driveIndex[noteId] ?: return@filter false)
            val local = repository.getByIdIncludeDeleted(noteId)
            // Local deletion wins: never re-download a note we've already deleted locally
            if (local?.isDeleted == true) return@filter false
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
        // on a future sync. Must happen before the index upload below.
        val pendingDeletedIds = repository.getPendingDeletedIds()
        if (pendingDeletedIds.isNotEmpty()) {
            Log.i(TAG, "Deleting ${pendingDeletedIds.size} notes from Drive")
            pendingDeletedIds.forEach { id ->
                driveDataSource.deleteNote(id)
                newIndex.remove(id)
            }
            repository.clearPendingDeletedIds()
        }

        // Step 6: push updated index
        driveDataSource.uploadIndex(newIndex)
        Log.i(TAG, "Index uploaded with ${newIndex.size} entries")

        syncCategories()
    }

    private suspend fun syncCategories() {
        val (remoteCategories, remoteDeletedIds) = driveDataSource.downloadCategories()
        val localCategories = categoryRepository.getAll()
        val pendingDeletedIds = categoryRepository.getPendingDeletedIds()

        if (remoteCategories.isEmpty() && remoteDeletedIds.isEmpty()) {
            // First sync from this device — upload local categories
            if (localCategories.isNotEmpty() || pendingDeletedIds.isNotEmpty()) {
                driveDataSource.uploadCategories(localCategories, pendingDeletedIds)
                categoryRepository.clearPendingDeletedIds()
            }
            return
        }

        // Apply remote deletions to local DB (without re-adding to pendingDeletedIds)
        if (remoteDeletedIds.isNotEmpty()) {
            categoryRepository.applyRemoteDeletions(remoteDeletedIds)
        }

        // Merge: all deleted IDs = remote + local pending
        val allDeletedIds = remoteDeletedIds + pendingDeletedIds

        // Active remote categories (not deleted) + local-only (not in remote, not deleted)
        val remoteIds = remoteCategories.map { it.id }.toSet()
        val localOnly = localCategories.filter { it.id !in remoteIds && it.id !in allDeletedIds }
        val merged = remoteCategories.filter { it.id !in allDeletedIds } + localOnly

        categoryRepository.upsertAll(merged)
        driveDataSource.uploadCategories(merged, allDeletedIds)
        categoryRepository.clearPendingDeletedIds()
    }

    private fun parseIso(isoString: String): Long =
        runCatching { Instant.parse(isoString).toEpochMilli() }.getOrDefault(0L)

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_IMMEDIATE = "sync_immediate"
        private const val TAG = "SyncWorker"
    }
}
