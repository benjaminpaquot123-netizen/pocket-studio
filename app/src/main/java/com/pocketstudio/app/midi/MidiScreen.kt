package com.pocketstudio.app.midi

import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

class MidiActions(
    val connect: (String) -> Unit = {},
    val disconnect: () -> Unit = {},
    val scanBluetooth: () -> Unit = {},
    val setFollowClock: (Boolean) -> Unit = {},
    val setSendClock: (Boolean) -> Unit = {},
)

/** MIDI learn: knobs already mapped, settings that can be, the one waiting for a knob. */
class LearnUi(
    /** Controller number and what it moves. */
    val mappings: List<Pair<Int, String>> = emptyList(),
    /** Every setting a knob can move: its label and an opaque key for [learn]. */
    val targets: List<Pair<String, Any>> = emptyList(),
    val waiting: String? = null,
    val learn: (Any?) -> Unit = {},
    val remove: (Int) -> Unit = {},
)

/** MIDI keyboards and controllers: connect, see what arrives, follow another machine's clock. */
@Composable
fun MidiScreen(ui: MidiUi, actions: MidiActions, learn: LearnUi = LearnUi()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("midi"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NeonCard {
            SectionLabel("Clavier MIDI", help = Glossary.MIDI)
            if (!ui.available) {
                Text("Ce téléphone ne gère pas le MIDI.", color = Neon.Amber)
                return@NeonCard
            }
            val connected = ui.sources.firstOrNull { it.id == ui.connected }
            Text(
                if (connected != null) "Connecté : ${connected.name} (${connected.kind})"
                else "Branche un clavier en USB (il se connecte tout seul) ou cherche-le en Bluetooth.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (connected != null) Neon.Acid else Neon.TextPrimary,
            )
            Text(ui.last ?: "En attente…", style = MonoStyle, color = Neon.Cyan, modifier = Modifier.testTag("midi_last"))
            ui.sources.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (s.kind == "Bluetooth") Icons.Filled.Bluetooth else Icons.Filled.Piano, contentDescription = null, tint = Neon.TextSecondary)
                    Text("  ${s.name}", Modifier.weight(1f), maxLines = 1)
                    if (s.id == ui.connected) TextButton(onClick = actions.disconnect) { Text("Déconnecter") }
                    else TextButton(onClick = { actions.connect(s.id) }, modifier = Modifier.testTag("midi_connect_${s.id}")) { Text("Connecter") }
                }
            }
            OutlinedButton(onClick = actions.scanBluetooth, enabled = !ui.scanning, modifier = Modifier.fillMaxWidth().testTag("midi_scan")) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null)
                Text(if (ui.scanning) "  Recherche en cours…" else "  Chercher un clavier Bluetooth")
            }
            Text(
                "Les notes jouent le Synthé. Sur le canal 10, les notes Do 2 à Ré# 3 (36 à 51) tapent les 16 pads.",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
        NeonCard {
            SectionLabel("Horloge")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Suivre l'horloge MIDI", style = MaterialTheme.typography.bodyLarge)
                    Text("Tempo, départ et arrêt viennent de l'autre machine (boîte à rythmes, ordinateur…).",
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                Switch(checked = ui.followClock, onCheckedChange = actions.setFollowClock, modifier = Modifier.testTag("midi_follow"))
            }
            ui.clockBpm?.let { Text("Tempo reçu : ${"%.1f".format(it)} BPM", style = MonoStyle, color = Neon.Cyan) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Envoyer l'horloge MIDI", style = MaterialTheme.typography.bodyLarge)
                    Text("L'appareil branché suit ton tempo, ton départ et ton arrêt.",
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                Switch(checked = ui.sendClock, onCheckedChange = actions.setSendClock, modifier = Modifier.testTag("midi_send_clock"))
            }
        }
        LearnCard(learn)
    }
}

@Composable
private fun LearnCard(learn: LearnUi) {
    NeonCard(modifier = Modifier.testTag("midi_learn")) {
        SectionLabel("Boutons du contrôleur (MIDI learn)", help = Glossary.MIDI_LEARN)
        Text(
            "Associe un bouton ou un fader de ton contrôleur à un réglage : choisis le réglage, puis tourne le bouton.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        learn.mappings.forEach { (cc, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bouton $cc", style = MonoStyle, color = Neon.Cyan, modifier = Modifier.padding(end = 8.dp))
                Text(label, Modifier.weight(1f), maxLines = 2)
                TextButton(onClick = { learn.remove(cc) }) { Text("Retirer") }
            }
        }
        if (learn.waiting != null) {
            Text("Tourne maintenant le bouton pour « ${learn.waiting} »…", color = Neon.Amber, modifier = Modifier.testTag("midi_learning"))
            TextButton(onClick = { learn.learn(null) }) { Text("Annuler") }
        } else {
            var open by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().testTag("midi_learn_add")) { Text("Associer un bouton") }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    learn.targets.forEach { (label, key) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { open = false; learn.learn(key) })
                    }
                }
            }
        }
    }
}
