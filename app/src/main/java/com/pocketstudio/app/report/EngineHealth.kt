package com.pocketstudio.app.report

import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.StreamInfo
import kotlin.math.log10

/**
 * One reading of the sound's health, about four times a second ([HealthMonitor]): what came out
 * of the engine and how the audio thread was doing.
 */
data class HealthSample(
    /** Wall clock, ms. */
    val atMs: Long,
    /** The loudest output sample since the last reading (linear, 1 = 0 dBFS). */
    val peak: Float,
    /** The master's short-term (3 s) and momentary (400 ms) loudness, LUFS ([NativeEngine.LOUDNESS_NONE]: none yet). */
    val shortTermLufs: Float = NativeEngine.LOUDNESS_NONE,
    val momentaryLufs: Float = NativeEngine.LOUDNESS_NONE,
    /** The limiter's gain now (linear, 1 = untouched). */
    val limiterGain: Float = 1f,
    /** The audio thread's load (render time over the sound's duration; near 1 it crackles). */
    val cpu: Float = 0f,
    /** Underruns since the engine started (cumulative), and output streams reopened. */
    val xruns: Int = 0,
    val restarts: Int = 0,
    val running: Boolean = false,
    /** A NaN or an infinity reached the output (the engine silenced it). */
    val nonFinite: Boolean = false,
    /** The transport was playing. */
    val playing: Boolean = false,
    /** The master volume's song automation at that moment (0..1), null: no such lane. */
    val masterLane: Float? = null,
)

/** What the app (not the engine) knows at a reading: the transport and the master's song lane. */
data class HealthProbe(val playing: Boolean, val masterLane: Float?)

/**
 * The engine's health over the last minute, sampled from the UI thread: the stream's peak (taken
 * every 100 ms by the app's stream poll) is held over [PERIOD_MS], then one [HealthSample] is kept
 * with the meters that take nothing from the other screens ([NativeEngine.healthMeters]). The
 * underruns, stream restarts and NaN alarms are also written in the [Journal] as they happen.
 * Never touches the audio thread: it only reads atomics through JNI.
 */
object HealthMonitor {
    const val PERIOD_MS = 250L
    /** 60 s at 4 readings a second. */
    const val KEEP = 240
    /** Under this peak (-80 dBFS) and stopped, nothing is going out: no need to rewrite the copy. */
    const val SOUND_FLOOR = 1e-4f

    val samples = Ring<HealthSample>(KEEP)

    /** Moves with each reading while sound goes out (playing, or any level): the live copy follows it. */
    val version: Long get() = counter.get()
    private val counter = java.util.concurrent.atomic.AtomicLong()

    /** Set by the screen that knows the transport and the project (null: neither known). */
    @Volatile var probe: (() -> HealthProbe)? = null

    private var windowStart = -1L
    private var windowPeak = 0f
    private var lastXruns = -1
    private var lastRestarts = -1
    private var lastNonFinite = false
    private val meters = FloatArray(NativeEngine.HEALTH)

    /** One stream poll ([info], read [elapsedMs] since boot, at [wallMs]); [readMeters] fills [NativeEngine.HEALTH] floats. */
    fun feed(info: StreamInfo, elapsedMs: Long, wallMs: Long, readMeters: (FloatArray) -> Unit = NativeEngine::healthMeters): HealthSample? = synchronized(this) {
        if (info.peak.isFinite()) windowPeak = maxOf(windowPeak, info.peak)
        if (windowStart < 0) windowStart = elapsedMs
        if (elapsedMs - windowStart < PERIOD_MS) return null
        meters.fill(0f)
        meters[0] = NativeEngine.LOUDNESS_NONE
        meters[1] = NativeEngine.LOUDNESS_NONE
        meters[2] = 1f
        runCatching { readMeters(meters) }
        val p = runCatching { probe?.invoke() }.getOrNull()
        val sample = HealthSample(
            atMs = wallMs, peak = windowPeak, momentaryLufs = meters[0], shortTermLufs = meters[1], limiterGain = meters[2], cpu = meters[3],
            xruns = info.xruns, restarts = info.restarts, running = info.running, nonFinite = info.nonFinite,
            playing = p?.playing ?: false, masterLane = p?.masterLane,
        )
        add(sample)
        windowStart = elapsedMs
        windowPeak = 0f
        sample
    }

