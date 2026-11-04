package com.pocketstudio.app.remix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import kotlin.math.max
import kotlin.math.min

/*
 * Lot C (n° 24): the curves of « Découper la chanson » (mockup docs/maquettes/decoupe-deux.png):
 * the whole stem small with the part shown framed, then that part big, each piece in its colour
 * with its number, the cut lines and their handles, the orange play head; a cut moved by a finger
 * shows a magnifier with its time and whether it sits on the beat. « Un bout » (Benjamin,
 * 2026-11-04: « propre et sans fioriture ») draws none of that: the stem, the one piece lit in the
 * stem's colour between its two bars « Début » and « Fin » (big handles), the rest dimmed.
 */

/** The pieces' colours, in turn (1 green, 2 blue, 3 purple, 4 orange, 5 yellow, 6 indigo). */
val PIECE_COLORS = listOf(
    Color(0xFF8BC34A), Color(0xFF29B6F6), Color(0xFFAB47BC), Color(0xFFFF7043), Color(0xFFFDD835), Color(0xFF7986CB),
)

fun pieceColor(i: Int): Color = PIECE_COLORS[((i % PIECE_COLORS.size) + PIECE_COLORS.size) % PIECE_COLORS.size]

/** The piece of [pieces] (in time order) holding [frame], -1 when none: a binary search, no allocation. */
internal fun pieceIndexAt(pieces: List<CutPiece>, frame: Long): Int {
    var lo = 0
    var hi = pieces.size - 1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        val p = pieces[mid]
        when {
            frame < p.start -> hi = mid - 1
            frame >= p.end -> lo = mid + 1
            else -> return mid
        }
    }
    return -1
}

