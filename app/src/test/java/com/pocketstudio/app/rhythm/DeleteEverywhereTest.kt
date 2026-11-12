package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.cellHasContent
import com.pocketstudio.model.firstTrackDeleted
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Owner (2026-11-04, « J'aimerais supprimer le synthé, je sais pas comment »): every track of « Mes
 * pistes » but the Batterie can go, Annuler brings it back, and the project saved and read again
 * keeps it gone. The same gap closed elsewhere: an unwanted audio take, a full cell of the Live grid.
 */
@RunWith(RobolectricTestRunner::class)
class DeleteEverywhereTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun vm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app)
    }

    private fun clip(vararg pitches: Int) = NoteClip(steps = 16, notes = pitches.mapIndexed { i, p -> Note(i * 4, 2, p) })

    /** Two bricks, the first track (« Synthé ») playing in both, an added « Basse » track. */
    private fun song(vm: RhythmViewModel) {
        vm.session.edit { p ->
            val a = clip(60, 62)
            val b = clip(67)
            p.copy(
                synthNotes = a,
                scenes = listOf(Scene("A", p.drums, p.acid, a), Scene("B", p.drums, p.acid, b)),
                scene = 0,
            )
        }
        vm.addTrackOf(TrackKind.Bass)
    }

    private fun keys(p: Project) = simpleTracks(p).map { it.key }

    private fun reopened(vm: RhythmViewModel): Project {
        assertTrue(runBlocking { vm.flushNow() })
        return RhythmViewModel(app).project.value
    }

    @Test
    fun theFirstTrackGoesFromMyTracksEmptiedInEveryBrickAndComesBackWithAnnuler() {
        val vm = vm()
        song(vm)
        val before = vm.project.value
        assertTrue(SongPart.inst(0) in keys(before))
        vm.deleteTrack(0)
        val p = vm.project.value
        assertTrue(p.firstTrackDeleted)
        assertFalse("gone from « Mes pistes »", SongPart.inst(0) in keys(p))
        assertTrue("the other tracks stay", SongPart.inst(vm.project.value.tracks.single().id) in keys(p))
        assertTrue("no note left in any brick", p.allScenes.all { it.synthNotes.notes.isEmpty() })
        // One step back: as it was.
        vm.undo()
        assertEquals(before.allScenes.map { it.synthNotes }, vm.project.value.allScenes.map { it.synthNotes })
        assertTrue(SongPart.inst(0) in keys(vm.project.value))
        vm.redo()
        // Saved and read again: still gone.
        val again = reopened(vm)
        assertTrue(again.firstTrackHidden)
        assertFalse(SongPart.inst(0) in keys(again))
    }

    /** Old projects load unchanged: the flag is left out of the file while false. */
    @Test
    fun theFlagIsLeftOutOfOldFiles() {
        val text = com.pocketstudio.model.ProjectJson.encode(Project())
        assertFalse(text.contains("firstTrackHidden"))
        assertFalse(com.pocketstudio.model.ProjectJson.decode(text).firstTrackHidden)
        val hidden = com.pocketstudio.model.ProjectJson.decode(com.pocketstudio.model.ProjectJson.encode(Project().copy(firstTrackHidden = true)))
        assertTrue(hidden.firstTrackDeleted)
    }

    @Test
    fun aTrackAddedAfterwardsTakesTheFirstTracksPlace() {
        val vm = vm()
        vm.deleteTrack(0)
        assertTrue(keys(vm.project.value).none { it.startsWith("inst:") })
        vm.addTrackOf(TrackKind.Melody)
        val p = vm.project.value
        assertFalse(p.firstTrackHidden)
        assertTrue("no hidden blank track beside it", p.tracks.isEmpty())
        assertEquals(TrackKind.Melody.trackName, p.track(0).name)
        assertTrue(SongPart.inst(0) in keys(p))
        assertEquals(0, vm.selectedTrack.value)
        // One step: Annuler gives the deleted state back.
        vm.undo()
        assertTrue(vm.project.value.firstTrackDeleted)
    }

    @Test
    fun anAddedTrackAndAnAudioTrackGoWithAnnulerPossible() {
        val vm = vm()
        song(vm)
        vm.session.edit { p -> p.copy(audioTracks = listOf(AudioTrack(0, "Voix", clip = AudioClip("voix.wav", "Voix", seconds = 2.0)))) }
        val bass = vm.project.value.tracks.single().id
        vm.deleteTrack(bass)
        vm.removeAudioTrack(0)
        assertEquals(listOf(SongPart.DRUMS, SongPart.inst(0)), keys(vm.project.value) - SongPart.ACID)
        vm.undo()
        assertTrue(SongPart.audio(0) in keys(vm.project.value))
        vm.undo()
        assertTrue(SongPart.inst(bass) in keys(vm.project.value))
    }

    @Test
    fun theAcidLineGoesFromEveryBrick() {
        val vm = vm()
        vm.session.edit { p ->
            val acid = p.acid.copy(steps = p.acid.steps.mapIndexed { i, s -> if (i % 4 == 0) AcidStep(gate = true) else s })
            p.copy(acid = acid, scenes = listOf(Scene("A", p.drums, acid, p.synthNotes), Scene("B", p.drums, acid, p.synthNotes)))
        }
        assertTrue(SongPart.ACID in keys(vm.project.value))
        vm.removeAcidLine()
        assertFalse(SongPart.ACID in keys(vm.project.value))
        assertTrue(vm.project.value.allScenes.none { s -> s.acid.steps.any { it.gate } })
        vm.undo()
        assertTrue(SongPart.ACID in keys(vm.project.value))
    }

    @Test
    fun theBatterieIsNotOffered() {
        val vm = vm()
        val drums = simpleTracks(vm.project.value).first { it.key == SongPart.DRUMS }
        assertFalse(drums.deletable)
        assertTrue(simpleTracks(vm.project.value).filter { it.key != SongPart.DRUMS }.all { it.deletable })
    }

    @Test
    fun anUnwantedTakeLeavesThePickerAndComesBack() {
        val vm = vm()
        val now = AudioClip("c.wav", "Prise 3", seconds = 1.0)
        val older = listOf(AudioClip("b.wav", "Prise 2", seconds = 1.0), AudioClip("a.wav", "Prise 1", seconds = 1.0))
        vm.session.edit { p -> p.copy(audioTracks = listOf(AudioTrack(0, "Voix", clip = now, previous = older[0], older = listOf(older[1])))) }
        vm.forgetTake(0, 0)
        val t = vm.project.value.audioTracks.single()
        assertEquals("the one playing stays", "c.wav", t.clip?.file)
        assertEquals(listOf("a.wav"), t.otherTakes.map { it.file })
        vm.undo()
        assertEquals(listOf("b.wav", "a.wav"), vm.project.value.audioTracks.single().otherTakes.map { it.file })
        vm.redo()
        assertEquals(listOf("a.wav"), reopened(vm).audioTracks.single().otherTakes.map { it.file })
    }

    @Test
    fun aFullCellOfTheLiveGridIsEmptiedInItsBrickOnly() {
        val vm = vm()
        song(vm)
        val key = SongPart.inst(0)
        assertTrue(vm.project.value.cellHasContent(key, 1))
        vm.clearCell(key, 1)
        assertFalse(vm.project.value.cellHasContent(key, 1))
        assertTrue("the other brick keeps its notes", vm.project.value.cellHasContent(key, 0))
        vm.undo()
        assertTrue(vm.project.value.cellHasContent(key, 1))
        assertEquals(2, vm.project.value.instrumentTracks.size)
    }
}
