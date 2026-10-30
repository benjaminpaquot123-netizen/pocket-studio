package com.pocketstudio.app.live

import com.pocketstudio.app.learn.spot
import com.pocketstudio.ui.ScaledAlertDialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.pocketstudio.ui.liveGlow
import com.pocketstudio.ui.trackColor
import com.pocketstudio.ui.fadingEdges
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
        key == SongPart.ACID -> LiveColumn(key, tr("Acid 303"), com.pocketstudio.app.acid.AcidLook.Track, 1)
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
    /**
     * A finger down on a pad ([key], [scene]), a row ([key] null) or a ■ ([scene] -1): true when it
     * launched there and then (the let-go then does nothing); false: the tap launches.
     */
    val press: (key: String?, scene: Int) -> Boolean = { _, _ -> false },
    /** The finger that pressed slid on (the grid scrolls): what its press armed is taken back. */
    val retract: (key: String?) -> Unit = {},
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
    /** « Versions du morceau »: what each take put aside, one touch to bring it back. */
    val restoreVersion: (Int) -> Unit = {},
    val dropVersion: (Int) -> Unit = {},
    /** « Notes »: a key down (track, pitch, velocity) and up (track, pitch); written into a take while it records. */
    val noteOn: (Int, Int, Float) -> Unit = { _, _, _ -> },
    val noteOff: (Int, Int) -> Unit = { _, _ -> },
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
enum class LivePanelTab { Effets, Acide, Pads, Notes }

/**
 * The Live page, full screen: a bar (back to Morceau, ▶/■, launch quantum, bar.beat counter),
 * the grid (a row per scene, a column per track) and a panel with the effects, the 303 and the
 * live pads. Phone upright: the grid above the panel (tracks as rows when their columns would
 * not fit across); a wide screen upright: effects and 303 side by side; lying down: the grid on
 * the left, the panel on the right. « Plein écran » hides the phone's bars and folds the panel
 * (to its tabs upright, to a thin rail lying down): the grid gets the whole screen.
 */
