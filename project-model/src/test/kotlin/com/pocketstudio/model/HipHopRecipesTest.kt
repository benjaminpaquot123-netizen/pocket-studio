package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private typealias HR = StarterWizard.Role
private typealias HP = StarterWizard.Part

/** The rap and urban styles (scripts/packs/recipes/hiphop.json) do what their cards promise. */
class HipHopRecipesTest {
    private val ids = listOf("boom_bap", "trap", "drill", "lofi", "phonk", "grime", "jersey_club", "reggaeton", "afrobeats")
    private fun style(id: String) = StarterWizard.style(id)!!
    private fun tracks(id: String, mood: StarterWizard.Mood = style(id).melody.mood): Project {
        val s = style(id)
        val parts = StarterWizard.defaultParts(s).associateWith { StarterWizard.soundsFor(it, s).first() }
        return StarterWizard.build(s, s.lines.associate { it.role to null }, mood, parts).switchScene(2)
    }

    @Test
    fun the808sSlide() {
        // Trap, drill, phonk, reggaeton: the bass glides and its line says where (its notes overlap there).
        for (id in listOf("trap", "drill", "phonk", "reggaeton")) {
            val s = style(id)
            assertTrue(id, s.melody.bassGlide && '/' in s.melody.bassPattern)
            val bass = tracks(id).instrumentTracks.first { it.name == "Basse" }
            assertTrue(id, StarterWizard.isLine(bass.notes.notes))
            assertFalse(id, SampledPatch.isMonophonic(bass.notes.notes))
            assertEquals(id, StarterWizard.BASS_GLIDE, bass.synth.get(SynthPatch.GLIDE))
        }
    }

    @Test
    fun eachStyleHasItsSignatureAndSaysWhatItsPartsDo() {
        for (id in ids) {
            val s = style(id)
            val sig = s.melody.signature
            assertTrue(id, sig != null && sig in s.melody.parts)
            for (p in s.melody.parts) assertTrue("$id $p", !s.melody.tips[p].isNullOrBlank())
            // The signature plays in every section of the song.
            val plan = StarterWizard.planFor(s, s.lines.map { it.role }.toSet(), s.melody.parts, acid = false)
            assertEquals(id, (0 until plan.count).toSet(), plan.parts[sig])
        }
        // Only what the genre wants: no chords in phonk and jersey club, no lead in boom bap, lo-fi, grime, reggaeton.
        for (id in listOf("phonk", "jersey_club")) assertFalse(id, HP.Pad in style(id).melody.parts)
        for (id in listOf("boom_bap", "lofi", "grime", "reggaeton")) assertFalse(id, HP.Lead in style(id).melody.parts)
    }

    @Test
    fun theSignatureSoundsAreTheRightOnes() {
        // Phonk: the 808 cowbell of the kit plays the melody, distorted 808 under it.
        val phonk = StarterWizard.soundsFor(HP.Lead, style("phonk")).first()
        assertEquals(StarterWizard.KIT_MELODIC.keys.single(), phonk.kitVoice)
        assertTrue(style("phonk").effects.any { it.target == "bass" && it.preset == "distortion" })
        // Lo-fi: the crackle of an old record on the Rhodes; boom bap: the warm vinyl.
        assertTrue(style("lofi").effects.any { it.target == "pad" && it.insert == EffectType.VINYL.name })
        assertTrue(style("boom_bap").effects.any { it.target == "pad" && it.preset == "warm_vinyl" })
        // Reggaeton: the lead is no longer the stabs' own sound.
        assertNotEquals(style("reggaeton").melody.pad, style("reggaeton").melody.lead)
        // Afrobeats: a shaker, a rim and congas (built-in voices without the pack), a happy song first.
        val afro = style("afrobeats")
        assertEquals(13, afro.lines.first { it.role == HR.Hat }.voice)
        assertEquals(10, afro.lines.first { it.role == HR.Snare }.voice)
        assertEquals(14, afro.lines.first { it.role == HR.Perc }.voice)
        assertTrue(afro.lines.first { it.role == HR.Snare }.pattern.none { it == 'X' })
        assertEquals(StarterWizard.Mood.Happy, afro.melody.mood)
        // Lo-fi's shaker is one; drill's and grime's clicks are rims.
        assertEquals(13, style("lofi").lines.first { it.role == HR.Perc }.voice)
        assertEquals(10, style("drill").lines.first { it.role == HR.Perc }.voice)
    }

    @Test
    fun theHatRollsAreRolls() {
        for (id in listOf("trap", "drill", "phonk")) {
            val hats = style(id).lines.first { it.role == HR.Hat }
            assertTrue(id, StarterWizard.hits(hats.pattern).values.any { it.repeats > 1 })
        }
    }

    @Test
    fun drillIsInHarmonicMinorWithAMajorFifthChord() {
        val drill = style("drill")
        val (scale, degrees) = drill.melody.progressions.getValue(StarterWizard.Mood.Dark)
        assertEquals(Scale.HarmonicMinor, scale)
        val h = Harmony(drill.melody.root, scale)
        val v = h.voiced(degrees.last(), 48, Voicing.Triad)
        assertEquals(4, v[1] - v[0])
    }

    @Test
    fun noNinthChordRubsAMinorNinth() {
        // A 9th voicing on a iii gives a minor ninth (La-Do-Mi-Sol-Si♭): jazz pianists avoid it.
        for (id in ids) {
            val s = style(id)
            if (s.melody.voicing != Voicing.Ninth) continue
            for ((mood, prog) in s.melody.progressions) {
                val h = Harmony(s.melody.root, prog.first)
                for (d in prog.second) {
                    val chord = h.voiced(d, 48, Voicing.Ninth)
                    assertNotEquals("$id $mood degree $d", 13, chord.last() - chord.first())
                }
            }
        }
    }

    @Test
    fun hipHopComesInBenjaminsOrder() {
        assertEquals(listOf("trap", "drill", "phonk", "boom_bap", "grime", "jersey_club", "lofi"), StarterWizard.stylesOf("Hip-hop").map { it.id })
    }
}
