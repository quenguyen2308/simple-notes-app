package com.yourname.simplenotes.ui.notes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.yourname.simplenotes.data.importer.ArchiveFormat
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.settings.SettingsPrefs
import com.yourname.simplenotes.ui.settings.SettingsScreen
import com.yourname.simplenotes.ui.theme.FOLDER_COLOR_PALETTE
import com.yourname.simplenotes.ui.theme.HeaderStyle
import com.yourname.simplenotes.util.BiometricHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

private data class FolderNode(val category: Category, val children: List<FolderNode>)

private fun buildFolderTree(categories: List<Category>): List<FolderNode> {
    val byParent = categories.groupBy { it.parentId }
    fun nodes(parentId: String?): List<FolderNode> =
        (byParent[parentId] ?: emptyList())
            .sortedBy { it.order }
            .map { cat -> FolderNode(cat, nodes(cat.id)) }
    return nodes(null)
}

enum class SortField(val label: String) {
    DATE_MODIFIED("Date modified"),
    DATE_CREATED("Date created"),
    TITLE("Title")
}

/**
 * 2-column masonry layout: notes are zig-zag assigned to columns by index,
 * so each column's card heights vary naturally with content (no
 * LazyVerticalStaggeredGrid available at the pinned Compose Foundation version).
 */
@Composable
private fun MasonryNoteGrid(
    notes: List<Note>,
    selectedNotes: Set<String>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    onShowActions: (Note) -> Unit,
    headerStyle: HeaderStyle,
    modifier: Modifier = Modifier,
    columns: Int = 2
) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(columns) { col ->
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                notes.filterIndexed { i, _ -> i % columns == col }.forEach { note ->
                    NoteCard(
                        note          = note,
                        isSelected    = selectedNotes.contains(note.id),
                        onClick       = { onNoteClick(note) },
                        onLongPress   = { onNoteLongPress(note) },
                        onShowActions = { onShowActions(note) },
                        headerStyle   = headerStyle,
                        tilted        = true
                    )
                }
            }
        }
    }
}

/**
 * List-mode row: a colored dot in a left gutter, threaded together by a vertical line running
 * through the whole list, next to the note card. [isFirst]/[isLast] trim the line so it starts
 * and ends at the dot instead of overshooting into empty space above/below the list.
 */
