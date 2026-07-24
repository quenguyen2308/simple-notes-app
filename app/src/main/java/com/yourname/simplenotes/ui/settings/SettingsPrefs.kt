package com.yourname.simplenotes.ui.settings

import android.content.Context

/** Thin wrapper around SharedPreferences for app settings. */
class SettingsPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    /** "system" | "light" | "dark" */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(v) = prefs.edit().putString(KEY_THEME, v).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF, true)
        set(v) = prefs.edit().putBoolean(KEY_NOTIF, v).apply()

    /** Material You dynamic color (Android 12+). Ignored on older devices. */
    var dynamicColorEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(v) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, v).apply()

    /** Note list view mode — stores [com.yourname.simplenotes.ui.notes.NoteViewType.name]. */
    var noteViewType: String
        get() = prefs.getString(KEY_VIEW_TYPE, "LIST") ?: "LIST"
        set(v) = prefs.edit().putString(KEY_VIEW_TYPE, v).apply()

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_NOTIF = "notifications_enabled"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        private const val KEY_VIEW_TYPE = "note_view_type"
    }
}
