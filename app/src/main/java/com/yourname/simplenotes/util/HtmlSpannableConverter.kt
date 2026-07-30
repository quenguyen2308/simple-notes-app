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
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.xml.sax.XMLReader
import java.util.regex.Pattern

/**
 * Converts between HTML strings and Android Spannable for rich-text editing.
 * Supports: bold, italic, underline, strikethrough, foreground color, background color, font size, links.
 *
 * EditText stores rich text as Spannable internally. This converter serializes it to/from
 * the same HTML format the app uses for Drive sync, so existing saved notes load correctly.
 */
object HtmlSpannableConverter {

    private const val TAG_BOLD = "b"
    private const val TAG_ITALIC = "i"
    private const val TAG_BOLD_ITALIC = "bi"
    private const val TAG_UNDERLINE = "u"
    private const val TAG_STRIKETHROUGH = "s"
    private const val TAG_FONT = "font"
    private const val TAG_BIG = "big"
    private const val TAG_SMALL = "small"
    private const val TAG_LINK = "a"

    private const val ATTR_COLOR = "color"
    private const val ATTR_BGCOLOR = "bgcolor"
    private const val ATTR_SIZE = "size"
    private const val ATTR_HREF = "href"

    /** Converts EditText HTML output to Spannable for display/editing in EditText. */
    @Suppress("DEPRECATION")
    fun htmlToSpannable(html: String): Spannable {
        if (html.isBlank()) return SpannableStringBuilder()

        return try {
            // spannableToHtml() only ever emits <br> for line breaks (no <p> wrapping), but two
            // older formats exist in real saved notes: (a) a buggy version that double-marked
            // every line break with *both* <p> and an adjacent <br>, and (b) the original rich
            // editor library, which wrapped each line in its own <p>...</p> with NO <br> at all
            // between them. So: collapse a <br> sitting directly against a <p> boundary (case a,
            // to avoid a doubled blank line), then treat every remaining </p> as one line break
            // in its own right (case b) — rather than stripping <p>/</p> to nothing, which used
            // to merge every paragraph in a (b)-style note into a single run with no separator.
            // Must be a Unicode Private Use Area char, not a C0 control char: Html.fromHtml()
            // silently drops C0 control chars from its output entirely (verified experimentally),
            // which would erase the very line breaks this placeholder exists to protect.
            val brPlaceholder = ""
            val cleaned = html
                .replace(Regex("(</p>)\\s*<br\\s*/?>", RegexOption.IGNORE_CASE), "$1")
                .replace(Regex("<br\\s*/?>\\s*(<p[^>]*>)", RegexOption.IGNORE_CASE), "$1")
                .replace(Regex("</p>", RegexOption.IGNORE_CASE), brPlaceholder)
                .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "")
            val spanned = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY, null, TagHandler())
            val spannable = SpannableStringBuilder(spanned)

            // Swap the placeholder back to a real newline now that spans are attached — a
            // 1-for-1 char substitution via Editable.replace keeps every span's offsets valid.
            var idx = spannable.indexOf(brPlaceholder)
            while (idx >= 0) {
                spannable.replace(idx, idx + 1, "\n")
                idx = spannable.indexOf(brPlaceholder, idx + 1)
            }

            // Post-process <font size="N"> → RelativeSizeSpan
            processFontSizeTags(spannable, cleaned)

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

