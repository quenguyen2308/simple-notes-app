package com.yourname.simplenotes.ui.theme

import androidx.compose.ui.graphics.Color

// "Bàn Làm Việc" (desk) palette — a corkboard-of-colored-notes identity, dark walnut desk at
// night, light oak/kraft desk by day. Same accent personality (coral/amber/sage) in both.
val DeskWalnut         = Color(0xFF2B2420) // dark background — the desk at night
val DeskWalnutSurface  = Color(0xFF3A3129) // dark elevated surface (search bar, sheets)
val DeskWalnutSurface2 = Color(0xFF453B31) // dark surfaceVariant (uncolored note cards)
val DeskParchment      = Color(0xFFF3ECDF) // text on dark
val DeskParchmentMuted = Color(0xFFC9BBA4) // muted text on dark

val DeskOak       = Color(0xFFF1E7D6) // light background — the desk by day
val DeskOakSurface  = Color(0xFFFFFDF8) // light elevated surface
val DeskOakSurface2 = Color(0xFFEDE1CB) // light surfaceVariant (uncolored note cards)
val DeskInk       = Color(0xFF2B2420) // text on light (mirrors the dark bg — same identity)
val DeskInkMuted  = Color(0xFF7A6F5E) // muted text on light

val DeskCoral      = Color(0xFFFF6F59) // primary accent — FAB, active states
val DeskCoralDark  = Color(0xFFC2543F) // pressed/shadow tone for the coral accent
val DeskAmber      = Color(0xFFE8A93A) // secondary accent — pinned/highlight
val DeskSage       = Color(0xFF4FA47C) // tertiary accent
val DeskOnAccent   = Color(0xFF2A1B14) // dark ink used as text/icon color on top of the accents
                                        // above — they're all light/mid hues in both themes.

/** Shared color options for folders — used by both the create and edit folder dialogs. */
val FOLDER_COLOR_PALETTE: List<Int> = listOf(
    // Blues
    0xFF1976D2.toInt(), 0xFF1565C0.toInt(), 0xFF0288D1.toInt(), 0xFF0097A7.toInt(),
    // Greens
    0xFF388E3C.toInt(), 0xFF2E7D32.toInt(), 0xFF558B2F.toInt(), 0xFF00897B.toInt(),
    // Reds / Pinks
    0xFFD32F2F.toInt(), 0xFFC62828.toInt(), 0xFFE91E63.toInt(), 0xFFAD1457.toInt(),
    // Purples
    0xFF7B1FA2.toInt(), 0xFF6A1B9A.toInt(), 0xFF4527A0.toInt(), 0xFF283593.toInt(),
    // Oranges / Yellows
    0xFFF57C00.toInt(), 0xFFE65100.toInt(), 0xFFF9A825.toInt(), 0xFFF57F17.toInt(),
    // Browns / Greys
    0xFF5D4037.toInt(), 0xFF4E342E.toInt(), 0xFF546E7A.toInt(), 0xFF37474F.toInt()
)
