package com.pocketstudio.app.synth

import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.Help
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.model.ArpMode
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Performance
import com.pocketstudio.model.Progression
import com.pocketstudio.model.Scale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.pocketstudio.app.acid.noteNameFr
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteEdits
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.Neon
import kotlin.math.abs
import kotlin.math.roundToInt

data class PianoRollActions(
    val add: (Note) -> Unit = {},
    val remove: (Note) -> Unit = {},
    val change: (old: Note, new: Note) -> Unit = { _, _ -> },
    val quantize: (gridTicks: Int, strength: Float) -> Unit = { _, _ -> },
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
)

private const val LOW = 24   // C1
private const val HIGH = 96  // C7
private val ROW = 20.dp
private val TICK = NoteClip.TICKS_PER_STEP
private val NEW_LENGTHS = listOf(1 to "1/16", 2 to "1/8", 4 to "1/4", 8 to "1/2")
private val BLACK = setOf(1, 3, 6, 8, 10)
/** Width of a sixteenth for each zoom level. */
private val ZOOMS = listOf(18.dp, 30.dp, 46.dp)
/** Grid for placing and moving notes, in ticks: 1/8, 1/16, 1/16 triplet, 1/32. */
private val GRIDS = listOf(48 to "1/8", 24 to "1/16", 16 to "1/16 t", 12 to "1/32")

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
) {
    var newSteps by rememberSaveable { mutableIntStateOf(2) }
    var chordMode by rememberSaveable { mutableStateOf(false) }
    var showGhosts by rememberSaveable { mutableStateOf(true) }
    var selectMode by rememberSaveable { mutableStateOf(initialSelection.isNotEmpty()) }
    var zoom by rememberSaveable { mutableIntStateOf(1) }
    var grid by rememberSaveable { mutableIntStateOf(TICK) }
    var selection by remember { mutableStateOf(initialSelection) }
    var clipboard by remember { mutableStateOf<List<Note>>(emptyList()) }
    // Notes edited elsewhere (undo, another tool) leave the selection.
    val live = selection.filter { it in clip.notes }.toSet()
    if (live != selection) selection = live
    val apply: (NoteEdits.Result, String?) -> Unit = { r, key -> actions.replaceClip(r.clip, key); selection = r.selection }
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    val hScroll = rememberScrollState()
    val stepWidth = ZOOMS[zoom]
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = !selectMode, onClick = { selectMode = false; selection = emptySet() }, label = { Text("✏ Dessiner") }, colors = chipColors,
                modifier = Modifier.testTag("mode_draw"))
            FilterChip(selected = selectMode, onClick = { selectMode = true }, label = { Text("⬚ Sélectionner") }, colors = chipColors,
                modifier = Modifier.testTag("mode_select"))
            Text("  Zoom", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { zoom = (zoom - 1).coerceAtLeast(0) }, enabled = zoom > 0, modifier = Modifier.testTag("zoom_out")) { Text("−") }
            TextButton(onClick = { zoom = (zoom + 1).coerceAtMost(ZOOMS.lastIndex) }, enabled = zoom < ZOOMS.lastIndex, modifier = Modifier.testTag("zoom_in")) { Text("+") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Boucle", style = MaterialTheme.typography.bodySmall)
            NoteClip.LENGTHS_IN_STEPS.forEach { steps ->
                FilterChip(selected = clip.steps == steps, onClick = { actions.setSteps(steps) },
                    label = { Text("${steps / 16} mes.") }, colors = chipColors)
            }
            Text("  Note", style = MaterialTheme.typography.bodySmall)
            NEW_LENGTHS.forEach { (steps, label) ->
                FilterChip(selected = newSteps == steps, onClick = { newSteps = steps }, label = { Text(label) }, colors = chipColors)
            }
            Text("  Grille", style = MaterialTheme.typography.bodySmall)
            GRIDS.forEach { (ticks, label) ->
                FilterChip(selected = grid == ticks, onClick = { grid = ticks }, label = { Text(label) }, colors = chipColors,
                    modifier = Modifier.testTag("grid_$ticks"))
            }
        }
        if (selectMode) {
            SelectionBar(clip, selection, clipboard, playStep, grid, chipColors,
                select = { selection = it }, copy = { clipboard = NoteEdits.copy(selection) }, apply = apply)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { actions.quantize(grid, 0.5f) }, modifier = Modifier.testTag("quantize_50")) { Text("Quantifier 50 %") }
                TextButton(onClick = { actions.quantize(grid, 1f) }) { Text("100 %") }
                FilterChip(selected = showGhosts, onClick = { showGhosts = !showGhosts }, label = { Text("Basse en fantôme") }, colors = chipColors)
                FilterChip(selected = chordMode, onClick = { chordMode = !chordMode }, label = { Text("Accords") }, colors = chipColors,
                    modifier = Modifier.testTag("chord_mode"))
                TextButton(onClick = actions.clear, enabled = clip.notes.isNotEmpty()) { Text("Effacer") }
            }
        }
        Grid(
            clip, if (showGhosts) ghosts else emptyList(), playStep, newSteps * TICK, actions, gridHeight, harmony, chordMode,
            hScroll, stepWidth, grid, selectMode, selection, select = { selection = it }, apply = apply,
        )
        VelocityLane(clip, selection, hScroll, stepWidth, apply)
        Text(
            if (selectMode) "Touche des notes ou trace un cadre pour les choisir ; glisse une note choisie pour tout déplacer. Couloir du bas : la force de chaque note."
            else "Touche une case : ajoute une note. Touche une note : l'enlève. Glisse : déplace. Glisse le bout : allonge. Couloir du bas : glisse pour régler la force.",
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
    }
}

