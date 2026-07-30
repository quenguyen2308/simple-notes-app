package com.yourname.simplenotes.domain.model

/** Full snapshot of user-configurable app settings, synced to Drive as a single `settings.json`. */
data class SettingsSnapshot(
    val themeMode: String,
    val notificationsEnabled: Boolean,
    val dynamicColorEnabled: Boolean,
    val noteViewType: String,
    val autoSaveEnabled: Boolean,
    val noteLockMethod: String,
    val defaultNoteBackground: Int,
    val showLinksEnabled: Boolean,
    val hideScrollbarEnabled: Boolean,
    /** "default" | "comic" | "cute" | "meadow" — visual style of the note-list header/FAB. */
    val headerStyle: String,
    /** epochMs of the last local change — last-write-wins against the Drive copy. */
    val updatedAt: Long
)
