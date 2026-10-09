package com.yourname.simplenotes.ui.notes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.yourname.simplenotes.ui.theme.SakuraBlushBg
import com.yourname.simplenotes.ui.theme.FrostedGlassBgDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBgLight
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderLight
import com.yourname.simplenotes.ui.theme.FrostedGlassTileDark
import com.yourname.simplenotes.ui.theme.FrostedGlassTileLight
import com.yourname.simplenotes.ui.theme.SakuraBlushBgDark
import com.yourname.simplenotes.ui.theme.SakuraBorderSoft
import com.yourname.simplenotes.ui.theme.SakuraBorderSoftDark
import com.yourname.simplenotes.ui.theme.SakuraPink
import com.yourname.simplenotes.ui.theme.SakuraPinkContainer
import com.yourname.simplenotes.ui.theme.SakuraPinkContainerDark
import com.yourname.simplenotes.ui.theme.SakuraPinkLight
import com.yourname.simplenotes.ui.theme.SakuraSurface
import com.yourname.simplenotes.ui.theme.SakuraSurfaceDark
import com.yourname.simplenotes.ui.theme.SakuraTextPrimary
import com.yourname.simplenotes.ui.theme.SakuraTextPrimaryDark
import com.yourname.simplenotes.ui.theme.SakuraTextSecondary
import com.yourname.simplenotes.ui.theme.SakuraTextSecondaryDark
import com.yourname.simplenotes.ui.theme.isAppInDarkTheme
import com.yourname.simplenotes.util.BiometricHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

