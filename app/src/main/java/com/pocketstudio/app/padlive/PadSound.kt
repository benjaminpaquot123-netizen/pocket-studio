package com.pocketstudio.app.padlive

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.model.LiveClip
import kotlin.math.pow

/**
 * A pad's own sound settings (lot B2: « hauteur », « à l'envers », « attaque ») baked into its
 * sound, read exactly as the engine's ClipLauncher plays it: backwards from its last frame, [LiveClip.pitch]
 * semitones faster or slower with the frames in between read on a straight line, faded in over
 * [LiveClip.attack]. A Live take's pads bounced to an audio track ([com.pocketstudio.app.audio.PadBounce],
 * which starts the sound over on each pass) then sound as they were heard.
 */
object PadSound {
    /** True when [clip] changes its sound (else the sound is used as it is). */
    fun shapes(clip: LiveClip): Boolean = clip.pitch != 0 || clip.reverse || clip.attack > 0f

    fun shaped(src: Pcm, clip: LiveClip): Pcm {
        if (!shapes(clip) || src.frames == 0) return src
        val ch = src.channels
        val frames = src.frames
        val speed = 2.0.pow(clip.pitch / 12.0)
        val n = kotlin.math.ceil(frames / speed).toInt().coerceAtLeast(1)
        val out = FloatArray(n * ch)
        val attack = Math.round(clip.attack * src.sampleRate)
        fun at(i: Int, c: Int): Float = src.data[(if (clip.reverse) frames - 1 - i else i) * ch + c]
        var pos = 0.0
        for (k in 0 until n) {
            if (pos >= frames) break
            val i0 = pos.toInt()
            val frac = (pos - i0).toFloat()
            // The engine's attack: frame k of a pass is played at k / attack of its level.
            val g = if (attack > 0 && k < attack) k.toFloat() / attack else 1f
            for (c in 0 until ch) {
                var v = at(i0, c)
                if (frac > 0f && i0 + 1 < frames) v += frac * (at(i0 + 1, c) - v)
                out[k * ch + c] = v * g
            }
            pos += speed
        }
        return Pcm(out, ch, src.sampleRate)
    }
}
