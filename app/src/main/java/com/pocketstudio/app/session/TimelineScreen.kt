@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.session

import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.ScaledDropdownMenu

import com.pocketstudio.app.learn.spot
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pocketstudio.model.ClipPreview
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.PartSpan
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.channelOfKey
import com.pocketstudio.model.clipPreview
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.partAtStep
import com.pocketstudio.model.partSpans
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.model.songBars
import com.pocketstudio.model.songLoopOf
import com.pocketstudio.model.splitBar
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.fadingHorizontalScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** A song curve drawn under its track: lane [index] of the project's automation, [key] its track (null: the whole mix), [label] in words. */
class TimelineLane(val index: Int, val key: String?, val label: String)

/** What the timeline shows: the project, its rows and song curves, and where the song is. */
class TimelineUi(
    val project: Project,
    /** One row per track (see [timelineRows] in AppRoot): its key, name, colour, an audio track's spans. */
    val rows: List<TimelineRow>,
    /** Song curves with points (« Morceau » mode), each under its track or with the whole mix. */
    val lanes: List<TimelineLane> = emptyList(),
    val playing: Boolean = false,
    /** The part playing (-1: none). */
    val songIndex: Int = -1,
    /** The part that comes next (-1: none). */
    val upcoming: Int = -1,
    /** The song step playing (-1 outside the song): read only when drawn. */
    val songStep: () -> Int = { -1 },
    /** The parts looped over (« Boucle »), null: none. */
    val loop: IntRange? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

/** Every gesture of the timeline, to the ViewModel (each edit one undo step). */
class TimelineActions(
    val back: () -> Unit = {},
    /** ▶ the song (from the loop's first part when looping), ■ stops. */
    val togglePlay: () -> Unit = {},
    /** The song plays from part (index), or it comes next while playing. */
    val playPart: (Int) -> Unit = {},
    val togglePartTrack: (Int, String) -> Unit = { _, _ -> },
    /** In part (first), track (second) plays scene (third). */
    val setPartScene: (Int, String, Int) -> Unit = { _, _, _ -> },
    val duplicatePart: (Int) -> Unit = {},
    val removePart: (Int) -> Unit = {},
    /** Part (first) moved to place (second). */
    val movePart: (Int, Int) -> Unit = { _, _ -> },
    val setRepeats: (Int, Int) -> Unit = { _, _ -> },
    /** Part (first) cut in two, (second) bars from its start. */
    val splitPart: (Int, Int) -> Unit = { _, _ -> },
    /** A part playing scene (second) inserted at place (first). */
    val insertPart: (Int, Int) -> Unit = { _, _ -> },
    /** The editor of track (second) on its scene in part (first): Rythme, Acide, Notes, Audio. */
    val openEditor: (Int, String) -> Unit = { _, _ -> },
    /** M and S of a mixer channel. */
    val toggleMute: (Int) -> Unit = {},
    val toggleSolo: (Int) -> Unit = {},
    val setLoop: (IntRange?) -> Unit = {},
    /** Song curve (lane index) in Mixeur › Automation. */
    val openLane: (Int) -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
    /** The part sheet's intentions (Variation, Fill, Break…) on part (index): one undo step each. */
    val partIntent: (Int, PartIntent) -> Unit = { _, _ -> },
    /** Phase 36: a touch on a track's name, « Changer le son » of that track (key); null: the name is not a button. */
    val trackSound: ((String) -> Unit)? = null,
    /** Phase 36: « Ce qui joue » in scene (index), from the part sheet a long press on a flag opens. */
    val whatPlays: (Int) -> Unit = {},
)

private val BAND_H = 30.dp
private val BARS_H = 24.dp
private val RULER_H = BAND_H + BARS_H
internal val ROW_MIN = 56.dp
internal val ROW_MAX = 132.dp
internal val ROW_TALL_MIN = 96.dp
internal val ROW_TALL_MAX = 200.dp
internal val LANE_H = 60.dp
private val MIN_BAR = 6.dp
private val MAX_BAR = 220.dp

/** The playhead past this share of the screen turns the page (as FL Studio and Ableton follow). */
internal const val FOLLOW_EDGE = 0.85f

/** After a page turn the playhead sits this far into the screen. */
internal const val FOLLOW_LEAD = 0.08f

/** Clip geometry, shared by the clips and the glow drawn over the one playing. */
private val CLIP_PAD_V = 3.dp
private val CLIP_GAP = 1.dp
private val CLIP_RADIUS = 5.dp

/** A line of the grid: a track's row, or one of its song curves under it. */
internal class TimelineLine(val row: TimelineRow?, val lane: TimelineLane?) {
    fun height(rowHeight: Dp): Dp = if (row != null) rowHeight else LANE_H
}

/** The rows, each followed by its curves when [lanes] are shown; the whole mix's curves last. */
internal fun timelineLines(rows: List<TimelineRow>, lanes: List<TimelineLane>, showLanes: Boolean): List<TimelineLine> {
    val out = ArrayList<TimelineLine>()
    rows.forEach { r ->
        out += TimelineLine(r, null)
        if (showLanes) lanes.filter { it.key == r.key }.forEach { out += TimelineLine(null, it) }
    }
    if (showLanes) lanes.filter { l -> l.key == null || rows.none { it.key == l.key } }.forEach { out += TimelineLine(null, it) }
    return out
}

/**
 * A track row's height: the rows share the [available] height left by the [lanes] curves, so a
 * short song fills the screen, never thinner than [ROW_MIN] (they scroll then) nor taller than
 * [ROW_MAX]. [tall]: « Pistes hautes », for a closer look at the clips.
 */
internal fun timelineRowHeight(available: Dp, rows: Int, lanes: Int, tall: Boolean): Dp {
    if (rows <= 0) return ROW_MIN
    val fit = (available - LANE_H * lanes) / rows
    val normal = fit.coerceIn(ROW_MIN, ROW_MAX)
    return if (tall) (normal * 1.6f).coerceIn(ROW_TALL_MIN, ROW_TALL_MAX) else normal
}

/**
 * Following the playhead, page by page: where the grid should scroll to when the playhead at
 * [headX] (pixels from the song's start) has passed [FOLLOW_EDGE] of the [viewport] or is before
 * its left edge (the song started again, a loop went back); null while it is well in view. The
 * new page puts the playhead [FOLLOW_LEAD] into the screen.
 */
internal fun followScroll(headX: Float, scrollX: Float, viewport: Float, maxScroll: Float): Float? {
    if (viewport <= 0f || !headX.isFinite()) return null
    if (headX >= scrollX && headX <= scrollX + viewport * FOLLOW_EDGE) return null
    val to = (headX - viewport * FOLLOW_LEAD).coerceIn(0f, maxScroll.coerceAtLeast(0f))
    // At the song's end the page cannot turn further: nothing to do.
    return to.takeIf { abs(it - scrollX) >= 1f }
}

/** Hue (degrees), saturation and lightness of [c], 0..1. */
private fun hsl(c: Color): FloatArray {
    val max = maxOf(c.red, c.green, c.blue)
    val min = minOf(c.red, c.green, c.blue)
    val l = (max + min) / 2f
    val d = max - min
    if (d < 1e-4f) return floatArrayOf(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f)).coerceAtLeast(1e-4f)
    val h = when (max) {
        c.red -> 60f * (((c.green - c.blue) / d) % 6f)
        c.green -> 60f * ((c.blue - c.red) / d + 2f)
        else -> 60f * ((c.red - c.green) / d + 4f)
    }
    return floatArrayOf((h + 360f) % 360f, s, l)
}

/** How far apart two colours look: hue degrees for colours, lightness for greys (0: the same). */
internal fun colorDistance(a: Color, b: Color): Float {
    val x = hsl(a)
    val y = hsl(b)
    val light = abs(x[2] - y[2]) * 180f
    if (x[1] < 0.15f || y[1] < 0.15f) return light + abs(x[1] - y[1]) * 180f
    val dh = abs(x[0] - y[0]).let { minOf(it, 360f - it) }
    return dh + light * 0.5f
}

/** Spare colours for the timeline, taken in this order when a track's colour is too close to another's. */
private val SPARE_COLORS = listOf(
    Color(0xFFF06292), Color(0xFF4DB6AC), Color(0xFFFFB74D), Color(0xFF9575CD), Color(0xFFAED581),
    Color(0xFF4DD0E1), Color(0xFFE57373), Color(0xFFFFF176), Color(0xFF90A4AE), Color(0xFFBCAAA4),
)

/** Under this distance two tracks would read as one on the timeline. */
internal const val COLOR_CLASH = 24f

/**
 * The tracks' colours on the timeline, each kept unless it looks like an earlier track's (two
 * greens, Basse acide and a green instrument): then the first spare colour far from all the
 * others (or the farthest one). The tracks keep their own colours everywhere else.
 */
internal fun timelineColors(colors: List<Color>): List<Color> {
    val out = ArrayList<Color>(colors.size)
    colors.forEachIndexed { i, c ->
        if (out.none { colorDistance(it, c) < COLOR_CLASH }) {
            out += c
            return@forEachIndexed
        }
        // The colours to keep away from: the ones given so far and the ones still to come.
        val taken = out + colors.drop(i + 1)
        fun gap(s: Color) = taken.minOfOrNull { colorDistance(it, s) } ?: 360f
        out += SPARE_COLORS.firstOrNull { gap(it) >= 30f } ?: SPARE_COLORS.maxBy(::gap)
    }
    return out
}

