package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PanSlider
import com.pocketstudio.ui.PanSliderDefaults
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 2: the pan slider a back swipe cannot move (Benjamin's kick hard left). A drag moves the
 * value only from the thumb, a cancelled gesture puts it back, the centre is caught, two touches
 * bring it back to the centre, and each gesture ends once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class PanSliderTest {
    @get:Rule
    val compose = createComposeRule()

    private var value by mutableFloatStateOf(0f)
    private val changes = mutableListOf<Float>()
    private var ends = 0

    private fun show(start: Float = 0f, step: Float = 0.1f, enabled: Boolean = true) {
        value = start
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.width(360.dp)) {
                    PanSlider(
                        value, { changes += it; value = it }, Modifier.testTag("pan"), onGestureEnd = { ends++ }, step = step, enabled = enabled,
                        label = "Pan du pad", stateLabel = { v -> if (v < 0f) "G ${(-v * 100).toInt()}" else if (v > 0f) "D ${(v * 100).toInt()}" else "Centre" },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    /** The track's width in px (the slider keeps its margins on each side). */
    private fun trackPx(): Float = with(compose.density) { (360.dp - PanSliderDefaults.Margin * 2).toPx() }

    private fun marginPx(): Float = with(compose.density) { PanSliderDefaults.Margin.toPx() }

    /** Where [v] is in the slider's own coordinates. */
    private fun xOf(v: Float): Float = marginPx() + (v + 1f) / 2f * trackPx()

    private fun drag(from: Float, by: Float, cancel: Boolean = false, steps: Int = 8) {
        compose.onNodeWithTag("pan").performTouchInput {
            down(Offset(from, height / 2f))
            repeat(steps) { moveBy(Offset(by / steps, 0f), delayMillis = 16) }
            if (cancel) cancel() else up()
        }
        compose.waitForIdle()
    }

    @Test
    fun aDragFromTheThumbMovesTheValue() {
        show()
        drag(from = xOf(0f), by = trackPx() / 4f)
        assertEquals(0.5f, value, 0.051f)
        assertEquals("one gesture, one end", 1, ends)
    }

    @Test
    fun aSwipeFromTheEdgeDoesNotJumpTheThumb() {
        show()
        // The finger lands near the left edge (a back swipe), slides in, then the system takes it.
        drag(from = 6f, by = 60f, cancel = true, steps = 3)
        assertEquals(0f, value, 0f)
        assertTrue("nothing was sent: $changes", changes.isEmpty())
        // Lifted instead (the system did not take it): still not a drag of the thumb.
        drag(from = xOf(-0.9f), by = 80f)
        assertEquals(0f, value, 0f)
        assertEquals(0, ends)
    }

    @Test
    fun aCancelledDragPutsTheValueBack() {
        show(start = 0.3f)
        drag(from = xOf(0.3f), by = -trackPx() / 3f, cancel = true)
        assertTrue("it moved during the drag: $changes", changes.size >= 2)
        assertEquals(0.3f, value, 0.0001f)
        assertEquals(1, ends)
    }

    @Test
    fun theCentreIsCaughtAndTwoTouchesBringItBack() {
        show(start = 0f, step = 0f)
        // A small drag stays at the centre (±0.05).
        drag(from = xOf(0f), by = trackPx() * 0.02f)
        assertEquals(0f, value, 0f)
        // A touch elsewhere puts the thumb there; two touches bring it back.
        compose.onNodeWithTag("pan").performTouchInput { click(Offset(xOf(0.8f), height / 2f)) }
        compose.waitForIdle()
        assertEquals(0.8f, value, 0.02f)
        compose.onNodeWithTag("pan").performTouchInput { doubleClick(Offset(xOf(0.8f), height / 2f)) }
        compose.waitForIdle()
        assertEquals(0f, value, 0f)
    }

    @Test
    fun accessibilityStillSetsIt() {
        show()
        val info = compose.onNodeWithTag("pan").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(-1f..1f, info.range)
        compose.onNodeWithTag("pan").performSemanticsAction(SemanticsActions.SetProgress) { it(-0.42f) }
        assertEquals(-0.4f, value, 0.0001f)
        assertEquals(1, ends)
        assertEquals(0f, PanSliderDefaults.shape(0.04f), 0f)
        assertEquals(0f, PanSliderDefaults.shape(Float.NaN), 0f)
        assertEquals(-1f, PanSliderDefaults.shape(-3f), 0f)
    }

    /** Review: TalkBack said « 50 % »; it says the pan's name and its side, and a keyboard moves it. */
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test
    fun talkBackAndAKeyboardKnowIt() {
        show(start = -0.3f)
        val config = compose.onNodeWithTag("pan").fetchSemanticsNode().config
        assertEquals(listOf("Pan du pad"), config[SemanticsProperties.ContentDescription])
        assertEquals("G 30", config[SemanticsProperties.StateDescription])
        compose.onNodeWithTag("pan").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("pan").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        assertEquals(-0.2f, value, 0.0001f)
        compose.onNodeWithTag("pan").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight); pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        assertEquals("the centre on the way", 0f, value, 0f)
        assertEquals(3, ends)
    }

    @Test
    fun disabledItSaysSoAndTakesNothing() {
        show(start = 0.5f, enabled = false)
        assertTrue(compose.onNodeWithTag("pan").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
        drag(from = xOf(0.5f), by = -trackPx() / 4f)
        assertEquals(0.5f, value, 0f)
    }
}
