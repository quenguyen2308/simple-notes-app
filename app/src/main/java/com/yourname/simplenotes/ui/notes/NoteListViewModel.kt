package com.yourname.simplenotes.ui.notes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yourname.simplenotes.data.importer.ArchiveImportResult
import com.yourname.simplenotes.data.importer.ArchiveNoteImporter
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.sync.SyncScheduler
import com.yourname.simplenotes.sync.SyncWorker
import com.yourname.simplenotes.ui.settings.SettingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Outcome of importing an external `.backup`/`.zip` archive, shown once then cleared. */
sealed class ArchiveImportOutcome {
    data class Success(val result: ArchiveImportResult) : ArchiveImportOutcome()
    object Failed : ArchiveImportOutcome()
}

class NoteListViewModel(
    private val repository: NoteRepository,
    private val syncScheduler: SyncScheduler,
    private val categoryRepository: CategoryRepository,
    context: Context
) : ViewModel() {

    private val appContext = context.applicationContext

    /** True while the immediate sync job is RUNNING — used to show a loading indicator. */
    val isSyncing: StateFlow<Boolean> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME_IMMEDIATE)
            .map { infos ->
                infos.any {
                    it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val settingsPrefs = SettingsPrefs(context)

    /** Currently selected category filter (null = show all). */
    val selectedCategoryId = MutableStateFlow<String?>(null)

    /** Active view type (list / grid / detail) — restored from the last saved choice. */
    val viewType = MutableStateFlow(
        runCatching { NoteViewType.valueOf(settingsPrefs.noteViewType) }.getOrDefault(NoteViewType.LIST)
    )

    /** All categories for the filter chip row. */
    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Currently selected label filter (null = show all). */
    val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel.asStateFlow()

    /** Currently selected background-color filter (null = show all). */
    private val _selectedColor = MutableStateFlow<Int?>(null)
    val selectedColor: StateFlow<Int?> = _selectedColor.asStateFlow()

    /** When true, [notes] only includes pinned notes (drawer "Đã ghim" shortcut). */
    private val _pinnedOnly = MutableStateFlow(false)
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly.asStateFlow()

    fun setPinnedOnly(value: Boolean) { _pinnedOnly.value = value }

    fun setLabelFilter(label: String?) { _selectedLabel.value = label }

    /** True unfiltered note count, for the drawer's "Tất cả ghi chú" badge ([notes] is filtered). */
    val totalNoteCount: StateFlow<Int> = repository.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** All unique labels across all notes (for filter chips). */
    val allLabels: StateFlow<List<String>> = repository.observeAll()
        .map { notes -> notes.flatMap { it.labels }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Non-white background colors used across existing notes (for color filter chips). */
    val allUsedColors: StateFlow<List<Int>> = repository.observeAll()
        .map { notes ->
            notes.map { it.backgroundColor }
                .filter { it != 0xFFFFFFFF.toInt() && it != -1 }
                .distinct()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Notes filtered by category, label, color, and pinned-only — pinned first, then by updatedAt desc. */
    val notes: StateFlow<List<Note>> = combine(
        repository.observeAll(),
        selectedCategoryId,
        _selectedLabel,
        _selectedColor,
        _pinnedOnly
    ) { allNotes, categoryId, label, color, pinnedOnly ->
        val filtered = allNotes
            .filter { note -> categoryId == null || note.folderId == categoryId }
            .filter { note -> label == null || label in note.labels }
            .filter { note -> color == null || note.backgroundColor == color }
            .filter { note -> !pinnedOnly || note.isPinned }
        filtered.sortedWith(
            compareByDescending<Note> { it.isPinned }.thenByDescending { it.contentUpdatedAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recent notes (sorted by last modified, latest first) - max 10 notes. */
    val recentNotes: StateFlow<List<Note>> = repository.observeAll()
        .map { notes ->
            notes.sortedByDescending { it.contentUpdatedAt }.take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Category note counts. */
    val categoryCounts: StateFlow<Map<String, Int>> = combine(
        repository.observeAll(),
        categories
    ) { allNotes, cats ->
        cats.associateBy({ it.id }) { cat ->
            allNotes.count { it.folderId == cat.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        syncScheduler.triggerImmediateSync()
    }

    /** Call from the UI when the screen resumes (app comes to foreground). */
    fun onResume() {
        syncScheduler.triggerImmediateSync()
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun saveNote(note: Note) {
        viewModelScope.launch { repository.save(note) }
    }

    /** Persists notes built from imported files (Settings → "Nhập ghi chú"), one per file. */
    fun importNotes(notes: List<Note>) {
        viewModelScope.launch { repository.saveAll(notes) }
    }

    private val _archiveImportOutcome = MutableStateFlow<ArchiveImportOutcome?>(null)
    /** Result of the last [importArchive] call — collect once, then call [clearArchiveImportOutcome]. */
    val archiveImportOutcome: StateFlow<ArchiveImportOutcome?> = _archiveImportOutcome.asStateFlow()

    fun clearArchiveImportOutcome() { _archiveImportOutcome.value = null }

    /**
     * Imports notes from a picked `.backup` (EasyNotes native backup) or split `.zip` archive
     * (Settings → "Nhập từ file .zip / .backup"), preserving each note's original creation/update
     * timestamps and mapping split-file category prefixes to folders.
     */
    fun importArchive(uri: Uri) {
        viewModelScope.launch {
            val folderCache = mutableMapOf<String, String>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        ArchiveNoteImporter.import(input) { name -> resolveFolderId(name, folderCache) }
                    }
                }.getOrNull()
            }

            if (result == null) {
                _archiveImportOutcome.value = ArchiveImportOutcome.Failed
                return@launch
            }
            repository.saveAll(result.notes)
            _archiveImportOutcome.value = ArchiveImportOutcome.Success(result)
        }
    }

    /** Finds a folder by (case-insensitive) name, creating it if absent; caches within one import run. */
    private suspend fun resolveFolderId(name: String, cache: MutableMap<String, String>): String? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return null
        val key = trimmed.lowercase()
        cache[key]?.let { return it }

        val existing = categoryRepository.getAll().firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        val id = existing?.id ?: UUID.randomUUID().toString().also { newId ->
            categoryRepository.save(Category(id = newId, name = trimmed))
        }
        cache[key] = id
        return id
    }

    /** Delete multiple notes by IDs — one bulk write and one sync trigger, not one per note. */
    fun deleteNotes(ids: List<String>) {
        viewModelScope.launch { repository.deleteAll(ids) }
    }

    /**
     * Move multiple notes to a target folder (null = remove from folder). Batched via
     * [NoteRepository.saveAll] — looping [NoteRepository.save] per note each triggers its own
     * immediate sync, and WorkManager's REPLACE policy cancels the previous (still in-flight)
     * one, so a large selection could leave most of the batch stuck dirty for a long time
     * (e.g. locking notes across several folders one at a time reproduced exactly this).
     */
    fun moveNotes(ids: List<String>, folderId: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = ids.mapNotNull { id ->
                repository.getById(id)?.copy(folderId = folderId, isDirty = true, updatedAt = now)
            }
            repository.saveAll(updated)
        }
    }

    /** Lock or unlock multiple notes by IDs — see [moveNotes] for why this is batched. */
    fun lockNotes(ids: List<String>, locked: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = ids.mapNotNull { id ->
                repository.getById(id)?.copy(isLocked = locked, isDirty = true, updatedAt = now)
            }
            repository.saveAll(updated)
        }
    }

    /** Toggle pin state of a single note. `updatedAt` bumps (needed so Drive's whole-note
     *  last-write-wins doesn't clobber this pin with a stale copy from another device — see
     *  NoteEditorViewModel's save logic for the same convention), but `contentUpdatedAt` does
     *  not, so the user-facing "last edited" date shown on the card stays put. */
    fun togglePin(noteId: String) {
        viewModelScope.launch {
            val note = repository.getById(noteId) ?: return@launch
            repository.save(note.copy(isPinned = !note.isPinned, isDirty = true, updatedAt = System.currentTimeMillis()))
        }
    }

    /** Unpins multiple notes at once (e.g. "Unpin favourites from top"). */
    fun unpinNotes(ids: List<String>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = ids.mapNotNull { id ->
                repository.getById(id)?.copy(isPinned = false, isDirty = true, updatedAt = now)
            }
            repository.saveAll(updated)
        }
    }

    fun selectCategory(id: String?) { selectedCategoryId.value = id }
    fun selectLabel(label: String?) { _selectedLabel.value = label }
    fun selectColor(color: Int?) { _selectedColor.value = color }
    fun setViewType(type: NoteViewType) {
        viewType.value = type
        settingsPrefs.noteViewType = type.name
    }

    fun addCategory(name: String, colorArgb: Int) {
        viewModelScope.launch {
            categoryRepository.save(
                Category(id = UUID.randomUUID().toString(), name = name, colorArgb = colorArgb)
            )
        }
    }

    /** Updates an existing folder's name and color (e.g. from the long-press edit dialog). */
    fun updateCategory(id: String, name: String, colorArgb: Int) {
        viewModelScope.launch {
            val category = categoryRepository.getById(id) ?: return@launch
            categoryRepository.save(category.copy(name = name.trim(), colorArgb = colorArgb))
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            categoryRepository.delete(id)
            if (selectedCategoryId.value == id) selectedCategoryId.value = null
        }
    }

    fun reorderCategories(categoryIds: List<String>) {
        viewModelScope.launch {
            categoryRepository.reorderFolders(null, categoryIds)
        }
    }

    /** Moves all notes in [id] to root (folderId=null) then deletes the folder. */
    fun deleteFolder(id: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val orphaned = repository.observeAll().first()
                .filter { it.folderId == id }
                .map { it.copy(folderId = null, isDirty = true, updatedAt = now) }
            repository.saveAll(orphaned)
            categoryRepository.delete(id)
            if (selectedCategoryId.value == id) selectedCategoryId.value = null
        }
    }

    /** Soft-deleted notes shown in Recycle Bin. */
    val deletedNotes: StateFlow<List<Note>> = repository.observeDeleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: String) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun permanentDelete(id: String) {
        viewModelScope.launch { repository.permanentDelete(id) }
    }

    fun clearRecycleBin() {
        viewModelScope.launch {
            repository.permanentDeleteAll(deletedNotes.value.map { it.id })
        }
    }
}
