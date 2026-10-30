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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
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
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.drawscope.translate
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
import com.pocketstudio.model.liveRowsShown
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
    /** Lot 4b: the empty cell recording what is played (null: none). */
    val cellRecording: CellRecording? = null,
    /** Lot 4c: the grid's moving light off (null: the phone's « Supprimer les animations »). */
    val reducedMotion: Boolean? = null,
    /** Step 4f: the track a MIDI keyboard plays (the selected one): Notes opens on it (null: the first). */
    val selectedTrack: Int? = null,
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
    /** Lot 4b, an empty cell: « Jouer et enregistrer ici » (touched again: [stopCellRecording]). */
    val recordCell: (key: String, scene: Int) -> Unit = { _, _ -> },
    val stopCellRecording: () -> Unit = {},
    /** « Un son de ma bibliothèque » for that cell (the sound browser). */
    val cellFromLibrary: (key: String, scene: Int) -> Unit = { _, _ -> },
    /** « Copier une autre case »: false when the two do not go together (said by the app). */
    val copyCell: (fromKey: String, fromScene: Int, toKey: String, toScene: Int) -> Boolean = { _, _, _, _ -> false },
    /** A drum pad played on the page (it goes into the drum cell recording). */
    val drumHit: (lane: Int) -> Unit = {},
    /** Step 4f: the track shown in Notes, so a MIDI keyboard plays (and records into) the same one. */
    val selectTrack: (Int) -> Unit = {},
    /** « Cases − 8 + »: how many bricks the grid shows (LiveCells.ROWS). */
    val setRows: (Int) -> Unit = {},
    /** Lot 4d: « − 113 + » in the bar, a BPM less or more (held: it repeats). */
    val nudgeTempo: (Int) -> Unit = {},
    /** A touch on the bar's tempo: the tempo sheet (keypad, ready-made tempos, tap). */
    val openTempo: () -> Unit = {},
)

