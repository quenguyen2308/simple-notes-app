package com.yourname.simplenotes.data.remote

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.SettingsSnapshot
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
 *   ├── note_<uuid>.json    — one file per note
 *   ├── deleted_notes.json  — { deletedIds: [...] } — persistent tombstones, never shrinks
 *   ├── categories.json     — { categories: [...], deletedIds: [...] }
 *   └── settings.json       — single SettingsSnapshot object (last-write-wins via updatedAt)
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
        // List call is not wrapped — network errors propagate so doWork() retries
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
                // Individual note parse failures are safe to skip
                runCatching {
                    val stream = drive.files().get(file.id).executeMediaAsInputStream()
                    Note.fromJson(stream.bufferedReader().readText())
                }.getOrNull()
            }
    }

    /** Fetches index.json — returns map of noteId → Drive modifiedTime string. */
    suspend fun fetchIndex(): Map<String, String> = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext emptyMap()
        // No try/catch: let network errors propagate so doWork() retries on failure
        val fileId = findFileId(drive, "index.json") ?: return@withContext emptyMap()
        val json = drive.files().get(fileId)
            .executeMediaAsInputStream()
            .bufferedReader().readText()
        JSONObject(json).let { obj ->
            obj.keys().asSequence().associateWith { obj.getString(it) }
        }
    }

    /** Uploads index.json with the latest noteId → modifiedTime map. */
    suspend fun uploadIndex(index: Map<String, String>) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        // No try-catch: errors propagate so doWork() retries the whole sync
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
                            put("updatedAt", cat.updatedAt)
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
     * No try/catch: network errors propagate so doWork() retries on failure.
     */
    suspend fun downloadCategories(): CategorySyncPayload = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext CategorySyncPayload.EMPTY
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
    }

    data class CategorySyncPayload(
        val categories: List<Category>,
        val deletedIds: Set<String>
    ) {
        companion object {
            val EMPTY = CategorySyncPayload(emptyList(), emptySet())
        }
    }

    /** Uploads the app settings as a single `settings.json` (one object, not a per-item collection). */
    suspend fun uploadSettings(snapshot: SettingsSnapshot) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        runCatching {
            val json = JSONObject().apply {
                put("themeMode", snapshot.themeMode)
                put("notificationsEnabled", snapshot.notificationsEnabled)
                put("dynamicColorEnabled", snapshot.dynamicColorEnabled)
                put("noteViewType", snapshot.noteViewType)
                put("autoSaveEnabled", snapshot.autoSaveEnabled)
                put("noteLockMethod", snapshot.noteLockMethod)
                put("defaultNoteBackground", snapshot.defaultNoteBackground)
                put("showLinksEnabled", snapshot.showLinksEnabled)
                put("hideScrollbarEnabled", snapshot.hideScrollbarEnabled)
                put("updatedAt", snapshot.updatedAt)
            }.toString()

            val content = ByteArrayContent("application/json", json.toByteArray())
            val metadata = com.google.api.services.drive.model.File().setName("settings.json")
            val existingId = findFileId(drive, "settings.json")
            if (existingId != null) {
                drive.files().update(existingId, metadata, content).execute()
            } else {
                metadata.setParents(listOf("appDataFolder"))
                drive.files().create(metadata, content).execute()
            }
        }
    }

    /** Returns the Drive settings snapshot, or null if not signed in / no settings uploaded yet. */
    suspend fun downloadSettings(): SettingsSnapshot? = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext null
        val fileId = findFileId(drive, "settings.json") ?: return@withContext null
        runCatching {
            val json = drive.files().get(fileId)
                .executeMediaAsInputStream().bufferedReader().readText()
            val obj = JSONObject(json)
            SettingsSnapshot(
                themeMode = obj.optString("themeMode", "system"),
                notificationsEnabled = obj.optBoolean("notificationsEnabled", true),
                dynamicColorEnabled = obj.optBoolean("dynamicColorEnabled", true),
                noteViewType = obj.optString("noteViewType", "LIST"),
                autoSaveEnabled = obj.optBoolean("autoSaveEnabled", true),
                noteLockMethod = obj.optString("noteLockMethod", "biometric"),
                defaultNoteBackground = obj.optInt("defaultNoteBackground", 0xFFFFFFFF.toInt()),
                showLinksEnabled = obj.optBoolean("showLinksEnabled", true),
                hideScrollbarEnabled = obj.optBoolean("hideScrollbarEnabled", false),
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        }.getOrNull()
    }

    /** Deletes a note file from Drive after its tombstone has been synced to all devices. */
    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        runCatching {
            findFileId(drive, "note_$noteId.json")
                ?.let { drive.files().delete(it).execute() }
        }
    }

    /**
     * Uploads the persistent set of permanently-deleted note IDs (never shrinks). Unlike
     * index.json — where a purged note simply disappears with no trace — this survives so a
     * device that missed the intermediate soft-delete (e.g. was offline while another device
     * both deleted the note AND emptied the trash) still learns the note is gone instead of
     * keeping it forever. Mirrors categories.json's `deletedIds` field.
     */
    suspend fun uploadDeletedNoteIds(ids: Set<String>) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        runCatching {
            val json = JSONObject().apply { put("deletedIds", JSONArray(ids.toList())) }.toString()
            val content = ByteArrayContent("application/json", json.toByteArray())
            val metadata = com.google.api.services.drive.model.File().setName("deleted_notes.json")
            val existingId = findFileId(drive, "deleted_notes.json")
            if (existingId != null) {
                drive.files().update(existingId, metadata, content).execute()
            } else {
                metadata.setParents(listOf("appDataFolder"))
                drive.files().create(metadata, content).execute()
            }
        }
    }

    /** Downloads the persistent set of permanently-deleted note IDs. Empty if never uploaded. */
    suspend fun downloadDeletedNoteIds(): Set<String> = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext emptySet()
        val fileId = findFileId(drive, "deleted_notes.json") ?: return@withContext emptySet()
        runCatching {
            val json = drive.files().get(fileId).executeMediaAsInputStream().bufferedReader().readText()
            val array = JSONObject(json).optJSONArray("deletedIds") ?: JSONArray()
            (0 until array.length()).map { array.getString(it) }.toSet()
        }.getOrDefault(emptySet())
    }

    private fun JSONObject.toCategory() = Category(
        id = getString("id"),
        name = getString("name"),
        colorArgb = getInt("colorArgb"),
        parentId = optString("parentId").takeIf { it.isNotEmpty() && it != "null" },
        order = getInt("order"),
        updatedAt = optLong("updatedAt", 0L)
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
