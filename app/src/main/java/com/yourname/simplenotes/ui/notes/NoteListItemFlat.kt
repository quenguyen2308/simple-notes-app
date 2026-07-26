package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.domain.model.Note
import java.text.SimpleDateFormat
import java.util.*

private val ColorStar = Color(0xFFF5A623)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItemFlat(
    note: Note,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgPinned   = MaterialTheme.colorScheme.secondaryContainer
    val bgNormal   = MaterialTheme.colorScheme.surface
    val bgSelected = MaterialTheme.colorScheme.primaryContainer
    val colorTitle   = MaterialTheme.colorScheme.onSurface
    val colorPreview = MaterialTheme.colorScheme.onSurfaceVariant
    val colorTs      = MaterialTheme.colorScheme.outline
    val colorBorder  = MaterialTheme.colorScheme.outlineVariant

    val bg = when {
        isSelected    -> bgSelected
        note.isPinned -> bgPinned
        else          -> bgNormal
    }

    val timestamp = remember(note.contentUpdatedAt) {
        val diff = System.currentTimeMillis() - note.contentUpdatedAt
        when {
            diff < 60_000        -> "Vừa xong"
            diff < 3_600_000     -> "${diff / 60_000}p trước"
            diff < 86_400_000    -> "${diff / 3_600_000}h trước"
            else -> SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date(note.contentUpdatedAt))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkmark
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Text("★ ", color = ColorStar, fontSize = 11.sp)
                    }
                    Text(
                        text = note.title.ifEmpty { "Ghi chú" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                // Preview or locked label
                val preview = if (note.isLocked) "[Đã khóa]"
                              else note.content.replace("\n", " ").trim()
                if (preview.isNotEmpty()) {
                    Text(
                        text = preview,
                        fontSize = 13.sp,
                        color = colorPreview,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                }
                // Timestamp
                Text(text = timestamp, fontSize = 12.sp, color = colorTs)
            }
        }

        // Bottom divider line (0.5dp per spec)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colorBorder)
        )
    }
}
