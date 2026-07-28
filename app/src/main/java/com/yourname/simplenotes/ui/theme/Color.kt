package com.yourname.simplenotes.ui.theme

import androidx.compose.ui.graphics.Color
import com.yourname.simplenotes.ui.editor.NOTE_COLORS

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

// "Samsung Notes" (One UI) palette — flat white/near-black surfaces, a single Samsung-blue accent.
val SamsungBlue        = Color(0xFF1874E1) // primary accent — light mode
val SamsungBlueDark     = Color(0xFF5B9BF3) // primary accent, lightened for contrast on dark bg
val SamsungGreen        = Color(0xFF34A853) // tertiary accent — pin/highlight, light mode
val SamsungGreenDark    = Color(0xFF6FCF8E) // tertiary accent, dark mode
val SamsungInk          = Color(0xFF1C1C1E) // text on light background
val SamsungInkMuted     = Color(0xFF6B6F76) // muted text on light background
val SamsungSurfaceLight = Color(0xFFF5F6F7) // light elevated surface (search bar, cards)
val SamsungSurfaceLight2 = Color(0xFFF1F2F4) // light surfaceVariant (chips, uncolored cards)
val SamsungBgDark       = Color(0xFF1C1C1E) // dark background
val SamsungSurfaceDark  = Color(0xFF242426) // dark elevated surface
val SamsungSurfaceDark2 = Color(0xFF2E2E30) // dark surfaceVariant
val SamsungParchment    = Color(0xFFF2F2F2) // text on dark background
val SamsungParchmentMuted = Color(0xFFB8BABE) // muted text on dark background

// "EasyNotes" palette — warm pastel identity, coral/mint/amber accents on cream paper.
val EasyCoral       = Color(0xFFFF6F91) // primary accent — light mode
val EasyCoralDark   = Color(0xFFFF9BB0) // primary accent, lightened for dark mode
val EasyMint        = Color(0xFF4FA47C) // secondary accent — light mode
val EasyMintDark    = Color(0xFF8FD1AE) // secondary accent, dark mode
val EasyAmber       = Color(0xFFE8A93A) // tertiary accent — light mode
val EasyAmberDark   = Color(0xFFF4C874) // tertiary accent, dark mode
val EasyOnAccentLight = Color(0xFF3A2200) // dark ink for text/icons on top of the light accents above
val EasyCream        = Color(0xFFFFFAF3) // light background — cream paper
val EasyCreamSurface = Color(0xFFFFFFFF) // light elevated surface
val EasyCreamSurface2 = Color(0xFFFFF2E0) // light surfaceVariant (uncolored note cards)
val EasyInk          = Color(0xFF3A2F28) // text on light background
val EasyInkMuted     = Color(0xFF6B5D53) // muted text on light background
val EasyWalnut       = Color(0xFF2B2118) // dark background — warm brown
val EasyWalnutSurface = Color(0xFF352A1F) // dark elevated surface
val EasyWalnutSurface2 = Color(0xFF3E3226) // dark surfaceVariant
val EasyParchment    = Color(0xFFF3ECDF) // text on dark background
val EasyParchmentMuted = Color(0xFFC9BBA4) // muted text on dark background

