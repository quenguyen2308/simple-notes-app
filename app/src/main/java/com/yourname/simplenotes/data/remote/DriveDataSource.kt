package com.yourname.simplenotes.data.remote

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.fromJson
import com.yourname.simplenotes.domain.model.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * All Drive I/O operations. Uses App Data folder (hidden, private to this app).
 *
 * Drive storage layout:
 *   appdata/
 *   ├── index.json          — {noteId: driveModifiedTime} map for fast conflict detection
 *   └── note_<uuid>.json    — one file per note
 */
class DriveDataSource(private val authManager: DriveAuthManager) {

    private fun drive(): Drive? {
        val account = authManager.getSignedInAccount() ?: return null
        return authManager.buildDriveService(account)
    }

    /**
     * Uploads or updates a single note file.
     * Returns Drive's modifiedTime (RFC 3339) on success, null on failure or not signed in.
     */
    suspend fun uploadNote(note: Note): String? = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext null
        runCatching {
            val filename = "note_${note.id}.json"
            val content = ByteArrayContent("application/json", note.toJson().toByteArray())
            val metadata = com.google.api.services.drive.model.File().setName(filename)
            val existingId = findFileId(drive, filename)

            if (existingId != null) {
                drive.files().update(existingId, metadata, content)
                    .setFields("modifiedTime")
                    .execute()
                    .modifiedTime?.toStringRfc3339()
            } else {
                metadata.setParents(listOf("appDataFolder"))
                drive.files().create(metadata, content)
                    .setFields("id,modifiedTime")
                    .execute()
                    .modifiedTime?.toStringRfc3339()
            }
        }.getOrNull()
    }

    /**
     * Downloads only the specific note files whose IDs are in [noteIds].
     * One LIST call to resolve filenames → fileIds, then one GET per needed note.
     */
    suspend fun downloadNotes(noteIds: Set<String>): List<Note> = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext emptyList()
        runCatching {
            val files = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name contains 'note_' and mimeType = 'application/json'")
                .setFields("files(id, name)")
                .execute()
                .files ?: emptyList()

            files
                .filter { file ->
                    val noteId = file.name.removePrefix("note_").removeSuffix(".json")
                    noteId in noteIds
                }
                .mapNotNull { file ->
                    runCatching {
                        val stream = drive.files().get(file.id).executeMediaAsInputStream()
                        Note.fromJson(stream.bufferedReader().readText())
                    }.getOrNull()
                }
        }.getOrElse { emptyList() }
    }

    /** Fetches index.json — returns map of noteId → Drive modifiedTime string. */
    suspend fun fetchIndex(): Map<String, String> = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext emptyMap()
        runCatching {
            val fileId = findFileId(drive, "index.json") ?: return@withContext emptyMap()
            val json = drive.files().get(fileId)
                .executeMediaAsInputStream()
                .bufferedReader().readText()
            JSONObject(json).let { obj ->
                obj.keys().asSequence().associateWith { obj.getString(it) }
            }
        }.getOrElse { emptyMap() }
    }

    /** Uploads index.json with the latest noteId → modifiedTime map. */
    suspend fun uploadIndex(index: Map<String, String>) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        runCatching {
            val json = JSONObject(index as Map<*, *>).toString()
            val content = ByteArrayContent("application/json", json.toByteArray())
            val metadata = com.google.api.services.drive.model.File().setName("index.json")
            val existingId = findFileId(drive, "index.json")
            if (existingId != null) {
                drive.files().update(existingId, metadata, content).execute()
            } else {
                metadata.setParents(listOf("appDataFolder"))
                drive.files().create(metadata, content).execute()
            }
        }
    }

    /**
     * Uploads categories + deleted IDs to Drive as a single categories.json.
     * Format: { "categories": [...], "deletedIds": [...] }
     */
    suspend fun uploadCategories(
        categories: List<Category>,
        deletedIds: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        runCatching {
            val json = JSONObject().apply {
                put("categories", JSONArray().apply {
                    categories.forEach { cat ->
                        put(JSONObject().apply {
                            put("id", cat.id)
                            put("name", cat.name)
                            put("colorArgb", cat.colorArgb)
                            put("parentId", cat.parentId ?: JSONObject.NULL)
                            put("order", cat.order)
                        })
                    }
                })
                put("deletedIds", JSONArray(deletedIds.toList()))
            }.toString()

            val content = ByteArrayContent("application/json", json.toByteArray())
            val metadata = com.google.api.services.drive.model.File().setName("categories.json")
            val existingId = findFileId(drive, "categories.json")
            if (existingId != null) {
                drive.files().update(existingId, metadata, content).execute()
            } else {
                metadata.setParents(listOf("appDataFolder"))
                drive.files().create(metadata, content).execute()
            }
        }
    }

    /**
     * Downloads categories.json from Drive.
     * Handles both old format (plain JSONArray) and new format (JSONObject with deletedIds).
     */
    suspend fun downloadCategories(): CategorySyncPayload = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext CategorySyncPayload.EMPTY
        runCatching {
            val fileId = findFileId(drive, "categories.json")
                ?: return@withContext CategorySyncPayload.EMPTY
            val json = drive.files().get(fileId)
                .executeMediaAsInputStream().bufferedReader().readText()

            if (json.trimStart().startsWith("[")) {
                // Backward-compatible: old format was a plain array
                val array = JSONArray(json)
                val categories = (0 until array.length()).map { i ->
                    array.getJSONObject(i).toCategory()
                }
                CategorySyncPayload(categories, emptySet())
            } else {
                val obj = JSONObject(json)
                val array = obj.getJSONArray("categories")
                val categories = (0 until array.length()).map { i ->
                    array.getJSONObject(i).toCategory()
                }
                val deletedArray = obj.optJSONArray("deletedIds") ?: JSONArray()
                val deletedIds = (0 until deletedArray.length())
                    .map { deletedArray.getString(it) }.toSet()
                CategorySyncPayload(categories, deletedIds)
            }
        }.getOrElse { CategorySyncPayload.EMPTY }
    }

    data class CategorySyncPayload(
        val categories: List<Category>,
        val deletedIds: Set<String>
    ) {
        companion object {
            val EMPTY = CategorySyncPayload(emptyList(), emptySet())
        }
    }

    /** Deletes a note file from Drive after its tombstone has been synced to all devices. */
    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        runCatching {
            findFileId(drive, "note_$noteId.json")
                ?.let { drive.files().delete(it).execute() }
        }
    }

    private fun JSONObject.toCategory() = Category(
        id = getString("id"),
        name = getString("name"),
        colorArgb = getInt("colorArgb"),
        parentId = optString("parentId").takeIf { it.isNotEmpty() && it != "null" },
        order = getInt("order")
    )

    private fun findFileId(drive: Drive, filename: String): String? =
        drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$filename'")
            .setFields("files(id)")
            .execute()
            .files
            ?.firstOrNull()
            ?.id
}
