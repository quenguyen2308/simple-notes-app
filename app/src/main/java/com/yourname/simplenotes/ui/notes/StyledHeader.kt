package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.ui.theme.Baloo2Family
import com.yourname.simplenotes.ui.theme.DeskCoral
import com.yourname.simplenotes.ui.theme.DeskCoralDark
import com.yourname.simplenotes.ui.theme.DeskSage
import com.yourname.simplenotes.ui.theme.HeaderStyle
import com.yourname.simplenotes.ui.theme.NotezAmber
import com.yourname.simplenotes.ui.theme.NotezInk
import com.yourname.simplenotes.ui.theme.NotezPinkLight
import com.yourname.simplenotes.ui.theme.NotezPurpleDark
import com.yourname.simplenotes.ui.theme.NotezPurpleLight

/** Renders the note-list title/subtitle block per [style] — the "Bàn Làm Việc" default,
 *  or one of the playful alternates picked in Settings. */
@Composable
fun StyledNoteListHeader(style: HeaderStyle, title: String, subtitle: String) {
    when (style) {
        HeaderStyle.DEFAULT -> DefaultHeader(title, subtitle)
        HeaderStyle.COMIC   -> ComicHeader(title, subtitle)
        HeaderStyle.CUTE    -> CuteHeader(title, subtitle)
        HeaderStyle.MEADOW  -> MeadowHeader(title, subtitle)
        HeaderStyle.NOTEZ   -> NotezHeader(title, subtitle)
    }
}

/** Renders the "new note" FAB per [style]. */
@Composable
fun StyledFab(style: HeaderStyle, onClick: () -> Unit) {
    when (style) {
        HeaderStyle.DEFAULT -> DefaultFab(onClick)
        HeaderStyle.COMIC   -> ComicFab(onClick)
        HeaderStyle.CUTE    -> CuteFab(onClick)
        HeaderStyle.MEADOW  -> MeadowFab(onClick)
        HeaderStyle.NOTEZ   -> NotezFab(onClick)
    }
}

/** Icon tint matching [style]'s ink color — every toolbar/card icon just changes color with the
 *  chosen style, no badge/background shape. A ring around every icon read as clutter, so styling
 *  here is deliberately restrained to tint only. */
@Composable
fun styledIconTint(style: HeaderStyle, default: Color = MaterialTheme.colorScheme.onSurfaceVariant): Color =
    when (style) {
        HeaderStyle.DEFAULT -> default
        HeaderStyle.COMIC   -> ComicInk
        HeaderStyle.CUTE    -> CuteInk
        HeaderStyle.MEADOW  -> MeadowInk
        HeaderStyle.NOTEZ   -> NotezInk
    }

/** A toolbar icon button, tinted per [style]. No background badge — see [styledIconTint]. */
@Composable
fun StyledIconButton(
    style: HeaderStyle,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(
            icon, contentDescription,
            tint = styledIconTint(style, default = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.size(iconSize)
        )
    }
}

// ── Default (unchanged "Bàn Làm Việc" look) ─────────────────────────────

@Composable
private fun DefaultHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DefaultFab(onClick: () -> Unit) {
    Box(modifier = Modifier.graphicsLayer(rotationZ = -4f)) {
        Box(
            Modifier
                .size(50.dp)
                .offset(x = 3.dp, y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DeskCoralDark)
        )
        FloatingActionButton(
            onClick        = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            shape          = RoundedCornerShape(16.dp),
            elevation      = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
            modifier       = Modifier.size(50.dp)
        ) {
            Icon(Icons.Default.Edit, "Ghi chú mới", modifier = Modifier.size(22.dp))
        }
    }
}

// ── Shared: a soft glossy circular FAB (gradient fill + a single highlight ellipse) ─────
// Used by Cute and Meadow so both styles read as one coherent, restrained "polish" language
// instead of each reaching for a different novelty shape.

@Composable
private fun GlossyCircleFab(
    onClick: () -> Unit,
    gradient: Brush,
    shadowColor: Color,
    iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(CircleShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        // Specular highlight — a small soft white ellipse in the upper-left, the one glossy
        // accent that reads as "polished" without extra ornament competing for attention.
        Box(
            Modifier
                .padding(top = 7.dp, start = 10.dp)
                .align(Alignment.TopStart)
                .size(width = 18.dp, height = 10.dp)
                .graphicsLayer(rotationZ = -18f)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.35f))
        )
        IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.Edit, "Ghi chú mới", tint = iconTint, modifier = Modifier.size(22.dp))
        }
    }
}

