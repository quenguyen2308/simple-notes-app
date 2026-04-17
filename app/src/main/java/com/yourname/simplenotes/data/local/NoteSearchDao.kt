package com.yourname.simplenotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * DAO for the FTS4 virtual table [NoteSearchEntity].
 *
 * Uses abstract class (not interface) so [upsertIndex] can be a concrete @Transaction method
 * that delegates to the abstract [insert] and [deleteByNoteId].
 */
@Dao
abstract class NoteSearchDao {

    /** Returns IDs of notes matching [query] using FTS MATCH syntax (supports prefix wildcard). */
    @Query("SELECT note_id FROM note_fts WHERE note_fts MATCH :query")
    abstract suspend fun searchNoteIds(query: String): List<String>

    @Insert
    abstract suspend fun insert(entity: NoteSearchEntity)

    /** Removes the FTS index entry for [noteId]. */
    @Query("DELETE FROM note_fts WHERE note_id = :noteId")
    abstract suspend fun deleteByNoteId(noteId: String)

    @Query("DELETE FROM note_fts")
    abstract suspend fun clearAll()

    /** Delete-then-insert to avoid duplicate FTS rows (FTS4 has no REPLACE/UPSERT support). */
    @Transaction
    open suspend fun upsertIndex(entity: NoteSearchEntity) {
        deleteByNoteId(entity.noteId)
        insert(entity)
    }
}
