package com.yourname.simplenotes.data.local

import android.content.Context

/** Persists the set of category IDs deleted locally but not yet uploaded to Drive. */
class CategorySyncPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("category_sync", Context.MODE_PRIVATE)

    var pendingDeletedIds: Set<String>
        get() = prefs.getStringSet(KEY, emptySet()) ?: emptySet()
        private set(value) = prefs.edit().putStringSet(KEY, value).apply()

    fun addDeletedId(id: String) {
        pendingDeletedIds = pendingDeletedIds + id
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "pending_deleted_ids"
    }
}
