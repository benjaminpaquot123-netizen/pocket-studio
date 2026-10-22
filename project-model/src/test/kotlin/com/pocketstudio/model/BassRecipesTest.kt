package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Mood
import com.pocketstudio.model.StarterWizard.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the recipes of scripts/packs/recipes/bass.json promise (bass music, synthwave, pop). */
class BassRecipesTest {
    private val ids = listOf("dnb", "liquid_dnb", "neurofunk", "jungle", "jump_up", "dubstep", "riddim", "synthwave", "pop")

    private fun style(id: String) = StarterWizard.style(id)!!

    private fun bassNotes(style: StarterWizard.Style, mood: Mood): List<Note> =
        StarterWizard.startingNotes(Part.Bass, style, mood, StarterWizard.harmonyOf(style, mood)).notes

    @Test
    fun noBassFallsUnderThePhonesReach() {
        // The bass sounds add a sub an octave below the note: a note under B1 (62 Hz) puts it
        // under 31 Hz, where nobody hears it and a phone plays nothing.
        for (id in ids) {
            val style = style(id)
            for (mood in style.melody.progressions.keys) {
                val lowest = bassNotes(style, mood).minOf { it.pitch }
                assertTrue("$id $mood: lowest bass note $lowest", lowest >= 35)
            }
        }
    }

    @Test
    fun theDubstepBassWobblesInTime() {
        val dubstep = style("dubstep")
        val sound = StarterWizard.soundsFor(Part.Bass, dubstep).first()
        assertEquals("Basse wobble", sound.name)
        // Its filter sweep follows the tempo (restarted on each note), deep enough to be heard.
        assertTrue(sound.synth.values[SynthPatch.LFO2_SYNC] > 0f)
        assertTrue(sound.synth.values[SynthPatch.SWEEP] >= 0.5f)
        // Long notes, so that each one wobbles several times.
        assertTrue(bassNotes(dubstep, dubstep.melody.mood).any { it.length >= 8 * NoteClip.TICKS_PER_STEP })
    }

    @Test
    fun theRiddimIsADrumAndBassFormulaWithItsSilences() {
        val riddim = style("riddim")
        assertEquals(setOf(Part.Bass), StarterWizard.defaultParts(riddim))
        assertEquals(0.0, riddim.swing, 0.0)
        // The bass keeps quiet under the snare of every bar.
        val snare = riddim.lines.first { it.role == StarterWizard.Role.Snare }.pattern.indexOf('X')
        val step = NoteClip.TICKS_PER_STEP
        for (bar in 0 until 4) {
            val at = (bar * 16 + snare) * step
            assertTrue("bar $bar", bassNotes(riddim, riddim.melody.mood).none { it.start <= at && at < it.end })
        }
    }

    @Test
    fun popIsHappyAndMakesASongWithItsHook() {
        val pop = style("pop")
        assertEquals(Mood.Happy, pop.melody.mood)
        assertEquals(Scale.Major to listOf(0, 4, 5, 3), pop.melody.progressions[Mood.Happy])
        assertEquals(StarterWizard.SongForm.Song, pop.form)
        assertEquals(setOf(Part.Bass, Part.Pad, Part.Lead), StarterWizard.defaultParts(pop))
        // The hook comes in at the Refrain, the snare doubles the clap from the Couplet on.
        val plan = StarterWizard.planFor(pop, pop.lines.map { it.role }.toSet(), StarterWizard.defaultParts(pop), acid = false)
        assertEquals(setOf(2), plan.parts[Part.Lead])
        assertEquals(setOf(1, 2), plan.lines[StarterWizard.Role.Snare])
    }
}
