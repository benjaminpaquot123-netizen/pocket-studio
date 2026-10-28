package com.pocketstudio.app.synth

import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.layout.onSizeChanged
import com.pocketstudio.model.i18n.inSentence
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount

import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.Help
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.model.ArpMode
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Performance
import com.pocketstudio.model.Progression
import com.pocketstudio.model.labelIn
import com.pocketstudio.model.Scale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteEdits
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import kotlin.math.abs
import kotlin.math.roundToInt

data class PianoRollActions(
    val add: (Note) -> Unit = {},
    val remove: (Note) -> Unit = {},
    val change: (old: Note, new: Note) -> Unit = { _, _ -> },
    val quantize: (gridTicks: Int, strength: Float) -> Unit = { _, _ -> },
    /** « Comme joué »: every note put in time goes back where it was played. */
    val asPlayed: () -> Unit = {},
    val setSteps: (Int) -> Unit = {},
    val clear: () -> Unit = {},
    val addChord: (start: Int, length: Int, pitch: Int) -> Unit = { _, _, _ -> },
    val setHarmony: (Harmony) -> Unit = {},
    val applyProgression: (Progression) -> Unit = {},
    val applyBassLine: (Progression) -> Unit = {},
    val applyArpeggio: (Progression) -> Unit = {},
    val setPerformance: (Performance) -> Unit = {},
    /** A whole-clip edit (selection tools); [key] merges a quick burst into one undo step. */
    val replaceClip: (NoteClip, key: String?) -> Unit = { _, _ -> },
    /** Phase 37: notes copied, kept by the app for every track and part (see [PianoRoll]'s clipboard). */
    val copyNotes: (com.pocketstudio.model.NoteClipboard) -> Unit = {},
)

/** A bar of the piano roll (its lines are drawn every 16 sixteenths). */

/**
 * The notes a touch on the tutorial's dotted note [dotted] writes: itself, or with « Accords » on its
 * whole dotted chord, the dotted notes at its start not written yet (testers, lot « tuto 5 »: « toucher
 * la note du bas pose les 3 », and it wrote one).
 */
internal fun dottedTouch(dotted: Note, hints: List<Note>, clip: NoteClip, chordMode: Boolean): List<Note> =
    if (!chordMode) listOf(dotted)
    else hints.filter { h -> h.start == dotted.start && clip.notes.none { it.pitch == h.pitch && it.start == h.start } }.ifEmpty { listOf(dotted) }

private const val LOW = 24   // C1
private const val HIGH = 108 // C8: the top of a piano, where the keyboard's highest octaves still write
/** A row (a semitone) on a phone, and on a tablet where fingers aim at a single row. */
private val ROW = 20.dp
private val TABLET_ROW = 28.dp
/** The keys column on the left: the C's named on a phone, every note on a tablet. */
private val KEYS = 36.dp
private val TABLET_KEYS = 56.dp
private val TICK = NoteClip.TICKS_PER_STEP
private val NEW_LENGTHS = listOf(1 to "1/16", 2 to "1/8", 4 to "1/4", 8 to "1/2")
private val BLACK = setOf(1, 3, 6, 8, 10)
/** Width of a sixteenth for each zoom level of « − » and « + » (a pinch goes anywhere between [MIN_STEP] and [MAX_STEP]). */
private val ZOOMS = listOf(18.dp, 30.dp, 46.dp)
/** A sixteenth at its widest (zoomed in), and at its narrowest a few dp: 16 bars still fit on a phone (« Tout voir »). */
private const val MAX_STEP = 64f
private const val MIN_STEP = 1.2f
/** A row (a semitone) at its tallest and at its smallest (« Tout voir »: a whole bass and lead fit; still seen, still touched). */
private const val MAX_ROW = 40f
private const val MIN_ROW = 5f
/** Below this row height the keys are not named (their text would not fit): the C's lines stay. */
private const val NAMED_ROW = 12f
/** Grid for placing and moving notes, in ticks: 1/8, 1/16, 1/16 triplet, 1/32. */
private val GRIDS = listOf(48 to "1/8", 24 to "1/16", 16 to "1/16 t", 12 to "1/32")

/**
 * The loop one bar longer ([by] 1) or shorter (-1) in bars of [barSteps]: a loop that is not whole
 * bars yet (made in another time signature) first goes to its nearest whole bars.
 */
fun loopBars(steps: Int, barSteps: Int, by: Int): Int {
    val bar = barSteps.coerceAtLeast(1)
    val bars = if (by > 0) steps / bar + by else (steps + bar - 1) / bar + by
    return (bars.coerceAtLeast(1) * bar).coerceIn(bar.coerceAtMost(NoteClip.MAX_STEPS), NoteClip.MAX_STEPS)
}

/** Velocity steps a long press cycles through (same as the drum grid). */
fun nextVelocity(v: Float) = when {
    v > 0.75f -> 0.5f
    v > 0.35f -> 0.2f
    else -> 1f
}

/**
 * Piano roll for the synth. Draw mode: tap an empty cell to add a note, tap a note to delete it,
 * drag it to move, drag its right end to lengthen, long press to cycle its strength. Select
 * mode: tap notes or draw a box, then move, copy, transpose… the selection. The velocity lane
 * under the grid sets strengths with a finger. Ghost notes show the acid bass line.
 */
