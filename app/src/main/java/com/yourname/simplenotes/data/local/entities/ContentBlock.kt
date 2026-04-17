package com.yourname.simplenotes.data.local.entities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─── Domain sealed class ────────────────────────────────────────────────────

/** Rich content block inside a note. Each note has an ordered list of these. */
sealed class ContentBlock {

    /**
     * A rich text segment. [htmlContent] is the Compose Rich Editor HTML
     * representation; [text] is the plain-text extract used for Drive sync and search.
     */
    data class Text(
        val text: String,
        val htmlContent: String = text,
        val formatting: TextFormatting = TextFormatting()
    ) : ContentBlock()

    /** Embedded image stored as a content URI or file URI. */
    data class Image(
        val uri: String,
        val caption: String = ""
    ) : ContentBlock()

    /** Interactive checklist block. */
    data class Checklist(
        val items: List<ChecklistItem>
    ) : ContentBlock()

    /** Drawing/sketch stored as a base-64 PNG. */
    data class Drawing(
        val dataBase64: String
    ) : ContentBlock()
}

// ─── Inline supporting types ─────────────────────────────────────────────────

/** Text span formatting applied to a [ContentBlock.Text]. */
data class TextFormatting(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: Int? = null,
    val backgroundColor: Int? = null,
    val fontSize: Int = 16
)

// ─── JSON transfer object (flat, discriminator-based) ────────────────────────

/**
 * Flat JSON representation used for Gson persistence.
 * The [type] field drives deserialization back to the sealed class.
 */
data class ContentBlockJson(
    val type: String,                         // "text" | "image" | "checklist" | "drawing"
    // Text block
    val text: String? = null,
    val htmlContent: String? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strikethrough: Boolean? = null,
    val color: Int? = null,
    val bgColor: Int? = null,
    val fontSize: Int? = null,
    // Image block
    val uri: String? = null,
    val caption: String? = null,
    // Checklist block (items serialized as nested JSON)
    val items: List<ChecklistItemJson>? = null,
    // Drawing block
    val dataBase64: String? = null
)

data class ChecklistItemJson(
    val id: String,
    val text: String,
    val isCompleted: Boolean,
    val order: Int
)

// ─── Serialization helpers ───────────────────────────────────────────────────

private val gson = Gson()
private val listType = object : TypeToken<List<ContentBlockJson>>() {}.type

/** Serializes a list of [ContentBlock] to a JSON string for Room storage. */
fun List<ContentBlock>.toJson(): String {
    val jsonList = map { block ->
        when (block) {
            is ContentBlock.Text -> ContentBlockJson(
                type = "text",
                text = block.text,
                htmlContent = block.htmlContent,
                bold = block.formatting.bold,
                italic = block.formatting.italic,
                underline = block.formatting.underline,
                strikethrough = block.formatting.strikethrough,
                color = block.formatting.color,
                bgColor = block.formatting.backgroundColor,
                fontSize = block.formatting.fontSize
            )
            is ContentBlock.Image -> ContentBlockJson(
                type = "image",
                uri = block.uri,
                caption = block.caption
            )
            is ContentBlock.Checklist -> ContentBlockJson(
                type = "checklist",
                items = block.items.map { it.toJson() }
            )
            is ContentBlock.Drawing -> ContentBlockJson(
                type = "drawing",
                dataBase64 = block.dataBase64
            )
        }
    }
    return gson.toJson(jsonList)
}

/** Deserializes a JSON string from Room back to a list of [ContentBlock]. */
fun String.toContentBlocks(): List<ContentBlock> {
    return try {
        val jsonList: List<ContentBlockJson> = gson.fromJson(this, listType)
        jsonList.mapNotNull { it.toDomain() }
    } catch (e: Exception) {
        // Fallback: treat raw string as a single plain-text block (migration safety)
        listOf(ContentBlock.Text(text = this, htmlContent = this))
    }
}

private fun ContentBlockJson.toDomain(): ContentBlock? {
    return when (type) {
        "text" -> ContentBlock.Text(
            text = text ?: "",
            htmlContent = htmlContent ?: text ?: "",
            formatting = TextFormatting(
                bold = bold ?: false,
                italic = italic ?: false,
                underline = underline ?: false,
                strikethrough = strikethrough ?: false,
                color = color,
                backgroundColor = bgColor,
                fontSize = fontSize ?: 16
            )
        )
        "image" -> if (uri != null) ContentBlock.Image(uri = uri, caption = caption ?: "") else null
        "checklist" -> ContentBlock.Checklist(items = items?.map { it.toDomain() } ?: emptyList())
        "drawing" -> if (dataBase64 != null) ContentBlock.Drawing(dataBase64 = dataBase64) else null
        else -> null
    }
}
