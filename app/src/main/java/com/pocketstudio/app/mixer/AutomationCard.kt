@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.mixer

import com.pocketstudio.ui.ScaledAlertDialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Buses
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.Macros
import com.pocketstudio.model.Project
import com.pocketstudio.model.hasPoints
import com.pocketstudio.model.i18n.percent
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.settingOf
import com.pocketstudio.model.withSongZone
import com.pocketstudio.model.withSongLine
import com.pocketstudio.model.withSongSmoothed
import com.pocketstudio.model.withStepLine
import com.pocketstudio.model.withStepsSmoothed
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.pillColors
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class AutomationUi(
    val lanes: List<AutomationLane> = emptyList(),
    val steps: Int = 16,
    /** The loop step playing (a still picture; the app passes [playStep] instead, read when drawn). */
    val currentStep: Int = -1,
    val recording: Boolean = false,
    /** The scene shown (per-scene clips edit its steps). */
    val scene: Int = 0,
    /** Bars of the song (song clips), at least a few to draw on. */
    val songBars: Int = 0,
    /** The song's bar playing now, -1 when not playing the song (a still picture, see [playSongStep]). */
    val songBar: Int = -1,
    val stepsPerBar: Int = 16,
    /** The loop step playing, -1 when stopped: read only when the curves are drawn (no recomposition). */
    val playStep: (() -> Int)? = null,
    /** The song step playing (16ths), -1 when the song does not play: read only when drawn. */
    val playSongStep: (() -> Int)? = null,
    /** The lane to show and light up (opened from « Ce qui joue »), -1: none. */
    val focusLane: Int = -1,
    /** The strip « Automatiser un réglage » starts on (the track being worked on), -1: the first. */
    val preferredCh: Int = -1,
    /** Phase 37: where each part of the song starts (bar) and its name, drawn along the song's curves. */
    val parts: List<Pair<Int, String>> = emptyList(),
    /** Audit 06: steps of the part shown (its longest loop, at most a curve's 64); 0: the drums' [steps]. */
    val partSteps: Int = 0,
    /** The step from the part's start, -1 when stopped (curves over the whole part): read when drawn. */
    val playPartStep: (() -> Int)? = null,
)

data class AutomationActions(
    val add: (AutoTarget) -> Unit = {},
    val remove: (Int) -> Unit = {},
    val clear: (Int) -> Unit = {},
    val setPoint: (lane: Int, step: Int, value: Float?) -> Unit = { _, _, _ -> },
    val toggleRecord: () -> Unit = {},
    val setMode: (lane: Int, mode: Int) -> Unit = { _, _ -> },
    val setSongPoint: (lane: Int, bar: Int, value: Float?) -> Unit = { _, _, _ -> },
    /** One edit of a lane (a line, smoothing, a beat curve's point): one undo step; [key] merges a stroke. */
    val edit: (lane: Int, key: String?, transform: (AutomationLane, Project) -> AutomationLane) -> Unit = { _, _, _ -> },
    /** A filter put on a track's first free slot, its cutoff automated (« choisir le filtre »). */
    val addFilter: (ch: Int) -> Unit = {},
    /** « Ce qui joue »: everything acting on the sound. */
    val overview: () -> Unit = {},
    /** « Voir le potard »: the control a lane moves, where it is set (null: no way there). */
    val showControl: ((AutoTarget) -> Unit)? = null,
    /** Phase 37: a song lane's points, one a bar (false) or one a beat, a quarter of a bar (true). */
    val setResolution: ((lane: Int, perBeat: Boolean) -> Unit)? = null,
    /** Audit 06: a loop lane over the whole part (its steps) or over the drum loop (0). */
    val setLoopSteps: ((lane: Int, steps: Int) -> Unit)? = null,
)

/** The lane modes and their names (read when drawn, in the interface's language). */
private val MODES get() = listOf(AutomationLane.LOOP to tr("Boucle"), AutomationLane.PER_SCENE to tr("Par partie"), AutomationLane.SONG to tr("Morceau"))

private fun List<StripInfo>.nameOf(ch: Int) = firstOrNull { it.ch == ch }?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) } ?: tr("Sortie")

/** What a target is called on screen. */
fun automationLabel(t: AutoTarget, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo> = BASIC_STRIPS): String {
    fun channelName(ch: Int) = strips.nameOf(ch)
    return when (t.kind) {
    AutoTarget.VOLUME -> tr("Volume · %s", if (t.isMasterVolume) tr("Sortie générale") else channelName(t.ch))
    AutoTarget.PAN -> tr("Place · %s", channelName(t.ch))
    AutoTarget.SEND -> tr("Envoi %s · %s", tr(Buses.NAMES.getOrElse(t.slot) { "?" }).lowercase(), channelName(t.ch))
    AutoTarget.INSERT_PARAM -> {
        val inserts = if (t.ch == MASTER_CHANNEL) master else channels.getOrNull(t.ch)?.inserts.orEmpty()
        val effect = inserts.getOrNull(t.slot)?.effect ?: EffectType.NONE
        "${tr(effect.label)} ${effect.params.getOrNull(t.param)?.label?.let { tr(it) }.orEmpty()} · ${channelName(t.ch)}"
    }
    AutoTarget.BUS_PARAM -> "${tr(Buses.NAMES.getOrElse(t.slot) { "?" })} ${Buses.PARAMS.getOrNull(t.slot)?.getOrNull(t.param)?.label?.let { tr(it) }.orEmpty()}"
    AutoTarget.ACID -> "303 ${acidKnobName(t.param)} · ${channelName(t.ch)}"
    AutoTarget.MACRO -> {
        val macro = t.param.coerceIn(0, com.pocketstudio.model.Macros.COUNT - 1)
        val name = when (t.param) {
            AutoTarget.MACRO_CHOP -> tr("Hachoir de voie")
            AutoTarget.MACRO_KILL_LOW -> tr("Coupe graves")
            AutoTarget.MACRO_KILL_MID -> tr("Coupe médiums")
            AutoTarget.MACRO_KILL_HIGH -> tr("Coupe aigus")
            AutoTarget.MACRO_THROW_REVERB -> tr("Lancer réverbe")
            AutoTarget.MACRO_THROW_ECHO -> tr("Lancer écho")
            else -> tr(Macros.NAMES[macro])
        }
        "$name · ${channelName(t.ch)}"
    }
    AutoTarget.LIVE -> tr("Live : %s", liveSettingName(t.param))
    AutoTarget.SYNTH -> "${com.pocketstudio.app.synth.synthKnobName(t.param)} · ${channelName(t.ch)}"
    AutoTarget.PLAITS -> "${com.pocketstudio.model.PlaitsPatch.PARAMS.getOrNull(t.param - 1)?.let { tr(it) } ?: "?"} · ${channelName(t.ch)}"
    else -> "?"
    }
}

