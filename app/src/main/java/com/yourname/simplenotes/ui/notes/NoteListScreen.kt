package com.yourname.simplenotes.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.settings.SettingsScreen
import com.yourname.simplenotes.util.BiometricHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val SamsungBlue = Color(0xFF1259C3)

enum class SortField(val label: String) {
    DATE_MODIFIED("Date modified"),
    DATE_CREATED("Date created"),
    TITLE("Title")
}

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
    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullToRefreshState()

    // Trigger sync when user pulls down, end indicator when sync finishes
    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.onResume()
            // Wait for sync to start then finish, timeout 10s as safety net
            kotlinx.coroutines.withTimeoutOrNull(10_000) {
                viewModel.isSyncing.first { it }   // wait until running
                viewModel.isSyncing.first { !it }  // wait until done
            }
            kotlinx.coroutines.delay(300)
            pullRefreshState.endRefresh()
        }
    }
    val context = LocalContext.current

    var viewingFolderId by remember { mutableStateOf<String?>(null) }
    var showSettings    by remember { mutableStateOf(false) }
    var showSearchBar   by remember { mutableStateOf(false) }
    var searchQuery     by remember { mutableStateOf("") }
    var showMoreMenu    by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var sortField       by remember { mutableStateOf(SortField.DATE_MODIFIED) }
    var sortAscending   by remember { mutableStateOf(false) }
    var selectedNotes   by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var bottomSheetNote by remember { mutableStateOf<Note?>(null) }
    var deleteConfirmNote by remember { mutableStateOf<Note?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showNoPasscodeDialog  by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Back: folder → home, settings → main
    BackHandler(enabled = viewingFolderId != null || showSettings) {
        when {
            viewingFolderId != null -> {
                viewingFolderId = null
                isSelectionMode = false
                selectedNotes   = emptySet()
                searchQuery     = ""
            }
            showSettings -> showSettings = false
        }
    }

    // Trigger sync whenever the screen resumes (app comes to foreground)
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    // notes is unfiltered (selectedCategoryId stays null); we filter in UI
    val totalNotes       = notes.size
    val lockedNotesCount = remember(notes) { notes.count { it.isLocked } }
    val currentFolder    = remember(viewingFolderId, categories) { categories.find { it.id == viewingFolderId } }

    val currentNotes = remember(notes, viewingFolderId, searchQuery, sortField, sortAscending) {
        val base = if (viewingFolderId == null) notes.filter { it.folderId == null }
                   else notes.filter { it.folderId == viewingFolderId }
        val filtered = if (searchQuery.isEmpty()) base
                       else base.filter {
                           it.title.contains(searchQuery, ignoreCase = true) ||
                           it.content.contains(searchQuery, ignoreCase = true)
                       }
        val comparator: Comparator<Note> = when (sortField) {
            SortField.DATE_MODIFIED -> compareBy { it.updatedAt }
            SortField.DATE_CREATED  -> compareBy { it.createdAt }
            SortField.TITLE         -> compareBy { it.title.lowercase() }
        }
        val sorted = filtered.sortedWith(comparator)
        if (sortAscending) sorted else sorted.reversed()
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
            title    = "Mở khóa ghi chú",
            onSuccess = { onNoteClick(noteId) },
            onError   = {}
        )
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(Modifier.height(16.dp))
                // Gear icon
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        showSettings = true
                        scope.launch { drawerState.close() }
                    }) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // All notes
                NavigationDrawerItem(
                    icon   = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    label  = { Text("All notes") },
                    badge  = { Text("$totalNotes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                    selected = false,
                    onClick = { viewingFolderId = null; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon   = { Icon(Icons.Default.Description, null) },
                    label  = { Text("Old format notes") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon   = { Icon(Icons.Default.Lock, null) },
                    label  = { Text("Locked notes") },
                    badge  = { Text("$lockedNotesCount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon   = { Icon(Icons.Default.Share, null) },
                    label  = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Shared notes")
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color  = Color.DarkGray,
                                shape  = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "BETA",
                                    fontSize = 9.sp,
                                    color    = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    selected = false,
                    onClick  = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon   = { Icon(Icons.Default.Delete, null) },
                    label  = { Text("Recycle bin") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Dotted separator
                Spacer(Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    drawLine(
                        color       = Color.Gray.copy(alpha = 0.4f),
                        start       = Offset(0f, 0f),
                        end         = Offset(size.width, 0f),
                        strokeWidth = 2f,
                        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Folders section header
                val totalFolderNotes = categories.sumOf { categoryCounts[it.id] ?: 0 }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Folders", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$totalFolderNotes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                // Folder list
                categories.forEach { folder ->
                    val count = categoryCounts[folder.id] ?: 0
                    NavigationDrawerItem(
                        icon   = {
                            Icon(
                                Icons.Default.Folder, null,
                                tint     = Color(folder.colorArgb),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label  = { Text(folder.name) },
                        badge  = { Text("$count", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                        selected = viewingFolderId == folder.id,
                        onClick  = {
                            viewingFolderId = folder.id
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        // ── Settings overlay ─────────────────────────────────────────
        if (showSettings) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Row(
                    modifier           = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment  = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showSettings = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(
                        "Settings",
                        style    = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                SettingsScreen(onThemeChange = onThemeChange)
            }
            return@ModalNavigationDrawer
        }

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
                            selectedCount    = selectedNotes.size,
                            allSelectedLocked = allSelectedLocked,
                            onSelectAll      = { selectedNotes = currentNotes.map { it.id }.toSet() },
                            onDeselect       = { exitSelectionMode() },
                            onDelete         = { showBulkDeleteConfirm = true },
                            onLock = {
                                if (!BiometricHelper.isDeviceSecure(context)) {
                                    showNoPasscodeDialog = true
                                } else {
                                    BiometricHelper.authenticateWithDeviceCredential(
                                        activity = context as FragmentActivity,
                                        title    = if (allSelectedLocked) "Mở khóa ghi chú" else "Khóa ghi chú",
                                        onSuccess = {
                                            viewModel.lockNotes(selectedNotes.toList(), locked = !allSelectedLocked)
                                            exitSelectionMode()
                                        },
                                        onError = {}
                                    )
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(
                        onClick        = { onNewNote(viewingFolderId) },
                        containerColor = SamsungBlue,
                        contentColor   = Color.White,
                        shape          = CircleShape,
                        modifier       = Modifier.size(48.dp)
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
                // ── Large centered header ────────────────────────────
                Column(
                    modifier              = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 4.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Text(
                        text       = if (viewingFolderId == null) "Folders" else currentFolder?.name ?: "",
                        fontSize   = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                    val subtitle = if (viewingFolderId == null)
                        "${categories.size} folders, $totalNotes notes"
                    else
                        "${currentNotes.size} notes"
                    Text(
                        text      = subtitle,
                        fontSize  = 13.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Toolbar row ─────────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.weight(1f))
                    if (viewingFolderId != null) {
                        IconButton(onClick = { /* export PDF – future */ }) {
                            Icon(Icons.Default.Description, "Export PDF", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    IconButton(onClick = { showSearchBar = !showSearchBar; if (!showSearchBar) searchQuery = "" }) {
                        Icon(
                            Icons.Default.Search, "Search",
                            tint = if (showSearchBar) SamsungBlue else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded        = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Edit") },
                                onClick = { showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("View") },
                                onClick = { showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("Create folder") },
                                onClick = { showCreateFolderDialog = true; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("Unpin favourites from top") },
                                onClick = { showMoreMenu = false }
                            )
                        }
                    }
                }

                // ── Search bar ───────────────────────────────────────
                if (showSearchBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        BasicSearchField(query = searchQuery, onQueryChange = { searchQuery = it }, modifier = Modifier.weight(1f))
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                // ── Breadcrumb (folder screen only) ─────────────────
                if (viewingFolderId != null) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FolderOpen, null,
                            modifier = Modifier.size(18.dp).clickable { viewingFolderId = null },
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = currentFolder?.name ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ── Sort bar ────────────────────────────────────────
                NotesSortBar(
                    sortField     = sortField,
                    sortAscending = sortAscending,
                    onSortField   = { sortField = it },
                    onToggleDir   = { sortAscending = !sortAscending }
                )

                // ── Pull-to-refresh wrapper ──────────────────────────
                Box(Modifier.fillMaxSize().nestedScroll(pullRefreshState.nestedScrollConnection)) {

                // ── HOME: folder grid + unfiled notes ────────────────
                if (viewingFolderId == null) {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (categories.isNotEmpty()) {
                            item {
                                FolderGrid(
                                    categories    = categories,
                                    categoryCounts = categoryCounts,
                                    onFolderClick  = { id -> viewingFolderId = id }
                                )
                            }
                        }

                        if (currentNotes.isEmpty()) {
                            item {
                                Box(
                                    modifier       = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Chưa có ghi chú\nNhấn + để tạo mới",
                                        color       = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign   = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(currentNotes.chunked(3)) { row ->
                                Row(
                                    modifier             = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { note ->
                                        Box(Modifier.weight(1f)) {
                                            NoteCard(
                                                note     = note,
                                                isSelected = selectedNotes.contains(note.id),
                                                onClick  = {
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
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }

                // ── FOLDER: notes grid ───────────────────────────────
                } else {
                    if (currentNotes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Chưa có ghi chú trong thư mục này",
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.padding(32.dp)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns              = GridCells.Fixed(3),
                            modifier             = Modifier.fillMaxSize(),
                            contentPadding       = PaddingValues(horizontal = 8.dp, vertical = 4.dp, ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            items(currentNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note     = note,
                                    isSelected = selectedNotes.contains(note.id),
                                    onClick  = {
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
                    }
                }

                // Pull-to-refresh indicator (overlays top of the Box)
                PullToRefreshContainer(
                    state    = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                } // end pull-to-refresh Box
            }
        }
    }

    // ── Dialogs & sheets ─────────────────────────────────────────────

    bottomSheetNote?.let { note ->
        NoteActionsBottomSheet(
            note      = note,
            onDismiss = { bottomSheetNote = null },
            onPin     = { viewModel.togglePin(note.id) },
            onDelete  = { deleteConfirmNote = note },
            onLock    = {
                viewModel.saveNote(note.copy(isLocked = !note.isLocked, isDirty = true, updatedAt = System.currentTimeMillis()))
            }
        )
    }

    deleteConfirmNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteConfirmNote = null },
            title   = { Text("Xóa ghi chú") },
            text    = { Text("Bạn có chắc muốn xóa \"${note.title.ifBlank { "Ghi chú" }}\" không?") },
            confirmButton = {
                TextButton(onClick = {
                    val doDelete = { viewModel.deleteNote(note.id); deleteConfirmNote = null }
                    if (note.isLocked) {
                        BiometricHelper.authenticateWithDeviceCredential(
                            activity = context as FragmentActivity,
                            title    = "Xác thực để xóa ghi chú đã khóa",
                            onSuccess = { doDelete() },
                            onError   = { deleteConfirmNote = null }
                        )
                    } else doDelete()
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmNote = null }) { Text("Hủy") } }
        )
    }

    if (showBulkDeleteConfirm) {
        val hasLockedNote = selectedNotes.any { id -> notes.find { it.id == id }?.isLocked == true }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title   = { Text("Xóa ghi chú") },
            text    = {
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
                            title    = "Xác thực để xóa ghi chú đã khóa",
                            onSuccess = { doDelete() },
                            onError   = { showBulkDeleteConfirm = false }
                        )
                    } else doDelete()
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Hủy") } }
        )
    }

    if (showNoPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showNoPasscodeDialog = false },
            title   = { Text("Chưa có mật khẩu thiết bị") },
            text    = { Text("Thiết bị chưa có mật khẩu màn hình khoá. Vui lòng cài đặt PIN hoặc mật khẩu trong Cài đặt.") },
            confirmButton = {
                TextButton(onClick = {
                    showNoPasscodeDialog = false
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                }) { Text("Đến Cài đặt") }
            },
            dismissButton = { TextButton(onClick = { showNoPasscodeDialog = false }) { Text("Hủy") } }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { name, color ->
                viewModel.addCategory(name, color)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
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
        value       = query,
        onValueChange = onQueryChange,
        singleLine  = true,
        textStyle   = androidx.compose.ui.text.TextStyle(
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.onBackground
        ),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) Text("Tìm kiếm ghi chú", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun FolderGrid(
    categories: List<Category>,
    categoryCounts: Map<String, Int>,
    onFolderClick: (String) -> Unit
) {
    Column(
        modifier             = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement  = Arrangement.spacedBy(8.dp)
    ) {
        categories.chunked(4).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
                    Box(Modifier.weight(1f)) {
                        FolderCard(
                            category  = category,
                            noteCount = categoryCounts[category.id] ?: 0,
                            onClick   = { onFolderClick(category.id) }
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FolderCard(
    category: Category,
    noteCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick    = onClick,
        shape      = RoundedCornerShape(12.dp),
        colors     = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation  = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier   = Modifier.aspectRatio(0.85f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Colored diagonal ribbon at top-right
            val ribbonColor = Color(category.colorArgb)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                path.moveTo(size.width * 0.32f, 0f)
                path.lineTo(size.width, 0f)
                path.lineTo(size.width, size.height * 0.52f)
                path.close()
                drawPath(path, ribbonColor)
            }
            // Note count – top left
            Text(
                text     = noteCount.toString(),
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
            )
            // Folder name – bottom left
            Text(
                text     = category.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(7.dp)
            )
        }
    }
}

@Composable
private fun NotesSortBar(
    sortField: SortField,
    sortAscending: Boolean,
    onSortField: (SortField) -> Unit,
    onToggleDir: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Sort, null,
            modifier = Modifier.size(15.dp),
            tint     = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))

        // Sort field label — tap to pick field
        Box {
            Text(
                sortField.label,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { showDropdown = true }
            )
            DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                SortField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                field.label,
                                fontWeight = if (field == sortField) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = { onSortField(field); showDropdown = false }
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Spacer(Modifier.width(8.dp))

        // Arrow — tap to toggle asc/desc
        Icon(
            if (sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            null,
            modifier = Modifier.size(15.dp).clickable { onToggleDir() },
            tint     = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF1976D2.toInt()) }
    val colorOptions = listOf(
        0xFF1976D2.toInt(), 0xFF388E3C.toInt(), 0xFFD32F2F.toInt(),
        0xFF7B1FA2.toInt(), 0xFFF57C00.toInt(), 0xFF0097A7.toInt()
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Create Folder") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { colorInt ->
                        Surface(
                            color    = Color(colorInt),
                            shape    = CircleShape,
                            onClick  = { selectedColor = colorInt },
                            modifier = Modifier.size(32.dp),
                            border   = if (selectedColor == colorInt)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                            else null
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled  = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
        modifier  = Modifier.fillMaxWidth(),
        color     = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("$selectedCount đã chọn", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
            Row {
                IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Chọn tất cả") }
                IconButton(onClick = onLock) {
                    Icon(
                        if (allSelectedLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        if (allSelectedLocked) "Mở khóa" else "Khóa"
                    )
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = onDeselect) { Icon(Icons.Default.Close, "Bỏ chọn") }
            }
        }
    }
}
