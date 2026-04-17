package com.yourname.simplenotes.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Manages folder hierarchy: CRUD, move, reorder, max 3-level depth enforcement. */
class FolderViewModel(
    private val categoryRepository: CategoryRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    /** Root-level folders with nested subFolders populated. */
    val folders: StateFlow<List<Category>> = categoryRepository.observeHierarchy()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedFolderId = MutableStateFlow<String?>(null)

    /** Creates a folder under [parentId]. Rejects if that would exceed 3 levels deep. */
    fun createFolder(name: String, colorArgb: Int, parentId: String? = null) {
        viewModelScope.launch {
            if (parentId != null && getFolderDepth(parentId) >= 2) return@launch
            categoryRepository.save(
                Category(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    colorArgb = colorArgb,
                    parentId = parentId
                )
            )
        }
    }

    fun renameFolder(id: String, newName: String) {
        viewModelScope.launch {
            val folder = categoryRepository.getById(id) ?: return@launch
            categoryRepository.save(folder.copy(name = newName.trim()))
        }
    }

    /** Deletes folder + all subfolders recursively; unassigns their notes (sets folderId = null). */
    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            val allDeleteIds = listOf(folderId) + collectAllSubFolderIds(folderId)
            noteRepository.observeAll().first()
                .filter { it.folderId in allDeleteIds }
                .forEach { note -> noteRepository.save(note.copy(folderId = null)) }
            allDeleteIds.forEach { id -> categoryRepository.delete(id) }
            if (selectedFolderId.value in allDeleteIds) selectedFolderId.value = null
        }
    }

    /** Moves [folderId] under [newParentId]. Rejects if that would exceed 3 levels deep. */
    fun moveFolder(folderId: String, newParentId: String?) {
        viewModelScope.launch {
            if (newParentId != null && getFolderDepth(newParentId) >= 2) return@launch
            categoryRepository.moveFolder(folderId, newParentId)
        }
    }

    fun reorderFolders(parentId: String?, folderIds: List<String>) {
        viewModelScope.launch { categoryRepository.reorderFolders(parentId, folderIds) }
    }

    fun moveNoteToFolder(noteId: String, folderId: String) {
        viewModelScope.launch {
            val note = noteRepository.getById(noteId) ?: return@launch
            noteRepository.save(note.copy(folderId = folderId))
        }
    }

    /** Returns the depth of [folderId] (root = 0, child = 1, grandchild = 2). */
    private suspend fun getFolderDepth(folderId: String): Int {
        var depth = 0
        var currentId: String? = folderId
        while (currentId != null && depth < 4) {
            currentId = categoryRepository.getById(currentId)?.parentId
            depth++
        }
        return depth
    }

    /** Collects IDs of all descendants of [folderId] from the current [folders] state. */
    private fun collectAllSubFolderIds(folderId: String): List<String> {
        val result = mutableListOf<String>()
        val flat = flattenFolders()
        fun collectRecursive(parentId: String) {
            flat.filter { it.parentId == parentId }.forEach { child ->
                result.add(child.id)
                collectRecursive(child.id)
            }
        }
        collectRecursive(folderId)
        return result
    }

    private fun flattenFolders(): List<Category> {
        val result = mutableListOf<Category>()
        fun flatten(cats: List<Category>) { cats.forEach { result.add(it); flatten(it.subFolders) } }
        flatten(folders.value)
        return result
    }
}
