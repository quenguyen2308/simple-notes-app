package com.yourname.simplenotes.data.local

import android.content.Context

/** Persists the set of note IDs permanently deleted locally but not yet deleted from Drive. */
class NoteSyncPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("note_sync", Context.MODE_PRIVATE)

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
