package com.yourname.simplenotes.domain.model

/** Derived note statistics. Computed on save, stored in metadata_json. */
data class NoteMetadata(
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    /** Estimated read time in minutes (wordCount / 200). */
    val readTime: Int = 0
) {
    companion object {
        /** Computes metadata from plain-text content. */
        fun from(plainText: String): NoteMetadata {
            val words = plainText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val chars = plainText.length
            return NoteMetadata(
                wordCount = words.size,
                characterCount = chars,
                readTime = maxOf(1, words.size / 200)
            )
        }

        val EMPTY = NoteMetadata()
    }
}
