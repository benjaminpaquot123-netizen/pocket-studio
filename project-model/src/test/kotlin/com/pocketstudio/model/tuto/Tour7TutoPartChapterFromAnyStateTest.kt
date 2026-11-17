package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
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
                        val label = s.check.removePrefix("part:").substringBeforeLast(':')
                        val absent = p.allScenes.none { !it.isEmpty && (it.label.trim().equals(label, ignoreCase = true) || it.label.trim().equals(Scene.shownLabel(label), ignoreCase = true)) }
                        val need = TutoChecks.need(s, p, target)
                        val q = need?.let { TutoChecks.fulfil(it, s, p, target)?.first }
                        // A part whose bass the user then writes (the jump up's Drop 2): the button makes the part,
                        // « Montre-moi » writes the rest; any other part is validated by its button alone.
                        val after = q?.takeIf { s.transpose != 0 && !TutoChecks.done(s, it, target) }?.let { TutoChecks.show(s, it, target) } ?: q
                        when {
                            absent && need == null -> bad += "$style ${level.name} $how ch.${at + 1} ${s.id}: no button"
                            need != null && q == null -> bad += "$style ${level.name} $how ch.${at + 1} ${s.id}: $need made nothing"
                            after != null && !TutoChecks.done(s, after, target) -> bad += "$style ${level.name} $how ch.${at + 1} ${s.id}: $need not validated"
                            after == null && TutoChecks.show(s, p, target)?.let { TutoChecks.done(s, it, target) } != true ->
                                bad += "$style ${level.name} $how ch.${at + 1} ${s.id}: « Montre-moi » not validated"
                        }
                        p = after ?: TutoChecks.show(s, p, target) ?: p
                    }
                }
            }
        }
        assertTrue("no part step checked", checked > 0)
        assertTrue("${bad.size} part steps not made by their button:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
