package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TracksTest {
    private val c4 = Note(0, 24, 60)
    private val g4 = Note(48, 24, 67)

    @Test
    fun addsUpToEightTracksWithFreeIdsAndColours() {
        var p = Project()
        repeat(7) { p = p.addTrack()!! }
        assertEquals((1..7).toList(), p.tracks.map { it.id })
        assertEquals(8, p.instrumentTracks.map { it.color }.distinct().size)
        assertNull(p.addTrack())
        // A freed id is used again.
        p = p.removeTrack(3)
        assertEquals(3, p.addTrack()!!.tracks.first { it.id !in p.tracks.map { t -> t.id } }.id)
    }

    @Test
    fun eachTrackKeepsItsOwnSoundAndNotes() {
        var p = Project().addTrack("Basse")!!
        val id = p.tracks.single().id
        p = p.withTrack(p.track(id).copy(engine = 1, notes = NoteClip(notes = listOf(g4))))
        p = p.withTrack(p.track(0).copy(name = "Lead", notes = NoteClip(notes = listOf(c4))))
        assertEquals("Lead", p.synthTrackName)
        assertEquals(listOf(c4), p.synthNotes.notes)
        assertEquals(listOf(g4), p.track(id).notes.notes)
        // The view of a track puts its sound where the synth screen looks.
        val view = p.viewOfTrack(id)
        assertEquals(1, view.synthEngine)
        assertEquals(listOf(g4), view.synthNotes.notes)
        assertEquals(p, p.viewOfTrack(0))
    }

    @Test
    fun scenesKeepEachTracksNotes() {
        var p = Project().addTrack()!!
        val id = p.tracks.single().id
        p = p.withTrack(p.track(id).copy(notes = NoteClip(notes = listOf(c4)))).sanitized()
        p = p.switchScene(1)
        assertTrue(p.track(id).notes.notes.isEmpty())
        p = p.withTrack(p.track(id).copy(notes = NoteClip(notes = listOf(g4)))).sanitized()
        assertEquals(listOf(c4), p.switchScene(0).track(id).notes.notes)
        assertEquals(listOf(g4), p.switchScene(0).switchScene(1).track(id).notes.notes)
        // Deleting the track forgets it in every scene.
        val gone = p.removeTrack(id).sanitized()
        assertTrue(gone.scenes.all { it.trackNotes.isEmpty() })
    }

    @Test
    fun aLongerTrackMakesTheSceneLonger() {
        var p = Project().addTrack()!!
        val id = p.tracks.single().id
        p = p.withTrack(p.track(id).copy(notes = NoteClip(steps = 64))).sanitized()
        assertEquals(64, p.currentSceneContent().steps)
    }

    @Test
    fun duplicateCopiesNotesAndMixer() {
        var p = Project().addTrack()!!
        p = p.withTrack(p.track(0).copy(notes = NoteClip(notes = listOf(c4))))
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == 2) c.copy(volume = 0.3f) else c }))
        val d = p.duplicateTrack(0)!!
        val copy = d.tracks.last()
        assertEquals(listOf(c4), copy.notes.notes)
        assertEquals(0.3f, d.mixer.channels[InstrumentTrack.mixerChannel(copy.id)].volume)
        assertNotEquals(p.tracks.first().id, copy.id)
    }

    @Test
    fun savedAndReadBack() {
        var p = Project().addTrack("Cordes")!!
        val id = p.tracks.single().id
        p = p.withTrack(p.track(id).copy(instrument = InstrumentChoice(0, 48, "Cordes"), notes = NoteClip(notes = listOf(c4)))).sanitized()
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
        // Bad values from disk are cleaned: duplicate or impossible ids, long names.
        val bad = p.copy(tracks = p.tracks + p.tracks + InstrumentTrack(9, "x") + InstrumentTrack(2, "y".repeat(80), color = 99)).sanitized()
        assertEquals(listOf(id, 2), bad.tracks.map { it.id })
        assertEquals(InstrumentTrack.MAX_NAME, bad.tracks[1].name.length)
        assertEquals(InstrumentTrack.COLORS - 1, bad.tracks[1].color)
    }

    @Test
    fun anOldProjectHasOneTrack() {
        val p = ProjectJson.decode("""{"name":"Ancien"}""")
        assertEquals(1, p.instrumentTracks.size)
        assertEquals("Synthé", p.track(0).name)
    }
}
