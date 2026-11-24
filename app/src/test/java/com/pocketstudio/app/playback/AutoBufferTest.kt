package com.pocketstudio.app.playback

import com.pocketstudio.engine.AutoBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Simulated underrun bursts: the output buffer goes up one notch (2 → 4 → 6) when dropouts pile up,
 * without restarting the stream in a loop and without ever going back down on its own.
 */
class AutoBufferTest {
    private class Feed(val auto: AutoBuffer = AutoBuffer(), var user: Int = 2) {
        var xruns = 0; var restarts = 0; var now = 0L; var applied = user
        val changes = mutableListOf<Int>()
        fun tick(addXruns: Int = 0, dtMs: Long = 100, running: Boolean = true): Int? {
            xruns += addXruns; now += dtMs
            val r = auto.update(user, xruns, restarts, running, now)
            if (r != null) { applied = r; changes += r }
            return r
        }
    }

    @Test fun repeatedUnderrunsRaiseOneNotch() {
        val f = Feed()
        f.tick()
        repeat(3) { f.tick(addXruns = 1, dtMs = 1000) }
        assertEquals(4, f.applied)
        assertEquals(listOf(4), f.changes)
    }

    @Test fun aSingleGlitchDoesNothing() {
        val f = Feed()
        f.tick()
        f.tick(addXruns = 1)
        repeat(200) { f.tick() }
        f.tick(addXruns = 1, dtMs = 15_000)
        assertEquals(2, f.applied)
        assertEquals(emptyList<Int>(), f.changes)
    }

    @Test fun aStormClimbsSlowlyAndStopsAtTheTop() {
        val f = Feed()
        f.tick()
        // 60 s of constant dropouts: one change per cooldown at most, capped at 6.
        repeat(600) { f.tick(addXruns = 2) }
        assertEquals(6, f.applied)
        assertEquals(listOf(4, 6), f.changes)
    }

    @Test fun neverGoesBackDownOnItsOwn() {
        val f = Feed()
        f.tick()
        repeat(3) { f.tick(addXruns = 1, dtMs = 500) }
        repeat(3000) { f.tick() }
        assertEquals(4, f.applied)
        assertEquals(listOf(4), f.changes)
    }

    @Test fun aReopenedStreamIsNotCountedAsDropouts() {
        val f = Feed()
        f.tick(addXruns = 5)
        // The stream was reopened (output change): its xrun counter restarts from 0, then the old
        // count comes back from a fresh stream. Neither jump is a dropout burst.
        f.restarts = 1; f.xruns = 0; f.tick()
        f.xruns = 40; f.restarts = 2; f.tick()
        repeat(50) { f.tick() }
        assertEquals(2, f.applied)
        assertEquals(emptyList<Int>(), f.changes)
    }

    @Test fun stoppedStreamIsIgnored() {
        val f = Feed()
        f.tick(running = false)
        repeat(5) { f.tick(addXruns = 1, running = false) }
        assertEquals(emptyList<Int>(), f.changes)
    }

    @Test fun userChoiceAboveTheBoostWinsAndANewChoiceResets() {
        val f = Feed()
        f.tick()
        repeat(3) { f.tick(addXruns = 1) }
        assertEquals(4, f.applied)
        // The user picks "Très sûr": the boost is no longer above it, nothing extra asked.
        f.user = 6
        f.auto.reset()
        assertNull(f.tick())
        repeat(3) { f.tick(addXruns = 1, dtMs = 20_000) }
        // Already at the top: no change, no reapply loop.
        assertEquals(listOf(4), f.changes)
    }
}