/** The Live page's tabs: Briques (the grid alone, the tools folded), then the panel's pages. */
enum class LivePanelTab { Briques, Effets, Acide, Pads, Notes }

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
    /** Lot 4d: Briques (the default) opens on the grid alone, the tools folded in their dock. */
    initialTab: LivePanelTab = LivePanelTab.Briques,
    /** Under the page's bar: the position strip (tempo, « mes. x/y »), given by the app. */
    under: (@Composable () -> Unit)? = null,
    /** Phase 36: « Plein écran » on or off (the app hides its bottom bar with the phone's). */
    onImmersive: (Boolean) -> Unit = {},
    /** The panel's page shown (a MIDI controller's knobs and pads follow it). */
    onTab: (LivePanelTab) -> Unit = {},
) {
    val columns = liveColumnsOf(ui.project)
    // The panel's page (kept while folded: ︿ opens it there again) and whether it is open.
    var tab by rememberSaveable { mutableStateOf(if (initialTab == LivePanelTab.Briques) LivePanelTab.Effets else initialTab) }
    var panelOpen by rememberSaveable { mutableStateOf(initialTab != LivePanelTab.Briques) }
    LaunchedEffect(tab, panelOpen) { onTab(if (panelOpen) tab else LivePanelTab.Briques) }
    // A tab of the dock or the panel: Briques folds the tools; Pads is the pads view on the whole
    // screen (lot 4a; while a pad is being set up, its card here); the others open the panel there.
    val choose: (LivePanelTab) -> Unit = { t ->
        when {
            t == LivePanelTab.Briques -> panelOpen = false
            t == LivePanelTab.Pads && pads?.editing == null && padsActions.fullScreen != null -> { tab = t; padsActions.fullScreen.invoke() }
            else -> { tab = t; panelOpen = true }
        }
    }
    // « Régler » from the pads view: back here, that pad's settings in the open panel.
    LaunchedEffect(pads?.editing) { if (pads?.editing != null) { tab = LivePanelTab.Pads; panelOpen = true } }
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
    // Lot 4b: an empty cell's menu, the cell waiting for the one to copy, the cell recording.
    var menuCell by rememberSaveable { mutableStateOf<String?>(null) }
    var copyInto by rememberSaveable { mutableStateOf<String?>(null) }
    val menu = LiveCell.read(menuCell)
    val copyTarget = LiveCell.read(copyInto)
    val trackName: (String) -> String = { key -> columns.firstOrNull { it.key == key }?.name ?: "" }
    // Recording a cell: the panel shows what to play on (the notes, or the drum pads there).
    LaunchedEffect(ui.cellRecording) { if (ui.cellRecording != null) { tab = LivePanelTab.Notes; panelOpen = true } }
    val cellTap: (LiveColumn, Int, Boolean) -> Unit = { c, scene, hasContent ->
        val rec = ui.cellRecording
        when {
            rec != null && rec.key == c.key && rec.scene == scene -> actions.stopCellRecording()
            copyTarget != null -> {
                if (hasContent && !(copyTarget.key == c.key && copyTarget.scene == scene)) actions.copyCell(c.key, scene, copyTarget.key, copyTarget.scene)
                copyInto = null
            }
            !hasContent -> menuCell = LiveCell(c.key, scene).save()
            else -> actions.launchPad(c.key, scene)
        }
    }
    val gridMarks = GridMarks(copyTarget ?: menu, ui.cellRecording, picking = copyTarget != null, tap = cellTap)
    BoxWithConstraints(Modifier.fillMaxSize().background(Neon.Background).testTag("live_screen")) {
        // Lying down (or full screen) every row of height counts: a slimmer bar, and the take's
        // line only once ● is touched (it then offers where the take goes).
        val short = immersive || maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            LiveTopBar(ui, actions, leave, slim = short)
            // Full screen is for the grid: the strip gives way there.
            if (under != null && !immersive) under()
            TakeBanner(ui, actions, showReady = !short, openVersions = { versionsOpen = true })
            ui.cellRecording?.let { CellRecordingBar(trackName(it.key), it, actions.stopCellRecording) }
            copyTarget?.let { CopyCellBar(trackName(it.key), it, cancel = { copyInto = null }) }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val landscape = maxWidth > maxHeight
                val wide = maxWidth >= 560.dp
                val panelWidth = if (maxWidth >= 900.dp) 420.dp else 360.dp
                if (landscape) {
                    Row(Modifier.fillMaxSize()) {
                        LiveGrid(ui, columns, actions, gridMarks, Modifier.weight(1f).fillMaxHeight())
                        if (panelOpen) {
                            LivePanel(
                                ui, columns, actions, fx, pads, padsActions, sideBySide = false, fold = { panelOpen = false },
                                tab = tab, choose = choose, immersive = immersive, toggleFull = toggleFull, sideways = true,
                                modifier = Modifier.width(panelWidth).fillMaxHeight(),
                            )
                        } else {
                            PanelRail(ui, choose, immersive, toggleFull, Modifier.width(RAIL_WIDTH).fillMaxHeight())
                        }
                    }
                } else {
                    // Lot 4d: folded, the grid has the screen and the tools wait in their dock. Open,
                    // the panel takes what the grid leaves past its rows made lower (about six
                    // bricks in view), at least PANEL_MIN (its pad in view: the grid then scrolls);
                    // a wide screen: its full height up to 430 dp.
                    if (panelOpen) {
                        val gridDense = 48.dp + HEADER_COMPACT + 48.dp + (ROW_MIN_DENSE + 4.dp) * 6 + 40.dp
                        val gridShort = HEADER_COMPACT + 96.dp + (MIN_CELL_H + 6.dp) + 24.dp
                        val panelHeight = when {
                            wide -> minOf(430.dp, maxHeight - gridDense).coerceAtLeast(PANEL_MIN)
                            else -> (maxHeight - gridDense).coerceIn(PANEL_MIN, PANEL_MAX)
                        }.coerceAtMost((maxHeight - gridShort).coerceAtLeast(PANEL_MIN))
                        Column(Modifier.fillMaxSize()) {
                            LiveGrid(ui, columns, actions, gridMarks, Modifier.fillMaxWidth().weight(1f), dense = true)
                            LivePanel(
                                ui, columns, actions, fx, pads, padsActions, sideBySide = wide, fold = { panelOpen = false },
                                tab = tab, choose = choose, immersive = immersive, toggleFull = toggleFull, sideways = false,
                                modifier = Modifier.fillMaxWidth().height(panelHeight),
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            LiveGrid(ui, columns, actions, gridMarks, Modifier.fillMaxWidth().weight(1f))
                            LiveDock(ui, wide, choose, unfold = { panelOpen = true }, immersive = immersive, toggleFull = toggleFull)
                        }
                    }
                }
            }
        }
        if (menu != null) {
            EmptyCellMenu(
                trackName(menu.key), menu,
                record = { menuCell = null; actions.recordCell(menu.key, menu.scene) },
                library = { menuCell = null; actions.cellFromLibrary(menu.key, menu.scene) },
                copy = { menuCell = null; copyInto = menu.save() },
                cancel = { menuCell = null },
            )
        }
    }
}

/**
 * Lot 4b on the grid: the cell marked « ici » ([here]: its menu open, or waiting for the cell to
 * copy), the one recording, whether a cell to copy is being picked, and what a touch on a cell does.
 */
