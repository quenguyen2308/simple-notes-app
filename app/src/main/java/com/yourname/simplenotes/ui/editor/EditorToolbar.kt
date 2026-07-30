package com.yourname.simplenotes.ui.editor

import android.text.Spannable
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.util.HtmlSpannableConverter

/** Formats toggled directly on the current selection (span-level, on/off). */
enum class FormatType { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, HIGHLIGHT }

/** Toolbar actions that aren't a simple span toggle. */
enum class ToolbarActionId { UNDO, REDO, HEADING, BULLET_LIST, CHECKLIST, LINK, IMAGE, MORE }

private sealed class ToolbarItem {
    data class Format(val type: FormatType, val icon: ImageVector, val label: String) : ToolbarItem()
    data class Action(val id: ToolbarActionId, val icon: ImageVector, val label: String) : ToolbarItem()
    object Divider : ToolbarItem()
}

private val toolbarItems = listOf(
    ToolbarItem.Action(ToolbarActionId.UNDO, Icons.AutoMirrored.Filled.Undo, "Hoàn tác"),
    ToolbarItem.Action(ToolbarActionId.REDO, Icons.AutoMirrored.Filled.Redo, "Làm lại"),
    ToolbarItem.Divider,
    ToolbarItem.Action(ToolbarActionId.HEADING, Icons.Default.Title, "Tiêu đề"),
    ToolbarItem.Format(FormatType.BOLD, Icons.Default.FormatBold, "In đậm"),
    ToolbarItem.Format(FormatType.ITALIC, Icons.Default.FormatItalic, "In nghiêng"),
    ToolbarItem.Format(FormatType.UNDERLINE, Icons.Default.FormatUnderlined, "Gạch dưới"),
    ToolbarItem.Format(FormatType.STRIKETHROUGH, Icons.Default.StrikethroughS, "Gạch ngang"),
    ToolbarItem.Divider,
    ToolbarItem.Action(ToolbarActionId.BULLET_LIST, Icons.AutoMirrored.Filled.FormatListBulleted, "Danh sách"),
    ToolbarItem.Action(ToolbarActionId.CHECKLIST, Icons.Default.CheckBox, "Việc cần làm"),
    ToolbarItem.Format(FormatType.HIGHLIGHT, Icons.Default.BorderColor, "Đánh dấu"),
    ToolbarItem.Divider,
    ToolbarItem.Action(ToolbarActionId.LINK, Icons.Default.Link, "Liên kết"),
    ToolbarItem.Action(ToolbarActionId.IMAGE, Icons.Default.Image, "Ảnh"),
    ToolbarItem.Action(ToolbarActionId.MORE, Icons.Default.MoreHoriz, "Thêm"),
)

/**
 * Scrollable formatting toolbar for the note editor's [AndroidRichTextEditor].
 *
 * Re-reads formatting state from [editText] on every recomposition (there's no Compose state
 * to observe on a raw EditText) — this is driven by the screen recomposing on every keystroke
 * via the ViewModel's `htmlContent`, same pattern as the toolbar this replaces.
 *
 * @param onHtmlChange called with the updated HTML after every span-only mutation (format
 *                      toggles, heading, highlight, link, font size, colors). Setting a span
 *                      does NOT fire EditText's TextWatcher the way typing does, so without this
 *                      the ViewModel's htmlContent would go stale and the formatting would
 *                      silently vanish on save.
 */
