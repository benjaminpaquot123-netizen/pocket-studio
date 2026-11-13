package com.pocketstudio.app.padlive

import kotlin.math.abs
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.session.PadClock
import com.pocketstudio.app.session.livePadColor
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.hit
import com.pocketstudio.ui.padLight
import com.pocketstudio.ui.rememberPadLight

/** What Pad Live shows (n° 25, lot B1, `docs/maquettes/padlive-trois.png`). */
class PadLiveUi(
    val pads: LivePads,
    /** Per pad of the four pages: 0 off, 1 waiting for its beat, 2 playing. */
    val states: List<Int>,
    val page: Int,
    /** The pack last put on the pads (its id, [PadLive.PACKS]). */
    val pack: String,
    val playing: Boolean,
    val tempoBpm: Double,
    val beatsPerBar: Int,
    /** « Préparation du pack Trap… » (null: ready). */
    val busy: String? = null,
    /** A held effect works on the music: the pads that play wear its veil. */
    val effectHeld: Boolean = false,
    val canUndo: Boolean = false,
    /** Lot B2: « Bégaie »'s division and the « Montée »'s length in bars (the project's effect settings). */
    val chop: LiveDivision = LiveDivision.SIXTEENTH,
    val riserBars: Int = 4,
    /** Lot B2: « Enregistrer ce que je joue ». */
    val played: PlayedTakeUi = PlayedTakeUi.Ready,
    /** Lot B2: the empty pad a take goes on (« A12 »; null: none free). */
    val emptyPad: String? = null,
    /** A word to show once (a take placed, too short…), null: none. */
    val message: String? = null,
    /** Lot B3: the challenge (« Défi ») and the pad its example lights now (null: none). */
    val challenge: ChallengeUi = ChallengeUi.Off,
    val challengeLit: Int? = null,
    /** Audit ultra round 1, n° 9: the best stars of each challenge (the panel's list). */
    val challengeStars: List<Int> = emptyList(),
    /** Lot B3: a Live take (● REC) just kept: « Perf gardée » with Écouter and Partager; its song lasts [perfSeconds]. */
    val takeDone: Boolean = false,
    val perfSeconds: Int = 0,
)

/**
 * Tour 6, padlive2 (P4, control seat A b #11: « le ▶ du haut reste ▶ avec 3 boucles en route »): something plays, the
 * transport or loops started on their own (waiting for their line included); the play buttons show ■ then.
 */
internal val PadLiveUi.soundsNow: Boolean get() = playing || states.any { it != 0 }

/** What a touch on a play button does: ▶ starts the music; ■ stops what plays (the transport's ■ also cuts the pads; pads alone: « Tout couper »). */
internal fun PadLiveActions.playOrStop(ui: PadLiveUi): () -> Unit = if (!ui.playing && ui.soundsNow) stopAll else togglePlay

/** Every gesture of Pad Live, given by the app. */
class PadLiveActions(
    val back: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val nudgeTempo: (Int) -> Unit = {},
    val openTempo: () -> Unit = {},
    /** A finger down (true) on a pad of any page, or up (false). */
    val touch: (pad: Int, down: Boolean) -> Unit = { _, _ -> },
    val setPage: (Int) -> Unit = {},
    val choosePack: (String) -> Unit = {},
    /** An empty pad: the one sound browser, for it. */
    val browse: (pad: Int) -> Unit = {},
    val setMode: (pad: Int, PadMode) -> Unit = { _, _ -> },
    val setGroup: (pad: Int, Int) -> Unit = { _, _ -> },
    val setBeats: (pad: Int, Int) -> Unit = { _, _ -> },
    val setGain: (pad: Int, Float) -> Unit = { _, _ -> },
    val clear: (pad: Int) -> Unit = {},
    val stopAll: () -> Unit = {},
    /** Held effects: Filtre (null: let go; 0 grave .. 1 aigu), Écho, Roulement, then Montée, Chute, Bégaie under « Plus ». */
    val filter: (Float?) -> Unit = {},
    /** Lot B2: the filter's resonance from its panel (0..1; null: back to its rest). */
    val resonance: (Float?) -> Unit = {},
    val echo: (Boolean) -> Unit = {},
    /** The roll at a division (lot B2: chosen on its panel; null: let go). */
    val roll: (LiveDivision?) -> Unit = {},
    val riser: () -> Unit = {},
    val brake: (Boolean) -> Unit = {},
    val stutter: (Boolean) -> Unit = {},
    val undo: () -> Unit = {},
    /** « Plus » › the whole Live page (bricks, notes, Acid 303) of the same project. */
    val openLive: () -> Unit = {},
    /** « Plus » › the effects box of the pads' strip. */
    val effects: () -> Unit = {},
    /** Lot B2: row [Int] (0..3) of the page shown launched together, on the bar. */
    val launchRow: (Int) -> Unit = {},
    /** Lot B2: a pad's own sound (pitch in semitones, backwards, attack and release in seconds). */
    val setPitch: (pad: Int, Int) -> Unit = { _, _ -> },
    val setReverse: (pad: Int, Boolean) -> Unit = { _, _ -> },
    val setAttack: (pad: Int, Float) -> Unit = { _, _ -> },
    val setRelease: (pad: Int, Float) -> Unit = { _, _ -> },
    /** Lot B2: « Enregistrer ce que je joue » touched, and where its take goes. */
    val played: () -> Unit = {},
    val placeOnPad: () -> Unit = {},
    val placeInBrick: () -> Unit = {},
    val dropTake: () -> Unit = {},
    val messageShown: () -> Unit = {},
    /** Lot B3: « Défi » opens the challenge panel; its buttons. */
    val challenge: () -> Unit = {},
    val challengeActions: ChallengeActions = ChallengeActions(),
    /** Lot B3: « Partager ma perf »; the kept take's « Écouter » and its line closed. */
    val share: () -> Unit = {},
    val listenTake: () -> Unit = {},
    val dismissTake: () -> Unit = {},
)

