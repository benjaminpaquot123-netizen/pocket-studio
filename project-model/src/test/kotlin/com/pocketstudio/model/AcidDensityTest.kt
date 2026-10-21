package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AcidDensityTest {
    private fun filled(density: Float, length: Int, seed: Int): Int {
        val p = AcidGenerator(Random(seed)).generate(AcidPattern(length = length, density = density))
        return p.steps.take(length).count { it.gate }
    }

    @Test
    fun aHundredPercentFillsEveryStepOnEveryBar() {
        (0 until 20).forEach { seed -> assertEquals(32, filled(1f, 32, seed)) }
    }

    @Test
    fun theUsualSettingKeepsItsRestsAndMoreDensityMeansMoreNotes() {
        val usual = (0 until 50).sumOf { filled(0.8f, 16, it) }
        val busy = (0 until 50).sumOf { filled(0.9f, 16, it) }
        val sparse = (0 until 50).sumOf { filled(0.2f, 16, it) }
        assertTrue("0.8 keeps rests: $usual", usual < 16 * 50 * 0.6)
        assertTrue("$sparse < $usual < $busy", sparse < usual && usual < busy)
    }
}
