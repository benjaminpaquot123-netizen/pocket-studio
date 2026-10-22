package com.pocketstudio.app.live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Audit 28 review m6: a finger that lands while the grid still flings only stops it. */
@RunWith(RobolectricTestRunner::class)
class LivePressTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aTouchThatOnlyStopsAFlingLaunchesNothing() {
        var scrolling = true
        var presses = 0
        var taps = 0
        compose.setContent {
            CompositionLocalProvider(LocalGridScrolling provides { scrolling }) {
                Box(Modifier.size(96.dp).launchOnPress(press = { presses++; true }, retract = {}, tap = { taps++ }).testTag("pad"))
            }
        }
        compose.onNodeWithTag("pad").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(0, presses)
        assertEquals(0, taps)
        // The grid still: the finger's down launches, as on a controller.
        scrolling = false
        compose.onNodeWithTag("pad").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(1, presses)
        assertEquals(0, taps)
    }
}
