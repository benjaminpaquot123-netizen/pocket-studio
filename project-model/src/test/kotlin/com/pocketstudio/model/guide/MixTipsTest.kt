package com.pocketstudio.model.guide

import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Mode simple's « Mix » tab: « ↺ Tout à zéro » back to the assistant's levels, and its one-tap tips. */
class MixTipsTest {
    private fun song(id: String = "acid_techno"): Project {
        val style = StarterWizard.style(id)!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    @Test
    fun theAssistantsLevelsAreKeptAndComeBack() {
        val p = song()
        val start = p.mixer.channels.map { it.volume }
        assertEquals(start, p.mixer.startVolumes)
        // Some strips away from 0 dB (the pads' balance): « Tout à zéro » is not a plain 0 dB.
        assertTrue(start.any { it != Mixer.DEFAULT_VOLUME })
        val moved = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.map { it.copy(volume = 0.3f) }))
        assertEquals(start.filterIndexed { i, _ -> i != Mixer.MASTER }, moved.mixer.atStartVolumes().channels.map { it.volume }.filterIndexed { i, _ -> i != Mixer.MASTER })
        // Kept through a save; a song made by hand has none (0 dB) and writes nothing.
        assertEquals(p.mixer.startVolumes, ProjectJson.decode(ProjectJson.encode(p)).mixer.startVolumes)
        assertFalse(ProjectJson.encode(Project()).contains("startVolumes"))
        val hand = Project().let { it.copy(mixer = it.mixer.copy(channels = it.mixer.channels.map { c -> c.copy(volume = 0.5f) })) }
        assertTrue(hand.mixer.atStartVolumes().channels.filterIndexed { i, _ -> i != Mixer.MASTER }.all { it.volume == Mixer.DEFAULT_VOLUME })
    }

    @Test
    fun aBassPushedUpGetsTheKicksTipAndItsFixUndoesIt() {
        val p = song()
        assertTrue("a fresh song: no tip", MixTips.of(p).isEmpty())
        val bass = GuideFacts.of(p).tracks.first { it.role in setOf(GuideRoles.BASS, GuideRoles.SUB, GuideRoles.BASS2, GuideRoles.ACID) && it.channel != Mixer.MASTER }
        val loud = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == bass.channel) c.copy(volume = GuideLevels.faderOf(GuideLevels.faderDb(c.volume) + 3f)) else c }))
        val tip = MixTips.of(loud).first()
        assertEquals(bass.channel, tip.ch)
        assertEquals("La basse et le kick se gênent.", tip.text)
        assertEquals("Baisser la basse de 2 dB", tip.fix)
        val fixed = MixTips.apply(loud, tip)
        assertEquals(GuideLevels.faderDb(loud.mixer.channels[bass.channel].volume) - 2f, GuideLevels.faderDb(fixed.mixer.channels[bass.channel].volume), 0.01f)
    }

    @Test
    fun clippingComesFirstThenASoloLeftOn() {
        val p = song()
        val solo = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == 1) c.copy(solo = true) else c }))
        val tips = MixTips.of(solo, clipping = true)
        assertEquals(listOf("clip", "solo"), tips.map { it.id }.take(2))
        assertEquals(p.mixer.masterDb - MixTips.CLIP_STEP_DB, MixTips.apply(solo, tips[0]).mixer.masterDb, 0.001f)
        assertTrue(MixTips.apply(solo, tips[1]).mixer.channels.none { it.solo })
    }

    @Test
    fun aFaderNearTheTopIsSaid() {
        val p = song()
        val hot = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == 0) c.copy(volume = 1f) else c }))
        val tip = MixTips.of(hot).first { it.ch == 0 }
        assertNotEquals(1f, MixTips.apply(hot, tip).mixer.channels[0].volume)
        assertEquals("Le baisser de 3 dB", tip.fix)
    }
}
