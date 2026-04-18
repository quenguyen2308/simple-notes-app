package com.yourname.simplenotes.ui.notes

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.folder.FolderScreen
import com.yourname.simplenotes.ui.settings.SettingsScreen
import com.yourname.simplenotes.util.BiometricHelper
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val SamsungBlue = Color(0xFF1259C3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNoteClick: (String) -> Unit,
    onNewNote: (String?) -> Unit,
    onSearchClick: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    viewModel: NoteListViewModel = koinViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var contentTab    by remember { mutableStateOf(0) } // 0=Tất cả, 1=Đã ghim, 2=Thư mục, 3=Cài đặt
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery   by remember { mutableStateOf("") }
    var selectedNotes by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var bottomSheetNote by remember { mutableStateOf<Note?>(null) }
    var deleteConfirmNote by remember { mutableStateOf<Note?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showNoPasscodeDialog by remember { mutableStateOf(false) }
    var viewingFolderId by remember { mutableStateOf<String?>(null) }
    var viewType by remember { mutableStateOf(NoteViewType.LIST) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(contentTab) {
        if (contentTab != 2) viewingFolderId = null
        if (contentTab != 0 && contentTab != 1) {
            showSearchBar = false
            searchQuery = ""
        }
    }

    val displayedNotes = remember(notes, contentTab, searchQuery) {
        val tabFiltered = if (contentTab == 1) notes.filter { it.isPinned } else notes
        if (searchQuery.isEmpty()) tabFiltered
        else tabFiltered.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    fun enterSelectionMode(noteId: String) { isSelectionMode = true; selectedNotes = setOf(noteId) }
    fun toggleSelection(noteId: String) {
        selectedNotes = if (selectedNotes.contains(noteId)) {
            val u = selectedNotes - noteId
            if (u.isEmpty()) isSelectionMode = false
            u
        } else selectedNotes + noteId
    }
    fun exitSelectionMode() { selectedNotes = emptySet(); isSelectionMode = false }

    fun handleNoteClick(noteId: String) {
        val note = notes.find { it.id == noteId } ?: return
        if (!note.isLocked) { onNoteClick(noteId); return }
        BiometricHelper.authenticateWithDeviceCredential(
            activity = context as FragmentActivity,
            title = "Mở khóa ghi chú",
            onSuccess = { onNoteClick(noteId) },
            onError = {}
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                // Gear icon — opens settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        contentTab = 3
                        scope.launch { drawerState.close() }
                    }) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(4.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    label = { Text("Tất cả ghi chú") },
                    selected = contentTab == 0,
                    onClick = { contentTab = 0; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PushPin, null) },
                    label = { Text("Đã ghim") },
                    selected = contentTab == 1,
                    onClick = { contentTab = 1; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, null) },
                    label = { Text("Thùng rác") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    "Thư mục",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FolderOpen, null) },
                    label = { Text("Quản lý thư mục") },
                    selected = contentTab == 2,
                    onClick = { contentTab = 2; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AnimatedContent(targetState = isSelectionMode, label = "bottom_bar") { inSelect ->
                    if (inSelect) {
                        val allSelectedLocked = remember(selectedNotes, notes) {
                            selectedNotes.isNotEmpty() &&
                            selectedNotes.all { id -> notes.find { it.id == id }?.isLocked == true }
                        }
                        SelectionActionBar(
                            selectedCount = selectedNotes.size,
                            allSelectedLocked = allSelectedLocked,
                            onSelectAll = { selectedNotes = notes.map { it.id }.toSet() },
                            onDeselect = { exitSelectionMode() },
                            onDelete = { showBulkDeleteConfirm = true },
                            onLock = {
                                if (!BiometricHelper.isDeviceSecure(context)) {
                                    showNoPasscodeDialog = true
                                } else {
                                    BiometricHelper.authenticateWithDeviceCredential(
                                        activity = context as FragmentActivity,
                                        title = if (allSelectedLocked) "Mở khóa ghi chú" else "Khóa ghi chú",
                                        onSuccess = {
                                            viewModel.lockNotes(
                                                selectedNotes.toList(),
                                                locked = !allSelectedLocked
                                            )
                                            exitSelectionMode()
                                        },
                                        onError = {}
                                    )
                                }
                            }
                        )
                    } else {
                        SamsungSimpleBottomBar(
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode && contentTab != 2 && contentTab != 3) {
                    FloatingActionButton(
                        onClick = { onNewNote(selectedCategoryId) },
                        containerColor = SamsungBlue,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, "Ghi chú mới", modifier = Modifier.size(22.dp))
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // ── Settings view ───────────────────────────────────────────
                if (contentTab == 3) {
                    SettingsScreen(onThemeChange = onThemeChange)
                    return@Scaffold
                }

                // ── Folder view ─────────────────────────────────────────────
                if (contentTab == 2) {
                    if (viewingFolderId != null) {
                        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    viewingFolderId = null
                                    viewModel.selectCategory(null)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                                }
                                Text(
                                    "Ghi chú trong thư mục",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            if (displayedNotes.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Chưa có ghi chú trong thư mục này",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(32.dp)
                                    )
                                }
                            } else {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(displayedNotes, key = { it.id }) { note ->
                                        NoteListItemFlat(
                                            note = note,
                                            isSelected = false,
                                            onClick = { handleNoteClick(note.id) },
                                            onLongPress = {}
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        FolderScreen(onFolderSelected = { folderId ->
                            viewingFolderId = folderId
                            viewModel.selectCategory(folderId)
                        })
                    }
                    return@Scaffold
                }

                // ── Header: title + toggleable search ───────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val headerTitle = when {
                            contentTab == 1 -> "Đã ghim"
                            selectedCategoryId == null -> "Tất cả ghi chú"
                            else -> categories.find { it.id == selectedCategoryId }?.name ?: "Tất cả ghi chú"
                        }
                        Text(
                            text = headerTitle,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            viewType = if (viewType == NoteViewType.LIST) NoteViewType.GRID else NoteViewType.LIST
                        }) {
                            Icon(
                                if (viewType == NoteViewType.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                                "Chuyển chế độ xem",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        }) {
                            Icon(
                                Icons.Default.Search,
                                "Tìm kiếm",
                                tint = if (showSearchBar) SamsungBlue
                                       else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    if (showSearchBar) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicSearchField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                modifier = Modifier.weight(1f)
                            )
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xóa",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ── Category chips ───────────────────────────────────────────
                if (categories.isNotEmpty()) {
                    DraggableCategoryChips(
                        categories = categories,
                        selectedId = selectedCategoryId,
                        onSelect = { viewModel.selectCategory(it) },
                        onReorder = { viewModel.reorderCategories(it) }
                    )
                }

                if (isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SamsungBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }

                // ── Notes list ──────────────────────────────────────────────
                if (displayedNotes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                isSyncing -> "Đang đồng bộ dữ liệu..."
                                searchQuery.isNotEmpty() -> "Không tìm thấy ghi chú"
                                contentTab == 1 -> "Chưa có ghi chú được ghim"
                                else -> "Chưa có ghi chú\nNhấn + để tạo mới"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else if (viewType == NoteViewType.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedNotes, key = { it.id }) { note ->
                            NoteCard(
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayedNotes, key = { it.id }) { note ->
                            NoteListItemFlat(
                                note = note,
                                isSelected = selectedNotes.contains(note.id),
                                onClick = {
                                    if (isSelectionMode) toggleSelection(note.id)
                                    else handleNoteClick(note.id)
                                },
                                onLongPress = {
                                    if (isSelectionMode) toggleSelection(note.id)
                                    else enterSelectionMode(note.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    bottomSheetNote?.let { note ->
        NoteActionsBottomSheet(
            note = note,
            onDismiss = { bottomSheetNote = null },
            onPin = { viewModel.togglePin(note.id) },
            onDelete = { deleteConfirmNote = note },
            onLock = {
                val updated = note.copy(isLocked = !note.isLocked, isDirty = true)
                viewModel.saveNote(updated)
            }
        )
    }

    deleteConfirmNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteConfirmNote = null },
            title = { Text("Xóa ghi chú") },
            text = { Text("Bạn có chắc muốn xóa \"${note.title.ifBlank { "Ghi chú" }}\" không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val doDelete = {
                            viewModel.deleteNote(note.id)
                            deleteConfirmNote = null
                        }
                        if (note.isLocked) {
                            BiometricHelper.authenticateWithDeviceCredential(
                                activity = context as FragmentActivity,
                                title = "Xác thực để xóa ghi chú đã khóa",
                                onSuccess = { doDelete() },
                                onError = { deleteConfirmNote = null }
                            )
                        } else {
                            doDelete()
                        }
                    }
                ) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmNote = null }) { Text("Hủy") }
            }
        )
    }

    if (showNoPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showNoPasscodeDialog = false },
            title = { Text("Chưa có mật khẩu thiết bị") },
            text = { Text("Thiết bị chưa có mật khẩu màn hình khoá. Vui lòng cài đặt PIN hoặc mật khẩu trong Cài đặt để sử dụng tính năng khóa ghi chú.") },
            confirmButton = {
                TextButton(onClick = {
                    showNoPasscodeDialog = false
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                }) { Text("Đến Cài đặt") }
            },
            dismissButton = {
                TextButton(onClick = { showNoPasscodeDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        val hasLockedNote = selectedNotes.any { id -> notes.find { it.id == id }?.isLocked == true }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Xóa ghi chú") },
            text = {
                Text(
                    if (hasLockedNote)
                        "Danh sách có ghi chú đã khóa. Bạn cần xác thực để xóa ${selectedNotes.size} ghi chú đã chọn."
                    else
                        "Bạn có chắc muốn xóa ${selectedNotes.size} ghi chú đã chọn không?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val doDelete = {
                        viewModel.deleteNotes(selectedNotes.toList())
                        showBulkDeleteConfirm = false
                        exitSelectionMode()
                    }
                    if (hasLockedNote) {
                        BiometricHelper.authenticateWithDeviceCredential(
                            activity = context as FragmentActivity,
                            title = "Xác thực để xóa ghi chú đã khóa",
                            onSuccess = { doDelete() },
                            onError = { showBulkDeleteConfirm = false }
                        )
                    } else {
                        doDelete()
                    }
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Hủy") }
            }
        )
    }
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun BasicSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground
        ),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) {
                    Text("Tìm kiếm ghi chú", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            }
        },
        modifier = modifier
    )
}

/** Simple bottom bar: hamburger menu on left, refresh icon on right. */
@Composable
private fun SamsungSimpleBottomBar(onMenuClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, "Mở menu", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Refresh, "Đồng bộ", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Horizontally scrollable category chips with long-press drag-to-reorder. */
@Composable
private fun DraggableCategoryChips(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedListState = remember { mutableStateOf<List<Category>>(emptyList()) }
    val draggedIndexState = remember { mutableStateOf<Int?>(null) }
    val dragOffsetXState = remember { mutableStateOf(0f) }
    val itemWidths = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(categories) {
        if (draggedIndexState.value == null) {
            orderedListState.value = categories.sortedBy { it.order }
        }
    }

    val orderedList = orderedListState.value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        orderedList.forEachIndexed { index, category ->
            val isDragging = draggedIndexState.value == index
            key(category.id) {
                FilterChip(
                    selected = selectedId == category.id,
                    onClick = { if (draggedIndexState.value == null) onSelect(category.id) },
                    label = { Text(category.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(category.colorArgb),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .onSizeChanged { size -> itemWidths[category.id] = size.width }
                        .alpha(if (isDragging) 0.55f else 1f)
                        .offset {
                            IntOffset(
                                x = if (isDragging) dragOffsetXState.value.roundToInt() else 0,
                                y = 0
                            )
                        }
                        .pointerInput(category.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    val idx = orderedListState.value.indexOfFirst { it.id == category.id }
                                    if (idx >= 0) {
                                        draggedIndexState.value = idx
                                        dragOffsetXState.value = 0f
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetXState.value += dragAmount.x
                                    val currentIdx = draggedIndexState.value ?: return@detectDragGesturesAfterLongPress
                                    val currentList = orderedListState.value
                                    val draggedId = currentList.getOrNull(currentIdx)?.id
                                        ?: return@detectDragGesturesAfterLongPress
                                    val itemW = (itemWidths[draggedId] ?: 0).toFloat() + 8.dp.toPx()
                                    val threshold = itemW * 0.5f
                                    when {
                                        dragOffsetXState.value > threshold && currentIdx < currentList.size - 1 -> {
                                            val newList = currentList.toMutableList()
                                            val swapped = newList.removeAt(currentIdx)
                                            newList.add(currentIdx + 1, swapped)
                                            orderedListState.value = newList
                                            draggedIndexState.value = currentIdx + 1
                                            dragOffsetXState.value -= itemW
                                        }
                                        dragOffsetXState.value < -threshold && currentIdx > 0 -> {
                                            val newList = currentList.toMutableList()
                                            val swapped = newList.removeAt(currentIdx)
                                            newList.add(currentIdx - 1, swapped)
                                            orderedListState.value = newList
                                            draggedIndexState.value = currentIdx - 1
                                            dragOffsetXState.value += itemW
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedIndexState.value = null
                                    dragOffsetXState.value = 0f
                                    onReorder(orderedListState.value.map { it.id })
                                },
                                onDragCancel = {
                                    draggedIndexState.value = null
                                    dragOffsetXState.value = 0f
                                }
                            )
                        }
                )
            }
        }

        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text("Tất cả", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SamsungBlue,
                selectedLabelColor = Color.White
            )
        )
    }
}

/** Selection action bar shown when notes are selected. */
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    allSelectedLocked: Boolean,
    onSelectAll: () -> Unit,
    onDeselect: () -> Unit,
    onDelete: () -> Unit,
    onLock: () -> Unit
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
                "$selectedCount đã chọn",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
            Row {
                IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Chọn tất cả") }
                IconButton(onClick = onLock) {
                    Icon(
                        if (allSelectedLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (allSelectedLocked) "Mở khóa" else "Khóa"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onDeselect) { Icon(Icons.Default.Close, "Bỏ chọn") }
            }
        }
    }
}
