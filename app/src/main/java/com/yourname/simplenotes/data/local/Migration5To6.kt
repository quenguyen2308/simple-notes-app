package com.yourname.simplenotes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration from schema v5 → v6.
 *
 * Changes:
 * - Creates note_fts: FTS4 virtual table (note_id, title, content) for full-text search
 * - Pre-populates FTS with existing non-deleted note titles (content re-indexed on next save)
 * - Creates search_history: stores up to 20 recent search queries
 */
val MIGRATION_5_TO_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // FTS4 virtual table — must match NoteSearchEntity column names exactly
        database.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `note_fts` " +
                "USING fts4(`note_id` TEXT, `title` TEXT, `content` TEXT)"
        )
        // Pre-populate with titles of existing notes; full content is indexed on next note save
        database.execSQL(
            "INSERT INTO note_fts (note_id, title, content) " +
                "SELECT id, title, '' FROM notes WHERE isDeleted = 0"
        )
        // Search history table
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `search_history` " +
                "(`query` TEXT NOT NULL PRIMARY KEY, `timestamp` INTEGER NOT NULL DEFAULT 0)"
        )
    }
}
