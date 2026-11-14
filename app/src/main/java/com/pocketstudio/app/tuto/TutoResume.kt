package com.pocketstudio.app.tuto

import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress

/**
 * Audit ultra round 3, n° 19 (seat E b, « Ik leer » › Eenvoudig › House a second time): the tutorial already under way
 * for that style and level, in a project still there, is taken up where it was left (never a new project at chapter 1
 * without a word). A course finished starts afresh in a new project, as before.
 */
object TutoResume {
    /** The project of [projects] (most recent first) holding [style]'s course at [level], not finished; null: none. */
    fun pick(projects: List<ProjectInfo>, load: (String) -> TutoProgress?, style: String, level: TutoLevel): Pair<String, TutoProgress>? =
        projects.asSequence().filter { !it.unreadable }
            .mapNotNull { p -> load(p.id)?.let { p.id to it } }
            .firstOrNull { (_, pr) -> pr.style == style && pr.level == level && !finished(pr) }

    /** Every step of its chapters done (Pro's listening steps aside, as [TutoUi.left] counts them). */
    fun finished(pr: TutoProgress): Boolean {
        val steps = TutoCourse.of(pr.style)?.chaptersFor(pr.level)?.flatMap { it.steps }
            ?.filter { !(pr.level == TutoLevel.Pro && it.listening) } ?: return true
        return steps.all { it.id in pr.done }
    }
}
