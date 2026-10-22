package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private typealias SP = StarterWizard.Part

/** The sounds the assistant picks: a style's own pack sound first, a synth that suits it without the pack. */
class StyleSoundsTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    /** The notes of the first track (the only part given) in the first section that plays them. */
    private fun Project.firstNotes(): List<Note> = allScenes.map { it.synthNotes.notes }.firstOrNull { it.isNotEmpty() } ?: track(0).notes.notes

    @Test
    fun aPackBassFallsBackToAGlidingSynthAndComesFirstWhenThePackIsThere() {
        val fallbacks = mapOf(
            "tech_house" to "Basse ronde", "uk_garage" to "Basse ronde", "house" to "Basse ronde",
            "neurofunk" to "Basse qui parle", "techno" to "Basse analogique",
        )
        for ((id, synth) in fallbacks) {
            val s = style(id)
            // Without the pack (most phones): the recipe's own synth bass (the round one glides, unlike a Plaits bass).
            val first = StarterWizard.soundsFor(SP.Bass, s).first()
            assertEquals(id, synth, first.name)
            assertTrue(id, first.sampled == null && (synth != "Basse ronde" || first.plaits == null))
            // With it: the style's own pack bass first.
            val packId = s.melody.packSounds.getValue(SP.Bass).substringBefore(':')
            val pack = PackManifest(packId, "Pack", sounds = listOf(PackSound("bass_01.flac", "bass", "Basse 1")))
            val withPack = StarterWizard.soundsFor(SP.Bass, s, listOf(pack)).first()
            assertEquals(id, "pack:$packId/bass_01.flac", withPack.sampled?.file)
        }
        // Tech house: the gliding round bass slides into the octave at the end of its two bars.
        val tech = style("tech_house")
        val p = StarterWizard.build(tech, emptyMap(), tech.melody.mood, mapOf(SP.Bass to StarterWizard.soundsFor(SP.Bass, tech).first()))
        assertEquals(StarterWizard.BASS_GLIDE, p.track(0).synth.get(SynthPatch.GLIDE))
    }

    @Test
    fun aPackChordSamplePlaysOneNotePerChord() {
        val gabber = style("gabber")
        val pack = PackManifest("gabber", "Gabber", sounds = listOf(PackSound("stab_01.flac", "stab", "Stab 1")))
        val stab = StarterWizard.soundsFor(SP.Pad, gabber, listOf(pack)).first()
        assertTrue(stab.chord && stab.sampled != null)
        val placed = stab.copy(sampled = stab.sampled!!.copy(file = "abc.wav"))
        val notes = StarterWizard.build(gabber, emptyMap(), gabber.melody.mood, mapOf(SP.Pad to placed)).firstNotes()
        assertTrue(notes.isNotEmpty())
        // One note per hit: a chord recorded in the sample is not stacked into a chord of chords.
        assertEquals(notes.size, notes.map { it.start }.toSet().size)
        // A synth plays the style's own voicing (octaves here: two notes per hit).
        val synth = StarterWizard.soundsFor(SP.Pad, gabber).first { it.sampled == null }
        val voiced = StarterWizard.build(gabber, emptyMap(), gabber.melody.mood, mapOf(SP.Pad to synth)).firstNotes()
        assertEquals(2 * voiced.map { it.start }.toSet().size, voiced.size)
        // A lead or a bass sample is never a chord.
        val bassPack = PackManifest("tech_house", "Tech", sounds = listOf(PackSound("bass_01.flac", "bass", "Basse 1")))
        assertTrue(StarterWizard.soundsFor(SP.Bass, style("tech_house"), listOf(bassPack)).none { it.chord })
    }

    @Test
    fun theRetroTechnoHooverDivesAnOctave() {
        val retro = style("retro_techno")
        assertTrue(retro.melody.leadGlide && '/' in retro.melody.leadPattern)
        val hoover = StarterWizard.soundsFor(SP.Lead, retro).first()
        assertEquals("Hoover", hoover.name)
        val p = StarterWizard.build(retro, emptyMap(), retro.melody.mood, mapOf(SP.Lead to hoover))
        assertEquals(StarterWizard.BASS_GLIDE, p.track(0).synth.get(SynthPatch.GLIDE))
        val notes = p.firstNotes().sortedBy { it.start }
        // The octave held into the root: they overlap a little and the synth glides down.
        assertTrue(notes.zipWithNext().any { (a, b) -> a.pitch - b.pitch == 12 && a.end > b.start })
        assertTrue(StarterWizard.isLine(notes))
        // A style whose lead does not glide keeps its synth as it was.
        val gabber = style("gabber")
        val lead = StarterWizard.soundsFor(SP.Lead, gabber).first()
        assertEquals(0f, StarterWizard.build(gabber, emptyMap(), gabber.melody.mood, mapOf(SP.Lead to lead)).track(0).synth.get(SynthPatch.GLIDE))
    }
}
