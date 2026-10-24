package com.pocketstudio.app.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.live.HideSystemBars
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.WholeWordsText
import com.pocketstudio.ui.trackColor
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The live pads on the whole screen, to play them on stage: big pads in their sound's colour, a
 * playing pad lit and breathing on the beat with its loop going round its edge, a queued pad
 * blinking (its edge empties until the launch line), a one-shot flashing. Above: back, tempo,
 * bar.beat, ▶/■, « Tout couper » and « Effets », which slides the live effects in (from below
 * upright, from the side lying down). Everything that moves is read when drawn: neither the beat
 * nor a loop's progress recompose a pad.
 */
@Composable
fun LivePadsFullScreen(
    ui: LivePadsUi,
    actions: LivePadsActions,
    close: () -> Unit,
    tempoBpm: Double,
    beatsPerBar: Int,
    playing: Boolean,
    /** Transport steps since play (-1: stopped), read only by the pads' clock. */
    clockOf: () -> Int,
    colorOf: (Int, LiveClip) -> Color = { i, clip -> livePadColor(clip.name, i) },
    togglePlay: (() -> Unit)? = null,
    effects: @Composable (Modifier) -> Unit,
    /** ● in the bar: the Live take, the pads recorded with it (null: none). */
    record: (@Composable () -> Unit)? = null,
    /** Under the bar: the take's line (REC, then « Morceau écrit »), given by the app. */
    banner: (@Composable () -> Unit)? = null,
) {
    HideSystemBars(true)
    if (androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current != null) {
        androidx.activity.compose.BackHandler(onBack = close)
    }
    var fxOpen by rememberSaveable { mutableStateOf(false) }
    val clock = remember { PadClock() }
    val statesNow by rememberUpdatedState(ui.states)
    val clipsNow by rememberUpdatedState(ui.pads.clips)
    val clockNow by rememberUpdatedState(clockOf)
    val tempoNow by rememberUpdatedState(tempoBpm)
    val barNow by rememberUpdatedState(beatsPerBar)
    // One frame clock for every pad, only while something moves (the transport, a pad on or queued).
    val active = playing || ui.states.any { it != 0 }
    LaunchedEffect(active) {
        if (!active) {
            clock.stop()
            return@LaunchedEffect
        }
        val tick: (Long) -> Unit = { n -> clock.tick(n, clockNow(), statesNow, clipsNow, tempoNow, barNow) }
        // A first frame as an ordinary one (shown even where endless animations are held, as in tests).
        androidx.compose.runtime.withFrameNanos(tick)
        while (true) withInfiniteAnimationFrameNanos(tick)
    }
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(Neon.Background, Color.Black, 0.25f), lerp(Neon.Background, Color.Black, 0.55f))))
            // Nothing under this page takes a touch meant for it.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("live_pads_full"),
    ) {
        // Upright the effects come up from below (a handle under the pads); lying down, from the side
        // (« Effets » in the bar on top).
        val landscape = maxWidth > maxHeight
        val wholeHeight = maxHeight
        val wholeWidth = maxWidth
        val pads: @Composable (Modifier) -> Unit = { m ->
            PadGrid(ui, actions, clock, colorOf, beatsPerBar, close, m)
        }
        Column(Modifier.fillMaxSize()) {
            StageBar(tempoBpm, beatsPerBar, playing, clock, fxOpen, close, togglePlay, actions.stopAll, if (landscape) ({ fxOpen = !fxOpen }) else null, record)
            banner?.invoke()
            if (landscape) {
                val panelWidth = if (wholeWidth >= 900.dp) 420.dp else minOf(380.dp, wholeWidth * 0.5f)
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    pads(Modifier.weight(1f).fillMaxHeight())
                    AnimatedVisibility(
                        fxOpen,
                        enter = slideInHorizontally(tween(220)) { it } + expandHorizontally(tween(220)),
                        exit = slideOutHorizontally(tween(180)) { it } + shrinkHorizontally(tween(180)),
                    ) {
                        FxPanel(sideways = true, { fxOpen = false }, effects, Modifier.width(panelWidth).fillMaxHeight())
                    }
                }
            } else {
                val panelHeight = (wholeHeight * 0.5f).coerceIn(minOf(300.dp, wholeHeight), 520.dp)
                pads(Modifier.fillMaxWidth().weight(1f))
                AnimatedVisibility(
                    fxOpen,
                    enter = slideInVertically(tween(220)) { it } + expandVertically(tween(220)),
                    exit = slideOutVertically(tween(180)) { it } + shrinkVertically(tween(180)),
                ) {
                    FxPanel(sideways = false, { fxOpen = false }, effects, Modifier.fillMaxWidth().height(panelHeight))
                }
                if (!fxOpen) FxHandle { fxOpen = true }
            }
        }
    }
}

