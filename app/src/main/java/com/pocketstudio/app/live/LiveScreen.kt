package com.pocketstudio.app.live

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.mixer.QuickFxActions
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsCard
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.model.LiveFxSettings
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.cellHasContent
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.liveColumns
import com.pocketstudio.model.livePosition
import com.pocketstudio.model.liveQuantumSteps
import com.pocketstudio.model.sceneLabel
import com.pocketstudio.model.slotLoopSteps
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.trackColor
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.fadingVerticalEdges

/** The drums' colour: an indigo no instrument track can have (TrackColors), so the columns never look alike. */
val DRUMS_COLOR = Color(0xFF7986CB)

/** One column of the live grid: a track, its colour and its mixer channel. */
data class LiveColumn(val key: String, val name: String, val color: Color, val channel: Int)

/** The grid's columns in order (drums, acid, instrument tracks, audio tracks, remix). */
fun liveColumnsOf(p: Project): List<LiveColumn> = p.liveColumns.mapNotNull { key ->
    when {
        key == SongPart.DRUMS -> LiveColumn(key, tr("Batterie"), DRUMS_COLOR, 0)
        key == SongPart.ACID -> LiveColumn(key, tr("Acide"), Color(0xFF81C784), 1)
        key == SongPart.REMIX -> LiveColumn(key, tr("Remix"), Color(0xFF90A4AE), 3)
        key.startsWith("inst:") -> p.instrumentTracks.firstOrNull { SongPart.inst(it.id) == key }?.let {
            LiveColumn(key, DefaultNames.shown(it.name), trackColor(it.color), InstrumentTrack.mixerChannel(it.id))
        }
        else -> p.audioTracks.firstOrNull { SongPart.audio(it.id) == key }?.let {
            LiveColumn(key, DefaultNames.shown(it.name), trackColor(it.color), AudioTrack.mixerChannel(it.id))
        }
    }
}

/**
 * What the Live page shows: the project, the grid as the engine plays it ([grid] null: stopped),
 * the transport [clock] in steps, the tracks muted with M, the 303 settings and returns moved here.
 */
class LiveScreenUi(
    val project: Project,
    val grid: LiveState? = null,
    clock: Int = -1,
    val playing: Boolean = false,
    val muted: Set<String> = emptySet(),
    val acid: Map<Int, Float> = emptyMap(),
    val returns: List<Float> = listOf(0.5f, 0.5f),
    /** The ● (« Enregistrer la performance ») and where the take goes: after the song or in its place. */
    val take: TakeUi = TakeUi.Ready,
    val takeAppend: Boolean = false,
    /** Mixer channels whose « Hachoir de voie » is held. */
    val chops: Set<Int> = emptySet(),
    /** « Annuler » of the « Morceau écrit » line still undoes only the take (nothing else changed since). */
    val takeUndoable: Boolean = true,
    /**
     * The transport step (about 30 times a second while playing), read only where it is shown (the
     * counter, the playing pads' pies): the page itself does not recompose with it. Default: [clock].
     */
    val clockOf: () -> Int = { clock },
    /** The output spectrum (dB per band, null: not measured), read only where drawn: faintly behind the pads XY. */
    val spectrum: () -> FloatArray? = { null },
    /** A light vibration on each beat while playing (Réglages, off by default). */
    val beatHaptics: Boolean = false,
    /** Pad modes whose sound the engine plays besides the first ones (Wobble, Répéter: see [xyModesFor]). */
    val extraModes: Set<XyMode> = emptySet(),
    /** What the whole mix's live effects are doing (held, locked, the tracks' kills, a Drop armed). */
    fx: LiveFxUi = LiveFxUi(),
    /**
     * The same, read only where it is shown ([fx]): the DJ filter and the Montée move it at finger
     * speed, and only the controls showing them recompose, not the page. Default: [fx].
     */
    val fxOf: () -> LiveFxUi = { fx },
) {
    /** What the effects are doing now: read in the small composables that show it (see [fxOf]). */
    val fx: LiveFxUi get() = fxOf()

    /** The project has a 303 line (a note in some scene): else the 303 page has nothing to play. */
    val hasAcid: Boolean = project.allScenes.indices.any { project.cellHasContent(SongPart.ACID, it) }
}

