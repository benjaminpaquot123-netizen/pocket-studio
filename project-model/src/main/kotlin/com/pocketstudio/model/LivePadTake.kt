package com.pocketstudio.model

import kotlin.math.ceil

/**
 * The live pads played during a Live take (« les pads sont enregistrés »): when each pad sounded,
 * as the engine played it (ClipLauncher: a tap on a silent pad waits for its grid line, its loop
 * length but at most a bar, on the transport's beats; a tap on a sounding or waiting one stops it
 * at once; a one-shot plays its sound once).
 */
object LivePadTake {
    /** A tap on [pad] (-1: « Tout couper ») at transport step [at], [velocity] its strength (a controller's pad; 1 on the screen). */
    data class Tap(val pad: Int, val at: Double, val velocity: Float = 1f)

    /**
     * Pad [pad] sounding from transport step [from] until [until] (a one-shot: [oneShot], it plays
     * its whole sound once from [from]). A loop starts again every [LiveClip.beats] beats.
     * [velocity]: the strength of the tap that started it (as the engine plays it, see ClipLauncher).
     */
    data class Span(val pad: Int, val from: Double, val until: Double, val oneShot: Boolean = false, val velocity: Float = 1f)

    /**
     * What [clips] played between transport steps [start] and [end]: [atStart] the pads already
     * sounding when the take began (they count from its start), then [taps] in order.
     */
    fun spans(clips: List<LiveClip?>, atStart: Set<Int>, taps: List<Tap>, start: Int, end: Int, beatsPerBar: Int): List<Span> {
        val on = HashMap<Int, Double>()  // pad -> where it starts (may be ahead: waiting)
        val strength = HashMap<Int, Float>()  // pad -> the velocity of the tap that started it
        val out = ArrayList<Span>()
        fun stop(pad: Int, at: Double) {
            val from = on.remove(pad) ?: return
            if (at > from) out += Span(pad, from, minOf(at, end.toDouble()), velocity = strength[pad] ?: 1f)
        }
        atStart.forEach { pad -> if (clips.getOrNull(pad)?.oneShot == false) on[pad] = start.toDouble() }
        for (tap in taps.sortedBy { it.at }) {
            if (tap.at >= end) break
            if (tap.pad < 0) {
                on.keys.toList().forEach { stop(it, tap.at) }
                continue
            }
            val clip = clips.getOrNull(tap.pad) ?: continue
            if (tap.pad in on) {
                stop(tap.pad, tap.at)
                continue
            }
            val grid = minOf(clip.beats, beatsPerBar.coerceAtLeast(1)) * 4.0
            val line = ceil(tap.at / grid - 1e-9) * grid
            if (line >= end) continue
            val v = tap.velocity.takeIf { it.isFinite() && it > 0f }?.coerceAtMost(1f) ?: 1f
            if (clip.oneShot) out += Span(tap.pad, maxOf(line, start.toDouble()), Double.POSITIVE_INFINITY, oneShot = true, velocity = v)
            else {
                on[tap.pad] = maxOf(line, start.toDouble())
                strength[tap.pad] = v
            }
        }
        on.keys.toList().forEach { stop(it, end.toDouble()) }
        return out.sortedWith(compareBy({ it.from }, { it.pad }))
    }
}
