package com.pocketstudio.app.ui

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Lot « tuto 5 » (Lucas, c/118): the second touch of a double tap on a button that changed the screen. */
class DoubleTouchGuardTest {
    private val g = DoubleTouchGuard
    private val density = 1.5f

    @Before
    fun fresh() = g.reset()

    private fun tap(x: Float, y: Float, at: Long): Boolean {
        val down = g.drop(ACTION_DOWN, x, y, at, density)
        val up = g.drop(ACTION_UP, x, y, at + 80, density)
        assertTrue(down == up)
        return down
    }

    @Test
    fun theSecondTouchOfADoubleTapThatChangedTheScreenIsDropped() {
        assertFalse(tap(293f, 944f, 10_000))
        g.screenChanged(10_100)
        // A thumb's double tap (150 ms), or the bench's two touches (about 1.2 s): dropped, press to release.
        assertTrue(g.drop(ACTION_DOWN, 295f, 940f, 10_150, density))
        assertTrue(g.drop(ACTION_MOVE, 296f, 941f, 10_180, density))
        assertTrue(g.drop(ACTION_UP, 296f, 941f, 10_230, density))
        // Once only: a third touch there is the user's own.
        assertFalse(tap(293f, 944f, 10_400))
    }

    @Test
    fun anythingElseGoesThrough() {
        // A touch that changes nothing on the screen: a double tap on a stepper stays two taps.
        assertFalse(tap(100f, 100f, 1_000))
        assertFalse(tap(100f, 100f, 1_150))
        // Another place, or too late, after a change of screen.
        assertFalse(tap(293f, 944f, 5_000))
        g.screenChanged(5_100)
        assertFalse(tap(100f, 300f, 5_200))
        assertFalse(tap(293f, 944f, 5_000 + DoubleTouchGuard.WINDOW_MS + 10))
        // A screen that changed long after the touch (not caused by it) arms nothing.
        assertFalse(tap(50f, 50f, 20_000))
        g.screenChanged(20_000 + DoubleTouchGuard.CAUSE_MS + 1)
        assertFalse(tap(50f, 50f, 21_100))
        // The clock starting again (a new run) never drops a touch.
        assertFalse(tap(60f, 60f, 30_000))
        g.screenChanged(30_050)
        assertFalse(tap(60f, 60f, 200))
    }
}
