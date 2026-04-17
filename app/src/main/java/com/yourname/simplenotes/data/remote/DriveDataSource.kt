package com.yourname.simplenotes.data.remote

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.fromJson
import com.yourname.simplenotes.domain.model.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * Downloads all note files from Drive.
     * Only called for IDs that need updating (based on index comparison in SyncWorker).
     */
    suspend fun downloadAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext emptyList()
        runCatching {
            val files = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name contains 'note_' and mimeType = 'application/json'")
                .setFields("files(id, name, modifiedTime)")
                .execute()
                .files ?: emptyList()

            files.mapNotNull { file ->
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

    /** Deletes a note file from Drive after its tombstone has been synced to all devices. */
    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext
        runCatching {
            findFileId(drive, "note_$noteId.json")
                ?.let { drive.files().delete(it).execute() }
        }
    }

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