/** The whole stem, small, the part shown framed: a touch or a slide brings that part of the song into view. */
@Composable
internal fun CutOverview(model: CutModel, view: StemBoardView, levels: BoardPeaks, modifier: Modifier) {
    val muted = Neon.TextSecondary.copy(alpha = 0.55f)
    val accent = Neon.Cyan
    val lane = Neon.SurfaceHigh.copy(alpha = 0.55f)
    Spacer(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .testTag("cut_overview")
            .pointerInput(view) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun center(x: Float) {
                        if (size.width <= 0 || view.framesPerPx <= 0.0) return
                        val frame = x / size.width * view.frames
                        val wanted = frame - view.width * view.framesPerPx / 2
                        view.scrollBy(-((wanted - view.start) / view.framesPerPx).toFloat())
                    }
                    center(down.position.x)
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        if (c.positionChanged()) center(c.position.x)
                        c.consume()
                    }
                }
            }
            .drawWithCache {
                val columns = max(1, (size.width / max(1f, density)).toInt())
                val heights = FloatArray(columns)
                val column = size.width / columns
                onDrawBehind {
                    drawRect(lane)
                    val stem = model.stem
                    val peaks = if (levels.ready > stem) levels.of(stem) else null
                    val mid = size.height / 2
                    if (peaks != null) {
                        peaks.fill(0L, peaks.frames, heights)
                        val scale = levels.scale(stem)
                        for (c in 0 until columns) {
                            val h = max(0.5f, min(mid, heights[c] * scale * mid))
                            drawLine(muted, Offset(c * column, mid - h), Offset(c * column, mid + h), strokeWidth = max(1f, column * 0.7f))
                        }
                    }
                    // « Un bout », in the stem's colour.
                    val sp = if (model.single) model.span else null
                    if (sp != null && view.frames > 0) {
                        val a = (sp.start.toDouble() / view.frames * size.width).toFloat()
                        val z = (sp.end.toDouble() / view.frames * size.width).toFloat()
                        drawRect(cutStemColor(stem).copy(alpha = 0.7f), Offset(a, 0f), Size(max(2f, z - a), size.height))
                    }
                    // The part shown, framed and lit.
                    if (view.frames > 0 && view.framesPerPx > 0) {
                        val x0 = (view.start / view.frames * size.width).toFloat()
                        val x1 = ((view.start + view.width * view.framesPerPx) / view.frames * size.width).toFloat()
                        drawRect(accent.copy(alpha = 0.18f), Offset(x0, 0f), Size(max(2f, x1 - x0), size.height))
                        drawRect(accent, Offset(x0, 0f), Size(max(2f, x1 - x0), size.height), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
            },
    )
}

/**
 * The part of the stem shown, big: each piece in its colour with its number on top, the cut lines
 * and their handles below, the orange play head ([CutModel.cursor]) and the green one of what is
 * heard. A touch moves the play head; a finger on a cut line moves it (magnet, magnifier); a slide
 * elsewhere scrolls; two fingers zoom.
 */
@Composable
internal fun CutZoom(
    model: CutModel,
    view: StemBoardView,
    levels: BoardPeaks,
    work: State<StemBoardState>,
    onTouched: () -> Unit,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val lane = Neon.SurfaceHigh.copy(alpha = 0.55f)
    val grey = Neon.TextSecondary.copy(alpha = 0.35f)
    val head = Neon.Cyan
    val play = Neon.Acid
    val numberStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Box(modifier) {
        Spacer(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(10.dp))
                .testTag("cut_wave")
                .onSizeChanged { view.layout(it.width.toFloat()) }
                .pointerInput(model, view) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slop = viewConfiguration.touchSlop
                        // A small zone, and only a line standing alone in it (see [CutModel.beginMove]):
                        // the whole song cut every bar leaves the touches to the play head and the pinch.
                        val grab = ((if (model.single) SPAN_GRAB else CUT_GRAB).toPx() * view.framesPerPx).toLong()
                        val grabbing = model.beginMove(view.frameAt(down.position.x), grab)
                        if (grabbing) onTouched()
                        var moved = false
                        try {
                            while (true) {
                                val e = awaitPointerEvent()
                                if (e.changes.count { it.pressed } > 1) {
                                    // A second finger: the pinch wins, even over a cut being moved.
                                    model.endMove()
                                    zoomAndScroll(view)
                                    return@awaitEachGesture
                                }
                                val c = e.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                                if (!c.pressed) break
                                if (!moved && (c.position - down.position).getDistance() > slop) moved = true
                                if (moved) {
                                    if (grabbing) {
                                        if (c.positionChanged()) model.moveTo(view.frameAt(c.position.x))
                                    } else {
                                        val dx = c.position.x - c.previousPosition.x
                                        if (dx != 0f) view.scrollBy(dx)
                                    }
                                    c.consume()
                                }
                            }
                        } finally {
                            model.endMove()
                        }
                        if (!moved) {
                            onTouched()
                            model.cursor = view.frameAt(down.position.x)
                        }
                    }
                }
                .drawWithCache {
                    val column = max(1f, density)
                    val columns = (size.width / column).toInt() + 2
                    val heights = FloatArray(columns)
                    val labels = arrayOfNulls<TextLayoutResult>(com.pocketstudio.model.Cuts.MAX_PIECES + 1)
                    fun label(n: Int): TextLayoutResult? = if (n !in labels.indices) null
                    else labels[n] ?: measurer.measure(n.toString(), numberStyle).also { labels[n] = it }
                    val margin = 26.dp.toPx()
                    val bottom = 22.dp.toPx()
                    val lineW = 2.dp.toPx()
                    val knob = 7.dp.toPx()
                    val badge = 10.dp.toPx()
                    val tri = Path()
                    val barW = 3.dp.toPx()
                    val handleW = 16.dp.toPx()
                    val handleH = 44.dp.toPx()
                    val edge = 6.dp.toPx()
                    onDrawBehind {
                        drawRect(lane)
                        val fpp = view.framesPerPx
                        if (fpp <= 0.0) return@onDrawBehind
                        val pieces = model.pieces
                        val stem = model.stem
                        if (model.single) {
                            // « Un bout »: the stem, its one piece lit between the two bars, the rest dimmed.
                            val sp = model.span
                            val color = cutStemColor(stem)
                            val a = sp?.let { view.x(it.start) } ?: 0f
                            val z = sp?.let { view.x(it.end) } ?: 0f
                            if (sp != null && z > 0f && a < size.width) {
                                drawRect(color.copy(alpha = 0.14f), Offset(max(0f, a), 0f), Size(min(size.width, z) - max(0f, a), size.height))
                            }
                            val peaks = if (levels.ready > stem) levels.of(stem) else null
                            val mid = size.height / 2
                            val amp = mid - edge * 2
                            if (peaks != null) {
                                val from = view.start.toLong()
                                peaks.fill(from, from + (columns * column * fpp).toLong(), heights)
                                val scale = levels.scale(stem)
                                var c = 0
                                while (c < columns) {
                                    val x = c * column
                                    val f = view.frameAt(x)
                                    val h = max(0.5f, min(amp, heights[c] * scale * amp))
                                    drawLine(if (sp != null && f in sp) color else grey, Offset(x, mid - h), Offset(x, mid + h), strokeWidth = column)
                                    c++
                                }
                            }
                            if (sp != null) {
                                // Outside the piece: a veil; the piece framed top and bottom.
                                if (a > 0f) drawRect(DIM, Offset.Zero, Size(min(a, size.width), size.height))
                                if (z < size.width) drawRect(DIM, Offset(max(0f, z), 0f), Size(size.width - max(0f, z), size.height))
                                val l = max(0f, a)
                                val r = min(size.width, z)
                                if (r > l) {
                                    drawLine(Color.White, Offset(l, barW / 2), Offset(r, barW / 2), strokeWidth = barW)
                                    drawLine(Color.White, Offset(l, size.height - barW / 2), Offset(r, size.height - barW / 2), strokeWidth = barW)
                                }
                                drawSpanBar(a, inward = 1f, barW, handleW, handleH)
                                drawSpanBar(z, inward = -1f, barW, handleW, handleH)
                            }
                        } else {
                        // Each piece's ground, in its colour.
                        pieces.forEachIndexed { i, p ->
                            val x0 = view.x(p.start)
                            val x1 = view.x(p.end)
                            if (x1 < 0f || x0 > size.width) return@forEachIndexed
                            drawRect(pieceColor(i).copy(alpha = 0.16f), Offset(max(0f, x0), 0f), Size(min(size.width, x1) - max(0f, x0), size.height))
                        }
                        // The curve: each column in its piece's colour, grey outside the pieces.
                        val peaks = if (levels.ready > stem) levels.of(stem) else null
                        val mid = (size.height - bottom + margin) / 2
                        val amp = (size.height - bottom - margin) / 2
                        if (peaks != null) {
                            val from = view.start.toLong()
                            peaks.fill(from, from + (columns * column * fpp).toLong(), heights)
                            val scale = levels.scale(stem)
                            var c = 0
                            while (c < columns) {
                                val x = c * column
                                val i = pieceIndexAt(pieces, view.frameAt(x))
                                val h = max(0.5f, min(amp, heights[c] * scale * amp))
                                drawLine(if (i >= 0) pieceColor(i) else grey, Offset(x, mid - h), Offset(x, mid + h), strokeWidth = column)
                                c++
                            }
                        }
                        // The cut lines, their handles below, the numbers on top.
                        var lastEnd = Long.MIN_VALUE
                        pieces.forEachIndexed { i, p ->
                            val x0 = view.x(p.start)
                            val x1 = view.x(p.end)
                            if (p.start != lastEnd) drawCutLine(x0, lineW, knob, bottom)
                            drawCutLine(x1, lineW, knob, bottom)
                            lastEnd = p.end
                            val cx = (max(0f, x0) + min(size.width, x1)) / 2
                            if (x1 - x0 > badge * 2.2f && cx in 0f..size.width) {
                                drawCircle(Color(0xCC111318), badge, Offset(cx, margin / 2 + 2f))
                                label(i + 1)?.let { drawText(it, topLeft = Offset(cx - it.size.width / 2f, margin / 2 + 2f - it.size.height / 2f)) }
                            }
                            if (p.loop && x1 - x0 > badge * 4) {
                                drawCircle(Color.White.copy(alpha = 0.85f), 3.dp.toPx(), Offset(x0 + badge, margin / 2 + 2f))
                            }
                        }
                        }
                        // The play head the tools act at: an orange line, a small triangle on top.
                        val hx = view.x(model.cursor)
                        if (hx in 0f..size.width) {
                            drawLine(head, Offset(hx, 0f), Offset(hx, size.height - bottom), strokeWidth = lineW)
                            tri.rewind()
                            tri.moveTo(hx - 7.dp.toPx(), 0f)
                            tri.lineTo(hx + 7.dp.toPx(), 0f)
                            tri.lineTo(hx, 9.dp.toPx())
                            tri.close()
                            drawPath(tri, head)
                        }
                        // What is heard now, green.
                        val heard = work.value.playing?.takeIf { it.stem == stem }?.let { work.value.playFrame } ?: -1L
                        if (heard >= 0) {
                            val px = view.x(heard)
                            if (px in 0f..size.width) {
                                drawLine(play.copy(alpha = 0.25f), Offset(px, 0f), Offset(px, size.height), strokeWidth = 6.dp.toPx())
                                drawLine(play, Offset(px, 0f), Offset(px, size.height), strokeWidth = lineW)
                            }
                        }
                    }
                },
        )
        if (model.single) model.span?.let { sp ->
            SpanBarNode(model, view, end = false, at = sp.start)
            SpanBarNode(model, view, end = true, at = sp.end)
        }
        model.movedTo?.let { at -> CutMagnifier(model, view, levels, at, Modifier.align(Alignment.TopStart)) }
    }
}

