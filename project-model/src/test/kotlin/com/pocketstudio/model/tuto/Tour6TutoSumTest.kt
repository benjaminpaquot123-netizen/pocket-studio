package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 6, N9: no sum without words, no pronoun with nothing before it. */
class Tour6TutoSumTest {
    private val steps: List<Triple<String, TutoStep, TutoLevel>> = TutoCourse.STYLES.flatMap { style ->
        TutoLevel.entries.flatMap { level -> TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.map { Triple(style, it, level) } }
    }

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /** N9: « le Break et la sienne 16 + 8 »: a sum without words, a pronoun with nothing before it. */
    @Test
    fun noSumWithoutWords() {
        val sum = Regex("""\d+\s*[+=]\s*\d+""")
        val bad = steps.filter { (_, s, l) -> sum.containsMatchIn(s.textFor(l)) || Regex("""\bla sienne\b|\ble sien\b""").containsMatchIn(s.textFor(l)) }
            .map { (st, s, l) -> "$st/${s.id}/${l.code}: ${s.textFor(l)}" }.distinct()
        report("sums or empty pronouns", bad)
    }
}
