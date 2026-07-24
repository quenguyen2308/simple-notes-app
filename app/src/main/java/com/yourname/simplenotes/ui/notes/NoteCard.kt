package com.yourname.simplenotes.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    // Pastel note colors are fixed light hues regardless of app theme, so text on a
    // custom-colored card always uses dark text — MaterialTheme.colorScheme.onSurface
    // would turn near-white in dark mode and become unreadable on a light pastel card.
    val onCard        = if (noteBackgroundColor != null) Color(0xFF1B1B1B) else MaterialTheme.colorScheme.onSurface
    val onCardVariant = if (noteBackgroundColor != null) Color(0xFF1B1B1B).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val onCardAccent  = if (noteBackgroundColor != null) onCard else MaterialTheme.colorScheme.primary
    val checklistItems = remember(note.contentBlocks) {
        note.contentBlocks.filterIsInstance<ContentBlock.Checklist>()
            .flatMap { it.items }
            .sortedBy { it.order }
    }
    val thumbnailUri = remember(note.contentBlocks) {
        note.contentBlocks.filterIsInstance<ContentBlock.Image>().firstOrNull()?.uri
    }

    Column(modifier = modifier) {
        // ── Card box: content only ───────────────────────────────────
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .then(
                    if (isSelected) Modifier.border(2.dp, onCardAccent, RoundedCornerShape(12.dp))
                    else Modifier
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongPress),
            shape     = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
            colors    = CardDefaults.cardColors(
                containerColor = noteBackgroundColor ?: MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box {
                Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                    // ── Thumbnail (first attached image, if any) ──────
                    if (thumbnailUri != null) {
                        AsyncImage(
                            model             = thumbnailUri,
                            contentDescription = null,
                            contentScale      = ContentScale.Crop,
                            modifier          = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── Title row ─────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = note.title.ifBlank { "Untitled" },
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis,
                            color      = onCard,
                            modifier   = Modifier.weight(1f).padding(end = 24.dp)
                        )
                        if (note.isPinned) {
                            Icon(Icons.Default.PushPin, null,
                                tint = onCardAccent,
                                modifier = Modifier.size(11.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // ── Excerpt / checklist count / locked state ──────
                    if (note.isLocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null,
                                tint = onCardAccent,
                                modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Locked", fontSize = 11.sp, color = onCardVariant)
                        }
                    } else if (checklistItems.isNotEmpty()) {
                        Column {
                            checklistItems.take(3).forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (item.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint     = onCardVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text          = item.text,
                                        fontSize      = 11.sp,
                                        color         = if (item.isCompleted) onCardVariant else onCard,
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                        maxLines      = 1,
                                        overflow      = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (checklistItems.size > 3) {
                                Text(
                                    text     = "+${checklistItems.size - 3} nữa",
                                    fontSize = 10.sp,
                                    color    = onCardVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    } else if (note.content.isNotBlank()) {
                        Text(
                            text       = note.content,
                            fontSize   = 11.sp,
                            maxLines   = 6,
                            overflow   = TextOverflow.Ellipsis,
                            color      = onCard,
                            lineHeight = 15.sp
                        )
                    }

                    // ── Tag pills ──────────────────────────────────────
                    if (note.labels.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        val pillBg = if (noteBackgroundColor != null) noteBackgroundColor.darken(0.28f) else MaterialTheme.colorScheme.primary
                        Row {
                            note.labels.take(2).forEach { label ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .background(pillBg, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = label, fontSize = 9.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text     = dateText,
                        fontSize = 10.sp,
                        color    = onCardVariant
                    )
                }

                // ── ⋮ button overlay (top-right) ────────────────────
                IconButton(
                    onClick  = onShowActions,
                    modifier = Modifier.size(28.dp).align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.MoreVert, "More",
                        modifier = Modifier.size(14.dp),
                        tint     = onCardVariant)
                }

                // ── Selection indicator (top-left) ───────────────────
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle, "Selected",
                        tint     = onCardAccent,
                        modifier = Modifier.size(18.dp).align(Alignment.TopStart).padding(start = 6.dp, top = 6.dp)
                    )
                }
            }
        }
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
