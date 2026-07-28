package com.yourname.simplenotes.ui.editor

import android.text.Spannable
import android.widget.EditText
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.util.HtmlSpannableConverter

/**
 * Row of rich-text format buttons (bold/italic/underline/strike/font size/text color/background color).
 *
 * Wraps [RichTextFormattingRowCore] inside a stateful composable that reads the current
 * formatting state from [editText] on every call and invokes the appropriate EditText span methods.
 * This keeps the toolbar reactive (active buttons update as cursor moves) without needing
 * a separate state source of truth.
 *
 * @param editText        reference to the EditText inside AndroidRichTextEditor
 * @param onHtmlChange    called with the updated HTML after every format toggle, since setting
 *                        a span (unlike typing) does NOT fire EditText's TextWatcher — without
 *                        this, formatting applied via the toolbar would appear live in the
 *                        editor but never reach the ViewModel, so it silently vanished on save.
 * @param modifier        standard Compose modifier
 */
@Composable
fun RichTextFormattingRow(
    editText: EditText?,
    onHtmlChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun syncHtml() {
        val text = editText?.text as? Spannable ?: return
        onHtmlChange(HtmlSpannableConverter.spannableToHtml(text))
    }

    RichTextFormattingRowCore(
        getState = { editText?.let { getFormatState(it) } ?: FormatState() },
        onBold = { editText?.let { toggleBold(it); syncHtml() } },
        onItalic = { editText?.let { toggleItalic(it); syncHtml() } },
        onUnderline = { editText?.let { toggleUnderline(it); syncHtml() } },
        onStrikethrough = { editText?.let { toggleStrikethrough(it); syncHtml() } },
        onTextColor = { color -> editText?.let { setTextColor(it, color); syncHtml() } },
        onBgColor = { color -> editText?.let { setBackgroundColor(it, color); syncHtml() } },
        onFontSize = { size -> editText?.let { setFontSize(it, size); syncHtml() } },
        modifier = modifier
    )
}

/**
 * Core toolbar UI — pure Compose, no EditText dependency.
 * Exists separately so it can be tested in isolation.
 *
 * @param getState    called every recomposition to get current cursor format state
 * @param onBold      called when Bold button is clicked
 * @param onItalic    called when Italic button is clicked
 * @param onUnderline called when Underline button is clicked
 * @param onStrikethrough called when Strikethrough button is clicked
 * @param onTextColor called with selected color to apply text color
 * @param onBgColor   called with selected color to apply highlight color
 * @param onFontSize  called with selected pixel size to apply font size
 */
@Composable
fun RichTextFormattingRowCore(
    getState: () -> FormatState,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onTextColor: (Color?) -> Unit,
    onBgColor: (Color?) -> Unit,
    onFontSize: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Re-read format state on every recomposition so active buttons are always correct
    val formatState = getState()
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker   by remember { mutableStateOf(false) }
    var showFontSizeMenu    by remember { mutableStateOf(false) }

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
                            onFontSize(size)
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
            isActive = formatState.isBold,
            onClick = onBold
        )

        // Italic
        FormatBtn(
            icon = Icons.Default.FormatItalic,
            contentDescription = "In nghiêng",
            isActive = formatState.isItalic,
            onClick = onItalic
        )

        // Underline
        FormatBtn(
            icon = Icons.Default.FormatUnderlined,
            contentDescription = "Gạch dưới",
            isActive = formatState.isUnderline,
            onClick = onUnderline
        )

        // Strikethrough
        FormatBtn(
            icon = Icons.Default.StrikethroughS,
            contentDescription = "Gạch ngang",
            isActive = formatState.isStrikethrough,
            onClick = onStrikethrough
        )

        ToolbarDivider()

        // Text colour
        FormatBtn(
            icon = Icons.Default.FormatColorText,
            contentDescription = "Màu chữ",
            isActive = formatState.textColor != null,
            iconTint = formatState.textColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { showTextColorPicker = true }
        )

        // Highlight colour
        FormatBtn(
            icon = Icons.Default.FormatColorFill,
            contentDescription = "Màu nền chữ",
            isActive = formatState.backgroundColor != null,
            iconTint = formatState.backgroundColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { showBgColorPicker = true }
        )
    }

    if (showTextColorPicker) {
        ColorPickerDialog(
            title = "Màu chữ",
            onColorSelected = { color ->
                onTextColor(color)
                showTextColorPicker = false
            },
            onDismiss = { showTextColorPicker = false }
        )
    }
    if (showBgColorPicker) {
        ColorPickerDialog(
            title = "Màu nền chữ",
            onColorSelected = { color ->
                onBgColor(color)
                showBgColorPicker = false
            },
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
