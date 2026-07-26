package com.yourname.simplenotes.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.data.local.entities.toJson
import com.yourname.simplenotes.data.local.entities.toContentBlocks
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.NoteMetadata

private val gson = Gson()
private val labelListType = object : TypeToken<List<String>>() {}.type

fun NoteEntity.toDomain() = Note(
    id = id,
    title = title,
    contentBlocks = contentBlocksJson.toContentBlocks(),
    folderId = folderId,
    backgroundColor = backgroundColor,
    isPinned = isPinned,
    labels = runCatching<List<String>> { gson.fromJson(labelsJson, labelListType) }
        .getOrDefault(emptyList()),
    createdAt = createdAt,
    updatedAt = updatedAt,
    contentUpdatedAt = contentUpdatedAt,
    metadata = runCatching { gson.fromJson(metadataJson, NoteMetadata::class.java) }
        .getOrDefault(NoteMetadata.EMPTY),
    isDirty = isDirty,
    isDeleted = isDeleted,
    isLocked = isLocked,
    pinHash = pinHash
)

fun Note.toEntity(): NoteEntity {
    // Recompute metadata from current plain-text content on every save
    val freshMetadata = NoteMetadata.from(content)
    return NoteEntity(
        id = id,
        title = title,
        contentBlocksJson = contentBlocks.toJson(),
        folderId = folderId,
        backgroundColor = backgroundColor,
        isPinned = isPinned,
        labelsJson = gson.toJson(labels),
        metadataJson = gson.toJson(freshMetadata),
        createdAt = createdAt,
        updatedAt = updatedAt,
        contentUpdatedAt = contentUpdatedAt,
        isDirty = isDirty,
        isDeleted = isDeleted,
        isLocked = isLocked,
        pinHash = pinHash
    )
}