@Composable
fun PianoRoll(
    clip: NoteClip,
    ghosts: List<Note>,
    playStep: Int,
    actions: PianoRollActions,
    modifier: Modifier = Modifier,
    gridHeight: androidx.compose.ui.unit.Dp = 380.dp,
    harmony: Harmony = Harmony(),
    initialSelection: Set<Note> = emptySet(),
    /** The keys on the left play their note (heard, and recorded while ● records); null: silent names. */
    keyOn: ((Int, Float) -> Unit)? = null,
    keyOff: ((Int) -> Unit)? = null,
    /**
     * Phase 37: the notes copied in any track or part (kept by the app); null: none. A local copy is
     * used when the screen is shown without the app (previews).
     */
    clipboard: com.pocketstudio.model.NoteClipboard? = null,
    /** Phase 37: a frozen track's notes cannot be changed (« Dégeler »): said, not drawn on. */
    frozen: Boolean = false,
    /** Sixteenths in a bar of the time signature (12 in 3/4): copies, pastes and the ruler count in its bars. */
    stepsPerBar: Int = 16,
    /** « Tuto de A à Z »: the notes a step asks for, dotted until written (a touch on one writes it). */
    hints: List<Note> = emptyList(),
    /** On the grid itself (the tutorial brings it into view). */
    gridModifier: Modifier = Modifier,
    /** The play position read where it is drawn (audit 16: the grid is not redrawn at each step); null: [playStep]. */
    playStepOf: (() -> Int)? = null,
    /** The track's name, said on the tutorial's line with the bar in sight (testers, lot « tuto 5 »). */
    trackName: String? = null,
    /**
     * « Tuto de A à Z » (testers, lot « tuto 5 »): the notes to write by hand are all written, « Copier sur
     * les mesures suivantes » does the rest: offered right on the tutorial's line over the grid (null: not now).
     */
    copyMotif: (() -> Unit)? = null,
) = com.pocketstudio.ui.LeftToRight {
    val barSteps = stepsPerBar.coerceIn(1, NoteClip.MAX_STEPS)
    val givenStep by androidx.compose.runtime.rememberUpdatedState(playStep)
    val stepOf: () -> Int = remember(playStepOf) { playStepOf ?: { givenStep } }
    // The bar playing, followed only when it changes (a paste goes on it), not at each step.
    val playBar by remember(stepOf, clip.steps, barSteps) {
        androidx.compose.runtime.derivedStateOf { stepOf().let { if (it >= 0) (it % clip.steps) / barSteps else -1 } }
    }
    val barTicks = barSteps * NoteClip.TICKS_PER_STEP
    var newSteps by rememberSaveable { mutableIntStateOf(2) }
    var chordMode by rememberSaveable { mutableStateOf(false) }
    var showGhosts by rememberSaveable { mutableStateOf(true) }
    var selectMode by rememberSaveable { mutableStateOf(initialSelection.isNotEmpty()) }
    // Owner (lot « tuto 5 », É8): a pinch zooms the time and the notes, « Tout voir » frames every note; the zoom
    // stays the user's (0: not chosen yet, a bar fits the width). Nothing scrolls nor zooms by itself.
    var stepDp by rememberSaveable { mutableFloatStateOf(0f) }
    var rowDp by rememberSaveable { mutableFloatStateOf(0f) }
    /** « Tout voir » touched (each touch frames again). */
    var fitAll by rememberSaveable { mutableIntStateOf(0) }
    var grid by rememberSaveable { mutableIntStateOf(TICK) }
    var selection by remember { mutableStateOf(initialSelection) }
    // The bar a paste goes on (« Coller sur la mes. N »): the one chosen above the grid, or the one after a copy.
    var targetBar by rememberSaveable { mutableIntStateOf(0) }
    var localBoard by remember { mutableStateOf<com.pocketstudio.model.NoteClipboard?>(null) }
    val board = clipboard ?: localBoard
    val copyNotes: (com.pocketstudio.model.NoteClipboard) -> Unit = { b ->
        localBoard = b
        actions.copyNotes(b)
        targetBar = b.fromBar + b.bars
    }
    // Notes edited elsewhere (undo, another tool) leave the selection.
    val live = selection.filter { it in clip.notes }.toSet()
    if (live != selection) selection = live
    val apply: (NoteEdits.Result, String?) -> Unit = { r, key -> actions.replaceClip(r.clip, key); selection = r.selection }
    val chipColors = pillColors()
    val hScroll = rememberScrollState()
    // A tablet: taller rows for the fingers, every note named, tools that wrap instead of being cut.
    val tablet = isTablet()
    val keysWidth = if (tablet) TABLET_KEYS else KEYS
    // Short loops fill the width on a big screen (tablet) instead of leaving the right half empty.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val viewWidth = (maxWidth - keysWidth).value.coerceAtLeast(1f)
        // Not chosen yet: a whole bar in sight on a phone (testers, lot « tuto 5 »: half a bar was shown, no bar number in it),
        // a short loop filling a tablet's width.
        val defaultStep = (viewWidth / barSteps).coerceIn(ZOOMS[0].value, ZOOMS[1].value)
        val stepWidth = maxOf(if (stepDp > 0f) stepDp else defaultStep, viewWidth / clip.steps).coerceAtMost(MAX_STEP).dp
        // A whole number of pixels a row: the keys (one box a row) and the grid's rows (drawn) stay in line at any zoom.
        val rowHeight = with(LocalDensity.current) {
            (if (rowDp > 0f) rowDp else (if (tablet) TABLET_ROW else ROW).value).dp.toPx().roundToInt().coerceAtLeast(1).toDp()
        }
        val baseRow = (if (tablet) TABLET_ROW else ROW).value
        val zoomBy: (Float, Float) -> Unit = { fx, fy ->
            // From the zoom as it is now (the states, read at each call: a pinch calls this many times between two frames).
            val step = maxOf(if (stepDp > 0f) stepDp else defaultStep, viewWidth / clip.steps)
            val row = if (rowDp > 0f) rowDp else baseRow
            stepDp = (step * fx).coerceIn(minOf(MIN_STEP, viewWidth / clip.steps), MAX_STEP)
            rowDp = (row * fy).coerceIn(MIN_ROW, MAX_ROW)
        }
        Column(Modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolRow(tablet) {
                FilterChip(selected = !selectMode, onClick = { selectMode = false; selection = emptySet() }, label = { Text(tr("✏ Dessiner")) }, colors = chipColors,
                    modifier = Modifier.testTag("mode_draw"))
                FilterChip(selected = selectMode, onClick = { selectMode = true }, label = { Text(tr("⬚ Sélectionner")) }, colors = chipColors,
                    modifier = Modifier.testTag("mode_select"))
                // « Tout voir » (owner, lot « tuto 5 »): every bar, from the lowest note to the highest, in one touch.
                FilterChip(
                    selected = false,
                    onClick = {
                        val pitches = (clip.notes + hints).map { it.pitch }
                        stepDp = (viewWidth / clip.steps).coerceAtMost(MAX_STEP)
                        val span = if (pitches.isEmpty()) 24 else pitches.max() - pitches.min() + 3
                        rowDp = (gridHeight.value / span).coerceIn(MIN_ROW, MAX_ROW)
                        fitAll++
                    },
                    label = { Text(tr("Tout voir"), maxLines = 1, softWrap = false) }, colors = chipColors,
                    modifier = Modifier.testTag("roll_fit"),
                )
                // The word with its buttons (a tablet's row wraps them together).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("  Zoom"), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { zoomBy(1 / 1.5f, 1f) }, enabled = stepWidth.value > minOf(MIN_STEP, viewWidth / clip.steps) + 0.01f, modifier = Modifier.testTag("zoom_out")) { Text("−") }
                    // The level between − and +, so the gap between them means something.
                    if (tablet) Text(com.pocketstudio.model.i18n.percent((stepWidth.value / ZOOMS[1].value * 100).roundToInt()), style = MaterialTheme.typography.labelLarge,
                        color = Neon.TextSecondary, modifier = Modifier.testTag("zoom_level"))
                    TextButton(onClick = { zoomBy(1.5f, 1f) }, enabled = stepWidth.value < MAX_STEP - 0.01f, modifier = Modifier.testTag("zoom_in")) { Text("+") }
                }
            }
            val loop: @Composable () -> Unit = {
                // Phase 37: any number of whole bars, 1 to 16. Audit final: bars of the time signature (12 steps in 3/4).
                Text(tr("Boucle"), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { actions.setSteps(loopBars(clip.steps, barSteps, -1)) }, enabled = clip.steps > barSteps, modifier = Modifier.testTag("loop_minus")) { Text("−") }
                    Text(com.pocketstudio.app.ui.barsShort(((clip.steps + barSteps - 1) / barSteps).coerceAtLeast(1)), style = MaterialTheme.typography.labelLarge, color = Neon.Cyan, maxLines = 1, softWrap = false,
                        modifier = Modifier.testTag("loop_bars"))
                    TextButton(onClick = { actions.setSteps(loopBars(clip.steps, barSteps, 1)) }, enabled = clip.steps + barSteps <= NoteClip.MAX_STEPS, modifier = Modifier.testTag("loop_plus")) { Text("+") }
                }
            }
            val noteLengths: @Composable () -> Unit = {
                Text(tr("  Note"), style = MaterialTheme.typography.bodySmall)
                NEW_LENGTHS.forEach { (steps, label) ->
                    FilterChip(selected = newSteps == steps, onClick = { newSteps = steps }, label = { Text(label) }, colors = chipColors)
                }
            }
            val grids: @Composable () -> Unit = {
                Text(tr("  Grille"), style = MaterialTheme.typography.bodySmall)
                GRIDS.forEach { (ticks, label) ->
                    FilterChip(selected = grid == ticks, onClick = { grid = ticks }, label = { Text(label) }, colors = chipColors,
                        modifier = Modifier.testTag("grid_$ticks"))
                }
            }
            if (tablet) {
                // A tablet: whole groups wrap (never a lone « 1/32 » on its line).
                ToolRow(true) {
                    listOf(loop, noteLengths, grids).forEach { group ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { group() }
                    }
                }
            } else {
                ToolRow(false) { loop(); noteLengths(); grids() }
            }
            if (frozen) {
                Text(tr("Piste gelée : elle joue son rendu audio dans le morceau. « Dégeler » (menu de la piste) pour modifier ses notes."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("roll_frozen"))
            }
            val pasteBar = if (playBar >= 0) playBar else targetBar.coerceIn(0, NoteClip.MAX_STEPS / barSteps - 1)
            val paste: () -> NoteEdits.Result? = {
                board?.let { b ->
                    NoteEdits.pasteOnBar(clip, b, pasteBar, barTicks).also { r ->
                        if (r.selection.isNotEmpty()) apply(r, null)
                        targetBar = pasteBar + b.bars
                    }
                }
            }
            if (selectMode || selection.isNotEmpty()) {
                SelectionBar(clip, selection, board, pasteBar, grid, chipColors, tablet,
                    select = { selection = it }, copy = { NoteEdits.toClipboard(selection, barTicks)?.let(copyNotes) }, paste = paste, apply = apply)
            } else {
                if (board != null) PasteRow(board, pasteBar, paste)
                ToolRow(tablet, spacing = 2.dp) {
                    // Testers (lot « tuto 5 »): « Accords » first, in sight (it was the last of a row cut at the edge).
                    FilterChip(selected = chordMode, onClick = { chordMode = !chordMode }, label = { Text(tr("Accords")) }, colors = chipColors,
                        modifier = Modifier.testTag("chord_mode"))
                    // « Remettre en rythme » from where the notes were played: « Comme joué » undoes it any time.
                    TextButton(onClick = { actions.quantize(grid, 0.5f) }, modifier = Modifier.testTag("quantize_50")) { Text(tr("En rythme 50 %")) }
                    TextButton(onClick = { actions.quantize(grid, 1f) }, modifier = Modifier.testTag("quantize_100")) { Text(com.pocketstudio.model.i18n.percent(100)) }
                    if (clip.anyMovedInTime) {
                        TextButton(onClick = actions.asPlayed, modifier = Modifier.testTag("notes_as_played")) { Text(tr("Comme joué")) }
                    }
                    FilterChip(selected = showGhosts, onClick = { showGhosts = !showGhosts }, label = { Text(tr("Basse en fantôme")) }, colors = chipColors)
                    TextButton(onClick = actions.clear, enabled = clip.notes.isNotEmpty()) { Text(tr("Effacer")) }
                }
            }
            // « Tuto de A à Z » (testers, lot « tuto 3 »): how many dotted notes are left, and which way when none is in sight;
            // one line the whole step (the grid never moves under a finger), a touch shows the next one.
            // Testers (lot « tuto 5 »): the tutorial's line, the bar numbers and the grid brought into view together
            // (the line and the bar number were left above the screen).
            Column(gridModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hints.isNotEmpty()) {
                    TutoNotesLeft(
                        hints.filter { h -> clip.notes.none { n -> n.pitch == h.pitch && n.start == h.start } }, hScroll, stepWidth, barSteps,
                        bars = ((clip.steps + barSteps - 1) / barSteps).coerceAtLeast(1), trackName = trackName, copy = copyMotif,
                    )
                }
                BarRuler(clip, hScroll, stepWidth, keysWidth, if (selection.isNotEmpty() || selectMode) targetBar else -1, barSteps) { bar ->
                    // A bar touched: its notes chosen (Copier, Supprimer…), and the bar a paste goes on.
                    targetBar = bar
                    selection = NoteEdits.inBar(clip, bar, barTicks)
                }
                Grid(
                    clip, if (showGhosts) ghosts else emptyList(), stepOf, newSteps * TICK, actions, gridHeight, harmony, chordMode,
                    hScroll, stepWidth, grid, selectMode, selection, select = { selection = it }, apply = apply,
                    rowHeight = rowHeight, keysWidth = keysWidth, allNames = tablet, keyOn = keyOn, keyOff = keyOff,
                    locked = frozen, barSteps = barSteps, hints = hints, zoomBy = zoomBy, fitAll = fitAll,
                )
            }
            VelocityLane(clip, selection, hScroll, stepWidth, apply, keysWidth)
            Text(
                if (selectMode) tr("Touche des notes ou trace un cadre pour les choisir ; glisse une note choisie pour tout déplacer. Couloir du bas : la force de chaque note.")
                else tr("Touche une case : ajoute une note. Touche une note : la choisit. Appui long : l'enlève. Glisse : déplace, le bout : allonge. Numéros en haut : toute une mesure. Couloir du bas : glisse pour régler la force."),
                style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary,
            )
        }
    }
}