/** Upright, under the pads: « Effets live » and ⌃, a touch slides the effects up. */
@Composable
private fun FxHandle(open: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp).height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(lerp(Neon.SurfaceHigh, Color.White, 0.04f), Neon.Surface)))
            .clickable(role = Role.Button, onClick = open)
            .testTag("live_pads_fx_toggle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Tune, contentDescription = null, tint = Neon.Acid)
        Text(tr("Effets live"), Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary, maxLines = 1)
        Icon(Icons.Filled.ExpandLess, contentDescription = null, tint = Neon.TextSecondary)
    }
}

/** The bar on top: back, tempo and bar.beat with its beat lights, ▶/■, « Tout couper », « Effets ». */
@Composable
private fun StageBar(
    tempoBpm: Double,
    beatsPerBar: Int,
    playing: Boolean,
    clock: PadClock,
    fxOpen: Boolean,
    close: () -> Unit,
    togglePlay: (() -> Unit)?,
    stopAll: () -> Unit,
    /** Lying down: « Effets » here (upright: the handle under the pads). */
    toggleFx: (() -> Unit)?,
    record: (@Composable () -> Unit)? = null,
) = BoxWithConstraints(Modifier.fillMaxWidth()) {
    val wide = maxWidth >= 600.dp
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = close, modifier = Modifier.testTag("live_pads_full_close")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Quitter le plein écran"))
        }
        if (wide) Text(tr("Pads live"), style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary, maxLines = 1)
        Spacer(Modifier.weight(1f))
        BeatCounter(tempoBpm, beatsPerBar, clock)
        Spacer(Modifier.weight(1f))
        if (togglePlay != null) {
            StageButton(
                if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, null,
                if (playing) tr("Arrêter") else tr("Lecture"), lit = playing, accent = Neon.Acid, tag = "live_pads_full_play", onClick = togglePlay,
            )
        }
        record?.invoke()
        StageButton(Icons.Filled.StopCircle, if (wide) tr("Tout couper") else null, tr("Tout couper"), lit = false, accent = Neon.Red, tag = "live_pads_full_stop", tint = Neon.Red, onClick = stopAll)
        if (toggleFx != null) StageButton(Icons.Filled.Tune, tr("Effets"), tr("Effets"), lit = fxOpen, accent = Neon.Cyan, tag = "live_pads_fx_toggle", onClick = toggleFx)
    }
}

