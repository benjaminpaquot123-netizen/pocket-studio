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

class AcidTest {
    @Test
    fun packMatchesEngineLayout() {
        assertEquals(0x110, AcidStep(gate = true).pack())  // octave 0 is stored as 1
        assertEquals(7 or (3 shl 4) or (1 shl 6) or (1 shl 7) or (1 shl 8), AcidStep(7, 2, accent = true, slide = true, gate = true).pack())
        assertEquals(1 shl 4, AcidStep(octave = 0).pack())
    }

    @Test
    fun generatorUsesTheNoteSetAndAnchorsTheOne() {
        val gen = AcidGenerator(kotlin.random.Random(1))
        repeat(50) {
            val set = listOf(0, 7, 12, 15)
            val p = gen.generate(AcidPattern(), set)
            assertTrue(p.steps[0].gate)
            p.steps.take(16).filter { it.gate }.forEach { s ->
                assertTrue("note ${s.note} oct ${s.octave}", (s.octave * 12 + s.note) in set)
            }
            assertTrue("steps past the length untouched", p.steps.drop(16).none { it.gate })
        }
    }

    @Test
    fun densityAndLocksAreRespected() {
        val gen = AcidGenerator(kotlin.random.Random(7))
        fun gates(d: Float) = (0 until 200).sumOf { gen.generate(AcidPattern(density = d)).steps.take(16).count { it.gate } }
        assertTrue(gates(1.0f) > gates(0.3f) * 2)

        val mine = AcidStep(note = 5, octave = 1, accent = true, gate = true)
        val locked = AcidPattern(locked = setOf(3)).withStep(3, mine)
        repeat(20) { assertEquals(mine, gen.generate(locked).steps[3]) }
    }

    @Test
    fun acidSurvivesSaveAndOldFilesGetDefaults() {
        val p = Project(acid = AcidPattern(root = 40).withStep(2, AcidStep(note = 3, gate = true)))
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
        assertEquals(AcidPattern().sanitized(), ProjectJson.decode("""{"name":"Vieux"}""").acid)
    }
}

class SynthPatchTest {
    @Test
    fun shortOrLongListsAreRepaired() {
        val p = ProjectJson.decode("""{"synth":{"values":[0.9, 2.0]}}""").synth
        assertEquals(SynthPatch.PARAM_COUNT, p.values.size)
        assertEquals(0.9f, p.get(0), 0f)
        assertEquals(1f, p.get(1), 0f)
        assertEquals(SynthPatch.DEFAULTS[5], p.get(5), 0f)
    }

    @Test
    fun presetsAreCompleteAndDistinct() {
        assertEquals(4, SynthPatch.PRESETS.size)
        SynthPatch.PRESETS.forEach { (_, patch) -> assertEquals(SynthPatch.PARAM_COUNT, patch.values.size) }
        assertEquals(4, SynthPatch.PRESETS.map { it.second }.toSet().size)
    }
}

class MixerModelTest {
    @Test
    fun brokenMixerIsRepaired() {
        val broken = Project(mixer = Mixer(listOf(Channel(volume = 7f, pan = -3f))))
        val fixed = broken.sanitized().mixer
        assertEquals(Mixer.CHANNELS, fixed.channels.size)
        assertEquals(1f, fixed.channels[0].volume)
        assertEquals(-1f, fixed.channels[0].pan)
        assertEquals(Channel(), fixed.channels[2])
    }
}

class PadSampleModelTest {
    @Test
    fun padSampleRoundTripsAndUnsafeNamesAreDropped() {
        val drums = DrumPattern().withSample(2, PadSample("a1b2.wav", "Ma voix"))
            .withSample(3, PadSample("../../etc/passwd", "piège"))
        val back = ProjectJson.decode(ProjectJson.encode(Project(drums = drums)))
        assertEquals(PadSample("a1b2.wav", "Ma voix"), back.drums.lanes[2].sample)
        assertNull(back.drums.lanes[3].sample)
        assertNull(back.drums.lanes[0].sample)
    }
}

