package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 9, lot gr-house2 (Benjamin's listening of the house family): the acid line no longer doubles the kick and
 * the acid song builds instead of starting all at once; house loses its dated sounds (the recurring vocal chop, the
 * 707 tambourine, the old arpeggio synth); tech house loses its clap echo and its dated intro; afro house no longer
 * starts like tech house and drops its little wood « toc » and high tom for real hand drums. Deep house is untouched.
 */
class HouseRetouchTest {
    private fun style(id: String) = StarterWizard.style(id)!!
    private val kickSteps = setOf(0, 4, 8, 12)

    private fun full(s: StarterWizard.Style): Project {
        val lines = s.lines.associate { it.role to null as PadSample? }
        return StarterWizard.build(s, lines, s.melody.mood, StarterWizard.Part.entries.toSet().associateWith { StarterWizard.soundsFor(it, s).first() })
    }

    private fun Project.hits(scene: Int, role: StarterWizard.Role) = allScenes[scene].drums.lanes[role.lane].hits.isNotEmpty()

    @Test
    fun theAcidLineStaysOffTheKick() {
        val acid = assertNotNull(style("acid_house").melody.acid).let { style("acid_house").melody.acid!! }
        val onKick = acid.pattern.withIndex().filter { (i, c) -> c != '.' && c != '-' && i % 16 in kickSteps }
        assertTrue("acid notes on the kick: ${onKick.map { it.index }}", onKick.isEmpty())
        val accentsOnKick = acid.accents.withIndex().filter { (i, c) -> c == 'x' && i % 16 in kickSteps }
        assertTrue("acid accents on the kick: ${accentsOnKick.map { it.index }}", accentsOnKick.isEmpty())
        val decay = acid.soundFrom(AcidSound()).decay
        assertTrue("a decay long enough to sing, not thump (got $decay)", decay >= 0.45f)
    }

    @Test
    fun theAcidSongBuildsInsteadOfStartingAllAtOnce() {
        val s = style("acid_house")
        assertTrue("a light transition: the Montée filters and fills instead of a noise riser", s.lightBuild)
        assertFalse(s.riser)
        assertTrue("the clap waits for the Drop (fewer layers start together)", StarterWizard.Role.Clap in s.drop.lines)
        val cutoff = s.melody.acid!!.moves.first { it.param == 0 }
        val groove = cutoff.sections.getValue(1)
        assertTrue("the 303 fades in from a closed filter in the Groove (got $groove)", groove.first <= 0.2f && groove.second > groove.first + 0.15f)
    }

    @Test
    fun houseLosesItsDatedSounds() {
        val s = style("house")
        assertNull("no vocal chop coming back every few bars", s.vocal)
        assertTrue("no 707 tambourine", s.kit.values.none { it == "tr707" })
        assertTrue("not the old arpeggio synth", s.melody.lead != "Arpège")
    }

    @Test
    fun techHouseStartsCleanAndClapsOnce() {
        val s = style("tech_house")
        val clap = s.lines.first { it.role == StarterWizard.Role.Clap }
        assertFalse("no clap echo", 'o' in clap.pattern)
        assertEquals("no clap-voice snap on the rim lane", null, s.lines.firstOrNull { it.role == StarterWizard.Role.Rim && it.voice == 4 })
        val p = full(s)
        for (r in listOf(StarterWizard.Role.Perc, StarterWizard.Role.Conga)) assertFalse("$r waits for the Drop", p.hits(0, r))
    }

    @Test
    fun afroHouseStartsOnItsHandDrums() {
        val s = style("afro_house")
        assertTrue("no high tom", s.lines.none { it.voice == 9 })
        assertTrue("no wood « toc »", s.lines.none { it.voice == 10 })
        assertTrue("the hand drums are real drums", s.kitSounds.values.count { it.startsWith("percussions_acoustiques:") } >= 2)
        assertFalse("its warm chords are there from the start, not held for the Drop", StarterWizard.Part.Pad in s.drop.parts)
        assertTrue("a light transition, not tech house's riser", s.lightBuild)
    }
}
