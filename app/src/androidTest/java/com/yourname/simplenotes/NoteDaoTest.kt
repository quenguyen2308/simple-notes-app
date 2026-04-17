package com.yourname.simplenotes

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourname.simplenotes.data.local.NoteDao
import com.yourname.simplenotes.data.local.NoteDatabase
import com.yourname.simplenotes.data.local.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var db: NoteDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NoteDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.noteDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun upsert_and_observe() = runTest {
        val note = NoteEntity("id-1", "Title", "Body", 1000L, 2000L, isDirty = true)
        dao.upsert(note)
        val result = dao.observeAll().first()
        assertEquals(1, result.size)
        assertEquals("Title", result[0].title)
    }

    @Test
    fun softDelete_hides_from_observeAll() = runTest {
        val note = NoteEntity("id-1", "Title", "Body", 1000L, 2000L)
        dao.upsert(note)
        dao.softDelete("id-1")
        val result = dao.observeAll().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDirtyNotes_returns_only_dirty() = runTest {
        dao.upsert(NoteEntity("id-1", "A", "", 1000L, 2000L, isDirty = true))
        dao.upsert(NoteEntity("id-2", "B", "", 1000L, 2000L, isDirty = false))
        val dirty = dao.getDirtyNotes()
        assertEquals(1, dirty.size)
        assertEquals("id-1", dirty[0].id)
    }

    @Test
    fun markClean_clears_dirty_flag() = runTest {
        dao.upsert(NoteEntity("id-1", "A", "", 1000L, 2000L, isDirty = true))
        dao.markClean("id-1")
        val dirty = dao.getDirtyNotes()
        assertTrue(dirty.isEmpty())
    }
}