/**
 * Pad Live on the whole screen: the pads ready to play, the tempo, three held effects, « Plus »
 * for the rest. Above: back, ▶/■, « − 124 + », ● (the Live take, [record]). Then the pages A to
 * D and « Pack : Fête ▾ », the 16 pads of the page, the effects, « Plus ». Each pad wears the one
 * light of every pad ([padLight]): nothing moves on a pad that does not play.
 */
@Composable
fun PadLiveScreen(
    ui: PadLiveUi,
    actions: PadLiveActions,
    /** Transport steps since play (-1: stopped), read only by the pads' clock. */
    clockOf: () -> Int,
    record: @Composable () -> Unit = {},
    /** Tests: the pads' clock does not run (a still capture). */
    still: Boolean = false,
    /** Lot B3: the pads' levels (the engine's, [com.pocketstudio.engine.NativeEngine.clipLevels]); null: the lights stay full while they play. */
    readLevels: ((FloatArray) -> Unit)? = null,
    /** Lot B3: the levels the lights read (tests give theirs). */
    levels: PadLevels = remember { PadLevels() },
    /** Lot B3: « Fête » shown (tests). */
    partyAtStart: Boolean = false,
) {
    var party by rememberSaveable { mutableStateOf(partyAtStart) }
    if (party) {
        androidx.activity.compose.BackHandler { party = false }
        PartyScreen(ui, actions, clockOf, levels, readLevels, leave = { party = false }, still = still)
        return
    }
    var more by rememberSaveable { mutableStateOf(false) }
    var settingsOf by rememberSaveable { mutableStateOf<Int?>(null) }
    val held = remember { HeldFx() }
    val clock = remember { PadClock() }
    val statesNow by rememberUpdatedState(ui.states)
    val clipsNow by rememberUpdatedState(ui.pads.all)
    val clockNow by rememberUpdatedState(clockOf)
    val tempoNow by rememberUpdatedState(ui.tempoBpm)
    val barNow by rememberUpdatedState(ui.beatsPerBar)
    val readNow by rememberUpdatedState(readLevels)
    // One frame clock for every pad, only while something plays: still otherwise.
    val active = !still && (ui.playing || ui.states.any { it != 0 })
    LaunchedEffect(active) {
        if (!active) {
            clock.stop()
            // Stopped: every light falls dark (a still capture keeps the levels it was given).
            if (!still) levels.clear()
            return@LaunchedEffect
        }
        val tick: (Long) -> Unit = { n ->
            clock.tick(n, clockNow(), statesNow, clipsNow, tempoNow, barNow)
            readNow?.let { levels.update(it) }
        }
        androidx.compose.runtime.withFrameNanos(tick)
        while (true) androidx.compose.animation.core.withInfiniteAnimationFrameNanos(tick)
    }
    // Lot D: lying down (a phone turned, a tablet), the pads fill the left and the scene's
    // controls (pages, effects, « Plus », the big red button) stand on the right, in reach of
    // the other hand; upright, one column as on the mockup.
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(Neon.Background, Color.Black, 0.2f), lerp(Neon.Background, Color.Black, 0.5f))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp)
            .testTag("padlive"),
    ) {
        val lying = padLiveLying(maxWidth, maxHeight)
        val side = padLiveSidePanel(maxWidth)
        val notes: @Composable () -> Unit = {
            ui.busy?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("padlive_busy"))
            }
            if (ui.busy == null) ui.message?.let { PadLiveMessage(it, actions.messageShown) }
            if (ui.takeDone) PerfLine(ui, actions)
        }
        if (!lying) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PadLiveBar(ui, actions, record)
                PageRow(ui, actions)
                // Audit lot 6, U21: the notes (« Perf gardée », a message) under the pads, in the room the grid leaves
                // below them: above, they pushed the pads down and made them smaller under the playing finger.
                PadsGrid(ui, actions, clock, held, readLevels, levels, settings = { settingsOf = it }, Modifier.fillMaxWidth().weight(1f))
                notes()
                PadLiveControls(ui, actions, more, toggleMore = { more = !more }, held, party = { party = true })
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PadLiveBar(ui, actions, record)
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PadsGrid(ui, actions, clock, held, readLevels, levels, settings = { settingsOf = it }, Modifier.fillMaxWidth().weight(1f).testTag("padlive_pads_side"))
                        notes()
                        Spacer(Modifier.height(0.dp))
                    }
                    Column(
                        Modifier.width(side).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("padlive_side"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PageRow(ui, actions)
                        PadLiveControls(ui, actions, more, toggleMore = { more = !more }, held, party = { party = true })
                    }
                }
            }
        }
    }
    settingsOf?.let { pad ->
        val clip = ui.pads.at(pad)
        if (clip == null) settingsOf = null
        else PadSettingsSheet(pad, clip, actions, close = { settingsOf = null })
    }
    (ui.played as? PlayedTakeUi.Done)?.let { PlayedTakeSheet(it.bars, ui.emptyPad, actions) }
}

