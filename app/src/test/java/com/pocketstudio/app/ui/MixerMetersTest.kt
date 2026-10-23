package com.pocketstudio.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.MixerScreen
import com.pocketstudio.app.mixer.holdMeterPeaks
import com.pocketstudio.app.mixer.stripCompositions
import com.pocketstudio.app.mixer.HardLimiting
import com.pocketstudio.app.mixer.LIMITER_METER
import com.pocketstudio.app.mixer.MASTER_IN_METER
import com.pocketstudio.app.mixer.masterMeterFraction
import com.pocketstudio.app.mixer.PeakHold
import com.pocketstudio.engine.NativeEngine
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The mixer's meters (20 readings a second) neither compose the mixer again nor publish when silent. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class MixerMetersTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aMeterReadingDoesNotComposeTheStripsAgain() {
        val meters = mutableStateOf(FloatArray(17))
        compose.setContent {
            PocketStudioTheme { MixerScreen(Project().mixer.channels, { meters.value }, MixerActions()) }
        }
        compose.waitForIdle()
        val before = stripCompositions
        repeat(5) { k ->
            meters.value = FloatArray(17) { 0.1f * k }.also { it[16] = 1f - 0.1f * k }
            compose.waitForIdle()
        }
        assertEquals(before, stripCompositions)
    }

    @Test
    fun silenceStopsPublishing() {
        val peaks = FloatArray(NativeEngine.METERS)
        val fresh = FloatArray(NativeEngine.METERS).also { it[0] = 0.8f; it[LIMITER_METER] = 1f }
        assertTrue(holdMeterPeaks(peaks, fresh))
        assertEquals(0.8f, peaks[0])
        // The sound stops: the peak falls a while, then reaches silence and nothing moves any more.
        fresh[0] = 0f
        var readings = 0
        while (holdMeterPeaks(peaks, fresh)) readings++
        assertTrue("fell in $readings readings", readings in 10..60)
        assertEquals(0f, peaks[0])
        assertFalse(holdMeterPeaks(peaks, fresh))
    }

    @Test
    fun theMetersCarryTheLevelIntoTheLimiter() {
        // The engine's layout (JniBridge mixerMeters): a peak per channel, the limiter's gain, the master's input.
        assertEquals(NativeEngine.MIXER_CHANNELS, LIMITER_METER)
        assertEquals(NativeEngine.MIXER_CHANNELS + 1, MASTER_IN_METER)
        assertEquals(NativeEngine.MIXER_CHANNELS + 2, NativeEngine.METERS)
        // The level into the limiter is held like a peak (it may go over 1); the limiter's gain is taken as it is.
        val peaks = FloatArray(NativeEngine.METERS)
        val fresh = FloatArray(NativeEngine.METERS).also { it[MASTER_IN_METER] = 1.8f; it[LIMITER_METER] = 0.4f }
        holdMeterPeaks(peaks, fresh)
        fresh[MASTER_IN_METER] = 0f
        fresh[LIMITER_METER] = 1f
        holdMeterPeaks(peaks, fresh)
        assertEquals(1.8f * 0.85f, peaks[MASTER_IN_METER], 1e-6f)
        assertEquals(1f, peaks[LIMITER_METER])
        // The master meter's scale: −40..+6 dBFS, the limiter's ceiling (−1 dBFS) at 39/46 of it.
        assertEquals(0f, masterMeterFraction(0f))
        assertEquals(39f / 46f, masterMeterFraction(0.891251f), 1e-3f)
        assertEquals(1f, masterMeterFraction(4f))
    }

    @Test
    fun theHintWaitsForTwoSecondsOfHardWork() {
        val w = HardLimiting()
        // Hard on every beat (twice a second), not in between: shown after two seconds, not before.
        var t = 0L
        var shownAt = -1L
        while (t <= 5000) {
            val gain = if (t % 500 < 100) 0.3f else 0.9f
            if (w.update(gain, t) && shownAt < 0) shownAt = t
            t += HardLimiting.READ_MS
        }
        assertEquals(2000L, shownAt)
        // It stays while the limiter keeps working, goes after four calm seconds.
        assertTrue(w.update(1f, 5500))
        assertFalse(w.update(1f, 9100))
        // Now and then only (a peak a few seconds apart): never.
        val sometimes = HardLimiting()
        assertFalse((0..20).map { k -> sometimes.update(if (k % 15 == 0) 0.3f else 1f, k * 300L) }.any { it })
        // Not hard enough (−5 dB): never.
        val mild = HardLimiting()
        assertFalse((0..50).map { k -> mild.update(0.56f, k * 100L) }.any { it })
    }

    @Test
    fun theHintLowersTheMasterBySixDecibelsInOneTouch() {
        val meters = FloatArray(NativeEngine.METERS).also { it[LIMITER_METER] = 0.3f; it[MASTER_IN_METER] = 3f }
        val set = mutableListOf<Float>()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme { MixerScreen(Project().mixer.channels, { meters }, MixerActions(setMasterDb = { set += it }), masterDb = -1f) }
        }
        compose.mainClock.advanceTimeBy(1500)
        compose.onNodeWithTag("limiter_hint").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1000)
        // (A click by its semantics: scrolling there would wait for a scroll animation on a stopped clock.)
        compose.onNodeWithTag("limiter_hint_lower").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(-7f), set)
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("limiter_hint").assertDoesNotExist()
    }

    @Test
    fun noHintWhileTheLimiterBarelyWorks() {
        val meters = FloatArray(NativeEngine.METERS).also { it[LIMITER_METER] = 0.8f; it[MASTER_IN_METER] = 1.1f }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme { MixerScreen(Project().mixer.channels, { meters }, MixerActions()) }
        }
        compose.mainClock.advanceTimeBy(5000)
        compose.onNodeWithTag("limiter_hint").assertDoesNotExist()
        compose.onNodeWithTag("master_meter").assertExists()
    }

    /** The last 50 ms' true peak is held for a second (20 readings): a clip light the eye can catch. */
    @Test
    fun theRecentPeakIsHeldForASecond() {
        val hold = PeakHold(readings = 3)
        assertEquals(-0.5f, hold.update(-0.5f), 0f)
        assertEquals(-0.5f, hold.update(-20f), 0f)
        assertEquals(-0.5f, hold.update(-20f), 0f)
        assertEquals(-20f, hold.update(-20f), 0f)
        // A higher peak takes over at once.
        assertEquals(-0.2f, hold.update(-0.2f), 0f)
    }
}
