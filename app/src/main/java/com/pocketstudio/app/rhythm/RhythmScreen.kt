@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.rhythm

import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.ScaledDropdownMenu

import com.pocketstudio.app.learn.spot
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.percent

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketstudio.model.PadSample
import com.pocketstudio.ui.LevelMeter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import com.pocketstudio.ui.aspectRatioAtMost
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.Project
import com.pocketstudio.model.hasOwnFx
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.sceneLabel
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

data class RhythmActions(
    val togglePlay: () -> Unit = {},
    val toggleMetronome: () -> Unit = {},
    val toggleCountIn: () -> Unit = {},
    val hitPad: (Int) -> Unit = {},
    val toggleStep: (Int) -> Unit = {},
    /** Drum loop length in bars (1 to 4). */
    val setBars: (Int) -> Unit = {},
    val fill: (every: Int) -> Unit = {},
    val rotate: (by: Int) -> Unit = {},
    val cycleRoll: (step: Int) -> Unit = {},
    /** The selected pad's pan, -1..1. */
    val setPadPan: (Float) -> Unit = {},
    /** One hit's own pan (-1..1) and pitch (semitones) on the selected pad. */
    val setHitPan: (step: Int, pan: Float) -> Unit = { _, _ -> },
    val setHitPitch: (step: Int, pitch: Int) -> Unit = { _, _ -> },
    /** One hit's strength, 0.05..1, on the selected pad (« Force »). */
    val setHitVelocity: (step: Int, velocity: Float) -> Unit = { _, _ -> },
    /** Opens the drums' quick effects. */
    val openEffects: () -> Unit = {},
    /** Opens the effects of the selected pad only. */
    val openPadEffects: () -> Unit = {},
    /** The pad's sample on the synth keyboard (opens Synthé). */
    val toKeyboard: () -> Unit = {},
    /** Opens the sound library for the selected pad. */
    val openLibrary: () -> Unit = {},
    /** All-pads grid: (lane, step). */
    val toggleHit: (Int, Int) -> Unit = { _, _ -> },
    val cycleVelocity: (Int) -> Unit = {},
    val clearPad: () -> Unit = {},
    val nudgeTempo: (Int) -> Unit = {},
    val tapTempo: () -> Unit = {},
    /** A touch on the tempo: the tempo sheet (keypad, ready-made tempos), as from the top. */
    val openTempo: () -> Unit = {},
    val setSwing: (Double) -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
    val importSample: () -> Unit = {},
    val startRecording: () -> Unit = {},
    val stopRecording: () -> Unit = {},
    val removeSample: () -> Unit = {},
    val dismissSampleError: () -> Unit = {},
    val editSample: (key: String?, transform: (PadSample) -> PadSample) -> Unit = { _, _ -> },
    val openSlicer: () -> Unit = {},
    val closeSlicer: () -> Unit = {},
    val setSliceMode: (SliceMode) -> Unit = {},
    val setSliceSensitivity: (Float) -> Unit = {},
    val setSliceGrid: (Int) -> Unit = {},
    val toggleSliceMarker: (Float) -> Unit = {},
    val applySlices: () -> Unit = {},
    val resampleSong: (bars: Int) -> Unit = {},
    val cancelResample: () -> Unit = {},
    val importFolder: () -> Unit = {},
    /** A sample pack as a .zip file. */
    val importZip: () -> Unit = {},
    /** A pad held: chosen without sounding (its sheet opens on it). */
    val selectPad: (Int) -> Unit = {},
    /** Pad (lane)'s own volume (0.8 = the kit's level), pan (-1..1), cut. */
    val setPadVolume: (Int, Float) -> Unit = { _, _ -> },
    val setPadPanOf: (Int, Float) -> Unit = { _, _ -> },
    /** Pad (lane)'s pan gesture ended (one journal line per gesture). */
    val padPanDone: (Int) -> Unit = {},
    val togglePadMute: (Int) -> Unit = {},
    /** Pad (lane) back to the kit's own voice. */
    val removeSampleOf: (Int) -> Unit = {},
    /** Sounds of the pad's kind its sheet offers (names), and one of them put on pad (lane). */
    val padSounds: (Int) -> List<String> = { emptyList() },
    val pickPadSound: (Int, Int) -> Unit = { _, _ -> },
    /** « Kit ▾ »: the list is about to show (packs read again), and a kit chosen (whole kit, one undo step). */
    val openKits: () -> Unit = {},
    val chooseKit: (String) -> Unit = {},
    /** A finger drawn across the steps: a cell set ([on]) or emptied; [paintEnd] when lifted (one undo step). */
    val paintStep: (step: Int, on: Boolean) -> Unit = { _, _ -> },
    val paintEnd: () -> Unit = {},
    /** A hit held: its roll (×1..×8, [ramp] 1 rising, -1 falling), or 32nds over its beat. */
    val setRoll: (step: Int, repeats: Int, ramp: Int) -> Unit = { _, _, _ -> },
    val burst: (step: Int, ramp: Int) -> Unit = { _, _ -> },
    /** Phase 37: the selected pad's own loop in steps (0 = the pattern's), a polyrhythm. */
    val setPadLength: (Int) -> Unit = {},
    /** Phase 37: a hit's chance to play on each pass (5..100 %), and « Autre tirage » (the chances drawn again). */
    val setHitChance: (step: Int, chance: Int) -> Unit = { _, _ -> },
    val rerollChances: () -> Unit = {},
)

