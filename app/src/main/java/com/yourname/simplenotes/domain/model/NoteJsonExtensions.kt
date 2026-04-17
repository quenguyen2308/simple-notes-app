package com.yourname.simplenotes.domain.model

import com.yourname.simplenotes.data.local.entities.ContentBlock
import org.json.JSONObject

/**
 * Serializes a note to JSON for Drive storage.
 * isDirty is app-local and not persisted.
 *
 * TODO (Phase 8): Extend to include `contentBlocksJson` so rich content (images,
 *  checklists, HTML formatting) survives Drive round-trips. For now, only plain text
 *  is synced for backward compatibility with existing Drive files.
 */
fun Note.toJson(): String = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("content", content)     // computed plain-text extract from TextBlocks
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("isDeleted", isDeleted)
}.toString()

/**
 * Deserializes a note from Drive JSON.
 * Plain `content` from Drive is wrapped in a single Text ContentBlock.
 * isDirty defaults to false — Drive data is already synced.
 */
fun Note.Companion.fromJson(json: String): Note {
    val obj = JSONObject(json)
    val plainContent = obj.optString("content", "")
    return Note(
        id = obj.getString("id"),
        title = obj.optString("title", ""),
        contentBlocks = listOf(ContentBlock.Text(text = plainContent, htmlContent = plainContent)),
        createdAt = obj.getLong("createdAt"),
        updatedAt = obj.getLong("updatedAt"),
        isDirty = false,
        isDeleted = obj.optBoolean("isDeleted", false)
    )
}
