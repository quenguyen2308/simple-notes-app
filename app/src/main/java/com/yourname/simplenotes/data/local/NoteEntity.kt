package com.yourname.simplenotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a note (schema v4).
 *
 * Rich content is stored as a JSON array of ContentBlock objects in [contentBlocksJson].
 * Sync-related fields (isDirty, isDeleted) are preserved for Google Drive sync.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** JSON-serialized List<ContentBlock> — see ContentBlock.toJson() / toContentBlocks(). */
    @ColumnInfo(name = "content_blocks_json") val contentBlocksJson: String,
    /** Folder assignment (null = root / unorganised). Added in v4. */
    @ColumnInfo(name = "folder_id") val folderId: String? = null,
    /** ARGB background colour for the note card. Default = white. */
    @ColumnInfo(name = "background_color") val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    /** Whether this note appears pinned at the top of the list. */
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    /** JSON array of label IDs applied to this note (e.g. ["id1","id2"]). */
    @ColumnInfo(name = "labels_json") val labelsJson: String = "[]",
    /** JSON-serialized NoteMetadata (word count, character count, read time). */
    @ColumnInfo(name = "metadata_json") val metadataJson: String = "{}",
    val createdAt: Long,
    /** Sync clock for Drive last-write-wins — bumped on every save, cosmetic or not. */
    val updatedAt: Long,
    /** User-facing "date modified" — bumped only on real content edits. See [NoteEntity]. */
    val contentUpdatedAt: Long = updatedAt,
    /** true = note has local changes not yet uploaded to Drive. */
    val isDirty: Boolean = true,
    val isDeleted: Boolean = false,
    /** Device-local lock flag (not synced). */
    val isLocked: Boolean = false,
    /** bcrypt hash of the PIN (device-local, not synced). */
    val pinHash: String? = null
)
