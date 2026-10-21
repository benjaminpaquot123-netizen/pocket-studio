package com.pocketstudio.model

import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Song curves, song clips and scene takes follow the arrangement (phase 24 audit, rappeur G1/M1/M3, pro G2). */
class SongFollowTest {
    private val volume = AutoTarget(AutoTarget.VOLUME, 2)

    /**
     * Scene A one bar, B two bars; song A, B, A (bars 0 | 1-2 | 3). A song curve with a point on
     * each bar (0.1, 0.2, 0.3, 0.4) and a song clip 4 steps into the last part (step 52).
     */
    private fun song(points: List<Float?> = listOf(0.1f, 0.2f, 0.3f, 0.4f)): Project {
        val p = Project().copySceneTo(1).switchScene(1)
        var q = p.copy(drums = p.drums.copy(steps = 32), song = listOf(SongPart(0), SongPart(1), SongPart(0)))
            .sanitized().switchScene(0).addAudioTrack()!!
        q = q.withAudioTrack(q.audioTracks[0].copy(clip = AudioClip("v.wav", "Prise", 8.0, start = 52, song = true)))
        return q.copy(automation = listOf(AutomationLane(volume, mode = AutomationLane.SONG, songValues = points))).sanitized()
    }

    private fun Project.points() = automation[0].songValues
    private fun Project.clipStart() = audioTracks[0].clip!!.start

    @Test
    fun anInsertedPartPushesWhatFollowsAndHoldsTheValueBeforeIt() {
        val p = song()
        assertEquals(listOf(0, 16, 48), p.song.indices.map { p.songStepOf(it) })
        // B inserted first: everything moves two bars on, the new bars hold the first value.
        val q = p.insertSongPart(0, 1)
        assertEquals(listOf(0.1f, 0.1f, 0.1f, 0.2f, 0.3f, 0.4f), q.points())
        assertEquals(84, q.clipStart())
    }

    @Test
    fun aRampAroundAnInsertedPartSoundsTheSameOnBothSides() {
        // A ramp 0 → 0.6 over bars 0..3 (points on bars 0 and 3 only).
        val p = song(listOf(0f, null, null, 0.6f))
        assertEquals(listOf(0f, 0.2f, 0.4f, 0.6f), p.automation[0].songCurve(4).map { Math.round(it * 100) / 100f })
        // A (one bar) inserted after the first part: it holds 0, then B and the last A play as before.
        val q = p.insertSongPart(1, 0)
        assertEquals(listOf(0f, 0f, 0.2f, 0.4f, 0.6f), q.automation[0].songCurve(5).map { Math.round(it * 100) / 100f })
    }

    @Test
    fun aRemovedPartTakesItsPointsAndTheRestCloseUp() {
        val q = song().removeSongPart(1)
        assertEquals(listOf(0.1f, 0.4f), q.points())
        assertEquals(20, q.clipStart())
        // A clip that started in the removed part keeps its offset from where that part was.
        val p = song()
        val inB = p.withAudioTrack(p.audioTracks[0].copy(clip = p.audioTracks[0].clip!!.copy(start = 20)))
        assertEquals(20, inB.removeSongPart(1).clipStart())
        // The last part removed: nothing to follow.
        val one = Project().copy(song = listOf(SongPart(0))).sanitized()
        assertTrue(one.removeSongPart(0).song.isEmpty())
    }

    @Test
    fun aMovedPartTakesItsPointsAndClipsAlong() {
        val q = song().moveSongPart(2, 0)
        assertEquals(listOf(0, 0, 1), q.song.map { it.scene })
        assertEquals(listOf(0.4f, 0.1f, 0.2f, 0.3f), q.points())
        assertEquals(4, q.clipStart())
    }

