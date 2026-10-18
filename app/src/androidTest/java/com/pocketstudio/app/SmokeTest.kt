package com.pocketstudio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocketstudio.engine.NativeEngine
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.pocketstudio.app.latency.LatencyUiState
import com.pocketstudio.app.latency.LatencyViewModel
import com.pocketstudio.engine.LatencyStart
import com.pocketstudio.engine.LatencyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator smoke test run by CI: the app starts, plays, and every tab opens without crashing. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule(order = 0)
    val mic: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    @get:Rule(order = 1)
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

        // The microphone path delivers samples to the latency meter (the emulator mic is silent,
        // so only the plumbing is checked here, not the measured value).
        assertEquals(LatencyStart.Ok, NativeEngine.startLatencyTest())
        compose.waitUntil(6_000) { NativeEngine.latencyState() == LatencyState.Done }
        val result = NativeEngine.latencyResult()
        assertTrue("microphone delivered ${result.inputFrames} frames (${NativeEngine.inputDescription()})", result.inputFrames > 0)

        // Same path as the Latence screen (ViewModel -> JNI -> engine). The emulator mic is silent,
        // so the run may fail, but never because the meter did not run (NoInput) or was cut.
        val vm = LatencyViewModel()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { vm.measure(wanted = 1, maxAttempts = 2) }
        compose.waitUntil(30_000) { vm.state.value.let { it is LatencyUiState.Measured || it is LatencyUiState.Failed } }
        val diagnostic = when (val s = vm.state.value) {
            is LatencyUiState.Measured -> s.diagnostic
            is LatencyUiState.Failed -> s.diagnostic
            else -> ""
        }
        assertTrue("meter must run: $diagnostic", !diagnostic.contains("NoInput") && !diagnostic.contains("Interrupted"))

        compose.onNodeWithTag("tab_Latence").performClick()
        compose.onNodeWithTag("latency").assertIsDisplayed()

        compose.onNodeWithTag("tab_Recette").performClick()
        compose.onNodeWithTag("recette").assertIsDisplayed()
        compose.onNodeWithText("Phase 0 : fondations").assertIsDisplayed()
    }
}
