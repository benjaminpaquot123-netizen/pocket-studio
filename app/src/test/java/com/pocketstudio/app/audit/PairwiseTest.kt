package com.pocketstudio.app.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The audit's pair generator covers every pair, far under the full product, the same on every run. */
class PairwiseTest {
    @Test
    fun everyPairOfTheCaptureMatrixIsCoveredInFewCases() {
        val factors = listOf(
            CaptureMatrixTest.SCREEN_NAMES, CaptureMatrixTest.LANGS, CaptureMatrixTest.DEVICES.keys.toList(), CaptureMatrixTest.FONTS,
        )
        val cases = Pairwise.cases(factors)
        for (i in factors.indices) for (j in i + 1 until factors.size) for (a in factors[i]) for (b in factors[j]) {
            assertTrue("$a with $b", cases.any { it[i] == a && it[j] == b })
        }
        val product = factors.fold(1) { p, f -> p * f.size }
        val floor = factors[0].size * factors[1].size
        assertTrue("${cases.size} cases (floor $floor, product $product)", cases.size <= floor * 1.15 && cases.size < product / 5)
        assertEquals(cases, Pairwise.cases(factors))
        assertEquals(CaptureMatrixTest.ALL.size, cases.size)
    }

    @Test
    fun smallFactorsToo() {
        val f = listOf(listOf("a", "b"), listOf("1", "2", "3"), listOf("x", "y"))
        val cases = Pairwise.cases(f)
        for (i in f.indices) for (j in i + 1 until f.size) for (a in f[i]) for (b in f[j]) assertTrue(cases.any { it[i] == a && it[j] == b })
        assertTrue(cases.size in 6..8)
    }
}
