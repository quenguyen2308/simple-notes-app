package com.yourname.simplenotes.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.BuildConfig
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.NoteMetadata
import com.yourname.simplenotes.sync.SyncScheduler
import com.yourname.simplenotes.ui.editor.NoteColorPicker
import com.yourname.simplenotes.ui.theme.isDynamicColorAvailable
import com.yourname.simplenotes.util.toEditorHtml
import java.util.UUID

private const val SUPPORT_EMAIL = "quenguyen2308@gmail.com"

@Composable
fun SettingsScreen(
    onThemeChange: (String) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onImportNotes: (List<Note>) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { SettingsPrefs(context) }
    val syncScheduler = remember { SyncScheduler(context) }
    // Settings sync to Drive as a single settings.json (last-write-wins by updatedAt) — push
    // right away instead of waiting for the next periodic/app-resume sync, mirroring notes/folders.
    fun syncSettingsNow() = syncScheduler.triggerImmediateSync()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val notes = uris.mapNotNull { readImportedTextNote(context, it) }
        if (notes.isNotEmpty()) {
            onImportNotes(notes)
            Toast.makeText(context, "Đã nhập ${notes.size} ghi chú", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Không đọc được nội dung file đã chọn", Toast.LENGTH_SHORT).show()
        }
    }

    var themeMode     by remember { mutableStateOf(prefs.themeMode) }
    var notifEnabled  by remember { mutableStateOf(prefs.notificationsEnabled) }
    var dynamicColorEnabled by remember { mutableStateOf(prefs.dynamicColorEnabled) }
    var autoSaveEnabled by remember { mutableStateOf(prefs.autoSaveEnabled) }
    var lockMethod    by remember { mutableStateOf(prefs.noteLockMethod) }
    var defaultBg     by remember { mutableStateOf(prefs.defaultNoteBackground) }
    var showLinksEnabled by remember { mutableStateOf(prefs.showLinksEnabled) }
    var hideScrollbarEnabled by remember { mutableStateOf(prefs.hideScrollbarEnabled) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLockMethodDialog by remember { mutableStateOf(false) }
    var showPageStyleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                "Cài đặt",
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Giao diện ─────────────────────────────────────────────────
        SectionHeader("Giao diện")
        SettingsGroup {
            PlainRow(
                title = "Chủ đề",
                subtitle = when (themeMode) {
                    "light" -> "Sáng"
                    "dark"  -> "Tối"
                    else    -> "Theo hệ thống"
                },
                onClick = { showThemeDialog = true }
            )
            if (isDynamicColorAvailable) {
                RowDivider()
                PlainRow(
                    title = "Màu động theo hình nền",
                    subtitle = "Material You — lấy màu từ hình nền thiết bị",
                    trailing = {
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = {
                                dynamicColorEnabled = it
                                prefs.dynamicColorEnabled = it
                                onDynamicColorChange(it)
                                syncSettingsNow()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Ghi chú ───────────────────────────────────────────────────
        SectionHeader("Ghi chú")
        SettingsGroup {
            PlainRow(
                title = "Tự động lưu ghi chú",
                subtitle = "Lưu ghi chú khi rời khỏi màn hình chỉnh sửa",
                trailing = {
                    Switch(
                        checked = autoSaveEnabled,
                        onCheckedChange = {
                            autoSaveEnabled = it
                            prefs.autoSaveEnabled = it
                            syncSettingsNow()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            )
            RowDivider()
            PlainRow(
                title = "Phương thức khóa ghi chú",
                subtitle = if (lockMethod == "pin") "Mã PIN" else "Sinh trắc học (vân tay/khuôn mặt)",
                onClick = { showLockMethodDialog = true }
            )
            RowDivider()
            PlainRow(
                title = "Kiểu trang và mẫu",
                subtitle = "Màu nền mặc định cho ghi chú mới",
                onClick = { showPageStyleDialog = true }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Thông báo ─────────────────────────────────────────────────
        SectionHeader("Thông báo")
        SettingsGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Thông báo nhắc nhở",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Nhận thông báo từ ứng dụng",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notifEnabled,
                    onCheckedChange = {
                        notifEnabled = it
                        prefs.notificationsEnabled = it
                        syncSettingsNow()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Nâng cao ──────────────────────────────────────────────────
        SectionHeader("Nâng cao")
        SettingsGroup {
            PlainRow(title = "Hiển thị liên kết trong ghi chú", trailing = {
                Switch(
                    checked = showLinksEnabled,
                    onCheckedChange = {
                        showLinksEnabled = it
                        prefs.showLinksEnabled = it
                        syncSettingsNow()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            })
            RowDivider()
            PlainRow(title = "Ẩn thanh cuộn khi chỉnh sửa", trailing = {
                Switch(
                    checked = hideScrollbarEnabled,
                    onCheckedChange = {
                        hideScrollbarEnabled = it
                        prefs.hideScrollbarEnabled = it
                        syncSettingsNow()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            })
        }

        Spacer(Modifier.height(8.dp))

        // ── Dữ liệu ───────────────────────────────────────────────────
        SectionHeader("Dữ liệu")
        SettingsGroup {
            PlainRow(
                title = "Nhập ghi chú từ file",
                subtitle = "Chọn file .txt — ví dụ xuất/chia sẻ từ Samsung Notes, Easy Note",
                onClick = { importLauncher.launch(arrayOf("text/plain")) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Về ứng dụng ───────────────────────────────────────────────
        SectionHeader("Về ứng dụng")
        SettingsGroup {
            PlainRow(title = "Phiên bản", subtitle = BuildConfig.VERSION_NAME)
            RowDivider()
            PlainRow(
                title = "Đánh giá ứng dụng",
                onClick = {
                    val packageName = context.packageName
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                        )
                    }
                }
            )
            RowDivider()
            PlainRow(
                title = "Liên hệ hỗ trợ",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ Simple Notes")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // No email client installed — silently ignore
                    }
                }
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    // ── Theme dialog ──────────────────────────────────────────────────
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Chọn chủ đề") },
            text = {
                Column {
                    listOf("system" to "Theo hệ thống", "light" to "Sáng", "dark" to "Tối").forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeMode = mode
                                    prefs.themeMode = mode
                                    onThemeChange(mode)
                                    syncSettingsNow()
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    themeMode = mode
                                    prefs.themeMode = mode
                                    onThemeChange(mode)
                                    syncSettingsNow()
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Đóng") }
            }
        )
    }

    // ── Note lock method dialog ─────────────────────────────────────────
    if (showLockMethodDialog) {
        AlertDialog(
            onDismissRequest = { showLockMethodDialog = false },
            title = { Text("Phương thức khóa ghi chú") },
            text = {
                Column {
                    listOf("biometric" to "Sinh trắc học (vân tay/khuôn mặt)", "pin" to "Mã PIN").forEach { (method, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    lockMethod = method
                                    prefs.noteLockMethod = method
                                    syncSettingsNow()
                                    showLockMethodDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lockMethod == method,
                                onClick = {
                                    lockMethod = method
                                    prefs.noteLockMethod = method
                                    syncSettingsNow()
                                    showLockMethodDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLockMethodDialog = false }) { Text("Đóng") }
            }
        )
    }

    // ── Page style / default background dialog ─────────────────────────
    if (showPageStyleDialog) {
        AlertDialog(
            onDismissRequest = { showPageStyleDialog = false },
            title = { Text("Kiểu trang và mẫu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Màu nền mặc định cho ghi chú mới",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    NoteColorPicker(
                        selectedColor = defaultBg,
                        onColorSelected = {
                            defaultBg = it
                            prefs.defaultNoteBackground = it
                            syncSettingsNow()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageStyleDialog = false }) { Text("Đóng") }
            }
        )
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

/** Reads a picked text file into a new [Note] (title = filename, falling back to its first line). */
private fun readImportedTextNote(context: android.content.Context, uri: Uri): Note? {
    val text = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()?.trim()
    if (text.isNullOrBlank()) return null

    val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    val title = displayName?.substringBeforeLast(".")?.take(80)?.takeIf { it.isNotBlank() }
        ?: text.lineSequence().firstOrNull { it.isNotBlank() }?.take(80)
        ?: "Ghi chú đã nhập"

    val now = System.currentTimeMillis()
    return Note(
        id = UUID.randomUUID().toString(),
        title = title,
        contentBlocks = listOf(ContentBlock.Text(text = text, htmlContent = text.toEditorHtml())),
        createdAt = now,
        updatedAt = now,
        metadata = NoteMetadata.from(text),
        isDirty = true
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        content = content
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun PlainRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Text(
                ">",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
