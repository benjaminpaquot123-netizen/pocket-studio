@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.session

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
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
import com.pocketstudio.model.ClipPreview
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.PartSpan
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
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
import com.pocketstudio.model.trackBadge
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.fadingHorizontalScroll
import kotlinx.coroutines.launch
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
)

private val HEADER_W = 128.dp
private val BAND_H = 22.dp
private val BARS_H = 24.dp
private val RULER_H = BAND_H + BARS_H
private val ROW_H = 64.dp
private val LANE_H = 34.dp
private val MIN_BAR = 6.dp
private val MAX_BAR = 220.dp

/** A line of the grid: a track's row, or one of its song curves under it. */
internal class TimelineLine(val row: TimelineRow?, val lane: TimelineLane?) {
    val height: Dp get() = if (row != null) ROW_H else LANE_H
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

/** Zoom (pixels per bar) and horizontal scroll of the timeline, shared by its ruler and its grid. */
@Stable
internal class TimelineView(barPx: Float, scrollX: Float = 0f) {
    var barPx by mutableFloatStateOf(barPx)
    var scrollX by mutableFloatStateOf(scrollX)
    /** The grid's visible width in pixels. */
    var viewport by mutableFloatStateOf(0f)
    /** The song's length in bars (plus room after its end). */
    var bars by mutableIntStateOf(1)

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
 * parts; one row per track (its colour, name, M and S) with a clip per part, drawn with what it
 * plays; the song curves under their track. Scroll sideways, pinch or ± to zoom; the playhead
 * moves as the song plays (drawn, never recomposed) and the screen can follow it.
 *
 * Gestures: a touch on a clip switches the track off or on in that part, a long press opens its
 * actions (open in its editor, choose its scene, repeats, cut, copy, remove). On the ruler: a
 * touch plays from that part; on the parts' band a long press then a slide moves a part; on the
 * bars a long press then a slide loops over parts (the engine moves on at part boundaries).
 */
@Composable
fun TimelineScreen(ui: TimelineUi, actions: TimelineActions, barDp: Dp? = null) {
    val p = ui.project
    var immersive by rememberSaveable { mutableStateOf(false) }
    com.pocketstudio.app.live.HideSystemBars(immersive)
    var showLanes by rememberSaveable { mutableStateOf(true) }
    val follow = rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(Neon.Background).testTag("timeline_screen")) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val density = LocalDensity.current
            val gridWidth = maxWidth - HEADER_W
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
            Column(Modifier.fillMaxSize()) {
                TimelineTopBar(ui, actions, immersive, toggleFull = { immersive = !immersive })
                TimelineTools(
                    ui, view, follow.value, setFollow = { follow.value = it }, showLanes, setLanes = { showLanes = it },
                    hasLanes = ui.lanes.isNotEmpty(), setLoop = actions.setLoop, minPx = minPx, maxPx = maxPx,
                )
                if (p.song.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(tr("Le morceau est vide : ajoute des parties dans Morceau pour les voir ici, piste par piste."), color = Neon.TextSecondary)
                        OutlinedButton(onClick = actions.back) { Text(tr("Revenir au morceau")) }
                    }
                    return@Column
                }
                TimelineBody(ui, actions, view, showLanes, follow, minPx, maxPx, Modifier.fillMaxWidth().weight(1f))
                if (!immersive) {
                    Text(
                        tr("Touche un clip pour couper ou remettre la piste, appui long pour ses actions. Règle : touche pour jouer, appui long et glisse pour déplacer une partie (en haut) ou boucler (en bas)."),
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().background(Neon.Surface).padding(horizontal = 12.dp, vertical = 6.dp).testTag("timeline_hint"),
                    )
                }
            }
        }
    }
}

/** ← Morceau, ▶/■, the clock (time and bar), ↶ ↷, ⛶. */
@Composable
private fun TimelineTopBar(ui: TimelineUi, actions: TimelineActions, immersive: Boolean, toggleFull: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(Neon.Surface).padding(horizontal = 4.dp).testTag("timeline_top_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = actions.back, modifier = Modifier.testTag("timeline_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Revenir au morceau"))
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = actions.togglePlay).testTag("timeline_play")
                .semantics { contentDescription = if (ui.playing) tr("Arrêter") else tr("Jouer le morceau") },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (ui.playing) Neon.Acid else Neon.Cyan), contentAlignment = Alignment.Center) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent)
            }
        }
        TimelineClock(ui, Modifier.weight(1f))
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

