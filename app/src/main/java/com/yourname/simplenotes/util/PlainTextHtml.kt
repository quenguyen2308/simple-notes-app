package com.yourname.simplenotes.util

/**
 * Converts plain text (from a share intent or an imported file) into the minimal HTML
 * the Compose Rich Editor's HTML parser expects — entities escaped, newlines as `<br>`.
 */
fun String.toEditorHtml(): String =
    split("\n").joinToString("<br>") { android.text.Html.escapeHtml(it) }