/** Tools for the selected notes (FL Studio Mobile's selection menu, as buttons). */
@Composable
private fun SelectionBar(
    clip: NoteClip,
    selection: Set<Note>,
    clipboard: com.pocketstudio.model.NoteClipboard?,
    pasteBar: Int,
    grid: Int,
    chipColors: androidx.compose.material3.SelectableChipColors,
    wrap: Boolean,
    select: (Set<Note>) -> Unit,
    copy: () -> Unit,
    paste: () -> NoteEdits.Result?,
    apply: (NoteEdits.Result, String?) -> Unit,
) {
    val any = selection.isNotEmpty()
    // A paste that did not all fit in the loop says so (until the next paste).
    var noRoom by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ToolRow(wrap, spacing = 2.dp) {
            Text(if (!any) tr("Rien de choisi") else if (selection.size > 1) tr("%d notes", selection.size) else tr("%d note", selection.size),
                style = MaterialTheme.typography.bodySmall, color = Neon.Cyan, modifier = Modifier.testTag("selection_count"))
            TextButton(onClick = { select(clip.notes.toSet()) }, modifier = Modifier.testTag("select_all")) { Text(tr("Tout")) }
            TextButton(onClick = { select(emptySet()) }, enabled = any) { Text(tr("Aucune")) }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, 1), "sel-pitch") }, enabled = any, modifier = Modifier.testTag("sel_up")) { Text(tr("▲ ½ ton")) }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, -1), "sel-pitch") }, enabled = any) { Text(tr("▼ ½ ton")) }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, 12), "sel-pitch") }, enabled = any) { Text(tr("▲ octave")) }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, -12), "sel-pitch") }, enabled = any) { Text(tr("▼ octave")) }
            TextButton(onClick = { apply(NoteEdits.shift(clip, selection, -grid), "sel-move") }, enabled = any) { Text("◀") }
            TextButton(onClick = { apply(NoteEdits.shift(clip, selection, grid), "sel-move") }, enabled = any) { Text("▶") }
        }
        ToolRow(wrap, spacing = 2.dp) {
            TextButton(
                onClick = {
                    // Like a paste: copies that do not fit in the loop are said, not silently lost.
                    val copied = NoteEdits.duplicate(clip, selection)
                    noRoom = copied.dropped > 0
                    if (copied.selection.isNotEmpty()) apply(copied, null)
                },
                enabled = any, modifier = Modifier.testTag("sel_duplicate"),
            ) { Text(tr("Dupliquer")) }
            TextButton(onClick = copy, enabled = any, modifier = Modifier.testTag("sel_copy")) { Text(tr("Copier")) }
            TextButton(
                onClick = {
                    // On the bar playing while it plays, else on the bar chosen (the one after a copy).
                    val pasted = paste()
                    noRoom = (pasted?.dropped ?: 0) > 0
                },
                enabled = clipboard != null, modifier = Modifier.testTag("sel_paste"),
            ) { Text(tr("Coller mes. %d", pasteBar + 1)) }
            TextButton(onClick = { apply(NoteEdits.quantize(clip, selection, grid), null) }, enabled = any) { Text(tr("Remettre en rythme")) }
            if (selection.any { it.played != null }) {
                TextButton(onClick = { apply(NoteEdits.asPlayed(clip, selection), null) }) { Text(tr("Comme joué")) }
            }
            TextButton(onClick = { apply(NoteEdits.legato(clip, selection), null) }, enabled = any) { Text(tr("Legato")) }
            TextButton(onClick = { apply(NoteEdits.humanize(clip, selection), null) }, enabled = any) { Text(tr("Humaniser")) }
            TextButton(onClick = { apply(NoteEdits.delete(clip, selection), null) }, enabled = any, modifier = Modifier.testTag("sel_delete")) {
                Text(tr("Supprimer"), color = if (any) Neon.Red else Neon.TextSecondary)
            }
        }
        if (noRoom) {
            Text(tr("Plus de place : allonge la boucle pour que tout y tienne."), style = MaterialTheme.typography.bodySmall, color = Neon.Amber,
                modifier = Modifier.testTag("paste_no_room"))
        }
    }
}

/**
 * Phase 37: the notes copied (in this track or another, this part or another) and « Coller sur la
 * mes. N » when nothing is chosen.
 */
