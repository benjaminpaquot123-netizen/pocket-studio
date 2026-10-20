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

class HarmonyTest {
    private val cMajor = Harmony(0, Scale.Major)
    private val aMinor = Harmony(9, Scale.Minor)

    @Test
    fun snapGoesToTheNearestScaleNote() {
        assertEquals(60, cMajor.snap(61))  // C# -> C (tie goes down)
        assertEquals(65, cMajor.snap(66))  // F# -> F
        assertEquals(61, Harmony(0, Scale.Off).snap(61))
        assertTrue(aMinor.inScale(57) && !aMinor.inScale(58))
    }

    @Test
    fun chordsFollowTheScale() {
        assertEquals(listOf(60, 64, 67), cMajor.chord(60))       // C major
        assertEquals(listOf(62, 65, 69), cMajor.chord(62))       // D minor
        assertEquals(listOf(71, 74, 77), cMajor.chord(71))       // B diminished
        assertEquals(listOf(67, 71, 74, 77), cMajor.chord(67, 4))  // G7
        assertEquals(listOf(57, 60, 64), aMinor.chord(57))       // A minor
        assertEquals(listOf(60, 64, 67), Harmony().chord(60))    // no scale: major triad
    }

    @Test
    fun progressionFillsFourBars() {
        val clip = NoteClip().withProgression(Progression.Pop, cMajor)
        assertEquals(64, clip.steps)
        assertEquals(12, clip.notes.size)
        // I, V, vi, IV in C from C3.
        assertEquals(listOf(48, 55, 57, 53), clip.notes.groupBy { it.start }.toSortedMap().values.map { c -> c.minOf { it.pitch } })
    }

    @Test
    fun arpeggioAndStrumExpandChords() {
        val chord = NoteClip(notes = cMajor.chord(60).map { Note(0, 96, it) })
        val up = Performance(ArpMode.Up, arpRate = 24).expand(chord)
        assertEquals(listOf(60, 64, 67, 60), up.map { it.pitch })
        assertEquals(listOf(0, 24, 48, 72), up.map { it.start })
        val updown = Performance(ArpMode.UpDown, arpRate = 12).expand(chord)
        assertEquals(listOf(60, 64, 67, 64, 60, 64, 67, 64), updown.map { it.pitch })
        val strum = Performance(strum = 6).expand(chord)
        assertEquals(listOf(0, 6, 12), strum.map { it.start })
        assertEquals(listOf(96, 90, 84), strum.map { it.length })  // all end together
        assertEquals(chord.notes, Performance().expand(chord))
    }
}

class AcidPagesTest {
    @Test
    fun longerLinesAndPageCopy() {
        var a = AcidPattern(length = 32).withStep(3, AcidStep(note = 5, gate = true, accent = true))
        a = a.copy(locked = setOf(3)).copyPage(0, 1)
        assertEquals(a.steps[3], a.steps[19])
        assertTrue(19 in a.locked && 3 in a.locked)
        assertEquals(128, AcidPattern(length = 500).sanitized().length)
        // Older projects stored 64 steps: they load with room for 8 bars.
        val old = ProjectJson.decode("""{"acid":{"length":32,"steps":[]}}""").acid
        assertEquals(AcidPattern.MAX_STEPS, old.steps.size)
        assertEquals(32, old.length)
    }

    @Test
    fun remixPlacesTheSongGridAndSurvivesBadValues() {
        val remix = Remix(setId = "abc", name = "Chanson", songBpm = 120.0, firstBeat = 0.25, startBar = 2)
            .withStem(3) { it.copy(mute = true) }
        assertEquals(96_000L, remix.barFrames(48_000))
        assertEquals(12_000L + 2 * 96_000L, remix.startFrame(48_000))
        val project = Project(remix = remix)
        assertEquals(project, ProjectJson.decode(ProjectJson.encode(project)))
        val bad = Remix(songBpm = Double.NaN, firstBeat = -3.0, startBar = -1, stems = listOf(StemMix(volume = 7f))).sanitized()
        assertEquals(0.0, bad.songBpm, 0.0)
        assertEquals(0L, bad.barFrames(48_000))
        assertEquals(0, bad.startBar)
        assertEquals(Remix.STEMS, bad.stems.size)
        assertEquals(1f, bad.stems[0].volume, 0f)
        assertEquals(null, ProjectJson.decode("""{"name":"Vieux"}""").remix.setId)
    }

