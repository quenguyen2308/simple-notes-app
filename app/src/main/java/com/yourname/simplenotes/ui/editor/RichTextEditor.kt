package com.yourname.simplenotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor as ComposeRichTextEditor

/**
 * Row of rich-text format buttons (font size, bold/italic/underline/strike, colors) with no
 * background/divider chrome of its own — meant to be embedded inside a parent toolbar surface
 * (e.g. NoteEditorScreen's floating bottom bar) rather than rendered as a standalone bar.
 */
@Composable
fun RichTextFormattingRow(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker   by remember { mutableStateOf(false) }
    var showFontSizeMenu    by remember { mutableStateOf(false) }

    // Read once per recomposition instead of once per button below — this row recomposes on
    // every cursor move/keystroke since it's always visible in the bottom toolbar.
    val currentStyle = state.currentSpanStyle

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Font size dropdown
        Box {
            FormatBtn(
                icon = Icons.Default.FormatSize,
                contentDescription = "Cỡ chữ",
                isActive = showFontSizeMenu,
                onClick = { showFontSizeMenu = true }
            )
            DropdownMenu(expanded = showFontSizeMenu, onDismissRequest = { showFontSizeMenu = false }) {
                listOf(12, 14, 16, 18, 20, 24).forEach { size ->
                    DropdownMenuItem(
                        text = { Text("$size") },
                        onClick = {
                            state.toggleSpanStyle(SpanStyle(fontSize = size.sp))
                            showFontSizeMenu = false
                        }
                    )
                }
            }
        }

        // Bold
        FormatBtn(
            icon = Icons.Default.FormatBold,
            contentDescription = "In đậm",
            isActive = currentStyle.fontWeight == FontWeight.Bold,
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
        )

        // Italic
        FormatBtn(
            icon = Icons.Default.FormatItalic,
            contentDescription = "In nghiêng",
            isActive = currentStyle.fontStyle == FontStyle.Italic,
            onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
        )

        // Underline
        FormatBtn(
            icon = Icons.Default.FormatUnderlined,
            contentDescription = "Gạch dưới",
            isActive = currentStyle.textDecoration?.contains(TextDecoration.Underline) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
        )

        // Strikethrough
        FormatBtn(
            icon = Icons.Default.StrikethroughS,
            contentDescription = "Gạch ngang",
            isActive = currentStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
        )

        ToolbarDivider()

        // Text colour — icon tinted with current text color
        FormatBtn(
            icon = Icons.Default.FormatColorText,
            contentDescription = "Màu chữ",
            isActive = currentStyle.color != Color.Unspecified,
            iconTint = currentStyle.color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { showTextColorPicker = true }
        )
        // Highlight colour — icon tinted with current highlight color
        FormatBtn(
            icon = Icons.Default.FormatColorFill,
            contentDescription = "Màu nền chữ",
            isActive = currentStyle.background != Color.Unspecified,
            iconTint = currentStyle.background.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { showBgColorPicker = true }
        )
    }

    if (showTextColorPicker) {
        ColorPickerDialog(
            title = "Màu chữ",
            onColorSelected = { state.toggleSpanStyle(SpanStyle(color = it)); showTextColorPicker = false },
            onDismiss = { showTextColorPicker = false }
        )
    }
    if (showBgColorPicker) {
        ColorPickerDialog(
            title = "Màu nền chữ",
            onColorSelected = { state.toggleSpanStyle(SpanStyle(background = it)); showBgColorPicker = false },
            onDismiss = { showBgColorPicker = false }
        )
    }
}

// ── Internal helpers ──────────────────────────────────────────────────────────

/** Icon-based format button with accessible touch target (≥44dp). */
@Composable
private fun FormatBtn(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    iconTint: Color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                CircleShape
            )
            .border(
                1.dp,
                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = iconTint
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(0.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    )
}

@Composable
private fun ColorPickerDialog(title: String, onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    val swatches = listOf(
        Color.Black, Color.DarkGray, Color.Red, Color(0xFFE91E63),
        Color(0xFF9C27B0), Color(0xFF1259C3), Color(0xFF00BCD4),
        Color(0xFF4CAF50), Color(0xFFCDDC39), Color(0xFFFF9800),
        Color(0xFFFF5722), Color.White
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                swatches.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                    .clickable { onColorSelected(color) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
