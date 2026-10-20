package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationClipsTest {
    private val volume = AutoTarget(AutoTarget.VOLUME, 2)

    @Test
    fun aPadsOwnStripIsNeverAutomated() {
        // The engine reads 4 bits of channel: pad 5's send (strip 20) would land on the master's first effect.
        val padSend = AutoTarget(AutoTarget.SEND, Mixer.padChannel(4), 0)
        assertFalse(padSend.isValid)
        assertTrue(AutoTarget(AutoTarget.INSERT_PARAM, 15, 3, 2).isValid)
        val lanes = AutomationLane.sanitize(listOf(AutomationLane(padSend), AutomationLane(volume)))
        assertEquals(listOf(volume), lanes.map { it.target })
    }

    @Test
    fun perSceneLanesKeepEachScenesSteps() {
        val lane = AutomationLane(volume, mode = AutomationLane.PER_SCENE)
            .withPoint(0, 0.2f, scene = 0).withPoint(0, 0.9f, scene = 1)
        assertEquals(0.2f, lane.valuesFor(0)[0])
        assertEquals(0.9f, lane.valuesFor(1)[0])
        assertNull(lane.valuesFor(2)[0])
        assertTrue(lane.values.all { it == null })  // the loop's own steps are untouched
    }

    @Test
    fun theSongCurveSlidesBetweenPoints() {
        val lane = AutomationLane(volume, mode = AutomationLane.SONG).withSongPoint(2, 0.0f).withSongPoint(6, 1.0f)
        val curve = lane.songCurve(10)
        assertEquals(listOf(0f, 0f, 0f, 0.25f, 0.5f, 0.75f, 1f, 1f, 1f, 1f), curve)
        assertTrue(AutomationLane(volume).songCurve(8).isEmpty())
        // Erasing the last point trims the list.
        assertEquals(3, lane.withSongPoint(6, null).songValues.size)
    }

    @Test
    fun savedAndCleaned() {
        val lane = AutomationLane(volume, mode = 7, sceneValues = mapOf(99 to listOf(2f), 1 to listOf(Float.NaN)), songValues = List(300) { 0.5f })
        val clean = lane.sanitized()
        assertEquals(AutomationLane.SONG, clean.mode)
        assertEquals(setOf(1), clean.sceneValues.keys)
        assertNull(clean.sceneValues[1]!![0])
        assertEquals(AutomationLane.MAX_SONG_BARS, clean.songValues.size)
        val p = Project(automation = listOf(AutomationLane(volume, mode = AutomationLane.SONG).withSongPoint(3, 0.4f))).sanitized()
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test
    fun songBarsAddUpParts() {
        val p = Project(scenes = listOf(Scene("A"), Scene("B", drums = DrumPattern().withSteps(32))), song = listOf(SongPart(0, 2), SongPart(1, 1))).sanitized()
        assertEquals(4, p.songBars)
    }
}
