package com.pocketstudio.app.live

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.LiveCells
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import kotlin.math.exp
import kotlin.math.hypot

/**
 * Lot 4c, the Briques grid « style B » (DECISIONS 2026-10-30): a near-black ground, the cells in a
 * soft gradient of their track's hue, an empty cell a small dot. The playing cell lights up (light
 * to deep), a sheen slides with its loop, a halo breathes on the beat, a thin bar shows where the
 * loop is; on each beat a soft wave of light leaves the playing cells and runs through the dots.
 * Everything moving is drawn on the UI thread from one clock ([GridClock]) and read only when
 * drawn (no recomposition); with the phone's animations off the clock does not run.
 */
internal object GridLook {
    val Ground = Color(0xFF1B1D20)
    val Panel = Color(0xFF25282C)
    val Dot = Color(0xFF40454C)
    val Ink = Color(0xFF15171A)
    private val Mid = Color(0xFF2D3034)
    private val Low = Color(0xFF222528)
    private val Queued = Color(0xFF202326)

    fun idleTop(a: Color) = lerp(Mid, a, 0.36f)
    fun idleBottom(a: Color) = lerp(Low, a, 0.16f)
    fun litBottom(a: Color) = lerp(Ground, a, 0.62f)
    fun litLight(a: Color) = lerp(a, Color.White, 0.6f)
    fun barTop(a: Color) = lerp(a, Color.White, 0.15f)
    fun queued(a: Color) = lerp(Queued, a, 0.10f)

    /** The wave: its front runs this fast (dp a second), this wide, fading through the beat and with distance. */
    const val WAVE_SPEED = 420f
    const val WAVE_WIDTH = 55f
    const val WAVE_FADE = 1.6f
    const val WAVE_REACH = 700f
}

/** The grid's one animation clock: the transport position (sixteenths, smoothed between polls) and the time since the beat. */
@Stable
internal class GridClock {
    /** Where the transport is, in sixteenths with a fraction (-1: stopped, or animations off). */
    var steps by mutableFloatStateOf(-1f)

    /** Seconds since the last beat (large: no beat). */
    var beatAge by mutableFloatStateOf(NO_BEAT)

    /** 1 on the beat, fading through it; [STILL] when the clock does not run. */
    val pulse: Float get() = if (steps < 0f) STILL else exp(-beatAge * 7f)

    /** The wave's strength at [distanceDp] from a playing cell (0: none). */
    fun wave(distanceDp: Float): Float {
        if (steps < 0f) return 0f
        val front = beatAge * GridLook.WAVE_SPEED
        val d = distanceDp - front
        return exp(-(d * d) / (2f * GridLook.WAVE_WIDTH * GridLook.WAVE_WIDTH)) * exp(-beatAge * GridLook.WAVE_FADE) * exp(-distanceDp / GridLook.WAVE_REACH)
    }

    companion object {
        const val NO_BEAT = 10f
        /** The halo's strength when nothing moves (animations off): a still, soft glow. */
        const val STILL = 0.35f
    }
}

/** Tests: whether a grid's clock is running (never with the animations off). */
@VisibleForTesting
internal var liveGridClockRunning = false

/**
 * The clock of [GridClock], run on each frame while [running] (playing, animations on); it reads
 * the polled transport step ([clockOf]) and smooths it with the frame time at [bpm].
 */
@Composable
internal fun rememberGridClock(running: Boolean, clockOf: () -> Int, bpm: Double): GridClock {
    val c = remember { GridClock() }
    val clock by rememberUpdatedState(clockOf)
    LaunchedEffect(running, bpm) {
        c.steps = -1f
        c.beatAge = GridClock.NO_BEAT
        liveGridClockRunning = running
        if (!running) return@LaunchedEffect
        val stepNs = 60e9 / bpm.coerceIn(20.0, 400.0) / STEPS_A_BEAT
        var last = Int.MIN_VALUE
        var lastAt = 0L
        while (true) {
            androidx.compose.animation.core.withInfiniteAnimationFrameNanos { now ->
                val s = clock()
                if (s < 0) {
                    c.steps = -1f
                    c.beatAge = GridClock.NO_BEAT
                } else {
                    if (s != last) { last = s; lastAt = now }
                    val smooth = s + ((now - lastAt) / stepNs).toFloat().coerceIn(0f, 0.999f)
                    c.steps = smooth
                    c.beatAge = ((smooth % STEPS_A_BEAT) * stepNs / 1e9).toFloat()
                }
            }
        }
    }
    return c
}

private const val STEPS_A_BEAT = 4

/** A playing cell the wave leaves from: its column and row, its colour. */
internal class WaveSource(val col: Int, val row: Int, val color: Color)

