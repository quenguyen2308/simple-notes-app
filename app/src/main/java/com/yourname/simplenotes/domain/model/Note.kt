package com.yourname.simplenotes.domain.model

import com.yourname.simplenotes.data.local.entities.ContentBlock

/**
 * Core domain model for a note (v2 — rich content).
 *
 * [content] is a computed plain-text extract from [contentBlocks], kept for
 * backward compatibility with Google Drive sync serialization.
 */
data class Note(
    val id: String,
    val title: String,
    /** Ordered list of rich content blocks (text, image, checklist, drawing). */
    val contentBlocks: List<ContentBlock>,
    val folderId: String? = null,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val isPinned: Boolean = false,
    val labels: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val metadata: NoteMetadata = NoteMetadata.EMPTY,
    val isDirty: Boolean = true,
    val isDeleted: Boolean = false,
    val isLocked: Boolean = false,
    val pinHash: String? = null
) {
    /**
     * Plain-text extract from all TextBlocks joined by newline.
     * Used by Drive sync serialization and full-text search indexing.
     */
    val content: String
        get() = contentBlocks
            .filterIsInstance<ContentBlock.Text>()
            .joinToString("\n") { it.text }

    companion object  // Reserved for extension functions (e.g. Note.fromJson())
}
