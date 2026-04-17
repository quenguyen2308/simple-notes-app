package com.yourname.simplenotes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration from schema v4 → v5.
 *
 * Changes to the categories table:
 * - parent_id TEXT: enables nested folder hierarchy (max 3 levels deep)
 * - order INTEGER: enables drag-drop reordering within a parent
 */
val MIGRATION_4_TO_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN parent_id TEXT")
        database.execSQL("ALTER TABLE categories ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
    }
}
