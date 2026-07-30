package com.yourname.simplenotes.data.importer

import com.google.gson.Gson
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.NoteMetadata
import com.yourname.simplenotes.util.toEditorHtml
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.zip.ZipInputStream

enum class ArchiveFormat { BACKUP, SPLIT_TEXT, UNKNOWN }

data class ArchiveImportResult(
    val notes: List<Note>,
    val skippedTrashed: Int = 0,
    val format: ArchiveFormat = ArchiveFormat.UNKNOWN
)

/**
 * Parses two known "export from another notes app" archive shapes:
 * - EasyNotes native `.backup`: a zip of per-note zip entries (`backup_<creation>_<lastModification>.zip`),
 *   each wrapping a `note.json` with `creation`/`lastModification` epoch-millis timestamps.
 * - a "split" `.zip` of flat `.txt` files named `yyyyMMdd_Category_Title.txt`.
 *
 * Both are plain zip containers, so format is detected per-entry rather than upfront.
 */
object ArchiveNoteImporter {

    private val gson = Gson()
    private val splitNameRegex = Regex("""^(\d{8})_([^_]+)_(.+)$""")

    private data class EasyNoteJson(
        val title: String? = null,
        val content: String? = null,
        val creation: Long = 0L,
        val lastModification: Long = 0L,
        val trashed: Boolean = false,
        val favorite: Int = 0,
        val tags: String? = null,
        val baseCategory: BaseCategoryJson? = null
    )

    /** EasyNotes' default "uncategorized" bucket has this id and no [name]. */
    private data class BaseCategoryJson(
        val id: Long = 0L,
        val name: String? = null
    )

    private sealed class ParsedEasyNote {
        data class Imported(val note: Note) : ParsedEasyNote()
        object Trashed : ParsedEasyNote()
        object Invalid : ParsedEasyNote()
    }

