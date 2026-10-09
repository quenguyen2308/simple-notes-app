package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.theme.FrostedGlassBgDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBgLight
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderLight
import com.yourname.simplenotes.ui.theme.SakuraBorderSoft
import com.yourname.simplenotes.ui.theme.SakuraPink
import com.yourname.simplenotes.ui.theme.SakuraSurface
import com.yourname.simplenotes.ui.theme.SakuraSurfaceDark
import com.yourname.simplenotes.ui.theme.SakuraTextPrimary
import com.yourname.simplenotes.ui.theme.isAppInDarkTheme

@Composable
fun RecycleBinScreen(
    notes: List<Note>,
    onRestore: (String) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var confirmDeleteNote by remember { mutableStateOf<Note?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
            }
            Text(
                "Thùng rác",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            if (notes.isNotEmpty()) {
                TextButton(onClick = { showClearAllConfirm = true }) {
                    Text("Dọn sạch", color = MaterialTheme.colorScheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }

        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌸", fontSize = 42.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Thùng rác đang trống",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    RecycleBinNoteItem(
                        note = note,
                        onRestore = { onRestore(note.id) },
                        onDelete = { confirmDeleteNote = note }
                    )
                }
            }
        }
    }

    confirmDeleteNote?.let { note ->
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { confirmDeleteNote = null },
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
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "Xóa vĩnh viễn",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else SakuraTextPrimary
                )
            },
            text = {
                Text(
                    text = "Ghi chú \"${note.title.ifBlank { "Không có tiêu đề" }}\" sẽ bị xóa vĩnh viễn khỏi thiết bị và không thể khôi phục lại.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentDelete(note.id)
                        confirmDeleteNote = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text("Xóa vĩnh viễn", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmDeleteNote = null },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showClearAllConfirm) {
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
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
                    text = "Dọn sạch thùng rác",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else SakuraTextPrimary
                )
            },
            text = {
                Text(
                    text = "Tất cả ${notes.size} ghi chú trong thùng rác sẽ bị xóa vĩnh viễn và không thể khôi phục lại.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearAllConfirm = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text("Dọn sạch", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAllConfirm = false },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun RecycleBinNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val daysLeft = remember(note.updatedAt) {
        val elapsed = System.currentTimeMillis() - note.updatedAt
        maxOf(0L, 30L - elapsed / (24L * 60 * 60 * 1000))
    }
    val expiryText = if (daysLeft == 0L) "Xóa hôm nay" else "Tự động xóa sau $daysLeft ngày"
    val expiryColor = if (daysLeft <= 3) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.onSurfaceVariant
    val noteColor = remember(note.backgroundColor) {
        val argb = note.backgroundColor
        if (argb == 0xFFFFFFFF.toInt() || argb == 0) null else androidx.compose.ui.graphics.Color(argb)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            noteColor?.copy(alpha = 0.4f) ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (noteColor != null) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(noteColor)
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Không có tiêu đề" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = expiryText,
                    fontSize = 12.sp,
                    color = expiryColor
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Default.RestoreFromTrash,
                    contentDescription = "Khôi phục",
                    tint = com.yourname.simplenotes.ui.theme.SakuraPink
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = "Xóa vĩnh viễn",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
