package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * The phone's analysis and the Linux one (scripts/bentest/empreinte.py) read the same numbers: the same fixed signal,
 * the same values written here and in test_empreinte.py (test_the_same_numbers_as_the_phone). One side changed alone,
 * one of the two tests goes red.
 */
class MemeAnalyseTest {
    @Test
    fun theFixedSignalReadsTheSameNumbersAsOnLinux() {
        val n = 5 * 48_000
        val x = FloatArray(n) { i ->
            (0.3 * sin(2 * PI * 440 * i / 48_000) + 0.1 * sin(2 * PI * 3000 * i / 48_000) +
                0.05 * sin(2 * PI * 97 * i / 48_000) + 0.02 * sin(2 * PI * 7000 * i / 48_000)).toFloat()
        }
        val b = Bandes(48_000)
        b.ajouter(x)
        assertEquals(28, b.trames)
        val e = b.energies()
        for ((band, want) in GOLD) assertEquals("${Tiers.NOMS[band]} Hz", 1.0, e[band] / want, 1e-4)
        val oct = Empreinte.octaves(e, b.trames, null)
        for (k in OCTAVES_GOLD.indices) {
            val w = OCTAVES_GOLD[k]
            if (w == null) assertNull("${Empreinte.OCTAVES[k]} Hz", oct[k]) else assertEquals("${Empreinte.OCTAVES[k]} Hz", w, oct[k]!!, 1e-3)
        }
    }

    companion object {
        /** Third-octave band (index in Tiers.NOMS) → energy, as empreinte.py reads it. */
        val GOLD = mapOf(3 to 1.761584e6, 9 to 6.341374e7, 10 to 4.135967e3, 18 to 7.046431e6, 21 to 2.818572e5)
        val OCTAVES_GOLD = listOf(-61.8423, -13.0104, null, 2.5527, null, null, -6.9897, -20.9691)
    }
}