/** Lot D: Pad Live lying down (pads on the left, controls on the right) from this room on. */
internal fun padLiveLying(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp): Boolean = width > height && width >= 520.dp

/** Lying down, the controls' column: about a third of the screen. */
private fun padLiveSidePanel(width: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp = (width * 0.36f).coerceIn(250.dp, 480.dp)

/** The 16 pads of the page shown, each row's ▶ at its end, a held effect's panel over the last rows. */
@Composable
private fun PadsGrid(
    ui: PadLiveUi,
    actions: PadLiveActions,
    clock: PadClock,
    held: HeldFx,
    readLevels: ((FloatArray) -> Unit)?,
    levels: PadLevels,
    settings: (Int) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier) {
        val gap = 8.dp
        // Lot B2: each row's ▶ at its end (the row launched together, on the bar).
        val byWidth = (maxWidth - ROW_BUTTON - gap * 4) / 4
        val byHeight = (maxHeight - gap * 3) / 4
        // Square or a little taller when the screen has room (a phone upright), never past its row.
        val side = minOf(byWidth * 1.15f, byHeight)
        Column(Modifier.align(Alignment.TopCenter), verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                    for (col in 0 until 4) {
                        val slot = row * 4 + col
                        val pad = PadLive.pad(ui.page, slot)
                        val clip = ui.pads.at(pad)
                        // A challenge's example lights its pads as it plays (as if they played).
                        val state = if (ui.challengeLit == pad) 2 else ui.states.getOrElse(pad) { 0 }
                        PadLivePad(
                            pad, clip, state, clock, ui.effectHeld,
                            Modifier.size(width = byWidth.coerceAtMost(side * 1.35f), height = side),
                            touch = actions.touch,
                            empty = { actions.browse(pad) },
                            settings = { settings(pad) },
                            level = if (readLevels != null && ui.challengeLit != pad) ({ levels.of(pad) }) else null,
                        )
                    }
                    RowButton(ui, row, Modifier.size(width = ROW_BUTTON, height = side), actions.launchRow)
                }
            }
        }
        // A held effect's panel over the pads' last rows (the pads do not move under the finger).
        HeldFxPanel(held, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
    }
}

/** Under (upright) or beside (lying down) the pads: the effects or the challenge, « Plus », the big red button. */
@Composable
private fun PadLiveControls(ui: PadLiveUi, actions: PadLiveActions, more: Boolean, toggleMore: () -> Unit, held: HeldFx, party: () -> Unit) {
    if (ui.challenge != ChallengeUi.Off) ChallengePanel(ui.challenge, ui.pads, actions.challengeActions, best = ui.challengeStars, screenBpm = ui.tempoBpm)
    else SceneEffects(ui, actions, more, held)
    AnimatedVisibility(more) { MoreRow(ui, actions, party = party) }
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Plan of the audits, step 4, n° 7 (G8, 4A): « Tout couper » always in sight, by the thumb (it was behind « Plus »).
        StopAllButton(actions.stopAll)
        PlayedButton(ui.played, actions.played, Modifier.weight(1f))
        // « Plus » open, « Moins » takes the room (« Aufnehmen, was ich spiele » must not be cut): the « ? » waits.
        if (!more) com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.ScreenHelp.PADLIVE_PLAYED, Modifier.align(Alignment.CenterVertically), about = listOf("padlive_played"))
        Row(
            Modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(if (more) Neon.SurfaceHigh else Neon.Surface)
                .clickable(role = Role.Button, onClick = toggleMore).padding(horizontal = 16.dp).testTag("padlive_more"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Audit lot 6, U18: open, it folds (« Moins », « Tutup », « 閉じる »), no more the ⋯ of a menu still to open.
            if (more) Icon(Icons.Filled.ExpandLess, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.testTag("padlive_more_fold"))
            else Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.testTag("padlive_more_dots"))
            Text(if (more) tr("Moins") else tr("Plus"), style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary)
        }
    }
}

