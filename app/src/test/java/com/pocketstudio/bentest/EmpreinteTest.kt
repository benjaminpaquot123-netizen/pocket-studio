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

/** « Écoute au micro », a song's fingerprint (T1b): pink noise through a known filter must read the filter, octave by octave. */
class EmpreinteTest {
    private fun bandes(x: FloatArray): Bandes = Bandes(RATE).also { b ->
        var i = 0
        while (i < x.size) {
            val n = minOf(1024, x.size - i)
            b.ajouter(x.copyOfRange(i, i + n), n)
            i += n
        }
    }

    @Test
    fun filteredPinkNoiseReadsItsFilterOctaveByOctave() {
        // A phone-like song: the low end cut at 200 Hz, a dip at 2 kHz.
        val song = Chaine(Biquad.highPass(200.0), Biquad.peak(2000.0, -6.0, q = 0.7))
        val heard = song.filtrer(SonsFabriques.rose(20 * RATE))
        val b = bandes(heard)
        val got = Empreinte.octaves(b.energies(), b.trames, null)
        val want = SonsFabriques.octavesAttendues(song::power)
        assertEquals(Empreinte.OCTAVES.size, got.size)
        for (k in got.indices) {
            assertNotNull("${Empreinte.OCTAVES[k]} Hz", got[k])
            assertTrue("${Empreinte.OCTAVES[k]} Hz: ${"%.2f".format(got[k])} dB, imposed ${"%.2f".format(want[k])} dB", abs(got[k]!! - want[k]) <= 1.0)
        }
        assertEquals(Feu.VERT, Empreinte.feu(got).first)
    }

    @Test
    fun theRoomsNoiseIsTakenOutAndWhatItHidesIsNotMadeUp() {
        val song = Chaine(Biquad.highPass(500.0), Biquad.highPass(500.0))
        val n = 20 * RATE
        val heard = SonsFabriques.plus(song.filtrer(SonsFabriques.rose(n, rms = 0.1)), SonsFabriques.rose(n, rms = 0.003, seed = 5))
        val room = bandes(SonsFabriques.rose(4 * RATE, rms = 0.003, seed = 6))
        val b = bandes(heard)
        val got = Empreinte.octaves(b.energies(), b.trames, room.puissances())
        val want = SonsFabriques.octavesAttendues(song::power)
        assertEquals(Empreinte.OCTAVES.size, got.size)
        assertNull("63 Hz: the song gives nothing there, the room's noise is not a bass", got[0])
        for (k in 3 until got.size) assertTrue("${Empreinte.OCTAVES[k]} Hz: ${got[k]} against ${want[k]}", abs(got[k]!! - want[k]) <= 1.5)
    }

    @Test
    fun nothingHeardIsRed() {
        val room = bandes(SonsFabriques.blanc(4 * RATE, 0.01, seed = 3))
        val b = bandes(SonsFabriques.blanc(10 * RATE, 0.01, seed = 4))
        val got = Empreinte.octaves(b.energies(), b.trames, room.puissances())
        assertEquals(Feu.ROUGE, Empreinte.feu(got).first)
    }
}
