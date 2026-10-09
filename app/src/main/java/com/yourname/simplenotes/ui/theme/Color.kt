package com.yourname.simplenotes.ui.theme

import androidx.compose.ui.graphics.Color
import com.yourname.simplenotes.ui.editor.NOTE_COLORS

// ── Glassmorphism & Bento Design System Palette ─────────────────────────
// Clean porcelain & frosted white for light mode; deep obsidian slate for dark mode.
val GlassPorcelainBg       = Color(0xFFF8FAFC) // light background — clean porcelain slate
val GlassPorcelainSurface  = Color(0xFFFFFFFF) // light surface — frosted acrylic white
val GlassPorcelainSurface2 = Color(0xFFF1F5F9) // light surface variant / elevated
val GlassSlateInk          = Color(0xFF0F172A) // text on light — slate 900
val GlassSlateMuted        = Color(0xFF64748B) // muted text on light — slate 500
val GlassBorderLight       = Color(0xFFE2E8F0) // clean glass border on light

val GlassObsidianBg        = Color(0xFF0E121A) // dark background — midnight obsidian slate
val GlassObsidianSurface   = Color(0xFF161B26) // dark surface — deep frosted acrylic
val GlassObsidianSurface2  = Color(0xFF1E2638) // dark surface variant / elevated
val GlassWhiteParchment    = Color(0xFFF8FAFC) // text on dark — crisp parchment
val GlassWhiteMuted        = Color(0xFF94A3B8) // muted text on dark — slate 400
val GlassBorderDark        = Color(0xFF242E42) // clean glass border on dark

// Radiant modern accents (Indigo, Pink, Emerald, Amber, Violet)
val GlassIndigo            = Color(0xFF6366F1) // primary accent light
val GlassIndigoLight       = Color(0xFFEEF2FF) // soft indigo container
val GlassElectricIndigo    = Color(0xFF818CF8) // primary accent dark
val GlassPink              = Color(0xFFEC4899) // vibrant gradient accent
val GlassEmerald           = Color(0xFF10B981) // success / mint accent
val GlassAmber             = Color(0xFFF59E0B) // warning / pinned accent

// ── Sakura Blossom Palette (PhotoEvents layout) ─────────────────────────
val SakuraPink             = Color(0xFFFF6584)
val SakuraPinkLight        = Color(0xFFFF8FA3)
val SakuraPinkGlow         = Color(0xFFFFB3C1)
val SakuraPinkContainer    = Color(0xFFFFE8EE)
val SakuraBlushBg          = Color(0xFFFFF8FA)
val SakuraSurface          = Color(0xFFFFFFFF)
val SakuraSurfaceVariant   = Color(0xFFFFF2F5)
val SakuraBorderSoft       = Color(0xFFFFE0E8)
val SakuraBorderSubtle     = Color(0xFFFFF0F4)
val SakuraTextPrimary      = Color(0xFF2E1A29)
val SakuraTextSecondary    = Color(0xFF7E6676)

// Sakura Blossom Dark Palette
val SakuraBlushBgDark        = Color(0xFF1B131A)
val SakuraSurfaceDark        = Color(0xFF261B24)
val SakuraSurfaceVariantDark = Color(0xFF322330)
val SakuraBorderSoftDark     = Color(0xFF4A3345)
val SakuraBorderSubtleDark   = Color(0xFF3A2536)
val SakuraTextPrimaryDark    = Color(0xFFFFF0F5)
val SakuraTextSecondaryDark  = Color(0xFFCBB4C5)
val SakuraPinkContainerDark  = Color(0xFF3F1D2B)

// ── Frosted Glass ("Mờ sương") Dialog Palette ───────────────────────
val FrostedGlassBgLight           = Color(0xF8FFFBFD) // 97% luminous frosted blush white
val FrostedGlassBgDark            = Color(0xF4201622) // 95% deep plum-slate frosted acrylic
val FrostedGlassTileLight         = Color(0xB3FFFFFF) // 70% frosted translucent white tile
val FrostedGlassTileDark          = Color(0x1AFFFFFF) // 10% frosted tile for dark mode
val FrostedGlassBorderLight       = Color(0xE6FFFFFF) // Crisp bright glass reflection edge (90% white)
val FrostedGlassBorderDark        = Color(0x2EFFFFFF) // Subtle edge for dark mode

// Backward-compatible aliases for existing references
val DeskWalnut         = GlassObsidianBg
val DeskWalnutSurface  = GlassObsidianSurface
val DeskWalnutSurface2 = GlassObsidianSurface2
val DeskParchment      = GlassWhiteParchment
val DeskParchmentMuted = GlassWhiteMuted

val DeskOak       = GlassPorcelainBg
val DeskOakSurface  = GlassPorcelainSurface
val DeskOakSurface2 = GlassPorcelainSurface2
val DeskInk       = GlassSlateInk
val DeskInkMuted  = GlassSlateMuted

val DeskCoral      = GlassIndigo
val DeskCoralDark  = Color(0xFF4F46E5)
val DeskAmber      = GlassAmber
val DeskSage       = GlassEmerald
val DeskOnAccent   = Color.White

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