/** The selected pad's own sound, if any, and what the sampler is busy with. */
data class PadSoundUi(
    val info: Map<String, SampleInfo> = emptyMap(),
    val task: SampleTask = SampleTask.Idle,
    val slicer: SlicerUi? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RhythmScreen(
    project: Project,
    transport: TransportUi,
    live: LiveUi,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: RhythmActions,
    modifier: Modifier = Modifier,
    padSound: PadSoundUi = PadSoundUi(),
    /** Shown above everything (the "Prochaine étape" card); [showGrid] scrolls the steps into view. */
    header: @Composable (showGrid: () -> Unit) -> Unit = {},
    /** The kits « Kit ▾ » offers. */
    kits: List<KitChoice> = emptyList(),
    /**
     * Under the pads: a take of the pads (its count-in, its card) or « Garder ce que je viens de
     * jouer » once pads were played (the record bar, given by the app; it shows nothing otherwise).
     */
    record: (@Composable () -> Unit)? = null,
) {
    // A pad held (its sheet) and the kit list.
    var padSheet by rememberSaveable { mutableStateOf<Int?>(null) }
    var kitSheet by rememberSaveable { mutableStateOf(false) }
    val hold: (Int) -> Unit = { lane -> actions.selectPad(lane); padSheet = lane }
    val kit = kitName(project)
    val kitBar: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KitChip(kit, onClick = { actions.openKits(); kitSheet = true })
            // A pad only sounds: what stays in the beat is in the cells (beginners tap pads and wait).
            Text(
                tr("Les pads sonnent, les cases gardent les coups. Appui long : son, volume."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2,
                modifier = Modifier.weight(1f).testTag("pads_hint"),
            )
        }
    }
    padSheet?.let { lane ->
        PadSheet(
            padSheetUi(project, lane, remember(lane, project.drums.lanes[lane].sample) { actions.padSounds(lane) }),
            PadSheetActions(
                pickSound = { actions.pickPadSound(lane, it) }, origin = { actions.removeSampleOf(lane) }, more = actions.openLibrary,
                setVolume = { actions.setPadVolume(lane, it) }, setPan = { actions.setPadPanOf(lane, it) }, toggleMute = { actions.togglePadMute(lane) },
                panDone = { actions.padPanDone(lane) },
            ),
            close = { padSheet = null },
        )
    }
    if (kitSheet) KitSheet(kit, kits, actions.chooseKit, close = { kitSheet = false })
    // The steps' card brought into view on demand (the next-step card's "Voir la grille").
    val gridRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val showGrid: () -> Unit = { scope.launch { gridRequester.bringIntoView() } }
    val steps: @Composable () -> Unit = {
        Box(Modifier.bringIntoViewRequester(gridRequester)) {
            StepGrid(project, transport.selectedPad, live.currentStep, transport.playing, actions, live.absoluteStep)
        }
    }
    // On a tablet the pads stay on the left and the steps of the chosen pad on the right.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.testTag("rhythm")) {
    // Lot 2 (audit 35): a tablet lying down (the A11: 528 dp high) cut the fourth row of pads;
    // there the pads are as high as the screen lets four rows and the kit's line fit.
    // A tablet by the device (not by the dp left at 130 %, where the A11 lying down gets one column).
    val tablet = androidx.compose.ui.platform.LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Lying down: a tablet whatever its columns (one at 130 %), or a screen wide enough for two.
    val lying = landscape && maxWidth > maxHeight && (tablet || maxWidth >= com.pocketstudio.ui.TWO_PANES_FROM)
    // At least 48 dp on the glass: at 130 % that is 37 dp of the interface.
    val minPad = 48.dp / com.pocketstudio.ui.LocalUiScale.current
    val fitPad = if (lying) ((maxHeight - LYING_ROOM) / 4).coerceIn(minPad, 150.dp) else null
    com.pocketstudio.ui.AdaptivePanes(
        Modifier,
        spacing = 8.dp,
        first = {
            header(showGrid)
            PadGrid(project, transport.selectedPad, live.litPads, actions.hitPad, onHold = hold, kitBar = kitBar, fitHeight = fitPad)
            record?.invoke()
            GrooveCard(project, transport, actions)
        },
        second = {
            steps()
            PadSoundCard(project, transport.selectedPad, padSound, actions)
        },
        // The phone keeps its order: the steps right under lower pads, the first row of cells on screen.
        phone = {
            header(showGrid)
            PadGrid(project, transport.selectedPad, live.litPads, actions.hitPad, compact = true, onHold = hold, kitBar = kitBar, fitHeight = fitPad)
            record?.invoke()
            steps()
            PadSoundCard(project, transport.selectedPad, padSound, actions)
            GrooveCard(project, transport, actions)
        },
    )
    }
}

/** Lying down, what the pads' column holds besides four rows: its padding, the kit's line, the gaps. */
private val LYING_ROOM = 24.dp + 24.dp + 48.dp + 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PadGrid(
    project: Project, selected: Int, lit: Set<Int>, onHit: (Int) -> Unit, compact: Boolean = false, onHold: (Int) -> Unit = {}, kitBar: @Composable () -> Unit = {},
    /** Each pad's height, to fit the screen's height (a tablet lying down); null: from its width. */
    fitHeight: androidx.compose.ui.unit.Dp? = null,
) {
    val haptics = LocalHapticFeedback.current
    // A tablet held upright gets the one column too (600 dp wide): taller pads, not a phone's flat ones.
    val padHeight = if (androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600) TABLET_COLUMN_PAD_HEIGHT else PHONE_PAD_HEIGHT
    // A phone: rows a little closer, so the first row of cells stays on screen under the pads.
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
        for (row in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 4) {
                    val lane = row * 4 + col
                    val color = DrumKitInfo.colors[lane]
                    val hasHits = project.drums.lanes[lane].hits.keys.any { it < project.drums.steps }  // past the loop: unheard
                    val strip = project.mixer.channels.getOrNull(com.pocketstudio.model.Mixer.padChannel(lane))
                    val hasFx = strip?.hasOwnFx() == true
                    DrumPad(
                        name = padName(project, lane), color = color, selected = lane == selected, lit = lane in lit, hasHits = hasHits, hasFx = hasFx,
                        muted = strip?.mute == true, pan = project.drums.lanes[lane].pan,
                        onHit = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onHit(lane)
                        },
                        onHold = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHold(lane)
                        },
                        // On a phone, lower pads leave the first row of steps on screen.
                        modifier = Modifier.weight(1f).then(
                            when {
                                fitHeight != null -> Modifier.height(fitHeight)
                                compact -> Modifier.height(padHeight)
                                else -> Modifier
                            },
                        ).testTag("pad_$lane"),
                        ratio = !compact && fitHeight == null,
                    )
                }
            }
        }
        // « Kit ▾ » and the one line that says what pads and cells do (on a 360 dp phone the cells
        // must start on screen).
        kitBar()
    }
}

/** A phone pad's height (wide pads, a little lower than on a tablet). */
private val PHONE_PAD_HEIGHT = 56.dp

/** A pad on an upright 8.7" tablet (one column, 600 dp wide): the first cells still show under them. */
private val TABLET_COLUMN_PAD_HEIGHT = 80.dp

/** The pad's name: the sound placed on it (a sample, a library sound), else its studio voice's (the 808), else the kit voice's name. */
internal fun padName(project: Project, lane: Int): String =
    project.drums.lanes.getOrNull(lane)?.sample?.name?.takeIf { it.isNotBlank() }?.let { com.pocketstudio.model.DefaultNames.shownSample(it) }
        ?: project.padVoices[lane]?.let { padVoiceName(it.kind) } ?: DrumKitInfo.names[lane]

