package com.pocketstudio.model.audit3

import com.pocketstudio.model.AutoTempo
import com.pocketstudio.model.History
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectSession
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.StemMix
import com.pocketstudio.model.TapTempo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Audit stage 3, « mutations »: PIT (1.17, run by hand on project-model, docs/lots/AUDIT-3.md) on
 * the save, the undo and the tempo left these changes of the code unnoticed by the tests; each test
 * here fails on its mutant (an edge moved by one, a call dropped, a value returned wrong).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MutationKillersTest {
    @get:Rule val tmp = TemporaryFolder()

    // --- History (undo) ---

    @Test
    fun aGestureRightAtTheEndOfTheWindowIsStillOneStep() {
        val h = History(0, coalesceWindowMs = 600)
        assertTrue(h.push(1, "drag", nowMs = 1000))
        assertTrue(h.push(2, "drag", nowMs = 1600))  // exactly the window: merged
        assertEquals(0, h.undo())
        assertFalse(h.canUndo)
    }

    @Test
    fun aGestureTakenBackSaysItChangedTheValue() {
        val h = History(0)
        h.push(1, "drag", nowMs = 0)
        assertTrue("back where it started is still a change", h.push(0, "drag", nowMs = 10))
        assertEquals(0, h.current)
        assertFalse(h.canUndo)
    }

    @Test
    fun aRedoEndsTheGestureItWasPartOf() {
        val h = History(0)
        h.push(1, "drag", nowMs = 0)
        h.undo()
        h.redo()
        h.push(2, "drag", nowMs = 1)  // the same key at once: a new step after a redo
        assertEquals(1, h.undo())
        assertEquals(0, h.undo())
    }

    @Test
    fun aTakeCommittedForgetsWhatCouldBeRedone() {
        val h = History(0)
        h.push(1)
        h.undo()
        assertTrue(h.canRedo)
        h.replace(5)
        h.commitFrom(0)
        assertFalse(h.canRedo)
        assertEquals(0, h.undo())
    }

    @Test
    fun undoAndRedoHandBackTheStepsTag() {
        val h = History(0)
        h.push(1, tag = "tempo")
        h.undo()
        assertEquals("tempo", h.lastTag)
        h.redo()
        assertEquals("tempo", h.lastTag)
    }

    @Test
    fun theSessionsUndoReturnsTheStepsTag() = runTest {
        val store = ProjectStore(tmp.newFolder())
        val s = ProjectSession("p", Project(), store, this, StandardTestDispatcher(testScheduler), clock = { testScheduler.currentTime })
        s.edit(tag = "nom") { it.copy(name = "Autre") }
        assertEquals("nom", s.undo())
        assertEquals("nom", s.redo())
    }

    // --- ProjectStore (save) ---

    @Test
    fun theCloudCopyIsMadeAgainFromTheFile() {
        val root = tmp.newFolder("projects")
        val backup = File(tmp.root, "backup")
        val store = ProjectStore(root, backup)
        store.save("p", Project(name = "Sauvé"))
        val copy = File(backup, "p${ProjectStore.MIRROR}")
        copy.delete()
        store.refreshMirror("p")
        assertEquals("Sauvé", ProjectJson.decode(GZIPInputStream(copy.inputStream()).bufferedReader().readText()).name)
        // Without a backup folder: nothing written, nothing thrown.
        ProjectStore(root).refreshMirror("p")
    }

    // --- Tempo ---

    @Test
    fun tapsExactlyAtTheResetDelayStillCount() {
        val t = TapTempo(resetAfterMs = 2000)
        assertNull(t.tap(0))
        assertEquals(Project.MIN_TEMPO, t.tap(2000)!!, 0.0)  // 30 BPM, held at the slowest
    }

    @Test
    fun theLastSixTapsAreAveraged() {
        val t = TapTempo(maxTaps = 6)
        var bpm: Double? = null
        for (at in listOf(0L, 500L, 1000L, 1500L, 2000L, 2600L)) bpm = t.tap(at)
        assertEquals(60_000.0 / (2600.0 / 5), bpm!!, 1e-9)
        bpm = t.tap(3100L)  // the first falls out
        assertEquals(60_000.0 / ((3100.0 - 500.0) / 5), bpm!!, 1e-9)
    }

    @Test
    fun aSoundsTempoAtItsEdges() {
        assertEquals("one second is already a loop", 120.0, AutoTempo.tempoOf(1.0, known = 120.0), 0.0)
        assertEquals(AutoTempo.MIN_BPM, AutoTempo.tempoOf(4.0, known = AutoTempo.MIN_BPM), 0.0)
        assertEquals(AutoTempo.MAX_BPM, AutoTempo.tempoOf(4.0, known = AutoTempo.MAX_BPM), 0.0)
        assertEquals(40.0, AutoTempo.tempoOf(6.0, detected = 40.0), 1e-9)
        assertEquals(300.0, AutoTempo.tempoOf(1.6, detected = 300.0), 1e-9)
        // 4.1 s detected at 118: 8 beats, so 117.07 (a whole number of beats).
        assertEquals(8 * 60.0 / 4.1, AutoTempo.tempoOf(4.1, detected = 118.0), 1e-9)
        assertEquals(0.0, AutoTempo.tempoOf(0.99, known = 120.0), 0.0)
    }

    @Test
    fun aSoundWithoutTempoLeadsNothing() {
        assertNull(AutoTempo.leadFor(Project(), "Boucle", 0.0))
        assertEquals(120.0, AutoTempo.leadFor(Project(), "Boucle", 120.0)!!.bpm, 0.0)
    }

    @Test
    fun aSongHeardAlongGivesTheProjectItsTempoOnlyWithItsTempoAndAStemHeard() {
        val song = Project(remix = Remix(setId = "s", songBpm = 0.0))
        assertFalse("no tempo known", AutoTempo.hasTempo(song))
        assertTrue(AutoTempo.hasTempo(song.copy(remix = song.remix.copy(songBpm = 120.0))))
        assertFalse("every stem muted", AutoTempo.hasTempo(song.copy(remix = song.remix.copy(songBpm = 120.0, stems = List(Remix.STEMS) { StemMix(mute = true) }))))
    }

    @Test
    fun fittingAtTheEdges() {
        assertEquals(1000L, AutoTempo.fittedFrames(1000L, 0.0, 120.0))
        assertEquals(1000L, AutoTempo.fittedFrames(1000L, 120.0, 0.0))
        assertEquals(500L, AutoTempo.fittedFrames(1000L, 60.0, 120.0))
        assertTrue("half the speed", AutoTempo.canFit(60.0, 120.0))
        assertTrue("twice the speed", AutoTempo.canFit(240.0, 120.0))
        assertFalse(AutoTempo.canFit(59.0, 120.0))
        assertFalse(AutoTempo.canFit(0.0, 120.0))
    }

    @Test
    fun hitsHaveNoTempo() {
        assertTrue(AutoTempo.isHit(true, null))
        assertTrue(AutoTempo.isHit(false, "kick"))
        assertFalse(AutoTempo.isHit(false, "loop"))
    }
}
