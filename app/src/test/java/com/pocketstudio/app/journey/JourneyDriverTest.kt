package com.pocketstudio.app.journey

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

/** [Journey] only uses its driver's six moves: a touch waits for a single, still node, brings it into view, then touches it. */
class JourneyDriverTest {
    private class Fake : JourneyDriver {
        val moves = ArrayList<String>()
        var copies = 2
        override fun count(tag: String, merged: Boolean): Int = if (tag == "intro") 0 else if (merged) copies else 1
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) { moves += "scroll $tag" }
        override fun click(tag: String) { moves += "click $tag" }
        override fun idle(ms: Long) { copies = 1; moves += "idle" }
        override fun tags(pattern: Regex) = listOf("key_60", "key_61").filter(pattern::matches)
    }

    @Test
    fun aTapWaitsForOneCopyThenScrollsAndTouches() {
        val d = Fake()
        Journey(d).tap("go")
        assertEquals(listOf("scroll go", "click go"), d.moves.filter { it != "idle" })
        assertEquals("idle", d.moves.first())
        assertEquals(listOf("key_60", "key_61"), Journey(d).tagsMatching(Journey.KEY))
    }
}
