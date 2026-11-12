package com.pocketstudio.app.live

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveCells
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.TempoLead
import com.pocketstudio.model.liveRowsShown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
 * Step 4f (Benjamin's n° 11): each new Live action is one « Annuler » step, comes back with
 * « Rétablir » and is still there once the project is saved, closed and opened again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveConnectedJourneyTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    private fun open(p: Project): RhythmViewModel {
        com.pocketstudio.engine.NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        val id = "connected-" + System.nanoTime()
        store.save(id, p.sanitized())
        CurrentProject.set(app, id)
        return RhythmViewModel(app)
    }

    private fun reopened(vm: RhythmViewModel): Project {
        kotlinx.coroutines.runBlocking { vm.flushNow() }
        return RhythmViewModel(app).project.value
    }

    private fun tone(seconds: Double) = Pcm(FloatArray((seconds * 48_000).toInt() * 2) { i -> (0.3 * sin(2 * PI * 220 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)

    @Test
    fun theCasesStepperIsOneStepOfItsOwnAndKept() {
        val vm = open(Project(name = "Cases", drums = DrumPattern().toggle(0, 0)))
        assertEquals(LiveCells.DEFAULT_ROWS, vm.project.value.liveRowsShown)
        vm.setLiveRows(12)
        assertEquals(12, vm.project.value.liveRowsShown)
        vm.undo()
        assertEquals(LiveCells.DEFAULT_ROWS, vm.project.value.liveRowsShown)
        vm.redo()
        assertEquals(12, vm.project.value.liveRowsShown)
        // An « Annuler » of something done before never takes the stepper back with it.
        vm.toggleHit(1, 4)
        vm.setLiveRows(16)
        vm.undo()
        assertEquals(12, vm.project.value.liveRowsShown)
        assertEquals(16, run { vm.redo(); vm.project.value.liveRowsShown })
        assertEquals(16, reopened(vm).liveRowsShown)
    }

    @Test
    fun aLivePadsSoundAndTheTempoItSetAreOneStepAndKeptWithTheirSource() {
        val vm = open(Project(name = "Vide"))
        assertTrue(vm.placeOnLivePad(0, tone(8 * 60.0 / 113), "Boys", known = 113.0))
        until("the loop never reached the pad") { vm.project.value.live.clips[0] != null }
        vm.undo()
        assertNull(vm.project.value.live.clips[0])
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        assertNull(vm.project.value.tempoLead)
        vm.redo()
        val back = reopened(vm)
        assertEquals("Boys", back.live.clips[0]!!.name)
        assertEquals(113.0, back.live.clips[0]!!.bpm, 0.0)
        assertEquals(113.0, back.tempoBpm, 0.0)
        assertEquals(TempoLead("Boys", 113.0), back.tempoLead)
    }

    @Test
    fun aStemBoardPieceOnALivePadFollowsTheTempo() {
        val vm = open(Project(name = "Remix", drums = DrumPattern().toggle(0, 0)))
        // Four beats of the song, already fitted to the project's 120 BPM by the stem board.
        kotlinx.coroutines.runBlocking { vm.stemBoardHost.putOnLivePad(tone(2.0), "Voix 1", 3, 120.0) }
        val clip = vm.project.value.live.clips[3]!!
        assertEquals(120.0, clip.bpm, 0.0)
        assertTrue(clip.follows)
        // A short cut (a shout, a hit) stays as it is.
        kotlinx.coroutines.runBlocking { vm.stemBoardHost.putOnLivePad(tone(0.4), "Cri", 4, 120.0) }
        assertTrue(!vm.project.value.live.clips[4]!!.follows)
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
