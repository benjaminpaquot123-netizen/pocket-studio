package com.pocketstudio.model.video

import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.partView
import com.pocketstudio.model.sceneLabel
import com.pocketstudio.model.i18n.tr
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/** One part of the song as the video shows it: its name, from [start] to [end] seconds of the video. */
data class VideoSection(val label: String, val start: Double, val end: Double)

/**
 * The « vidéo 9:16 » of a song (phase 39): which 30 seconds, where they are in the song, and the
 * parts they cross. The audio is the song rendered from its start up to [renderFrames] (the
 * export's own render, nothing changed), of which the video keeps [frames] frames from
 * [startFrame] on; [sections] are the parts heard, in video seconds. [fps] images a second.
 */
data class VideoPlan(
    val title: String,
    val startFrame: Long,
    val frames: Long,
    val sections: List<VideoSection>,
    val sampleRate: Int = SAMPLE_RATE,
    val fps: Int = FPS,
    /** The song part the video was centred on (-1: the loop, no song). */
    val part: Int = -1,
    /** The song's own length in seconds (the loop's: [seconds]). */
    val songSeconds: Double = 0.0,
) {
    /** Frames to render from the song's start: up to the end of the window. */
    val renderFrames: Long get() = startFrame + frames

    val seconds: Double get() = frames.toDouble() / sampleRate

    /** Images in the video: one every 1/[fps] s, the last one starting before the sound ends. */
    val videoFrames: Int get() = ceil(frames.toDouble() * fps / sampleRate).toInt()

    /** When image [i] shows, in microseconds (MediaCodec's clock). */
    fun ptsUs(i: Int): Long = i.toLong() * 1_000_000L / fps

    /** The audio frame (in the window) image [i] shows. */
    fun audioFrameOf(i: Int): Long = (i.toLong() * sampleRate / fps).coerceAtMost(frames)

    /** The section playing [t] seconds into the video (the last one past the end), or -1 without any. */
    fun sectionAt(t: Double): Int {
        if (sections.isEmpty()) return -1
        val i = sections.indexOfFirst { t < it.end }
        return if (i < 0) sections.lastIndex else i
    }

    /**
     * The gain of window frame [frame]: a short fade in when the window starts inside the song
     * (no click), and a fade out over the last [FADE_OUT_S] seconds.
     */
    fun gainAt(frame: Long): Float {
        val inFrames = if (startFrame > 0) (FADE_IN_S * sampleRate).toLong() else 0L
        val outFrames = minOf((FADE_OUT_S * sampleRate).toLong(), frames / 2)
        val fadeIn = if (inFrames > 0 && frame < inFrames) frame.toFloat() / inFrames else 1f
        val left = frames - frame
        val fadeOut = if (outFrames > 0 && left < outFrames) (left.coerceAtLeast(0L)).toFloat() / outFrames else 1f
        return (fadeIn * fadeOut).coerceIn(0f, 1f)
    }

    companion object {
        const val SAMPLE_RATE = 48_000
        const val FPS = 30
        const val SECONDS = 30.0
        const val FADE_IN_S = 0.05
        const val FADE_OUT_S = 1.5
        const val WIDTH = 720
        const val HEIGHT = 1280
    }
}

object VideoPlanner {
    /** The sections the video looks for first, best first (a part's French label, or its base: « Drop 2 », « Drop + »). */
    val WANTED = listOf("Drop", "Refrain", "Post-refrain")

    /** A part's section name without its copy number or Énergie suffix (« Drop 2 +» → « Drop »). */
    fun baseSection(label: String): String =
        label.removeSuffix(Scene.ENERGY_SUFFIX).trim().replace(Regex(""" \d+$"""), "")

    /**
     * The part of [p]'s song the video is about: the first Drop, else the first Refrain (or
     * Post-refrain); without any, the fullest part (the most tracks playing, the first of the
     * fullest), as the bench reads a song's loudest section. -1 without a song.
     */
    fun pickPart(p: Project): Int {
        if (p.song.isEmpty()) return -1
        for (wanted in WANTED) {
            val i = p.song.indexOfFirst { baseSection(p.sceneLabel(it.scene)) == wanted }
            if (i >= 0) return i
        }
        val scores = p.song.map { energy(p, it) }
        val best = scores.max()
        return scores.indexOf(best)
    }

    /** How full a part sounds: drum lanes playing, the 303 (counted twice) and each instrument track with notes (twice). */
    fun energy(p: Project, part: SongPart): Int {
        val v = p.partView(part)
        val drums = v.drums.lanes.count { lane -> lane.hits.keys.any { it < v.drums.steps } }
        val acid = if (v.acid.steps.take(v.acid.length.coerceAtLeast(0)).any { it.gate }) 2 else 0
        val tracks = v.instrumentTracks.count { it.notes.notes.isNotEmpty() } * 2
        return drums + acid + tracks
    }