// "Đêm Indigo" (Indigo Night) — deep violet-to-navy gradient backdrop, cool teal accent.
val IndigoGradientTopDark    = Color(0xFF2D1B69)
val IndigoGradientMidDark    = Color(0xFF1A1A3E)
val IndigoGradientBottomDark = Color(0xFF14142B)
val IndigoGradientTopLight    = Color(0xFFEDE9FF)
val IndigoGradientMidLight    = Color(0xFFDCD4FF)
val IndigoGradientBottomLight = Color(0xFFC7BFFF)
val IndigoPrimaryDark    = Color(0xFF7C6FE0) // violet accent — FAB, active tab
val IndigoPrimaryLight   = Color(0xFF6C5CE0)
val IndigoSecondaryDark  = Color(0xFF4FD1C5) // teal accent — search/highlight
val IndigoSecondaryLight = Color(0xFF00A99D)
val IndigoTertiaryDark   = Color(0xFFF2C94C) // amber accent — pinned
val IndigoTertiaryLight  = Color(0xFFE0A72E)
val IndigoSurfaceDark    = Color(0xFF262755) // "glass" card floating over the dark gradient
val IndigoSurfaceDark2   = Color(0xFF32335E) // surfaceVariant / inactive chips
val IndigoInkDark        = Color(0xFFEDEBFF) // text on dark
val IndigoInkMutedDark   = Color(0xFFC9C6E8)
val IndigoSurfaceLight   = Color(0xFFFFFFFF) // "glass" card floating over the light gradient
val IndigoSurfaceLight2  = Color(0xFFEDE7FF)
val IndigoInkLight       = Color(0xFF2A1B69) // text on light
val IndigoInkMutedLight  = Color(0xFF6E639B)

// "Sương Lavender" (Lavender Mist) — airy pastel lilac-to-white, apricot/sky-blue tag accents.
val LavenderGradientTopLight    = Color(0xFFF3EEFF)
val LavenderGradientMidLight    = Color(0xFFFAF7FF)
val LavenderGradientBottomLight = Color(0xFFFFFFFF)
val LavenderGradientTopDark    = Color(0xFF2A2438)
val LavenderGradientMidDark    = Color(0xFF352E48)
val LavenderGradientBottomDark = Color(0xFF241F30)
val LavenderPrimaryLight   = Color(0xFF9B87F5) // soft violet accent
val LavenderPrimaryDark    = Color(0xFFB9A6FF)
val LavenderSecondaryLight = Color(0xFFF2A65A) // apricot accent — like the "Groceries" tag
val LavenderSecondaryDark  = Color(0xFFF5B87C)
val LavenderTertiaryLight  = Color(0xFF6FB7E8) // sky-blue accent — like the "Daily" tag
val LavenderTertiaryDark   = Color(0xFF8FCBEE)
val LavenderSurfaceLight   = Color(0xFFFFFFFF)
val LavenderSurfaceLight2  = Color(0xFFEDE7FB)
val LavenderInkLight       = Color(0xFF3A3358)
val LavenderInkMutedLight  = Color(0xFF6E6591)
val LavenderSurfaceDark    = Color(0xFF342C46)
val LavenderSurfaceDark2   = Color(0xFF433A59)
val LavenderInkDark        = Color(0xFFEDE7FB)
val LavenderInkMutedDark   = Color(0xFFB8AFD6)

// "Bình Minh Bạc Hà" (Mint Sunrise) — mint-to-peach gradient, teal-green/coral accents.
val MintGradientTopLight    = Color(0xFFDFF6E8)
val MintGradientMidLight    = Color(0xFFFDF3D9)
val MintGradientBottomLight = Color(0xFFFCE3D0)
val MintGradientTopDark    = Color(0xFF16302A)
val MintGradientMidDark    = Color(0xFF2B2A22)
val MintGradientBottomDark = Color(0xFF35241C)
val MintPrimaryLight   = Color(0xFF3FAE7A) // teal-green accent — checked checkbox
val MintPrimaryDark    = Color(0xFF6FD3A6)
val MintSecondaryLight = Color(0xFFF2916B) // coral/peach accent
val MintSecondaryDark  = Color(0xFFF5AE8C)
val MintTertiaryLight  = Color(0xFFEBC15C) // golden accent
val MintTertiaryDark   = Color(0xFFF0D479)
val MintSurfaceLight   = Color(0xFFFFFFFF)
val MintSurfaceLight2  = Color(0xFFE3F3E8)
val MintInkLight       = Color(0xFF2E3B33)
val MintInkMutedLight  = Color(0xFF5E7268)
val MintSurfaceDark    = Color(0xFF23352E)
val MintSurfaceDark2   = Color(0xFF2C3F37)
val MintInkDark        = Color(0xFFE7F3EA)
val MintInkMutedDark   = Color(0xFFAEC7BB)

