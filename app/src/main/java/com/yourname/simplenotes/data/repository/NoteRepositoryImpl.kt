package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.data.local.NoteDao
import com.yourname.simplenotes.data.local.NoteSearchDao
import com.yourname.simplenotes.data.local.NoteSearchEntity
import com.yourname.simplenotes.data.local.NoteSyncPrefs
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
    private val noteSearchDao: NoteSearchDao,
    private val noteSyncPrefs: NoteSyncPrefs
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

    override suspend fun saveAll(notes: List<Note>) {
        if (notes.isEmpty()) return
        dao.upsertAll(notes.map { it.toEntity() })
        notes.forEach { note ->
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
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id)
        noteSearchDao.deleteByNoteId(id)  // Remove from FTS index
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun deleteAll(ids: List<String>) {
        if (ids.isEmpty()) return
        dao.softDeleteAll(ids)
        ids.forEach { noteSearchDao.deleteByNoteId(it) }
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun getDirtyNotes(): List<Note> =
        dao.getDirtyNotes().map { it.toDomain() }

    override suspend fun markClean(id: String) =
        dao.markClean(id)

    override fun observeDeleted(): Flow<List<Note>> =
        dao.observeDeleted().map { list -> list.map { it.toDomain() } }

    override suspend fun restore(id: String) {
        val note = dao.getByIdIncludeDeleted(id)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        val restored = note.copy(isDeleted = false, isDirty = true, updatedAt = now)
        dao.upsert(restored.toEntity())
        noteSearchDao.upsertIndex(
            NoteSearchEntity(
                noteId = restored.id,
                title = restored.title,
                content = restored.contentBlocks
                    .filterIsInstance<ContentBlock.Text>()
                    .joinToString(" ") { it.text }
            )
        )
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun permanentDelete(id: String) {
        // Remember this ID so SyncWorker also deletes the note (and its Drive file) remotely —
        // otherwise the next sync's download step sees "local == null, still in Drive index"
        // and re-downloads the note, resurrecting it.
        noteSyncPrefs.addDeletedId(id)
        dao.permanentDeleteById(id)
        noteSearchDao.deleteByNoteId(id)
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun permanentDeleteAll(ids: List<String>) {
        if (ids.isEmpty()) return
        ids.forEach { id ->
            noteSyncPrefs.addDeletedId(id)
            dao.permanentDeleteById(id)
            noteSearchDao.deleteByNoteId(id)
        }
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun purgeOldDeleted(cutoffMs: Long) {
        val ids = dao.getDeletedNoteIdsOlderThan(cutoffMs)
        ids.forEach {
            noteSyncPrefs.addDeletedId(it)
            noteSearchDao.deleteByNoteId(it)
        }
        dao.purgeOldDeletedNotes(cutoffMs)
    }

    override suspend fun getPendingDeletedIds(): Set<String> = noteSyncPrefs.pendingDeletedIds

    override suspend fun clearPendingDeletedIds() = noteSyncPrefs.clear()

    override suspend fun getCleanDeletedNoteIds(): List<String> = dao.getCleanDeletedNoteIds()

    override suspend fun purgeRemotelyDeleted(ids: List<String>) {
        ids.forEach { id ->
            dao.permanentDeleteById(id)
            noteSearchDao.deleteByNoteId(id)
        }
    }

    override suspend fun clearOrphanedFolderRefs(validFolderIds: Set<String>) =
        dao.clearOrphanedFolderRefs(validFolderIds.toList())

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