/** Suivre, Boucle, Courbes, zoom − + and « Tout voir », on a line that scrolls when narrow. */
@Composable
private fun TimelineTools(
    ui: TimelineUi, view: TimelineView, follow: Boolean, setFollow: (Boolean) -> Unit, showLanes: Boolean, setLanes: (Boolean) -> Unit,
    hasLanes: Boolean, setLoop: (IntRange?) -> Unit, minPx: Float, maxPx: Float,
) {
    val scope = rememberCoroutineScope()
    val p = ui.project
    fun zoomTo(target: Float) {
        val from = view.barPx
        val to = target.coerceIn(minPx, maxPx)
        scope.launch {
            // Smooth: the bars widen around the middle of the screen.
            var last = from
            animate(from, to, animationSpec = tween(220)) { v, _ ->
                view.zoomAround(view.viewport / 2f, v / last, minPx, maxPx)
                last = view.barPx
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().height(48.dp).background(lerp(Neon.Surface, Color.Black, 0.15f)).fadingHorizontalScroll().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ToolPill(tr("Suivre"), follow, "timeline_follow", null) { setFollow(!follow) }
        val loop = ui.loop
        ToolPill(
            if (loop == null) tr("Boucle") else if (loop.first == loop.last) tr("Boucle : partie %d", loop.first + 1) else tr("Boucle : parties %1\$d à %2\$d", loop.first + 1, loop.last + 1),
            loop != null, "timeline_loop", Icons.Filled.Repeat,
        ) {
            if (loop != null) {
                setLoop(null)
            } else {
                // The part playing, or the first one on the screen.
                val part = ui.songIndex.takeIf { it >= 0 }
                    ?: p.partAtStep((view.scrollX / (view.barPx / p.stepsPerBar)).toInt()).takeIf { it >= 0 } ?: 0
                setLoop(songLoopOf(part, part, p.song.size))
            }
        }
        if (hasLanes) ToolPill(tr("Courbes"), showLanes, "timeline_lanes", Icons.AutoMirrored.Filled.ShowChart) { setLanes(!showLanes) }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { zoomTo(view.barPx / 1.6f) }, modifier = Modifier.testTag("timeline_zoom_out")) {
            Icon(Icons.Filled.ZoomOut, contentDescription = tr("Dézoomer"))
        }
        IconButton(onClick = { zoomTo(view.barPx * 1.6f) }, modifier = Modifier.testTag("timeline_zoom_in")) {
            Icon(Icons.Filled.ZoomIn, contentDescription = tr("Zoomer"))
        }
        IconButton(
            onClick = {
                // The whole song on the screen.
                val target = (view.viewport / (p.songBars.coerceAtLeast(1) + 1)).coerceIn(minPx, maxPx)
                scope.launch {
                    val from = view.barPx
                    val fromX = view.scrollX
                    animate(0f, 1f, animationSpec = tween(250)) { f, _ ->
                        view.barPx = from + (target - from) * f
                        view.scrollTo(fromX * (1f - f))
                    }
                }
            },
            modifier = Modifier.testTag("timeline_fit"),
        ) {
            Icon(Icons.Filled.ZoomOutMap, contentDescription = tr("Voir tout le morceau"))
        }
    }
}

/** A switch of the tool line: lit in the accent when on. */
@Composable
private fun ToolPill(label: String, on: Boolean, tag: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, toggle: () -> Unit) {
    Row(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (on) Neon.Cyan.copy(alpha = 0.22f) else Neon.SurfaceHigh.copy(alpha = 0.6f))
            .clickable(onClick = toggle).padding(horizontal = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (on) Neon.Cyan else Neon.TextSecondary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Neon.Cyan else Neon.TextPrimary, maxLines = 1)
    }
}

/** The colours the timeline draws with, read once from the theme. */
private class TimelinePalette(
    val grid: Color, val rowA: Color, val rowB: Color, val lane: Color, val barLine: Color, val beatLine: Color, val partLine: Color,
    val band: Color, val playing: Color, val next: Color, val loop: Color, val text: Color, val textDim: Color, val head: Color, val mix: Color,
)

