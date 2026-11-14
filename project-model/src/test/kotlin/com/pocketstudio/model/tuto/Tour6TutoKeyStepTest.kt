package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 6, N3: the key's step says where the key is set. */
class Tour6TutoKeyStepTest {
    private val steps: List<Triple<String, TutoStep, TutoLevel>> = TutoCourse.STYLES.flatMap { style ->
        TutoLevel.entries.flatMap { level -> TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.map { Triple(style, it, level) } }
    }

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /** N3: « règle la tonalité sur Fa phrygien… » said neither where nor how: the key's step names its button. */
    @Test
    fun theKeyStepSaysWhereItIsSet() {
        val bad = steps.filter { (_, s, l) -> s.check == "key" && "« Je le fais »" !in s.textFor(l) }.map { (st, s, l) -> "$st/${s.id}/${l.code}: ${s.textFor(l)}" }.distinct()
        report("key steps that do not say where", bad)
    }
}
