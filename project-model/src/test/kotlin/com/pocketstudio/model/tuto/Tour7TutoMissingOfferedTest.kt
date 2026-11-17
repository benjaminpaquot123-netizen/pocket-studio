package com.pocketstudio.model.tuto

import com.pocketstudio.model.tuto.TutoPlay.End
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, audit « Fable », rule R-C: what a step lacks is always offered. A chapter opened from the list on the
 * tutorial's empty project (chapters 10 and 12 in the seats' reports: « Créer la partie » that made nothing, no
 * « Montre-moi ») always has a button that changes the project or « Montre-moi », and they tick the step.
 */
class Tour7TutoMissingOfferedTest {
    @Test
    fun aChapterOpenedOnTheEmptyProjectIsNeverStuck() {
        val stuck = ArrayList<String>()
        var played = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            for (c in TutoCourse.of(style)!!.chaptersFor(level)) {
                var p = target.blank()
                for (s in c.steps.filterNot { it.manual || it.listening || it.check == "shared" }) {
                    val o = TutoPlay.play(s, p, target)
                    played++
                    if (o.end == End.DeadEnd || o.end == End.NeverDone) stuck += "$style $level ${s.id} [${s.check}]: ${o.end} ${o.why}"
                    p = o.project
                }
            }
        }
        assertTrue("nothing played", played > 1000)
        assertTrue("${stuck.size} steps stuck on $played\n" + stuck.take(40).joinToString("\n"), stuck.isEmpty())
    }

    /** A course played in order lacks nothing: « Poser ce qui manque » is never offered there. */
    @Test
    fun aCoursePlayedInOrderIsNeverOfferedWhatIsMissing() {
        val offered = ArrayList<String>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            var p = target.blank()
            for (c in TutoCourse.of(style)!!.chaptersFor(level)) for (s in c.steps.filterNot { it.manual || it.listening || it.check == "shared" }) {
                p = TutoPlay.play(s, p, target) { _, need -> if (need is TutoNeed.Course) offered += "$style $level ${s.id} [${s.check}]: $need" }.project
            }
        }
        assertTrue("${offered.size} steps offered what is missing in a course played in order\n" + offered.distinct().take(40).joinToString("\n"), offered.isEmpty())
    }
}
