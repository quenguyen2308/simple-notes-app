package com.yourname.simplenotes.ui.theme

/** Visual style applied to the note-list header + FAB. Persisted via SettingsPrefs.headerStyle
 *  and synced through SettingsSnapshot.headerStyle. */
enum class HeaderStyle(val storageKey: String, val label: String) {
    DEFAULT("default", "Mặc định"),
    COMIC("comic", "Truyện tranh siêu anh hùng"),
    CUTE("cute", "Hoạt hình tròn trịa, dễ thương"),
    MEADOW("meadow", "Đồng cỏ nắng"),
    NOTEZ("notez", "NOTEZ (GenZ)");

    companion object {
        fun fromStorageKey(key: String): HeaderStyle = entries.find { it.storageKey == key } ?: DEFAULT
    }
}
