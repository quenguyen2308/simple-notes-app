package com.yourname.simplenotes

import com.yourname.simplenotes.data.importer.ArchiveFormat
import com.yourname.simplenotes.data.importer.ArchiveNoteImporter
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * [ArchiveNoteImporter] parses two real "export from another notes app" shapes:
 * EasyNotes' native `.backup` (nested per-note zips) and a "split" `.zip` of flat
 * `yyyyMMdd_Category_Title.txt` files. These tests build small synthetic archives
 * in-memory (not the user's real sample files, which contain personal data) that
 * mirror the same structure.
 */
class ArchiveNoteImporterTest {

    @Before
    fun stubAndroidHtml() {
        // toEditorHtml() calls android.text.Html.escapeHtml, which the Android stub jar
        // throws on outside Robolectric — stub it to plain passthrough for this JVM test.
        mockkStatic(android.text.Html::class)
        every { android.text.Html.escapeHtml(any()) } answers { firstArg<CharSequence>().toString() }
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `parses EasyNotes backup format, preserving timestamps and skipping trashed notes`() = runBlocking {
        val activeNoteJson = """
            {"title":"Ghi chú test","content":"Nội dung tiếng Việt","creation":1700000000000,
             "lastModification":1710000000000,"trashed":false,"favorite":1,"tags":"work,personal",
             "baseCategory":{"id":1745751486175,"name":"Bank"}}
        """.trimIndent()
        val trashedNoteJson = """
            {"title":"Đã xóa","content":"bỏ qua","creation":1600000000000,
             "lastModification":1600000000001,"trashed":true}
        """.trimIndent()

        val outer = zipOf(
            "backup_1700000000000_1710000000000.zip" to zipOf("note.json" to activeNoteJson.toByteArray(Charsets.UTF_8)),
            "backup_1600000000000_1600000000001.zip" to zipOf("note.json" to trashedNoteJson.toByteArray(Charsets.UTF_8))
        )

        val result = ArchiveNoteImporter.import(ByteArrayInputStream(outer)) { name -> "folder-$name" }

        assertEquals(ArchiveFormat.BACKUP, result.format)
        assertEquals(1, result.notes.size)
        assertEquals(1, result.skippedTrashed)

        val note = result.notes.single()
        assertEquals("Ghi chú test", note.title)
        assertEquals("Nội dung tiếng Việt", note.content)
        assertEquals(1700000000000L, note.createdAt)
        assertEquals(1710000000000L, note.updatedAt)
        assertTrue(note.isPinned)
        assertEquals(listOf("work", "personal"), note.labels)
        assertEquals("folder-Bank", note.folderId)
    }

    @Test
    fun `backup notes with no baseCategory name are left unfiled`() = runBlocking {
        // EasyNotes' default "uncategorized" bucket has an id but no name field at all.
        val uncategorizedJson = """
            {"title":"Không thư mục","content":"nội dung","creation":1700000000000,
             "lastModification":1700000000001,"baseCategory":{"id":100000001}}
        """.trimIndent()
        val outer = zipOf(
            "backup_1700000000000_1700000000001.zip" to zipOf("note.json" to uncategorizedJson.toByteArray(Charsets.UTF_8))
        )

        val result = ArchiveNoteImporter.import(ByteArrayInputStream(outer)) { name -> "folder-$name" }

        assertEquals(1, result.notes.size)
        assertEquals(null, result.notes.single().folderId)
    }

    @Test
    fun `parses split text zip, mapping category prefix to a folder and date to timestamp`() = runBlocking {
        val outer = zipOf(
            "20250427_Bank_HSBC.txt" to "số tài khoản 123".toByteArray(Charsets.UTF_8),
            "20250426_Lulu_Ngũ đại (五大).txt" to "nội dung có dấu".toByteArray(Charsets.UTF_8)
        )

        val resolvedFolders = mutableMapOf<String, String>()
        val result = ArchiveNoteImporter.import(ByteArrayInputStream(outer)) { name ->
            resolvedFolders.getOrPut(name) { "folder-$name" }
        }

        assertEquals(ArchiveFormat.SPLIT_TEXT, result.format)
        assertEquals(2, result.notes.size)

        val hsbc = result.notes.first { it.title == "HSBC" }
        assertEquals("folder-Bank", hsbc.folderId)
        assertEquals("số tài khoản 123", hsbc.content)
        val expectedTimestamp = LocalDate.of(2025, 4, 27)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedTimestamp, hsbc.createdAt)
        assertEquals(expectedTimestamp, hsbc.updatedAt)

        // Also verifies the mojibake fix-up for non-ASCII filenames (Vietnamese + CJK).
        val luluNote = result.notes.first { it.title == "Ngũ đại (五大)" }
        assertEquals("folder-Lulu", luluNote.folderId)
    }
}