/** The parts' flags on the ruler: a colour per scene (A, B, C…). */
private val SCENE_COLORS = listOf(
    Color(0xFFFFB86B), Color(0xFF7FB2FF), Color(0xFFFF7A90), Color(0xFF7EE0B5),
    Color(0xFFC59BFF), Color(0xFFFFE07A), Color(0xFF6FD3E8), Color(0xFFFF9FD6),
)

private fun sceneColor(scene: Int): Color = SCENE_COLORS[Math.floorMod(scene, SCENE_COLORS.size)]

/** Zoom (pixels per bar) and horizontal scroll of the timeline, shared by its ruler and its grid. */
@Stable
internal class TimelineView(barPx: Float, scrollX: Float = 0f) {
    var barPx by mutableFloatStateOf(barPx)
    var scrollX by mutableFloatStateOf(scrollX)
    /** The grid's visible width in pixels. */
    var viewport by mutableFloatStateOf(0f)
    /** The song's length in bars (plus room after its end). */
    var bars by mutableIntStateOf(1)
    /** Steps in a bar now (the time signature can change while the screen is open). */
    var stepsPerBar by mutableIntStateOf(16)
    /** Two fingers on the grid: the follow waits. */
    var pinching = false

    fun maxScroll(): Float = (bars * barPx + barPx * 2 - viewport).coerceAtLeast(0f)

    fun scrollTo(x: Float) {
        scrollX = x.coerceIn(0f, maxScroll())
    }

    /** Zoom by [factor] keeping the point at [anchor] (pixels from the grid's left) where it is. */
    fun zoomAround(anchor: Float, factor: Float, min: Float, max: Float) {
        if (!factor.isFinite() || factor <= 0f) return
        val bar = (scrollX + anchor) / barPx
        barPx = (barPx * factor).coerceIn(min, max)
        scrollTo(bar * barPx - anchor)
    }

    companion object {
        val Saver = listSaver<TimelineView, Float>(save = { listOf(it.barPx, it.scrollX) }, restore = { TimelineView(it[0], it[1]) })
    }
}

/**
 * The one owner of the timeline's scroll. A finger drag, a fling, the zoom, « Tout voir » and the
 * follow all go through [state]'s mutex: only one moves the grid at a time, a finger always wins
 * over the follow (which then stops), and a new zoom replaces the one still running.
 */
@Stable
internal class TimelineScroller(private val view: TimelineView, private val scope: CoroutineScope) {
    /** Tells a finger drag (the follow stops) from the screen's own moves. */
    val interactions = MutableInteractionSource()

    val state = ScrollableState { delta ->
        val before = view.scrollX
        view.scrollTo(before - delta)
        before - view.scrollX
    }

    /** Where the zoom running now goes, so a second press zooms on from there (NaN: none). */
    private var zoomGoal = Float.NaN

    /** Zoom by [factor] (animated), keeping the point at [anchor] where it is. */
    fun zoomBy(factor: Float, min: Float, max: Float, anchor: () -> Float) {
        val goal = ((if (zoomGoal.isNaN()) view.barPx else zoomGoal) * factor).coerceIn(min, max)
        zoomGoal = goal
        scope.launch {
            try {
                state.scroll(MutatePriority.UserInput) {
                    var last = view.barPx
                    animate(view.barPx, goal, animationSpec = tween(200)) { v, _ ->
                        view.zoomAround(anchor(), v / last, min, max)
                        last = view.barPx
                    }
                }
            } finally {
                if (zoomGoal == goal) zoomGoal = Float.NaN
            }
        }
    }

    /** The whole song on the screen (animated). */
    fun fit(target: Float) {
        zoomGoal = Float.NaN
        scope.launch {
            state.scroll(MutatePriority.UserInput) {
                val from = view.barPx
                val fromX = view.scrollX
                animate(0f, 1f, animationSpec = tween(250)) { f, _ ->
                    view.barPx = from + (target - from) * f
                    view.scrollTo(fromX * (1f - f))
                }
            }
        }
    }

    /** A gesture the screen does not animate (a pinch) takes over: whatever moves the grid stops. */
    fun stop() {
        scope.launch { state.stopScroll(MutatePriority.UserInput) }
    }

    /** The follow's page turn: gives way to any finger or zoom. */
    suspend fun glideTo(x: Float) {
        state.scroll(MutatePriority.Default) {
            animate(view.scrollX, x, animationSpec = tween(260, easing = FastOutSlowInEasing)) { v, _ -> scrollBy(view.scrollX - v) }
        }
    }
}

/** Where a touch on the grid landed: part [part], on a track ([key]) or a curve ([lane]), [bar] bars into the part. */
internal data class TimelineHit(val part: Int, val key: String?, val lane: Int?, val bar: Int)

/**
 * The part and line under ([x], [y]) on the grid, [x] from its visible left edge, [y] from its
 * top; null past the song's end or below the last line. Pure: the screen's touches and its tests.
 */
internal fun timelineHit(
    x: Float, y: Float, scrollX: Float, stepPx: Float, stepsPerBar: Int,
    spans: List<PartSpan>, lines: List<TimelineLine>, lineTops: FloatArray, lineHeights: FloatArray,
): TimelineHit? {
    if (stepPx <= 0f) return null
    val step = ((scrollX + x) / stepPx).toInt()
    val span = spans.firstOrNull { step >= it.start && step < it.end } ?: return null
    val i = lines.indices.firstOrNull { y >= lineTops[it] && y < lineTops[it] + lineHeights[it] } ?: return null
    val line = lines[i]
    return TimelineHit(span.index, line.row?.key, line.lane?.index, (step - span.start) / stepsPerBar.coerceAtLeast(1))
}

/** A clip's long press: its actions, shown where the finger is ([x], [y] on the grid). */
private data class ClipMenu(val part: Int, val key: String?, val lane: Int?, val bar: Int, val x: Float, val y: Float)

/** The editor a track opens in, in words. */
private fun editorName(key: String): String = when {
    key == SongPart.DRUMS -> tr("Ouvrir dans Rythme")
    key == SongPart.ACID -> tr("Ouvrir dans Acide")
    key.startsWith("audio:") -> tr("Ouvrir dans Pistes audio")
    else -> tr("Ouvrir dans Notes")
}

/**
 * « Ligne du temps »: the whole song, every track, on one screen. A ruler of bars with the song's
 * parts as coloured flags; one row per track (its colour, name, M and S, a fixed column) with a
 * clip per part, drawn with what it plays; the song curves under their track. The rows share the
 * screen's height. Scroll sideways, pinch or ± to zoom; the playhead moves as the song plays
 * (drawn, never recomposed) and the screen follows it page by page until a finger scrolls.
 *
 * Gestures: a touch on a clip switches the track off or on in that part, a long press opens its
 * actions (open in its editor, choose its scene, repeats, cut, copy, remove). On the ruler: a
 * touch plays from that part; on the parts' flags a long press then a slide moves a part; on the
 * bars a long press then a slide loops over parts (the engine moves on at part boundaries).
 */
