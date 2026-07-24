package com.yourname.simplenotes.ui.editor

import android.content.Intent
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.yourname.simplenotes.util.BiometricHelper
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/** Icon-over-label bottom toolbar button (Image / Checkbox / Màu). */
@Composable
private fun LabeledToolbarButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 10.sp, color = color)
    }
}

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
    viewModel: NoteEditorViewModel = koinViewModel()
) {
    LaunchedEffect(noteId) { viewModel.load(noteId, initialCategoryId) }

    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showOverflowMenu      by remember { mutableStateOf(false) }
    var showColorDialog       by remember { mutableStateOf(false) }
    var showCategoryDialog    by remember { mutableStateOf(false) }
    var showLabelsDialog      by remember { mutableStateOf(false) }
    var showNoPasscodeDialog  by remember { mutableStateOf(false) }

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

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(backDispatcher) {
        val cb = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { viewModel.save(); onBack() }
        }
        backDispatcher?.addCallback(cb)
        onDispose { cb.remove() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addImage(it.toString()) }
    }

    // Pastel note colors are fixed light hues regardless of app theme, so a custom-colored
    // note always uses a light background + dark text — theme surface/onSurface colors would
    // turn dark-background/light-text in dark mode and become unreadable against the pastel.
    val hasCustomColor = viewModel.backgroundColor != 0xFFFFFFFF.toInt() && viewModel.backgroundColor != 0
    val editorBg        = if (hasCustomColor) Color(viewModel.backgroundColor) else MaterialTheme.colorScheme.surface
    val onEditorBg       = if (hasCustomColor) Color(0xFF1B1B1B) else MaterialTheme.colorScheme.onSurface
    val onEditorBgMuted  = if (hasCustomColor) Color(0xFF1B1B1B).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = editorBg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.save(); onBack() }) {
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
                                            append(viewModel.richTextState.annotatedString.text)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Insert image
                    LabeledToolbarButton(
                        icon    = Icons.Default.Image,
                        label   = "Image",
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                    // Toggle checklist / rich-text mode
                    LabeledToolbarButton(
                        icon     = if (viewModel.isChecklistMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        label    = "Checkbox",
                        isActive = viewModel.isChecklistMode,
                        onClick  = viewModel::toggleChecklistMode
                    )
                    // Background color
                    LabeledToolbarButton(
                        icon    = Icons.Default.Palette,
                        label   = "Màu",
                        onClick = { showColorDialog = true }
                    )

                    // Rich-text formatting controls (hidden in checklist mode) share the
                    // same floating pill instead of a second stacked bar.
                    if (!viewModel.isChecklistMode) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(24.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        RichTextFormattingRow(state = viewModel.richTextState)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(editorBg)
        ) {
            // Title field — sits directly on the note's color, not inside the content card
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
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
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
                EditorChip(text = topBarLabel, color = onEditorBgMuted, background = onEditorBg.copy(alpha = 0.1f))
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

            Spacer(Modifier.height(12.dp))

            // Content card — the actual note body floats on a neutral surface, distinct from
            // the colored identity area above (title/time/tags).
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Image section
                    NoteImageSection(
                        imageBlocks = viewModel.imageBlocks,
                        onRemoveImage = viewModel::removeImage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Content area
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
                        com.mohamedrejeb.richeditor.ui.material3.RichTextEditor(
                            state = viewModel.richTextState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = RichTextEditorDefaults.richTextEditorColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
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
}
