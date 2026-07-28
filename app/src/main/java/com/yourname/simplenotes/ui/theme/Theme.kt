package com.yourname.simplenotes.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary          = DeskCoral,
    onPrimary        = DeskOnAccent,
    secondary        = DeskAmber,
    onSecondary      = DeskOnAccent,
    tertiary         = DeskSage,
    onTertiary       = DeskOnAccent,
    background       = DeskWalnut,
    onBackground     = DeskParchment,
    surface          = DeskWalnutSurface,
    onSurface        = DeskParchment,
    surfaceVariant   = DeskWalnutSurface2,
    onSurfaceVariant = DeskParchmentMuted,
    outline          = DeskParchmentMuted,
    outlineVariant   = DeskWalnutSurface2
)

private val LightColorScheme = lightColorScheme(
    primary          = DeskCoral,
    onPrimary        = DeskOnAccent,
    secondary        = DeskAmber,
    onSecondary      = DeskOnAccent,
    tertiary         = DeskSage,
    onTertiary       = DeskOnAccent,
    background       = DeskOak,
    onBackground     = DeskInk,
    surface          = DeskOakSurface,
    onSurface        = DeskInk,
    surfaceVariant   = DeskOakSurface2,
    onSurfaceVariant = DeskInkMuted,
    outline          = DeskInkMuted,
    outlineVariant   = DeskOakSurface2
)

private val SamsungDarkColorScheme = darkColorScheme(
    primary          = SamsungBlueDark,
    onPrimary        = Color(0xFF00305E),
    secondary        = SamsungInkMuted,
    onSecondary      = SamsungBgDark,
    tertiary         = SamsungGreenDark,
    onTertiary       = Color(0xFF00390F),
    background       = SamsungBgDark,
    onBackground     = SamsungParchment,
    surface          = SamsungSurfaceDark,
    onSurface        = SamsungParchment,
    surfaceVariant   = SamsungSurfaceDark2,
    onSurfaceVariant = SamsungParchmentMuted,
    outline          = SamsungParchmentMuted,
    outlineVariant   = SamsungSurfaceDark2
)

private val SamsungLightColorScheme = lightColorScheme(
    primary          = SamsungBlue,
    onPrimary        = Color.White,
    secondary        = SamsungInkMuted,
    onSecondary      = Color.White,
    tertiary         = SamsungGreen,
    onTertiary       = Color.White,
    background       = Color.White,
    onBackground     = SamsungInk,
    surface          = SamsungSurfaceLight,
    onSurface        = SamsungInk,
    surfaceVariant   = SamsungSurfaceLight2,
    onSurfaceVariant = SamsungInkMuted,
    outline          = SamsungInkMuted,
    outlineVariant   = SamsungSurfaceLight2
)

private val EasyDarkColorScheme = darkColorScheme(
    primary          = EasyCoralDark,
    onPrimary        = Color(0xFF4A0E22),
    secondary        = EasyMintDark,
    onSecondary      = Color(0xFF04341F),
    tertiary         = EasyAmberDark,
    onTertiary       = EasyOnAccentLight,
    background       = EasyWalnut,
    onBackground     = EasyParchment,
    surface          = EasyWalnutSurface,
    onSurface        = EasyParchment,
    surfaceVariant   = EasyWalnutSurface2,
    onSurfaceVariant = EasyParchmentMuted,
    outline          = EasyParchmentMuted,
    outlineVariant   = EasyWalnutSurface2
)

private val EasyLightColorScheme = lightColorScheme(
    primary          = EasyCoral,
    onPrimary        = Color.White,
    secondary        = EasyMint,
    onSecondary      = Color.White,
    tertiary         = EasyAmber,
    onTertiary       = EasyOnAccentLight,
    background       = EasyCream,
    onBackground     = EasyInk,
    surface          = EasyCreamSurface,
    onSurface        = EasyInk,
    surfaceVariant   = EasyCreamSurface2,
    onSurfaceVariant = EasyInkMuted,
    outline          = EasyInkMuted,
    outlineVariant   = EasyCreamSurface2
)

private val IndigoNightDarkColorScheme = darkColorScheme(
    primary          = IndigoPrimaryDark,
    onPrimary        = Color.White,
    secondary        = IndigoSecondaryDark,
    onSecondary      = Color(0xFF00332F),
    tertiary         = IndigoTertiaryDark,
    onTertiary       = Color(0xFF3A2C00),
    background       = IndigoGradientMidDark,
    onBackground     = IndigoInkDark,
    surface          = IndigoSurfaceDark,
    onSurface        = IndigoInkDark,
    surfaceVariant   = IndigoSurfaceDark2,
    onSurfaceVariant = IndigoInkMutedDark,
    outline          = IndigoInkMutedDark,
    outlineVariant   = IndigoSurfaceDark2
)