@Composable
private fun StageButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String?,
    description: String,
    lit: Boolean,
    accent: Color,
    tag: String,
    /** The icon's own colour while not lit (« Tout couper » in red). */
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.height(48.dp).clip(RoundedCornerShape(24.dp))
            .background(if (lit) accent else Neon.SurfaceHigh)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = lit
            }
            .padding(horizontal = if (label == null) 12.dp else 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (lit) Neon.OnAccent else Neon.TextPrimary
        Icon(icon, contentDescription = null, tint = if (lit) ink else tint ?: ink)
        if (label != null) Text(label, Modifier.padding(start = 6.dp), color = ink, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/**
 * « 128 BPM » and « 5.3 » (bar.beat), with a light per beat of the bar, the one playing lit (the
 * bar's first in orange). The numbers change once a beat; the lights are redrawn, not recomposed.
 */
@Composable
private fun BeatCounter(tempoBpm: Double, beatsPerBar: Int, clock: PadClock) {
    val beat by remember(clock) { derivedStateOf { clock.beat().let { if (it < 0) -1 else floor(it).toInt() } } }
    val bpb = beatsPerBar.coerceAtLeast(1)
    val tempo = "${tempoBpm.roundToInt()}"
    Row(
        Modifier.semantics(mergeDescendants = true) {}.testTag("live_pads_full_beat"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(tempo, style = MonoStyle.copy(fontSize = 18.sp), color = Neon.TextPrimary, maxLines = 1)
            Text("BPM", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
        }
        Text(
            if (beat < 0) "–.–" else "${beat / bpb + 1}.${beat % bpb + 1}",
            style = MonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = if (beat < 0) Neon.TextSecondary else Neon.TextPrimary, maxLines = 1,
        )
        val first = Neon.Cyan
        val other = Neon.Acid
        val off = Color.White.copy(alpha = 0.14f)
        Canvas(Modifier.size(width = (12 * bpb).dp, height = 14.dp)) {
            val b = clock.beat()
            val now = if (b < 0) -1 else floor(b).toInt() % bpb
            val fade = if (b < 0) 0f else (1f - (b - floor(b)).toFloat()).let { it * it }
            val step = size.width / bpb
            for (k in 0 until bpb) {
                val c = Offset(step * (k + 0.5f), size.height / 2)
                val on = if (k == now) 0.45f + 0.55f * fade else 0f
                drawCircle(off, radius = 4.dp.toPx(), center = c)
                if (on > 0f) {
                    val color = if (k == 0) first else other
                    drawCircle(color.copy(alpha = 0.35f * on), radius = 7.dp.toPx(), center = c)
                    drawCircle(color.copy(alpha = on), radius = 4.dp.toPx(), center = c)
                }
            }
        }
    }
}

/** The pads in as many columns as gives them the biggest size, filling the room they have. */
@Composable
private fun PadGrid(
    ui: LivePadsUi,
    actions: LivePadsActions,
    clock: PadClock,
    colorOf: (Int, LiveClip) -> Color,
    beatsPerBar: Int,
    close: () -> Unit,
    modifier: Modifier,
) = BoxWithConstraints(modifier.padding(12.dp)) {
    val count = ui.pads.size.coerceIn(1, LivePads.MAX)
    val gap = 12.dp
    val cols = bestColumns(count, maxWidth, maxHeight, gap)
    val rows = (count + cols - 1) / cols
    val padW = (maxWidth - gap * (cols - 1)) / cols
    val padH = (maxHeight - gap * (rows - 1)) / rows
    val big = minOf(padW, padH) >= 140.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (c in 0 until cols) {
                    val i = r * cols + c
                    if (i >= count) {
                        Spacer(Modifier.size(padW, padH))
                        continue
                    }
                    val clip = ui.pads.clips.getOrNull(i)
                    StagePad(
                        i, clip, ui.states.getOrElse(i) { 0 }, clip?.let { colorOf(i, it) } ?: Neon.SurfaceHigh, clock, beatsPerBar, big,
                        Modifier.size(padW, padH),
                    ) {
                        // An empty pad is set up on the page below.
                        if (clip == null) {
                            close()
                            actions.edit(i)
                        } else {
                            actions.tap(i)
                        }
                    }
                }
            }
        }
    }
}

/** Columns giving the biggest pads for [count] pads in [w] x [h], in full rows (no hole at the end). */
internal fun bestColumns(count: Int, w: Dp, h: Dp, gap: Dp): Int {
    val all = (1..minOf(count, 8)).toList()
    val full = all.filter { count % it == 0 }.ifEmpty { all }
    return full.maxBy { cols ->
        val rows = (count + cols - 1) / cols
        minOf((w - gap * (cols - 1)).value / cols, (h - gap * (rows - 1)).value / rows)
    }
}

