package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.MixerScreen
import com.pocketstudio.app.mixer.fitProbeCompositions
import com.pocketstudio.app.mixer.stripCompositions
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tour 7 review (M1, M3): the console's fitted faders keep its state and do not compose it again at each measure. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class MixerFitStateTest {
    @get:Rule
    val compose = createComposeRule()

    /** A remix's stems attached under the console (the faders no longer fitted) leave a strip's open sheet open. */
    @Test
    fun anOpenSheetSurvivesTheFitGoingAway() {
        var stems by mutableStateOf(false)
        compose.setContent {
            PocketStudioTheme {
                MixerScreen(
                    Project().mixer.channels, { FloatArray(17) }, MixerActions(),
                    songTracks = if (stems) ({ Text("stems") }) else null,
                )
            }
        }
        compose.onNodeWithTag("strip_more_0").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("strip_sheet").assertExists()
        stems = true
        compose.waitForIdle()
        compose.onNodeWithTag("strip_sheet").assertExists()
    }

    /** A new height (a sheet's or the keyboard's animation) measures again, without composing the hidden probes again. */
    @Test
    fun aNewHeightDoesNotComposeTheStripsAgain() {
        var high by mutableStateOf(890)
        // Made once: the root composes again at each height, and new channels would compose every strip again.
        val channels = Project().mixer.channels
        val meters = { FloatArray(17) }
        val actions = MixerActions()
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxWidth().height(high.dp)) {
                    MixerScreen(channels, meters, actions)
                }
            }
        }
        compose.waitForIdle()
        val before = stripCompositions
        val probes = fitProbeCompositions
        assertTrue("the probes were never composed", probes > 0)
        repeat(4) {
            high -= 4
            compose.waitForIdle()
        }
        assertEquals("the probes composed again", probes, fitProbeCompositions)
        assertEquals("the strips composed again", before, stripCompositions)
    }

    /** The unseen probes' strips are not counted as the console's: one composition per shown strip. */
    @Test
    fun theProbesAreNotCountedAsStrips() {
        val channels = Project().mixer.channels
        stripCompositions = 0
        compose.setContent { PocketStudioTheme { MixerScreen(channels, { FloatArray(17) }, MixerActions()) } }
        compose.waitForIdle()
        val shown = compose.onAllNodes(hasTestTagStartingWith("strip_more_")).fetchSemanticsNodes().size
        assertTrue("no strip", shown > 0)
        assertEquals(shown, stripCompositions)
    }

    private fun hasTestTagStartingWith(prefix: String) =
        SemanticsMatcher("tag starts with $prefix") { SemanticsProperties.TestTag in it.config && it.config[SemanticsProperties.TestTag].startsWith(prefix) }
}
