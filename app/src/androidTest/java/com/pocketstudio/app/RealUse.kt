package com.pocketstudio.app

import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketstudio.engine.NativeEngine
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin

/**
 * What the « tests d'usage réel » (docs/TESTS-REELS.md) share on a device: the app's files and
 * settings reset as on a new phone, waiting, the output's peak, memory, the main thread's
 * responsiveness, the share sheet caught, a WAV's and a decoded M4A's length and true peak.
 */
object RealUse {
    const val TAG = "RealUse"
    val instrumentation: Instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val app: Application get() = instrumentation.targetContext.applicationContext as Application

    fun log(line: String) = Log.i(TAG, line)

    /**
     * What the tests grant up front, as a user would at the first ask: the microphone, and on
     * Android 13+ the notifications (asked at the first recording: its dialog would stay over the app).
     */
    val PERMISSIONS: Array<String> = listOfNotNull(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.POST_NOTIFICATIONS.takeIf { android.os.Build.VERSION.SDK_INT >= 33 },
    ).toTypedArray()

    /** The CI's x86_64 emulator (« ranchu »): its audio is not a phone's. */
    val onEmulator: Boolean get() = android.os.Build.HARDWARE == "ranchu" || android.os.Build.HARDWARE == "goldfish"

    /** The app as on a phone where it was just installed: no project, no setting, no screen seen. */
    fun freshInstall() {
        instrumentation.runOnMainSync { if (NativeEngine.isAvailable) NativeEngine.stop() }
        // A save still running from the screen just closed would bring a project folder back.
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        for (dir in listOf("projects", "projects-trash", "stems")) File(app.filesDir, dir).deleteRecursively()
        File(app.cacheDir, "exports").deleteRecursively()
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { f ->
            val name = f.name.removeSuffix(".xml")
            app.getSharedPreferences(name, 0).edit().clear().commit()
        }
        com.pocketstudio.app.settings.SettingsStore.forgetForTests()
        com.pocketstudio.app.settings.SettingsStore.load(app)
        com.pocketstudio.app.onboarding.LaunchRule.askAtStartForTest = true
    }

