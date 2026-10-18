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
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.ProjectStore
import java.io.File
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
        compose.waitUntil(10_000) { NativeEngine.streamInfo().let { it.running && it.callbacks > 0 } }

        // Rhythm screen: the starter groove plays and makes sound.
        compose.onNodeWithTag("rhythm").assertIsDisplayed()
        compose.onNodeWithTag("transport_play").performClick()
        compose.waitUntil(5_000) { NativeEngine.currentStep() >= 0 }
        var heard = false
        compose.waitUntil(5_000) {
            heard = heard || Metrics.streamInfo.value.peak > 0.1f
            heard
        }
        assertTrue("drums must sound", heard)
        compose.onNodeWithTag("transport_play").performClick()
        compose.waitUntil(2_000) { NativeEngine.currentStep() == -1 }

        // A live pad fires its lane.
        val before = IntArray(NativeEngine.LANES).also { NativeEngine.laneTriggers(it) }
        compose.onNodeWithTag("pad_4").performClick()
        compose.waitUntil(2_000) {
            IntArray(NativeEngine.LANES).also { NativeEngine.laneTriggers(it) }[4] > before[4]
        }

        // An edit survives the app going to the background (autosave flush on stop).
        compose.onNodeWithTag("step_3").performClick()  // pad 4 (clap) is selected now
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        val store = ProjectStore(File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "projects"))
        var saved = false
        for (i in 0 until 50) {
            saved = store.load(RhythmViewModel.PROJECT_ID)?.drums?.lanes?.get(4)?.hits?.containsKey(3) == true
            if (saved) break
            Thread.sleep(100)
        }
        assertTrue("edit must be saved when the app goes to the background", saved)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.waitUntil(10_000) { NativeEngine.streamInfo().let { it.running && it.callbacks > 0 } }

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

        // Tools: test tone, then latency page; then the checklist.
        compose.onNodeWithTag("tab_Outils").performClick()
        compose.onNodeWithTag("tool_Son").performClick()
        compose.onNodeWithTag("play").performClick()
        compose.onNodeWithText("Arrêter").assertIsDisplayed()
        compose.onNodeWithTag("play").performClick()
        compose.onNodeWithTag("tool_Latence").performClick()
        compose.onNodeWithTag("latency").assertIsDisplayed()

        compose.onNodeWithTag("tool_Recette").performClick()
        compose.onNodeWithTag("recette").assertIsDisplayed()

        compose.onNodeWithTag("tab_Mixeur").performClick()
        compose.onNodeWithTag("mixer").assertIsDisplayed()
        compose.onNodeWithTag("mute_0").performClick()
        compose.onNodeWithTag("mute_0").performClick()
        assertTrue("no NaN in the output", !NativeEngine.streamInfo().nonFinite)
    }
}
