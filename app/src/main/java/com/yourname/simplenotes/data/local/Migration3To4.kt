package com.yourname.simplenotes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration from schema v3 → v4.
 *
 * Changes:
 * - Replaces plain `content TEXT` with `content_blocks_json TEXT` (rich content)
 * - Adds: folder_id, background_color, is_pinned, labels_json,
 *         word_count, character_count, metadata_json
 * - Existing note content is wrapped in a single Text ContentBlock JSON array
 *
 * The app uses [fallbackToDestructiveMigration] as a safety net, so if this
 * migration fails, the database is wiped and re-populated from Google Drive.
 */
val MIGRATION_3_TO_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create new notes table with v4 schema
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notes_v4` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `content_blocks_json` TEXT NOT NULL,
                `folder_id` TEXT,
                `background_color` INTEGER NOT NULL DEFAULT ${0xFFFFFFFF.toInt()},
                `is_pinned` INTEGER NOT NULL DEFAULT 0,
                `labels_json` TEXT NOT NULL DEFAULT '[]',
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDirty` INTEGER NOT NULL DEFAULT 1,
                `isDeleted` INTEGER NOT NULL DEFAULT 0,
                `isLocked` INTEGER NOT NULL DEFAULT 0,
                `pinHash` TEXT
            )
            """.trimIndent()
        )

        // 2. Migrate existing notes — wrap plain text in a single Text ContentBlock
        //    JSON: [{"type":"text","text":"<content>","htmlContent":"<content>"}]
        database.execSQL(
            """
            INSERT INTO notes_v4 (
                id, title, content_blocks_json, folder_id,
                background_color, is_pinned, labels_json, metadata_json,
                createdAt, updatedAt, isDirty, isDeleted, isLocked, pinHash
            )
            SELECT
                id,
                title,
                '[{"type":"text","text":' || json_quote(COALESCE(content, '')) ||
                    ',"htmlContent":' || json_quote(COALESCE(content, '')) || '}]',
                NULL,
                ${0xFFFFFFFF.toInt()},
                0,
                '[]',
                '{}',
                createdAt,
                updatedAt,
                isDirty,
                isDeleted,
                isLocked,
                pinHash
            FROM notes
            """.trimIndent()
        )

        // 3. Drop old table and rename new one
        database.execSQL("DROP TABLE IF EXISTS `notes`")
        database.execSQL("ALTER TABLE `notes_v4` RENAME TO `notes`")
    }
}
