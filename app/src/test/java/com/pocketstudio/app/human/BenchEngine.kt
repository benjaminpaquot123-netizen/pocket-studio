package com.pocketstudio.app.human

import android.os.SystemClock
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.OfflineExport
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The phone's engine as far as the « testeur humain » can tell, for « Partager » (lot « tuto 4 »,
 * É0): Robolectric has no native engine, so the share said « le moteur audio n'est pas prêt »
 * where a phone exports. While the bench sets it as [com.pocketstudio.engine.NativeEngine.sink],
 * each render writes the length asked (the song, its tail) of a sound made from what the app described
 * to that private engine ([Voice]: its notes, levels and tempo; a WAV the app then compresses, checks,
 * measures and shares as it would a song), and the live engine's commands are let go. Never in the app.
 *
 * The Remix (2026-10-28): a separated song's stems are read from their WAVs when asked (a piece
 * for « → Pad », the waveforms), and where the song is while it plays along the project or alone
 * (« Écouter ») follows the bench's clock, as the engine's would.
 */
internal class BenchEngine : CommandSink {
    private var next = 1L

    /** Renders asked for since this engine was made (tests). */
    @Volatile var renders = 0
        private set

    override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
        if (target != 0L) {
            // Audit ultra round 2 (n° 8): a private engine's loudness target (NativeEngine.setLoudnessTarget, 0: none),
            // honoured by its render as the native engine's two-pass master does (host test mp_club_and_streaming_exports_differ).
            if (type == CMD_MASTER && a == MASTER_TARGET) targets[target] = value
            // Audit ultra round 3 (lot A d): what the project is, heard in its render (two projects never read the same).
            described.getOrPut(target) { Described() }.add(type, value, a, b)
            return
        }
        when (type) {
            CMD_SET_TRANSPORT -> {
                if (value <= 0f) freezeTakes()
                // A stop ends a mark the take was waiting for (a phone's engine drops it with its song position).
                if (value <= 0f && markAsked > 0) markAsked = -1
                transportSince = if (value > 0f) SystemClock.uptimeMillis() else -1L
                stepBase = 0.0
                countInBars = if (value > 0f) a.coerceAtLeast(0) else 0
                // ▶ as Engine.cpp's SetTransport: the scene starts on step 0, the song at song step [b] (OUTSIDE_SONG, -1:
                // a scene alone); a swap armed before is dropped. ■: nothing waits any more.
                song.start(inSong = value > 0f && b >= 0, songStep = b.coerceAtLeast(0))
            }
            // A tempo change while playing: the steps already played are kept (the clock does not jump back or ahead).
            CMD_SET_TEMPO -> if (value > 0f) {
                val since = transportSince
                if (since >= 0) {
                    val now = SystemClock.uptimeMillis()
                    stepBase += (now - since) * tempo / 15_000.0
                    transportSince = now
                }
                tempo = value.toDouble()
            }
            // Audit method v2 (engine always there): « Enregistrer la performance » marks the song on the next launch line and
            // waits for the engine to say it did (NativeEngine.songMarkStep); the bench's never said it, the take stayed « Prêt ».
            CMD_MARK_SONG -> {
                markQuantum = a.coerceAtLeast(0)
                markAsked = transportSteps.coerceAtLeast(-1).let { if (it < 0) 0 else it + 1 }
            }
            // Banc fiable 3.2 (T4-17) and its review: the scene swaps as Engine.cpp arms and plays them (see [SongClock]).
            CMD_ARM_SCENE_SWAP -> song.arm(a, b, value, stepsNow())
            CMD_BEATS_PER_BAR -> if (a > 0) beatsPerBar = a
            CMD_STEM_START -> stemStart = a.toLong()
            CMD_PLAY_PREVIEW -> previewSince = if (value > 0f) SystemClock.uptimeMillis() else -1L
            // Round 1 (lot banc, « Montre-moi » silent): the pads and notes the app plays through the engine, heard by the bench.
            CMD_TRIGGER_PAD -> hits.add(a)
            CMD_SYNTH_NOTE_ON -> notesOn.incrementAndGet()
        }
    }

    /** The pads triggered since the last [heardHits] (lanes, in order). */
    private val hits = java.util.concurrent.ConcurrentLinkedQueue<Int>()
    private val notesOn = java.util.concurrent.atomic.AtomicInteger()

    /** What the app played on the engine since the last call: the pads' lanes, and how many synth notes. */
    fun heardHits(): Pair<List<Int>, Int> {
        val lanes = generateSequence { hits.poll() }.toList()
        return lanes to notesOn.getAndSet(0)
    }

    // --- The microphone of the audio tracks (round 1, lot banc; NativeEngine.micStart & co. through the sink) ----

    /** A take armed: how long it may last, the count-in frames kept in front, what it recorded once the transport stopped. */
    private class Take(val seconds: Int, val preroll: Int) {
        @Volatile var frozen = -1L
    }

    private val takes = ConcurrentHashMap<Long, Take>()

    /** How long the count-in lasts before the take records (ms; set by the bench from the project's tempo). */
    @Volatile var countInMs = 2_000L

    /** Takes recorded (tests). */
    @Volatile var micTakes = 0
        private set

    @Synchronized override fun micStart(onStart: Boolean, seconds: Int, prerollFrames: Int): Long =
        (next++).also { takes[it] = Take(seconds, prerollFrames) }

    /** Frames the take has recorded: from the end of the count-in after ▶, as long as the transport runs. */
    private fun recorded(t: Take): Long {
        if (t.frozen >= 0) return t.frozen
        val since = transportSince
        if (since < 0) return 0L
        val ms = SystemClock.uptimeMillis() - since - countInMs
        return (ms * RATE / 1000).coerceIn(0L, t.seconds.toLong() * RATE)
    }

    override fun micStatus(handle: Long): FloatArray {
        val t = takes[handle] ?: return floatArrayOf(2f, 0f, 0f)
        val frames = recorded(t)
        val state = when {
            // The transport stopped after it started (or the take is full): over, as the engine's.
            t.frozen >= 0 || frames >= t.seconds.toLong() * RATE -> 2f
            // Before ▶, then the count-in: waiting for the « 1 ».
            transportSince < 0 || SystemClock.uptimeMillis() - transportSince < countInMs -> 0f
            else -> 1f
        }
        return floatArrayOf(state, frames.toFloat(), if (state == 1f) BenchMic.PEAK else 0.01f)
    }

    override fun micFinish(handle: Long): FloatArray? {
        val t = takes.remove(handle) ?: return null
        val frames = recorded(t).toInt()
        micTakes++
        return FloatArray(t.preroll + frames) { i -> if (i < t.preroll) 0f else BenchMic.voice((i - t.preroll).toLong()) }
    }

    /** The transport stops: each take keeps what it recorded until then (at most [upTo] frames: the song's last bar line). */
    private fun freezeTakes(upTo: Long = Long.MAX_VALUE) {
        if (transportSince >= 0) takes.values.forEach { if (it.frozen < 0) it.frozen = minOf(recorded(it), upTo.coerceAtLeast(0L)) }
    }

    override fun scheduleFrom(target: Long, frame: Long) = Unit

    override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
        if (target != 0L) {
            if (sample != 0L) described.getOrPut(target) { Described() }.add(INSTALL + kind.ordinal, 1f, index, 0)
            return
        }
        when (kind) {
            CommandSink.Install.Stems -> playedStems = sample
            CommandSink.Install.PreviewStems -> previewStems = sample
            else -> Unit
        }
    }
    @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
    override fun sampleFrames(handle: Long): Long = OfflineExport.SAMPLE_RATE.toLong()
    override fun sampleRelease(handle: Long) = Unit
    @Synchronized override fun exportCreate(): Long = next++
    override fun exportDestroy(target: Long) {
        targets.remove(target)
        described.remove(target)
    }

    /** Each private engine's loudness target (LUFS), as described to it. */
    private val targets = ConcurrentHashMap<Long, Float>()

    /** What the app described to each private engine (its project), see [Described]. */
    private val described = ConcurrentHashMap<Long, Described>()

    /**
     * A project as its description reaches a private engine: how many notes and hits, its channels' levels, its
     * tempo, and a print of every command (two projects, or a loop of 1 bar and a song of 4, never sound the same).
     */
    private class Described {
        private var notes = 0
        private var levels = 0.0
        private var channels = 0
        private var print = 1469598103934665603L
        var bpm = 120.0
            private set

        @Synchronized fun add(type: Int, value: Float, a: Int, b: Int) {
            for (x in intArrayOf(type, a, b, value.toBits())) print = (print xor x.toLong()) * 1099511628211L
            when (type) {
                CMD_SET_DRUM_HIT, CMD_SYNTH_NOTE, CMD_SET_ACID_STEP -> if (value > 0f || type == CMD_SET_ACID_STEP && b != 0) notes++
                CMD_CHANNEL_VOLUME -> { levels += value.coerceIn(0f, 2f); channels++ }
                CMD_SET_TEMPO -> if (value in 20f..400f) bpm = value.toDouble()
            }
        }

        /** The sound it gives: louder the more it plays (−24 to −9 dBFS), its pitch and colour from its print. */
        @Synchronized fun voice(rate: Int): Voice {
            val level = if (channels == 0) 0.8 else levels / channels
            val busy = 1.0 - kotlin.math.exp(-notes / 40.0)
            val amp = (0.063 + 0.29 * busy) * (level / 0.8).coerceIn(0.25, 1.25)
            val h = (print ushr 17).toInt() and 0x7FFFFFFF
            return Voice(rate, amp.coerceAtMost(0.35), 110.0 * 2.0.pow((h % 25) / 12.0), 0.1 + (h / 25 % 20) / 100.0, bpm)
        }
    }

    /**
     * A tone at [freq] Hz with its second harmonic ([colour]), pulsing on each beat at [bpm] (stereo, the right
     * a little quieter). Made from two tables (a cycle, a beat): minutes of it cost little.
     */
    internal class Voice(val rate: Int, val amp: Double, freq: Double, colour: Double, bpm: Double) {
        private val cycle = FloatArray((rate / freq).roundToInt().coerceAtLeast(8)) { i ->
            val t = 2 * PI * i / ((rate / freq).roundToInt().coerceAtLeast(8))
            (amp * (sin(t) + colour * sin(2 * t + 0.7)) / (1 + colour)).toFloat()
        }
        private val beat = FloatArray((rate * 60.0 / bpm).roundToInt().coerceAtLeast(1)) { i -> (0.78 + 0.22 * kotlin.math.exp(-i / (0.09 * rate))).toFloat() }

        /** The sound at frame [i] (left; the right is 0.9 of it). */
        fun at(i: Long): Float = cycle[(i % cycle.size).toInt()] * beat[(i % beat.size).toInt()]
    }

    /**
     * [count] frames from [from] of a render [total] frames long, its sound [sound] frames then dying away (the
     * tail), the last [fade] frames fading to silence; stereo interleaved into [out].
     */
    private fun fill(v: Voice, out: FloatArray, from: Long, count: Int, sound: Long, total: Long, fade: Long, gain: Float) {
        val tail = 0.05 * v.rate
        for (k in 0 until count) {
            val i = from + k
            var x = v.at(i) * gain
            if (i >= sound) x *= kotlin.math.exp(-(i - sound) / tail).toFloat()
            if (fade > 0 && i >= total - fade) x *= ((total - i).toFloat() / fade).coerceIn(0f, 1f)
            out[2 * k] = x
            out[2 * k + 1] = 0.9f * x
        }
    }

    /** What [target]'s render plays: its project's [Voice] at [rate] (a project described to no one: a quiet one). */
    private fun voiceOf(target: Long, rate: Int): Voice = (described[target] ?: Described()).voice(rate)

    /**
     * The gain that brings [target]'s render to its loudness target (1 without one), measured with the app's own meter
     * over the whole of it, its peak kept under −1 dBFS as the master's limiter would: a club and a streaming export
     * read their own targets (round 2), each project with its own peak.
     */
    private fun targetGain(target: Long, v: Voice, total: Long, sound: Long, fade: Long): Float {
        val lufs = targets[target]?.takeIf { it < 0f } ?: return 1f
        val meter = com.pocketstudio.app.export.Loudness.Meter(v.rate, 2)
        val piece = FloatArray(2 * CHUNK)
        var peak = 0f
        var at = 0L
        while (at < total) {
            val n = minOf(CHUNK.toLong(), total - at).toInt()
            fill(v, piece, at, n, sound, total, fade, 1f)
            for (k in 0 until 2 * n) peak = maxOf(peak, abs(piece[k]))
            meter.add(piece, n)
            at += n
        }
        val measured = meter.integrated() ?: return 1f
        val gain = 10.0.pow((lufs - measured) / 20.0)
        return minOf(gain, 10.0.pow(-1.0 / 20.0) / peak.coerceAtLeast(1e-6f)).toFloat()
    }

    /** Each render's progress (lot « tuto 5 »: the bar moves as on a phone) and its « Annuler ». */
    private val progress = java.util.concurrent.ConcurrentHashMap<Long, Float>()
    private val cancelled: MutableSet<Long> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    override fun exportProgress(target: Long): Float = progress[target] ?: 0f
    override fun exportCancel(target: Long) { cancelled += target }

    override fun exportRender(
        target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
        sampleRate: Int, bits: Int,
    ): ExportResult {
        renders++
        // A phone renders a 3-minute song in a few seconds: the bench takes [RENDER_MS], its progress moving. Waited in slices
        // of 50 ms: a cancel (the app's, or the bench's at the end of a test: [cancelAllRenders]) ends it at once, not after a
        // step of a long render (PLAN-RESTE 4.5: a 60 s export of BenchTour1Test ran on into the tests after it).
        for (k in 1..STEPS) {
            val until = System.currentTimeMillis() + renderMs / STEPS
            while (true) {
                if (target in cancelled || cancelAll) return ExportResult.Cancelled
                val left = until - System.currentTimeMillis()
                if (left <= 0) break
                Thread.sleep(minOf(50L, left))
            }
            progress[target] = k / STEPS.toFloat()
        }
        // The length asked (round 3, lot A d: always « 3 s » before): the song, then its tail (with autoTail, the engine
        // stops once the sound has died away: 0,5 s here), at the file's own rate.
        val tail = if (autoTail) minOf(tailFrames, RATE / 2L) else tailFrames
        val scale = sampleRate.toDouble() / RATE
        val total = Math.round(minOf(frames + tail, MAX_SECONDS * RATE) * scale).coerceAtLeast(1L)
        val sound = Math.round(frames * scale)
        val fade = Math.round(fadeOutFrames * scale)
        val v = voiceOf(target, sampleRate)
        val g = targetGain(target, v, total, sound, fade)
        writeWav(File(path), total, sampleRate) { out, from, n -> fill(v, out, from, n, sound, total, fade, g) }
        return ExportResult.Ok
    }

    /** How long a render takes here (real time), [RENDER_MS] unless a test wants it longer. */
    @Volatile var renderMs = RENDER_MS

    @Volatile private var cancelAll = false

    /** The end of a test: every render under way ends as cancelled (the app's export says « annulé »), none runs on. */
    fun cancelAllRenders() {
        cancelAll = true
        renderMs = RENDER_MS
    }

    override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
        val total = minOf(frames + tailFrames, MAX_SECONDS * RATE).coerceAtLeast(1L)
        val v = voiceOf(target, RATE)
        val g = targetGain(target, v, total, frames, 0L)
        return FloatArray(2 * total.toInt()).also { fill(v, it, 0L, total.toInt(), frames, total, 0L, g) }
    }

    /** A 32-bit float stereo WAV of [frames] frames, written a piece at a time ([piece] fills each one). */
    private fun writeWav(file: File, frames: Long, rate: Int, piece: (FloatArray, Long, Int) -> Unit) {
        val bytes = frames * 8
        val head = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt((36 + bytes).toInt()); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(3); putShort(2); putInt(rate); putInt(rate * 8); putShort(8); putShort(32)
            put("data".toByteArray()); putInt(bytes.toInt())
        }
        val samples = FloatArray(2 * CHUNK)
        val buf = ByteBuffer.allocate(8 * CHUNK).order(ByteOrder.LITTLE_ENDIAN)
        java.io.FileOutputStream(file).use { out ->
            out.write(head.array())
            var at = 0L
            while (at < frames) {
                val n = minOf(CHUNK.toLong(), frames - at).toInt()
                piece(samples, at, n)
                buf.clear()
                buf.asFloatBuffer().put(samples, 0, 2 * n)
                out.write(buf.array(), 0, 8 * n)
                at += n
            }
        }
    }

    // --- The Remix's stems ---------------------------------------------------------------------

    /** A song's four stems: their WAVs (read a piece at a time), or one sound in memory ([pcm]). */
    private class Stems(val files: List<WavFile>, val pcm: FloatArray?, val ratio: Double) {
        val frames: Long = if (pcm != null) pcm.size / 2L else Math.round(files.maxOf { it.frames.toDouble() * RATE / it.rate } * ratio)
    }

    private val stems = ConcurrentHashMap<Long, Stems>()
    @Volatile private var transportSince = -1L
    @Volatile private var stemStart = 0L
    @Volatile private var playedStems = 0L
    @Volatile private var previewSince = -1L
    @Volatile private var previewStems = 0L

    /** Bundles held now (tests: each one let go when the app is done with it). */
    val stemsHeld: Int get() = stems.size

    @Synchronized private fun keep(s: Stems): Long = (next++).also { stems[it] = s }

    override fun stemsLoad(paths: Array<String>): Long {
        val files = paths.map { File(it) }
        if (files.any { !it.isFile }) return 0L
        return runCatching { keep(Stems(files.map(::WavFile), null, 1.0)) }.getOrDefault(0L)
    }

    override fun stemsFromPcm(data: FloatArray): Long = keep(Stems(emptyList(), data.copyOf(), 1.0))
    override fun stemsFrames(handle: Long): Long = stems[handle]?.frames ?: 0L
    override fun stemsRelease(handle: Long): Boolean {
        stems.remove(handle)
        return true
    }

    override fun stemsStretched(handle: Long, ratio: Double): Long {
        val s = stems[handle] ?: return 0L
        return keep(Stems(s.files, s.pcm, s.ratio * ratio))
    }

    /** Stereo interleaved at 48 kHz, from frame [start] of the (stretched) song. */
    override fun stemsExtract(handle: Long, stem: Int, start: Long, count: Long): FloatArray {
        val s = stems[handle] ?: return FloatArray(0)
        val n = minOf(count, s.frames - start).coerceAtLeast(0L).toInt()
        if (n == 0) return FloatArray(0)
        s.pcm?.let { pcm -> return if (stem == 0) pcm.copyOfRange(2 * start.toInt(), 2 * (start.toInt() + n)) else FloatArray(2 * n) }
        val wav = s.files.getOrNull(stem) ?: return FloatArray(2 * n)
        val step = wav.rate.toDouble() / RATE / s.ratio  // source frames per output frame
        val first = floor(start * step).toLong()
        val src = wav.read(first, (n * step).toInt() + 3)
        return FloatArray(2 * n) { k ->
            val pos = (start + k / 2) * step - first
            val i = pos.toInt()
            val f = (pos - i).toFloat()
            val a = src.getOrElse(2 * i + k % 2) { 0f }
            val b = src.getOrElse(2 * (i + 1) + k % 2) { a }
            a + (b - a) * f
        }
    }

    /** The peak (0..1) of each of [buckets] slices of one stem. */
    override fun stemsPeaks(handle: Long, stem: Int, buckets: Int): FloatArray {
        val s = stems[handle] ?: return FloatArray(buckets)
        val out = FloatArray(buckets)
        if (s.pcm != null) {
            if (stem == 0) for (k in s.pcm.indices) {
                val b = (k / 2L * buckets / (s.pcm.size / 2)).toInt().coerceAtMost(buckets - 1)
                out[b] = maxOf(out[b], abs(s.pcm[k]))
            }
            return out
        }
        val wav = s.files.getOrNull(stem) ?: return out
        var at = 0L
        while (at < wav.frames) {
            val piece = wav.read(at, 1 shl 15)
            for (k in piece.indices) {
                val b = ((at + k / 2) * buckets / wav.frames).toInt().coerceAtMost(buckets - 1)
                out[b] = maxOf(out[b], abs(piece[k]).coerceAtMost(1f))
            }
            at += 1 shl 15
        }
        return out
    }

    /** The song playing along the project: from its start frame, as long as the transport runs. */
    override fun stemPosition(): Long {
        val since = transportSince
        val s = stems[playedStems] ?: return -1L
        if (since < 0) return -1L
        val pos = stemStart + (SystemClock.uptimeMillis() - since) * RATE / 1000
        return if (pos in 0 until s.frames) pos else -1L  // before the song comes in (R2): not heard yet
    }

    /**
     * The transport's sixteenths since ▶ as the bench's play head counts them (-1 stopped), set by the bench.
     * Audit ultra tour 0 (seat c): a Pad Live take kept with this engine reads where the transport is from it
     * (the app's no-engine path reads [com.pocketstudio.app.rhythm.RhythmViewModel.takeClockForTest]).
     */
    @Volatile var transportSteps = -1

    /** The project's tempo and bar (the bench keeps them as the project's: the app sets them before the engine came). */
    @Volatile var tempo = 120.0
    @Volatile var beatsPerBar = 4
    @Volatile private var countInBars = 0

    /**
     * Audit ultra round 3 (zone 4, n° 23): where the engine plays, in sixteenths since ▶ with the fraction, negative during
     * the count-in (as NativeEngine.playPosition). Stuck at -1 before, the count-in showed « 1… » for good and a keyboard's
     * notes were never recorded (the app records at this position).
     */
    override fun playPosition(): Double {
        val since = transportSince
        if (since < 0) return -1.0
        val steps = stepBase + (SystemClock.uptimeMillis() - since) * tempo / 15_000.0
        return steps - countInBars * beatsPerBar * 4.0
    }

    /** The steps played before the last tempo change (since ▶). */
    @Volatile private var stepBase = 0.0

    override fun transportStep(): Int = transportSteps

    /**
     * Banc fiable 3.2, rewritten after its review: the scene swaps of Engine.cpp (onStep, swapScene), played lazily on the
     * bench's clock. One swap waits at a time (a new arm replaces it, a negative [a] cancels it); it happens on the first bar
     * line after it was armed where the scene's own step reaches [a] (0: the next bar line); b 1 restarts the song at song
     * step [value], b 3 leaves the song, b 2 ends it (the transport stops on that bar line). Each one is counted
     * ([sceneSwaps]): the app follows the song's parts by that count.
     */
    private inner class SongClock {
        var inSong = false
        var songStart = 0
        var sceneStart = 0
        var swaps = 0
        private var swapAt = -1
        private var kind = 0
        private var restartAt = 0
        private var armedAt = -1

        @Synchronized fun start(inSong: Boolean, songStep: Int) {
            this.inSong = inSong
            songStart = -songStep
            sceneStart = 0
            swapAt = -1
        }

        @Synchronized fun arm(a: Int, b: Int, value: Float, now: Int) {
            advance()
            swapAt = if (a < 0) -1 else a
            kind = b
            restartAt = if (value.isFinite()) value.toInt() else 0
            armedAt = now
        }

        /** The waiting swap, if its bar line has come: played at that step. */
        @Synchronized fun advance() {
            if (swapAt < 0) return
            val now = stepsNow()
            if (now < 0) return
            val bar = (beatsPerBar * 4).coerceAtLeast(1)
            val minLocal = maxOf(1, armedAt + 1 - sceneStart, swapAt)
            val due = sceneStart + (minLocal + bar - 1) / bar * bar
            if (now < due) return
            swapAt = -1
            swaps++
            when (kind) {
                1 -> { songStart = due - restartAt; inSong = true }
                3 -> inSong = false
                // The song's end (T5-Ab-27): the transport stops on that bar line, and a take ends there with what it recorded up
                // to it, as the engine's does (the app then keeps it: RhythmViewModel.songEnded).
                2 -> { freezeTakes(Math.round(due * 15.0 * RATE / tempo)); transportSince = -1L; inSong = false; return }
            }
            sceneStart = due
        }
    }

    private val song = SongClock()

    /** Whole sixteenths since ▶ from the bench's clock (count-in excluded), -1 stopped or counting in. */
    private fun stepsNow(): Int = playPosition().let { if (it < 0) -1 else it.toInt() }

    /** Banc fiable 3.2: the steps since ▶ (the position of a part that loops); -1 stopped. Was always -1: counter frozen. */
    override fun absoluteStep(): Int {
        song.advance()
        return stepsNow()
    }

    /** Banc fiable 3.2: steps since the song (re)started, as the top bar's « mesure N/… » reads them; -1 outside the song. */
    override fun songStep(): Int {
        song.advance()
        val now = stepsNow()
        return if (now < 0 || !song.inSong) -1 else now - song.songStart
    }

    /** The scene swaps played (Engine.cpp's sceneSwaps_): the app moves to the next part on each. */
    override fun sceneSwaps(): Int {
        song.advance()
        return song.swaps
    }

    /** Where the song was marked (a transport step), once the transport has reached that launch line; -1 before or with none. */
    override fun songMarkStep(): Int {
        val asked = markAsked
        if (asked < 0) return -1
        val q = markQuantum
        val line = if (q <= 0 || asked == 0) asked else ((asked + q - 1) / q) * q
        return if (transportSteps >= line) line else -1
    }

    /** The transport step from which the next launch line counts (0: marked before ▶), -1 when nothing is asked. */
    @Volatile private var markAsked = -1
    @Volatile private var markQuantum = 0
    override fun transportPosition(): Double = if (transportSteps >= 0) transportSteps.toDouble() else -1.0

    /** « Écouter » on the Remix page: from the top, until its end. */
    override fun previewPosition(): Long {
        val since = previewSince
        val s = stems[previewStems] ?: return -1L
        if (since < 0) return -1L
        val pos = (SystemClock.uptimeMillis() - since) * RATE / 1000
        if (pos >= s.frames) {
            previewSince = -1L
            return -1L
        }
        return pos
    }

    /** A float or 16-bit stereo WAV, read a piece at a time (stems are a few dozen MB). */
    private class WavFile(val file: File) {
        val rate: Int
        val channels: Int
        val bits: Int
        val dataAt: Long
        val frames: Long

        init {
            RandomAccessFile(file, "r").use { f ->
                var pos = 12L
                var r = 0
                var c = 0
                var b = 0
                var data = -1L
                var size = 0L
                val head = ByteArray(8)
                while (pos + 8 <= f.length()) {
                    f.seek(pos)
                    f.readFully(head)
                    val id = String(head, 0, 4)
                    val n = ByteBuffer.wrap(head, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                    if (id == "fmt ") {
                        val fmt = ByteArray(16).also { f.readFully(it) }
                        val bb = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                        c = bb.getShort(2).toInt()
                        r = bb.getInt(4)
                        b = bb.getShort(14).toInt()
                    } else if (id == "data") {
                        data = pos + 8
                        size = n
                        break
                    }
                    pos += 8 + n + (n and 1)
                }
                require(data > 0 && r > 0 && c in 1..2 && (b == 32 || b == 16)) { "not a WAV: $file" }
                rate = r
                channels = c
                bits = b
                dataAt = data
                frames = size / (c * b / 8)
            }
        }

        /** [count] frames from [from], stereo interleaved (zeros past the end). */
        fun read(from: Long, count: Int): FloatArray {
            val out = FloatArray(2 * count)
            val have = (frames - from).coerceIn(0L, count.toLong()).toInt()
            if (have == 0 || from < 0) return out
            val width = bits / 8
            val bytes = ByteArray(have * channels * width)
            RandomAccessFile(file, "r").use { f ->
                f.seek(dataAt + from * channels * width)
                f.readFully(bytes)
            }
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until have) for (ch in 0..1) {
                val k = i * channels + if (channels == 2) ch else 0
                out[2 * i + ch] = if (width == 4) bb.getFloat(4 * k) else bb.getShort(2 * k) / 32768f
            }
            return out
        }
    }

    companion object {
        /** The longest render the bench makes (10 minutes): never reached by a song a tester makes. */
        const val MAX_SECONDS = 600L
        private const val CHUNK = 8192
        /** [install]'s kinds, counted in a project's print after the commands. */
        private const val INSTALL = 1000
        const val RENDER_MS = 1_200L
        private const val STEPS = 24
        private const val RATE = OfflineExport.SAMPLE_RATE
        // NativeEngine's own command numbers (its private CMD_… constants).
        private const val CMD_SET_TRANSPORT = 5
        private const val CMD_BEATS_PER_BAR = 57
        private const val CMD_STEM_START = 32
        private const val CMD_PLAY_PREVIEW = 47
        private const val CMD_MARK_SONG = 75
        private const val CMD_ARM_SCENE_SWAP = 40
        private const val CMD_TRIGGER_PAD = 11
        private const val CMD_SYNTH_NOTE_ON = 16
        private const val CMD_SET_TEMPO = 6
        private const val CMD_SET_DRUM_HIT = 10
        private const val CMD_SET_ACID_STEP = 12
        private const val CMD_CHANNEL_VOLUME = 20
        private const val CMD_SYNTH_NOTE = 26

        private const val CMD_MASTER = 67
        private const val MASTER_TARGET = 4
    }
}
