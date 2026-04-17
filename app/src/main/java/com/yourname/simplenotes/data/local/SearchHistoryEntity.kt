package com.yourname.simplenotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stores recent search queries; deduplicated by [query] text, max 20 via DAO query. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
