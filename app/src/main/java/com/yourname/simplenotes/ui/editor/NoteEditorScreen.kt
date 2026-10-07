package com.yourname.simplenotes.ui.editor

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.text.HtmlCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.simplenotes.util.BiometricHelper
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/** Small pill chip for the time/tag row under the title (e.g. "14:30, Hôm nay", "#Tag"). */
@Composable
private fun EditorChip(
    text: String,
    color: Color,
    background: Color,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    onBack: () -> Unit,
    initialCategoryId: String? = null,
    sharedText: String? = null,
    viewModel: NoteEditorViewModel = koinViewModel()
) {
    LaunchedEffect(noteId) { viewModel.load(noteId, initialCategoryId, sharedText) }

    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showOverflowMenu      by remember { mutableStateOf(false) }
    var showColorDialog       by remember { mutableStateOf(false) }
    var showCategoryDialog    by remember { mutableStateOf(false) }
    var showLabelsDialog      by remember { mutableStateOf(false) }
    var showNoPasscodeDialog  by remember { mutableStateOf(false) }
    var showDetailsDialog     by remember { mutableStateOf(false) }
    /** Reference to the EditText inside AndroidRichTextEditor, used by the toolbar. */
    var editTextRef            by remember { mutableStateOf<EditText?>(null) }

    // The EditText is a native View hosted via AndroidView, not a Compose text field, so
    // removing it from composition on back-navigation does NOT auto-dismiss its IME session —
    // the keyboard stays visible over the note list until manually dismissed here.
    fun hideKeyboard() {
        val et = editTextRef ?: return
        (et as? ScrollAwareEditText)?.flushPendingHtml()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(et.windowToken, 0)
    }

    val topBarLabel = remember(viewModel.createdAtMs) {
        val ms = if (viewModel.createdAtMs > 0L) viewModel.createdAtMs else System.currentTimeMillis()
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
        val today = Calendar.getInstance()
        val noteDay = Calendar.getInstance().apply { timeInMillis = ms }
        val dateLabel = if (noteDay.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                            noteDay.get(Calendar.YEAR) == today.get(Calendar.YEAR))
                            "Hôm nay"
                        else
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ms))
        "$timeStr · $dateLabel"
    }

    // Word and character count statistics for note info
    val wordCount = remember(viewModel.htmlContent, viewModel.checklistItems, viewModel.isChecklistMode) {
        val text = if (viewModel.isChecklistMode) {
            viewModel.checklistItems.joinToString(" ") { it.text }
        } else {
            HtmlCompat.fromHtml(viewModel.htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        }
        if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
    }
    val charCount = remember(viewModel.htmlContent, viewModel.checklistItems, viewModel.isChecklistMode) {
        val text = if (viewModel.isChecklistMode) {
            viewModel.checklistItems.joinToString("") { it.text }
        } else {
            HtmlCompat.fromHtml(viewModel.htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        }
        text.length
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(backDispatcher) {
        val cb = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { hideKeyboard(); viewModel.save(); onBack() }
        }
        backDispatcher?.addCallback(cb)
        onDispose { cb.remove() }
    }

    // Auto-save on app pause/stop (e.g. user hits Home or switches apps) to prevent data loss
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                hideKeyboard()
                viewModel.save()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addImage(it.toString()) }
    }

    // Pastel note colors are fixed light hues regardless of app theme.
    // In dark mode: use deep obsidian slate tinted with note color to eliminate "flashbang" glare
    // while keeping note color identity; text is high-contrast crisp white.
    // In light mode: use custom pastel background + dark charcoal #1B1B1B text.
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val hasCustomColor = viewModel.backgroundColor != 0xFFFFFFFF.toInt() && viewModel.backgroundColor != 0
    val customColor = if (hasCustomColor) Color(viewModel.backgroundColor) else null

    val editorBg = if (isDark) {
        if (customColor != null) {
            Color(
                red = (0.07f + customColor.red * 0.12f).coerceIn(0f, 1f),
                green = (0.09f + customColor.green * 0.12f).coerceIn(0f, 1f),
                blue = (0.12f + customColor.blue * 0.14f).coerceIn(0f, 1f),
                alpha = 1.0f
            )
        } else {
            MaterialTheme.colorScheme.surface
        }
    } else {
        customColor ?: MaterialTheme.colorScheme.surface
    }

    val onEditorBg = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        if (hasCustomColor) Color(0xFF1B1B1B) else MaterialTheme.colorScheme.onSurface
    }

    val onEditorBgMuted = if (isDark) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        if (hasCustomColor) Color(0xFF1B1B1B).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        containerColor = editorBg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { hideKeyboard(); viewModel.save(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = onEditorBgMuted)
                    }
                },
                title = {},
                actions = {
                    IconButton(onClick = viewModel::onPinToggle) {
                        Icon(
                            Icons.Default.PushPin, "Ghim",
                            modifier = Modifier.size(18.dp),
                            tint = if (viewModel.isPinned) (if (hasCustomColor) onEditorBg else MaterialTheme.colorScheme.primary)
                                   else onEditorBgMuted
                        )
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Thêm", tint = onEditorBgMuted, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (viewModel.isLocked) "Mở khóa ghi chú" else "Khóa ghi chú") },
                                leadingIcon = { Icon(if (viewModel.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    if (viewModel.isLocked) {
                                        BiometricHelper.authenticateWithDeviceCredential(
                                            activity = context as FragmentActivity,
                                            title = "Mở khóa ghi chú",
                                            onSuccess = { viewModel.setLock(locked = false, newPinHash = null) },
                                            onError = {}
                                        )
                                    } else {
                                        if (BiometricHelper.isDeviceSecure(context))
                                            viewModel.setLock(locked = true, newPinHash = null)
                                        else showNoPasscodeDialog = true
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Chia sẻ") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    val text = buildString {
                                        append(viewModel.title); appendLine()
                                        if (viewModel.isChecklistMode) {
                                            viewModel.checklistItems.forEach { item ->
                                                appendLine("${if (item.isCompleted) "☑" else "☐"} ${item.text}")
                                            }
                                        } else {
                                            append(HtmlCompat.fromHtml(viewModel.htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT))
                                        }
                                    }
                                    context.startActivity(Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, viewModel.title)
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }, "Chia sẻ ghi chú"
                                    ))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Danh mục") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                                onClick = { showCategoryDialog = true; showOverflowMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Nhãn") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                                onClick = { showLabelsDialog = true; showOverflowMenu = false }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Chi tiết ghi chú") },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                onClick = { showDetailsDialog = true; showOverflowMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            // Single floating pill toolbar — inset from the screen edges so it reads as one
            // cohesive card over the note's background, not a flat bar cut across the bottom.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp)),
                color           = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                tonalElevation  = 2.dp
            ) {
                EditorToolbar(
                    editText = editTextRef,
                    isChecklistActive = viewModel.isChecklistMode,
                    canUndo = viewModel.canUndo,
                    canRedo = viewModel.canRedo,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onChecklistToggle = viewModel::toggleChecklistMode,
                    onInsertImage = { galleryLauncher.launch("image/*") },
                    onOpenNoteColorPicker = { showColorDialog = true },
                    onHtmlChange = viewModel::onHtmlContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(editorBg)
        ) {
            // Title field — sits directly on the note's color, aligned with body margin
            TextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = {
                    Text(
                        "Tiêu đề",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = onEditorBgMuted
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = onEditorBg
                ),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = onEditorBg,
                    selectionColors = TextSelectionColors(
                        handleColor = onEditorBg,
                        backgroundColor = onEditorBg.copy(alpha = 0.25f)
                    )
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        val et = editTextRef
                        if (et != null) {
                            et.isFocusableInTouchMode = true
                            et.requestFocus()
                            et.setSelection(et.text?.length ?: 0)
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
            )

            // Time + tag chips — also on the colored background, below the title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                EditorChip(
                    text = topBarLabel,
                    color = onEditorBgMuted,
                    background = onEditorBg.copy(alpha = 0.1f),
                    onClick = { showDetailsDialog = true }
                )
                if (viewModel.labels.isEmpty()) {
                    EditorChip(
                        text       = "+ Nhãn",
                        color      = onEditorBgMuted,
                        background = onEditorBg.copy(alpha = 0.1f),
                        onClick    = { showLabelsDialog = true }
                    )
                } else {
                    viewModel.labels.take(2).forEach { label ->
                        EditorChip(
                            text       = "#$label",
                            color      = onEditorBgMuted,
                            background = onEditorBg.copy(alpha = 0.1f),
                            onClick    = { showLabelsDialog = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Image section (only render if note has images attached)
            if (viewModel.imageBlocks.isNotEmpty()) {
                NoteImageSection(
                    imageBlocks = viewModel.imageBlocks,
                    onRemoveImage = viewModel::removeImage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Content area — occupies all remaining vertical and horizontal space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (viewModel.isChecklistMode) {
                    NoteChecklistEditor(
                        items = viewModel.checklistItems,
                        onAddItem = viewModel::addChecklistItem,
                        onRemoveItem = viewModel::removeChecklistItem,
                        onToggleItem = viewModel::toggleChecklistItem,
                        onUpdateItemText = viewModel::updateChecklistItemText,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidRichTextEditor(
                        html = viewModel.htmlContent,
                        onHtmlChange = viewModel::onHtmlContentChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        textColor = onEditorBg,
                        modifier = Modifier.fillMaxSize(),
                        undoRedoVersion = viewModel.undoRedoVersion,
                        onEditTextReady = { editTextRef = it }
                    )
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Màu nền") },
            text = {
                NoteColorPicker(
                    selectedColor = viewModel.backgroundColor,
                    onColorSelected = { viewModel.onBackgroundColorChange(it); showColorDialog = false }
                )
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) { Text("Đóng") }
            }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Chọn danh mục") },
            text = {
                Column {
                    val opts = listOf(null) + categories
                    opts.forEach { cat ->
                        val isActive = cat?.id == viewModel.selectedCategoryId
                        TextButton(
                            onClick = { viewModel.onCategoryChange(cat?.id); showCategoryDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                cat?.name ?: "Không có danh mục",
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Đóng") }
            }
        )
    }

    if (showLabelsDialog) {
        AlertDialog(
            onDismissRequest = { showLabelsDialog = false },
            title = { Text("Nhãn") },
            text = {
                NoteLabelSection(
                    labels = viewModel.labels,
                    onAddLabel = viewModel::addLabel,
                    onRemoveLabel = viewModel::removeLabel
                )
            },
            confirmButton = {
                TextButton(onClick = { showLabelsDialog = false }) { Text("Đóng") }
            }
        )
    }

    if (showNoPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showNoPasscodeDialog = false },
            title = { Text("Chưa có mật khẩu thiết bị") },
            text = { Text("Thiết bị chưa có mật khẩu màn hình khoá. Vui lòng cài đặt PIN, hình vẽ hoặc mật khẩu trong Cài đặt để sử dụng tính năng khóa ghi chú.") },
            confirmButton = {
                TextButton(onClick = {
                    showNoPasscodeDialog = false
                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }) { Text("Đến Cài đặt") }
            },
            dismissButton = {
                TextButton(onClick = { showNoPasscodeDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showDetailsDialog) {
        val createdFormatted = remember(viewModel.createdAtMs) {
            val ms = if (viewModel.createdAtMs > 0L) viewModel.createdAtMs else System.currentTimeMillis()
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ms))
        }
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Chi tiết ghi chú") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Số từ:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$wordCount từ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Số ký tự:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$charCount ký tự", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ngày tạo:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(createdFormatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Định dạng:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (viewModel.isChecklistMode) "Danh sách công việc" else "Văn bản đa dạng",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (viewModel.labels.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nhãn:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                viewModel.labels.joinToString(", ") { "#$it" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) { Text("Đóng") }
            }
        )
    }
}
