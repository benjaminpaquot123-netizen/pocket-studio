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
}