/** Tools for the selected notes (FL Studio Mobile's selection menu, as buttons). */
@Composable
private fun SelectionBar(
    clip: NoteClip,
    selection: Set<Note>,
    clipboard: List<Note>,
    playStep: Int,
    grid: Int,
    chipColors: androidx.compose.material3.SelectableChipColors,
    select: (Set<Note>) -> Unit,
    copy: () -> Unit,
    apply: (NoteEdits.Result, String?) -> Unit,
) {
    val any = selection.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (any) "${selection.size} note${if (selection.size > 1) "s" else ""}" else "Rien de choisi",
                style = MaterialTheme.typography.bodySmall, color = Neon.Cyan, modifier = Modifier.testTag("selection_count"))
            TextButton(onClick = { select(clip.notes.toSet()) }, modifier = Modifier.testTag("select_all")) { Text("Tout") }
            TextButton(onClick = { select(emptySet()) }, enabled = any) { Text("Aucune") }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, 1), "sel-pitch") }, enabled = any, modifier = Modifier.testTag("sel_up")) { Text("▲ ½ ton") }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, -1), "sel-pitch") }, enabled = any) { Text("▼ ½ ton") }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, 12), "sel-pitch") }, enabled = any) { Text("▲ octave") }
            TextButton(onClick = { apply(NoteEdits.transpose(clip, selection, -12), "sel-pitch") }, enabled = any) { Text("▼ octave") }
            TextButton(onClick = { apply(NoteEdits.shift(clip, selection, -grid), "sel-move") }, enabled = any) { Text("◀") }
            TextButton(onClick = { apply(NoteEdits.shift(clip, selection, grid), "sel-move") }, enabled = any) { Text("▶") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = { apply(NoteEdits.duplicate(clip, selection), null) }, enabled = any, modifier = Modifier.testTag("sel_duplicate")) { Text("Dupliquer") }
            TextButton(onClick = copy, enabled = any, modifier = Modifier.testTag("sel_copy")) { Text("Copier") }
            TextButton(
                onClick = {
                    // At the playhead while it plays, else just after the last note.
                    val at = if (playStep >= 0) (playStep % clip.steps) * TICK / grid * grid
                    else ((clip.notes.maxOfOrNull { it.end } ?: 0) + grid - 1) / grid * grid
                    apply(NoteEdits.paste(clip, clipboard, at.coerceIn(0, clip.ticks - 1)), null)
                },
                enabled = clipboard.isNotEmpty(), modifier = Modifier.testTag("sel_paste"),
            ) { Text("Coller") }
            TextButton(onClick = { apply(NoteEdits.quantize(clip, selection, grid), null) }, enabled = any) { Text("Quantifier") }
            TextButton(onClick = { apply(NoteEdits.legato(clip, selection), null) }, enabled = any) { Text("Legato") }
            TextButton(onClick = { apply(NoteEdits.humanize(clip, selection), null) }, enabled = any) { Text("Humaniser") }
            TextButton(onClick = { apply(NoteEdits.delete(clip, selection), null) }, enabled = any, modifier = Modifier.testTag("sel_delete")) {
                Text("Supprimer", color = if (any) Neon.Red else Neon.TextSecondary)
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
) {
    val density = LocalDensity.current
    val stepPx = with(density) { stepWidth.toPx() }
    val barColor = Neon.Cyan
    val selColor = Neon.Acid
    val lineColor = Neon.Outline
    // Latest values for the gesture without restarting it on every edit.
    val current by androidx.compose.runtime.rememberUpdatedState(clip to selection)
    Row(Modifier.fillMaxWidth().height(56.dp)) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { Text("Force", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary) }
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
    playStep: Int,
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
) {
    val vScroll = rememberScrollState()
    val density = LocalDensity.current
    val rowPx = with(density) { ROW.toPx() }
    val stepPx = with(density) { stepWidth.toPx() }
    val slop = LocalViewConfiguration.current.touchSlop
    val rows = HIGH - LOW + 1
    // Start around C4-C5.
    LaunchedEffect(Unit) { vScroll.scrollTo(((HIGH - 76) * rowPx).roundToInt()) }
    var drag by remember { mutableStateOf<Pair<Note, Note>?>(null) }
    var box by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var groupMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }  // (ticks, semitones) while dragging a selection
    val snap: (Int) -> Int = { t -> (t.toFloat() / grid).roundToInt() * grid }

    val colors = GridColors(
        bg = Neon.Surface, black = Neon.Background, line = Neon.Outline.copy(alpha = 0.35f),
        beat = Neon.Outline, bar = Neon.TextSecondary, note = Neon.Cyan, ghost = Neon.Magenta.copy(alpha = 0.28f),
        play = Neon.TextPrimary, outside = Color.Black.copy(alpha = 0.35f), rootRow = Neon.Cyan.copy(alpha = 0.10f),
    )
    val selColor = Neon.Acid
    Row(Modifier.fillMaxWidth().height(height)) {
        Column(Modifier.width(36.dp).verticalScroll(vScroll)) {
            for (p in HIGH downTo LOW) {
                Box(Modifier.size(36.dp, ROW), contentAlignment = Alignment.CenterStart) {
                    if (p % 12 == 0) Text("${noteNameFr(p)}${p / 12 - 1}", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
                }
            }
        }
        Box(Modifier.weight(1f).verticalScroll(vScroll).horizontalScroll(hScroll)) {
            Canvas(
                Modifier
                    .size(stepWidth * clip.steps, ROW * rows)
                    .testTag("piano_roll")
                    .pointerInput(clip, newLength, harmony, chordMode, selectMode, selection, grid, stepPx) {
                        fun pitchAt(y: Float) = (HIGH - (y / rowPx).toInt()).coerceIn(LOW, HIGH)
                        fun tickAt(x: Float) = (x / stepPx * TICK).toInt().coerceIn(0, clip.ticks - 1)
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val found = clip.noteAt(tickAt(down.position.x), pitchAt(down.position.y))
                            if (selectMode) {
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
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val dx = change.position.x - down.position.x
                                    val dy = change.position.y - down.position.y
                                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                                    if (moved) {
                                        delta = snap((dx / stepPx * TICK).roundToInt()) to -(dy / rowPx).roundToInt()
                                        groupMove = delta
                                    }
                                    change.consume()
                                }
                                groupMove = null
                                if (!moved) {
                                    select(if (found in selection) selection - found else selection + found)
                                } else {
                                    val moving = if (found in selection) selection else setOf(found)
                                    val shifted = NoteEdits.shift(clip, moving, delta.first)
                                    apply(NoteEdits.transpose(shifted.clip, shifted.selection, delta.second), null)
                                }
                                return@awaitEachGesture
                            }
                            if (found == null) {
                                val up = waitForUpOrCancellation() ?: return@awaitEachGesture  // a scroll took it
                                if ((up.position - down.position).getDistance() < slop) {
                                    val start = tickAt(down.position.x) / grid * grid
                                    val pitch = harmony.snap(pitchAt(down.position.y))
                                    if (chordMode) actions.addChord(start, newLength, pitch) else actions.add(Note(start, newLength, pitch))
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
                                        val len = ((note.length + dx / stepPx * TICK) / 6f).roundToInt() * 6
                                        note.copy(length = len.coerceIn(6, NoteClip.MAX_TICKS))
                                    } else {
                                        val start = snap((note.start + dx / stepPx * TICK).roundToInt())
                                        note.copy(
                                            start = start.coerceIn(0, clip.ticks - grid),
                                            pitch = harmony.snap((note.pitch - (dy / rowPx).roundToInt()).coerceIn(LOW, HIGH)),
                                        )
                                    }
                                    drag = note to result
                                }
                                change.consume()
                            }
                            drag = null
                            val held = currentEvent.changes.firstOrNull()?.uptimeMillis?.minus(down.uptimeMillis) ?: 0L
                            when {
                                moved -> if (result != note) actions.change(note, result)
                                held > 450 -> actions.change(note, note.copy(velocity = nextVelocity(note.velocity)))
                                else -> actions.remove(note)
                            }
                        }
                    },
            ) {
                val w = size.width
                // Rows: black keys darker.
                for (p in LOW..HIGH) {
                    val y = (HIGH - p) * rowPx
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
                        t % (16 * TICK) == 0 -> colors.bar
                        t % (4 * TICK) == 0 -> colors.beat
                        else -> colors.line
                    }
                    drawLine(c, Offset(x, 0f), Offset(x, size.height), if (t % (4 * TICK) == 0) 2f else 1f)
                    t += grid
                }
                fun noteRect(n: Note) = Offset(n.start / TICK.toFloat() * stepPx, (HIGH - n.pitch) * rowPx + 1f) to
                    Size((n.length / TICK.toFloat() * stepPx).coerceAtLeast(4f), rowPx - 2f)
                ghosts.filter { it.pitch in LOW..HIGH }.forEach { g ->
                    val (o, sz) = noteRect(g)
                    drawRoundRect(colors.ghost, o, sz, CornerRadius(4f))
                }
                val dragging = drag
                val group = groupMove
                clip.notes.forEach { n ->
                    val selected = n in selection
                    val shown = when {
                        dragging != null && dragging.first == n -> dragging.second
                        group != null && selected -> n.copy(start = (n.start + group.first).coerceIn(0, clip.ticks - 1), pitch = n.pitch + group.second)
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
                if (playStep >= 0) {
                    val x = (playStep % clip.steps) * stepPx
                    drawLine(colors.play, Offset(x, 0f), Offset(x, size.height), 2f)
                }
            }
        }
    }
}

