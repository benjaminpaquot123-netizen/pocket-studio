package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, F044 (t9-75, uk_garage Pro, « La basse »): the chapter said « Fini » while the piano roll still said
 * « encore 1 note en pointillé » once the bass was copied an octave down. The rule: once every step of a chapter
 * on notes is done, no step of it still shows a dotted note left to write on its track.
 */
class T9L17ChapterDoneNoDottedTest {
    private fun left(s: TutoStep, p: Project, target: TutoTarget): Int {
        val (track, ghosts) = TutoChecks.ghostNotes(s, p, target) ?: return 0
        val have: Set<String> = TutoChecks.notesOf(p, track).map { n -> "${n.pitch}@${n.start}" }.toSet()
        return ghosts.count { n -> "${n.pitch}@${n.start}" !in have }
    }

    @Test
    fun ukGarageBassChapterDoneLeavesNoDottedNote() {
        val style = "uk_garage"
        val course = TutoCourse.of(style)!!
        val target = TutoTarget.of(style)!!
        val chapter = course.chaptersFor(TutoLevel.Pro).first { c -> c.steps.any { it.id == "bass_sub" } }
        var p = target.blank()
        for (s in chapter.steps) {
            if (s.listening) continue
            var tries = 0
            while (!TutoChecks.done(s, p, target, null) && tries++ < 8) {
                val need = TutoChecks.need(s, p, target, null)
                val q = need?.let { TutoChecks.fulfil(it, s, p, target)?.first } ?: TutoChecks.show(s, p, target) ?: break
                if (q == p) break
                p = q
            }
            assertTrue("${s.id} done", TutoChecks.done(s, p, target, null))
        }
        val dotted = chapter.steps.filter { !it.listening }.associate { it.id to left(it, p, target) }.filterValues { it > 0 }
        assertTrue("chapter done, dotted notes left: $dotted", dotted.isEmpty())
    }
}
