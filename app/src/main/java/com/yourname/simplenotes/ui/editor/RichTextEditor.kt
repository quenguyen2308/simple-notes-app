package com.yourname.simplenotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor as ComposeRichTextEditor

// Design tokens per spec 2.2
private val ToolbarBg     = Color(0xFFF9F9F9)
private val ToolbarBorder = Color(0xFFEEEEEE)
private val BtnBg         = Color(0xFFFFFFFF)
private val BtnBorder     = Color(0xFFDDDDDD)
private val BtnText       = Color(0xFF555555)
private val BtnActiveBg   = Color(0xFFE8F0FE)
private val BtnActiveBrd  = Color(0xFF1259C3)
private val BtnActiveText = Color(0xFF1259C3)

/**
 * Horizontally scrollable formatting toolbar per Samsung Notes spec 2.2.
 * Exposed as public so NoteEditorScreen can place it above the title field.
 */
@Composable
fun RichTextFormattingBar(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker   by remember { mutableStateOf(false) }
    var showFontSizeMenu    by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = ToolbarBorder, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ToolbarBg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                isActive = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            )

            // Italic
            FormatBtn(
                icon = Icons.Default.FormatItalic,
                contentDescription = "In nghiêng",
                isActive = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            )

            // Underline
            FormatBtn(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Gạch dưới",
                isActive = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
            )

            // Strikethrough
            FormatBtn(
                icon = Icons.Default.StrikethroughS,
                contentDescription = "Gạch ngang",
                isActive = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
            )

            ToolbarDivider()

            // Text colour — icon tinted with current text color
            FormatBtn(
                icon = Icons.Default.FormatColorText,
                contentDescription = "Màu chữ",
                isActive = state.currentSpanStyle.color != Color.Unspecified,
                iconTint = state.currentSpanStyle.color.takeIf { it != Color.Unspecified } ?: BtnText,
                onClick = { showTextColorPicker = true }
            )
            // Highlight colour — icon tinted with current highlight color
            FormatBtn(
                icon = Icons.Default.FormatColorFill,
                contentDescription = "Màu nền chữ",
                isActive = state.currentSpanStyle.background != Color.Unspecified,
                iconTint = state.currentSpanStyle.background.takeIf { it != Color.Unspecified } ?: BtnText,
                onClick = { showBgColorPicker = true }
            )
        }
        HorizontalDivider(color = ToolbarBorder, thickness = 0.5.dp)
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
    iconTint: Color = if (isActive) BtnActiveText else BtnText,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                if (isActive) BtnActiveBg else BtnBg,
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (isActive) BtnActiveBrd else BtnBorder,
                RoundedCornerShape(6.dp)
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
            .background(Color(0xFFDDDDDD))
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
