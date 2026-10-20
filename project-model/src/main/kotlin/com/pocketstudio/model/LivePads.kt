package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ln

/**
 * A live pad's sound. [file] lives in the project's `samples/` folder, like a drum pad's sound.
 * A loop restarts every [beats] beats while it is on; a [oneShot] plays once per tap.
 */
@Serializable
data class LiveClip(
    val file: String,
    val name: String,
    val beats: Int = 4,
    val oneShot: Boolean = false,
    val gain: Float = 1f,
) {
    val isSafe: Boolean get() = PadSample(file, name).isSafe

    fun sanitized() = copy(
        name = name.take(40),
        beats = beats.coerceIn(1, LivePads.MAX_BEATS),
        gain = gain.takeIf { it.isFinite() }?.coerceIn(0f, 2f) ?: 1f,
    )
}

/** The live pads grid: [size] pads shown, each one empty or holding a [LiveClip]. */
@Serializable
data class LivePads(
    val size: Int = 8,
    val clips: List<LiveClip?> = List(MAX) { null },
) {
    /** Pads up to the last one holding a sound (the grid never hides a sound). */
    val used: Int get() = clips.indexOfLast { it != null } + 1

    fun sanitized(): LivePads {
        val fixed = (clips + List((MAX - clips.size).coerceAtLeast(0)) { null }).take(MAX)
            .map { c -> c?.takeIf { it.isSafe }?.sanitized() }
        val minSize = fixed.indexOfLast { it != null } + 1
        return copy(size = SIZES.firstOrNull { it >= maxOf(size, minSize) } ?: MAX, clips = fixed)
    }

    fun with(pad: Int, clip: LiveClip?): LivePads =
        if (pad !in 0 until MAX) this else copy(clips = clips.mapIndexed { i, c -> if (i == pad) clip else c })

    companion object {
        const val MAX = 16
        const val MAX_BEATS = 64
        val SIZES = listOf(4, 8, 12, 16)
        /** Loop lengths offered, in beats (1 beat, 2 beats, then bars). */
        val BEATS = listOf(1, 2, 4, 8, 16, 32)

        /** The loop length closest to a sound of [seconds] at [bpm]: a kick gives 1 beat, a 2-bar loop 8. */
        fun beatsFor(seconds: Double, bpm: Double): Int {
            val exact = seconds * bpm / 60.0
            if (!exact.isFinite() || exact <= 1.0) return 1
            return BEATS.minBy { abs(ln(it / exact)) }
        }
    }
}

/** Every sample file the project uses (drum pads of every scene, live pads and audio clips). */
val Project.sampleFiles: Set<String> get() =
    (allScenes.flatMap { s -> s.drums.lanes.mapNotNull { it.sample?.file } } +
        drums.lanes.mapNotNull { it.sample?.file } + live.clips.mapNotNull { it?.file } +
        audioTracks.mapNotNull { it.clip?.file }).toSet()
