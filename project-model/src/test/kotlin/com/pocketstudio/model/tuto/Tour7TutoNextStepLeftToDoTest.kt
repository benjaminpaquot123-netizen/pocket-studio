package com.pocketstudio.model.tuto

import com.pocketstudio.model.tuto.TutoPlay.End
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, audit « Fable », rule R-A: a step that asks for a gesture is to do when it comes. A course played in order
 * at its level, each step by its buttons then « Montre-moi »: what a step shows never does the next one's lesson
 * (the soft claps, the hats' strength, the triplets, the second bass bar, the pump were ticked before being asked).
 */
class Tour7TutoNextStepLeftToDoTest {
    @Test
    fun aStepThatAsksForAGestureIsToDoWhenItComes() {
        val already = ArrayList<String>()
        var seen = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            var p = target.blank()
            for (c in TutoCourse.of(style)!!.chaptersFor(level)) {
                for (s in c.steps.filterNot { it.manual || it.listening || it.check == "shared" }) {
                    val o = TutoPlay.play(s, p, target)
                    if (TutoPlay.asksAGesture(s)) {
                        seen++
                        if (o.end == End.Already) already += "$style $level ${s.id} [${s.check}]"
                    }
                    p = o.project
                }
            }
        }
        assertTrue("nothing played", seen > 500)
        assertTrue("${already.size} steps ticked on arrival on $seen\n" + already.joinToString("\n"), already.isEmpty())
    }
}
