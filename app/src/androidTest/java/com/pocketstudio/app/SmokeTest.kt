package com.pocketstudio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocketstudio.engine.NativeEngine
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator smoke test run by CI: the app starts, plays, and every tab opens without crashing. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsPlaysAndNavigates() {
        assertTrue("native engine must load", NativeEngine.isAvailable)
        compose.onNodeWithText("Pocket Studio").assertIsDisplayed()

        // The engine really runs: stream started and audio callbacks flowing.
        compose.waitUntil(10_000) { NativeEngine.streamInfo().let { it.running && it.callbacks > 0 } }

        // Pressing play makes the engine output signal, not just the button change.
        compose.onNodeWithTag("play").performClick()
        compose.onNodeWithText("Arrêter").assertIsDisplayed()
        var heard = false
        compose.waitUntil(5_000) {
            heard = heard || Metrics.streamInfo.value.peak > 0.1f
            heard
        }
        assertTrue("engine must produce sound after Play", heard)
        compose.onNodeWithTag("play").performClick()
        assertTrue("no NaN in the output", !NativeEngine.streamInfo().nonFinite)

        compose.onNodeWithTag("tab_Latence").performClick()
        compose.onNodeWithTag("latency").assertIsDisplayed()

        compose.onNodeWithTag("tab_Recette").performClick()
        compose.onNodeWithTag("recette").assertIsDisplayed()
        compose.onNodeWithText("Phase 0 : fondations").assertIsDisplayed()
    }
}
