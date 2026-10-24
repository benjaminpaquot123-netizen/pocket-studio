package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "+ Piste": a track of a kind, named for it, with a suited sound, and a melody written on request. */
class NewTracksTest {
    private val house = StarterWizard.style("house")!!

    private fun added(before: Project, after: Project) = after.tracks.first { t -> before.tracks.none { it.id == t.id } }

    @Test
    fun eachKindIsNamedForItAndSoundsLikeTheStylesPart() {
        val p = Project()
        TrackKind.entries.forEach { kind ->
            val next = NewTracks.add(p, kind, house)!!
            val t = added(p, next)
            assertEquals(kind.trackName, t.name)
            assertTrue(t.notes.notes.isEmpty())
        }
        // The style's own sounds, as the assistant gives them.
        assertEquals(StarterWizard.soundsFor(StarterWizard.Part.Bass, house).first().name, SoundLibrary.synthNameOf(added(p, NewTracks.add(p, TrackKind.Bass, house)!!).synth))
        assertEquals(StarterWizard.soundsFor(StarterWizard.Part.Pad, house).first().name, NewTracks.sound(TrackKind.Chords, house).name)
        assertEquals(StarterWizard.soundsFor(StarterWizard.Part.Lead, house).first().name, NewTracks.sound(TrackKind.Melody, house).name)
        // Without a style: the first of the family.
        assertEquals(SoundLibrary.SYNTH.first { it.category == "Basses" }.name, NewTracks.sound(TrackKind.Bass, null).name)
        assertEquals(SoundLibrary.SYNTH.first { it.category == "Pluck" }.name, NewTracks.sound(TrackKind.Pluck, null).name)
        // Kinds sound different from one another.
        assertNotEquals(NewTracks.sound(TrackKind.Bass, house), NewTracks.sound(TrackKind.Chords, house))
        // The arpeggio plays the chords held, note by note.
        assertEquals(ArpMode.UpDown, added(p, NewTracks.add(p, TrackKind.Arp, house)!!).notes.performance.arp)
        // A gliding style's bass glides.
        val trap = StarterWizard.STYLES.first { it.melody.bassGlide }
        assertEquals(StarterWizard.BASS_GLIDE, added(p, NewTracks.add(p, TrackKind.Bass, trap)!!).synth.get(SynthPatch.GLIDE), 0.001f)
    }

    @Test
    fun noSoundNeedsAFile() {
        StarterWizard.STYLES.forEach { style ->
            TrackKind.entries.forEach { kind -> assertNull("${style.id} $kind", NewTracks.sound(kind, style).sampled) }
        }
    }

    @Test
    fun aMelodyWrittenForMeIsFourBarsInTheSongsKey() {
        val inKey = Project(harmony = Harmony(2, Scale.Minor, lock = true))  // D minor
        val next = NewTracks.add(inKey, TrackKind.Melody, house, write = true)!!
        val t = added(inKey, next)
        assertEquals("Mélodie", t.name)
        assertEquals(StarterWizard.PHRASE_STEPS, t.notes.steps)
        assertTrue(t.notes.notes.size >= 8)
        t.notes.notes.forEach { assertTrue("${it.pitch}", inKey.harmony.inScale(it.pitch)) }
        assertEquals(inKey.harmony, next.harmony)
        // A song without a key (the assistant without a melody) gets the style's.
        val free = Project(name = "House")
        val written = NewTracks.add(free, TrackKind.Melody, house, write = true)!!
        assertEquals(StarterWizard.progressionOf(house, house.melody.mood).first, written.harmony.scale)
        assertEquals(house.melody.root, written.harmony.root)
        added(free, written).notes.notes.forEach { assertTrue(written.harmony.inScale(it.pitch)) }
        // A style that arpeggiates still gets a written riff (no arpeggiator), and so does no style.
        val arpStyle = StarterWizard.STYLES.first { it.melody.leadPattern.isBlank() }
        listOf(arpStyle, null).forEach { s ->
            val m = added(free, NewTracks.add(free, TrackKind.Melody, s, write = true)!!)
            assertEquals(ArpMode.Off, m.notes.performance.arp)
            assertTrue(m.notes.notes.isNotEmpty())
        }
    }

    @Test
    fun noTrackPastTheEighth() {
        var p = Project()
        repeat(InstrumentTrack.MAX - 1) { p = NewTracks.add(p, TrackKind.Melody, null)!! }
        assertNull(NewTracks.add(p, TrackKind.Melody, null))
    }

    @Test
    fun theNamesAreSavedInFrenchAndShownTranslated() {
        val en = com.pocketstudio.model.i18n.I18n.table(com.pocketstudio.model.i18n.Lang.EN)
        try {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.EN
            assertEquals("Melody", DefaultNames.shown("Mélodie"))
            assertEquals("Chords", DefaultNames.shown("Accords"))
            assertEquals(en["Arpège"], DefaultNames.shown("Arpège"))
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        }
    }
}
