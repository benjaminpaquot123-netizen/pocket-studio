package com.pocketstudio.app.mixer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.nav.acidTechnoSong
import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Macros
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L2, F024 (grave; seat t9-49 gesture 78 « glisse 270 250 270 600 »: a vertical slide from the middle of a
 * fader on a Mix that scrolls). « Faire défiler le Mix ne change aucun réglage. » Option A: a fader is taken only within
 * 32 dp of its cap, the pad XY only from its dot; elsewhere the finger scrolls the page. A plain Compose finger, no bench.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi")
class Tour9MixScrollTest {
    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<Float>()
    private lateinit var scroll: ScrollState

    /** [content] in a page that scrolls, with room above and below it. */
    private fun page(content: @Composable () -> Unit) {
        compose.setContent {
            scroll = rememberScrollState()
            PocketStudioTheme {
                Column(Modifier.height(640.dp).verticalScroll(scroll)) {
                    Spacer(Modifier.height(300.dp))
                    content()
                    Spacer(Modifier.height(900.dp))
                }
            }
        }
        compose.waitForIdle()
    }

    private fun proFader() = page {
        var v by remember { mutableFloatStateOf(0.8f) }
        VerticalFader(v, { v = it; changes += it }, { 0f }, "Volume", "fader")
    }

    private fun xyPad() = page {
        XyPad(1, Macros.DEFAULTS, QuickFxActions(setMacroPair = { _, _, x, _, y -> changes += x; changes += y }))
    }

    /** A slow slide on [tag] from [from] to [to] (fractions of the node); gives how far the page went. */
    private fun slide(tag: String, from: Offset, to: Offset): Int {
        compose.onNodeWithTag(tag).performScrollTo()
        compose.waitForIdle()
        val before = scroll.value
        changes.clear()
        compose.onNodeWithTag(tag).performTouchInput {
            swipe(Offset(width * from.x, height * from.y), Offset(width * to.x, height * to.y), durationMillis = 500)
        }
        compose.waitForIdle()
        return scroll.value - before
    }

    /** The seat's gesture: a vertical slide from the fader's middle (its cap at 0 dB, far above). */
    @Test
    fun f024_aSlideFromTheProFadersMiddleScrollsAndMovesNothing() {
        proFader()
        val went = slide("fader", Offset(0.5f, 0.6f), Offset(0.5f, 0.05f))
        assertTrue("the fader moved while the page was scrolled: $changes", changes.isEmpty())
        assertNotEquals("the page did not scroll", 0, went)
    }

    @Test
    fun f024_aSlideFromTheProFadersCapMovesItAndThePageStays() {
        proFader()
        // The cap at 0 dB: 20 % from the top of the travel.
        val went = slide("fader", Offset(0.42f, 0.2f), Offset(0.42f, 0.6f))
        assertEquals("the page scrolled under the fader", 0, went)
        assertTrue("the fader did not follow the finger: $changes", changes.isNotEmpty() && changes.last() < 0.6f)
    }

    /** The phone's font at 200 %: the simple Mix is taller than the screen and scrolls. */
    @Test
    @Config(fontScale = 2.0f)
    fun f024_aSlideFromTheSimpleMixsFaderAwayFromItsCapScrollsAndMovesNothing() {
        val t = simpleTracks(acidTechnoSong())[1]
        compose.setContent {
            PocketStudioTheme {
                SimpleMixScreen(
                    SimpleMixUi("Acid techno", simpleTracks(acidTechnoSong()), emptyMap(), 0f, false, null, null),
                    { FloatArray(NativeEngine.METERS) }, { FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE } },
                    SimpleMixActions(setVolume = { _, v -> changes += v }), words = { OutputWords.GOOD },
                )
            }
        }
        compose.waitForIdle()
        val tag = "mix_fader_${t.ch}"
        val top = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top
        changes.clear()
        // From the bottom of its travel (the cap high above), up: the seat's slide.
        compose.onNodeWithTag(tag).performTouchInput { swipe(Offset(width / 2f, height * 0.95f), Offset(width / 2f, height * 0.1f), durationMillis = 500) }
        compose.waitForIdle()
        assertTrue("the simple Mix's fader moved while the page was slid: $changes", changes.isEmpty())
        assertNotEquals("the simple Mix did not scroll", top, compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top)
    }

    @Test
    fun f024_aSlideOnThePadXyAwayFromItsDotScrollsAndMovesNothing() {
        xyPad()
        // The dot at rest: in the middle, at the bottom. A slide up the pad's left edge.
        val went = slide("xy_pad_1", Offset(0.05f, 0.9f), Offset(0.05f, 0.05f))
        assertTrue("the pad XY moved while the page was scrolled: $changes", changes.isEmpty())
        assertNotEquals("the page did not scroll", 0, went)
    }

    @Test
    fun f024_aSlideFromThePadXysDotMovesItAndThePageStays() {
        xyPad()
        // The dot at rest: colour in the middle, space at the bottom (14 dp above the pad's 170 dp edge).
        val went = slide("xy_pad_1", Offset(Macros.DEFAULTS[Macros.COLOR], 156f / 170f), Offset(0.9f, 0.1f))
        assertEquals("the page scrolled under the pad XY", 0, went)
        assertTrue("the pad XY did not follow the finger: $changes", changes.size >= 2)
    }
}
