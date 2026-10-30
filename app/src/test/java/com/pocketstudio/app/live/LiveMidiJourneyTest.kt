package com.pocketstudio.app.live

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.midi.ControllerAction
import com.pocketstudio.app.midi.rhythmKnobHost
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePadTake
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Step 4f (Benjamin's n° 11): a MIDI controller (MiniLab 3 or any keyboard) on the new Live. Its
 * keys go where the screen's keys go (a cell recording, a performance take), its pads into a drum
 * cell recording, its pads' strength reaches the live pads and the take keeps it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveMidiJourneyTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))
    private val t = NoteClip.TICKS_PER_STEP

    /** A: a kick and two synth notes; B: empty (the cells to record into). */
    private fun open(): RhythmViewModel {
        val notes = NoteClip(notes = listOf(Note(0, 24, 60), Note(96, 24, 64)))
        val p = Project(
            name = "Midi",
            scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), synthNotes = notes), Scene("B")),
            scene = 0, drums = DrumPattern().toggle(0, 0), synthNotes = notes,
        ).sanitized()
        val id = "midi-" + System.nanoTime()
        store.save(id, p)
        CurrentProject.set(app, id)
        return RhythmViewModel(app)
    }

    @Test
    fun keysOfAMidiKeyboardGoIntoTheCellRecordingOfItsTrack() {
        val vm = open()
        vm.playedClockForTest = 20.0
        vm.recordCell(SongPart.inst(0), 1)
        // The keyboard plays the cell's track.
        assertEquals(0, vm.selectedTrack.value)
        vm.playedClockForTest = 32.0; vm.midiNote(0, 62, 0.7f, true)
        vm.playedClockForTest = 34.0; vm.midiNote(0, 62, 0f, false)
        vm.playedClockForTest = 48.0
        vm.stopCellRecording()
        val clip = vm.project.value.allScenes[1].synthNotes
        assertEquals(listOf(0 to 62), clip.notes.map { it.start to it.pitch })
        assertEquals(2 * t, clip.notes.single().length)
        assertEquals(0.7f, clip.notes.single().velocity, 1e-3f)
    }

    @Test
    fun recordingIntoACellOfAnotherTrackMakesTheKeyboardPlayThatTrack() {
        val vm = open()
        vm.addTrack()
        val other = vm.project.value.instrumentTracks.last().id
        vm.selectTrack(0)
        vm.recordCell(SongPart.inst(other), 1)
        assertEquals(other, vm.selectedTrack.value)
    }

    @Test
    fun keysOfAMidiKeyboardGoIntoAPerformanceTake() {
        val vm = open()
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        assertTrue(vm.take.value is TakeUi.Recording)
        vm.takeClockForTest = 20; vm.midiNote(0, 67, 0.8f, true)
        vm.takeClockForTest = 22; vm.midiNote(0, 67, 0f, false)
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertTrue(vm.take.value is TakeUi.Done)
        // As the screen's keys: step 20 of A's one-bar loop, step 4, two sixteenths long.
        assertTrue(vm.project.value.allScenes[0].synthNotes.notes.any { it.pitch == 67 && it.start == 4 * t && it.length == 2 * t })
        vm.undo()
        assertTrue(vm.project.value.allScenes[0].synthNotes.notes.none { it.pitch == 67 })
    }

    /**
     * Hot-plug: the keyboard unplugged (or a « all notes off ») while keys are held: their notes
     * end there for the app too (a cell recording or a take never keeps one ringing to its end).
     */
    @Test
    fun aKeyboardUnpluggedWhileAKeyIsHeldEndsItsNoteForTheApp() {
        com.pocketstudio.engine.NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        try {
            val midi = com.pocketstudio.app.midi.MidiViewModel(app)
            val got = ArrayList<com.pocketstudio.app.midi.MidiCommand>()
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            scope.launch { midi.commands.collect { got += it } }
            midi.receiveForTest(byteArrayOf(0x90.toByte(), 62, 100))
            assertEquals(com.pocketstudio.app.midi.MidiCommand.Note(0, 62, 100 / 127f, true), got.last())
            midi.disconnect()
            assertEquals(com.pocketstudio.app.midi.MidiCommand.Note(0, 62, 0f, false), got.last())
            // Plugged again: it plays as before.
            midi.receiveForTest(byteArrayOf(0x90.toByte(), 64, 90))
            assertEquals(com.pocketstudio.app.midi.MidiCommand.Note(0, 64, 90 / 127f, true), got.last())
            midi.receiveForTest(byteArrayOf(0xB0.toByte(), 123, 0))  // all notes off
            assertTrue(got.contains(com.pocketstudio.app.midi.MidiCommand.Note(0, 64, 0f, false)))
            scope.cancel()
        } finally {
            com.pocketstudio.engine.NativeEngine.sink = null
        }
    }

    @Test
    fun padsOfTheControllerGoIntoADrumCellRecordingWithTheirStrength() {
        val vm = open()
        vm.playedClockForTest = 3.0
        vm.recordCell(SongPart.DRUMS, 1)
        vm.playedClockForTest = 16.1
        assertTrue(vm.midiCellPad(2, 0.4f))
        vm.playedClockForTest = 20.0
        assertTrue(vm.midiCellPad(1, 1f))
        vm.playedClockForTest = 32.0
        vm.stopCellRecording()
        val d = vm.project.value.allScenes[1].drums
        assertEquals(setOf(0), d.lanes[2].hits.keys)
        assertEquals(0.4f, d.lanes[2].hits[0]!!.velocity, 1e-3f)
        assertEquals(setOf(4), d.lanes[1].hits.keys)
        // No drum cell recording: the pads keep their Live role (launches, mutes, live pads).
        assertFalse(vm.midiCellPad(2, 0.4f))
    }

    @Test
    fun aLivePadHitSoftlyOnTheControllerPlaysSoftAndTheTakeKeepsIt() {
        val vm = open()
        vm.session.edit { it.copy(live = it.live.with(0, LiveClip("x.wav", "Boucle", beats = 4))) }
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.takeClockForTest = 20
        val host = rhythmKnobHost(vm)
        host.act(ControllerAction.LivePad(0, velocity = 0.5f))
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertEquals(listOf(LivePadTake.Span(0, 32.0, 48.0, velocity = 0.5f)), vm.padSpansForTest)
    }
}
