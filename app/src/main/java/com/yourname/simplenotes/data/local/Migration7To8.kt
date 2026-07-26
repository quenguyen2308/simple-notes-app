package com.yourname.simplenotes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration from schema v7 → v8.
 *
 * Changes:
 * - Adds notes.contentUpdatedAt (epochMs): a "date modified" shown to the user that only bumps
 *   on real content edits (title/body/checklist), separate from updatedAt which is the sync
 *   clock and must bump on every save (color, pin, lock, folder…) for Drive's last-write-wins
 *   conflict resolution to work. Existing rows default to their current updatedAt.
 */
val MIGRATION_7_TO_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE `notes` ADD COLUMN `contentUpdatedAt` INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL("UPDATE `notes` SET `contentUpdatedAt` = `updatedAt`")
    }
}
