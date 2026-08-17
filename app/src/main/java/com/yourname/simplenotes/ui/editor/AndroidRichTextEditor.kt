package com.yourname.simplenotes.ui.editor

import android.graphics.Typeface
import android.text.Selection
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.widget.doAfterTextChanged
import com.yourname.simplenotes.util.HtmlSpannableConverter
import kotlin.math.roundToInt

/**
 * A rich-text editor backed by Android's native EditText (via AndroidView).
 *
 * EditText provides full gesture support — double-tap to select a word, long-press for
 * selection handles, copy/paste — matching the behavior users expect from Samsung Notes,
 * Easy Notes, and other native Android apps.
 *
 * Rich formatting (bold, italic, underline, strikethrough, colors, font size) is stored as
 * Spannable spans and serialized to/from HTML for storage and Drive sync.
 */
@Composable
fun AndroidRichTextEditor(
    html: String,
    onHtmlChange: (String) -> Unit,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    /** Called once with the EditText instance when the view is created. */
    onEditTextReady: ((EditText) -> Unit)? = null
) {
    val context = LocalContext.current
    val textColorInt = textColor.toArgb()

    // Track the current HTML to avoid re-setting when nothing changed
    var lastHtml by remember { mutableStateOf("") }
    var isInternalChange by remember { mutableStateOf(false) }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            ScrollAwareEditText(ctx).apply {
                setPadding(
                    dpToPx(ctx, 6),
                    dpToPx(ctx, 12),
                    dpToPx(ctx, 6),
                    dpToPx(ctx, 12)
                )
                // Multi-line, no autocorrect/suggestions — just like Samsung Notes.
                // Do NOT add TYPE_TEXT_VARIATION_VISIBLE_PASSWORD here: many IMEs (Gboard
                // Telex/VNI, Laban Key, etc.) disable diacritic/tone-mark composition when a
                // field reports itself as a password field, which silently breaks Vietnamese
                // text input (dấu can't be applied).
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = EditorInfo.IME_ACTION_NEXT
                isHorizontalScrollBarEnabled = false
                isVerticalScrollBarEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(textColorInt)
                textSize = textStyle.fontSize.value
                typeface = Typeface.DEFAULT

                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                isFocusable = true
                // isFocusableInTouchMode and showSoftInputOnFocus are managed by ScrollAwareEditText

                // Load initial content
                val spannable = HtmlSpannableConverter.htmlToSpannable(html)
                setText(spannable)
                lastHtml = html

                // Sync EditText → ViewModel on every text change
                doAfterTextChanged { editable ->
                    if (isInternalChange) return@doAfterTextChanged
                    val newHtml = HtmlSpannableConverter.spannableToHtml(editable ?: return@doAfterTextChanged)
                    lastHtml = newHtml
                    onHtmlChange(newHtml)
                }

                // Expose EditText ref to the parent screen for the toolbar
                onEditTextReady?.invoke(this)
            }
        },
        update = { editText ->
            // Only update if HTML changed from outside (e.g. loading a new note)
            if (html != lastHtml) {
                isInternalChange = true
                val spannable = HtmlSpannableConverter.htmlToSpannable(html)
                // Preserve the full selection range (not just a collapsed cursor) across this
                // reparse — the toolbar's format buttons trigger this same path (see
                // EditorToolbar.syncHtml), so collapsing to a point here would deselect
                // the user's text after every single toggle, forcing them to reselect before
                // applying a second style (e.g. bold then italic on the same word).
                val selStart = minOf(editText.selectionStart, editText.selectionEnd).coerceIn(0, spannable.length)
                val selEnd = maxOf(editText.selectionStart, editText.selectionEnd).coerceIn(0, spannable.length)
                editText.setText(spannable)
                if (spannable.isNotEmpty()) {
                    Selection.setSelection(editText.text, selStart, selEnd)
                }
                lastHtml = html
                isInternalChange = false
            }

            // Sync text color
            if (editText.currentTextColor != textColorInt) {
                editText.setTextColor(textColorInt)
            }
            if (editText.textSize != textStyle.fontSize.value) {
                editText.textSize = textStyle.fontSize.value
            }
            editText.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun dpToPx(ctx: android.content.Context, dp: Int): Int =
    (dp * ctx.resources.displayMetrics.density).roundToInt()

/** Fixed highlight color used by the toolbar's toggleable Highlight button (as opposed to the
 *  arbitrary background color picker, which uses [setBackgroundColor] with any color). */
val HIGHLIGHT_COLOR = Color(0xFFFFF59D)

/** Scale used to render a "heading" line — whole-line [RelativeSizeSpan] + bold. */
private const val HEADING_SCALE = 1.4f

private const val BULLET_PREFIX = "• "

/**
 * Represents the current formatting state at the cursor position in an EditText.
 */
data class FormatState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isHighlighted: Boolean = false,
    val textColor: Color? = null,
    val fontSizePx: Int? = null
)

/**
 * Returns the formatting state at the current cursor/selection in the given EditText.
 */
