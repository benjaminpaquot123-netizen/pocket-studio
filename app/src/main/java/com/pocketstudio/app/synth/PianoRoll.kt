package com.pocketstudio.app.synth

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
    val setPerformance: (Performance) -> Unit = {},
)

private const val LOW = 24   // C1
private const val HIGH = 96  // C7
private val ROW = 20.dp
private val STEP = 30.dp
private val TICK = NoteClip.TICKS_PER_STEP
private val NEW_LENGTHS = listOf(1 to "1/16", 2 to "1/8", 4 to "1/4", 8 to "1/2")
private val BLACK = setOf(1, 3, 6, 8, 10)

/** Velocity steps a long press cycles through (same as the drum grid). */
fun nextVelocity(v: Float) = when {
    v > 0.75f -> 0.5f
    v > 0.35f -> 0.2f
    else -> 1f
}

/**
 * Piano roll for the synth: tap an empty cell to add a note, tap a note to delete it, drag it
 * to move, drag its right end to lengthen, long press to cycle its strength. Ghost notes show
 * the acid bass line, faint, behind.
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
) {
    var newSteps by rememberSaveable { mutableIntStateOf(2) }
    var chordMode by rememberSaveable { mutableStateOf(false) }
    var showGhosts by rememberSaveable { mutableStateOf(true) }
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = { actions.quantize(TICK, 0.5f) }, modifier = Modifier.testTag("quantize_50")) { Text("Quantifier 50 %") }
            TextButton(onClick = { actions.quantize(TICK, 1f) }) { Text("100 %") }
            FilterChip(selected = showGhosts, onClick = { showGhosts = !showGhosts }, label = { Text("Basse en fantôme") }, colors = chipColors)
            FilterChip(selected = chordMode, onClick = { chordMode = !chordMode }, label = { Text("Accords") }, colors = chipColors,
                modifier = Modifier.testTag("chord_mode"))
            TextButton(onClick = actions.clear, enabled = clip.notes.isNotEmpty()) { Text("Effacer") }
        }
        Grid(clip, if (showGhosts) ghosts else emptyList(), playStep, newSteps * TICK, actions, gridHeight, harmony, chordMode)
        Text(
            "Touche une case : ajoute une note. Touche une note : l'enlève. Glisse : déplace. Glisse le bout : allonge. Appui long : plus doux.",
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
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
) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val density = LocalDensity.current
    val rowPx = with(density) { ROW.toPx() }
    val stepPx = with(density) { STEP.toPx() }
    val slop = LocalViewConfiguration.current.touchSlop
    val rows = HIGH - LOW + 1
    // Start around C4-C5.
    LaunchedEffect(Unit) { vScroll.scrollTo(((HIGH - 76) * rowPx).roundToInt()) }
    var drag by remember { mutableStateOf<Pair<Note, Note>?>(null) }

    val colors = GridColors(
        bg = Neon.Surface, black = Neon.Background, line = Neon.Outline.copy(alpha = 0.35f),
        beat = Neon.Outline, bar = Neon.TextSecondary, note = Neon.Cyan, ghost = Neon.Magenta.copy(alpha = 0.28f),
        play = Neon.TextPrimary, outside = Color.Black.copy(alpha = 0.35f), rootRow = Neon.Cyan.copy(alpha = 0.10f),
    )
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
                    .size(STEP * clip.steps, ROW * rows)
                    .testTag("piano_roll")
                    .pointerInput(clip, newLength, harmony, chordMode) {
                        fun pitchAt(y: Float) = (HIGH - (y / rowPx).toInt()).coerceIn(LOW, HIGH)
                        fun tickAt(x: Float) = (x / stepPx * TICK).toInt().coerceIn(0, clip.ticks - 1)
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val found = clip.noteAt(tickAt(down.position.x), pitchAt(down.position.y))
                            if (found == null) {
                                val up = waitForUpOrCancellation() ?: return@awaitEachGesture  // a scroll took it
                                if ((up.position - down.position).getDistance() < slop) {
                                    val start = tickAt(down.position.x) / TICK * TICK
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
                                        val start = ((note.start + dx / stepPx * TICK) / TICK).roundToInt() * TICK
                                        note.copy(
                                            start = start.coerceIn(0, clip.ticks - TICK),
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
                for (s in 0..clip.steps) {
                    val x = s * stepPx
                    val c = when {
                        s % 16 == 0 -> colors.bar
                        s % 4 == 0 -> colors.beat
                        else -> colors.line
                    }
                    drawLine(c, Offset(x, 0f), Offset(x, size.height), if (s % 4 == 0) 2f else 1f)
                }
                fun noteRect(n: Note) = Offset(n.start / TICK.toFloat() * stepPx, (HIGH - n.pitch) * rowPx + 1f) to
                    Size((n.length / TICK.toFloat() * stepPx).coerceAtLeast(4f), rowPx - 2f)
                ghosts.filter { it.pitch in LOW..HIGH }.forEach { g ->
                    val (o, sz) = noteRect(g)
                    drawRoundRect(colors.ghost, o, sz, CornerRadius(4f))
                }
                val dragging = drag
                clip.notes.forEach { n ->
                    val shown = if (dragging != null && dragging.first == n) dragging.second else n
                    val (o, sz) = noteRect(shown)
                    drawRoundRect(colors.note.copy(alpha = 0.35f + 0.65f * shown.velocity), o, sz, CornerRadius(4f))
                    drawRoundRect(colors.note, o, sz, CornerRadius(4f), style = Stroke(1.5f))
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
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(78.dp))
        content()
    }
}

/** Key, scale, progressions and how chords are played (arpeggio, strum). */
@Composable
fun HarmonyPanel(harmony: Harmony, performance: Performance, actions: PianoRollActions) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
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
    }
    ChipRow("Progression") {
        Progression.entries.forEach { pr ->
            TextButton(onClick = { actions.applyProgression(pr) }) { Text(pr.label) }
        }
    }
    ChipRow("Arpège") {
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
