package com.pocketstudio.app.record

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.NativeEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Audit ultra round 1, zone 4 (« Enregistrement »), tested without the bench's seats:
 * - n° 29 (seats A d, C c, E b): a microphone that cannot open (busy, its stream closed, no engine)
 *   is said, with what to do, and the next « Enregistrer » is tried again (the bench's engine had no
 *   microphone at all: its native call threw, the take stayed « waiting » and every touch after was ignored);
 * - n° 31 (seat C c): « Rien n'a été joué pendant la prise » goes after a moment;
 * - n° 31 (seat F a): the Clavier's « Garder ce que je viens de jouer » keeps the keys, not a pad hit before.
 */
@RunWith(RobolectricTestRunner::class)
class RecordZone4Test {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The bench's engine, its microphone as asked: [mic] gives the take's handle (0: it did not open), or throws. */
    private class Mic(private val inner: BenchEngine = BenchEngine(), var mic: () -> Long = { 0L }) : CommandSink by inner {
        var asked = 0
        override fun micStart(onStart: Boolean, seconds: Int, prerollFrames: Int): Long { asked++; return mic() }
    }

    @After
    fun end() {
        NativeEngine.sink = null
    }

    private fun vm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app)
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    @Test
    fun aMicrophoneThatCannotOpenIsSaidAndTriedAgain() {
        val sink = Mic()
        NativeEngine.sink = sink
        val vm = vm()
        val id = vm.micTakeTrack()!!
        // Busy, or its stream closed: no take.
        vm.recordAudio(id)
        until("never said") { vm.notice.value != null }
        assertTrue(vm.notice.value!!, "micro" in vm.notice.value!! && "autorisation" in vm.notice.value!!)
        assertNull(vm.audioRecording.value)
        vm.dismissNotice()
        // It throws (the bench had no microphone: its native call): said too, and never stuck.
        sink.mic = { throw UnsatisfiedLinkError("no microphone") }
        vm.recordAudio(id)
        until("a throw never said") { vm.notice.value != null }
        assertNull("the take is not left waiting", vm.audioRecording.value)
        assertEquals(2, sink.asked)
        assertFalse(vm.transport.value.playing)
    }

    @Test
    fun withoutTheEngineEnregistrerSaysWhy() {
        NativeEngine.sink = null
        val vm = vm()
        vm.recordAudio(0)
        assertTrue(vm.notice.value!!, "moteur audio" in vm.notice.value!!)
        assertNull(vm.audioRecording.value)
    }

    @Test
    fun nothingPlayedIsSaidAMomentThenGoes() {
        val vm = vm()
        vm.playingForTest = true
        vm.recordTake(TakeSource.KEYS)
        vm.recordTake(TakeSource.KEYS)
        assertTrue(vm.takeEmpty.value)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(RhythmViewModel.TAKE_EMPTY_MS + 500))
        assertFalse("« Rien n'a été joué » never went", vm.takeEmpty.value)
    }

    @Test
    fun theKeysKeptWithoutAPadHitTouchedBefore() {
        val vm = vm()
        vm.playingForTest = false
        val lane = (0 until 16).first { l -> vm.project.value.drums.lanes[l].hits.isEmpty() }
        vm.playedClockForTest = 10.0
        vm.hitPad(lane)
        vm.playedClockForTest = 20.0
        vm.synthNoteOn(60, 0.8f)
        vm.playedClockForTest = 21.0
        vm.synthNoteOff(60)
        vm.playedClockForTest = 22.0
        vm.synthNoteOn(64, 0.8f)
        vm.playedClockForTest = 23.0
        vm.synthNoteOff(64)
        // The Clavier's record bar.
        recordBarActions(vm, TakeSource.KEYS).keepPlayed()
        val card = vm.noteTake.value!!
        assertEquals(2, card.notes)
        assertEquals("no pad hit in a keys' take", 0, card.hits)
        assertTrue(vm.project.value.drums.lanes[lane].hits.isEmpty())
    }

    @Test
    fun theTopRecordSaysWhatItRecords() {
        assertEquals("Enregistrer ce que je joue au clavier", recordLabelFor(TakeSource.KEYS, mic = false, recording = false))
        assertEquals("Enregistrer ce que je joue aux pads", recordLabelFor(TakeSource.PADS, mic = false, recording = false))
        assertEquals("Enregistrer le micro sur la piste audio", recordLabelFor(null, mic = true, recording = false))
        assertEquals("Enregistrer : micro, clavier ou pads", recordLabelFor(null, mic = false, recording = false))
        assertEquals(listOf(1, 2, 0), listOf(0, 1, 2).map(::nextCountIn))
        assertEquals("Décompte : 2 mesures", countInLabel(2))
    }
}
