package com.yourname.simplenotes.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.domain.model.NoteMetadata
import com.yourname.simplenotes.sync.SyncScheduler
import com.yourname.simplenotes.ui.editor.NoteColorPicker
import com.yourname.simplenotes.ui.theme.FrostedGlassBgDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBgLight
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderLight
import com.yourname.simplenotes.ui.theme.FrostedGlassTileDark
import com.yourname.simplenotes.ui.theme.FrostedGlassTileLight
import com.yourname.simplenotes.ui.theme.SakuraBorderSoft
import com.yourname.simplenotes.ui.theme.SakuraPink
import com.yourname.simplenotes.ui.theme.SakuraSurface
import com.yourname.simplenotes.ui.theme.SakuraSurfaceDark
import com.yourname.simplenotes.ui.theme.isAppInDarkTheme
import com.yourname.simplenotes.util.toEditorHtml
import java.util.UUID

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onImportNotes: (List<Note>) -> Unit = {},
    onImportArchive: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { SettingsPrefs(context) }
    val syncScheduler = remember { SyncScheduler(context) }
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

    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var defaultBg by remember { mutableStateOf(prefs.defaultNoteBackground) }

    val archiveImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportArchive(uri)
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPageStyleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Header ──────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Cài đặt",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Scrollable Settings List ────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Giao diện ───────────────────────────────────────────
            SettingsSection(title = "GIAO DIỆN") {
                BentoRow(
                    icon = Icons.Default.Palette,
                    title = "Chủ đề giao diện",
                    subtitle = when (themeMode) {
                        "light" -> "Sáng"
                        "dark"  -> "Tối"
                        else    -> "Theo hệ thống"
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            // ── Ghi chú ─────────────────────────────────────────────
            SettingsSection(title = "GHI CHÚ") {
                BentoRow(
                    icon = Icons.Default.FormatColorFill,
                    title = "Màu nền mặc định cho ghi chú",
                    subtitle = "Màu nền khởi tạo cho các ghi chú mới",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(defaultBg))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    onClick = { showPageStyleDialog = true }
                )
            }

            // ── Dữ liệu & Khôi phục ─────────────────────────────────
            SettingsSection(title = "DỮ LIỆU & KHÔI PHỤC") {
                BentoRow(
                    icon = Icons.Default.Description,
                    title = "Nhập ghi chú từ file (.txt)",
                    subtitle = "Chọn các file văn bản thuần (.txt) từ máy hoặc ứng dụng khác",
                    onClick = { importLauncher.launch(arrayOf("text/plain")) }
                )
                RowDivider()
                BentoRow(
                    icon = Icons.Default.Restore,
                    title = "Khôi phục từ file .backup / .zip",
                    subtitle = "Khôi phục từ bản sao lưu EasyNotes hoặc file .zip ghi chú",
                    onClick = { archiveImportLauncher.launch(arrayOf("*/*")) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Theme dialog ──────────────────────────────────────────────────
    if (showThemeDialog) {
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
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
                    text = "Chủ đề giao diện",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SakuraPink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("system", "Theo hệ thống", Icons.Default.BrightnessAuto),
                        Triple("light", "Sáng", Icons.Default.LightMode),
                        Triple("dark", "Tối", Icons.Default.DarkMode)
                    ).forEach { (mode, label, icon) ->
                        val isSelected = themeMode == mode
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SakuraPink.copy(alpha = 0.14f) else if (isDark) FrostedGlassTileDark else FrostedGlassTileLight,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) SakuraPink else if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeMode = mode
                                    prefs.themeMode = mode
                                    onThemeChange(mode)
                                    syncSettingsNow()
                                    showThemeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) SakuraPink else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SakuraPink else if (isDark) Color.White else Color(0xFF1F2937),
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        themeMode = mode
                                        prefs.themeMode = mode
                                        onThemeChange(mode)
                                        syncSettingsNow()
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = SakuraPink)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemeDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SakuraPink, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đóng", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }

    // ── Page style / default background dialog ─────────────────────────
    if (showPageStyleDialog) {
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { showPageStyleDialog = false },
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
                    Icon(Icons.Default.FormatColorFill, null, tint = SakuraPink, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "Màu nền mặc định",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SakuraPink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Chọn màu nền khởi tạo cho tất cả ghi chú mới tạo:",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    // Live preview card
                    Surface(
                        color = Color(defaultBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft.copy(alpha = 0.6f)),
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
                                "Ghi chú mẫu xem trước",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (Color(defaultBg) == Color.White) Color.Black else Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }
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
                Button(
                    onClick = { showPageStyleDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SakuraPink, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xong", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
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
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SakuraPink,
            modifier = Modifier.padding(start = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp, end = 16.dp)
    )
}

@Composable
private fun BentoRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Soft icon badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SakuraPink.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SakuraPink,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
