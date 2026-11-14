package com.pocketstudio.model

import org.junit.Assert.assertTrue
import org.junit.Test

/** Audit ultra round 1, n° 5: a style's pack fills its page's loops (C b saw only its row of hits). */
class PadLivePacksFillTest {
    @Test
    fun everyStylePackHasLoopsToPlay() {
        PadLive.PACKS.filter { it.style != null }.forEach { pack ->
            val recipe = StarterWizard.style(pack.style!!)!!
            val song = PadLive.packSong(pack, PadLive.TEMPO)!!
            val plan = LivePadPlan.of(song, max = PadLive.MAX_LOOPS)
            println("${pack.id}: ${plan.size} ${plan.map { it.name }}")
            assertTrue("${pack.id}: ${plan.size} loops", plan.size >= 4)
        }
    }
}