/** A studio voice's name on its pad ([com.pocketstudio.model.PadVoice.NAMES], translated). */
internal fun padVoiceName(kind: Int): String = when (kind) {
    com.pocketstudio.model.PadVoice.BASS_808 -> "808"
    com.pocketstudio.model.PadVoice.KICK_STUDIO -> tr("Kick studio")
    com.pocketstudio.model.PadVoice.KICK_HARD -> tr("Kick dur")
    com.pocketstudio.model.PadVoice.RUMBLE -> tr("Rumble")
    else -> ""
}

/**
 * One pad, as on a drum machine: its colour in relief, a glow when it sounds (fading out), a
 * coloured edge when chosen, a small press.
 */
@Composable
private fun DrumPad(name: String, color: Color, selected: Boolean, lit: Boolean, hasHits: Boolean, hasFx: Boolean, onHit: () -> Unit, modifier: Modifier, ratio: Boolean = true, muted: Boolean = false, onHold: () -> Unit = {}, pan: Float = 0f) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Lights at once, fades in a quarter second.
    val glow by animateFloatAsState(if (lit || pressed) 1f else 0f, if (lit || pressed) snap<Float>() else tween<Float>(260), label = "pad-glow")
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "pad-press")
    val surface = Neon.Surface
    val rest = if (selected) 0.42f else 0.24f
    val top = lerp(lerp(surface, color, rest + 0.1f), color, glow)
    val bottom = lerp(lerp(Neon.Background, color, rest), lerp(color, Color.Black, 0.18f), glow)
    Box(
        modifier
            .then(if (ratio) Modifier.aspectRatioAtMost(1.2f, 150.dp) else Modifier)
            .scale(press)
            .shadow(if (glow > 0.05f) 18.dp * glow else if (selected) 8.dp else 3.dp, shape, ambientColor = color, spotColor = color)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Brush.verticalGradient(listOf(lerp(color, Color.White, 0.3f), color))
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), color.copy(alpha = 0.25f))),
                shape,
            )
            .combinedClickable(interactionSource = interaction, indication = null, onLongClickLabel = tr("Son, volume, pan"), onLongClick = onHold, onClick = onHit)
            // A pad cut (its sheet): dimmed, its hits stay.
            .then(if (muted) Modifier.alpha(0.4f) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        // A phone's pads are narrow (75 dp at a big interface size): « Cymbale » shrinks a little
        // rather than break mid-word.
        com.pocketstudio.ui.WholeWordsText(
            name,
            textAlign = TextAlign.Center,
            // Lower pads (a phone): two lines set closer, clear of the LED and of the colour strip.
            style = if (ratio) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
            color = lerp(Neon.TextPrimary, Color(0xFF1E2023), glow),
            maxLines = 2,
            modifier = Modifier.padding(horizontal = if (ratio) 4.dp else 8.dp, vertical = 2.dp).padding(bottom = if (ratio) 0.dp else 4.dp),
        )
        // A small LED when the pad has hits in the loop.
        if (hasHits) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(7.dp).size(7.dp)
                    .shadow(4.dp, CircleShape, ambientColor = color, spotColor = color)
                    .clip(CircleShape).background(lerp(color, Color.White, 0.2f)),
            )
        }
        // Effects of its own, and a pan off the centre (« G 100 »): small badges in the corner, so
        // a pad sent to one side is seen without opening it.
        val panned = kotlin.math.abs(pan) > 0.05f
        if (hasFx || panned) {
            Row(Modifier.align(Alignment.TopStart).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (hasFx) Text(
                    "FX", style = MaterialTheme.typography.labelSmall, color = Neon.OnAccent,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Neon.Cyan).padding(horizontal = 4.dp),
                )
                if (panned) Text(
                    panLabel(pan), style = MaterialTheme.typography.labelSmall, color = Neon.TextPrimary, maxLines = 1, softWrap = false,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 4.dp).testTag("pad_pan_badge"),
                )
            }
        }
        // The pad's colour as a strip along its bottom edge.
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = if (ratio) 6.dp else 4.dp).fillMaxWidth(0.4f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.6f + 0.4f * glow)))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepGrid(project: Project, lane: Int, currentStep: Int, playing: Boolean, actions: RhythmActions, absoluteStep: Int = -1) {
    val haptics = LocalHapticFeedback.current
    val hits = project.drums.lanes[lane].hits
    // Phase 37: the pad's own loop (a polyrhythm); the cells past it are kept but not played.
    val padLoop = project.drums.lanes[lane].loopSteps(project.drums.steps)
    val ownLoop = padLoop != project.drums.steps
    val padPlayStep = if (!ownLoop) currentStep else if (playing && absoluteStep >= 0) absoluteStep % padLoop else -1
    val color = DrumKitInfo.colors[lane]
    // One page per bar of the time signature (12 steps in 3/4, 16 in 4/4…).
    val per = project.stepsPerBar
    val bars = (project.drums.steps / per).coerceAtLeast(1)
    var chosenBar by rememberSaveable { mutableIntStateOf(0) }
    var follow by rememberSaveable { mutableStateOf(true) }
    // 0 hits, 1 rolls, 2 pan, 3 pitch.
    var tool by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    // A hit held: its force and roll menu.
    var rollStep by rememberSaveable { mutableIntStateOf(-1) }
    val ownSound = project.drums.lanes[lane].sample != null
    var allPads by rememberSaveable { mutableStateOf(false) }
    // Playing and following: the page turns with the playhead.
    val bar = (if (playing && follow && currentStep >= 0) currentStep / per else chosenBar).coerceIn(0, bars - 1)
    val chipColors = pillColors()
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The sound on the pad now (« Kick de ma voix ») and the section being edited (the top bar
            // only has room for its letter on a narrow phone); a long name wraps.
            val section = if (project.allScenes.size > 1) " · " + com.pocketstudio.model.Scene.shownLabel(project.sceneLabel(project.scene)).ifBlank { com.pocketstudio.model.Scene.NAMES.getOrElse(project.scene) { "?" } } else ""
            SectionLabel(
                (if (allPads) tr("Pas : tous les pads") else tr("Pas : %s", padName(project, lane))) + section, Modifier.weight(1f),
                color = if (allPads) Neon.Cyan else color, help = Glossary.STEP, foldKey = "Pas",
            )
            FilterChip(selected = allPads, onClick = { allPads = !allPads }, label = { Text(tr("Tous les pads"), maxLines = 1, softWrap = false) }, colors = chipColors,
                modifier = Modifier.testTag("all_pads"))
        }
        // The loop's length and its bar pages come under the cells: on a phone the first row of
        // cells stays on screen under the pads (the length is changed far less often than hits).
        val lengths: @Composable () -> Unit = {
            FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Longueur"), style = MaterialTheme.typography.bodyMedium)
                (1..4).filter { it * per <= 64 }.forEach { b ->
                    FilterChip(selected = bars == b, onClick = { actions.setBars(b) }, label = { Text(if (b == 1) tr("1 mesure") else "$b") },
                        colors = chipColors, modifier = Modifier.testTag("drum_bars_$b"))
                }
            }
            if (bars > 1) {
                FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("Mesure"), style = MaterialTheme.typography.bodyMedium)
                    (0 until bars).forEach { b ->
                        FilterChip(selected = bar == b, onClick = { chosenBar = b; follow = false }, label = { Text("${b + 1}") },
                            colors = chipColors, modifier = Modifier.testTag("drum_page_$b"))
                    }
                    FilterChip(selected = follow, onClick = { follow = !follow; chosenBar = bar }, label = { Text(tr("Suivre")) }, colors = chipColors)
                }
            }
            if (!allPads) PadLoopRow(padLoop, project.drums.steps, ownLoop, actions.setPadLength)
            if (project.drums.lanes.any { l -> l.hits.values.any { it.chance < 100 } }) {
                TextButton(onClick = actions.rerollChances, modifier = Modifier.heightIn(min = 48.dp).testTag("chance_reroll")) {
                    Text(tr("Autre tirage des coups au hasard"))
                }
            }
        }
        if (allPads) {
            AllPadsGrid(project, bar, currentStep, actions)
            lengths()
            return@NeonCard
        }
        // A finger drawn across the cells paints hits (or erases them, when the first cell had one).
        val latestHits by rememberUpdatedState(hits)
        val latestActions by rememberUpdatedState(actions)
        val loopSteps = project.drums.steps
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.spot("step_cells", tr("Glisse pour poser")).testTag("step_cells").then(
                if (tool != 0) Modifier
                else Modifier.pointerInput(bar, per, loopSteps) {
                    val cols = (per / 2).coerceAtLeast(1)
                    fun cellAt(at: androidx.compose.ui.geometry.Offset): Int? {
                        if (size.width <= 0 || size.height <= 0) return null
                        val col = (at.x / size.width * cols).toInt().coerceIn(0, cols - 1)
                        val row = (at.y / size.height * 2).toInt().coerceIn(0, 1)
                        return (bar * per + row * cols + col).takeIf { it < loopSteps }
                    }
                    // From the cell first touched (the drag is known only past the touch slop).
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                            ?: return@awaitEachGesture
                        val first = cellAt(down.position)
                        val on = first == null || first !in latestHits
                        var last = first
                        // The stroke always ends as one undo step, even cut short (the screen left
                        // mid-stroke cancels this gesture: review m9).
                        try {
                            first?.let { latestActions.paintStep(it, on) }
                            fun paint(at: androidx.compose.ui.geometry.Offset) {
                                val step = cellAt(at)
                                if (step != null && step != last) {
                                    last = step
                                    latestActions.paintStep(step, on)
                                }
                            }
                            paint(drag.position)
                            horizontalDrag(drag.id) { change ->
                                paint(change.position)
                                change.consume()
                            }
                        } finally {
                            latestActions.paintEnd()
                        }
                    }
                },
            ),
        ) {
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until per / 2) {
                    val step = bar * per + row * (per / 2) + col
                    val velocity = hits[step]?.velocity
                    val isBeat = step % 4 == 0
                    val playing = step == padPlayStep
                    val hit = hits[step]
                    // Past the pad's own loop: kept, unheard (dimmed).
                    val pastLoop = ownLoop && step >= padLoop
                    val isPicked = tool >= 2 && step == picked && hit != null
                    // Empty cells are hollowed into the panel; the beats a little lighter.
                    val fill = Color.Black.copy(alpha = if (isBeat) 0.16f else 0.28f)
                    val cell = RoundedCornerShape(7.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatioAtMost(0.8f, 110.dp)
                            .alpha(if (pastLoop) 0.3f else 1f)
                            .then(if (playing && velocity != null) Modifier.shadow(10.dp, cell, ambientColor = color, spotColor = color) else Modifier)
                            .clip(cell)
                            .background(fill)
                            .border(
                                if (playing || isPicked) 2.dp else 1.dp,
                                when {
                                    isPicked -> Neon.Cyan
                                    playing -> Neon.TextPrimary
                                    else -> Color.White.copy(alpha = 0.07f)
                                },
                                cell,
                            )
                            .then(
                                if (tool == 4) {
                                    // « Force »: a touch on a hit sets its strength by where it lands
                                    // (top = full); the slider below then fine-tunes it.
                                    Modifier.pointerInput(step, hit != null, project.drums.steps) {
                                        detectTapGestures { at ->
                                            if (hit != null && step < project.drums.steps) {
                                                picked = step
                                                actions.setHitVelocity(step, (1f - at.y / size.height).coerceIn(0f, 1f))
                                            }
                                        }
                                    }
                                } else {
                                    Modifier.combinedClickable(
                                        onClick = {
                                            // A loop shorter than the page (an older length): its end is not editable.
                                            if (step < project.drums.steps) {
                                                when (tool) {
                                                    0 -> actions.toggleStep(step)
                                                    1 -> actions.cycleRoll(step)
                                                    else -> if (hit != null) picked = step
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            // A hit: its force and its roll; an empty cell: a strong hit.
                                            if (hit != null && step < project.drums.steps) rollStep = step else actions.cycleVelocity(step)
                                        },
                                    )
                                },
                            )
                            .testTag("step_$step"),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // The hit fills the cell up to its strength.
                        if (velocity != null) {
                            Box(
                                Modifier.fillMaxWidth().fillMaxHeight(velocity.coerceIn(0.08f, 1f))
                                    .background(Brush.verticalGradient(listOf(lerp(color, Color.White, if (playing) 0.45f else 0.2f), color))),
                            )
                            val repeats = hits[step]?.repeats ?: 1
                            if (repeats > 1) {
                                val ramp = when (hits[step]?.ramp) { 1 -> "↗"; -1 -> "↘"; else -> "" }
                                Text("×$repeats$ramp", style = MaterialTheme.typography.labelLarge, color = Color(0xFF1E2023),
                                    modifier = Modifier.align(Alignment.Center))
                            }
                            // Own pan or pitch: a small mark at the top of the cell.
                            val mark = listOfNotNull(
                                hit?.pan?.takeIf { it != 0f }?.let { panShort(it) },
                                hit?.pitch?.takeIf { it != 0 }?.let { if (it > 0) "+$it" else "$it" },
                                hit?.chance?.takeIf { it < 100 }?.let { percent(it) },
                            ).joinToString(" ")
                            if (mark.isNotEmpty()) {
                                Text(mark, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E2023),
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp))
                            }
                        }
                        if (isBeat) {
                            Text(
                                "${(step % per) / 4 + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Neon.TextSecondary,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                        if (rollStep == step && hit != null) {
                            RollMenu(hit, step % per + 1, close = { rollStep = -1 },
                                force = { actions.setHitVelocity(step, it) }, roll = { n, r -> actions.setRoll(step, n, r) }, burst = { actions.burst(step, it) },
                                chance = { actions.setHitChance(step, it) })
                        }
                    }
                }
            }
        }
        }
        Row(Modifier.fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = tool == 0, onClick = { tool = 0 }, label = { Text(tr("Coups")) }, colors = chipColors)
            FilterChip(selected = tool == 1, onClick = { tool = 1 }, label = { Text(tr("Roulements")) }, colors = chipColors,
                modifier = Modifier.testTag("roll_tool"))
            FilterChip(selected = tool == 2, onClick = { tool = 2 }, label = { Text(tr("Pan")) }, colors = chipColors,
                modifier = Modifier.testTag("pan_tool"))
            FilterChip(selected = tool == 3, onClick = { tool = 3 }, label = { Text(tr("Hauteur")) }, colors = chipColors,
                modifier = Modifier.testTag("pitch_tool"))
            FilterChip(selected = tool == 4, onClick = { tool = 4 }, label = { Text(tr("Force")) }, colors = chipColors,
                modifier = Modifier.testTag("velocity_tool"))
        }
        // Whole-pad edits, on their own line that wraps: none is cut on a small phone.
        val tight = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        val small = Modifier.defaultMinSize(minWidth = 44.dp)
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tr("Remplir"), style = MaterialTheme.typography.bodySmall)
            listOf(1 to tr("tout"), 2 to "1/2", 4 to "1/4").forEach { (every, label) ->
                TextButton(onClick = { actions.fill(every) }, contentPadding = tight, modifier = small.testTag("fill_$every")) { Text(label) }
            }
            TextButton(onClick = { actions.rotate(-1) }, enabled = hits.isNotEmpty(), contentPadding = tight, modifier = small.testTag("rotate_left")) { Text("◀") }
            TextButton(onClick = { actions.rotate(1) }, enabled = hits.isNotEmpty(), contentPadding = tight, modifier = small.testTag("rotate_right")) { Text("▶") }
            TextButton(onClick = actions.clearPad, enabled = hits.isNotEmpty(), contentPadding = tight, modifier = small.testTag("clear_pad")) { Text(tr("Effacer"), maxLines = 1, softWrap = false) }
        }
        val pickedHit = hits[picked]?.takeIf { tool >= 2 }
        if (pickedHit != null && tool == 2) {
            PanRow(tr("Pan du coup %d", picked % per + 1), pickedHit.pan, onChange = { actions.setHitPan(picked, it) }, tag = "hit_pan")
        }
        if (pickedHit != null && tool == 4) {
            LabeledSlider(tr("Force du coup %d", picked % per + 1), percent((pickedHit.velocity * 100).roundToInt()), pickedHit.velocity,
                com.pocketstudio.model.DrumPattern.MIN_VELOCITY..1f) { actions.setHitVelocity(picked, it) }
        }
        if (pickedHit != null && tool == 3 && ownSound) {
            LabeledSlider(tr("Hauteur du coup %d", picked % per + 1), semitones(pickedHit.pitch), pickedHit.pitch.toFloat(), -12f..12f, steps = 23) {
                actions.setHitPitch(picked, it.roundToInt())
            }
        }
        Text(
            when (tool) {
                1 -> tr("Roulements : touche un coup pour le doubler, tripler, quadrupler (×2, ×3, ×4), puis revenir à un seul.")
                2 -> tr("Pan : touche un coup, puis glisse pour l'envoyer à gauche ou à droite (en plus du pan du pad).")
                3 -> if (ownSound) tr("Hauteur : touche un coup, puis glisse pour le monter ou le descendre (en demi-tons).")
                else tr("Hauteur : seulement pour un son importé, enregistré ou généré (pas pour les sons synthétisés).")
                4 -> tr("Force : touche un coup, plus haut pour plus fort, plus bas pour plus doux, puis règle-le avec le curseur.")
                else -> tr("Touche une case pour ajouter un coup, glisse le doigt pour en peindre plusieurs. Appui long sur un coup : force et roulements (×2 à ×8).")
            },
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
        lengths()
    }
}