/** Outside « Un bout »: the song dimmed. */
private val DIM = Color(0x8C0B0C10)

/** The colour of stem [stem] (0 drums, 1 bass, 2 other, 3 voice), as its chip's dot. */
internal fun cutStemColor(stem: Int): Color = when (stem) {
    VOCALS_STEM -> Color(0xFF8BC34A)
    0 -> Color(0xFF7986CB)
    1 -> Color(0xFFFF7043)
    else -> Color(0xFF29B6F6)
}

/** A bar of « Un bout » at [x]: white, full height, its grip handle on the piece's side ([inward] +1 right, -1 left). */
private fun DrawScope.drawSpanBar(x: Float, inward: Float, width: Float, handleW: Float, handleH: Float) {
    if (x < -handleW || x > size.width + handleW) return
    drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = width)
    val left = if (inward > 0) x - width / 2 else x - handleW + width / 2
    val top = (size.height - handleH) / 2
    drawRoundRect(Color.White, Offset(left, top), Size(handleW, handleH), androidx.compose.ui.geometry.CornerRadius(handleW / 3))
    val grip = Color(0xFF4B5160)
    val cx = left + handleW / 2
    val g = handleW * 0.18f
    for (dx in listOf(-g, g)) drawLine(grip, Offset(cx + dx, top + handleH * 0.3f), Offset(cx + dx, top + handleH * 0.7f), strokeWidth = width * 0.6f)
}

