package com.yourname.simplenotes.domain.model

/** Domain model for a note category / folder. Supports hierarchy up to 3 levels deep. */
data class Category(
    val id: String,
    val name: String,
    val colorArgb: Int = 0xFF6650A4.toInt(),
    val parentId: String? = null,
    val order: Int = 0,
    val notesCount: Int = 0,
    val subFolders: List<Category> = emptyList()
)
