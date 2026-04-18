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

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_NOTIF = "notifications_enabled"
    }
}
