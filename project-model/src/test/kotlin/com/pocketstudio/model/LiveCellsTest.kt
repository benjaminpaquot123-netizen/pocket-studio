package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** An empty Live cell filled from the Live page: recorded, copied, a sound of the library (Benjamin's n° 9). */
class LiveCellsTest {
    /** A: a kick and two notes on the synth; B: only a 303 line (its drum and synth cells empty). */
    private fun twoScenes(): Project = Project(
        scenes = listOf(
            Scene("A", drums = DrumPattern().toggle(0, 0), synthNotes = NoteClip(notes = listOf(Note(0, 24, 60), Note(96, 24, 64)))),
            Scene("B", acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { AcidStep(gate = it % 4 == 0) })),
        ),
        scene = 0, drums = DrumPattern().toggle(0, 0), synthNotes = NoteClip(notes = listOf(Note(0, 24, 60), Note(96, 24, 64))),
    ).sanitized()

    private fun note(pitch: Int, at: Double, length: Double = 1.0, track: Int = 0) = PlayedMemory.KeptNote(track, pitch, 0.8f, at, length)

    @Test
    fun theLengthSnapsToTheNearestWholeBarWithNoMaximum() {
        assertEquals(1, LiveCells.bars(0.0, 3.0, 16))  // touched again at once: one bar
        assertEquals(2, LiveCells.bars(16.0, 16.0 + 2 * 16 + 5, 16))  // a hair late: two bars
        assertEquals(3, LiveCells.bars(0.0, 3 * 16 - 6.0, 16))  // a hair early: three bars
        assertEquals(3, LiveCells.bars(0.0, 3 * 12 + 1.0, 12))  // 3/4
        assertEquals(40, LiveCells.bars(0.0, 40 * 16.0, 16))  // as long as it lasts
        // A loop holds 16 bars of notes and, since step 7a, of drums: a longer recording keeps its last bars.
        assertEquals(16, LiveCells.keptBars(SongPart.inst(0), 40, 16))
        assertEquals(16, LiveCells.keptBars(SongPart.DRUMS, 40, 16))
        assertEquals(10, LiveCells.keptBars(SongPart.DRUMS, 10, 16))
    }

    @Test
    fun aDrumCellKeepsTenBarsPlayedAndTheEngineIsSentThemAll() {
        // Step 7a: before, a drum cell kept only its last 4 bars.
        val hits = listOf(PlayedMemory.KeptHit(0, 1f, 0.0), PlayedMemory.KeptHit(1, 0.7f, 150.0))
        val p = twoScenes().withRecordedCell(SongPart.DRUMS, 1, emptyList(), hits, 0.0, 10 * 16.0)!!
        val drums = p.allScenes[1].drums
        assertEquals(160, drums.steps)
        assertEquals(1f, drums.lanes[0].hits.getValue(0).velocity)
        assertEquals(0.7f, drums.lanes[1].hits.getValue(150).velocity)
        // A sync sends every step the loop plays, and never less than before (64).
        assertEquals(160, drums.engineReach)
        assertEquals(DrumPattern.SYNC_STEPS, DrumPattern().engineReach)
        assertEquals(200, DrumPattern().withLaneLength(3, 200).engineReach)
        // Saved and reopened as it is.
        assertEquals(drums, ProjectJson.decode(ProjectJson.encode(p)).allScenes[1].drums)
    }

    @Test
    fun notesPlayedGoIntoTheCellOnItsBarsAndLoop() {
        val p = twoScenes()
        assertFalse(p.cellHasContent(SongPart.inst(0), 1))
        // Recorded from bar 3 (step 32): a note a hair early, one on the 2nd beat, one past the end of bar 4.
        val notes = listOf(note(60, 31.8), note(62, 36.0, 2.0), note(64, 32.0 + 32.0), note(70, 40.0, track = 5))
        val out = p.withRecordedCell(SongPart.inst(0), 1, notes, emptyList(), origin = 32.0, end = 32.0 + 31.0)!!
        val clip = out.allScenes[1].synthNotes
        assertEquals(32, clip.steps)  // two bars
        assertEquals(listOf(0 to 60, 0 to 64, 96 to 62), clip.notes.map { it.start to it.pitch }.sortedWith(compareBy({ it.first }, { it.second })))
        assertEquals(48, clip.notes.first { it.pitch == 62 }.length)
        assertTrue(out.cellHasContent(SongPart.inst(0), 1))
        // The scene shown and its other cells did not move.
        assertEquals(p.allScenes[0], out.allScenes[0])
        assertEquals(0, out.scene)
        // Nothing of that track played: nothing to write.
        assertNull(p.withRecordedCell(SongPart.inst(0), 1, listOf(note(70, 33.0, track = 5)), emptyList(), 32.0, 48.0))
        // A 303 cell takes only the notes played on the 303 (step 7b), none here.
        assertNull(p.withRecordedCell(SongPart.ACID, 1, notes, emptyList(), 32.0, 48.0))
    }

    @Test
    fun notesPlayedOnThe303BecomeItsSteps() {
        // Step 7b: every kind of track records but the remix.
        assertTrue(LiveCells.canRecord(SongPart.ACID))
        assertTrue(LiveCells.canRecord(SongPart.audio(0)))
        assertFalse(LiveCells.canRecord(SongPart.REMIX))
        val p = twoScenes()
        val acid = { pitch: Int, at: Double, length: Double, velocity: Float -> PlayedMemory.KeptNote(LiveCells.ACID_TRACK, pitch, velocity, at, length) }
        // From bar 2 (step 16): the root a hair early, a fifth up held into the octave (a slide), the octave hard (an accent).
        val notes = listOf(acid(36, 15.9, 1.0, 0.6f), acid(43, 20.0, 4.2, 0.6f), acid(48, 24.0, 1.0, 0.95f), note(70, 22.0))
        val out = p.withRecordedCell(SongPart.ACID, 0, notes, emptyList(), 16.0, 32.0)!!.allScenes[0].acid
        assertEquals(16, out.length)
        assertEquals(AcidStep(note = 0, octave = 0, gate = true), out.steps[0])
        assertEquals(AcidStep(note = 7, octave = 0, slide = true, gate = true), out.steps[4])
        assertEquals(AcidStep(note = 0, octave = 1, accent = true, gate = true), out.steps[8])
        assertEquals(3, out.steps.count { it.gate })
        // Played far above the 303's range: brought down by octaves; 12 bars played: the last 8 kept (its longest loop).
        val high = LiveCells.acidSteps(listOf(acid(36 + 50, 0.0, 1.0, 0.5f)), 0.0, 16, 36)[0]
        assertEquals(36 + 50 - 24, 36 + high.note + 12 * high.octave)
        assertEquals(8, LiveCells.keptBars(SongPart.ACID, 12, 16))
    }

    @Test
    fun aMicrophoneTakeIsCutOnItsBars() {
        val key = SongPart.audio(0)
        // 120 BPM at 48 kHz: 6000 frames a sixteenth. Heard from step 10; the first sound at step 16.2 (bar 2, a hair late).
        val fps = 6000.0
        val cut = LiveCells.micCut(key, 10.0, (6.0 + 31.5) .let { (it * fps).toInt() }, (6.2 * fps).toInt(), fps, 16)!!
        assertEquals(2, cut.bars)  // 16.2 → 47.5: two bars, the nearest
        assertEquals((6 * fps).toInt(), cut.fromFrame)  // bar 2 starts 6 steps into the take
        assertEquals((32 * fps).toInt(), cut.frames)
        // Ended early: silence after; started late on its bar line: silence before.
        val data = FloatArray(10) { 1f }
        assertEquals(listOf(0f, 1f, 1f), LiveCells.cutMic(data, LiveCells.MicCut(-1, 3, 1)).toList())
        assertEquals(listOf(1f, 0f), LiveCells.cutMic(data, LiveCells.MicCut(9, 2, 1)).toList())
        // Nothing heard: nothing to cut.
        assertNull(LiveCells.micCut(key, 0.0, 1000, -1, fps, 16))
    }

    @Test
    fun padsPlayedGoIntoTheDrumCell() {
        val p = twoScenes()
        val hits = listOf(PlayedMemory.KeptHit(1, 0.9f, 16.0), PlayedMemory.KeptHit(2, 0.5f, 20.2), PlayedMemory.KeptHit(1, 0.9f, 16.0 + 64.0 + 3.0))
        val out = p.withRecordedCell(SongPart.DRUMS, 1, emptyList(), hits, origin = 16.0, end = 16.0 + 20 * 16.0)!!
        val d = out.allScenes[1].drums
        // Twenty bars played: the drum loop holds sixteen, the last sixteen (bars 5 to 20).
        assertEquals(256, d.steps)
        assertEquals(setOf(3), d.lanes[1].hits.keys)  // the one played in bar 5
        assertTrue(d.lanes[2].hits.isEmpty())  // played in bar 1: before the bars kept
        // A recording of the bars in time.
        val two = p.withRecordedCell(SongPart.DRUMS, 1, emptyList(), hits.take(2), origin = 16.0, end = 48.0)!!.allScenes[1].drums
        assertEquals(32, two.steps)
        assertEquals(setOf(0), two.lanes[1].hits.keys)
        assertEquals(setOf(4), two.lanes[2].hits.keys)
        assertEquals(0.5f, two.lanes[2].hits[4]!!.velocity, 1e-6f)
    }

    @Test
    fun aCellCopiesOntoACellOfTheSameKind() {
        val p = twoScenes().addTrack()!!
        val drums = p.withCopiedCell(SongPart.DRUMS, 0, SongPart.DRUMS, 1)!!
        assertEquals(p.allScenes[0].drums.lanes[0].hits, drums.allScenes[1].drums.lanes[0].hits)
        // Notes from the synth onto another instrument track, in another scene.
        val id = p.tracks.single().id
        val notes = p.withCopiedCell(SongPart.inst(0), 0, SongPart.inst(id), 1)!!
        assertEquals(p.allScenes[0].synthNotes.notes, notes.allScenes[1].trackNotes[id]!!.notes)
        assertTrue(notes.cellHasContent(SongPart.inst(id), 1))
        // Not between kinds, not from an empty cell, not onto itself.
        assertNull(p.withCopiedCell(SongPart.DRUMS, 0, SongPart.inst(0), 1))
        assertNull(p.withCopiedCell(SongPart.inst(0), 1, SongPart.inst(id), 0))
        assertNull(p.withCopiedCell(SongPart.DRUMS, 0, SongPart.DRUMS, 0))
        assertFalse(LiveCells.canCopy(SongPart.REMIX, SongPart.REMIX))
    }

    @Test
    fun aSoundGoesOnAnAudioTrackInThatBrickAndSurvivesTheFile() {
        val p = twoScenes().addAudioTrack()!!
        val key = SongPart.audio(p.audioTracks.single().id)
        val clip = AudioClip("s1.wav", "Boucle", seconds = 4.0, loopSteps = 32, bpm = 120.0)
        val out = p.withAudioInCell(key, 1, clip)!!
        assertTrue(out.cellHasContent(key, 1))
        assertFalse(out.cellHasContent(key, 0))
        assertEquals(1, out.audioTracks.single().clip!!.scene)
        // A drum cell: the sound on an audio track of its own (a new one: the first is taken), same brick.
        val again = out.withAudioInCell(SongPart.DRUMS, 1, clip.copy(file = "s2.wav"))!!
        assertEquals(2, again.audioTracks.size)
        assertTrue(again.cellHasContent(SongPart.audio(again.audioTracks.last().id), 1))
        // An audio cell copied onto another audio track with no clip.
        val copied = again.copy(audioTracks = again.audioTracks + AudioTrack(5, "Audio 6")).withCopiedCell(key, 1, SongPart.audio(5), 0)!!
        assertEquals("s1.wav", copied.audioTracks.first { it.id == 5 }.clip!!.file)
        assertEquals(0, copied.audioTracks.first { it.id == 5 }.clip!!.scene)
        // Saved and opened again: everything is there.
        val read = ProjectJson.decode(ProjectJson.encode(copied)).sanitized()
        assertTrue(read.cellHasContent(SongPart.audio(5), 0))
        assertTrue(read.cellHasContent(key, 1))
    }

    @Test
    fun aLibraryLoopIsFittedToWholeBarsAndAHitIsNot() {
        assertEquals(2, LiveCells.loopBars(4.1, 120.0, 4))  // 2.05 bars
        assertEquals(1, LiveCells.loopBars(1.7, 120.0, 4))
        assertEquals(4, LiveCells.loopBars(7.0, 120.0, 4))  // 3.5 bars: nearer 4 than 2
        assertNull(LiveCells.loopBars(0.4, 120.0, 4))
        assertNotNull(LiveCells.loopBars(1.5, 120.0, 3))
    }

    @Test
    fun theCasesStepperGoesThroughItsChoicesAndIsKept() {
        assertEquals(8, Project().liveRowsShown)
        assertEquals(12, LiveCells.stepRows(8, up = true))
        assertEquals(6, LiveCells.stepRows(8, up = false))
        assertEquals(16, LiveCells.stepRows(16, up = true))
        assertEquals(4, LiveCells.stepRows(4, up = false))
        val p = Project(liveRows = 12).sanitized()
        assertEquals(12, ProjectJson.decode(ProjectJson.encode(p)).sanitized().liveRowsShown)
        // Not one of the choices: back to 8; left out of the file at its default.
        assertEquals(8, Project(liveRows = 5).sanitized().liveRowsShown)
        assertFalse(ProjectJson.encode(Project().sanitized()).contains("liveRows"))
    }

    @Test
    fun aRowPastTheLastBrickIsANewBrickToFill() {
        val p = twoScenes()
        // Recorded into row D (index 3): bricks C and D added, empty but for that cell.
        val rec = p.withRecordedCell(SongPart.DRUMS, 3, emptyList(), listOf(PlayedMemory.KeptHit(0, 0.9f, 0.0)), 0.0, 16.0)!!
        assertEquals(4, rec.allScenes.size)
        assertTrue(rec.cellHasContent(SongPart.DRUMS, 3))
        assertFalse(rec.cellHasContent(SongPart.DRUMS, 2))
        assertEquals(0, rec.scene)
        // Nothing played: no brick added.
        assertNull(p.withRecordedCell(SongPart.DRUMS, 3, emptyList(), emptyList(), 0.0, 16.0))
        // Copied into a new row; a sound of the library too.
        val copied = p.withCopiedCell(SongPart.ACID, 1, SongPart.ACID, 5)!!
        assertEquals(6, copied.allScenes.size)
        assertTrue(copied.cellHasContent(SongPart.ACID, 5))
        val withAudio = p.addAudioTrack()!!
        val key = SongPart.audio(withAudio.audioTracks.single().id)
        val placed = withAudio.withAudioInCell(key, 2, AudioClip("s1.wav", "Boucle", seconds = 4.0, loopSteps = 32, bpm = 120.0))!!
        assertTrue(placed.cellHasContent(key, 2))
        // Past the last possible brick: nothing.
        assertNull(p.withCopiedCell(SongPart.ACID, 1, SongPart.ACID, Scene.MAX))
        // Saved and opened again.
        assertTrue(ProjectJson.decode(ProjectJson.encode(rec)).sanitized().cellHasContent(SongPart.DRUMS, 3))
    }
}