/**
 * TalkBack's « Début du bout » (or « Fin du bout », [end]): a 48 dp node over the bar at frame
 * [at], its time said, moved one beat at a time by its custom actions (the finger drags the bar on
 * the curve underneath: this node takes no touch).
 */
@Composable
private fun SpanBarNode(model: CutModel, view: StemBoardView, end: Boolean, at: Long) {
    val name = if (end) tr("Fin du bout") else tr("Début du bout")
    val later = if (model.hasGrid) tr("Avancer d'un temps") else tr("Avancer un peu")
    val earlier = if (model.hasGrid) tr("Reculer d'un temps") else tr("Reculer un peu")
    Box(
        Modifier
            .layout { measurable, constraints ->
                val w = SPAN_NODE.roundToPx()
                val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                // The node sits on the piece's side of its bar, as the handle.
                val bar = view.x(at)
                val left = (if (end) bar - w + w / 4f else bar - w / 4f).toInt()
                layout(constraints.maxWidth, placeable.height) { placeable.place(IntOffset(left, 0)) }
            }
            .fillMaxHeight()
            .semantics {
                contentDescription = name
                stateDescription = cutClock(at)
                customActions = listOf(
                    CustomAccessibilityAction(later) { model.nudgeSpan(end, 1) },
                    CustomAccessibilityAction(earlier) { model.nudgeSpan(end, -1) },
                )
            }
            .testTag(if (end) "cut_span_end" else "cut_span_start"),
    )
}

/** TalkBack's node over a bar of « Un bout ». */
private val SPAN_NODE = 48.dp

/** « Un bout »: each side of a bar, the zone a finger grabs it in (48 dp in all). */
internal val SPAN_GRAB = 24.dp

