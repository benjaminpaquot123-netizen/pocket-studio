package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, lot 1 D8: a chapter opened from the list (goTo) whose step names a part the user never made (house, chapter
 * « Range tes parties » without « Construis tes parties »: no « Montée ») said nothing and offered nothing: the song
 * could not be ordered. Every style and level, the course played with its part steps left out: each later step that
 * names a part the course makes, absent, offers « Créer la partie » for it, and its touch makes it.
 */
class Tour7TutoGoToMissingPartTest {
    private fun has(p: Project, label: String) = p.allScenes.any { s ->
        !s.isEmpty && (s.label.trim().equals(label, true) || s.label.trim().equals(Scene.shownLabel(label), true))
    }

    private fun cited(s: TutoStep): List<String> = when {
        s.check == "song:order" || s.check == "song:phrases" -> s.song.split(',').map { it.substringBeforeLast(':').trim() }.filter { it.isNotEmpty() }
        s.check == "fill" || s.check == "gap" || s.check == "crash" || s.check.startsWith("automation") -> TutoChecks.partsOf(s)
        s.check.startsWith("changed:drums:") -> listOf(s.check.removePrefix("changed:drums:"))
        else -> emptyList()
    }

    @Test
    fun aNamedPartMissingIsOfferedAndMade() {
        val bad = ArrayList<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                val steps = course.chaptersFor(level).flatMap { it.steps }
                val made = steps.filter { it.check.startsWith("part:") }.map { it.check.removePrefix("part:").substringBeforeLast(':') }.toSet()
                var p = target.blank()
                for (s in steps) {
                    if (s.check.startsWith("part:")) continue
                    if (!TutoChecks.done(s, p, target)) {
                        val missing = cited(s).filter { it in made && !has(p, it) && !(it in TutoTarget.ENDINGS && has(p, "Intro")) }
                        if (missing.isNotEmpty()) {
                            checked++
                            val need = TutoChecks.need(s, p, target)
                            val q = (need as? TutoNeed.Part)?.let { TutoChecks.fulfil(it, s, p, target)?.first }
                            if (need !is TutoNeed.Part || need.label !in missing) bad += "$style ${level.name} ${s.id}: ${missing.first()} missing, need $need"
                            else if (q == null || !has(q, need.label)) bad += "$style ${level.name} ${s.id}: « Créer la partie ${need.label} » made nothing"
                            else p = q
                        }
                    }
                    if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
                }
            }
        }
        assertTrue("no step met a missing part", checked > 0)
        assertTrue("${bad.size} missing parts not offered:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
