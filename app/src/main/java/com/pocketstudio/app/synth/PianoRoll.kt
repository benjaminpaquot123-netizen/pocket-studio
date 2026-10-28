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
/** Width of a sixteenth for each zoom level. */
private val ZOOMS = listOf(18.dp, 30.dp, 46.dp)
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
    var zoom by rememberSaveable { mutableIntStateOf(1) }
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
        val stepWidth = maxOf(ZOOMS[zoom], (maxWidth - keysWidth) / clip.steps)
        Column(Modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolRow(tablet) {
                FilterChip(selected = !selectMode, onClick = { selectMode = false; selection = emptySet() }, label = { Text(tr("✏ Dessiner")) }, colors = chipColors,
                    modifier = Modifier.testTag("mode_draw"))
                FilterChip(selected = selectMode, onClick = { selectMode = true }, label = { Text(tr("⬚ Sélectionner")) }, colors = chipColors,
                    modifier = Modifier.testTag("mode_select"))
                Text(tr("  Zoom"), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { zoom = (zoom - 1).coerceAtLeast(0) }, enabled = zoom > 0, modifier = Modifier.testTag("zoom_out")) { Text("−") }
                    // The level between − and +, so the gap between them means something.
                    if (tablet) Text("${zoom + 1}×", style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, modifier = Modifier.testTag("zoom_level"))
                    TextButton(onClick = { zoom = (zoom + 1).coerceAtMost(ZOOMS.lastIndex) }, enabled = zoom < ZOOMS.lastIndex, modifier = Modifier.testTag("zoom_in")) { Text("+") }
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
                    // « Remettre en rythme » from where the notes were played: « Comme joué » undoes it any time.
                    TextButton(onClick = { actions.quantize(grid, 0.5f) }, modifier = Modifier.testTag("quantize_50")) { Text(tr("En rythme 50 %")) }
                    TextButton(onClick = { actions.quantize(grid, 1f) }, modifier = Modifier.testTag("quantize_100")) { Text(com.pocketstudio.model.i18n.percent(100)) }
                    if (clip.anyMovedInTime) {
                        TextButton(onClick = actions.asPlayed, modifier = Modifier.testTag("notes_as_played")) { Text(tr("Comme joué")) }
                    }
                    FilterChip(selected = showGhosts, onClick = { showGhosts = !showGhosts }, label = { Text(tr("Basse en fantôme")) }, colors = chipColors)
                    FilterChip(selected = chordMode, onClick = { chordMode = !chordMode }, label = { Text(tr("Accords")) }, colors = chipColors,
                        modifier = Modifier.testTag("chord_mode"))
                    TextButton(onClick = actions.clear, enabled = clip.notes.isNotEmpty()) { Text(tr("Effacer")) }
                }
            }
            // « Tuto de A à Z » (testers, lot « tuto 3 »): how many dotted notes are left, and which way when none is in sight;
            // one line the whole step (the grid never moves under a finger), a touch shows the next one.
            if (hints.isNotEmpty()) TutoNotesLeft(hints.filter { h -> clip.notes.none { n -> n.pitch == h.pitch && n.start == h.start } }, hScroll, stepWidth)
            BarRuler(clip, hScroll, stepWidth, keysWidth, if (selection.isNotEmpty() || selectMode) targetBar else -1, barSteps) { bar ->
                // A bar touched: its notes chosen (Copier, Supprimer…), and the bar a paste goes on.
                targetBar = bar
                selection = NoteEdits.inBar(clip, bar, barTicks)
            }
            Box(gridModifier) {
                Grid(
                    clip, if (showGhosts) ghosts else emptyList(), stepOf, newSteps * TICK, actions, gridHeight, harmony, chordMode,
                    hScroll, stepWidth, grid, selectMode, selection, select = { selection = it }, apply = apply,
                    rowHeight = if (tablet) TABLET_ROW else ROW, keysWidth = keysWidth, allNames = tablet, keyOn = keyOn, keyOff = keyOff,
                    locked = frozen, barSteps = barSteps, hints = hints,
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
                for (b in 0 until ((clip.steps + barSteps - 1) / barSteps).coerceAtLeast(1)) {
                    val on = b == chosen
                    Box(
                        Modifier.width(stepWidth * minOf(barSteps, clip.steps - b * barSteps).coerceAtLeast(1)).height(32.dp).padding(horizontal = 1.dp)
                            .background(if (on) Neon.Acid.copy(alpha = 0.25f) else Neon.SurfaceHigh, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .clickable { pick(b) }
                            .testTag("roll_bar_$b"),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("${b + 1}", style = MaterialTheme.typography.labelLarge, color = if (on) Neon.Acid else Neon.TextSecondary,
                            modifier = Modifier.padding(start = 6.dp))
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
    // Testers (lot « tuto 3 »): the step's dotted notes gone (the next step), the view stays where the user's notes are.
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(hintTop) {
        if (hintTop != null || !placed) vScroll.scrollTo(((high - (hintTop ?: 76)) * rowPx).roundToInt())
        placed = true
    }
    val tutoHint = Neon.Amber
    // « Tuto de A à Z » (testers, lot « tuto 3 »): the dotted notes still to touch. None in sight (the line goes on
    // past the screen, a demonstration writes them) or « Je le fais » asking: the roll goes to the first, lit a moment.
    val left = remember(hints, clip.notes) {
        hints.filter { h -> h.pitch in low..high && clip.notes.none { n -> n.pitch == h.pitch && n.start == h.start } }.sortedWith(compareBy({ it.start }, { -it.pitch }))
    }
    var view by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var lit by remember { mutableStateOf<Note?>(null) }
    val focus = com.pocketstudio.app.tuto.TutoHints.focus
    LaunchedEffect(left, focus, view) {
        if (view.width <= 0 || view.height <= 0) return@LaunchedEffect
        val asked = focus != com.pocketstudio.app.tuto.TutoHints.focusSeen
        com.pocketstudio.app.tuto.TutoHints.focusSeen = focus
        val next = left.firstOrNull() ?: return@LaunchedEffect
        fun x(n: Note) = n.start / TICK.toFloat() * stepPx
        fun y(n: Note) = (high - n.pitch) * rowPx
        fun across(n: Note) = x(n) >= hScroll.value - 1 && x(n) + stepPx <= hScroll.value + view.width + 1
        fun upDown(n: Note) = y(n) >= vScroll.value - 1 && y(n) + rowPx <= vScroll.value + view.height + 1
        if (!asked && left.any { across(it) && upDown(it) }) return@LaunchedEffect
        // The last note touched seen a moment first (a demonstration goes on at once).
        if (!asked && !com.pocketstudio.app.tuto.TutoHints.demo) kotlinx.coroutines.delay(300)
        if (asked) lit = next
        if (!across(next)) hScroll.animateScrollTo((x(next) - 2 * stepPx).roundToInt().coerceIn(0, hScroll.maxValue))
        if (!upDown(next)) vScroll.animateScrollTo((y(next) - view.height / 2f + rowPx / 2).roundToInt().coerceIn(0, vScroll.maxValue))
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
    Row(Modifier.fillMaxWidth().height(height)) {
        Column(Modifier.width(keysWidth).verticalScroll(vScroll).testTag("roll_keys")) {
            for (p in high downTo low) RollKey(p, rowHeight, keysWidth, english, named = allNames || p % 12 == 0, keyOn, keyOff)
        }
        Box(Modifier.weight(1f).onSizeChanged { view = it }.verticalScroll(vScroll).horizontalScroll(hScroll)) {
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
                                    // A dotted note of the tutorial touched: written as it is (its length, its place).
                                    if (dotted != null) actions.add(dotted)
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
 * « Encore 3 notes en pointillé » (the tutorial's notes [left] to touch), « plus loin → » or
 * « ← plus tôt » when none of them is in sight; a touch brings the next one into view.
 */
@Composable
private fun TutoNotesLeft(left: List<Note>, hScroll: androidx.compose.foundation.ScrollState, stepWidth: androidx.compose.ui.unit.Dp) {
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
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(Neon.Amber.copy(alpha = 0.12f))
            .clickable { com.pocketstudio.app.tuto.TutoHints.focus++ }.padding(horizontal = 10.dp).testTag("tuto_notes_left"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(com.pocketstudio.app.tuto.tutoNotesLeft(left.size, ahead), style = MaterialTheme.typography.labelLarge,
            color = if (left.isEmpty()) Neon.Acid else Neon.Amber, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
