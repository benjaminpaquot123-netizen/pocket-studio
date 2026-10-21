package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.LiveSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The Live page's grid through the ViewModel (no audio engine here: the launches the engine would
 * report are fed in by hand, as its event queue does).
 */
@RunWith(RobolectricTestRunner::class)
class LiveVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Two scenes: A (shown) with a kick, B with another drum hit. */
    private fun twoScenes(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()  // B, shown
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        assertEquals(0, vm.project.value.scene)
        return vm
    }

    @Test
    fun aPadWaitsForItsBarThenOnlyThatTrackPlaysAnotherScene() {
        val vm = twoScenes()
        vm.togglePlay()
        assertTrue(vm.transport.value.playing)
        assertEquals(0, vm.liveGrid.value!!.row)
        vm.launchPad(SongPart.DRUMS, 1)
        val armed = vm.liveGrid.value!!
        assertEquals(1, armed.slot(LiveSlots.DRUMS).pending)
        assertEquals(0, armed.slot(LiveSlots.DRUMS).playing)
        assertFalse(vm.waitsForTest(LiveSlots.DRUMS))  // until the bar, the drums still play the scene edited
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 16)
        val g = vm.liveGrid.value!!
        assertEquals(1, g.slot(LiveSlots.DRUMS).playing)
        assertEquals(16, g.slot(LiveSlots.DRUMS).start)
        assertTrue(g.diverged)
        // Editing scene A: the drums (playing B) wait, the acid (playing A) hears it at once.
        assertTrue(vm.waitsForTest(LiveSlots.DRUMS))
        assertFalse(vm.waitsForTest(LiveSlots.ACID))
        assertEquals(0, vm.project.value.scene)  // the screens stay on the scene shown
        // A whole row: once every track plays B, B is the scene shown and nothing waits.
        vm.launchRow(1)
        assertEquals(1, vm.liveGrid.value!!.pendingRow)
        for (k in 0 until LiveSlots.COUNT) vm.liveLaunchedForTest(k, 1, 32)
        assertEquals(1, vm.project.value.scene)
        assertFalse(vm.liveGrid.value!!.diverged)
        assertFalse(vm.waitsForTest(LiveSlots.DRUMS))
        // A track stopped: its edits wait (it must stay silent).
        vm.stopLiveTrack(SongPart.ACID)
        assertEquals(-1, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
        vm.liveLaunchedForTest(LiveSlots.ACID, -1, 48)
        assertTrue(vm.waitsForTest(LiveSlots.ACID))
        // Stop: the grid is gone, edits reach the engine again.
        vm.togglePlay()
        assertNull(vm.liveGrid.value)
        assertFalse(vm.waitsForTest(LiveSlots.ACID))
    }

    @Test
    fun aPadTouchedWhileStoppedStartsItsTrackAlone() {
        val vm = twoScenes()
        vm.launchPad(SongPart.ACID, 1)
        assertTrue(vm.transport.value.playing)
        assertEquals(1, vm.project.value.scene)  // the pad's scene is shown
        val g = vm.liveGrid.value!!
        assertNull(g.slot(LiveSlots.ACID).pending)
        assertEquals(-1, g.slot(LiveSlots.DRUMS).pending)  // the others stop on the first step
        for (k in 0 until LiveSlots.COUNT) if (k != LiveSlots.ACID) vm.liveLaunchedForTest(k, -1, 0)
        assertEquals(1, vm.liveGrid.value!!.slot(LiveSlots.ACID).playing)
        assertTrue(vm.liveGrid.value!!.diverged)
        assertFalse(vm.liveGrid.value!!.hasPending)
        // « ■ Tout » then a row while stopped: the row starts the scene whole.
        vm.stopLiveAll()
        assertEquals(-1, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
        vm.togglePlay()
        vm.launchRow(0)
        assertEquals(0, vm.project.value.scene)
        assertFalse(vm.liveGrid.value!!.hasPending)
        assertFalse(vm.liveGrid.value!!.diverged)
    }

    @Test
    fun muteQuantumAndGesturesAreNotWrittenAndEndWithStop() {
        val vm = twoScenes()
        val depth = vm.session.undoDepth
        vm.setLiveQuantum(2)
        assertEquals(2, vm.project.value.liveQuantum)
        vm.setLiveQuantum(3)  // not a choice
        assertEquals(2, vm.project.value.liveQuantum)
        vm.togglePlay()
        vm.toggleLiveMute(SongPart.DRUMS)
        assertEquals(setOf(SongPart.DRUMS), vm.liveMuted.value)
        assertFalse(vm.project.value.mixer.channels[0].mute)
        val cutoff = vm.project.value.acid.sound.cutoff
        vm.setLiveAcid(0, 0.9f)
        vm.setLiveAcid(5, 0.9f)  // tuning: not on the Live page
        assertEquals(mapOf(0 to 0.9f), vm.liveAcid.value)
        assertEquals(cutoff, vm.project.value.acid.sound.cutoff)
        vm.resetLiveAcid(0)  // double tap: back to the saved setting
        assertTrue(vm.liveAcid.value.isEmpty())
        vm.setLiveAcid(1, 0.2f)
        vm.setLiveReturn(0, 0.8f)
        assertEquals(listOf(0.8f, 0.5f), vm.liveReturns.value)
        assertEquals(depth, vm.session.undoDepth)  // nothing of it is an undo step
        vm.togglePlay()
        assertTrue(vm.liveMuted.value.isEmpty())
        assertTrue(vm.liveAcid.value.isEmpty())
        assertEquals(listOf(0.5f, 0.5f), vm.liveReturns.value)
    }

    @Test
    fun aTakeIsWrittenIntoTheSongInOneUndoStep() {
        val vm = twoScenes()
        val depth = vm.session.undoDepth
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()  // armed for the next bar
        assertEquals(TakeUi.Armed(16, 11), vm.take.value)
        assertTrue(vm.performanceTaking)
        vm.takeClockForTest = 16
        vm.tickPerformance()
        assertEquals(TakeUi.Recording(16, 0, 1), vm.take.value)
        // The drums go to B on bar 3; the 303's cutoff moves on beat 5 of the take.
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 36
        vm.setLiveAcid(0, 0.9f)
        vm.livePerformance(AutoTarget.LIVE_CHOP, 1f)
        vm.takeClockForTest = 44
        vm.livePerformance(AutoTarget.LIVE_CHOP, 0f)
        vm.tickPerformance()
        assertEquals(TakeUi.Recording(16, 3, 2), vm.take.value)  // 28 steps at 120 BPM, bar 2 of the song
        // Annuler waits for the take's end.
        val during = vm.project.value
        vm.undo()
        assertEquals(during, vm.project.value)
        vm.togglePerformance()  // ■ Fin: the take ends on the next bar line (step 48)
        assertEquals(TakeUi.Done(2, 2, 0), vm.take.value)
        val song = vm.project.value.song
        assertEquals(listOf(SongPart(0, 1), SongPart(0, 1, play = mapOf(SongPart.DRUMS to 1), bars = 1)), song)
        val lanes = vm.project.value.automation
        assertEquals(setOf(AutoTarget.acid(0), AutoTarget.live(AutoTarget.LIVE_CHOP)), lanes.map { it.target }.toSet())
        assertTrue(lanes.all { it.mode == AutomationLane.SONG && it.songPerBeat })
        assertTrue(lanes.first { it.target.kind == AutoTarget.LIVE }.hold)
        assertEquals(0.9f, lanes.first { it.target.kind == AutoTarget.ACID }.songValues[5])
        assertFalse(vm.performanceTaking)
        // One undo step for the whole take.
        assertEquals(depth + 1, vm.session.undoDepth)
        vm.undoPerformance()
        assertTrue(vm.project.value.song.isEmpty())
        assertTrue(vm.project.value.automation.isEmpty())
        assertEquals(TakeUi.Ready, vm.take.value)
    }

    @Test
    fun aTakeGoesAfterTheSongOrEndsWithStop() {
        val vm = twoScenes()
        vm.addSongPart(1)
        vm.setTakeAppend(true)
        vm.togglePlay()
        vm.takeClockForTest = 3
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        // The song is one bar long already: the take starts on its bar 2.
        assertEquals(TakeUi.Recording(16, 0, 2), vm.take.value)
        vm.takeClockForTest = 40
        vm.togglePlay()  // Stop: the take ends at the bar being played (step 48)
        assertFalse(vm.transport.value.playing)
        assertEquals(TakeUi.Done(1, 0, 1), vm.take.value)
        assertEquals(listOf(SongPart(1, 1), SongPart(0, 2)), vm.project.value.song)
        // Armed then cancelled: nothing written.
        vm.togglePlay()
        vm.takeClockForTest = 3
        vm.togglePerformance()
        vm.togglePerformance()
        assertEquals(TakeUi.Ready, vm.take.value)
        assertEquals(2, vm.project.value.song.size)
    }

    /** The engine's slot states after a song swap on [step]: every slot on the row's scene from there. */
    private fun swappedOn(step: Int) = IntArray(LiveSlots.COUNT * 3) { i -> when (i % 3) { 0 -> LiveSlots.ROW_TAG; 1 -> LiveSlots.NO_LAUNCH; else -> step } }

    @Test
    fun aTakeArmedDuringASongPartStartsFromWhatThePartPlays() {
        val vm = twoScenes()
        // A part written by an earlier take: row A, the drums muted, the acid on B.
        val part = SongPart(0, 1, setOf(SongPart.DRUMS), play = mapOf(SongPart.ACID to 1), bars = 4)
        vm.session.edit { it.copy(song = listOf(SongPart(1), part)) }
        vm.togglePlay()
        // The engine swapped the part in on step 64; the 40 ms watch saw it on step 67.
        vm.takeClockForTest = 67
        vm.songPartSwappedForTest(1, swappedOn(64))
        val g = vm.liveGrid.value!!
        assertEquals(LiveSlot(-1, null, 64), g.slot(LiveSlots.DRUMS))
        assertEquals(LiveSlot(1, null, 64), g.slot(LiveSlots.ACID))
        // Edits of the drums do not reach the muted track; the acid's per-scene curves are B's.
        assertTrue(vm.waitsForTest(LiveSlots.DRUMS))
        assertEquals(1, vm.laneSceneForTest(AutoTarget.acid(0)))
        assertEquals(0, vm.laneSceneForTest(AutoTarget.live(AutoTarget.LIVE_FILTER)))
        assertNull(vm.laneSceneForTest(AutoTarget(AutoTarget.VOLUME, 0)))  // the drums stopped: left alone
        // ● armed there: it starts on the next bar (80) with the part as it plays.
        vm.togglePerformance()
        assertEquals(TakeUi.Armed(80, 13), vm.take.value)
        vm.takeClockForTest = 80
        vm.tickPerformance()
        vm.setLiveAcid(0, 0.9f)
        // Held in the engine to the take's end (no lane pulls it back between two gestures).
        assertEquals(setOf(AutoTarget.acid(0).pack()), vm.takeHeldForTest)
        vm.takeClockForTest = 100
        vm.togglePerformance()  // ends on the bar line 112
        assertTrue(vm.takeHeldForTest.isEmpty())
        assertEquals(listOf(SongPart(0, 1, setOf(SongPart.DRUMS), play = mapOf(SongPart.ACID to 1), bars = 2)), vm.project.value.song)
    }

    @Test
    fun aTakeThatFillsTheSongEndsAndSaysSo() {
        val vm = twoScenes()
        vm.togglePlay()
        vm.takeClockForTest = 3
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        // The acid changes scene on every bar: a new part each time.
        var b = 1
        while (vm.take.value is TakeUi.Recording && b < 200) {
            vm.launchPad(SongPart.ACID, b % 2)
            vm.liveLaunchedForTest(LiveSlots.ACID, b % 2, 16 + b * 16)
            vm.takeClockForTest = 16 + b * 16 + 1
            vm.tickPerformance()
            b++
        }
        assertTrue(vm.take.value is TakeUi.Done)
        assertEquals(SongPart.MAX_PARTS, vm.project.value.song.size)
        assertTrue(b < 140)  // it ended there, not 70 bars later
        assertTrue(vm.notice.value!!.contains("128"))
    }
}
