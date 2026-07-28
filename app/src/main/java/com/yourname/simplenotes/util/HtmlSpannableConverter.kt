package com.yourname.simplenotes.util

import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import org.xml.sax.XMLReader
import java.util.regex.Pattern

/**
 * Converts between HTML strings and Android Spannable for rich-text editing.
 * Supports: bold, italic, underline, strikethrough, foreground color, background color, font size.
 *
 * EditText stores rich text as Spannable internally. This converter serializes it to/from
 * the same HTML format the app uses for Drive sync, so existing saved notes load correctly.
 */
object HtmlSpannableConverter {

    private const val TAG_BOLD = "b"
    private const val TAG_ITALIC = "i"
    private const val TAG_UNDERLINE = "u"
    private const val TAG_STRIKETHROUGH = "s"
    private const val TAG_FONT = "font"
    private const val TAG_BIG = "big"
    private const val TAG_SMALL = "small"
    private const val TAG_BR = "br"
    private const val TAG_P = "p"
    private const val TAG_SPAN = "span"

    private const val ATTR_COLOR = "color"
    private const val ATTR_BGCOLOR = "bgcolor"
    private const val ATTR_SIZE = "size"

    /** Converts EditText HTML output to Spannable for display/editing in EditText. */
    @Suppress("DEPRECATION")
    fun htmlToSpannable(html: String): Spannable {
        if (html.isBlank()) return SpannableStringBuilder()

        return try {
            val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, null, TagHandler())
            val spannable = SpannableStringBuilder(spanned)

            // Post-process <font size="N"> → RelativeSizeSpan
            processFontSizeTags(spannable, html)

            spannable
        } catch (e: Exception) {
            // Fallback: return plain text
            SpannableStringBuilder(
                Html.fromHtml(
                    html.replace("<", "&lt;").replace(">", "&gt;"),
                    Html.FROM_HTML_MODE_COMPACT
                )
            )
        }
    }

    /** Converts Spannable (from EditText) back to HTML string for storage. */
    fun spannableToHtml(spannable: Spannable): String {
        if (spannable.isEmpty()) return ""

        val builder = StringBuilder()
        var i = 0
        val len = spannable.length
        var inParagraph = false

        fun openParagraph() {
            if (!inParagraph) {
                builder.append("<p>")
                inParagraph = true
            }
        }

        fun closeParagraph() {
            if (inParagraph) {
                builder.append("</p>")
                inParagraph = false
            }
        }

        while (i < len) {
            val char = spannable[i]
            val prevChar = if (i > 0) spannable[i - 1] else '\n'

            // Handle newlines: flush paragraph
            if (char == '\n') {
                closeParagraph()
                builder.append("<br>")
                i++
                continue
            }

            // Opening a new paragraph (previous was newline or start)
            if (prevChar == '\n') {
                openParagraph()
            }

            val spans = spannable.getSpans(i, i + 1, Any::class.java)
            val bold = spans.filterIsInstance<StyleSpan>().find { it.style == Typeface.BOLD }
            val italic = spans.filterIsInstance<StyleSpan>().find { it.style == Typeface.ITALIC }
            val underline = spans.find { it is UnderlineSpan }
            val strike = spans.find { it is StrikethroughSpan }
            val fg = spans.filterIsInstance<ForegroundColorSpan>().firstOrNull()
            val bg = spans.filterIsInstance<BackgroundColorSpan>().firstOrNull()
            val sizeUp = spans.filterIsInstance<RelativeSizeSpan>().filter { it.sizeChange > 1f }.minByOrNull { it.sizeChange }
            val sizeDown = spans.filterIsInstance<RelativeSizeSpan>().filter { it.sizeChange < 1f }.maxByOrNull { it.sizeChange }

            if (bold != null || italic != null) {
                builder.append("<")
                if (bold != null) builder.append("b")
                if (italic != null) builder.append("i")
                builder.append(">")
            }
            if (underline != null) builder.append("<u>")
            if (strike != null) builder.append("<s>")
            if (fg != null) {
                builder.append("<font color=\"#")
                builder.append(fg.foregroundColor.toString(16).padStart(8, '0').takeLast(6))
                builder.append("\">")
            }
            if (bg != null) {
                builder.append("<font bgcolor=\"#")
                builder.append(bg.backgroundColor.toString(16).padStart(8, '0').takeLast(6))
                builder.append("\">")
            }
            if (sizeUp != null) {
                val px = (sizeUp.sizeChange * 14).toInt()
                builder.append("<font size=\"$px\">")
            }
            if (sizeDown != null) {
                val px = (sizeDown.sizeChange * 14).toInt()
                builder.append("<font size=\"$px\">")
            }

            builder.append(
                when (char) {
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '&' -> "&amp;"
                    else -> char
                }
            )

            // Close tags at span end boundaries
            val nextPos = i + 1
            spans.filterIsInstance<RelativeSizeSpan>()
                .filter { spannable.getSpanStart(it) == i && spannable.getSpanEnd(it) == nextPos }
                .forEach { builder.append("</font>") }
            spans.filterIsInstance<BackgroundColorSpan>()
                .filter { spannable.getSpanStart(it) == i && spannable.getSpanEnd(it) == nextPos }
                .forEach { builder.append("</font>") }
            spans.filterIsInstance<ForegroundColorSpan>()
                .filter { spannable.getSpanStart(it) == i && spannable.getSpanEnd(it) == nextPos }
                .forEach { builder.append("</font>") }
            spans.filterIsInstance<StrikethroughSpan>()
                .filter { spannable.getSpanStart(it) == i && spannable.getSpanEnd(it) == nextPos }
                .forEach { builder.append("</s>") }
            spans.filterIsInstance<UnderlineSpan>()
                .filter { spannable.getSpanStart(it) == i && spannable.getSpanEnd(it) == nextPos }
                .forEach { builder.append("</u>") }
            val closeBold = bold != null && spans.filterIsInstance<StyleSpan>()
                .filter { it.style == Typeface.BOLD }
                .any { spannable.getSpanStart(it) == i && spannable.getSpanEnd(it) == nextPos }
            val closeItalic = italic != null && spans.filterIsInstance<StyleSpan>()
                .filter { it.style == Typeface.ITALIC }
                .any { spannable.getSpanStart(it) == i && spannable.getSpanEnd(it) == nextPos }
            if (closeBold || closeItalic) {
                builder.append("</")
                if (closeBold) builder.append("b")
                if (closeItalic) builder.append("i")
                builder.append(">")
            }

            // Check if paragraph should close after this character (next is newline or span ends here)
            val nextIsNewline = nextPos < len && spannable[nextPos] == '\n'
            val paragraphShouldClose = nextIsNewline || spans.any { span ->
                spannable.getSpanStart(span) <= i && spannable.getSpanEnd(span) == nextPos
            }
            if (paragraphShouldClose) {
                closeParagraph()
            }

            i++
        }

        // Close any open paragraph at end
        closeParagraph()
        return builder.toString()
    }

    // ── Tag handler for Html.fromHtml ─────────────────────────────────────────

    private class TagHandler : Html.TagHandler {
        override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
            when (tag.lowercase()) {
                TAG_BOLD -> {
                    if (opening) setSpan(output, StyleSpan(Typeface.BOLD))
                    else removeLastSpanOfType(output, StyleSpan::class.java)
                }
                TAG_ITALIC -> {
                    if (opening) setSpan(output, StyleSpan(Typeface.ITALIC))
                    else removeLastSpanOfType(output, StyleSpan::class.java)
                }
                TAG_UNDERLINE -> {
                    if (opening) setSpan(output, UnderlineSpan())
                    else removeLastSpanOfType(output, UnderlineSpan::class.java)
                }
                TAG_STRIKETHROUGH -> {
                    if (opening) setSpan(output, StrikethroughSpan())
                    else removeLastSpanOfType(output, StrikethroughSpan::class.java)
                }
                TAG_BIG -> {
                    if (opening) setSpan(output, RelativeSizeSpan(1.2f))
                    else removeLastSpanOfType(output, RelativeSizeSpan::class.java)
                }
                TAG_SMALL -> {
                    if (opening) setSpan(output, RelativeSizeSpan(0.85f))
                    else removeLastSpanOfType(output, RelativeSizeSpan::class.java)
                }
                TAG_FONT -> {
                    handleFontTag(opening, output, xmlReader)
                }
            }
        }

        private fun handleFontTag(opening: Boolean, output: Editable, xmlReader: XMLReader) {
            if (!opening) {
                removeLastSpanOfType(output, ForegroundColorSpan::class.java)
                removeLastSpanOfType(output, BackgroundColorSpan::class.java)
                removeLastSpanOfType(output, RelativeSizeSpan::class.java)
                return
            }
            if (opening) {
                // Parse attributes from XMLReader using reflection
                val color = getXmlAttr(xmlReader, ATTR_COLOR)
                if (color != null) {
                    val parsed = parseColor(color)
                    if (parsed != 0) setSpan(output, ForegroundColorSpan(parsed))
                }
                val bg = getXmlAttr(xmlReader, ATTR_BGCOLOR)
                if (bg != null) {
                    val parsed = parseColor(bg)
                    if (parsed != 0) setSpan(output, BackgroundColorSpan(parsed))
                }
                val size = getXmlAttr(xmlReader, ATTR_SIZE)
                if (size != null) {
                    val sizePx = size.removeSurrounding("\"").toIntOrNull()
                    if (sizePx != null && sizePx > 0) {
                        setSpan(output, RelativeSizeSpan(sizePx / 14f))
                    }
                }
            }
        }

        private fun getXmlAttr(reader: XMLReader, name: String): String? {
            return try {
                val attrsField = reader.javaClass.getDeclaredField("theAtts")
                attrsField.isAccessible = true
                val atts = attrsField.get(reader)
                val method = atts.javaClass.getMethod("getValue", String::class.java)
                method.invoke(atts, name) as? String
            } catch (_: Exception) {
                null
            }
        }

        private fun setSpan(output: Editable, span: Any) {
            val len = output.length
            if (len > 0) {
                output.setSpan(span, 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        private fun <T> removeLastSpanOfType(output: Editable, clazz: Class<T>) {
            val spans = output.getSpans(0, output.length, clazz)
            if (spans.isNotEmpty()) {
                output.removeSpan(spans.last())
            }
        }
    }

    private fun processFontSizeTags(spannable: SpannableStringBuilder, html: String) {
        val regex = Pattern.compile("""<font\s+size\s*=\s*["']?(\d+)["']?\s*>""")
        val matcher = regex.matcher(html)
        while (matcher.find()) {
            val px = matcher.group(1)?.toIntOrNull() ?: continue
            val scale = px / 14f
            val start = matcher.start()
            val end = (start + 1).coerceAtMost(spannable.length)
            if (start < end) {
                spannable.setSpan(RelativeSizeSpan(scale), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun parseColor(colorStr: String): Int {
        return try {
            when {
                colorStr.startsWith("#") && (colorStr.length == 7 || colorStr.length == 9) -> {
                    android.graphics.Color.parseColor(colorStr)
                }
                colorStr.startsWith("rgb") -> {
                    val nums = Regex("""\d+""").findAll(colorStr).map { it.value.toInt() }.toList()
                    if (nums.size >= 3) android.graphics.Color.rgb(nums[0], nums[1], nums[2]) else 0
                }
                else -> android.graphics.Color.parseColor(colorStr)
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Converts plain text shared from other apps into the minimal HTML format used by the editor.
     */
    fun plainTextToHtml(text: String): String =
        text.split("\n").joinToString("<br>") { android.text.Html.escapeHtml(it) }
}
