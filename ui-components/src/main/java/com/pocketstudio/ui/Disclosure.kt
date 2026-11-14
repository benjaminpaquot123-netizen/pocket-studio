package com.pocketstudio.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** One value a [Disclosure] hides, as its summary says it; [changed]: not the default one (said in the accent). */
class Hidden(val text: String, val changed: Boolean = false)

/**
 * A fold that says what it hides (rule 13 of docs/DESIGN.md, « Un repli montre ce qu'il cache »; hands-on audit G4):
 * full width, at least 48 dp, a filled row with a thin border, its [label], under it the values it hides ([summary],
 * a value that is not the default one in the accent), and a 20 dp chevron on the right, ⌄ closed and ⌃ open (the ›
 * belongs to [LinkRow], which opens somewhere else). Closed, TalkBack reads the label and the summary and hears that it opens; open, that it closes. The summary
 * wraps, it is never cut. [modifier] lands on the touchable row (its test tag). The cards' own fold (principle 9: a small
 * pale arrow by the title) stays for the cards.
 */
@Composable
fun Disclosure(label: String, summary: List<Hidden>, open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    // ⌄ closed, ⌃ open (Android's way): the › stays for the link rows, which open somewhere else (review, moyen n° 4).
    val turn by animateFloatAsState(if (open) 180f else 0f, StudioMotion.spatial(), label = "disclosure-chevron")
    val accent = Neon.Cyan
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(Neon.SurfaceHigh, shape).border(BorderStroke(1.dp, Neon.Outline), shape)
            .clickable(role = Role.Button, onClick = onToggle)
            // TalkBack: « Développer » while closed, « Réduire » while open (the touch does the same).
            .semantics { if (open) collapse { onToggle(); true } else expand { onToggle(); true } }
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Neon.TextPrimary)
            if (summary.isNotEmpty()) {
                val secondary = Neon.TextSecondary
                Text(
                    buildAnnotatedString {
                        summary.forEachIndexed { i, h ->
                            if (i > 0) withStyle(SpanStyle(color = secondary)) { append(" · ") }
                            withStyle(SpanStyle(color = if (h.changed) accent else secondary, fontWeight = if (h.changed) FontWeight.SemiBold else null)) { append(h.text) }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        val chevron = Neon.TextSecondary
        Canvas(Modifier.size(20.dp).rotate(turn)) {
            val w = size.width
            val stroke = 2.dp.toPx()
            drawLine(chevron, Offset(w * 0.2f, w * 0.36f), Offset(w * 0.5f, w * 0.66f), stroke, StrokeCap.Round)
            drawLine(chevron, Offset(w * 0.5f, w * 0.66f), Offset(w * 0.8f, w * 0.36f), stroke, StrokeCap.Round)
        }
    }
}
