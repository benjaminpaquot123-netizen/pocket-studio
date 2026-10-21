@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.rhythm

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.percent

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.SwapVert
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
    val importFolder: () -> Unit = {},
    /** A sample pack as a .zip file. */
    val importZip: () -> Unit = {},
    /** What ▶ plays: the whole song (true) or the section shown, in a loop (false). */
    val setListenSong: (Boolean) -> Unit = {},
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
    /**
     * What ▶ plays when the project has a song: the whole song (true) or the section shown (false);
     * null hides the choice (no song yet: ▶ plays the section anyway).
     */
    listenSong: Boolean? = null,
    /** Shown above everything (the "Prochaine étape" card); [showGrid] scrolls the steps into view. */
    header: @Composable (showGrid: () -> Unit) -> Unit = {},
) {
    // The steps' card brought into view on demand (the next-step card's "Voir la grille").
    val gridRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val showGrid: () -> Unit = { scope.launch { gridRequester.bringIntoView() } }
    val steps: @Composable () -> Unit = {
        Box(Modifier.bringIntoViewRequester(gridRequester)) {
            StepGrid(project, transport.selectedPad, live.currentStep, transport.playing, actions)
        }
    }
    // On a tablet the pads stay on the left and the steps of the chosen pad on the right.
    com.pocketstudio.ui.AdaptivePanes(
        modifier.testTag("rhythm"),
        spacing = 8.dp,
        first = {
            header(showGrid)
            TransportBar(project, transport, canUndo, canRedo, actions, listenSong)
            PadGrid(project, transport.selectedPad, live.litPads, actions.hitPad)
            GrooveCard(project, transport, actions)
        },
        second = {
            steps()
            PadSoundCard(project, transport.selectedPad, padSound, actions)
        },
        // The phone keeps its order: the steps right under lower pads, the first row of cells on screen.
        phone = {
            header(showGrid)
            TransportBar(project, transport, canUndo, canRedo, actions, listenSong)
            PadGrid(project, transport.selectedPad, live.litPads, actions.hitPad, compact = true)
            steps()
            PadSoundCard(project, transport.selectedPad, padSound, actions)
            GrooveCard(project, transport, actions)
        },
    )
}

@Composable
private fun TransportBar(project: Project, transport: TransportUi, canUndo: Boolean, canRedo: Boolean, actions: RhythmActions, listenSong: Boolean?) {
    // A small phone (360 dp): a slimmer Play, and redo left to the top bar, so the listening
    // choice and « Taper » keep their words.
    val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 380
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (narrow) 6.dp else 8.dp)) {
            val playColor = if (transport.playing) Neon.Magenta else Neon.Cyan
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier.height(56.dp)
                    .shadow(if (transport.playing) 16.dp else 8.dp, shape, ambientColor = playColor, spotColor = playColor)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(lerp(playColor, Color.White, 0.18f), playColor, lerp(playColor, Color.Black, 0.12f))))
                    .clickable(role = Role.Button, onClick = actions.togglePlay)
                    .padding(horizontal = if (narrow) 14.dp else 22.dp)
                    .testTag("transport_play"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (transport.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent)
                Text(if (transport.playing) tr(" Stop") else tr(" Jouer"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.OnAccent)
            }
            // What ▶ plays, where it is played (the same choice as Morceau › Écouter).
            if (listenSong != null) ListenChoice(listenSong, actions.setListenSong, Modifier.weight(1f))
            else Box(Modifier.weight(1f))
            IconButton(onClick = actions.undo, enabled = canUndo, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = tr("Annuler la dernière action"))
            }
            if (!narrow) {
                IconButton(onClick = actions.redo, enabled = canRedo, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = tr("Rétablir"))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RepeatingIconButton({ actions.nudgeTempo(-1) }) { Icon(Icons.Filled.Remove, contentDescription = tr("Tempo moins")) }
            // The tempo on a small dark display, as on a drum machine.
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .padding(horizontal = if (narrow) 8.dp else 12.dp, vertical = 4.dp).testTag("tempo"),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("${project.tempoBpm.roundToInt()}", style = MonoStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold), color = Neon.Cyan)
                Text(" BPM", style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            RepeatingIconButton({ actions.nudgeTempo(1) }) { Icon(Icons.Filled.Add, contentDescription = tr("Tempo plus")) }
            HelpDot(Glossary.TEMPO)
            Box(Modifier.weight(1f))
            OutlinedButton(
                onClick = actions.tapTempo,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Neon.TextPrimary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (narrow) 12.dp else 24.dp),
                modifier = Modifier.testTag("tap"),
            ) { Text(tr("Taper"), maxLines = 1, softWrap = false) }
        }
    }
}