/** ← Pad Live · ▶/■ · « − 124 + » · ●. */
@Composable
private fun PadLiveBar(ui: PadLiveUi, actions: PadLiveActions, record: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.back, modifier = Modifier.size(44.dp).testTag("padlive_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"), tint = Neon.Amber)
        }
        // Lot D2: smaller before it is cut (« Pad… » on a 360 dp phone), in every language.
        com.pocketstudio.ui.FitText(tr("Pad Live"), Modifier.weight(1f).testTag("padlive_title"), MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val sounding = ui.soundsNow
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(if (sounding) Neon.Acid else Neon.SurfaceHigh)
                .clickable(role = Role.Button, onClick = actions.playOrStop(ui)).testTag("padlive_play")
                .semantics { contentDescription = if (sounding) tr("Arrêter") else tr("Jouer") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (sounding) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = if (sounding) Color.Black else Neon.TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Neon.Surface), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { actions.nudgeTempo(-1) }, modifier = Modifier.size(44.dp).testTag("padlive_tempo_less")) {
                Icon(Icons.Filled.Remove, contentDescription = tr("Moins vite"))
            }
            Column(
                Modifier.clickable(role = Role.Button, onClick = actions.openTempo).testTag("padlive_tempo"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${ui.tempoBpm.toInt()}", style = MonoStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Neon.Amber)
                Text(tr("BPM"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
            }
            IconButton(onClick = { actions.nudgeTempo(1) }, modifier = Modifier.size(44.dp).testTag("padlive_tempo_more")) {
                Icon(Icons.Filled.Add, contentDescription = tr("Plus vite"))
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.testTag("padlive_rec")) { record() }
    }
}

/**
 * One pad of Pad Live: its colour, its name at the bottom, how it plays in the corner (a touch
 * there: its settings), the one light of every pad. A sound pad acts on the finger's down (a hit
 * at once; « tenir » until the finger leaves); an empty one opens the sound browser.
 */
@Composable
private fun PadLivePad(
    pad: Int,
    clip: LiveClip?,
    state: Int,
    clock: PadClock,
    veil: Boolean,
    modifier: Modifier,
    touch: (Int, Boolean) -> Unit,
    empty: () -> Unit,
    settings: () -> Unit,
    level: (() -> Float)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val light = rememberPadLight()
    val color = clip?.let { livePadColor(it.name, pad % PadLive.PAGE_SIZE) } ?: Neon.Surface
    val shape = RoundedCornerShape(14.dp)
    val touchNow by rememberUpdatedState(touch)
    val emptyNow by rememberUpdatedState(empty)
    val settingsNow by rememberUpdatedState(settings)
    val has by rememberUpdatedState(clip != null)
    val loops = clip != null && !clip.oneShot
    val beats = clip?.beats ?: 4
    val name = clip?.let { DefaultNames.shownSample(it.name) }
    Box(
        modifier
            .clip(shape)
            .background(
                if (clip == null) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.03f), Color.Black.copy(alpha = 0.18f)))
                else Brush.verticalGradient(listOf(lerp(Neon.SurfaceHigh, color, 0.5f), lerp(Neon.Surface, color, 0.22f))),
            )
            .border(1.dp, if (clip == null) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.10f), shape)
            .padLight(
                light, color, playing = clip != null && state == 2, waiting = clip != null && state == 1,
                progress = if (loops) ({ clock.position(pad, beats) }) else null,
                veil = veil && state == 2, corner = 14.dp, touchable = clip != null, level = level,
            )
            .pointerInput(pad) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!has) {
                        if (waitForUpOrCancellation() != null) emptyNow()
                        return@awaitEachGesture
                    }
                    // The corner's word (its settings) is read here, from the pad's own touch: a small
                    // clickable there got the 48 dp touch room of its own and reached the pad's middle.
                    val corner = padCorner(size.width.toDp(), size.height.toDp())
                    if (down.position.x >= size.width - corner.first.toPx() && down.position.y <= corner.second.toPx()) {
                        if (waitForUpOrCancellation() != null) settingsNow()
                        return@awaitEachGesture
                    }
                    // The pad pressed, kept: a page switch restarts this with another index, and
                    // a « tenir » pad must still let go of this one (fix 2, n° 2).
                    val localPad = pad
                    haptics.hit()
                    touchNow(localPad, true)
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        touchNow(localPad, false)
                    }
                }
            }
            .semantics {
                // Lot B3: the same words on Pad Live and in « Fête » (sound, mode, state, place).
                contentDescription = padDescription(pad, clip, state)
                role = Role.Button
                onClick {
                    if (clip == null) emptyNow() else { touchNow(pad, true); touchNow(pad, false) }
                    true
                }
                if (clip != null) onLongClick(tr("Réglages du pad")) { settings(); true }
            }
            .testTag("padlive_pad_$pad"),
    ) {
        if (clip == null) {
            Text("+", Modifier.align(Alignment.Center), color = Neon.TextSecondary.copy(alpha = 0.55f), fontSize = 26.sp, fontWeight = FontWeight.Light)
            return@Box
        }
        // How it plays, in the corner: a touch there opens its settings (read by the pad's own touch above).
        // Audit ultra round 1, n° 7: on a phone lying down the pads are low, and the corner reached their
        // middle (a touch to play opened the settings): at most a third of the pad's height ([padCorner]).
        BoxWithConstraints(Modifier.matchParentSize()) {
        val corner = padCorner(maxWidth, maxHeight)
        Box(
            Modifier.align(Alignment.TopEnd).size(width = corner.first, height = corner.second)
                // Round 3, n° 7: a spoken name, not only an action label (TalkBack read « bouton », 16 times a page).
                .semantics {
                    contentDescription = tr("Réglages du pad") + ", " + tr("pad %s", padName(pad))
                    role = Role.Button
                    onClick(tr("Réglages du pad")) { settings(); true }
                }
                .testTag("padlive_mode_$pad"),
        )
        // Audit stage 1: one line that gets smaller (« przytrzymaj », « uderzenie · 1 » lost their end, word by word).
        // Relecture tour 1 (intégration): drawn over the pad's width, not the touch corner's alone (« coup · 1 » was « coup… »);
        // the corner (its touch, read above) stays where it was, under the word's end.
        com.pocketstudio.ui.FitText(
            modeWord(PadLive.modeOf(clip)) + if (clip.group > 0) " · ${clip.group}" else "",
            // On a dark veil: readable (WCAG AA) over the pad's light top too (audit stages 1 and 5: 2.2 to 3.4:1 before).
            Modifier.align(Alignment.TopEnd).widthIn(max = (maxWidth - 12.dp).coerceAtLeast(corner.first))
                .padding(top = 4.dp, end = 6.dp).clip(RoundedCornerShape(6.dp)).background(com.pocketstudio.ui.PadWordScrim)
                .padding(horizontal = 4.dp).testTag("padlive_word_$pad"),
            MaterialTheme.typography.labelSmall, color = Neon.TextPrimary, min = 8.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        }
        // A short pad (a small phone with « Plus » open): its name a little smaller, clear of the corner's word.
        BoxWithConstraints(Modifier.matchParentSize()) {
            val style = MaterialTheme.typography.titleSmall
            // Lot D2: a word is never split over two lines (« Monté/e »): smaller first.
            com.pocketstudio.ui.FitText(
                name.orEmpty(),
                // (Audit stage 1: room for its second line on a short pad; 24 dp keeps it clear of the corner word in tall scripts, Arabic at 360 dp.)
                Modifier.align(Alignment.BottomStart).heightIn(max = (maxHeight - 24.dp).coerceAtLeast(16.dp))
                    .padding(horizontal = 10.dp).padding(bottom = 4.dp).testTag("padlive_name_$pad"),
                style, color = Neon.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 2, min = 9.sp,
            )
        }
    }
}

