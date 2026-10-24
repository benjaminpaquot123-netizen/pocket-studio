package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 35 « Enregistrer sans peur »: takes put in time without losing them, the last bars kept, versions of the song. */
class RecordTakesTest {
    private val t = NoteClip.TICKS_PER_STEP

    @Test
    fun puttingInTimeKeepsWherePlayedAndComesBack() {
        val played = NoteClip(16, listOf(Note(t + 5, 20, 60), Note(4 * t - 7, 20, 62), Note(8 * t, 24, 64)))
        val inTime = played.inTime({ true }, t, 1f)
        assertEquals(listOf(t, 4 * t, 8 * t), inTime.notes.map { it.start })
        assertEquals(listOf(t + 5, 4 * t - 7, null), inTime.notes.map { it.played })
        // Half way, from where it was played (not from where the full strength put it).
        val half = inTime.inTime({ true }, t, 0.5f)
        assertEquals(listOf(t + 3, 4 * t - 3, 8 * t), half.notes.map { it.start })
        // « Comme joué »: exactly as played, nothing left over.
        assertEquals(played.notes, half.asPlayed { true }.notes)
        assertFalse(half.asPlayed { true }.anyMovedInTime)
        assertTrue(half.anyMovedInTime)
    }

    @Test
    fun aNotePulledOntoTheLoopsEndStartsTheLoop() {
        val clip = NoteClip(16, listOf(Note(16 * t - 3, 10, 60)))
        val n = clip.inTime({ true }, t, 1f).notes.single()
        assertEquals(0, n.start)
        assertEquals(16 * t - 3, n.played)
    }

    @Test
    fun onlyTheChosenNotesMoveAndTheFileStaysTheSameWithout() {
        val a = Note(t + 5, 20, 60)
        val b = Note(2 * t + 3, 20, 62)
        val moved = NoteClip(16, listOf(a, b)).inTime({ it == a }, t, 1f)
        assertEquals(listOf(t, 2 * t + 3), moved.notes.map { it.start })
        // A note never put in time is saved as before (no « played » key).
        val json = ProjectJson.encode(Project(synthNotes = NoteClip(16, listOf(b))))
        assertFalse(json.contains("played"))
        val back = ProjectJson.decode(ProjectJson.encode(Project(synthNotes = moved)))
        assertEquals(moved.notes, back.synthNotes.notes)
    }

    @Test
    fun theRollsQuantizeIsNonDestructiveAndAHandMoveForgets() {
        val clip = NoteClip(16, listOf(Note(t + 5, 20, 60)))
        val q = NoteEdits.quantize(clip, clip.notes.toSet(), t, 1f)
        assertEquals(t + 5, q.clip.notes.single().played)
        assertEquals(t + 5, NoteEdits.asPlayed(q.clip, q.selection).clip.notes.single().start)
        val shifted = NoteEdits.shift(q.clip, q.selection, t)
        assertNull(shifted.clip.notes.single().played)
    }

    @Test
    fun theMemoryKeepsTheLastBarsWithoutAllocatingAndWrapsAround() {
        val m = PlayedMemory(capacity = 8)
        m.newEpoch(playing = true)
        // 10 notes in a ring of 8: the first two go.
        for (i in 0 until 10) {
            m.noteOn(0, 60 + i, 0.8f, 16.0 + i * 4)
            m.noteOff(0, 60 + i, 16.0 + i * 4 + 2)
        }
        val c = m.capture(now = 60.0, bars = 8, stepsPerBar = 16)
        assertEquals((62..69).toList(), c.notes.map { it.pitch })
        // On the music's clock the bars are the music's: the first kept note (step 24) is in bar 1 (from 0), at 8.
        assertEquals(1, c.firstBar)
        assertEquals(8.0, c.notes.first().start, 1e-9)
        assertEquals(2.0, c.notes.first().length, 1e-9)
        assertEquals(3, c.bars)
    }

    @Test
    fun onlyTheLastEightBarsAndTheCurrentClockAreKept() {
        val m = PlayedMemory()
        m.newEpoch(playing = false)
        m.noteOn(0, 40, 0.8f, 0.0)
        m.noteOff(0, 40, 1.0)
        m.newEpoch(playing = true)
        m.noteOn(0, 50, 0.8f, 3.0)
        m.noteOff(0, 50, 4.0)
        m.noteOn(0, 52, 0.8f, 200.0)
        m.noteOff(0, 52, 202.0)
        // 8 bars before step 210: from step 82; the note at 3 is too old, the one on the old clock is not this one's.
        val c = m.capture(210.0, 8, 16)
        assertEquals(listOf(52), c.notes.map { it.pitch })
        assertEquals(12, c.firstBar)
        assertEquals(8.0, c.notes.single().start, 1e-9)
    }