    @Test
    fun effectsSurviveSavingAndBadValues() {
        val ch = Channel(inserts = listOf(Insert.of(EffectType.COMPRESSOR), Insert(), Insert.of(EffectType.EQ).copy(params = listOf(1f, 0.5f, 0.5f, 0f)), Insert()), sends = listOf(0.3f, 0f))
        val p = Project(mixer = Mixer(channels = List(Mixer.CHANNELS) { if (it == 0) ch else Channel() }, master = List(4) { Insert.of(EffectType.SATURATION) }))
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
        val bad = Mixer(
            channels = listOf(Channel(inserts = listOf(Insert(type = 42, params = listOf(Float.NaN))), sends = listOf(9f))),
            master = emptyList(),
            buses = listOf(listOf(2f)),
        ).sanitized()
        assertEquals(EffectType.NONE, bad.channels[0].inserts[0].effect)
        assertEquals(4, bad.channels[0].inserts.size)
        assertEquals(4, bad.channels[0].inserts[0].params.size)
        assertEquals(1f, bad.channels[0].sends[0], 0f)
        assertEquals(4, bad.master.size)
        assertEquals(listOf(1f, 0.5f, 1f, 0.1f), bad.buses[0])
        assertEquals(Buses.DEFAULTS[1], bad.buses[1])
        // Older files without effects load with empty slots.
        val old = ProjectJson.decode("""{"mixer":{"channels":[{"volume":0.5}]}}""").mixer
        assertEquals(EffectType.NONE, old.channels[0].inserts[3].effect)
        assertEquals(Buses.DEFAULTS, old.buses)
        assertEquals("+12.0 dB".replace('.', ','), EffectType.EQ.params[0].format(1f).replace('.', ','))
    }

    @Test
    fun automationLanesAreKeptTidy() {
        val vol = AutoTarget(AutoTarget.VOLUME, ch = 2)
        val lane = AutomationLane(vol).withPoint(4, 0f).withPoint(8, 0.8f).withPoint(99, 1f)
        assertEquals(0f, lane.values[4])
        assertEquals(null, lane.values[5])
        assertEquals((0 shl 16) or (2 shl 12), vol.pack())
        val p = Project(automation = listOf(lane))
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
        val messy = Project(
            automation = List(12) { AutomationLane(AutoTarget(AutoTarget.SEND, ch = it % 4, slot = it % 2), listOf(Float.NaN, 3f)) } +
                AutomationLane(AutoTarget(kind = 9)),
        ).sanitized().automation
        assertEquals(AutomationLane.MAX_LANES, messy.size.coerceAtLeast(AutomationLane.MAX_LANES).coerceAtMost(8))
        assertEquals(messy.size, messy.map { it.target }.toSet().size)  // no duplicate targets
        assertEquals(null, messy[0].values[0])
        assertEquals(1f, messy[0].values[1])
        assertEquals(AutomationLane.STEPS, messy[0].values.size)
    }

    @Test
    fun scenesKeepTheirOwnPatterns() {
        val a = Project(drums = DrumPattern().toggle(0, 0)).sanitized()
        assertEquals(0, a.scenes.size)  // one scene: nothing extra stored
        assertEquals(1, a.allScenes.size)
        // Copy A to B, switch to B and change it: A keeps its kick only.
        val b = a.copySceneTo(1).switchScene(1).let { it.copy(drums = it.drums.toggle(2, 4)) }.sanitized()
        assertEquals(1, b.scene)
        assertEquals(2, b.scenes.size)
        assertEquals(null, b.scenes[0].drums.lanes[2].hits[4])
        assertTrue(b.scenes[1].drums.lanes[2].hits[4] != null)
        val backToA = b.switchScene(0)
        assertEquals(null, backToA.drums.lanes[2].hits[4])
        assertTrue(backToA.drums.lanes[0].hits[0] != null)
        // The 303's sound is shared; switching to a new empty scene keeps it and clears the notes.
        val loud = backToA.copy(acid = backToA.acid.copy(sound = backToA.acid.sound.copy(cutoff = 0.9f)))
        val c = loud.switchScene(2)
        assertEquals(0.9f, c.acid.sound.cutoff, 0f)
        assertTrue(c.currentSceneContent().isEmpty)
        assertEquals(3, c.scenes.size)
        // Steps: the longest part, in whole bars.
        assertEquals(32, Scene(drums = DrumPattern(steps = 16), acid = AcidPattern(length = 20)).steps)
        // Song parts pointing at missing scenes are dropped; repeats are clamped.
        val song = c.copy(song = listOf(SongPart(0, 0), SongPart(7, 2), SongPart(1, 99))).sanitized().song
        assertEquals(listOf(SongPart(0, 1), SongPart(1, SongPart.MAX_REPEATS)), song)
        assertEquals(c.sanitized(), ProjectJson.decode(ProjectJson.encode(c.sanitized())))
    }
}