internal class GridMarks(
    val here: LiveCell? = null,
    val recording: CellRecording? = null,
    val picking: Boolean = false,
    val tap: ((LiveColumn, Int, Boolean) -> Unit)? = null,
)

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
private fun PanelRail(ui: LiveScreenUi, onTab: (LivePanelTab) -> Unit, immersive: Boolean, toggleFull: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).background(Neon.Surface)
            .padding(vertical = 4.dp).testTag("live_panel_rail"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FullScreenButton(immersive, toggleFull)
        liveTabs(sideBySide = false).filter { it != LivePanelTab.Briques }.forEach { t ->
            val label = tabLabel(t)
            val idle = t == LivePanelTab.Acide && !ui.hasAcid
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { onTab(t) }.testTag("live_rail_${t.name}")
                    .semantics { contentDescription = tr("Ouvrir %s", label) },
                contentAlignment = Alignment.Center,
            ) {
                // « Acid 303 » on two lines in the 48 dp rail.
                Text(
                    label, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (idle) Neon.TextSecondary.copy(alpha = 0.6f) else Neon.TextPrimary,
                )
            }
        }
    }
}

/** The folded panel lying down. */
private val RAIL_WIDTH = 56.dp

/** The open panel upright, at least (the grid scrolls past that), and at most on a phone. */
private val PANEL_MIN = 300.dp
private val PANEL_MAX = 470.dp

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
    // Lot 4d: ← alone, then « − 113 + » (the tempo always at hand, whatever the tab); « Lancer à »
    // went down beside « Cases ». A narrow bar keeps the figure alone (a touch: the tempo sheet).
    // The counter and ● REC are placed first and never squeezed out.
    val compact = maxWidth < LIVE_BAR_FULL
    Row(
        Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        IconButton(onClick = leave, modifier = Modifier.size(48.dp).testTag("live_back").spot("live_back", tr("Revenir"))) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
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
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { LiveTempo(p.tempoBpm, actions) }
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
 * added or a bigger text never turns it: the hand finds its scenes where they were). Lot 4c:
 * « Cases − 8 + » sets how many bricks share the height (none smaller than [ROW_MIN_H]: past that
 * the grid scrolls); rows past the song's last brick are new bricks, their dots ready to fill.
 */
