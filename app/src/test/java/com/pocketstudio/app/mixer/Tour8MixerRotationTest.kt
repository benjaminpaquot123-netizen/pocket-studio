package com.pocketstudio.app.mixer

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 8 (lot mixer8): turning the phone lying down and back (the mixer goes from one column to two, its faders no
 * longer fitted) keeps a strip's open sheet, the console's scrolled strips and the master's open « Effets »; so does a
 * remix's stems coming under the console meanwhile. Dragging a fader composes the console once per frame, never the
 * unseen probes that size its faders.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w900dp-h900dp")
class Tour8MixerRotationTest {
    @get:Rule
    val compose = createComposeRule()

    private var lying by mutableStateOf(false)
    private var stems by mutableStateOf(false)

    /** Eight strips: wider than an upright phone, so the console scrolls sideways. */
    private val strips = listOf(0, 1, 2, 3, 5, 6, 7, 8).map { StripInfo(it, "P$it") }

    private fun showMixer() {
        val channels = Project().mixer.channels
        val meters = { FloatArray(17) }
        val actions = MixerActions()
        compose.setContent {
            val base = LocalConfiguration.current
            val turned = remember(lying) {
                Configuration(base).apply { orientation = if (lying) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT }
            }
            CompositionLocalProvider(LocalConfiguration provides turned) {
                PocketStudioTheme {
                    Box(Modifier.size(if (lying) 890.dp else 400.dp, if (lying) 400.dp else 890.dp)) {
                        MixerScreen(
                            channels, meters, actions, strips = strips,
                            songTracks = if (stems) ({ Text("stems") }) else null,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun consoleScroll(): SemanticsNodeInteraction = compose.onNode(
        hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange) and hasAnyAncestor(hasTestTag("console")),
    )

    private fun scrolled(): Float = consoleScroll().fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()

    /** The console scrolled, the master's « Effets » open, then a strip's sheet open. */
    private fun setUpState(): Float {
        consoleScroll().performSemanticsAction(SemanticsActions.ScrollBy) { it(60f, 0f) }
        compose.waitForIdle()
        compose.onNodeWithTag("master_effects").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithTag("fx_chain_$MASTER_CHANNEL").assertExists()
        compose.onNodeWithTag("strip_more_2").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithTag("strip_sheet").assertExists()
        val by = scrolled()
        assertTrue("the console did not scroll", by > 0f)
        return by
    }

    private fun assertKept(by: Float, where: String) {
        compose.onNodeWithTag("strip_sheet").assertExists("$where: the strip's sheet closed")
        compose.onNodeWithTag("fx_chain_$MASTER_CHANNEL").assertExists("$where: the master's effects closed")
        assertEquals("$where: the console's scroll was lost", by, scrolled(), 0.5f)
    }

    @Test
    fun turningThePhoneThereAndBackKeepsTheMixersState() {
        showMixer()
        val by = setUpState()
        lying = true
        compose.waitForIdle()
        assertKept(by, "lying down")
        lying = false
        compose.waitForIdle()
        assertKept(by, "upright again")
    }

    @Test
    fun turningThePhoneWhileARemixIsMadeKeepsTheMixersState() {
        showMixer()
        val by = setUpState()
        lying = true
        compose.waitForIdle()
        stems = true
        compose.waitForIdle()
        assertKept(by, "lying down, the remix made")
        lying = false
        compose.waitForIdle()
        assertKept(by, "upright again, with the remix")
    }

    /** 60 frames of one fader dragged: at most one strip composed per frame, the unseen probes never. */
    @Test
    fun aFaderDragComposesTheConsoleOncePerFrame() {
        var channels by mutableStateOf(Project().mixer.channels)
        val meters = { FloatArray(17) }
        val actions = MixerActions(setVolume = { ch, v -> channels = channels.mapIndexed { i, c -> if (i == ch) c.copy(volume = v) else c } })
        compose.setContent { PocketStudioTheme { MixerScreen(channels, meters, actions) } }
        compose.waitForIdle()
        val strips0 = stripCompositions
        val probes0 = fitProbeCompositions
        assertTrue("the probes were never composed", probes0 > 0)
        repeat(60) { frame ->
            actions.setVolume(0, 0.8f - frame * 0.01f)
            compose.waitForIdle()
        }
        assertEquals("the unseen probes composed again while a fader moved", probes0, fitProbeCompositions)
        val strips = stripCompositions - strips0
        assertTrue("the console's strips composed $strips times over 60 frames", strips <= 60)
    }
}
