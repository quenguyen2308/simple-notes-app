package com.yourname.simplenotes.ui.theme

/** Full color identity applied across the app (background/surface/accent colors).
 *  Persisted via SettingsPrefs.appTheme and synced through SettingsSnapshot.appTheme.
 *  Independent of light/dark mode and of Material You dynamic color — dynamic color, when
 *  enabled, still takes priority over whichever [AppTheme] is selected here. */
enum class AppTheme(val storageKey: String, val label: String) {
    DEFAULT("default", "Mặc định (Bàn Làm Việc)"),
    SAMSUNG("samsung", "Samsung Notes"),
    EASY("easy", "EasyNotes"),
    INDIGO_NIGHT("indigo_night", "Đêm Indigo"),
    LAVENDER_MIST("lavender_mist", "Sương Lavender"),
    MINT_SUNRISE("mint_sunrise", "Bình Minh Bạc Hà"),
    AURORA("aurora", "Cực Quang"),
    NOTEZ("notez", "NOTEZ (GenZ)");

    companion object {
        fun fromStorageKey(key: String): AppTheme = entries.find { it.storageKey == key } ?: DEFAULT
    }
}
