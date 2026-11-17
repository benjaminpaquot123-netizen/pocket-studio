package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, tuto3 (seat c, ko, techno chapter 10 « Construis tes parties »): reached with the chapters before it built
 * as the bench's amenée does (each step as « Montre-moi » or its button does), step 1/7 offered no « Créer la partie
 * « Intro » » and the chapter could not be finished without « Suivant ». Every style and level: the chapter that makes
 * the parts, reached that way or from a blank project, offers each part's button and its touch validates the step.
 */
class Tour7TutoPartChapterFromAnyStateTest {
    private fun built(start: Project, chapters: List<TutoChapter>, target: TutoTarget): Project {
        var now = start
        for (s in chapters.flatMap { it.steps }) {
            if (TutoChecks.done(s, now, target)) continue
            now = TutoChecks.show(s, now, target) ?: TutoChecks.need(s, now, target)?.let { TutoChecks.fulfil(it, s, now, target)?.first } ?: now
        }
        return now
    }

    @Test
    fun eachPartStepIsMadeByItsButton() {
        val bad = ArrayList<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                val chapters = course.chaptersFor(level)
                val at = chapters.indexOfFirst { c -> c.steps.any { it.check.startsWith("part:") } }.takeIf { it >= 0 } ?: continue
                for ((how, start) in listOf("built" to built(target.blank(), chapters.take(at), target), "blank" to target.blank())) {
                    var p = start
                    for (s in chapters[at].steps) {
                        if (TutoChecks.done(s, p, target)) continue
                        if (!s.check.startsWith("part:")) { p = TutoChecks.show(s, p, target) ?: p; continue }
                        checked++
                        val need = TutoChecks.need(s, p, target)
                        val q = need?.let { TutoChecks.fulfil(it, s, p, target)?.first }
                        when {
                            need == null -> bad += "$style ${level.name} $how ch.${at + 1} ${s.id}: no button"
                            q == null -> bad += "$style ${level.name} $how ch.${at + 1} ${s.id}: $need made nothing"
                            !TutoChecks.done(s, q, target) -> bad += "$style ${level.name} $how ch.${at + 1} ${s.id}: $need not validated"
                        }
                        p = q ?: TutoChecks.show(s, p, target) ?: p
                    }
                }
            }
        }
        assertTrue("no part step checked", checked > 0)
        assertTrue("${bad.size} part steps not made by their button:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