/**
 * A big pad: its colour in a gradient, lit when it plays, its edge filling up with the loop; a
 * queued pad's edge blinks and empties until its launch; a one-shot flashes when it fires. Fires on
 * the finger's down (a light buzz), like a controller's pad.
 */
@Composable
private fun StagePad(
    i: Int,
    clip: LiveClip?,
    state: Int,
    color: Color,
    clock: PadClock,
    beatsPerBar: Int,
    big: Boolean,
    modifier: Modifier,
    onDown: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val down by rememberUpdatedState(onDown)
    val lit = animateFloatAsState(if (state == 2) 1f else 0f, tween(200), label = "stage-lit")
    // The finger's touch, and a one-shot firing: a white flash fading out.
    val press = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val oneShot = clip?.oneShot == true
    LaunchedEffect(state, oneShot) {
        if (state == 2 && oneShot) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
    }
    val beats = clip?.beats ?: 4
    val radius = if (big) 22.dp else 16.dp
    val surface = Neon.Surface
    val surfaceHigh = Neon.SurfaceHigh
    Box(
        modifier
            .drawWithCache {
                val r = radius.toPx()
                val edgeW = (if (big) 5.dp else 4.dp).toPx()
                val outline = outlineFromTop(size, r, edgeW / 2)
                val measure = PathMeasure().apply { setPath(outline, false) }
                val length = measure.length
                val part = Path()
                val corner = CornerRadius(r)
                val litBrush = Brush.verticalGradient(listOf(lerp(color, Color.White, 0.22f), color, lerp(color, Color.Black, 0.38f)))
                val idleBrush = Brush.verticalGradient(listOf(lerp(surfaceHigh, color, 0.42f), lerp(surface, color, 0.16f)))
                val emptyBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.035f), Color.Black.copy(alpha = 0.2f)))
                val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), endY = size.height * 0.5f)
                onDrawWithContent {
                    val on = lit.value
                    val b = clock.beat()
                    val running = b >= 0
                    // On the beat: 1, fading through it; the bar's first beat a little stronger.
                    val frac = if (running) (b - floor(b)).toFloat() else 1f
                    val pulse = (1f - frac).let { it * it } * if (running && floor(b).toInt() % beatsPerBar.coerceAtLeast(1) == 0) 1f else 0.7f
                    // The glow around a playing pad, breathing on the beat.
                    if (on > 0.01f) {
                        for (k in 1..4) {
                            val spread = k * 2.5.dp.toPx()
                            drawRoundRect(
                                color.copy(alpha = (on * (0.12f + 0.3f * pulse) / k).coerceIn(0f, 1f)),
                                topLeft = Offset(-spread, -spread), size = Size(size.width + 2 * spread, size.height + 2 * spread),
                                cornerRadius = CornerRadius(r + spread), style = Stroke(2.5.dp.toPx()),
                            )
                        }
                    }
                    when {
                        clip == null -> drawRoundRect(emptyBrush, cornerRadius = corner)
                        else -> {
                            drawRoundRect(idleBrush, cornerRadius = corner)
                            if (on > 0f) drawRoundRect(litBrush, cornerRadius = corner, alpha = on)
                        }
                    }
                    drawRoundRect(sheen, cornerRadius = corner)
                    // A light flash on the beat while it plays, the touch and a one-shot's hit on top.
                    val white = (0.16f * on * pulse) + 0.22f * press.value + 0.55f * flash.value
                    if (white > 0.005f) drawRoundRect(Color.White.copy(alpha = white.coerceAtMost(0.8f)), cornerRadius = corner)
                    // The edge.
                    val edge = Stroke(edgeW, cap = StrokeCap.Round)
                    when {
                        clip == null -> drawRoundRect(Color.White.copy(alpha = 0.07f), cornerRadius = corner, style = Stroke(1.dp.toPx()))
                        state == 1 -> {
                            // Queued: the edge blinks on the eighths and empties until the launch line.
                            val blink = if (running) 0.35f + 0.65f * (if ((b * 2 - floor(b * 2)) < 0.5) 1f else 0.25f) else 0.7f
                            drawPath(outline, color.copy(alpha = 0.25f), style = Stroke(edgeW))
                            val left = if (running) clock.waitLeft(beats, beatsPerBar) else 1f
                            part.reset()
                            measure.getSegment(0f, length * left, part, true)
                            drawPath(part, lerp(color, Color.White, 0.35f).copy(alpha = blink), style = edge)
                        }
                        state == 2 && !oneShot -> {
                            // Where the loop is: the edge fills up from the top, round the pad.
                            drawPath(outline, Color.Black.copy(alpha = 0.28f), style = Stroke(edgeW))
                            part.reset()
                            measure.getSegment(0f, length * clock.position(i, beats), part, true)
                            drawPath(part, Color.White.copy(alpha = 0.95f), style = edge)
                        }
                        state == 2 -> drawRoundRect(Color.White.copy(alpha = 0.55f), cornerRadius = corner, style = Stroke(2.dp.toPx()))
                        else -> drawRoundRect(Color.White.copy(alpha = 0.14f), cornerRadius = corner, style = Stroke(1.dp.toPx()))
                    }
                    drawContent()
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        press.snapTo(1f)
                        press.animateTo(0f, tween(260))
                    }
                    down()
                }
            }
            .semantics {
                val what = when {
                    clip == null -> tr("vide")
                    state == 2 -> tr("joue")
                    state == 1 -> tr("en attente")
                    else -> tr("prêt")
                }
                contentDescription = "${clip?.name?.let(DefaultNames::shownSample) ?: tr("Pad %d", i + 1)} · $what"
                role = Role.Button
                onClick { down(); true }
            }
            .testTag("live_full_pad_$i"),
        contentAlignment = Alignment.Center,
    ) {
        PadFace(clip, state, color, big)
    }
}