    @Test
    fun stoppedTheFirstNoteStartsTheLoopAndAHeldKeyEndsNow() {
        val m = PlayedMemory()
        m.newEpoch(playing = false)
        m.noteOn(1, 60, 0.7f, 103.3)
        m.noteOff(1, 60, 105.3)
        m.noteOn(1, 64, 0.7f, 107.3)  // still held
        val c = m.capture(110.3, 8, 16)
        assertFalse(c.playing)
        assertEquals(listOf(0.0, 4.0), c.notes.map { it.start })
        assertEquals(3.0, c.notes[1].length, 1e-9)
        val clip = NoteClip(16).withCaptured(c, id = 1, stepsPerBar = 16)!!
        assertEquals(listOf(0, 4 * t), clip.notes.map { it.start })
        assertNull(NoteClip(16).withCaptured(c, id = 0, stepsPerBar = 16))
    }

    @Test
    fun aCaptureLongerThanTheLoopGrowsItAndKeepsItsPlaceInTheMusic() {
        val m = PlayedMemory()
        m.newEpoch(playing = true)
        // Bars 5 to 7 of the music (from 0: 4, 5, 6).
        m.noteOn(0, 60, 0.8f, 4 * 16.0)
        m.noteOff(0, 60, 4 * 16.0 + 4)
        m.noteOn(0, 62, 0.8f, 6 * 16.0 + 8)
        m.noteOff(0, 62, 6 * 16.0 + 12)
        val c = m.capture(7 * 16.0, 8, 16)
        assertEquals(4, c.firstBar)
        assertEquals(3, c.bars)
        // A one-bar loop grows to four bars; bar 4 of the music is bar 0 of a four-bar loop.
        val clip = NoteClip(16, listOf(Note(0, 24, 48))).withCaptured(c, 0, 16)!!
        assertEquals(64, clip.steps)
        assertEquals(listOf(0 to 48, 0 to 60, (32 + 8) * t to 62), clip.notes.map { it.start to it.pitch })
    }

    @Test
    fun padsGoOnTheNearestStepAndAHitAlreadyThereStays() {
        val m = PlayedMemory()
        m.newEpoch(playing = true)
        m.pad(2, 0.9f, 16.0 + 3.8)
        m.pad(0, 0.9f, 16.0 + 8.1)
        val c = m.capture(32.0, 8, 16)
        val drums = DrumPattern().let { d -> d.copy(lanes = d.lanes.toMutableList().also { it[0] = it[0].copy(hits = mapOf(8 to Hit(0.5f))) }) }
        val out = drums.withCaptured(c, 16)!!
        assertEquals(setOf(4), out.lanes[2].hits.keys)
        assertEquals(0.5f, out.lanes[0].hits.getValue(8).velocity)
    }

    // --- Versions of the song ---------------------------------------------------------------------

    /** Scenes A and B (a kick each), the song A B B. */
    private fun song(): Project {
        fun kick(step: Int) = DrumPattern().let { d -> d.copy(lanes = d.lanes.toMutableList().also { it[0] = it[0].copy(hits = mapOf(step to Hit())) }) }
        return Project(scenes = listOf(Scene("A", drums = kick(0)), Scene("B", drums = kick(4))), drums = kick(0))
            .copy(song = listOf(SongPart(0), SongPart(1, repeats = 2))).sanitized()
    }

    @Test
    fun aVersionKeepsTheSongAndARestoreSwapsIt() {
        val p = song()
        val kept = p.withVersionKept(SongVersion.BEFORE_TAKE, 1000L)
        assertEquals(1, kept.songVersions.size)
        // The take writes another song: the version still has the old one.
        val after = kept.copy(song = listOf(SongPart(1))).sanitized()
        assertEquals(p.song, after.songVersions[0].song)
        val back = after.restoreVersion(0, 2000L)
        assertEquals(p.song, back.song)
        // The song it replaced is kept in turn: nothing is ever lost.
        assertEquals(listOf(SongPart(1)), back.songVersions[0].song)
        assertEquals(after.song, back.restoreVersion(0, 3000L).song)
        assertEquals(1, back.songVersions.size)
        assertEquals(SongVersion.RESTORE, back.songVersions[0].kind)
        // In the file, and back.
        assertEquals(back.songVersions, ProjectJson.decode(ProjectJson.encode(back)).songVersions)
        assertFalse(ProjectJson.encode(p).contains("songVersions"))
    }