/** Every gesture of the Live page goes to the ViewModel (the recording of a performance follows them). */
class LiveActions(
    val back: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val setQuantum: (Int) -> Unit = {},
    val launchPad: (key: String, scene: Int) -> Unit = { _, _ -> },
    val launchRow: (Int) -> Unit = {},
    val stopTrack: (String) -> Unit = {},
    val stopAll: () -> Unit = {},
    val toggleMute: (String) -> Unit = {},
    val performance: (param: Int, value: Float) -> Unit = { _, _ -> },
    val setReturn: (bus: Int, value: Float) -> Unit = { _, _ -> },
    val fx: QuickFxActions = QuickFxActions(),
    val setSend: (ch: Int, bus: Int, value: Float) -> Unit = { _, _, _ -> },
    val openEffects: (ch: Int) -> Unit = {},
    val setAcid: (param: Int, value: Float) -> Unit = { _, _ -> },
    val holdAcid: (param: Int, held: Boolean) -> Unit = { _, _ -> },
    val resetAcid: (param: Int) -> Unit = {},
    /** ● / ■ Fin. */
    val record: () -> Unit = {},
    val setTakeAppend: (Boolean) -> Unit = {},
    val undoTake: () -> Unit = {},
    val listenTake: () -> Unit = {},
    val dismissTake: () -> Unit = {},
    val trackChop: (ch: Int, on: Boolean) -> Unit = { _, _ -> },
    /**
     * « Tout remettre »: every live effect back to rest in one engine command (its short fades).
     * Null: the page does it with the controls above ([resetLiveEffects]).
     */
    val resetAll: (() -> Unit)? = null,
    /** A held effect of the whole mix (AutoTarget.LIVE_TAPE_STOP, _CHOP, _BACKSPIN, _REVERSE, _HALF_TIME, _ECHO_OUT, _FREEZE, the kills). */
    val hold: (param: Int, on: Boolean) -> Unit = { _, _ -> },
    /** The roll pads: the whole mix repeats a division while held (null: let go). */
    val roll: (LiveDivision?) -> Unit = {},
    /** A band (0 bass, 1 mids, 2 highs) of track strip [ch] cut while on (the whole mix: [hold] with a kill). */
    val kill: (band: Int, on: Boolean, ch: Int) -> Unit = { _, _, _ -> },
    /** A dub throw: strip [ch] (Mixer.MASTER: the whole mix) into the reverb (0) or the echo (1) for a beat. */
    val throwFx: (bus: Int, ch: Int) -> Unit = { _, _ -> },
    /** A noise riser over a few bars from the next bar line ([down]: falling). */
    val riser: (down: Boolean) -> Unit = {},
    /** « Montée » (0 at rest .. 1). */
    val buildUp: (Float) -> Unit = {},
    /** « Drop »: everything back at rest on the next bar line. */
    val drop: () -> Unit = {},
    /** A whole-mix effect kept on (or its position kept) when let go, or no longer. */
    val setLock: (param: Int, locked: Boolean) -> Unit = { _, _ -> },
    /** The effects' settings (start grid, what a release does, lengths), kept with the project. */
    val setFxSettings: (LiveFxSettings) -> Unit = {},
    /** An effect's setting on a track (the Wobble pad). */
    val setInsertParam: (ch: Int, slot: Int, param: Int, value: Float) -> Unit = { _, _, _, _ -> },
)

/** The bottom panel's pages. */
enum class LivePanelTab { Effets, Acide, Pads }

/**
 * The Live page, full screen: a bar (back to Morceau, ▶/■, launch quantum, bar.beat counter),
 * the grid (a row per scene, a column per track) and a panel with the effects, the 303 and the
 * live pads. Phone upright: the grid above the panel; a wide screen upright: effects and 303
 * side by side; lying down: the grid on the left, the panel on the right.
 */
