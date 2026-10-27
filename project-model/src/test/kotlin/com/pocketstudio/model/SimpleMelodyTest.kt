package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit final (01 M3, 05 #5): the Mode simple's « Enregistrer ma mélodie » wrote over the
 * assistant's riff (in one part only), or into the bass of a song without a lead. Its melody now
 * has a track of its own, heard through the song.
 */
class SimpleMelodyTest {
    private val house = StarterWizard.style("house")!!
    private val song = StarterWizard.build(
        house, house.lines.associate { it.role to null as PadSample? }, house.melody.mood,
        (StarterWizard.defaultParts(house) + StarterWizard.Part.Lead).associateWith { StarterWizard.soundsFor(it, house).first() },
    )

    private fun added(before: Project, after: Project) = after.tracks.first { t -> before.tracks.none { it.id == t.id } }

    @Test
    fun aMelodyTrackOfItsOwnSoundingAsTheLead() {
        assertNull(SimpleMelody.trackOf(song))
        val lead = song.tracks.last()
        val next = SimpleMelody.add(song, lead.id, house)!!
        val mine = added(song, next)
        assertEquals(SimpleMelody.NAME, mine.name)
        assertEquals(mine.id, SimpleMelody.trackOf(next))
        assertEquals("the lead's sound", lead.synth, mine.synth)
        assertEquals(lead.engine, mine.engine)
        assertEquals("its notes are the user's", false, mine.followChords)
        // Empty in every part, the lead's riff untouched.
        next.allScenes.indices.forEach { s ->
            assertTrue("scene $s", next.sceneView(s).track(mine.id).notes.notes.isEmpty())
            assertEquals("scene $s", song.sceneView(s).track(lead.id).notes, next.sceneView(s).track(lead.id).notes)
        }
    }

    @Test
    fun withoutALeadItIsAMelodyNotTheBass() {
        val p = Project()
        val next = SimpleMelody.add(p, null, house)!!
        val mine = added(p, next)
        assertEquals(SimpleMelody.NAME, mine.name)
        assertEquals(added(p, NewTracks.add(p, TrackKind.Melody, house)!!).synth, mine.synth)
    }

    @Test
    fun aTakeIsHeardThroughTheSong() {
        val withTrack = SimpleMelody.add(song, song.tracks.last().id, house)!!
        val id = SimpleMelody.trackOf(withTrack)!!
        val scene = withTrack.song[1].scene
        // A note recorded in the second part's scene.
        val inScene = withTrack.switchScene(scene).let { q -> q.withTrack(q.track(id).copy(notes = q.track(id).notes.copy(notes = listOf(Note(0, 24, 72, 0.8f))))) }.switchScene(withTrack.scene)
        val spread = SimpleMelody.spread(inScene, id, scene)
        val key = SongPart.inst(id)
        spread.song.forEach { part ->
            assertEquals("part on scene ${part.scene}", scene, part.sceneFor(key))
            assertFalse(spread.sceneView(part.sceneFor(key)).track(id).notes.notes.isEmpty())
        }
        // Again: nothing more to change.
        assertEquals(spread, SimpleMelody.spread(spread, id, scene))
        // A part where the track has notes of its own keeps them.
        val other = spread.song.first { it.scene != scene }.scene
        val own = spread.switchScene(other).let { q -> q.withTrack(q.track(id).copy(notes = q.track(id).notes.copy(notes = listOf(Note(48, 24, 74, 0.8f))))) }.switchScene(spread.scene)
        val unspread = own.copy(song = own.song.map { it.copy(play = it.play - key) })
        val again = SimpleMelody.spread(unspread, id, scene)
        again.song.filter { it.scene == other }.forEach { assertEquals(other, it.sceneFor(key)) }
    }
}
