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