@Composable
private fun LiveGrid(ui: LiveScreenUi, columns: List<LiveColumn>, actions: LiveActions, marks: GridMarks, modifier: Modifier, dense: Boolean = false) = BoxWithConstraints(modifier.background(GridLook.Ground)) {
    val p = ui.project
    val shown = p.liveRowsShown
    val rows = maxOf(shown, p.allScenes.size).coerceAtMost(Scene.MAX)
    val scenes = (0 until rows).toList()
    val n = columns.size.coerceAtLeast(1)
    val compact = dense || maxHeight < 440.dp
    // Lot 4d: on a phone upright, five tracks or more take narrower columns (and gaps) so that all of
    // them fit across; past [MIN_CELL_W] the columns scroll, with a › that says so.
    val crowded = maxWidth < 440.dp && n >= 5
    val gap = if (compact || crowded) 4.dp else 6.dp
    val sceneWidth = when {
        maxWidth >= 700.dp -> 104.dp
        maxWidth >= 440.dp -> 80.dp
        crowded -> 44.dp
        else -> 52.dp
    }
    val innerW = maxWidth - GRID_PAD_H * 2
    val innerH = maxHeight - GRID_PAD_V * 2
    val fitW = (innerW - sceneWidth - gap * n) / n
    val h = rememberScrollState()
    val v = rememberScrollState()
    val reduced = ui.reducedMotion ?: com.pocketstudio.app.onboarding.animationsOff(androidx.compose.ui.platform.LocalContext.current)
    // Blinks only while a launch or a stop waits, and only redraws (the cells read it when drawn).
    val blink = rememberBlink(!reduced && ui.playing && ui.grid?.hasPending == true)
    // One clock for everything that moves (halo, sheen, wave), read when drawn.
    val clock = rememberGridClock(!reduced && ui.playing, ui.clockOf, p.tempoBpm)
    // Where the wave leaves from: the cells playing now.
    val sources = remember(ui.grid, ui.playing, columns) {
        if (!ui.playing) emptyList() else columns.mapIndexedNotNull { i, c ->
            val slot = LiveSlots.of(c.key) ?: return@mapIndexedNotNull null
            val row = ui.grid?.slot(slot)?.playing ?: return@mapIndexedNotNull null
            if (row >= 0 && p.cellHasContent(c.key, row)) WaveSource(i, row, c.color) else null
        }
    }
    // Only one cell with something in it: the first loop, an invitation to add the next.
    val single = remember(p, columns) {
        val full = columns.flatMapIndexed { i, c -> p.allScenes.indices.filter { p.cellHasContent(c.key, it) }.map { i to it } }
        full.singleOrNull()
    }
    // A touch that only stops a fling launches nothing (see launchOnPress).
    val scrolling = remember(h, v) { { h.isScrollInProgress || v.isScrollInProgress } }
    androidx.compose.runtime.CompositionLocalProvider(LocalGridScrolling provides scrolling) {
        val header = if (compact) HEADER_COMPACT else HEADER_HEIGHT
        val stopH = if (compact) 44.dp else 48.dp
        val top = 48.dp
        val cellW = fitW.coerceIn(MIN_CELL_W, MAX_CELL_W)
        val cellH = ((innerH - top - header - stopH - gap * (shown + 2)) / shown).coerceAtLeast(if (dense) ROW_MIN_DENSE else ROW_MIN_H)
        val look = CellLook(clock, sources, cellW.value, cellH.value, gap.value)
        Column(Modifier.padding(horizontal = GRID_PAD_H, vertical = GRID_PAD_V).testTag("live_grid"), verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(Modifier.fillMaxWidth().height(top - gap), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                // Lot 4d: « Lancer à » here (the bar keeps the tempo), beside « Cases ».
                QuantumButton(p, actions.setQuantum, Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(8.dp))
                CasesStepper(shown, actions.setRows)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Spacer(Modifier.width(sceneWidth).height(header))
                Box {
                    ScrolledRow(h, gap) { columns.forEach { c -> ColumnHeader(c, c.key in ui.muted, cellW, header) { actions.toggleMute(c.key) } } }
                    // Over the last name (never over an M): more tracks wait to the right.
                    if (h.canScrollForward) MoreColumns(h, Modifier.align(Alignment.TopEnd).padding(top = 3.dp))
                }
            }
            Box(Modifier.weight(1f, fill = false)) {
                Column(Modifier.fadingVerticalEdges(v).verticalScroll(v), verticalArrangement = Arrangement.spacedBy(gap)) {
                    scenes.forEach { scene ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            SceneButton(ui, scene, sceneWidth, cellH, blink, press = { actions.press(null, scene) }, retract = { actions.retract(null) }) { actions.launchRow(scene) }
                            ScrolledRow(h, gap) {
                                columns.forEachIndexed { i, c ->
                                    val hint = single != null && ui.playing && single.second == scene && single.first + 1 == i
                                    GridCell(ui, c, i, scene, cellW, cellH, blink, look, actions, marks, hint)
                                }
                            }
                        }
                    }
                }
                if (single != null && ui.playing && marks.here == null && marks.recording == null) {
                    // Over the dots, below the first loop (the touches go through to the cells).
                    Text(
                        tr("Ta première boucle tourne. Touche un point pour y ajouter un son."),
                        Modifier.align(Alignment.Center).padding(start = sceneWidth + 12.dp, end = 12.dp)
                            .clip(RoundedCornerShape(12.dp)).background(GridLook.Ground)
                            .border(1.dp, Color(0xFF2C3035), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp).testTag("live_first_loop"),
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFAAB2BD), textAlign = TextAlign.Center,
                    )
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

/** What every cell's drawing shares: the clock, where the wave leaves from, the cells' size and gap (dp). */
internal class CellLook(val clock: GridClock, val sources: List<WaveSource>, val w: Float, val h: Float, val gap: Float)

/** One pad of the grid: track [c] (column [col]) in scene [scene], as the engine plays it. */
@Composable
private fun GridCell(ui: LiveScreenUi, c: LiveColumn, col: Int, scene: Int, width: Dp, height: Dp, blink: State<Float>, look: CellLook, actions: LiveActions, marks: GridMarks, hint: Boolean) {
    val p = ui.project
    val slot = LiveSlots.of(c.key) ?: return
    val state = ui.grid?.slot(slot)
    val playing = ui.playing && state?.playing == scene
    val start = state?.start ?: 0
    val loop = if (playing) p.slotLoopSteps(c.key, scene) else 0
    val hasContent = p.cellHasContent(c.key, scene)
    val bars = remember(p, c.key, scene, hasContent) { if (hasContent) cellBars(p, c.key, scene) else null }
    val mark = when {
        marks.recording?.let { it.key == c.key && it.scene == scene } == true -> CellMark.Rec
        marks.here?.let { it.key == c.key && it.scene == scene } == true -> CellMark.Here
        else -> CellMark.None
    }
    // An empty cell, the one recording, or a cell picked to copy: a touch, never a launch on the press.
    val plain = hasContent && !marks.picking && mark == CellMark.None
    val tap = marks.tap
    val clock = look.clock
    LivePadCell(
        c, rowName(p, scene), width, height, hasContent, playing, pending = ui.playing && state?.pending == scene,
        // The loop's progress, smooth between the clock's polls while the animations run.
        position = {
            val s = clock.steps
            if (s >= 0f && loop > 0) (((s - start) % loop + loop) % loop) / loop else livePosition(ui.clockOf(), start, loop)
        },
        blink, look, col, scene, bars, hint,
        tag = "live_pad_${c.key}_$scene",
        press = { if (plain || tap == null) actions.press(c.key, scene) else false }, retract = { actions.retract(c.key) },
        mark = mark,
    ) { if (tap != null) tap(c, scene, hasContent) else actions.launchPad(c.key, scene) }
}

@Composable
private fun ScrolledRow(state: ScrollState, gap: Dp, content: @Composable () -> Unit) {
    // Faded on the side where more tracks (or scenes) wait.
    Row(Modifier.fadingEdges(state).horizontalScroll(state), horizontalArrangement = Arrangement.spacedBy(gap)) { content() }
}

/** Lot 4d: more tracks to the right (the columns scroll): a › over the last name, a touch shows the last ones. */
@Composable
private fun MoreColumns(state: ScrollState, modifier: Modifier) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(
        modifier.size(24.dp, 20.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)).background(Neon.Cyan.copy(alpha = 0.9f))
            .clickable { scope.launch { state.animateScrollTo(state.maxValue) } }
            .semantics { contentDescription = tr("Voir les autres pistes") }.testTag("live_columns_more"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(18.dp))
    }
}

/** M: silent at once (not written in the project). */
@Composable
private fun MuteButton(c: LiveColumn, muted: Boolean, toggleMute: () -> Unit, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(if (muted) Neon.Amber else Color(0xFF34383E))
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
        Modifier.width(width).height(height).clip(RoundedCornerShape(10.dp)).background(GridLook.Panel),
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(c.color.copy(alpha = 0.9f)))
        // A narrow column (six tracks on a phone): the name gets smaller before it is cut.
        val style = MaterialTheme.typography.labelMedium
        Text(
            c.name, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 2.dp, vertical = 2.dp), style = style,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, softWrap = false,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
        )
        // Under the name: a finger that misses it does not land on a pad (which would launch a
        // scene, into the take). Lying down it is shorter, as wide as the column.
        MuteButton(
            c, muted, toggleMute,
            Modifier.fillMaxWidth().height(if (height < HEADER_HEIGHT) 30.dp else 48.dp).padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        )
    }
}

