package com.yourname.simplenotes.ui.notes

import androidx.compose.ui.graphics.Color

/** Darkens this color toward black by [factor] (0f..1f) — used for readable pill text/labels on pastel card backgrounds. */
fun Color.darken(factor: Float = 0.35f): Color = Color(
    red = red * (1 - factor),
    green = green * (1 - factor),
    blue = blue * (1 - factor),
    alpha = alpha
)
