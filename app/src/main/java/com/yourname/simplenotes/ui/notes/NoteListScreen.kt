package com.yourname.simplenotes.ui.notes

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.folder.FolderScreen
import com.yourname.simplenotes.util.BiometricHelper
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNoteClick: (String) -> Unit,
    onNewNote: () -> Unit,
    onSearchClick: () -> Unit,
    viewModel: NoteListViewModel = koinViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val viewType by viewModel.viewType.collectAsStateWithLifecycle()
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle()
    val selectedLabel by viewModel.selectedLabel.collectAsStateWithLifecycle()
    val allUsedColors by viewModel.allUsedColors.collectAsStateWithLifecycle()
    val selectedColor by viewModel.selectedColor.collectAsStateWithLifecycle()

    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Notes, 1: Folders, 2: Settings
    var showViewMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Selection mode state
    var selectedNotes by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var bottomSheetNote by remember { mutableStateOf<Note?>(null) }

    fun enterSelectionMode(noteId: String) {
        isSelectionMode = true
        selectedNotes = setOf(noteId)
    }

    fun toggleSelection(noteId: String) {
        selectedNotes = if (selectedNotes.contains(noteId)) {
            val updated = selectedNotes - noteId
            if (updated.isEmpty()) isSelectionMode = false
            updated
        } else {
            selectedNotes + noteId
        }
    }

    fun exitSelectionMode() {
        selectedNotes = emptySet()
        isSelectionMode = false
    }

    fun handleNoteClick(noteId: String) {
        val note = notes.find { it.id == noteId } ?: return
        if (!note.isLocked) { onNoteClick(noteId); return }
        // Unlock using device credential (biometric + device PIN/pattern/password)
        BiometricHelper.authenticateWithDeviceCredential(
            activity = context as FragmentActivity,
            title = "Unlock Note",
            onSuccess = { onNoteClick(noteId) },
            onError = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { /* Menu action */ }) {
                            Icon(Icons.Default.GridView, "Menu", modifier = Modifier.size(24.dp))
                        }
                        Text(
                            "Ghi Chú Cá Nhân",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onSearchClick) {
                                Icon(Icons.Default.Search, "Search")
                            }
                            Box {
                                IconButton(onClick = { showViewMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "View options")
                                }
                                DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                                    DropdownMenuItem(text = { Text("List view") },
                                        onClick = { viewModel.setViewType(NoteViewType.LIST); showViewMenu = false },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) })
                                    DropdownMenuItem(text = { Text("Grid view") },
                                        onClick = { viewModel.setViewType(NoteViewType.GRID); showViewMenu = false },
                                        leadingIcon = { Icon(Icons.Default.GridView, null) })
                                }
                            }
                            // Avatar button
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("AV", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = Color(0xFFFFC107), // Yellow accent
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, "New note", modifier = Modifier.size(28.dp))
            }
        },
        bottomBar = {
            AnimatedContent(targetState = isSelectionMode, label = "bottom_bar") { inSelectionMode ->
                if (inSelectionMode) {
                    SelectionActionBar(
                        selectedCount = selectedNotes.size,
                        onSelectAll = { selectedNotes = notes.map { it.id }.toSet() },
                        onDeselect = { exitSelectionMode() },
                        onDelete = {
                            viewModel.deleteNotes(selectedNotes.toList())
                            exitSelectionMode()
                        }
                    )
                } else {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                            label = { Text("Ghi Chú") },
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.FolderOpen, null) },
                            label = { Text("Thư Mục") },
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text("Cài Đặt") },
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Folders tab — folder browser; tapping a folder filters Notes tab
            if (activeTab == 1) {
                FolderScreen(
                    onFolderSelected = { folderId ->
                        viewModel.selectCategory(folderId)
                        activeTab = 0
                    }
                )
                return@Scaffold
            }

            // Content sections
            // Filter headers — rendered outside lazy containers to avoid nesting restrictions
            Column {
                // Category chips with add button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            isSelected = selectedCategoryId == cat.id,
                            onClick = { viewModel.selectCategory(cat.id) }
                        )
                    }
                    // Add category button
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showAddCategory = true; newCategoryName = "" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Label filter chips row — only shown when labels exist
                if (allLabels.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Labels:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        FilterChip(
                            selected = selectedLabel == null,
                            onClick = { viewModel.selectLabel(null) },
                            label = { Text("All") }
                        )
                        allLabels.forEach { label ->
                            FilterChip(
                                selected = selectedLabel == label,
                                onClick = { viewModel.selectLabel(if (selectedLabel == label) null else label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Color filter chips row — only shown when colored notes exist
                if (allUsedColors.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Color:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        FilterChip(
                            selected = selectedColor == null,
                            onClick = { viewModel.selectColor(null) },
                            label = { Text("All") }
                        )
                        allUsedColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(color), CircleShape)
                                    .border(
                                        width = if (selectedColor == color) 2.dp else 0.5.dp,
                                        color = if (selectedColor == color)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.selectColor(
                                            if (selectedColor == color) null else color
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            // Notes list — grid view uses LazyVerticalGrid (2 columns), others use LazyColumn
            if (notes.isNotEmpty() && viewType == NoteViewType.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        AnimatedNoteCard(
                            note = note,
                            isSelected = selectedNotes.contains(note.id),
                            onClick = {
                                if (isSelectionMode) toggleSelection(note.id)
                                else handleNoteClick(note.id)
                            },
                            onLongPress = {
                                if (isSelectionMode) toggleSelection(note.id)
                                else enterSelectionMode(note.id)
                            },
                            onShowActions = { bottomSheetNote = note }
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    // Notes for selected category
                    if (notes.isNotEmpty()) {
                        items(notes, key = { it.id }) { note ->
                            when (viewType) {
                                NoteViewType.LIST -> NoteListItem(
                                    note = note,
                                    isSelected = selectedNotes.contains(note.id),
                                    onClick = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else handleNoteClick(note.id)
                                    },
                                    onLongPress = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else enterSelectionMode(note.id)
                                    },
                                    onDelete = { viewModel.deleteNote(note.id) }
                                )
                                NoteViewType.DETAIL -> NoteDetailItem(
                                    note = note,
                                    onClick = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else handleNoteClick(note.id)
                                    },
                                    onDelete = { viewModel.deleteNote(note.id) }
                                )
                                else -> {}
                            }
                        }
                    } else if (categories.isNotEmpty() && selectedCategoryId != null) {
                        // Category selected but has no notes
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No notes in this category. Tap + to create one.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Empty state
                    if (categories.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No categories yet. Tap + to create one.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it },
                    label = { Text("Category name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim(), 0xFF1E88E5.toInt())
                            showAddCategory = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddCategory = false }) { Text("Cancel") } }
        )
    }

    // Note actions bottom sheet — shown when user taps "..." on a grid card
    bottomSheetNote?.let { note ->
        NoteActionsBottomSheet(
            note = note,
            onDismiss = { bottomSheetNote = null },
            onPin = { viewModel.togglePin(note.id) },
            onDelete = { viewModel.deleteNote(note.id) }
        )
    }
}

/**
 * Action bar shown instead of NavigationBar when one or more notes are selected.
 * Provides select-all, delete, and cancel (deselect) actions.
 */
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
            Row {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onDeselect) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                }
            }
        }
    }
}