/** A brick's button: its letter and name; the brick playing outlined in orange, one waiting blinks. */
@Composable
private fun SceneButton(
    ui: LiveScreenUi, scene: Int, width: Dp, height: Dp, blink: State<Float>,
    press: () -> Boolean, retract: () -> Unit, launch: () -> Unit,
) {
    val p = ui.project
    val current = ui.playing && ui.grid?.row == scene
    val waiting = ui.playing && ui.grid?.pendingRow == scene
    val label = p.sceneLabel(scene).takeIf { it.isNotBlank() }?.let(Scene::shownLabel)
    val accent = Neon.Cyan
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.width(width).height(height).clip(shape)
            .background(if (current) Brush.horizontalGradient(listOf(Color(0xFF3A3227), GridLook.Panel)) else SolidColor(GridLook.Panel))
            .let { m ->
                when {
                    waiting -> m.blinkingBorder(1.5.dp, 10.dp) { accent.copy(alpha = blink.value) }
                    current -> m.border(1.5.dp, accent, shape)
                    else -> m
                }
            }
            .launchOnPress(press, retract, launch).padding(horizontal = 2.dp).testTag("live_row_$scene"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Scene.NAMES.getOrElse(scene) { "?" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        if (label != null && height >= 52.dp) {
            Text(
                label, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, textAlign = TextAlign.Center,
                maxLines = if (height >= 72.dp && width >= 72.dp) 2 else 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A pad, style B: empty, a small dot the beat's wave runs through; with something to play, a soft
 * gradient of its track's hue and a little preview of its loop; waiting for its launch line (its
 * outline blinks, an arrow); playing (lit from light to deep, a sheen sliding with its loop, a
 * halo breathing on the beat, a thin bar where the loop is). Lighting up and going out fade in
 * 220 ms. Everything that moves is read when drawn: neither the clock nor the beat recompose a pad.
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
    look: CellLook,
    col: Int,
    scene: Int,
    bars: FloatArray?,
    hint: Boolean,
    tag: String,
    press: () -> Boolean,
    retract: () -> Unit,
    mark: CellMark = CellMark.None,
    launch: () -> Unit,
) {
    SideEffect { livePadCompositions++ }
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(9.dp)
    // A track playing an empty cell: nothing lights up there.
    val lit = animateFloatAsState(if (playing && hasContent) 1f else 0f, tween(220), label = "pad-lit")
    val color = c.color
    val clock = look.clock
    val showDot = !hasContent && !pending && mark == CellMark.None && !hint
    Box(
        Modifier.size(width, height)
            // Brushes made once per size: a playing pad redraws every frame without garbage.
            .drawWithCache {
                onDrawBehind {
                    if (showDot) {
                        // The wave of the beat passing through this dot: the nearest playing cell's.
                        var best = 0f
                        var tint: Color? = null
                        look.sources.forEach { s ->
                            val w = clock.wave(waveDistance(col, scene, s, look.w, look.h, look.gap))
                            if (w > best) { best = w; tint = s.color }
                        }
                        emptyDot(best, tint)
                    }
                }
            }
            .clip(shape)
            .drawWithCache {
                val idle = Brush.linearGradient(listOf(GridLook.idleTop(color), GridLook.idleBottom(color)), Offset.Zero, Offset(size.width * 0.3f, size.height))
                val litFill = Brush.linearGradient(listOf(color, GridLook.litBottom(color)), Offset.Zero, Offset(size.width * 0.36f, size.height))
                val light = Brush.radialGradient(
                    0f to GridLook.litLight(color), 0.55f to Color.Transparent,
                    center = Offset(size.width * 0.2f, 0f), radius = size.width * 1.1f,
                )
                val barBrush = Brush.verticalGradient(listOf(GridLook.barTop(color), color.copy(alpha = 0.55f)))
                val sheenW = size.width * 0.45f
                val sheen = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.25f), Color.Transparent), 0f, sheenW)
                val line = 1.dp.toPx()
                val progTop = 6.dp.toPx()
                val progSide = 7.dp.toPx()
                val progH = 4.dp.toPx()
                val progR = CornerRadius(2.dp.toPx())
                val halo = 6.dp.toPx()
                val haloLine = halo / HALO_LAYERS
                val haloStroke = Stroke(haloLine)
                val haloColor = lerp(color, Color.White, 0.35f)
                val radius = 9.dp.toPx()
                onDrawBehind {
                    val on = lit.value
                    if (pending && on < 0.01f) {
                        drawRect(GridLook.queued(color))
                    } else if (hasContent) {
                        if (on < 0.99f) {
                            drawRect(idle)
                            drawRect(color.copy(alpha = 0.3f), size = Size(size.width, line))
                        }
                        if (on > 0.01f) {
                            drawRect(litFill, alpha = on)
                            drawRect(light, alpha = on)
                            drawRect(Color.White.copy(alpha = 0.4f * on), size = Size(size.width, line))
                            // The sheen slides across with the loop.
                            val x = size.width * (-0.5f + 1.6f * position())
                            translate(left = x) { drawRect(sheen, size = Size(sheenW, size.height), alpha = on) }
                        }
                        bars?.let { previewBars(it, if (on > 0.5f) null else barBrush, GridLook.Ink.copy(alpha = 0.8f), if (on > 0.5f) 1f else 0.75f) }
                        if (on > 0.01f) {
                            // The halo, breathing on the beat: a glow along the inside of the edge
                            // (outside, the grid's rows would cut it).
                            val glow = halo * (0.45f + 0.55f * clock.pulse)
                            for (k in 0 until HALO_LAYERS) {
                                val inset = halo * k / HALO_LAYERS + haloLine / 2
                                drawRoundRect(
                                    haloColor.copy(alpha = (on * glow / halo * 0.7f * (1f - k.toFloat() / HALO_LAYERS)).coerceIn(0f, 1f)),
                                    Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset),
                                    CornerRadius((radius - inset).coerceAtLeast(0f)), style = haloStroke,
                                )
                            }
                        }
                        if (on > 0.5f) {
                            // Where the loop is: a thin bar along the top.
                            val w = size.width - 2 * progSide
                            drawRoundRect(GridLook.Ink.copy(alpha = 0.2f), Offset(progSide, progTop), Size(w, progH), progR)
                            drawRoundRect(GridLook.Ink, Offset(progSide, progTop), Size(w * position(), progH), progR)
                        }
                    }
                }
            }
            .let { m ->
                when {
                    mark == CellMark.Rec -> m.border(3.dp, Neon.Red, shape)
                    mark == CellMark.Here -> m.border(2.dp, Neon.Amber, shape)
                    pending -> m.blinkingBorder(1.5.dp, 9.dp) { color.copy(alpha = 0.35f + 0.55f * blink.value) }
                    hint -> m.border(1.dp, Color(0xFF3A3F46), shape)
                    else -> m
                }
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
        if (mark != CellMark.None) {
            // Lot 4b: « ici » (the cell being filled) or « ● REC » (recording what is played).
            if (mark == CellMark.Rec) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(20.dp).graphicsLayer { alpha = 0.55f + 0.45f * clock.pulse }.clip(CircleShape).background(Neon.Red))
                    Text("REC", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                }
            } else {
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Neon.Amber).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(tr("ici"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, softWrap = false)
                }
            }
        } else if (pending) {
            Text("→", Modifier.graphicsLayer { alpha = 0.5f + 0.5f * blink.value }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        } else if (hint) {
            Text("+", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF8C949F))
        }
    }
}