/**
 * The 8 bars of a cell's little preview (0..1): how busy each eighth of its loop is (hits, notes,
 * the 303's steps); a sound (audio, remix) gets a steady, made-up outline of its own.
 */
internal fun cellBars(p: Project, key: String, scene: Int): FloatArray {
    val out = FloatArray(BARS)
    val s = p.allScenes.getOrNull(scene)
    when {
        s == null -> return out
        key == SongPart.DRUMS -> {
            val n = s.drums.steps.coerceAtLeast(1)
            s.drums.lanes.forEach { lane -> lane.hits.keys.filter { it < n }.forEach { out[it * BARS / n] += 1f } }
        }
        key == SongPart.ACID -> {
            val n = s.acid.length.coerceAtLeast(1)
            s.acid.steps.take(n).forEachIndexed { i, st -> if (st.gate) out[i * BARS / n] += if (st.accent) 1.5f else 1f }
        }
        key.startsWith("inst:") -> {
            val id = key.removePrefix("inst:").toIntOrNull()
            val clip = if (id == 0) s.synthNotes else s.trackNotes[id]
            if (clip != null) {
                val ticks = clip.ticks.coerceAtLeast(1)
                clip.notes.forEach { out[(it.start.coerceIn(0, ticks - 1) * BARS / ticks)] += 1f }
            }
        }
        else -> {
            var h = (key.hashCode() * 31 + scene) and 0x7fffffff
            for (i in 0 until BARS) { h = (h * 1103515245 + 12345) and 0x7fffffff; out[i] = 0.4f + (h % 600) / 1000f }
            return out
        }
    }
    val max = out.maxOrNull()?.takeIf { it > 0f } ?: return out.also { it.fill(0.15f) }
    for (i in out.indices) out[i] = if (out[i] <= 0f) 0.15f else 0.3f + 0.7f * out[i] / max
    return out
}

private const val BARS = 8

/** The preview bars at the foot of a cell: [fill] as given (a brush or a colour per bar). */
internal fun DrawScope.previewBars(bars: FloatArray, brush: Brush?, color: Color, alpha: Float) {
    if (size.height < 30.dp.toPx()) return
    val side = 8.dp.toPx()
    val gap = 2.dp.toPx()
    val foot = 7.dp.toPx()
    val tall = size.height * 0.3f
    val w = (size.width - 2 * side - gap * (bars.size - 1)) / bars.size
    if (w <= 0f) return
    val r = CornerRadius(1.dp.toPx())
    bars.forEachIndexed { i, v ->
        val h = tall * v
        val x = side + i * (w + gap)
        val top = size.height - foot - h
        if (brush != null) drawRoundRect(brush, Offset(x, top), Size(w, h), r, alpha = alpha)
        else drawRoundRect(color, Offset(x, top), Size(w, h), r, alpha = alpha)
    }
}

/** An empty cell: a small dot, swelling and taking the colour of the wave passing through it. */
internal fun DrawScope.emptyDot(wave: Float, color: Color?) {
    val c = Offset(size.width / 2, size.height / 2)
    val r = 3.dp.toPx() * (1f + 0.9f * wave)
    if (wave > 0.04f && color != null) {
        drawCircle(color.copy(alpha = 0.35f * wave), r + 10.dp.toPx() * wave, c)
        drawCircle(lerp(GridLook.Dot, color, wave.coerceAtMost(1f)), r, c)
    } else {
        drawCircle(GridLook.Dot, r, c)
    }
}

/** How far (dp) the cell at [col], [row] is from [s], cells [w] × [h] dp apart by [gap]. */
internal fun waveDistance(col: Int, row: Int, s: WaveSource, w: Float, h: Float, gap: Float): Float =
    hypot((col - s.col) * (w + gap), (row - s.row) * (h + gap))

/** « Cases − 8 + »: how many bricks the grid shows (4, 6, 8, 12, 16). */
@Composable
internal fun CasesStepper(rows: Int, setRows: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(GridLook.Panel).padding(start = 10.dp).testTag("live_cases"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr("Cases"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, maxLines = 1)
        val less = LiveCells.stepRows(rows, up = false)
        val more = LiveCells.stepRows(rows, up = true)
        IconButton(onClick = { setRows(less) }, enabled = less != rows, modifier = Modifier.size(44.dp).testTag("live_cases_less")) {
            Icon(Icons.Filled.Remove, contentDescription = tr("Moins de cases"))
        }
        Text("$rows", style = MonoStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Neon.TextPrimary, maxLines = 1,
            modifier = Modifier.testTag("live_cases_count"))
        IconButton(onClick = { setRows(more) }, enabled = more != rows, modifier = Modifier.size(44.dp).testTag("live_cases_more")) {
            Icon(Icons.Filled.Add, contentDescription = tr("Plus de cases"))
        }
    }
}
