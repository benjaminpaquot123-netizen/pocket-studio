package com.pocketstudio.app.human

import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The bench's own helpers, guarded (PLAN-RESTE R.6): what the Bench* tests use to drive the app must cost the bench's
 * clock what a finger costs, or what they measure (a 15 s demo, a count-in, a song's end) is read once it is over.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchSelfTest : HumanTester("selftest") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    /**
     * R.6 (re-reads of banc-r1): [reachTag] swiped up to 8 times (« glisse » then « attends 1 »: 22 to 26 s of the bench's
     * clock) to bring into the screen's middle band an element already shown whole near an edge (« Mes projets » in the top
     * bar, a button at the bottom). A finger touches such an element where it is: no swipe, no wait, the same place.
     */
    @Test
    fun reachTagTouchesAVisibleElementInPlace() {
        begin()
        val h = screenHeight.toFloat()
        val tagged = compose.onAllNodes(SemanticsMatcher("tagged") { n -> SemanticsProperties.TestTag in n.config })
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        val candidates = tagged.filter { n -> shownWhole(n) }.mapNotNull { n -> n.config[SemanticsProperties.TestTag].let { t -> centerOf(t)?.let { t to it } } }
            .filter { (_, at) -> at.second < 0.2f * h || at.second > 0.8f * h }.sortedBy { it.first }
        assertTrue("an element shown whole outside the screen's middle band, on the welcome:\n" + screenLines().joinToString("\n"), candidates.isNotEmpty())
        val (tag, at) = candidates.first()
        val clock = SystemClock.uptimeMillis()
        val reached = reachTag(tag)
        val spent = SystemClock.uptimeMillis() - clock
        assertEquals("« $tag » touched where it is", at, reached)
        assertTrue("the bench's clock moved $spent ms to reach « $tag », already shown (a finger: none)", spent < 300)
    }
}
