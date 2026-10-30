package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Pads − 8 + » and the pads' shape (Benjamin, 2026-10-30): square or a little wide, full rows, under the thumb. */
class LivePadsShapeTest {
    @Test
    fun `the stepper goes through 4 6 8 12 16 and never hides a sound`() {
        val eight = LivePads(size = 8)
        assertEquals(12, eight.stepped(+1))
        assertEquals(6, eight.stepped(-1))
        assertEquals(4, LivePads(size = 6).stepped(-1))
        assertEquals(4, LivePads(size = 4).stepped(-1))
        assertEquals(16, LivePads(size = 16).stepped(+1))
        // A sound on pad 7: down to 6 would hide it.
        val used = LivePads(size = 8).with(6, LiveClip("a.wav", "A"))
        assertEquals(8, used.stepped(-1))
        // An old project at 6 pads keeps 6 (now a size of its own).
        assertEquals(6, LivePads(size = 6).sanitized().size)
    }

    private fun check(count: Int, w: Float, h: Float, cols: Int) {
        val g = padGrid(count, w, h, 12f)
        assertEquals("$count pads in $w x $h", cols, g.cols)
        assertEquals("full rows", count, g.cols * g.rows)
        assertTrue("never taller than wide: $g", g.padW >= g.padH - 0.01f)
        assertTrue("at most 1.35 : 1: $g", g.padW <= g.padH * LivePads.MAX_WIDE + 0.01f)
        assertTrue("fits: $g", g.gridW <= w + 0.01f && g.gridH <= h + 0.01f)
    }

    @Test
    fun `upright phone - 2 columns up to 8, 3 for 12, 4 for 16`() {
        val w = 388f
        val h = 640f
        check(4, w, h, 2)
        check(6, w, h, 2)
        check(8, w, h, 2)
        check(12, w, h, 3)
        check(16, w, h, 4)
    }

    @Test
    fun `room left over goes above - pads stay square, not stretched`() {
        // 4 pads on a tall screen: 2 x 2 squares, the rest of the height left empty (above them).
        val g = padGrid(4, 388f, 700f, 12f)
        assertEquals(g.padW, g.padH, 0.01f)
        assertTrue(g.gridH < 700f - 200f)
    }

    @Test
    fun `short and wide room - pads get narrower, never wider than 1_35`() {
        val g = padGrid(8, 800f, 330f, 12f)
        assertEquals(4, g.cols)
        check(8, 800f, 330f, 4)
        check(4, 800f, 330f, 4)
        val flat = padGrid(2 * 2, 1000f, 200f, 12f)
        assertTrue(flat.padW <= flat.padH * LivePads.MAX_WIDE + 0.01f)
    }
}