/** What a pad says: its family's icon, its name, then « 2 mesures » / « une fois » or that it waits. */
@Composable
private fun PadFace(clip: LiveClip?, state: Int, color: Color, big: Boolean) {
    if (clip == null) {
        Text("+", color = Neon.TextSecondary.copy(alpha = 0.6f), fontSize = if (big) 36.sp else 28.sp, fontWeight = FontWeight.Light)
        return
    }
    val ink = when {
        state == 2 && color.luminance() > 0.45f -> Color(0xFF11151A)
        state == 2 -> Color.White
        else -> Neon.TextPrimary
    }
    val name = DefaultNames.shownSample(clip.name)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Icon(
            looksOf(clip.name).icon, contentDescription = null,
            tint = if (state == 2) ink.copy(alpha = 0.8f) else color,
            modifier = Modifier.size(if (big) 30.dp else 22.dp),
        )
        WholeWordsText(
            name, Modifier.padding(top = 4.dp), color = ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = if (big) 22.sp else 16.sp, lineHeight = if (big) 25.sp else 18.sp), maxLines = 2,
        )
        Text(
            when {
                state == 1 -> tr("en attente")
                clip.oneShot -> tr("une fois")
                else -> loopLabel(clip.beats)
            },
            color = ink.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium, maxLines = 1,
        )
    }
}

private fun loopLabel(beats: Int) = when {
    beats == 1 -> tr("1 temps")
    beats < 4 || beats % 4 != 0 -> tr("%d temps", beats)
    else -> com.pocketstudio.app.ui.barsShort(beats / 4)
}