/** A Live page's whole-mix setting, as its page names it. */
private fun liveSettingName(param: Int): String = when (param) {
    AutoTarget.LIVE_FILTER -> tr("Filtre DJ")
    AutoTarget.LIVE_RESONANCE -> tr("Résonance")
    AutoTarget.LIVE_REPEAT -> tr("Répéter")
    AutoTarget.LIVE_DIVISION -> tr("Division")
    AutoTarget.LIVE_TAPE_STOP -> tr("Freinage")
    AutoTarget.LIVE_CHOP -> tr("Hachoir")
    AutoTarget.LIVE_REVERB_RETURN -> tr("Réverbe")
    AutoTarget.LIVE_ECHO_RETURN -> tr("Écho")
    AutoTarget.LIVE_BACKSPIN -> tr("Rembobinage")
    AutoTarget.LIVE_REVERSE -> tr("À l'envers")
    AutoTarget.LIVE_HALF_TIME -> tr("Demi-tempo")
    AutoTarget.LIVE_ECHO_OUT -> tr("Sortie en écho")
    AutoTarget.LIVE_KILL_LOW -> tr("Coupe graves")
    AutoTarget.LIVE_KILL_MID -> tr("Coupe médiums")
    AutoTarget.LIVE_KILL_HIGH -> tr("Coupe aigus")
    AutoTarget.LIVE_FREEZE -> tr("Réverbe figée")
    AutoTarget.LIVE_THROW_REVERB -> tr("Lancer réverbe")
    AutoTarget.LIVE_THROW_ECHO -> tr("Lancer écho")
    AutoTarget.LIVE_RISER -> tr("Souffle montant")
    AutoTarget.LIVE_DOWNLIFTER -> tr("Souffle descendant")
    AutoTarget.LIVE_BUILD_UP -> tr("Montée")
    AutoTarget.LIVE_DROP -> tr("Drop")
    else -> tr("Tout remettre")
}

/** A 303 knob's name, as the Acide screen shows it ([com.pocketstudio.model.AcidSound.values] order). */
fun acidKnobName(param: Int): String = when (param) {
    0 -> tr("Coupure")
    1 -> tr("Résonance")
    2 -> tr("Enveloppe")
    3 -> tr("Déclin")
    4 -> tr("Accent")
    7 -> tr("Saturation")
    else -> "?"
}

