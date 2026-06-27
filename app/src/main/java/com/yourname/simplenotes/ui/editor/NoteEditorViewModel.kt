package com.yourname.simplenotes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedrejeb.richeditor.model.RichTextState
import com.yourname.simplenotes.data.local.entities.ChecklistItem
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.NoteMetadata
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    var title by mutableStateOf("")
        private set

    /** Owned by the Composable layer; the ViewModel reads HTML from it on save. */
    val richTextState = RichTextState()

    var selectedCategoryId by mutableStateOf<String?>(null)
        private set
    var isLocked by mutableStateOf(false)
        private set
    var pinHash by mutableStateOf<String?>(null)
        private set
    var backgroundColor by mutableStateOf(0xFFFFFFFF.toInt())
        private set
    var isPinned by mutableStateOf(false)
        private set

    var labels by mutableStateOf(listOf<String>())
        private set

    // ─── Image state ──────────────────────────────────────────────────────────

    /** Image ContentBlocks currently attached to this note. */
    var imageBlocks by mutableStateOf(listOf<ContentBlock.Image>())
        private set

    fun addImage(uri: String) {
        imageBlocks = imageBlocks + ContentBlock.Image(uri = uri)
    }

    fun removeImage(uri: String) {
        imageBlocks = imageBlocks.filter { it.uri != uri }
    }

    // ─── Checklist state ──────────────────────────────────────────────────────

    var isChecklistMode by mutableStateOf(false)
        private set

    var checklistItems by mutableStateOf(listOf<ChecklistItem>())
        private set

    val createdAtMs: Long get() = createdAt

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var noteId: String? = null
    private var createdAt: Long = 0L
    private var existingContentBlocks: List<ContentBlock> = emptyList()
    private var isNewNote = false

    /** Load an existing note by id, or prepare a blank note for "new". */
    fun load(id: String?, initialCategoryId: String? = null) {
        if (id == null || id == "new") {
            noteId = UUID.randomUUID().toString()
            createdAt = System.currentTimeMillis()
            isNewNote = true
            selectedCategoryId = initialCategoryId
            return
        }
        viewModelScope.launch {
            repository.getById(id)?.let { note ->
                noteId = note.id
                title = note.title
                createdAt = note.createdAt
                selectedCategoryId = note.folderId
                isLocked = note.isLocked
                pinHash = note.pinHash
                backgroundColor = note.backgroundColor
                isPinned = note.isPinned
                labels = note.labels
                existingContentBlocks = note.contentBlocks
                imageBlocks = note.contentBlocks.filterIsInstance<ContentBlock.Image>()

                // Detect checklist block — takes priority over text block
                val checklistBlock = note.contentBlocks
                    .filterIsInstance<ContentBlock.Checklist>()
                    .firstOrNull()
                if (checklistBlock != null) {
                    isChecklistMode = true
                    checklistItems = checklistBlock.items
                } else {
                    // Restore rich text state from the first Text block's HTML
                    val html = note.contentBlocks
                        .filterIsInstance<ContentBlock.Text>()
                        .firstOrNull()?.htmlContent ?: ""
                    richTextState.setHtml(html)
                }
            }
        }
    }

    fun onTitleChange(value: String) { title = value }
    fun onCategoryChange(id: String?) { selectedCategoryId = id }
    fun onBackgroundColorChange(color: Int) { backgroundColor = color }
    fun onPinToggle() { isPinned = !isPinned }

    // ─── Checklist operations ─────────────────────────────────────────────────

    fun toggleChecklistMode() {
        isChecklistMode = !isChecklistMode
        // Seed a blank first item when entering checklist mode with no items yet
        if (isChecklistMode && checklistItems.isEmpty()) {
            checklistItems = listOf(
                ChecklistItem(
                    id = UUID.randomUUID().toString(),
                    text = "",
                    isCompleted = false,
                    order = 0
                )
            )
        }
    }

    fun addChecklistItem() {
        val newItem = ChecklistItem(
            id = UUID.randomUUID().toString(),
            text = "",
            isCompleted = false,
            order = checklistItems.size
        )
        checklistItems = checklistItems + newItem
    }

    fun removeChecklistItem(id: String) {
        checklistItems = checklistItems.filter { it.id != id }
    }

    fun toggleChecklistItem(id: String) {
        checklistItems = checklistItems.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun updateChecklistItemText(id: String, text: String) {
        checklistItems = checklistItems.map {
            if (it.id == id) it.copy(text = text) else it
        }
    }

    // ─── Label operations ─────────────────────────────────────────────────────

    fun addLabel(label: String) {
        val trimmed = label.trim()
        if (trimmed.isNotBlank() && trimmed !in labels) {
            labels = labels + trimmed
        }
    }

    fun removeLabel(label: String) {
        labels = labels - label
    }

    /** Called when user confirms a new PIN or removes lock. */
    fun setLock(locked: Boolean, newPinHash: String?) {
        isLocked = locked
        pinHash = newPinHash
    }

    /** Persists the current note to Room (triggers immediate Drive sync via repository). */
    fun save() {
        val id = noteId ?: return

        // Skip saving a brand-new note that has no meaningful content
        if (isNewNote) {
            val hasContent = if (isChecklistMode)
                checklistItems.any { it.text.isNotBlank() }
            else
                richTextState.annotatedString.text.isNotBlank()
            if (title.isBlank() && !hasContent && imageBlocks.isEmpty()) return
        }

        viewModelScope.launch {
            val html = richTextState.toHtml()
            val plainText = richTextState.annotatedString.text

            // Build content blocks based on current mode; imageBlocks is the source of truth
            val contentBlocks = if (isChecklistMode) {
                listOf(ContentBlock.Checklist(items = checklistItems)) + imageBlocks
            } else {
                val textBlock = ContentBlock.Text(text = plainText, htmlContent = html)
                listOf(textBlock) + imageBlocks
            }

            val hasContent = if (isChecklistMode)
                checklistItems.any { it.text.isNotBlank() }
            else
                plainText.isNotBlank()
            val effectiveTitle = if (title.isBlank() && (hasContent || imageBlocks.isNotEmpty())) {
                "Text note ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())}"
            } else title

            repository.save(
                Note(
                    id = id,
                    title = effectiveTitle,
                    contentBlocks = contentBlocks,
                    folderId = selectedCategoryId,
                    backgroundColor = backgroundColor,
                    isPinned = isPinned,
                    labels = labels,
                    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
                    updatedAt = System.currentTimeMillis(),
                    metadata = NoteMetadata.from(plainText),
                    isDirty = true,
                    isDeleted = false,
                    isLocked = isLocked,
                    pinHash = pinHash
                )
            )
        }
    }
}
