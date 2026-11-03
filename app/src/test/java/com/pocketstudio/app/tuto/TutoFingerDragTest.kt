package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * REVIEW n° 20 (rest): « Montre-moi »'s « glisser » is drawn, not a touch: the finger presses at the
 * start of the knob's (or fader's) path, slides along it frame after frame, and lifts at its end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-mdpi")
class TutoFingerDragTest {
    @get:Rule
    val compose = createComposeRule()

    /** The finger's tip (where [TutoFinger] puts it: 35 % in, 8 % down of its 40 dp box, mdpi: 1 px = 1 dp). */
    private fun tip(): Offset = compose.onNodeWithTag("tuto_finger_tip", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        .let { Offset(it.left + it.width * 0.35f, it.top + it.height * 0.08f) }

    /** The tip at every frame of one gesture, the test's clock driving the animation. */
    private fun follow(target: Rect, drag: TutoDrag?): List<Offset> {
        compose.mainClock.autoAdvance = false
        compose.setContent { PocketStudioTheme { Box(Modifier.fillMaxSize()) { TutoFinger(target, 1, null, 16, drag = drag) } } }
        compose.mainClock.advanceTimeByFrame()
        return (0 until (FINGER_DRAG_MS / 16 + 4)).map {
            compose.mainClock.advanceTimeByFrame()
            tip()
        }
    }

    @Test
    fun aKnobTurnedUpIsShownByAFingerSlidingRightAlongIt() {
        val knob = Rect(40f, 300f, 320f, 360f)
        val path = follow(knob, TutoDrag.RIGHT)
        val (from, to) = dragPath(knob, TutoDrag.RIGHT)
        // It starts where it presses, ends where it lifts, on the knob's line all along.
        assertTrue("starts at the press: ${path.first()} vs $from", (path.first() - from).getDistance() < 3f)
        assertTrue("ends at the lift: ${path.last()} vs $to", (path.last() - to).getDistance() < 3f)
        path.forEach { assertTrue("on the knob's line: $it", abs(it.y - from.y) < 5f && it.x in knob.left..knob.right) }
        // It really slides: never back, through many places in between (not a jump from one end to the other).
        path.zipWithNext().forEach { (a, b) -> assertTrue("never back: $a then $b", b.x >= a.x - 0.5f) }
        val between = path.map { it.x.toInt() }.distinct().count { it > from.x + 5f && it < to.x - 5f }
        assertTrue("$between places on the way", between >= 10)
        // Half way through its time it is on the way, neither still at the press nor already at the lift.
        val mid = path[path.size / 2]
        assertTrue("on the way at half time: $mid", mid.x > from.x + (to.x - from.x) * 0.2f && mid.x < to.x - 3f)
    }

    @Test
    fun aFaderLoweredIsShownByAFingerSlidingDown() {
        val strip = Rect(100f, 200f, 176f, 560f)
        val path = follow(strip, TutoDrag.DOWN)
        val (from, to) = dragPath(strip, TutoDrag.DOWN)
        assertTrue(to.y > from.y)
        assertTrue((path.first() - from).getDistance() < 3f)
        assertTrue((path.last() - to).getDistance() < 3f)
        path.forEach { assertEquals(from.x, it.x, 3f) }
        path.zipWithNext().forEach { (a, b) -> assertTrue(b.y >= a.y - 0.5f) }
        assertTrue(path.map { it.y.toInt() }.distinct().size >= 10)
    }

    @Test
    fun aTouchStaysOnItsPlace() {
        val pad = Rect(40f, 300f, 120f, 380f)
        val path = follow(pad, null)
        path.forEach { assertEquals(pad.center.x, it.x, 3f) }
    }

    @Test
    fun theDemonstrationSlidesOnKnobsAndFadersAndTouchesTheRest() {
        assertEquals(TutoDrag.RIGHT, TutoController.dragOf("changed:synth:lead:CUTOFF+RESONANCE"))
        assertEquals(TutoDrag.DOWN, TutoController.dragOf("changed:volumes:pad,lead"))
        assertEquals(TutoDrag.RIGHT, TutoController.dragOf("send:0"))
        assertNull(TutoController.dragOf("lane:kick"))
        assertNull(TutoController.dragOf("tempo"))
    }
}
