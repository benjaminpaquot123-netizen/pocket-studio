package com.pocketstudio.app.midi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

/**
 * The hint shown for a few seconds when a knob turns: the screen's eight knobs in two rows of
 * four, each with its number, its setting and where it is; the knob just turned is outlined,
 * with « Tourne ▲ / ▼ » while it has not reached its setting yet. A touch opens the full list.
 */
@Composable
fun KnobHintBar(hint: KnobHint, open: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Neon.Surface)
            .border(1.dp, Neon.Outline, RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = open)
            .padding(horizontal = 10.dp, vertical = 8.dp).testTag("knob_hint"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(16.dp))
            Text(
                "  " + tr("Potards · %s", hint.screen.label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(tr("Tout voir"), style = MaterialTheme.typography.labelMedium, color = Neon.Cyan)
        }
        hint.slots.chunked(4).forEachIndexed { row, four ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                four.forEachIndexed { col, slot ->
                    val i = row * 4 + col
                    KnobCell(i, slot.label, hint.values.getOrElse(i) { 0f }, touched = i == hint.touched, turn = if (i == hint.touched) hint.turn else 0, modifier = Modifier.weight(1f))
                }
                repeat(4 - four.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KnobCell(index: Int, label: String, value: Float, touched: Boolean, turn: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.clip(shape).background(if (touched) Neon.SurfaceHigh else Neon.Background)
            .border(if (touched) 2.dp else 0.dp, if (touched) Neon.Cyan else Neon.Background, shape)
            .padding(horizontal = 6.dp, vertical = 4.dp).testTag("knob_cell_$index"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // The name has the whole width; the knob's number sits before its level.
        Text(
            label, style = MaterialTheme.typography.labelMedium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (touched) Neon.TextPrimary else Neon.TextSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${index + 1}", style = MonoStyle, fontSize = 11.sp, color = if (touched) Neon.Cyan else Neon.TextSecondary)
            if (turn != 0) {
                Text(if (turn > 0) tr("Tourne ▲") else tr("Tourne ▼"), style = MaterialTheme.typography.labelSmall, color = Neon.Amber, maxLines = 1, modifier = Modifier.testTag("knob_turn"))
            } else {
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Neon.SurfaceHigh)) {
                    Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (touched) Neon.Acid else Neon.Cyan))
                }
            }
        }
    }
}

class KnobPanelActions(
    /** « Changer »: another setting for knob (index), or null for the app's own choice. */
    val change: (Int, KnobTarget?) -> Unit = { _, _ -> },
    /** « Apprendre »: the next knob turned takes this place (null: cancel). */
    val learn: (Int?) -> Unit = {},
    val reset: () -> Unit = {},
    val setEnabled: (Boolean) -> Unit = {},
    val close: () -> Unit = {},
)

/**
 * « Potards du contrôleur »: the eight knobs of the screen shown, each with « Changer » (another
 * of the screen's settings) and « Apprendre » (another knob of the controller for this place),
 * « Remettre comme au départ », and the switch for the whole idea.
 */
@Composable
fun KnobPanel(hint: KnobHint, candidates: List<KnobSlot>, enabled: Boolean, actions: KnobPanelActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 12.dp).navigationBarsPadding()
            .testTag("knob_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Potards du contrôleur"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tr("Sur %s, tes 8 premiers potards font :", hint.screen.label), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
            }
            IconButton(onClick = actions.close, modifier = Modifier.testTag("knob_panel_close")) { Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary) }
        }
        hint.slots.forEachIndexed { i, slot -> PanelRow(i, slot, hint, candidates, actions) }
        Text(
            tr("Chaque potard ne bouge son réglage qu'une fois arrivé à sa valeur : le son ne saute jamais."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Potards automatiques"), style = MaterialTheme.typography.bodyLarge)
                Text(tr("Ils suivent l'écran affiché : la 303 sur la basse acide, le son sur une piste de Sons, les volumes dans Mix, les effets en Live."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            Switch(checked = enabled, onCheckedChange = actions.setEnabled, modifier = Modifier.testTag("knob_enabled"))
        }
        TextButton(onClick = actions.reset, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("knob_reset")) { Text(tr("Remettre comme au départ")) }
    }
}

@Composable
private fun PanelRow(i: Int, slot: KnobSlot, hint: KnobHint, candidates: List<KnobSlot>, actions: KnobPanelActions) {
    NeonCard(modifier = Modifier.testTag("knob_row_$i")) {
        // The knob and its setting on a line of their own, the two buttons under them: room for long names at 130 %.
        val cc = hint.ccs.getOrNull(i)
        Text(if (cc != null) tr("Potard %1\$d · CC %2\$d", i + 1, cc) else tr("Potard %d", i + 1), style = MonoStyle, color = Neon.TextSecondary)
        if (hint.learning == i) {
            Text(tr("Tourne le potard de ton contrôleur…"), color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("knob_learning"))
        } else {
            Text(slot.label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            var open by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("knob_change_$i")) { Text(tr("Changer"), maxLines = 1) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text(tr("Choix de l'appli")) }, onClick = { open = false; actions.change(i, null) })
                    candidates.forEach { c ->
                        DropdownMenuItem(text = { Text(c.label) }, onClick = { open = false; actions.change(i, c.target) })
                    }
                }
            }
            if (hint.learning == i) {
                TextButton(onClick = { actions.learn(null) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("Annuler"), maxLines = 1) }
            } else {
                TextButton(onClick = { actions.learn(i) }, modifier = Modifier.heightIn(min = 48.dp).testTag("knob_learn_$i")) { Text(tr("Apprendre"), maxLines = 1) }
            }
        }
    }
}

/**
 * The knobs on screen: the hint floating above the tab bar after a turn (it never takes the
 * touch from what is under it, but a touch on it opens the list), and the list as a sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnobOverlay(vm: KnobsViewModel, device: String?) {
    val hint by vm.hint.collectAsStateWithLifecycle()
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val h = hint ?: return
    if (vm.pinned) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::close, sheetState = state, containerColor = Neon.Surface) {
            KnobPanel(
                h, vm.candidates(), enabled,
                KnobPanelActions(change = vm::override, learn = vm::learn, reset = vm::reset, setEnabled = vm::setEnabled, close = vm::close),
            )
        }
    } else {
        Popup(alignment = Alignment.BottomCenter, properties = PopupProperties(focusable = false)) {
            Box(Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 92.dp)) {
                KnobHintBar(h, open = { vm.open(device) })
            }
        }
    }
}

/** Outils › MIDI: the automatic knobs in a few words, their switch and the list. */
@Composable
fun AutoKnobsCard(enabled: Boolean, setEnabled: (Boolean) -> Unit, open: () -> Unit) {
    NeonCard(modifier = Modifier.testTag("midi_auto_knobs")) {
        SectionLabel(tr("Potards automatiques"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("Les 8 premiers potards de ton contrôleur règlent tout seuls l'écran affiché : la 303 sur la basse acide, le son sur une piste de Sons, les volumes dans Mix, les effets en Live."),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = setEnabled, modifier = Modifier.testTag("midi_auto_knobs_switch"))
        }
        OutlinedButton(onClick = open, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("midi_auto_knobs_open")) {
            Text(tr("Voir et changer les potards"))
        }
    }
}

/** Keeps [vm] told which screen is shown; nothing to draw. */
@Composable
fun KnobScreenFollower(vm: KnobsViewModel, screen: KnobScreen) {
    LaunchedEffect(screen) { vm.screen = screen }
}