/**
 * Phase 37: the selected pad's own loop, « − 5 pas + » (a polyrhythm: a pad of 3 steps against the
 * 4 of the others), and « Comme la boucle » to give it the pattern's loop back.
 */
@Composable
private fun PadLoopRow(padLoop: Int, patternSteps: Int, ownLoop: Boolean, set: (Int) -> Unit) {
    FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.testTag("pad_loop")) {
        Text(tr("Ce pad boucle sur"), style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { set((padLoop - 1).coerceAtLeast(1)) }, enabled = padLoop > 1, modifier = Modifier.testTag("pad_len_minus")) { Text("−") }
            Text(tr("%d pas", padLoop), style = MonoStyle, color = if (ownLoop) Neon.Cyan else Neon.TextSecondary, modifier = Modifier.testTag("pad_len_value"))
            TextButton(onClick = { set((padLoop + 1).coerceAtMost(patternSteps)) }, enabled = padLoop < patternSteps, modifier = Modifier.testTag("pad_len_plus")) { Text("+") }
        }
        if (ownLoop) TextButton(onClick = { set(0) }, modifier = Modifier.testTag("pad_len_reset")) { Text(tr("Comme la boucle")) }
    }
    if (ownLoop) {
        Text(tr("Polyrythme : ce pad repart tous les %d pas, les autres tous les %d.", padLoop, patternSteps),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

@Composable
private fun GrooveCard(project: Project, transport: TransportUi, actions: RhythmActions) {
    NeonCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val chipColors = pillColors()
            FilterChip(selected = transport.metronome, onClick = actions.toggleMetronome, label = { Text(tr("Métronome")) }, colors = chipColors)
            FilterChip(selected = transport.countIn, onClick = actions.toggleCountIn, label = { Text(tr("Décompte")) }, colors = chipColors)
            HelpDot(Glossary.METRONOME, Modifier.align(Alignment.CenterVertically))
            Box(Modifier.weight(1f))
            OutlinedButton(
                onClick = actions.openEffects, modifier = Modifier.testTag("drum_effects"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
            ) { Text(tr("Effets"), maxLines = 1, softWrap = false) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Swing"))
            HelpDot(Glossary.SWING)
            Box(Modifier.weight(1f))
            Text(percent((project.swing / Project.MAX_SWING * 100).roundToInt()), style = MonoStyle, color = Neon.Cyan)
        }
        ProSlider(
            value = (project.swing / Project.MAX_SWING).toFloat(),
            onValueChange = { actions.setSwing(it * Project.MAX_SWING) },
        )
    }
}

/**
 * A hit held: its force (fort, moyen, doux), its roll (×1 to ×8, rising or falling) and a burst
 * of 32nds over its beat. Each choice is one undo step; the menu stays open to combine them.
 */
@Composable
private fun RollMenu(
    hit: com.pocketstudio.model.Hit, beatStep: Int, close: () -> Unit, force: (Float) -> Unit, roll: (Int, Int) -> Unit, burst: (Int) -> Unit,
    chance: (Int) -> Unit = {},
) {
    val chips = pillColors()
    ScaledDropdownMenu(expanded = true, onDismissRequest = close, modifier = Modifier.testTag("roll_menu")) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Coup %d", beatStep), style = MaterialTheme.typography.titleSmall)
            Text(tr("Force"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1f to tr("Fort"), 0.5f to tr("Moyen"), 0.2f to tr("Doux")).forEach { (v, label) ->
                    FilterChip(selected = kotlin.math.abs(hit.velocity - v) < 0.05f, onClick = { force(v) }, label = { Text(label) }, colors = chips,
                        modifier = Modifier.testTag("roll_force_${(v * 10).roundToInt()}"))
                }
            }
            Text(tr("Roulement"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                com.pocketstudio.model.DrumPattern.ROLLS.forEach { n ->
                    FilterChip(selected = hit.repeats == n, onClick = { roll(n, hit.ramp) }, label = { Text("×$n") }, colors = chips, modifier = Modifier.testTag("roll_$n"))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1 to tr("↗ Montant"), 0 to tr("Égal"), -1 to tr("↘ Descendant")).forEach { (r, label) ->
                    FilterChip(selected = hit.ramp == r, enabled = hit.repeats > 1, onClick = { roll(hit.repeats, r) }, label = { Text(label) }, colors = chips,
                        modifier = Modifier.testTag("roll_ramp_$r"))
                }
            }
            TextButton(onClick = { burst(1); close() }, modifier = Modifier.testTag("roll_burst")) { Text(tr("Rafale de 1/32 sur le temps")) }
            // Phase 37: a hit that plays only now and then (the same draw each time, the export too).
            Text(tr("Chance de jouer"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                com.pocketstudio.model.DrumPattern.CHANCES.forEach { c ->
                    FilterChip(selected = hit.chance == c, onClick = { chance(c) }, label = { Text(percent(c)) }, colors = chips,
                        modifier = Modifier.testTag("roll_chance_$c"))
                }
            }
        }
    }
}

@Composable
internal fun PadSoundCard(project: Project, lane: Int, ui: PadSoundUi, actions: RhythmActions) {
    val color = DrumKitInfo.colors[lane]
    val sample = project.drums.lanes[lane].sample
    NeonCard(modifier = Modifier.testTag("pad_sound")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Its foldKey stays the former title: a card folded before stays folded.
            SectionLabel(tr("Son du pad"), color = color, help = Glossary.SAMPLE, foldKey = "Sample du pad")
            Box(Modifier.weight(1f))
            Text(
                sample?.name?.let { com.pocketstudio.model.DefaultNames.shownSample(it) } ?: tr("%s (synthétisé)", DrumKitInfo.names[lane]),
                style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp),
                color = if (sample != null) Neon.TextPrimary else Neon.TextSecondary,
            )
        }
        // The first thing a beginner wants here: another sound, from the library.
        val busy = ui.task is SampleTask.Busy || ui.task is SampleTask.Recording
        Button(
            onClick = { actions.dismissSampleError(); actions.openLibrary() }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_library"),
        ) {
            Icon(Icons.Filled.LibraryMusic, contentDescription = null)
            Text(tr(" Changer le son"))
        }
        // The effect box on this pad alone, or on the whole drum kit.
        val narrow = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.openPadEffects, contentPadding = narrow, modifier = Modifier.weight(1f).testTag("pad_effects")) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                com.pocketstudio.ui.WholeWordsText(tr("  Effets du pad").trim(), Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = actions.openEffects, contentPadding = narrow, modifier = Modifier.weight(1f).testTag("drum_kit_effects")) {
                com.pocketstudio.ui.WholeWordsText(tr("Toute la batterie"), textAlign = TextAlign.Center)
            }
        }
        val volume = project.mixer.channels.getOrNull(com.pocketstudio.model.Mixer.padChannel(lane))?.volume ?: 0.8f
        LabeledSlider(tr("Volume du pad"), percent((volume / 0.8f * 100).roundToInt()), volume, 0f..1f) { actions.setPadVolume(lane, it) }
        val pan = project.drums.lanes[lane].pan
        PanRow(tr("Pan du pad"), pan, onChange = { actions.setPadPanOf(lane, it) }, onEnd = { actions.padPanDone(lane) }, tag = "pad_sound_pan")
        val info = sample?.let { ui.info[it.file] }
        val slicer = ui.slicer?.takeIf { sample != null && it.file == sample.file }
        if (sample != null && info != null && slicer != null) {
            SlicerPanel(slicer, info, lane, color, actions)
            return@NeonCard
        }
        if (sample != null) {
            // A sampler: the sound replayed higher or lower on each key of the keyboard.
            // Secondary to "Changer le son": outlined, one main action per card.
            OutlinedButton(onClick = actions.toKeyboard, modifier = Modifier.fillMaxWidth().testTag("pad_to_keyboard")) {
                Icon(Icons.Filled.Piano, contentDescription = null)
                Text(tr("  Jouer ce sample au clavier"))
            }
        }
        if (sample != null && info != null) {
            // The sample editor stays folded until asked for: a beginner sees the sound, not its knobs.
            var editing by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick = { editing = !editing }, modifier = Modifier.testTag("edit_sample")) {
                Text(if (editing) tr("Masquer les réglages du son") else tr("Régler le son : début, fin, hauteur…"))
            }
            if (editing) {
                Waveform(info.peaks, color, region = sample.start to sample.end)
                Text(
                    tr("%.2f s · pince pour zoomer", info.seconds * (sample.end - sample.start)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Neon.TextSecondary,
                )
                PadSampleControls(sample, info.seconds, project.tempoBpm, actions.editSample)
                OutlinedButton(onClick = actions.openSlicer, modifier = Modifier.fillMaxWidth().testTag("open_slicer")) {
                    Icon(Icons.Filled.ContentCut, contentDescription = null); Text(tr(" Découper en tranches"))
                }
            }
        }
        when (val task = ui.task) {
            is SampleTask.Busy -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = color)
                Text(task.message, modifier = Modifier.weight(1f))
                if (task.cancellable) {
                    TextButton(onClick = actions.cancelResample, modifier = Modifier.testTag("resample_cancel")) { Text(tr("Annuler")) }
                }
            }
            is SampleTask.Recording -> {
                Text(tr("Enregistrement… %.1f s / 30 s", task.seconds), color = Neon.RedText)
                LevelMeter(task.level)
                Button(
                    onClick = actions.stopRecording,
                    modifier = Modifier.fillMaxWidth().testTag("record_stop"),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon.Red, contentColor = Color.Black),
                ) { Icon(Icons.Filled.Stop, contentDescription = null); Text(tr(" Arrêter")) }
            }
            else -> {
                if (task is SampleTask.Failed) {
                    Text(task.message, color = Neon.RedText, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { actions.dismissSampleError(); actions.importSample() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp), modifier = Modifier.weight(1f).testTag("import_sample")) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null); com.pocketstudio.ui.WholeWordsText(tr(" Importer"), maxLines = 1)
                    }
                    OutlinedButton(onClick = { actions.dismissSampleError(); actions.startRecording() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp), modifier = Modifier.weight(1f).testTag("record_sample")) {
                        Icon(Icons.Filled.Mic, contentDescription = null); com.pocketstudio.ui.WholeWordsText(tr(" Micro"), maxLines = 1)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Pack sur les pads suivants :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { actions.dismissSampleError(); actions.importFolder() }, modifier = Modifier.testTag("import_folder")) { Text(tr("Dossier")) }
                    TextButton(onClick = { actions.dismissSampleError(); actions.importZip() }, modifier = Modifier.testTag("import_zip")) { Text(".zip") }
                }
                // Resampling replaces the pad's sound with the whole mix: asked first, never by surprise.
                var capture by rememberSaveable { mutableIntStateOf(0) }
                // Wraps instead of squeezing the last choice into a column of letters.
                androidx.compose.foundation.layout.FlowRow(
                    itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(tr("Capturer tout le morceau :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    listOf(1, 2, 4).forEach { bars ->
                        TextButton(onClick = { capture = bars }, modifier = Modifier.testTag("resample_$bars")) {
                            Text(if (bars == 1) tr("1 mesure") else tr("%d mes.", bars), maxLines = 1, softWrap = false)
                        }
                    }
                }
                if (capture > 0) {
                    val bars = capture
                    ScaledAlertDialog(
                        onDismissRequest = { capture = 0 },
                        title = { Text(tr("Capturer tout le morceau ?")) },
                        text = {
                            Text(
                                (
                                    if (bars == 1) tr("Le son du pad « %s » sera remplacé par l'enregistrement de tout ce qui joue sur une mesure : la batterie, la basse acide, les synthés.", DrumKitInfo.names[lane])
                                    else tr("Le son du pad « %s » sera remplacé par l'enregistrement de tout ce qui joue sur %d mesures : la batterie, la basse acide, les synthés.", DrumKitInfo.names[lane], bars)
                                ) + " " + tr("Pratique pour en faire une boucle. Pour garder ton son, choisis « Non » ; la flèche Annuler en haut le remet aussi."),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { capture = 0; actions.dismissSampleError(); actions.resampleSong(bars) }, modifier = Modifier.testTag("resample_confirm")) {
                                Text(tr("Capturer"))
                            }
                        },
                        dismissButton = { TextButton(onClick = { capture = 0 }) { Text(tr("Non")) } },
                    )
                }
                if (sample != null) {
                    TextButton(onClick = actions.removeSample, modifier = Modifier.testTag("remove_sample")) {
                        Text(tr("Revenir au son %s", DrumKitInfo.names[lane]))
                    }
                } else {
                    Text(
                        tr("Mets ton propre son sur ce pad : un fichier de l'appareil (MP3, WAV, FLAC, M4A…) ou le micro."),
                        style = MaterialTheme.typography.bodySmall,
                        color = Neon.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PadSampleControls(s: PadSample, seconds: Double, bpm: Double, edit: (String?, (PadSample) -> PadSample) -> Unit) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val chipColors = pillColors()
    LabeledSlider(tr("Début"), tr("%.2f s", s.start * seconds), s.start, 0f..1f) { v ->
        edit("start") { it.copy(start = minOf(v, it.end - 0.001f)) }
    }
    LabeledSlider(tr("Fin"), tr("%.2f s", s.end * seconds), s.end, 0f..1f) { v ->
        edit("end") { it.copy(end = maxOf(v, it.start + 0.001f)) }
    }
    val semis = s.pitch.roundToInt()
    LabeledSlider(tr("Hauteur"), if (semis == 0) tr("normale") else tr("%s demi-tons", "${if (semis > 0) "+" else ""}$semis"), s.pitch, -24f..24f) { v ->
        edit("pitch") { it.copy(pitch = v.roundToInt().toFloat()) }
    }
    val regionSeconds = seconds * (s.end - s.start)
    WithHelp(Glossary.FIT_TEMPO) {
        Text(
            if (s.fitBeats == 0) tr("Calé au tempo : non (le son garde sa vitesse)")
            else tr("Calé au tempo : %d temps à %d BPM, sans changer la hauteur", s.fitBeats, bpm.roundToInt()),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
    Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val suggestion = PadSample.suggestBeats(regionSeconds, bpm)
        PadSample.FIT_BEATS.forEach { beats ->
            FilterChip(
                selected = s.fitBeats == beats,
                onClick = { edit(null) { it.copy(fitBeats = beats) } },
                label = { Text(if (beats == 0) tr("Non") else if (beats == suggestion) "$beats ★" else "$beats") },
                colors = chipColors,
                modifier = Modifier.testTag("fit_$beats"),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = s.reverse, onClick = { edit(null) { it.copy(reverse = !it.reverse) } }, label = { Text(tr("À l'envers")) }, colors = chipColors)
        FilterChip(selected = s.loop, onClick = { edit(null) { it.copy(loop = !it.loop) } }, label = { Text(tr("En boucle")) }, colors = chipColors)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { advanced = !advanced }, modifier = Modifier.weight(1f, fill = false)) {
            Text(if (advanced) tr("Masquer l'enveloppe et les groupes") else tr("Enveloppe, volume, groupe d'étouffement"))
        }
        HelpDot(Glossary.ENVELOPE)
    }
    if (advanced) {
        LabeledSlider(tr("Volume"), percent((s.gain * 100).roundToInt()), s.gain, 0f..2f) { v -> edit("gain") { it.copy(gain = v) } }
        LabeledSlider(tr("Attaque"), ms(s.attack), s.attack, 0f..1f) { v -> edit("attack") { it.copy(attack = v) } }
        LabeledSlider(tr("Décroissance"), ms(s.decay), s.decay, 0f..2f) { v -> edit("decay") { it.copy(decay = v) } }
        LabeledSlider(tr("Maintien"), percent((s.sustain * 100).roundToInt()), s.sustain, 0f..1f) { v -> edit("sustain") { it.copy(sustain = v) } }
        LabeledSlider(tr("Relâche"), ms(s.release), s.release, 0f..2f) { v -> edit("release") { it.copy(release = v) } }
        Text(tr("Groupe d'étouffement : un coup coupe les autres pads du même groupe (charleston ouvert/fermé)."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..PadSample.CHOKE_GROUPS).forEach { g ->
                FilterChip(
                    selected = s.choke == g,
                    onClick = { edit(null) { it.copy(choke = g) } },
                    label = { Text(if (g == 0) tr("Aucun") else "$g") },
                    colors = chipColors,
                )
            }
        }
    }
}

internal fun panLabel(pan: Float): String = when {
    pan < -0.05f -> tr("G %d", (-pan * 100).roundToInt())
    pan > 0.05f -> tr("D %d", (pan * 100).roundToInt())
    else -> tr("Centre")
}

private fun panShort(pan: Float) = if (pan < 0f) tr("G") else tr("D")

private fun semitones(n: Int) = when {
    n == 0 -> "0"
    n > 0 -> if (n > 1) tr("+%d demi-tons", n) else tr("+%d demi-ton", n)
    else -> if (n < -1) tr("%d demi-tons", n) else tr("%d demi-ton", n)
}

private fun ms(seconds: Float) =
    if (seconds < 1f) "${(seconds * 1000).roundToInt()} ms" else tr("%.2f s", seconds)

/**
 * A pan's name, its value (« G 30 », « Centre ») and the [com.pocketstudio.ui.PanSlider] (tenths,
 * safe at the screen's edge, two touches = centre).
 */
@Composable
internal fun PanRow(label: String, pan: Float, onChange: (Float) -> Unit, onEnd: () -> Unit = {}, tag: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(panLabel(pan), style = MonoStyle, color = Neon.Cyan)
    }
    com.pocketstudio.ui.PanSlider(
        pan, onChange, step = 0.1f, onGestureEnd = onEnd, modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
        label = label, stateLabel = ::panLabel,
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    value: String,
    v: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = v.coerceIn(range), onValueChange = onChange, valueRange = range, steps = steps)
}

@Composable
private fun SlicerPanel(s: SlicerUi, info: SampleInfo, lane: Int, color: Color, actions: RhythmActions) {
    val chipColors = pillColors()
    Text(tr("Découpe"), style = MaterialTheme.typography.titleMedium)
    Waveform(info.peaks, color, markers = s.markers, markerColor = Neon.TextPrimary, onTap = actions.toggleSliceMarker,
        modifier = Modifier.testTag("slicer_wave"))
    Text(tr("Touche la forme d'onde pour ajouter ou retirer une coupe. Pince pour zoomer."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SliceMode.entries.forEach { m ->
            FilterChip(selected = s.mode == m, onClick = { actions.setSliceMode(m) }, label = { Text(m.label) }, colors = chipColors)
        }
    }
    when (s.mode) {
        SliceMode.Attacks -> LabeledSlider(
            tr("Sensibilité"),
            if (s.busy) "…" else percent((s.sensitivity * 100).roundToInt()),
            s.sensitivity, 0f..1f,
        ) { actions.setSliceSensitivity(it) }
        SliceMode.Grid -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Slicer.GRIDS.forEach { n ->
                FilterChip(selected = s.grid == n, onClick = { actions.setSliceGrid(n) }, label = { Text("$n") }, colors = chipColors)
            }
        }
    }
    val count = minOf(s.markers.size, DrumKitInfo.names.size - lane)
    Text(
        if (count <= 1) tr("Une seule tranche : ajoute des coupes.") else tr("%d tranches → pads %d à %d (Annuler possible)", count, lane + 1, lane + count),
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = actions.closeSlicer, modifier = Modifier.weight(1f)) { Text(tr("Fermer")) }
        Button(
            onClick = actions.applySlices,
            enabled = count > 1 && !s.busy,
            modifier = Modifier.weight(1f).testTag("apply_slices"),
        ) { Text(tr("Sur les pads")) }
    }
}

/** Every pad's steps for one bar: rows of pads, tap a cell to add or remove a hit. */
@Composable
internal fun AllPadsGrid(project: Project, bar: Int, currentStep: Int, actions: RhythmActions) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.testTag("all_pads_grid")) {
        project.drums.lanes.forEachIndexed { lane, l ->
            val color = DrumKitInfo.colors[lane]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(DrumKitInfo.names[lane], style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = if (l.hits.keys.none { it < project.drums.steps }) Neon.TextSecondary else Neon.TextPrimary, modifier = Modifier.width(72.dp),
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                for (col in 0 until project.stepsPerBar) {
                    val step = bar * project.stepsPerBar + col
                    val hit = l.hits[step]
                    Box(
                        Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (hit != null) color.copy(alpha = 0.35f + 0.65f * hit.velocity) else if (col % 4 == 0) Neon.SurfaceHigh else Neon.Surface)
                            .border(if (step == currentStep) 2.dp else 0.5.dp, if (step == currentStep) Neon.TextPrimary else Neon.Outline, RoundedCornerShape(3.dp))
                            .clickable { actions.toggleHit(lane, step) }
                            .testTag("cell_${lane}_$step"),
                    )
                }
            }
        }
    }
}