@Composable
private fun StopButton(label: String, modifier: Modifier, waiting: Boolean, blink: State<Float>, tag: String, press: () -> Boolean, retract: () -> Unit, stop: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .let { m ->
                val red = Neon.Red
                if (waiting) m.drawBehind { drawRect(red.copy(alpha = 0.25f + 0.35f * blink.value)) } else m.background(GridLook.Panel)
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
        // « ■ Tout » in a narrow scene column (six tracks on a phone): smaller, never cut.
        val style = MaterialTheme.typography.titleSmall
        Text(
            label, style = style, color = Neon.TextPrimary, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 2.dp),
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
        )
    }
}

/** Lot 4d: a tab of the Live page's tools, as the dock and the open panel name it. */
@Composable
private fun tabLabel(t: LivePanelTab, sideBySide: Boolean = false): String = when (t) {
    LivePanelTab.Briques -> tr("Briques")
    LivePanelTab.Pads -> tr("Pads")
    LivePanelTab.Effets -> if (sideBySide) tr("Effets et 303") else tr("Effets")
    LivePanelTab.Notes -> tr("Notes")
    LivePanelTab.Acide -> tr("Acid 303")
}

private fun tabIcon(t: LivePanelTab) = when (t) {
    LivePanelTab.Briques -> Icons.Filled.GridView
    LivePanelTab.Pads -> Icons.Filled.Apps
    LivePanelTab.Effets -> Icons.Filled.AutoAwesome
    LivePanelTab.Notes -> Icons.Filled.MusicNote
    LivePanelTab.Acide -> Icons.Filled.GraphicEq
}

