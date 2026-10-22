package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.Note
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/** Phase 29 package C: what is played, edited or undone while the song plays goes where it is heard. */
@RunWith(RobolectricTestRunner::class)
class SongRecordTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Scenes A, B, C (kick on 0; 0 and 4; 0, 4 and 8) and the song A B C. */
    private fun vmWithSong(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(0, 4)
        vm.duplicateScene()
        vm.toggleHit(0, 8)
        vm.selectScene(0)
        listOf(0, 1, 2).forEach(vm::addSongPart)
        vm.playingForTest = true
        return vm
    }

    /** The engine's slots: every track on the part's row, from [step]. */
    private fun onRow(step: Int = 0) = IntArray(LiveSlots.COUNT * 3) { i -> when (i % 3) { 0 -> LiveSlots.ROW_TAG; 1 -> LiveSlots.NO_LAUNCH; else -> step } }

    private fun RhythmViewModel.hits(scene: Int, lane: Int) = project.value.allScenes[scene].drums.lanes[lane].hits.keys

    /** Lanes the starting kit leaves empty in every scene. */
    private fun RhythmViewModel.emptyLanes() = (0 until 16).filter { lane -> project.value.allScenes.all { it.drums.lanes[lane].hits.isEmpty() } }

    private fun idle() = shadowOf(android.os.Looper.getMainLooper()).idle()

    @Test
    fun aHitAtAPartChangeGoesToTheSceneItWasHeardIn() {
        val vm = vmWithSong()
        vm.toggleRecord()
        vm.songPartSwappedForTest(0, onRow())
        // The engine went on to part B (one bar after A), the screens follow it.
        vm.songPartSwappedForTest(1, onRow(16))
        vm.selectScene(1)
        assertEquals(1, vm.project.value.scene)
        val (x, y, z) = vm.emptyLanes()
        // Heard a sixteenth before B began (the output latency): A's last step, not B's first.
        vm.recordPositionForTest = -1.0
        vm.hitPad(x)
        assertEquals(setOf(15), vm.hits(0, x))
        assertEquals(emptySet<Int>(), vm.hits(1, x))
        // Just after (or a hair before, rounding onto it): B's first step.
        vm.recordPositionForTest = -0.3
        vm.hitPad(y)
        vm.recordPositionForTest = 2.1
        vm.hitPad(z)
        assertEquals(setOf(0), vm.hits(1, y))
        assertEquals(setOf(2), vm.hits(1, z))
        assertEquals(emptySet<Int>(), vm.hits(0, y) + vm.hits(0, z))
    }

    @Test
    fun stayingOnASceneATakeGoesToTheSceneHeardAndSaysSo() {
        val vm = vmWithSong()
        vm.setFollowSong(false)  // « Rester sur ma scène »: A stays shown
        vm.toggleRecord()
        vm.songPartSwappedForTest(1, onRow(16))
        idle()
        assertEquals(0, vm.project.value.scene)
        assertEquals(1, vm.recordInto.value)
        val lane = vm.emptyLanes().first()
        vm.recordPositionForTest = 2.0
        vm.hitPad(lane)
        assertEquals(setOf(2), vm.hits(1, lane))
        assertEquals(emptySet<Int>(), vm.hits(0, lane))
        // A note played there too, on the open track, in B.
        vm.recordPositionForTest = 4.0
        vm.recordNoteOn(60, 0.8f)
        vm.recordPositionForTest = 6.0
        vm.recordNoteOff(60)
        assertEquals(listOf(Note(96, 48, 60, 0.8f)), vm.project.value.allScenes[1].synthNotes.notes)
        assertTrue(vm.project.value.allScenes[0].synthNotes.notes.isEmpty())
        // Shown and heard again the same: nothing said.
        vm.toggleRecord()
        idle()
        assertNull(vm.recordInto.value)
    }

    /** C2 (already so since the live grid follows the song parts): an edit of a track the part mutes waits. */
    @Test
    fun aTrackMutedInThePlayingPartIsNotSentWhenEdited() {
        val vm = vmWithSong()
        vm.session.edit { it.copy(song = listOf(SongPart(0, muted = setOf(SongPart.DRUMS)), SongPart(1))) }
        vm.togglePlay()
        vm.songPartSwappedForTest(0, onRow())
        assertTrue(vm.waitsForTest(LiveSlots.DRUMS))
        assertFalse(vm.waitsForTest(LiveSlots.ACID))
        assertFalse(vm.waitsForTest(LiveSlots.INST))
    }

    @Test
    fun aFaderMoveSendsItsStripOnly() {
        val vm = vmWithSong()
        val pushes = mutableListOf<String>()
        vm.mixerPushesForTest = pushes
        vm.setChannelVolume(0, 0.5f)
        vm.setChannelPan(0, -0.3f)
        vm.toggleMute(1)
        assertEquals(listOf("strip 0", "strip 0", "strip 1"), pushes)
    }

    @Test
    fun undoWhileTheSongPlaysKeepsThePartChosenToComeNext() {
        val vm = vmWithSong()
        vm.togglePlay()
        vm.songPartSwappedForTest(0, onRow())
        vm.queueSongPartForTest(2)  // « ensuite »: C after A
        vm.toggleHit(3, 3)
        vm.undo()
        assertEquals(0, vm.sessionUi.value.songIndex)
        assertEquals(2, vm.sessionUi.value.queued)
        // A part chosen that the undo takes away is forgotten.
        vm.addSongPart(1)
        vm.queueSongPartForTest(3)
        vm.undo()
        assertEquals(3, vm.project.value.song.size)
        assertEquals(-1, vm.sessionUi.value.queued)
    }
}
