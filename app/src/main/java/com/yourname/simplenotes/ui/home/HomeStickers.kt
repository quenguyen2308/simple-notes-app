package com.yourname.simplenotes.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.ui.theme.NotezAmber

/** Small tinted Material-icon "sticker" accent for the NOTEZ Home greeting — approximates the
 *  mockup's hand-drawn sparkle doodle using an existing bundled icon vector instead of a new
 *  image asset, same "one restrained accent" discipline as GlossyCircleFab's specular highlight
 *  in StyledHeader.kt. */
@Composable
fun HomeSparkleAccent(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.AutoAwesome,
        contentDescription = null,
        tint = NotezAmber.copy(alpha = 0.9f),
        modifier = modifier.size(18.dp).graphicsLayer(rotationZ = -12f)
    )
}
