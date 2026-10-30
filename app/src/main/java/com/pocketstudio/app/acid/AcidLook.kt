package com.pocketstudio.app.acid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * The Acid 303's colour: a yellow at the level of the other tracks' (the instrument tracks' own
 * yellow, [com.pocketstudio.ui.TrackColors]). Benjamin (2026-10-30) took back the loud yellow and
 * the smiley of 2026-10-29 (« c'est trop là »): its chip looks like « Batterie »'s, its name stays.
 */
object AcidLook {
    /** The 303's yellow: its chip's dot and ring, its title. */
    val Yellow = Color(0xFFFFD54F)

    /** The 303's colour where a track has one (Mix, Live, the timeline): the same yellow. */
    val Track = Yellow
}

/**
 * « Acid 303 » in a row of track chips (Sons, the Mode simple's Clavier), drawn as « Batterie »'s:
 * its yellow dot, lit in yellow when it is the one shown. A touch opens its editor.
 */
@Composable
fun AcidChip(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val color = AcidLook.Yellow
    Row(
        modifier.height(44.dp).widthIn(max = 160.dp).clip(shape)
            .background(if (selected) color.copy(alpha = 0.22f) else Neon.Surface)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else Neon.Outline), shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag("sons_Acide"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Column {
            Text(
                tr("Acid 303"), style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                tr("Ligne de basse"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The top of the Acid 303's editor: its name, and in the Mode simple what it is for. */
@Composable
fun AcidTitle(simple: Boolean) {
    Column(Modifier.padding(horizontal = 4.dp).semantics(mergeDescendants = true) {}.testTag("acid_title")) {
        Text(tr("Acid 303"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AcidLook.Yellow)
        if (simple) {
            Text(
                tr("La basse qui couine de l'acid house. Touche « Écris-moi une ligne », puis touche une case pour changer sa note."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
}
