package com.yourname.simplenotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.ui.theme.SakuraPink

// 12 preset note background colors (ARGB) — vivid pastels, not washed-out tints
val NOTE_COLORS = listOf(
    0xFFFFFFFF.toInt(), // White (default)
    0xFFFFE066.toInt(), // Yellow
    0xFFA8E6B0.toInt(), // Green
    0xFFA8D8F0.toInt(), // Blue
    0xFFFFB3C6.toInt(), // Pink
    0xFFFFC178.toInt(), // Orange
    0xFFD4C5F9.toInt(), // Purple
    0xFF8DE0D0.toInt(), // Teal
    0xFFFF9494.toInt(), // Red / coral
    0xFFE0E0E0.toInt(), // Grey
    0xFFB3E5FC.toInt(), // Light Blue
    0xFFDCE775.toInt(), // Lime
)

/**
 * Clean 2-row grid of 36dp colored circles for picking a note background color.
 * Selected circle shows a SakuraPink border + check icon.
 */
@Composable
fun NoteColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = remember { NOTE_COLORS.chunked(6) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowColors.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) SakuraPink
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (color == 0xFFFFFFFF.toInt()) SakuraPink else Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
