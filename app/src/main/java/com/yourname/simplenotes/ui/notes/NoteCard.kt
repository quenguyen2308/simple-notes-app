package com.yourname.simplenotes.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatCardDate(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000L        -> "Just now"
        diff < 3_600_000L     -> "${diff / 60_000}m ago"
        diff < 86_400_000L    -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    onShowActions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateText = remember(note.updatedAt) { formatCardDate(note.updatedAt) }

    val noteBackgroundColor = remember(note.backgroundColor) {
        val argb = note.backgroundColor
        if (argb == 0xFFFFFFFF.toInt() || argb == 0) null else Color(argb)
    }
    val checklistCount = remember(note.contentBlocks) {
        note.contentBlocks.filterIsInstance<ContentBlock.Checklist>().sumOf { it.items.size }
    }
    val hasImages = remember(note.contentBlocks) {
        note.contentBlocks.any { it is ContentBlock.Image }
    }

    Column(modifier = modifier) {
        // ── Card box: content only ───────────────────────────────────
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongPress),
            shape     = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
            colors    = CardDefaults.cardColors(
                containerColor = noteBackgroundColor ?: Color.White
            )
        ) {
            Box {
                Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                    if (note.isLocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Locked", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (checklistCount > 0) {
                        Text(
                            text = "$checklistCount item${if (checklistCount != 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (note.content.isNotBlank()) {
                        Text(
                            text       = note.content,
                            fontSize   = 11.sp,
                            maxLines   = 6,
                            overflow   = TextOverflow.Ellipsis,
                            color      = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                }

                // ── ⋮ button overlay (top-right) ────────────────────
                IconButton(
                    onClick  = onShowActions,
                    modifier = Modifier.size(28.dp).align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.MoreVert, "More",
                        modifier = Modifier.size(14.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // ── Selection indicator (top-left) ───────────────────
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle, "Selected",
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).align(Alignment.TopStart).padding(start = 6.dp, top = 6.dp)
                    )
                }
            }
        }

        // ── Title + date below the card box ─────────────────────────
        Spacer(Modifier.height(4.dp))
        if (note.labels.isNotEmpty()) {
            Row {
                note.labels.take(2).forEach { label ->
                    Text(
                        text     = label,
                        fontSize = 9.sp,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = note.title.ifBlank { "Untitled" },
                fontWeight = FontWeight.Bold,
                fontSize   = 12.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.weight(1f)
            )
            if (note.isPinned) {
                Icon(Icons.Default.PushPin, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp))
            }
            if (hasImages) {
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.Image, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp))
            }
        }
        Text(
            text     = dateText,
            fontSize = 10.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnimatedNoteCard(
    note: Note,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    onShowActions: () -> Unit = {},
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter   = fadeIn() + scaleIn(initialScale = 0.95f),
        exit    = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        NoteCard(
            note          = note,
            isSelected    = isSelected,
            onLongPress   = onLongPress,
            onClick       = onClick,
            onShowActions = onShowActions,
            modifier      = modifier
        )
    }
}
