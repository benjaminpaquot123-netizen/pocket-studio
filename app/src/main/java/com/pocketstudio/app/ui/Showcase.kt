package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.Neon

/*
 * The finish of the screens seen first (the launch chooser, the welcome, « Nouveau projet », the
 * language list): the same graphite panel lit from above as NeonCard, for cards that are buttons.
 * Static brushes, built when composed: nothing is drawn again while music plays. Nothing here
 * changes a size or a place: shadows and edges are drawn, never laid out.
 */

/**
 * A card that is a button: a graphite panel (a light gradient from the top), a soft shadow and an
 * edge lighter at the top, never under the outline's contrast (DESIGN.md: borders at 3:1).
 * [accent] tints it (the main choice): an accent wash from the top corner, an accent edge [edge] wide.
 */
@Composable
fun Modifier.showcasePanel(shape: Shape, accent: Color? = null, edge: Dp = 1.dp, wash: Float = 0.22f): Modifier {
    val surface = Neon.Surface
    val outline = Neon.Outline
    val base = if (accent == null) {
        Brush.verticalGradient(listOf(lerp(surface, Color.White, 0.045f), surface, lerp(surface, Color.Black, 0.06f)))
    } else {
        // The tint over the screen's background, as before, a little deeper at the far corner.
        Brush.linearGradient(listOf(accent.copy(alpha = wash + 0.05f), accent.copy(alpha = wash - 0.07f)))
    }
    val rim = accent ?: outline
    val border = Brush.verticalGradient(listOf(lerp(rim, Color.White, 0.3f), rim, rim))
    return this
        // A dark shadow, never a coloured halo (halos are for what plays, DESIGN.md).
        .shadow(if (accent != null) 6.dp else 4.dp, shape)
        .clip(shape)
        // The tint lies on the screen's own colour, opaque: the shadow never shows through it.
        .then(if (accent != null) Modifier.background(Neon.Background) else Modifier)
        .background(base)
        // A sheen on the upper part: the light comes from above.
        .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.05f), 0.45f to Color.Transparent))
        .border(edge, border, shape)
}

/**
 * A choice's round icon: [filled] a disc of [color] lit from above (the main choice), otherwise a
 * soft tint of it with a thin ring; the icon keeps its colour and size.
 */
@Composable
fun IconMedallion(icon: ImageVector, color: Color, size: Dp, iconSize: Dp, filled: Boolean = false, shape: Shape = CircleShape) {
    val fill = if (filled) {
        Brush.verticalGradient(listOf(lerp(color, Color.White, 0.22f), color, lerp(color, Color.Black, 0.1f)))
    } else {
        Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.13f)))
    }
    val ring = if (filled) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0f)))
    } else {
        Brush.verticalGradient(listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.12f)))
    }
    Box(
        Modifier.size(size)
            .then(if (filled) Modifier.shadow(4.dp, shape, ambientColor = color, spotColor = color) else Modifier)
            .clip(shape).background(fill).border(1.dp, ring, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Neon.OnAccent else color, modifier = Modifier.size(iconSize))
    }
}
