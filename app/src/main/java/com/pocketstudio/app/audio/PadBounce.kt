package com.pocketstudio.app.audio

import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePadTake

/**
 * The live pads of a Live take as one sound (« les pads sont enregistrés »): each pad's sound
 * where it played ([LivePadTake.spans]), its loop started again every [LiveClip.beats] beats and
 * cut (with a short fade) where it stopped, at the clip's volume. Transport step [start] is the
 * first frame; [end] the last.
 */
object PadBounce {
    /** Past this the bounce is mono (a long set: half the memory). */
    const val STEREO_SECONDS = 120.0

    /** The engine's longest « Fin » (ClipLauncher::kMaxRelease), in seconds. */
    const val MAX_RELEASE = 4f

    fun render(
        spans: List<LivePadTake.Span>,
        clips: List<LiveClip?>,
        sounds: Map<Int, Pcm>,
        start: Int,
        end: Int,
        bpm: Double,
        rate: Int = 48_000,
    ): Pcm? {
        if (spans.isEmpty() || end <= start || bpm <= 0.0) return null
        val perStep = 15.0 / bpm * rate
        val frames = Math.round((end - start) * perStep).toInt()
        if (frames <= 0) return null
        val stereo = frames < STEREO_SECONDS * rate && sounds.values.any { it.channels == 2 }
        val ch = if (stereo) 2 else 1
        val out = FloatArray(frames * ch)
        val fade = (rate / 200).coerceAtLeast(1)
        var heard = false
        // [src] from frame 0 at [at], [length] frames of it; from [fadeFrom] on it fades out to
        // nothing over [fadeLen] frames (a loop cut: just before its end; a pad stopped with a « Fin »: from the stop on).
        fun put(src: Pcm, gain: Float, at: Long, length: Int, fadeFrom: Int = Int.MAX_VALUE, fadeLen: Int = 1) {
            val n = minOf(length, src.frames)
            for (i in 0 until n) {
                val f = at + i
                if (f < 0) continue
                if (f >= frames) break
                val g = if (i >= fadeFrom) gain * (1f - (i - fadeFrom).toFloat() / fadeLen).coerceAtLeast(0f) else gain
                val o = f.toInt() * ch
                when {
                    src.channels == ch -> for (c in 0 until ch) out[o + c] += src.data[i * ch + c] * g
                    ch == 2 -> { val v = src.data[i] * g; out[o] += v; out[o + 1] += v }
                    else -> out[o] += 0.5f * (src.data[i * 2] + src.data[i * 2 + 1]) * g
                }
                heard = true
            }
        }
        // A loop cut before its sound ends fades out over its last few milliseconds.
        fun cut(n: Int, src: Pcm) = if (n < src.frames) (n - fade).coerceAtLeast(0) else Int.MAX_VALUE
        for (s in spans) {
            val clip = clips.getOrNull(s.pad) ?: continue
            val src = sounds[s.pad] ?: continue
            if (s.oneShot) {
                put(src, clip.gain * s.velocity, Math.round((s.from - start) * perStep), src.frames)
                continue
            }
            // At least a beat: a clip of 0 beats (a damaged file) would never move on.
            val pass = clip.beats.coerceAtLeast(1) * 4.0
            // B3: the pad's « Fin » (release), as the engine plays it when the pad stops: the last
            // pass goes on past the stop, fading out from there (ClipLauncher::stopSlot).
            val release = Math.round(clip.release.coerceIn(0f, MAX_RELEASE) * rate).toInt()
            var a = s.from
            while (a < s.until) {
                val b = minOf(a + pass, s.until)
                val n = Math.round((b - a) * perStep).toInt()
                val at = Math.round((a - start) * perStep)
                if (b >= s.until && release > fade) put(src, clip.gain * s.velocity, at, n + release, fadeFrom = n, fadeLen = release)
                else put(src, clip.gain * s.velocity, at, n, fadeFrom = cut(n, src), fadeLen = fade)
                a += pass
            }
        }
        return if (heard) Pcm(out, ch, rate) else null
    }
}