fun getFormatState(editText: android.widget.EditText): FormatState {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0 || editText.text.isNullOrEmpty()) return FormatState()

    val text = editText.text as? Spannable ?: return FormatState()
    val pos = selEnd.coerceAtMost(text.length - 1).coerceAtLeast(0)
    val spans = text.getSpans(pos, pos + 1, Any::class.java)

    var bold = false
    var italic = false
    var underline = false
    var strike = false
    var fgColor: Color? = null
    var bgColor: Color? = null
    var fontSize: Int? = null

    spans.forEach { span ->
        when (span) {
            is StyleSpan -> {
                when (span.style) {
                    Typeface.BOLD -> bold = true
                    Typeface.ITALIC -> italic = true
                    Typeface.BOLD_ITALIC -> { bold = true; italic = true }
                }
            }
            is UnderlineSpan -> underline = true
            is StrikethroughSpan -> strike = true
            is ForegroundColorSpan -> fgColor = Color(span.foregroundColor)
            is BackgroundColorSpan -> bgColor = Color(span.backgroundColor)
            is RelativeSizeSpan -> {
                // Convert back to approximate px based on default 14sp
                fontSize = (span.sizeChange * 14f).roundToInt()
            }
        }
    }

    return FormatState(
        isBold = bold, isItalic = italic, isUnderline = underline,
        isStrikethrough = strike, isHighlighted = bgColor == HIGHLIGHT_COLOR,
        textColor = fgColor, fontSizePx = fontSize
    )
}

/**
 * Applies or removes bold at the current selection in an EditText.
 */
fun toggleBold(editText: android.widget.EditText) {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0) return
    val text = editText.text as? Spannable ?: return
    toggleStyleSpan(text, selStart, selEnd, Typeface.BOLD)
}

/**
 * Applies or removes italic at the current selection in an EditText.
 */
fun toggleItalic(editText: android.widget.EditText) {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0) return
    val text = editText.text as? Spannable ?: return
    toggleStyleSpan(text, selStart, selEnd, Typeface.ITALIC)
}

/**
 * Applies or removes underline at the current selection in an EditText.
 */
fun toggleUnderline(editText: android.widget.EditText) {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0) return
    val text = editText.text as? Spannable ?: return
    toggleSpan<UnderlineSpan>(text, selStart, selEnd)
}

/**
 * Applies or removes strikethrough at the current selection in an EditText.
 */
fun toggleStrikethrough(editText: android.widget.EditText) {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0) return
    val text = editText.text as? Spannable ?: return
    toggleSpan<StrikethroughSpan>(text, selStart, selEnd)
}

/**
 * Sets the text color at the current selection in an EditText.
 * If [color] is null, removes the color span.
 */
fun setTextColor(editText: android.widget.EditText, color: Color?) {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0) return
    val text = editText.text as? Spannable ?: return
    if (color != null) {
        setSpan<ForegroundColorSpan>(text, selStart, selEnd) { ForegroundColorSpan(color.toArgb()) }
    } else {
        text.getSpans(selStart, selEnd, ForegroundColorSpan::class.java).forEach { text.removeSpan(it) }
    }
}

/**
 * Sets the font size at the current selection in an EditText.
 * Uses RelativeSizeSpan relative to the default 14sp.
 */
fun setFontSize(editText: android.widget.EditText, sizePx: Int) {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0) return
    val text = editText.text as? Spannable ?: return
    // Remove existing RelativeSizeSpans in selection
    text.getSpans(selStart, selEnd, RelativeSizeSpan::class.java).forEach { text.removeSpan(it) }
    val scale = sizePx / 14f
    text.setSpan(RelativeSizeSpan(scale), selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}

/**
 * Toggles a fixed-color highlight (like a text marker) at the current selection.
 * Distinct from [setBackgroundColor], which sets an arbitrary background color from a picker.
 */
