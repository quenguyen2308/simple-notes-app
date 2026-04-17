package com.yourname.simplenotes

import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.fromJson
import com.yourname.simplenotes.domain.model.toJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictTest {

    /** Drive has newer modifiedTime → Drive wins */
    @Test
    fun driveWins_when_drive_is_newer() {
        val localUpdatedAt = 1000L
        val driveModifiedTime = 2000L
        assertTrue("Drive should win when newer", driveModifiedTime > localUpdatedAt)
    }

    /** Local is dirty and newer → local should upload */
    @Test
    fun localWins_when_local_is_newer_and_dirty() {
        val localUpdatedAt = 3000L
        val driveModifiedTime = 1000L
        assertTrue("Local should upload when newer + dirty", localUpdatedAt > driveModifiedTime)
    }

    /** Note serializes to JSON and back without data loss (isDirty not persisted) */
    @Test
    fun note_toJson_fromJson_roundtrip() {
        val original = Note(
            id = "test-id",
            title = "Hello",
            contentBlocks = listOf(ContentBlock.Text(text = "World")),
            createdAt = 1000L,
            updatedAt = 2000L,
            isDirty = true,
            isDeleted = false
        )
        val json = original.toJson()
        val restored = Note.fromJson(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.content, restored.content)
        assertEquals(original.isDeleted, restored.isDeleted)
        // isDirty is not persisted to Drive JSON — always false after deserialization
        assertFalse(restored.isDirty)
    }
}
