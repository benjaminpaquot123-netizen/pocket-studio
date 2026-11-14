package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit ultra round 1 (n° 13, 14, seats A c, E d, F c): one loudness target in the whole app. The
 * master pro's « Spotify −14 » and « Volume visé » « Club » were both on, the export took the pro
 * one; « Club » was −8 here, −9 there. Now [LoudnessTargets.choiceOf] reads one choice, every chip
 * clears the other row, « Club » is the same value everywhere and Streaming aims at the whole song.
 */
class OneLoudnessTargetTest {
    private val house = Project(style = "house")

    @Test
    fun aProjectOfBeforeReadsTheTargetItsExportUsed() {
        val both = house.mixer.copy(target = LoudnessTarget.CLUB, pro = MasterPro(targetLufs = -14f))
        assertEquals(TargetChoice.STREAMING, LoudnessTargets.choiceOf(both))
        assertEquals(TargetChoice.APPLE, LoudnessTargets.choiceOf(both.copy(pro = MasterPro(targetLufs = -16f))))
        // The old pro « Club −9 » is a value of its own now: said as it is, never called « Club ».
        assertEquals(TargetChoice.FREE, LoudnessTargets.choiceOf(both.copy(pro = MasterPro(targetLufs = -9f))))
        assertEquals(TargetChoice.CLUB, LoudnessTargets.choiceOf(both.copy(pro = null)))
    }

    @Test
    fun eachChoiceIsTheOneTarget() {
        var m = house.mixer.copy(target = LoudnessTarget.AUTO, pro = MasterPro(targetLufs = -14f, ceilingDb = -0.3f))
        m = LoudnessTargets.choose(m, TargetChoice.CLUB)
        assertEquals(LoudnessTarget.CLUB, m.target)
        assertNull(m.pro?.targetLufs)
        assertEquals("the pro's other settings stay", -0.3f, m.pro!!.ceilingDb, 0f)
        assertEquals(-8f, LoudnessTargets.of(house.copy(mixer = m))!!, 0f)
        assertFalse(LoudnessTargets.wholeSong(m))
        m = LoudnessTargets.choose(m, TargetChoice.STREAMING)
        assertEquals(-14f, LoudnessTargets.of(house.copy(mixer = m))!!, 0f)
        assertTrue("streaming is measured on the whole song, as the platforms do", LoudnessTargets.wholeSong(m))
        m = LoudnessTargets.choose(m, TargetChoice.FREE, -11.5f)
        assertEquals(TargetChoice.FREE, LoudnessTargets.choiceOf(m))
        assertEquals(-11.5f, LoudnessTargets.of(house.copy(mixer = m))!!, 0f)
        assertTrue(m.target != LoudnessTarget.OFF)
        m = LoudnessTargets.choose(m, TargetChoice.OFF)
        assertNull(LoudnessTargets.of(house.copy(mixer = m)))
        assertNull("nothing left to save", LoudnessTargets.choose(house.mixer.copy(pro = MasterPro(targetLufs = -16f)), TargetChoice.OFF).pro)
        // The Guide and the tutorials set the same one.
        val guided = LoudnessTargets.withTarget(house.mixer.copy(pro = MasterPro(targetLufs = -16f)), LoudnessTarget.CLUB)
        assertEquals(TargetChoice.CLUB, LoudnessTargets.choiceOf(guided))
    }

    @Test
    fun clubIsOneValueAndEveryLabelSaysLufs() {
        // « Club »: −8, or the style's own when louder (drum & bass −7,5): the same on every screen.
        assertEquals(-8f, LoudnessTargets.shown(TargetChoice.CLUB, LoudnessTargets.forStyle("house", Project.MIX_V2))!!, 0f)
        assertEquals(-7.5f, LoudnessTargets.shown(TargetChoice.CLUB, LoudnessTargets.forStyle("dnb", Project.MIX_V2))!!, 0f)
        assertEquals("Club · −8 LUFS", LoudnessTargets.label(TargetChoice.CLUB, -8f))
        assertEquals("Streaming · −14 LUFS", LoudnessTargets.label(TargetChoice.STREAMING, -14f))
        assertEquals("Apple Music · −16 LUFS", LoudnessTargets.label(TargetChoice.APPLE, -16f))
        assertEquals("Désactivée", LoudnessTargets.label(TargetChoice.OFF, null))
    }

    /** n° 31 (seat F a): « Garder ce que je viens de jouer » on the Clavier keeps the keys, not a pad hit touched before on Rythme. */
    @Test
    fun theKeysAloneAreKeptWithoutAPadPlayedBefore() {
        val m = PlayedMemory()
        m.newEpoch(false)
        m.pad(2, 0.9f, 10.0)
        m.noteOn(0, 60, 0.8f, 20.0); m.noteOff(0, 60, 21.0)
        m.noteOn(0, 64, 0.8f, 22.0); m.noteOff(0, 64, 23.0)
        val keys = m.capture(24.0, PlayedMemory.BARS, 16, keys = true, pads = false)
        assertEquals(2, keys.notes.size)
        assertTrue(keys.hits.isEmpty())
        assertEquals("the first key starts the bar, not the pad before it", 0.0, keys.notes.first().start, 1e-9)
        val both = m.capture(24.0, PlayedMemory.BARS, 16)
        assertEquals(1, both.hits.size)
    }
}
