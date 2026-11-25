package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/**
 * The kick per style family (Benjamin, 2026-11-25: « le son est pur mais ça manque de volume et le
 * kick tape pas assez », version B: kick 49 Hz, bass 41.2 Hz on another note): every style's kick
 * pad carries its family's note, sweep (start over the note, 25 to 45 ms, so 70 to 145 ms to land),
 * drive and length, which the engine reads (PITCH_DROP, DROP_TIME).
 */
class KickFamiliesTest {
    private fun kick(id: String): PadVoice {
        val s = StarterWizard.style(id)!!
        return StudioDrums.kickVoice(s, Harmony(s.melody.root))!!
    }

    private fun ratio(v: PadVoice) = 2.0.pow(v.param(PadVoice.PITCH_DROP)!! / 12.0)

    @Test
    fun everyStyleHasItsFamilysKick() {
        for (s in StarterWizard.STYLES) {
            val v = StudioDrums.kickVoice(s, Harmony(s.melody.root))!!
            val drop = v.param(PadVoice.PITCH_DROP)
            val time = v.param(PadVoice.DROP_TIME)
            assertTrue(s.id, drop != null && time != null && v.param(PadVoice.DRIVE) != null && v.param(PadVoice.DECAY) != null)
            // The sweep lands on the note in 70 to 145 ms (about three time constants of 25 to 45 ms).
            assertTrue("${s.id} $time", time!! in 0.025f..0.045f)
            assertTrue("${s.id} $drop", ratio(v) in 2.7..6.1)
        }
        assertTrue(StudioDrums.KICK_FAMILIES.keys.all { StarterWizard.style(it) != null })
        assertTrue(StarterWizard.STYLES.all { it.id in StudioDrums.KICK_FAMILIES })
    }

    @Test
    fun theTableReadsAsBenjaminsVersionB() {
        // Techno: 49 Hz (G1) in its key's root or fifth nearest, x4.5 over 35 ms.
        val techno = kick("techno")
        assertEquals(4.5, ratio(techno), 0.05)
        assertEquals(0.035f, techno.param(PadVoice.DROP_TIME)!!, 1e-4f)
        // House rounder and cleaner than disco, trance the cleanest and slowest sweep.
        assertTrue(kick("house").param(PadVoice.DRIVE)!! < kick("disco").param(PadVoice.DRIVE)!!)
        assertTrue(kick("trance").param(PadVoice.DRIVE)!! <= 0.2f)
        assertEquals(0.045f, kick("trance").param(PadVoice.DROP_TIME)!!, 1e-4f)
        assertTrue(kick("acid_house").param(PadVoice.DRIVE)!! <= 0.15f)
        // Gabber long and saturated, the hard kick; acidcore x2.8.
        assertEquals(PadVoice.KICK_HARD, kick("gabber").kind)
        assertEquals(0.29f, kick("gabber").param(PadVoice.DECAY)!!, 1e-4f)
        assertEquals(2.8, ratio(kick("acidcore")), 0.05)
        // Jersey club deep and very short, lofi high and short.
        assertTrue(kick("jersey_club").param(PadVoice.DECAY)!! <= 0.12f)
        assertTrue(kick("lofi").param(PadVoice.TUNE)!! > 66f)
        // Drum & bass keeps the tutorial's kick (« Kit ▾ » › Drum & bass recognises it).
        assertTrue(StudioDrums.isDnbKick(kick("dnb")))
        assertTrue(StudioDrums.isDnbKick(StudioDrums.dnbKick(5)))
    }

    @Test
    fun theKickIsInTheKeyNearItsNote() {
        for (s in StarterWizard.STYLES) {
            if (s.id in StudioDrums.BASS_808) continue
            val target = StudioDrums.kickFamily(s).hz
            for (root in 0..11) {
                val hz = StudioDrums.kickVoice(s, Harmony(root))!!.param(PadVoice.TUNE)!!
                val st = 12 * ln(hz / 440.0) / ln(2.0) + 69
                val pc = Math.floorMod(Math.round(st).toInt(), 12)
                assertTrue("${s.id} $root $hz", pc == root || pc == Math.floorMod(root + 7, 12))
                assertTrue("${s.id} $root $hz", abs(12 * ln(hz / target) / ln(2f)) <= 3.6f)
            }
        }
    }
}
