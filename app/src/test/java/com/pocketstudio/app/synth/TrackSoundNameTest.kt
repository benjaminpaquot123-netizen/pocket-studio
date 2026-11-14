package com.pocketstudio.app.synth

import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.guide.GuideSheets
import org.junit.Assert.assertEquals
import org.junit.Test
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.withTrack

/**
 * Audit ultra round 1, n° 36 (seat F a): « Orgue » chosen in « Changer le son », the track's card still said
 * « Synthé » (its kind, [InstrumentTrack.soundLabel]). The track bar and the Mode simple's « Le son » show
 * the sound's own name, as the Guide does ([GuideSheets.soundName]).
 */
class TrackSoundNameTest {
    @Test
    fun aLibrarySynthSoundIsShownByItsName() {
        val organ = SoundLibrary.SYNTH.first { it.name == "Orgue" }
        val t = InstrumentTrack(id = 0, name = "Mélodie", synth = organ.sound)
        assertEquals("Synthé", t.soundLabel)
        assertEquals("Orgue", GuideSheets.soundName(t))
        // What the two places show (SynthScreen's SimpleSoundCard and the TrackBar) reads this name.
        val src = listOf("src/main/java/com/pocketstudio/app/synth/SynthScreen.kt", "src/main/java/com/pocketstudio/app/synth/TrackBar.kt")
            .map { java.io.File(it).readText() }
        src.forEach { assertEquals(false, "tr(t.soundLabel)" in it) }
    }
}

/** n° 35 (seat D c): « Changer le son » of a lead track whose sound is its own opened on « Basses »; now on its family. */
class TrackSoundFamilyTest {
    @Test
    fun aTracksNameGivesTheFamilyToOpenOn() {
        assertEquals("Leads", com.pocketstudio.app.sounds.familyOfTrack("Lead"))
        assertEquals("Leads", com.pocketstudio.app.sounds.familyOfTrack("Mélodie"))
        assertEquals("Basses", com.pocketstudio.app.sounds.familyOfTrack("Basse 808"))
        assertEquals("Nappes", com.pocketstudio.app.sounds.familyOfTrack("Accords"))
        assertEquals(null, com.pocketstudio.app.sounds.familyOfTrack("Piste 3"))
        // Every family named is one of the synth's.
        listOf("Lead", "Basse", "Accords", "Arpège", "Piano").forEach { n ->
            assertEquals(true, com.pocketstudio.app.sounds.familyOfTrack(n) in com.pocketstudio.model.SoundCategories.SYNTH)
        }
    }
}

/** n° 35 (seat D c): « Basse en fantôme » ticked on a Synthé over a Basse track, no 303 line: no ghost at all. */
class BassGhostTest {
    @Test
    fun theBassTracksNotesAreTheGhostsWhenThereIsNoAcidLine() {
        var p = com.pocketstudio.model.Project(name = "")
        p = p.addTrack("Basse")!!
        val bass = p.tracks.last()
        val notes = listOf(0, 4, 8, 12).map { com.pocketstudio.model.Note(start = it * com.pocketstudio.model.NoteClip.TICKS_PER_STEP, length = 120, pitch = 36) }
        p = p.withTrack(bass.copy(notes = bass.notes.copy(notes = notes)))
        val ghosts = com.pocketstudio.app.ui.ghostNotes(p, 0)
        assertEquals(notes.map { it.start }, ghosts.map { it.start }.take(4))
        assertEquals(true, ghosts.isNotEmpty())
        // The bass's own roll shows no ghost of itself.
        assertEquals(emptyList<Int>(), com.pocketstudio.app.ui.ghostNotes(p, bass.id).map { it.start })
    }
}
