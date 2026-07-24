package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeAll(): Flow<List<Note>>
    suspend fun getById(id: String): Note?
    suspend fun getByIdIncludeDeleted(id: String): Note?
    suspend fun save(note: Note)
    suspend fun delete(id: String)
    suspend fun getDirtyNotes(): List<Note>
    suspend fun markClean(id: String)
    /** Called by SyncWorker to merge notes downloaded from Drive (never marks them dirty). */
    suspend fun upsertFromRemote(notes: List<Note>)

    fun observeDeleted(): Flow<List<Note>>
    suspend fun restore(id: String)
    suspend fun permanentDelete(id: String)
    suspend fun purgeOldDeleted(cutoffMs: Long)

    /** Note IDs permanently deleted locally but not yet deleted from Drive — for SyncWorker to clean up remotely. */
    suspend fun getPendingDeletedIds(): Set<String>
    suspend fun clearPendingDeletedIds()
}
