package com.yourname.simplenotes.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    /** Streams the 20 most recent search queries, newest first. */
    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun observeHistory(): Flow<List<String>>

    /** Insert or update (refreshes timestamp on duplicate query). */
    @Upsert
    suspend fun upsert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