    fun waitFor(what: String, seconds: Int, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1000L
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long ($seconds s) for $what" }
            Thread.sleep(50)
        }
    }

    /** Highest output sample since the last look (the app's own poll may have taken some: both are read). */
    fun peakNow(): Float = maxOf(NativeEngine.streamInfo().peak, Metrics.streamInfo.value.peak)

    /** The stream as this device runs it, once in the log (the CI emulator's differs from a phone's). */
    fun describeStream(): String = NativeEngine.streamInfo().let {
        "stream: ${it.sampleRate} Hz, burst ${it.framesPerBurst}, buffer ${it.bufferSize}/${it.bufferCapacity}, aaudio=${it.aaudio} " +
            "mmap=${it.mmap} exclusive=${it.exclusive} lowLatency=${it.lowLatency} latency=${"%.1f".format(it.outputLatencyMs)} ms " +
            "xruns=${it.xruns} callbacks=${it.callbacks} running=${it.running}"
    }.also { log(it) }

    fun db(x: Float) = if (x > 1e-9f) 20 * log10(x.toDouble()) else -200.0

    /** Java heap in use (after a collection) and native heap allocated, in MB. */
    fun memoryMb(): Pair<Double, Double> {
        Runtime.getRuntime().gc()
        System.runFinalization()
        Runtime.getRuntime().gc()
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / 1048576.0 to Debug.getNativeHeapAllocatedSize() / 1048576.0
    }

    /** How long the main thread takes to run an empty task (an ANR is 5 s of this). */
    fun mainThreadDelayMs(timeoutSeconds: Long = 10): Long {
        val start = System.nanoTime()
        val done = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { done.countDown() }
        check(done.await(timeoutSeconds, TimeUnit.SECONDS)) { "the main thread did not answer in $timeoutSeconds s (ANR)" }
        return (System.nanoTime() - start) / 1_000_000
    }

    /**
     * The main thread watched for a whole round: a task posted every 500 ms, the longest it took
     * to run (an ANR is 5 s of this), so a block between two looks is seen too.
     */
    class MainWatchdog : AutoCloseable {
        @Volatile var worstMs = 0L
            private set
        @Volatile private var running = true
        private val thread = Thread {
            while (running) {
                runCatching { worstMs = maxOf(worstMs, mainThreadDelayMs(timeoutSeconds = 30)) }.onFailure { worstMs = maxOf(worstMs, 30_000) }
                Thread.sleep(500)
            }
        }.apply { isDaemon = true; name = "main-watchdog"; start() }

        override fun close() {
            running = false
            thread.join(35_000)
        }
    }

    /**
     * The system's share sheet the app opens, caught before it shows (it would take the screen):
     * [check] gets what the app asked to share.
     */
    class ShareCatcher : AutoCloseable {
        @Volatile var lastIntent: Intent? = null
            private set
        @Volatile var hits = 0
            private set
        private val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CHOOSER) return null
                this@ShareCatcher.lastIntent = intent
                this@ShareCatcher.hits++
                return Instrumentation.ActivityResult(0, null)
            }
        }.also { instrumentation.addMonitor(it) }

        override fun close() = instrumentation.removeMonitor(monitor)
    }

    /** A WAV file's frames and rate (any PCM or float WAV the app writes). */
    fun wavFrames(file: File): Pair<Long, Int> {
        RandomAccessFile(file, "r").use { f ->
            val head = ByteArray(12).also { f.readFully(it) }
            check(String(head, 0, 4) == "RIFF" && String(head, 8, 4) == "WAVE") { "${file.name}: not a WAV" }
            var channels = 0
            var rate = 0
            var bits = 0
            while (f.filePointer + 8 <= f.length()) {
                val h = ByteArray(8).also { f.readFully(it) }
                val id = String(h, 0, 4)
                val size = ByteBuffer.wrap(h, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                if (id == "fmt ") {
                    val fmt = ByteArray(16).also { f.readFully(it) }
                    val b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                    b.short
                    channels = b.short.toInt()
                    rate = b.int
                    b.int
                    b.short
                    bits = b.short.toInt()
                    f.seek(f.filePointer + size - 16 + (size and 1))
                } else if (id == "data") {
                    check(channels > 0 && bits > 0) { "${file.name}: data before fmt" }
                    val bytes = minOf(size, f.length() - f.filePointer)
                    return bytes / (channels * bits / 8) to rate
                } else {
                    f.seek(f.filePointer + size + (size and 1))
                }
            }
        }
        error("${file.name}: no data chunk")
    }

    /** A decoded file's length (s) and true peak (dBTP, 4x oversampled as ITU-R BS.1770 reads it). */
    class Decoded(val seconds: Double, val truePeakDb: Double, val sampleRate: Int, val channels: Int)

    /** [file] decoded by the phone's decoder (as a player plays it), measured here. */
    fun decode(file: File): Decoded {
        val raw = File(app.cacheDir, "realuse-${System.nanoTime()}.raw")
        try {
            val audio = com.pocketstudio.app.audio.AudioDecoder.decodeToFile(app, android.net.Uri.fromFile(file), raw, 1200.0, { false })
            val tp = TruePeak(audio.channels)
            raw.inputStream().buffered(1 shl 16).use { input ->
                val buf = ByteArray(1 shl 16)
                val floats = FloatArray(buf.size / 4)
                var carry = 0
                while (true) {
                    val n = input.read(buf, carry, buf.size - carry)
                    if (n < 0) break
                    val total = carry + n
                    val whole = total / 4
                    ByteBuffer.wrap(buf, 0, whole * 4).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floats, 0, whole)
                    tp.add(floats, whole)
                    carry = total - whole * 4
                    if (carry > 0) System.arraycopy(buf, whole * 4, buf, 0, carry)
                }
            }
            return Decoded(audio.seconds, tp.db(), audio.sampleRate, audio.channels)
        } finally {
            raw.delete()
        }
    }

    /**
     * True peak, interleaved samples in: each channel upsampled 4x by a Kaiser-windowed sinc
     * (16 taps a phase, far finer than the 0.1 dB a meter rounds to), the highest absolute value.
     */
    class TruePeak(private val channels: Int) {
        private val phases = 4
        private val taps = 16
        // Phase p reads the point p/4 of a sample after x[m - 7] (m the newest): tap k weighs x[m - k].
        private val h = Array(phases) { p ->
            DoubleArray(taps) { k ->
                val n = k - taps / 2 + 1 - p / phases.toDouble()  // that sample's distance from the point read
                val x = PI * n
                val sinc = if (abs(n) < 1e-12) 1.0 else sin(x) / x
                sinc * kaiser(((n + taps / 2) / taps).coerceIn(0.0, 1.0), 8.0)
            }
        }
        private val history = Array(channels) { DoubleArray(taps) }
        private var peak = 0.0
        private var channel = 0

        fun add(samples: FloatArray, count: Int) {
            for (i in 0 until count) {
                val x = samples[i].toDouble()
                peak = max(peak, abs(x))
                val hist = history[channel]
                // The newest sample at the end (a short shift: 16 values).
                System.arraycopy(hist, 1, hist, 0, taps - 1)
                hist[taps - 1] = x
                for (p in 1 until phases) {
                    var y = 0.0
                    val c = h[p]
                    for (k in 0 until taps) y += c[k] * hist[taps - 1 - k]
                    peak = max(peak, abs(y))
                }
                channel = (channel + 1) % channels
            }
        }

        fun db() = if (peak > 1e-12) 20 * log10(peak) else -200.0

        private fun kaiser(t: Double, beta: Double): Double {
            val r = 2 * t - 1
            return besselI0(beta * kotlin.math.sqrt(max(0.0, 1 - r * r))) / besselI0(beta)
        }

        private fun besselI0(x: Double): Double {
            var sum = 1.0
            var term = 1.0
            var k = 1
            while (term > 1e-12 * sum) {
                term *= (x / (2 * k)) * (x / (2 * k))
                sum += term
                k++
            }
            return sum
        }
    }
}