private const val FOLDER_ALL = "__all__"

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
            tilted        = false,
            compact       = true,
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
    var showSortSheet    by remember { mutableStateOf(false) }

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
                   else if (viewingFolderId == FOLDER_ALL) notes
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
                drawerShape          = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier             = Modifier.width(310.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // ── Header: Bento Brand / Profile Card ─────────────
                    val isDark = isAppInDarkTheme()
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) SakuraPinkContainerDark.copy(alpha = 0.5f) else SakuraPinkContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, SakuraPink.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            if (accountPhotoUrl != null) {
                                AsyncImage(
                                    model             = accountPhotoUrl,
                                    contentDescription = null,
                                    contentScale      = ContentScale.Crop,
                                    modifier          = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, SakuraPink, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(SakuraPink.copy(alpha = 0.18f))
                                        .border(1.2.dp, SakuraPink.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (account?.displayName != null) account.displayName!!.take(1).uppercase() else "🌸",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SakuraPink
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = account?.displayName ?: "Simple Notes",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = account?.email ?: "Sổ tay cá nhân & Ý tưởng",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── Scrollable middle section ──────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DrawerSectionLabel("GHI CHÚ")
                        DrawerNavItem(
                            icon     = Icons.AutoMirrored.Rounded.Notes,
                            label    = "Tất cả ghi chú",
                            count    = totalNoteCount,
                            selected = viewingFolderId == FOLDER_ALL && !pinnedOnly && selectedLabel == null,
                            onClick  = {
                                viewingFolderId = FOLDER_ALL
                                viewModel.setPinnedOnly(false)
                                viewModel.setLabelFilter(null)
                                scope.launch { drawerState.close() }
                            }
                        )
                        val pinnedCount = remember(notes) { notes.count { it.isPinned } }
                        DrawerNavItem(
                            icon     = Icons.Rounded.PushPin,
                            label    = "Đã ghim",
                            count    = pinnedCount.takeIf { it > 0 },
                            selected = pinnedOnly,
                            onClick  = {
                                viewingFolderId = null
                                viewModel.setPinnedOnly(true)
                                viewModel.setLabelFilter(null)
                                scope.launch { drawerState.close() }
                            }
                        )
                        if (categories.isNotEmpty()) {
                            val drawerUnassignedCount = remember(notes) { notes.count { it.folderId == null } }
                            DrawerNavItem(
                                icon     = Icons.Rounded.FolderOpen,
                                label    = "Chưa phân loại",
                                count    = drawerUnassignedCount,
                                selected = viewingFolderId == null && !pinnedOnly && selectedLabel == null,
                                onClick  = {
                                    viewingFolderId = null
                                    viewModel.setPinnedOnly(false)
                                    viewModel.setLabelFilter(null)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }

                        // ── Nhãn (Tags) section ────────────────────────
                        if (allLabels.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(4.dp))
                            DrawerSectionLabel("NHÃN")
                            allLabels.forEachIndexed { index, label ->
                                val tagCount = remember(notes, label) { notes.count { it.labels.contains(label) } }
                                DrawerTagItem(
                                    color    = DRAWER_TAG_COLORS[index % DRAWER_TAG_COLORS.size],
                                    label    = "#$label",
                                    count    = tagCount.takeIf { it > 0 },
                                    selected = selectedLabel == label,
                                    onClick  = {
                                        viewingFolderId = null
                                        viewModel.setPinnedOnly(false)
                                        viewModel.setLabelFilter(label)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        }

                        // ── Thư mục (Folders) section ─────────────────
                        if (categories.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "THƯ MỤC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SakuraPink,
                                    modifier = Modifier.weight(1f)
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
                        }
                    }

                    // ── System section (Pinned at Bottom) ──────────────
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(6.dp))
                    DrawerNavItem(
                        icon     = Icons.Rounded.DeleteOutline,
                        label    = "Thùng rác",
                        count    = deletedNotes.size.takeIf { it > 0 },
                        selected = showRecycleBin,
                        onClick  = {
                            showRecycleBin = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavItem(
                        icon     = Icons.Rounded.Settings,
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
            SettingsScreen(
                onBack               = { showSettings = false },
                onThemeChange        = onThemeChange,
                onDynamicColorChange = onDynamicColorChange,
                onImportNotes        = { viewModel.importNotes(it) },
                onImportArchive      = { viewModel.importArchive(it) }
            )
            return@ModalNavigationDrawer
        }

        Scaffold(
            containerColor = if (isAppInDarkTheme()) SakuraBlushBgDark else SakuraBlushBg,
            bottomBar = {
                AnimatedContent(targetState = isSelectionMode, label = "bottom_bar") { inSelect ->
                    if (inSelect) {
                        val allSelectedLocked = remember(selectedNotes, notes) {
                            selectedNotes.isNotEmpty() &&
                            selectedNotes.all { id -> notes.find { it.id == id }?.isLocked == true }
                        }
                        val allNoteIds = remember(currentNotes) { currentNotes.map { it.id }.toSet() }
                        PhotoEventsSelectionBar(
                            selectedCount = selectedNotes.size,
                            onCancel      = { exitSelectionMode() },
                            onSelectAll   = {
                                if (selectedNotes == allNoteIds) {
                                    exitSelectionMode()
                                } else {
                                    selectedNotes = allNoteIds
                                }
                            },
                            onMove   = { moveTargetNoteIds = selectedNotes.toList() },
                            onLock   = {
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
                            },
                            onDelete = { showBulkDeleteConfirm = true }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    PhotoEventsFab(onClick = { onNewNote(viewingFolderId) })
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(if (isAppInDarkTheme()) SakuraBlushBgDark else SakuraBlushBg)
            ) {
                // ── Top Header: PhotoEvents Header (Sakura Brand Box + Drawer Menu + Search + Sort Pill) ──
                val currentSortLabel = when (sortField) {
                    SortField.DATE_MODIFIED -> if (!sortAscending) "Mới nhất" else "Cũ nhất"
                    SortField.DATE_CREATED  -> if (!sortAscending) "Mới tạo" else "Tạo cũ"
                    SortField.TITLE         -> if (sortAscending) "A → Z" else "Z → A"
                }

                PhotoEventsHeader(
                    sortLabel = currentSortLabel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSearchClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) searchQuery = ""
                    },
                    onSortClick = { showSortSheet = true }
                )

                // ── Search bar (toggleable) ──
                if (showSearchBar) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    LaunchedEffect(Unit) {
                        searchFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    val isDark = isAppInDarkTheme()
                    val searchBg = if (isDark) SakuraSurfaceDark else SakuraSurface
                    val searchBorder = if (isDark) SakuraBorderSoftDark else SakuraBorderSoft

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .shadow(3.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(searchBg)
                            .border(1.2.dp, searchBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = SakuraPink, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        BasicSearchField(
                            query         = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier      = Modifier.weight(1f).focusRequester(searchFocusRequester)
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close, null,
                                tint     = if (isDark) SakuraTextSecondaryDark else SakuraTextSecondary,
                                modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                // ── Pinned Category 2-Row Grid Strip (PhotoEvents 2-row strip) ──
                val unassignedCount = remember(notes) { notes.count { it.folderId == null } }
                PhotoEventsCategoryStrip(
                    categories           = categories,
                    categoryCounts       = categoryCounts,
                    unassignedNotesCount = unassignedCount,
                    totalNotesCount      = notes.size,
                    selectedCategoryId   = viewingFolderId,
                    onSelectCategory     = { viewingFolderId = it },
                    onAddCategoryClick   = { showCreateFolderDialog = true },
                    onEditCategoryClick  = { folderToEdit = it },
                    onReorderCategories  = { ids -> viewModel.reorderCategories(ids) }
                )

                // ── Notes list / grid within Pull-to-refresh wrapper ──
                Box(Modifier.fillMaxSize().nestedScroll(pullRefreshState.nestedScrollConnection).clipToBounds()) {
                    if (currentNotes.isEmpty()) {
                        PhotoEventsEmptyState(modifier = Modifier.align(Alignment.Center))
                    } else if (viewType == NoteViewType.GRID) {
                        LazyVerticalStaggeredGrid(
                            columns               = StaggeredGridCells.Fixed(2),
                            modifier              = Modifier.fillMaxSize(),
                            contentPadding        = PaddingValues(horizontal = 10.dp, vertical = 4.dp).let {
                                PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 80.dp)
                            },
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing   = 10.dp
                        ) {
                            items(currentNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note          = note,
                                    isSelected    = selectedNotes.contains(note.id),
                                    onClick       = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else handleNoteClick(note.id)
                                    },
                                    onLongPress   = {
                                        if (isSelectionMode) toggleSelection(note.id)
                                        else {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                                            enterSelectionMode(note.id)
                                        }
                                    },
                                    onShowActions = { bottomSheetNote = note },
                                    headerStyle   = headerStyle,
                                    tilted        = true
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

                    // Pull-to-refresh indicator (overlays top of the Box)
                    PullToRefreshContainer(
                        state    = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
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
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { colorPickerNote = null },
            containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
                RoundedCornerShape(28.dp)
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SakuraPink.copy(alpha = 0.12f))
                        .border(1.dp, SakuraPink.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, null, tint = SakuraPink, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "Đổi màu nền ghi chú",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        color = Color(note.backgroundColor),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0x1FFFFFFF) else SakuraBorderSoft.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌸", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = note.title.ifBlank { "Xem trước màu ghi chú" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (Color(note.backgroundColor) == Color.White) Color.Black else Color.Black.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    com.yourname.simplenotes.ui.editor.NoteColorPicker(
                        selectedColor = note.backgroundColor,
                        onColorSelected = { color ->
                            viewModel.saveNote(note.copy(backgroundColor = color, isDirty = true, updatedAt = System.currentTimeMillis()))
                            colorPickerNote = null
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { colorPickerNote = null },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SakuraPink, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xong", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }

    deleteConfirmNote?.let { note ->
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { deleteConfirmNote = null },
            containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
                RoundedCornerShape(28.dp)
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "Xóa ghi chú",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc muốn chuyển ghi chú \"${note.title.ifBlank { "Ghi chú" }}\" vào thùng rác không?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val doDelete = { viewModel.deleteNote(note.id); deleteConfirmNote = null }
                        if (note.isLocked) {
                            BiometricHelper.authenticateWithDeviceCredential(
                                activity = context as FragmentActivity,
                                title    = "Xác thực để xóa ghi chú đã khóa",
                                onSuccess = { doDelete() },
                                onError   = { deleteConfirmNote = null }
                            )
                        } else doDelete()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text("Xóa", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleteConfirmNote = null },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        val hasLockedNote = selectedNotes.any { id -> notes.find { it.id == id }?.isLocked == true }
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
                RoundedCornerShape(28.dp)
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "Xóa ${selectedNotes.size} ghi chú",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (hasLockedNote)
                        "Danh sách có ghi chú đã khóa. Bạn cần xác thực để chuyển ${selectedNotes.size} ghi chú đã chọn vào thùng rác."
                    else
                        "Bạn có chắc muốn chuyển ${selectedNotes.size} ghi chú đã chọn vào thùng rác không?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text("Xóa tất cả", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBulkDeleteConfirm = false },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showNoPasscodeDialog) {
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { showNoPasscodeDialog = false },
            containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
                RoundedCornerShape(28.dp)
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SakuraPink.copy(alpha = 0.12f))
                        .border(1.dp, SakuraPink.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LockReset, null, tint = SakuraPink, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "Chưa có mật khẩu thiết bị",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Thiết bị chưa có mã PIN hoặc mật khẩu màn hình khoá. Vui lòng cài đặt bảo mật trong Cài đặt hệ thống để sử dụng tính năng khóa ghi chú.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoPasscodeDialog = false
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SakuraPink, contentColor = Color.White)
                ) {
                    Text("Đến Cài đặt", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showNoPasscodeDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    moveTargetNoteIds?.let { ids ->
        val initialFolderId = ids.mapNotNull { id -> notes.find { it.id == id }?.folderId }
            .distinct()
            .singleOrNull()
        var pickedFolderId by remember(ids) { mutableStateOf(initialFolderId) }
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { moveTargetNoteIds = null },
            containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
                RoundedCornerShape(28.dp)
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SakuraPink.copy(alpha = 0.12f))
                        .border(1.dp, SakuraPink.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = SakuraPink, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = if (ids.size > 1) "Chuyển ${ids.size} ghi chú vào thư mục" else "Chuyển vào thư mục",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
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
                Button(
                    onClick = {
                        viewModel.moveNotes(ids, pickedFolderId)
                        moveTargetNoteIds = null
                        if (ids.size > 1) exitSelectionMode()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SakuraPink, contentColor = Color.White)
                ) {
                    Text("Chuyển", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { moveTargetNoteIds = null },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showSortSheet) {
        PhotoEventsSortBottomSheet(
            currentSortField     = sortField,
            currentSortAscending = sortAscending,
            currentViewType      = viewType,
            onSelectSort         = { field, asc ->
                sortField     = field
                sortAscending = asc
            },
            onToggleViewType     = {
                viewModel.setViewType(
                    if (viewType == NoteViewType.GRID) NoteViewType.LIST else NoteViewType.GRID
                )
            },
            onDismiss            = { showSortSheet = false }
        )
    }

    if (showCreateFolderDialog) {
        PhotoEventsCreateCategoryDialog(
            onSave = { name, color ->
                viewModel.addCategory(name, color)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    folderToDelete?.let { folder ->
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
                RoundedCornerShape(28.dp)
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "Xóa thư mục",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa thư mục \"${folder.name}\" không? Các ghi chú bên trong vẫn được giữ lại và chuyển về mục chưa phân loại.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
                        if (viewingFolderId == folder.id) {
                            viewingFolderId = null
                            isSelectionMode = false
                            selectedNotes = emptySet()
                        }
                        folderToDelete = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text("Xóa", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { folderToDelete = null },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    folderToEdit?.let { folder ->
        PhotoEventsEditCategoryDialog(
            category = folder,
            onSave   = { updated ->
                viewModel.updateCategory(folder.id, updated.name, updated.colorArgb)
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
        val cellHeight = cellWidth * 0.75f
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
                                detectTapGestures(
                                    onTap = { onFolderClick(category.id) }
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
    // Concept 2: Glassmorphism Bento Card
    // Frosted glass look with vibrant glowing category-tinted border,
    // ambient colored glow shadow, mini folder icon, and clean pill badge.
    val categoryColor = remember(category.colorArgb) { Color(category.colorArgb) }
    val scale by animateFloatAsState(if (isDragging) 1.08f else 1f, label = "folderCardScale")
    val cardShape = RoundedCornerShape(14.dp)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderBrush = remember(categoryColor) {
        Brush.linearGradient(
            0.0f to categoryColor.copy(alpha = 0.85f),
            0.5f to categoryColor.copy(alpha = 0.30f),
            1.0f to categoryColor.copy(alpha = 0.70f),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        // Ambient glow halo
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 2.dp)
                .clip(cardShape)
                .background(categoryColor.copy(alpha = if (isDragging) 0.35f else 0.16f))
        )

        // Glassmorphic Card Container
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = if (isDragging) 8.dp else 3.dp,
                    shape = cardShape,
                    ambientColor = categoryColor.copy(alpha = 0.35f),
                    spotColor = categoryColor.copy(alpha = 0.45f)
                )
                .clip(cardShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            surfaceColor.copy(alpha = 0.92f),
                            surfaceColor.copy(alpha = 0.80f)
                        )
                    )
                )
                .background(categoryColor.copy(alpha = 0.05f))
                .border(width = 1.2.dp, brush = borderBrush, shape = cardShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 7.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Folder icon + Count pill with options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.14f))
                            .clickable(onClick = onMoreClick)
                            .padding(horizontal = 4.dp, vertical = 1.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = noteCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn thư mục",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // Bottom row: Category name
                Text(
                    text = category.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        IconButton(
            onClick  = onToggleView,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                if (viewType == NoteViewType.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                contentDescription = if (viewType == NoteViewType.GRID) "List view" else "Grid view",
                modifier = Modifier.size(18.dp),
                tint     = tint
            )
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FOLDER_COLOR_PALETTE.first()) }
    val isDark = isAppInDarkTheme()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(
            BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
            RoundedCornerShape(28.dp)
        ),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(SakuraPink.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, SakuraPink.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SakuraPink,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                "Tạo thư mục mới",
                fontWeight = FontWeight.Bold,
                color = SakuraPink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên thư mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    "Màu thư mục",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SakuraPink
                )
                FolderColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
            ) { Text("Lưu", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
            ) { Text("Hủy") }
        }
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
    val isDark = isAppInDarkTheme()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(
            BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
            RoundedCornerShape(28.dp)
        ),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(SakuraPink.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, SakuraPink.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SakuraPink,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                "Chỉnh sửa thư mục",
                fontWeight = FontWeight.Bold,
                color = SakuraPink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên thư mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    "Màu thư mục",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SakuraPink
                )
                FolderColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Xóa thư mục", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), selectedColor) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
            ) { Text("Lưu", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
            ) { Text("Hủy") }
        }
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
        fontSize   = 11.sp,
        fontWeight = FontWeight.Bold,
        color      = SakuraPink,
        modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/** Bento-styled nav row for the drawer with Sakura accents and soft rounded icons. */
@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    count: Int? = null,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val bg = if (selected) {
        if (isDark) SakuraPink.copy(alpha = 0.18f)
        else SakuraPink.copy(alpha = 0.12f)
    } else Color.Transparent
    val border = if (selected) {
        if (isDark) SakuraPink.copy(alpha = 0.40f)
        else SakuraPink.copy(alpha = 0.30f)
    } else Color.Transparent
    val content = if (selected) SakuraPink else MaterialTheme.colorScheme.onSurface
    val iconBg = if (selected) {
        SakuraPink.copy(alpha = 0.22f)
    } else {
        if (isDark) Color(0x22FFFFFF) else SakuraPink.copy(alpha = 0.08f)
    }
    val iconTint = if (selected) {
        SakuraPink
    } else {
        if (isDark) Color(0xFFE2D6DC) else SakuraPink.copy(alpha = 0.85f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (selected) Modifier.border(1.dp, border, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconBg)
                .then(
                    if (selected) Modifier.border(1.dp, SakuraPink.copy(alpha = 0.35f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = content, modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (count != null) {
            DrawerCount(count = count, isSelected = selected)
        }
    }
}

/** Right-aligned count pill shared by all drawer rows. */
@Composable
private fun DrawerCount(count: Int, isSelected: Boolean = false) {
    val isDark = isAppInDarkTheme()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) SakuraPink.copy(alpha = 0.22f)
                else if (isDark) Color(0x22FFFFFF)
                else SakuraPink.copy(alpha = 0.08f)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            "$count",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) SakuraPink else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Soft rounded tag row for a label filter shortcut in the drawer. */
@Composable
private fun DrawerTagItem(
    color: Color,
    label: String,
    selected: Boolean,
    count: Int? = null,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val bg = if (selected) {
        if (isDark) color.copy(alpha = 0.18f)
        else color.copy(alpha = 0.12f)
    } else Color.Transparent
    val border = if (selected) color.copy(alpha = 0.40f) else Color.Transparent
    val content = if (selected) color else MaterialTheme.colorScheme.onSurface
    val iconBg = if (selected) color.copy(alpha = 0.24f) else color.copy(alpha = 0.12f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (selected) Modifier.border(1.dp, border, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconBg)
                .then(
                    if (selected) Modifier.border(1.dp, color.copy(alpha = 0.40f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalOffer,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = content,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (count != null) {
            DrawerCount(count = count, isSelected = selected)
        }
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
    val categoryColor = Color(node.category.colorArgb)
    val isDark = isAppInDarkTheme()
    val bg = if (isSelected) {
        if (isDark) categoryColor.copy(alpha = 0.18f)
        else categoryColor.copy(alpha = 0.12f)
    } else Color.Transparent
    val border = if (isSelected) categoryColor.copy(alpha = 0.40f) else Color.Transparent
    val iconBg = if (isSelected) categoryColor.copy(alpha = 0.24f) else categoryColor.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (isSelected) Modifier.border(1.dp, border, RoundedCornerShape(16.dp)) else Modifier)
            .clickable { onFolderClick(node.category.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp + (depth * 14).dp,
                    end = 10.dp,
                    top = 5.dp,
                    bottom = 5.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                .background(iconBg)
                .then(
                    if (isSelected) Modifier.border(1.dp, categoryColor.copy(alpha = 0.40f), CircleShape)
                    else Modifier
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                node.category.name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) (if (isDark) categoryColor else SakuraPink)
                        else MaterialTheme.colorScheme.onSurface
            )
            if (count > 0) {
                DrawerCount(count = count, isSelected = isSelected)
            }
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
