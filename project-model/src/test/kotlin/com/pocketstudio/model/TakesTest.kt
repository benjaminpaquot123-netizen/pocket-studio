package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TakesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** Scene A one bar, B two bars; song A ×2, B, A. */
    private fun song(): Project {
        val p = Project().copySceneTo(1).switchScene(1)
        return p.copy(drums = p.drums.copy(steps = 32), song = listOf(SongPart(0, repeats = 2), SongPart(1), SongPart(0)))
            .sanitized().switchScene(0)
    }

    @Test
    fun eachPartStartsOnItsPlaceInTheSong() {
        val p = song()
        assertEquals(listOf(0, 32, 64, 80), (0..3).map { p.songStepOf(it) })
        // Automation recorded from part 3 lands on bar 4 of the song (step 64), not on bar 1.
        assertEquals(4, p.songStepOf(2) / p.stepsPerBar)
        assertEquals(0, p.songStepOf(-1))
    }

    @Test
    fun aPartStartsWithWhatThePartsBeforeLeftTheAutomationAt() {
        val volume = AutoTarget(AutoTarget.VOLUME, 2)
        val lane = AutomationLane(volume, mode = AutomationLane.PER_SCENE)
            .withPoint(4, 0.3f, scene = 0)   // A: one point on step 4
            .withPoint(20, 0.9f, scene = 1)  // B: one point on step 20 of its 32
        val loop = AutomationLane(AutoTarget(AutoTarget.PAN, 2))  // never written
        val p = song().copy(automation = listOf(lane, loop))
        assertTrue(p.automationBefore(0).isEmpty())
        assertEquals(mapOf(0 to 0.3f), p.automationBefore(1))
        assertEquals(mapOf(0 to 0.9f), p.automationBefore(2))  // B's last point holds into A
        assertEquals(mapOf(0 to 0.3f), p.automationBefore(3))
        // A song lane sets its value on every step: nothing to carry.
        val songLane = AutomationLane(volume, mode = AutomationLane.SONG).withSongPoint(0, 0.5f)
        assertTrue(song().copy(automation = listOf(songLane)).automationBefore(3).isEmpty())
    }

    @Test
    fun aRetakeKeepsTheClipsSettings() {
        var p = Project().addAudioTrack("Voix")!!
        val old = AudioClip("old.wav", "Prise 1", 3.0, start = 32, loopSteps = 16, gain = 0.7f, fadeIn = 0.2f, fadeOut = 0.5f, once = false, offsetMs = -15f)
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = old))
        val start = takeStart(old, TakeAnchor())
        assertEquals(32, start)
        val q = p.withTake(PendingTake(0, "new.wav", "Prise 2", 9.0, 120.0, start = start, replaces = "old.wav"))
        val clip = q.audioTracks[0].clip!!
        assertEquals("new.wav", clip.file)
        assertEquals(0.7f, clip.gain)
        assertEquals(0.2f, clip.fadeIn)
        assertEquals(0.5f, clip.fadeOut)
        assertEquals(32, clip.start)
        assertEquals(-15f, clip.offsetMs)
        assertFalse(clip.once)  // the user's choice stays
        assertEquals(5 * 16, clip.loopSteps)  // 9 s at 120 BPM: 4.5 bars → 5
        assertEquals(120.0, clip.bpm, 0.0)
        // Applied twice (the app closed after it was put on): nothing changes.
        assertEquals(q, q.withTake(PendingTake(0, "new.wav", "Prise 2", 9.0, 120.0, start = start, replaces = "old.wav")))
    }

    @Test
    fun anOlderTakeNeverReplacesANewerOne() {
        // Take A (over "old.wav") failed to be placed; take B was recorded and placed since.
        var p = Project().addAudioTrack("Voix")!!
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = AudioClip("old.wav", "Prise 1", 3.0)))
        val a = PendingTake(0, "a.wav", "Prise 2", 4.0, 120.0, replaces = "old.wav")
        val b = PendingTake(0, "b.wav", "Prise 3", 4.0, 120.0, replaces = "old.wav")
        val withB = p.withTake(b)
        assertEquals("b.wav", withB.audioTracks[0].clip?.file)
        // At the next launch A's note is still there: B stays.
        assertEquals(withB, withB.withTake(a))
        // A take on an empty track, then the track got a newer clip: the same.
        val empty = Project().addAudioTrack()!!
        val first = PendingTake(0, "c.wav", "Prise", 4.0, 120.0)
        val newer = empty.withTake(PendingTake(0, "d.wav", "Prise", 4.0, 120.0))
        assertEquals("d.wav", newer.withTake(first).audioTracks[0].clip?.file)
        // A track emptied since takes it back (nothing newer to lose).
        assertEquals("a.wav", p.withAudioTrack(p.audioTracks[0].copy(clip = null)).withTake(a).audioTracks[0].clip?.file)
    }

    @Test
    fun songClipsPlayOnlyInTheSong() {
        var p = Project().addAudioTrack()!!.addAudioTrack()!!
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = AudioClip("v.wav", "Prise", 20.0, song = true)))
        p = p.withAudioTrack(p.audioTracks[1].copy(clip = AudioClip("g.wav", "Prise", 2.0)))
        // A scene on its own (live or a loop export): the song clip is silent, a scene clip plays.
        assertFalse(p.audioPlays(0, null))
        assertTrue(p.audioPlays(1, null))
        // In the song: both, unless the part mutes them.
        assertTrue(p.audioPlays(0, SongPart(0)))
        assertFalse(p.audioPlays(0, SongPart(0, muted = setOf(SongPart.audio(0)))))
        // So a loop export is not made longer for it.
        assertEquals(1, p.audioEndBars())
    }

    @Test
    fun aFirstSceneTakePlaysOncePerPartAndASongTakeOnTheSong() {
        val p = Project().addAudioTrack()!!
        val scene = p.withTake(PendingTake(0, "a.wav", "Prise", 4.0, 120.0)).audioTracks[0].clip!!
        assertTrue(scene.once)
        assertFalse(scene.song)
        val songTake = p.withTake(PendingTake(0, "b.wav", "Prise", 4.0, 120.0, song = true, start = 64)).audioTracks[0].clip!!
        assertTrue(songTake.song)
        assertFalse(songTake.once)
        assertEquals(64, songTake.start)
        // Recorded on the whole song over a scene clip: it starts where the recording did.
        assertEquals(48, takeStart(scene.copy(start = 16), TakeAnchor(song = true, from = 48)))
        // A song retake keeps a later start; one before the recording began cannot be kept.
        assertEquals(96, takeStart(songTake.copy(start = 96), TakeAnchor(song = true, from = 64)))
        assertEquals(64, takeStart(songTake.copy(start = 32), TakeAnchor(song = true, from = 64)))
    }

    @Test
    fun aTakeWhoseTrackWasRemovedBringsItBack() {
        val p = Project()
        val q = p.withTake(PendingTake(2, "a.wav", "Prise", 4.0, 120.0))
        assertEquals(listOf(2), q.audioTracks.map { it.id })
        assertEquals("a.wav", q.audioTracks[0].clip?.file)
        assertEquals(p, p.withTake(PendingTake(7, "a.wav", "Prise", 4.0, 120.0)))
    }

    @Test
    fun waitingTakesAreReadBackAndCleared() {
        val folder = tmp.newFolder("projet-1")
        java.io.File(folder, "samples").mkdirs()
        java.io.File(folder, "samples/abc.wav").writeBytes(ByteArray(4))
        val take = PendingTake(1, "abc.wav", "Prise 21:14", 12.5, 140.0, song = true, start = 128)
        PendingTakes.write(folder, take)
        PendingTakes.write(folder, take.copy(file = "missing.wav"))  // its sound never made it: ignored
        assertEquals(listOf(take), PendingTakes.read(folder))
        val p = Project().withTakes(PendingTakes.read(folder))
        assertEquals("abc.wav", p.audioTracks.single().clip?.file)
        assertTrue("abc.wav" in p.sampleFiles)
        assertEquals(setOf("abc.wav", "missing.wav"), PendingTakes.files(folder))
        // Clearing what was read leaves a note written since (a take finishing meanwhile).
        val read = PendingTakes.read(folder)
        PendingTakes.write(folder, take.copy(file = "later.wav"))
        PendingTakes.clear(folder, read)
        assertEquals(setOf("missing.wav", "later.wav"), PendingTakes.files(folder))
        PendingTakes.remove(folder, "later.wav")
        assertEquals(setOf("missing.wav"), PendingTakes.files(folder))
    }

    @Test
    fun onceClipsLoopWithTheirScenePlayedAlone() {
        val c = AudioClip("a.wav", "a", seconds = 26.0, once = true)  // 13 bars at 120 BPM
        assertEquals(16 * 16, c.engineLoopSteps(sceneSteps = 128, tempo = 120.0))  // two passes of an 8-bar scene
        assertEquals(0, c.engineLoopSteps(sceneSteps = null, tempo = 120.0))      // a song part: once per part
        assertEquals(64, c.copy(once = false, loopSteps = 64).engineLoopSteps(128, 120.0))
        assertEquals(0, c.copy(song = true).engineLoopSteps(128, 120.0))
    }

    @Test
    fun oldClipsLoadWithTheirOldBehaviour() {
        val json = """{"file":"a.wav","name":"a","seconds":2.0,"start":16,"loopSteps":32}"""
        val c = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(AudioClip.serializer(), json).sanitized()
        assertFalse(c.song)
        assertFalse(c.once)
        assertEquals(0f, c.offsetMs)
        assertEquals(0.0, c.bpm, 0.0)
        assertEquals(32, c.engineLoopSteps(64, 120.0))
        assertNull(AudioClip("a.wav", "a", offsetMs = Float.NaN).sanitized().offsetMs.takeIf { it != 0f })
        assertEquals(200f, AudioClip("a.wav", "a", offsetMs = 900f).sanitized().offsetMs)
        assertEquals(-240, AudioClip("a.wav", "a", offsetMs = -5f).offsetFrames(48000))
        assertTrue(AudioClip("a.wav", "a", bpm = 124.0).tempoDiffers(126.0))
        assertFalse(AudioClip("a.wav", "a").tempoDiffers(126.0))
    }
}