private val IndigoNightLightColorScheme = lightColorScheme(
    primary          = IndigoPrimaryLight,
    onPrimary        = Color.White,
    secondary        = IndigoSecondaryLight,
    onSecondary      = Color.White,
    tertiary         = IndigoTertiaryLight,
    onTertiary       = Color.White,
    background       = IndigoGradientTopLight,
    onBackground     = IndigoInkLight,
    surface          = IndigoSurfaceLight,
    onSurface        = IndigoInkLight,
    surfaceVariant   = IndigoSurfaceLight2,
    onSurfaceVariant = IndigoInkMutedLight,
    outline          = IndigoInkMutedLight,
    outlineVariant   = IndigoSurfaceLight2
)

private val LavenderMistDarkColorScheme = darkColorScheme(
    primary          = LavenderPrimaryDark,
    onPrimary        = Color(0xFF2A1B69),
    secondary        = LavenderSecondaryDark,
    onSecondary      = Color(0xFF3A2200),
    tertiary         = LavenderTertiaryDark,
    onTertiary       = Color(0xFF00304A),
    background       = LavenderGradientMidDark,
    onBackground     = LavenderInkDark,
    surface          = LavenderSurfaceDark,
    onSurface        = LavenderInkDark,
    surfaceVariant   = LavenderSurfaceDark2,
    onSurfaceVariant = LavenderInkMutedDark,
    outline          = LavenderInkMutedDark,
    outlineVariant   = LavenderSurfaceDark2
)

private val LavenderMistLightColorScheme = lightColorScheme(
    primary          = LavenderPrimaryLight,
    onPrimary        = Color.White,
    secondary        = LavenderSecondaryLight,
    onSecondary      = Color.White,
    tertiary         = LavenderTertiaryLight,
    onTertiary       = Color.White,
    background       = LavenderGradientTopLight,
    onBackground     = LavenderInkLight,
    surface          = LavenderSurfaceLight,
    onSurface        = LavenderInkLight,
    surfaceVariant   = LavenderSurfaceLight2,
    onSurfaceVariant = LavenderInkMutedLight,
    outline          = LavenderInkMutedLight,
    outlineVariant   = LavenderSurfaceLight2
)

private val MintSunriseDarkColorScheme = darkColorScheme(
    primary          = MintPrimaryDark,
    onPrimary        = Color(0xFF00341F),
    secondary        = MintSecondaryDark,
    onSecondary      = Color(0xFF3A1400),
    tertiary         = MintTertiaryDark,
    onTertiary       = Color(0xFF3A2C00),
    background       = MintGradientMidDark,
    onBackground     = MintInkDark,
    surface          = MintSurfaceDark,
    onSurface        = MintInkDark,
    surfaceVariant   = MintSurfaceDark2,
    onSurfaceVariant = MintInkMutedDark,
    outline          = MintInkMutedDark,
    outlineVariant   = MintSurfaceDark2
)

private val MintSunriseLightColorScheme = lightColorScheme(
    primary          = MintPrimaryLight,
    onPrimary        = Color.White,
    secondary        = MintSecondaryLight,
    onSecondary      = Color.White,
    tertiary         = MintTertiaryLight,
    onTertiary       = Color.White,
    background       = MintGradientTopLight,
    onBackground     = MintInkLight,
    surface          = MintSurfaceLight,
    onSurface        = MintInkLight,
    surfaceVariant   = MintSurfaceLight2,
    onSurfaceVariant = MintInkMutedLight,
    outline          = MintInkMutedLight,
    outlineVariant   = MintSurfaceLight2
)

private val AuroraDarkColorScheme = darkColorScheme(
    primary          = AuroraPrimaryDark,
    onPrimary        = Color(0xFF00332F),
    secondary        = AuroraSecondaryDark,
    onSecondary      = Color(0xFF3A0A20),
    tertiary         = AuroraTertiaryDark,
    onTertiary       = Color(0xFF2A1B69),
    background       = AuroraGradientMidDark,
    onBackground     = AuroraInkDark,
    surface          = AuroraSurfaceDark,
    onSurface        = AuroraInkDark,
    surfaceVariant   = AuroraSurfaceDark2,
    onSurfaceVariant = AuroraInkMutedDark,
    outline          = AuroraInkMutedDark,
    outlineVariant   = AuroraSurfaceDark2
)

private val AuroraLightColorScheme = lightColorScheme(
    primary          = AuroraPrimaryLight,
    onPrimary        = Color.White,
    secondary        = AuroraSecondaryLight,
    onSecondary      = Color.White,
    tertiary         = AuroraTertiaryLight,
    onTertiary       = Color.White,
    background       = AuroraSurfaceLight2,
    onBackground     = AuroraInkLight,
    surface          = AuroraSurfaceLight,
    onSurface        = AuroraInkLight,
    surfaceVariant   = AuroraSurfaceLight2,
    onSurfaceVariant = AuroraInkMutedLight,
    outline          = AuroraInkMutedLight,
    outlineVariant   = AuroraSurfaceLight2
)

