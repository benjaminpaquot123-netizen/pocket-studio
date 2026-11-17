package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, tuto3 (seat d, fr, House « Les transitions » opened on an empty project): the panel never said the
 * « Montée » was missing, the crash was dotted in « A », and the Drop 2's four maracas never ticked. The rule: every
 * chapter of every style and level, opened on a blank project, is finished by what the panel offers (its button,
 * then « Montre-moi »), with the baseline the panel takes when the step begins (re-taken once its part is made).
 */
class Tour7TutoEveryChapterFromBlankTest {
    /**
     * The steps on the song's parts and their transitions (made, named, ordered, filled, crashed, varied): what a chapter
     * opened on an empty project still finishes. The mix and the sound (volumes, knobs, effects, curves on a track) ask
     * for the instruments the earlier chapters make: out of this rule (docs/decisions/tour-7-tuto3.md).
     */
    private fun projectChecked(s: TutoStep) = s.check.startsWith("part:") || s.check.startsWith("label:") || s.check.startsWith("song:") ||
        s.check in setOf("crash", "crash:once", "fill", "gap") || s.check.startsWith("changed:drums:")

    private fun base(s: TutoStep, p: Project): Any? =
        s.check.takeIf { it.startsWith("changed:") }?.let { TutoChecks.signature(it.removePrefix("changed:"), p) }

    @Test
    fun everyChapterFinishesFromBlank() {
        val bad = ArrayList<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                course.chaptersFor(level).forEachIndexed { ci, chapter ->
                    var p = target.blank()
                    for (s in chapter.steps) {
                        if (!projectChecked(s)) continue
                        var baseline = base(s, p)
                        if (TutoChecks.done(s, p, target, baseline)) continue
                        checked++
                        var tries = 0
                        while (!TutoChecks.done(s, p, target, baseline) && tries++ < 8) {
                            val need = TutoChecks.need(s, p, target, baseline)
                            val q = need?.let { TutoChecks.fulfil(it, s, p, target)?.first }
                                ?: TutoChecks.show(s, p, target) ?: break
                            if (baseline == null) baseline = base(s, q)
                            if (q == p) break
                            p = q
                        }
                        if (!TutoChecks.done(s, p, target, baseline)) bad += "$style ${level.name} ch.${ci + 1} ${s.id} (${s.check})"
                    }
                }
            }
        }
        assertTrue("no step checked", checked > 50)
        assertTrue("${bad.size} steps not finished from a blank project:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
