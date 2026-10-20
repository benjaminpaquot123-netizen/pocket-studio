package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    private fun project(): Project {
        var p = Project(
            drums = DrumPattern().toggle(0, 0),
            synthNotes = NoteClip(notes = listOf(Note(0, 24, 60))),
            song = listOf(SongPart(0, 2)),
        ).addTrack("Cordes")!!.addAudioTrack("Voix")!!
        p = p.withTrack(p.track(1).copy(notes = NoteClip(notes = listOf(Note(0, 24, 72)))))
        return p.sanitized()
    }

    @Test
    fun aPartPlaysOnlyItsTracks() {
        var p = project()
        p = p.togglePartTrack(0, SongPart.DRUMS).togglePartTrack(0, SongPart.inst(1)).togglePartTrack(0, SongPart.audio(0)).sanitized()
        val part = p.song[0]
        assertEquals(setOf("drums", "inst:1", "audio:0"), part.muted)
        val v = p.partView(part)
        assertTrue(v.drums.lanes.all { it.hits.isEmpty() })
        assertTrue(v.track(1).notes.notes.isEmpty())
        assertEquals(1, v.synthNotes.notes.size)  // the first track still plays
        assertFalse(part.plays(SongPart.audio(0)))
        // Toggling again brings the track back; the project itself is untouched.
        assertTrue(p.togglePartTrack(0, SongPart.DRUMS).song[0].plays(SongPart.DRUMS))
        assertEquals(1, p.drums.lanes[0].hits.size)
    }

    @Test
    fun mutesOfDeletedTracksAreForgotten() {
        var p = project().togglePartTrack(0, SongPart.inst(1)).sanitized()
        p = p.removeTrack(1).sanitized()
        assertTrue(p.song[0].muted.isEmpty())
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test
    fun threeFourBarsAndSceneLengths() {
        val p = Project(beatsPerBar = 3, drums = DrumPattern().withSteps(24))
        assertEquals(12, p.stepsPerBar)
        assertEquals(24, p.sanitized().drums.steps)
        assertEquals(24, p.sceneSteps(0))
        // A 16-step piano roll in 3/4 makes a scene of two 3/4 bars.
        assertEquals(24, p.copy(drums = DrumPattern().withSteps(12)).sceneSteps(0))
        assertEquals(7, Project(beatsPerBar = 11).sanitized().beatsPerBar)
    }

    @Test
    fun midiExportLeavesOutMutedTracksAndKeepsTheMeter() {
        val p = project().copy(beatsPerBar = 3).togglePartTrack(0, SongPart.DRUMS).sanitized()
        val content = MidiFile.read(MidiFile.export(p, song = true))
        assertTrue(content.notes.none { it.channel == MidiFile.DRUM_CHANNEL })
        // Two repeats of a two-bar 3/4 scene (24 steps): the 16-step clip starts at 0 and 16 in each.
        assertEquals(4, content.notes.count { it.channel == 0 })
    }
}