private val NotezDarkColorScheme = darkColorScheme(
    primary          = NotezPurpleDark,
    onPrimary        = Color(0xFF2A1B69),
    secondary        = NotezPinkDark,
    onSecondary      = Color(0xFF4A0E22),
    tertiary         = NotezMintDark,
    onTertiary       = Color(0xFF04341F),
    background       = NotezGradientMidDark,
    onBackground     = NotezParchmentDark,
    surface          = NotezSurfaceDark,
    onSurface        = NotezParchmentDark,
    surfaceVariant   = NotezSurfaceDark2,
    onSurfaceVariant = NotezParchmentMutedDark,
    outline          = NotezParchmentMutedDark,
    outlineVariant   = NotezSurfaceDark2
)

private val NotezLightColorScheme = lightColorScheme(
    primary          = NotezPurpleLight,
    onPrimary        = Color.White,
    secondary        = NotezPinkLight,
    onSecondary      = Color.White,
    tertiary         = NotezMintLight,
    onTertiary       = Color.White,
    background       = NotezGradientMidLight,
    onBackground     = NotezInk,
    surface          = NotezSurfaceLight,
    onSurface        = NotezInk,
    surfaceVariant   = NotezSurfaceLight2,
    onSurfaceVariant = NotezInkMuted,
    outline          = NotezInkMuted,
    outlineVariant   = NotezSurfaceLight2
)

/** Material You dynamic color is only available on Android 12+ (API 31). */
val isDynamicColorAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Gradient backdrop for the 4 "NoteSphere"-style themes — null for the flat/solid themes
 *  (Default, Samsung Notes, EasyNotes) and whenever Material You dynamic color is active.
 *  Read via [Modifier.appBackground] at each screen's root instead of a flat surface color. */
private val LocalAppBackgroundBrush = staticCompositionLocalOf<Brush?> { null }

/** Root-level background for a screen: the active theme's gradient brush when one is set,
 *  falling back to the flat `MaterialTheme.colorScheme.background` otherwise — so this is a
 *  drop-in replacement for `.background(MaterialTheme.colorScheme.background)` everywhere. */
@Composable
fun Modifier.appBackground(): Modifier {
    val brush = LocalAppBackgroundBrush.current
    return if (brush != null) background(brush) else background(MaterialTheme.colorScheme.background)
}

@Composable
fun SimpleNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    appTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamic = dynamicColor && isDynamicColorAvailable
    val colorScheme = when {
        dynamic -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        appTheme == AppTheme.SAMSUNG       -> if (darkTheme) SamsungDarkColorScheme else SamsungLightColorScheme
        appTheme == AppTheme.EASY          -> if (darkTheme) EasyDarkColorScheme else EasyLightColorScheme
        appTheme == AppTheme.INDIGO_NIGHT  -> if (darkTheme) IndigoNightDarkColorScheme else IndigoNightLightColorScheme
        appTheme == AppTheme.LAVENDER_MIST -> if (darkTheme) LavenderMistDarkColorScheme else LavenderMistLightColorScheme
        appTheme == AppTheme.MINT_SUNRISE  -> if (darkTheme) MintSunriseDarkColorScheme else MintSunriseLightColorScheme
        appTheme == AppTheme.AURORA        -> if (darkTheme) AuroraDarkColorScheme else AuroraLightColorScheme
        appTheme == AppTheme.NOTEZ         -> if (darkTheme) NotezDarkColorScheme else NotezLightColorScheme
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }
    val backgroundBrush = if (dynamic) null else when (appTheme) {
        AppTheme.INDIGO_NIGHT -> Brush.verticalGradient(
            if (darkTheme) listOf(IndigoGradientTopDark, IndigoGradientMidDark, IndigoGradientBottomDark)
            else listOf(IndigoGradientTopLight, IndigoGradientMidLight, IndigoGradientBottomLight)
        )
        AppTheme.LAVENDER_MIST -> Brush.verticalGradient(
            if (darkTheme) listOf(LavenderGradientTopDark, LavenderGradientMidDark, LavenderGradientBottomDark)
            else listOf(LavenderGradientTopLight, LavenderGradientMidLight, LavenderGradientBottomLight)
        )
        AppTheme.MINT_SUNRISE -> Brush.verticalGradient(
            if (darkTheme) listOf(MintGradientTopDark, MintGradientMidDark, MintGradientBottomDark)
            else listOf(MintGradientTopLight, MintGradientMidLight, MintGradientBottomLight)
        )
        AppTheme.AURORA -> Brush.linearGradient(
            if (darkTheme) listOf(AuroraGradientTopDark, AuroraGradientMidDark, AuroraGradientBottomDark)
            else listOf(AuroraGradientTopLight, AuroraGradientMidLight, AuroraGradientBottomLight)
        )
        AppTheme.NOTEZ -> Brush.linearGradient(
            if (darkTheme) listOf(NotezGradientTopDark, NotezGradientMidDark, NotezGradientBottomDark)
            else listOf(NotezGradientTopLight, NotezGradientMidLight, NotezGradientBottomLight)
        )
        else -> null
    }

    CompositionLocalProvider(LocalAppBackgroundBrush provides backgroundBrush) {
        MaterialTheme(colorScheme = colorScheme, typography = SimpleNotesTypography, content = content)
    }
}
