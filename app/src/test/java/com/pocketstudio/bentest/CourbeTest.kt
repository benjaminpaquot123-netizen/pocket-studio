package com.pocketstudio.bentest

import com.pocketstudio.bentest.SonsFabriques.Biquad
import com.pocketstudio.bentest.SonsFabriques.Chaine
import com.pocketstudio.bentest.SonsFabriques.RATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * « Écoute au micro », the speaker's curve (T1b): a sweep made by the robot goes through a speaker we know (a filter
 * imposed here), with a delay and the room's noise; the analysis must find the curve imposed, band by band.
 */
class CourbeTest {
    private fun bandes(x: FloatArray, bloc: Int = 960): Bandes {
        val b = Bandes(RATE)
        var i = 0
        while (i < x.size) {
            val n = minOf(bloc, x.size - i)
            b.ajouter(x.copyOfRange(i, i + n), n)
            i += n
        }
        return b
    }

    /** The sweep [speaker] plays, heard 150 ms late with the room's [noiseRms], and the room alone before. */
    private fun mesure(speaker: Chaine, noiseRms: Double, sweepS: Double = 8.0, level: Float = 1f): Courbe.Resultat {
        val sweep = Courbe.balayage(RATE, sweepS)
        assertEquals("the sweep lasts what was asked", (sweepS * RATE).toInt(), sweep.size)
        val heard = SonsFabriques.plus(
            SonsFabriques.silence((0.15 * RATE).toInt()) + speaker.filtrer(sweep).map { it * level }.toFloatArray() + SonsFabriques.silence(RATE / 2),
            SonsFabriques.blanc(((sweepS + 0.65) * RATE).toInt(), noiseRms, seed = 7),
        )
        val room = bandes(SonsFabriques.blanc(4 * RATE, noiseRms, seed = 8))
        // The reference padded as the robot pads it: every sample of the sweep under two half windows.
        val ref = bandes(SonsFabriques.silence(Tiers.TAILLE / 2) + sweep + SonsFabriques.silence(Tiers.TAILLE))
        val mic = bandes(heard)
        return Courbe.mesurer(ref.energies(), mic.energies(), mic.trames, room.puissances(), Courbe.tramesBalayees(RATE, sweepS))
    }

    /**
     * The sweep stays a few frames in each band: the room competes with it during those frames only. A sweep at
     * 0.3 (power 0.045) over white noise of RMS 0.05: in the 1 kHz band (231.6 Hz wide) the room's power is
     * 0.0025 × 231.6 / 24 000, so the band is heard about 32.7 dB over it, not the 19 dB the whole listening would give.
     */
    @Test
    fun aBandIsJudgedAgainstTheNoiseOfItsOwnFrames() {
        val r = mesure(Chaine(Biquad.peak(1000.0, 0.0)), noiseRms = 0.05)
        val k = Tiers.NOMS.indexOf("1000")
        val bw = Tiers.haut(k) - Tiers.bas(k)
        val want = 10 * kotlin.math.log10(0.045 / (0.05 * 0.05 * bw / (RATE / 2.0)))
        assertEquals("1 kHz over the room during its own frames", want, r.rsb[k]!!, 2.0)
        // A small speaker's bass a little over the room while swept is heard, not dropped.
        val small = mesure(Chaine(Biquad.highPass(400.0)), noiseRms = 0.05)
        assertNotNull("63 Hz, 32 dB down, still 12 dB over the room during its frames", small.db[Tiers.NOMS.indexOf("63")])
    }

    @Test
    fun theSweepThroughAKnownSpeakerGivesBackItsCurve() {
        // A small speaker: nothing much under 400 Hz, a bump at 3 kHz, falling after 8 kHz.
        val speaker = Chaine(Biquad.highPass(400.0), Biquad.peak(3000.0, 6.0), Biquad.lowPass(8000.0))
        val r = mesure(speaker, noiseRms = 0.0005)
        val got = r.db
        val want = SonsFabriques.courbeAttendue(speaker::power)
        assertEquals(Tiers.NOMS.size, got.size)
        for (k in Tiers.CENTRES.indices) {
            val f = Tiers.CENTRES[k]
            if (f < 100 || f > 12_000) continue
            val g = got[k]
            assertNotNull("${Tiers.NOMS[k]} Hz heard", g)
            assertTrue("${Tiers.NOMS[k]} Hz: ${"%.2f".format(g)} dB, imposed ${"%.2f".format(want[k])} dB", abs(g!! - want[k]) <= 1.0)
        }
        assertEquals("0 dB in the middle", 0.0, got[Tiers.NOMS.indexOf("1000")]!!, 1.0)
        assertTrue("the 3 kHz bump found", got[Tiers.NOMS.indexOf("3150")]!! > 4.0)
        assertTrue("the low cut found", got[Tiers.NOMS.indexOf("160")]!! < -14.0)
        assertEquals(Feu.VERT, Courbe.feu(r).first)
        assertTrue("1 kHz well over the room", r.rsb[Tiers.NOMS.indexOf("1000")]!! > 30.0)
    }

    @Test
    fun aBandUnderTheRoomsNoiseIsNotMadeUp() {
        // A speaker that gives nothing under 1 kHz (two high-passes), in a noisy room.
        val speaker = Chaine(Biquad.highPass(1000.0), Biquad.highPass(1000.0))
        val got = mesure(speaker, noiseRms = 0.01).db
        assertNull("50 Hz is the room's noise, not the speaker", got[Tiers.NOMS.indexOf("50")])
        assertNotNull("1 kHz heard", got[Tiers.NOMS.indexOf("1000")])
        assertNotNull("4 kHz heard", got[Tiers.NOMS.indexOf("4000")])
    }

    @Test
    fun nothingHeardIsRedAndANoisyRoomOrange() {
        val nothing = mesure(Chaine(Biquad.highPass(400.0)), noiseRms = 0.01, level = 0f)
        assertEquals(Feu.ROUGE, Courbe.feu(nothing).first)
        // The middle of the curve under 20 dB over the room's noise (during its own frames: about 17 dB at 1 kHz).
        val noisy = mesure(Chaine(Biquad.highPass(400.0)), noiseRms = 0.3)
        val f = Courbe.feu(noisy)
        assertEquals("a room too noisy for part of the curve: ${f.second}", Feu.ORANGE, f.first)
        assertTrue(f.second, f.second.contains("bruit"))
    }
}
