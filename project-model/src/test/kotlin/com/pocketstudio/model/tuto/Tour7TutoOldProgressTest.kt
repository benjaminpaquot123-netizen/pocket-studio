package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fresh review of « fable-repar » (C): the Facile steps cut in pieces (« clap_2 », « bass_end_2 », « make_drop2_2 »…)
 * have ids no progress saved before the cut holds: a course finished then read as unfinished after the update, and
 * came back on a piece. A progress saved before the cut counts a piece done when the step it was cut from is done; one
 * saved since keeps its pieces as they are.
 */
class Tour7TutoOldProgressTest {
    private val piece = Regex("""_\d+$""")

    /** The Facile steps of [style] as they were before the cut: the pieces out. */
    private fun oldIds(style: String): List<String> {
        val out = ArrayList<String>()
        for (c in TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)) {
            var before: TutoStep? = null
            for (s in c.steps) {
                val cut = s.levels == "f" && piece.containsMatchIn(s.id) && before?.check == s.check
                if (!cut) out += s.id
                before = s
            }
        }
        return out
    }

    private fun old(style: String, done: List<String>, current: String = "") =
        """{"style":"$style","level":"Facile","done":[${done.joinToString(",") { "\"$it\"" }}],"current":"$current","active":true,"kit":"builtin"}"""

    @Test
    fun aCourseFinishedBeforeTheCutIsStillFinished() {
        val lost = ArrayList<String>()
        var pieces = 0
        for (style in TutoCourse.STYLES) {
            val ids = oldIds(style)
            val steps = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
            pieces += steps.size - ids.size
            val p = TutoProgress.decode(old(style, ids, current = ids.last()))
            if (p == null) { lost += "$style: not read"; continue }
            val left = steps.filter { it.id !in p.done }.map { it.id }
            if (left.isNotEmpty()) lost += "$style: finished before the cut, ${left.size} steps left: $left"
            if (p.current != ids.last()) lost += "$style: shown « ${p.current} », was « ${ids.last()} »"
            if (p.kit != "builtin" || !p.active || p.level != TutoLevel.Facile) lost += "$style: $p"
        }
        assertTrue("no step cut in pieces: the test checks nothing", pieces > 50)
        assertTrue("${lost.size} lost:\n" + lost.take(30).joinToString("\n"), lost.isEmpty())
    }

    @Test
    fun aCourseUnderWayBeforeTheCutGoesOnAtItsNextStep() {
        // Techno, the clap done, the bass not yet: the clap's piece is done with it, the bass's are still to do.
        val ids = oldIds("techno")
        val upTo = ids.subList(0, ids.indexOf("clap_do") + 1)
        val p = TutoProgress.decode(old("techno", upTo))!!
        val steps = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
        val next = steps.first { it.id !in p.done }.id
        assertEquals("the course goes on at the step after the clap, not at a piece of it", ids[upTo.size], next)
        assertFalse("a piece of a step not done is done", steps.any { piece.containsMatchIn(it.id) && it.id in p.done && it.id != "clap_2" })
    }

    @Test
    fun aProgressSavedSinceTheCutKeepsItsPiecesToDo() {
        // Saved by this version (encode), the clap done and its piece not: read back, the piece is still to do.
        val saved = TutoProgress("techno", TutoLevel.Facile, done = setOf("kick_do", "clap_do"), current = "clap_2").encode()
        val p = TutoProgress.decode(saved)!!
        assertFalse("« clap_2 », to do when saved, is read done", "clap_2" in p.done)
        assertEquals("clap_2", p.current)
        // And saved again, read again: the same.
        assertEquals(p, TutoProgress.decode(p.encode()))
    }
}
