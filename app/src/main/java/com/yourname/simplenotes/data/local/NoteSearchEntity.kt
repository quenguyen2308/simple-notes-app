package com.yourname.simplenotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/** FTS4 virtual table entity for full-text search across note titles and body text. */
@Entity(tableName = "note_fts")
@Fts4
data class NoteSearchEntity(
    @ColumnInfo(name = "note_id") val noteId: String,
    val title: String,
    val content: String  // plain text extracted from ContentBlock.Text blocks
)