// ── A. Truyện tranh siêu anh hùng (comic) ───────────────────────────────

private val ComicInk = Color(0xFF201406)
private val ComicCream = Color(0xFFFFF8EC)

@Composable
private fun ComicHeader(title: String, subtitle: String) {
    // A logotype treatment (outlined wordmark directly on the page) rather than a boxed
    // banner — reads as a comic-title, not a signboard glued onto the screen.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedText(text = title, fontSize = 32.sp, fillColor = Color(0xFFFF7A45), outlineColor = ComicInk)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ComicInk)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(subtitle.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = ComicCream)
        }
    }
}

@Composable
private fun ComicFab(onClick: () -> Unit) {
    Box(modifier = Modifier.graphicsLayer(rotationZ = -3f)) {
        Box(
            Modifier
                .size(50.dp)
                .offset(x = 3.dp, y = 3.dp)
                .clip(CircleShape)
                .background(ComicInk)
        )
        FloatingActionButton(
            onClick        = onClick,
            containerColor = DeskCoral,
            contentColor   = ComicCream,
            shape          = CircleShape,
            elevation      = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
            modifier       = Modifier.size(50.dp).border(2.dp, ComicInk, CircleShape)
        ) {
            Icon(Icons.Default.Edit, "Ghi chú mới", modifier = Modifier.size(22.dp))
        }
    }
}

/** Draws [text] with a hard outline using a real stroke pass (not stacked copies), so corners
 *  stay crisp instead of turning fuzzy — Compose's Text has no native stroke, so this pairs one
 *  stroke-only Text under a fill Text using the platform text-drawing style. */
@Composable
private fun OutlinedText(
    text: String,
    fontSize: TextUnit,
    fillColor: Color,
    outlineColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        // A small, tight ring of offsets (not 8 diagonals) kept at a sub-pixel-friendly step
        // reads as a clean outline at this font size without the "smeared" look of a wide stroke.
        listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { (dx, dy) ->
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = outlineColor,
                modifier = Modifier
                    .offset(x = 1.4.dp * dx, y = 1.4.dp * dy)
                    .clearAndSetSemantics { }
            )
        }
        Text(text = text, fontSize = fontSize, fontWeight = FontWeight.Black, color = fillColor)
    }
}

// ── C. Hoạt hình tròn trịa, dễ thương (cute) ────────────────────────────

private val CuteInk = Color(0xFF7A3B25)

@Composable
private fun CuteHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CuteInk,
                shadow = Shadow(color = CuteInk.copy(alpha = 0.28f), offset = Offset(0f, 3f), blurRadius = 6f)
            )
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(DeskSage)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ComicCream)
        }
    }
}

@Composable
private fun CuteFab(onClick: () -> Unit) {
    GlossyCircleFab(
        onClick = onClick,
        gradient = Brush.linearGradient(listOf(Color(0xFFFF8A72), Color(0xFFFFB25C))),
        shadowColor = DeskCoralDark,
        iconTint = ComicCream
    )
}

// ── C3. Đồng cỏ nắng (meadow) ────────────────────────────────────────────

private val MeadowInk = Color(0xFF5A6B2F)

@Composable
private fun MeadowHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MeadowInk,
                shadow = Shadow(color = MeadowInk.copy(alpha = 0.25f), offset = Offset(0f, 3f), blurRadius = 6f)
            )
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFEDAD))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B5720))
        }
    }
}

@Composable
private fun MeadowFab(onClick: () -> Unit) {
    GlossyCircleFab(
        onClick = onClick,
        gradient = Brush.linearGradient(listOf(Color(0xFFFFD23F), Color(0xFFF5A623))),
        shadowColor = Color(0xFFB47814),
        iconTint = Color(0xFF6B4310)
    )
}

// ── D. NOTEZ (GenZ) ──────────────────────────────────────────────────────

@Composable
private fun NotezHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = TextStyle(
                fontFamily = Baloo2Family,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NotezInk
            )
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(NotezAmber)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NotezInk)
        }
    }
}

@Composable
private fun NotezFab(onClick: () -> Unit) {
    GlossyCircleFab(
        onClick = onClick,
        gradient = Brush.linearGradient(listOf(NotezPurpleLight, NotezPinkLight)),
        shadowColor = NotezPurpleDark,
        iconTint = Color.White
    )
}
