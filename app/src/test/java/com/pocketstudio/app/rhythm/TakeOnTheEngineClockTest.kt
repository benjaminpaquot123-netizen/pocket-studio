package com.pocketstudio.app.rhythm

import android.os.Looper
import android.os.SystemClock
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.Metrics
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.StreamInfo
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.partSteps
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Emulator journey (PR #48), with the engine's clock: on the assistant's jump up song (174 BPM,
 * 116 ms of output latency), the song listened to from « Ton morceau est prêt », then Sons ›
 * « Lead » › ● and keys of the scale a beat into the take. The engine is a sink whose position
 * moves on with the clock as a real stream's does (negative during the count-in).
 */
@RunWith(RobolectricTestRunner::class)
class TakeOnTheEngineClockTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /**
     * The live engine's transport on Robolectric's clock (SystemClock moves on with the looper):
     * its position in sixteenths of the scene playing (negative during the count-in), and the
     * scene swaps the app arms (song parts, « ⟳ Partie »), on the bar line as the engine makes them.
     */
    private class ClockSink : CommandSink {
        var bpm = 120.0
        private var startMs = -1L
        private var countInSteps = 0
        private var songFrom = -1
        private var sceneStart = 0
        private var swapAt = -1  // armed: the scene's step it may swap from (0: the next bar)
        private var armedAt = 0
        private var endsSong = false
        private var swaps = 0
        val plays = ArrayList<String>()

        private fun transport(): Double = if (startMs < 0) Double.NaN else (SystemClock.uptimeMillis() - startMs) / 1000.0 * bpm / 15.0 - countInSteps

        /** Swaps due by now, as Engine::onStep makes them (on a bar line of the scene playing). */
        private fun follow() {
            val t = transport()
            if (t.isNaN() || t < 0 || swapAt < 0) return
            val local = armedAt - sceneStart
            val at = maxOf(if (swapAt == 0) 1 else swapAt, (local / 16) * 16 + 1).let { (it + 15) / 16 * 16 }
            if (sceneStart + at > t.toInt()) return
            sceneStart += at
            swapAt = -1
            swaps++
            if (endsSong) {
                startMs = -1L
                plays += "song end"
            }
        }

        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L) return
            follow()
            when (type) {
                5 -> if (value > 0f) {  // CMD_SET_TRANSPORT: play (a: count-in bars, b: song step) or stop
                    startMs = SystemClock.uptimeMillis()
                    countInSteps = a * 16
                    songFrom = b
                    sceneStart = 0
                    swapAt = -1
                    plays += "play countIn=$a song=$b"
                } else {
                    startMs = -1L
                    swapAt = -1
                    plays += "stop"
                }
                6 -> bpm = value.toDouble()  // CMD_SET_TEMPO
                40 -> {  // CMD_ARM_SCENE_SWAP: after a steps (-1: cancel), b 2: the song ends there
                    swapAt = if (a < 0) -1 else a
                    endsSong = b == 2
                    armedAt = maxOf(0, transport().let { if (it.isNaN()) 0 else it.toInt() })
                }
            }
        }
        @Synchronized private fun local(): Double = transport().let { if (it.isNaN()) it else { follow(); if (it < 0) it else it - sceneStart } }
        override fun playPosition(): Double = local().let { if (it.isNaN()) -1.0 else it }
        override fun transportPosition(): Double = transport().let { if (it.isNaN() || it < 0) -1.0 else it }
        override fun transportStep(): Int = transportPosition().let { if (it < 0) -1 else it.toInt() }
        override fun absoluteStep(): Int = local().let { if (it.isNaN() || it < 0) -1 else it.toInt() }
        override fun currentStep(): Int = absoluteStep().let { if (it < 0) -1 else it % 16 }
        override fun songStep(): Int = transportStep().let { if (it < 0 || songFrom < 0) -1 else it + songFrom }
        @Synchronized override fun sceneSwaps(): Int {
            follow()
            return swaps
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = 1L
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        override fun exportCreate(): Long = 2L
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    private val sink = ClockSink()
    private lateinit var vm: RhythmViewModel

    @After
    fun end() {
        NativeEngine.sink = null
        Metrics.updateStream(StreamInfo())
    }

    /** [ms] of the engine's clock (and of the app's coroutines) go by. */
    private fun pass(ms: Long) {
        var left = ms
        while (left > 0) {
            val d = minOf(20L, left)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(d))
            left -= d
        }
    }

    private fun jumpUp() = StarterWizard.style("jump_up")!!.let { style ->
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts)
    }

    /** The jump up song open, the engine a clock with 116 ms of output latency. */
    private fun start() {
        File(app.filesDir, "projects").deleteRecursively()
        NativeEngine.sink = sink
        Metrics.updateStream(StreamInfo(outputLatencyMs = 116.0))
        vm = RhythmViewModel(app)
        pass(500)
        val song = jumpUp()
        vm.session.edit { song }
        sink.bpm = vm.project.value.tempoBpm
        pass(200)
    }

    private val keys = listOf(60, 65, 72, 77)  // C and F, in F phrygian, on two octaves (the emulator's)

    /** Sons › « Lead », ● and its count-in, a beat, four keys of the scale (220 ms down, 180 ms up), ■. */
    private fun takeOnLead(): Int? {
        val p = vm.project.value
        val lead = p.tracks.first { it.name == "Lead" }
        vm.selectTrack(lead.id)
        assertTrue(keys.all { p.harmony.inScale(it) })
        val wasPlaying = vm.transport.value.playing
        vm.recordTake(TakeSource.KEYS)
        var waited = 0L
        while (vm.noteTake.value?.phase != TakePhase.RECORDING) {
            pass(40)
            waited += 40
            assertTrue("the count-in ends (${sink.plays}, ${vm.noteTake.value})", waited < 5_000)
        }
        if (!wasPlaying) pass((60_000.0 / p.tempoBpm).toLong())
        val at = ArrayList<String>()
        for (key in keys) {
            at += "%.2f".format(NativeEngine.playPosition())
            vm.synthNoteOn(key)
            pass(220)
            vm.synthNoteOff(key)
            pass(180)
        }
        vm.recordTake(TakeSource.KEYS)
        println("take at $at (${sink.plays}): ${vm.noteTake.value}, empty ${vm.takeEmpty.value}, ${vm.lastTakeReport}")
        val notes = vm.noteTake.value?.notes
        assertEquals("the take's journal line", "Prise keys : 4 touches, $notes notes, 0 coups", vm.lastTakeReport?.substringBefore(" (non"))
        return notes
    }

    /** Stopped, nothing played before: the four keys are kept. */
    @Test
    fun stoppedTheKeysAreKept() {
        start()
        assertEquals(4, takeOnLead())
    }

    /** The journey: the song listened to (« Écouter » of the song-ready card), ■, then the take. */
    @Test
    fun afterTheSongWasListenedToTheKeysAreKept() {
        start()
        vm.toggleSong()
        pass(3_000)
        vm.togglePlay()
        assertTrue("stopped (${sink.plays})", !vm.transport.value.playing)
        assertEquals(4, takeOnLead())
    }

    /** The whole journey: the song listened to, ■, « Fill à la fin » and « Montée avant » on parts, then the take. */
    @Test
    fun afterTheSongAndThePartIntentsTheKeysAreKept() {
        start()
        vm.toggleSong()
        pass(60_000)
        println("after a minute: scene ${vm.project.value.scene}, ui ${vm.sessionUi.value}, swaps ${NativeEngine.sceneSwaps()}, ${vm.project.value.song.size} parts")
        vm.togglePlay()
        val p0 = vm.project.value
        val fillPart = (1..minOf(4, p0.song.size - 1)).first { com.pocketstudio.app.session.partSheetUi(p0, it)?.endsOnFill == false }
        vm.partIntent(fillPart, com.pocketstudio.app.session.PartIntent.Fill)
        pass(200)
        val p1 = vm.project.value
        val buildUpPart = (1..minOf(4, p1.song.size - 1)).first { it != fillPart }
        vm.partIntent(buildUpPart, com.pocketstudio.app.session.PartIntent.BuildUp(4))
        pass(200)
        assertTrue("${vm.done.value}", vm.done.value.orEmpty().startsWith("Fait"))
        println("scene ${vm.project.value.scene}, ui ${vm.sessionUi.value}, playing ${vm.transport.value.playing}")
        assertEquals(4, takeOnLead())
    }

    /** ● while the song plays (from the song-ready card): no count-in, the keys go into the part heard. */
    @Test
    fun whileTheSongPlaysTheKeysAreKept() {
        start()
        vm.toggleSong()
        pass(1_000)
        assertEquals(4, takeOnLead())
        assertTrue(vm.transport.value.playing)
    }

    /** ● while the song plays, the keys played across a part change. */
    @Test
    fun acrossAPartChangeTheKeysAreKept() {
        start()
        vm.toggleSong()
        val first = vm.project.value.let { it.partSteps(it.song[0]) }
        // Half a second before the second part (the keys take 1.6 s).
        pass(((first / (vm.project.value.tempoBpm / 15.0)) * 1000).toLong() - 500)
        assertEquals(4, takeOnLead())
        println("swaps ${NativeEngine.sceneSwaps()}, ui ${vm.sessionUi.value}")
        assertTrue("a part change during the take", NativeEngine.sceneSwaps() >= 1)
    }

    /** « ⟳ Partie »: the part shown loops; ● while it plays. */
    @Test
    fun whileThePartLoopsTheKeysAreKept() {
        start()
        vm.setLoopPart(true)
        vm.togglePlay()
        pass(1_000)
        assertTrue(vm.transport.value.playing)
        assertEquals(4, takeOnLead())
    }

    /** « ⟳ Partie », stopped: ● counts in and the part loops under the keys. */
    @Test
    fun partLoopStoppedTheKeysAreKept() {
        start()
        vm.setLoopPart(true)
        assertEquals(4, takeOnLead())
    }

    /**
     * Keys played during the count-in only: nothing kept (they are before the « 1 »), and the
     * take's journal line says they were played, and where, rather than « nothing played ».
     */
    @Test
    fun keysDuringTheCountInAreSaidNotKept() {
        start()
        vm.selectTrack(vm.project.value.tracks.first { it.name == "Lead" }.id)
        vm.recordTake(TakeSource.KEYS)
        pass(300)
        assertEquals(TakePhase.COUNT_IN, vm.noteTake.value?.phase)
        vm.synthNoteOn(60)
        pass(100)
        vm.synthNoteOff(60)
        vm.recordTake(TakeSource.KEYS)
        assertTrue(vm.takeEmpty.value)
        val report = vm.lastTakeReport.orEmpty()
        assertTrue(report, report.startsWith("Prise keys : 1 touches, 0 notes") && "key 60: position -" in report)
    }
}