    /**
     * [resolveFolder] maps a category name — from the split-text filename, or from a
     * `.backup` note's `baseCategory.name` — to a folder id, creating the folder if it
     * doesn't already exist.
     */
    suspend fun import(
        input: InputStream,
        resolveFolder: suspend (String) -> String?
    ): ArchiveImportResult {
        val notes = mutableListOf<Note>()
        var skippedTrashed = 0
        var format = ArchiveFormat.UNKNOWN

        ZipInputStream(input, Charsets.ISO_8859_1).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = fixEntryName(entry.name).substringAfterLast('/')
                    when {
                        name.endsWith(".zip", ignoreCase = true) -> {
                            format = ArchiveFormat.BACKUP
                            when (val parsed = parseEasyNoteEntry(zis.readBytes(), resolveFolder)) {
                                is ParsedEasyNote.Imported -> notes += parsed.note
                                ParsedEasyNote.Trashed -> skippedTrashed++
                                ParsedEasyNote.Invalid -> Unit
                            }
                        }
                        name.endsWith(".txt", ignoreCase = true) -> {
                            if (format == ArchiveFormat.UNKNOWN) format = ArchiveFormat.SPLIT_TEXT
                            val text = zis.readBytes().toString(Charsets.UTF_8)
                            parseSplitTextEntry(name, text, resolveFolder)?.let { notes += it }
                        }
                        else -> Unit // ignore unrelated entries (e.g. a top-level folder marker)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return ArchiveImportResult(notes = notes, skippedTrashed = skippedTrashed, format = format)
    }

    /**
     * Many of these archives are written with entry names UTF-8 encoded but without the
     * zip "language encoding" flag set, so the default decode mangles anything non-ASCII
     * into Latin-1/CP437 mojibake (every byte 0-255 maps to one char in that same range).
     * Re-reading those raw bytes (recovered losslessly via ISO-8859-1) as UTF-8 fixes it.
     *
     * If the entry DID have the UTF-8 flag set, [raw] is already correctly decoded and may
     * contain characters outside Latin-1 (e.g. Vietnamese/CJK) — encoding those through
     * ISO-8859-1 would lossily replace them with '?', so skip the fix in that case.
     */
    private fun fixEntryName(raw: String): String {
        if (raw.any { it.code > 0xFF }) return raw
        val fixed = String(raw.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        val fixedReplacements = fixed.count { it == '�' }
        val rawReplacements = raw.count { it == '�' }
        return if (fixedReplacements > rawReplacements) raw else fixed
    }

    private suspend fun parseEasyNoteEntry(
        nestedZipBytes: ByteArray,
        resolveFolder: suspend (String) -> String?
    ): ParsedEasyNote {
        val json = runCatching {
            ZipInputStream(ByteArrayInputStream(nestedZipBytes)).use { nested ->
                var e = nested.nextEntry
                while (e != null) {
                    if (!e.isDirectory && e.name.substringAfterLast('/') == "note.json") {
                        return@use nested.readBytes().toString(Charsets.UTF_8)
                    }
                    nested.closeEntry()
                    e = nested.nextEntry
                }
                null
            }
        }.getOrNull() ?: return ParsedEasyNote.Invalid

        val parsed = runCatching { gson.fromJson(json, EasyNoteJson::class.java) }.getOrNull()
            ?: return ParsedEasyNote.Invalid
        if (parsed.trashed) return ParsedEasyNote.Trashed

        val content = parsed.content.orEmpty()
        if (content.isBlank() && parsed.title.isNullOrBlank()) return ParsedEasyNote.Invalid

        val title = parsed.title?.takeIf { it.isNotBlank() }
            ?: content.lineSequence().firstOrNull { it.isNotBlank() }?.take(80)
            ?: "Ghi chú đã nhập"
        val createdAt = parsed.creation.takeIf { it > 0 } ?: System.currentTimeMillis()
        val updatedAt = parsed.lastModification.takeIf { it > 0 } ?: createdAt
        val labels = parsed.tags.orEmpty()
            .split(",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val folderId = parsed.baseCategory?.name?.takeIf { it.isNotBlank() }?.let { resolveFolder(it) }

        return ParsedEasyNote.Imported(
            Note(
                id = UUID.randomUUID().toString(),
                title = title,
                contentBlocks = listOf(ContentBlock.Text(text = content, htmlContent = content.toEditorHtml())),
                folderId = folderId,
                isPinned = parsed.favorite != 0,
                labels = labels,
                createdAt = createdAt,
                updatedAt = updatedAt,
                contentUpdatedAt = updatedAt,
                metadata = NoteMetadata.from(content),
                isDirty = true
            )
        )
    }

    private suspend fun parseSplitTextEntry(
        fileName: String,
        rawText: String,
        resolveFolder: suspend (String) -> String?
    ): Note? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        // Caller only reaches here for entries matched by endsWith(".txt", ignoreCase = true).
        val baseName = fileName.dropLast(4)
        val match = splitNameRegex.find(baseName)
        val dateToken = match?.groupValues?.get(1)
        val categoryName = match?.groupValues?.get(2)
        val titleRaw = match?.groupValues?.get(3) ?: baseName

        val timestamp = dateToken?.let { parseYyyyMMdd(it) } ?: System.currentTimeMillis()
        val folderId = categoryName?.let { resolveFolder(it) }
        val title = titleRaw.trim().takeIf { it.isNotBlank() } ?: "Ghi chú đã nhập"

        return Note(
            id = UUID.randomUUID().toString(),
            title = title,
            contentBlocks = listOf(ContentBlock.Text(text = text, htmlContent = text.toEditorHtml())),
            folderId = folderId,
            createdAt = timestamp,
            updatedAt = timestamp,
            contentUpdatedAt = timestamp,
            metadata = NoteMetadata.from(text),
            isDirty = true
        )
    }

    private fun parseYyyyMMdd(token: String): Long = runCatching {
        val year = token.substring(0, 4).toInt()
        val month = token.substring(4, 6).toInt()
        val day = token.substring(6, 8).toInt()
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())
}
