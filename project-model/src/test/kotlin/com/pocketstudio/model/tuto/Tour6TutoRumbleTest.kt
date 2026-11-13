package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 6, T14: the rumble's cells said once, its pad named. */
class Tour6TutoRumbleTest {
    private val steps: List<Triple<String, TutoStep, TutoLevel>> = TutoCourse.STYLES.flatMap { style ->
        TutoLevel.entries.flatMap { level -> TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.map { Triple(style, it, level) } }
    }

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /** T14: techno's rumble « touche les mêmes cases que le kick » AND « il gronde juste après chaque coup ». */
    @Test
    fun theRumbleIsSaidOnce() {
        val bad = steps.filter { (_, s, l) -> s.check == "lane:rumble" && "mêmes cases" in s.textFor(l) && "après" in s.textFor(l) }
            .map { (st, s, l) -> "$st/${s.id}/${l.code}: ${s.textFor(l)}" }.distinct()
        report("rumble steps that say two places", bad)
        steps.filter { (_, s, l) -> s.check == "lane:rumble" && l == TutoLevel.Facile }.forEach { (st, s, _) ->
            assertTrue("$st/${s.id}: the pad is not named", "« Rumble »" in s.text)
            if ("mêmes cases que le kick" in s.text) {
                val t = TutoTarget.of(st)!!
                assertTrue("$st/${s.id}: not the kick's cells", t.line("rumble")!!.pattern.map { it != '.' } == t.line("kick")!!.pattern.map { it != '.' })
            }
        }
    }
}
