package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeAll(): Flow<List<Note>>
    suspend fun getById(id: String): Note?
    suspend fun getByIdIncludeDeleted(id: String): Note?
    suspend fun save(note: Note)
    /**
     * Bulk-saves many notes with a single sync trigger at the end (e.g. importing a backup
     * file) instead of one per note — [save] calling triggerImmediateSync() on every note
     * means a large batch repeatedly cancels-and-replaces its own in-flight sync worker via
     * WorkManager's REPLACE policy, so the notes saved earlier in the batch can end up
     * uploaded by a run that gets cancelled before finishing, silently delaying (or on a flaky
     * connection, dropping) the rest until some later sync happens to run to completion.
     */
    suspend fun saveAll(notes: List<Note>)
    suspend fun delete(id: String)
    /** Bulk soft-delete with a single sync trigger — see [saveAll] for why that matters. */
    suspend fun deleteAll(ids: List<String>)
    suspend fun getDirtyNotes(): List<Note>
    suspend fun markClean(id: String)
    /** Called by SyncWorker to merge notes downloaded from Drive (never marks them dirty). */
    suspend fun upsertFromRemote(notes: List<Note>)

    fun observeDeleted(): Flow<List<Note>>
    suspend fun restore(id: String)
    suspend fun permanentDelete(id: String)
    /** Bulk permanent delete (e.g. "Empty recycle bin") with a single sync trigger. */
    suspend fun permanentDeleteAll(ids: List<String>)
    suspend fun purgeOldDeleted(cutoffMs: Long)

    /** Note IDs permanently deleted locally but not yet deleted from Drive — for SyncWorker to clean up remotely. */
    suspend fun getPendingDeletedIds(): Set<String>
    suspend fun clearPendingDeletedIds()

    /** IDs of already-synced trashed notes — for SyncWorker to detect a permanent deletion made on another device. */
    suspend fun getCleanDeletedNoteIds(): List<String>
    /** Removes notes already gone from Drive (deleted elsewhere) — local-only cleanup, no Drive round-trip needed. */
    suspend fun purgeRemotelyDeleted(ids: List<String>)

    /** Un-assigns any note referencing a folder ID outside [validFolderIds] — see NoteDao.clearOrphanedFolderRefs. */
    suspend fun clearOrphanedFolderRefs(validFolderIds: Set<String>)
}
