package com.pocketstudio.model.tuto

import com.pocketstudio.model.tuto.TutoPlay.End
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, audit « Fable », rule R-B: a part is never born done. A course played in order from the tutorial's empty
 * project, each step by its buttons then « Montre-moi »: the silence, the roll and the crash of a part the course
 * just made are still to do when their step comes (the seats saw « Bravo » on arrival, nothing to erase).
 */
class Tour7TutoPartNeverBornDoneTest {
    @Test
    fun theSilenceTheRollAndTheCrashAreToDoWhenTheirStepComes() {
        val born = ArrayList<String>()
        var seen = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            var p = target.blank()
            for (c in TutoCourse.of(style)!!.chaptersFor(level)) {
                for (s in c.steps.filterNot { it.manual || it.listening || it.check == "shared" }) {
                    val o = TutoPlay.play(s, p, target)
                    if (s.check == "gap" || s.check == "fill" || s.check == "crash") {
                        seen++
                        if (o.end == End.Already) born += "$style $level ${s.id} [${s.check}]"
                    }
                    p = o.project
                }
            }
        }
        assertTrue("nothing played", seen > 60)
        assertTrue("${born.size} steps born done on $seen\n" + born.joinToString("\n"), born.isEmpty())
    }
}