/** Every setting that can be automated with the current effects. */
fun automationTargets(channels: List<Channel>, master: List<Insert>, strips: List<StripInfo> = BASIC_STRIPS): List<AutoTarget> = buildList {
    strips.map { it.ch }.forEach { ch ->
        // The acid line's knobs first: the filter that opens and closes is what acid music moves.
        if (ch == AutoTarget.ACID_CHANNEL) AutoTarget.ACID_PARAMS.forEach { add(AutoTarget.acid(it)) }
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

private fun effectOf(t: AutoTarget, channels: List<Channel>, master: List<Insert>): EffectType {
    val inserts = if (t.ch == MASTER_CHANNEL) master else channels.getOrNull(t.ch)?.inserts.orEmpty()
    return inserts.getOrNull(t.slot)?.effect ?: EffectType.NONE
}

/** A filter's cutoff (an insert's, or the 303's): what electronic music moves most. */
fun isFilterCutoff(t: AutoTarget, channels: List<Channel>, master: List<Insert>): Boolean =
    (t.kind == AutoTarget.ACID && t.param == 0) ||
        (t.kind == AutoTarget.INSERT_PARAM && t.param == 0 && effectOf(t, channels, master) == EffectType.FILTER)

/** What moving a target does, in plain words (the picker, the lanes, « Ce qui joue »). */
fun targetHint(t: AutoTarget, channels: List<Channel>, master: List<Insert>): String = when {
    isFilterCutoff(t, channels, master) && t.kind == AutoTarget.ACID -> tr("Ouvre et ferme le filtre de la 303 : le « wiou » de l'acide.")
    isFilterCutoff(t, channels, master) -> tr("Ouvre (brillant) ou ferme (sourd) le son : le balayage de filtre.")
    t.kind == AutoTarget.VOLUME -> tr("Plus fort ou moins fort : fondus, entrées, coupures.")
    t.kind == AutoTarget.PAN -> tr("Le son va à gauche ou à droite.")
    t.kind == AutoTarget.SEND && t.slot == 0 -> tr("Plus ou moins de réverbe : de l'espace, du lointain.")
    t.kind == AutoTarget.SEND -> tr("Plus ou moins d'écho : des répétitions en rythme.")
    t.kind == AutoTarget.ACID -> when (t.param) {
        1 -> tr("Fait siffler le filtre de la 303.")
        2 -> tr("Le coup de filtre à chaque note.")
        3 -> tr("La longueur de chaque note.")
        4 -> tr("Les notes accentuées claquent plus.")
        else -> tr("Plus de grain, un son plus sale.")
    }
    t.kind == AutoTarget.INSERT_PARAM -> effectOf(t, channels, master).blurb.takeIf { it.isNotBlank() }?.let { tr(it) } ?: tr("Un réglage de l'effet.")
    t.kind == AutoTarget.BUS_PARAM -> tr("Règle la réverbe ou l'écho commun à toutes les pistes.")
    t.kind == AutoTarget.MACRO -> tr("Un réglage rapide de la piste (boîte d'effets).")
    t.kind == AutoTarget.LIVE -> tr("Un effet de la page Live, sur tout le mélange.")
    else -> ""
}

/** The picker's order: a filter's cutoff, the volume, the reverb, the echo, the 303, the effects, the pan. */
fun targetRank(t: AutoTarget, channels: List<Channel>, master: List<Insert>): Int = when {
    isFilterCutoff(t, channels, master) -> 0
    t.kind == AutoTarget.VOLUME -> 1
    t.kind == AutoTarget.SEND && t.slot == 0 -> 2
    t.kind == AutoTarget.SEND -> 3
    t.kind == AutoTarget.ACID -> 4
    t.kind == AutoTarget.INSERT_PARAM -> 5
    t.kind == AutoTarget.PAN -> 6
    else -> 7
}

/** A target's name without its track's (the picker shows them under their track). */
fun targetShortName(t: AutoTarget, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo>): String =
    automationLabel(t, channels, master, strips).let { if (t.kind == AutoTarget.BUS_PARAM || t.kind == AutoTarget.LIVE) it else it.substringBeforeLast(" · ") }

/**
 * A lane's value as its setting shows it, [v] 0..1 as the lane writes it: dB for a volume, left /
 * right for a pan, the effect's own unit (Hz, ms, dB, %) for an effect, % for the 303's knobs (as
 * their 0..100), on / off for a held control.
 */
fun automationValueText(t: AutoTarget, v: Float, channels: List<Channel>, master: List<Insert>): String {
    val x = v.coerceIn(0f, 1f)
    val pct = percent((x * 100).roundToInt())
    return when {
        t.isOnOff -> if (x >= 0.5f) tr("marche") else tr("arrêt")
        t.kind == AutoTarget.VOLUME -> faderDb(x)
        t.kind == AutoTarget.PAN -> {
            val pan = ((x * 2f - 1f) * 100).roundToInt()
            when {
                pan == 0 -> tr("centre")
                pan < 0 -> tr("G %d", -pan)
                else -> tr("D %d", pan)
            }
        }
        t.kind == AutoTarget.INSERT_PARAM -> effectOf(t, channels, master).params.getOrNull(t.param)?.format?.invoke(x) ?: pct
        t.kind == AutoTarget.BUS_PARAM -> Buses.PARAMS.getOrNull(t.slot)?.getOrNull(t.param)?.format?.invoke(x) ?: pct
        t.kind == AutoTarget.LIVE && t.param == AutoTarget.LIVE_FILTER -> when {
            x < 0.48f -> tr("Passe-bas")
            x > 0.52f -> tr("Passe-haut")
            else -> tr("ouvert")
        }
        else -> pct
    }
}

/** Who a lane belongs to, as its chip says it: the track (its name and colour), the master, the buses, the Live page. */
private fun laneOwner(t: AutoTarget, strips: List<StripInfo>): Pair<String, Color?> = when {
    t.kind == AutoTarget.BUS_PARAM -> tr("Réverbe et écho communs") to null
    t.kind == AutoTarget.LIVE -> tr("Page Live") to null
    t.ch == MASTER_CHANNEL -> tr("Sortie générale") to null
    else -> strips.firstOrNull { it.ch == t.ch }?.let { com.pocketstudio.model.DefaultNames.shown(it.name) to it.color } ?: (tr("Sortie") to null)
}

/** A control a lane moves can be shown where it is set (« Voir le potard »). */
fun hasControl(t: AutoTarget): Boolean = when (t.kind) {
    AutoTarget.ACID, AutoTarget.INSERT_PARAM, AutoTarget.SEND, AutoTarget.BUS_PARAM -> true
    AutoTarget.MACRO -> t.param < Macros.COUNT
    else -> false
}

/**
 * The lane's value at [pos] (in its points: loop steps, or song points; below 0, stopped: its
 * start): a song curve's point, or a loop's step (a step without a point keeps the last one written,
 * the loop wrapping). Null when the lane has no point.
 */
internal fun laneValueAt(song: Boolean, values: List<Float?>, curve: List<Float>, count: Int, pos: Float): Float? {
    val at = pos.coerceAtLeast(0f).toInt()
    if (song) return curve.getOrNull(at.coerceAtMost(curve.lastIndex))
    val n = count.coerceIn(1, AutomationLane.STEPS)
    val s = at % n
    for (k in 0 until n) values.getOrNull(Math.floorMod(s - k, n))?.let { return it }
    return null
}

/**
 * Where the time scale under a curve has its numbers: (point, label), about eight at most. A song
 * curve counts bars ([perBar] points each), a loop counts beats (4 steps each), from 1.
 */
internal fun timeTicks(song: Boolean, first: Int, visible: Int, perBar: Int): List<Pair<Int, String>> {
    val unit = if (song) perBar.coerceAtLeast(1) else 4
    val shown = (visible + unit - 1) / unit
    val stride = listOf(1, 2, 4, 8, 16, 32, 64, 128).firstOrNull { shown / it <= 8 } ?: 256
    val out = ArrayList<Pair<Int, String>>()
    var k = (first + unit - 1) / unit
    k = (k + stride - 1) / stride * stride
    while (k * unit < first + visible) {
        out += k * unit to "${k + 1}"
        k += stride
    }
    return out
}

/** Drawing tools of a lane. */
private const val TOOL_DRAW = 0
private const val TOOL_LINE = 1
/** A zone of the song set to one level (song curves). */
private const val TOOL_ZONE = 2
private val ZOOMS = listOf(1, 2, 4, 8)

/** The curve's height, and the width of its value scale. */
private val CURVE_HEIGHT = 136.dp
private val AXIS_WIDTH = 56.dp

@Composable
fun AutomationCard(
    ui: AutomationUi, channels: List<Channel>, master: List<Insert>, actions: AutomationActions,
    strips: List<StripInfo> = BASIC_STRIPS, modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    NeonCard(accent = Neon.Amber, modifier = modifier.testTag("automation")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Courbes"), Modifier.weight(1f), color = Neon.Amber, help = Glossary.AUTOMATION)
            FilterChip(
                selected = ui.recording, onClick = actions.toggleRecord,
                label = { Text(tr("Enregistrer")) },
                leadingIcon = { Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = if (ui.recording) Neon.Red else Neon.TextSecondary) },
                colors = pillColors(Neon.Red),
                modifier = Modifier.testTag("automation_record"),
            )
        }
        Text(
            if (ui.recording) tr("Lance la lecture et bouge un réglage : il s'enregistre pas à pas sur la boucle.")
            else tr("Fais bouger un réglage tout seul : choisis-le, puis dessine sa courbe (au doigt, ou une ligne en deux touches)."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        // The ways in first: what to move, and everything that already moves.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { picking = true }, enabled = ui.lanes.size < AutomationLane.MAX_LANES,
                colors = ButtonDefaults.buttonColors(containerColor = Neon.Amber, contentColor = Neon.OnAccent),
                modifier = Modifier.heightIn(min = 48.dp).testTag("automation_add"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" " + tr("Automatiser un réglage"))
            }
            OutlinedButton(onClick = actions.overview, modifier = Modifier.heightIn(min = 48.dp).testTag("automation_overview")) {
                Icon(Icons.Filled.Waves, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + tr("Ce qui joue"))
            }
        }
        if (ui.lanes.isEmpty()) {
            Text(tr("Rien ne bouge encore. Idée : le filtre d'une nappe qui s'ouvre sur 8 mesures."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        ui.lanes.forEachIndexed { i, lane -> LaneBlock(i, lane, ui, channels, master, strips, actions) }
    }
    if (picking) {
        TargetPicker(
            used = ui.lanes.map { it.target }.toSet(), channels = channels, master = master, strips = strips,
            pick = { picking = false; actions.add(it) },
            addFilter = { picking = false; actions.addFilter(it) },
            dismiss = { picking = false },
            preferred = ui.preferredCh,
        )
    }
}

/**
 * One lane: whose it is (the track's coloured chip first, then the setting), its value now (a small
 * dial that follows the playhead), a way to the control it moves, its kind (loop, scene, song), its
 * tools and its curve with a value scale on the side and bars (or beats) under it.
 */
@Composable
internal fun LaneBlock(
    i: Int, lane: AutomationLane, ui: AutomationUi, channels: List<Channel>, master: List<Insert>,
    strips: List<StripInfo>, actions: AutomationActions,
) = com.pocketstudio.ui.LeftToRight {
    val focus = remember { BringIntoViewRequester() }
    LaunchedEffect(ui.focusLane) { if (ui.focusLane == i) focus.bringIntoView() }
    val lit = ui.focusLane == i
    val shape = RoundedCornerShape(12.dp)
    val song = lane.mode == AutomationLane.SONG
    val perBar = lane.pointsPerBar(ui.stepsPerBar / 4)
    val bars = maxOf(ui.songBars, 8)
    // A loop over the whole part (audit 06) counts its own steps, from the part's start.
    val own = !song && lane.loopSteps > 0
    val count = if (song) bars * perBar else (if (own) lane.loopSteps else ui.steps).coerceIn(1, AutomationLane.STEPS)
    val values = if (song) lane.songValues else lane.valuesFor(ui.scene)
    val curve = if (song) remember(lane, count) { lane.songCurve(count) } else emptyList()
    // Where the playhead is, in points of this curve (bars, beats or steps); read when drawn.
    val unitSteps = if (song) (if (lane.songPerBeat) 4 else ui.stepsPerBar.coerceAtLeast(1)) else 1
    val playhead: () -> Float = remember(ui.playStep, ui.playSongStep, ui.playPartStep, ui.currentStep, ui.songBar, song, own, count, unitSteps, ui.stepsPerBar) {
        if (own) {
            val f = ui.playPartStep
            if (f != null) { { f().let { s -> if (s < 0) -1f else Math.floorMod(s, count).toFloat() } } } else { { -1f } }
        } else if (song) {
            val f = ui.playSongStep
            if (f != null) { { f().let { s -> if (s < 0) -1f else s.toFloat() / unitSteps } } }
            else { { if (ui.songBar < 0) -1f else ui.songBar * ui.stepsPerBar.toFloat() / unitSteps } }
        } else {
            val f = ui.playStep
            if (f != null) { { f().toFloat() } } else { { ui.currentStep.toFloat() } }
        }
    }
    val (owner, ownerColor) = laneOwner(lane.target, strips)
    val tint = ownerColor ?: if (lane.target.kind == AutoTarget.BUS_PARAM) Neon.Magenta else Neon.Cyan
    Column(
        Modifier.fillMaxWidth().bringIntoViewRequester(focus)
            .then(if (lit) Modifier.border(2.dp, Neon.Amber, shape).background(Neon.Amber.copy(alpha = 0.06f), shape).padding(8.dp) else Modifier.padding(top = 4.dp))
            .testTag("auto_lane_$i"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // The track first, in its colour: which track this curve moves is never a guess.
                TrackChip(owner, tint, "auto_track_$i")
                Text(targetShortName(lane.target, channels, master, strips), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { actions.clear(i) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("Vider")) }
            IconButton(onClick = { actions.remove(i) }) { Icon(Icons.Filled.Delete, contentDescription = tr("Supprimer")) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Its value as it plays (stopped: at the start), the way its knob would show it.
            LaneDial(
                value = { laneValueAt(song, values, curve, count, playhead()) },
                text = { automationValueText(lane.target, it, channels, master) },
                color = tint, tag = "auto_dial_$i",
            )
            Text(targetHint(lane.target, channels, master), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 3)
        }
        val show = actions.showControl
        if (show != null && hasControl(lane.target)) {
            OutlinedButton(onClick = { show(lane.target) }, modifier = Modifier.heightIn(min = 48.dp).testTag("auto_show_$i")) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + if (lane.target.kind == AutoTarget.ACID) tr("Voir le potard") else tr("Voir le réglage"))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MODES.forEach { (mode, label) ->
                FilterChip(
                    selected = lane.mode == mode, onClick = { actions.setMode(i, mode) }, label = { Text(label) },
                    modifier = Modifier.testTag("auto_mode_${i}_$mode"),
                )
            }
        }
        // Audit 06: a loop curve over the drum loop or over the whole part, when the part is longer.
        val loopSteps = actions.setLoopSteps
        val part = ui.partSteps.coerceAtMost(AutomationLane.STEPS)
        if (!song && loopSteps != null && (part > ui.steps || own)) {
            fun length(steps: Int) = if (ui.stepsPerBar > 0 && steps % ui.stepsPerBar == 0) com.pocketstudio.app.ui.barsShort(steps / ui.stepsPerBar) else tr("%d pas", steps)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !own, onClick = { loopSteps(i, 0) }, label = { Text(tr("Sur la batterie · %s", length(ui.steps))) },
                    modifier = Modifier.testTag("auto_len_drums_$i"))
                FilterChip(selected = own, onClick = { loopSteps(i, maxOf(part, ui.steps)) }, label = { Text(tr("Sur toute la partie · %s", length(if (own) lane.loopSteps else maxOf(part, ui.steps)))) },
                    modifier = Modifier.testTag("auto_len_part_$i"))
            }
        }
        // Phase 37: along the song, a point a bar or a point a beat (a quarter of a bar in 4/4).
        val resolution = actions.setResolution
        if (song && resolution != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !lane.songPerBeat, onClick = { resolution(i, false) }, label = { Text(tr("1 point par mesure")) },
                    modifier = Modifier.testTag("auto_res_bar_$i"))
                FilterChip(selected = lane.songPerBeat, onClick = { resolution(i, true) }, label = { Text(tr("1 point par temps (¼ de mesure)")) },
                    modifier = Modifier.testTag("auto_res_beat_$i"))
            }
        }
        Text(
            when {
                !song && lane.mode == AutomationLane.PER_SCENE -> tr("Pas de la partie %s (chaque partie a les siens).", com.pocketstudio.model.Scene.NAMES.getOrElse(ui.scene) { "?" })
                !song -> tr("La même courbe à chaque tour de boucle (%d pas).", count)
                ui.songBars == 0 -> tr("Morceau vide : la courbe suit les mesures depuis le début de la lecture.")
                lane.songPerBeat -> tr("Une valeur par temps (¼ de mesure) sur tout le morceau (%d mesures) ; elle glisse d'un point à l'autre.", bars)
                else -> tr("Une valeur par mesure sur tout le morceau (%d mesures) ; elle glisse d'un point à l'autre.", bars)
            },
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        // A loop repeats every bar or so: a change over a part of the song is the song's curve.
        if (!song && ui.songBars > 0 && !lane.hasPoints) {
            Text(
                tr("Pour changer ce réglage sur une partie du morceau (plus fort au début…), choisis « Morceau »."),
                style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("auto_song_tip_$i"),
            )
        }
        var chosenTool by rememberSaveable(i) { mutableIntStateOf(TOOL_DRAW) }
        val tool = if (!song && chosenTool == TOOL_ZONE) TOOL_DRAW else chosenTool
        var zoom by rememberSaveable(i, song) { mutableIntStateOf(if (song && count > 32) 2 else 1) }
        var start by rememberSaveable(i, song) { mutableIntStateOf(0) }
        val visible = (count + zoom - 1) / zoom
        val first = start.coerceIn(0, (count - visible).coerceAtLeast(0))
        // Zoomed in while playing: the window follows the playhead.
        LaunchedEffect(zoom, count, visible) {
            if (zoom > 1) snapshotFlow { playhead() }.collect { pos ->
                if (pos >= 0 && (pos < start || pos >= start + visible)) start = (pos.toInt() / visible * visible).coerceIn(0, (count - visible).coerceAtLeast(0))
            }
        }
        LaneTools(
            i, tool, { chosenTool = it }, zoom,
            zoomBy = { up ->
                val z = ZOOMS[(ZOOMS.indexOf(zoom) + if (up) 1 else -1).coerceIn(0, ZOOMS.lastIndex)]
                zoom = z
            },
            canMove = zoom > 1, first = first, visible = visible, count = count, song = song, perBar = perBar,
            move = { dir -> start = (first + dir * maxOf(1, visible / 2)).coerceIn(0, (count - visible).coerceAtLeast(0)) },
            smooth = {
                actions.edit(i, null) { l, p -> if (l.mode == AutomationLane.SONG) l.withSongSmoothed() else l.withStepsSmoothed(p.scene) }
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row {
                ValueAxis(lane.target, channels, master)
                CurveEditor(
                    tag = if (song) "song_lane_${lane.target.pack()}" else "lane_${lane.target.pack()}",
                    count = count, first = first, visible = visible, song = song, values = values, curve = curve,
                    beat = if (song) perBar else 4, bar = if (song) perBar * 4 else ui.stepsPerBar,
                    playhead = playhead, tool = tool,
                    onPoint = { u, v ->
                        when {
                            !song -> actions.setPoint(i, u, v)
                            perBar == 1 -> actions.setSongPoint(i, u, v)
                            else -> actions.edit(i, "auto-song-$i") { l, _ -> l.withSongPoint(u, v) }
                        }
                    },
                    onLine = { a, va, b, vb ->
                        actions.edit(i, null) { l, p -> if (l.mode == AutomationLane.SONG) l.withSongLine(a, va, b, vb) else l.withStepLine(a, va, b, vb, p.scene) }
                    },
                    onZone = { a, b, v ->
                        // Around the zone the curve keeps what it was (the setting itself where it had none).
                        actions.edit(i, null) { l, p -> if (l.mode == AutomationLane.SONG) l.withSongZone(a, b, v, p.settingOf(l.target), count) else l }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (song) tr("mesure") else tr("temps"), Modifier.width(AXIS_WIDTH).padding(end = 4.dp),
                    style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1, textAlign = TextAlign.End,
                )
                TimeAxis(song, first, visible, perBar, Modifier.weight(1f))
            }
            // Phase 37: the parts' names under the song's curve, where each starts (« B · Drop »).
            if (song && ui.parts.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("partie"), Modifier.width(AXIS_WIDTH).padding(end = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1, textAlign = TextAlign.End)
                    PartAxis(ui.parts, first, visible, perBar, Modifier.weight(1f).testTag("auto_parts_$i"))
                }
            }
        }
    }
}

/** The track a lane moves: a dot of its colour and its name, on a tint of it. */
@Composable
private fun TrackChip(name: String, color: Color, tag: String) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.22f)).padding(horizontal = 8.dp, vertical = 3.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(name, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium, color = Neon.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A small knob showing [value] (null: nothing written: an empty ring and « – ») and its words
 * ([text]: dB, Hz, %…). Read when drawn: the playhead moving redraws it, never recomposes it.
 */
@Composable
private fun LaneDial(value: () -> Float?, text: (Float) -> String, color: Color, tag: String) {
    val measurer = rememberTextMeasurer()
    val style = com.pocketstudio.ui.MonoStyle.copy(color = Neon.TextPrimary)
    val track = Neon.SurfaceHigh
    val needle = Neon.TextPrimary
    Canvas(Modifier.size(width = 136.dp, height = 44.dp).testTag(tag)) {
        val v = value()
        val d = size.height
        val stroke = 4.dp.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arc = Size(d - stroke, d - stroke)
        drawArc(track, 135f, 270f, false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
        if (v != null) {
            val x = v.coerceIn(0f, 1f)
            drawArc(color, 135f, 270f * x, false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
            val angle = Math.toRadians((135f + 270f * x).toDouble())
            val c = Offset(d / 2, d / 2)
            val r = d / 2 - stroke * 2f
            drawLine(needle, c, Offset(c.x + r * cos(angle).toFloat(), c.y + r * sin(angle).toFloat()), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
        val layout = measurer.measure(if (v != null) text(v) else "–", style)
        drawText(layout, topLeft = Offset(d + 8.dp.toPx(), (size.height - layout.size.height) / 2f))
    }
}

/** The value scale left of a curve: its top, middle and bottom as the setting shows them. */
@Composable
private fun ValueAxis(t: AutoTarget, channels: List<Channel>, master: List<Insert>) {
    val style = MaterialTheme.typography.labelSmall
    val ink = Neon.TextSecondary
    Column(
        Modifier.width(AXIS_WIDTH).height(CURVE_HEIGHT).padding(end = 4.dp).testTag("auto_scale_${t.pack()}"),
        verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End,
    ) {
        Text(automationValueText(t, 1f, channels, master), style = style, color = ink, maxLines = 1, softWrap = false)
        Text(if (t.isOnOff) "" else automationValueText(t, 0.5f, channels, master), style = style, color = ink, maxLines = 1, softWrap = false)
        Text(automationValueText(t, 0f, channels, master), style = style, color = ink, maxLines = 1, softWrap = false)
    }
}

/** The numbers under a curve: its bars (a song) or beats (a loop), where they begin. */
@Composable
private fun TimeAxis(song: Boolean, first: Int, visible: Int, perBar: Int, modifier: Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.height(18.dp)) {
        val ticks = timeTicks(song, first, visible, perBar)
        ticks.forEach { (unit, label) ->
            Text(
                label, Modifier.offset(x = maxWidth * ((unit - first).toFloat() / visible.coerceAtLeast(1)) + 2.dp),
                style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1, softWrap = false,
            )
        }
    }
}

/**
 * Phase 37: the song's parts along a curve's window ([first], [visible] points of [perBar] a bar):
 * a line where each starts and its name, cut where the next one begins.
 */
@Composable
private fun PartAxis(parts: List<Pair<Int, String>>, first: Int, visible: Int, perBar: Int, modifier: Modifier) {
    val lineColor = Neon.Magenta
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.height(20.dp)) {
        val shown = visible.coerceAtLeast(1)
        val per = perBar.coerceAtLeast(1)
        parts.forEachIndexed { k, (bar, name) ->
            val from = bar * per
            val until = parts.getOrNull(k + 1)?.first?.times(per) ?: Int.MAX_VALUE
            if (until <= first || from >= first + shown) return@forEachIndexed
            val x = ((from - first).coerceAtLeast(0).toFloat() / shown)
            val w = ((minOf(until, first + shown) - maxOf(from, first)).toFloat() / shown).coerceAtLeast(0f)
            Row(Modifier.offset(x = maxWidth * x).width(maxWidth * w).height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(2.dp).height(16.dp).background(lineColor))
                Text(name, Modifier.padding(start = 2.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Draw, line or zone, smooth, zoom and the window's place. */
@Composable
private fun LaneTools(
    i: Int, tool: Int, setTool: (Int) -> Unit, zoom: Int, zoomBy: (Boolean) -> Unit, canMove: Boolean,
    first: Int, visible: Int, count: Int, song: Boolean, perBar: Int, move: (Int) -> Unit, smooth: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = tool == TOOL_DRAW, onClick = { setTool(TOOL_DRAW) }, label = { Text(tr("Dessiner")) },
            leadingIcon = { Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
            colors = pillColors(Neon.Amber), modifier = Modifier.testTag("auto_tool_draw_$i"),
        )
        FilterChip(
            selected = tool == TOOL_LINE, onClick = { setTool(TOOL_LINE) }, label = { Text(tr("Ligne")) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
            colors = pillColors(Neon.Amber), modifier = Modifier.testTag("auto_tool_line_$i"),
        )
        // A zone of the song at one level (« plus fort au début »): song curves only.
        if (song) FilterChip(
            selected = tool == TOOL_ZONE, onClick = { setTool(TOOL_ZONE) }, label = { Text(tr("Zone")) },
            leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp)) },
            colors = pillColors(Neon.Amber), modifier = Modifier.testTag("auto_tool_zone_$i"),
        )
        TextButton(onClick = smooth, modifier = Modifier.heightIn(min = 48.dp).testTag("auto_smooth_$i")) { Text(tr("Lisser")) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { zoomBy(false) }, enabled = zoom > 1, modifier = Modifier.testTag("auto_zoom_out_$i")) {
                Icon(Icons.Filled.ZoomOut, contentDescription = tr("Voir plus large"))
            }
            Text("×$zoom", style = com.pocketstudio.ui.MonoStyle, color = Neon.TextSecondary)
            IconButton(onClick = { zoomBy(true) }, enabled = zoom < ZOOMS.last() && count / (zoom * 2) >= 4, modifier = Modifier.testTag("auto_zoom_in_$i")) {
                Icon(Icons.Filled.ZoomIn, contentDescription = tr("Voir de plus près"))
            }
        }
        if (canMove) Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { move(-1) }, enabled = first > 0, modifier = Modifier.testTag("auto_left_$i")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Plus tôt"))
            }
            val last = minOf(first + visible, count)
            Text(
                if (song) tr("mes. %1\$d à %2\$d", first / perBar + 1, (last + perBar - 1) / perBar) else tr("pas %1\$d à %2\$d", first + 1, last),
                style = com.pocketstudio.ui.MonoStyle, color = Neon.TextSecondary,
            )
            IconButton(onClick = { move(1) }, enabled = first + visible < count, modifier = Modifier.testTag("auto_right_$i")) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("Plus tard"))
            }
        }
    }
    // The way to draw said once (the first lane), and whenever the ramp or zone tool is on.
    if (tool != TOOL_DRAW || i == 0) Text(
        when (tool) {
            TOOL_LINE -> tr("Ligne : touche le début puis la fin (ou glisse de l'un à l'autre) ; une rampe bien droite les relie.")
            TOOL_ZONE -> tr("Zone : glisse sur les mesures à régler (ou touche la première puis la dernière) ; la hauteur où tu lâches donne le niveau. Avant et après, rien ne change.")
            else -> tr("Dessine au doigt ; deux touches rapides effacent un point. « Lisser » adoucit un trait tremblé, « Annuler » le défait.")
        },
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
}

/**
 * A curve to draw on: [count] points ([visible] of them shown from [first]), a bar each for a
 * loop's steps or a line through a song's points, big handles, the playhead read only when drawn.
 * [tool]: drawing; a line (two touches or a slide draw a straight ramp); a zone (two touches or a
 * slide pick the points, the last touch's height their level).
 */
@Composable
private fun CurveEditor(
    tag: String, count: Int, first: Int, visible: Int, song: Boolean, values: List<Float?>, curve: List<Float>,
    beat: Int, bar: Int, playhead: () -> Float, tool: Int,
    onPoint: (Int, Float?) -> Unit, onLine: (Int, Float, Int, Float) -> Unit, onZone: (Int, Int, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val point by rememberUpdatedState(onPoint)
    val ramp by rememberUpdatedState(onLine)
    val zone by rememberUpdatedState(onZone)
    val from by rememberUpdatedState(first)
    val shown by rememberUpdatedState(visible)
    val total by rememberUpdatedState(count)
    var anchor by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    var dragTo by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    val color = Neon.Amber
    val ghost = Neon.Amber.copy(alpha = 0.25f)
    val grid = Neon.Outline.copy(alpha = 0.5f)
    val head = Neon.Acid
    val back = Neon.SurfaceHigh.copy(alpha = 0.5f)
    val ramping = Neon.Cyan
    val twoTouch = tool == TOOL_LINE || tool == TOOL_ZONE
    val zoning = tool == TOOL_ZONE
    fun unitAt(x: Float, w: Float) = (from + (x / w * shown).toInt()).coerceIn(0, total - 1)
    fun valueAt(y: Float, h: Float) = (1f - y / h).coerceIn(0f, 1f)
    fun finish(a: Pair<Int, Float>, b: Pair<Int, Float>) {
        if (zoning) zone(minOf(a.first, b.first), maxOf(a.first, b.first), b.second) else ramp(a.first, a.second, b.first, b.second)
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(CURVE_HEIGHT)
            .background(back, RoundedCornerShape(8.dp))
            .testTag(tag)
            .pointerInput(tool) {
                if (twoTouch) {
                    detectTapGestures { o ->
                        val u = unitAt(o.x, size.width.toFloat()) to valueAt(o.y, size.height.toFloat())
                        val a = anchor
                        if (a == null) { anchor = u; dragTo = null } else { anchor = null; dragTo = null; finish(a, u) }
                    }
                } else {
                    detectTapGestures(
                        onTap = { o -> point(unitAt(o.x, size.width.toFloat()), valueAt(o.y, size.height.toFloat())) },
                        onDoubleTap = { o -> point(unitAt(o.x, size.width.toFloat()), null) },
                    )
                }
            }
            .pointerInput(tool) {
                var last: Pair<Int, Float>? = null
                detectDragGestures(
                    onDragStart = { o ->
                        val u = unitAt(o.x, size.width.toFloat()) to valueAt(o.y, size.height.toFloat())
                        if (twoTouch) { anchor = u; dragTo = u } else { point(u.first, u.second); last = u }
                    },
                    onDragEnd = {
                        val a = anchor
                        val b = dragTo
                        if (twoTouch && a != null && b != null) finish(a, b)
                        if (twoTouch) { anchor = null; dragTo = null }
                        last = null
                    },
                    onDragCancel = { anchor = null; dragTo = null; last = null },
                ) { change, _ ->
                    val u = unitAt(change.position.x, size.width.toFloat()) to valueAt(change.position.y, size.height.toFloat())
                    if (twoTouch) { dragTo = u; return@detectDragGestures }
                    // A fast finger skips points: the ones in between follow the line it drew.
                    val l = last
                    if (l != null && kotlin.math.abs(u.first - l.first) > 1) {
                        val dir = if (u.first > l.first) 1 else -1
                        var k = l.first + dir
                        while (k != u.first) {
                            point(k, l.second + (u.second - l.second) * (k - l.first) / (u.first - l.first))
                            k += dir
                        }
                    }
                    if (l == null || l.first != u.first || kotlin.math.abs(l.second - u.second) > 0.004f) point(u.first, u.second)
                    last = u
                }
            },
    ) {
        val w = size.width / visible
        fun xOf(u: Float) = (u - first) * w
        // The grid: beats faint, bars stronger, the middle line.
        for (u in first..first + visible) {
            if (u % beat != 0) continue
            val strong = u % bar == 0
            drawLine(grid.copy(alpha = if (strong) 0.9f else 0.35f), Offset(xOf(u.toFloat()), 0f), Offset(xOf(u.toFloat()), size.height), strokeWidth = if (strong) 2f else 1f)
        }
        drawLine(grid.copy(alpha = 0.3f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        val handle = minOf(w * 0.45f, 9.dp.toPx()).coerceAtLeast(3.dp.toPx())
        if (song) {
            if (curve.isNotEmpty()) {
                val path = Path()
                val fill = Path()
                val end = minOf(first + visible, curve.size)
                for (u in first until end) {
                    val x = xOf(u.toFloat())
                    val y = size.height - curve[u] * size.height
                    if (u == first) { path.moveTo(x, y); fill.moveTo(x, size.height); fill.lineTo(x, y) } else { path.lineTo(x, y); fill.lineTo(x, y) }
                }
                if (end > first) {
                    // To the window's edge: where the next point goes.
                    val y = size.height - curve[minOf(end, curve.size - 1)] * size.height
                    path.lineTo(xOf(end.toFloat()), y)
                    fill.lineTo(xOf(end.toFloat()), y)
                    fill.lineTo(xOf(end.toFloat()), size.height)
                    fill.close()
                    drawPath(fill, color.copy(alpha = 0.12f))
                    drawPath(path, color.copy(alpha = 0.8f), style = Stroke(width = 2.5.dp.toPx()))
                }
            }
            for (u in first until minOf(first + visible, values.size)) {
                val v = values[u] ?: continue
                val c = Offset(xOf(u.toFloat()), size.height - v * size.height)
                drawCircle(Color.Black.copy(alpha = 0.4f), radius = handle + 2.dp.toPx(), center = c)
                drawCircle(color, radius = handle, center = c)
            }
        } else {
            var held: Float? = values.take(total).lastOrNull { it != null }  // the loop wraps
            for (s in 0 until first) values.getOrNull(s)?.let { held = it }
            for (s in first until minOf(first + visible, total)) {
                val v = values.getOrNull(s)
                if (v != null) held = v
                val value = v ?: held ?: continue
                val h = value * size.height
                val x = xOf(s.toFloat())
                drawRoundRect(
                    if (v != null) color else ghost, Offset(x + w * 0.12f, size.height - h), Size(w * 0.76f, h.coerceAtLeast(3f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
                // A handle on top of each step drawn: where the finger takes it.
                if (v != null) drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(x + w * 0.12f, size.height - h - 1.5.dp.toPx()), Size(w * 0.76f, 3.dp.toPx()))
            }
        }
        anchor?.let { (u, v) ->
            if (zoning) {
                // The zone being picked, and the level it gets (the finger's height).
                val end = dragTo ?: (u to v)
                val lo = minOf(u, end.first)
                val hi = maxOf(u, end.first)
                drawRect(ramping.copy(alpha = 0.18f), Offset(xOf(lo.toFloat()), 0f), Size((hi - lo + 1) * w, size.height))
                val y = size.height - end.second * size.height
                drawLine(ramping, Offset(xOf(lo.toFloat()), y), Offset(xOf((hi + 1).toFloat()), y), 3.dp.toPx())
            } else {
                // The ramp being drawn: its start, and the line to the finger.
                val a = Offset(xOf(u.toFloat()), size.height - v * size.height)
                drawCircle(ramping, radius = handle + 2.dp.toPx(), center = a, style = Stroke(2.dp.toPx()))
                dragTo?.let { (u2, v2) ->
                    drawLine(ramping, a, Offset(xOf(u2.toFloat()), size.height - v2 * size.height), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
                }
            }
        }
        drawPlayhead(playhead(), first, visible, w, head, column = !song)
    }
}

/** The playhead: a bright line with a glow and a tab on top (and the step playing lit, on a loop). */
private fun DrawScope.drawPlayhead(pos: Float, first: Int, visible: Int, w: Float, color: Color, column: Boolean) {
    if (pos < 0f || pos < first || pos >= first + visible) return
    val x = (pos - first) * w
    if (column) drawRect(color.copy(alpha = 0.16f), Offset(x, 0f), Size(w, size.height))
    drawLine(color.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 8.dp.toPx())
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.5.dp.toPx())
    val tab = Path().apply {
        moveTo(x - 7.dp.toPx(), 0f)
        lineTo(x + 7.dp.toPx(), 0f)
        lineTo(x, 9.dp.toPx())
        close()
    }
    drawPath(tab, color)
}

/** « Que veux-tu faire bouger ? »: the track first, then its most useful settings, each with what it does. */
@Composable
private fun TargetPicker(
    used: Set<AutoTarget>, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo>,
    pick: (AutoTarget) -> Unit, addFilter: (Int) -> Unit, dismiss: () -> Unit, preferred: Int = -1,
) {
    ScaledAlertDialog(
        onDismissRequest = dismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 10.dp).testTag("automation_picker"),
        title = { Text(tr("Que veux-tu faire bouger ?")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) { TargetPickerContent(used, channels, master, strips, pick, addFilter, preferred) }
        },
        confirmButton = { TextButton(onClick = dismiss, modifier = Modifier.testTag("automation_picker_close")) { Text(tr("Fermer")) } },
    )
}

/** One choice of the picker's first step: a track, the master or the shared buses, with what can move there. */
private class PickGroup(val key: Int, val name: String, val color: Color, val targets: List<AutoTarget>, val filterOffer: Boolean)

/** The picker's first step for the shared reverb and echo (not a strip). */
private const val GROUP_BUSES = -2

/**
 * The picker: 1. the track (a chip each, in its colour; [preferred] chosen first, else the first),
 * 2. what moves on it, the most useful first (filter, volume, reverb). The master and the shared
 * buses are chosen the same way.
 */
@Composable
fun TargetPickerContent(
    used: Set<AutoTarget>, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo>,
    pick: (AutoTarget) -> Unit, addFilter: (Int) -> Unit, preferred: Int = -1,
) {
    val all = remember(channels, master, strips, used) { automationTargets(channels, master, strips).filter { it !in used } }
    val groups = buildList<PickGroup> {
        strips.forEach { st ->
            val targets = all.filter { it.kind != AutoTarget.BUS_PARAM && it.kind != AutoTarget.LIVE && it.ch == st.ch }
            val c = channels.getOrNull(st.ch)
            val canAddFilter = st.ch <= AutoTarget.MAX_CH && c != null &&
                c.inserts.none { it.effect == EffectType.FILTER } && c.inserts.any { it.effect == EffectType.NONE }
            if (targets.isNotEmpty() || canAddFilter) add(PickGroup(st.ch, com.pocketstudio.model.DefaultNames.shown(st.name), st.color ?: Neon.Cyan, targets, canAddFilter))
        }
        all.filter { it.kind == AutoTarget.INSERT_PARAM && it.ch == MASTER_CHANNEL }.takeIf { it.isNotEmpty() }?.let {
            add(PickGroup(MASTER_CHANNEL, tr("Sortie générale"), Neon.Cyan, it, false))
        }
        all.filter { it.kind == AutoTarget.BUS_PARAM }.takeIf { it.isNotEmpty() }?.let {
            add(PickGroup(GROUP_BUSES, tr("Réverbe et écho communs"), Neon.Magenta, it, false))
        }
    }
    var chosen by rememberSaveable { mutableIntStateOf(preferred) }
    val group = groups.firstOrNull { it.key == chosen } ?: groups.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("1. Choisis la piste :"), style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEach { g -> TrackChoice(g.name, g.color, g === group, "pick_track_${g.key}") { chosen = g.key } }
        }
        if (group == null) return@Column
        Text(tr("2. Ce qui bouge sur « %s » :", group.name), style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary)
        val tag = when (group.key) {
            MASTER_CHANNEL -> "pick_group_master"
            GROUP_BUSES -> "pick_group_buses"
            else -> "pick_group_${group.key}"
        }
        PickerGroup(
            group.name, group.color, tag, group.targets, channels, master, strips, pick,
            filterOffer = if (group.filterOffer) ({ addFilter(group.key) }) else null, filterTag = "pick_filter_${group.key}",
        )
    }
}

/** A track to choose in the picker: a dot of its colour and its name; the chosen one filled and outlined. */
@Composable
private fun TrackChoice(name: String, color: Color, selected: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.heightIn(min = 48.dp).clip(shape)
            .background(if (selected) color.copy(alpha = 0.28f) else Neon.SurfaceHigh.copy(alpha = 0.6f))
            .border(if (selected) 2.dp else 1.dp, if (selected) color else color.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp).testTag(tag)
            .semantics { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(name, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary, maxLines = 1)
    }
}


@Composable
private fun PickerGroup(
    name: String, color: Color, tag: String, targets: List<AutoTarget>, channels: List<Channel>, master: List<Insert>,
    strips: List<StripInfo>, pick: (AutoTarget) -> Unit, filterOffer: (() -> Unit)? = null, filterTag: String = "",
) {
    if (targets.isEmpty() && filterOffer == null) return
    var more by rememberSaveable(tag) { mutableStateOf(false) }
    val sorted = remember(targets, channels, master) { targets.sortedBy { targetRank(it, channels, master) } }
    val top = if (filterOffer != null) 3 else 4
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 4.dp, height = 20.dp).background(color, RoundedCornerShape(2.dp)))
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        if (filterOffer != null) {
            PickerItem(tr("Filtre (balayage)"), tr("Ajoute un filtre à cette piste et fais-le s'ouvrir ou se fermer : l'effet qu'on entend le plus."), filterTag, filterOffer)
        }
        (if (more) sorted else sorted.take(top)).forEach { t ->
            PickerItem(targetShortName(t, channels, master, strips), targetHint(t, channels, master), "pick_${t.pack()}") { pick(t) }
        }
        if (sorted.size > top) {
            TextButton(onClick = { more = !more }, modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_more")) {
                Text(
                    when {
                        more -> tr("Moins de réglages")
                        sorted.size - top == 1 -> tr("Voir 1 autre réglage")
                        else -> tr("Voir les %d autres réglages", sorted.size - top)
                    },
                )
            }
        }
    }
}

@Composable
private fun PickerItem(title: String, hint: String, tag: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .background(Neon.SurfaceHigh.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Neon.TextPrimary)
        if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}
