package com.pocketstudio.app

import com.pocketstudio.app.mixer.automationValueText
import com.pocketstudio.app.mixer.hasControl
import com.pocketstudio.app.mixer.laneValueAt
import com.pocketstudio.app.mixer.timeTicks
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the automation curves say around them: their values in the setting's words, the bars or beats under them. */
class AutomationScaleTest {
    private val channels = List(16) { Channel() }

    @Test
    fun aCurveSaysItsValuesAsItsSettingShowsThem() {
        val volume = AutoTarget(AutoTarget.VOLUME, 1)
        assertEquals("−∞ dB", automationValueText(volume, 0f, channels, emptyList()))
        assertTrue(automationValueText(volume, 0.8f, channels, emptyList()).endsWith("dB"))
        assertEquals("centre", automationValueText(AutoTarget(AutoTarget.PAN, 0), 0.5f, channels, emptyList()))
        assertEquals("G 100", automationValueText(AutoTarget(AutoTarget.PAN, 0), 0f, channels, emptyList()))
        // A filter's cutoff in hertz, from its own scale (40 Hz to 18 kHz).
        val filtered = channels.mapIndexed { i, c -> if (i == 2) c.copy(inserts = listOf(Insert.of(EffectType.FILTER)) + c.inserts.drop(1)) else c }
        val cutoff = AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0)
        assertTrue(automationValueText(cutoff, 0f, filtered, emptyList()).endsWith("Hz"))
        assertTrue(automationValueText(cutoff, 1f, filtered, emptyList()).contains("kHz"))
        // The 303's knobs as their 0..100, a held control on or off.
        assertEquals("100 %", automationValueText(AutoTarget.acid(0), 1f, channels, emptyList()))
        assertEquals("marche", automationValueText(AutoTarget.macro(0, AutoTarget.MACRO_CHOP), 1f, channels, emptyList()))
    }

    @Test
    fun theTimeScaleCountsBarsOfASongAndBeatsOfALoop() {
        // A loop of 16 steps: its 4 beats.
        assertEquals(listOf(0 to "1", 4 to "2", 8 to "3", 12 to "4"), timeTicks(song = false, first = 0, visible = 16, perBar = 1))
        // 60 bars of a song: every 8th bar, about eight numbers.
        val song = timeTicks(song = true, first = 0, visible = 60, perBar = 1)
        assertEquals("1", song.first().second)
        assertTrue(song.size <= 8)
        assertEquals(8, song[1].first)
        // Zoomed in on bars 9 to 16 of a curve written per beat (4 points a bar): each bar.
        assertEquals((8 until 16).map { it * 4 to "${it + 1}" }, timeTicks(song = true, first = 32, visible = 32, perBar = 4))
    }

    @Test
    fun theDialReadsTheValuePlaying() {
        val steps = listOf(0.2f, null, null, 0.8f) + List(60) { null }
        assertEquals(0.2f, laneValueAt(false, steps, emptyList(), 4, 1f))
        assertEquals(0.8f, laneValueAt(false, steps, emptyList(), 4, 3f))
        // Stopped: the start.
        assertEquals(0.2f, laneValueAt(false, steps, emptyList(), 4, -1f))
        assertEquals(null, laneValueAt(false, List(64) { null }, emptyList(), 16, 2f))
        assertEquals(0.5f, laneValueAt(true, emptyList(), listOf(0.1f, 0.5f), 2, 1.5f))
        assertEquals(null, laneValueAt(true, emptyList(), emptyList(), 8, 0f))
    }

    @Test
    fun theKnobsThatCanBeShownHaveTheirLink() {
        assertTrue(hasControl(AutoTarget.acid(0)))
        assertTrue(hasControl(AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0)))
        assertFalse(hasControl(AutoTarget(AutoTarget.VOLUME, 0)))
        assertFalse(hasControl(AutoTarget.live(AutoTarget.LIVE_FILTER)))
    }
}