/** The corner of a pad [w] × [h] whose touch opens its settings: never down to its middle (audit ultra round 1, n° 7). */
internal fun padCorner(w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> =
    minOf(64.dp, w * 0.6f) to minOf(36.dp, h * 0.32f)

/** A pad's mode in one small word (the mockup's « boucle », « coup », « tenir »). */
internal fun modeWord(mode: PadMode): String = when (mode) {
    PadMode.SHOT -> tr("coup")
    PadMode.LOOP -> tr("boucle")
    PadMode.HOLD -> tr("tenir")
}

/**
 * « Effets de scène · Appuie et maintiens »: Filtre (the finger's place on it: grave to aigu; slid
 * up, the panel's grid: cutoff and resonance), Écho, Roulement (slid up: its division); with
 * « Plus », Montée (over the project's riser length), Chute and Bégaie too. Let go: everything comes back.
 */
@Composable
private fun SceneEffects(ui: PadLiveUi, actions: PadLiveActions, more: Boolean, held: HeldFx) {
    val heldMove: (HeldKind, androidx.compose.ui.geometry.Offset?, Float) -> Unit = { kind, at, across ->
        when (val c = held.move(kind, at, across)) {
            is HeldFx.Change.Filter -> {
                actions.filter(c.cutoff)
                actions.resonance(c.resonance)
            }
            is HeldFx.Change.FilterOff -> {
                actions.filter(null)
                if (c.resonance) actions.resonance(null)
            }
            is HeldFx.Change.Roll -> actions.roll(c.division)
            HeldFx.Change.RollOff -> actions.roll(null)
            null -> Unit
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("EFFETS LIVE"), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
            com.pocketstudio.ui.HelpDot(
                com.pocketstudio.app.help.ScreenHelp.PADLIVE_EFFECTS,
                about = listOf("padlive_fx_filter", "padlive_fx_echo", "padlive_fx_roll", "padlive_fx_riser", "padlive_fx_brake", "padlive_fx_stutter"),
            )
            Spacer(Modifier.weight(1f))
            Text(tr("Appuie et maintiens"), style = MaterialTheme.typography.labelMedium, color = Neon.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeldEffect(tr("Filtre"), tr("grave ↔ aigu"), "filter", Modifier.weight(1f), track = { at, x -> heldMove(HeldKind.FILTER, at, x) }) {}
            HeldEffect(tr("Écho"), tr("en sortie"), "echo", Modifier.weight(1f), hold = actions.echo)
            HeldEffect(tr("Roulement"), HeldFx.divisionWords(held.division), "roll", Modifier.weight(1f), track = { at, x -> heldMove(HeldKind.ROLL, at, x) }) {}
        }
        if (more) {
            // Audit ultra round 1, n° 8: the Montée goes on for its bars once the finger is up: its button
            // stays lit, the bar under its words emptying as the bars go by (nothing showed it before).
            var riserFrom by remember { mutableStateOf(-1L) }
            var riserLeft by remember { mutableStateOf(0f) }
            val riserMs = (ui.riserBars * ui.beatsPerBar * 60_000.0 / ui.tempoBpm.coerceAtLeast(1.0)).toLong().coerceAtLeast(1L)
            LaunchedEffect(riserFrom) {
                if (riserFrom < 0) return@LaunchedEffect
                var start = -1L
                riserLeft = 1f
                while (riserLeft > 0f) {
                    androidx.compose.runtime.withFrameMillis { t ->
                        if (start < 0) start = t
                        riserLeft = (1f - (t - start).toFloat() / riserMs).coerceIn(0f, 1f)
                    }
                }
                riserFrom = -1L
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeldEffect(
                    tr("Montée"), riserWords(ui.riserBars), "riser", Modifier.weight(1f),
                    lit = riserLeft > 0f, progress = riserLeft.takeIf { it > 0f }, onRelease = true,
                    hold = { if (it) { actions.riser(); riserFrom = System.nanoTime() } },
                )
                HeldEffect(tr("Chute"), tr("freinage"), "brake", Modifier.weight(1f), hold = actions.brake)
                HeldEffect(tr("Bégaie"), HeldFx.divisionWords(ui.chop), "stutter", Modifier.weight(1f), hold = actions.stutter)
            }
        }
    }
}

/** G14: a held effect's first moments, where a glide up or down is the column's scroll, not the effect. */
private const val GLIDE_MS = 120L

/** The riser's length in words that fit its button on a phone (« 4 mesures »; the B1 « souffle qui monte » was cut). */
internal fun riserWords(bars: Int): String = com.pocketstudio.model.i18n.trCount(bars, "1 mesure", "%d mesures")

/**
 * A held effect's button: on while the finger stays ([hold]); [track]: also where the finger is
 * (window coordinates, null: let go) and its place across the button (0..1).
 */
@Composable
private fun HeldEffect(
    title: String,
    line: String,
    tag: String,
    modifier: Modifier,
    track: ((androidx.compose.ui.geometry.Offset?, Float) -> Unit)? = null,
    /** Audit ultra round 1, n° 8: lit as if held (the Montée going on after the finger left), with what is [progress] left (1..0). */
    lit: Boolean = false,
    progress: Float? = null,
    /** Montée (G14): starts when a touch ends without gliding, never on a glide. */
    onRelease: Boolean = false,
    hold: (Boolean) -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    val on = held || lit
    val holdNow by rememberUpdatedState(hold)
    val trackNow by rememberUpdatedState(track)
    var coords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.heightIn(min = 60.dp).clip(shape).background(if (on) Neon.Amber.copy(alpha = 0.22f) else Neon.Surface)
            .border(if (on) 2.dp else 1.dp, if (on) Neon.Amber else Color.White.copy(alpha = 0.06f), shape)
            .onGloballyPositioned { coords = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    held = true
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    fun at(p: androidx.compose.ui.geometry.Offset) = coords?.takeIf { it.isAttached }?.localToWindow(p) ?: p
                    trackNow?.invoke(at(down.position), (down.position.x / w).coerceIn(0f, 1f))
                    // Plan of the audits, step 4, n° 7 (G14, T5-Ed-40, tel-11: a glide lit « Montée »): an effect that is
                    // not slid (all but Filtre and Roulement) lets a glide scroll the column. [onRelease] (Montée): it
                    // starts when the finger leaves without gliding; a held one starts at once and lets go if the finger
                    // glides up or down in its first moments ([GLIDE_MS]), the move left to the column.
                    val glides = trackNow == null
                    if (!onRelease) holdNow(true)
                    var on = !onRelease
                    var glided = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (onRelease && !glided) {
                                    holdNow(true)
                                    on = true
                                }
                                break
                            }
                            if (glides && !glided) {
                                val d = change.position - down.position
                                val early = onRelease || change.uptimeMillis - down.uptimeMillis <= GLIDE_MS
                                if (early && (change.isConsumed || (abs(d.y) > viewConfiguration.touchSlop && abs(d.y) > abs(d.x)))) {
                                    glided = true
                                    held = false
                                    if (on) holdNow(false)
                                    on = false
                                }
                                if (!early) change.consume()
                                continue
                            }
                            if (glided) continue
                            trackNow?.invoke(at(change.position), (change.position.x / w).coerceIn(0f, 1f))
                            change.consume()
                        }
                    } finally {
                        // Also when the button goes under the finger (« Moins », turning, « Fête »):
                        // the effect never stays on (fix 2, n° 2).
                        held = false
                        trackNow?.invoke(null, 0.5f)
                        if (on) holdNow(false)
                    }
                }
            }
            .semantics {
                contentDescription = "$title · $line"
                role = Role.Button
                if (on) stateDescription = tr("en cours")
                onClick {
                    trackNow?.invoke(null, 0.5f)
                    holdNow(true); holdNow(false); true
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("padlive_fx_$tag"),
        verticalArrangement = Arrangement.Center,
    ) {
        // Three buttons across a small phone: the words get a little smaller before they are cut
        // (lot B2: « souffle qui monte » and « grave ↔ aigu » were, StillFitsTest-style check in PadLiveSceneTest).
        FitText(title, MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), Neon.TextPrimary, "padlive_fx_${tag}_title")
        FitText(line, MaterialTheme.typography.labelSmall, Neon.TextSecondary, "padlive_fx_${tag}_line")
        progress?.let { left ->
            Box(
                Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Neon.SurfaceHigh)
                    .testTag("padlive_fx_${tag}_progress"),
            ) { Box(Modifier.fillMaxWidth(left).height(3.dp).background(Neon.Amber)) }
        }
    }
}

