package com.yourname.simplenotes.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.simplenotes.data.remote.DriveDataSource
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    private suspend fun sync() {
        // Step 1: fetch Drive index {noteId → modifiedTimeIso}
        val driveIndex = driveDataSource.fetchIndex()

        // Collect dirty local notes upfront
        val dirtyNotes = repository.getDirtyNotes()

        // Step 2: determine which Drive notes are newer than local copies.
        // Use getByIdIncludeDeleted so locally-deleted notes are compared by timestamp,
        // not silently treated as "missing" (which would incorrectly un-delete them).
        val notesToDownload = driveIndex.keys.filter { noteId ->
            val driveTime = parseIso(driveIndex[noteId] ?: return@filter false)
            val local = repository.getByIdIncludeDeleted(noteId)
            local == null || driveTime > local.updatedAt
        }

        // Bug 3 fix: download only the specific notes needed, not all notes.
        if (notesToDownload.isNotEmpty()) {
            val remoteNotes = driveDataSource.downloadNotes(notesToDownload.toSet())
            repository.upsertFromRemote(remoteNotes)
        }

        // Step 3: upload dirty local notes (skip those where Drive won the conflict)
        val newIndex = driveIndex.toMutableMap()
        for (note in dirtyNotes) {
            if (note.id in notesToDownload) continue  // Drive won — skip upload
            val modifiedTime = driveDataSource.uploadNote(note) ?: continue
            repository.markClean(note.id)
            // Bug 1 fix: keep tombstones in index so Device B can download and apply the deletion.
            newIndex[note.id] = modifiedTime
        }

        // Step 4: push updated index
        driveDataSource.uploadIndex(newIndex)

        // Step 5: sync categories — Drive is source of truth when remote exists,
        // otherwise push local categories up
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
        runCatching {
            ISO_FORMAT.parse(isoString)?.time ?: 0L
        }.getOrDefault(0L)

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_IMMEDIATE = "sync_immediate"

        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