    @Test
    fun noSongNoVersionAndTheOldestGo() {
        assertTrue(Project().withVersionKept(SongVersion.BEFORE_TAKE, 0L).songVersions.isEmpty())
        var p = song()
        // A different song each time (the same one twice in a row is kept once).
        repeat(SongVersion.MAX + 3) { p = p.copy(song = listOf(SongPart(0, repeats = it + 1))).withVersionKept(SongVersion.BEFORE_TAKE, it.toLong()) }
        assertEquals(SongVersion.MAX, p.songVersions.size)
        assertEquals((SongVersion.MAX + 2).toLong(), p.songVersions.first().at)
        assertEquals(SongVersion.MAX - 1, p.dropVersion(0).songVersions.size)
    }

    @Test
    fun aRestoreBringsBackTheScenesNotesToo() {
        val p = song()
        val kept = p.withVersionKept(SongVersion.BEFORE_TAKE, 0L)
        val played = kept.copy(synthNotes = NoteClip(16, listOf(Note(0, 24, 72)))).sanitized()
        assertEquals(1, played.allScenes[0].synthNotes.notes.size)
        val back = played.restoreVersion(0, 1L)
        assertTrue(back.allScenes[0].synthNotes.notes.isEmpty())
        assertTrue(back.synthNotes.notes.isEmpty())
    }

    // --- Live quantum -------------------------------------------------------------------------------

    @Test
    fun aBeatAndHalfABarAndWholeBarsDuringATake() {
        val p = Project()
        assertEquals(16, p.liveQuantumSteps)
        assertEquals(4, p.copy(liveQuantumFine = LiveSlots.QUANTUM_BEAT).liveQuantumSteps)
        assertEquals(8, p.copy(liveQuantumFine = LiveSlots.QUANTUM_HALF_BAR).liveQuantumSteps)
        assertEquals(6, p.copy(liveQuantumFine = LiveSlots.QUANTUM_HALF_BAR, beatsPerBar = 3).liveQuantumSteps)
        assertEquals(16, p.copy(liveQuantumFine = LiveSlots.QUANTUM_BEAT).liveLaunchSteps(taking = true))
        assertEquals(32, p.copy(liveQuantum = 2).liveLaunchSteps(taking = true))
        assertEquals(0, p.copy(liveQuantumFine = 7).sanitized().liveQuantumFine)
        assertEquals(5, LIVE_QUANTUM_CHOICES.size)
    }

    // --- Takes of an audio track ---------------------------------------------------------------------

    @Test
    fun everyTakeIsKeptAndCanBeChosen() {
        var p = Project().addAudioTrack()!!
        listOf("a.wav", "b.wav", "c.wav").forEachIndexed { i, f ->
            p = p.withTake(PendingTake(0, f, "Prise $i", 4.0, 120.0, replaces = p.audioTracks[0].clip?.file))
        }
        val t = p.audioTracks[0]
        assertEquals("c.wav", t.clip!!.file)
        assertEquals(listOf("b.wav", "a.wav"), t.otherTakes.map { it.file })
        assertEquals(setOf("a.wav", "b.wav"), p.previousTakeFiles)
        val chosen = p.chooseTake(0, 1)
        assertEquals("a.wav", chosen.audioTracks[0].clip!!.file)
        assertEquals(listOf("c.wav", "b.wav"), chosen.audioTracks[0].otherTakes.map { it.file })
        assertEquals(chosen.audioTracks, ProjectJson.decode(ProjectJson.encode(chosen)).audioTracks)
        // At most MAX_TAKES in all.
        repeat(AudioTrack.MAX_TAKES + 2) { i -> p = p.withTake(PendingTake(0, "x$i.wav", "x", 4.0, 120.0, replaces = p.audioTracks[0].clip?.file)) }
        assertEquals(AudioTrack.MAX_TAKES - 1, p.audioTracks[0].otherTakes.size)
    }
}
