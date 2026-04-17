package com.yourname.simplenotes.data.local.entities

import java.util.UUID

/** A single item in a [ContentBlock.Checklist]. */
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val order: Int
)

// ─── Mapping helpers ─────────────────────────────────────────────────────────

fun ChecklistItem.toJson() = ChecklistItemJson(
    id = id,
    text = text,
    isCompleted = isCompleted,
    order = order
)

fun ChecklistItemJson.toDomain() = ChecklistItem(
    id = id,
    text = text,
    isCompleted = isCompleted,
    order = order
)
