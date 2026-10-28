@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.remix

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.separation.Demucs
import com.pocketstudio.ui.LeftToRight
import com.pocketstudio.ui.LocalFolding
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.pillColors
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min

/**
 * What the stem board shows: the separated song [setId] (« [name] »), [frames] long at
 * [BOARD_RATE], its tempo ([bpm] 0: unknown, the ruler then counts seconds) and first beat
 * ([firstBeat], seconds), and where its peaks come from.
 */
class StemBoardUi(
    val setId: String,
    val name: String,
    val frames: Long,
    val bpm: Double,
    val firstBeat: Double,
    val peaks: StemPeaks,
    /** The stretch being listened to (null: silence): its « Écouter » becomes « Arrêter ». */
    val playing: StemSelection? = null,
    /**
     * The song frame being heard, -1 when none. Read while drawing: give the value of a Compose
     * state, so that the play head moves without recomposing the board.
     */
    val position: () -> Long = { -1L },
    /** What the last action did, or why not (« Voix → pad 9 »). */
    val notice: String? = null,
    /** Where the peaks are computed (never the main thread in the app; tests may compute them inline). */
    val peaksContext: CoroutineContext = Dispatchers.Default,
)

/** The actions of a board nothing is plugged into yet: every button does nothing. */
object NoStemBoardActions : StemBoardActions {
    override fun play(sel: StemSelection) = Unit
    override fun stop() = Unit
    override fun send(sel: StemSelection, dest: StemDestination) = Unit
    override fun chop(setId: String, stem: Int, max: Int) = Unit
}

/** Index of the voice among the stems, which « Découper la voix en phrases » cuts. */
val VOCALS_STEM: Int = Demucs.STEMS.indexOf("vocals")

/** How many pads « Découper la voix en phrases » fills at most. */
const val CHOP_MAX = 8

@Composable
fun rememberStemBoardState(ui: StemBoardUi): StemBoardState = remember(ui.setId, ui.frames) { StemBoardState(ui.frames) }

/**
 * « Tableau des pistes »: the four stems one under the other, each with its big curve, a ruler of
 * bars and beats on top. Pinch to zoom, long press then slide (or slide the ruler) to scroll; slide
 * a finger along a curve to choose a stretch, whose two handles snap to the beats (« Aimant »);
 * then listen to it, give it fades and send it to a pad, an instrument, a track or a live pad.
 */
