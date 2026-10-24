package com.pocketstudio.app.library

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.Loudness
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * Lot 1 (APK 181: the assistant's « Nappe / accords » step much louder than the others): every
 * excerpt played to choose a sound (the assistant's steps, the library, real instruments, pack
 * samples, the demos) is brought to one loudness, [REFERENCE_LUFS], measured the same way for
 * all (the loudest 3 s, K-weighted: [Loudness.Meter.shortTermMax]). A held chord, a pluck, a
 * sample from a pack or a General MIDI piano then sound as loud as each other: only the sound
 * is compared, not its level. Within [MAX_BOOST_DB] up, its peak kept under [CEILING_DBFS]: a
 * very short, very peaky sound (a hat) may stay a little under.
 */
object PreviewLevel {
    /** The level of every preview (lot 1: the assistant's excerpts spread from -6.6 to -17.6; the peakiest, a 303 lead, reaches -17.4 at the ceiling). */
    const val REFERENCE_LUFS = -16.0
    /** How far a preview is matched: within this of [REFERENCE_LUFS] (unless its peak holds it under). */
    const val TOLERANCE_LU = 1.5
    /** The most a quiet sound is lifted. */
    const val MAX_BOOST_DB = 18.0
    /** The most a loud one is turned down. */
    const val MAX_CUT_DB = 30.0
    /** Samples never go over this once matched. */
    const val CEILING_DBFS = -1.0

    /** [pcm]'s loudness as heard (a mono sound plays on both sides: +3 dB); null when silent. */
    fun loudness(pcm: Pcm): Double? {
        if (pcm.channels < 1 || pcm.data.size < pcm.channels) return null
        val meter = Loudness.Meter(pcm.sampleRate, pcm.channels)
        meter.add(pcm.data, pcm.data.size / pcm.channels)
        val l = meter.shortTermMax() ?: return null
        return if (pcm.channels == 1) l + 10 * log10(2.0) else l
    }

    /** The gain in dB [pcm] is played with (0 for silence). */
    fun gainDb(pcm: Pcm): Double {
        val l = loudness(pcm) ?: return 0.0
        if (l < -70.0) return 0.0
        var g = (REFERENCE_LUFS - l).coerceIn(-MAX_CUT_DB, MAX_BOOST_DB)
        val peak = pcm.data.maxOfOrNull { abs(it) }?.toDouble() ?: 0.0
        if (peak > 0.0) g = minOf(g, CEILING_DBFS - 20 * log10(peak))
        return g
    }

    /** [pcm] at the preview level (the same object when nothing changes). */
    fun normalize(pcm: Pcm): Pcm {
        val g = gainDb(pcm)
        if (abs(g) < 0.05) return pcm
        val k = 10.0.pow(g / 20).toFloat()
        return Pcm(FloatArray(pcm.data.size) { pcm.data[it] * k }, pcm.channels, pcm.sampleRate)
    }
}