@Composable
private fun TimelineNoteRow(
    note: Note,
    isSelected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onShowActions: () -> Unit,
    headerStyle: HeaderStyle,
    modifier: Modifier = Modifier
) {
    val dotColor = remember(note.backgroundColor) {
        val argb = note.backgroundColor
        if (argb == 0xFFFFFFFF.toInt() || argb == 0) null else Color(argb)
    } ?: MaterialTheme.colorScheme.outline
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(modifier = Modifier.width(28.dp).fillMaxHeight()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            drawLine(
                color       = lineColor,
                start       = Offset(centerX, if (isFirst) centerY else 0f),
                end         = Offset(centerX, if (isLast) centerY else size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(color = dotColor, radius = 5.dp.toPx(), center = Offset(centerX, centerY))
        }
        NoteCard(
            note          = note,
            isSelected    = isSelected,
            onClick       = onClick,
            onLongPress   = onLongPress,
            onShowActions = onShowActions,
            headerStyle   = headerStyle,
            modifier      = Modifier.weight(1f).padding(top = 3.dp, end = 8.dp, bottom = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNoteClick: (String) -> Unit,
    onNewNote: (String?) -> Unit,
    onSearchClick: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    viewModel: NoteListViewModel = koinViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val viewType by viewModel.viewType.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val totalNoteCount by viewModel.totalNoteCount.collectAsStateWithLifecycle()
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle()
    val selectedLabel by viewModel.selectedLabel.collectAsStateWithLifecycle()
    val pinnedOnly by viewModel.pinnedOnly.collectAsStateWithLifecycle()
    val archiveImportOutcome by viewModel.archiveImportOutcome.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullToRefreshState()

    // Trigger sync when user pulls down, end indicator when sync finishes
    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.onResume()
            // Wait for sync to start then finish, timeout 5s as safety net
            kotlinx.coroutines.withTimeoutOrNull(2_000) {
                viewModel.isSyncing.first { it }   // wait until running
                viewModel.isSyncing.first { !it }  // wait until done
            }
            pullRefreshState.endRefresh()
        }
    }
    val context = LocalContext.current
    val view = LocalView.current
    val account = remember { GoogleSignIn.getLastSignedInAccount(context) }
    val accountPhotoUrl = account?.photoUrl

    LaunchedEffect(archiveImportOutcome) {
        val outcome = archiveImportOutcome ?: return@LaunchedEffect
        val message = when (outcome) {
            is ArchiveImportOutcome.Success -> {
                val result = outcome.result
                val formatLabel = when (result.format) {
                    ArchiveFormat.BACKUP -> "EasyNotes .backup"
                    ArchiveFormat.SPLIT_TEXT -> ".zip"
                    ArchiveFormat.UNKNOWN -> "file"
                }
                if (result.notes.isEmpty()) {
                    "Không tìm thấy ghi chú nào để nhập trong $formatLabel"
                } else {
                    buildString {
                        append("Đã nhập ${result.notes.size} ghi chú từ $formatLabel")
                        if (result.skippedTrashed > 0) append(", bỏ qua ${result.skippedTrashed} ghi chú trong thùng rác")
                    }
                }
            }
            ArchiveImportOutcome.Failed -> "Không đọc được file — kiểm tra lại định dạng .backup/.zip"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.clearArchiveImportOutcome()
    }

    var viewingFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings    by remember { mutableStateOf(false) }
    val settingsPrefs   = remember { SettingsPrefs(context) }
    var headerStyle by remember { mutableStateOf(HeaderStyle.fromStorageKey(settingsPrefs.headerStyle)) }
    // Settings is shown as an overlay within this same composable (not a nav route), so re-read
    // the picked style each time it's dismissed — there's no other signal that it may have changed.
    LaunchedEffect(showSettings) {
        if (!showSettings) headerStyle = HeaderStyle.fromStorageKey(settingsPrefs.headerStyle)
    }
    var showRecycleBin  by remember { mutableStateOf(false) }
    var searchQuery     by remember { mutableStateOf("") }
    var showSearchBar   by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
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
    // Target note ids for the "move to folder" dialog — a single id from the note's own
    // bottom sheet, or multiple ids from the selection action bar's bulk move button.
    var moveTargetNoteIds by remember { mutableStateOf<List<String>?>(null) }
    var colorPickerNote  by remember { mutableStateOf<Note?>(null) }
    var folderToDelete   by remember { mutableStateOf<Category?>(null) }
    var folderToEdit     by remember { mutableStateOf<Category?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Back: recycle bin/settings → main, else search collapses before folder → home
    BackHandler(enabled = showSearchBar || viewingFolderId != null || showSettings || showRecycleBin) {
        when {
            showRecycleBin -> showRecycleBin = false
            showSettings -> showSettings = false
            showSearchBar -> {
                showSearchBar = false
                searchQuery   = ""
            }
            viewingFolderId != null -> {
                viewingFolderId = null
                isSelectionMode = false
                selectedNotes   = emptySet()
                searchQuery     = ""
            }
        }
    }

    // Trigger sync whenever the screen resumes (app comes to foreground)
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    // notes is unfiltered (selectedCategoryId stays null); we filter in UI
    val totalNotes       = notes.size
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
            SortField.DATE_MODIFIED -> compareBy { it.contentUpdatedAt }
            SortField.DATE_CREATED  -> compareBy { it.createdAt }
            SortField.TITLE         -> compareBy { it.title.lowercase() }
        }
        val sorted = filtered.sortedWith(comparator)
        val ordered = if (sortAscending) sorted else sorted.reversed()
        ordered.sortedByDescending { it.isPinned }
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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape          = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier             = Modifier.width(300.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    // ── Header: account avatar + name/email ─────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        if (accountPhotoUrl != null) {
                            AsyncImage(
                                model             = accountPhotoUrl,
                                contentDescription = null,
                                contentScale      = ContentScale.Crop,
                                modifier          = Modifier.size(64.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (account?.displayName ?: "?").take(1).uppercase(),
                                    fontSize   = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                account?.displayName ?: "Người dùng",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            if (account?.email != null) {
                                Text(
                                    account.email!!,
                                    fontSize = 14.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    // ── Main section: Ghi chú ────────────────────────────
                    DrawerSectionLabel("Ghi chú")
                    DrawerNavItem(
                        icon     = Icons.AutoMirrored.Filled.Notes,
                        label    = "Tất cả ghi chú",
                        count    = totalNoteCount,
                        selected = viewingFolderId == null && !pinnedOnly && selectedLabel == null,
                        onClick  = {
                            viewingFolderId = null
                            viewModel.setPinnedOnly(false)
                            viewModel.setLabelFilter(null)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavItem(
                        icon     = Icons.Default.PushPin,
                        label    = "Đã ghim",
                        selected = pinnedOnly,
                        onClick  = {
                            viewingFolderId = null
                            viewModel.setPinnedOnly(true)
                            viewModel.setLabelFilter(null)
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    // ── Tags section: Nhãn ────────────────────────────────
                    if (allLabels.isNotEmpty()) {
                        DrawerSectionLabel("Nhãn")
                        allLabels.forEachIndexed { index, label ->
                            DrawerTagItem(
                                color    = DRAWER_TAG_COLORS[index % DRAWER_TAG_COLORS.size],
                                label    = "#$label",
                                selected = selectedLabel == label,
                                onClick  = {
                                    viewingFolderId = null
                                    viewModel.setPinnedOnly(false)
                                    viewModel.setLabelFilter(label)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Folders section ───────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "THƯ MỤC", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
                        )
                        DrawerCount(categories.size)
                    }
                    val folderTree = remember(categories) { buildFolderTree(categories) }
                    folderTree.forEach { node ->
                        FolderDrawerItem(
                            node             = node,
                            depth            = 0,
                            categoryCounts   = categoryCounts,
                            selectedFolderId = viewingFolderId,
                            onFolderClick    = { id ->
                                viewingFolderId = id
                                viewModel.setPinnedOnly(false)
                                viewModel.setLabelFilter(null)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // ── System section ────────────────────────────────────
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    DrawerNavItem(
                        icon     = Icons.Default.Delete,
                        label    = "Thùng rác",
                        count    = deletedNotes.size.takeIf { it > 0 },
                        selected = showRecycleBin,
                        onClick  = {
                            showRecycleBin = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavItem(
                        icon     = Icons.Default.Settings,
                        label    = "Cài đặt",
                        selected = showSettings,
                        onClick  = {
                            showSettings = true
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        // ── Recycle Bin overlay ──────────────────────────────────────
        if (showRecycleBin) {
            RecycleBinScreen(
                notes          = deletedNotes,
                onRestore      = { viewModel.restore(it) },
                onPermanentDelete = { viewModel.permanentDelete(it) },
                onClearAll     = { viewModel.clearRecycleBin() },
                onBack         = { showRecycleBin = false }
            )
            return@ModalNavigationDrawer
        }

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
                SettingsScreen(
                    onThemeChange = onThemeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onImportNotes = { viewModel.importNotes(it) },
                    onImportArchive = { viewModel.importArchive(it) }
                )
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
                        val allNoteIds = remember(currentNotes) { currentNotes.map { it.id }.toSet() }
                        SelectionActionBar(
                            headerStyle      = headerStyle,
                            selectedCount    = selectedNotes.size,
                            allSelected       = selectedNotes.isNotEmpty() && selectedNotes == allNoteIds,
                            allSelectedLocked = allSelectedLocked,
                            onSelectAll      = {
                                if (selectedNotes == allNoteIds) {
                                    // Already all selected — tapping again clears the selection.
                                    exitSelectionMode()
                                } else {
                                    selectedNotes = allNoteIds
                                }
                            },
                            onMoveToFolder   = { moveTargetNoteIds = selectedNotes.toList() },
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
                    StyledFab(style = headerStyle, onClick = { onNewNote(viewingFolderId) })
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
                Box(
                    modifier          = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 4.dp),
                    contentAlignment  = Alignment.Center
                ) {
                    val title = if (viewingFolderId == null) "Folders" else currentFolder?.name ?: ""
                    val subtitle = if (viewingFolderId == null)
                        "${categories.size} folders, $totalNotes notes"
                    else
                        "${currentNotes.size} notes"
                    StyledNoteListHeader(style = headerStyle, title = title, subtitle = subtitle)
                }

                // ── Toolbar row ─────────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledIconButton(
                        style = headerStyle,
                        onClick = { scope.launch { drawerState.open() } },
                        icon = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                    Spacer(Modifier.weight(1f))
                    StyledIconButton(
                        style = headerStyle,
                        onClick = {
                            if (showSearchBar) {
                                showSearchBar = false
                                searchQuery = ""
                            } else {
                                showSearchBar = true
                            }
                        },
                        icon = Icons.Default.Search,
                        contentDescription = "Tìm kiếm"
                    )
                    Box {
                        StyledIconButton(
                            style = headerStyle,
                            onClick = { showMoreMenu = true },
                            icon = Icons.Default.MoreVert,
                            contentDescription = "More"
                        )
                        DropdownMenu(
                            expanded        = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Edit") },
                                onClick = { isSelectionMode = true; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("Create folder") },
                                onClick = { showCreateFolderDialog = true; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("Unpin favourites from top") },
                                onClick = {
                                    viewModel.unpinNotes(currentNotes.filter { it.isPinned }.map { it.id })
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }

                // ── Search bar — collapsed to an icon until tapped, no avatar (Settings
                // lives in the drawer) ────────────────────────────────────────────
                if (showSearchBar) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    LaunchedEffect(Unit) {
                        searchFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        BasicSearchField(
                            query         = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier      = Modifier.weight(1f).focusRequester(searchFocusRequester)
                        )
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

                // ── Pull-to-refresh wrapper ──────────────────────────
                Box(Modifier.fillMaxSize().nestedScroll(pullRefreshState.nestedScrollConnection).clipToBounds()) {

                // ── HOME: folder grid + sort bar + unfiled notes ──────
                if (viewingFolderId == null) {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (categories.isNotEmpty()) {
                            item {
                                FolderGrid(
                                    categories     = categories,
                                    categoryCounts = categoryCounts,
                                    onFolderClick  = { id -> viewingFolderId = id },
                                    onFolderMoreClick = { id ->
                                        folderToEdit = categories.find { it.id == id }
                                    },
                                    onReorder = { ids -> viewModel.reorderCategories(ids) }
                                )
                            }
                        }

                        item {
                            NotesSortBar(
                                sortField     = sortField,
                                sortAscending = sortAscending,
                                viewType      = viewType,
                                headerStyle   = headerStyle,
                                onSortField   = { sortField = it },
                                onToggleDir   = { sortAscending = !sortAscending },
                                onToggleView  = {
                                    viewModel.setViewType(
                                        if (viewType == NoteViewType.GRID) NoteViewType.LIST else NoteViewType.GRID
                                    )
                                }
                            )
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
                        } else if (viewType == NoteViewType.GRID) {
                            item {
                                MasonryNoteGrid(
                                    notes         = currentNotes,
                                    selectedNotes = selectedNotes,
                                    onNoteClick   = { note ->
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else handleNoteClick(note.id)
                                    },
                                    onNoteLongPress = { note ->
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                                            enterSelectionMode(note.id)
                                        }
                                    },
                                    onShowActions = { note -> bottomSheetNote = note },
                                    headerStyle   = headerStyle
                                )
                            }
                        } else {
                            itemsIndexed(currentNotes, key = { _, it -> it.id }) { index, note ->
                                TimelineNoteRow(
                                    note       = note,
                                    isSelected = selectedNotes.contains(note.id),
                                    isFirst    = index == 0,
                                    isLast     = index == currentNotes.lastIndex,
                                    onClick  = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else handleNoteClick(note.id)
                                    },
                                    onLongPress = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                                            enterSelectionMode(note.id)
                                        }
                                    },
                                    onShowActions = { bottomSheetNote = note },
                                    headerStyle = headerStyle,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }

                // ── FOLDER: sort bar + notes grid ────────────────────
                } else {
                    Column(Modifier.fillMaxSize()) {
                    NotesSortBar(
                        sortField     = sortField,
                        sortAscending = sortAscending,
                        viewType      = viewType,
                        headerStyle   = headerStyle,
                        onSortField   = { sortField = it },
                        onToggleDir   = { sortAscending = !sortAscending },
                        onToggleView  = {
                            viewModel.setViewType(
                                if (viewType == NoteViewType.GRID) NoteViewType.LIST else NoteViewType.GRID
                            )
                        }
                    )
                    if (currentNotes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Chưa có ghi chú trong thư mục này",
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.padding(32.dp)
                            )
                        }
                    } else if (viewType == NoteViewType.GRID) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                MasonryNoteGrid(
                                    notes         = currentNotes,
                                    selectedNotes = selectedNotes,
                                    onNoteClick   = { note ->
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else handleNoteClick(note.id)
                                    },
                                    onNoteLongPress = { note ->
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                                            enterSelectionMode(note.id)
                                        }
                                    },
                                    onShowActions = { note -> bottomSheetNote = note },
                                    headerStyle   = headerStyle
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            itemsIndexed(currentNotes, key = { _, it -> it.id }) { index, note ->
                                TimelineNoteRow(
                                    note       = note,
                                    isSelected = selectedNotes.contains(note.id),
                                    isFirst    = index == 0,
                                    isLast     = index == currentNotes.lastIndex,
                                    onClick  = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else handleNoteClick(note.id)
                                    },
                                    onLongPress = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                                            enterSelectionMode(note.id)
                                        }
                                    },
                                    onShowActions = { bottomSheetNote = note },
                                    headerStyle = headerStyle,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                    } // end folder Column
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
            note           = note,
            onDismiss      = { bottomSheetNote = null },
            onPin          = { viewModel.togglePin(note.id) },
            onDelete       = { deleteConfirmNote = note },
            onMoveToFolder = { moveTargetNoteIds = listOf(note.id) },
            onChangeColor  = { colorPickerNote = note },
            onLock         = {
                fun applyLock(locked: Boolean) {
                    viewModel.saveNote(note.copy(isLocked = locked, isDirty = true, updatedAt = System.currentTimeMillis()))
                }
                if (note.isLocked) {
                    BiometricHelper.authenticateWithDeviceCredential(
                        activity = context as FragmentActivity,
                        title    = "Mở khóa ghi chú",
                        onSuccess = { applyLock(false) },
                        onError   = {}
                    )
                } else if (!BiometricHelper.isDeviceSecure(context)) {
                    showNoPasscodeDialog = true
                } else {
                    applyLock(true)
                }
            }
        )
    }

    colorPickerNote?.let { note ->
        AlertDialog(
            onDismissRequest = { colorPickerNote = null },
            title   = { Text("Change color") },
            text    = {
                com.yourname.simplenotes.ui.editor.NoteColorPicker(
                    selectedColor   = note.backgroundColor,
                    onColorSelected = { color ->
                        // updatedAt MUST bump here: cross-device sync is last-write-wins on
                        // updatedAt (see SyncWorker), so leaving it unchanged means the color
                        // change can silently lose to (or get overwritten by) another device.
                        viewModel.saveNote(note.copy(backgroundColor = color, isDirty = true, updatedAt = System.currentTimeMillis()))
                        colorPickerNote = null
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { colorPickerNote = null }) { Text("Done") }
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

    moveTargetNoteIds?.let { ids ->
        // Pre-select the common folder when every target note already shares one.
        val initialFolderId = ids.mapNotNull { id -> notes.find { it.id == id }?.folderId }
            .distinct()
            .singleOrNull()
        var pickedFolderId by remember(ids) { mutableStateOf(initialFolderId) }
        AlertDialog(
            onDismissRequest = { moveTargetNoteIds = null },
            title   = { Text(if (ids.size > 1) "Chuyển ${ids.size} ghi chú vào thư mục" else "Chuyển thư mục") },
            text    = {
                Column {
                    com.yourname.simplenotes.ui.folder.FolderBrowser(
                        folders          = categories,
                        selectedFolderId = pickedFolderId,
                        onFolderSelect   = { pickedFolderId = it },
                        onFolderLongPress = {},
                        modifier         = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.moveNotes(ids, pickedFolderId)
                    moveTargetNoteIds = null
                    if (ids.size > 1) exitSelectionMode()
                }) { Text("Chuyển") }
            },
            dismissButton = {
                TextButton(onClick = { moveTargetNoteIds = null }) { Text("Hủy") }
            }
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

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title   = { Text("Delete folder") },
            text    = { Text("Delete \"${folder.name}\"? Notes inside will be moved to root.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    if (viewingFolderId == folder.id) {
                        viewingFolderId = null
                        isSelectionMode = false
                        selectedNotes = emptySet()
                    }
                    folderToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text("Cancel") } }
        )
    }

    folderToEdit?.let { folder ->
        EditFolderDialog(
            folder  = folder,
            onSave  = { name, color ->
                viewModel.updateCategory(folder.id, name, color)
                folderToEdit = null
            },
            onDelete = {
                folderToDelete = folder
                folderToEdit = null
            },
            onDismiss = { folderToEdit = null }
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

/**
 * A 4-column grid of folder cards that supports long-press drag-and-drop reordering.
 * Cards are laid out with absolute pixel offsets (rather than a Row/Column flow or
 * LazyVerticalGrid) so each card's position can be independently animated as the drag
 * reshuffles [orderedIds] — non-dragged cards slide smoothly into their new slot while
 * the dragged card tracks the finger directly. The final order is reported via
 * [onReorder] once the drag ends, and persisted through [NoteListViewModel.reorderCategories].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderGrid(
    categories: List<Category>,
    categoryCounts: Map<String, Int>,
    onFolderClick: (String) -> Unit,
    onFolderMoreClick: (String) -> Unit = {},
    onReorder: (List<String>) -> Unit = {}
) {
    if (categories.isEmpty()) return
    val columns = 4
    val spacing = 8.dp
    val byId = remember(categories) { categories.associateBy { it.id } }

    var orderedIds by remember { mutableStateOf(categories.map { it.id }) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    // Accumulated finger movement since drag start, plus the dragged card's slot position
    // at drag start — kept separate so reshuffling orderedIds mid-drag (which moves the
    // dragged item's own index) never feeds back into its own rendered position.
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffsetPx by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(categories) {
        if (draggingId == null) orderedIds = categories.map { it.id }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val density = LocalDensity.current
        val cellWidth = (maxWidth - spacing * (columns - 1)) / columns
        val cellHeight = cellWidth * 3f / 5f
        val cellWidthPx = with(density) { cellWidth.toPx() }
        val cellHeightPx = with(density) { cellHeight.toPx() }
        val spacingPx = with(density) { spacing.toPx() }
        val rows = (orderedIds.size + columns - 1) / columns
        val totalHeight = cellHeight * rows + spacing * (rows - 1).coerceAtLeast(0)

        Box(Modifier.fillMaxWidth().height(totalHeight)) {
            orderedIds.forEachIndexed { index, id ->
                key(id) {
                    val category = byId[id]
                    if (category != null) {
                    val isDragging = id == draggingId
                    val col = index % columns
                    val row = index / columns
                    val targetXPx = (cellWidthPx + spacingPx) * col
                    val targetYPx = (cellHeightPx + spacingPx) * row
                    val animatedX by animateFloatAsState(targetXPx, label = "folderX")
                    val animatedY by animateFloatAsState(targetYPx, label = "folderY")

                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellHeight)
                            .offset {
                                if (isDragging) {
                                    IntOffset(
                                        (dragStartOffsetPx.x + dragOffsetPx.x).roundToInt(),
                                        (dragStartOffsetPx.y + dragOffsetPx.y).roundToInt()
                                    )
                                } else {
                                    IntOffset(animatedX.roundToInt(), animatedY.roundToInt())
                                }
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .pointerInput(id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        val startIndex = orderedIds.indexOf(id)
                                        if (startIndex >= 0) {
                                            draggingId = id
                                            dragOffsetPx = Offset.Zero
                                            dragStartOffsetPx = Offset(
                                                (cellWidthPx + spacingPx) * (startIndex % columns),
                                                (cellHeightPx + spacingPx) * (startIndex / columns)
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffsetPx = Offset.Zero
                                        onReorder(orderedIds)
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffsetPx = Offset.Zero
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffsetPx += amount
                                        val currentIndex = orderedIds.indexOf(id)
                                        if (currentIndex >= 0) {
                                            val rawX = dragStartOffsetPx.x + dragOffsetPx.x
                                            val rawY = dragStartOffsetPx.y + dragOffsetPx.y
                                            val targetCol = (rawX / (cellWidthPx + spacingPx))
                                                .roundToInt().coerceIn(0, columns - 1)
                                            val targetRow = (rawY / (cellHeightPx + spacingPx))
                                                .roundToInt().coerceIn(0, rows - 1)
                                            val targetIndex = (targetRow * columns + targetCol)
                                                .coerceIn(0, orderedIds.lastIndex)
                                            if (targetIndex != currentIndex) {
                                                orderedIds = orderedIds.toMutableList().apply {
                                                    add(targetIndex, removeAt(currentIndex))
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        FolderCard(
                            category    = category,
                            noteCount   = categoryCounts[category.id] ?: 0,
                            onClick     = { onFolderClick(category.id) },
                            onMoreClick = { onFolderMoreClick(category.id) },
                            isDragging  = isDragging
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    category: Category,
    noteCount: Int,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    isDragging: Boolean = false
) {
    // Same "Bàn Làm Việc" sticky-note treatment as note cards: a small stable per-folder tilt
    // (derived from the folder's own id, so it doesn't reshuffle on recomposition) plus a flat,
    // hard-edged paper shadow tinted with the folder's own color instead of a soft blur.
    val tiltDeg = remember(category.id) {
        val h = ((category.id.hashCode() % 10_000) + 10_000) % 10_000
        (h / 10_000f) * 4.4f - 2.2f // roughly -2.2°..+2.2°
    }
    val shadowColor = remember(category.colorArgb) { Color(category.colorArgb).darken(0.4f) }
    val scale by animateFloatAsState(if (isDragging) 1.06f else 1f, label = "folderCardScale")

    Box(modifier = Modifier.graphicsLayer(rotationZ = tiltDeg, scaleX = scale, scaleY = scale)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shadowColor)
        )
        Card(
            shape      = RoundedCornerShape(16.dp),
            colors     = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation  = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 0.dp),
            modifier   = Modifier.aspectRatio(5f / 3f).clickable(onClick = onClick)
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Color strip: top-right, width=3/5, height=1/5, bottom-left corner rounded
            val ribbonColor = Color(category.colorArgb)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stripW = size.width * 0.6f
                val stripH = size.height * 0.2f
                val r = stripH * 0.7f            // radius matches bottom-left roundness
                val left = size.width - stripW
                val path = Path().apply {
                    moveTo(left, 0f)                         // top-left (square)
                    lineTo(size.width, 0f)                   // top-right
                    lineTo(size.width, stripH)               // bottom-right
                    lineTo(left + r, stripH)                 // bottom edge to arc tangent
                    arcTo(
                        rect = Rect(left, stripH - 2 * r, left + 2 * r, stripH),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(left, 0f)                         // up left edge
                    close()
                }
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
            // Options (rename/color/delete) — moved off long-press so long-press is free to
            // start a drag-to-reorder gesture instead.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f))
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Tùy chọn thư mục",
                    tint = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        }
    }
}

@Composable
private fun NotesSortBar(
    sortField: SortField,
    sortAscending: Boolean,
    viewType: NoteViewType,
    headerStyle: HeaderStyle,
    onSortField: (SortField) -> Unit,
    onToggleDir: () -> Unit,
    onToggleView: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    val tint = styledIconTint(headerStyle)

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Sort, null,
            modifier = Modifier.size(15.dp),
            tint     = tint
        )
        Spacer(Modifier.width(4.dp))

        // Sort field label — tap to pick field
        Box {
            Text(
                sortField.label,
                fontSize = 12.sp,
                color    = tint,
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
            tint     = tint
        )

        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Spacer(Modifier.width(8.dp))

        // Grid / List toggle
        Icon(
            if (viewType == NoteViewType.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
            contentDescription = if (viewType == NoteViewType.GRID) "List view" else "Grid view",
            modifier = Modifier.size(16.dp).clickable { onToggleView() },
            tint     = tint
        )
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FOLDER_COLOR_PALETTE.first()) }
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
                FolderColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
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

/** Edit dialog for an existing folder: rename + change its color range, opened via long-press. */
@Composable
private fun EditFolderDialog(
    folder: Category,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedColor by remember { mutableStateOf(folder.colorArgb) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Edit Folder") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Text(
                    "Màu thư mục",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FolderColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                TextButton(
                    onClick  = onDelete,
                    modifier = Modifier.align(Alignment.Start)
                ) { Text("Xóa thư mục", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank()) onSave(name, selectedColor) },
                enabled  = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Scrollable grid of folder color swatches, shared by create + edit folder dialogs. */
@Composable
private fun FolderColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(FOLDER_COLOR_PALETTE) { colorInt ->
            Surface(
                color    = Color(colorInt),
                shape    = CircleShape,
                onClick  = { onColorSelected(colorInt) },
                modifier = Modifier.size(32.dp),
                border   = if (selectedColor == colorInt)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                else null
            ) {}
        }
    }
}

/** Pastel dot colors cycled through the "Nhãn" (tags) list in the drawer. */
private val DRAWER_TAG_COLORS = listOf(
    Color(0xFFFFB3C6), Color(0xFFA8D8F0), Color(0xFFD4C5F9),
    Color(0xFFA8E6B0), Color(0xFFFFC178), Color(0xFF8DE0D0)
)

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text,
        fontSize   = 12.sp,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** Pill-shaped nav row for the drawer (mirrors the left-nav reference: 56dp, fully rounded, selected = primaryContainer). */
@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    count: Int? = null,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            label, fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = content, modifier = Modifier.weight(1f)
        )
        if (count != null) {
            DrawerCount(count)
        }
    }
}

/** Right-aligned count label shared by all drawer rows so digits line up regardless of row type or digit count. */
@Composable
private fun DrawerCount(count: Int) {
    Text(
        "$count",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.widthIn(min = 20.dp),
        textAlign = TextAlign.End
    )
}

/** Colored-dot row for a label/tag filter shortcut in the drawer. */
@Composable
private fun DrawerTagItem(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(16.dp))
        Text(
            label, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FolderDrawerItem(
    node: FolderNode,
    depth: Int,
    categoryCounts: Map<String, Int>,
    selectedFolderId: String?,
    onFolderClick: (String) -> Unit
) {
    val isSelected = selectedFolderId == node.category.id
    val count = categoryCounts[node.category.id] ?: 0
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable { onFolderClick(node.category.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp + (depth * 16).dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder, null,
                tint     = Color(node.category.colorArgb),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                node.category.name,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
            )
            DrawerCount(count)
        }
    }
    node.children.forEach { child ->
        FolderDrawerItem(
            node             = child,
            depth            = depth + 1,
            categoryCounts   = categoryCounts,
            selectedFolderId = selectedFolderId,
            onFolderClick    = onFolderClick
        )
    }
}

/** Selection action bar shown when notes are selected. */
@Composable
private fun SelectionActionBar(
    headerStyle: HeaderStyle,
    selectedCount: Int,
    allSelected: Boolean,
    allSelectedLocked: Boolean,
    onSelectAll: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDeselect: () -> Unit,
    onDelete: () -> Unit,
    onLock: () -> Unit
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("$selectedCount đã chọn", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = onSelectAll, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = if (allSelected) "Bỏ chọn tất cả" else "Chọn tất cả",
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalIconButton(onClick = onMoveToFolder, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, "Chuyển thư mục", modifier = Modifier.size(20.dp))
                }
                FilledTonalIconButton(onClick = onLock, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (allSelectedLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (allSelectedLocked) "Mở khóa" else "Khóa",
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalIconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor   = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, "Xóa", modifier = Modifier.size(20.dp))
                }
                FilledTonalIconButton(onClick = onDeselect, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, "Bỏ chọn", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
