package com.pocketstudio.app.live

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.liveActionsOf
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.cellHasContent
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import kotlin.math.PI
import kotlin.math.sin

/**
 * Benjamin's n° 9 through the Live page and the ViewModel: an empty cell's menu, then each of its
 * three choices; each undone in one step, kept through a save and an opening.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveCellJourneyTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** A: a kick and two synth notes; B: a 303 line only (its drum and synth cells empty). */
    private fun open(): RhythmViewModel {
        val notes = NoteClip(notes = listOf(Note(0, 24, 60), Note(96, 24, 64)))
        val acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { AcidStep(gate = it % 4 == 0) })
        val p = Project(
            name = "Cases",
            scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), synthNotes = notes), Scene("B", acid = acid)),
            scene = 0, drums = DrumPattern().toggle(0, 0), synthNotes = notes,
        ).sanitized()
        val id = "cells-" + System.nanoTime()
        store.save(id, p)
        CurrentProject.set(app, id)
        return RhythmViewModel(app)
    }

    private var librarySaid: Pair<String, Int>? = null

    private fun show(vm: RhythmViewModel) {
        compose.setContent {
            val p by vm.project.collectAsState()
            val rec by vm.cellRecording.collectAsState()
            PocketStudioTheme {
                LiveScreen(
                    LiveScreenUi(p, cellRecording = rec),
                    liveActionsOf(vm, cellLibrary = { key, scene -> librarySaid = key to scene }),
                )
            }
        }
    }

    private fun reopened(vm: RhythmViewModel): Project {
        kotlinx.coroutines.runBlocking { vm.flushNow() }
        return RhythmViewModel(app).project.value
    }

    @Test
    fun anEmptyCellAsksWhatGoesInItAndAFullOneStillLaunches() {
        val vm = open()
        show(vm)
        compose.onNodeWithTag("live_pad_${SongPart.DRUMS}_1").performClick()
        compose.onNodeWithTag("live_cell_menu").assertIsDisplayed()
        compose.onNodeWithTag("live_cell_record").assertIsDisplayed()
        compose.onNodeWithTag("live_cell_library").assertIsDisplayed()
        compose.onNodeWithTag("live_cell_copy").assertIsDisplayed()
        compose.onNodeWithTag("live_cell_cancel").performClick()
        compose.onNodeWithTag("live_cell_menu").assertDoesNotExist()
        assertNull(vm.liveGrid.value?.takeIf { it.hasPending })
        assertFalse(vm.project.value.cellHasContent(SongPart.DRUMS, 1))
        // « Un son de ma bibliothèque »: the browser for that cell.
        compose.onNodeWithTag("live_pad_${SongPart.inst(0)}_1").performClick()
        compose.onNodeWithTag("live_cell_library").performClick()
        assertEquals(SongPart.inst(0) to 1, librarySaid)
    }

    @Test
    fun playedNotesBecomeTheCellsLoopOnWholeBarsUndoneInOneStepAndKept() {
        val vm = open()
        show(vm)
        vm.playedClockForTest = 20.0
        compose.onNodeWithTag("live_pad_${SongPart.inst(0)}_1").performClick()
        compose.onNodeWithTag("live_cell_record").performClick()
        compose.waitForIdle()
        assertEquals(CellRecording(SongPart.inst(0), 1), vm.cellRecording.value)
        compose.onNodeWithTag("live_cell_rec_bar").assertIsDisplayed()
        // Played from bar 3 (step 32), the first note a hair early; touched again a little before bar 5.
        vm.playedClockForTest = 31.8; vm.liveNoteOn(0, 60, 0.8f)
        vm.playedClockForTest = 33.8; vm.liveNoteOff(0, 60)
        vm.playedClockForTest = 40.0; vm.liveNoteOn(0, 67, 0.8f)
        vm.playedClockForTest = 42.0; vm.liveNoteOff(0, 67)
        vm.playedClockForTest = 62.5
        compose.onNodeWithTag("live_pad_${SongPart.inst(0)}_1").performClick()
        compose.waitForIdle()
        assertNull(vm.cellRecording.value)
        val clip = vm.project.value.allScenes[1].synthNotes
        assertEquals(32, clip.steps)  // two whole bars
        assertEquals(listOf(0 to 60, 8 * NoteClip.TICKS_PER_STEP to 67), clip.notes.map { it.start to it.pitch })
        // The scene A notes are untouched; Annuler takes the whole recording back at once.
        assertEquals(2, vm.project.value.allScenes[0].synthNotes.notes.size)
        vm.undo()
        assertFalse(vm.project.value.cellHasContent(SongPart.inst(0), 1))
        vm.redo()
        assertEquals(clip.notes, reopened(vm).allScenes[1].synthNotes.notes)
    }

    @Test
    fun drumPadsPlayedGoIntoTheDrumCell() {
        val vm = open()
        show(vm)
        vm.playedClockForTest = 3.0
        compose.onNodeWithTag("live_pad_${SongPart.DRUMS}_1").performClick()
        compose.onNodeWithTag("live_cell_record").performClick()
        compose.waitForIdle()
        // The panel shows the drum pads to play on.
        compose.onNodeWithTag("live_drum_pads").assertIsDisplayed()
        vm.playedClockForTest = 16.1
        compose.onNodeWithTag("live_drum_pad_2").performTouchInput { down(center); up() }
        vm.playedClockForTest = 20.0
        compose.onNodeWithTag("live_drum_pad_1").performTouchInput { down(center); up() }
        vm.playedClockForTest = 32.0
        vm.stopCellRecording()
        val d = vm.project.value.allScenes[1].drums
        assertEquals(16, d.steps)
        assertEquals(setOf(0), d.lanes[2].hits.keys)
        assertEquals(setOf(4), d.lanes[1].hits.keys)
        vm.undo()
        assertFalse(vm.project.value.cellHasContent(SongPart.DRUMS, 1))
    }

    @Test
    fun aCellCopiedFromAnotherUndoneInOneStepAndKept() {
        val vm = open()
        show(vm)
        compose.onNodeWithTag("live_pad_${SongPart.DRUMS}_1").performClick()
        compose.onNodeWithTag("live_cell_copy").performClick()
        compose.onNodeWithTag("live_cell_copy_bar").assertIsDisplayed()
        // The cell to copy: A's drums (a touch that copies, it does not launch).
        compose.onNodeWithTag("live_pad_${SongPart.DRUMS}_0").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("live_cell_copy_bar").assertDoesNotExist()
        assertEquals(vm.project.value.allScenes[0].drums.lanes[0].hits, vm.project.value.allScenes[1].drums.lanes[0].hits)
        assertTrue(vm.project.value.cellHasContent(SongPart.DRUMS, 1))
        vm.undo()
        assertFalse(vm.project.value.cellHasContent(SongPart.DRUMS, 1))
        vm.redo()
        assertTrue(reopened(vm).cellHasContent(SongPart.DRUMS, 1))
        // Not between kinds: nothing happens (the app says why).
        assertFalse(vm.copyCell(SongPart.ACID, 1, SongPart.inst(0), 1))
        assertTrue(vm.notice.value != null)
    }

    @Test
    fun aLibrarySoundGoesIntoThatBrickOnAnAudioTrackUndoneInOneStepAndKept() {
        com.pocketstudio.engine.NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        val vm = open()
        // Two seconds of a tone at 120 BPM: a bar, a loop.
        val pcm = Pcm(FloatArray(96_000 * 2) { i -> (0.3 * sin(2 * PI * 220 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)
        assertTrue(vm.placeInCell(SongPart.DRUMS, 1, pcm, "Ma boucle"))
        until("the sound never reached the cell") { vm.project.value.audioTracks.any { it.clip != null } }
        val t = vm.project.value.audioTracks.single()
        assertEquals(1, t.clip!!.scene)
        assertEquals(vm.project.value.stepsPerBar * (com.pocketstudio.model.LiveCells.loopBars(2.0, vm.project.value.tempoBpm, 4) ?: 1), t.clip!!.loopSteps)
        assertTrue(vm.project.value.cellHasContent(SongPart.audio(t.id), 1))
        assertFalse(vm.project.value.cellHasContent(SongPart.audio(t.id), 0))
        vm.undo()
        assertTrue(vm.project.value.audioTracks.none { it.clip != null })
        vm.redo()
        assertTrue(reopened(vm).cellHasContent(SongPart.audio(t.id), 1))
    }

    @org.junit.After
    fun end() {
        com.pocketstudio.engine.NativeEngine.sink = null
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }
}
