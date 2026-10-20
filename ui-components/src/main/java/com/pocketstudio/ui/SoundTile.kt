package com.pocketstudio.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A sound to choose, as the best music apps show them: an icon on its family's colour, the name,
 * a short line about it; the chosen one glows in its colour, a press sinks a little.
 */
@Composable
fun SoundTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tile-press",
    )
    val border by animateColorAsState(if (selected) accent else Neon.Outline.copy(alpha = 0.45f), label = "tile-border")
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "tile-border-width")
    val glow by animateDpAsState(if (selected) 12.dp else 0.dp, label = "tile-glow")
    val wash by animateFloatAsState(if (selected) 0.32f else 0.13f, label = "tile-wash")
    Row(
        modifier
            .scale(scale)
            .shadow(glow, shape, clip = false, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = wash), Neon.SurfaceHigh.copy(alpha = 0.6f))))
            .border(borderWidth, border, shape)
            .clickable(interactionSource = interaction, indication = ripple(color = accent), role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .heightIn(min = 58.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.22f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = Neon.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.2.sp,
                    color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) trailing()
        else if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
    }
}

/** A family to show: its icon and name; the chosen one fills with its colour. */
@Composable
fun FamilyChip(label: String, icon: ImageVector, accent: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val fill by animateColorAsState(if (selected) accent.copy(alpha = 0.28f) else Neon.SurfaceHigh.copy(alpha = 0.5f), label = "family-fill")
    val edge by animateColorAsState(if (selected) accent else Neon.Outline.copy(alpha = 0.4f), label = "family-edge")
    val text by animateColorAsState(if (selected) Neon.TextPrimary else Neon.TextSecondary, label = "family-text")
    Row(
        modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .heightIn(min = 36.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) accent else Neon.TextSecondary, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = text, maxLines = 1)
    }
}

/** A family title over its tiles: a dot of its colour and its name. */
@Composable
fun TileSection(title: String, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
    }
}

/**
 * [count] tiles in as many columns as fit (at least [minTileWidth] each), inside a scrolling
 * column: [content] draws tile [index] with the width it gets.
 */
@Composable
fun TileGrid(count: Int, modifier: Modifier = Modifier, minTileWidth: Dp = 150.dp, gap: Dp = 8.dp, content: @Composable (index: Int, modifier: Modifier) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gap) / (minTileWidth + gap)).toInt().coerceIn(1, 4)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (start in 0 until count step columns) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (i in start until start + columns) {
                        if (i < count) content(i, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
