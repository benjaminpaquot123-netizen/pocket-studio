package com.pocketstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A track's chip in a row of tracks (Sons, the Acid 303, every instrument track; hands-on audit G7, rule 16 of
 * docs/DESIGN.md « Jamais de hauteur fixe autour d'un texte »): its colour's dot, its [name], under it [sub] (its sound,
 * « 16 pads »), lit in its [color] when [selected]. At least 44 dp high and as tall as its two lines need at any font
 * (it was 44 dp fixed: the second line cut from font 1.23 on the owner's tablet). At most 160 dp wide at font 1.0, wider
 * in proportion at a bigger font (up to 1.5): a name that fits at 1.0 fits at 1.3 too; a longer one ends in « … », its
 * whole name still read by TalkBack. [touch]: how it is touched (selectable, clickable), on the chip's own shape;
 * [modifier]: around it (its test tag, where it shows in a row that scrolls). [trailing]: after the words (a menu's ⋮).
 */
@Composable
fun TrackChip(
    name: String,
    sub: String,
    color: Color,
    selected: Boolean,
    touch: Modifier,
    modifier: Modifier = Modifier,
    subColor: Color = Neon.TextSecondary,
    subModifier: Modifier = Modifier,
    endPadding: androidx.compose.ui.unit.Dp = 10.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    val font = LocalDensity.current.fontScale
    val grow = font.coerceIn(1f, MAX_GROW)
    Row(
        // Its two one-line words, centred, no padding above or below: (20 + 16) sp in Latin letters, about 41 sp in Japanese,
        // Chinese, Korean or Arabic (their fonts' taller lines). Never more than 44 dp × the font scale, which always holds
        // them (41 s ≤ 44 s), and keeps a row of chips as tall as before where a test's stand-in font is taller.
        modifier.heightIn(min = 44.dp, max = 44.dp * font.coerceAtLeast(1f)).widthIn(max = 160.dp * grow).clip(shape)
            .background(if (selected) color.copy(alpha = 0.22f) else Neon.Surface)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else Neon.Outline), shape)
            .then(touch)
            .padding(start = 10.dp, end = endPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Column(Modifier.weight(1f, fill = false)) {
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = subModifier)
        }
        trailing?.invoke()
    }
}

/** The biggest font scale a [TrackChip] grows its width for. */
private const val MAX_GROW = 1.5f
