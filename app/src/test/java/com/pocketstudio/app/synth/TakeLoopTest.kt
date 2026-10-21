package com.pocketstudio.app.synth

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TakeLoopTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun aOneBarLoopGrowsToFourBarsAndSoundsTheSame() {
        val riff = listOf(Note(0, 24, 60), Note(96, 48, 64))
        val grown = TakeLoop.grown(NoteClip(16, riff))!!
        assertEquals(64, grown.steps)
        // The bar repeated in each of the four bars: what played before plays the same.
        assertEquals((0 until 4).flatMap { b -> riff.map { it.copy(start = it.start + b * 16 * NoteClip.TICKS_PER_STEP) } }, grown.notes)
        // Empty: just longer.
        assertEquals(NoteClip(64), TakeLoop.grown(NoteClip(16)))
        // Notes hidden past a shortened loop come back instead of being covered by copies.
        val hidden = NoteClip(64, listOf(Note(0, 24, 60), Note(16 * 24 * 2, 24, 67))).copy(steps = 16).sanitized()
        assertEquals(listOf(Note(0, 24, 60), Note(16 * 24 * 2, 24, 67)), TakeLoop.grown(hidden)!!.notes)
        // Already longer than a bar: left alone.
        assertNull(TakeLoop.grown(NoteClip(32, riff)))
    }

    @Test
    fun armingTheTakeOnTheSynthGivesFourBarsInItsOwnUndoStep() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.setNoteClipSteps(16)
        vm.clearNotes()
        vm.addNote(Note(0, 24, 60))
        // ● from the drums: the note loop is not touched.
        vm.toggleRecord()
        vm.toggleRecord()
        assertEquals(16, vm.project.value.track(0).notes.steps)
        assertFalse(vm.takeLoopGrown.value)
        // ● from the synth: four bars, and it says so.
        vm.toggleRecord(notes = true)
        assertEquals(64, vm.project.value.track(0).notes.steps)
        assertEquals(4, vm.project.value.track(0).notes.notes.size)
        assertTrue(vm.takeLoopGrown.value)
        assertFalse("no song: nothing else grew", vm.takeLoopSong.value)
        vm.toggleRecord()
        // Undo gives the one bar back (after the take's own step, if it recorded anything).
        repeat(2) { if (vm.project.value.track(0).notes.steps != 16) vm.undo() }
        assertEquals(16, vm.project.value.track(0).notes.steps)
        assertEquals(listOf(Note(0, 24, 60)), vm.project.value.track(0).notes.notes)
        vm.dismissTakeLoop()
        assertFalse(vm.takeLoopGrown.value)
    }

    @Test
    fun armingTheTakeSaysWhenTheSongGrowsToo() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.setNoteClipSteps(16)
        vm.clearNotes()
        vm.addNote(Note(0, 24, 60))
        vm.addSongPart(0)
        vm.addSongPart(0)
        assertEquals(16, vm.project.value.sceneSteps(0))
        vm.toggleRecord(notes = true)
        vm.toggleRecord()
        // The scene now lasts four bars, and so do its two song parts: the notice says so.
        assertEquals(64, vm.project.value.sceneSteps(0))
        assertTrue(vm.takeLoopGrown.value)
        assertTrue(vm.takeLoopSong.value)
        vm.dismissTakeLoop()
        assertFalse(vm.takeLoopSong.value)
    }
}
