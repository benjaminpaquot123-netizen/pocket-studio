package com.pocketstudio.app.live

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.TempoActions
import com.pocketstudio.app.ui.TempoBanner
import com.pocketstudio.app.ui.TempoPad
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.TempoLead
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * Step 4e (Benjamin's list n° 16) through the ViewModel and the screen: the first loop with a tempo
 * sets the project's, a hit never does, « Annuler » gives the tempo before back in one step, the
 * tempo sheet offers « Revenir au tempo de … ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class AutoTempoJourneyTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    private fun open(p: Project): RhythmViewModel {
        com.pocketstudio.engine.NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        val id = "tempo-" + System.nanoTime()
        store.save(id, p.sanitized())
        CurrentProject.set(app, id)
        return RhythmViewModel(app)
    }

    /** [seconds] of a tone (stereo, 48 kHz). */
    private fun tone(seconds: Double) = Pcm(FloatArray((seconds * 48_000).toInt() * 2) { i -> (0.3 * sin(2 * PI * 220 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)

    @Test
    fun theFirstLoopOnALivePadSetsTheTempoAndAnnulerGivesItBack() {
        val vm = open(Project(name = "Vide"))
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        // Boys at 113: 8 beats, 4.25 s.
        assertTrue(vm.placeOnLivePad(0, tone(8 * 60.0 / 113), "Boys", known = 113.0))
        until("the loop never reached the pad") { vm.project.value.live.clips[0] != null }
        val p = vm.project.value
        assertEquals(113.0, p.tempoBpm, 0.0)
        assertEquals(TempoLead("Boys", 113.0), p.tempoLead)
        assertEquals(113.0, p.live.clips[0]!!.bpm, 0.0)
        assertEquals(8, p.live.clips[0]!!.beats)
        val banner = assertNotNullAnd(vm.tempoSet.value)
        assertEquals(120.0, banner.previous, 0.0)
        // « Annuler »: the tempo before, the loop kept (it follows the tempo, stretched).
        vm.undoTempoSet()
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        assertNotNull(vm.project.value.live.clips[0])
        assertNull(vm.tempoSet.value)
        // A second loop never sets it again.
        assertTrue(vm.placeOnLivePad(1, tone(4 * 60.0 / 90), "Autre", known = 90.0))
        until("the second loop never reached its pad") { vm.project.value.live.clips[1] != null }
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        assertNull(vm.tempoSet.value)
        // « Revenir au tempo de Boys »: its exact tempo.
        vm.backToLeadTempo()
        assertEquals(113.0, vm.project.value.tempoBpm, 0.0)
    }

    @Test
    fun aHitOrAShoutNeverSetsTheTempo() {
        val vm = open(Project(name = "Vide"))
        assertTrue(vm.placeOnLivePad(0, tone(2.0), "Go go go!", known = 0.0, oneShot = true))
        until("the shout never reached the pad") { vm.project.value.live.clips[0] != null }
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        assertNull(vm.project.value.tempoLead)
        assertNull(vm.tempoSet.value)
    }

    @Test
    fun aLoopInACellOfAProjectWithMusicKeepsItsTempoAndFollowsIt() {
        val vm = open(Project(name = "Beat", drums = DrumPattern().toggle(0, 0)))
        assertTrue(vm.placeInCell(SongPart.DRUMS, 1, tone(8 * 60.0 / 113), "Boys", known = 113.0))
        until("the loop never reached the cell") { vm.project.value.audioTracks.any { it.clip != null } }
        val clip = vm.project.value.audioTracks.single().clip!!
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        assertNull(vm.tempoSet.value)
        // Kept as it is, at its own tempo: stretched to the project's while it plays.
        assertEquals(113.0, clip.bpm, 0.0)
        assertTrue(clip.follows)
        assertEquals(2 * vm.project.value.stepsPerBar, clip.loopSteps)
    }

    @Test
    fun theFirstLoopInACellOfAnEmptyProjectSetsTheTempoInTheSameUndoStep() {
        val vm = open(Project(name = "Vide"))
        assertTrue(vm.placeInCell(SongPart.DRUMS, 0, tone(8 * 60.0 / 113), "Boys", known = 113.0))
        until("the loop never reached the cell") { vm.project.value.audioTracks.any { it.clip != null } }
        assertEquals(113.0, vm.project.value.tempoBpm, 0.0)
        assertNotNull(vm.tempoSet.value)
        // The general « Annuler » takes the sound and its tempo back together.
        vm.undo()
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        assertTrue(vm.project.value.audioTracks.none { it.clip != null })
    }

    @Test
    fun theBannerSaysWhoSetTheTempoAndAnnulerUndoesIt() {
        var undone = 0
        compose.setContent {
            PocketStudioTheme { TempoBanner(RhythmViewModel.TempoSet("Boys", 113.2, 120.0), undo = { undone++ }, dismiss = {}) }
        }
        compose.onNodeWithText("Tempo calé sur Boys : 113").assertIsDisplayed()
        compose.onNodeWithTag("tempo_banner_undo").performClick()
        assertEquals(1, undone)
    }

    @Test
    fun theTempoSheetOffersTheLeadsTempoOnlyWhileTheTempoIsElsewhere() {
        var back = 0
        var tempo by androidx.compose.runtime.mutableStateOf(120.0)
        compose.setContent {
            PocketStudioTheme { TempoPad(tempo, TempoActions(lead = TempoLead("Boys", 113.0), backToLead = { back++; tempo = 113.0 }), close = {}) }
        }
        compose.onNodeWithText("Revenir au tempo de Boys (113)").assertIsDisplayed()
        compose.onNodeWithTag("tempo_back_to_lead").performClick()
        compose.waitForIdle()
        assertEquals(1, back)
        compose.onNodeWithTag("tempo_back_to_lead").assertDoesNotExist()
    }

    private fun <T : Any> assertNotNullAnd(v: T?): T {
        assertNotNull(v)
        return v!!
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
