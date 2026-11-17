package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, seat 2a (ko, techno, `amenee=tuto:techno:5`: the course started at its bass chapter, its drum chapters never
 * played): at « Créer la partie « Intro » » the part was made (« + B · 인트로 ») but the step stayed undone
 * (« 이 단계는 아직 안 끝났어요 »). The Groove had no drums, so an Intro of its kick, hats and rim alone was empty.
 * Every style and level, the course played from its first chapter of notes (its drums never written): its first part step's buttons make a part that holds.
 */
class Tour7TutoIntroFromLaterChapterTest {
    @Test
    fun theFirstPartIsMadeFromAGrooveWithoutDrums() {
        val bad = ArrayList<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                val chapters = course.chaptersFor(level)
                val from = chapters.indexOfFirst { c -> c.steps.any { it.check.startsWith("notes:") } }.takeIf { it >= 0 } ?: continue
                var p = target.blank()
                for (s in chapters.drop(from).flatMap { it.steps }) {
                    if (s.check.startsWith("part:")) {
                        // Its buttons as the app plays them ([TutoPlay.play]): « Poser ce qui manque » first when the
                        // loop is too poor for the part (tour 7, R-C), then « Créer la partie »; no « Montre-moi ».
                        val o = TutoPlay.play(s, p, target)
                        val scenes = o.project.allScenes.size - p.allScenes.size
                        if (scenes <= 0) bad += "$style ${level.name} ${s.id}: no part made (${o.end} ${o.why})"
                        else if (o.end != TutoPlay.End.Done || o.before != null || !TutoChecks.done(s, o.project, target)) bad += "$style ${level.name} ${s.id}: part made, step not done (${o.end})"
                        checked++
                        break
                    }
                    if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
                }
            }
        }
        assertTrue("no part step met", checked > 0)
        assertTrue("${bad.size} first parts refused:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
