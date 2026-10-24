package com.pocketstudio.app.export

import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.i18n.tr
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * True peak (ITU-R BS.1770: the peak of the wave between the samples, read 4 times oversampled),
 * measured on what a listener's player rebuilds: phase 38 checks the decoded M4A with it. A
 * 48-tap windowed-sinc interpolator (12 taps a phase); reads within 0.1 dB of a 4× reference on
 * music (the test's half-band sine reads its own amplitude).
 */
class TruePeakMeter(private val channels: Int) {
    private val history = Array(channels) { FloatArray(TAPS) }
    private var at = 0
    private var channel = 0
    var peak = 0.0
        private set

    /** The next interleaved sample (channel after channel). */
    fun add(v: Float) {
        val h = history[channel]
        h[at] = v
        var best = abs(v.toDouble())
        for (phase in 1 until PHASES) {
            var y = 0.0
            val c = COEFFS[phase]
            for (k in 0 until TAPS) y += c[k] * h[(at - k + TAPS) % TAPS]
            best = max(best, abs(y))
        }
        if (best > peak) peak = best
        if (++channel == channels) {
            channel = 0
            at = (at + 1) % TAPS
        }
    }

    val peakDb: Double get() = if (peak <= 0.0) -120.0 else 20 * log10(peak)

    companion object {
        const val PHASES = 4
        const val TAPS = 12

        /** COEFFS[p][k]: the weight of the sample k steps back for the point p/4 of a step after the newest but (TAPS/2 - 1) steps late. */
        private val COEFFS: Array<DoubleArray> = Array(PHASES) { p ->
            val half = TAPS / 2
            DoubleArray(TAPS) { k ->
                // Distance (in input samples) from the point read to sample k back, the point sitting half the window back.
                val t = (half - 1 + p.toDouble() / PHASES) - k
                val sinc = if (abs(t) < 1e-9) 1.0 else sin(PI * t) / (PI * t)
                // Hann window over the TAPS samples.
                val w = 0.5 + 0.5 * cos(PI * t / half)
                sinc * w
            }.let { c -> val s = c.sum(); DoubleArray(TAPS) { c[it] / s } }
        }

        /** The true peak (dBTP) of raw interleaved little-endian floats (what [com.pocketstudio.app.audio.AudioDecoder.decodeToFile] writes). */
        fun ofRawFloats(file: File, channels: Int): Double {
            val meter = TruePeakMeter(channels)
            file.inputStream().buffered(1 shl 16).use { input ->
                // Read in blocks (a 3-minute song is 17 million floats).
                val bytes = ByteArray(1 shl 16)
                val floats = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                var kept = 0
                while (true) {
                    val n = input.read(bytes, kept, bytes.size - kept)
                    if (n < 0) break
                    val have = kept + n
                    val whole = have / 4 * 4
                    for (i in 0 until whole step 4) meter.add(floats.getFloat(i))
                    kept = have - whole
                    if (kept > 0) System.arraycopy(bytes, whole, bytes, 0, kept)
                }
            }
            return meter.peakDb
        }
    }
}

/**
 * The M4A never saturates once decoded (phase 38): it is encoded from a source held at
 * [AacEncoder.CEILING_DBTP], then decoded again and its true peak read; over [MAX_DECODED_DBTP]
 * (an encoder that rebuilds the wave higher than measured), it is encoded once more, turned down
 * by what it went over plus a margin. [encode] writes the file at a gain, [decodedPeak] reads its
 * true peak back (null: could not be read, the file is kept as it is).
 */
object M4aGuard {
    const val MAX_DECODED_DBTP = -1.0
    /** Under the limit by this much after a second encoding. */
    const val MARGIN_DB = 0.3
    const val MAX_TRIES = 3

    data class Result(val decodedPeakDb: Double?, val gain: Float, val tries: Int)

    /** Under the limit by this much after the mix is rendered again with its limiter lower. */
    const val MARGIN_LIMIT_DB = 0.2
    /** Renders at most (the first one included); then only the gain is left. */
    const val MAX_RENDERS = 3
    /** The limiter is never asked under this (a squashed mix): the gain takes over. */
    const val MIN_CEILING_DBTP = -9f

    data class Run(val decodedPeakDb: Double?, val ceiling: Float, val gain: Float, val renders: Int, val encodings: Int)

    /**
     * The M4A of a mix already rendered with its limiter at [AacEncoder.CEILING_DBTP] (true peak
     * [sourcePeakDb]): encoded, decoded again; over [MAX_DECODED_DBTP], the mix is rendered again
     * ([render], the limiter's new ceiling → the new mix's true peak) with the ceiling lower by
     * what the decoded file went over plus [MARGIN_LIMIT_DB]: the limiter takes the peaks down and
     * the loudness stays (plain lowering lost up to 6 dB on today's mixes). Still over after
     * [MAX_RENDERS] renders, or with the ceiling under [MIN_CEILING_DBTP]: [encodeWithin] lowers it.
     */
    suspend fun run(
        sourcePeakDb: Double?,
        render: suspend (ceiling: Float) -> Double?,
        encode: suspend (gain: Float) -> Unit,
        decodedPeak: suspend () -> Double?,
    ): Run {
        var ceiling = AacEncoder.CEILING_DBTP
        var source = sourcePeakDb
        var renders = 1
        var encodings = 0
        while (true) {
            val gain = AacEncoder.headroomGain(source, ceiling)
            encode(gain)
            encodings++
            val peak = decodedPeak() ?: return Run(null, ceiling, gain, renders, encodings)
            if (peak <= MAX_DECODED_DBTP) return Run(peak, ceiling, gain, renders, encodings)
            val next = (ceiling - (peak - MAX_DECODED_DBTP + MARGIN_LIMIT_DB)).toFloat()
            if (renders >= MAX_RENDERS || next < MIN_CEILING_DBTP) {
                // The last resort: this mix, encoded lower (as [encodeWithin]).
                var g = gain * 10.0.pow((MAX_DECODED_DBTP - MARGIN_DB - peak) / 20.0).toFloat()
                var last: Double = peak
                for (tries in 1..MAX_TRIES) {
                    encode(g)
                    encodings++
                    last = decodedPeak() ?: return Run(null, ceiling, g, renders, encodings)
                    if (last <= MAX_DECODED_DBTP || tries == MAX_TRIES) break
                    g *= 10.0.pow((MAX_DECODED_DBTP - MARGIN_DB - last) / 20.0).toFloat()
                }
                return Run(last, ceiling, g, renders, encodings)
            }
            ceiling = next
            source = render(ceiling)
            renders++
        }
    }

    fun encodeWithin(gain: Float, encode: (Float) -> Unit, decodedPeak: () -> Double?): Result {
        var g = gain
        var tries = 0
        while (true) {
            encode(g)
            tries++
            val peak = decodedPeak() ?: return Result(null, g, tries)
            if (peak <= MAX_DECODED_DBTP || tries >= MAX_TRIES) return Result(peak, g, tries)
            g *= 10.0.pow((MAX_DECODED_DBTP - MARGIN_DB - peak) / 20.0).toFloat()
        }
    }
}

/**
 * The end-of-export report in plain words (phase 38): how long, how loud, whether it saturates,
 * and « Prêt à partager » when nothing needs a fix. [truePeakDb] is the file's (the decoded M4A's
 * when there is one, [checkedAfterCompression]).
 */
data class ExportSummary(
    val seconds: Double?,
    val lufs: Double?,
    val truePeakDb: Double?,
    val checkedAfterCompression: Boolean = false,
    val sampleRate: Int = 48000,
    val bits: Int = 24,
    val format: String = "WAV",
) {
    /** Over −1 dBTP: a phone or a platform may crackle on it. */
    val saturates: Boolean get() = (truePeakDb ?: -120.0) > M4aGuard.MAX_DECODED_DBTP + 0.05
    val tooQuiet: Boolean get() = lufs != null && lufs < -23.0
    val ready: Boolean get() = lufs != null && !saturates && !tooQuiet

    fun durationLine(): String? = seconds?.let { tr("Durée : %s", durationLabel(it)) }

    fun levelLine(): String? = lufs?.let { tr("Niveau : %s", Loudness.describe(it)) }

    fun peakLine(): String? = truePeakDb?.takeIf { it > -100.0 }?.let { tp ->
        when {
            saturates -> tr("Crête : trop haute (%s dBTP), ça risque de grésiller", LoudnessTargets.format(tp))
            checkedAfterCompression -> tr("Crête : sous la limite, vérifiée après la compression")
            else -> tr("Crête : sous la limite, pas de saturation")
        }
    }

    fun formatLine(): String = when {
        format == "M4A" -> tr("M4A, %s kHz", if (sampleRate == 44100) "44,1" else "48")
        else -> tr("%1\$s, %2\$s kHz, %3\$d bits", format, if (sampleRate == 44100) "44,1" else "48", bits)
    }

    fun verdict(): String = when {
        ready -> tr("Prêt à partager")
        saturates -> tr("À revoir : baisse un peu le volume général avant de partager")
        tooQuiet -> tr("Prêt, mais très doux : monte le volume pour qu'il sonne comme les autres")
        else -> tr("Exporté")
    }

    companion object {
        fun durationLabel(seconds: Double): String {
            val total = seconds.roundToInt()
            return if (total >= 60) tr("%d min %d s", total / 60, total % 60) else tr("%d s", total)
        }
    }
}