@Composable
fun TimelineScreen(
    ui: TimelineUi, actions: TimelineActions, barDp: Dp? = null,
    /** Under the timeline's bar: the position strip (tempo, « mes. x/y »), given by the app. */
    under: (@Composable () -> Unit)? = null,
    /**
     * Phase 36: inside the Structure space, under the app's own top bar and play strip: no bar of
     * its own (▶, undo and the position are the app's), only its tools, [lead] first (the
     * « Ligne du temps / Parties » switch), and [expand] to see it on the whole screen.
     */
    embedded: Boolean = false,
    lead: (@Composable () -> Unit)? = null,
    expand: (() -> Unit)? = null,
) {
    val p = ui.project
    var immersive by rememberSaveable { mutableStateOf(false) }
    com.pocketstudio.app.live.HideSystemBars(immersive)
    var showLanes by rememberSaveable { mutableStateOf(true) }
    var tallRows by rememberSaveable { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    val follow = rememberSaveable { mutableStateOf(true) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Neon.Background).testTag("timeline_screen")) {
        val density = LocalDensity.current
        val landscape = maxWidth > maxHeight && maxWidth >= 600.dp
        val headerW = if (maxWidth >= 600.dp) 188.dp else 116.dp
        val gridWidth = maxWidth - headerW
        val bars = p.songBars.coerceAtLeast(1)
        val minPx = with(density) { MIN_BAR.toPx() }
        val maxPx = with(density) { MAX_BAR.toPx() }
        val view = rememberSaveable(saver = TimelineView.Saver) {
            // The whole song on the screen when it fits at a readable size, else 20 dp a bar.
            val fit = (gridWidth - 16.dp) / bars
            val start = barDp ?: if (fit >= 14.dp) fit.coerceAtMost(48.dp) else 20.dp
            TimelineView(with(density) { start.toPx() })
        }
        view.bars = bars
        view.viewport = with(density) { gridWidth.toPx() }
        view.stepsPerBar = p.stepsPerBar.coerceAtLeast(1)
        val scope = rememberCoroutineScope()
        val scroller = remember(view) { TimelineScroller(view, scope) }
        val latestStep by rememberUpdatedState(ui.songStep)
        // A finger drag (never the screen's own moves) stops the follow.
        LaunchedEffect(scroller) {
            scroller.interactions.interactions.collect { if (it is DragInteraction.Start) follow.value = false }
        }
        // ▶ always brings the playhead back into view.
        LaunchedEffect(ui.playing) { if (ui.playing) follow.value = true }
        LaunchedEffect(follow.value, ui.playing, scroller) {
            if (!follow.value || !ui.playing) return@LaunchedEffect
            // A fling left over stops: from now on the playhead leads.
            scroller.state.stopScroll(MutatePriority.UserInput)
            var job: Job? = null
            var goal = Float.NaN
            snapshotFlow {
                val s = latestStep()
                if (s < 0) Float.NaN else followScroll(s * view.barPx / view.stepsPerBar, view.scrollX, view.viewport, view.maxScroll()) ?: Float.NaN
            }.collect { to ->
                if (to.isNaN() || view.pinching) return@collect
                // Already going there: let the glide finish.
                if (job?.isActive == true && abs(to - goal) < view.viewport * 0.25f) return@collect
                job?.cancel()
                goal = to
                job = launch { scroller.glideTo(to) }
            }
        }
        // Zoom around the playhead when it is on the screen, else around the middle.
        val zoomAnchor = {
            val s = latestStep()
            val x = s * view.barPx / view.stepsPerBar - view.scrollX
            if (s >= 0 && x in 0f..view.viewport) x else view.viewport / 2f
        }
        val tools: @Composable (Modifier, Boolean) -> Unit = { m, withHelp ->
            TimelineTools(
                ui, view, follow.value, setFollow = { follow.value = it }, showLanes, setLanes = { showLanes = it },
                tallRows, setTall = { tallRows = it }, openHelp = if (withHelp) ({ help = true }) else null, setLoop = actions.setLoop, modifier = m,
            )
        }
        Column(Modifier.fillMaxSize()) {
            val zoom: @Composable () -> Unit = { ZoomIcons(view, scroller, p.songBars, minPx, maxPx, zoomAnchor) }
            if (embedded) {
                // One line: the switch, the tools (Suivre, Boucle, courbes, hauteur), the zoom when wide, ⛶.
                Row(
                    Modifier.fillMaxWidth().background(lerp(Neon.Surface, Color.Black, 0.18f)).padding(start = 4.dp).testTag("timeline_tools_bar"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    lead?.invoke()
                    tools(Modifier.weight(1f), !landscape)
                    if (landscape) {
                        zoom()
                        HelpIcon { help = true }
                    }
                    if (expand != null) ToolIcon(Icons.Filled.Fullscreen, tr("Plein écran"), "timeline_expand", onClick = expand)
                }
            } else if (landscape) {
                // Wide: one bar, the zoom with the tools.
                TimelineTopBar(ui, actions, immersive, toggleFull = { immersive = !immersive }) {
                    tools(Modifier.weight(1f), false)
                    zoom()
                    HelpIcon { help = true }
                }
            } else {
                TimelineTopBar(ui, actions, immersive, toggleFull = { immersive = !immersive }, middle = null)
                tools(Modifier.fillMaxWidth().background(lerp(Neon.Surface, Color.Black, 0.18f)), true)
            }
            // Full screen is for the clips: the strip gives way there.
            if (under != null && !immersive && !embedded) under()
            if (p.song.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Le morceau est vide : ajoute des parties (Structure › Parties) pour les voir ici, piste par piste."), color = Neon.TextSecondary)
                    OutlinedButton(onClick = actions.back, modifier = Modifier.testTag("timeline_empty_back")) { Text(if (embedded) tr("Voir les parties") else tr("Revenir au morceau")) }
                }
                return@Column
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                TimelineBody(ui, actions, view, scroller, showLanes, tallRows, headerW, minPx, maxPx, Modifier.fillMaxSize())
                if (!landscape) {
                    // Narrow: the zoom floats over the grid's corner, always in reach.
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(RoundedCornerShape(24.dp))
                            .background(Neon.Surface.copy(alpha = 0.92f)).padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { zoom() }
                }
            }
        }
    }
    if (help) TimelineHelp(close = { help = false })
}

/** ← Morceau, ▶/■, the clock (time and bar), [middle] (the tools, side by side when wide), ↶ ↷, ⛶. */
@Composable
private fun TimelineTopBar(
    ui: TimelineUi, actions: TimelineActions, immersive: Boolean, toggleFull: () -> Unit,
    middle: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(Neon.Surface).padding(horizontal = 4.dp).testTag("timeline_top_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        IconButton(onClick = actions.back, modifier = Modifier.testTag("timeline_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Revenir au morceau"))
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = actions.togglePlay).testTag("timeline_play")
                .semantics { contentDescription = if (ui.playing) tr("Arrêter") else tr("Jouer le morceau") },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(if (ui.playing) Neon.Magenta else Neon.Cyan), contentAlignment = Alignment.Center) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent)
            }
        }
        if (middle != null) {
            TimelineClock(ui, Modifier.padding(start = 4.dp).width(90.dp))
            middle()
        } else {
            TimelineClock(ui, Modifier.padding(start = 6.dp).weight(1f))
        }
        IconButton(onClick = actions.undo, enabled = ui.canUndo, modifier = Modifier.testTag("timeline_undo")) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = tr("Annuler"))
        }
        IconButton(onClick = actions.redo, enabled = ui.canRedo, modifier = Modifier.testTag("timeline_redo")) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = tr("Rétablir"))
        }
        IconButton(onClick = toggleFull, modifier = Modifier.testTag("timeline_fullscreen")) {
            Icon(
                if (immersive) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = if (immersive) tr("Quitter le plein écran") else tr("Plein écran"),
                tint = if (immersive) Neon.Cyan else Neon.TextPrimary,
            )
        }
    }
}

/** "1:05" from seconds. */
private fun clockText(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

/** The time gone and the song's length, the bar and beat: the words change once a beat at most. */
@Composable
private fun TimelineClock(ui: TimelineUi, modifier: Modifier) {
    val p = ui.project
    val beatSeconds = 60.0 / p.tempoBpm
    val totalSteps = p.partSpans.lastOrNull()?.end ?: 0
    val total = (totalSteps / 4 * beatSeconds).toInt()
    val songStep by rememberUpdatedState(ui.songStep)
    val beat by remember { derivedStateOf { songStep().let { if (it < 0) -1 else it / 4 } } }
    val beatsPerBar = p.beatsPerBar.coerceAtLeast(1)
    Column(modifier.testTag("timeline_clock"), verticalArrangement = Arrangement.Center) {
        Text(
            (if (beat >= 0) clockText((beat * beatSeconds).toInt()) else "0:00") + " / " + clockText(total),
            style = MonoStyle, fontSize = 16.sp, color = if (beat >= 0) Neon.TextPrimary else Neon.TextSecondary, maxLines = 1,
        )
        Text(
            if (beat >= 0) tr("mes. %1\$d.%2\$d", beat / beatsPerBar + 1, beat % beatsPerBar + 1) + " / " + p.songBars
            else (if (p.songBars == 1) tr("1 mesure") else tr("%d mesures", p.songBars)),
            style = MonoStyle, color = if (beat >= 0) Neon.Magenta else Neon.TextSecondary, maxLines = 1,
        )
    }
}

/** Suivre, Boucle, then the view's buttons: the curves, the tracks' height, help. */
@Composable
private fun TimelineTools(
    ui: TimelineUi, view: TimelineView, follow: Boolean, setFollow: (Boolean) -> Unit,
    showLanes: Boolean, setLanes: (Boolean) -> Unit, tall: Boolean, setTall: (Boolean) -> Unit, openHelp: (() -> Unit)?,
    setLoop: (IntRange?) -> Unit, modifier: Modifier,
) {
    val p = ui.project
    Row(
        modifier.height(52.dp).fadingHorizontalScroll().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToolPill(tr("Suivre"), follow, "timeline_follow", Icons.Filled.KeyboardDoubleArrowRight) { setFollow(!follow) }
        val loop = ui.loop
        ToolPill(
            when {
                loop == null -> tr("Boucle")
                loop.first == loop.last -> tr("Boucle %d", loop.first + 1)
                else -> tr("Boucle %1\$d–%2\$d", loop.first + 1, loop.last + 1)
            },
            loop != null, "timeline_loop", Icons.Filled.Repeat,
        ) {
            if (loop != null) {
                setLoop(null)
            } else {
                // The part playing, or the first one on the screen.
                val part = ui.songIndex.takeIf { it >= 0 }
                    ?: p.partAtStep((view.scrollX / (view.barPx / view.stepsPerBar)).toInt()).takeIf { it >= 0 } ?: 0
                setLoop(songLoopOf(part, part, p.song.size))
            }
        }
        if (ui.lanes.isNotEmpty()) {
            ToolIcon(
                Icons.AutoMirrored.Filled.ShowChart, if (showLanes) tr("Cacher les courbes") else tr("Montrer les courbes"), "timeline_lanes", on = showLanes,
            ) { setLanes(!showLanes) }
        }
        ToolIcon(
            if (tall) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
            if (tall) tr("Pistes moins hautes") else tr("Pistes plus hautes"), "timeline_height", on = tall,
        ) { setTall(!tall) }
        if (openHelp != null) HelpIcon(openHelp)
    }
}

/** Zoom + − and « Tout voir ». */
@Composable
private fun ZoomIcons(view: TimelineView, scroller: TimelineScroller, songBars: Int, minPx: Float, maxPx: Float, zoomAnchor: () -> Float) {
    ToolIcon(Icons.Filled.ZoomIn, tr("Zoomer"), "timeline_zoom_in") { scroller.zoomBy(1.6f, minPx, maxPx, zoomAnchor) }
    ToolIcon(Icons.Filled.ZoomOut, tr("Dézoomer"), "timeline_zoom_out") { scroller.zoomBy(1f / 1.6f, minPx, maxPx, zoomAnchor) }
    ToolIcon(Icons.Filled.ZoomOutMap, tr("Voir tout le morceau"), "timeline_fit") {
        scroller.fit((view.viewport / (songBars.coerceAtLeast(1) + 1)).coerceIn(minPx, maxPx))
    }
}

@Composable
private fun HelpIcon(open: () -> Unit) = ToolIcon(Icons.AutoMirrored.Filled.HelpOutline, tr("Aide de la ligne du temps"), "timeline_help", onClick = open)

/** A switch of the tool line: an icon and a word, lit in the accent when on. */
@Composable
private fun ToolPill(label: String, on: Boolean, tag: String, icon: ImageVector, toggle: () -> Unit) {
    Row(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (on) Neon.Cyan.copy(alpha = 0.22f) else Neon.SurfaceHigh.copy(alpha = 0.55f))
            .clickable(onClick = toggle).semantics { selected = on }.padding(start = 8.dp, end = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (on) Neon.Cyan else Neon.TextSecondary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Neon.Cyan else Neon.TextPrimary, maxLines = 1)
    }
}

