package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** The house family's recipes (scripts/packs/recipes/house.json): each style sounds like itself. */
class HouseStylesTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    private fun beat(s: StarterWizard.Style): Map<StarterWizard.Role, Map<Int, Hit>> {
        val steps = StarterWizard.drumSteps(s).coerceAtLeast(32)
        return s.lines.associate { it.role to StarterWizard.hits(it, steps) }
    }

    @Test
    fun houseTechHouseAfroHouseAndDiscoHaveTheirOwnBeat() {
        val ids = listOf("house", "tech_house", "afro_house", "disco")
        // Not only the percussion: the hats (or what stands for them) differ from one style to the next.
        for (a in ids) for (b in ids) if (a < b) {
            val (x, y) = beat(style(a)) to beat(style(b))
            val hats = listOf(StarterWizard.Role.Hat, StarterWizard.Role.OpenHat, StarterWizard.Role.Snare, StarterWizard.Role.Clap)
            assertTrue("$a and $b share their beat", hats.any { x[it]?.keys != y[it]?.keys })
            assertTrue("$a and $b share their hats", x[StarterWizard.Role.Hat]?.keys != y[StarterWizard.Role.Hat]?.keys)
        }
        // Disco has a drummer's snare, house a clap; afro house a shaker and congas, and only a discreet open hat.
        assertTrue(style("disco").lines.any { it.role == StarterWizard.Role.Snare })
        val afro = style("afro_house")
        assertTrue(afro.lines.filter { it.role == StarterWizard.Role.OpenHat }.all { 'X' !in it.pattern })
        assertEquals(VOICE_SHAKER, afro.lines.single { it.role == StarterWizard.Role.Hat }.voice)
        assertEquals(VOICE_CONGA, afro.lines.single { it.role == StarterWizard.Role.Perc }.voice)
        assertEquals(VOICE_COWBELL, style("acid_house").lines.single { it.role == StarterWizard.Role.Perc }.voice)
    }

    @Test
    fun amapianoPlaysARealLogDrumBetweenTheKicks() {
        val s = style("amapiano")
        val bass = StarterWizard.soundsFor(StarterWizard.Part.Bass, s).first()
        assertEquals("Log drum", bass.name)
        assertTrue(bass.plaits != null)
        // Every log drum note starts off the kicks (on the same beat the bounce flattens).
        val kicks = s.lines.single { it.role == StarterWizard.Role.Kick }.pattern.indices.filter { s.lines[0].pattern[it] != '.' }.toSet()
        val p = StarterWizard.build(s, mapOf(StarterWizard.Role.Kick to null), s.melody.mood, mapOf(StarterWizard.Part.Bass to bass))
        // The bass is the first track: its notes, in the sections where it plays.
        val notes = p.allScenes.map { it.synthNotes.notes }.first { it.isNotEmpty() }
        assertTrue(notes.size >= 16)
        assertTrue(notes.none { (it.start / NoteClip.TICKS_PER_STEP) % 16 in kicks })
        assertEquals(StarterWizard.Part.Bass, s.melody.signature)
    }

    @Test
    fun theBassesStayAudible() {
        // A synth bass adds a sub-oscillator (or a second oscillator) an octave down: it must stay above
        // ~30 Hz, where a club still hears it and a phone shows it through its overtones.
        for (id in listOf("house", "deep_house", "tech_house", "uk_garage", "speed_garage", "acid_house", "afro_house", "disco", "amapiano")) {
            val s = style(id)
            val sound = StarterWizard.soundsFor(StarterWizard.Part.Bass, s).first()
            for (mood in s.melody.progressions.keys) {
                val notes = StarterWizard.startingNotes(StarterWizard.Part.Bass, s, mood, StarterWizard.harmonyOf(s, mood)).notes
                val lowest = notes.minOf { it.pitch } - if (sound.plaits == null && sound.sampled == null) 12 else 0
                val hz = 440.0 * 2.0.pow((lowest - 69) / 12.0)
                assertTrue("$id $mood: ${"%.1f".format(hz)} Hz", hz >= 30.0)
            }
        }
    }

    @Test
    fun theStylesThatAreJoyfulStartJoyful() {
        for (id in listOf("house", "disco", "uk_garage", "amapiano")) assertEquals(id, StarterWizard.Mood.Happy, style(id).melody.mood)
        // The speed garage bass talks and glides where its line slides.
        val speed = style("speed_garage")
        assertEquals("Basse qui parle", speed.melody.bass)
        assertTrue(speed.melody.bassGlide && '/' in speed.melody.bassPattern)
    }

    private companion object {
        const val VOICE_COWBELL = 11
        const val VOICE_SHAKER = 13
        const val VOICE_CONGA = 14
    }
}