@Composable
fun StemBoard(
    ui: StemBoardUi,
    actions: StemBoardActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: StemBoardState = rememberStemBoardState(ui),
) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val levels = remember(ui.setId, ui.frames, ui.peaks) { PeakLevels(ui.frames, Demucs.STEMS.size) }
    LaunchedEffect(levels) { levels.load(ui.peaks, ui.peaksContext) }
    val grid = remember(ui.bpm, ui.firstBeat) { BeatGrid.of(ui.bpm, ui.firstBeat, BOARD_RATE) }
    val playing = rememberUpdatedState(ui.playing)
    val position = rememberUpdatedState(ui.position)
    // Cards stay open here: the board's panels have no title to fold.
    CompositionLocalProvider(LocalFolding provides false) {
        BoxWithConstraints(modifier.fillMaxSize().padding(12.dp).testTag("board")) {
            val board: @Composable (Modifier) -> Unit = { m ->
                NeonCard(m) {
                    LeftToRight {
                        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BoardRuler(state, grid, playing, position, Modifier.fillMaxWidth().height(28.dp))
                            Demucs.STEMS.forEachIndexed { i, stem ->
                                BoardTrack(
                                    stem = i,
                                    label = StemStore.label(stem),
                                    color = stemColor(stem),
                                    state = state,
                                    levels = levels,
                                    grid = grid,
                                    playing = playing,
                                    position = position,
                                    modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (maxWidth > maxHeight && maxWidth >= 600.dp) {
                // Lying down: the curves keep the whole height, the actions go on the side.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoardHeader(ui, state, grid, onClose)
                        board(Modifier.weight(1f))
                    }
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        BoardActionsBar(ui, state, actions)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoardHeader(ui, state, grid, onClose)
                    board(Modifier.weight(1f))
                    BoardActionsBar(ui, state, actions)
                }
            }
        }
    }
}

/** Each stem in its family's colour, as the sounds of that family are everywhere else. */
private fun stemColor(stem: String): Color = when (stem) {
    "drums" -> SoundLooks.of("batterie").color
    "bass" -> SoundLooks.of("basse").color
    "vocals" -> SoundLooks.of("voix").color
    else -> SoundLooks.of("claviers").color
}

@Composable
private fun BoardHeader(ui: StemBoardUi, state: StemBoardState, grid: BeatGrid?, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onClose, modifier = Modifier.testTag("board_close")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
        }
        Column(Modifier.weight(1f)) {
            Text(tr("Tableau des pistes"), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(ui.name, ui.bpm.takeIf { it > 0 }?.let { "%.0f BPM".format(it) }).joinToString(" · ")
            Text(details, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = state::fit, enabled = state.zoomedIn, modifier = Modifier.testTag("board_fit")) { Text(tr("Tout voir")) }
        FilterChip(
            selected = state.magnet && grid != null,
            enabled = grid != null,
            onClick = { state.magnet = !state.magnet },
            label = { Text(tr("Aimant")) },
            colors = pillColors(),
            modifier = Modifier.testTag("board_magnet"),
        )
    }
}

/** Bar numbers and beat ticks (seconds when the tempo is unknown); sliding it scrolls, pinching it zooms. */
@Composable
private fun BoardRuler(
    state: StemBoardState,
    grid: BeatGrid?,
    playing: State<StemSelection?>,
    position: State<() -> Long>,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MonoStyle.copy(color = Neon.TextSecondary, fontSize = MaterialTheme.typography.labelSmall.fontSize)
    val tick = Neon.Outline
    val accent = Neon.Cyan
    val play = Neon.Acid
    val haptic = LocalHapticFeedback.current
    Spacer(
        modifier
            .testTag("board_ruler")
            .onSizeChanged { state.layout(it.width.toFloat()) }
            .boardGestures(state, stem = -1, snap = { null }, onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
            .drawWithCache {
                // One measured label per bar (or second), made the first time it shows: none per frame.
                val count = if (grid != null) (state.frames / grid.bar).toInt() + 2 else (state.frames / BOARD_RATE).toInt() + 2
                val labels = arrayOfNulls<TextLayoutResult>(count.coerceIn(1, 100_000))
                fun label(i: Int, seconds: Boolean): TextLayoutResult? {
                    if (i !in labels.indices) return null
                    return labels[i] ?: measurer.measure(if (seconds) "%d:%02d".format(i / 60, i % 60) else i.toString(), labelStyle).also { labels[i] = it }
                }
                val minLabelPx = 36.dp.toPx()
                val minTickPx = 6.dp.toPx()
                val gap = 3.dp.toPx()
                onDrawBehind {
                    val fpp = state.framesPerPx
                    if (fpp <= 0.0) return@onDrawBehind
                    val h = size.height
                    if (grid != null) {
                        val barPx = (grid.bar / fpp).toFloat()
                        val beatPx = (grid.beat / fpp).toFloat()
                        val labelEvery = stepFor(barPx, minLabelPx)
                        if (beatPx >= minTickPx) {
                            var i = grid.firstBeatFrom(state.start)
                            while (true) {
                                val x = state.x(grid.beatFrame(i))
                                if (x > size.width) break
                                val bar = grid.barOpenedBy(i)
                                drawLine(tick, Offset(x, if (bar > 0) h * 0.35f else h * 0.7f), Offset(x, h), strokeWidth = 1.dp.toPx())
                                i++
                            }
                        }
                        val tickEvery = stepFor(barPx, minTickPx)
                        var b = Math.floorDiv(grid.firstBeatFrom(state.start), BeatGrid.BEATS_PER_BAR.toLong())
                        b = Math.floorDiv(b, tickEvery.toLong()) * tickEvery
                        while (true) {
                            val x = state.x(grid.beatFrame(b * BeatGrid.BEATS_PER_BAR))
                            if (x > size.width) break
                            if (x >= -1f && b >= 0) {
                                drawLine(tick, Offset(x, h * 0.35f), Offset(x, h), strokeWidth = 1.dp.toPx())
                                if (b % labelEvery == 0L) {
                                    label((b + 1).toInt(), seconds = false)?.let { drawText(it, topLeft = Offset(x + gap, 0f)) }
                                }
                            }
                            b += tickEvery
                        }
                    } else {
                        val secPx = (BOARD_RATE / fpp).toFloat()
                        val step = SECOND_STEPS.firstOrNull { it * secPx >= minLabelPx } ?: SECOND_STEPS.last()
                        var s = (state.start / BOARD_RATE).toLong() / step * step
                        while (true) {
                            val x = state.x(s.toDouble() * BOARD_RATE)
                            if (x > size.width) break
                            if (x >= -1f) {
                                drawLine(tick, Offset(x, h * 0.35f), Offset(x, h), strokeWidth = 1.dp.toPx())
                                label(s.toInt(), seconds = true)?.let { drawText(it, topLeft = Offset(x + gap, 0f)) }
                            }
                            s += step
                        }
                    }
                    if (state.hasSelection) {
                        val x0 = state.x(state.selStart).coerceAtLeast(0f)
                        val x1 = state.x(state.selEnd).coerceAtMost(size.width)
                        if (x1 > x0) drawRect(accent.copy(alpha = 0.55f), Offset(x0, h - 4.dp.toPx()), Size(x1 - x0, 4.dp.toPx()))
                    }
                    drawPlayHead(playing.value, position.value(), state, play)
                }
            },
    )
}

private val SECOND_STEPS = intArrayOf(1, 2, 5, 10, 15, 30, 60, 120, 300)

/** The smallest power of 2 of [unitPx] that is at least [minPx] wide. */
private fun stepFor(unitPx: Float, minPx: Float): Int {
    var k = 1
    while (k < (1 shl 20) && k * unitPx < minPx) k *= 2
    return k
}

/** The green line of what plays, with a soft glow, while a stretch is listened to. */
private fun DrawScope.drawPlayHead(playing: StemSelection?, frame: Long, state: StemBoardState, color: Color) {
    if (playing == null || frame < 0) return
    val x = state.x(frame)
    if (x < 0f || x > size.width) return
    drawLine(color.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 6.dp.toPx())
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
}

/** A selection edge at [x]: a lit line and its handle, a pill with a soft glow. */
private fun DrawScope.drawHandle(x: Float, accent: Color, edgeWidth: Float, knobW: Float, knobH: Float) {
    if (x < -knobW || x > size.width + knobW) return
    val mid = size.height / 2
    drawLine(accent, Offset(x, 0f), Offset(x, size.height), strokeWidth = edgeWidth)
    drawCircle(accent.copy(alpha = 0.22f), radius = knobH * 0.62f, center = Offset(x, mid))
    drawRoundRect(accent, Offset(x - knobW / 2, mid - knobH / 2), Size(knobW, knobH), CornerRadius(knobW / 2))
    drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(x - 1f, mid - knobH * 0.3f), Size(2f, knobH * 0.6f), CornerRadius(1f))
}

/** One stem: its peaks as mountains in its colour, the beats behind, the selection and its handles on top. */
@Composable
private fun BoardTrack(
    stem: Int,
    label: String,
    color: Color,
    state: StemBoardState,
    levels: PeakLevels,
    grid: BeatGrid?,
    playing: State<StemSelection?>,
    position: State<() -> Long>,
    modifier: Modifier,
) {
    val lane = Neon.SurfaceHigh.copy(alpha = 0.55f)
    val line = Neon.Outline
    val accent = Neon.Cyan
    val play = Neon.Acid
    val haptic = LocalHapticFeedback.current
    Box(modifier.semantics { contentDescription = label }) {
        Spacer(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .testTag("board_track_$stem")
                .boardGestures(
                    state, stem,
                    snap = { grid.takeIf { state.magnet } },
                    onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                )
                .drawWithCache {
                    // Everything a frame needs is made here, once per size: drawing allocates nothing.
                    val column = max(1f, density)
                    val columns = (size.width / column).toInt() + 2
                    val heights = FloatArray(columns)
                    val path = Path()
                    val mountains = Brush.verticalGradient(0f to color, 0.5f to color.copy(alpha = 0.28f), 1f to color)
                    val veil = Brush.verticalGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.16f)))
                    val corner = CornerRadius(10.dp.toPx())
                    val margin = 6.dp.toPx()
                    val minBeatPx = 8.dp.toPx()
                    val minBarPx = 6.dp.toPx()
                    val edgeWidth = 2.dp.toPx()
                    val knobW = 10.dp.toPx()
                    val knobH = min(size.height * 0.5f, 34.dp.toPx())
                    val border = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                    onDrawBehind {
                        drawRect(lane)
                        val fpp = state.framesPerPx
                        if (fpp <= 0.0) return@onDrawBehind
                        val mid = size.height / 2
                        // The beats behind (the bars brighter); only every few bars when zoomed out.
                        if (grid != null) {
                            val beatPx = (grid.beat / fpp).toFloat()
                            if (beatPx >= minBeatPx) {
                                var i = grid.firstBeatFrom(state.start)
                                while (true) {
                                    val x = state.x(grid.beatFrame(i))
                                    if (x > size.width) break
                                    val strong = grid.barOpenedBy(i) > 0
                                    drawLine(line.copy(alpha = if (strong) 0.45f else 0.16f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                                    i++
                                }
                            } else {
                                val every = stepFor(beatPx * BeatGrid.BEATS_PER_BAR, minBarPx)
                                var b = Math.floorDiv(grid.firstBeatFrom(state.start), BeatGrid.BEATS_PER_BAR.toLong())
                                b = Math.floorDiv(b, every.toLong()) * every
                                while (true) {
                                    val x = state.x(grid.beatFrame(b * BeatGrid.BEATS_PER_BAR))
                                    if (x > size.width) break
                                    if (x >= 0f) drawLine(line.copy(alpha = 0.30f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                                    b += every
                                }
                            }
                        }
                        // The selection's veil under the curve.
                        val selected = state.hasSelection && state.selStem == stem
                        val x0 = if (selected) state.x(state.selStart) else 0f
                        val x1 = if (selected) state.x(state.selEnd) else 0f
                        if (selected && x1 > 0f && x0 < size.width) {
                            drawRect(veil, Offset(max(0f, x0), 0f), Size(min(size.width, x1) - max(0f, x0), size.height))
                        }
                        // The mountains: the top edge from left to right, the bottom one back.
                        if (levels.ready > stem) {
                            val amp = mid - margin
                            val level = levels.levelFor(fpp * column)
                            path.rewind()
                            path.moveTo(0f, mid)
                            var c = 0
                            while (c < columns) {
                                val x = c * column
                                val from = (state.start + x * fpp).toLong()
                                val to = (state.start + (x + column) * fpp).toLong()
                                val h = max(0.5f, levels.columnPeak(stem, level, from, to) * amp)
                                heights[c] = h
                                path.lineTo(x, mid - h)
                                c++
                            }
                            c = columns - 1
                            while (c >= 0) {
                                path.lineTo(c * column, mid + heights[c])
                                c--
                            }
                            path.close()
                            drawPath(path, mountains)
                        } else {
                            drawLine(color.copy(alpha = 0.4f), Offset(0f, mid), Offset(size.width, mid), strokeWidth = 1f)
                        }
                        // The edges and their two handles, lit.
                        if (selected) {
                            drawHandle(x0, accent, edgeWidth, knobW, knobH)
                            drawHandle(x1, accent, edgeWidth, knobW, knobH)
                        }
                        drawPlayHead(playing.value, position.value(), state, play)
                        drawRoundRect(line.copy(alpha = 0.35f), cornerRadius = corner, style = border)
                    }
                },
        )
        Row(
            Modifier.padding(start = 8.dp, top = 6.dp)
                .background(Neon.Surface.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary)
        }
        if (levels.ready <= stem) {
            Text(
                tr("Chargement des courbes…"),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** What the finger is doing, decided from its first moves. */
private enum class Gesture { Tap, Slide, TwoFingers, LongPress }

/**
 * The board's gestures. On a curve ([stem] >= 0): sliding chooses a stretch (or moves the handle
 * under the finger), a touch clears it, a long press then a slide scrolls. On the ruler ([stem]
 * -1): sliding scrolls. Two fingers anywhere: pinch to zoom, move to scroll.
 */
private fun Modifier.boardGestures(state: StemBoardState, stem: Int, snap: () -> BeatGrid?, onLongPress: () -> Unit): Modifier =
    pointerInput(state, stem) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            val edge = if (stem >= 0) state.edgeAt(stem, down.position.x, 24.dp.toPx()) else 0
            var lastX = down.position.x
            val gesture = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                var g: Gesture? = null
                while (g == null) {
                    val e = awaitPointerEvent()
                    val c = e.changes.firstOrNull { it.id == down.id }
                    if (c != null) lastX = c.position.x
                    g = when {
                        e.changes.count { it.pressed } > 1 -> Gesture.TwoFingers
                        c == null || !c.pressed -> Gesture.Tap
                        (c.position - down.position).getDistance() > slop -> Gesture.Slide
                        else -> null
                    }
                }
                g
            } ?: Gesture.LongPress
            when {
                gesture == Gesture.TwoFingers -> transform(state)
                gesture == Gesture.Tap -> if (stem >= 0 && edge == 0) state.clearSelection()
                stem < 0 || (gesture == Gesture.LongPress && edge == 0) -> {
                    if (gesture == Gesture.LongPress) onLongPress()
                    pan(state, down.id)
                }
                edge != 0 -> {
                    val from = state.selStart
                    val to = state.selEnd
                    follow(state, down.id, lastX) { x ->
                        val r = BoardSelect.moveEdge(from, to, edge, state.frameAt(x), snap(), state.frames)
                        state.select(stem, r.first, r.last + 1)
                    }
                }
                else -> {
                    val anchor = state.frameAt(down.position.x)
                    val before = Triple(state.selStem, state.selStart, state.selEnd)
                    val done = follow(state, down.id, lastX) { x ->
                        val r = BoardSelect.drag(anchor, state.frameAt(x), snap(), state.frames)
                        state.select(stem, r.first, r.last + 1)
                    }
                    // A second finger came: it was a pinch, not a selection.
                    if (!done) {
                        if (before.first >= 0) state.select(before.first, before.second, before.third) else state.clearSelection()
                    }
                }
            }
        }
    }

/** Calls [move] with each position of pointer [id] (first with [x]) until it lifts (true) or a second finger comes (false, then pinches). */
private suspend fun AwaitPointerEventScope.follow(state: StemBoardState, id: PointerId, x: Float, move: (Float) -> Unit): Boolean {
    move(x)
    while (true) {
        val e = awaitPointerEvent()
        if (e.changes.count { it.pressed } > 1) {
            transform(state)
            return false
        }
        val c = e.changes.firstOrNull { it.id == id } ?: return true
        if (!c.pressed) return true
        if (c.positionChanged()) move(c.position.x)
        c.consume()
    }
}

/** One finger scrolls the song until it lifts. */
private suspend fun AwaitPointerEventScope.pan(state: StemBoardState, id: PointerId) {
    while (true) {
        val e = awaitPointerEvent()
        if (e.changes.count { it.pressed } > 1) return transform(state)
        val c = e.changes.firstOrNull { it.id == id } ?: return
        if (!c.pressed) return
        val dx = c.position.x - c.previousPosition.x
        if (dx != 0f) state.scrollBy(dx)
        c.consume()
    }
}

/** Two fingers: pinch zooms around them, moving them scrolls; until every finger lifts. */
private suspend fun AwaitPointerEventScope.transform(state: StemBoardState) {
    while (true) {
        val e = awaitPointerEvent()
        if (e.changes.none { it.pressed }) return
        val zoom = e.calculateZoom()
        val pan = e.calculatePan()
        val centroid = e.calculateCentroid(useCurrent = true)
        if (zoom != 1f && centroid.isSpecified) state.zoomBy(zoom, centroid.x)
        if (pan.x != 0f) state.scrollBy(pan.x)
        e.changes.forEach { if (it.positionChanged()) it.consume() }
    }
}

/** Under the board: what to do with the chosen stretch, or how to choose one. */
@Composable
private fun BoardActionsBar(ui: StemBoardUi, state: StemBoardState, actions: StemBoardActions) {
    val sel = state.selection(ui.setId)
    NeonCard(accent = if (sel != null) Neon.Cyan else Neon.Outline, modifier = Modifier.testTag("board_actions")) {
        if (sel == null) {
            Text(
                tr("Glisse le doigt sur une courbe, de gauche à droite, pour choisir un morceau. Deux doigts pour zoomer ; appui long puis glisse pour défiler."),
                style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
            )
        } else {
            val listening = ui.playing?.let { it.stem == sel.stem && it.startFrame == sel.startFrame && it.endFrame == sel.endFrame } == true
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${StemStore.label(Demucs.STEMS[sel.stem])} · ${boardClock(sel.startFrame)} · ${boardClock(sel.frames)}",
                    style = MonoStyle, color = Neon.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("board_selection"),
                )
                Button(
                    onClick = { if (listening) actions.stop() else actions.play(sel) },
                    modifier = Modifier.testTag("board_play"),
                ) {
                    Icon(if (listening) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                    Text(" " + if (listening) tr("Arrêter") else tr("Écouter"))
                }
            }
            FlowRow(
                itemVerticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(tr("Fondu"), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
                listOf(BoardFade.Small to tr("Court"), BoardFade.Medium to tr("Moyen"), BoardFade.None to tr("Aucun")).forEach { (fade, name) ->
                    FilterChip(
                        selected = state.fade == fade, onClick = { state.fade = fade },
                        label = { Text(name) }, colors = pillColors(),
                        modifier = Modifier.testTag("board_fade_${fade.name.lowercase()}"),
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                OutlinedButton(onClick = { actions.send(sel, StemDestination.Pad()) }, contentPadding = padding, modifier = Modifier.testTag("board_to_pad")) { Text("→ Pad") }
                OutlinedButton(onClick = { actions.send(sel, StemDestination.Instrument) }, contentPadding = padding, modifier = Modifier.testTag("board_to_instrument")) {
                    Text("→ " + tr("Instrument"))
                }
                OutlinedButton(onClick = { actions.send(sel, StemDestination.Track()) }, contentPadding = padding, modifier = Modifier.testTag("board_to_track")) {
                    Text("→ " + tr("Piste"))
                }
                OutlinedButton(onClick = { actions.send(sel, StemDestination.Live) }, contentPadding = padding, modifier = Modifier.testTag("board_to_live")) { Text("→ Live") }
            }
        }
        if (sel == null || sel.stem == VOCALS_STEM) {
            OutlinedButton(onClick = { actions.chop(ui.setId, VOCALS_STEM, CHOP_MAX) }, modifier = Modifier.fillMaxWidth().testTag("board_chop")) {
                Icon(Icons.Filled.ContentCut, contentDescription = null)
                Text(" " + tr("Découper la voix en phrases"))
            }
        }
        ui.notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.Amber, modifier = Modifier.testTag("board_notice")) }
    }
}
