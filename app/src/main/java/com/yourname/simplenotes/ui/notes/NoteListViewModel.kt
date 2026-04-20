package com.yourname.simplenotes.ui.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.sync.SyncScheduler
import com.yourname.simplenotes.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NoteListViewModel(
    private val repository: NoteRepository,
    private val syncScheduler: SyncScheduler,
    private val categoryRepository: CategoryRepository,
    context: Context
) : ViewModel() {

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

    /** Currently selected category filter (null = show all). */
    val selectedCategoryId = MutableStateFlow<String?>(null)

    /** Active view type (list / grid / detail). */
    val viewType = MutableStateFlow(NoteViewType.LIST)

    /** All categories for the filter chip row. */
    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Currently selected label filter (null = show all). */
    val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel.asStateFlow()

    /** Currently selected background-color filter (null = show all). */
    private val _selectedColor = MutableStateFlow<Int?>(null)
    val selectedColor: StateFlow<Int?> = _selectedColor.asStateFlow()

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

    /** Notes filtered by category, label, and color — pinned first, then by updatedAt desc. */
    val notes: StateFlow<List<Note>> = combine(
        repository.observeAll(),
        selectedCategoryId,
        _selectedLabel,
        _selectedColor
    ) { allNotes, categoryId, label, color ->
        val filtered = allNotes
            .filter { note -> categoryId == null || note.folderId == categoryId }
            .filter { note -> label == null || label in note.labels }
            .filter { note -> color == null || note.backgroundColor == color }
        filtered.sortedWith(
            compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recent notes (sorted by last modified, latest first) - max 10 notes. */
    val recentNotes: StateFlow<List<Note>> = repository.observeAll()
        .map { notes ->
            notes.sortedByDescending { it.updatedAt }.take(10)
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

    /** Delete multiple notes by IDs. */
    fun deleteNotes(ids: List<String>) {
        viewModelScope.launch { ids.forEach { repository.delete(it) } }
    }

    /** Move multiple notes to a target folder (null = remove from folder). */
    fun moveNotes(ids: List<String>, folderId: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            ids.forEach { id ->
                val note = repository.getById(id) ?: return@forEach
                repository.save(note.copy(folderId = folderId, isDirty = true, updatedAt = now))
            }
        }
    }

    /** Lock or unlock multiple notes by IDs. */
    fun lockNotes(ids: List<String>, locked: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            ids.forEach { id ->
                val note = repository.getById(id) ?: return@forEach
                repository.save(note.copy(isLocked = locked, isDirty = true, updatedAt = now))
            }
        }
    }

    /** Toggle pin state of a single note. */
    fun togglePin(noteId: String) {
        viewModelScope.launch {
            val note = repository.getById(noteId) ?: return@launch
            repository.save(note.copy(isPinned = !note.isPinned, isDirty = true, updatedAt = System.currentTimeMillis()))
        }
    }

    fun selectCategory(id: String?) { selectedCategoryId.value = id }
    fun selectLabel(label: String?) { _selectedLabel.value = label }
    fun selectColor(color: Int?) { _selectedColor.value = color }
    fun setViewType(type: NoteViewType) { viewType.value = type }

    fun addCategory(name: String, colorArgb: Int) {
        viewModelScope.launch {
            categoryRepository.save(
                Category(id = UUID.randomUUID().toString(), name = name, colorArgb = colorArgb)
            )
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
            repository.observeAll().first()
                .filter { it.folderId == id }
                .forEach { note ->
                    repository.save(note.copy(folderId = null, isDirty = true, updatedAt = now))
                }
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
            deletedNotes.value.forEach { repository.permanentDelete(it.id) }
        }
    }
}