private class GridColors(
    val bg: Color, val black: Color, val line: Color, val beat: Color, val bar: Color,
    val note: Color, val ghost: Color, val play: Color, val outside: Color, val rootRow: Color,
)

private val ROOTS = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
private val ARP_RATES = listOf(48 to "1/8", 24 to "1/16", 12 to "1/32")
private val STRUMS = listOf(0 to "Non", 3 to "Léger", 8 to "Moyen", 16 to "Lent")

@Composable
private fun ChipRow(label: String, help: Help? = null, content: @Composable () -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.width(78.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f, fill = false))
            if (help != null) HelpDot(help)
        }
        content()
    }
}

/** Key, scale, progressions and how chords are played (arpeggio, strum). */
@Composable
fun HarmonyPanel(harmony: Harmony, performance: Performance, actions: PianoRollActions, notes: List<Note> = emptyList()) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    val guesses = NoteEdits.guessScales(notes)
    if (guesses.isNotEmpty()) {
        ChipRow("Deviner") {
            guesses.forEach { g ->
                TextButton(onClick = { actions.setHarmony(g.copy(lock = harmony.lock)) }, modifier = Modifier.testTag("guess_${g.root}_${g.scale.name}")) {
                    Text("${ROOTS[g.root]} ${g.scale.label.lowercase()}")
                }
            }
        }
    }
    ChipRow("Gamme") {
        Scale.entries.forEach { sc ->
            FilterChip(selected = harmony.scale == sc, onClick = { actions.setHarmony(harmony.copy(scale = sc)) },
                label = { Text(sc.label) }, colors = chipColors, modifier = Modifier.testTag("scale_${sc.name}"))
        }
    }
    if (harmony.scale != Scale.Off) {
        ChipRow("Tonique") {
            ROOTS.forEachIndexed { i, name ->
                FilterChip(selected = harmony.root == i, onClick = { actions.setHarmony(harmony.copy(root = i)) }, label = { Text(name) }, colors = chipColors)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                WithHelp(Glossary.SCALE_LOCK) { Text("Verrouiller la gamme", style = MaterialTheme.typography.bodyMedium) }
                Text("Le clavier, les pads d'accords et le MIDI ne jouent que les notes de la gamme : pas de fausse note.",
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            Switch(checked = harmony.lock, onCheckedChange = { actions.setHarmony(harmony.copy(lock = it)) }, modifier = Modifier.testTag("scale_lock"))
        }
    }
    // Pick a progression, then what to make of it: chords, a bass line or an arpeggio.
    var chosen by rememberSaveable { mutableStateOf(Progression.Pop) }
    ChipRow("Progression", Glossary.PROGRESSION) {
        Progression.entries.forEach { pr ->
            FilterChip(selected = chosen == pr, onClick = { chosen = pr }, label = { Text(pr.label) }, colors = chipColors,
                modifier = Modifier.testTag("progression_${pr.name}"))
        }
    }
    ChipRow("Créer") {
        TextButton(onClick = { actions.applyProgression(chosen) }, modifier = Modifier.testTag("make_chords")) { Text("Accords") }
        TextButton(onClick = { actions.applyBassLine(chosen) }, modifier = Modifier.testTag("make_bass")) { Text("Ligne de basse") }
        TextButton(onClick = { actions.applyArpeggio(chosen) }, modifier = Modifier.testTag("make_arp")) { Text("Arpège") }
    }
    Text("Remplace les notes de la piste choisie (Annuler les rend). Une piste par rôle : basse, accords, arpège.",
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    ChipRow("Arpège", Glossary.ARPEGGIO) {
        ArpMode.entries.forEach { m ->
            FilterChip(selected = performance.arp == m, onClick = { actions.setPerformance(performance.copy(arp = m)) },
                label = { Text(m.label) }, colors = chipColors, modifier = Modifier.testTag("arp_${m.name}"))
        }
    }
    if (performance.arp != ArpMode.Off) {
        ChipRow("Vitesse") {
            ARP_RATES.forEach { (ticks, label) ->
                FilterChip(selected = performance.arpRate == ticks, onClick = { actions.setPerformance(performance.copy(arpRate = ticks)) },
                    label = { Text(label) }, colors = chipColors)
            }
        }
    }
    ChipRow("Strum") {
        STRUMS.forEach { (ticks, label) ->
            FilterChip(selected = performance.strum == ticks, onClick = { actions.setPerformance(performance.copy(strum = ticks)) },
                label = { Text(label) }, colors = chipColors)
        }
    }
    Text(
        "La progression remplace les notes (Annuler possible). Arpège et strum jouent les accords posés ensemble.",
        style = MaterialTheme.typography.bodySmall,
        color = Neon.TextSecondary,
    )
}
