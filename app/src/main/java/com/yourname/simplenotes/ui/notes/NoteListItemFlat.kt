package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.theme.isAppInDarkTheme
import com.yourname.simplenotes.util.HtmlSpannableConverter
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

    val isDark = isAppInDarkTheme()
    val noteColor = remember(note.backgroundColor) {
        val argb = note.backgroundColor
        if (argb == 0xFFFFFFFF.toInt() || argb == 0) null else Color(argb)
    }

    val cardBg = when {
        isSelected -> if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        note.isPinned -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        else -> if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.80f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        noteColor != null -> noteColor.copy(alpha = if (isDark) 0.5f else 0.4f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.45f)
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
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent left dot or indicator
            if (noteColor != null) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(noteColor)
                )
                Spacer(Modifier.width(10.dp))
            }

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
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                if (note.isLocked) {
                    Text(
                        text = "[Đã khóa]",
                        fontSize = 13.sp,
                        color = colorPreview,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                } else if (note.content.isNotBlank()) {
                    val preview = remember(note.contentBlocks) {
                        val html = note.contentBlocks.filterIsInstance<ContentBlock.Text>()
                            .joinToString("<br>") { it.htmlContent }
                        HtmlSpannableConverter.htmlToAnnotatedString(html)
                    }
                    Text(
                        text = preview,
                        fontSize = 13.sp,
                        color = colorPreview,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                }
                // Timestamp
                Text(text = timestamp, fontSize = 11.5.sp, color = colorTs)
            }
        }
    }
}
