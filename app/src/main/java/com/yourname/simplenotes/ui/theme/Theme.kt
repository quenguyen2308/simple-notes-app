package com.yourname.simplenotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary              = GlassElectricIndigo,
    onPrimary            = Color(0xFF0F172A),
    primaryContainer     = Color(0xFF312E81),
    onPrimaryContainer   = GlassWhiteParchment,
    secondary            = GlassPink,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary             = GlassEmerald,
    onTertiary           = Color.White,
    background           = GlassObsidianBg,
    onBackground         = GlassWhiteParchment,
    surface              = GlassObsidianSurface,
    onSurface            = GlassWhiteParchment,
    surfaceVariant       = GlassObsidianSurface2,
    onSurfaceVariant     = GlassWhiteMuted,
    outline              = GlassWhiteMuted.copy(alpha = 0.5f),
    outlineVariant       = GlassBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary              = GlassIndigo,
    onPrimary            = Color.White,
    primaryContainer     = GlassIndigoLight,
    onPrimaryContainer   = Color(0xFF3730A3),
    secondary            = GlassPink,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF9D174D),
    tertiary             = GlassEmerald,
    onTertiary           = Color.White,
    background           = GlassPorcelainBg,
    onBackground         = GlassSlateInk,
    surface              = GlassPorcelainSurface,
    onSurface            = GlassSlateInk,
    surfaceVariant       = GlassPorcelainSurface2,
    onSurfaceVariant     = GlassSlateMuted,
    outline              = GlassSlateMuted.copy(alpha = 0.5f),
    outlineVariant       = GlassBorderLight
)

/** Material You dynamic color is only available on Android 12+ (API 31). */
val isDynamicColorAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun SimpleNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && isDynamicColorAvailable ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
