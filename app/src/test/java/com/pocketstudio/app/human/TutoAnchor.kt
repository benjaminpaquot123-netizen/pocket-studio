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
    /** [p] with chapters 1 to [chapter] − 1 of [style] at [level] done; null when the style has no course. */
    fun built(p: Project, style: String, level: TutoLevel, chapter: Int): Project? {
        val course = TutoCourse.of(style) ?: return null
        val target = TutoTarget.of(style) ?: return null
        var now = p
        for (s in course.chaptersFor(level).take(chapter - 1).flatMap { it.steps }) {
            if (TutoChecks.done(s, now, target)) continue
            now = TutoChecks.show(s, now, target)
                ?: TutoChecks.need(s, now, target)?.let { TutoChecks.fulfil(it, s, now, target)?.first }
                ?: now
        }
        return now
    }
}
