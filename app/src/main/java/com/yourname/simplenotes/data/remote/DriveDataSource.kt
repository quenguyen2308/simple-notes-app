package com.yourname.simplenotes.data.remote

import android.util.Log
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.SettingsSnapshot
import com.yourname.simplenotes.domain.model.fromJson
import com.yourname.simplenotes.domain.model.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
     * Resolves every existing `note_<id>.json` file in one list call — pass the result to
     * [uploadNote] for a batch of notes so each one doesn't do its own list() round trip to
     * check whether it already exists (e.g. importing 100+ notes at once would otherwise make
     * ~2 Drive API calls per note just to find out "no, it's new", risking rate limiting).
     */
    suspend fun listNoteFileIds(): Map<String, String> = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext emptyMap()
        runCatching {
            drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name contains 'note_' and mimeType = 'application/json'")
                .setFields("files(id, name)")
                .execute()
                .files
                ?.associate { it.name.removePrefix("note_").removeSuffix(".json") to it.id }
                .orEmpty()
        }.getOrElse {
            Log.w(TAG, "listNoteFileIds failed, falling back to per-note lookups", it)
            emptyMap()
        }
    }

    /**
     * Uploads or updates a single note file. Pass [knownFileId] (from [listNoteFileIds]) when
     * uploading many notes in a batch to skip this note's own existence check.
     * Returns Drive's modifiedTime (RFC 3339) on success, null on failure or not signed in.
     */
    suspend fun uploadNote(note: Note, knownFileId: String? = null): String? = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext null
        runCatching {
            val filename = "note_${note.id}.json"
            val content = ByteArrayContent("application/json", note.toJson().toByteArray())
            val metadata = com.google.api.services.drive.model.File().setName(filename)
            val existingId = knownFileId ?: findFileId(drive, filename)

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
        }.onFailure { e ->
            Log.w(TAG, "uploadNote failed for ${note.id}: ${e.message}", e)
        }.getOrNull()
    }

    /**
     * Downloads only the specific note files whose IDs are in [noteIds].
     * One LIST call to resolve filenames → fileIds, then one GET per needed note — the GETs
     * run concurrently (each is an independent network round trip) instead of one at a time,
     * since a multi-note sync was otherwise paying full RTT per note serially.
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

        coroutineScope {
            files
                .filter { file ->
                    val noteId = file.name.removePrefix("note_").removeSuffix(".json")
                    noteId in noteIds
                }
                .map { file ->
                    async {
                        // Individual note parse failures are safe to skip
                        runCatching {
                            val stream = drive.files().get(file.id).executeMediaAsInputStream()
                            Note.fromJson(stream.bufferedReader().readText())
                        }.getOrNull()
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }

    /**
     * Fetches index.json — returns map of noteId → Drive modifiedTime string.
     *
     * Unlike other single-file reads, this doesn't go through [findFileId]. Two things it
     * defends against, both variations on "a note's own file uploaded fine, but no device ever
     * learns it exists because the index doesn't mention it":
     *  1. Duplicate index.json files (see [findFileId]'s doc) — blindly picking one and
     *     deleting the rest would silently drop whichever note entries only exist in the
     *     discarded copy, so every copy is read and merged (newest timestamp wins per noteId)
     *     before any cleanup happens.
     *  2. A sync that got cancelled (e.g. by an overlapping SyncWorker run — see the mutex in
     *     SyncWorker) between successfully uploading a note and writing its index entry. The
     *     actual set of `note_*.json` files is the ground truth here, so any note file with no
     *     matching index entry gets backfilled.
     * Either case leaves the index changed, so it's re-persisted (and duplicates cleaned up)
     * before returning.
     */
    suspend fun fetchIndex(): Map<String, String> = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext emptyMap()
        // No try/catch on the list calls: let network errors propagate so doWork() retries
        val indexFiles = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = 'index.json'")
            .setFields("files(id, modifiedTime)")
            .execute()
            .files

        val merged = mutableMapOf<String, String>()
        var changed = false

        if (!indexFiles.isNullOrEmpty()) {
            indexFiles.forEach { file ->
                runCatching {
                    val json = drive.files().get(file.id).executeMediaAsInputStream().bufferedReader().readText()
                    val obj = JSONObject(json)
                    obj.keys().forEach { key ->
                        val value = obj.getString(key)
                        val existing = merged[key]
                        if (existing == null || parseIsoMillis(value) > parseIsoMillis(existing)) {
                            merged[key] = value
                        }
                    }
                }.onFailure { e -> Log.w(TAG, "fetchIndex: failed reading index.json copy (${file.id})", e) }
            }
            if (indexFiles.size > 1) {
                Log.w(TAG, "fetchIndex: merging ${indexFiles.size} duplicate index.json files into ${merged.size} note entries")
                changed = true
            }
        }

        runCatching {
            val noteFiles = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name contains 'note_' and mimeType = 'application/json'")
                .setFields("files(name, modifiedTime)")
                .execute()
                .files.orEmpty()
            noteFiles.forEach { file ->
                val noteId = file.name.removePrefix("note_").removeSuffix(".json")
                val modifiedTime = file.modifiedTime?.toStringRfc3339()
                if (noteId.isNotEmpty() && modifiedTime != null && noteId !in merged) {
                    merged[noteId] = modifiedTime
                    changed = true
                }
            }
        }.onFailure { e -> Log.w(TAG, "fetchIndex: failed to reconcile against note_*.json files", e) }

        if (changed) {
            runCatching {
                val mergedJson = JSONObject(merged as Map<*, *>).toString()
                val content = ByteArrayContent("application/json", mergedJson.toByteArray())
                val survivor = indexFiles?.maxByOrNull { it.modifiedTime?.value ?: 0L }
                if (survivor != null) {
                    indexFiles.filter { it.id != survivor.id }.forEach { dup ->
                        runCatching { drive.files().delete(dup.id).execute() }
                            .onFailure { e -> Log.w(TAG, "fetchIndex: failed to delete duplicate index.json (${dup.id})", e) }
                    }
                    drive.files().update(survivor.id, com.google.api.services.drive.model.File(), content).execute()
                } else {
                    // No index.json existed at all, but reconciliation found note files — create
                    // one directly with the backfilled data instead of leaving it missing.
                    val metadata = com.google.api.services.drive.model.File()
                        .setName("index.json").setParents(listOf("appDataFolder"))
                    drive.files().create(metadata, content).execute()
                }
            }.onFailure { e -> Log.w(TAG, "fetchIndex: failed to persist reconciled index.json", e) }
        }

        merged
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
                put("headerStyle", snapshot.headerStyle)
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
                headerStyle = obj.optString("headerStyle", "default"),
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

    private fun findFileId(drive: Drive, filename: String): String? {
        val files = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$filename'")
            .setFields("files(id, modifiedTime)")
            .execute()
            .files
        if (files.isNullOrEmpty()) return null

        val newest = files.maxByOrNull { it.modifiedTime?.value ?: 0L }!!
        val duplicates = files.filter { it.id != newest.id }
        if (duplicates.isNotEmpty()) {
            // Two syncs racing on their first run (both see "no existing file" and both create
            // one — e.g. two devices' first-ever sync, or two SyncWorker instances overlapping
            // on the same device) leave duplicates behind. Reads/writes then unpredictably hit
            // whichever copy this list call happens to return: a device uploading a note updates
            // one copy of index.json while another device keeps reading a different, stale copy
            // and never learns the note exists — silent, permanent non-sync with no error anywhere.
            // Self-heal by deleting the older copies so every future read/write is unambiguous.
            Log.w(TAG, "findFileId: found ${files.size} files named '$filename' — deleting ${duplicates.size} stale duplicate(s)")
            duplicates.forEach { dup ->
                runCatching { drive.files().delete(dup.id).execute() }
                    .onFailure { e -> Log.w(TAG, "findFileId: failed to delete duplicate '$filename' (${dup.id})", e) }
            }
        }
        return newest.id
    }

    private fun parseIsoMillis(isoString: String): Long =
        runCatching { java.time.Instant.parse(isoString).toEpochMilli() }.getOrDefault(0L)

    companion object {
        private const val TAG = "DriveDataSource"
    }
}
