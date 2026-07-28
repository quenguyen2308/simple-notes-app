package com.yourname.simplenotes.ui.settings

import android.content.Context
import android.content.SharedPreferences
import com.yourname.simplenotes.domain.model.SettingsSnapshot

/** Thin wrapper around SharedPreferences for app settings. */
class SettingsPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    /** Writes [block] and stamps [updatedAt] in the same transaction — every local edit bumps it. */
    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply {
            block()
            putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }.apply()
    }

    /** "system" | "light" | "dark" */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(v) = edit { putString(KEY_THEME, v) }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF, true)
        set(v) = edit { putBoolean(KEY_NOTIF, v) }

    /** Material You dynamic color (Android 12+). Ignored on older devices. */
    var dynamicColorEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(v) = edit { putBoolean(KEY_DYNAMIC_COLOR, v) }

    /** Note list view mode — stores [com.yourname.simplenotes.ui.notes.NoteViewType.name]. */
    var noteViewType: String
        get() = prefs.getString(KEY_VIEW_TYPE, "LIST") ?: "LIST"
        set(v) = edit { putString(KEY_VIEW_TYPE, v) }

    /** Auto-save while editing a note (vs. requiring an explicit save action). */
    var autoSaveEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOSAVE, true)
        set(v) = edit { putBoolean(KEY_AUTOSAVE, v) }

    /** "biometric" | "pin" — default method offered when locking a note. */
    var noteLockMethod: String
        get() = prefs.getString(KEY_LOCK_METHOD, "biometric") ?: "biometric"
        set(v) = edit { putString(KEY_LOCK_METHOD, v) }

    /** Default background color (ARGB) applied to newly created notes. */
    var defaultNoteBackground: Int
        get() = prefs.getInt(KEY_DEFAULT_BG, 0xFFFFFFFF.toInt())
        set(v) = edit { putInt(KEY_DEFAULT_BG, v) }

    var showLinksEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LINKS, true)
        set(v) = edit { putBoolean(KEY_SHOW_LINKS, v) }

    var hideScrollbarEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SCROLLBAR, false)
        set(v) = edit { putBoolean(KEY_HIDE_SCROLLBAR, v) }

    /** "default" | "comic" | "cute" | "meadow" — visual style of the note-list header/FAB. */
    var headerStyle: String
        get() = prefs.getString(KEY_HEADER_STYLE, "default") ?: "default"
        set(v) = edit { putString(KEY_HEADER_STYLE, v) }

    /** "default" | "samsung" | "easy" — app-wide color identity, see [com.yourname.simplenotes.ui.theme.AppTheme]. */
    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "default") ?: "default"
        set(v) = edit { putString(KEY_APP_THEME, v) }

    /** epochMs of the last local settings change. Used for last-write-wins Drive sync. */
    val updatedAt: Long
        get() = prefs.getLong(KEY_UPDATED_AT, 0L)

    fun toSnapshot(): SettingsSnapshot = SettingsSnapshot(
        themeMode = themeMode,
        notificationsEnabled = notificationsEnabled,
        dynamicColorEnabled = dynamicColorEnabled,
        noteViewType = noteViewType,
        autoSaveEnabled = autoSaveEnabled,
        noteLockMethod = noteLockMethod,
        defaultNoteBackground = defaultNoteBackground,
        showLinksEnabled = showLinksEnabled,
        hideScrollbarEnabled = hideScrollbarEnabled,
        headerStyle = headerStyle,
        appTheme = appTheme,
        updatedAt = updatedAt
    )

    /**
     * Applies a snapshot downloaded from Drive verbatim, including its [SettingsSnapshot.updatedAt] —
     * NOT `System.currentTimeMillis()` — so this device converges on the same timestamp as the
     * source instead of immediately re-uploading what it just received on the next sync.
     */
    fun applySnapshot(snapshot: SettingsSnapshot) {
        prefs.edit()
            .putString(KEY_THEME, snapshot.themeMode)
            .putBoolean(KEY_NOTIF, snapshot.notificationsEnabled)
            .putBoolean(KEY_DYNAMIC_COLOR, snapshot.dynamicColorEnabled)
            .putString(KEY_VIEW_TYPE, snapshot.noteViewType)
            .putBoolean(KEY_AUTOSAVE, snapshot.autoSaveEnabled)
            .putString(KEY_LOCK_METHOD, snapshot.noteLockMethod)
            .putInt(KEY_DEFAULT_BG, snapshot.defaultNoteBackground)
            .putBoolean(KEY_SHOW_LINKS, snapshot.showLinksEnabled)
            .putBoolean(KEY_HIDE_SCROLLBAR, snapshot.hideScrollbarEnabled)
            .putString(KEY_HEADER_STYLE, snapshot.headerStyle)
            .putString(KEY_APP_THEME, snapshot.appTheme)
            .putLong(KEY_UPDATED_AT, snapshot.updatedAt)
            .apply()
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_NOTIF = "notifications_enabled"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        private const val KEY_VIEW_TYPE = "note_view_type"
        private const val KEY_AUTOSAVE = "auto_save_enabled"
        private const val KEY_LOCK_METHOD = "note_lock_method"
        private const val KEY_DEFAULT_BG = "default_note_background"
        private const val KEY_SHOW_LINKS = "show_links_enabled"
        private const val KEY_HIDE_SCROLLBAR = "hide_scrollbar_enabled"
        private const val KEY_HEADER_STYLE = "header_style"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_UPDATED_AT = "settings_updated_at"
    }
}