/** A button of the tool line: an icon alone, 48 dp, lit when [on]. */
@Composable
private fun ToolIcon(icon: ImageVector, description: String, tag: String, on: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
        Icon(icon, contentDescription = description, tint = if (on) Neon.Cyan else Neon.TextPrimary)
    }
}

/** The gestures, in a few short lines (the « ? » of the tool line). */
@Composable
private fun TimelineHelp(close: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = close,
        title = { Text(tr("Ligne du temps")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("timeline_help_text")) {
                listOf(
                    tr("Toucher un clip : couper ou remettre la piste dans cette partie."),
                    tr("Deux touches sur un clip : l'ouvrir dans son éditeur, sur cette partie."),
                    tr("Toucher le nom d'une piste : changer son son (le kit pour la batterie)."),
                    tr("Appui long sur un drapeau de partie : sa feuille (Fill, Break, Montée…)."),
                    tr("Appui long sur un clip : l'ouvrir, changer de partie, répéter, couper, dupliquer."),
                    tr("Toucher la règle : jouer depuis cette partie."),
                    tr("Appui long puis glisser : sur un drapeau, déplacer la partie ; sur les mesures, boucler."),
                    tr("Deux doigts : zoomer. Un doigt qui fait défiler arrête « Suivre », Lecture le remet."),
                ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(tr("Compris")) } },
    )
}

/** The colours the timeline draws with, read once from the theme. */
private class TimelinePalette(
    val grid: Color, val rowA: Color, val rowB: Color, val lane: Color, val ruler: Color, val band: Color,
    val barLine: Color, val beatLine: Color, val partLine: Color, val separator: Color,
    val playing: Color, val next: Color, val loop: Color, val text: Color, val textDim: Color, val head: Color, val mix: Color,
    val header: Color, val ink: Color,
)

@Composable
private fun timelinePalette(): TimelinePalette {
    val bg = Neon.Background
    return TimelinePalette(
        grid = lerp(bg, Color.Black, 0.30f),
        rowA = lerp(bg, Color.Black, 0.20f),
        rowB = lerp(bg, Color.Black, 0.27f),
        lane = lerp(bg, Color.Black, 0.40f),
        ruler = lerp(Neon.Surface, Color.Black, 0.28f),
        band = lerp(Neon.Surface, Color.Black, 0.40f),
        barLine = Color.White.copy(alpha = 0.09f),
        beatLine = Color.White.copy(alpha = 0.035f),
        partLine = Color.White.copy(alpha = 0.20f),
        separator = Color.Black.copy(alpha = 0.35f),
        playing = Neon.Magenta,
        next = Neon.Amber,
        loop = Neon.Amber,
        text = Neon.TextPrimary,
        textDim = Neon.TextSecondary,
        head = Color.White,
        mix = Neon.Cyan,
        header = lerp(Neon.Surface, Color.Black, 0.12f),
        ink = Color(0xFF16181C),
    )
}

/**
 * The ruler and the rows. The header column stays put and opaque on the left; the ruler and the
 * grid scroll sideways together and each clips its drawing to its own bounds.
 */
@Composable
private fun TimelineBody(
    ui: TimelineUi, actions: TimelineActions, view: TimelineView, scroller: TimelineScroller, showLanes: Boolean, tall: Boolean,
    headerW: Dp, minPx: Float, maxPx: Float, modifier: Modifier,
) {
    val p = ui.project
    val spans = remember(p) { p.partSpans }
    val lines = remember(ui.rows, ui.lanes, showLanes) { timelineLines(ui.rows, ui.lanes, showLanes) }
    val trackColors = remember(ui.rows) { ui.rows.map { it.key }.zip(timelineColors(ui.rows.map { it.color })).toMap() }
    val colors = timelinePalette()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val rowH = timelineRowHeight(maxHeight - RULER_H - 2.dp, lines.count { it.row != null }, lines.count { it.lane != null }, tall)
        val lineHeights = remember(lines, rowH, density) { FloatArray(lines.size) { with(density) { lines[it].height(rowH).toPx() } } }
        val lineTops = remember(lineHeights) { FloatArray(lineHeights.size).also { t -> for (i in 1 until t.size) t[i] = t[i - 1] + lineHeights[i - 1] } }
        val totalHeight = lines.fold(0.dp) { a, l -> a + l.height(rowH) }
        val pinch = Modifier.pointerInput(view) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                try {
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2) {
                            if (!view.pinching) {
                                // Two fingers take the grid: the follow and any glide or fling stop.
                                view.pinching = true
                                scroller.stop()
                            }
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                view.zoomAround(event.calculateCentroid(useCurrent = true).x, zoom, minPx, maxPx)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                } finally {
                    view.pinching = false
                }
            }
        }
        val scrolls = Modifier.scrollable(scroller.state, Orientation.Horizontal, interactionSource = scroller.interactions)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(RULER_H)) {
                RulerCorner(headerW, colors)
                TimelineRuler(ui, actions, view, spans, colors, Modifier.weight(1f).fillMaxHeight().clipToBounds().then(scrolls), pinch)
            }
            Row(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                Column(Modifier.width(headerW).zIndex(1f).background(colors.header)) {
                    lines.forEach { line ->
                        when {
                            line.row != null -> TrackHeader(p, line.row, trackColors[line.row.key] ?: line.row.color, rowH, headerW >= 150.dp, colors, actions)
                            line.lane != null -> LaneHeader(line.lane, trackColors[line.lane.key] ?: Neon.Cyan, colors, actions)
                        }
                    }
                }
                TimelineGrid(
                    ui, actions, view, spans, lines, lineTops, lineHeights, trackColors, colors,
                    Modifier.weight(1f).height(totalHeight).clipToBounds().then(scrolls), pinch,
                )
            }
        }
    }
}