@Composable
private fun timelinePalette(): TimelinePalette {
    val bg = Neon.Background
    return TimelinePalette(
        grid = lerp(bg, Color.Black, 0.35f),
        rowA = lerp(bg, Color.Black, 0.22f),
        rowB = lerp(bg, Color.Black, 0.30f),
        lane = lerp(bg, Color.Black, 0.45f),
        barLine = Color.White.copy(alpha = 0.10f),
        beatLine = Color.White.copy(alpha = 0.04f),
        partLine = Color.White.copy(alpha = 0.22f),
        band = Neon.SurfaceHigh,
        playing = Neon.Magenta,
        next = Neon.Amber,
        loop = Neon.Amber,
        text = Neon.TextPrimary,
        textDim = Neon.TextSecondary,
        head = Neon.TextPrimary,
        mix = Neon.Cyan,
    )
}

/** The ruler and the rows: the header column stays, the grid scrolls sideways under the ruler. */
@Composable
private fun TimelineBody(
    ui: TimelineUi, actions: TimelineActions, view: TimelineView, showLanes: Boolean,
    follow: androidx.compose.runtime.MutableState<Boolean>, minPx: Float, maxPx: Float, modifier: Modifier,
) {
    val p = ui.project
    val spans = remember(p) { p.partSpans }
    val lines = remember(ui.rows, ui.lanes, showLanes) { timelineLines(ui.rows, ui.lanes, showLanes) }
    val colors = timelinePalette()
    val density = LocalDensity.current
    val lineHeights = remember(lines, density) { FloatArray(lines.size) { with(density) { lines[it].height.toPx() } } }
    val lineTops = remember(lineHeights) { FloatArray(lineHeights.size).also { t -> for (i in 1 until t.size) t[i] = t[i - 1] + lineHeights[i - 1] } }
    val totalHeight = lines.fold(0.dp) { a, l -> a + l.height }
    // A finger moving the grid sideways stops the screen from following the playhead.
    val scrollable = rememberScrollableState { delta ->
        val before = view.scrollX
        view.scrollTo(before - delta)
        if (view.scrollX != before && follow.value) follow.value = false
        before - view.scrollX
    }
    // Following the playhead: when it nears the right edge (or is off screen), the grid glides on.
    val songStep by rememberUpdatedState(ui.songStep)
    LaunchedEffect(follow.value, ui.playing) {
        if (!follow.value || !ui.playing) return@LaunchedEffect
        snapshotFlow { songStep() }.collect { s ->
            if (s < 0) return@collect
            val x = s * view.barPx / p.stepsPerBar
            val w = view.viewport
            if (x < view.scrollX + w * 0.05f || x > view.scrollX + w * 0.8f) {
                val to = (x - w * 0.2f).coerceIn(0f, view.maxScroll())
                animate(view.scrollX, to, animationSpec = tween(300)) { v, _ -> view.scrollX = v }
            }
        }
    }
    val pinch = Modifier.pointerInput(view) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } >= 2) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) {
                        view.zoomAround(event.calculateCentroid(useCurrent = true).x, zoom, minPx, maxPx)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(RULER_H)) {
            RulerCorner()
            TimelineRuler(ui, actions, view, spans, colors, Modifier.weight(1f).fillMaxHeight().scrollable(scrollable, Orientation.Horizontal), pinch)
        }
        Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.width(HEADER_W)) {
                    lines.forEach { line ->
                        if (line.row != null) TrackHeader(p, line.row, actions) else if (line.lane != null) LaneHeader(line.lane, ui.rows, actions)
                    }
                }
                TimelineGrid(
                    ui, actions, view, spans, lines, lineTops, lineHeights, colors,
                    Modifier.weight(1f).height(totalHeight).scrollable(scrollable, Orientation.Horizontal), pinch,
                )
            }
        }
    }
}

