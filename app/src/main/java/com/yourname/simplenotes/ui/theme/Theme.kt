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
    primary = SamsungBlueLight,
    secondary = SamsungBlue,
    tertiary = SamsungBlueDark
)

private val LightColorScheme = lightColorScheme(
    primary   = SamsungBlue,
    secondary = SamsungBlueDark,
    tertiary  = SamsungBlueLight,
    background = androidx.compose.ui.graphics.Color.White,
    surface    = androidx.compose.ui.graphics.Color.White
)

@Composable
fun SimpleNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, content = content)
}
