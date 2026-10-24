package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The live grid's pure logic: slots, launches and what each pad holds. */
class LiveTest {
    @Test
    fun slotsAndKeysGoBothWays() {
        val keys = listOf(
            SongPart.DRUMS, SongPart.ACID, SongPart.inst(0), SongPart.inst(7), SongPart.audio(0), SongPart.audio(3), SongPart.REMIX,
            SongPart.audio(4), SongPart.audio(7),
        )
        val slots = keys.map { LiveSlots.of(it) }
        // Audio tracks 5..8 after the remix: the slots saved in takes before keep their meaning.
        assertEquals(listOf(0, 1, 2, 9, 10, 13, 14, 15, 18), slots)
        keys.forEach { assertEquals(it, LiveSlots.key(LiveSlots.of(it)!!)) }
        assertNull(LiveSlots.of("inst:8"))
        assertNull(LiveSlots.of("audio:8"))
        assertNull(LiveSlots.of("nope"))
        assertEquals((1 shl 19) - 1, LiveSlots.ALL)
    }

    /** A: drums of 16 steps; B: an acid line of 48 steps. */
    private fun songProject(): Project = Project(
        scenes = listOf(
            Scene("A", drums = DrumPattern().toggle(0, 0)),
            Scene("B", drums = DrumPattern().toggle(1, 0), acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { AcidStep(gate = it % 4 == 0) }, length = 48)),
        ),
        scene = 0, drums = DrumPattern().toggle(0, 0),
    ).sanitized()

    @Test
    fun theGridDuringTheSongIsWhatThePartPlays() {
        val p = songProject()
        // A part written by a take: row A, drums muted, the acid on B two bars into its line.
        val part = SongPart(0, 1, setOf(SongPart.DRUMS), play = mapOf(SongPart.ACID to 1), from = mapOf(SongPart.ACID to 2), bars = 4)
        // The engine swapped it in on step 64 (its slots: row tags, the acid started 32 steps earlier);
        // the app's 40 ms watch only saw it on step 67.
        val states = IntArray(LiveSlots.COUNT * 3) { i -> when (i % 3) { 0 -> LiveSlots.ROW_TAG; 1 -> LiveSlots.NO_LAUNCH; else -> 64 } }
        states[LiveSlots.ACID * 3 + 2] = 32
        val g = p.liveStateOf(part, part.scene, 67, states)
        assertEquals(0, g.row)
        assertEquals(LiveSlot(-1, null, 64), g.slot(LiveSlots.DRUMS))
        assertEquals(LiveSlot(1, null, 32), g.slot(LiveSlots.ACID))
        assertEquals(LiveSlot(0, null, 64), g.slot(LiveSlots.INST))
        assertTrue(g.diverged)
        // Without the engine (▶ from that part): from the start step less the bars carried on.
        assertEquals(LiveSlot(1, null, -32), p.liveStateOf(part, 0, 0).slot(LiveSlots.ACID))
        // A slot launched on its own since keeps its scene.
        states[LiveSlots.DRUMS * 3] = 1
        assertEquals(1, p.liveStateOf(part, 0, 67, states).slot(LiveSlots.DRUMS).playing)
        // ● armed during that part (the take starts on step 96): its first part is the part as heard,
        // the acid four bars into its three-bar line.
        val take = PerformanceTake(96, g)
        val written = p.writeTake(take, 128, append = false).project.song
        assertEquals(listOf(SongPart(0, 1, setOf(SongPart.DRUMS), play = mapOf(SongPart.ACID to 1), from = mapOf(SongPart.ACID to 1), bars = 2)), written)
        // The old way (every slot on the row from the step the watch saw) wrote drums never heard.
        val old = p.writeTake(PerformanceTake(96, LiveState.on(0, 67)), 128, append = false).project.song
        assertTrue(old.first().muted.isEmpty())
    }

    @Test
    fun theRemixColumnFollowsThePartMutes() {
        val p = songProject().copy(remix = Remix(setId = "set")).sanitized()
        val g = p.liveStateOf(SongPart(1, 1, setOf(SongPart.REMIX)), 1, 16)
        assertEquals(-1, g.slot(LiveSlots.REMIX).playing)
        assertEquals(1, g.slot(LiveSlots.ACID).playing)
        assertEquals(LiveState.on(1, 16), p.liveStateOf(null, 1, 16))
        // Kept in the song (the remix loaded), dropped when there is none.
        val kept = p.copy(song = listOf(SongPart(1, 1, setOf(SongPart.REMIX)))).sanitized().song.single()
        assertEquals(setOf(SongPart.REMIX), kept.muted)
        assertTrue(songProject().copy(song = listOf(SongPart(1, 1, setOf(SongPart.REMIX)))).sanitized().song.single().muted.isEmpty())
    }

    @Test
    fun aPadWaitsThenPlaysFromItsOwnStart() {
        var s = LiveState.on(0)
        assertFalse(s.diverged)
        s = s.armed(LiveSlots.mask(LiveSlots.DRUMS), 2)
        assertEquals(2, s.slot(LiveSlots.DRUMS).pending)
        assertTrue(s.hasPending)
        assertNull(s.pendingRow)
        assertEquals(0, s.slot(LiveSlots.DRUMS).playing)  // still the old part until the bar
        s = s.launched(LiveSlots.DRUMS, 2, 16)
        assertEquals(LiveSlot(2, null, 16), s.slot(LiveSlots.DRUMS))
        assertTrue(s.diverged)
        assertFalse(s.hasPending)
        assertEquals(0, s.row)
        // Last choice wins; a stop; a cancel.
        s = s.armed(LiveSlots.mask(LiveSlots.ACID), 3).armed(LiveSlots.mask(LiveSlots.ACID), -1)
        assertEquals(-1, s.slot(LiveSlots.ACID).pending)
        s = s.cancelled(LiveSlots.mask(LiveSlots.ACID))
        assertNull(s.slot(LiveSlots.ACID).pending)
        s = s.armed(LiveSlots.mask(LiveSlots.ACID), -1).launched(LiveSlots.ACID, -1, 32)
        assertEquals(-1, s.slot(LiveSlots.ACID).playing)
    }

    @Test
    fun aWholeRowMakesThePlainSceneAgain() {
        var s = LiveState.on(0).armed(LiveSlots.mask(LiveSlots.DRUMS), 1).launched(LiveSlots.DRUMS, 1, 16)
        s = s.armed(LiveSlots.ALL, 3)
        assertEquals(3, s.pendingRow)
        // One slot chosen on its own afterwards: no longer a row.
        assertNull(s.armed(LiveSlots.mask(LiveSlots.ACID), 2).pendingRow)
        for (k in 0 until LiveSlots.COUNT) s = s.launched(k, 3, 32)
        assertEquals(3, s.row)
        assertNull(s.pendingRow)
        assertFalse(s.diverged)
        assertFalse(s.hasPending)
        // Stopping everything is not a row.
        var stopped = s.armed(LiveSlots.ALL, -1)
        assertNull(stopped.pendingRow)
        for (k in 0 until LiveSlots.COUNT) stopped = stopped.launched(k, -1, 48)
        assertEquals(3, stopped.row)
        assertTrue(stopped.diverged)
    }

    @Test
    fun padsShowWhatEachTrackHoldsInEachScene() {
        val drums = DrumPattern().toggle(0, 0)
        val acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { i -> AcidStep(gate = i == 3) })
        val p = Project(
            scenes = listOf(
                Scene("A", drums = drums),
                Scene("B", acid = acid, synthNotes = NoteClip(32, listOf(Note(0, 24, 60)))),
            ),
            scene = 0, drums = drums,
        ).addTrack("Basse")!!.addAudioTrack()!!.sanitized()
        val inst = p.tracks.first().id
        assertTrue(p.cellHasContent(SongPart.DRUMS, 0))
        assertFalse(p.cellHasContent(SongPart.DRUMS, 1))
        assertFalse(p.cellHasContent(SongPart.ACID, 0))
        assertTrue(p.cellHasContent(SongPart.ACID, 1))
        assertTrue(p.cellHasContent(SongPart.inst(0), 1))
        assertFalse(p.cellHasContent(SongPart.inst(inst), 1))
        assertFalse(p.cellHasContent(SongPart.audio(0), 0))
        assertFalse(p.cellHasContent(SongPart.REMIX, 0))
        assertFalse(p.cellHasContent(SongPart.DRUMS, 5))  // no such scene
        assertEquals(32, p.slotLoopSteps(SongPart.inst(0), 1))
        assertEquals(16, p.slotLoopSteps(SongPart.DRUMS, 0))
        // A scene audio clip belongs to its scene only; a song clip to none.
        val withClip = p.withAudioTrack(p.audioTracks.first().copy(clip = AudioClip("a.wav", "a", scene = 1)))
        assertTrue(withClip.cellHasContent(SongPart.audio(0), 1))
        assertFalse(withClip.cellHasContent(SongPart.audio(0), 0))
        assertFalse(withClip.withAudioTrack(withClip.audioTracks.first().copy(clip = AudioClip("a.wav", "a", song = true))).cellHasContent(SongPart.audio(0), 1))
        // Columns: every track, the remix once one is loaded.
        assertEquals(listOf(SongPart.DRUMS, SongPart.ACID, SongPart.inst(0), SongPart.inst(inst), SongPart.audio(0)), p.liveColumns)
        assertEquals(SongPart.REMIX, p.copy(remix = Remix(setId = "x")).liveColumns.last())
    }

    @Test
    fun theQuantumIsSavedAndKeptSane() {
        assertEquals(1, Project().liveQuantum)
        assertEquals(1, ProjectJson.decode("""{"name":"Vieux"}""").liveQuantum)  // older projects
        assertEquals(4, ProjectJson.decode(ProjectJson.encode(Project(liveQuantum = 4))).liveQuantum)
        assertEquals(1, Project(liveQuantum = 3).sanitized().liveQuantum)
        assertEquals(32, Project(liveQuantum = 2).liveQuantumSteps)
        assertEquals(24, Project(liveQuantum = 2, beatsPerBar = 3).liveQuantumSteps)
    }

    @Test
    fun theNextLaunchAndThePositionRing() {
        assertEquals(0, nextLaunchStep(-1, 16))
        assertEquals(16, nextLaunchStep(0, 16))
        assertEquals(16, nextLaunchStep(15, 16))
        assertEquals(32, nextLaunchStep(16, 16))
        assertEquals(64, nextLaunchStep(36, 32))
        assertEquals(0f, livePosition(16, 16, 16))
        assertEquals(0.5f, livePosition(24, 16, 16))
        assertEquals(0.875f, livePosition(12, -16, 32))  // started part-way in
        assertEquals(0f, livePosition(-1, 0, 16))
    }

    private fun threeScenes(): Project = Project(
        scenes = listOf(
            Scene("A", drums = DrumPattern().toggle(0, 0)),
            Scene("B", drums = DrumPattern().withSteps(32).toggle(1, 0)),
            Scene("C", drums = DrumPattern().toggle(2, 0)),
        ),
        scene = 0, drums = DrumPattern().toggle(0, 0),
    ).sanitized()

    @Test
    fun mixedPartsLoadAndSaveAndOldFilesStayTheSame() {
        val plain = threeScenes().copy(song = listOf(SongPart(0, 2), SongPart(1, 1, setOf(SongPart.ACID)))).sanitized()
        val text = ProjectJson.encode(plain)
        // A song made by hand is saved as before: none of the new fields.
        listOf("\"play\"", "\"from\"", "\"bars\"", "songPerBeat", "\"hold\"").forEach { assertFalse(it, text.contains(it)) }
        assertEquals(plain, ProjectJson.decode(text))
        // An older file (no new fields) loads as a plain song.
        val old = ProjectJson.decode("""{"name":"Vieux","scenes":[{"name":"A"},{"name":"B"}],"song":[{"scene":1,"repeats":3}]}""")
        assertEquals(listOf(SongPart(1, 3)), old.song)
        assertFalse(old.song[0].mixed)
        // A part written from the live grid comes back as it was.
        val mixed = plain.copy(song = listOf(SongPart(0, 1, play = mapOf(SongPart.DRUMS to 1), from = mapOf(SongPart.ACID to 2), bars = 3))).sanitized()
        val back = ProjectJson.decode(ProjectJson.encode(mixed))
        assertEquals(mixed.song, back.song)
        assertEquals(48, back.partSteps(back.song[0]))
        assertEquals(3, back.songBars)
    }

    @Test
    fun mixedPartsAreKeptSane() {
        val p = threeScenes()
        val part = SongPart(
            0, 5, setOf(SongPart.DRUMS, "audio:9"),
            play = mapOf(SongPart.ACID to 1, SongPart.DRUMS to 0, "inst:7" to 2, SongPart.inst(0) to 9),
            from = mapOf(SongPart.ACID to 0, SongPart.inst(0) to 99999, "nope" to 2),
            bars = 999,
        )
        val s = p.copy(song = listOf(part)).sanitized().song.single()
        assertEquals(1, s.repeats)  // its own length: no repeats
        assertEquals(setOf(SongPart.DRUMS), s.muted)
        assertEquals(mapOf(SongPart.ACID to 1), s.play)  // its own scene, a missing track or scene: gone
        assertEquals(mapOf(SongPart.inst(0) to SongPart.MAX_FROM), s.from)
        assertEquals(SongPart.MAX_BARS, s.bars)
        assertEquals(1, p.copy(song = listOf(SongPart(0, 1, bars = 0))).sanitized().song.single().bars)
        // 128 parts now.
        assertEquals(128, p.copy(song = List(200) { SongPart(0) }).sanitized().song.size)
        // The length: its own bars, else its scene's times its repeats.
        assertEquals(64, p.partSteps(SongPart(1, 2)))
        assertEquals(16, p.partSteps(SongPart(1, 2, bars = 1)))
        assertEquals(p.partSteps(SongPart(1, 2)) + 16, p.copy(song = listOf(SongPart(1, 2), SongPart(0))).sanitized().songStepOf(2))
    }

    @Test
    fun scenesFollowTheMixedParts() {
        // A scene only a part's track plays is kept (an empty one at the end too).
        val p = threeScenes().switchScene(0)
        val four = p.copy(scenes = p.scenes + Scene("D"))
        assertEquals(3, four.sanitized().allScenes.size)
        val withEmpty = four.copy(song = listOf(SongPart(0, play = mapOf(SongPart.ACID to 3)))).sanitized()
        assertEquals(4, withEmpty.allScenes.size)
        assertEquals(mapOf(SongPart.ACID to 3), withEmpty.song.single().play)
        // A scene dragged elsewhere: the tracks that played it follow it.
        val song = p.copy(song = listOf(SongPart(0, play = mapOf(SongPart.ACID to 1, SongPart.DRUMS to 2)))).sanitized()
        val moved = song.moveScene(1, 2)  // swap B and C
        assertEquals(mapOf(SongPart.ACID to 2, SongPart.DRUMS to 1), moved.song.single().play)
        // Put in place of the part's own scene: that track now just plays the part's scene.
        val onto = song.moveScene(2, 0, replace = true)
        assertEquals(0, onto.song.single().scene)
        assertEquals(mapOf(SongPart.ACID to 1), onto.song.single().play)
        // Copying a scene onto a slot a track plays: it plays the copy.
        val copied = song.copySceneTo(1)
        assertEquals(copied.allScenes[0].drums, copied.allScenes[1].drums)
        assertEquals(mapOf(SongPart.ACID to 1, SongPart.DRUMS to 2), copied.song.single().play)
        // The timeline's long press: a track on another scene (its own again: plain).
        val chosen = p.copy(song = listOf(SongPart(0))).sanitized().withPartScene(0, SongPart.DRUMS, 2)
        assertEquals(mapOf(SongPart.DRUMS to 2), chosen.song.single().play)
        assertTrue(chosen.song.single().mixed)
        assertFalse(chosen.withPartScene(0, SongPart.DRUMS, 0).song.single().mixed)
        assertEquals(chosen, chosen.withPartScene(0, "nope", 1))
    }

    @Test
    fun perBeatCurvesFollowTheArrangement() {
        val p = threeScenes().copy(song = listOf(SongPart(0), SongPart(2))).sanitized()
        val target = AutoTarget(AutoTarget.VOLUME, 2)
        // A point per beat: beat 1 of part 0, beat 2 of part 1 (song beat 6).
        val lane = AutomationLane(target, mode = AutomationLane.SONG, songPerBeat = true).withSongPoint(1, 0.2f).withSongPoint(6, 0.8f)
        val withLane = p.copy(automation = listOf(lane))
        val swapped = withLane.moveSongPart(1, 0)
        val values = swapped.automation.single().songValues
        // Part 1 now first: its beat 2 is song beat 2; part 0's beat 1 is song beat 5.
        assertEquals(0.8f, values[2])
        assertEquals(0.2f, values[5])
        // A per-bar curve becomes per beat without changing what is heard.
        val bars = AutomationLane(target, mode = AutomationLane.SONG).withSongPoint(0, 0.0f).withSongPoint(2, 1.0f)
        val beats = bars.perBeat(4)
        assertEquals(listOf(0f, null, null, null, null, null, null, null, 1f), beats.songValues)
        assertEquals(bars.songCurve(3)[1], beats.songCurve(12)[4], 1e-6f)
        // Hold: steps instead of sliding.
        assertEquals(listOf(0f, 0f, 1f), bars.copy(hold = true).songCurve(3))
        // Per-beat curves hold up to 4096 points (1024 bars of 4/4: a long live set).
        assertEquals(4095, AutomationLane(target, songPerBeat = true).withSongPoint(4095, 1f).songValues.lastIndex)
        assertTrue(AutomationLane(target, songPerBeat = true).withSongPoint(4096, 1f).songValues.isEmpty())
        assertTrue(AutomationLane(target).withSongPoint(300, 1f).songValues.isEmpty())
    }
}
