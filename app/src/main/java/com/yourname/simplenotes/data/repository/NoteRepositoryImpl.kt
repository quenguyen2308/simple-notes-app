package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.data.local.NoteDao
import com.yourname.simplenotes.data.local.NoteSearchDao
import com.yourname.simplenotes.data.local.NoteSearchEntity
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.data.local.toDomain
import com.yourname.simplenotes.data.local.toEntity
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val dao: NoteDao,
    private val syncScheduler: SyncScheduler,
    private val noteSearchDao: NoteSearchDao
) : NoteRepository {

    override fun observeAll(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Note? =
        dao.getById(id)?.toDomain()

    override suspend fun getByIdIncludeDeleted(id: String): Note? =
        dao.getByIdIncludeDeleted(id)?.toDomain()

    override suspend fun save(note: Note) {
        dao.upsert(note.toEntity())
        // Update FTS index with plain text extracted from content blocks
        noteSearchDao.upsertIndex(
            NoteSearchEntity(
                noteId = note.id,
                title = note.title,
                content = note.contentBlocks
                    .filterIsInstance<ContentBlock.Text>()
                    .joinToString(" ") { it.text }
            )
        )
        // Trigger near real-time sync after every save
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id)
        noteSearchDao.deleteByNoteId(id)  // Remove from FTS index
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun getDirtyNotes(): List<Note> =
        dao.getDirtyNotes().map { it.toDomain() }

    override suspend fun markClean(id: String) =
        dao.markClean(id)

    override suspend fun upsertFromRemote(notes: List<Note>) {
        val entities = notes.map { it.toEntity().copy(isDirty = false) }
        dao.upsertAll(entities)
        notes.forEach { note ->
            if (note.isDeleted) {
                noteSearchDao.deleteByNoteId(note.id)
            } else {
                noteSearchDao.upsertIndex(
                    NoteSearchEntity(
                        noteId = note.id,
                        title = note.title,
                        content = note.contentBlocks
                            .filterIsInstance<ContentBlock.Text>()
                            .joinToString(" ") { it.text }
                    )
                )
            }
        }
    }
}