fun toggleHighlight(editText: android.widget.EditText) {
    val selStart = editText.selectionStart
    val selEnd = editText.selectionEnd
    if (selStart < 0 || selStart == selEnd) return
    val text = editText.text as? Spannable ?: return
    val existing = text.getSpans(selStart, selEnd, BackgroundColorSpan::class.java)
        .filter { it.backgroundColor == HIGHLIGHT_COLOR.toArgb() }
    if (existing.isNotEmpty()) {
        existing.forEach { text.removeSpan(it) }
    } else {
        text.setSpan(BackgroundColorSpan(HIGHLIGHT_COLOR.toArgb()), selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

/**
 * Toggles "heading" styling (bold + larger text) on the whole line the cursor is on.
 */
fun toggleHeading(editText: android.widget.EditText) {
    val text = editText.text as? Spannable ?: return
    val pos = editText.selectionStart.coerceIn(0, text.length)
    val start = lineStart(text, pos)
    val end = lineEnd(text, pos)
    if (start >= end) return

    if (isHeadingLine(editText)) {
        text.getSpans(start, end, RelativeSizeSpan::class.java)
            .filter { it.sizeChange == HEADING_SCALE && text.getSpanStart(it) == start && text.getSpanEnd(it) == end }
            .forEach { text.removeSpan(it) }
        text.getSpans(start, end, StyleSpan::class.java)
            .filter { it.style == Typeface.BOLD && text.getSpanStart(it) == start && text.getSpanEnd(it) == end }
            .forEach { text.removeSpan(it) }
    } else {
        text.setSpan(RelativeSizeSpan(HEADING_SCALE), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

/** Whether the line the cursor is on is currently styled as a heading. */
fun isHeadingLine(editText: android.widget.EditText): Boolean {
    val text = editText.text as? Spannable ?: return false
    val pos = editText.selectionStart.coerceIn(0, text.length)
    val start = lineStart(text, pos)
    val end = lineEnd(text, pos)
    if (start >= end) return false
    return text.getSpans(start, end, RelativeSizeSpan::class.java)
        .any { it.sizeChange == HEADING_SCALE && text.getSpanStart(it) == start && text.getSpanEnd(it) == end }
}

/** Toggles a "• " bullet prefix on the line the cursor is on. */
fun toggleBulletList(editText: android.widget.EditText) {
    val editable = editText.text ?: return
    val pos = editText.selectionStart.coerceIn(0, editable.length)
    val start = lineStart(editable, pos)
    if (isBulletLine(editText)) {
        editable.delete(start, start + BULLET_PREFIX.length)
    } else {
        editable.insert(start, BULLET_PREFIX)
    }
}

/** Whether the line the cursor is on already starts with the bullet prefix. */
fun isBulletLine(editText: android.widget.EditText): Boolean {
    val text = editText.text ?: return false
    val pos = editText.selectionStart.coerceIn(0, text.length)
    val start = lineStart(text, pos)
    return start + BULLET_PREFIX.length <= text.length &&
        text.subSequence(start, start + BULLET_PREFIX.length).toString() == BULLET_PREFIX
}

/** Whether the current selection is entirely covered by a link ([URLSpan]). */
fun hasLinkInSelection(editText: android.widget.EditText): Boolean {
    val start = editText.selectionStart
    val end = editText.selectionEnd
    if (start < 0 || start == end) return false
    val text = editText.text as? Spannable ?: return false
    return text.getSpans(start, end, URLSpan::class.java).any {
        text.getSpanStart(it) <= start && text.getSpanEnd(it) >= end
    }
}

/** Turns the current selection into a link pointing at [url], replacing any existing link there. */
fun applyLink(editText: android.widget.EditText, url: String) {
    val start = editText.selectionStart
    val end = editText.selectionEnd
    if (start < 0 || start == end || url.isBlank()) return
    val text = editText.text as? Spannable ?: return
    text.getSpans(start, end, URLSpan::class.java).forEach { text.removeSpan(it) }
    text.setSpan(URLSpan(url), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    setSpan<ForegroundColorSpan>(text, start, end) { ForegroundColorSpan(LINK_COLOR) }
}

/** Removes the link (and its color) covering the current selection, keeping the plain text. */
fun removeLink(editText: android.widget.EditText) {
    val start = editText.selectionStart
    val end = editText.selectionEnd
    if (start < 0 || start == end) return
    val text = editText.text as? Spannable ?: return
    text.getSpans(start, end, URLSpan::class.java).forEach { text.removeSpan(it) }
    text.getSpans(start, end, ForegroundColorSpan::class.java)
        .filter { it.foregroundColor == LINK_COLOR }
        .forEach { text.removeSpan(it) }
}

private const val LINK_COLOR = 0xFF1259C3.toInt()

private fun lineStart(text: CharSequence, pos: Int): Int {
    var i = pos.coerceIn(0, text.length)
    while (i > 0 && text[i - 1] != '\n') i--
    return i
}

private fun lineEnd(text: CharSequence, pos: Int): Int {
    var i = pos.coerceIn(0, text.length)
    while (i < text.length && text[i] != '\n') i++
    return i
}

// ── Internal helpers ─────────────────────────────────────────────────────────

private fun toggleStyleSpan(text: Spannable, start: Int, end: Int, style: Int) {
    @Suppress("UNCHECKED_CAST")
    val existing = text.getSpans(start, end, StyleSpan::class.java as Class<Any>).filter {
        (it as? StyleSpan)?.style == style || (it as? StyleSpan)?.style == Typeface.BOLD_ITALIC
    }
    if (existing.isNotEmpty()) {
        existing.forEach { text.removeSpan(it) }
    } else {
        text.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

private inline fun <reified T : Any> toggleSpan(text: Spannable, start: Int, end: Int) {
    val existing = text.getSpans(start, end, T::class.java)
    if (existing.isNotEmpty()) {
        existing.forEach { text.removeSpan(it) }
    } else {
        try {
            val span = T::class.java.getDeclaredConstructor().newInstance()
            text.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } catch (_: Exception) { }
    }
}

private inline fun <reified T : Any> setSpan(
    text: Spannable,
    start: Int,
    end: Int,
    factory: () -> T
) {
    // Remove existing spans in selection
    text.getSpans(start, end, T::class.java).forEach { text.removeSpan(it) }
    if (start < end) {
        text.setSpan(factory(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