// "Cực Quang" (Aurora) — vivid teal-lavender-magenta multi-hue gradient, like a folder/tag sidebar.
val AuroraGradientTopLight    = Color(0xFF5CE1D6)
val AuroraGradientMidLight    = Color(0xFFB6A6F0)
val AuroraGradientBottomLight = Color(0xFFF48FB1)
val AuroraGradientTopDark    = Color(0xFF102B2E)
val AuroraGradientMidDark    = Color(0xFF241B3D)
val AuroraGradientBottomDark = Color(0xFF3A1830)
val AuroraPrimaryLight   = Color(0xFF19B3AA) // vivid teal accent
val AuroraPrimaryDark    = Color(0xFF4FD9CE)
val AuroraSecondaryLight = Color(0xFFE0568B) // magenta/pink accent
val AuroraSecondaryDark  = Color(0xFFF08FB8)
val AuroraTertiaryLight  = Color(0xFF8B6FE0) // purple accent
val AuroraTertiaryDark   = Color(0xFFB39CF2)
val AuroraSurfaceLight   = Color(0xFFFFFFFF)
val AuroraSurfaceLight2  = Color(0xFFEFE8FA)
val AuroraInkLight       = Color(0xFF2B2340)
val AuroraInkMutedLight  = Color(0xFF6E6485)
val AuroraSurfaceDark    = Color(0xFF2A2440)
val AuroraSurfaceDark2   = Color(0xFF362E4F)
val AuroraInkDark        = Color(0xFFF1EAFA)
val AuroraInkMutedDark   = Color(0xFFC6BBDF)

// "NOTEZ" (GenZ) — playful pastel identity: cream-blush-lavender gradient backdrop,
// violet/pink/mint accents. Matches the "simple. cute. powerful." reference mockup.
val NotezGradientTopLight    = Color(0xFFFDEAE7) // blush pink blob
val NotezGradientMidLight    = Color(0xFFFDF6EC) // warm cream center
val NotezGradientBottomLight = Color(0xFFE9E3FB) // lavender blob
val NotezGradientTopDark    = Color(0xFF2E2140)
val NotezGradientMidDark    = Color(0xFF211A2B)
val NotezGradientBottomDark = Color(0xFF1B2436)
val NotezPurpleLight   = Color(0xFF7C6AE8) // primary accent — FAB, active tab, kanban tags
val NotezPurpleDark    = Color(0xFFB6A8FF)
val NotezPinkLight     = Color(0xFFFF6F91) // secondary accent — pin/heart/highlight
val NotezPinkDark      = Color(0xFFFF9BB5)
val NotezMintLight     = Color(0xFF3FC7A0) // tertiary accent — checklist/success
val NotezMintDark      = Color(0xFF7EE0BF)
val NotezAmber         = Color(0xFFFFC94D) // extra decorative accent — highlighter/star/badge
val NotezSurfaceLight  = Color(0xFFFFFFFF) // white note/editor cards
val NotezSurfaceLight2 = Color(0xFFFBF1E4) // warm cream chip / uncolored card
val NotezInk           = Color(0xFF241B2E) // near-black text — matches the logo's marker ink
val NotezInkMuted      = Color(0xFF7C7189)
val NotezSurfaceDark   = Color(0xFF2B2236)
val NotezSurfaceDark2  = Color(0xFF352B42)
val NotezParchmentDark      = Color(0xFFF3ECFB)
val NotezParchmentMutedDark = Color(0xFFC9BEDD)

/** Shared color options for folders — used by both the create and edit folder dialogs.
 *  Includes the saturated folder tones plus the note background palette ([NOTE_COLORS]),
 *  so a folder can be tinted with the same soft pastel used for note cards. */
val FOLDER_COLOR_PALETTE: List<Int> = (listOf(
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
) + NOTE_COLORS).distinct()
