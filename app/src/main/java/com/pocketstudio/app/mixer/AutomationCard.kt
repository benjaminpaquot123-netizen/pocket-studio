@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Buses
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

data class AutomationUi(
    val lanes: List<AutomationLane> = emptyList(),
    val steps: Int = 16,
    val currentStep: Int = -1,
    val recording: Boolean = false,
    /** The scene shown (per-scene clips edit its steps). */
    val scene: Int = 0,
    /** Bars of the song (song clips), at least a few to draw on. */
    val songBars: Int = 0,
    /** The song's bar playing now, -1 when not playing the song. */
    val songBar: Int = -1,
    val stepsPerBar: Int = 16,
)

data class AutomationActions(
    val add: (AutoTarget) -> Unit = {},
    val remove: (Int) -> Unit = {},
    val clear: (Int) -> Unit = {},
    val setPoint: (lane: Int, step: Int, value: Float?) -> Unit = { _, _, _ -> },
    val toggleRecord: () -> Unit = {},
    val setMode: (lane: Int, mode: Int) -> Unit = { _, _ -> },
    val setSongPoint: (lane: Int, bar: Int, value: Float?) -> Unit = { _, _, _ -> },
)

private val MODES = listOf(AutomationLane.LOOP to "Boucle", AutomationLane.PER_SCENE to "Par scène", AutomationLane.SONG to "Morceau")

private fun List<StripInfo>.nameOf(ch: Int) = firstOrNull { it.ch == ch }?.name ?: "Sortie"

/** What a target is called on screen. */
fun automationLabel(t: AutoTarget, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo> = BASIC_STRIPS): String {
    fun channelName(ch: Int) = strips.nameOf(ch)
    return when (t.kind) {
    AutoTarget.VOLUME -> "Volume · ${channelName(t.ch)}"
    AutoTarget.PAN -> "Panoramique · ${channelName(t.ch)}"
    AutoTarget.SEND -> "Envoi ${Buses.NAMES.getOrElse(t.slot) { "?" }.lowercase()} · ${channelName(t.ch)}"
    AutoTarget.INSERT_PARAM -> {
        val inserts = if (t.ch == MASTER_CHANNEL) master else channels.getOrNull(t.ch)?.inserts.orEmpty()
        val effect = inserts.getOrNull(t.slot)?.effect ?: EffectType.NONE
        "${effect.label} ${effect.params.getOrNull(t.param)?.label.orEmpty()} · ${channelName(t.ch)}"
    }
    AutoTarget.BUS_PARAM -> "${Buses.NAMES.getOrElse(t.slot) { "?" }} ${Buses.PARAMS.getOrNull(t.slot)?.getOrNull(t.param)?.label.orEmpty()}"
    else -> "?"
    }
}

/** Every setting that can be automated with the current effects. */
fun automationTargets(channels: List<Channel>, master: List<Insert>, strips: List<StripInfo> = BASIC_STRIPS): List<AutoTarget> = buildList {
    strips.map { it.ch }.forEach { ch ->
        add(AutoTarget(AutoTarget.VOLUME, ch))
        add(AutoTarget(AutoTarget.PAN, ch))
        repeat(Buses.COUNT) { add(AutoTarget(AutoTarget.SEND, ch, it)) }
        channels.getOrNull(ch)?.inserts?.forEachIndexed { slot, ins ->
            ins.effect.params.indices.forEach { add(AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, it)) }
        }
    }
    master.forEachIndexed { slot, ins -> ins.effect.params.indices.forEach { add(AutoTarget(AutoTarget.INSERT_PARAM, MASTER_CHANNEL, slot, it)) } }
    repeat(Buses.COUNT) { bus -> Buses.PARAMS[bus].indices.forEach { add(AutoTarget(AutoTarget.BUS_PARAM, 0, bus, it)) } }
}

@Composable
fun AutomationCard(ui: AutomationUi, channels: List<Channel>, master: List<Insert>, actions: AutomationActions, strips: List<StripInfo> = BASIC_STRIPS) {
    NeonCard(accent = Neon.Amber, modifier = Modifier.testTag("automation")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Automation", Modifier.weight(1f), color = Neon.Amber, help = Glossary.AUTOMATION)
            FilterChip(
                selected = ui.recording, onClick = actions.toggleRecord,
                label = { Text("Enregistrer") },
                leadingIcon = { Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = if (ui.recording) Neon.Red else Neon.TextSecondary) },
                colors = pillColors(Neon.Red),
                modifier = Modifier.testTag("automation_record"),
            )
        }
        Text(
            if (ui.recording) "Lance la lecture et bouge un réglage : il s'enregistre pas à pas sur la boucle."
            else "Dessine au doigt la valeur de chaque pas de la boucle (deux touches rapides : effacer le pas).",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        ui.lanes.forEachIndexed { i, lane ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(automationLabel(lane.target, channels, master, strips), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { actions.clear(i) }) { Text("Vider") }
                IconButton(onClick = { actions.remove(i) }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
            }
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                MODES.forEach { (mode, label) ->
                    FilterChip(
                        selected = lane.mode == mode, onClick = { actions.setMode(i, mode) }, label = { Text(label) },
                        modifier = Modifier.testTag("auto_mode_${i}_$mode"),
                    )
                }
            }
            when (lane.mode) {
                AutomationLane.SONG -> {
                    val bars = maxOf(ui.songBars, 8)
                    Text(
                        if (ui.songBars == 0) "Morceau vide : la courbe suit les mesures depuis le début de la lecture."
                        else "Une valeur par mesure sur tout le morceau ($bars mesures) ; elle glisse d'un point à l'autre.",
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                    )
                    SongLaneEditor(lane, bars, ui.songBar) { bar, v -> actions.setSongPoint(i, bar, v) }
                }
                else -> {
                    if (lane.mode == AutomationLane.PER_SCENE) {
                        Text("Pas de la scène ${com.pocketstudio.model.Scene.NAMES.getOrElse(ui.scene) { "?" }} (chaque scène a les siens).",
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
                    LaneEditor(lane.copy(values = lane.valuesFor(ui.scene)), ui.steps, ui.currentStep) { step, v -> actions.setPoint(i, step, v) }
                }
            }
        }
        if (ui.lanes.size < AutomationLane.MAX_LANES) {
            var open by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().testTag("automation_add")) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Automatiser un réglage")
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    val used = ui.lanes.map { it.target }.toSet()
                    automationTargets(channels, master, strips).filter { it !in used }.forEach { t ->
                        DropdownMenuItem(text = { Text(automationLabel(t, channels, master, strips)) }, onClick = { open = false; actions.add(t) })
                    }
                }
            }
        }
    }
}