    /**
     * Strips HTML tags from the editor's HTML output and converts <br> to single newlines.
     * Used for the Note.content plain-text field shown in the note list/preview.
     *
     * Note: does NOT rely on Html.fromHtml() for tag/newline handling — that API adds extra
     * newlines around <p> tags which would cause double-line-break artifacts in the note list
     * preview. It IS used as a last step purely to decode HTML entities (named like &amp;,
     * &comma;, &dstrok; and numeric like &#432;, &#7901;) once no tags remain, since notes
     * saved by older editor versions can contain arbitrary entity-escaped text, not just the
     * handful of entities HTML itself requires.
     *
     * Two older formats are handled specially, both seen in real saved notes: (a) a buggy
     * version that double-marked every line break with *both* <p> and an adjacent <br>, and
     * (b) the original rich editor library, which wrapped each line in its own <p>...</p> with
     * NO <br> at all between them. A <br> sitting directly against a <p> boundary is collapsed
     * away first (case a, so it isn't counted twice), then every remaining </p> is treated as
     * one line break in its own right (case b) — stripping <p>/</p> to nothing here would merge
     * every paragraph of a (b)-style note into one run with no separator at all.
     */
    fun htmlToPlainText(html: String): String {
        if (html.isBlank()) return ""
        // Must be a Unicode Private Use Area char, not a C0 control char: Html.fromHtml()
        // silently drops C0 control chars from its output entirely (verified experimentally),
        // which would erase the very line breaks this placeholder exists to protect.
        val brPlaceholder = ""
        val stripped = html
            .replace(Regex("(</p>)\\s*<br\\s*/?>", RegexOption.IGNORE_CASE), "$1")
            .replace(Regex("<br\\s*/?>\\s*(<p[^>]*>)", RegexOption.IGNORE_CASE), "$1")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), brPlaceholder)
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), brPlaceholder)
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), "")
            // Some fallback paths (e.g. Drive JSON deserialized without a contentBlocksJson,
            // see NoteJsonExtensions.fromJson) store raw plain text — real newlines, not <br> —
            // as htmlContent. Html.fromHtml() below collapses literal whitespace like any other
            // HTML renderer would, so protect those the same way as <br> or they'd flatten to spaces.
            .replace(Regex("\r?\n"), brPlaceholder)
        return Html.fromHtml(stripped, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace(' ', ' ') // &nbsp; decodes to a non-breaking space; normalize to a plain one
            .replace(brPlaceholder, "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    /**
     * Converts Spannable (from EditText) back to HTML string for storage.
     *
     * Line breaks are represented with a single `<br>` per newline — nothing else. An earlier
     * version also wrapped each line in `<p>...</p>`, which double-marked every line break
     * (the paragraph boundary *and* the `<br>` both represented the same newline); reloading
     * that HTML through Html.fromHtml then added its own spacing around the `<p>` tags on top
     * of the explicit `<br>`, so the gap between lines grew a little more each time a note was
     * reopened and re-saved. See htmlToSpannable() for handling notes already saved that way.
     */
    fun spannableToHtml(spannable: Spannable): String {
        if (spannable.isEmpty()) return ""

        val builder = StringBuilder()
        var i = 0
        val len = spannable.length

        while (i < len) {
            val char = spannable[i]

            if (char == '\n') {
                builder.append("<br>")
                i++
                continue
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
            val link = spans.filterIsInstance<URLSpan>().firstOrNull()

            // A span must only open a tag at its own start offset and close it at its own end
            // offset — checking mere presence at position i (as an earlier version of this code
            // did) reopens the tag at every character it covers and never closes a span longer
            // than one character, since start==i is only true once but was also required for the
            // close check to fire. That left every multi-character bold/italic/etc. selection
            // serialized as unclosed tags trailing to the end of the document.
            val nextPos = i + 1
            fun startsHere(span: Any?) = span != null && spannable.getSpanStart(span) == i
            fun endsHere(span: Any?) = span != null && spannable.getSpanEnd(span) == nextPos

            if (startsHere(link)) {
                builder.append("<a href=\"").append((link as URLSpan).url.replace("\"", "&quot;")).append("\">")
            }
            val openBold = startsHere(bold)
            val openItalic = startsHere(italic)
            if (openBold || openItalic) {
                builder.append("<")
                if (openBold) builder.append("b")
                if (openItalic) builder.append("i")
                builder.append(">")
            }
            if (startsHere(underline)) builder.append("<u>")
            if (startsHere(strike)) builder.append("<s>")
            if (fg != null && startsHere(fg)) {
                builder.append("<font color=\"#")
                builder.append(rgbHex(fg.foregroundColor))
                builder.append("\">")
            }
            if (bg != null && startsHere(bg)) {
                builder.append("<font bgcolor=\"#")
                builder.append(rgbHex(bg.backgroundColor))
                builder.append("\">")
            }
            if (sizeUp != null && startsHere(sizeUp)) {
                val px = (sizeUp.sizeChange * 14).toInt()
                builder.append("<font size=\"$px\">")
            }
            if (sizeDown != null && startsHere(sizeDown)) {
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

            // Close tags at span end boundaries, in reverse of the order they were opened above
            if (sizeUp != null && endsHere(sizeUp)) builder.append("</font>")
            if (sizeDown != null && endsHere(sizeDown)) builder.append("</font>")
            if (bg != null && endsHere(bg)) builder.append("</font>")
            if (fg != null && endsHere(fg)) builder.append("</font>")
            if (endsHere(strike)) builder.append("</s>")
            if (endsHere(underline)) builder.append("</u>")
            val closeBold = endsHere(bold)
            val closeItalic = endsHere(italic)
            if (closeBold || closeItalic) {
                builder.append("</")
                if (closeBold) builder.append("b")
                if (closeItalic) builder.append("i")
                builder.append(">")
            }
            if (endsHere(link)) builder.append("</a>")

            i++
        }

        return builder.toString()
    }

    // ── Tag handler for Html.fromHtml ─────────────────────────────────────────

    private class TagHandler : Html.TagHandler {
        // Tracks the output offset where <bi> opened, so its close can span exactly the text
        // in between. Unlike "b"/"i"/"u"/"s"/"font" — which Android's HtmlToSpannedConverter
        // recognizes and spans internally via its own correctly-tracked start/end, never even
        // reaching this custom handler — "bi" is unrecognized by Android and is the one tag this
        // class is actually responsible for getting right end-to-end.
        private val boldItalicStarts = ArrayDeque<Int>()

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
                TAG_BOLD_ITALIC -> {
                    // spannableToHtml() emits this combined tag (rather than nested <b><i>) for
                    // a character that's both bold and italic — Html.fromHtml() only recognizes
                    // plain "b"/"i", so without this case the tag is silently unrecognized and
                    // the formatting is dropped the next time the note is loaded.
                    if (opening) {
                        boldItalicStarts.addLast(output.length)
                    } else {
                        val start = boldItalicStarts.removeLastOrNull() ?: return
                        val end = output.length
                        if (start < end) {
                            output.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            output.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
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
                TAG_LINK -> {
                    if (opening) {
                        val href = getXmlAttr(xmlReader, ATTR_HREF)
                        if (href != null) setSpan(output, URLSpan(href))
                    } else {
                        removeLastSpanOfType(output, URLSpan::class.java)
                    }
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

    /**
     * Hex-encodes the RGB bits of an ARGB color int as "rrggbb".
     *
     * Int.toString(16) is wrong here: any opaque color (alpha 0xFF) sets the sign bit, so the
     * ARGB int is negative, and Kotlin/Java's toString(radix) renders negative numbers as a
     * "-" followed by the hex digits of the *magnitude* — not two's-complement hex — which
     * silently produced the wrong RGB value (or an invalid one) for almost every color.
     */
    private fun rgbHex(argb: Int): String =
        (argb and 0xFFFFFF).toString(16).padStart(6, '0')

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

    /**
     * Converts editor HTML into a Compose [AnnotatedString] carrying the same bold/italic/
     * underline/strikethrough/color/size spans as the editor. Reuses [htmlToSpannable] rather
     * than re-parsing HTML directly, so note previews (list rows, grid cards) render the exact
     * same formatting as the editor instead of falling back to plain text.
     */
    fun htmlToAnnotatedString(html: String): AnnotatedString {
        val spannable = htmlToSpannable(html)
        return buildAnnotatedString {
            append(spannable.toString())
            spannable.getSpans(0, spannable.length, Any::class.java).forEach { span ->
                val start = spannable.getSpanStart(span)
                val end = spannable.getSpanEnd(span)
                if (start < 0 || end < 0 || start >= end) return@forEach
                val style = when (span) {
                    is StyleSpan -> when (span.style) {
                        Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                        Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                        Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                        else -> null
                    }
                    is UnderlineSpan -> SpanStyle(textDecoration = TextDecoration.Underline)
                    is StrikethroughSpan -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    is ForegroundColorSpan -> SpanStyle(color = Color(span.foregroundColor))
                    is BackgroundColorSpan -> SpanStyle(background = Color(span.backgroundColor))
                    is RelativeSizeSpan -> SpanStyle(fontSize = (14f * span.sizeChange).sp)
                    is URLSpan -> SpanStyle(color = Color(0xFF1259C3), textDecoration = TextDecoration.Underline)
                    else -> null
                }
                if (style != null) addStyle(style, start, end)
            }
        }
    }
}