/** The tabs in order: Briques · Pads · Effets · Notes · Acid 303 (a wide screen: the 303 beside the effects). */
private fun liveTabs(sideBySide: Boolean): List<LivePanelTab> =
    if (sideBySide) listOf(LivePanelTab.Briques, LivePanelTab.Pads, LivePanelTab.Effets, LivePanelTab.Notes)
    else listOf(LivePanelTab.Briques, LivePanelTab.Pads, LivePanelTab.Effets, LivePanelTab.Notes, LivePanelTab.Acide)

/**
 * Lot 4d, the tools folded: one floating dock over the app's navigation, Briques · Pads · Effets ·
 * Notes · Acid 303 (an icon over each name), ⛶ and ︿; the grid has the rest of the screen.
 */
@Composable
private fun LiveDock(ui: LiveScreenUi, sideBySide: Boolean, choose: (LivePanelTab) -> Unit, unfold: () -> Unit, immersive: Boolean, toggleFull: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().background(GridLook.Ground).padding(horizontal = 10.dp, vertical = 6.dp)) {
        // A narrow dock (a small phone, a big text size): the icons alone, their names said.
        val words = maxWidth >= 300.dp
        Row(
            Modifier.fillMaxWidth().height(DOCK_HEIGHT).shadow(10.dp, shape).clip(shape).background(Neon.Surface)
                .border(1.dp, Color.White.copy(alpha = 0.06f), shape).padding(horizontal = 4.dp).testTag("live_dock"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            liveTabs(sideBySide).forEach { t ->
                val label = tabLabel(t, sideBySide)
                val selected = t == LivePanelTab.Briques
                // No 303 line in the project: its tab stays (its page says how to get one), greyed.
                val idle = t == LivePanelTab.Acide && !ui.hasAcid
                val ink = when {
                    selected -> Neon.Cyan
                    idle -> Neon.TextSecondary.copy(alpha = 0.6f)
                    else -> Neon.TextPrimary
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp))
                        .clickable { choose(t) }.testTag("live_tab_${t.name}")
                        .semantics(mergeDescendants = true) { this.selected = selected; if (!words) contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tabIcon(t), contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                    if (words) {
                        val style = MaterialTheme.typography.labelMedium
                        Text(
                            label, color = ink, style = style, maxLines = 1, softWrap = false,
                            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
                        )
                    }
                }
            }
            FullScreenButton(immersive, toggleFull)
            IconButton(onClick = unfold, modifier = Modifier.testTag("live_panel_fold")) {
                Icon(Icons.Filled.ExpandLess, contentDescription = tr("Déplier"))
            }
        }
    }
}

/** The dock's height (its floating margins aside). */
private val DOCK_HEIGHT = 60.dp

