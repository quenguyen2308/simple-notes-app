package com.yourname.simplenotes.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.SearchRepository
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.SearchFilters
import com.yourname.simplenotes.domain.model.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val isLoading = MutableStateFlow(false)

    /** Currently selected folder filter (null = all folders). */
    val selectedFolderId = MutableStateFlow<String?>(null)

    val searchHistory: StateFlow<List<String>> = searchRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Flat category list for folder filter chips. */
    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var searchJob: Job? = null
    private var lastQuery: String = ""

    fun search(query: String) {
        lastQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults.value = emptyList()
            isLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            isLoading.value = true
            try {
                searchRepository.search(query, SearchFilters(folderId = selectedFolderId.value))
                    .collect { results ->
                        searchResults.value = results
                        isLoading.value = false
                    }
                searchRepository.saveHistory(query)
            } catch (_: Exception) {
                searchResults.value = emptyList()
                isLoading.value = false
            }
        }
    }

    /** Updates folder filter and re-runs the last search immediately. */
    fun setFolderFilter(folderId: String?) {
        selectedFolderId.value = folderId
        if (lastQuery.isNotBlank()) search(lastQuery)
    }

    fun removeFromHistory(query: String) {
        viewModelScope.launch { searchRepository.removeFromHistory(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { searchRepository.clearHistory() }
    }
}
