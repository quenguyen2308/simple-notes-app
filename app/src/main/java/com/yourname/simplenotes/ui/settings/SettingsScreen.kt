package com.yourname.simplenotes.ui.settings

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
import com.yourname.simplenotes.ui.theme.isDynamicColorAvailable

@Composable
fun SettingsScreen(
    onThemeChange: (String) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { SettingsPrefs(context) }

    var themeMode     by remember { mutableStateOf(prefs.themeMode) }
    var notifEnabled  by remember { mutableStateOf(prefs.notificationsEnabled) }
    var dynamicColorEnabled by remember { mutableStateOf(prefs.dynamicColorEnabled) }
    var showThemeDialog by remember { mutableStateOf(false) }

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
                trailing = {
                    Text("Bật", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            )
            RowDivider()
            PlainRow(
                title = "Phương thức khóa ghi chú",
                onClick = {}
            )
            RowDivider()
            PlainRow(
                title = "Kiểu trang và mẫu",
                onClick = {}
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
                    checked = true,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            })
            RowDivider()
            PlainRow(title = "Ẩn thanh cuộn khi chỉnh sửa", trailing = {
                Switch(
                    checked = false,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            })
        }

        Spacer(Modifier.height(8.dp))

        // ── Về ứng dụng ───────────────────────────────────────────────
        SectionHeader("Về ứng dụng")
        SettingsGroup {
            PlainRow(title = "Phiên bản", subtitle = "1.0.0")
            RowDivider()
            PlainRow(title = "Đánh giá ứng dụng", onClick = {})
            RowDivider()
            PlainRow(title = "Liên hệ hỗ trợ", onClick = {})
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
}

// ── Private helpers ───────────────────────────────────────────────────────────

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