/**
 * Lot 4d, the tools open: a panel from the bottom (lying down, [sideways]: on the right). On top
 * the same tabs as the dock, as one segmented control; then a header with the page's title and its
 * tools as icons (the effects: Tout remettre, Verrouiller, Réglages; then ⛶ and Replier); then the
 * page (the effects: « L'effet », « Comment il joue », the pad). A wide screen upright: effects and
 * 303 side by side. [tight] (a short panel): no handle nor header, ⛶ and Replier beside the tabs,
 * the effects' tools in their own first row.
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
    fold: () -> Unit,
    tab: LivePanelTab,
    choose: (LivePanelTab) -> Unit,
    immersive: Boolean,
    toggleFull: () -> Unit,
    sideways: Boolean,
    modifier: Modifier,
) = BoxWithConstraints(modifier) {
    val tight = maxHeight < TIGHT_PANEL
    val shown = if (sideBySide && tab == LivePanelTab.Acide) LivePanelTab.Effets else tab
    val shape = if (sideways) RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp) else RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Column(Modifier.fillMaxSize().clip(shape).background(Neon.Surface).testTag("live_panel")) {
        if (!tight && !sideways) {
            // The sheet's handle: a touch folds it.
            Box(
                Modifier.fillMaxWidth().height(18.dp).clickable(onClickLabel = tr("Replier"), onClick = fold).testTag("live_panel_handle"),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(40.dp, 4.dp).clip(CircleShape).background(Neon.TextSecondary.copy(alpha = 0.5f))) }
        } else {
            Spacer(Modifier.height(6.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Segmented(ui, liveTabs(sideBySide), shown, sideBySide, choose, Modifier.weight(1f))
            if (tight) {
                FullScreenButton(immersive, toggleFull)
                FoldButton(sideways, fold)
            }
        }
        if (!tight) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionTitle(if (shown == LivePanelTab.Effets) tr("L'effet") else tabLabel(shown, sideBySide), Modifier.weight(1f))
                if (shown == LivePanelTab.Effets) LiveFxSafety(ui, actions, fx, Modifier, iconsOnly = true)
                FullScreenButton(immersive, toggleFull)
                FoldButton(sideways, fold)
            }
        } else {
            Spacer(Modifier.height(6.dp))
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(start = 10.dp, end = 10.dp, bottom = 6.dp)) {
            when {
                shown == LivePanelTab.Pads -> {
                    // A pad being set up (« Régler » from the pads view): its settings, under the pads, in view.
                    val scroll = rememberScrollState()
                    LaunchedEffect(pads?.editing, scroll.maxValue) { if (pads?.editing != null) scroll.animateScrollTo(scroll.maxValue) }
                    Column(Modifier.verticalScroll(scroll)) {
                        if (pads != null) LivePadsCard(pads, padsActions)
                    }
                }
                shown == LivePanelTab.Notes -> LiveNotes(ui, actions, Modifier.fillMaxSize())
                sideBySide -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveFx(ui, columns, actions, Modifier.weight(1f).fillMaxHeight(), fx, tools = tight)
                    LiveAcid(ui, actions, Modifier.weight(1f).fillMaxHeight())
                }
                shown == LivePanelTab.Acide -> LiveAcid(ui, actions, Modifier.fillMaxSize())
                else -> LiveFx(ui, columns, actions, Modifier.fillMaxSize(), fx, tools = tight)
            }
        }
    }
}

/** Below this height the open panel drops its handle and header (see [LivePanel]). */
private val TIGHT_PANEL = 380.dp

/** The open panel's tabs, one segmented control: the page shown lit; « Briques » folds the panel. */
@Composable
private fun Segmented(ui: LiveScreenUi, tabs: List<LivePanelTab>, shown: LivePanelTab, sideBySide: Boolean, choose: (LivePanelTab) -> Unit, modifier: Modifier) {
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(GridLook.Ground.copy(alpha = 0.7f)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { t ->
            val label = tabLabel(t, sideBySide)
            val selected = t == shown
            val idle = t == LivePanelTab.Acide && !ui.hasAcid
            // The tabs share the row by the length of their names (a bigger text makes them smaller).
            Box(
                Modifier.weight(label.length + 3f).fillMaxHeight().clip(RoundedCornerShape(21.dp)).background(if (selected) Neon.Cyan else Color.Transparent)
                    .clickable { choose(t) }.padding(horizontal = 2.dp).testTag("live_tab_${t.name}").semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                val style = MaterialTheme.typography.labelLarge
                Text(
                    label, color = if (selected) Neon.OnAccent else if (idle) Neon.TextSecondary.copy(alpha = 0.6f) else Neon.TextPrimary,
                    style = style, maxLines = 1, softWrap = false,
                    autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
                )
            }
        }
    }
}

/** ﹀ (lying down: →): the panel folds back to the dock (or the rail). */
@Composable
private fun FoldButton(sideways: Boolean, fold: () -> Unit) {
    IconButton(onClick = fold, modifier = Modifier.testTag("live_panel_fold")) {
        Icon(if (sideways) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.ExpandMore, contentDescription = tr("Replier"))
    }
}

/** The grid's column headers: the track's colour, its name, then a 48 dp M (lying down: [HEADER_COMPACT]). */
private val HEADER_HEIGHT = 80.dp
private val HEADER_COMPACT = 56.dp

/** A cell never gets smaller than a finger: past these the grid scrolls. */
private val MIN_CELL_W = 44.dp
private val MIN_CELL_H = 48.dp
/** Nor wider than a big pad, however few the tracks. */
private val MAX_CELL_W = 120.dp
/** Lot 4c: a brick's row stops shrinking here (« Cases » past what fits: the grid scrolls). */
private val ROW_MIN_H = 40.dp
/** Lot 4d: lower with the tools open (about six bricks stay in view over the panel). */
private val ROW_MIN_DENSE = 30.dp
/** The playing cell's halo: thin strokes inside its edge, fainter inwards. */
private const val HALO_LAYERS = 6
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