/** The ruler's corner: what its two lines are. */
@Composable
private fun RulerCorner() {
    Column(Modifier.width(HEADER_W).fillMaxHeight().background(Neon.Surface).padding(start = 12.dp)) {
        Box(Modifier.height(BAND_H), contentAlignment = Alignment.CenterStart) {
            Text(tr("Parties"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
        }
        Box(Modifier.height(BARS_H), contentAlignment = Alignment.CenterStart) {
            Text(tr("Mesures"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
        }
    }
}

/** A track's header: its colour, its name, M and S (its mixer channel). */
@Composable
private fun TrackHeader(p: Project, row: TimelineRow, actions: TimelineActions) {
    val ch = channelOfKey(row.key)
    val channel = ch?.let { p.mixer.channels.getOrNull(it) }
    Row(
        Modifier.width(HEADER_W).height(ROW_H).background(lerp(Neon.Surface, Color.Black, 0.1f)).testTag("timeline_header_${row.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(row.color, lerp(row.color, Color.Black, 0.4f)))))
        Column(Modifier.weight(1f).padding(start = 8.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                DefaultNames.shown(row.name), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, color = Neon.TextPrimary,
            )
            if (ch != null && channel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuteSolo("M", channel.mute, Neon.Amber, tr("Couper la piste %s", DefaultNames.shown(row.name)), "timeline_mute_${row.key}") { actions.toggleMute(ch) }
                    MuteSolo("S", channel.solo, Neon.Cyan, tr("Écouter la piste %s seule", DefaultNames.shown(row.name)), "timeline_solo_${row.key}") { actions.toggleSolo(ch) }
                }
            }
        }
    }
}

@Composable
private fun MuteSolo(letter: String, on: Boolean, color: Color, description: String, tag: String, toggle: () -> Unit) {
    Box(
        Modifier.size(width = 40.dp, height = 28.dp).clip(RoundedCornerShape(6.dp))
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
private fun LaneHeader(lane: TimelineLane, rows: List<TimelineRow>, actions: TimelineActions) {
    val color = rows.firstOrNull { it.key == lane.key }?.color ?: Neon.Cyan
    Row(
        Modifier.width(HEADER_W).height(LANE_H).background(lerp(Neon.Surface, Color.Black, 0.3f))
            .clickable(onClickLabel = tr("Modifier la courbe dans le Mixeur")) { actions.openLane(lane.index) }
            .padding(start = 12.dp, end = 4.dp).testTag("timeline_lane_${lane.index}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(lane.label, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
    }
}

/** Measured texts kept between frames (the same few labels are drawn again and again). */
private class LabelCache(private val measure: (String) -> TextLayoutResult) {
    private val cache = HashMap<String, TextLayoutResult>()
    fun get(text: String): TextLayoutResult = cache.getOrPut(text) { if (cache.size > 400) cache.clear(); measure(text) }
}

/** Bars between two numbers on the ruler so they never crowd: 1, 2, 4, 8… */
private fun numberEvery(barPx: Float, minGap: Float): Int {
    var n = 1
    while (n * barPx < minGap && n < 256) n *= 2
    return n
}

/**
 * The ruler: the song's parts (the one playing lit, the next one outlined, the loop in amber)
 * over the bar numbers, and the playhead's head.
 */
@Composable
private fun TimelineRuler(
    ui: TimelineUi, actions: TimelineActions, view: TimelineView, spans: List<PartSpan>, colors: TimelinePalette,
    modifier: Modifier, pinch: Modifier,
) {
    val p = ui.project
    val measurer = rememberTextMeasurer()
    val bandStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    val numberStyle = MonoStyle.copy(fontSize = 11.sp)
    val bandLabels = remember(measurer, bandStyle) { LabelCache { measurer.measure(it, bandStyle, maxLines = 1) } }
    val numbers = remember(measurer, numberStyle) { LabelCache { measurer.measure(it, numberStyle, maxLines = 1) } }
    val haptic = LocalHapticFeedback.current
    val latestUi by rememberUpdatedState(ui)
    val latestActions by rememberUpdatedState(actions)
    // A slide after a long press: moving part [dragFrom] (on the band) or looping from [loopFrom] (on the bars).
    var dragFrom by remember { mutableIntStateOf(-1) }
    var loopFrom by remember { mutableIntStateOf(-1) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var longPressed by remember { mutableStateOf(false) }
    val bandPx = with(LocalDensity.current) { BAND_H.toPx() }
    fun partAt(x: Float): Int {
        val stepPx = view.barPx / latestUi.project.stepsPerBar
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { longPressed = false },
                    onTap = { at ->
                        if (longPressed) return@detectTapGestures
                        // A touch on the ruler plays from the part there (the song moves on at part boundaries).
                        val stepPx = view.barPx / latestUi.project.stepsPerBar
                        val i = latestUi.project.partAtStep(((view.scrollX + at.x) / stepPx).toInt())
                        if (i >= 0) latestActions.playPart(i)
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { at ->
                        longPressed = true
                        val i = partAt(at.x)
                        if (i < 0) return@detectDragGesturesAfterLongPress
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragX = at.x
                        if (at.y < bandPx) dragFrom = i else loopFrom = i
                    },
                    onDrag = { change, amount -> change.consume(); dragX += amount.x },
                    onDragEnd = {
                        val to = partAt(dragX)
                        if (dragFrom >= 0 && to >= 0 && to != dragFrom) latestActions.movePart(dragFrom, to)
                        if (loopFrom >= 0 && to >= 0) latestActions.setLoop(songLoopOf(loopFrom, to, latestUi.project.song.size))
                        dragFrom = -1
                        loopFrom = -1
                    },
                    onDragCancel = { dragFrom = -1; loopFrom = -1 },
                )
            }
            .then(pinch)
            .testTag("timeline_ruler")
            .semantics { contentDescription = tr("Règle des mesures : touche pour jouer à partir d'une partie") },
    ) {
        val stepPx = view.barPx / p.stepsPerBar
        val x0 = view.scrollX
        val w = size.width
        drawRect(colors.grid)
        clipRect {
            // The parts' band.
            val bandH = bandPx
            val gap = 1.dp.toPx()
            spans.forEach { sp ->
                val left = sp.start * stepPx - x0
                val right = sp.end * stepPx - x0
                if (right < 0f || left > w) return@forEach
                val i = sp.index
                val part = p.song[i]
                val fill = when {
                    i == ui.songIndex -> colors.playing.copy(alpha = 0.55f)
                    i == dragFrom -> colors.text.copy(alpha = 0.35f)
                    else -> colors.band
                }
                drawRoundRect(fill, Offset(left + gap, 2.dp.toPx()), Size((right - left - 2 * gap).coerceAtLeast(1f), bandH - 4.dp.toPx()), CornerRadius(4.dp.toPx()))
                if (i == ui.upcoming) {
                    drawRoundRect(colors.next, Offset(left + gap, 2.dp.toPx()), Size((right - left - 2 * gap).coerceAtLeast(1f), bandH - 4.dp.toPx()), CornerRadius(4.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                }
                val title = p.sceneTitle(part.scene) + (if (part.bars == null && part.repeats > 1) " ×${part.repeats}" else "") + (if (part.mixed) " ≈" else "")
                val layout = bandLabels.get(title)
                val room = right - left - 8.dp.toPx()
                if (room > 10.dp.toPx()) {
                    clipRect(left = left + 4.dp.toPx(), right = right - 4.dp.toPx()) {
                        drawText(layout, color = colors.text, topLeft = Offset(maxOf(left, 0f) + 5.dp.toPx(), (bandH - layout.size.height) / 2f))
                    }
                }
            }
            // The bar numbers and their ticks.
            val every = numberEvery(view.barPx, 30.dp.toPx())
            val first = (x0 / view.barPx).toInt().coerceAtLeast(0)
            val last = ((x0 + w) / view.barPx).toInt() + 1
            for (b in first..minOf(last, view.bars + 1)) {
                val x = b * view.barPx - x0
                val numbered = b % every == 0
                drawLine(if (numbered) colors.textDim else colors.barLine, Offset(x, if (numbered) bandH else size.height - 6.dp.toPx()), Offset(x, size.height), 1.dp.toPx())
                if (numbered && b < view.bars) {
                    drawText(numbers.get("${b + 1}"), color = colors.textDim, topLeft = Offset(x + 3.dp.toPx(), bandH + 3.dp.toPx()))
                }
            }
            // The loop (or the loop being chosen): amber over both lines.
            val chosen = if (loopFrom >= 0) songLoopOf(loopFrom, partAt(dragX), p.song.size) else ui.loop
            if (chosen != null && chosen.last < spans.size) {
                val l = spans[chosen.first].start * stepPx - x0
                val r = spans[chosen.last].end * stepPx - x0
                drawRect(colors.loop.copy(alpha = 0.16f), Offset(l, 0f), Size(r - l, size.height))
                drawRect(colors.loop, Offset(l, size.height - 3.dp.toPx()), Size(r - l, 3.dp.toPx()))
            }
            // Where a part being moved goes.
            if (dragFrom >= 0) {
                val to = partAt(dragX)
                if (to >= 0 && to != dragFrom) {
                    val x = (if (to > dragFrom) spans[to].end else spans[to].start) * stepPx - x0
                    drawRect(colors.next, Offset(x - 1.5.dp.toPx(), 0f), Size(3.dp.toPx(), size.height))
                }
            }
            // The playhead's head.
            val s = ui.songStep()
            if (s >= 0) {
                val x = s * stepPx - x0
                val head = Path().apply {
                    moveTo(x - 6.dp.toPx(), bandH)
                    lineTo(x + 6.dp.toPx(), bandH)
                    lineTo(x, bandH + 9.dp.toPx())
                    close()
                }
                drawPath(head, colors.head)
                drawLine(colors.head, Offset(x, bandH), Offset(x, size.height), 2.dp.toPx())
            }
        }
    }
}

/**
 * The grid: lines of bars and beats, a clip per part and track (its colour in a gradient, what it
 * plays drawn small, a muted one hollow), the song curves; over it, drawn from the playing step
 * alone, the playhead and the clips playing now in a soft glow.
 */
@Composable
private fun TimelineGrid(
    ui: TimelineUi, actions: TimelineActions, view: TimelineView, spans: List<PartSpan>, lines: List<TimelineLine>,
    lineTops: FloatArray, lineHeights: FloatArray, colors: TimelinePalette, modifier: Modifier, pinch: Modifier,
) {
    val p = ui.project
    val measurer = rememberTextMeasurer()
    val clipStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val clipLabels = remember(measurer) { LabelCache { measurer.measure(it, clipStyle, maxLines = 1) } }
    // What each track plays in each scene, worked out once per version of the project.
    val previews = remember(p) { HashMap<Pair<Int, String>, ClipPreview>() }
    // The song curves as the engine plays them, a value per point.
    val curves = remember(p.automation, p.songBars, p.beatsPerBar) {
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
        return timelineHit(at.x, at.y, view.scrollX, view.barPx / pr.stepsPerBar, pr.stepsPerBar, pr.partSpans, latestLines, latestTops, latestHeights)
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
                        onLongPress = { at ->
                            val h = hit(at) ?: return@detectTapGestures
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menu = ClipMenu(h.part, h.key, h.lane, h.bar, at.x, at.y)
                        },
                    )
                }
                .then(pinch)
                .testTag("timeline_grid")
                .semantics { contentDescription = tr("Ligne du temps : %1\$d parties, %2\$d pistes", p.song.size, ui.rows.size) },
        ) {
            val stepPx = view.barPx / p.stepsPerBar
            val x0 = view.scrollX
            val w = size.width
            drawRect(colors.grid)
            // Line backgrounds.
            var trackRow = 0
            lines.forEachIndexed { li, line ->
                val c = if (line.row != null) (if (trackRow++ % 2 == 0) colors.rowA else colors.rowB) else colors.lane
                drawRect(c, Offset(0f, lineTops[li]), Size(w, lineHeights[li]))
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
                drawLine(if (b % 4 == 0) colors.partLine.copy(alpha = 0.14f) else colors.barLine, Offset(x, 0f), Offset(x, size.height), 1f)
            }
            spans.forEach { sp ->
                val x = sp.start * stepPx - x0
                if (x in -2f..w + 2f) drawLine(colors.partLine, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
            // Clips.
            val inset = 5.dp.toPx()
            val gap = 1.5.dp.toPx()
            val radius = CornerRadius(6.dp.toPx())
            val titleH = 15.dp.toPx()
            spans.forEach { sp ->
                val left = sp.start * stepPx - x0
                val right = sp.end * stepPx - x0
                if (right < 0f || left > w) return@forEach
                val part = p.song[sp.index]
                lines.forEachIndexed { li, line ->
                    val row = line.row ?: return@forEachIndexed
                    val top = lineTops[li] + inset
                    val h = lineHeights[li] - 2 * inset
                    val cl = left + gap
                    val cw = (right - left - 2 * gap).coerceAtLeast(1f)
                    val on = part.plays(row.key)
                    val color = row.color
                    if (!on) {
                        // Silent here: a hollow outline, what it would play barely seen.
                        drawRoundRect(color.copy(alpha = 0.07f), Offset(cl, top), Size(cw, h), radius)
                        drawRoundRect(color.copy(alpha = 0.30f), Offset(cl, top), Size(cw, h), radius, style = Stroke(1.dp.toPx()))
                        return@forEachIndexed
                    }
                    val spans01 = row.spans?.getOrNull(sp.index)
                    if (spans01 != null) {
                        // An audio track: pale where its take is not heard, its takes in full colour.
                        drawRoundRect(color.copy(alpha = 0.16f), Offset(cl, top), Size(cw, h), radius)
                        spans01.forEach { r ->
                            val a = cl + cw * r.start
                            val b = cl + cw * r.endInclusive
                            drawRoundRect(
                                Brush.verticalGradient(listOf(color.copy(alpha = 0.95f), lerp(color, Color.Black, 0.35f)), startY = top, endY = top + h),
                                Offset(a, top), Size((b - a).coerceAtLeast(1f), h), radius,
                            )
                        }
                        drawRoundRect(color.copy(alpha = 0.6f), Offset(cl, top), Size(cw, h), radius, style = Stroke(1.dp.toPx()))
                        return@forEachIndexed
                    }
                    // The clip: the track's colour, lighter at the top, a bright edge and a darker title band.
                    drawRoundRect(
                        Brush.verticalGradient(listOf(lerp(color, Color.White, 0.08f), lerp(color, Color.Black, 0.38f)), startY = top, endY = top + h),
                        Offset(cl, top), Size(cw, h), radius,
                    )
                    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(cl, top), Size(cw, titleH), radius)
                    drawLine(Color.White.copy(alpha = 0.28f), Offset(cl + radius.x, top + 0.5f), Offset(cl + cw - radius.x, top + 0.5f), 1f)
                    // Its title: the scene it plays (« ↪ » when it carries on).
                    val scene = part.sceneFor(row.key)
                    val badge = part.trackBadge(row.key)
                    val title = if (badge.isEmpty()) Scene.NAMES.getOrElse(scene) { "?" } else badge
                    if (cw > 16.dp.toPx()) {
                        val layout = clipLabels.get(title)
                        clipRect(left = cl, top = top, right = cl + cw, bottom = top + titleH) {
                            drawText(layout, color = Color.White.copy(alpha = 0.92f), topLeft = Offset(maxOf(cl, 0f) + 4.dp.toPx(), top + (titleH - layout.size.height) / 2f))
                        }
                    }
                    // What it plays, drawn small, repeated over the part (from where it carries on).
                    if (stepPx >= 1f) {
                        val preview = previews.getOrPut(scene to row.key) { p.clipPreview(scene, row.key) }
                        if (!preview.isEmpty) {
                            val ct = top + titleH + 2.dp.toPx()
                            val chh = (h - titleH - 4.dp.toPx()).coerceAtLeast(2f)
                            clipRect(left = cl, top = top, right = cl + cw, bottom = top + h) {
                                drawPreview(preview, part.fromBars(row.key) * p.stepsPerBar, sp.steps, left, stepPx, ct, chh, w)
                            }
                        }
                    }
                }
            }
            // The song curves.
            lines.forEachIndexed { li, line ->
                val lane = line.lane ?: return@forEachIndexed
                val curve = curves[lane.index] ?: return@forEachIndexed
                if (curve.isEmpty()) return@forEachIndexed
                val laneObj = p.automation.getOrNull(lane.index) ?: return@forEachIndexed
                val unit = if (laneObj.songPerBeat) beatPx else view.barPx
                val color = ui.rows.firstOrNull { it.key == lane.key }?.color ?: colors.mix
                val top = lineTops[li] + 4.dp.toPx()
                val h = lineHeights[li] - 8.dp.toPx()
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
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f)), startY = top, endY = top + h))
                drawPath(line0, color, style = Stroke(1.5.dp.toPx()))
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
                val stepPx = view.barPx / p.stepsPerBar
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
                    val inset = 5.dp.toPx()
                    val gap = 1.5.dp.toPx()
                    val r = 6.dp.toPx()
                    lines.forEachIndexed { li, line ->
                        val row = line.row ?: return@forEachIndexed
                        if (!part.plays(row.key)) return@forEachIndexed
                        val top = lineTops[li] + inset
                        val h = lineHeights[li] - 2 * inset
                        val o = Offset(left + gap, top)
                        val z = Size((right - left - 2 * gap).coerceAtLeast(1f), h)
                        drawRoundRect(row.color.copy(alpha = 0.22f * pulse), o - Offset(3.dp.toPx(), 3.dp.toPx()), Size(z.width + 6.dp.toPx(), z.height + 6.dp.toPx()), CornerRadius(r + 3.dp.toPx()), style = Stroke(5.dp.toPx()))
                        drawRoundRect(Color.White.copy(alpha = 0.55f * pulse), o, z, CornerRadius(r), style = Stroke(1.5.dp.toPx()))
                    }
                }
                val x = s * stepPx - x0
                if (x in -4f..size.width + 4f) {
                    drawLine(colors.head.copy(alpha = 0.18f), Offset(x, 0f), Offset(x, size.height), 7.dp.toPx())
                    drawLine(colors.head, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                }
            },
        )
        menu?.let { m -> ClipMenuAt(m, ui, actions, close = { menu = null }) }
    }
}

/** [preview] repeated over a part of [partSteps] steps whose clip starts at [left], the track [fromSteps] into its loop. */
private fun DrawScope.drawPreview(preview: ClipPreview, fromSteps: Int, partSteps: Int, left: Float, stepPx: Float, top: Float, height: Float, width: Float) {
    val loop = preview.steps.coerceAtLeast(1)
    val ink = Color.Black.copy(alpha = 0.55f)
    val bar = (height / 9f).coerceIn(2f, 4.dp.toPx())
    // Zoomed far out the marks would be too small to read (and thousands to draw): one soft band instead.
    if (loop * stepPx < 24.dp.toPx()) {
        val x = left.coerceAtLeast(0f)
        val end = minOf(left + partSteps * stepPx, width)
        if (end > x) drawRect(ink.copy(alpha = 0.25f), Offset(x, top + height * 0.35f), Size(end - x, height * 0.3f))
        return
    }
    var base = -(fromSteps % loop)
    var guard = 0
    while (base < partSteps && guard++ < 4096) {
        val loopLeft = left + base * stepPx
        // Loops wholly off the screen are skipped.
        if (loopLeft + loop * stepPx >= 0f && loopLeft <= width) {
            for (m in preview.marks) {
                val s = base + m.start
                if (s >= partSteps) continue
                val x = left + s.coerceAtLeast(0) * stepPx
                val end = left + minOf(s + m.length, partSteps) * stepPx
                if (end <= x || end < 0f || x > width) continue
                val y = top + m.y * height - bar / 2f
                drawRoundRect(ink.copy(alpha = 0.3f + 0.4f * m.strength), Offset(x + 0.5f, y), Size((end - x - 1f).coerceAtLeast(1.5f), bar), CornerRadius(bar / 2f))
            }
        }
        base += loop
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
    val key = m.key
    val full = p.song.size >= SongPart.MAX_PARTS
    val songBar = (p.partSpans.getOrNull(m.part)?.start ?: 0) / p.stepsPerBar
    val cutAt = p.splitBar(m.part, m.bar.coerceAtLeast(1))
    Box(Modifier.offset { IntOffset(m.x.roundToInt(), m.y.roundToInt()) }.size(1.dp)) {
        DropdownMenu(expanded = picking == null && !inserting, onDismissRequest = close) {
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
                        text = { Text(tr("Changer de scène…")) }, leadingIcon = { Icon(Icons.Filled.SwapVert, null) },
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
                text = { Text(tr("Insérer une scène après…")) }, leadingIcon = { Icon(Icons.Filled.Add, null) }, enabled = !full,
                onClick = { inserting = true }, modifier = Modifier.testTag("tl_insert"),
            )
            DropdownMenuItem(
                text = { Text(tr("Boucler sur cette partie")) }, leadingIcon = { Icon(Icons.Filled.Repeat, null) },
                onClick = { close(); actions.setLoop(m.part..m.part) }, modifier = Modifier.testTag("tl_loop"),
            )
            if (m.lane != null) {
                DropdownMenuItem(
                    text = { Text(tr("Modifier la courbe dans le Mixeur")) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, null) },
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
            title = tr("Partie %1\$d : la scène de %2\$s", m.part + 1, ui.rows.firstOrNull { it.key == k }?.name?.let(DefaultNames::shown) ?: k),
            p = p,
            current = part.sceneFor(k),
            onPick = { scene -> actions.setPartScene(m.part, k, scene); close() },
            onDismiss = close,
        )
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
    AlertDialog(
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
