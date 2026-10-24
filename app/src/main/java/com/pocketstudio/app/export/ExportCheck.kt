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
    // Each sample written twice (at and at + TAPS): the last TAPS ones always lie in a row, read
    // without a modulo per tap (a 5-minute song is 29 million samples, each read 36 times).
    private val history = Array(channels) { FloatArray(2 * TAPS) }
    private var at = 0
    private var channel = 0
    var peak = 0.0
        private set

    /** The next interleaved sample (channel after channel). */
    fun add(v: Float) {
        val h = history[channel]
        h[at] = v
        h[at + TAPS] = v
        val newest = at + TAPS
        var best = abs(v.toDouble())
        for (phase in 1 until PHASES) {
            var y = 0.0
            val c = COEFFS[phase]
            for (k in 0 until TAPS) y += c[k] * h[newest - k]
            best = max(best, abs(y))
        }
        if (best > peak) peak = best
        if (++channel == channels) {
            channel = 0
            at = if (at + 1 == TAPS) 0 else at + 1
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

    /** What the guard is doing, for an honest progress line (« Compression M4A », « Vérification », « Nouvel essai 2/3 »…). */
    data class Step(val kind: Kind, val attempt: Int, val of: Int) {
        enum class Kind { Encode, Check, Render }

        /** The line shown under « Je prépare ton morceau… ». */
        fun label(): String = when {
            attempt <= 1 -> if (kind == Kind.Check) tr("Vérification") else tr("Compression M4A")
            of == 0 -> when (kind) {
                // The last resort: the same mix, encoded a little lower.
                Kind.Check -> tr("Réglage du volume : vérification")
                else -> tr("Réglage du volume : compression")
            }
            else -> when (kind) {
                Kind.Render -> tr("Nouvel essai %1\$d/%2\$d : fabrication", attempt, of)
                Kind.Encode -> tr("Nouvel essai %1\$d/%2\$d : compression", attempt, of)
                Kind.Check -> tr("Nouvel essai %1\$d/%2\$d : vérification", attempt, of)
            }
        }
    }

    /**
     * The M4A of a mix already rendered with its limiter at [AacEncoder.CEILING_DBTP] (true peak
     * [sourcePeakDb]): encoded, decoded again; over [MAX_DECODED_DBTP], the mix is rendered again
     * ([render], the limiter's new ceiling → the new mix's true peak) with the ceiling lower by
     * what the decoded file went over plus [MARGIN_LIMIT_DB]: the limiter takes the peaks down and
     * the loudness stays (plain lowering lost up to 6 dB on today's mixes). Still over after
     * [MAX_RENDERS] renders, or with the ceiling under [MIN_CEILING_DBTP]: [encodeWithin] lowers it.
     *
     * Lot 1: [step] is told each step before it starts (the screen names it); a new render, and
     * any lowering after the first, happen only while [mayGoOn] allows that step (the export's
     * time budget: a whole 5-minute song renders in a minute or two on a phone, three renders and
     * five encodings were the « Compression M4A » that never ended).
     */
    suspend fun run(
        sourcePeakDb: Double?,
        render: suspend (ceiling: Float) -> Double?,
        encode: suspend (gain: Float) -> Unit,
        decodedPeak: suspend () -> Double?,
        step: (Step) -> Unit = {},
        mayGoOn: (Step) -> Boolean = { true },
    ): Run {
        var ceiling = AacEncoder.CEILING_DBTP
        var source = sourcePeakDb
        var renders = 1
        var encodings = 0
        while (true) {
            val gain = AacEncoder.headroomGain(source, ceiling)
            step(Step(Step.Kind.Encode, renders, MAX_RENDERS))
            encode(gain)
            encodings++
            step(Step(Step.Kind.Check, renders, MAX_RENDERS))
            val peak = decodedPeak() ?: return Run(null, ceiling, gain, renders, encodings)
            if (peak <= MAX_DECODED_DBTP) return Run(peak, ceiling, gain, renders, encodings)
            val next = (ceiling - (peak - MAX_DECODED_DBTP + MARGIN_LIMIT_DB)).toFloat()
            val again = Step(Step.Kind.Render, renders + 1, MAX_RENDERS)
            if (renders >= MAX_RENDERS || next < MIN_CEILING_DBTP || !mayGoOn(again)) {
                // The last resort: this mix, encoded lower (as [encodeWithin]); the first try always
                // (the file must not saturate), the next ones only within the budget.
                var g = gain * 10.0.pow((MAX_DECODED_DBTP - MARGIN_DB - peak) / 20.0).toFloat()
                var encoded = gain
                var last: Double = peak
                for (tries in 1..MAX_TRIES) {
                    val e = Step(Step.Kind.Encode, tries + 1, 0)
                    if (tries > 1 && !mayGoOn(e)) break
                    step(e)
                    encode(g)
                    encoded = g
                    encodings++
                    step(Step(Step.Kind.Check, tries + 1, 0))
                    last = decodedPeak() ?: return Run(null, ceiling, g, renders, encodings)
                    if (last <= MAX_DECODED_DBTP || tries == MAX_TRIES) break
                    g *= 10.0.pow((MAX_DECODED_DBTP - MARGIN_DB - last) / 20.0).toFloat()
                }
                return Run(last, ceiling, encoded, renders, encodings)
            }
            ceiling = next
            step(again)
            source = render(ceiling)
            renders++
        }
    }

    /**
     * Lot 1 review (M1): the check of an M4A already encoded never fails the export. A file that
     * cannot be read back (an unreadable M4A, a decoder that stops answering:
     * [CodecStalledException]) is kept as encoded: null, [log]ged. A cancel still stops the export.
     */
    fun checkOrKeep(log: (String) -> Unit, read: () -> Double?): Double? = try {
        read()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: CodecStalledException) {
        log(e.message ?: "codec")
        null
    } catch (e: Exception) {
        log(e.message ?: e.javaClass.simpleName)
        null
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

/**
 * Lot 1: an export always ends. [allows] says whether an optional step of the M4A check (a new
 * render of the whole song, a further lowering) still fits in [budgetMs] from the start of the
 * export, from what the last render, encoding and check took ([took]).
 */
class ExportBudget(private val budgetMs: Long = BUDGET_MS, private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private val start = now()
    private val last = HashMap<M4aGuard.Step.Kind, Long>()

    val elapsedMs: Long get() = now() - start

    /** Runs [block] and keeps how long it took as the cost of the next [kind]. */
    suspend fun <T> took(kind: M4aGuard.Step.Kind, block: suspend () -> T): T {
        val t0 = now()
        try {
            return block()
        } finally {
            last[kind] = now() - t0
        }
    }

    /** [step] (and what follows it: a render is then encoded and checked) still fits in the budget. */
    fun allows(step: M4aGuard.Step): Boolean {
        val cost = when (step.kind) {
            M4aGuard.Step.Kind.Render -> cost(M4aGuard.Step.Kind.Render) + cost(M4aGuard.Step.Kind.Encode) + cost(M4aGuard.Step.Kind.Check)
            else -> cost(M4aGuard.Step.Kind.Encode) + cost(M4aGuard.Step.Kind.Check)
        }
        return elapsedMs + cost <= budgetMs
    }

    private fun cost(kind: M4aGuard.Step.Kind) = last[kind] ?: 0L

    companion object {
        /** A shared song: about a minute of work at most beyond its first render and its first check. */
        const val BUDGET_MS = 60_000L
    }
}

/**
 * Lot 1: a render whose progress stays still for [stallMs] is stuck (it is cancelled, the export
 * says so). [armed] false: it watches nothing until [arm] (review M2: the video's render first
 * waits for the project's sounds, up to 15 s and more, at a progress of 0).
 */
class StallWatch(private val stallMs: Long = RENDER_STALL_MS, armed: Boolean = true, private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private var last = Float.NaN
    private var since = now()
    private var armed = armed

    /** The render is really under way: from now on, a still progress counts. */
    fun arm() {
        armed = true
        since = now()
    }

    /** True once [progress] has not moved for [stallMs] (never before [arm]; the progress is followed all the same). */
    fun stalled(progress: Float): Boolean {
        val t = now()
        if (progress != last) {
            last = progress
            since = t
        }
        return armed && t - since > stallMs
    }

    companion object {
        /** A render moves on every few milliseconds: a whole minute still is a stuck one. */
        const val RENDER_STALL_MS = 60_000L
    }
}
