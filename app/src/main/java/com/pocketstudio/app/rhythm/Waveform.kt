package com.pocketstudio.app.rhythm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Visible part of a waveform: [start] and [width] are fractions of the whole sound. */
data class WaveWindow(val start: Float = 0f, val width: Float = 1f) {
    /** Pinch around [focus] (0..1 of the view) and pan by [pan] (fraction of the view). */
    fun transform(zoom: Float, focus: Float, pan: Float): WaveWindow {
        val newWidth = (width / zoom).coerceIn(MIN_WIDTH, 1f)
        val anchor = start + focus * width
        val newStart = (anchor - focus * newWidth - pan * newWidth).coerceIn(0f, 1f - newWidth)
        return WaveWindow(newStart, newWidth)
    }

    companion object {
        const val MIN_WIDTH = 1f / 64
    }
}

/** Min/max outline of a sound ([peaks] from [PadSamples.peaks]); pinch to zoom, drag to move. */
@Composable
fun Waveform(
    peaks: FloatArray,
    color: Color,
    modifier: Modifier = Modifier,
    region: Pair<Float, Float> = 0f to 1f,
    markers: List<Float> = emptyList(),
    markerColor: Color = Color.White,
    onTap: ((Float) -> Unit)? = null,
    /** Bar lines over the sound: (fraction of it, bar number), numbered where there is room. */
    bars: List<Pair<Float, Int>> = emptyList(),
) {
    val barText = androidx.compose.ui.text.rememberTextMeasurer()
    val barStyle = androidx.compose.ui.text.TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp))
    var start by remember(peaks) { mutableFloatStateOf(0f) }
    var width by remember(peaks) { mutableFloatStateOf(1f) }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .testTag("waveform")
            .pointerInput(peaks, onTap) {
                if (onTap != null) detectTapGestures { p -> onTap(start + width * p.x / size.width) }
            }
            .pointerInput(peaks) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val w = WaveWindow(start, width).transform(zoom, centroid.x / size.width, pan.x / size.width)
                    start = w.start
                    width = w.width
                }
            },
    ) {
        val buckets = peaks.size / 2
        if (buckets == 0) return@Canvas
        val dim = color.copy(alpha = 0.3f)
        val mid = size.height / 2
        val columns = size.width.toInt().coerceAtLeast(1)
        for (x in 0 until columns) {
            val from = ((start + width * x / columns) * buckets).toInt().coerceIn(0, buckets - 1)
            val to = ((start + width * (x + 1) / columns) * buckets).toInt().coerceIn(from + 1, buckets)
            var lo = 0f
            var hi = 0f
            for (b in from until to) {
                lo = minOf(lo, peaks[b * 2])
                hi = maxOf(hi, peaks[b * 2 + 1])
            }
            val where = start + width * (x + 0.5f) / columns
            drawLine(
                if (where < region.first || where > region.second) dim else color,
                Offset(x.toFloat(), mid - hi.coerceIn(-1f, 1f) * mid),
                Offset(x.toFloat(), mid - lo.coerceIn(-1f, 1f) * mid + 1f),
                strokeWidth = 1f,
            )
        }
        var lastLabel = -1000f
        bars.forEach { (at, bar) ->
            val x = (at - start) / width * size.width
            if (x !in 0f..size.width) return@forEach
            drawLine(Color.White.copy(alpha = 0.22f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            // A number where it does not run into the one before.
            if (x - lastLabel >= 28f * density) {
                drawText(barText, "$bar", Offset(x + 2f * density, 1f * density), barStyle)
                lastLabel = x
            }
        }
        markers.forEach { m ->
            val x = (m - start) / width * size.width
            if (x in 0f..size.width) drawLine(markerColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
        }
    }
}
