package com.yourname.simplenotes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.yourname.simplenotes.R

/** Rounded body/UI font — bundled as a single variable-font file (Quicksand's default instance),
 *  so heavier requested weights (Bold, SemiBold, …) render via Android's normal faux-bold rather
 *  than a true variable-weight axis — the pinned Compose BOM here predates Compose's
 *  FontVariation API, so this is the low-risk path instead of bumping that dependency. */
val QuicksandFamily = FontFamily(Font(R.font.quicksand_variable, FontWeight.Normal))

/** Bold rounded display font for the "NOTEZ" header style — used explicitly via TextStyle at
 *  specific call sites (see StyledHeader.kt's NotezHeader), never wired into [SimpleNotesTypography]
 *  globally, so screens that haven't been redesigned yet are unaffected. */
val Baloo2Family = FontFamily(Font(R.font.baloo2_variable, FontWeight.Normal))

/** Material3's default type scale with every slot's typeface swapped to [QuicksandFamily] —
 *  sizes/weights/line-heights are untouched, only the font changes, so this is safe to apply
 *  globally across every screen and every [AppTheme] immediately. */
val SimpleNotesTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge   = base.displayLarge.copy(fontFamily = QuicksandFamily),
        displayMedium  = base.displayMedium.copy(fontFamily = QuicksandFamily),
        displaySmall   = base.displaySmall.copy(fontFamily = QuicksandFamily),
        headlineLarge  = base.headlineLarge.copy(fontFamily = QuicksandFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = QuicksandFamily),
        headlineSmall  = base.headlineSmall.copy(fontFamily = QuicksandFamily),
        titleLarge     = base.titleLarge.copy(fontFamily = QuicksandFamily),
        titleMedium    = base.titleMedium.copy(fontFamily = QuicksandFamily),
        titleSmall     = base.titleSmall.copy(fontFamily = QuicksandFamily),
        bodyLarge      = base.bodyLarge.copy(fontFamily = QuicksandFamily),
        bodyMedium     = base.bodyMedium.copy(fontFamily = QuicksandFamily),
        bodySmall      = base.bodySmall.copy(fontFamily = QuicksandFamily),
        labelLarge     = base.labelLarge.copy(fontFamily = QuicksandFamily),
        labelMedium    = base.labelMedium.copy(fontFamily = QuicksandFamily),
        labelSmall     = base.labelSmall.copy(fontFamily = QuicksandFamily)
    )
}
