package com.pocketstudio.app.acid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * The Acid 303's own look (Benjamin, 2026-10-29: « en jaune, avec un look acide »): acid yellow
 * with near-black ink (contrast about 14:1), and the acid house smiley as its sign.
 */
object AcidLook {
    /** Acid yellow: the 303's chip, its title, its smiley. */
    val Yellow = Color(0xFFFFE135)

    /** The ink on the yellow (text, the smiley's eyes and smile). */
    val Ink = Color(0xFF17140A)

    /** The 303's colour where a track has one (Mix, Live, the timeline): the same yellow. */
    val Track = Yellow
}

/** The acid house smiley: a yellow face, two eyes, a wide smile, drawn in [ink]. */
@Composable
fun AcidSmiley(size: Dp, modifier: Modifier = Modifier, face: Color = AcidLook.Yellow, ink: Color = AcidLook.Ink) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        val line = (r * 0.13f).coerceAtLeast(1.5f)
        drawCircle(face, r, c)
        drawCircle(ink, r - line / 2f, c, style = Stroke(line))
        // Two tall eyes, a little above the middle.
        val eye = Size(r * 0.2f, r * 0.42f)
        listOf(-0.32f, 0.32f).forEach { dx ->
            drawOval(ink, topLeft = Offset(c.x + dx * r - eye.width / 2f, c.y - r * 0.5f), size = eye)
        }
        // The smile: the lower part of a circle, with its rounded ends.
        val m = r * 0.58f
        drawArc(
            ink, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(c.x - m, c.y - m * 0.95f), size = Size(m * 2f, m * 2f),
            style = Stroke(line, cap = StrokeCap.Round),
        )
    }
}

/**
 * « Acid 303 » in a row of track chips (Sons, the Mode simple's Clavier): always yellow, so it is
 * found at once; the one shown has the accent's ring around it. A touch opens its editor.
 */
@Composable
fun AcidChip(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier.height(44.dp).widthIn(max = 180.dp).clip(shape)
            .background(AcidLook.Yellow)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Neon.Cyan else AcidLook.Ink.copy(alpha = 0.35f)), shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(start = 6.dp, end = 10.dp)
            .testTag("sons_Acide"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AcidSmiley(26.dp)
        Column {
            Text(
                tr("Acid 303"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold,
                color = AcidLook.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                tr("Ligne de basse"), style = MaterialTheme.typography.labelSmall, color = AcidLook.Ink.copy(alpha = 0.78f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The top of the Acid 303's editor: its smiley and its name in yellow, and in the Mode simple what it is for. */
@Composable
fun AcidTitle(simple: Boolean) {
    Row(Modifier.padding(horizontal = 4.dp).semantics(mergeDescendants = true) {}.testTag("acid_title"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AcidSmiley(32.dp)
        Column {
            Text(tr("Acid 303"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = AcidLook.Yellow)
            if (simple) {
                Text(
                    tr("La basse qui couine de l'acid house. Touche « Écris-moi une ligne », puis touche une case pour changer sa note."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
            }
        }
    }
}