@Composable
fun LiveScreen(
    ui: LiveScreenUi,
    actions: LiveActions,
    pads: LivePadsUi? = null,
    padsActions: LivePadsActions = LivePadsActions(),
    initialTab: LivePanelTab = LivePanelTab.Effets,
) {
    val columns = liveColumnsOf(ui.project)
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var panelOpen by rememberSaveable { mutableStateOf(true) }
    // Leaving during a take: said first (the take would go on unseen, then overwrite what was
    // done elsewhere); « Quitter » ends it (written, or dropped while only armed).
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    val leave: () -> Unit = {
        if (ui.take.running) confirmLeave = true else actions.back()
    }
    if (androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current != null) {
        androidx.activity.compose.BackHandler(onBack = leave)
    }
    if (confirmLeave && ui.take.running) {
        LeaveDuringTake(
            armed = ui.take is TakeUi.Armed,
            leave = { confirmLeave = false; actions.record(); actions.back() },
            stay = { confirmLeave = false },
        )
    }
    // The take ended meanwhile (the song full): nothing left to ask.
    LaunchedEffect(ui.take.running) { if (!ui.take.running) confirmLeave = false }
    // The lock, the pad modes, what the lock keeps: shared by the effects panel's pages, kept
    // through a rotation. Leaving the page lets go of what the lock kept (nothing stays on unseen):
    // the ViewModel does it as the page closes (RhythmViewModel.leaveLive), not a rotation.
    val fx = rememberSaveable(saver = LiveFxState.Saver) { LiveFxState() }
    if (ui.beatHaptics && ui.playing) BeatTicks(ui.clockOf)
    Column(Modifier.fillMaxSize().background(Neon.Background).testTag("live_screen")) {
        LiveTopBar(ui, actions, leave)
        TakeBanner(ui, actions)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val landscape = maxWidth > maxHeight
            val wide = maxWidth >= 560.dp
            val pad = if (maxWidth >= 560.dp && maxHeight >= 560.dp) 80.dp else 64.dp
            val header = HEADER_HEIGHT
            val panelWidth = if (maxWidth >= 900.dp) 420.dp else 360.dp
            if (landscape) {
                Row(Modifier.fillMaxSize()) {
                    LiveGrid(ui, columns, actions, pad, Modifier.weight(1f).fillMaxHeight())
                    LivePanel(
                        ui, columns, actions, fx, pads, padsActions, sideBySide = false, open = true, onToggle = null, tab = tab, onTab = { tab = it },
                        modifier = Modifier.width(panelWidth).fillMaxHeight(),
                    )
                }
            } else {
                // The grid always keeps room for three rows; on a phone the panel takes what the
                // scenes leave (more for the effects with few scenes), on a wide screen its full height.
                val gridMin = pad * 3 + header + 52.dp + 16.dp
                val gridAll = (pad + 6.dp) * ui.project.allScenes.size + header + 52.dp + 16.dp
                val room = (maxHeight - gridMin).coerceAtLeast(52.dp)
                val panelHeight = when {
                    !panelOpen -> 52.dp
                    wide -> minOf(430.dp, room)
                    else -> minOf((maxHeight - gridAll).coerceIn(340.dp, 470.dp), room)
                }
                Column(Modifier.fillMaxSize()) {
                    LiveGrid(ui, columns, actions, pad, Modifier.fillMaxWidth().weight(1f))
                    LivePanel(
                        ui, columns, actions, fx, pads, padsActions, sideBySide = wide, open = panelOpen, onToggle = { panelOpen = !panelOpen },
                        tab = tab, onTab = { tab = it; panelOpen = true }, modifier = Modifier.fillMaxWidth().height(panelHeight),
                    )
                }
            }
        }
    }
}