class PadSampleParamsTest {
    @Test
    fun paramsAreRepairedOnLoad() {
        val bad = PadSample("a.wav", "x", start = 0.9f, end = 0.2f, pitch = 99f, sustain = Float.NaN, choke = 9, gain = -1f)
        val ok = Project(drums = DrumPattern().withSample(0, bad)).sanitized().drums.lanes[0].sample!!
        assertTrue(ok.start < ok.end)
        assertEquals(24f, ok.pitch)
        assertEquals(1f, ok.sustain)
        assertEquals(4, ok.choke)
        assertEquals(0f, ok.gain)
        assertEquals(11, ok.engineParams().size)
    }

    @Test
    fun olderFilesWithoutParamsGetDefaults() {
        val json = """{"drums":{"lanes":[{"hits":{},"sample":{"file":"a.wav","name":"x"}}]}}"""
        val s = ProjectJson.decode(json).drums.lanes[0].sample!!
        assertEquals(PadSample("a.wav", "x"), s)
    }
}

class FitToTempoTest {
    @Test
    fun suggestionIsTheClosestBeatCount() {
        assertEquals(4, PadSample.suggestBeats(seconds = 2.1, bpm = 120.0))   // 4.2 beats
        assertEquals(8, PadSample.suggestBeats(seconds = 3.6, bpm = 120.0))   // 7.2 beats
        assertEquals(1, PadSample.suggestBeats(seconds = 0.1, bpm = 90.0))
    }

    @Test
    fun fittedLengthFollowsTheTempoAndBadValuesAreDropped() {
        val s = PadSample("a.wav", "x", fitBeats = 4)
        assertEquals(96_000L, s.fittedFrames(120.0))
        assertEquals(0, PadSample("a.wav", "x", fitBeats = 3).sanitized().fitBeats)
        val neutral = s.copy(start = 0.2f, end = 0.6f, pitch = 7f).engineParams(stretched = true)
        assertEquals(0f, neutral[0]); assertEquals(1f, neutral[1]); assertEquals(0f, neutral[2])
    }
}

class NoteClipTest {
    @Test
    fun quantizeMovesTowardTheGridByTheStrength() {
        val clip = NoteClip(notes = listOf(Note(start = 30, length = 24, pitch = 60)))  // grid 24: target 24
        assertEquals(24, clip.quantized(24, 1f).notes[0].start)
        assertEquals(27, clip.quantized(24, 0.5f).notes[0].start)
        assertEquals(30, clip.quantized(24, 0f).notes[0].start)
        assertEquals(24, clip.quantized(24, 1f).notes[0].length)  // lengths kept
    }

    @Test
    fun badNotesAreDroppedOrRepaired() {
        val clip = NoteClip(
            steps = 20,
            notes = listOf(
                Note(start = -5, length = 10, pitch = 60),       // before the clip: dropped
                Note(start = 10, length = 99999, pitch = 300),   // repaired
                Note(start = 500, length = 10, pitch = 60, velocity = Float.NaN),  // dropped
            ),
        ).sanitized()
        assertEquals(16, clip.steps)
        assertEquals(1, clip.notes.size)
        assertEquals(NoteClip.MAX_TICKS, clip.notes[0].length)
        assertEquals(127, clip.notes[0].pitch)
    }

    @Test
    fun tapFindsTheNoteAndGhostsFollowTheAcidLine() {
        val n = Note(start = 48, length = 24, pitch = 64)
        val clip = NoteClip().add(n)
        assertEquals(n, clip.noteAt(60, 64))
        assertNull(clip.noteAt(72, 64))
        val acid = AcidPattern(length = 2, root = 36).let { a ->
            a.withStep(0, AcidStep(note = 3, octave = 1, gate = true)).withStep(1, AcidStep())
        }
        val ghosts = NoteClip.ghostsOf(acid, 4)
        assertEquals(listOf(0, 48), ghosts.map { it.start })
        assertEquals(36 + 3 + 12, ghosts[0].pitch)
    }

    @Test
    fun notesSurviveSaveAndLoad() {
        val p = Project(synthNotes = NoteClip(steps = 32, notes = listOf(Note(12, 36, 62, 0.5f))))
        assertEquals(p.synthNotes, ProjectJson.decode(ProjectJson.encode(p)).synthNotes)
    }
}
