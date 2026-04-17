package com.yourname.simplenotes.data.repository

import com.yourname.simplenotes.domain.model.SearchFilters
import com.yourname.simplenotes.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    /** Executes FTS search and returns ranked results matching [query] and [filters]. */
    fun search(query: String, filters: SearchFilters = SearchFilters()): Flow<List<SearchResult>>

    fun observeHistory(): Flow<List<String>>
    suspend fun saveHistory(query: String)
    suspend fun removeFromHistory(query: String)
    suspend fun clearHistory()
}
