package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lot A2: what the trials call a break in the sound (a quarter second silent while playing is a hole of over 200 ms). */
class EpreuvesTest {
    private fun lu(peak: Float, playing: Boolean = true, gap: Float = 1f, bloc: Float = 0f) =
        Ecoute.Lu(0, 0, 0.2f, peak, playing = playing, gapWorst = gap, blocMs = bloc)

    @Test
    fun aSilentQuarterSecondWhilePlayingIsAHole() {
        val reads = List(8) { lu(0.5f) }.toMutableList().also { it[5] = lu(0f) }
        assertEquals(1, Epreuves.trous(reads))
        // Stopped, it is no hole.
        assertEquals(0, Epreuves.trous(reads.map { if (it.peak == 0f) it.copy(playing = false) else it }))
    }

    @Test
    fun aSilentSecondAJumpNanAndALongGapAreBreaks() {
        val fine = List(12) { lu(0.3f) }
        assertEquals(emptyList<String>(), Epreuves.coupures(Ecoute.bilan(fine, 0)))
        val silent = fine.toMutableList().also { for (k in 4..7) it[k] = lu(0f) }
        assertTrue(Epreuves.coupures(Ecoute.bilan(silent, 0)).single().contains("muette"))
        val jump = List(12) { lu(0.05f) }.toMutableList().also { it[6] = lu(0.9f) }
        assertTrue(Epreuves.coupures(Ecoute.bilan(jump, 0)).single().startsWith("SAUT"))
        val nan = fine.toMutableList().also { it[3] = it[3].copy(nonFinite = true) }
        assertTrue(Epreuves.coupures(Ecoute.bilan(nan, 0)).single().contains("NaN"))
        // 40 blocks of 5.3 ms: 213 ms without a callback.
        val gap = fine.toMutableList().also { it[2] = lu(0.3f, gap = 40f, bloc = 5.33f) }
        assertTrue(Epreuves.coupures(Ecoute.bilan(gap, 0)).single().contains("ms entre deux blocs"))
    }

    @Test
    fun theMemoryLeaksOverAFifthMore() {
        assertEquals(null, Epreuves.memoireMonte(200, 240))
        assertEquals("+41 Mo (+21 %)", Epreuves.memoireMonte(200, 241))
        assertEquals(null, Epreuves.memoireMonte(null, 900))
    }

    @Test
    fun aScreenIsSlowOnlyTenPointsOverTheStillOne() {
        assertTrue(Epreuves.imagesRouge(0.30, 0.05))
        // The phone itself is slow: the still screen at 25 %, the screen at 30 %: not the app's fault.
        assertTrue(!Epreuves.imagesRouge(0.30, 0.25))
        assertTrue(!Epreuves.imagesRouge(0.12, 0.0))
        assertTrue(Epreuves.imagesRouge(0.20, null))
    }

    @Test
    fun aSilentVideoHasNoSound() {
        assertTrue(Epreuves.sonVideo(Relecture.Relu(10.0, -3.0)))
        assertTrue(!Epreuves.sonVideo(Relecture.Relu(10.0, -90.0)))
        assertTrue(!Epreuves.sonVideo(Relecture.Relu(10.0, Double.NEGATIVE_INFINITY)))
    }
}