@Composable
fun EditorToolbar(
    editText: EditText?,
    isChecklistActive: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onChecklistToggle: () -> Unit,
    onInsertImage: () -> Unit,
    onOpenNoteColorPicker: () -> Unit,
    onHtmlChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun syncHtml() {
        val text = editText?.text as? Spannable ?: return
        onHtmlChange(HtmlSpannableConverter.spannableToHtml(text))
    }

    val formatState = editText?.let { getFormatState(it) } ?: FormatState()
    val headingActive = editText?.let { isHeadingLine(it) } ?: false
    val bulletActive = editText?.let { isBulletLine(it) } ?: false
    val linkActive = editText?.let { hasLinkInSelection(it) } ?: false
    val hasSelection = editText?.let { it.selectionStart != it.selectionEnd } ?: false

    var showLinkDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        toolbarItems.forEach { item ->
            when (item) {
                is ToolbarItem.Divider -> ToolbarDivider()

                is ToolbarItem.Format -> {
                    val isActive = when (item.type) {
                        FormatType.BOLD -> formatState.isBold
                        FormatType.ITALIC -> formatState.isItalic
                        FormatType.UNDERLINE -> formatState.isUnderline
                        FormatType.STRIKETHROUGH -> formatState.isStrikethrough
                        FormatType.HIGHLIGHT -> formatState.isHighlighted
                    }
                    ToolbarIconButton(
                        icon = item.icon,
                        contentDescription = item.label,
                        isActive = isActive,
                        enabled = !isChecklistActive,
                        onClick = {
                            editText?.let {
                                when (item.type) {
                                    FormatType.BOLD -> toggleBold(it)
                                    FormatType.ITALIC -> toggleItalic(it)
                                    FormatType.UNDERLINE -> toggleUnderline(it)
                                    FormatType.STRIKETHROUGH -> toggleStrikethrough(it)
                                    FormatType.HIGHLIGHT -> toggleHighlight(it)
                                }
                                syncHtml()
                            }
                        }
                    )
                }

                is ToolbarItem.Action -> if (item.id == ToolbarActionId.MORE) {
                    Box {
                        ToolbarIconButton(
                            icon = item.icon,
                            contentDescription = item.label,
                            isActive = showMoreMenu,
                            enabled = true,
                            onClick = { showMoreMenu = true }
                        )
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            Text(
                                "Cỡ chữ",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            listOf(12, 14, 16, 18, 20, 24).forEach { size ->
                                DropdownMenuItem(
                                    text = { Text("$size sp") },
                                    onClick = { editText?.let { setFontSize(it, size) }; syncHtml(); showMoreMenu = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Màu chữ") },
                                leadingIcon = { Icon(Icons.Default.FormatColorText, null) },
                                onClick = { showMoreMenu = false; showTextColorPicker = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Màu nền ghi chú") },
                                leadingIcon = { Icon(Icons.Default.Palette, null) },
                                onClick = { showMoreMenu = false; onOpenNoteColorPicker() }
                            )
                        }
                    }
                } else {
                    val isActive = when (item.id) {
                        ToolbarActionId.HEADING -> headingActive
                        ToolbarActionId.BULLET_LIST -> bulletActive
                        ToolbarActionId.CHECKLIST -> isChecklistActive
                        ToolbarActionId.LINK -> linkActive
                        else -> false
                    }
                    val enabled = when (item.id) {
                        ToolbarActionId.UNDO -> canUndo && !isChecklistActive
                        ToolbarActionId.REDO -> canRedo && !isChecklistActive
                        ToolbarActionId.HEADING, ToolbarActionId.BULLET_LIST -> !isChecklistActive
                        ToolbarActionId.LINK -> !isChecklistActive && (hasSelection || linkActive)
                        else -> true
                    }
                    ToolbarIconButton(
                        icon = item.icon,
                        contentDescription = item.label,
                        isActive = isActive,
                        enabled = enabled,
                        onClick = {
                            when (item.id) {
                                ToolbarActionId.UNDO -> onUndo()
                                ToolbarActionId.REDO -> onRedo()
                                ToolbarActionId.HEADING -> { editText?.let { toggleHeading(it) }; syncHtml() }
                                ToolbarActionId.BULLET_LIST -> editText?.let { toggleBulletList(it) }
                                ToolbarActionId.CHECKLIST -> onChecklistToggle()
                                ToolbarActionId.LINK -> {
                                    if (linkActive) { editText?.let { removeLink(it) }; syncHtml() } else showLinkDialog = true
                                }
                                ToolbarActionId.IMAGE -> onInsertImage()
                                ToolbarActionId.MORE -> Unit // handled above
                            }
                        }
                    )
                }
            }
        }
    }

    if (showLinkDialog) {
        LinkInputDialog(
            onConfirm = { url ->
                editText?.let { applyLink(it, url) }
                syncHtml()
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }
    if (showTextColorPicker) {
        ColorPickerDialog(
            title = "Màu chữ",
            currentColor = formatState.textColor,
            onColorSelected = { color -> editText?.let { setTextColor(it, color) }; syncHtml(); showTextColorPicker = false },
            onDismiss = { showTextColorPicker = false }
        )
    }
}

@Composable
private fun LinkInputDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("https://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm liên kết") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                placeholder = { Text("https://example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url.trim()) }, enabled = url.isNotBlank()) { Text("Thêm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                if (isActive && enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                CircleShape
            )
            .border(
                1.dp,
                if (isActive && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp), tint = tint)
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

/** Full Material Design palette (500-weight hues) plus black/white/grey — 24 colors total. */
private val COLOR_SWATCHES = listOf(
    Color.Black, Color(0xFF424242), Color(0xFF9E9E9E), Color(0xFF607D8B), Color(0xFFBDBDBD), Color.White,
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3),
    Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
    Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548), Color(0xFF1259C3),
)

@Composable
private fun ColorPickerDialog(
    title: String,
    currentColor: Color?,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                COLOR_SWATCHES.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { color ->
                            val isSelected = color == currentColor
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFDDDDDD),
                                        CircleShape
                                    )
                                    .clickable { onColorSelected(color) }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Đang chọn",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Xong") } }
    )
}
