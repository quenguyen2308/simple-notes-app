package com.yourname.simplenotes.domain.model

/** A note returned from full-text search with a context snippet. */
data class SearchResult(
    val note: Note,
    val matchedText: String,
    val relevance: Float = 1.0f
)

/** Narrows search results to a specific folder and/or background color. */
data class SearchFilters(
    val folderId: String? = null,
    val colorArgb: Int? = null
)
