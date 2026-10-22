package com.pocketstudio.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.MixerScreen
import com.pocketstudio.app.mixer.holdMeterPeaks
import com.pocketstudio.app.mixer.stripCompositions
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
        val peaks = FloatArray(5)
        val fresh = FloatArray(5).also { it[0] = 0.8f; it[4] = 1f }
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
}