@Composable
fun LiveScreen(
    ui: LiveScreenUi,
    actions: LiveActions,
    pads: LivePadsUi? = null,
    padsActions: LivePadsActions = LivePadsActions(),
    initialTab: LivePanelTab = LivePanelTab.Effets,
    /** Under the page's bar: the position strip (tempo, « mes. x/y »), given by the app. */
    under: (@Composable () -> Unit)? = null,
    /** Phase 36: « Plein écran » on or off (the app hides its bottom bar with the phone's). */
    onImmersive: (Boolean) -> Unit = {},
    /** The panel's page shown (a MIDI controller's knobs and pads follow it). */
    onTab: (LivePanelTab) -> Unit = {},
) {
    val columns = liveColumnsOf(ui.project)
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    LaunchedEffect(tab) { onTab(tab) }
    var panelOpen by rememberSaveable { mutableStateOf(true) }
    var immersive by rememberSaveable { mutableStateOf(false) }
    val toggleFull: () -> Unit = {
        immersive = !immersive
        // Full screen is for the grid: the panel folds (one touch on a tab brings it back).
        if (immersive) panelOpen = false
    }
    HideSystemBars(immersive)
    androidx.compose.runtime.LaunchedEffect(immersive) { onImmersive(immersive) }
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
    var versionsOpen by rememberSaveable { mutableStateOf(false) }
    if (versionsOpen) {
        com.pocketstudio.app.record.SongVersionsSheet(
            ui.project,
            com.pocketstudio.app.record.SongVersionsActions(
                restore = { versionsOpen = false; actions.restoreVersion(it) }, drop = actions.dropVersion, close = { versionsOpen = false },
            ),
        )
    }
    if (ui.beatHaptics && ui.playing) BeatTicks(ui.clockOf)
    BoxWithConstraints(Modifier.fillMaxSize().background(Neon.Background).testTag("live_screen")) {
        // Lying down (or full screen) every row of height counts: a slimmer bar, and the take's
        // line only once ● is touched (it then offers where the take goes).
        val short = immersive || maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            LiveTopBar(ui, actions, leave, slim = short)
            // Full screen is for the grid: the strip gives way there.
            if (under != null && !immersive) under()
            TakeBanner(ui, actions, showReady = !short, openVersions = { versionsOpen = true })
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val landscape = maxWidth > maxHeight
                val wide = maxWidth >= 560.dp
                val panelWidth = if (maxWidth >= 900.dp) 420.dp else 360.dp
                if (landscape) {
                    Row(Modifier.fillMaxSize()) {
                        LiveGrid(ui, columns, actions, Modifier.weight(1f).fillMaxHeight())
                        if (panelOpen) {
                            LivePanel(
                                ui, columns, actions, fx, pads, padsActions, sideBySide = false, open = true, onToggle = { panelOpen = false },
                                tab = tab, onTab = { tab = it }, immersive = immersive, toggleFull = toggleFull, sideways = true,
                                modifier = Modifier.width(panelWidth).fillMaxHeight(),
                            )
                        } else {
                            PanelRail(ui, tab, onTab = { tab = it; panelOpen = true }, immersive, toggleFull, Modifier.width(RAIL_WIDTH).fillMaxHeight())
                        }
                    }
                } else {
                    // The grid keeps room for its header, three rows and the ■ row; the panel takes
                    // what the scenes leave (more for the effects with few scenes), on a wide screen
                    // its full height; folded, only its tabs.
                    // Too short for both (a big text size, a small phone): the open panel keeps
                    // PANEL_MIN (its pad in view) and the grid its header, a row and ■ (it scrolls).
                    val scenes = ui.project.allScenes.size
                    val gridMin = HEADER_HEIGHT + 48.dp + (MIN_CELL_H + 6.dp) * 3 + 24.dp
                    val gridShort = HEADER_COMPACT + 48.dp + (MIN_CELL_H + 6.dp) + 24.dp
                    val gridAll = HEADER_HEIGHT + 48.dp + (64.dp + 6.dp) * scenes + 24.dp
                    val room = (maxHeight - gridMin).coerceAtLeast(PANEL_FOLDED)
                    val panelHeight = when {
                        !panelOpen -> PANEL_FOLDED
                        wide -> minOf(430.dp, room)
                        room < PANEL_MIN -> minOf(maxHeight - gridShort, PANEL_MIN).coerceAtLeast(PANEL_FOLDED)
                        else -> minOf((maxHeight - gridAll).coerceIn(PANEL_MIN, 470.dp), room)
                    }
                    Column(Modifier.fillMaxSize()) {
                        LiveGrid(ui, columns, actions, Modifier.fillMaxWidth().weight(1f))
                        LivePanel(
                            ui, columns, actions, fx, pads, padsActions, sideBySide = wide, open = panelOpen, onToggle = { panelOpen = !panelOpen },
                            tab = tab, onTab = { tab = it; panelOpen = true }, immersive = immersive, toggleFull = toggleFull, sideways = false,
                            modifier = Modifier.fillMaxWidth().height(panelHeight),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full screen: the phone's status and navigation bars hidden while [hidden] (a swipe from the edge
 * shows them for a moment), back as soon as it is off or the page closes.
 */
@Composable
internal fun HideSystemBars(hidden: Boolean) {
    val view = LocalView.current
    DisposableEffect(hidden, view) {
        val window = view.context.findWindow()
        if (!hidden || window == null) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        hiddenBars++
        // Screens can stack (full-screen pads over the Live page): the bars come back with the last one.
        onDispose { if (--hiddenBars == 0) controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/** How many open screens want the system bars hidden (main thread only). */
private var hiddenBars = 0

private tailrec fun android.content.Context.findWindow(): android.view.Window? = when (this) {
    is android.app.Activity -> window
    is android.content.ContextWrapper -> baseContext.findWindow()
    else -> null
}

/** ⛶: the grid on the whole screen, or back. */
@Composable
private fun FullScreenButton(on: Boolean, toggle: () -> Unit) {
    IconButton(onClick = toggle, modifier = Modifier.testTag("live_fullscreen")) {
        Icon(
            if (on) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
            contentDescription = if (on) tr("Quitter le plein écran") else tr("Plein écran"),
            tint = if (on) Neon.Cyan else Neon.TextPrimary,
        )
    }
}

/** Lying down with the panel folded: a thin rail with its tabs (a touch opens the panel there) and ⛶. */
@Composable
private fun PanelRail(ui: LiveScreenUi, tab: LivePanelTab, onTab: (LivePanelTab) -> Unit, immersive: Boolean, toggleFull: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).background(Neon.Surface)
            .padding(vertical = 4.dp).testTag("live_panel_rail"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FullScreenButton(immersive, toggleFull)
        listOf(LivePanelTab.Effets to tr("Effets"), LivePanelTab.Acide to tr("Acid 303"), LivePanelTab.Pads to tr("Pads"), LivePanelTab.Notes to tr("Notes")).forEach { (t, label) ->
            val idle = t == LivePanelTab.Acide && !ui.hasAcid
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (t == tab) Neon.SurfaceHigh else Color.Transparent)
                    .clickable { onTab(t) }.testTag("live_rail_${t.name}")
                    .semantics { contentDescription = tr("Ouvrir %s", label) },
                contentAlignment = Alignment.Center,
            ) {
                // « Acid 303 » on two lines in the 48 dp rail.
                Text(
                    label, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (idle) Neon.TextSecondary.copy(alpha = 0.6f) else if (t == tab) Neon.Cyan else Neon.TextPrimary,
                )
            }
        }
    }
}

/** The folded panel lying down. */
private val RAIL_WIDTH = 56.dp

/** The folded panel upright: its tabs only. */
private val PANEL_FOLDED = 56.dp

/** The open panel upright, at least (the grid scrolls past that). */
private val PANEL_MIN = 300.dp

/** « Finir la prise ? »: leaving the Live page during a take ends it (or drops it while armed). */
@Composable
private fun LeaveDuringTake(armed: Boolean, leave: () -> Unit, stay: () -> Unit) {
    ScaledAlertDialog(
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

/**
 * ← Morceau, ▶/■, « Lancer à », the counter (bar.beat, its ring filling up to the next launch) and ●;
 * [slim] lying down or full screen (48 dp: its buttons keep their size).
 */
@Composable
private fun LiveTopBar(ui: LiveScreenUi, actions: LiveActions, leave: () -> Unit, slim: Boolean = false) = BoxWithConstraints(
    Modifier.fillMaxWidth().height(if (slim) 48.dp else 56.dp)
        // Lit from above with a machined foot, as the app's top bar (lot « design pro »).
        .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.White, 0.03f), lerp(Neon.Surface, Color.Black, 0.08f))))
        .drawBehind {
            drawLine(BAR_FOOT_SHADE, Offset(0f, size.height - 1.5f), Offset(size.width, size.height - 1.5f), 1f)
            drawLine(BAR_FOOT_LIGHT, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
        }
        .testTag("live_top_bar"),
) {
    val p = ui.project
    // A narrow screen (a small phone, a big interface size): ← alone, « Lancer à » gives way with
    // « … ». The counter and ● REC are placed first and never squeezed out.
    val compact = maxWidth < LIVE_BAR_FULL
    Row(
        Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        if (compact) {
            IconButton(onClick = leave, modifier = Modifier.size(48.dp).testTag("live_back").spot("live_back", tr("Revenir"))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
            }
        } else {
            TextButton(onClick = leave, modifier = Modifier.testTag("live_back").spot("live_back", tr("Revenir"))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(tr("Retour"), maxLines = 1)
            }
        }
        Box(
            Modifier.size(48.dp).liveGlow(ui.playing, Neon.Acid, inset = 2.dp).clip(CircleShape).clickable(onClick = actions.togglePlay).testTag("live_play").spot("live_play", tr("Jouer la partie"))
                .semantics { contentDescription = if (ui.playing) tr("Arrêter") else tr("Jouer") },
            contentAlignment = Alignment.Center,
        ) {
            val playColor = if (ui.playing) Neon.Acid else Neon.Cyan
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(lerp(playColor, Color.White, 0.2f), playColor))).background(BUTTON_GLOSS),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent)
            }
        }
        // « Lancer à » : touching it goes 1 → 2 → 4 bars → a beat → half a bar. It takes what is left, cut with « … ».
        val code = liveQuantumCode(p)
        val next = LIVE_QUANTUM_CYCLE[(LIVE_QUANTUM_CYCLE.indexOf(code) + 1).mod(LIVE_QUANTUM_CYCLE.size)]
        Box(Modifier.weight(1f)) {
            Column(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh)
                    .clickable { actions.setQuantum(next) }.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 4.dp).testTag("live_quantum")
                    .spot("live_quantum", tr("Quand ça part")),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(tr("Lancer à"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    liveQuantumLabel(code), style = MaterialTheme.typography.labelLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Counter(ui)
        RecordButton(ui.take, actions.record)
    }
}

/** « Lancer à » as [LiveActions.setQuantum] codes it: 1, 2, 4 bars; -1 a beat; -2 half a bar. */
fun liveQuantumCode(p: Project): Int = when (p.liveQuantumFine) {
    LiveSlots.QUANTUM_BEAT -> -1
    LiveSlots.QUANTUM_HALF_BAR -> -2
    else -> p.liveQuantum
}

/** The order a touch on « Lancer à » goes through. */
val LIVE_QUANTUM_CYCLE = listOf(1, 2, 4, -1, -2)

fun liveQuantumLabel(code: Int): String = when (code) {
    -1 -> tr("1 temps")
    -2 -> tr("½ mesure")
    1 -> tr("1 mesure")
    else -> tr("%d mesures", code)
}

/** Below this width the Live bar's ← loses its word. */
private val LIVE_BAR_FULL = 380.dp

/**
 * ●: ready (a red dot), armed (a blinking ring until the take starts on the launch line),
 * recording (■ Fin), writing.
 */
@Composable
internal fun RecordButton(take: TakeUi, record: () -> Unit) {
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
            .testTag("live_record").spot("live_record", tr("Enregistrer ta prise")).semantics { contentDescription = description },
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
 * countdown, « REC 0:42 · mesure 21 », then « Morceau écrit » with Annuler and Écouter. Without
 * [showReady] (lying down, full screen) the line waits for ●: armed, it offers the same choice.
 */
@Composable
internal fun TakeBanner(ui: LiveScreenUi, actions: LiveActions, showReady: Boolean = true, openVersions: () -> Unit = {}) {
    val take = ui.take
    val parts = ui.project.song.size
    if (take is TakeUi.Ready && (parts == 0 || !showReady)) return
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(if (take is TakeUi.Recording) Neon.Red.copy(alpha = 0.16f) else Neon.SurfaceHigh.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 4.dp).testTag("live_take").spot("live_take", tr("Où va ta prise")),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val style = MaterialTheme.typography.bodyMedium
        when (take) {
            is TakeUi.Recording -> {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Neon.Red))
                Column(Modifier.weight(1f)) {
                    Text(
                        tr("REC %1\$d:%2\$02d · mesure %3\$d", take.seconds / 60, take.seconds % 60, take.bar),
                        Modifier.testTag("live_take_clock"), style = MonoStyle, maxLines = 1,
                    )
                    // A limit coming is said before it bites; else what the take leaves out.
                    Text(
                        take.warning ?: tr("Pads enregistrés, pas M : pour un break, utilise ■."),
                        Modifier.testTag("live_take_note"), style = MaterialTheme.typography.labelSmall,
                        color = if (take.warning != null) Neon.Amber else Neon.TextSecondary, maxLines = 2,
                    )
                }
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
                if (ui.project.songVersions.isNotEmpty()) {
                    TextButton(onClick = openVersions, modifier = Modifier.heightIn(min = 48.dp).testTag("live_take_versions")) { Text(tr("Versions"), maxLines = 1) }
                }
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
                        TakeChoice(tr("Nouvelle version"), !ui.takeAppend, "live_take_replace") { actions.setTakeAppend(false) }
                        TakeChoice(tr("Ajouter à la suite"), ui.takeAppend, "live_take_append") { actions.setTakeAppend(true) }
                        // The songs earlier takes put aside: one touch away (the song is never lost).
                        val versions = ui.project.songVersions.size
                        if (versions > 0 && !armed) {
                            TextButton(onClick = openVersions, modifier = Modifier.heightIn(min = 48.dp).testTag("live_versions")) {
                                Text(tr("Versions (%d)", versions), maxLines = 1)
                            }
                        }
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
    val waiting = ui.grid?.hasPending == true
    // Waiting: the ring counts down to the first launch line armed (each keeps the quantum it was armed with).
    val q = ui.grid?.let { g -> nextLaunchQuantum(g, step, p.liveQuantumSteps) } ?: p.liveQuantumSteps
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
 * The grid: the scene buttons stay on the left, the track columns scroll sideways together, a
 * header (name, colour, M) above and the ■ of each track below. Always this way round (a track
 * added or a bigger text never turns it: the hand finds its scenes where they were). The cells
 * share the room (none smaller than a finger: past that, the grid scrolls).
 */
@Composable
private fun LiveGrid(ui: LiveScreenUi, columns: List<LiveColumn>, actions: LiveActions, modifier: Modifier) = BoxWithConstraints(modifier) {
    val p = ui.project
    val scenes = p.allScenes.indices.toList()
    val n = columns.size.coerceAtLeast(1)
    val rows = scenes.size.coerceAtLeast(1)
    val compact = maxHeight < 440.dp
    val gap = if (compact) 4.dp else 6.dp
    val sceneWidth = when {
        maxWidth >= 700.dp -> 128.dp
        maxWidth >= 440.dp -> 104.dp
        else -> 80.dp
    }
    val innerW = maxWidth - GRID_PAD_H * 2
    val innerH = maxHeight - GRID_PAD_V * 2
    val fitW = (innerW - sceneWidth - gap * n) / n
    val h = rememberScrollState()
    val v = rememberScrollState()
    // Blinks only while a launch or a stop waits, and only redraws (the cells read it when drawn).
    val blink = rememberBlink(ui.playing && ui.grid?.hasPending == true)
    // The playing clips breathe on the beat: one animation for the whole grid, read when drawn.
    val pulse = rememberBeatPulse(ui.playing, ui.clockOf, p.tempoBpm)
    // A touch that only stops a fling launches nothing (see launchOnPress).
    val scrolling = remember(h, v) { { h.isScrollInProgress || v.isScrollInProgress } }
    androidx.compose.runtime.CompositionLocalProvider(LocalGridScrolling provides scrolling) {
        val header = if (compact) HEADER_COMPACT else HEADER_HEIGHT
        val stopH = if (compact) 44.dp else 48.dp
        val cellW = fitW.coerceIn(MIN_CELL_W, MAX_CELL_W)
        val cellH = ((innerH - header - stopH - gap * (rows + 1)) / rows)
            .coerceIn(MIN_CELL_H, minOf(MAX_CELL_H, maxOf(cellW, MIN_CELL_H)))
        Column(Modifier.padding(horizontal = GRID_PAD_H, vertical = GRID_PAD_V).testTag("live_grid"), verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.width(sceneWidth).height(header), contentAlignment = Alignment.BottomStart) {
                    Text(tr("Briques"), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
                }
                ScrolledRow(h, gap) { columns.forEach { c -> ColumnHeader(c, c.key in ui.muted, cellW, header) { actions.toggleMute(c.key) } } }
            }
            Column(Modifier.weight(1f, fill = false).fadingVerticalEdges(v).verticalScroll(v), verticalArrangement = Arrangement.spacedBy(gap)) {
                scenes.forEach { scene ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        SceneButton(ui, scene, sceneWidth, cellH, blink, pulse, press = { actions.press(null, scene) }, retract = { actions.retract(null) }) { actions.launchRow(scene) }
                        ScrolledRow(h, gap) { columns.forEach { c -> GridCell(ui, c, scene, cellW, cellH, blink, pulse, actions) } }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                StopButton(tr("■ Tout"), Modifier.size(sceneWidth, stopH), waiting = false, blink, "live_stop_all", { actions.press(null, -1) }, { actions.retract(null) }, actions.stopAll)
                ScrolledRow(h, gap) {
                    columns.forEach { c ->
                        val slot = LiveSlots.of(c.key) ?: return@forEach
                        val state = ui.grid?.slot(slot)
                        StopButton(
                            "■", Modifier.size(cellW, stopH), waiting = ui.playing && state?.pending == -1, blink, "live_stop_${c.key}",
                            { actions.press(c.key, -1) }, { actions.retract(c.key) },
                        ) { actions.stopTrack(c.key) }
                    }
                }
            }
        }
    }
}

/** One pad of the grid: track [c] in scene [scene], as the engine plays it. */
@Composable
private fun GridCell(ui: LiveScreenUi, c: LiveColumn, scene: Int, width: Dp, height: Dp, blink: State<Float>, pulse: State<Float>, actions: LiveActions) {
    val p = ui.project
    val slot = LiveSlots.of(c.key) ?: return
    val state = ui.grid?.slot(slot)
    val playing = ui.playing && state?.playing == scene
    val start = state?.start ?: 0
    val loop = if (playing) p.slotLoopSteps(c.key, scene) else 0
    LivePadCell(
        c, rowName(p, scene), width, height, p.cellHasContent(c.key, scene), playing, pending = ui.playing && state?.pending == scene,
        position = { livePosition(ui.clockOf(), start, loop) }, blink, pulse,
        // The line the launch was armed for (« Lancer à » changed since does not move it).
        waitLeft = { launchWaitLeft(ui.clockOf(), state?.quantum?.takeIf { it > 0 } ?: p.liveQuantumSteps) },
        tag = "live_pad_${c.key}_$scene",
        press = { actions.press(c.key, scene) }, retract = { actions.retract(c.key) },
    ) { actions.launchPad(c.key, scene) }
}

@Composable
private fun ScrolledRow(state: ScrollState, gap: Dp, content: @Composable () -> Unit) {
    // Faded on the side where more tracks (or scenes) wait.
    Row(Modifier.fadingEdges(state).horizontalScroll(state), horizontalArrangement = Arrangement.spacedBy(gap)) { content() }
}

/** M: silent at once (not written in the project). */
@Composable
private fun MuteButton(c: LiveColumn, muted: Boolean, toggleMute: () -> Unit, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(if (muted) Neon.Amber else Neon.SurfaceHigh)
            .clickable(onClick = toggleMute).testTag("live_mute_${c.key}").spot("live_mute_${c.key}", if (c.key == com.pocketstudio.model.SongPart.DRUMS) tr("Couper le son") else null)
            .semantics { contentDescription = if (muted) tr("Remettre le son de %s", c.name) else tr("Couper le son de %s", c.name) },
        contentAlignment = Alignment.Center,
    ) {
        Text("M", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (muted) Color.Black else Neon.TextSecondary)
    }
}

/** A column's head: the track's colour, its name, then its M (a full 48 dp when there is room). */
@Composable
private fun ColumnHeader(c: LiveColumn, muted: Boolean, width: Dp, height: Dp, toggleMute: () -> Unit) {
    Column(
        Modifier.width(width).height(height).clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.White, 0.04f), lerp(Neon.Surface, Color.Black, 0.05f)))),
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(listOf(c.color, c.color.copy(alpha = 0.45f)))))
        Text(
            c.name, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
        // Under the name: a finger that misses it does not land on a pad (which would launch a
        // scene, into the take). Lying down it is shorter, as wide as the column.
        MuteButton(
            c, muted, toggleMute,
            Modifier.fillMaxWidth().height(if (height < HEADER_HEIGHT) 30.dp else 48.dp).padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun SceneButton(
    ui: LiveScreenUi, scene: Int, width: Dp, height: Dp, blink: State<Float>, pulse: State<Float>,
    press: () -> Boolean, retract: () -> Unit, launch: () -> Unit,
) {
    val p = ui.project
    val current = ui.playing && ui.grid?.row == scene
    val waiting = ui.playing && ui.grid?.pendingRow == scene
    val label = p.sceneLabel(scene).takeIf { it.isNotBlank() }?.let(Scene::shownLabel)
    val cyan = Neon.Cyan
    val rest = Color.White.copy(alpha = 0.06f)
    Row(
        Modifier.width(width).height(height).clip(RoundedCornerShape(12.dp))
            .background(
                if (current) SolidColor(Neon.Cyan.copy(alpha = 0.22f))
                else Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.White, 0.04f), lerp(Neon.Surface, Color.Black, 0.05f))),
            )
            .let { m ->
                when {
                    waiting -> m.blinkingBorder(2.dp, 12.dp) { cyan.copy(alpha = blink.value) }
                    // The scene playing: its edge breathes with the beat (read when drawn).
                    current -> m.blinkingBorder(2.dp, 12.dp) { cyan.copy(alpha = 0.55f + 0.45f * pulse.value) }
                    else -> m.border(2.dp, rest, RoundedCornerShape(12.dp))
                }
            }
            .launchOnPress(press, retract, launch).padding(horizontal = 6.dp).testTag("live_row_$scene"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (width >= 72.dp) Text("▶", color = if (current) Neon.Cyan else Neon.TextSecondary, style = MaterialTheme.typography.titleSmall)
        Column {
            Text(Scene.NAMES.getOrElse(scene) { "?" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            if (label != null) {
                Text(
                    label, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary,
                    maxLines = if (height >= 64.dp) 2 else 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A pad: empty, with something to play (tinted with its track's colour), waiting for its launch
 * line (its edge blinks, a ring counts down), or playing (lit, a soft glow breathing on the beat,
 * a ring going round with its loop). Lighting up and going out fade in 220 ms. Everything that
 * moves is read when drawn: neither the clock nor the beat recompose a pad.
 */
@Composable
private fun LivePadCell(
    c: LiveColumn,
    row: String,
    width: Dp,
    height: Dp,
    hasContent: Boolean,
    playing: Boolean,
    pending: Boolean,
    position: () -> Float,
    blink: State<Float>,
    pulse: State<Float>,
    waitLeft: () -> Float,
    tag: String,
    press: () -> Boolean,
    retract: () -> Unit,
    launch: () -> Unit,
) {
    SideEffect { livePadCompositions++ }
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(12.dp)
    val lit = animateFloatAsState(if (playing) 1f else 0f, tween(220), label = "pad-lit")
    val color = c.color
    val idleTop = if (hasContent) color.copy(alpha = 0.34f) else lerp(Neon.Surface, Color.Black, 0.07f)
    val idleBottom = if (hasContent) color.copy(alpha = 0.16f) else Neon.Surface
    val litBottom = lerp(color, Color.Black, 0.28f)
    val edge = Color.White.copy(alpha = if (hasContent) 0.14f else 0.05f)
    Box(
        Modifier.size(width, height)
            // Brushes and strokes made once per size: a playing pad redraws every frame without garbage.
            .drawWithCache {
                val ring = Stroke(1.5.dp.toPx())
                val r = 12.dp.toPx()
                onDrawBehind {
                    // The glow around a playing pad, stronger on each beat.
                    val on = lit.value
                    if (on > 0.01f) {
                        val beat = pulse.value
                        for (k in 1..3) {
                            val spread = k * 1.5.dp.toPx()
                            drawRoundRect(
                                color.copy(alpha = (on * (0.14f + 0.26f * beat) / k).coerceIn(0f, 1f)),
                                topLeft = Offset(-spread, -spread), size = Size(size.width + 2 * spread, size.height + 2 * spread),
                                cornerRadius = CornerRadius(r + spread), style = ring,
                            )
                        }
                    }
                }
            }
            .clip(shape)
            .drawWithCache {
                // Resting: a pad with something to play stands out (lit from above), an empty one is a slight hollow.
                val idle = Brush.verticalGradient(listOf(idleTop, idleBottom))
                val litFill = Brush.verticalGradient(listOf(color, litBottom))
                val sheen = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.07f), 0.45f to Color.Transparent)
                onDrawBehind {
                    val on = lit.value
                    if (on < 0.99f) drawRect(idle)
                    if (on > 0.01f) {
                        drawRect(litFill, alpha = on)
                        // A light flash on the beat, fading through it.
                        drawRect(Color.White.copy(alpha = 0.18f * on * pulse.value))
                    }
                    if (hasContent) drawRect(sheen)
                }
            }
            .let { m ->
                if (pending) m.blinkingBorder(3.dp, 12.dp) { color.copy(alpha = blink.value) }
                else m.border(1.dp, edge, shape)
            }
            // Launched on the finger's down (a controller's pad), a light buzz then.
            .launchOnPress(
                press = { press().also { if (it) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } },
                retract = retract,
                tap = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    launch()
                },
            )
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
            // Where the loop is: a ring going round from the top.
            val track = Color.Black.copy(alpha = 0.22f)
            Canvas(Modifier.size(minOf(width, height) * 0.5f)) {
                val s = 4.dp.toPx()
                val topLeft = Offset(s / 2, s / 2)
                val arc = Size(size.width - s, size.height - s)
                drawArc(track, 0f, 360f, false, topLeft = topLeft, size = arc, style = Stroke(s))
                drawArc(Color.White.copy(alpha = 0.92f), -90f, 360f * position(), false, topLeft = topLeft, size = arc, style = Stroke(s, cap = StrokeCap.Round))
            }
        } else if (pending) {
            // The ring empties until the launch line: when it is gone, the pad plays.
            Box(Modifier.size(minOf(width, height) * 0.62f).countdownRing(waitLeft, color), contentAlignment = Alignment.Center) {
                Text("→", Modifier.graphicsLayer { alpha = blink.value }, style = MaterialTheme.typography.titleLarge, color = color)
            }
        } else if (hasContent) {
            Text("▶", style = MaterialTheme.typography.titleSmall, color = color.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun StopButton(label: String, modifier: Modifier, waiting: Boolean, blink: State<Float>, tag: String, press: () -> Boolean, retract: () -> Unit, stop: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .let { m ->
                val red = Neon.Red
                if (waiting) m.drawBehind { drawRect(red.copy(alpha = 0.25f + 0.35f * blink.value)) } else m.background(Neon.SurfaceHigh)
            }
            .launchOnPress(press, retract, stop).testTag(tag)
            // One id per control; the drums' ■ alone says what a track's ■ does (one label, not seven).
            .spot(tag, when (tag) {
                "live_stop_all" -> tr("Tout arrêter")
                "live_stop_" + com.pocketstudio.model.SongPart.DRUMS -> tr("Arrêter cette piste")
                else -> null
            })
            .semantics { if (label == "■") contentDescription = tr("Arrêter la piste") },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary, maxLines = 1)
    }
}

/**
 * The panel: Effets | 303 | Pads (on a wide screen upright, effects and 303 side by side), with ⛶
 * and its fold (upright: down to its tabs; lying down, [sideways]: to a rail on the side).
 */
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
    onToggle: () -> Unit,
    tab: LivePanelTab,
    onTab: (LivePanelTab) -> Unit,
    immersive: Boolean,
    toggleFull: () -> Unit,
    sideways: Boolean,
    modifier: Modifier,
) {
    val shown = if (sideBySide && tab == LivePanelTab.Acide) LivePanelTab.Effets else tab
    val shape = if (sideways) RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Column(modifier.clip(shape).background(Neon.Surface).testTag("live_panel")) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val tabs = if (sideBySide) listOf(LivePanelTab.Effets to tr("Effets et 303"), LivePanelTab.Pads to tr("Pads"), LivePanelTab.Notes to tr("Notes"))
            else listOf(LivePanelTab.Effets to tr("Effets"), LivePanelTab.Acide to tr("Acid 303"), LivePanelTab.Pads to tr("Pads"), LivePanelTab.Notes to tr("Notes"))
            // The tabs share the row by the length of their names (a bigger text makes them
            // smaller, never hides one under ⛶).
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEach { (t, label) ->
                    val selected = open && shown == t
                    // No 303 line in the project: its tab stays (it says how to get one) but greyed.
                    val idle = t == LivePanelTab.Acide && !ui.hasAcid
                    Box(
                        Modifier.weight(label.length + 3f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (selected) Neon.Cyan else Neon.SurfaceHigh)
                            .clickable {
                                onTab(t)
                                // Lot 4a: « Pads » is the pads view, on the whole screen (Back: here, the card and its settings).
                                if (t == LivePanelTab.Pads && pads?.editing == null) padsActions.fullScreen?.invoke()
                            }.padding(horizontal = 4.dp).testTag("live_tab_${t.name}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        val style = MaterialTheme.typography.labelLarge
                        Text(
                            label, color = if (selected) Neon.OnAccent else if (idle) Neon.TextSecondary.copy(alpha = 0.6f) else Neon.TextPrimary,
                            style = style, maxLines = 1, softWrap = false,
                            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
                        )
                    }
                }
            }
            FullScreenButton(immersive, toggleFull)
            IconButton(onClick = onToggle, modifier = Modifier.testTag("live_panel_fold")) {
                Icon(
                    when {
                        sideways -> Icons.AutoMirrored.Filled.KeyboardArrowRight
                        open -> Icons.Filled.ExpandMore
                        else -> Icons.Filled.ExpandLess
                    },
                    contentDescription = if (open) tr("Replier") else tr("Déplier"),
                )
            }
        }
        if (!open) return@Column
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp)) {
            when {
                shown == LivePanelTab.Pads -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (pads != null) LivePadsCard(pads, padsActions)
                }
                shown == LivePanelTab.Notes -> LiveNotes(ui, actions, Modifier.fillMaxSize())
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

/** The grid's column headers: the track's colour, its name, then a 48 dp M (lying down: [HEADER_COMPACT]). */
private val HEADER_HEIGHT = 80.dp
private val HEADER_COMPACT = 56.dp

/** A cell never gets smaller than a finger: past these the grid scrolls. */
private val MIN_CELL_W = 56.dp
private val MIN_CELL_H = 48.dp
/** Nor bigger than a big pad, however few the tracks. */
private val MAX_CELL_W = 120.dp
private val MAX_CELL_H = 96.dp
private val GRID_PAD_H = 8.dp
private val GRID_PAD_V = 6.dp

private val BAR_FOOT_SHADE = Color.Black.copy(alpha = 0.32f)
private val BAR_FOOT_LIGHT = Color.White.copy(alpha = 0.07f)

/** The light on a raised button's upper half (Play). */
private val BUTTON_GLOSS = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.2f), 0.5f to Color.White.copy(alpha = 0.04f), 0.55f to Color.Transparent)

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

/**
 * 1 on each beat while [playing], fading to 0 through the beat (0 when stopped). One animation for
 * the whole grid, driven by the polled clock; read its value where it is drawn (the playing pads'
 * glow, the scene playing): nothing recomposes with it.
 */
@Composable
private fun rememberBeatPulse(playing: Boolean, clockOf: () -> Int, bpm: Double): State<Float> {
    val pulse = remember { Animatable(0f) }
    val clock by rememberUpdatedState(clockOf)
    if (playing) {
        LaunchedEffect(bpm) {
            val fade = (60_000.0 / bpm.coerceIn(40.0, 300.0) * 0.85).toInt()
            snapshotFlow { clock().let { if (it < 0) -1 else it / STEPS_PER_BEAT } }
                .distinctUntilChanged()
                .collectLatest { beat ->
                    if (beat < 0) return@collectLatest
                    pulse.snapTo(1f)
                    pulse.animateTo(0f, tween(fade, easing = FastOutSlowInEasing))
                }
        }
    } else {
        LaunchedEffect(Unit) { pulse.snapTo(0f) }
    }
    return pulse.asState()
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
