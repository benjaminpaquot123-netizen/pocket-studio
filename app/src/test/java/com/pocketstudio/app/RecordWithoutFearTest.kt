package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.app.record.takeCounter
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SongVersion
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.liveQuantumSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Phase 35 « Enregistrer sans peur », in the app's logic: takes, their card, the last bars kept, Live versions. */
@RunWith(RobolectricTestRunner::class)
class RecordWithoutFearTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val t = NoteClip.TICKS_PER_STEP

    private fun vm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app).also { it.playingForTest = true }
    }

    /** Key [pitch] down at sixteenth [from], up at [to], while the take records. */
    private fun RhythmViewModel.play(pitch: Int, from: Double, to: Double) {
        recordPositionForTest = from
        playedClockForTest = from
        synthNoteOn(pitch, 0.8f)
        recordPositionForTest = to
        playedClockForTest = to
        synthNoteOff(pitch)
    }

    private fun RhythmViewModel.notes() = project.value.synthNotes.notes

    @Test
    fun aTakeIsKeptAsPlayedThenPutInTimeWithoutLosingAnythingEachAnUndoStep() {
        val vm = vm()
        val depth0 = vm.session.undoDepth
        vm.recordTake(TakeSource.KEYS)
        assertEquals(TakePhase.RECORDING, vm.noteTake.value!!.phase)
        vm.play(60, 1.2, 2.9)
        vm.play(62, 3.8, 5.0)
        vm.recordTake(TakeSource.KEYS)
        val done = vm.noteTake.value!!
        assertEquals(TakePhase.DONE, done.phase)
        assertEquals(2, done.notes)
        // As played: not on the sixteenths.
        assertEquals(listOf(28, 91), vm.notes().map { it.start })
        vm.takeInTime(1f)
        assertEquals(listOf(t, 4 * t), vm.notes().map { it.start })
        assertEquals(1f, vm.noteTake.value!!.strength)
        // Another strength, from where it was played (not from the grid): the same undo step.
        vm.takeInTime(0.5f)
        assertEquals(listOf(26, 94), vm.notes().map { it.start })
        vm.takeAsPlayed()
        assertEquals(listOf(28, 91), vm.notes().map { it.start })
        vm.takeInTime(1f)
        vm.keepNoteTake()
        assertNull(vm.noteTake.value)
        // One undo: as played again; one more: no take; one more: the one-bar loop is back (it grew to four for the take).
        vm.undo()
        assertEquals(listOf(28, 91), vm.notes().map { it.start })
        vm.undo()
        assertTrue(vm.notes().isEmpty())
        assertEquals(64, vm.project.value.synthNotes.steps)
        vm.undo()
        assertEquals(16, vm.project.value.synthNotes.steps)
        assertEquals(depth0, vm.session.undoDepth)
    }

    @Test
    fun refaireTakesTheTakeAwayAndRecordsAgain() {
        val vm = vm()
        vm.recordTake(TakeSource.KEYS)
        vm.play(60, 1.0, 2.0)
        vm.recordTake(TakeSource.KEYS)
        vm.redoTake()
        assertTrue(vm.notes().isEmpty())
        assertTrue(vm.recording.value)
        assertEquals(TakePhase.RECORDING, vm.noteTake.value!!.phase)
        vm.play(64, 8.0, 9.0)
        vm.recordTake(TakeSource.KEYS)
        assertEquals(listOf(64), vm.notes().map { it.pitch })
        assertEquals(1, vm.noteTake.value!!.notes)
    }

    @Test
    fun aTakeWithNothingPlayedSaysSoWithoutACard() {
        val vm = vm()
        vm.recordTake(TakeSource.KEYS)
        vm.recordTake(TakeSource.KEYS)
        assertNull(vm.noteTake.value)
        assertTrue(vm.notice.value!!.contains("Rien"))
    }

    @Test
    fun theLastBarsPlayedAreKeptInOneTouchEvenWithoutRecording() {
        val vm = vm()
        vm.playingForTest = false
        assertFalse(vm.canKeepPlayed.value)
        // Played freely, stopped: no take.
        vm.playedClockForTest = 100.0
        vm.synthNoteOn(60, 0.8f)
        vm.playedClockForTest = 102.0
        vm.synthNoteOff(60)
        vm.playedClockForTest = 104.4
        vm.synthNoteOn(67, 0.8f)
        vm.playedClockForTest = 106.0
        vm.synthNoteOff(67)
        assertTrue(vm.notes().isEmpty())
        assertTrue(vm.canKeepPlayed.value)
        vm.keepPlayed()
        // The first note starts the loop; the rest as played.
        assertEquals(listOf(0 to 60, Math.round(4.4 * t).toInt() to 67), vm.notes().map { it.start to it.pitch })
        assertFalse(vm.canKeepPlayed.value)
        val card = vm.noteTake.value!!
        assertEquals(TakeSource.CAPTURE, card.source)
        assertEquals(2, card.notes)
        vm.takeInTime(1f)
        assertEquals(listOf(0, 4 * t), vm.notes().map { it.start })
        // « Enlever »: gone, in one step of its own.
        vm.redoTake()
        assertTrue(vm.notes().isEmpty())
        vm.undo()
        assertEquals(2, vm.notes().size)
    }

    @Test
    fun padsPlayedAreKeptToo() {
        val vm = vm()
        vm.playingForTest = false
        val lane = (0 until 16).first { l -> vm.project.value.drums.lanes[l].hits.isEmpty() }
        vm.playedClockForTest = 10.0
        vm.hitPad(lane)
        vm.playedClockForTest = 14.1
        vm.hitPad(lane)
        vm.keepPlayed()
        assertEquals(setOf(0, 4), vm.project.value.drums.lanes[lane].hits.keys)
        assertEquals(2, vm.noteTake.value!!.hits)
        assertFalse(vm.noteTake.value!!.canPutInTime)
    }

    @Test
    fun theCounterSaysThePartAndItsBar() {
        val vm = vm()
        vm.toggleHit(0, 0)
        vm.setDrumBars(4)
        vm.renameScene(0, "Couplet")
        vm.duplicateScene()
        vm.renameScene(1, "Refrain")
        vm.selectScene(0)
        listOf(0, 0, 1).forEach(vm::addSongPart)
        val p = vm.project.value
        // The song's bar 6 (from 0) is the second Couplet's bar 3 (4 bars each).
        assertEquals("Couplet · mes. 3/4", takeCounter(p, 1, 6))
        assertEquals("Refrain · mes. 1/4", takeCounter(p, 2, 8))
        // A part alone: its own bars.
        assertEquals("Couplet · mes. 2/4", takeCounter(p, -1, 5))
        assertNull(takeCounter(p, -1, -1))
    }

    // --- Live: no take overwrites the song without a way back --------------------------------------

    /** Scenes A and B, the song A B, then a Live take (« Nouvelle version », the default) launching B's drums. */
    private fun liveTake(): Pair<RhythmViewModel, List<SongPart>> {
        val vm = vm()
        vm.playingForTest = false
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        listOf(0, 1).forEach(vm::addSongPart)
        val song = vm.project.value.song
        assertFalse(vm.takeAppend.value)
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertTrue(vm.take.value is TakeUi.Done)
        return vm to song
    }

    @Test
    fun aLiveTakeKeepsTheSongItReplacesAsAVersionAndARestoreSwapsThem() {
        val (vm, before) = liveTake()
        val taken = vm.project.value.song
        assertTrue(taken != before)
        val v = vm.project.value.songVersions.single()
        assertEquals(SongVersion.BEFORE_TAKE, v.kind)
        assertEquals(before, v.song)
        // One touch brings the song back; the take's song is kept in its place.
        vm.restoreSongVersion(0)
        assertEquals(before, vm.project.value.song)
        assertEquals(taken, vm.project.value.songVersions.single().song)
        assertEquals(SongVersion.RESTORE, vm.project.value.songVersions.single().kind)
        // One undo step.
        vm.undo()
        assertEquals(taken, vm.project.value.song)
        assertEquals(before, vm.project.value.songVersions.single().song)
        // And the take's own undo still takes it all back.
        vm.undo()
        assertEquals(before, vm.project.value.song)
        assertTrue(vm.project.value.songVersions.isEmpty())
    }

    @Test
    fun aVersionIsKeptOnDiskAndSurvivesReopening() {
        val (vm, before) = liveTake()
        // The take saves the song itself (then drops its journal): let the main thread run until it is on disk.
        val store = com.pocketstudio.model.ProjectStore(File(app.filesDir, "projects"))
        val deadline = System.currentTimeMillis() + 10_000
        var saved = store.load(vm.projectId)
        while (saved?.songVersions.isNullOrEmpty() && System.currentTimeMillis() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))
            Thread.sleep(20)
            saved = store.load(vm.projectId)
        }
        saved!!
        assertEquals(before, saved.songVersions.single().song)
        assertEquals(vm.project.value.song, saved.song)
    }

    @Test
    fun aBeatOrHalfABarLaunchesButATakeWritesWholeBars() {
        val vm = vm()
        vm.setLiveQuantum(-1)
        assertEquals(4, vm.project.value.liveQuantumSteps)
        vm.setLiveQuantum(-2)
        assertEquals(8, vm.project.value.liveQuantumSteps)
        vm.setLiveQuantum(2)
        assertEquals(0, vm.project.value.liveQuantumFine)
        assertEquals(2, vm.project.value.liveQuantum)
    }

    @Test
    fun theTakeOfTheRollHasTheScenesNotesAndNoteTakeCardSaysNothingAfterUndo() {
        val vm = vm()
        vm.recordTake(TakeSource.KEYS)
        vm.play(60, 1.0, 2.0)
        vm.recordTake(TakeSource.KEYS)
        assertEquals(1, vm.project.value.allScenes[0].synthNotes.notes.size)
        vm.undo()
        assertNull(vm.noteTake.value)
    }

    /** Scenes A and B, the song A B, a live pad, then a Live take recording (not ended). */
    private fun recordingLive(): RhythmViewModel {
        val vm = vm()
        vm.playingForTest = false
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        listOf(0, 1).forEach(vm::addSongPart)
        vm.session.edit { it.copy(live = it.live.with(0, com.pocketstudio.model.LiveClip("x.wav", "Batterie", beats = 4))) }
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        assertTrue(vm.take.value is TakeUi.Recording)
        return vm
    }

    @Test
    fun notesPlayedOnTheLivePageGoIntoTheTakeOnTheSixteenthAndTheVersionKeepsTheSceneAsItWas() {
        val vm = recordingLive()
        val before = vm.project.value.allScenes[0].synthNotes.notes
        vm.takeClockForTest = 20
        vm.liveNoteOn(0, 64, 0.8f)
        vm.takeClockForTest = 22
        vm.liveNoteOff(0, 64)
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertTrue(vm.take.value is TakeUi.Done)
        val notes = vm.project.value.allScenes[0].synthNotes.notes
        // Step 20 of the row's loop (launched at 0), a one-bar loop: step 4.
        assertTrue(notes.any { it.pitch == 64 && it.start == 4 * t && it.length == 2 * t })
        // The song before the take, its scene as it was: no new note there.
        assertEquals(before, vm.project.value.songVersions.single().scenes[0].synthNotes.notes)
        // One undo: the take and its notes.
        vm.undo()
        assertTrue(vm.project.value.allScenes[0].synthNotes.notes.none { it.pitch == 64 })
    }

    @Test
    fun theLivePadsPlayedDuringATakeAreRecorded() {
        val vm = recordingLive()
        vm.takeClockForTest = 20
        vm.toggleLive(0)  // waits for its bar line (step 32)
        vm.takeClockForTest = 40
        vm.togglePerformance()  // ends at the bar line after: 48
        assertEquals(listOf(com.pocketstudio.model.LivePadTake.Span(0, 32.0, 48.0)), vm.padSpansForTest)
    }
}