    @Test
    fun aDuplicatedPartCopiesItsPoints() {
        val q = song().duplicateSongPart(1)
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.2f, 0.3f, 0.4f), q.points())
        assertEquals(84, q.clipStart())
    }

    @Test
    fun moreRepeatsHoldTheLastValueAndPushTheRest() {
        val p = song()
        val q = p.withSongRepeats(1, 2)
        assertEquals(2, q.song[1].repeats)
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.3f, 0.3f, 0.4f), q.points())
        assertEquals(84, q.clipStart())
        // Back to one: the extra bars go, the rest comes back.
        val back = q.withSongRepeats(1, 1)
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f), back.points())
        assertEquals(52, back.clipStart())
        assertSame(p, p.withSongRepeats(1, 1))
        assertEquals(SongPart.MAX_REPEATS, p.withSongRepeats(1, 99).song[1].repeats)
    }

    @Test
    fun aSceneThatGetsShorterMovesWhatFollowsIt() {
        val p = song()
        // B made one bar long (its scene's own edit): the part after it moves a bar earlier.
        val edited = p.switchScene(1).let { it.copy(drums = it.drums.copy(steps = 16)) }.sanitized().switchScene(0)
        val q = edited.followPartLengths(p)
        assertEquals(listOf(0.1f, 0.2f, 0.4f), q.points())
        assertEquals(36, q.clipStart())
        // Nothing changed in length: the same project back.
        val renamed = p.copy(name = "x")
        assertSame(renamed, renamed.followPartLengths(p))
    }

    @Test
    fun anotherTimeSignatureKeepsEachPointAndClipInItsPart() {
        val p = song()
        val q = p.withBeatsPerBar(3).followPartLengths(p)
        assertEquals(12, q.stepsPerBar)
        // In 3/4 the one-bar scene A lasts two bars (its 16-step piano roll): the bars added hold.
        assertEquals(listOf(0, 24, 48), q.song.indices.map { q.songStepOf(it) })
        assertEquals(listOf(0.1f, 0.1f, 0.2f, 0.3f, 0.4f, 0.4f), q.points())
        assertEquals(q.songStepOf(2) + 4, q.clipStart())  // 4 steps into the last part
    }

    @Test
    fun aSongClipPastBar64KeepsItsBarInAnotherTimeSignature() {
        var p = Project().addAudioTrack()!!
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = AudioClip("v.wav", "Prise", 8.0, start = 100 * 16, song = true)))
        assertEquals(100 * 12, p.withBeatsPerBar(3).audioTracks[0].clip!!.start)
    }

    @Test
    fun aSongClipKeepsItsBarWhenTheSessionChangesTheTimeSignature() = kotlinx.coroutines.test.runTest {
        // Through the session, where the parts' new lengths also move what the song places after them.
        val dir = java.nio.file.Files.createTempDirectory("ts").toFile()
        val store = ProjectStore(dir)
        val io = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val session = ProjectSession("p", song(), store, this, io, clock = { testScheduler.currentTime })
        session.edit { it.withBeatsPerBar(5) }
        val p = session.project.value
        // Bars of 20: A | B B | A from steps 0, 20, 60; the clip stays 4 steps into the last part.
        assertEquals(listOf(0, 20, 60), p.song.indices.map { p.songStepOf(it) })
        assertEquals(64, p.clipStart())
        // Past bar 64: a long song (5 parts of 16 bars), the clip in bar 70, then back to 4/4.
        var long = song().copy(song = List(5) { SongPart(0, repeats = 16) }).sanitized()
        long = long.withAudioTrack(long.audioTracks[0].copy(clip = long.audioTracks[0].clip!!.copy(start = 70 * 16 + 4)))
        val longSession = ProjectSession("q", long, store, this, io, clock = { testScheduler.currentTime })
        longSession.edit { it.withBeatsPerBar(5) }
        assertEquals(70 * 20 + 4, longSession.project.value.clipStart())
        longSession.edit { it.withBeatsPerBar(4) }
        assertEquals(70 * 16 + 4, longSession.project.value.clipStart())
        advanceUntilIdle()
        dir.deleteRecursively()
    }

    @Test
    fun aSceneTakePlaysOnlyInThePartsOfItsScene() {
        var p = song()
        p = p.withTake(PendingTake(0, "c.wav", "Prise", 2.0, 120.0, replaces = "v.wav", scene = 1))
        val clip = p.audioTracks[0].clip!!
        assertEquals(1, clip.scene)
        assertFalse(clip.song)
        // Parts of A: silent; the part of B: heard. Alone: with B only.
        assertFalse(p.audioPlays(0, SongPart(0)))
        assertTrue(p.audioPlays(0, SongPart(1)))
        assertFalse(p.audioPlays(0, SongPart(1, muted = setOf(SongPart.audio(0)))))
        assertFalse(p.audioPlays(0, null, scene = 0))
        assertTrue(p.audioPlays(0, null, scene = 1))
        // The timeline shows it in part 1 only (two seconds at 120 BPM = 16 steps of its 32).
        val spans = p.audioSpans(0)
        assertTrue(spans[0].isEmpty() && spans[2].isEmpty())
        assertEquals(0f, spans[1].single().start, 1e-6f)
        assertEquals(0.5f, spans[1].single().endInclusive, 1e-6f)
        // Older clips (no scene) still play everywhere.
        val old = p.withAudioTrack(p.audioTracks[0].copy(clip = clip.copy(scene = null)))
        assertTrue(old.audioPlays(0, SongPart(0)) && old.audioPlays(0, null, scene = 0))
        // A loop export of scene A does not wait for B's take.
        assertEquals(0, p.audioEndBars())
        // A song take never belongs to a scene.
        assertNull(Project().withTake(PendingTake(0, "s.wav", "Prise", 2.0, 120.0, song = true, scene = 1)).audioTracks[0].clip!!.scene)
    }

    @Test
    fun aSongClipIsDrawnWhereItIsHeard() {
        val spans = song().audioSpans(0)  // 8 s at 120 BPM = 64 steps from step 52: the last 12 of the song
        assertTrue(spans[0].isEmpty() && spans[1].isEmpty())
        assertEquals(0.25f, spans[2].single().start, 1e-6f)
        assertEquals(1f, spans[2].single().endInclusive, 1e-6f)
    }

    @Test
    fun cuttingTheStartOfATakeKeepsWhatIsLeftInPlace() {
        // 120 BPM: an eighth is 250 ms. Half a second cut: two eighths later, nothing left over.
        val c = AudioClip("v.wav", "Prise", 4.0, start = 16, song = true)
        val cut = c.afterCutStart(0.5, 120.0)
        assertEquals(20, cut.start)
        assertEquals(0f, cut.offsetMs, 1e-3f)
        // 0.6 s: the start moves by whole eighths (never onto a swung sixteenth), the rest is an offset.
        val more = c.copy(offsetMs = 10f).afterCutStart(0.6, 120.0)
        assertEquals(0, (more.start - 16) % 2)
        assertEquals(16 * 15.0 / 120 * 1000 + 10 + 600, (more.start * 15.0 / 120 * 1000 + more.offsetMs), 0.01)
        assertTrue(Math.abs(more.offsetMs) <= 125f)
        assertSame(c, c.afterCutStart(0.0, 120.0))
    }

    @Test
    fun cuttingALongStartNeverPushesASceneClipPastItsScene() {
        // A 30 s take on a 4-bar scene (64 steps, 8 s at 120 BPM): 20 s cut would start it at step 176.
        val take = AudioClip("v.wav", "Prise", 30.0, start = 4, scene = 0)
        val far = take.afterCutStart(20.0, 120.0, sceneSteps = 64)
        assertTrue("start ${far.start}", far.start < 64)
        assertEquals(take, far)
        // A shorter cut still moves it, inside the scene.
        assertEquals(12, take.afterCutStart(1.0, 120.0, sceneSteps = 64).start)
        // A song clip is not bound to a scene.
        assertEquals(164, take.copy(song = true, scene = null).afterCutStart(20.0, 120.0, sceneSteps = 64).start)
    }

    @Test
    fun aRetakeKeepsThePreviousTakeOneTouchAway() {
        val p = Project().addAudioTrack()!!.withTake(PendingTake(0, "a.wav", "Prise 1", 4.0, 120.0, scene = 0))
        val q = p.withTake(PendingTake(0, "b.wav", "Prise 2", 4.0, 120.0, replaces = "a.wav", scene = 0))
        assertEquals("b.wav", q.audioTracks[0].clip?.file)
        assertEquals("a.wav", q.audioTracks[0].previous?.file)
        // Kept on disk (and in backups), not loaded.
        assertEquals(setOf("a.wav"), q.previousTakeFiles)
        assertFalse("a.wav" in q.sampleFiles)
        // Restored: the two swap places; once more, back again.
        val r = q.restorePreviousTake(0)
        assertEquals("a.wav", r.audioTracks[0].clip?.file)
        assertEquals("b.wav", r.audioTracks[0].previous?.file)
        assertEquals("b.wav", r.restorePreviousTake(0).audioTracks[0].clip?.file)
        // Saved and read back.
        assertEquals(q.audioTracks, ProjectJson.decode(ProjectJson.encode(q)).audioTracks)
        assertSame(p, p.restorePreviousTake(0))
    }

    @Test
    fun theTopRecordButtonNeverReplacesAnotherPassage() {
        val p = song()  // track 0 holds a song take starting in part 2
        val inPart2 = TakeAnchor(song = true, from = p.songStepOf(2))
        val inPart1 = TakeAnchor(song = true, from = p.songStepOf(1))
        // A retake of the same part on the track recorded last: that track.
        assertEquals(0, p.micTrackFor(0, inPart2, 0))
        // Another part: not that track; no empty one either: a new track is needed.
        assertNull(p.micTrackFor(0, inPart1, 0))
        // An empty track is used before adding one.
        val two = p.addAudioTrack()!!
        assertEquals(1, two.micTrackFor(0, inPart1, 0))
        assertEquals(1, two.micTrackFor(null, inPart2, 0))
        // A new voice track is called « Voix », then « Voix 2 ».
        assertEquals("Voix", voiceName(p))
        assertEquals("Voix 2", voiceName(p.addAudioTrack("Voix")!!))
        assertEquals(2, p.partAt(52))
        assertEquals(1, p.partAt(16))
        assertEquals(-1, Project().partAt(0))
    }

    @Test
    fun aSceneAlonePlaysSongCurvesAtItsPlaceInTheSong() {
        val p = song()
        // B first plays on bar 1: its value there (0.2); a scene not in the song: none.
        assertEquals(mapOf(0 to 0.2f), p.songCurvesAt(1))
        assertTrue(p.copy(song = listOf(SongPart(0))).songCurvesAt(1).isEmpty())
    }

    @Test
    fun theListeningChoiceIsSavedAndOldProjectsHaveNone() {
        assertNull(ProjectJson.decode(ProjectJson.encode(Project())).listenSong)
        assertEquals(false, ProjectJson.decode(ProjectJson.encode(Project(listenSong = false))).listenSong)
        assertNull(ProjectJson.decode("""{"name":"Vieux"}""").listenSong)
    }
}
