package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LivePads(
    val size: Int = 8,
    val clips: List<LiveClip?> = List(MAX) { null },
    /** The ready-to-play set ([LiveStarter]) was offered to this project once: never again (emptied by hand, they stay empty). */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val starterOffered: Boolean = false,
) {
    /** Pads up to the last one holding a sound (the grid never hides a sound). */
    val used: Int get() = clips.indexOfLast { it != null } + 1

    fun sanitized(): LivePads {
        val fixed = (clips + List((MAX - clips.size).coerceAtLeast(0)) { null }).take(MAX)
            .map { c -> c?.takeIf { it.isSafe }?.sanitized() }
        val minSize = fixed.indexOfLast { it != null } + 1
        return copy(size = SIZES.firstOrNull { it >= maxOf(size, minSize) } ?: MAX, clips = fixed)
    }

    /**
     * The size after « + » ([dir] > 0) or « − » ([dir] < 0): the next of [SIZES], never fewer pads
     * than [used] (a sound is never hidden); [size] itself when there is no further step.
     */
    fun stepped(dir: Int): Int {
        val now = SIZES.indexOfFirst { it >= size }.let { if (it < 0) SIZES.lastIndex else it }
        val next = SIZES.getOrNull(now + if (dir > 0) 1 else -1) ?: return size
        return if (next < used) size else next
    }

    fun with(pad: Int, clip: LiveClip?): LivePads =
        if (pad !in 0 until MAX) this else copy(clips = clips.mapIndexed { i, c -> if (i == pad) clip else c })

    companion object {
        const val MAX = 16
        const val MAX_BEATS = 64
        /** « Pads − 8 + »: 4, 6, 8 (2 columns), 12 (3), 16 (4). */
        val SIZES = listOf(4, 6, 8, 12, 16)
        /** The widest a pad gets (width : height); never taller than wide. */
        const val MAX_WIDE = 1.35f
        /** Columns a pad grid may take: always full rows. */
        val COLUMNS = listOf(2, 3, 4)
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

/** Where the pads go: [cols] x [rows] pads of [padW] x [padH], the grid [gridW] x [gridH] at the bottom of the room. */
data class PadGrid(val cols: Int, val rows: Int, val padW: Float, val padH: Float, val gridW: Float, val gridH: Float)

/**
 * The live pads' grid for [count] pads in a room [w] x [h] with [gap] between pads (any unit):
 * 2, 3 or 4 columns in full rows (a single row when that is all that fits), each pad square or a
 * little wide (at most [LivePads.MAX_WIDE] : 1), never taller than wide; the columns giving the
 * biggest pads win. The room left over stays above (the pads under the thumb).
 */
fun padGrid(count: Int, w: Float, h: Float, gap: Float): PadGrid {
    val n = count.coerceIn(1, LivePads.MAX)
    val options = LivePads.COLUMNS.filter { n % it == 0 && it <= n }.ifEmpty { listOf(n.coerceAtMost(4)) }
    return options.map { cols ->
        val rows = (n + cols - 1) / cols
        var padW = ((w - gap * (cols - 1)) / cols).coerceAtLeast(1f)
        val roomH = ((h - gap * (rows - 1)) / rows).coerceAtLeast(1f)
        // Never taller than wide; never wider than MAX_WIDE: then narrower, centred.
        var padH = minOf(padW, roomH)
        if (padW > padH * LivePads.MAX_WIDE) padW = padH * LivePads.MAX_WIDE
        padH = minOf(padH, padW)
        PadGrid(cols, rows, padW, padH, padW * cols + gap * (cols - 1), padH * rows + gap * (rows - 1))
    }.maxWith(compareBy<PadGrid> { it.padW * it.padH }.thenBy { -it.cols })
}

/** Every sample file the project uses (drum pads of every scene, live pads, audio clips, sampled instruments). */
val Project.sampleFiles: Set<String> get() =
    (allScenes.flatMap { s -> s.drums.lanes.mapNotNull { it.sample?.file } } +
        drums.lanes.mapNotNull { it.sample?.file } + live.clips.mapNotNull { it?.file } +
        audioTracks.mapNotNull { it.clip?.file } + allTrackSounds.mapNotNull { it.sampled.file }).toSet()

/**
 * The project without the sounds [files] (missing from an imported .zip): a drum pad goes back to
 * its kit sound, a live pad or an audio clip goes empty, a sampled track keeps its notes and plays
 * its synth until a sound is chosen again.
 */
fun Project.withoutSounds(files: Set<String>): Project {
    if (files.isEmpty()) return this
    fun DrumPattern.cleaned() = copy(lanes = lanes.map { lane -> if (lane.sample?.file?.let { it in files } == true) lane.copy(sample = null) else lane })
    fun SampledPatch.cleaned() = if (file?.let { it in files } == true) copy(file = null, name = "") else this
    return copy(
        drums = drums.cleaned(),
        scenes = scenes.map { it.copy(drums = it.drums.cleaned()) },
        live = live.copy(clips = live.clips.map { c -> c?.takeIf { it.file !in files } }),
        audioTracks = audioTracks.map { t ->
            t.mapTakes { c -> c.takeIf { it.file !in files } }
        },
        sampled = sampled.cleaned(),
        tracks = tracks.map { it.copy(sampled = it.sampled.cleaned(), layers = it.layers.map { l -> l.copy(sound = l.sound.copy(sampled = l.sound.sampled.cleaned())) }) },
        synthLayers = synthLayers.map { l -> l.copy(sound = l.sound.copy(sampled = l.sound.sampled.cleaned())) },
    ).sanitized()
}
