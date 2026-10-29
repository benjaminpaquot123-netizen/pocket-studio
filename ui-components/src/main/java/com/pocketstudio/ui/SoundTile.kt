package com.pocketstudio.ui

import com.pocketstudio.model.i18n.upperShown
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    /** A short word in a pill after the name (« À moi »: a sound the user kept), or none. */
    badge: String? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val border by animateColorAsState(if (selected) accent else Neon.Outline.copy(alpha = 0.45f), label = "tile-border")
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "tile-border-width")
    val glow by animateDpAsState(if (selected) 12.dp else 0.dp, label = "tile-glow")
    val wash by animateFloatAsState(if (selected) 0.32f else 0.13f, label = "tile-wash")
    Row(
        modifier
            .pressScale(interaction)
            .shadow(glow, shape, clip = false, ambientColor = accent, spotColor = accent)
            .clip(shape)
            // Opaque under the colour wash, or the glow behind would show through.
            .background(Neon.Surface)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = wash), Neon.SurfaceHigh.copy(alpha = 0.6f))))
            .border(borderWidth, border, shape)
            .clickable(interactionSource = interaction, indication = ripple(color = accent), role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .heightIn(min = 58.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.22f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    // Two lines rather than a cut name ("Distorsion de phase").
                    title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp,
                    color = Neon.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badge != null) {
                    Text(
                        badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(accent.copy(alpha = 0.18f))
                            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
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
    // Each family keeps its colour (a tint at rest, so they tell apart at a glance); the chosen
    // one is filled with it, dark ink on top, like every other chosen chip of the app.
    val fill by animateColorAsState(if (selected) accent else lerp(Neon.SurfaceHigh, accent, 0.12f).copy(alpha = 0.7f), label = "family-fill")
    val edge by animateColorAsState(if (selected) accent else accent.copy(alpha = 0.35f), label = "family-edge")
    val text by animateColorAsState(if (selected) Neon.OnAccent else Neon.TextPrimary.copy(alpha = 0.85f), label = "family-text")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .pressScale(interaction)
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .heightIn(min = 36.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Neon.OnAccent else accent, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = text, maxLines = 1, softWrap = false)
    }
}

/** Chips on one line that scrolls sideways; the chosen one ([selected]) is brought into view. */
@Composable
fun ChipRow(count: Int, selected: Int, modifier: Modifier = Modifier, item: @Composable (index: Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(selected) {
        if (selected in 0 until count) state.animateScrollToItem((selected - 1).coerceAtLeast(0))
    }
    LazyRow(modifier.fillMaxWidth().fadingEdges(state), state = state, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(count) { item(it) }
    }
}

/** A family title over its tiles: a dot of its colour and its name. */
@Composable
fun TileSection(title: String, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Text(title.upperShown(), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
    }
}

/**
 * [count] tiles in as many columns as fit (at least [minTileWidth] each), inside a scrolling
 * column: [content] draws tile [index] with the width it gets. On its first opening the rows
 * come in one after another.
 */
@Composable
fun TileGrid(count: Int, modifier: Modifier = Modifier, minTileWidth: Dp = 150.dp, gap: Dp = 8.dp, content: @Composable (index: Int, modifier: Modifier) -> Unit) {
    val stagger = rememberStagger()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gap) / (minTileWidth + gap)).toInt().coerceIn(1, 4)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (start in 0 until count step columns) {
                Row(Modifier.staggerIn(stagger, start / columns), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (i in start until start + columns) {
                        if (i < count) content(i, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
