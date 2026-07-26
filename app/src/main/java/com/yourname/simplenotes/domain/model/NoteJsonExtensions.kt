package com.yourname.simplenotes.domain.model

import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.data.local.entities.toContentBlocks
import com.yourname.simplenotes.data.local.entities.toJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes a note to JSON for Drive storage.
 * Includes all fields needed for full fidelity sync across devices.
 * isDirty and isLocked/pinHash are device-local and not included.
 */
fun Note.toJson(): String = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("content", content)                          // plain-text extract (for search/compat)
    put("contentBlocksJson", contentBlocks.toJson()) // rich content
    put("folderId", folderId ?: JSONObject.NULL)
    put("isPinned", isPinned)
    put("backgroundColor", backgroundColor)
    put("labels", JSONArray(labels))
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("contentUpdatedAt", contentUpdatedAt)
    put("isDeleted", isDeleted)
}.toString()

/**
 * Deserializes a note from Drive JSON.
 * Backward-compatible: old files without contentBlocksJson fall back to plain text.
 * isDirty = false — data from Drive is already synced.
 * isLocked/pinHash are device-local and default to false/null.
 */
fun Note.Companion.fromJson(json: String): Note {
    val obj = JSONObject(json)

    val contentBlocks = if (obj.has("contentBlocksJson")) {
        runCatching { obj.getString("contentBlocksJson").toContentBlocks() }
            .getOrElse {
                val plain = obj.optString("content", "")
                listOf(ContentBlock.Text(text = plain, htmlContent = plain))
            }
    } else {
        val plain = obj.optString("content", "")
        listOf(ContentBlock.Text(text = plain, htmlContent = plain))
    }

    val labelsArray = obj.optJSONArray("labels")
    val labels = if (labelsArray != null) {
        (0 until labelsArray.length()).map { labelsArray.getString(it) }
    } else emptyList()

    return Note(
        id             = obj.getString("id"),
        title          = obj.optString("title", ""),
        contentBlocks  = contentBlocks,
        folderId       = obj.optString("folderId").takeIf { it.isNotEmpty() && it != "null" },
        isPinned       = obj.optBoolean("isPinned", false),
        backgroundColor = obj.optInt("backgroundColor", 0xFFFFFFFF.toInt()),
        labels         = labels,
        createdAt      = obj.getLong("createdAt"),
        updatedAt      = obj.getLong("updatedAt"),
        // Backward-compatible: notes uploaded before this field existed fall back to updatedAt.
        contentUpdatedAt = obj.optLong("contentUpdatedAt", obj.getLong("updatedAt")),
        isDirty        = false,
        isDeleted      = obj.optBoolean("isDeleted", false)
    )
}
