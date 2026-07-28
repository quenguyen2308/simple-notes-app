package com.yourname.simplenotes.ui.editor

import android.content.Context
import android.text.Html
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.simplenotes.data.local.entities.ChecklistItem
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.NoteMetadata
import com.yourname.simplenotes.ui.settings.SettingsPrefs
import com.yourname.simplenotes.util.HtmlSpannableConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Keystrokes within this window collapse into a single undo step. */
private const val HISTORY_COALESCE_MS = 700L
private const val MAX_HISTORY_SIZE = 100

class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    context: Context
) : ViewModel() {

    private val settingsPrefs = SettingsPrefs(context)

    var title by mutableStateOf("")
        private set

    /** Current HTML content of the rich-text editor. Updated by the EditText on every change. */
    var htmlContent by mutableStateOf("")
        private set

    // ─── Undo/redo history ────────────────────────────────────────────────────
    // Snapshots of htmlContent. Pushes are coalesced by time so a burst of keystrokes collapses
    // into one undo step instead of one per character.

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var lastHistoryPushAt = 0L

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private fun refreshHistoryFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(htmlContent)
        htmlContent = previous
        refreshHistoryFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(htmlContent)
        htmlContent = next
        refreshHistoryFlags()
    }

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
        undoStack.clear()
        redoStack.clear()
        refreshHistoryFlags()
        if (id == null || id == "new") {
            noteId = UUID.randomUUID().toString()
            createdAt = System.currentTimeMillis()
            isNewNote = true
            selectedCategoryId = initialCategoryId
            backgroundColor = settingsPrefs.defaultNoteBackground
            if (!sharedText.isNullOrBlank()) {
                htmlContent = HtmlSpannableConverter.plainTextToHtml(sharedText)
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
                    // Restore rich text HTML from the first Text block
                    htmlContent = note.contentBlocks
                        .filterIsInstance<ContentBlock.Text>()
                        .firstOrNull()?.htmlContent ?: ""
                }
            }
        }
    }

    fun onTitleChange(value: String) { title = value }

    /** Called by AndroidRichTextEditor on every text change. */
    fun onHtmlContentChange(html: String) {
        if (html == htmlContent) return
        val now = System.currentTimeMillis()
        if (now - lastHistoryPushAt > HISTORY_COALESCE_MS) {
            undoStack.addLast(htmlContent)
            if (undoStack.size > MAX_HISTORY_SIZE) undoStack.removeFirst()
        }
        lastHistoryPushAt = now
        redoStack.clear()
        htmlContent = html
        refreshHistoryFlags()
    }

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
                HtmlSpannableConverter.htmlToPlainText(htmlContent)
                    .toString().isNotBlank()
            if (title.isBlank() && !hasContent && imageBlocks.isEmpty()) return
        }

        viewModelScope.launch {
            val html = htmlContent
            // Rebuild plain text from HTML for search/preview/sync
            val plainText = HtmlSpannableConverter.htmlToPlainText(html)
                .toString().trim()

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
            // Plain text alone doesn't capture the edit: toggling bold/italic/color/font-size
            // on a selection changes htmlContent without changing a single character of plain
            // text, so it must also be compared here — otherwise the write below gets skipped
            // by nothingChanged and formatting silently fails to persist. htmlContent is safe to
            // compare directly (unlike the old Compose rich-text library that predated the
            // EditText-based editor) because AndroidRichTextEditor never feeds its load-time
            // html→Spannable round trip back into htmlContent — only an actual user edit does.
            val originalWasChecklist = existingContentBlocks.any { it is ContentBlock.Checklist }
            val originalPlainText = existingContentBlocks
                .filterIsInstance<ContentBlock.Text>()
                .joinToString("\n") { it.text }
            val originalHtml = existingContentBlocks
                .filterIsInstance<ContentBlock.Text>()
                .firstOrNull()?.htmlContent ?: ""
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
                (if (isChecklistMode) checklistItems != originalChecklistItems
                 else plainText != originalPlainText || html != originalHtml) ||
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
