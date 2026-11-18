package com.pocketstudio.app.human

import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoTarget

/**
 * Tour 7, banc B1 (METHODE-V4 §4.5, « siège court ancré »): a seat anchored at a tuto chapter starts with the project
 * the chapters before it build, each step done as its « Montre-moi » does (or as its button makes a part), so the
 * anchored chapter finds what a person who played them would have (« Montée » to arrange at house's chapter 12).
 */
internal object TutoAnchor {
    /** [p] with chapters 1 to [chapter] − 1 of [style] at [level] done ([skipped]: each step it could not do); null when the style has no course. */
    fun built(p: Project, style: String, level: TutoLevel, chapter: Int, skipped: (String) -> Unit = {}): Project? {
        val course = TutoCourse.of(style) ?: return null
        val target = TutoTarget.of(style) ?: return null
        var now = p
        for (s in course.chaptersFor(level).take(chapter - 1).flatMap { it.steps }) {
            if (TutoChecks.done(s, now, target)) continue
            TutoChecks.show(s, now, target)?.let { now = it; continue }
            val need = TutoChecks.need(s, now, target) ?: continue
            // Tour 8, banc9 (point h): a step the anchor could not do is said, not lost. « Could not » is a need it could
            // not fulfil: a step with nothing to build in the project (listen, read: « kick_hear », « ready ») is no miss.
            TutoChecks.fulfil(need, s, now, target)?.first?.let { now = it } ?: skipped(s.id)
        }
        return now
    }
}
