package com.pocketstudio.app.rhythm

import com.pocketstudio.model.i18n.tr

import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.PadSample
import kotlin.math.abs

enum class SliceMode {
    Attacks, Grid;

    val label: String get() = when (this) {
        Attacks -> tr("Attaques")
        Grid -> tr("Grille")
    }
}

/** The slicing panel: cut positions (fractions of the whole sound, the first is always 0). */
data class SlicerUi(
    val file: String,
    val markers: List<Float> = listOf(0f),
    val mode: SliceMode = SliceMode.Attacks,
    val sensitivity: Float = 0.5f,
    val grid: Int = 8,
    val busy: Boolean = false,
)

/** Pure slicing rules, shared by the screen and the tests. */
object Slicer {
    val GRIDS = listOf(2, 4, 8, 16)

    fun grid(n: Int): List<Float> = List(n.coerceIn(1, DrumPattern.PAD_COUNT)) { it.toFloat() / n }

    /** Adds a cut at [at], or removes the cut within [tolerance] of it (never the first). */
    fun toggle(markers: List<Float>, at: Float, tolerance: Float = 0.015f): List<Float> {
        val near = markers.drop(1).minByOrNull { abs(it - at) }
        return if (near != null && abs(near - at) <= tolerance) {
            markers - near
        } else if (at > tolerance && at < 1f - tolerance && markers.size < DrumPattern.PAD_COUNT) {
            (markers + at).sorted()
        } else {
            markers
        }
    }

    /** Pads [firstPad]… each get one slice of [source]'s file; slices past the last pad are dropped. */
    fun toPads(source: PadSample, markers: List<Float>, firstPad: Int): List<Pair<Int, PadSample>> {
        val cuts = (markers.filter { it in 0f..1f } + 0f).distinct().sorted()
        return cuts.mapIndexedNotNull { i, start ->
            val pad = firstPad + i
            if (pad >= DrumPattern.PAD_COUNT) return@mapIndexedNotNull null
            val end = cuts.getOrElse(i + 1) { 1f }
            pad to PadSample(file = source.file, name = "${source.name.take(34)} ${i + 1}", start = start, end = end, gain = source.gain)
        }
    }

    fun apply(drums: DrumPattern, slices: List<Pair<Int, PadSample>>): DrumPattern =
        slices.fold(drums) { d, (pad, s) -> d.withSample(pad, s) }
}