    /** Keeps [sample] and writes what changed for the worse in the journal. */
    fun add(sample: HealthSample) = synchronized(this) {
        if (lastXruns >= 0 && sample.xruns > lastXruns) Journal.app("Décrochage audio : +${sample.xruns - lastXruns} (total ${sample.xruns})")
        if (lastRestarts >= 0 && sample.restarts > lastRestarts) Journal.app("Son rouvert par Android (n° ${sample.restarts})")
        if (sample.nonFinite && !lastNonFinite) Journal.error("moteur", "Valeur invalide (NaN) en sortie : son coupé par sécurité")
        lastXruns = sample.xruns
        lastRestarts = sample.restarts
        lastNonFinite = sample.nonFinite
        samples.add(sample)
        if (sample.playing || sample.peak > SOUND_FLOOR) counter.incrementAndGet()
    }

    internal fun reset() = synchronized(this) {
        samples.clear()
        windowStart = -1L
        windowPeak = 0f
        lastXruns = -1
        lastRestarts = -1
        lastNonFinite = false
        probe = null
    }
}

/** One second of the last minute, from the samples in it (null fields: no sample that second). */
data class HealthSecond(
    val peakDb: Float?,
    val lufs: Float?,
    val cpu: Float?,
    val xrun: Boolean = false,
    val restart: Boolean = false,
    val nan: Boolean = false,
    val playing: Boolean = false,
)

object HealthSummary {
    /** Peaks under this show as silence. */
    const val FLOOR_DB = -60f
    /** From quiet to loud: one character per 6 dB, from [FLOOR_DB] to 0 dBFS. */
    const val RAMP = "_.:-=+*#%@"

    fun db(linear: Float): Float = if (linear > 1e-6f) (20.0 * log10(linear.toDouble())).toFloat() else -120f

    /**
     * The [seconds] seconds before [nowMs], oldest first, each from the samples taken in it: the
     * loudest peak, the last short-term loudness, the highest load, and whether an underrun, a
     * stream restart or a NaN happened in it (against the sample before).
     */
    fun perSecond(samples: List<HealthSample>, nowMs: Long, seconds: Int = 60): List<HealthSecond> {
        val buckets = Array(seconds) { mutableListOf<HealthSample>() }
        samples.forEach { s ->
            val ago = ((nowMs - s.atMs) / 1000).toInt()
            if (ago in 0 until seconds) buckets[seconds - 1 - ago].add(s)
        }
        // The reading just before the window: an underrun in its first second counts against it.
        var prev: HealthSample? = samples.lastOrNull { (nowMs - it.atMs) / 1000 >= seconds }
        return buckets.map { b ->
            if (b.isEmpty()) return@map HealthSecond(null, null, null)
            val before = prev
            prev = b.last()
            HealthSecond(
                peakDb = db(b.maxOf { it.peak }),
                lufs = b.last().shortTermLufs.takeIf { it > NativeEngine.LOUDNESS_NONE + 1f },
                cpu = b.maxOf { it.cpu },
                xrun = before != null && b.last().xruns > before.xruns || b.zipWithNext().any { (a, c) -> c.xruns > a.xruns },
                restart = before != null && b.last().restarts > before.restarts || b.zipWithNext().any { (a, c) -> c.restarts > a.restarts },
                nan = b.any { it.nonFinite },
                playing = b.any { it.playing },
            )
        }
    }

    /** One character per second: its peak on [RAMP] (a space: no reading that second). */
    fun sparkline(seconds: List<HealthSecond>): String = seconds.joinToString("") { s ->
        val db = s.peakDb ?: return@joinToString " "
        if (db < FLOOR_DB) RAMP.first().toString()
        else RAMP[(((db - FLOOR_DB) / -FLOOR_DB) * (RAMP.length - 1)).toInt().coerceIn(0, RAMP.length - 1)].toString()
    }

    /** Under the sparkline: x an underrun, R a stream restart, ! a NaN, > the music playing (else a dot). */
    fun markers(seconds: List<HealthSecond>): String = seconds.joinToString("") { s ->
        when {
            s.nan -> "!"
            s.restart -> "R"
            s.xrun -> "x"
            s.playing -> ">"
            s.peakDb == null -> " "
            else -> "."
        }
    }
}
