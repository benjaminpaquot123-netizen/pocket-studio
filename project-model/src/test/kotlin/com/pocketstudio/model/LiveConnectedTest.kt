package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 4f (Benjamin's n° 11): what the new Live puts in its cells (a recorded cell in a new brick,
 * a library sound on an audio track of a brick, a copied cell) goes through a performance take
 * into the song as it was heard, so the song plays and exports the same; and it is kept on disk.
 */
class LiveConnectedTest {
    private val notes = NoteClip(notes = listOf(Note(0, 24, 60)))

    /** A: a kick and a synth note; nothing else (bricks B and C are new rows of the grid). */
    private fun project(): Project = Project(
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), synthNotes = notes)),
        scene = 0, drums = DrumPattern().toggle(0, 0), synthNotes = notes,
    ).sanitized()

    /** Brick B: notes recorded into its synth cell; brick C: a library loop on an audio track; A's drums copied onto C. */
    private fun filled(): Project {
        val played = listOf(PlayedMemory.KeptNote(0, 64, 0.8f, 32.0, 2.0))
        var p = assertNotNullAnd(project().withRecordedCell(SongPart.inst(0), 1, played, emptyList(), 32.0, 48.0))
        p = assertNotNullAnd(p.withAudioInCell(SongPart.DRUMS, 2, AudioClip("loop.wav", "Ma boucle", seconds = 2.0, loopSteps = 16, bpm = 113.0, fit = true)))
        p = assertNotNullAnd(p.withCopiedCell(SongPart.DRUMS, 0, SongPart.DRUMS, 2))
        return p.sanitized()
    }

    @Test
    fun cellsMadeOnTheLiveGoThroughATakeIntoTheSongAsHeard() {
        val p = filled()
        assertEquals(3, p.allScenes.size)
        val audio = p.audioTracks.single { it.clip != null }
        // Row A from 0, row B from bar 2, row C from bar 3; the take ends at bar 4.
        val take = PerformanceTake(0, LiveState.on(0, 0))
        for (k in 0 until LiveSlots.COUNT) take.launched(k, 1, 16)
        for (k in 0 until LiveSlots.COUNT) take.launched(k, 2, 32)
        val w = p.writeTake(take, 48, append = false)
        val song = w.project.song
        assertEquals(listOf(0, 1, 2), song.map { it.scene })
        // B plays the recorded notes, C the copied kick and the loop (only there: it belongs to C).
        assertEquals(listOf(64), w.project.partView(song[1]).synthNotes.notes.map { it.pitch })
        assertTrue(w.project.partView(song[2]).drums.lanes[0].hits.isNotEmpty())
        assertFalse(w.project.audioPlays(audio.id, song[0]))
        assertFalse(w.project.audioPlays(audio.id, song[1]))
        assertTrue(w.project.audioPlays(audio.id, song[2]))
        // It still follows the project's tempo there (stretched as live).
        assertTrue(w.project.audioTracks.single { it.id == audio.id }.clip!!.follows)
    }

    @Test
    fun theNewLivesSettingsAreKeptOnDisk() {
        val p = filled().copy(
            liveRows = 12,
            tempoBpm = 113.0,
            tempoLead = TempoLead("Boys", 113.0),
            live = filled().live.with(0, LiveClip("boys.wav", "Boys", beats = 8, bpm = 113.0)).copy(size = 12),
        ).sanitized()
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(12, back.liveRowsShown)
        assertEquals(TempoLead("Boys", 113.0), back.tempoLead)
        assertEquals(113.0, back.tempoBpm, 0.0)
        assertEquals(12, back.live.size)
        assertEquals(113.0, back.live.clips[0]!!.bpm, 0.0)
        assertTrue(back.live.clips[0]!!.follows)
        assertEquals(p.allScenes, back.allScenes)
        assertEquals(p.audioTracks, back.audioTracks)
    }

    private fun <T : Any> assertNotNullAnd(v: T?): T {
        assertNotNull(v)
        return v!!
    }
}