/** The effects slid in: a title with ✕, then the live effects themselves. */
@Composable
private fun FxPanel(sideways: Boolean, close: () -> Unit, effects: @Composable (Modifier) -> Unit, modifier: Modifier) {
    val shape = if (sideways) RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp) else RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.White, 0.04f), Neon.Surface)))
            .testTag("live_pads_fx_panel"),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 4.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(Neon.Acid))
            Text(tr("Effets live"), Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary, maxLines = 1)
            IconButton(onClick = close, modifier = Modifier.testTag("live_pads_fx_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer"))
            }
        }
        effects(Modifier.fillMaxWidth().weight(1f).padding(start = 10.dp, end = 10.dp, bottom = 10.dp))
    }
}

/**
 * A rounded rectangle's outline as one path starting at the middle of its top edge and going
 * clockwise, inset by [inset]: a part of it shows how far a loop has gone.
 */
private fun outlineFromTop(size: Size, radius: Float, inset: Float): Path {
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    val rad = radius.coerceAtMost(minOf(r - l, b - t) / 2).coerceAtLeast(0f)
    val d = rad * 2
    return Path().apply {
        moveTo((l + r) / 2, t)
        lineTo(r - rad, t)
        arcTo(Rect(r - d, t, r, t + d), -90f, 90f, false)
        lineTo(r, b - rad)
        arcTo(Rect(r - d, b - d, r, b), 0f, 90f, false)
        lineTo(l + rad, b)
        arcTo(Rect(l, b - d, l + d, b), 90f, 90f, false)
        lineTo(l, t + rad)
        arcTo(Rect(l, t, l + d, t + d), 180f, 90f, false)
        close()
    }
}

/**
 * The pads' beat clock, drawn from: the transport's while it plays (polled steps, smoothed by the
 * frame time), else the live pads' own one, which starts with the first pad launched. Only
 * [frame] is state: whoever reads [beat] when drawing is redrawn each frame, never recomposed.
 */
@Stable
internal class PadClock {
    private var frame by mutableLongStateOf(0L)
    private var anchorNanos = 0L
    private var anchorBeat = 0.0
    private var running = false
    private var bpm = 120.0
    private var lastStep = -1
    private var beatsPerBar = 4
    private val prev = IntArray(LivePads.MAX)
    private val starts = DoubleArray(LivePads.MAX)
    /** No frame seen yet since the page opened (or everything stopped). */
    private var fresh = true

    private fun beatAt(n: Long): Double = if (!running) -1.0 else anchorBeat + (n - anchorNanos) * bpm / 60e9

    /** Beats since the clock started, with the fraction (-1: stopped). */
    fun beat(): Double = beatAt(frame)

    private fun anchor(n: Long, beat: Double) {
        anchorNanos = n
        anchorBeat = beat
        running = true
    }

    fun tick(n: Long, step: Int, states: List<Int>, clips: List<LiveClip?>, tempo: Double, bar: Int) {
        val t = tempo.coerceIn(20.0, 400.0)
        if (t != bpm) {
            if (running) anchor(n, beatAt(n))
            bpm = t
        }
        beatsPerBar = bar.coerceAtLeast(1)
        val anyPlaying = states.any { it == 2 }
        if (step >= 0) {
            val want = step / 4.0
            if (lastStep < 0) {
                // The transport just started: the loops start over with it (the engine snaps them).
                anchor(n, want)
                for (k in starts.indices) {
                    val beats = clips.getOrNull(k)?.beats ?: continue
                    starts[k] = floor(want / beats) * beats
                }
            } else if (step != lastStep) {
                // A new step seen (up to a poll late): the beat is at least there, at most a step on.
                val now = beatAt(n)
                if (now < want || now > want + 0.45) anchor(n, want + 0.05)
            }
        } else if (!anyPlaying) {
            running = false
        }
        lastStep = step
        for (k in 0 until minOf(states.size, LivePads.MAX)) {
            val s = states[k]
            if (s == 2 && prev[k] != 2) {
                // Off on its own clock, the first pad launched plays at once: beat 0.
                if (!running) anchor(n, 0.0)
                val beats = clips.getOrNull(k)?.beats ?: 4
                val grid = gridOf(beats)
                // Already playing when the page opened: in step with the clock, as the engine keeps it.
                starts[k] = if (fresh) floor(beatAt(n) / beats) * beats else (beatAt(n) / grid).roundToInt() * grid.toDouble()
            }
            prev[k] = s
        }
        fresh = false
        frame = n
    }