/**
 * "Écouter : cette section / tout le morceau", next to Play: a touch switches. The section loops
 * (what is edited is what is heard); the song starts from its first part.
 */
@Composable
private fun ListenChoice(song: Boolean, set: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.height(56.dp).clip(shape)
            .border(1.dp, Neon.Outline, shape)
            .clickable(role = Role.Switch, onClickLabel = tr("Changer ce que Jouer fait entendre")) { set(!song) }
            .padding(horizontal = 8.dp)
            .testTag("rhythm_listen"),
        verticalArrangement = Arrangement.Center,
    ) {
        // The swap mark on the small line: the value keeps the whole width (a phone row is tight).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Écouter :"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
            Icon(Icons.Filled.SwapVert, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.padding(start = 2.dp).size(14.dp))
        }
        Text(
            if (song) tr("tout le morceau") else tr("cette section"),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Neon.Cyan, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** An icon button that repeats while held (after a short wait, faster and faster): tempo − and +. */
@Composable
private fun RepeatingIconButton(onStep: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val step by androidx.compose.runtime.rememberUpdatedState(onStep)
    // Set once the hold has started repeating: the release then adds no extra step.
    var repeated by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        repeated = false
        kotlinx.coroutines.delay(REPEAT_AFTER_MS)
        repeated = true
        var wait = REPEAT_FIRST_MS
        while (true) {
            step()
            kotlinx.coroutines.delay(wait)
            wait = (wait - 15).coerceAtLeast(REPEAT_FASTEST_MS)
        }
    }
    IconButton(onClick = { if (!repeated) step(); repeated = false }, interactionSource = interaction) { content() }
}

private const val REPEAT_AFTER_MS = 450L
private const val REPEAT_FIRST_MS = 140L
private const val REPEAT_FASTEST_MS = 50L

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PadGrid(project: Project, selected: Int, lit: Set<Int>, onHit: (Int) -> Unit, compact: Boolean = false) {
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
                    val hasFx = project.mixer.channels.getOrNull(com.pocketstudio.model.Mixer.padChannel(lane))?.hasOwnFx() == true
                    DrumPad(
                        name = padName(project, lane), color = color, selected = lane == selected, lit = lane in lit, hasHits = hasHits, hasFx = hasFx,
                        onHit = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onHit(lane)
                        },
                        // On a phone, lower pads leave the first row of steps on screen.
                        modifier = Modifier.weight(1f).then(if (compact) Modifier.height(padHeight) else Modifier).testTag("pad_$lane"),
                        ratio = !compact,
                    )
                }
            }
        }
        // A pad only sounds: what stays in the beat is in the cells (beginners tap pads and wait).
        // One short line: on a 360 dp phone the cells must start on screen.
        Text(
            tr("Les pads sonnent, les cases gardent les coups."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("pads_hint"),
        )
    }
}

/** A phone pad's height (wide pads, a little lower than on a tablet). */
private val PHONE_PAD_HEIGHT = 56.dp

/** A pad on an upright 8.7" tablet (one column, 600 dp wide): the first cells still show under them. */
private val TABLET_COLUMN_PAD_HEIGHT = 80.dp

/** The pad's name: the sound placed on it (a sample, a library sound), else the kit voice's name. */
internal fun padName(project: Project, lane: Int): String =
    project.drums.lanes.getOrNull(lane)?.sample?.name?.takeIf { it.isNotBlank() }?.let { com.pocketstudio.model.DefaultNames.shownSample(it) } ?: DrumKitInfo.names[lane]

/**
 * One pad, as on a drum machine: its colour in relief, a glow when it sounds (fading out), a
 * coloured edge when chosen, a small press.
 */
