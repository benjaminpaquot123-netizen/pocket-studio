package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.renameScene
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, control of « fable-repar » (seat d): a part of the song renamed (a name the course does not use), a later step that works in it offers
 * « Créer la partie »: the part it makes is played by the song, at the place the course's order gives it, in every
 * style and level.
 */
class Tour7TutoMadePartPlacedTest {
    @Test
    fun thePartMadeFromALaterStepIsInTheSongAtItsPlace() {
        val bad = ArrayList<String>()
        var seen = 0
        val styles = HashSet<String>()
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                val steps = course.chaptersFor(level).flatMap { it.steps }
                var p = target.blank()
                for (s in steps) if (!s.manual && !s.listening) p = TutoPlay.play(s, p, target, level).project
                val order = steps.firstOrNull { it.check == "song:order" } ?: continue
                val wanted = TutoChecks.orderOf(order, p).first
                for (s in steps) {
                    if (s.manual || s.listening || s.check.startsWith("part:") || s.check.startsWith("song:")) continue
                    for (label in wanted) {
                        val at = p.allScenes.indexOfFirst { TutoChecks.isLabel(it.label, label) }
                        if (at < 0 || p.song.none { it.scene == at }) continue
                        val q = p.renameScene(at, "Mon passage")
                        val need = TutoPlay.button(s, q, target, TutoPlay.arrive(s, q), level) as? TutoNeed.Part ?: continue
                        if (!TutoChecks.isLabel(need.label, label)) continue
                        seen++
                        styles += style
                        val r = TutoPlay.press(need, s, q, target, level)?.first
                        if (r == null) { bad += "$style ${level.name} ${s.id}: « Créer la partie « $label » » does nothing"; continue }
                        val made = r.allScenes.indexOfFirst { TutoChecks.isLabel(it.label, label) }
                        val names = r.song.map { r.allScenes[it.scene].label }
                        if (r.song.none { it.scene == made }) { bad += "$style ${level.name} ${s.id}: « $label » made, the song never plays it: $names"; continue }
                        val (want, have) = TutoChecks.orderOf(order, r)
                        if (want != have) bad += "$style ${level.name} ${s.id}: « $label » out of its place: $names"
                        if (r.song.filter { it.scene != made } != q.song) bad += "$style ${level.name} ${s.id}: the song's other parts changed: $names"
                    }
                }
            }
        }
        assertTrue("only $seen buttons played in $styles", seen >= 20 && styles.size >= 4)
        assertTrue("${bad.size} of $seen:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
