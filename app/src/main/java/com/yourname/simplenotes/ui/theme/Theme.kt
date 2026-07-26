package com.yourname.simplenotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