/** « Finir la prise ? »: leaving the Live page during a take ends it (or drops it while armed). */
@Composable
private fun LeaveDuringTake(armed: Boolean, leave: () -> Unit, stay: () -> Unit) {
    AlertDialog(
        onDismissRequest = stay,
        title = { Text(if (armed) tr("Annuler la prise ?") else tr("Finir la prise ?")) },
        text = {
            Text(
                if (armed) tr("La prise n'a pas encore commencé : quitter la page Live l'annule.")
                else tr("Quitter la page Live arrête la prise et l'écrit dans le morceau."),
            )
        },
        confirmButton = { TextButton(onClick = leave, modifier = Modifier.testTag("live_leave_confirm")) { Text(tr("Quitter")) } },
        dismissButton = { TextButton(onClick = stay, modifier = Modifier.testTag("live_leave_stay")) { Text(tr("Rester")) } },
        modifier = Modifier.testTag("live_leave_dialog"),
    )
}

/** ← Morceau, ▶/■, « Lancer à », the counter (bar.beat, its ring filling up to the next launch) and ●. */
@Composable
private fun LiveTopBar(ui: LiveScreenUi, actions: LiveActions, leave: () -> Unit) {
    val p = ui.project
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(Neon.Surface).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TextButton(onClick = leave, modifier = Modifier.testTag("live_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(tr("Morceau"), maxLines = 1)
        }
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (ui.playing) Neon.Acid else Neon.Cyan)
                .clickable(onClick = actions.togglePlay).testTag("live_play")
                .semantics { contentDescription = if (ui.playing) tr("Arrêter") else tr("Jouer") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent)
        }
        // « Lancer à » : touching it goes 1 → 2 → 4 bars.
        val bars = p.liveQuantum
        val next = LiveSlots.QUANTA[(LiveSlots.QUANTA.indexOf(bars) + 1).mod(LiveSlots.QUANTA.size)]
        Column(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh)
                .clickable { actions.setQuantum(next) }.padding(horizontal = 10.dp, vertical = 4.dp).testTag("live_quantum"),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(tr("Lancer à"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
            Text(if (bars == 1) tr("1 mesure") else tr("%d mesures", bars), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        Counter(ui)
        RecordButton(ui.take, actions.record)
    }
}

/**
 * ●: ready (a red dot), armed (a blinking ring until the take starts on the launch line),
 * recording (■ Fin), writing.
 */
@Composable
private fun RecordButton(take: TakeUi, record: () -> Unit) {
    val blink = rememberBlink(take is TakeUi.Armed, low = 0.25f, periodMs = 400)
    val label = when (take) {
        is TakeUi.Armed -> tr("Armé")
        is TakeUi.Recording -> tr("Fin")
        is TakeUi.Writing -> "…"
        else -> "REC"
    }
    val description = when (take) {
        is TakeUi.Armed -> tr("Annuler la prise")
        is TakeUi.Recording -> tr("Finir la prise et écrire le morceau")
        else -> tr("Enregistrer la performance")
    }
    Column(
        Modifier.width(56.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .background(if (take is TakeUi.Recording) Neon.Red.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(enabled = take !is TakeUi.Writing, onClick = record)
            .testTag("live_record").semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when (take) {
                is TakeUi.Recording -> Box(Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)).background(Neon.Red))
                is TakeUi.Armed -> Box(Modifier.size(24.dp)) {
                    Box(Modifier.matchParentSize().graphicsLayer { alpha = blink.value }.border(3.dp, Neon.Red, CircleShape))
                    Box(Modifier.matchParentSize().padding(5.dp).clip(CircleShape).background(Neon.Red.copy(alpha = 0.6f)))
                }
                else -> Box(Modifier.size(22.dp).clip(CircleShape).background(Neon.Red))
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Neon.TextPrimary, maxLines = 1)
    }
}

/**
 * Under the bar, what the ● does: where the take goes (in place of the song or after it), the
 * countdown, « REC 0:42 · mesure 21 », then « Morceau écrit » with Annuler and Écouter.
 */
@Composable
private fun TakeBanner(ui: LiveScreenUi, actions: LiveActions) {
    val take = ui.take
    val parts = ui.project.song.size
    if (take is TakeUi.Ready && parts == 0) return
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(if (take is TakeUi.Recording) Neon.Red.copy(alpha = 0.16f) else Neon.SurfaceHigh.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 4.dp).testTag("live_take"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val style = MaterialTheme.typography.bodyMedium
        when (take) {
            is TakeUi.Recording -> {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Neon.Red))
                Text(
                    tr("REC %1\$d:%2\$02d · mesure %3\$d", take.seconds / 60, take.seconds % 60, take.bar),
                    Modifier.weight(1f).testTag("live_take_clock"), style = MonoStyle, maxLines = 1,
                )
                TextButton(onClick = actions.record, modifier = Modifier.testTag("live_take_end")) { Text(tr("■ Fin"), maxLines = 1) }
            }
            is TakeUi.Writing -> Text(tr("Écriture du morceau…"), Modifier.weight(1f), style = style)
            is TakeUi.Done -> {
                Text(
                    tr("Morceau écrit : %1\$d parties, %2\$d réglages", take.parts, take.settings),
                    Modifier.weight(1f).testTag("live_take_done"), style = style, maxLines = 2,
                )
                // Annuler undoes the take only: once something else changed, it is gone (↶ in the top bar then).
                if (ui.takeUndoable) {
                    TextButton(onClick = actions.undoTake, modifier = Modifier.heightIn(min = 48.dp).testTag("live_take_undo")) { Text(tr("Annuler"), maxLines = 1) }
                }
                TextButton(onClick = actions.listenTake, modifier = Modifier.heightIn(min = 48.dp).testTag("live_take_listen")) { Text(tr("Écouter"), maxLines = 1) }
                IconButton(onClick = actions.dismissTake, modifier = Modifier.testTag("live_take_dismiss")) {
                    Icon(Icons.Filled.Close, contentDescription = tr("Fermer"))
                }
            }
            else -> {
                // Ready or armed: where the take goes, while it can still change.
                val armed = take is TakeUi.Armed
                Text(
                    if (armed) tr("Armé : la prise part à la prochaine ligne de lancement.") else tr("La prise :"),
                    Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2,
                )
                if (parts > 0) {
                    Row(Modifier.weight(1f, fill = false).fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TakeChoice(tr("Remplacer le morceau"), !ui.takeAppend, "live_take_replace") { actions.setTakeAppend(false) }
                        TakeChoice(tr("Ajouter à la suite"), ui.takeAppend, "live_take_append") { actions.setTakeAppend(true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TakeChoice(label: String, selected: Boolean, tag: String, choose: () -> Unit) {
    Box(
        Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (selected) Neon.Red.copy(alpha = 0.85f) else Neon.Surface)
            .clickable(onClick = choose).padding(horizontal = 12.dp).testTag(tag)
            .semantics { if (selected) contentDescription = "$label ✓" },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Neon.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun Counter(ui: LiveScreenUi) {
    val p = ui.project
    // The only part of the bar that follows the clock (it alone recomposes, 30 times a second).
    val step = ui.clockOf()
    val q = p.liveQuantumSteps
    val waiting = ui.grid?.hasPending == true
    val ring = if (step < 0) 0f else (step % q) / q.toFloat()
    val text = if (step < 0) "–" else "${step / p.stepsPerBar + 1}.${step % p.stepsPerBar / 4 + 1}"
    val accent = if (waiting) Neon.Cyan else Neon.Outline
    val track = Neon.SurfaceHigh
    Box(Modifier.size(48.dp).testTag("live_counter"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp)) {
            val stroke = 4.dp.toPx()
            drawArc(track, 0f, 360f, false, style = Stroke(stroke), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
            drawArc(accent, -90f, 360f * ring, false, style = Stroke(stroke), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
        }
        Text(text, style = MonoStyle, color = Neon.TextPrimary)
    }
}

/**
 * The grid: the scene buttons stay on the left, the track columns scroll sideways together; a
 * header (name, colour, M) above and the ■ of each track below.
 */
@Composable
private fun LiveGrid(ui: LiveScreenUi, columns: List<LiveColumn>, actions: LiveActions, pad: Dp, modifier: Modifier) {
    val p = ui.project
    val scenes = p.allScenes.indices.toList()
    val h = rememberScrollState()
    val v = rememberScrollState()
    val sceneWidth = if (pad > 64.dp) 128.dp else 104.dp
    val gap = 6.dp
    // Blinks only while a launch or a stop waits, and only redraws (the cells read it when drawn).
    val blink = rememberBlink(ui.playing && ui.grid?.hasPending == true)
    Column(modifier.padding(horizontal = 8.dp, vertical = 6.dp).testTag("live_grid"), verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(Modifier.width(sceneWidth).height(HEADER_HEIGHT), contentAlignment = Alignment.BottomStart) {
                Text(tr("Scènes"), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
            }
            ScrolledRow(h, gap) { columns.forEach { c -> ColumnHeader(c, c.key in ui.muted, pad) { actions.toggleMute(c.key) } } }
        }
        Column(Modifier.weight(1f, fill = false).fadingVerticalEdges(v).verticalScroll(v), verticalArrangement = Arrangement.spacedBy(gap)) {
            scenes.forEach { scene ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    SceneButton(ui, scene, sceneWidth, pad, blink) { actions.launchRow(scene) }
                    ScrolledRow(h, gap) {
                        columns.forEach { c ->
                            val slot = LiveSlots.of(c.key) ?: return@forEach
                            val state = ui.grid?.slot(slot)
                            val playing = ui.playing && state?.playing == scene
                            val start = state?.start ?: 0
                            val loop = if (playing) p.slotLoopSteps(c.key, scene) else 0
                            LivePadCell(
                                c, rowName(p, scene), pad, p.cellHasContent(c.key, scene), playing, pending = ui.playing && state?.pending == scene,
                                position = { livePosition(ui.clockOf(), start, loop) }, blink,
                                waitLeft = { launchWaitLeft(ui.clockOf(), p.liveQuantumSteps) },
                                tag = "live_pad_${c.key}_$scene",
                            ) { actions.launchPad(c.key, scene) }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            StopButton(tr("■ Tout"), sceneWidth, waiting = false, blink, "live_stop_all", actions.stopAll)
            ScrolledRow(h, gap) {
                columns.forEach { c ->
                    val slot = LiveSlots.of(c.key) ?: return@forEach
                    val state = ui.grid?.slot(slot)
                    StopButton("■", pad, waiting = ui.playing && state?.pending == -1, blink, "live_stop_${c.key}") { actions.stopTrack(c.key) }
                }
            }
        }
    }
}

@Composable
private fun ScrolledRow(state: ScrollState, gap: Dp, content: @Composable () -> Unit) {
    Row(Modifier.horizontalScroll(state), horizontalArrangement = Arrangement.spacedBy(gap)) { content() }
}

@Composable
private fun ColumnHeader(c: LiveColumn, muted: Boolean, pad: Dp, toggleMute: () -> Unit) {
    Column(Modifier.width(pad).height(HEADER_HEIGHT).clip(RoundedCornerShape(10.dp)).background(Neon.Surface)) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(c.color))
        Text(
            c.name, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
        // M: silent at once (not written in the project). A full 48 dp under the name: a finger
        // that misses it does not land on a pad (which would launch a scene, into the take).
        Box(
            Modifier.fillMaxWidth().height(48.dp).clickable(onClick = toggleMute).testTag("live_mute_${c.key}")
                .semantics { contentDescription = if (muted) tr("Remettre le son de %s", c.name) else tr("Couper le son de %s", c.name) }
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp).clip(RoundedCornerShape(8.dp))
                .background(if (muted) Neon.Amber else Neon.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text("M", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (muted) Color.Black else Neon.TextSecondary)
        }
    }
}

@Composable
private fun SceneButton(ui: LiveScreenUi, scene: Int, width: Dp, height: Dp, blink: State<Float>, launch: () -> Unit) {
    val p = ui.project
    val current = ui.playing && ui.grid?.row == scene
    val waiting = ui.playing && ui.grid?.pendingRow == scene
    val label = p.sceneLabel(scene).takeIf { it.isNotBlank() }?.let(Scene::shownLabel)
    Row(
        Modifier.width(width).height(height).clip(RoundedCornerShape(12.dp))
            .background(if (current) Neon.Cyan.copy(alpha = 0.22f) else Neon.Surface)
            .let { m ->
                val cyan = Neon.Cyan
                if (waiting) m.blinkingBorder(2.dp, 12.dp) { cyan.copy(alpha = blink.value) }
                else m.border(2.dp, if (current) Neon.Cyan else Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            }
            .clickable(onClick = launch).padding(horizontal = 8.dp).testTag("live_row_$scene"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("▶", color = if (current) Neon.Cyan else Neon.TextSecondary, style = MaterialTheme.typography.titleMedium)
        Column {
            Text(Scene.NAMES.getOrElse(scene) { "?" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (label != null) Text(label, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A pad: empty, with something to play, waiting for its launch line (blinks), or playing (filled, its position as a pie). */
@Composable
private fun LivePadCell(
    c: LiveColumn,
    row: String,
    size: Dp,
    hasContent: Boolean,
    playing: Boolean,
    pending: Boolean,
    position: () -> Float,
    blink: State<Float>,
    waitLeft: () -> Float,
    tag: String,
    launch: () -> Unit,
) {
    SideEffect { livePadCompositions++ }
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(12.dp)
    val fill = when {
        playing -> c.color
        hasContent -> c.color.copy(alpha = 0.28f)
        else -> Neon.Surface
    }
    Box(
        Modifier.size(size).clip(shape).background(fill)
            .let { m ->
                if (pending) m.blinkingBorder(3.dp, 12.dp) { c.color.copy(alpha = blink.value) }
                else m.border(1.dp, Color.White.copy(alpha = if (hasContent) 0.14f else 0.05f), shape)
            }
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                launch()
            }
            .testTag(tag)
            .semantics {
                // TalkBack: the track, the row, and what the pad does now.
                val state = when {
                    playing -> tr("joue")
                    pending -> tr("en attente")
                    hasContent -> tr("prêt")
                    else -> tr("vide")
                }
                contentDescription = "${c.name} · $row · $state"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (playing) {
            val ink = Color.Black.copy(alpha = 0.55f)
            Canvas(Modifier.size(size * 0.42f)) {
                drawCircle(ink.copy(alpha = 0.25f))
                drawArc(ink, -90f, 360f * position(), true)
            }
        } else if (pending) {
            // The ring empties until the launch line: when it is gone, the pad plays.
            Box(Modifier.size(size * 0.62f).countdownRing(waitLeft, c.color), contentAlignment = Alignment.Center) {
                Text("→", Modifier.graphicsLayer { alpha = blink.value }, style = MaterialTheme.typography.titleLarge, color = c.color)
            }
        } else if (hasContent) {
            Text("▶", style = MaterialTheme.typography.titleSmall, color = c.color.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun StopButton(label: String, width: Dp, waiting: Boolean, blink: State<Float>, tag: String, stop: () -> Unit) {
    Box(
        Modifier.width(width).height(48.dp).clip(RoundedCornerShape(10.dp))
            .let { m ->
                val red = Neon.Red
                if (waiting) m.drawBehind { drawRect(red.copy(alpha = 0.25f + 0.35f * blink.value)) } else m.background(Neon.SurfaceHigh)
            }
            .clickable(onClick = stop).testTag(tag)
            .semantics { if (label == "■") contentDescription = tr("Arrêter la piste") },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary, maxLines = 1)
    }
}

/** The panel: Effets | 303 | Pads (on a wide screen upright, effects and 303 side by side), folds to its tabs. */
@Composable
private fun LivePanel(
    ui: LiveScreenUi,
    columns: List<LiveColumn>,
    actions: LiveActions,
    fx: LiveFxState,
    pads: LivePadsUi?,
    padsActions: LivePadsActions,
    sideBySide: Boolean,
    open: Boolean,
    onToggle: (() -> Unit)?,
    tab: LivePanelTab,
    onTab: (LivePanelTab) -> Unit,
    modifier: Modifier,
) {
    val shown = if (sideBySide && tab == LivePanelTab.Acide) LivePanelTab.Effets else tab
    Column(modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Neon.Surface).testTag("live_panel")) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val tabs = if (sideBySide) listOf(LivePanelTab.Effets to tr("Effets et 303"), LivePanelTab.Pads to tr("Pads"))
            else listOf(LivePanelTab.Effets to tr("Effets"), LivePanelTab.Acide to "303", LivePanelTab.Pads to tr("Pads"))
            tabs.forEach { (t, label) ->
                val selected = shown == t
                // No 303 line in the project: its tab stays (it says how to get one) but greyed.
                val idle = t == LivePanelTab.Acide && !ui.hasAcid
                Box(
                    Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (selected) Neon.Cyan else Neon.SurfaceHigh)
                        .clickable { onTab(t) }.padding(horizontal = 16.dp).testTag("live_tab_${t.name}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, color = if (selected) Neon.OnAccent else if (idle) Neon.TextSecondary.copy(alpha = 0.6f) else Neon.TextPrimary,
                        style = MaterialTheme.typography.labelLarge, maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (onToggle != null) {
                IconButton(onClick = onToggle, modifier = Modifier.testTag("live_panel_fold")) {
                    Icon(if (open) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = if (open) tr("Replier") else tr("Déplier"))
                }
            }
        }
        if (!open) return@Column
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp)) {
            when {
                shown == LivePanelTab.Pads -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (pads != null) LivePadsCard(pads, padsActions)
                }
                sideBySide -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveFx(ui, columns, actions, Modifier.weight(1f).fillMaxHeight(), fx)
                    LiveAcid(ui, actions, Modifier.weight(1f).fillMaxHeight())
                }
                shown == LivePanelTab.Acide -> LiveAcid(ui, actions, Modifier.fillMaxSize())
                else -> LiveFx(ui, columns, actions, Modifier.fillMaxSize(), fx)
            }
        }
    }
}

/** The grid's column headers: the track's colour, its name, then a 48 dp M. */
private val HEADER_HEIGHT = 80.dp

/** A row's name for TalkBack: « B · Couplet ». */
private fun rowName(p: Project, scene: Int): String {
    val label = p.sceneLabel(scene).takeIf { it.isNotBlank() }?.let(Scene::shownLabel)
    val name = Scene.NAMES.getOrElse(scene) { "?" }
    return if (label != null) "$name · $label" else name
}

/**
 * A blink between [low] and 1 while [active] (else 1, and no animation runs). Read its value where
 * it is drawn ([blinkingBorder], graphicsLayer): the cells are redrawn, never recomposed, by it.
 */
@Composable
private fun rememberBlink(active: Boolean, low: Float = 0.35f, periodMs: Int = 450): State<Float> {
    if (!active) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "blink").animateFloat(
        low, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Reverse), label = "blink",
    )
}

/** A rounded border whose colour is read when drawn (a blink redraws it without recomposing). */
private fun Modifier.blinkingBorder(width: Dp, radius: Dp, color: () -> Color): Modifier = drawWithContent {
    drawContent()
    val w = width.toPx()
    drawRoundRect(
        color(), topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius((radius.toPx() - w / 2).coerceAtLeast(0f)), style = Stroke(w),
    )
}

/** Steps in a beat (sixteenths). */
private const val STEPS_PER_BEAT = 4

/**
 * A light vibration on each beat while the page plays (an option: it can annoy). It follows the
 * polled clock (about 30 times a second): a feel of the beat, not the engine's timing.
 */
@Composable
private fun BeatTicks(clockOf: () -> Int) {
    val haptics = LocalHapticFeedback.current
    val clock by rememberUpdatedState(clockOf)
    LaunchedEffect(Unit) {
        snapshotFlow { clock().let { if (it < 0) -1 else it / STEPS_PER_BEAT } }
            .distinctUntilChanged()
            .collect { beat -> if (beat >= 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
}

/** Tests: how many times a pad was composed (neither the clock nor a blink may recompose the grid). */
@VisibleForTesting
internal var livePadCompositions = 0
