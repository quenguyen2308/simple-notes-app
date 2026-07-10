package com.yourname.simplenotes.ui.editor

import android.content.Intent
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.yourname.simplenotes.util.BiometricHelper
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

private val SamsungBlue = Color(0xFF1259C3)
private val BottomBarBg = Color(0xFFF5F5F5)
private val BottomBarBorder = Color(0xFFEEEEEE)
private val IconTint = Color(0xFF555555)

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

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.save(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = Color(0xFF444444))
                    }
                },
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(topBarLabel, fontSize = 13.sp, color = Color(0xFF999999))
                    }
                },
                actions = {
                    IconButton(onClick = {
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
                    }) { Icon(Icons.Default.Share, "Chia sẻ", modifier = Modifier.size(18.dp)) }

                    IconButton(onClick = viewModel::onPinToggle) {
                        Icon(
                            Icons.Default.PushPin, "Ghim",
                            modifier = Modifier.size(18.dp),
                            tint = if (viewModel.isPinned) SamsungBlue
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Thêm", modifier = Modifier.size(18.dp))
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
                                text = { Text("Màu nền") },
                                leadingIcon = { Icon(Icons.Default.Palette, null) },
                                onClick = { showColorDialog = true; showOverflowMenu = false }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BottomBarBg,
                shadowElevation = 0.dp
            ) {
                Column {
                    HorizontalDivider(color = BottomBarBorder, thickness = 0.5.dp)

                    // Formatting toolbar (rich text mode only)
                    if (!viewModel.isChecklistMode) {
                        RichTextFormattingBar(state = viewModel.richTextState)
                    }

                    // Action row: checklist | image | spacer | undo | redo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle checklist / rich-text mode
                        IconButton(
                            onClick = viewModel::toggleChecklistMode,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (viewModel.isChecklistMode) Icons.Default.CheckBox
                                else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (viewModel.isChecklistMode) "Chế độ văn bản" else "Chế độ checklist",
                                tint = if (viewModel.isChecklistMode) SamsungBlue else IconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        // Insert image
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Chèn ảnh",
                                tint = IconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(viewModel.backgroundColor))
        ) {
            // Title field
            TextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = {
                    Text(
                        "Tiêu đề",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF999999)
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    color = Color(0xFF111111)
                ),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)

            // Image section
            NoteImageSection(
                imageBlocks = viewModel.imageBlocks,
                onInsertImage = { galleryLauncher.launch("image/*") },
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
                        color = Color(0xFF333333)
                    ),
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = SamsungBlue,
                    )
                )
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
                                color = if (isActive) SamsungBlue else MaterialTheme.colorScheme.onSurface
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