/** A cut line at [x]: white, with its round handle at the bottom. */
private fun DrawScope.drawCutLine(x: Float, width: Float, knob: Float, bottom: Float) {
    if (x < -knob || x > size.width + knob) return
    drawLine(Color.White.copy(alpha = 0.9f), Offset(x, 0f), Offset(x, size.height - bottom / 2), strokeWidth = width)
    drawCircle(Color.White, knob, Offset(x, size.height - bottom / 2))
    drawCircle(Color(0xFF6B7280), knob * 0.45f, Offset(x, size.height - bottom / 2))
}

/** The magnifier over a moved cut: a few hundredths of a second around it, its time, « sur le temps ». */
@Composable
private fun CutMagnifier(model: CutModel, view: StemBoardView, levels: BoardPeaks, at: Long, modifier: Modifier) {
    val accent = Neon.Cyan
    val widthDp = 200.dp
    val x = view.x(at)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .offsetX(x, widthDp)
            .width(widthDp)
            .clip(shape)
            .background(Color(0xFF15171C))
            .border(2.dp, accent, shape)
            .padding(6.dp)
            .testTag("cut_magnifier"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.fillMaxWidth().height(56.dp).drawWithCache {
                val columns = max(1, (size.width / (3.dp.toPx())).toInt())
                val heights = FloatArray(columns)
                val col = size.width / columns
                onDrawBehind {
                    val span = MAGNIFIER_FRAMES
                    val from = at - span / 2
                    val peaks = if (levels.ready > model.stem) levels.of(model.stem) else null
                    val mid = size.height / 2
                    if (peaks != null) {
                        peaks.fill(from, from + span, heights)
                        val scale = levels.scale(model.stem)
                        for (c in 0 until columns) {
                            val f = from + span * c / columns
                            val i = pieceIndexAt(model.pieces, f)
                            val h = max(1f, min(mid, heights[c] * scale * mid))
                            val sp = model.span
                            val tint = when {
                                model.single -> if (sp != null && f in sp) cutStemColor(model.stem) else Color.Gray
                                i >= 0 -> pieceColor(i)
                                else -> Color.Gray
                            }
                            drawLine(tint, Offset(c * col + col / 2, mid - h), Offset(c * col + col / 2, mid + h), strokeWidth = col * 0.75f)
                        }
                    }
                    drawLine(Color.White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2.dp.toPx())
                }
            },
        )
        val where = if (model.onBeat(at)) " · " + tr("sur le temps") else ""
        Text(
            cutClock(at) + where,
            style = MonoStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize, fontWeight = FontWeight.Bold),
            color = accent,
            modifier = Modifier.padding(top = 2.dp).testTag("cut_magnifier_time"),
        )
    }
}

/** Frames shown across the magnifier: 0.3 s. */
private const val MAGNIFIER_FRAMES = BOARD_RATE * 3L / 10

/** The magnifier centred over [x] px of the curve, never past its edges. */
private fun Modifier.offsetX(x: Float, width: androidx.compose.ui.unit.Dp): Modifier =
    this.then(Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0))
        val w = width.roundToPx()
        val left = (x - w / 2f).toInt().coerceIn(0, max(0, constraints.maxWidth - w))
        layout(constraints.maxWidth, placeable.height) { placeable.place(IntOffset(left, 0)) }
    })

/** "0:28.46": a time in the song, to the hundredth (the magnifier). */
fun cutClock(frames: Long, rate: Int = BOARD_RATE): String {
    val hundredths = (frames * 100 + rate / 2) / rate
    val s = hundredths / 100
    return "%d:%02d.%02d".format(s / 60, s % 60, hundredths % 100)
}

/** Each side of a cut line, the zone a finger grabs it in. */
internal val CUT_GRAB = 12.dp

/** Two fingers: pinch zooms around them, moving them scrolls; until every finger lifts. */
private suspend fun AwaitPointerEventScope.zoomAndScroll(view: StemBoardView) {
    while (true) {
        val e = awaitPointerEvent()
        if (e.changes.none { it.pressed }) return
        val zoom = e.calculateZoom()
        val pan = e.calculatePan()
        val centroid = e.calculateCentroid(useCurrent = true)
        if (zoom != 1f && centroid.isSpecified) view.zoomBy(zoom, centroid.x)
        if (pan.x != 0f) view.scrollBy(pan.x)
        e.changes.forEach { if (it.positionChanged()) it.consume() }
    }
}
