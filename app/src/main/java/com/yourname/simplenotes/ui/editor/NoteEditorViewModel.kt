package com.yourname.simplenotes.ui.editor

import android.content.Context
import androidx.core.text.HtmlCompat
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
import com.yourname.simplenotes.ui.settings.SettingsPrefs
import com.yourname.simplenotes.util.toEditorHtml
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
    private val categoryRepository: CategoryRepository,
    context: Context
) : ViewModel() {

    private val settingsPrefs = SettingsPrefs(context)

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

    // Full snapshot of the loaded note, used at save() time to tell whether anything actually
    // changed (skip the write+sync entirely if not — just viewing a note shouldn't dirty it)
    // and, within that, whether it was a real content edit (title/body/checklist/images) vs. a
    // cosmetic-only change (color, pin, lock, folder…) — only the former should bump
    // contentUpdatedAt (the user-facing "date modified").
    private var originalNote: Note? = null

    /**
     * Load an existing note by id, or prepare a blank note for "new".
     * [sharedText] pre-fills the body when the note was opened from another app's
     * share sheet (e.g. Samsung Notes, Easy Note) — ignored for existing notes.
     */
    fun load(id: String?, initialCategoryId: String? = null, sharedText: String? = null) {
        if (id == null || id == "new") {
            noteId = UUID.randomUUID().toString()
            createdAt = System.currentTimeMillis()
            isNewNote = true
            selectedCategoryId = initialCategoryId
            backgroundColor = settingsPrefs.defaultNoteBackground
            if (!sharedText.isNullOrBlank()) {
                richTextState.setHtml(sharedText.toEditorHtml())
            }
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
                originalNote = note
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
            // richTextState.annotatedString.text replaces '\n' with ' ' internally (paragraph
            // breaks are structural ParagraphStyle spans, not literal newlines), so the plain
            // text used for search/preview/sync must be rebuilt from the HTML instead, which
            // does encode each paragraph as its own <p>/<br> block.
            val plainText = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

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

            val original = originalNote
            // Compare by *meaning*, not by the raw HTML string: the rich editor's setHtml()/
            // toHtml() round trip commonly reformats HTML (different <p>/<br> structure, etc.)
            // even when the user didn't touch anything, so comparing contentBlocks (which
            // embeds htmlContent) directly would flag a no-op view as an edit — which used to
            // bump contentUpdatedAt (the user-facing "date modified") just from opening a note.
            val originalWasChecklist = existingContentBlocks.any { it is ContentBlock.Checklist }
            val originalPlainText = existingContentBlocks
                .filterIsInstance<ContentBlock.Text>()
                .joinToString("\n") { it.text }
            val originalChecklistItems = existingContentBlocks
                .filterIsInstance<ContentBlock.Checklist>()
                .firstOrNull()?.items.orEmpty()
            val originalImages = existingContentBlocks.filterIsInstance<ContentBlock.Image>()

            val now = System.currentTimeMillis()
            // Only a real edit to title/body/checklist/images bumps the user-facing "date
            // modified" — reordering the folder, pinning, locking, etc. don't, even though they
            // still bump updatedAt below (needed for Drive's last-write-wins conflict resolution).
            val contentChanged = isNewNote ||
                effectiveTitle != original?.title ||
                isChecklistMode != originalWasChecklist ||
                (if (isChecklistMode) checklistItems != originalChecklistItems else plainText != originalPlainText) ||
                imageBlocks != originalImages
            val newContentUpdatedAt = if (contentChanged) now else original?.contentUpdatedAt ?: now

            // Nothing at all changed (not even folder/color/pin/lock) — skip the write entirely
            // instead of just leaving contentUpdatedAt alone, so merely opening and closing a
            // note doesn't mark it dirty or trigger a pointless Drive re-upload.
            val nothingChanged = original != null && !isNewNote && !contentChanged &&
                original.folderId == selectedCategoryId &&
                original.backgroundColor == backgroundColor &&
                original.isPinned == isPinned &&
                original.labels == labels &&
                original.isLocked == isLocked &&
                original.pinHash == pinHash
            if (nothingChanged) return@launch

            repository.save(
                Note(
                    id = id,
                    title = effectiveTitle,
                    contentBlocks = contentBlocks,
                    folderId = selectedCategoryId,
                    backgroundColor = backgroundColor,
                    isPinned = isPinned,
                    labels = labels,
                    createdAt = if (createdAt == 0L) now else createdAt,
                    updatedAt = now,
                    contentUpdatedAt = newContentUpdatedAt,
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
