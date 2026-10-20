package com.pocketstudio.app.synth

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.ArpUi
import com.pocketstudio.app.rhythm.PlayUi
import com.pocketstudio.ui.Neon

class PlayActions(
    val setBend: (Float) -> Unit = {},
    val setMod: (Float) -> Unit = {},
    val setSustain: (Boolean) -> Unit = {},
    val setArp: (ArpUi) -> Unit = {},
)

private val RATES = listOf(0.5f to "1/32", 1f to "1/16", 2f to "1/8", 4f to "1/4")
private val ARP_MODES = listOf("Montant", "Descendant", "Aller-retour", "Au hasard")

/**
 * Live playing under the keyboard: pitch bend (springs back), modulation, sustain pedal and the
 * arpeggiator (hold keys: their notes play one after the other, in time).
 */
@Composable
fun PlayPanel(play: PlayUi, actions: PlayActions) {
    Column(Modifier.fillMaxWidth().testTag("play_panel")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Bend", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(52.dp))
            Slider(
                value = play.bend, onValueChange = actions.setBend, valueRange = -1f..1f,
                onValueChangeFinished = { actions.setBend(0f) },  // a pitch wheel springs back
                modifier = Modifier.weight(1f).testTag("bend"),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Modul.", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(52.dp))
            Slider(value = play.mod, onValueChange = actions.setMod, modifier = Modifier.weight(1f).testTag("mod"))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = play.sustain, onClick = { actions.setSustain(!play.sustain) }, label = { Text("Sustain") },
                modifier = Modifier.testTag("sustain"))
            FilterChip(selected = play.arp.on, onClick = { actions.setArp(play.arp.copy(on = !play.arp.on)) }, label = { Text("Arpège") },
                modifier = Modifier.testTag("arp"))
            if (play.arp.on) {
                RATES.forEach { (steps, label) ->
                    FilterChip(selected = play.arp.steps == steps, onClick = { actions.setArp(play.arp.copy(steps = steps)) }, label = { Text(label) })
                }
            }
        }
        if (play.arp.on) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ARP_MODES.forEachIndexed { i, label ->
                    FilterChip(selected = play.arp.mode == i, onClick = { actions.setArp(play.arp.copy(mode = i)) }, label = { Text(label) })
                }
                (1..4).forEach { o ->
                    FilterChip(selected = play.arp.octaves == o, onClick = { actions.setArp(play.arp.copy(octaves = o)) },
                        label = { Text(if (o == 1) "1 octave" else "$o oct.") })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Longueur des notes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("${(play.arp.gate * 100).toInt()} %", color = Neon.Cyan)
            }
            Slider(value = play.arp.gate, onValueChange = { actions.setArp(play.arp.copy(gate = it.coerceIn(0.05f, 1f))) })
            Text("Garde des touches enfoncées : l'arpège les joue à tour de rôle, en rythme.",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    }
}
