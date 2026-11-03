package com.pocketstudio.app.padlive

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.midi.MidiClockOut
import com.pocketstudio.app.midi.MidiCommand
import com.pocketstudio.app.midi.MidiViewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import com.pocketstudio.model.PadMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Pad Live (n° 25, lot B2) from a MIDI controller (an Arturia MiniLab 3 here): its 16 pads play
 * the page shown, with each pad's mode (« tenir » stops when the pad is let go) and strength; a
 * row launched goes to the engine as one command; the MIDI clock goes out with the engine's own
 * times, and comes in (Start, tempo, Stop). The engine's commands are read from a stand-in.
 */
@RunWith(RobolectricTestRunner::class)
class PadLiveMidiTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The engine's stand-in: the commands posted to the live engine (type, value, a, b). */
    private class Sink : CommandSink {
        private var next = 1L
        val posts = ArrayList<List<Number>>()
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target == 0L) posts += listOf(type, value, a, b)
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit

        /** The pads switched (ToggleClip) with their strength. */
        @Synchronized fun toggles(): List<Pair<Int, Float>> = posts.filter { it[0] == TOGGLE_CLIP }.map { it[2].toInt() to it[1].toFloat() }
        @Synchronized fun of(type: Int): List<List<Number>> = posts.filter { it[0] == type }

        companion object {
            const val TOGGLE_CLIP = 46
            const val LAUNCH_CLIP_ROW = 150
            const val MIDI_CLOCK_OUT = 153
        }
    }

    private val sink = Sink()

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
        NativeEngine.sink = sink
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    /** A « Live » project: page B holds a loop, a « tenir » loop and a hit; page A a loop. */
    private fun liveVm(): RhythmViewModel = RhythmViewModel(app).also { vm ->
        vm.livePadsSoundingForTest = emptySet()
        vm.session.edit { p ->
            p.copy(
                tempoBpm = 124.0,
                live = p.live
                    .withAt(0, LiveClip("a.wav", "Kick", 1))
                    .withAt(PadLive.pad(1, 0), LiveClip("b.wav", "Basse", 4))
                    .withAt(PadLive.pad(1, 4), PadLive.withMode(LiveClip("c.wav", "Sirène", 4), PadMode.HOLD))
                    .withAt(PadLive.pad(1, 5), LiveClip("d.wav", "Clap", 1, oneShot = true)),
                padLive = PadLiveSet(home = true, page = 1),
            )
        }
    }

    /** The MiniLab's pad [pad] (0..15: channel 10, notes 36 on) hit at [velocity], or let go. */
    private fun MidiViewModel.pad(pad: Int, velocity: Int) =
        receiveForTest(byteArrayOf(0x99.toByte(), (36 + pad).toByte(), velocity.toByte()))

    @Test
    fun miniLabPadsPlayThePageShownWithTheirModes() {
        val vm = liveVm()
        val midi = MidiViewModel(app)
        midi.padsToDrums = false  // the Live screens take the pads
        val scope = CoroutineScope(Dispatchers.Unconfined)
        // As the app routes them: Pad Live first, while it shows.
        val elsewhere = ArrayList<MidiCommand>()
        scope.launch { midi.commands.collect { c -> if (!vm.padLive.midi(c)) elsewhere += c } }
        // Not shown: the pads go where they went before (the Live page's pads).
        midi.pad(0, 100)
        assertTrue(sink.toggles().isEmpty())
        assertTrue(elsewhere.last() is MidiCommand.Pad)
        vm.padLive.shown = true
        // Pad 1 of the MiniLab: pad B1 (the page shown), a loop at its strength; let go: nothing.
        midi.pad(0, 64)
        midi.pad(0, 0)
        assertEquals(listOf(PadLive.pad(1, 0) to 64 / 127f), sink.toggles())
        // Pad 5: B5, « tenir »: on with the pad, off when let go.
        midi.pad(4, 127)
        vm.livePadsSoundingForTest = setOf(PadLive.pad(1, 4))
        midi.pad(4, 0)
        assertEquals(listOf(PadLive.pad(1, 4), PadLive.pad(1, 4)), sink.toggles().drop(1).map { it.first })
        // Pad 6: a hit, each strike.
        vm.livePadsSoundingForTest = emptySet()
        midi.pad(5, 90)
        midi.pad(5, 90)
        assertEquals(2, sink.toggles().count { it.first == PadLive.pad(1, 5) })
        // An empty pad: nothing. Page A shown: pad 1 is A1.
        midi.pad(9, 90)
        vm.padLive.setPage(0)
        midi.pad(0, 90)
        assertEquals(0, sink.toggles().last().first)
        scope.cancel()
    }

    @Test
    fun aRowGoesToTheEngineAsOneCommandOnTheBar() {
        val vm = liveVm()
        // Row 2 of page B (B5 « tenir », B6 a hit): both start on the next bar line, together.
        vm.padLive.launchRow(1, 1)
        val row = sink.of(Sink.LAUNCH_CLIP_ROW)
        assertEquals(1, row.size)
        assertEquals(PadLive.pad(1, 4), row[0][2].toInt())
        assertEquals(PadLive.ROW_SIZE, row[0][3].toInt())
        assertTrue("no pad switched one by one", sink.toggles().isEmpty())
        // An empty row: nothing.
        vm.padLive.launchRow(1, 3)
        assertEquals(1, sink.of(Sink.LAUNCH_CLIP_ROW).size)
    }

    /** FIX-1: switched off and on again quickly, the engine's clock queue (single reader) is never read by two threads. */
    @Test
    fun theClockQueueHasOneReaderWhenSwitchedQuickly() {
        val inside = java.util.concurrent.atomic.AtomicInteger(0)
        val overlaps = java.util.concurrent.atomic.AtomicInteger(0)
        val reads = java.util.concurrent.atomic.AtomicInteger(0)
        val midi = MidiViewModel(app)
        midi.clockSentForTest = { _, _ -> }
        midi.clockDrainForTest = { _ ->
            if (inside.incrementAndGet() > 1) overlaps.incrementAndGet()
            reads.incrementAndGet()
            Thread.sleep(3)
            inside.decrementAndGet()
            0
        }
        for (i in 0 until 12) {
            midi.setSendClock(true)
            Thread.sleep(6)
            midi.setSendClock(false)
            Thread.sleep(4)
            midi.setSendClock(true)
            Thread.sleep(20)
            midi.setSendClock(false)
        }
        Thread.sleep(200)
        assertTrue("the queue was read (${reads.get()})", reads.get() > 12)
        assertEquals("two readers at once", 0, overlaps.get())
    }

    @Test
    fun theClockGoesOutWithTheEnginesTimesAndComesIn() {
        // Out: the engine's messages (status, when heard), sent with the output's latency added.
        val due = ArrayList<Long>()
        for (i in 0 until 300) due += listOf(if (i == 0) 0xFAL else 0xF8L, 1_000_000_000L + i * 20_000_000L)
        var read = 0
        val sent = ArrayList<Pair<Int, Long>>()
        val out = MidiClockOut(
            send = { status, at -> sent += status to at },
            drain = { buf ->
                val n = minOf(buf.size / 2, (due.size - read) / 2)
                for (i in 0 until n * 2) buf[i] = due[read + i]
                read += n * 2
                n
            },
            latencyNanos = { 12_000_000L },
        )
        assertEquals(300, out.pump())
        assertEquals(0xFA to 1_012_000_000L, sent[0])
        assertEquals(0xF8 to 1_012_000_000L + 299 * 20_000_000L, sent.last())
        assertTrue(sent.all { it.first == 0xF8 || it.first == 0xFA })
        assertEquals(0, out.pump())
        // « Envoyer l'horloge »: the engine is asked for it, and let go of it.
        val midi = MidiViewModel(app)
        midi.clockSentForTest = { _, _ -> }
        midi.setSendClock(true)
        assertEquals(1f, sink.of(Sink.MIDI_CLOCK_OUT).last()[1].toFloat())
        assertFalse("following and sending at once would loop", midi.state.value.followClock)
        midi.setSendClock(false)
        assertEquals(0f, sink.of(Sink.MIDI_CLOCK_OUT).last()[1].toFloat())

        // In: another machine's Start, its clock at 125 BPM (a tick every 20 ms), Stop.
        val vm = liveVm()
        midi.setFollowClock(true)
        val got = ArrayList<MidiCommand>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch { midi.commands.collect { got += it } }
        midi.receiveForTest(byteArrayOf(0xFA.toByte()))
        assertEquals(MidiCommand.Start, got.last())
        val start = System.nanoTime()
        for (i in 1..30) {
            while (System.nanoTime() - start < i * 20_000_000L) Thread.onSpinWait()
            midi.receiveForTest(byteArrayOf(0xF8.toByte()))
        }
        val tempo = got.filterIsInstance<MidiCommand.Tempo>().last().bpm
        assertTrue("tempo $tempo", tempo in 118.0..132.0)
        // As the app routes it: Pad Live's project follows (its loops with it).
        vm.followClockTempo(tempo)
        assertEquals(tempo, vm.project.value.tempoBpm, 1e-9)
        midi.receiveForTest(byteArrayOf(0xFC.toByte()))
        assertEquals(MidiCommand.Stop, got.last())
        scope.cancel()
    }
}