@Composable
private fun PasteRow(board: com.pocketstudio.model.NoteClipboard, pasteBar: Int, paste: () -> NoteEdits.Result?) {
    var noRoom by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("paste_row")) {
        val what = if (board.notes.size > 1) tr("%d notes", board.notes.size) else tr("%d note", board.notes.size)
        Text(
            if (board.from.isNotBlank()) tr("Copié : %1\$s (%2\$s)", what, board.from) else tr("Copié : %s", what),
            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2,
        )
        TextButton(onClick = { noRoom = (paste()?.dropped ?: 0) > 0 }, modifier = Modifier.testTag("sel_paste")) { Text(tr("Coller mes. %d", pasteBar + 1)) }
    }
    if (noRoom) {
        Text(tr("Plus de place : allonge la boucle pour que tout y tienne."), style = MaterialTheme.typography.bodySmall, color = Neon.Amber)
    }
}

/** Phase 37: the bars' numbers over the grid; a touch picks a bar's notes and where a paste goes. */
@Composable
private fun BarRuler(
    clip: NoteClip,
    hScroll: androidx.compose.foundation.ScrollState,
    stepWidth: androidx.compose.ui.unit.Dp,
    keysWidth: androidx.compose.ui.unit.Dp,
    chosen: Int,
    barSteps: Int,
    pick: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(32.dp)) {
        Box(Modifier.width(keysWidth), contentAlignment = Alignment.Center) {
            Text(tr("mes."), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1, softWrap = false)
        }
        Box(Modifier.weight(1f).horizontalScroll(hScroll)) {
            Row {
                // The time signature's bars (the last one cut where the loop ends in 3/4).
                val density = LocalDensity.current
                val stepPx = with(density) { stepWidth.toPx() }
                val labelPx = with(density) { 30.dp.toPx() }
                for (b in 0 until ((clip.steps + barSteps - 1) / barSteps).coerceAtLeast(1)) {
                    val on = b == chosen
                    val barPx = stepPx * minOf(barSteps, clip.steps - b * barSteps).coerceAtLeast(1)
                    Box(
                        Modifier.width(stepWidth * minOf(barSteps, clip.steps - b * barSteps).coerceAtLeast(1)).height(32.dp).padding(horizontal = 1.dp)
                            .background(if (on) Neon.Acid.copy(alpha = 0.25f) else Neon.SurfaceHigh, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .clickable { pick(b) }
                            .testTag("roll_bar_$b"),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        // Testers (lot « tuto 5 »): the number stays in sight while its bar is (it was at the bar's start,
                        // out of the screen with half a bar shown). Read at layout: a scroll does not recompose.
                        Text("${b + 1}", style = MaterialTheme.typography.labelLarge, color = if (on) Neon.Acid else Neon.TextSecondary, maxLines = 1, softWrap = false,
                            modifier = Modifier.offset {
                                androidx.compose.ui.unit.IntOffset((hScroll.value - b * barSteps * stepPx).coerceIn(0f, (barPx - labelPx).coerceAtLeast(0f)).roundToInt(), 0)
                            }.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

/** One bar per note, as tall as it is strong: drag a finger along it to set the strengths. */
@Composable
private fun VelocityLane(
    clip: NoteClip,
    selection: Set<Note>,
    hScroll: androidx.compose.foundation.ScrollState,
    stepWidth: androidx.compose.ui.unit.Dp,
    apply: (NoteEdits.Result, String?) -> Unit,
    keysWidth: androidx.compose.ui.unit.Dp,
) {
    val density = LocalDensity.current
    val stepPx = with(density) { stepWidth.toPx() }
    val barColor = Neon.Cyan
    val selColor = Neon.Acid
    val lineColor = Neon.Outline.copy(alpha = 0.55f)
    // Latest values for the gesture without restarting it on every edit.
    val current by androidx.compose.runtime.rememberUpdatedState(clip to selection)
    Row(Modifier.fillMaxWidth().height(56.dp)) {
        // The same word as the Rhythm tool and the glossary ("Force", "Velocity", "Kracht"), shrunk to fit the keys' width.
        Box(Modifier.width(keysWidth).padding(horizontal = 1.dp), contentAlignment = Alignment.Center) {
            val label = MaterialTheme.typography.labelSmall
            androidx.compose.foundation.text.BasicText(
                tr("Force"), style = label.copy(color = Neon.TextSecondary, letterSpacing = 0.sp), maxLines = 1,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = label.fontSize, stepSize = 0.5.sp),
            )
        }
        Box(Modifier.weight(1f).horizontalScroll(hScroll)) {
            Canvas(
                Modifier.size(stepWidth * clip.steps, 56.dp).testTag("velocity_lane").pointerInput(stepPx) {
                    awaitEachGesture {
                        var change = awaitFirstDown()
                        while (true) {
                            val (c, sel) = current
                            val tick = (change.position.x / stepPx * TICK).toInt()
                            val v = (1f - change.position.y / size.height).coerceIn(0.05f, 1f)
                            // Notes starting in the touched sixteenth (only the selected ones when there is a selection).
                            val targets = c.notes.filter { tick / TICK == it.start / TICK && (sel.isEmpty() || it in sel) }.toSet()
                            if (targets.isNotEmpty()) {
                                val r = NoteEdits.setVelocity(c, targets, v)
                                apply(NoteEdits.Result(r.clip, if (sel.isEmpty()) sel else (sel - targets) + r.selection), "vel-lane")
                            }
                            change.consume()
                            val event = awaitPointerEvent()
                            change = event.changes.firstOrNull { it.pressed } ?: break
                        }
                    }
                },
            ) {
                drawLine(lineColor, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
                clip.notes.forEach { n ->
                    val x = n.start / TICK.toFloat() * stepPx
                    val h = n.velocity * (size.height - 4f)
                    drawRect(if (n in selection) selColor else barColor, Offset(x + 1f, size.height - h), Size((stepPx * 0.4f).coerceAtLeast(3f), h))
                }
            }
        }
    }
}

@Composable
private fun Grid(
    clip: NoteClip,
    ghosts: List<Note>,
    playStep: () -> Int,
    newLength: Int,
    actions: PianoRollActions,
    height: androidx.compose.ui.unit.Dp,
    harmony: Harmony,
    chordMode: Boolean,
    hScroll: androidx.compose.foundation.ScrollState,
    stepWidth: androidx.compose.ui.unit.Dp,
    grid: Int,
    selectMode: Boolean,
    selection: Set<Note>,
    select: (Set<Note>) -> Unit,
    apply: (NoteEdits.Result, String?) -> Unit,
    rowHeight: androidx.compose.ui.unit.Dp,
    keysWidth: androidx.compose.ui.unit.Dp,
    allNames: Boolean,
    keyOn: ((Int, Float) -> Unit)?,
    keyOff: ((Int) -> Unit)?,
    locked: Boolean = false,
    barSteps: Int = 16,
    hints: List<Note> = emptyList(),
    /** A pinch: the time (x) and the notes (y) zoomed by these factors. */
    zoomBy: (Float, Float) -> Unit = { _, _ -> },
    /** « Tout voir » touched: the lowest to the highest note framed, from the first bar. */
    fitAll: Int = 0,
) {
    val vScroll = rememberScrollState()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val english = com.pocketstudio.app.settings.englishNotes()
    val density = LocalDensity.current
    val rowPx = with(density) { rowHeight.toPx() }
    val stepPx = with(density) { stepWidth.toPx() }
    val slop = LocalViewConfiguration.current.touchSlop
    // Do1 to Do8, wider when notes lie past it (an import, a transposed track): every note can be seen and touched.
    val low = minOf(LOW, clip.notes.minOfOrNull { it.pitch } ?: LOW)
    val high = maxOf(HIGH, clip.notes.maxOfOrNull { it.pitch } ?: HIGH)
    val rows = high - low + 1
    // Start around C4-C5; the tutorial's dotted notes in view when it shows some (a bass is low).
    // (around the line's middle: a few high notes of a bass must not take the low ones out of sight)
    val hintTop = hints.map { it.pitch }.sorted().takeIf { it.isNotEmpty() }?.let { p -> (p[p.size / 2] + 7).coerceIn(low, high) }
    // Testers (lot « tuto 5 »): placed once when the roll opens and once when a step begins (the user's own
    // « Étape suivante »), never again by itself: the notes written by hand turning into the whole line (the
    // copy) moved the view and its « Do3 » became « Do2 » under the finger.
    val step = com.pocketstudio.app.tuto.TutoHints.step
    var placedFor by rememberSaveable { mutableStateOf<String?>(null) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(step, hintTop != null) {
        val key = if (hintTop != null) step ?: "" else null
        if (!placed || (key != null && key != placedFor)) vScroll.scrollTo(((high - (hintTop ?: 76)) * rowPx).roundToInt())
        if (key != null) placedFor = key
        placed = true
    }
    val tutoHint = Neon.Amber
    // « Tuto de A à Z » (testers, lot « tuto 3 »): the dotted notes still to touch. Lot « tuto 5 »: the roll goes to the
    // next one only when asked (a touch on « Encore 3 notes… » or « Je le fais »), lit a moment; never after a note.
    val left = remember(hints, clip.notes) {
        hints.filter { h -> h.pitch in low..high && clip.notes.none { n -> n.pitch == h.pitch && n.start == h.start } }.sortedWith(compareBy({ it.start }, { -it.pitch }))
    }
    var view by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var lit by remember { mutableStateOf<Note?>(null) }
    val focus = com.pocketstudio.app.tuto.TutoHints.focus
    LaunchedEffect(focus, view) {
        if (view.width <= 0 || view.height <= 0) return@LaunchedEffect
        val asked = focus != com.pocketstudio.app.tuto.TutoHints.focusSeen
        com.pocketstudio.app.tuto.TutoHints.focusSeen = focus
        if (!asked) return@LaunchedEffect
        val next = left.firstOrNull() ?: return@LaunchedEffect
        fun x(n: Note) = n.start / TICK.toFloat() * stepPx
        fun y(n: Note) = (high - n.pitch) * rowPx
        lit = next
        if (x(next) < hScroll.value || x(next) + stepPx > hScroll.value + view.width) hScroll.animateScrollTo((x(next) - 2 * stepPx).roundToInt().coerceIn(0, hScroll.maxValue))
        if (y(next) < vScroll.value || y(next) + rowPx > vScroll.value + view.height) vScroll.animateScrollTo((y(next) - view.height / 2f + rowPx / 2).roundToInt().coerceIn(0, vScroll.maxValue))
    }
    // The gesture lives across zooms: it reads the latest sizes and zoom (each step multiplies the size it has now).
    val latestZoom = rememberUpdatedState(zoomBy)
    val latestSizes = rememberUpdatedState(stepPx to rowPx)
    // A pinch keeps what is under the fingers where it is: the place (in steps and rows) put back once the new size is laid out.
    var anchor by remember { mutableStateOf<FloatArray?>(null) }
    LaunchedEffect(stepPx, rowPx) {
        val a = anchor ?: return@LaunchedEffect
        anchor = null
        androidx.compose.runtime.withFrameNanos { }
        hScroll.scrollTo((a[0] * stepPx - a[1]).roundToInt().coerceAtLeast(0))
        vScroll.scrollTo((a[2] * rowPx - a[3]).roundToInt().coerceAtLeast(0))
    }
    // « Tout voir »: the first bar on the left, the highest note (a row above it) at the top.
    // Only a new touch frames (the roll shown again keeps the user's place).
    val fitSeen = remember { intArrayOf(fitAll) }
    LaunchedEffect(fitAll) {
        if (fitAll == fitSeen[0]) return@LaunchedEffect
        fitSeen[0] = fitAll
        androidx.compose.runtime.withFrameNanos { }
        hScroll.scrollTo(0)
        val top = (clip.notes + hints).maxOfOrNull { it.pitch }
        vScroll.scrollTo(if (top == null) ((high - 76) * rowPx).roundToInt() else ((high - top - 1).coerceAtLeast(0) * rowPx).roundToInt())
    }
    LaunchedEffect(lit) { if (lit != null) { kotlinx.coroutines.delay(2600); lit = null } }
    val litPulse = if (lit != null) {
        androidx.compose.animation.core.rememberInfiniteTransition(label = "tuto_note").animateFloat(
            0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(450), androidx.compose.animation.core.RepeatMode.Reverse), label = "tuto_note_pulse",
        )
    } else null
    var drag by remember { mutableStateOf<Pair<Note, Note>?>(null) }
    var box by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var groupMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }  // (ticks, semitones) while dragging a selection
    val snap: (Int) -> Int = { t -> (t.toFloat() / grid).roundToInt() * grid }

    val colors = GridColors(
        bg = Neon.Surface, black = Neon.Background, line = Neon.Outline.copy(alpha = 0.35f),
        beat = Neon.Outline.copy(alpha = 0.55f), bar = Neon.TextSecondary, note = Neon.Cyan, ghost = Neon.Magenta.copy(alpha = 0.28f),
        play = Neon.TextPrimary, outside = Color.Black.copy(alpha = 0.35f), rootRow = Neon.Cyan.copy(alpha = 0.10f),
    )
    val selColor = Neon.Acid
    // Testers (lot « tuto 4 »): the keys and the grid each scrolled with the same state, and the keys' labels
    // (« Do3 / Do2 ») jumped an octave while the grid stayed: one vertical scroll now carries both.
    val keysPx = with(density) { keysWidth.roundToPx() }
    Row(
        Modifier.fillMaxWidth().height(height)
            .onSizeChanged { view = androidx.compose.ui.unit.IntSize((it.width - keysPx).coerceAtLeast(0), it.height) }
            .testTag("roll_view")
            // Two fingers (owner, lot « tuto 5 », É8): spread or pinch, across for the time, up and down for the notes.
            // Taken before the grid and the scrolls see them: a note never lands under a pinch.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    var last: Offset? = null
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val fingers = event.changes.filter { it.pressed }
                        if (fingers.isEmpty()) break
                        if (fingers.size < 2) { last = null; continue }
                        val a = fingers[0].position
                        val b = fingers[1].position
                        val spread = Offset(abs(a.x - b.x), abs(a.y - b.y))
                        val center = Offset((a.x + b.x) / 2 - keysPx, (a.y + b.y) / 2)
                        last?.let { before ->
                            // An axis counts once the fingers are apart along it (a pinch along the time leaves the rows as they are).
                            val min = 24.dp.toPx()
                            val fx = if (before.x > min && spread.x > min) spread.x / before.x else 1f
                            val fy = if (before.y > min && spread.y > min) spread.y / before.y else 1f
                            if (fx != 1f || fy != 1f) {
                                val (sp, rp) = latestSizes.value
                                anchor = floatArrayOf((hScroll.value + center.x) / sp, center.x, (vScroll.value + center.y) / rp, center.y)
                                latestZoom.value(fx, fy)
                            }
                        }
                        last = spread
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .verticalScroll(vScroll),
    ) {
        Column(Modifier.width(keysWidth).testTag("roll_keys")) {
            // Zoomed far out the rows are too small to carry a name: the C's lines stay.
            for (p in high downTo low) RollKey(p, rowHeight, keysWidth, english, named = rowHeight.value >= NAMED_ROW && (allNames || p % 12 == 0), keyOn, keyOff)
        }
        Box(Modifier.weight(1f).horizontalScroll(hScroll)) {
            Canvas(
                Modifier
                    .size(stepWidth * clip.steps, rowHeight * rows)
                    .testTag("piano_roll")
                    // Audit final (22): the notes within TalkBack's reach (the grid is drawn).
                    .semantics {
                        contentDescription = tr("Grille des notes : %d notes", clip.notes.size)
                        if (!locked) customActions = rollAccessibleActions(clip.notes, english, actions)
                    }
                    .pointerInput(clip, newLength, harmony, chordMode, selectMode, selection, grid, stepPx, locked, hints) {
                        fun pitchAt(y: Float) = (high - (y / rowPx).toInt()).coerceIn(low, high)
                        fun tickAt(x: Float) = (x / stepPx * TICK).toInt().coerceIn(0, clip.ticks - 1)
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (locked) return@awaitEachGesture  // a frozen track: scrolls only
                            val found = clip.noteAt(tickAt(down.position.x), pitchAt(down.position.y))
                            // Phase 37: a note of the selection dragged moves the whole selection (in both modes).
                            if (selectMode || (found != null && found in selection && selection.size > 1)) {
                                if (found == null) {
                                    // A box: the first move decides between a box and a scroll (long press first = box).
                                    var moved = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        if (!moved && (change.position - down.position).getDistance() > slop) {
                                            if (change.uptimeMillis - down.uptimeMillis < 300) return@awaitEachGesture  // a quick swipe scrolls
                                            moved = true
                                        }
                                        if (moved) {
                                            box = down.position to change.position
                                            change.consume()
                                        }
                                    }
                                    val b = box
                                    box = null
                                    if (b == null) {
                                        if (!moved) select(emptySet())  // a tap on nothing clears
                                        return@awaitEachGesture
                                    }
                                    val (a, c) = b
                                    select(NoteEdits.inBox(clip, tickAt(minOf(a.x, c.x)), tickAt(maxOf(a.x, c.x)) + 1, pitchAt(maxOf(a.y, c.y)), pitchAt(minOf(a.y, c.y))))
                                    return@awaitEachGesture
                                }
                                down.consume()
                                var moved = false
                                var delta = 0 to 0
                                // The block stops at the loop's ends and the keyboard's: its notes keep their gaps.
                                val moving = if (found in selection) selection else setOf(found)
                                val across = NoteEdits.shiftRange(clip, moving)
                                val upDown = NoteEdits.transposeRange(moving)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    // A second finger: a pinch (É8), the selection stays where it was.
                                    if (event.changes.count { it.pressed } > 1) { groupMove = null; return@awaitEachGesture }
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val dx = change.position.x - down.position.x
                                    val dy = change.position.y - down.position.y
                                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                                    if (moved) {
                                        delta = snap((dx / stepPx * TICK).roundToInt()).coerceIn(across) to (-(dy / rowPx).roundToInt()).coerceIn(upDown)
                                        groupMove = delta
                                    }
                                    change.consume()
                                }
                                groupMove = null
                                if (!moved) {
                                    val held = currentEvent.changes.firstOrNull()?.uptimeMillis?.minus(down.uptimeMillis) ?: 0L
                                    if (!selectMode && held > 450) {
                                        // Draw mode: a long press deletes, as everywhere in the roll.
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        actions.remove(found)
                                        select(selection - found)
                                    } else {
                                        select(if (found in selection) selection - found else selection + found)
                                    }
                                } else {
                                    val shifted = NoteEdits.shift(clip, moving, delta.first)
                                    apply(NoteEdits.transpose(shifted.clip, shifted.selection, delta.second), null)
                                }
                                return@awaitEachGesture
                            }
                            if (found == null) {
                                val up = waitForUpOrCancellation() ?: return@awaitEachGesture  // a scroll took it
                                if ((up.position - down.position).getDistance() < slop && selection.isNotEmpty()) {
                                    // Something chosen: a touch on nothing lets it go (FL Studio Mobile's way).
                                    select(emptySet())
                                } else if ((up.position - down.position).getDistance() < slop) {
                                    val tick = tickAt(down.position.x)
                                    val dotted = hints.firstOrNull { it.pitch == pitchAt(down.position.y) && tick in it.start until it.end }
                                    val start = tick / grid * grid
                                    val pitch = harmony.snap(pitchAt(down.position.y))
                                    // A dotted note of the tutorial touched: written as it is (its length, its place); with
                                    // « Accords » on, its whole dotted chord at once, one undo step (testers, lot « tuto 5 »: one note).
                                    val chord = dotted?.let { dottedTouch(it, hints, clip, chordMode) }.orEmpty()
                                    if (chord.size > 1) actions.replaceClip(chord.fold(clip) { c, n -> c.add(n) }, null)
                                    else if (dotted != null) actions.add(dotted)
                                    else if (chordMode) actions.addChord(start, newLength, pitch) else actions.add(Note(start, newLength, pitch))
                                }
                                return@awaitEachGesture
                            }
                            val note: Note = found
                            down.consume()
                            val nearEnd = down.position.x > note.end / TICK.toFloat() * stepPx - stepPx * 0.35f
                            var moved = false
                            var result = note
                            while (true) {
                                val event = awaitPointerEvent()
                                // A second finger: a pinch (É8), the note stays where it was.
                                if (event.changes.count { it.pressed } > 1) { drag = null; return@awaitEachGesture }
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                                if (moved) {
                                    result = if (nearEnd) {
                                        // The end goes onto the grid chosen (« Grille »), a grid step at the shortest.
                                        val len = snap((note.end + dx / stepPx * TICK).roundToInt()) - note.start
                                        note.copy(length = len.coerceIn(minOf(grid, note.length), NoteClip.MAX_TICKS))
                                    } else {
                                        val start = snap((note.start + dx / stepPx * TICK).roundToInt())
                                        note.copy(
                                            start = start.coerceIn(0, clip.ticks - grid),
                                            pitch = harmony.snap((note.pitch - (dy / rowPx).roundToInt()).coerceIn(low, high)),
                                        )
                                    }
                                    drag = note to result
                                }
                                change.consume()
                            }
                            drag = null
                            val held = currentEvent.changes.firstOrNull()?.uptimeMillis?.minus(down.uptimeMillis) ?: 0L
                            // Phase 37 (FL Studio Mobile): a touch chooses the note, a long press deletes it.
                            when {
                                moved -> {
                                    if (result != note) actions.change(note, result)
                                    if (note in selection) select(selection - note + result)
                                }
                                held > 450 -> {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    actions.remove(note)
                                    select(selection - note)
                                }
                                else -> select(if (note in selection) selection - note else selection + note)
                            }
                        }
                    },
            ) {
                val w = size.width
                // Rows: black keys darker.
                for (p in low..high) {
                    val y = (high - p) * rowPx
                    drawRect(if (p % 12 in BLACK) colors.black else colors.bg, Offset(0f, y), Size(w, rowPx))
                    // Locked scale: notes outside it are darkened, the root row is tinted.
                    if (harmony.scale != Scale.Off) {
                        if (!harmony.inScale(p)) drawRect(colors.outside, Offset(0f, y), Size(w, rowPx))
                        else if (Math.floorMod(p - harmony.root, 12) == 0) drawRect(colors.rootRow, Offset(0f, y), Size(w, rowPx))
                    }
                    if (p % 12 == 0) drawLine(colors.beat, Offset(0f, y + rowPx), Offset(w, y + rowPx), 1f)
                }
                // Grid lines: every grid step, stronger on beats and bars.
                var t = 0
                while (t <= clip.ticks) {
                    val x = t / TICK.toFloat() * stepPx
                    val c = when {
                        t % (barSteps * TICK) == 0 -> colors.bar
                        t % (4 * TICK) == 0 -> colors.beat
                        else -> colors.line
                    }
                    drawLine(c, Offset(x, 0f), Offset(x, size.height), if (t % (4 * TICK) == 0) 2f else 1f)
                    t += grid
                }
                fun noteRect(n: Note) = Offset(n.start / TICK.toFloat() * stepPx, (high - n.pitch) * rowPx + 1f) to
                    Size((n.length / TICK.toFloat() * stepPx).coerceAtLeast(4f), rowPx - 2f)
                ghosts.filter { it.pitch in low..high }.forEach { g ->
                    val (o, sz) = noteRect(g)
                    drawRoundRect(colors.ghost, o, sz, CornerRadius(4f))
                }
                // The tutorial's notes still to write: a soft fill and a dashed outline that stand out on the grid.
                val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()))
                hints.filter { it.pitch in low..high && clip.notes.none { n -> n.pitch == it.pitch && n.start == it.start } }.forEach { h ->
                    val (o, sz) = noteRect(h)
                    drawRoundRect(tutoHint.copy(alpha = 0.22f), o, sz, CornerRadius(4f))
                    drawRoundRect(tutoHint, o, sz, CornerRadius(4f), style = Stroke(2.dp.toPx(), pathEffect = dash))
                }
                // « Je le fais »: the next one to touch, lit (a ring that beats).
                lit?.let { h ->
                    val (o, sz) = noteRect(h)
                    val grow = 3.dp.toPx() + 3.dp.toPx() * (litPulse?.value ?: 0f)
                    drawRoundRect(tutoHint.copy(alpha = 0.95f - 0.4f * (litPulse?.value ?: 0f)), Offset(o.x - grow, o.y - grow), Size(sz.width + 2 * grow, sz.height + 2 * grow),
                        CornerRadius(6.dp.toPx()), style = Stroke(3.dp.toPx()))
                }
                val dragging = drag
                val group = groupMove
                clip.notes.forEach { n ->
                    val selected = n in selection
                    val shown = when {
                        dragging != null && dragging.first == n -> dragging.second
                        group != null && selected -> n.copy(start = (n.start + group.first).coerceIn(0, clip.ticks - 1), pitch = (n.pitch + group.second).coerceIn(0, 127))
                        else -> n
                    }
                    val (o, sz) = noteRect(shown)
                    val c = if (selected) selColor else colors.note
                    drawRoundRect(c.copy(alpha = 0.35f + 0.65f * shown.velocity), o, sz, CornerRadius(4f))
                    drawRoundRect(c, o, sz, CornerRadius(4f), style = Stroke(if (selected) 3f else 1.5f))
                }
                box?.let { (a, c) ->
                    val tl = Offset(minOf(a.x, c.x), minOf(a.y, c.y))
                    val sz = Size(abs(a.x - c.x), abs(a.y - c.y))
                    drawRect(selColor.copy(alpha = 0.15f), tl, sz)
                    drawRect(selColor, tl, sz, style = Stroke(2f))
                }
            }
            // The play line on a layer of its own, read as it is drawn: the grid under it is not
            // drawn again at each step (audit 16).
            androidx.compose.foundation.layout.Spacer(
                Modifier.matchParentSize().graphicsLayer().drawBehind {
                    val step = playStep()
                    if (step >= 0) {
                        val x = (step % clip.steps) * stepPx
                        drawLine(colors.play, Offset(x, 0f), Offset(x, size.height), 2f)
                    }
                },
            )
        }
    }
}

private class GridColors(
    val bg: Color, val black: Color, val line: Color, val beat: Color, val bar: Color,
    val note: Color, val ghost: Color, val play: Color, val outside: Color, val rootRow: Color,
)

private val ARP_RATES = listOf(48 to "1/8", 24 to "1/16", 12 to "1/32")
private val STRUMS: List<Pair<Int, String>>
    get() = listOf(0 to tr("Non"), 3 to tr("Léger"), 8 to tr("Moyen"), 16 to tr("Lent"))

@Composable
private fun ChipRow(label: String, help: Help? = null, content: @Composable () -> Unit) {
    // The labels line up at 78 dp; a longer one (Dutch « Akkoordenschema ») takes what it needs, never cut.
    val title: @Composable () -> Unit = {
        Row(Modifier.widthIn(min = 78.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
            if (help != null) HelpDot(help)
        }
    }
    if (isTablet()) {
        // A tablet: the choices wrap under each other instead of running off the card's edge.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            title()
            androidx.compose.foundation.layout.FlowRow(
                Modifier.weight(1f), itemVerticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
            ) { content() }
        }
    } else {
        // The label stays put (never scrolled out of sight, never cut): only the choices scroll.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            title()
            Row(Modifier.weight(1f).fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
        }
    }
}

/** A tablet (its smaller side at least 600 dp, upright or lying down), not a phone lying down. */
@Composable
internal fun isTablet(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600

/** A row of tools: one line that scrolls on a phone, wrapped lines on a tablet ([wrap]). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ToolRow(wrap: Boolean, spacing: androidx.compose.ui.unit.Dp = 6.dp, content: @Composable () -> Unit) {
    if (wrap) {
        androidx.compose.foundation.layout.FlowRow(
            itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing), verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { content() }
    } else {
        Row(Modifier.fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing)) { content() }
    }
}

/**
 * One key of the piano roll's keyboard on the left, the height of its row: white or black like a
 * piano's, [named] with its note, and played while touched when [keyOn] is given.
 */
@Composable
private fun RollKey(
    pitch: Int,
    height: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    english: Boolean,
    named: Boolean,
    keyOn: ((Int, Float) -> Unit)?,
    keyOff: ((Int) -> Unit)?,
) {
    val black = pitch % 12 in BLACK
    var down by remember { mutableStateOf(false) }
    val white = Color(0xFFE6E6E1)
    val ebony = Color(0xFF1E2023)
    // The latest pair (chord keys toggled meanwhile), without restarting a gesture under the finger.
    val currentOn by rememberUpdatedState(keyOn)
    val currentOff by rememberUpdatedState(keyOff)
    Box(
        Modifier.size(width, height)
            .then(
                if (keyOn == null) Modifier
                else Modifier.playable(
                    com.pocketstudio.model.NoteNames.withOctave(pitch, english),
                    start = { currentOn?.invoke(pitch, 0.8f) }, stop = { currentOff?.invoke(pitch) },
                ).pointerInput(pitch) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        // The note stops with the pair that started it (a chord stops as a chord).
                        val on = currentOn ?: return@awaitEachGesture
                        val off = currentOff
                        down = true
                        on(pitch, 0.8f)
                        try {
                            // Released, or the column scrolled under the finger: the note stops.
                            waitForUpOrCancellation()
                        } finally {
                            // Also when the gesture is cancelled (tab switch, rotation): no stuck note.
                            down = false
                            off?.invoke(pitch)
                        }
                    }
                },
            )
            .testTag("roll_key_$pitch"),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val lit = Neon.Cyan
        Canvas(Modifier.matchParentSize()) {
            drawRect(if (down && !black) lit else white)
            // A thin line between white keys; a C's line a little stronger (the octave starts there).
            drawLine(Color.Black.copy(alpha = if (pitch % 12 == 0) 0.45f else 0.15f), Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
            if (black) drawRect(if (down) lit else ebony, Offset.Zero, Size(size.width * 0.6f, size.height))
        }
        if (named) {
            // The C's carry their octave (where am I?); the others just their name, a black key's on it.
            val name = if (pitch % 12 == 0) com.pocketstudio.model.NoteNames.withOctave(pitch, english)
            else com.pocketstudio.model.NoteNames.pitchClass(pitch, english, sharp = "♯")
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    black -> Color(0xFFB9BCC2)
                    pitch % 12 == 0 -> Color(0xFF1E2023)
                    else -> Color(0xFF5A5D63)
                },
                maxLines = 1, softWrap = false,
                modifier = if (black) Modifier.align(Alignment.CenterStart).padding(start = 3.dp) else Modifier.padding(end = 3.dp),
            )
        }
    }
}

