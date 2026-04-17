package com.yourname.simplenotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor as ComposeRichTextEditor

// ─── Public API ──────────────────────────────────────────────────────────────

/**
 * Full rich text editing surface: toolbar + editor area.
 *
 * The caller owns the [RichTextState]; this composable only renders and
 * routes toolbar actions to it. Saves/auto-saves happen at the screen level.
 */
@Composable
fun RichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RichTextToolbar(state = state)
        HorizontalDivider()
        ComposeRichTextEditor(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

// ─── Toolbar ─────────────────────────────────────────────────────────────────

@Composable
private fun RichTextToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker by remember { mutableStateOf(false) }
    var showFontSizeMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bold
            ToolbarToggleButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Bold",
                isActive = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                }
            )
            // Italic
            ToolbarToggleButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                isActive = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                }
            )
            // Underline
            ToolbarToggleButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                isActive = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                }
            )
            // Strikethrough
            ToolbarToggleButton(
                icon = Icons.Default.FormatStrikethrough,
                contentDescription = "Strikethrough",
                isActive = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                }
            )

            ToolbarDivider()

            // Text colour swatch
            ColorSwatch(
                color = state.currentSpanStyle.color.takeIf { it != Color.Unspecified }
                    ?: MaterialTheme.colorScheme.onSurface,
                contentDescription = "Text color",
                hasBorder = false,
                onClick = { showTextColorPicker = true }
            )
            // Background colour swatch
            ColorSwatch(
                color = state.currentSpanStyle.background.takeIf { it != Color.Unspecified }
                    ?: Color.Transparent,
                contentDescription = "Highlight color",
                hasBorder = true,
                onClick = { showBgColorPicker = true }
            )

            ToolbarDivider()

            // Font size
            Box {
                TextButton(
                    onClick = { showFontSizeMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    val currentSize = state.currentSpanStyle.fontSize
                        .takeIf { it != 0.sp } ?: 16.sp
                    Text("${currentSize.value.toInt()}sp", style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(
                    expanded = showFontSizeMenu,
                    onDismissRequest = { showFontSizeMenu = false }
                ) {
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
        }
    }

    // Colour picker dialogs
    if (showTextColorPicker) {
        ColorPickerDialog(
            title = "Text color",
            onColorSelected = { color ->
                state.toggleSpanStyle(SpanStyle(color = color))
                showTextColorPicker = false
            },
            onDismiss = { showTextColorPicker = false }
        )
    }
    if (showBgColorPicker) {
        ColorPickerDialog(
            title = "Highlight color",
            onColorSelected = { color ->
                state.toggleSpanStyle(SpanStyle(background = color))
                showBgColorPicker = false
            },
            onDismiss = { showBgColorPicker = false }
        )
    }
}

// ─── Toolbar helpers ──────────────────────────────────────────────────────────

@Composable
private fun ToolbarToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolbarDivider() {
    HorizontalDivider(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
    )
}

@Composable
private fun ColorSwatch(
    color: Color,
    contentDescription: String,
    hasBorder: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (hasBorder) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    else Modifier
                )
        )
    }
}

// ─── Colour picker dialog ─────────────────────────────────────────────────────

/** Simple 12-swatch colour picker matching the note background preset palette. */
@Composable
private fun ColorPickerDialog(
    title: String,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val swatches = listOf(
        Color.Black, Color.DarkGray, Color.Red, Color(0xFFE91E63),
        Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF00BCD4),
        Color(0xFF4CAF50), Color(0xFFCDDC39), Color(0xFFFF9800),
        Color(0xFFFF5722), Color.White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                swatches.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .then(
                                        Modifier
                                    ),
                            ) {
                                IconButton(
                                    onClick = { onColorSelected(color) },
                                    modifier = Modifier.fillMaxSize()
                                ) {}
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
