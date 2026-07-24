package com.yourname.simplenotes

import com.yourname.simplenotes.data.local.NoteDao
import com.yourname.simplenotes.data.local.NoteSearchDao
import com.yourname.simplenotes.data.local.NoteSyncPrefs
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.data.repository.NoteRepositoryImpl
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.sync.SyncScheduler
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NoteRepositoryTest {

    private val dao: NoteDao = mockk(relaxed = true)
    private val syncScheduler: SyncScheduler = mockk(relaxed = true)
    private val noteSearchDao: NoteSearchDao = mockk(relaxed = true)
    private val noteSyncPrefs: NoteSyncPrefs = mockk(relaxed = true)
    private val repository = NoteRepositoryImpl(dao, syncScheduler, noteSearchDao, noteSyncPrefs)

    @Test
    fun save_upserts_to_dao_and_triggers_sync() = runTest {
        val note = Note(
            id = "id",
            title = "T",
            contentBlocks = listOf(ContentBlock.Text(text = "C")),
            createdAt = 1000L,
            updatedAt = 2000L,
            isDirty = true,
            isDeleted = false
        )
        repository.save(note)
        coVerify { dao.upsert(any()) }
        verify { syncScheduler.triggerImmediateSync() }
    }

    @Test
    fun delete_soft_deletes_and_triggers_sync() = runTest {
        repository.delete("id-1")
        coVerify { dao.softDelete("id-1", any()) }
        verify { syncScheduler.triggerImmediateSync() }
    }

    // Regression test: permanentDelete must record the ID for SyncWorker to delete on Drive too,
    // otherwise the next sync's download step re-downloads the note from Drive and resurrects it.
    @Test
    fun permanentDelete_records_pending_drive_deletion_and_triggers_sync() = runTest {
        repository.permanentDelete("id-1")
        verify { noteSyncPrefs.addDeletedId("id-1") }
        coVerify { dao.permanentDeleteById("id-1") }
        verify { syncScheduler.triggerImmediateSync() }
    }
}