/**
 * Key, scale, progressions and how chords are played (arpeggio, strum). [toSound]: takes the user
 * to the track's sound, which these settings do not change.
 */
@Composable
fun HarmonyPanel(harmony: Harmony, clip: NoteClip, actions: PianoRollActions, toSound: (() -> Unit)? = null) {
    val performance = clip.performance
    val notes = clip.notes
    val chipColors = pillColors()
    val english = com.pocketstudio.app.settings.englishNotes()
    val guesses = NoteEdits.guessScales(notes)
    if (guesses.isNotEmpty()) {
        ChipRow(tr("Deviner")) {
            guesses.forEach { g ->
                TextButton(onClick = { actions.setHarmony(g.copy(lock = harmony.lock)) }, modifier = Modifier.testTag("guess_${g.root}_${g.scale.name}")) {
                    Text("${g.keyName(english)} ${tr(g.scale.label).inSentence()}")
                }
            }
        }
    }
    ChipRow(tr("Gamme")) {
        Scale.entries.forEach { sc ->
            FilterChip(selected = harmony.scale == sc, onClick = { actions.setHarmony(harmony.copy(scale = sc)) },
                label = { Text(tr(sc.label)) }, colors = chipColors, modifier = Modifier.testTag("scale_${sc.name}"))
        }
    }
    if (harmony.scale != Scale.Off) {
        ChipRow(tr("Tonique")) {
            (0..11).forEach { i ->
                val name = com.pocketstudio.model.NoteNames.key(i, english)
                FilterChip(selected = harmony.root == i, onClick = { actions.setHarmony(harmony.copy(root = i)) }, label = { Text(name) }, colors = chipColors)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                WithHelp(Glossary.SCALE_LOCK) { Text(tr("Verrouiller la gamme"), style = MaterialTheme.typography.bodyMedium) }
                Text(tr("Le clavier, les pads d'accords et le MIDI ne jouent que les notes de la gamme : pas de fausse note."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            Switch(checked = harmony.lock, onCheckedChange = { actions.setHarmony(harmony.copy(lock = it)) }, modifier = Modifier.testTag("scale_lock"))
        }
        // A scale guides, it does not rewrite: the notes already placed stay where they are, unless asked.
        Text(tr("Changer de gamme ne déplace pas les notes déjà posées."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("scale_keeps_notes"))
        val outside = NoteEdits.outOfScale(notes, harmony)
        if (outside > 0) {
            TextButton(onClick = { actions.replaceClip(NoteEdits.fitToScale(clip, harmony), null) }, modifier = Modifier.testTag("fit_to_scale")) {
                Text(trCount(outside, "Mettre la note hors gamme dans la gamme", "Mettre les %d notes hors gamme dans la gamme"))
            }
        }
    }
    // Pick a progression, then what to make of it: chords, a bass line or an arpeggio.
    var chosen by rememberSaveable { mutableStateOf(Progression.Pop) }
    ChipRow(tr("Progression"), Glossary.PROGRESSION) {
        Progression.entries.forEach { pr ->
            // The numerals of the chords this key really gives (minor ones in lower case).
            FilterChip(selected = chosen == pr, onClick = { chosen = pr }, label = { Text(pr.labelIn(harmony)) }, colors = chipColors,
                modifier = Modifier.testTag("progression_${pr.name}"))
        }
    }
    ChipRow(tr("Créer")) {
        TextButton(onClick = { actions.applyProgression(chosen) }, modifier = Modifier.testTag("make_chords")) { Text(tr("Accords")) }
        TextButton(onClick = { actions.applyBassLine(chosen) }, modifier = Modifier.testTag("make_bass")) { Text(tr("Ligne de basse")) }
        TextButton(onClick = { actions.applyArpeggio(chosen) }, modifier = Modifier.testTag("make_arp")) { Text(tr("Arpège")) }
    }
    Text(tr("Remplace les notes de la piste choisie (Annuler les rend). Une piste par rôle : basse, accords, arpège."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    // Arpeggio and strum only play chords (notes starting together): without one they have nothing to do.
    val chords = NoteEdits.hasChords(notes)
    Text(tr("Jeu des accords"), style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary,
        modifier = Modifier.padding(top = 6.dp).testTag("chord_play_title"))
    Text(tr("Agit sur les accords (plusieurs notes en même temps)."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    ChipRow(tr("Arpège"), Glossary.ARPEGGIO) {
        ArpMode.entries.forEach { m ->
            // « Off » stays at hand: an arpeggio left on must be able to go, chord or not.
            FilterChip(selected = performance.arp == m, onClick = { actions.setPerformance(performance.copy(arp = m)) }, enabled = chords || m == ArpMode.Off,
                label = { Text(tr(m.label)) }, colors = chipColors, modifier = Modifier.testTag("arp_${m.name}"))
        }
    }
    if (performance.arp != ArpMode.Off) {
        ChipRow(tr("Vitesse")) {
            ARP_RATES.forEach { (ticks, label) ->
                FilterChip(selected = performance.arpRate == ticks, onClick = { actions.setPerformance(performance.copy(arpRate = ticks)) }, enabled = chords,
                    label = { Text(label) }, colors = chipColors)
            }
        }
    }
    ChipRow(tr("Strum")) {
        STRUMS.forEach { (ticks, label) ->
            FilterChip(selected = performance.strum == ticks, onClick = { actions.setPerformance(performance.copy(strum = ticks)) }, enabled = chords || ticks == 0,
                label = { Text(label) }, colors = chipColors, modifier = Modifier.testTag("strum_$ticks"))
        }
    }
    if (!chords) {
        // The usual mix-up: a note chosen, its sound looked for here. The sound is the track's.
        Text(
            tr("Pas d'accord dans ces notes : rien à changer ici. Le son, lui, est le même pour toute la piste et se règle plus bas."),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("chord_play_none"),
        )
        if (toSound != null) {
            TextButton(onClick = toSound, modifier = Modifier.testTag("go_to_sound")) { Text(tr("Régler le son de la piste")) }
        }
    }
}

/** At most this many notes get TalkBack actions (three each), the first in time. */
private const val ACCESSIBLE_NOTES = 40

/** For TalkBack, each note (the first [ACCESSIBLE_NOTES]): remove it, a semitone up, a semitone down. */
internal fun rollAccessibleActions(notes: List<Note>, english: Boolean, actions: PianoRollActions): List<androidx.compose.ui.semantics.CustomAccessibilityAction> =
    notes.sortedWith(compareBy({ it.start }, { it.pitch })).take(ACCESSIBLE_NOTES).flatMap { n ->
        val name = tr("%1\$s au pas %2\$d", com.pocketstudio.model.NoteNames.withOctave(n.pitch, english), n.start / NoteClip.TICKS_PER_STEP + 1)
        listOf(
            androidx.compose.ui.semantics.CustomAccessibilityAction(tr("Supprimer %s", name)) { actions.remove(n); true },
            androidx.compose.ui.semantics.CustomAccessibilityAction(tr("%s : un demi-ton plus haut", name)) {
                if (n.pitch < HIGH) actions.change(n, n.copy(pitch = n.pitch + 1)); true
            },
            androidx.compose.ui.semantics.CustomAccessibilityAction(tr("%s : un demi-ton plus bas", name)) {
                if (n.pitch > LOW) actions.change(n, n.copy(pitch = n.pitch - 1)); true
            },
        )
    }

/**
 * The tutorial's line over the grid (testers, lot « tuto 5 »): the track and the bar in sight
 * (« Basse · mesure 2/8 »), then « Encore 3 notes en pointillé » (the tutorial's notes [left] to
 * touch), « plus loin → » or « ← plus tôt » when none of them is in sight; a touch brings the next
 * one into view. The notes to write by hand all written ([copy]): « Motif posé ! » and the
 * « Copier » button right here. One line the whole step: the grid never moves under a finger.
 */
@Composable
private fun TutoNotesLeft(
    left: List<Note>, hScroll: androidx.compose.foundation.ScrollState, stepWidth: androidx.compose.ui.unit.Dp, barSteps: Int, bars: Int,
    trackName: String?, copy: (() -> Unit)?,
) {
    val stepPx = with(LocalDensity.current) { stepWidth.toPx() }
    // Read where it is drawn: a scroll recomposes this line only.
    val ahead by remember(left, stepPx) {
        androidx.compose.runtime.derivedStateOf {
            val from = hScroll.value.toFloat()
            val to = from + hScroll.viewportSize
            fun x(n: Note) = n.start / TICK.toFloat() * stepPx
            when {
                left.isEmpty() || hScroll.viewportSize == 0 || left.any { x(it) in from..to } -> null
                left.all { x(it) > to } -> true
                left.all { x(it) < from } -> false
                else -> null
            }
        }
    }
    // The bar at the middle of the screen (the one the eye is on).
    val bar by remember(stepPx, barSteps, bars) {
        androidx.compose.runtime.derivedStateOf {
            ((hScroll.value + hScroll.viewportSize / 2f) / (stepPx * barSteps)).toInt().coerceIn(0, bars - 1) + 1
        }
    }
    val where = com.pocketstudio.app.tuto.tutoRollWhere(trackName, bar, bars)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background((if (copy != null) Neon.Acid else Neon.Amber).copy(alpha = 0.12f))
            .then(if (copy == null) Modifier.clickable { com.pocketstudio.app.tuto.TutoHints.focus++ } else Modifier)
            .padding(horizontal = 10.dp).testTag("tuto_notes_left"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(where, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.testTag("tuto_roll_where"))
            Text(
                if (copy != null) com.pocketstudio.app.tuto.tutoMotifDone() else com.pocketstudio.app.tuto.tutoNotesLeft(left.size, ahead),
                style = MaterialTheme.typography.labelLarge,
                color = if (left.isEmpty() || copy != null) Neon.Acid else Neon.Amber, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.testTag("tuto_notes_left_text"),
            )
        }
        if (copy != null) {
            androidx.compose.material3.Button(
                onClick = copy, modifier = Modifier.heightIn(min = 40.dp).testTag("tuto_roll_copy"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) { Text(tr("Copier"), maxLines = 1, softWrap = false) }
        }
    }
}
