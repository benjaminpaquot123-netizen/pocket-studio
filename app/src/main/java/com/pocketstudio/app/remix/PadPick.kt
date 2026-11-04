package com.pocketstudio.app.remix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledAlertDialog

/**
 * Audit ultra round 1, n° 2: no pad is free for a piece of the song (the kit's first eight are
 * never taken unasked): the 16 pads as the drums show them, the one touched takes it (its sound
 * is replaced, « Annuler » puts it back), « Annuler » here sends nothing.
 */
@Composable
fun PadPickDialog(names: List<String>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Plus de pad libre : sur quel pad ?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    tr("Le son du pad touché est remplacé (la flèche retour le remet)."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
                for (row in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0 until 4) {
                            val lane = row * 4 + col
                            val name = names.getOrElse(lane) { "" }
                            val said = tr("Pad %1\$d : %2\$s", lane + 1, name)
                            Column(
                                Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(10.dp))
                                    .background(DrumKitInfo.colors[lane].copy(alpha = 0.28f))
                                    .clickable(role = Role.Button) { onPick(lane) }
                                    .semantics { contentDescription = said }
                                    .padding(4.dp).testTag("pad_pick_$lane"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("${lane + 1}", style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary)
                                Text(
                                    name, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("pad_pick_cancel")) { Text(tr("Annuler")) } },
        modifier = Modifier.testTag("pad_pick"),
    )
}

/** The board's question ([StemBoardState.pick]), wherever the board's pieces are sent from. */
@Composable
fun PadPickHost(state: StemBoardState, actions: StemBoardActions) {
    state.pick?.let { pick -> PadPickDialog(pick.names, actions::pickPad, actions::dismissPick) }
}