/** One line that shrinks (down to 9 sp) before it is cut: the shared [com.pocketstudio.ui.FitText]. */
@Composable
private fun FitText(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, tag: String) =
    com.pocketstudio.ui.FitText(text, Modifier.fillMaxWidth().testTag(tag), style, color = color)

/**
 * A row's ▶ (lot B2): its pads launched together on the next bar line; ■ when its loops all play
 * (a touch stops them). Nothing on a row without a sound.
 */
@Composable
private fun RowButton(ui: PadLiveUi, row: Int, modifier: Modifier, launch: (Int) -> Unit) {
    val first = PadLive.pad(ui.page, row * PadLive.ROW_SIZE)
    val pads = (first until first + PadLive.ROW_SIZE)
    val filled = pads.filter { ui.pads.at(it) != null }
    if (filled.isEmpty()) {
        Box(modifier)
        return
    }
    val loops = filled.filter { ui.pads.at(it)?.oneShot == false }
    val allOn = loops.isNotEmpty() && loops.all { ui.states.getOrElse(it) { 0 } != 0 }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.clip(shape).background(Neon.Surface).border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .clickable(role = Role.Button, onClickLabel = tr("Lancer la ligne")) { launch(row) }
            .semantics { contentDescription = if (allOn) tr("Arrêter la ligne %d", row + 1) else tr("Lancer la ligne %d à la mesure", row + 1) }
            .testTag("padlive_row_$row"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (allOn) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null,
            tint = if (allOn) Neon.Acid else Neon.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

/** A row's ▶'s width. */
private val ROW_BUTTON = 26.dp

/**
 * « Plus »: everything stopped, « Annuler », the whole Live page, the pads' effects box; lot B3:
 * « Fête » (the pads on the whole screen) and « Partager » (ma perf).
 */
@Composable
private fun MoreRow(ui: PadLiveUi, actions: PadLiveActions, party: () -> Unit) {
    // Two rows of three (lot B3: six buttons on one row cut « Briques » in two on a 360 dp phone).
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MoreButton(Icons.AutoMirrored.Filled.Undo, tr("Annuler (défaire)"), "padlive_undo", Modifier.weight(1f), actions.undo, enabled = ui.canUndo)
            MoreButton(Icons.Filled.GridView, tr("Briques"), "padlive_live", Modifier.weight(1f), actions.openLive)
            MoreButton(Icons.Filled.Tune, tr("Effets"), "padlive_effects", Modifier.weight(1f), actions.effects)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MoreButton(Icons.Filled.Celebration, tr("Gros pads"), "padlive_party_open", Modifier.weight(1f), party)
            // Audit lot 6, R10: a kept take's line has its own « Partager » (the same sheet): one only on the screen.
            if (ui.takeDone) Spacer(Modifier.weight(1f))
            else MoreButton(Icons.Filled.Share, tr("Partager"), "padlive_share", Modifier.weight(1f), actions.share)
            com.pocketstudio.ui.HelpDot(
                com.pocketstudio.app.help.ScreenHelp.PADLIVE_MORE, Modifier.align(Alignment.CenterVertically),
                about = listOf("padlive_stop_all", "padlive_live", "padlive_effects", "padlive_party_open", "padlive_share"),
            )
        }
    }
}

