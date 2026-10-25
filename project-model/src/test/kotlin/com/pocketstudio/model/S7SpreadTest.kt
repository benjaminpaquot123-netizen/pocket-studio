package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S7 son (docs/son/banc/S7.md): the assistant's songs spread wide (mix version 4): the widener's
 * « Étalement » on their pads, chords and leads, never on what plays the bass; their Break a little
 * lower; a project saved before keeps its mix.
 */
class S7SpreadTest {
    private fun song(style: StarterWizard.Style): Project {
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts)
    }

    private fun spreadOf(p: Project, ch: Int): Float =
        p.mixer.channels[ch].inserts.filter { it.effect == EffectType.WIDENER && !it.bypass }.maxOfOrNull { it.params.getOrElse(2) { 0f } } ?: 0f

    @Test
    fun theNewSongsSpreadTheirVoicesAndKeepTheirBassesMono() {
        var spread = 0
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            assertEquals(style.id, Project.MIX_V4, p.mixVersion)
            // What plays the bass (the basses, the sub, the 808's pad): no spread.
            for (ch in p.bassChannels()) assertEquals("${style.id} bass strip $ch", 0f, spreadOf(p, ch), 0f)
            // The chords and pads (the main voices above the bass): spread, as wide as the style asks.
            val pads = p.padTrackIds().map { InstrumentTrack.mixerChannel(it) }
            val wide = pads.count { spreadOf(p, it) > 0f }
            if (wide > 0) spread++
            if (WideMix.spreadScale(style) == 0f) assertEquals(style.id, 0, wide)
            for (ch in pads) {
                val s = spreadOf(p, ch)
                val scale = WideMix.spreadScale(style)
                assertTrue("${style.id} pad strip $ch: $s", s == 0f || s == scale * WideMix.PAD_SPREAD || s == scale * WideMix.BASS_MUSIC_PAD_SPREAD)
            }
            // The mix's lows stay mono under 120 Hz (the spread starts at 250 Hz anyway).
            assertEquals(style.id, WideMix.MONO_BASS_HZ, p.mixer.monoBassHz, 0f)
        }
        // Nearly every style has a pad or chords with free room for it.
        assertTrue("$spread styles spread", spread >= StarterWizard.STYLES.size * 3 / 4)
    }

    @Test
    fun theWidenerAsItWasWithoutItsSpread() {
        // A widener of a project saved before has no third setting: it reads as 0 (off), as always.
        val old = Insert(EffectType.WIDENER.ordinal, listOf(0.65f, 0.35f)).sanitized()
        assertEquals(0f, old.params.getOrElse(2) { 0f }, 0f)
        assertEquals(listOf(0.5f, 0f, 0.7f, 0f), WideMix.spreadWidener(0.7f).params)
        // The setting is named and shown.
        assertEquals("Étalement", EffectType.WIDENER.params[2].label)
        // A project saved before keeps its mix version (1) and an unknown newer one is kept in range.
        assertEquals(Project.MIX_V1, ProjectJson.decode(ProjectJson.encode(Project())).mixVersion)
        assertEquals(Project.MIX_V4, Project(mixVersion = 4).sanitized().mixVersion)
    }

    @Test
    fun theBreakIsTrimmedForTheSpread() {
        val house = song(StarterWizard.style("house")!!)
        val lane = house.automation.first { it.target == AutoTarget.masterVolume() }
        val sections = StarterWizard.songSections(house)
        val brk = sections.first { house.allScenes[it.scene].label.startsWith("Break") }
        val beat = brk.start * house.beatsPerBar + 1
        val expected = WizardMix.fader(minOf(WizardMix.BREAK_DB, WizardMix.grooveDb(StarterWizard.style("house")!!) - 1f) - WizardMix.BREAK_SPREAD_TRIM_DB)
        assertEquals(expected, lane.songValues[beat]!!, 1e-4f)
    }
}
