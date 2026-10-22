@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.mixer

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
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.withSongLine
import com.pocketstudio.model.withSongSmoothed
import com.pocketstudio.model.withStepLine
import com.pocketstudio.model.withStepsSmoothed
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.pillColors

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
)

/** The lane modes and their names (read when drawn, in the interface's language). */
private val MODES get() = listOf(AutomationLane.LOOP to tr("Boucle"), AutomationLane.PER_SCENE to tr("Par scène"), AutomationLane.SONG to tr("Morceau"))

private fun List<StripInfo>.nameOf(ch: Int) = firstOrNull { it.ch == ch }?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) } ?: tr("Sortie")

/** What a target is called on screen. */
fun automationLabel(t: AutoTarget, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo> = BASIC_STRIPS): String {
    fun channelName(ch: Int) = strips.nameOf(ch)
    return when (t.kind) {
    AutoTarget.VOLUME -> tr("Volume · %s", channelName(t.ch))
    AutoTarget.PAN -> tr("Panoramique · %s", channelName(t.ch))
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

/** Drawing tools of a lane. */
private const val TOOL_DRAW = 0
private const val TOOL_LINE = 1
private val ZOOMS = listOf(1, 2, 4, 8)

@Composable
fun AutomationCard(
    ui: AutomationUi, channels: List<Channel>, master: List<Insert>, actions: AutomationActions,
    strips: List<StripInfo> = BASIC_STRIPS, modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    NeonCard(accent = Neon.Amber, modifier = modifier.testTag("automation")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Automation"), Modifier.weight(1f), color = Neon.Amber, help = Glossary.AUTOMATION)
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
        )
    }
}

/** One lane: its name and what it does, its kind (loop, scene, song), its tools and its curve. */
@Composable
private fun LaneBlock(
    i: Int, lane: AutomationLane, ui: AutomationUi, channels: List<Channel>, master: List<Insert>,
    strips: List<StripInfo>, actions: AutomationActions,
) {
    val focus = remember { BringIntoViewRequester() }
    LaunchedEffect(ui.focusLane) { if (ui.focusLane == i) focus.bringIntoView() }
    val lit = ui.focusLane == i
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().bringIntoViewRequester(focus)
            .then(if (lit) Modifier.border(2.dp, Neon.Amber, shape).background(Neon.Amber.copy(alpha = 0.06f), shape).padding(8.dp) else Modifier.padding(top = 4.dp))
            .testTag("auto_lane_$i"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(automationLabel(lane.target, channels, master, strips), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(targetHint(lane.target, channels, master), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2)
            }
            TextButton(onClick = { actions.clear(i) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("Vider")) }
            IconButton(onClick = { actions.remove(i) }) { Icon(Icons.Filled.Delete, contentDescription = tr("Supprimer")) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MODES.forEach { (mode, label) ->
                FilterChip(
                    selected = lane.mode == mode, onClick = { actions.setMode(i, mode) }, label = { Text(label) },
                    modifier = Modifier.testTag("auto_mode_${i}_$mode"),
                )
            }
        }
        val song = lane.mode == AutomationLane.SONG
        val perBar = lane.pointsPerBar(ui.stepsPerBar / 4)
        val bars = maxOf(ui.songBars, 8)
        val count = if (song) bars * perBar else ui.steps.coerceIn(1, AutomationLane.STEPS)
        Text(
            when {
                !song && lane.mode == AutomationLane.PER_SCENE -> tr("Pas de la scène %s (chaque scène a les siens).", com.pocketstudio.model.Scene.NAMES.getOrElse(ui.scene) { "?" })
                !song -> tr("La même courbe à chaque tour de boucle (%d pas).", count)
                ui.songBars == 0 -> tr("Morceau vide : la courbe suit les mesures depuis le début de la lecture.")
                lane.songPerBeat -> tr("Une valeur par temps sur tout le morceau (%d mesures), écrite depuis la page Live.", bars)
                else -> tr("Une valeur par mesure sur tout le morceau (%d mesures) ; elle glisse d'un point à l'autre.", bars)
            },
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        var tool by rememberSaveable(i) { mutableIntStateOf(TOOL_DRAW) }
        var zoom by rememberSaveable(i, song) { mutableIntStateOf(if (song && count > 32) 2 else 1) }
        var start by rememberSaveable(i, song) { mutableIntStateOf(0) }
        val visible = (count + zoom - 1) / zoom
        val first = start.coerceIn(0, (count - visible).coerceAtLeast(0))
        // Where the playhead is, in points of this curve (bars, beats or steps); read when drawn.
        val unitSteps = if (song) (if (lane.songPerBeat) 4 else ui.stepsPerBar.coerceAtLeast(1)) else 1
        val playhead: () -> Float = remember(ui.playStep, ui.playSongStep, ui.currentStep, ui.songBar, song, unitSteps, ui.stepsPerBar) {
            if (song) {
                val f = ui.playSongStep
                if (f != null) { { f().let { s -> if (s < 0) -1f else s.toFloat() / unitSteps } } }
                else { { if (ui.songBar < 0) -1f else ui.songBar * ui.stepsPerBar.toFloat() / unitSteps } }
            } else {
                val f = ui.playStep
                if (f != null) { { f().toFloat() } } else { { ui.currentStep.toFloat() } }
            }
        }
        // Zoomed in while playing: the window follows the playhead.
        LaunchedEffect(zoom, count, visible) {
            if (zoom > 1) snapshotFlow { playhead() }.collect { pos ->
                if (pos >= 0 && (pos < start || pos >= start + visible)) start = (pos.toInt() / visible * visible).coerceIn(0, (count - visible).coerceAtLeast(0))
            }
        }
        LaneTools(
            i, tool, { tool = it }, zoom,
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
        val values = if (song) lane.songValues else lane.valuesFor(ui.scene)
        val curve = if (song) remember(lane, count) { lane.songCurve(count) } else emptyList()
        CurveEditor(
            tag = if (song) "song_lane_${lane.target.pack()}" else "lane_${lane.target.pack()}",
            count = count, first = first, visible = visible, song = song, values = values, curve = curve,
            beat = if (song) perBar else 4, bar = if (song) perBar * 4 else ui.stepsPerBar,
            playhead = playhead, line = tool == TOOL_LINE,
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
        )
    }
}

/** Draw or line, smooth, zoom and the window's place. */
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
    // The way to draw said once (the first lane), and whenever the ramp tool is on.
    if (tool == TOOL_LINE || i == 0) Text(
        if (tool == TOOL_LINE) tr("Ligne : touche le début puis la fin (ou glisse de l'un à l'autre) ; une rampe bien droite les relie.")
        else tr("Dessine au doigt ; deux touches rapides effacent un point. « Lisser » adoucit un trait tremblé, « Annuler » le défait."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
}

/**
 * A curve to draw on: [count] points ([visible] of them shown from [first]), a bar each for a
 * loop's steps or a line through a song's points, big handles, the playhead read only when drawn.
 * [line]: two touches (or a slide) draw a straight ramp.
 */
@Composable
private fun CurveEditor(
    tag: String, count: Int, first: Int, visible: Int, song: Boolean, values: List<Float?>, curve: List<Float>,
    beat: Int, bar: Int, playhead: () -> Float, line: Boolean,
    onPoint: (Int, Float?) -> Unit, onLine: (Int, Float, Int, Float) -> Unit,
) {
    val point by rememberUpdatedState(onPoint)
    val ramp by rememberUpdatedState(onLine)
    val from by rememberUpdatedState(first)
    val shown by rememberUpdatedState(visible)
    val total by rememberUpdatedState(count)
    var anchor by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    var dragTo by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    val color = Neon.Amber
    val ghost = Neon.Amber.copy(alpha = 0.25f)
    val grid = Neon.Outline
    val head = Neon.Acid
    val back = Neon.SurfaceHigh.copy(alpha = 0.5f)
    val ramping = Neon.Cyan
    fun unitAt(x: Float, w: Float) = (from + (x / w * shown).toInt()).coerceIn(0, total - 1)
    fun valueAt(y: Float, h: Float) = (1f - y / h).coerceIn(0f, 1f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(136.dp)
            .background(back, RoundedCornerShape(8.dp))
            .testTag(tag)
            .pointerInput(line) {
                if (line) {
                    detectTapGestures { o ->
                        val u = unitAt(o.x, size.width.toFloat()) to valueAt(o.y, size.height.toFloat())
                        val a = anchor
                        if (a == null) anchor = u else { anchor = null; ramp(a.first, a.second, u.first, u.second) }
                    }
                } else {
                    detectTapGestures(
                        onTap = { o -> point(unitAt(o.x, size.width.toFloat()), valueAt(o.y, size.height.toFloat())) },
                        onDoubleTap = { o -> point(unitAt(o.x, size.width.toFloat()), null) },
                    )
                }
            }
            .pointerInput(line) {
                var last: Pair<Int, Float>? = null
                detectDragGestures(
                    onDragStart = { o ->
                        val u = unitAt(o.x, size.width.toFloat()) to valueAt(o.y, size.height.toFloat())
                        if (line) { anchor = u; dragTo = u } else { point(u.first, u.second); last = u }
                    },
                    onDragEnd = {
                        val a = anchor
                        val b = dragTo
                        if (line && a != null && b != null) ramp(a.first, a.second, b.first, b.second)
                        if (line) { anchor = null; dragTo = null }
                        last = null
                    },
                    onDragCancel = { anchor = null; dragTo = null; last = null },
                ) { change, _ ->
                    val u = unitAt(change.position.x, size.width.toFloat()) to valueAt(change.position.y, size.height.toFloat())
                    if (line) { dragTo = u; return@detectDragGestures }
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
        // The ramp being drawn: its start, and the line to the finger.
        anchor?.let { (u, v) ->
            val a = Offset(xOf(u.toFloat()), size.height - v * size.height)
            drawCircle(ramping, radius = handle + 2.dp.toPx(), center = a, style = Stroke(2.dp.toPx()))
            dragTo?.let { (u2, v2) ->
                drawLine(ramping, a, Offset(xOf(u2.toFloat()), size.height - v2 * size.height), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
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

/** « Que veux-tu faire bouger ? »: by track, the most useful settings first, each with what it does. */
@Composable
private fun TargetPicker(
    used: Set<AutoTarget>, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo>,
    pick: (AutoTarget) -> Unit, addFilter: (Int) -> Unit, dismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 10.dp).testTag("automation_picker"),
        title = { Text(tr("Que veux-tu faire bouger ?")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) { TargetPickerContent(used, channels, master, strips, pick, addFilter) }
        },
        confirmButton = { TextButton(onClick = dismiss, modifier = Modifier.testTag("automation_picker_close")) { Text(tr("Fermer")) } },
    )
}

/** The picker's list: each track's settings, the master's, the shared buses'. */
@Composable
fun TargetPickerContent(
    used: Set<AutoTarget>, channels: List<Channel>, master: List<Insert>, strips: List<StripInfo>,
    pick: (AutoTarget) -> Unit, addFilter: (Int) -> Unit,
) {
    val all = remember(channels, master, strips, used) { automationTargets(channels, master, strips).filter { it !in used } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Choisis la piste, puis le réglage : les plus utiles d'abord (filtre, volume, réverbe)."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        strips.forEach { st ->
            val targets = all.filter { it.kind != AutoTarget.BUS_PARAM && it.kind != AutoTarget.LIVE && it.ch == st.ch }
            val c = channels.getOrNull(st.ch)
            val canAddFilter = st.ch <= AutoTarget.MAX_CH && c != null &&
                c.inserts.none { it.effect == EffectType.FILTER } && c.inserts.any { it.effect == EffectType.NONE }
            PickerGroup(com.pocketstudio.model.DefaultNames.shown(st.name), st.color ?: Neon.Cyan, "pick_group_${st.ch}", targets, channels, master, strips, pick,
                filterOffer = if (canAddFilter) ({ addFilter(st.ch) }) else null, filterTag = "pick_filter_${st.ch}")
        }
        PickerGroup(tr("Sortie générale"), Neon.Cyan, "pick_group_master", all.filter { it.kind == AutoTarget.INSERT_PARAM && it.ch == MASTER_CHANNEL }, channels, master, strips, pick)
        PickerGroup(tr("Réverbe et écho communs"), Neon.Magenta, "pick_group_buses", all.filter { it.kind == AutoTarget.BUS_PARAM }, channels, master, strips, pick)
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
