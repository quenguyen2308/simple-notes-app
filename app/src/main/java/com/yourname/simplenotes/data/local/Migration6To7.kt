package com.yourname.simplenotes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration from schema v6 → v7.
 *
 * Changes:
 * - Adds categories.updatedAt (epochMs), used for last-write-wins conflict resolution
 *   when merging local categories with Drive during SyncWorker.syncCategories(). Existing
 *   rows default to 0 so a genuinely newer remote copy still wins on first sync after upgrade.
 */
val MIGRATION_6_TO_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE `categories` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
