package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** « Zone »: a part of the song at one level, the rest of the curve as it was. */
class AutomationZoneTest {
    private val volume = AutoTarget(AutoTarget.VOLUME, 1)

    @Test
    fun anEmptyLaneGetsItsLevelOverTheZoneAndTheSettingAround() {
        // Louder over bars 1 to 8 of a 32-bar song; the fader sits at 0.6.
        val lane = AutomationLane(volume, mode = AutomationLane.SONG).withSongZone(0, 7, 0.9f, 0.6f, 32)
        val curve = lane.songCurve(32)
        (0..7).forEach { assertEquals("bar $it", 0.9f, curve[it], 1e-6f) }
        (8 until 32).forEach { assertEquals("bar $it", 0.6f, curve[it], 1e-6f) }
        // It jumps back on bar 9: the engine holds bar 8 at the zone's level (no slide down through it).
        assertEquals(0.9f, lane.songValueAtStep(7 * 16 + 15, 32, 16)!!, 1e-6f)
        assertEquals(0.6f, lane.songValueAtStep(8 * 16, 32, 16)!!, 1e-6f)
    }

    @Test
    fun aZoneInsideACurveKeepsItBeforeAndAfter() {
        // A fade in from bar 1 to bar 17; then bars 5 to 8 set to 0.2.
        val ramp = AutomationLane(volume, mode = AutomationLane.SONG).withSongPoint(0, 0f).withSongPoint(16, 0.8f)
        val before = ramp.songCurve(24)
        val lane = ramp.withSongZone(7, 4, 0.2f, 0.5f, 24)
        val curve = lane.songCurve(24)
        (0..3).forEach { assertEquals("bar $it", before[it], curve[it], 1e-6f) }
        (4..7).forEach { assertEquals("bar $it", 0.2f, curve[it], 1e-6f) }
        assertEquals(before[8], curve[8], 1e-6f)
        (16 until 24).forEach { assertEquals("bar $it", 0.8f, curve[it], 1e-6f) }
        // Into the zone with a jump: bar 4 (index 3) holds its own value to its end.
        assertEquals(before[3], lane.songValueAtStep(3 * 16 + 15, 24, 16)!!, 1e-6f)
    }

    @Test
    fun theZoneReplacesThePointsDrawnInside() {
        val drawn = AutomationLane(volume, mode = AutomationLane.SONG).withSongPoint(2, 0.1f).withSongPoint(3, 1f)
        val lane = drawn.withSongZone(1, 4, 0.5f, 0.7f, 8)
        assertEquals(listOf(0.1f, 0.5f, null, null, 0.5f, 1f), lane.songValues)
        assertEquals(listOf(1, 5), lane.songJumps)
    }
}
