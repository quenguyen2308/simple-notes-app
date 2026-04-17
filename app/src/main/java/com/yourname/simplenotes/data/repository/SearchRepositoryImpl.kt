package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.data.local.NoteDao
import com.yourname.simplenotes.data.local.NoteSearchDao
import com.yourname.simplenotes.data.local.SearchHistoryDao
import com.yourname.simplenotes.data.local.SearchHistoryEntity
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.data.local.toDomain
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.SearchFilters
import com.yourname.simplenotes.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SearchRepositoryImpl(
    private val noteSearchDao: NoteSearchDao,
    private val noteDao: NoteDao,
    private val historyDao: SearchHistoryDao
) : SearchRepository {

    override fun search(query: String, filters: SearchFilters): Flow<List<SearchResult>> = flow {
        if (query.isBlank()) { emit(emptyList()); return@flow }

        val ftsQuery = sanitizeFtsQuery(query)
        val matchedIds = try {
            noteSearchDao.searchNoteIds(ftsQuery)
        } catch (_: Exception) {
            emptyList()
        }

        if (matchedIds.isEmpty()) { emit(emptyList()); return@flow }

        val notes = noteDao.getByIds(matchedIds).map { it.toDomain() }
        val results = notes
            .filter { note -> filters.folderId == null || note.folderId == filters.folderId }
            .filter { note -> filters.colorArgb == null || note.backgroundColor == filters.colorArgb }
            .map { note -> SearchResult(note = note, matchedText = extractSnippet(note, query)) }

        emit(results)
    }

    override fun observeHistory(): Flow<List<String>> = historyDao.observeHistory()

    override suspend fun saveHistory(query: String) {
        if (query.isBlank()) return
        historyDao.upsert(SearchHistoryEntity(query = query.trim()))
    }

    override suspend fun removeFromHistory(query: String) = historyDao.delete(query)

    override suspend fun clearHistory() = historyDao.clearAll()

    /**
     * Escapes FTS special characters and appends per-word prefix wildcard (*).
     * e.g. "my note" → "my* note*"  (matches "my" AND prefix "note")
     */
    private fun sanitizeFtsQuery(query: String): String {
        val cleaned = query.trim().replace(Regex("""["()*:+\-]"""), "")
        return cleaned.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
    }

    /**
     * Returns a ~100-char snippet around the first occurrence of [query] in the note's text.
     * Falls back to the note title if no text blocks are found.
     */
    private fun extractSnippet(note: Note, query: String): String {
        val plainText = note.contentBlocks
            .filterIsInstance<ContentBlock.Text>()
            .joinToString(" ") { it.text }
            .ifBlank { note.title }
        val firstWord = query.trim().split(" ").first().lowercase()
        val idx = plainText.lowercase().indexOf(firstWord)
        return if (idx < 0) plainText.take(120)
        else plainText.substring(maxOf(0, idx - 20), minOf(plainText.length, idx + 100)).trim()
    }
}
