package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.synth.loopBars
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Audit final (lot notes), in the app's logic: takes, the MIDI clock, linked basses, the loop's bars. */
@RunWith(RobolectricTestRunner::class)
class NotesLotTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val t = NoteClip.TICKS_PER_STEP

    private fun vm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app).also { it.playingForTest = true }
    }

    @Test
    fun aPadPlayedOnAStepKeepsItsRollPitchAndGlide() {
        val vm = vm()
        val rolled = Hit(0.9f, repeats = 3, pitch = 5, glide = 40)
        vm.session.edit { p -> p.copy(drums = p.drums.copy(lanes = p.drums.lanes.toMutableList().also { it[0] = it[0].copy(hits = mapOf(4 to rolled)) })) }
        vm.recordTake(TakeSource.PADS)
        vm.recordPositionForTest = 4.1
        vm.midiNote(9, 36, 0.6f, true)
        vm.recordTake(TakeSource.PADS)
        assertEquals(rolled.copy(velocity = 0.6f), vm.project.value.drums.lanes[0].hits[4])
    }

    @Test
    fun theSustainPedalHoldsANoteOfATakeUntilItComesUp() {
        val vm = vm()
        vm.recordTake(TakeSource.KEYS)
        vm.recordPositionForTest = 1.0
        vm.synthNoteOn(60, 0.8f)
        vm.recordSustain(true)
        vm.recordPositionForTest = 2.0
        vm.synthNoteOff(60)
        // Released, still held by the pedal: nothing written yet.
        assertTrue(vm.project.value.synthNotes.notes.isEmpty())
        vm.recordPositionForTest = 6.0
        vm.recordSustain(false)
        vm.recordTake(TakeSource.KEYS)
        assertEquals(listOf(Note(t, 5 * t, 60, 0.8f)), vm.project.value.synthNotes.notes)
    }

    @Test
    fun aMidiStartNeverCountsIn() {
        val vm = vm()
        vm.toggleCountIn()
        assertTrue(vm.transport.value.countIn)
        vm.midiStart(fromTop = true)
        assertTrue(vm.transport.value.playing)
        assertEquals(0, vm.countInForTest)
        // A Start while playing starts again from the top (both machines line up); Continue goes on.
        vm.midiStart(fromTop = false)
        assertTrue(vm.transport.value.playing)
        vm.togglePlay()
        vm.togglePlay()
        assertEquals(1, vm.countInForTest)
    }

    @Test
    fun theAssistantsSubFollowsTheBassEditedInThePianoRoll() {
        val vm = vm()
        val style = StarterWizard.style("jump_up")!!
        val built = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
        vm.session.edit { built }
        val p = vm.project.value
        val bass = p.instrumentTracks.first { it.name == "Basse" }.id
        val sub = p.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }.id
        val second = p.instrumentTracks.first { it.name == StarterWizard.BASS_SWITCH_TRACK }.id
        vm.selectScene(2)
        vm.selectTrack(bass)
        vm.addNote(Note(15 * t, 12, 43))
        val q = vm.project.value
        val notes = q.track(bass).notes.notes
        assertTrue(Note(15 * t, 12, 43) in notes)
        assertEquals(StarterWizard.subNotes(notes), q.track(sub).notes.notes)
        assertEquals(notes, q.track(second).notes.notes)
        // One undo step takes all three back.
        vm.undo()
        assertEquals(p.allScenes[2].trackNotes[sub], vm.project.value.allScenes[2].trackNotes[sub])
    }

    @Test
    fun theLoopGrowsAndShrinksByBarsOfTheTimeSignature() {
        assertEquals(24, loopBars(12, 12, 1))
        assertEquals(12, loopBars(24, 12, -1))
        // A 4/4 loop of 64 steps in 3/4: to whole 3/4 bars first.
        assertEquals(72, loopBars(64, 12, 1))
        assertEquals(60, loopBars(64, 12, -1))
        assertEquals(32, loopBars(16, 16, 1))
    }
}