/** The ruler's corner, over the track names: what its two lines are. */
@Composable
private fun RulerCorner(width: Dp, colors: TimelinePalette) {
    Column(
        Modifier.width(width).fillMaxHeight().background(colors.header).drawBehind {
            drawLine(colors.separator, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
        }.padding(start = 12.dp),
    ) {
        Box(Modifier.height(BAND_H), contentAlignment = Alignment.CenterStart) {
            Text(tr("Parties"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
        }
        Box(Modifier.height(BARS_H), contentAlignment = Alignment.CenterStart) {
            Text(tr("Mesures"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
        }
    }
}

/** A track's header: its colour, its name, M and S (its mixer channel); on one line when [wide] and low. */
@Composable
private fun TrackHeader(p: Project, row: TimelineRow, color: Color, height: Dp, wide: Boolean, colors: TimelinePalette, actions: TimelineActions) {
    val ch = channelOfKey(row.key)
    val channel = ch?.let { p.mixer.channels.getOrNull(it) }
    val name = DefaultNames.shown(row.name)
    val compact = height < 72.dp && wide
    Row(
        Modifier.fillMaxWidth().height(height)
            .background(lerp(colors.header, color, 0.07f))
            .drawBehind { drawLine(colors.separator, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }
            .testTag("timeline_header_${row.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(start = 5.dp, top = 6.dp, bottom = 6.dp).width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(color))
        val sound = actions.trackSound
        val title: @Composable (Modifier) -> Unit = { m ->
            Text(
                name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, color = Neon.TextPrimary,
                // Phase 36: the track's name opens « Changer le son » (the kit for the drums).
                modifier = if (sound == null) m else m.clip(RoundedCornerShape(6.dp))
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = tr("Changer le son de %s", name)) { sound(row.key) }
                    .heightIn(min = 32.dp).wrapContentHeight(Alignment.CenterVertically)
                    .spot("timeline_name_${row.key}", if (row.key == com.pocketstudio.model.SongPart.DRUMS) tr("Changer le son") else null)
                    .testTag("timeline_name_${row.key}"),
            )
        }
        val buttons: @Composable () -> Unit = {
            if (ch != null && channel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuteSolo("M", channel.mute, Neon.Amber, tr("Couper la piste %s", name), "timeline_mute_${row.key}") { actions.toggleMute(ch) }
                    MuteSolo("S", channel.solo, Neon.Cyan, tr("Écouter la piste %s seule", name), "timeline_solo_${row.key}") { actions.toggleSolo(ch) }
                }
            }
        }
        if (compact) {
            Row(Modifier.weight(1f).padding(start = 8.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                title(Modifier.weight(1f).padding(end = 4.dp))
                buttons()
            }
        } else {
            Column(Modifier.weight(1f).padding(start = 8.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(if (height < 72.dp) 2.dp else 6.dp)) {
                title(Modifier)
                buttons()
            }
        }
    }
}

@Composable
private fun MuteSolo(letter: String, on: Boolean, color: Color, description: String, tag: String, toggle: () -> Unit) {
    Box(
        Modifier.size(width = 36.dp, height = 28.dp).clip(RoundedCornerShape(6.dp))
            .background(if (on) color else Neon.SurfaceHigh)
            .clickable(onClick = toggle)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (on) Color.Black else Neon.TextSecondary)
    }
}

/** A song curve's header: what it moves; a touch opens it in the Mixeur. */
@Composable
private fun LaneHeader(lane: TimelineLane, color: Color, colors: TimelinePalette, actions: TimelineActions) {
    Row(
        Modifier.fillMaxWidth().height(LANE_H).background(lerp(colors.header, Color.Black, 0.22f))
            .drawBehind { drawLine(colors.separator, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }
            .clickable(onClickLabel = tr("Modifier la courbe dans Mix")) { actions.openLane(lane.index) }
            .padding(start = 14.dp, end = 4.dp).testTag("timeline_lane_${lane.index}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(lane.label, style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
    }
}

/** Measured texts kept between frames (the same few labels are drawn again and again). */
private class LabelCache(private val measure: (String) -> TextLayoutResult) {
    private val cache = HashMap<String, TextLayoutResult>()
    fun get(text: String): TextLayoutResult = cache.getOrPut(text) { if (cache.size > 400) cache.clear(); measure(text) }
}

/** Bars between two numbers on the ruler so they never crowd: 1, 2, 4, 8… */
internal fun numberEvery(barPx: Float, minGap: Float): Int {
    var n = 1
    while (n * barPx < minGap && n < 256) n *= 2
    return n
}

/**
 * The ruler: the song's parts as flags in their scene's colour (the one playing bright with a
 * green line, the next one outlined, the loop in amber) over the bar numbers, and the playhead's head.
 */
@Composable
private fun TimelineRuler(
    ui: TimelineUi, actions: TimelineActions, view: TimelineView, spans: List<PartSpan>, colors: TimelinePalette,
    modifier: Modifier, pinch: Modifier,
) {
    val p = ui.project
    val measurer = rememberTextMeasurer()
    val flagStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
    val numberStyle = MonoStyle.copy(fontSize = 11.sp)
    val flagLabels = remember(measurer, flagStyle) { LabelCache { measurer.measure(it, flagStyle, maxLines = 1) } }
    val numbers = remember(measurer, numberStyle) { LabelCache { measurer.measure(it, numberStyle, maxLines = 1) } }
    val haptic = LocalHapticFeedback.current
    val latestUi by rememberUpdatedState(ui)
    val latestActions by rememberUpdatedState(actions)
    // A slide after a long press: moving part [dragFrom] (on the flags) or looping from [loopFrom] (on the bars).
    var dragFrom by remember { mutableIntStateOf(-1) }
    var loopFrom by remember { mutableIntStateOf(-1) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    // Phase 36: a long press on a part's flag, released without sliding, opens its sheet.
    var sheetFor by remember { mutableStateOf<Int?>(null) }
    val slop = with(LocalDensity.current) { 12.dp.toPx() }
    val bandPx = with(LocalDensity.current) { BAND_H.toPx() }
    fun partAt(x: Float): Int {
        val stepPx = view.barPx / view.stepsPerBar
        val step = ((view.scrollX + x) / stepPx).toInt()
        val sp = latestUi.project.partSpans
        return when {
            sp.isEmpty() -> -1
            step >= sp.last().end -> sp.last().index
            else -> latestUi.project.partAtStep(step.coerceAtLeast(0))
        }
    }
    Canvas(
        modifier
            // One detector for the three gestures, so they never race: a touch plays from the part
            // there; a long press then a slide moves a part (on the flags) or loops over parts (on
            // the bars); a long press let go where it was opens the part's sheet (on the flags).
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val long = awaitLongPressOrCancellation(down.id)
                    if (long == null) {
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (up != null && up.changedToUp() && !up.isConsumed) {
                            // A touch on the ruler plays from the part there (the song moves on at part boundaries).
                            val stepPx = view.barPx / view.stepsPerBar
                            val i = latestUi.project.partAtStep(((view.scrollX + down.position.x) / stepPx).toInt())
                            if (i >= 0) latestActions.playPart(i)
                        }
                        return@awaitEachGesture
                    }
                    val at = long.position
                    val i = partAt(at.x)
                    if (i < 0) return@awaitEachGesture
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dragX = at.x
                    dragStartX = at.x
                    if (at.y < bandPx) dragFrom = i else loopFrom = i
                    val ended = drag(long.id) { change ->
                        dragX += change.positionChange().x
                        change.consume()
                    }
                    if (ended) {
                        val to = partAt(dragX)
                        if (dragFrom >= 0 && abs(dragX - dragStartX) < slop) sheetFor = dragFrom
                        else if (dragFrom >= 0 && to >= 0 && to != dragFrom) latestActions.movePart(dragFrom, to)
                        if (loopFrom >= 0 && to >= 0) latestActions.setLoop(songLoopOf(loopFrom, to, latestUi.project.song.size))
                    }
                    dragFrom = -1
                    loopFrom = -1
                }
            }
            .then(pinch)
            .spot("timeline_ruler", tr("Jouer · appui long : Fill"))
            .testTag("timeline_ruler")
            .semantics { contentDescription = tr("Règle des mesures : touche pour jouer à partir d'une partie") },
    ) {
        val stepPx = view.barPx / view.stepsPerBar
        val x0 = view.scrollX
        val w = size.width
        val h = size.height
        val bandH = bandPx
        drawRect(colors.ruler)
        drawRect(colors.band, size = Size(w, bandH))
        // The bars: a number every few bars (as many as fit), a tick each bar, small ones on the beats.
        val beats = p.beatsPerBar.coerceAtLeast(1)
        val beatPx = view.barPx / beats
        val every = numberEvery(view.barPx, 34.dp.toPx())
        val first = (x0 / view.barPx).toInt().coerceAtLeast(0)
        val last = minOf(((x0 + w) / view.barPx).toInt() + 1, view.bars)
        for (b in first..last) {
            val x = b * view.barPx - x0
            val numbered = b % every == 0
            if (beatPx >= 8.dp.toPx() && b < view.bars) {
                for (k in 1 until beats) drawLine(colors.barLine, Offset(x + k * beatPx, h - 4.dp.toPx()), Offset(x + k * beatPx, h), 1f)
            }
            if (numbered) {
                drawLine(colors.textDim.copy(alpha = 0.55f), Offset(x, bandH), Offset(x, h), 1.dp.toPx())
                if (b < view.bars) drawText(numbers.get("${b + 1}"), color = colors.textDim, topLeft = Offset(x + 4.dp.toPx(), bandH + (BARS_H.toPx() - 14.sp.toPx()) / 2f))
            } else {
                drawLine(colors.textDim.copy(alpha = 0.30f), Offset(x, h - 7.dp.toPx()), Offset(x, h), 1.dp.toPx())
            }
        }
        // The loop (or the loop being chosen): amber over the bars, brackets at its ends.
        val chosen = if (loopFrom >= 0) songLoopOf(loopFrom, partAt(dragX), p.song.size) else ui.loop
        if (chosen != null && chosen.last < spans.size) {
            val l = spans[chosen.first].start * stepPx - x0
            val r = spans[chosen.last].end * stepPx - x0
            drawRect(colors.loop.copy(alpha = 0.16f), Offset(l, bandH), Size(r - l, h - bandH))
            drawRect(colors.loop, Offset(l, h - 3.dp.toPx()), Size(r - l, 3.dp.toPx()))
            drawRect(colors.loop, Offset(l, bandH), Size(2.dp.toPx(), h - bandH))
            drawRect(colors.loop, Offset(r - 2.dp.toPx(), bandH), Size(2.dp.toPx(), h - bandH))
        }
        // The parts: a pole at each start, a flag with its name (kept in view while the part is), a line under its length.
        val flagTop = 4.dp.toPx()
        val flagH = bandH - 10.dp.toPx()
        val r4 = CornerRadius(4.dp.toPx())
        spans.forEach { sp ->
            val left = sp.start * stepPx - x0
            val right = sp.end * stepPx - x0
            if (right < 0f || left > w) return@forEach
            val i = sp.index
            val part = p.song[i]
            val playing = i == ui.songIndex
            val base = sceneColor(part.scene)
            val c = when {
                i == dragFrom -> base.copy(alpha = 0.35f)
                playing -> base
                else -> lerp(base, colors.band, 0.25f)
            }
            // Its length, under the flag.
            drawRect(if (playing) colors.playing else c.copy(alpha = 0.55f), Offset(left, bandH - 3.dp.toPx()), Size(right - left, 3.dp.toPx()))
            // The pole down through the bars.
            drawLine(c.copy(alpha = 0.85f), Offset(left + 0.75.dp.toPx(), flagTop), Offset(left + 0.75.dp.toPx(), h), 1.5.dp.toPx())
            val title = p.sceneTitle(part.scene) + (if (part.bars == null && part.repeats > 1) " ×${part.repeats}" else "") + (if (part.mixed) " ≈" else "")
            val layout = flagLabels.get(title)
            val room = right - left - 3.dp.toPx()
            val flagW = minOf(layout.size.width + 14.dp.toPx(), room)
            if (flagW >= 10.dp.toPx()) {
                // Sticky: while the part is on the screen its flag stays at the left edge.
                val fx = maxOf(left, 0f).coerceAtMost(right - flagW - 2.dp.toPx()).coerceAtLeast(left)
                drawRoundRect(c, Offset(fx, flagTop), Size(flagW, flagH), r4)
                if (i == ui.upcoming) drawRoundRect(colors.next, Offset(fx, flagTop), Size(flagW, flagH), r4, style = Stroke(2.dp.toPx()))
                clipRect(left = fx, top = flagTop, right = fx + flagW - 3.dp.toPx(), bottom = flagTop + flagH) {
                    drawText(layout, color = colors.ink, topLeft = Offset(fx + 7.dp.toPx(), flagTop + (flagH - layout.size.height) / 2f))
                }
            }
        }
        // Where a part being moved goes.
        if (dragFrom >= 0) {
            val to = partAt(dragX)
            if (to >= 0 && to != dragFrom) {
                val x = (if (to > dragFrom) spans[to].end else spans[to].start) * stepPx - x0
                drawRect(colors.next, Offset(x - 1.5.dp.toPx(), 0f), Size(3.dp.toPx(), h))
            }
        }
        drawLine(colors.separator, Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.dp.toPx())
        // The playhead's head: a bright marker on the bars, pointing down at the grid.
        val s = ui.songStep()
        if (s >= 0) {
            val x = s * stepPx - x0
            val hw = 6.dp.toPx()
            val top = bandH + 2.dp.toPx()
            val tip = h
            val head = Path().apply {
                moveTo(x - hw, top)
                lineTo(x + hw, top)
                lineTo(x + hw, tip - 8.dp.toPx())
                lineTo(x, tip)
                lineTo(x - hw, tip - 8.dp.toPx())
                close()
            }
            drawPath(head, Color.Black.copy(alpha = 0.45f), style = Stroke(2.dp.toPx()))
            drawPath(head, colors.head)
        }
    }
    sheetFor?.let { i ->
        val sheet = partSheetUi(p, i)
        if (sheet == null) {
            LaunchedEffect(Unit) { sheetFor = null }
        } else {
            val full = p.song.size >= SongPart.MAX_PARTS
            PartSheet(
                sheet, onIntent = { actions.partIntent(i, it) }, close = { sheetFor = null },
                more = PartMoreActions(
                    whatPlays = { p.song.getOrNull(i)?.let { actions.whatPlays(it.scene) } },
                    moveUp = if (i > 0) ({ actions.movePart(i, i - 1) }) else null,
                    moveDown = if (i < p.song.lastIndex) ({ actions.movePart(i, i + 1) }) else null,
                    duplicate = if (!full) ({ actions.duplicatePart(i) }) else null,
                    remove = { actions.removePart(i) },
                ),
            )
        }
    }
}

/**
 * The grid: lines of bars and beats, a clip per part and track (a darker body in its colour, a
 * strip with its name, what it plays drawn small, a divider at each repeat; a muted one dashed),
 * the song curves; over it, drawn from the playing step alone, the playhead and the clips playing
 * now in a soft glow.
 */
@Composable
private fun TimelineGrid(
    ui: TimelineUi, actions: TimelineActions, view: TimelineView, spans: List<PartSpan>, lines: List<TimelineLine>,
    lineTops: FloatArray, lineHeights: FloatArray, trackColors: Map<String, Color>, colors: TimelinePalette, modifier: Modifier, pinch: Modifier,
) {
    val p = ui.project
    val measurer = rememberTextMeasurer()
    val clipStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val clipLabels = remember(measurer) { LabelCache { measurer.measure(it, clipStyle, maxLines = 1) } }
    // What each track plays in each scene, worked out once per version of the project.
    val previews = remember(p) { HashMap<Pair<Int, String>, ClipPreview>() }
    // The song curves as the engine plays them, a value per point.
    val curves = remember(p.automation, p.songBars, p.beatsPerBar, ui.lanes) {
        ui.lanes.associate { l ->
            val lane = p.automation.getOrNull(l.index)
            val points = if (lane?.songPerBeat == true) p.songBars * p.beatsPerBar else p.songBars
            l.index to (lane?.songCurve(points.coerceAtLeast(1)) ?: emptyList())
        }
    }
    val latestUi by rememberUpdatedState(ui)
    val latestActions by rememberUpdatedState(actions)
    val latestLines by rememberUpdatedState(lines)
    val latestTops by rememberUpdatedState(lineTops)
    val latestHeights by rememberUpdatedState(lineHeights)
    var menu by remember { mutableStateOf<ClipMenu?>(null) }
    val haptic = LocalHapticFeedback.current
    fun hit(at: Offset): TimelineHit? {
        val pr = latestUi.project
        return timelineHit(at.x, at.y, view.scrollX, view.barPx / view.stepsPerBar, view.stepsPerBar, pr.partSpans, latestLines, latestTops, latestHeights)
    }
    Box(modifier) {
        Canvas(
            Modifier.matchParentSize()
                // Its own layer: the playhead moving over it never redraws the clips.
                .graphicsLayer {}
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { at ->
                            val h = hit(at) ?: return@detectTapGestures
                            when {
                                h.key != null -> latestActions.togglePartTrack(h.part, h.key)
                                h.lane != null -> latestActions.openLane(h.lane)
                            }
                        },
                        // Phase 36 (FL Studio Mobile's gesture): two touches on a clip open it in its editor, on that part.
                        onDoubleTap = { at ->
                            val h = hit(at) ?: return@detectTapGestures
                            if (h.key != null) latestActions.openEditor(h.part, h.key)
                        },
                        onLongPress = { at ->
                            val h = hit(at) ?: return@detectTapGestures
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menu = ClipMenu(h.part, h.key, h.lane, h.bar, at.x, at.y)
                        },
                    )
                }
                .then(pinch)
                .spot("timeline_grid", tr("2 touches : ouvrir"))
                .testTag("timeline_grid")
                .semantics { contentDescription = tr("Ligne du temps : %1\$d parties, %2\$d pistes", p.song.size, ui.rows.size) },
        ) {
            val stepPx = view.barPx / view.stepsPerBar
            val x0 = view.scrollX
            val w = size.width
            drawRect(colors.grid)
            // Line backgrounds and the separators between them.
            var trackRow = 0
            lines.forEachIndexed { li, line ->
                val c = if (line.row != null) (if (trackRow++ % 2 == 0) colors.rowA else colors.rowB) else colors.lane
                drawRect(c, Offset(0f, lineTops[li]), Size(w, lineHeights[li]))
                val y = lineTops[li] + lineHeights[li] - 0.5f
                drawLine(colors.separator, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }
            // Beats, bars (stronger every 4), part boundaries.
            val beats = p.beatsPerBar.coerceAtLeast(1)
            val beatPx = view.barPx / beats
            val first = (x0 / view.barPx).toInt().coerceAtLeast(0)
            val last = ((x0 + w) / view.barPx).toInt() + 1
            for (b in first..last) {
                val x = b * view.barPx - x0
                if (beatPx >= 10.dp.toPx()) {
                    for (k in 1 until beats) {
                        val bx = x + k * beatPx
                        drawLine(colors.beatLine, Offset(bx, 0f), Offset(bx, size.height), 1f)
                    }
                }
                drawLine(if (b % 4 == 0) colors.partLine.copy(alpha = 0.13f) else colors.barLine, Offset(x, 0f), Offset(x, size.height), 1f)
            }
            spans.forEach { sp ->
                val x = sp.start * stepPx - x0
                if (x in -2f..w + 2f) drawLine(colors.partLine, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
            // Clips.
            val padV = CLIP_PAD_V.toPx()
            val gap = CLIP_GAP.toPx()
            val radius = CornerRadius(CLIP_RADIUS.toPx())
            val dashed = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
            spans.forEach { sp ->
                val left = sp.start * stepPx - x0
                val right = sp.end * stepPx - x0
                if (right < 0f || left > w) return@forEach
                val part = p.song[sp.index]
                // A part repeating its scene: a divider at each new pass.
                val passSteps = if (part.bars == null && part.repeats > 1) sp.steps / part.repeats else 0
                lines.forEachIndexed { li, line ->
                    val row = line.row ?: return@forEachIndexed
                    val top = lineTops[li] + padV
                    val h = lineHeights[li] - 2 * padV
                    val cl = left + gap
                    val cw = (right - left - 2 * gap).coerceAtLeast(1f)
                    val color = trackColors[row.key] ?: row.color
                    if (!part.plays(row.key)) {
                        // Silent here: a dashed outline, the empty slot it would fill.
                        drawRoundRect(color.copy(alpha = 0.05f), Offset(cl, top), Size(cw, h), radius)
                        drawRoundRect(color.copy(alpha = 0.32f), Offset(cl, top), Size(cw, h), radius, style = dashed)
                        return@forEachIndexed
                    }
                    val body = lerp(color, colors.ink, 0.58f)
                    val ink = lerp(color, Color.White, 0.30f)
                    val strip = (h * 0.24f).coerceIn(13.dp.toPx(), 17.dp.toPx())
                    val spans01 = row.spans?.getOrNull(sp.index)
                    if (spans01 != null) {
                        // An audio track: the part's slot pale, each stretch where its take is heard as a clip with its waveform.
                        drawRoundRect(color.copy(alpha = 0.07f), Offset(cl, top), Size(cw, h), radius)
                        spans01.forEach { r ->
                            val a = cl + cw * r.start
                            val b = cl + cw * r.endInclusive
                            val bw = (b - a).coerceAtLeast(1f)
                            if (b < 0f || a > w) return@forEach
                            drawRoundRect(body, Offset(a, top), Size(bw, h), radius)
                            clipRect(left = a, top = top, right = a + bw, bottom = top + strip) { drawRoundRect(color, Offset(a, top), Size(bw, h), radius) }
                            val peaks = row.peaks
                            val bodyTop = top + strip
                            val bodyH = h - strip
                            if (peaks != null && peaks.isNotEmpty() && row.takeSteps > 0) {
                                // The take from where this stretch starts in it.
                                val from = if (row.takeStart >= 0) sp.start + r.start * sp.steps - row.takeStart else 0f
                                drawWave(peaks, row.takeSteps, from, a, bw, stepPx, bodyTop, bodyH, w, ink)
                            } else {
                                drawRect(ink.copy(alpha = 0.35f), Offset(maxOf(a, 0f), bodyTop + bodyH * 0.42f), Size((minOf(b, w) - maxOf(a, 0f)).coerceAtLeast(0f), bodyH * 0.16f))
                            }
                            drawRoundRect(lerp(color, Color.White, 0.2f).copy(alpha = 0.55f), Offset(a, top), Size(bw, h), radius, style = Stroke(1.dp.toPx()))
                        }
                        return@forEachIndexed
                    }
                    // The clip: a darker body in the track's colour, a strip in full colour along its top.
                    drawRoundRect(body, Offset(cl, top), Size(cw, h), radius)
                    clipRect(left = cl, top = top, right = cl + cw, bottom = top + strip) { drawRoundRect(color, Offset(cl, top), Size(cw, h), radius) }
                    // Its name, once, at its start: the scene it plays (« ↪ » when it carries on).
                    val scene = part.sceneFor(row.key)
                    if (cw > 18.dp.toPx()) {
                        val title = (if (part.fromBars(row.key) > 0) "↪ " else "") + p.sceneTitle(scene)
                        val layout = clipLabels.get(title)
                        clipRect(left = cl, top = top, right = cl + cw - 3.dp.toPx(), bottom = top + strip) {
                            drawText(layout, color = colors.ink, topLeft = Offset(cl + 5.dp.toPx(), top + (strip - layout.size.height) / 2f))
                        }
                    }
                    // What it plays, drawn small, repeated over the part (from where it carries on).
                    val bodyTop = top + strip + 3.dp.toPx()
                    val bodyH = (h - strip - 6.dp.toPx()).coerceAtLeast(2f)
                    if (stepPx >= 0.5f) {
                        val preview = previews.getOrPut(scene to row.key) { p.clipPreview(scene, row.key) }
                        if (!preview.isEmpty) {
                            clipRect(left = cl, top = top, right = cl + cw, bottom = top + h) {
                                drawPreview(preview, part.fromBars(row.key) * view.stepsPerBar, sp.steps, left, stepPx, bodyTop, bodyH, w, ink)
                            }
                        }
                    }
                    // The repeats: soft dividers through the body.
                    if (passSteps > 0 && passSteps * stepPx >= 6.dp.toPx()) {
                        var k = passSteps
                        while (k < sp.steps) {
                            val x = left + k * stepPx
                            if (x in 0f..w) drawLine(colors.ink.copy(alpha = 0.55f), Offset(x, top + strip), Offset(x, top + h - 1.dp.toPx()), 1.dp.toPx())
                            k += passSteps
                        }
                    }
                    drawRoundRect(lerp(color, Color.White, 0.2f).copy(alpha = 0.45f), Offset(cl, top), Size(cw, h), radius, style = Stroke(1.dp.toPx()))
                }
            }
            // The song curves: a filled area under a line, a dot on each point written.
            lines.forEachIndexed { li, line ->
                val lane = line.lane ?: return@forEachIndexed
                val curve = curves[lane.index] ?: return@forEachIndexed
                if (curve.isEmpty()) return@forEachIndexed
                val laneObj = p.automation.getOrNull(lane.index) ?: return@forEachIndexed
                val unit = if (laneObj.songPerBeat) beatPx else view.barPx
                val color = lane.key?.let { trackColors[it] } ?: colors.mix
                val top = lineTops[li] + 7.dp.toPx()
                val h = lineHeights[li] - 14.dp.toPx()
                // The middle, as a guide.
                drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, top + h / 2f), Offset(w, top + h / 2f), 1f)
                val from = ((x0 / unit).toInt() - 1).coerceAtLeast(0)
                val to = minOf(curve.size - 1, ((x0 + w) / unit).toInt() + 1)
                if (from > to) return@forEachIndexed
                val line0 = Path()
                val fill = Path()
                for (k in from..to) {
                    val x = k * unit - x0
                    val y = top + h * (1f - curve[k].coerceIn(0f, 1f))
                    if (k == from) { line0.moveTo(x, y); fill.moveTo(x, top + h); fill.lineTo(x, y) } else { line0.lineTo(x, y); fill.lineTo(x, y) }
                }
                // After the last point the curve holds its value to the song's end.
                val endX = if (to == curve.size - 1) minOf(view.bars * view.barPx - x0, w) else to * unit - x0
                val endY = top + h * (1f - curve[to].coerceIn(0f, 1f))
                line0.lineTo(endX, endY)
                fill.lineTo(endX, endY)
                fill.lineTo(endX, top + h)
                fill.close()
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.50f), color.copy(alpha = 0.06f)), startY = top, endY = top + h))
                drawPath(line0, color, style = Stroke(2.dp.toPx()))
                if (unit >= 6.dp.toPx()) {
                    val written = laneObj.songValues
                    for (k in from..minOf(to, written.size - 1)) {
                        if (written[k] == null) continue
                        val c = Offset(k * unit - x0, top + h * (1f - curve[k].coerceIn(0f, 1f)))
                        drawCircle(colors.ink, 4.dp.toPx(), c)
                        drawCircle(color, 2.8.dp.toPx(), c)
                    }
                }
            }
            // Past the song's end: shaded.
            val end = view.bars * view.barPx - x0
            if (end < w) drawRect(Color.Black.copy(alpha = 0.35f), Offset(end.coerceAtLeast(0f), 0f), Size(w - end.coerceAtLeast(0f), size.height))
        }
        // The playhead and the glow of what plays: read at each step, drawn alone on top.
        Spacer(
            Modifier.matchParentSize().graphicsLayer {}.drawBehind {
                val s = ui.songStep()
                if (s < 0) return@drawBehind
                val stepPx = view.barPx / view.stepsPerBar
                val x0 = view.scrollX
                val active = ui.songIndex.takeIf { it in spans.indices }
                if (active != null) {
                    val sp = spans[active]
                    val part = p.song[active]
                    val left = sp.start * stepPx - x0
                    val right = sp.end * stepPx - x0
                    // What has played of the part: lightly washed.
                    val head = (s * stepPx - x0).coerceIn(left, right)
                    drawRect(Color.White.copy(alpha = 0.05f), Offset(left, 0f), Size((head - left).coerceAtLeast(0f), size.height))
                    // A beat pulse in the glow (strong on the beat, settling till the next one).
                    val pulse = 1f - (s % 4) / 4f * 0.55f
                    val padV = CLIP_PAD_V.toPx()
                    val gap = CLIP_GAP.toPx()
                    val r = CLIP_RADIUS.toPx()
                    lines.forEachIndexed { li, line ->
                        val row = line.row ?: return@forEachIndexed
                        if (!part.plays(row.key)) return@forEachIndexed
                        val color = trackColors[row.key] ?: row.color
                        val top = lineTops[li] + padV
                        val h = lineHeights[li] - 2 * padV
                        val o = Offset(left + gap, top)
                        val z = Size((right - left - 2 * gap).coerceAtLeast(1f), h)
                        drawRoundRect(color.copy(alpha = 0.25f * pulse), o - Offset(2.dp.toPx(), 2.dp.toPx()), Size(z.width + 4.dp.toPx(), z.height + 4.dp.toPx()), CornerRadius(r + 2.dp.toPx()), style = Stroke(3.dp.toPx()))
                        drawRoundRect(Color.White.copy(alpha = 0.50f * pulse), o, z, CornerRadius(r), style = Stroke(1.5.dp.toPx()))
                    }
                }
                val x = s * stepPx - x0
                if (x in -6f..size.width + 6f) {
                    drawLine(colors.head.copy(alpha = 0.10f), Offset(x, 0f), Offset(x, size.height), 9.dp.toPx())
                    drawLine(Color.Black.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), 4.dp.toPx())
                    drawLine(colors.head, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                }
            },
        )
        menu?.let { m -> ClipMenuAt(m, ui, actions, close = { menu = null }) }
    }
}

/**
 * [preview] repeated over a part of [partSteps] steps whose clip starts at [left], the track
 * [fromSteps] into its loop, in [ink]: drum hits as ticks on their pad's row, notes as bars at
 * their pitch, the 303 as bars joined by its line.
 */
private fun DrawScope.drawPreview(
    preview: ClipPreview, fromSteps: Int, partSteps: Int, left: Float, stepPx: Float, top: Float, height: Float, width: Float, ink: Color,
) {
    val loop = preview.steps.coerceAtLeast(1)
    // Zoomed far out the marks would be too small to read (and thousands to draw): one soft band instead.
    if (loop * stepPx < 20.dp.toPx()) {
        val x = left.coerceAtLeast(0f)
        val end = minOf(left + partSteps * stepPx, width)
        if (end > x) drawRect(ink.copy(alpha = 0.28f), Offset(x, top + height * 0.36f), Size(end - x, height * 0.28f))
        return
    }
    val drums = preview.kind == ClipPreview.DRUMS
    val acid = preview.kind == ClipPreview.ACID
    val rowGap = height / preview.rows.coerceAtLeast(1)
    val tickW = (stepPx * 0.5f).coerceIn(1.5.dp.toPx(), 3.dp.toPx())
    val tickH = (rowGap * 0.72f).coerceIn(2.dp.toPx(), 12.dp.toPx())
    val bar = (height / 9f).coerceIn(2.dp.toPx(), 5.dp.toPx())
    var base = -(fromSteps % loop)
    var guard = 0
    while (base < partSteps && guard++ < 4096) {
        val loopLeft = left + base * stepPx
        // Loops wholly off the screen are skipped.
        if (loopLeft + loop * stepPx >= 0f && loopLeft <= width) {
            var line: Path? = null
            for (m in preview.marks) {
                val s = base + m.start
                if (s >= partSteps) continue
                val x = left + s.coerceAtLeast(0) * stepPx
                val end = left + minOf(s + m.length, partSteps) * stepPx
                if (end <= x || end < 0f || x > width) continue
                val y = top + m.y * height
                val a = 0.45f + 0.55f * m.strength
                if (drums) {
                    drawRoundRect(ink.copy(alpha = a), Offset(x + 0.5f, y - tickH / 2f), Size(tickW, tickH), CornerRadius(tickW / 2f))
                } else {
                    if (acid && stepPx >= 2.5.dp.toPx()) {
                        val cx = (x + end) / 2f
                        line = (line ?: Path().apply { moveTo(cx, y) }).apply { lineTo(cx, y) }
                    }
                    drawRoundRect(ink.copy(alpha = a), Offset(x + 0.5f, y - bar / 2f), Size((end - x - 1f).coerceAtLeast(1.5f), bar), CornerRadius(bar / 2f))
                }
            }
            line?.let { drawPath(it, ink.copy(alpha = 0.35f), style = Stroke(1.dp.toPx())) }
        }
        base += loop
    }
}

/**
 * An audio take's waveform over a stretch [a]..[a]+[bw] of the grid: [peaks] cover [takeSteps]
 * steps, the stretch starts [fromStep] steps into the take; mirrored around the middle.
 */
private fun DrawScope.drawWave(
    peaks: FloatArray, takeSteps: Int, fromStep: Float, a: Float, bw: Float, stepPx: Float, top: Float, height: Float, width: Float, ink: Color,
) {
    val mid = top + height / 2f
    val half = height / 2f - 1.dp.toPx()
    val col = 2.dp.toPx()
    var x = maxOf(a, 0f)
    val end = minOf(a + bw, width)
    while (x < end) {
        val step = fromStep + (x - a) / stepPx
        val i = (step / takeSteps * peaks.size).toInt()
        if (i >= 0 && i < peaks.size) {
            val amp = peaks[i].coerceIn(0f, 1f) * half
            if (amp > 0.5f) drawLine(ink.copy(alpha = 0.8f), Offset(x, mid - amp), Offset(x, mid + amp), col * 0.7f)
        }
        x += col
    }
}

/** A clip's (or a curve's) actions, at the finger. */
@Composable
private fun ClipMenuAt(m: ClipMenu, ui: TimelineUi, actions: TimelineActions, close: () -> Unit) {
    val p = ui.project
    val part = p.song.getOrNull(m.part)
    if (part == null) {
        // The part went (Annuler, an edit elsewhere): the menu closes.
        LaunchedEffect(Unit) { close() }
        return
    }
    var picking by remember { mutableStateOf<String?>(null) }
    var inserting by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    val sheetUi = partSheetUi(p, m.part)
    val key = m.key
    val full = p.song.size >= SongPart.MAX_PARTS
    val songBar = (p.partSpans.getOrNull(m.part)?.start ?: 0) / p.stepsPerBar
    val cutAt = p.splitBar(m.part, m.bar.coerceAtLeast(1))
    Box(Modifier.offset { IntOffset(m.x.roundToInt(), m.y.roundToInt()) }.size(1.dp)) {
        ScaledDropdownMenu(expanded = picking == null && !inserting && !sheet, onDismissRequest = close) {
            val trackName = ui.rows.firstOrNull { it.key == key }?.name?.let(DefaultNames::shown)
            Text(
                tr("Partie %d", m.part + 1) + " · " + p.sceneTitle(part.scene) + (trackName?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (key != null) {
                DropdownMenuItem(
                    text = { Text(editorName(key)) }, leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = { close(); actions.openEditor(m.part, key) }, modifier = Modifier.testTag("tl_open"),
                )
                val on = part.plays(key)
                DropdownMenuItem(
                    text = { Text(if (on) tr("Couper la piste dans cette partie") else tr("Remettre la piste dans cette partie")) },
                    leadingIcon = { Icon(if (on) Icons.Filled.Remove else Icons.Filled.Add, null) },
                    onClick = { close(); actions.togglePartTrack(m.part, key) }, modifier = Modifier.testTag("tl_toggle"),
                )
                if (!key.startsWith("audio:")) {
                    DropdownMenuItem(
                        text = { Text(tr("Changer de partie…")) }, leadingIcon = { Icon(Icons.Filled.SwapVert, null) },
                        onClick = { picking = key }, modifier = Modifier.testTag("tl_scene"),
                    )
                }
                HorizontalDivider()
            }
            if (part.bars == null) {
                DropdownMenuItem(
                    text = { Text(tr("Une fois de plus (× %d)", part.repeats + 1)) }, leadingIcon = { Icon(Icons.Filled.Add, null) },
                    enabled = part.repeats < SongPart.MAX_REPEATS,
                    onClick = { close(); actions.setRepeats(m.part, part.repeats + 1) }, modifier = Modifier.testTag("tl_more"),
                )
                DropdownMenuItem(
                    text = { Text(tr("Une fois de moins (× %d)", (part.repeats - 1).coerceAtLeast(1))) }, leadingIcon = { Icon(Icons.Filled.Remove, null) },
                    enabled = part.repeats > 1,
                    onClick = { close(); actions.setRepeats(m.part, part.repeats - 1) }, modifier = Modifier.testTag("tl_less"),
                )
            }
            DropdownMenuItem(
                text = { Text(if (cutAt != null) tr("Couper la partie à la mesure %d", songBar + cutAt + 1) else tr("Couper la partie")) },
                leadingIcon = { Icon(Icons.Filled.ContentCut, null) }, enabled = cutAt != null,
                onClick = { close(); if (cutAt != null) actions.splitPart(m.part, cutAt) }, modifier = Modifier.testTag("tl_split"),
            )
            DropdownMenuItem(
                text = { Text(tr("Dupliquer la partie")) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }, enabled = !full,
                onClick = { close(); actions.duplicatePart(m.part) }, modifier = Modifier.testTag("tl_duplicate"),
            )
            DropdownMenuItem(
                text = { Text(tr("Insérer une partie après…")) }, leadingIcon = { Icon(Icons.Filled.Add, null) }, enabled = !full,
                onClick = { inserting = true }, modifier = Modifier.testTag("tl_insert"),
            )
            // The part sheet's intentions, the same list as in the Morceau tab's sheet.
            if (sheetUi != null) {
                HorizontalDivider()
                Text(
                    tr("Transformer la partie"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                partChoices(sheetUi).filter { it.inMenu }.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.label) }, leadingIcon = { Icon(c.icon, null) }, enabled = c.enabled,
                        onClick = { close(); actions.partIntent(m.part, c.intent) }, modifier = Modifier.testTag("tl_${c.tag}"),
                    )
                }
                DropdownMenuItem(
                    text = { Text(tr("Énergie, longueur, plus…")) }, leadingIcon = { Icon(Icons.Filled.Tune, null) },
                    onClick = { sheet = true }, modifier = Modifier.testTag("tl_part_sheet"),
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(tr("Boucler sur cette partie")) }, leadingIcon = { Icon(Icons.Filled.Repeat, null) },
                onClick = { close(); actions.setLoop(m.part..m.part) }, modifier = Modifier.testTag("tl_loop"),
            )
            if (m.lane != null) {
                DropdownMenuItem(
                    text = { Text(tr("Modifier la courbe dans Mix")) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, null) },
                    onClick = { close(); actions.openLane(m.lane) }, modifier = Modifier.testTag("tl_lane"),
                )
            }
            DropdownMenuItem(
                text = { Text(tr("Retirer la partie du morceau")) }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = { close(); actions.removePart(m.part) }, modifier = Modifier.testTag("tl_remove"),
            )
        }
    }
    picking?.let { k ->
        TimelineScenePicker(
            title = tr("Partie %1\$d : ce que joue %2\$s", m.part + 1, ui.rows.firstOrNull { it.key == k }?.name?.let(DefaultNames::shown) ?: k),
            p = p,
            current = part.sceneFor(k),
            onPick = { scene -> actions.setPartScene(m.part, k, scene); close() },
            onDismiss = close,
        )
    }
    if (sheet && sheetUi != null) {
        PartSheet(sheetUi, onIntent = { actions.partIntent(m.part, it) }, close = close)
    }
    if (inserting) {
        TimelineScenePicker(
            title = tr("Insérer après la partie %d", m.part + 1), p = p, current = -1,
            onPick = { scene -> actions.insertPart(m.part + 1, scene); close() },
            onDismiss = close,
        )
    }
}

/** One of the scenes in use, picked (the one [current] marked). */
@Composable
private fun TimelineScenePicker(title: String, p: Project, current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                p.allScenes.indices.forEach { i ->
                    OutlinedButton(
                        onClick = { onPick(i) },
                        border = if (i == current) androidx.compose.foundation.BorderStroke(2.dp, Neon.Cyan) else androidx.compose.foundation.BorderStroke(1.dp, Neon.Outline),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("tl_pick_$i"),
                    ) {
                        Text(p.sceneTitle(i), maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Annuler")) } },
    )
}
