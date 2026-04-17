package com.yourname.simplenotes.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.simplenotes.data.remote.DriveDataSource
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
    private val driveDataSource: DriveDataSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { sync() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    private suspend fun sync() {
        // Step 1: fetch Drive index {noteId → modifiedTimeIso}
        val driveIndex = driveDataSource.fetchIndex()

        // Collect dirty local notes upfront
        val dirtyNotes = repository.getDirtyNotes()
        val dirtyIds = dirtyNotes.map { it.id }.toSet()

        // Step 2: determine which Drive notes are newer than local copies
        val notesToDownload = driveIndex.keys.filter { noteId ->
            val driveTime = parseIso(driveIndex[noteId] ?: return@filter false)
            if (noteId in dirtyIds) {
                // Conflict: Drive wins only if strictly newer
                val local = repository.getById(noteId) ?: return@filter true
                driveTime > local.updatedAt
            } else {
                val local = repository.getById(noteId)
                local == null || driveTime > local.updatedAt
            }
        }

        if (notesToDownload.isNotEmpty()) {
            val remoteNotes = driveDataSource.downloadAllNotes()
            repository.upsertFromRemote(remoteNotes.filter { it.id in notesToDownload })
        }

        // Step 3: upload dirty local notes (skip those where Drive won the conflict)
        val newIndex = driveIndex.toMutableMap()
        for (note in dirtyNotes) {
            if (note.id in notesToDownload) continue  // Drive won — skip upload
            val modifiedTime = driveDataSource.uploadNote(note) ?: continue
            repository.markClean(note.id)
            if (note.isDeleted) {
                newIndex.remove(note.id)
            } else {
                newIndex[note.id] = modifiedTime
            }
        }

        // Step 4: push updated index
        driveDataSource.uploadIndex(newIndex)
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
