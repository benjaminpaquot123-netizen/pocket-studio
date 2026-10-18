package com.pocketstudio.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectJsonTest {
    @Test
    fun roundTripKeepsEverything() {
        val project = Project(name = "Acid test", tempoBpm = 132.0, swing = 0.25)
            .copy(drums = DrumPattern().toggle(0, 0).toggle(0, 4, 0.5f).toggle(3, 15))
        val decoded = ProjectJson.decode(ProjectJson.encode(project))
        assertEquals(project, decoded)
    }

    @Test
    fun olderFileWithMissingFieldsGetsDefaults() {
        val decoded = ProjectJson.decode("""{"name":"Vieux","tempoBpm":90}""")
        assertEquals("Vieux", decoded.name)
        assertEquals(90.0, decoded.tempoBpm, 0.0)
        assertEquals(16, decoded.drums.lanes.size)
        assertEquals(16, decoded.mixer.channels.size)
    }

    @Test
    fun newerFileWithUnknownFieldsStillLoads() {
        val decoded = ProjectJson.decode("""{"name":"Futur","tempoBpm":128,"stems":[1,2,3],"drums":{"steps":16,"groove":"x"}}""")
        assertEquals("Futur", decoded.name)
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        val decoded = ProjectJson.decode("""{"tempoBpm":999,"swing":-1,"drums":{"steps":7,"lanes":[{"hits":{"3":{},"40":{}}}]}}""")
        assertEquals(Project.MAX_TEMPO, decoded.tempoBpm, 0.0)
        assertEquals(0.0, decoded.swing, 0.0)
        assertEquals(DrumPattern.MIN_STEPS, decoded.drums.steps)
        assertEquals(DrumPattern.PAD_COUNT, decoded.drums.lanes.size)
        assertEquals(setOf(3), decoded.drums.lanes[0].hits.keys)
    }
}

class ProjectStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun store() = ProjectStore(tmp.root)

    @Test
    fun saveThenLoad() {
        val store = store()
        val project = Project(name = "A", tempoBpm = 100.0)
        store.save("a", project)
        assertEquals(project, store.load("a"))
        assertEquals(listOf("a"), store.list())
        assertNull(store.load("missing"))
    }

    @Test
    fun secondSaveKeepsPreviousVersionAsBackup() {
        val store = store()
        store.save("a", Project(name = "v1"))
        store.save("a", Project(name = "v2"))
        val backup = File(store.folder("a"), ProjectStore.BACKUP).readText()
        assertEquals("v1", ProjectJson.decode(backup).name)
        assertFalse("no temp file left", File(store.folder("a"), ProjectStore.TMP).exists())
    }

    @Test
    fun corruptedMainFileFallsBackToBackup() {
        val store = store()
        store.save("a", Project(name = "v1"))
        store.save("a", Project(name = "v2"))
        // Simulates a broken main file (e.g. disk error): the backup saves the day.
        File(store.folder("a"), ProjectStore.MAIN).writeText("{\"name\": \"v3\", trunc")
        assertEquals("v1", store.load("a")!!.name)
    }

    @Test
    fun crashDuringWriteLeavesPreviousVersionIntact() {
        val store = store()
        store.save("a", Project(name = "v1"))
        // Simulates the app being killed while writing: only a partial temp file exists.
        File(store.folder("a"), ProjectStore.TMP).writeText("{\"name\": \"v2\", \"tem")
        assertEquals("v1", store.load("a")!!.name)
        // And the next save still works.
        store.save("a", Project(name = "v3"))
        assertEquals("v3", store.load("a")!!.name)
    }
}

class HistoryTest {
    @Test
    fun unlimitedUndoRedo() {
        val history = History(0)
        for (i in 1..10_000) history.push(i, nowMs = i * 10_000L)
        assertEquals(10_000, history.undoDepth)
        repeat(10_000) { history.undo() }
        assertEquals(0, history.current)
        assertFalse(history.canUndo)
        repeat(10_000) { history.redo() }
        assertEquals(10_000, history.current)
    }

    @Test
    fun sliderDragIsOneUndoStep() {
        val history = History(0)
        for (i in 1..100) history.push(i, coalesceKey = "tempo", nowMs = 1_000L + i * 16)
        assertEquals(1, history.undoDepth)
        assertEquals(0, history.undo())
    }

    @Test
    fun differentKeysOrPausesAreSeparateSteps() {
        val history = History(0)
        history.push(1, "tempo", nowMs = 0)
        history.push(2, "swing", nowMs = 10)
        history.push(3, "swing", nowMs = 5_000)
        assertEquals(3, history.undoDepth)
    }

    @Test
    fun newEditClearsRedoAndNoOpIsIgnored() {
        val history = History(0)
        history.push(1, nowMs = 0)
        history.undo()
        assertTrue(history.canRedo)
        assertFalse(history.push(0, nowMs = 1))
        assertTrue("no-op keeps redo", history.canRedo)
        history.push(5, nowMs = 2)
        assertFalse(history.canRedo)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSessionTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun autosaveIsDebouncedAndFlushWritesImmediately() = runTest {
        val store = ProjectStore(tmp.root)
        val io = StandardTestDispatcher(testScheduler)
        val session = ProjectSession("p", Project(), store, this, io, autosaveDelayMs = 400, clock = { testScheduler.currentTime })

        for (bpm in 100..140) {
            session.edit("tempo") { it.copy(tempoBpm = bpm.toDouble()) }
            advanceTimeBy(16)
        }
        assertNull("nothing written while the slider moves", store.load("p"))
        advanceTimeBy(500)
        runCurrent()
        assertEquals(140.0, store.load("p")!!.tempoBpm, 0.0)

        session.edit { it.copy(name = "Nuit acide") }
        session.flush()
        assertEquals("Nuit acide", store.load("p")!!.name)
    }

    @Test
    fun undoIsSavedToo() = runTest {
        val store = ProjectStore(tmp.root)
        val io = StandardTestDispatcher(testScheduler)
        val session = ProjectSession("p", Project(), store, this, io, clock = { testScheduler.currentTime })
        session.edit { it.copy(drums = it.drums.toggle(0, 0)) }
        assertTrue(session.canUndo.value)
        session.undo()
        assertFalse(session.canUndo.value)
        assertTrue(session.canRedo.value)
        session.flush()
        assertTrue(store.load("p")!!.drums.lanes[0].hits.isEmpty())
    }

    @Test
    fun saveErrorIsReported() = runTest {
        val blocker = File(tmp.root, "blocked").apply { writeText("not a folder") }
        val store = ProjectStore(blocker) // writing under a regular file fails
        val io = StandardTestDispatcher(testScheduler)
        val session = ProjectSession("p", Project(), store, this, io, clock = { testScheduler.currentTime })
        session.edit { it.copy(name = "x") }
        session.flush()
        assertNotNull(session.lastSaveError.value)
    }
}

class TapTempoTest {
    @Test
    fun steadyTapsGiveTheTempo() {
        val tap = TapTempo()
        assertNull(tap.tap(0))
        assertEquals(120.0, tap.tap(500)!!, 0.01)
        tap.tap(1000)
        assertEquals(120.0, tap.tap(1500)!!, 0.01)
    }

    @Test
    fun longPauseStartsOverAndRangeIsClamped() {
        val tap = TapTempo()
        tap.tap(0); tap.tap(500)
        assertNull("pause resets", tap.tap(10_000))
        assertEquals(Project.MAX_TEMPO, tap.tap(10_050)!!, 0.0)
    }
}