/**
 * « Tout couper » (G8, Benjamin's 4A): a black ■ on red with its word under it (black as on the green ▶: white on this red
 * reads at 2.8:1 only), beside « Enregistrer ce que je joue »:
 * the pads, the music and its bars stop (RhythmViewModel.cutEverything).
 */
@Composable
private fun StopAllButton(stop: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(STOP_ALL_WIDTH).heightIn(min = 52.dp).clip(shape).background(Neon.Red)
            .clickable(role = Role.Button, onClick = stop).padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("padlive_stop_all"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
        com.pocketstudio.ui.FitText(
            tr("Tout couper"), Modifier.fillMaxWidth().testTag("padlive_stop_all_words"), MaterialTheme.typography.labelSmall,
            color = Color.Black, maxLines = 2, min = 8.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** « Tout couper »'s width: its word whole on two lines at most, « Enregistrer ce que je joue » keeps the rest. */
private val STOP_ALL_WIDTH = 64.dp

/** Lot B3: a Live take just kept (● REC, then Fin): « Perf gardée · 0:42 », « Écouter », « Partager », ✕. */
@Composable
private fun PerfLine(ui: PadLiveUi, actions: PadLiveActions) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Neon.SurfaceHigh).padding(start = 12.dp)
            .testTag("padlive_perf"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Audit ultra round 1, n° 10: « Performan… » was cut (pt-BR, 360 dp): two lines that get smaller before any cut.
        com.pocketstudio.ui.FitText(
            tr("Perf gardée · %s", "%d:%02d".format(ui.perfSeconds / 60, ui.perfSeconds % 60)), Modifier.weight(1f).testTag("padlive_perf_text"),
            MaterialTheme.typography.bodyMedium, color = Neon.Acid, fontWeight = FontWeight.Bold, maxLines = 2,
        )
        androidx.compose.material3.TextButton(onClick = actions.listenTake, modifier = Modifier.heightIn(min = 48.dp).testTag("padlive_perf_listen")) {
            Text(tr("Écouter"), maxLines = 1)
        }
        androidx.compose.material3.TextButton(onClick = actions.share, modifier = Modifier.heightIn(min = 48.dp).testTag("padlive_perf_share")) {
            Text(tr("Partager"), maxLines = 1, fontWeight = FontWeight.Bold, color = Neon.Amber)
        }
        IconButton(onClick = actions.dismissTake, modifier = Modifier.size(44.dp).testTag("padlive_perf_close")) {
            Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MoreButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tag: String, modifier: Modifier, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(Neon.Surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 8.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (enabled) Neon.TextPrimary else Neon.TextSecondary.copy(alpha = 0.5f)
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            autoSize = com.pocketstudio.ui.WholeWordsAutoSize(9.sp, MaterialTheme.typography.labelMedium.fontSize),
        )
    }
}
