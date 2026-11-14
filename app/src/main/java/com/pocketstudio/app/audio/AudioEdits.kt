package com.pocketstudio.app.audio

import kotlin.math.abs

/** Destructive edits of an audio clip; each returns a new sound (the original stays for Annuler). */
object AudioEdits {
    /** The loudest point brought to -1 dBFS (silence stays silence). */
    fun normalize(pcm: Pcm, ceiling: Float = 0.89f): Pcm {
        val peak = pcm.data.maxOfOrNull { abs(it) } ?: 0f
        if (peak < 1e-6f) return pcm
        val g = ceiling / peak
        return Pcm(FloatArray(pcm.data.size) { pcm.data[it] * g }, pcm.channels, pcm.sampleRate)
    }

    /** The song frame [old]'s first frame is heard on (for [punch]): its step and its whole offset. */
    fun punchOldAt(old: com.pocketstudio.model.AudioClip, perStep: Double, rate: Int): Long =
        Math.round(old.start * perStep) + old.offsetFrames(rate)

    /**
     * The song frame a punch-in's take (started on step [takeStart]) is heard on: as late as [old]
     * was made by « Décaler la prise » (a latency correction the new bars need too), but not by
     * what a cut left in its offset, a place in time of the old sound only (03 M2).
     */
    fun punchNewAt(old: com.pocketstudio.model.AudioClip, takeStart: Int, perStep: Double, rate: Int): Long =
        Math.round(takeStart * perStep) + old.manualOffsetFrames(rate)

    /** Where the louder phrases of a voice take are brought (about -14 dBFS RMS over 50 ms). */
    const val VOICE_LEVEL = 0.2f
    /** …as long as its loudest point stays under this (+3 dBFS: the strip's compressor and the master take it). */
    const val VOICE_PEAK = 1.41f

    /**
     * The clip volume (1..[maxGain]) that brings a microphone take to a usual voice level (03 G2):
     * its louder phrases (the 90th percentile of 50 ms windows above -50 dBFS) at [VOICE_LEVEL],
     * unless its loudest point would go past [VOICE_PEAK]. Never quieter than recorded; 1 for
     * silence. Only the volume changes: the sound file stays as it was.
     */
    fun voiceLevelGain(pcm: Pcm, maxGain: Float = 4f): Float {
        val ch = pcm.channels.coerceAtLeast(1)
        val frames = pcm.frames
        val window = (pcm.sampleRate / 20).coerceAtLeast(1)
        val levels = ArrayList<Float>()
        var peak = 0f
        var f = 0
        while (f < frames) {
            val end = minOf(frames, f + window)
            var sum = 0.0
            for (i in f until end) {
                val v = pcm.data[i * ch]
                sum += v.toDouble() * v
                peak = maxOf(peak, abs(v))
            }
            val rms = kotlin.math.sqrt(sum / (end - f)).toFloat()
            if (rms > 0.00316f) levels.add(rms)
            f = end
        }
        if (levels.isEmpty() || peak < 1e-6f) return 1f
        levels.sort()
        val loud = levels[((levels.size - 1) * 0.9).toInt()]
        val g = minOf(VOICE_LEVEL / loud, VOICE_PEAK / peak).coerceIn(1f, maxGain)
        return Math.round(g * 20f) / 20f
    }

    /** Played backwards (frames reversed, channels kept). */
    fun reverse(pcm: Pcm): Pcm {
        val ch = pcm.channels
        val frames = pcm.frames
        val out = FloatArray(frames * ch)
        for (f in 0 until frames) for (c in 0 until ch) out[f * ch + c] = pcm.data[(frames - 1 - f) * ch + c]
        return Pcm(out, ch, pcm.sampleRate)
    }

    /** Keeps [from]..[to] (fractions 0..1 of the length), with 2 ms ramps so the cuts never click. */
    fun trim(pcm: Pcm, from: Float, to: Float): Pcm {
        val ch = pcm.channels
        val frames = pcm.frames
        val a = (from.coerceIn(0f, 1f) * frames).toInt()
        val b = (to.coerceIn(0f, 1f) * frames).toInt().coerceAtLeast(a + 1).coerceAtMost(frames)
        val out = pcm.data.copyOfRange(a * ch, b * ch)
        val n = b - a
        val ramp = minOf(pcm.sampleRate / 500, n / 2)
        for (i in 0 until ramp) {
            val g = i.toFloat() / ramp
            for (c in 0 until ch) {
                out[i * ch + c] *= g
                out[(n - 1 - i) * ch + c] *= g
            }
        }
        return Pcm(out, ch, pcm.sampleRate)
    }

    /**
     * Punch-in (« Refaire mes. 5-8 »): [old] with the stretch [from] until [until] (frames on the
     * song's clock) taken from [new], the rest kept. [oldAt] and [newAt]: the song frame each
     * sound's first frame is heard on. The new sound fades in and out over [fadeFrames] across
     * the edges (no click, no gap); [old] grows when the stretch goes past its end. The result
     * has [old]'s channels ([new] is mixed down or doubled).
     */
    fun punch(old: Pcm, new: Pcm, oldAt: Long, newAt: Long, from: Long, until: Long, fadeFrames: Int = old.sampleRate / 100): Pcm {
        val ch = old.channels
        val a = (from - oldAt).coerceAtLeast(0L)
        val b = (until - oldAt).coerceAtLeast(a)
        // Room for the fade out after the stretch, when the new take has it.
        val end = maxOf(old.frames.toLong(), minOf(b + fadeFrames, newAt + new.frames - oldAt))
        val frames = end.coerceAtMost(Int.MAX_VALUE.toLong() / ch).toInt()
        val out = FloatArray(frames * ch)
        System.arraycopy(old.data, 0, out, 0, minOf(old.data.size, out.size))
        fun newAt(frame: Long, c: Int): Float {
            val f = frame + oldAt - newAt
            if (f < 0 || f >= new.frames) return 0f
            val i = f.toInt()
            return when {
                new.channels == ch -> new.data[i * ch + c]
                new.channels == 1 -> new.data[i]
                else -> 0.5f * (new.data[i * new.channels] + new.data[i * new.channels + 1])
            }
        }
        val fade = fadeFrames.coerceAtLeast(1)
        val first = (a - fade / 2).coerceAtLeast(0L)
        val last = minOf(b + fade / 2, frames.toLong())
        for (f in first until last) {
            // 0 outside, 1 inside, an equal-power slope across each edge.
            val inEdge = ((f - (a - fade / 2)).toFloat() / fade).coerceIn(0f, 1f)
            val outEdge = (((b + fade / 2) - f).toFloat() / fade).coerceIn(0f, 1f)
            val w = minOf(inEdge, outEdge)
            val gNew = kotlin.math.sin(w * Math.PI.toFloat() / 2f)
            val gOld = kotlin.math.cos(w * Math.PI.toFloat() / 2f)
            val i = f.toInt()
            for (c in 0 until ch) out[i * ch + c] = out[i * ch + c] * gOld + newAt(f, c) * gNew
        }
        return Pcm(out, ch, old.sampleRate)
    }
}