/** One lane: a bar per step (drawn by finger), held values ghosted, the playing step lit. */
@Composable
private fun LaneEditor(lane: AutomationLane, steps: Int, currentStep: Int, setPoint: (Int, Float?) -> Unit) {
    val onPoint by rememberUpdatedState(setPoint)
    val color = Neon.Amber
    val ghost = Neon.Amber.copy(alpha = 0.25f)
    val grid = Neon.Outline
    val head = Neon.TextPrimary
    fun at(x: Float, y: Float, w: Float, h: Float): Pair<Int, Float> =
        (x / w * steps).toInt().coerceIn(0, steps - 1) to (1f - y / h).coerceIn(0f, 1f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("lane_${lane.target.pack()}")
            .pointerInput(steps) {
                detectTapGestures(
                    onTap = { o -> val (s, v) = at(o.x, o.y, size.width.toFloat(), size.height.toFloat()); onPoint(s, v) },
                    onDoubleTap = { o -> val (s, _) = at(o.x, o.y, size.width.toFloat(), size.height.toFloat()); onPoint(s, null) },
                )
            }
            .pointerInput(steps) {
                detectDragGestures { change, _ ->
                    val (s, v) = at(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                    onPoint(s, v)
                }
            },
    ) {
        val w = size.width / steps
        var held: Float? = lane.values.take(steps).lastOrNull { it != null }  // the loop wraps
        for (s in 0 until steps) {
            val x = s * w
            if (s % 4 == 0) drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = if (s % 16 == 0) 2f else 1f)
            val v = lane.values.getOrNull(s)
            if (v != null) held = v
            val shown = v ?: held
            if (shown != null) {
                val h = shown * size.height
                drawRect(if (v != null) color else ghost, Offset(x + w * 0.15f, size.height - h), Size(w * 0.7f, h.coerceAtLeast(2f)))
            }
            if (s == currentStep) drawRect(head.copy(alpha = 0.25f), Offset(x, 0f), Size(w, size.height))
        }
    }
}

/** A song clip: a point per bar (drawn by finger), the line the engine follows between them. */
@Composable
private fun SongLaneEditor(lane: AutomationLane, bars: Int, playingBar: Int, setPoint: (Int, Float?) -> Unit) {
    val onPoint by rememberUpdatedState(setPoint)
    val color = Neon.Amber
    val grid = Neon.Outline
    val head = Neon.TextPrimary
    fun at(x: Float, y: Float, w: Float, h: Float): Pair<Int, Float> =
        (x / w * bars).toInt().coerceIn(0, bars - 1) to (1f - y / h).coerceIn(0f, 1f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("song_lane_${lane.target.pack()}")
            .pointerInput(bars) {
                detectTapGestures(
                    onTap = { o -> val (b, v) = at(o.x, o.y, size.width.toFloat(), size.height.toFloat()); onPoint(b, v) },
                    onDoubleTap = { o -> val (b, _) = at(o.x, o.y, size.width.toFloat(), size.height.toFloat()); onPoint(b, null) },
                )
            }
            .pointerInput(bars) {
                detectDragGestures { change, _ ->
                    val (b, v) = at(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                    onPoint(b, v)
                }
            },
    ) {
        val w = size.width / bars
        for (b in 0 until bars) if (b % 4 == 0) drawLine(grid, Offset(b * w, 0f), Offset(b * w, size.height), strokeWidth = if (b % 16 == 0) 2f else 1f)
        if (playingBar in 0 until bars) drawRect(head.copy(alpha = 0.2f), Offset(playingBar * w, 0f), Size(w, size.height))
        val curve = lane.songCurve(bars)
        if (curve.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path()
            curve.forEachIndexed { b, v ->
                val x = b * w + w / 2
                val y = size.height - v * size.height
                if (b == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
        lane.songValues.forEachIndexed { b, v ->
            if (v != null && b < bars) drawCircle(color, radius = minOf(w / 2, 10f), center = Offset(b * w + w / 2, size.height - v * size.height))
        }
    }
}