    /** Seconds of sixteenth [steps] at [p]'s tempo. */
    private fun seconds(p: Project, steps: Long): Double = steps * 15.0 / p.tempoBpm

    /** The name a part shows in the video: its section (« Refrain »), else « Partie A ». */
    fun partLabel(p: Project, scene: Int): String {
        val label = p.sceneLabel(scene)
        return if (label.isNotBlank()) Scene.shownLabel(label) else tr("Partie %s", Scene.NAMES.getOrElse(scene) { "?" })
    }

    /**
     * The video of [p]: [length] seconds (the whole song when shorter) from the start of the part
     * [pickPart] chose, moved earlier when the song ends before, on a bar line. Without a song,
     * the scene looped for [length] seconds. [title]: the name shown (the project's).
     */
    fun plan(p: Project, title: String, length: Double = VideoPlan.SECONDS, sampleRate: Int = VideoPlan.SAMPLE_RATE): VideoPlan {
        if (p.song.isEmpty()) {
            val frames = (length * sampleRate).roundToLong()
            return VideoPlan(title, 0, frames, listOf(VideoSection(partLabel(p, p.scene), 0.0, length)), sampleRate, part = -1, songSeconds = length)
        }
        // Where each part starts and ends, in seconds of the song.
        val bounds = ArrayList<Pair<Double, Double>>()
        var steps = 0L
        p.song.forEach { part ->
            val s = p.partSteps(part).toLong()
            bounds += seconds(p, steps) to seconds(p, steps + s)
            steps += s
        }
        val total = seconds(p, steps)
        val duration = minOf(length, total)
        val index = pickPart(p)
        var start = bounds[index].first
        if (start + duration > total) {
            // Too close to the end: earlier, on a bar line, so the window still fits in the song.
            val bar = seconds(p, p.stepsPerBar.toLong())
            start = floor((total - duration) / bar + 1e-9) * bar
        }
        start = start.coerceAtLeast(0.0)
        val startFrame = (start * sampleRate).roundToLong()
        val frames = minOf((duration * sampleRate).roundToLong(), (total * sampleRate).roundToLong() - startFrame).coerceAtLeast(0)
        val end = start + frames.toDouble() / sampleRate
        val sections = p.song.indices.mapNotNull { i ->
            val (a, b) = bounds[i]
            if (b <= start || a >= end) null else VideoSection(partLabel(p, p.song[i].scene), (a - start).coerceAtLeast(0.0), (b - start).coerceAtMost(end - start))
        }
        return VideoPlan(title, startFrame, frames, merged(sections), sampleRate, part = index, songSeconds = total)
    }

    /** Neighbouring parts of the same name read as one (an A played twice in a row is one « Refrain »). */
    private fun merged(sections: List<VideoSection>): List<VideoSection> = buildList {
        sections.forEach { s ->
            val last = lastOrNull()
            if (last != null && last.label == s.label) set(size - 1, last.copy(end = s.end)) else add(s)
        }
    }
}

/**
 * What the picture shows of the sound: for each video image, its level (RMS) and peak over the
 * image's slice of [pcm] (interleaved stereo), both 0..1 ([VideoPlan.audioFrameOf] slices).
 */
object VideoLevels {
    class Levels(val rms: FloatArray, val peak: FloatArray)

    fun of(plan: VideoPlan, pcm: FloatArray, channels: Int = 2): Levels {
        val n = plan.videoFrames
        val rms = FloatArray(n)
        val peak = FloatArray(n)
        val total = pcm.size / channels
        for (i in 0 until n) {
            val a = plan.audioFrameOf(i).toInt().coerceAtMost(total)
            val b = plan.audioFrameOf(i + 1).toInt().coerceAtMost(total)
            var sum = 0.0
            var top = 0f
            for (f in a until b) {
                for (c in 0 until channels) {
                    val x = pcm[f * channels + c]
                    sum += x * x
                    val ax = kotlin.math.abs(x)
                    if (ax > top) top = ax
                }
            }
            val count = (b - a) * channels
            rms[i] = if (count > 0) kotlin.math.sqrt(sum / count).toFloat().coerceIn(0f, 1f) else 0f
            peak[i] = top.coerceIn(0f, 1f)
        }
        return Levels(rms, peak)
    }

    /** A level shown on a dB scale (−48..0 dBFS → 0..1): quiet parts still move. */
    fun shown(level: Float): Float {
        if (level <= 0f) return 0f
        val db = 20.0 * kotlin.math.log10(level.toDouble())
        return ((db + 48.0) / 48.0).toFloat().coerceIn(0f, 1f)
    }
}