    fun stop() {
        running = false
        lastStep = -1
        prev.fill(0)
        fresh = true
        frame += 1
    }

    private fun gridOf(beats: Int) = if (beats < beatsPerBar) beats else beatsPerBar

    /** Where pad [i]'s loop of [beats] is (0..1). */
    fun position(i: Int, beats: Int): Float {
        val b = beat()
        if (b < 0 || beats <= 0) return 0f
        val p = ((b - starts[i]) / beats) % 1.0
        return (if (p < 0) p + 1 else p).toFloat()
    }

    /** What is left (1..0) until a queued pad of [beats] launches on its line. */
    fun waitLeft(beats: Int, bar: Int): Float {
        val b = beat()
        if (b < 0) return 1f
        val grid = (if (beats < bar) beats else bar).coerceAtLeast(1).toDouble()
        val next = ceil(b / grid - 1e-9) * grid
        return ((next - b) / grid).toFloat().coerceIn(0f, 1f)
    }
}

/** A pad's family look from its name (« Basse », « Nappe », « Kick »...), when it says one. */
private fun looksOf(name: String) = SoundLooks.of(familyOf(name) ?: "")

private fun familyOf(name: String): String? {
    val words = DefaultNames.shownSample(name).lowercase().split(' ', '-', '_', '·', '.').filter { it.isNotBlank() }
    for (w in words) {
        val family = when {
            w.startsWith("batt") || w.startsWith("drum") || w == "beat" || w == "kit" -> "batterie"
            w.startsWith("kick") || w == "bd" -> "kick"
            w.startsWith("snare") || w.startsWith("caisse") -> "snare"
            w.startsWith("clap") -> "clap"
            w.startsWith("hat") || w.startsWith("charley") || w.startsWith("hihat") -> "hat"
            w.startsWith("perc") -> "perc"
            w.startsWith("bass") || w == "sub" || w.startsWith("808") -> "basse"
            w.startsWith("nappe") || w == "pad" || w == "pads" || w.startsWith("strings") -> "nappes"
            w.startsWith("lead") || w.startsWith("synth") -> "lead"
            w.startsWith("stab") || w.startsWith("hoover") -> "stab"
            w.startsWith("pluck") || w.startsWith("arp") -> "pluck"
            w.startsWith("piano") || w.startsWith("keys") || w.startsWith("orgue") || w.startsWith("chord") || w.startsWith("accord") -> "keys"
            w.startsWith("voix") || w.startsWith("vox") || w.startsWith("vocal") || w.startsWith("chant") -> "voix"
            w.startsWith("impact") || w.startsWith("riser") || w == "fx" || w.startsWith("effet") || w.startsWith("boom") || w.startsWith("sweep") -> "fx"
            w.startsWith("ride") || w.startsWith("crash") || w.startsWith("cymb") -> "ride"
            w.startsWith("tom") -> "tom"
            else -> null
        }
        if (family != null) return family
    }
    return null
}

/**
 * A live pad's colour: its track's when it bears a track's name ([tracks], the Live page's columns),
 * else its family's (« Basse », « Nappe »...), else one of the tracks' colours by its place.
 */
fun livePadColor(name: String, index: Int, tracks: List<Pair<String, Color>> = emptyList()): Color {
    val shown = DefaultNames.shownSample(name).trim()
    tracks.firstOrNull { it.first.equals(shown, ignoreCase = true) }?.let { return it.second }
    familyOf(name)?.let { return SoundLooks.of(it).color }
    return trackColor(index)
}
