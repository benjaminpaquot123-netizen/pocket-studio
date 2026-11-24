package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The house family's signature sound (grand remplacement v2, lots L9 and L2 tech house, `R/house.json`):
 * each style leads with recent landmark tracks, has a body (doubled chords, a texture or the default body),
 * its drum bus pushed (kick and bass in front) and a real chop where it has a voice; tech house keeps its
 * old clap and its approved effects; acid squeals and moves; amapiano's log drum sits on a background.
 */
class HouseSignatureTest {
    private val ids = listOf("house", "deep_house", "tech_house", "uk_garage", "speed_garage", "acid_house", "afro_house", "disco", "amapiano")
    private fun style(id: String) = StarterWizard.style(id)!!
    private fun year(ref: String) = Regex("""\((\d{4})\)""").find(ref)?.groupValues?.get(1)?.toInt() ?: 0

    @Test
    fun everyStyleSoundsModern() {
        for (id in ids) {
            val s = style(id)
            assertTrue("$id: a recent landmark first, got ${s.references.firstOrNull()}", year(s.references.first()) >= 2016)
            assertTrue("$id: at least 2 recent landmarks", s.references.count { year(it) >= 2016 } >= 2)
            val body = StarterWizard.withBody(s)
            assertTrue("$id: a body (doubled chords or a texture)", body.melody.chordDouble != null || body.melody.texture != null)
            val mix = assertNotNull("$id: kick and bass in front (its drum bus)", body.mix).let { body.mix!! }
            assertTrue("$id: the drum bus in parallel", (mix.glueBlend ?: 1f) < 1f)
            if (s.vocal != null) assertNotNull("$id: a real chop instead of the robot voice", s.vocalSample)
            assertTrue("$id: a high bed (shaker or hats in sixteenths)", body.lines.any { it.role == StarterWizard.Role.Shaker || it.voice == 13 })
        }
    }

    @Test
    fun techHouseKeepsItsClapAndItsEffects() {
        val s = style("tech_house")
        assertEquals("the old clap, frozen", "tech_house", s.kit[StarterWizard.Role.Clap])
        assertEquals("no file swapped in for the clap", null, s.kitSounds[StarterWizard.Role.Clap])
        assertTrue("no layer on the clap", s.lines.first { it.role == StarterWizard.Role.Clap }.layers.isEmpty())
        assertEquals(
            "the approved effects",
            listOf("bass" to "pump", "lead" to "", "clap" to "", "kick" to "punch"),
            s.effects.take(4).map { it.target to it.preset },
        )
        assertTrue("a conga in the high bed", s.lines.any { it.role == StarterWizard.Role.Conga })
        assertTrue("snaps on the turnarounds", s.lines.any { it.role == StarterWizard.Role.Snap })
        assertEquals("organ stabs doubled", StarterWizard.ChordDouble.Octave, s.melody.chordDouble)
    }

    @Test
    fun acidSquealsAndMoves() {
        val s = style("acid_house")
        val acid = assertNotNull(s.melody.acid).let { s.melody.acid!! }
        assertTrue("a modern tempo", s.bpm >= 128.0)
        assertTrue("its own 303 sound", acid.sound.isNotEmpty())
        assertTrue("knobs set on top of it", acid.knobs.size >= 4)
        assertTrue("accents", acid.accents.count { it == 'x' } >= 5)
        assertTrue("glides", acid.slides.count { it == 'x' } >= 4)
        assertTrue("a two-bar riff", acid.pattern.length == 32)
        assertTrue("the filter and more turned along the song", acid.moves.size >= 3)
        assertTrue("the cutoff moves in every section", acid.moves.maxOf { it.sections.size } >= 4)
        assertTrue("a kick that hits", s.effects.any { it.target == "kick" && it.preset == "punch" })
    }

    @Test
    fun amapianoLogDrumOnABackground() {
        val s = StarterWizard.withBody(style("amapiano"))
        assertEquals("Log drum", s.melody.bass)
        assertNotNull("a background", s.melody.texture)
        assertTrue("the log drum pumped by the kick", "808" in s.mix!!.pump)
    }
}
