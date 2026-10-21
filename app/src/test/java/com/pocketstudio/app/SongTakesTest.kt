package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RecordTarget
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.defaultRecordTarget
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.songStepOf
import com.pocketstudio.model.withAudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import java.io.File

/** Phase 24 audit (rappeur, pro, débutante): takes, song curves and listening in the app's logic. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class SongTakesTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Scenes A (one bar) and B (two bars), the song A B A, a song curve on every bar and a song take in the last part. */
    private fun vmWithSong(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()  // B, shown
        vm.setDrumBars(2)
        vm.selectScene(0)
        listOf(0, 1, 0).forEach(vm::addSongPart)
        vm.addAudioTrack()
        vm.addAutomation(AutoTarget(AutoTarget.VOLUME, 2))
        vm.setAutomationMode(0, AutomationLane.SONG)
        listOf(0.1f, 0.2f, 0.3f, 0.4f).forEachIndexed { bar, v -> vm.setAutomationSongPoint(0, bar, v) }
        vm.session.edit { p -> p.withAudioTrack(p.audioTracks[0].copy(clip = AudioClip("v.wav", "Prise", 4.0, start = 52, song = true))) }
        return vm
    }

    private fun RhythmViewModel.points() = project.value.automation[0].songValues
    private fun RhythmViewModel.clipStart() = project.value.audioTracks[0].clip!!.start

    @Test
    fun songCurvesAndTakesFollowEachArrangementEditInTheSameUndoStep() {
        val vm = vmWithSong()
        assertEquals(listOf(0, 16, 48), vm.project.value.song.indices.map { vm.project.value.songStepOf(it) })
        vm.insertSongPart(0, 1)
        assertEquals(listOf(0.1f, 0.1f, 0.1f, 0.2f, 0.3f, 0.4f), vm.points())
        assertEquals(84, vm.clipStart())
        vm.undo()
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f), vm.points())
        assertEquals(52, vm.clipStart())
        vm.moveSongPart(2, 0)
        assertEquals(listOf(0.4f, 0.1f, 0.2f, 0.3f), vm.points())
        assertEquals(4, vm.clipStart())
        vm.undo()
        vm.removeSongPart(1)
        assertEquals(listOf(0.1f, 0.4f), vm.points())
        assertEquals(20, vm.clipStart())
        vm.undo()
        vm.duplicateSongPart(1)
        assertEquals(84, vm.clipStart())
        vm.undo()
        vm.setSongRepeats(1, 2)
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.3f, 0.3f, 0.4f), vm.points())
        assertEquals(84, vm.clipStart())
        vm.undo()
        assertEquals(52, vm.clipStart())
    }

    @Test
    fun aSceneMadeShorterMovesWhatTheSongPlacesAfterIt() {
        val vm = vmWithSong()
        vm.selectScene(1)
        vm.setDrumBars(1)  // B: one bar instead of two
        assertEquals(listOf(0.1f, 0.2f, 0.4f), vm.points())
        assertEquals(36, vm.clipStart())
        vm.undo()
        assertEquals(52, vm.clipStart())
        // Another time signature: each point and take keeps its place in its part.
        vm.setBeatsPerBar(3)
        val p = vm.project.value
        assertEquals(p.songStepOf(2) + 4, vm.clipStart())
    }

    @Test
    fun withASongATakeGoesOnTheWholeSongByDefault() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RecordTarget(), vm.recordTarget.value)  // no song: the scene
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.addSongPart(0)
        vm.addSongPart(1)
        // Scene B is shown: from its first part.
        assertEquals(RecordTarget(song = true, fromPart = 1), defaultRecordTarget(vm.project.value))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RecordTarget(song = true, fromPart = 1), vm.recordTarget.value)
        // A choice stays.
        vm.setRecordTarget(RecordTarget(song = false))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RecordTarget(song = false), vm.recordTarget.value)
    }

    @Test
    fun theTopRecordButtonTakesAnotherTrackForAnotherPart() {
        val vm = vmWithSong()  // track 0 holds a take in part 3; the default target is part 1 (scene A shown)
        val id = vm.micTakeTrack()
        assertEquals(1, id)
        assertEquals("Voix", vm.project.value.audioTracks.first { it.id == id }.name)
        // The take on track 0 is untouched.
        assertEquals("v.wav", vm.project.value.audioTracks[0].clip?.file)
        // An empty track is used before adding another.
        assertEquals(1, vm.micTakeTrack())
        assertEquals(2, vm.project.value.audioTracks.size)
    }

    @Test
    fun theListeningChoiceIsKeptWithTheProject() {
        val vm = vmWithSong()
        assertFalse(vm.listensToSong)  // the Rhythm tab, never chosen: the section
        assertNull(vm.project.value.listenSong)
        vm.setListenSong(true)
        assertEquals(true, vm.project.value.listenSong)
        assertTrue(vm.listensToSong)
        // Not an undo step: Annuler undoes the last edit, not the choice.
        val before = vm.project.value.automation
        vm.setAutomationSongPoint(0, 5, 0.9f)
        vm.undo()
        assertEquals(before, vm.project.value.automation)
        assertEquals(true, vm.project.value.listenSong)
        // Read back with the project.
        vm.flush()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(vm.session.project.value.listenSong == true)
    }
}