@Composable
private fun DrumPad(name: String, color: Color, selected: Boolean, lit: Boolean, hasHits: Boolean, hasFx: Boolean, onHit: () -> Unit, modifier: Modifier, ratio: Boolean = true) {
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
            .clickable(interactionSource = interaction, indication = null, onClick = onHit),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name,
            textAlign = TextAlign.Center,
            // Lower pads (a phone): two lines set closer, clear of the LED and of the colour strip.
            style = if (ratio) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
            color = lerp(Neon.TextPrimary, Color(0xFF1E2023), glow),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            // A phone's pads are narrow (80 dp on a 360 dp phone): « Cymbale » must not break mid-word.
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
        // Effects of its own: a small badge in the corner.
        if (hasFx) {
            Text(
                "FX", style = MaterialTheme.typography.labelSmall, color = Neon.OnAccent,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Neon.Cyan).padding(horizontal = 4.dp),
            )
        }
        // The pad's colour as a strip along its bottom edge.
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = if (ratio) 6.dp else 4.dp).fillMaxWidth(0.4f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.6f + 0.4f * glow)))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepGrid(project: Project, lane: Int, currentStep: Int, playing: Boolean, actions: RhythmActions) {
    val haptics = LocalHapticFeedback.current
    val hits = project.drums.lanes[lane].hits
    val color = DrumKitInfo.colors[lane]
    // One page per bar of the time signature (12 steps in 3/4, 16 in 4/4…).
    val per = project.stepsPerBar
    val bars = (project.drums.steps / per).coerceAtLeast(1)
    var chosenBar by rememberSaveable { mutableIntStateOf(0) }
    var follow by rememberSaveable { mutableStateOf(true) }
    // 0 hits, 1 rolls, 2 pan, 3 pitch.
    var tool by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
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
        }
        if (allPads) {
            AllPadsGrid(project, bar, currentStep, actions)
            lengths()
            return@NeonCard
        }
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until per / 2) {
                    val step = bar * per + row * (per / 2) + col
                    val velocity = hits[step]?.velocity
                    val isBeat = step % 4 == 0
                    val playing = step == currentStep
                    val hit = hits[step]
                    val isPicked = tool >= 2 && step == picked && hit != null
                    // Empty cells are hollowed into the panel; the beats a little lighter.
                    val fill = Color.Black.copy(alpha = if (isBeat) 0.16f else 0.28f)
                    val cell = RoundedCornerShape(7.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatioAtMost(0.8f, 110.dp)
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
                                            actions.cycleVelocity(step)
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
                                Text("×$repeats", style = MaterialTheme.typography.labelLarge, color = Color(0xFF1E2023),
                                    modifier = Modifier.align(Alignment.Center))
                            }
                            // Own pan or pitch: a small mark at the top of the cell.
                            val mark = listOfNotNull(
                                hit?.pan?.takeIf { it != 0f }?.let { panShort(it) },
                                hit?.pitch?.takeIf { it != 0 }?.let { if (it > 0) "+$it" else "$it" },
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
            LabeledSlider(tr("Pan du coup %d", picked % per + 1), panLabel(pickedHit.pan), pickedHit.pan, -1f..1f, steps = 19) {
                actions.setHitPan(picked, (it * 10).roundToInt() / 10f)
            }
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
                else -> tr("Touche une case pour ajouter un coup. Appui long sur un coup : fort → moyen → doux (la case se vide à moitié).")
            },
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
        lengths()
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
                Text(tr("  Effets du pad"), maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = actions.openEffects, contentPadding = narrow, modifier = Modifier.weight(1f).testTag("drum_kit_effects")) {
                Text(tr("Toute la batterie"), maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        val pan = project.drums.lanes[lane].pan
        LabeledSlider(tr("Pan du pad"), panLabel(pan), pan, -1f..1f, steps = 19) { actions.setPadPan((it * 10).roundToInt() / 10f) }
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
                Text(task.message)
            }
            is SampleTask.Recording -> {
                Text(tr("Enregistrement… %.1f s / 30 s", task.seconds), color = Neon.Red)
                LevelMeter(task.level)
                Button(
                    onClick = actions.stopRecording,
                    modifier = Modifier.fillMaxWidth().testTag("record_stop"),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon.Red, contentColor = Color.Black),
                ) { Icon(Icons.Filled.Stop, contentDescription = null); Text(tr(" Arrêter")) }
            }
            else -> {
                if (task is SampleTask.Failed) {
                    Text(task.message, color = Neon.Red, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { actions.dismissSampleError(); actions.importSample() }, modifier = Modifier.weight(1f).testTag("import_sample")) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null); Text(tr(" Importer"))
                    }
                    OutlinedButton(onClick = { actions.dismissSampleError(); actions.startRecording() }, modifier = Modifier.weight(1f).testTag("record_sample")) {
                        Icon(Icons.Filled.Mic, contentDescription = null); Text(tr(" Micro"))
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
                    AlertDialog(
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
