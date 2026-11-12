package com.pocketstudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A line that opens something (rule 12 of docs/DESIGN.md, « Un lien a l'air d'un lien »; hands-on audit G3): a filled
 * row with rounded corners, its icon (or a [leading] mark) on the left, its [title] in the text's colour, what it is for
 * under it ([subtitle]), the chevron › on the right. Never an orange sentence alone: the orange stays for the screen's
 * main action. At least 56 dp high (its words centred in it, no padding above and below that would make a row of them taller than the old lines); its words wrap (two lines each at most), never cut to « … » at a big font.
 * [modifier] lands on the touchable row (its test tag, its Guide spot).
 */
@Composable
fun LinkRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    /** What TalkBack says a touch does (« Appuie deux fois pour … »); null: the row's own words. */
    clickLabel: String? = null,
    /** In place of [icon]: a mark of its own (a track's colour). */
    leading: (@Composable () -> Unit)? = null,
    /** The subtitle's colour (a warning, « coupée »). */
    subtitleColor: Color = Neon.TextSecondary,
    /** On the subtitle alone (its test tag). */
    subtitleModifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(LinkRowShape).background(Neon.SurfaceHigh.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 4.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            leading != null -> leading()
            icon != null -> Icon(icon, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            // Two lines at most, never split mid-word (a long track name in a narrow card).
            WholeWordsText(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Neon.TextPrimary, maxLines = 2)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = subtitleColor, maxLines = 3, modifier = subtitleModifier) }
        }
        Chevron(Neon.TextSecondary, Modifier.testTag("link_chevron"))
    }
}

private val LinkRowShape = RoundedCornerShape(12.dp)

/** The › of a [LinkRow] (‹ in a right-to-left language), drawn: the same stroke as the cards' fold arrow, bigger. */
@Composable
private fun Chevron(color: Color, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(16.dp)) {
            val w = size.width
            val stroke = 2.dp.toPx()
            val (tip, back) = if (rtl) w * 0.32f to w * 0.64f else w * 0.68f to w * 0.36f
            drawLine(color, Offset(back, w * 0.18f), Offset(tip, w * 0.5f), stroke, StrokeCap.Round)
            drawLine(color, Offset(tip, w * 0.5f), Offset(back, w * 0.82f), stroke, StrokeCap.Round)
        }
    }
}
