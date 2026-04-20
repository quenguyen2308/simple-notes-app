package com.yourname.simplenotes.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /** Observe all non-deleted notes, newest first. Emits on every change. */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): NoteEntity?

    /** Includes soft-deleted rows — used by SyncWorker for timestamp-based conflict detection. */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getByIdIncludeDeleted(id: String): NoteEntity?

    /** Returns notes that need to be uploaded to Drive. */
    @Query("SELECT * FROM notes WHERE isDirty = 1")
    suspend fun getDirtyNotes(): List<NoteEntity>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    /** Soft delete — marks isDeleted so the sync worker can upload a tombstone to Drive. */
    @Query("UPDATE notes SET isDeleted = 1, isDirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    /** Batch-fetch notes by ID list; used by SearchRepository to inflate FTS results. */
    @Query("SELECT * FROM notes WHERE id IN (:ids) AND isDeleted = 0")
    suspend fun getByIds(ids: List<String>): List<NoteEntity>

    /** Called by SyncWorker after a note is successfully uploaded to Drive. */
    @Query("UPDATE notes SET isDirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)

    /** Observe soft-deleted notes for Recycle Bin. */
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeleted(): Flow<List<NoteEntity>>

    /** Permanently removes a note row from the database. */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentDeleteById(id: String)

    /** Returns IDs of soft-deleted notes older than [cutoffMs] (for FTS cleanup before bulk delete). */
    @Query("SELECT id FROM notes WHERE isDeleted = 1 AND updatedAt < :cutoffMs")
    suspend fun getDeletedNoteIdsOlderThan(cutoffMs: Long): List<String>

    /** Bulk-removes soft-deleted notes whose deletion timestamp is older than [cutoffMs]. */
    @Query("DELETE FROM notes WHERE isDeleted = 1 AND updatedAt < :cutoffMs")
    suspend fun purgeOldDeletedNotes(cutoffMs: Long)
}
