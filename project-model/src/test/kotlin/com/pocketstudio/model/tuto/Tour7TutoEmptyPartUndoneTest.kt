package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.showing
import com.pocketstudio.model.syncScenes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, control of « fable-repar » (seat d, V2): a step ticked in a part, that part emptied (or another, empty, given
 * its name): the step is to do again ([TutoPlay.undone]), in every style and level; while its part holds what the
 * course laid, it keeps its tick.
 */
class Tour7TutoEmptyPartUndoneTest {
    /** [p] whose parts named as one of [labels] hold nothing any more (their name kept). */
    private fun emptied(p: Project, labels: List<String>): Project {
        val q = p.syncScenes()
        return q.showing(q.allScenes.map { s -> if (labels.any { TutoChecks.isLabel(s.label, it) }) Scene(name = s.name, label = s.label) else s }, q.scene)
    }

    private fun inAPart(s: TutoStep): Boolean {
        val c = s.check
        return !s.manual && !s.listening && !c.startsWith("changed:") && (c == "fill" || c == "gap" || c.startsWith("crash") || c.startsWith("part:") || s.part.isNotBlank())
    }

    @Test
    fun aStepOfAnEmptiedPartIsToDoAgain() {
        val bad = ArrayList<String>()
        var seen = 0
        var transitions = 0
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                var p = target.blank()
                val ticked = ArrayList<TutoStep>()
                for (s in course.chaptersFor(level).flatMap { it.steps }) {
                    if (s.manual || s.listening) continue
                    val out = TutoPlay.play(s, p, target, level)
                    p = out.project
                    if (inAPart(s) && TutoPlay.done(s, p, target, out.base)) ticked += s
                }
                for (s in ticked) {
                    seen++
                    val parts = if (s.check.startsWith("part:")) listOf(s.check.removePrefix("part:").substringBeforeLast(':')) else TutoChecks.partsOf(s)
                    // The course played to its end: a transition is read as it is now, the others by their part being there.
                    val transition = s.check == "fill" || s.check == "gap" || s.check.startsWith("crash")
                    if (transition) transitions++
                    if (!transition && TutoPlay.undone(s, p, target)) bad += "$style ${level.name} ${s.id}: to do again though its part $parts is there"
                    if (!TutoPlay.undone(s, emptied(p, parts), target)) bad += "$style ${level.name} ${s.id}: still ticked with $parts emptied"
                }
            }
        }
        assertTrue("only $seen steps in a part played ($transitions transitions)", seen > 300 && transitions > 100)
        assertTrue("${bad.size} of $seen:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun aStepOfTheWholeLoopKeepsItsTick() {
        var seen = 0
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style) ?: continue
            for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }.filterNot(::inAPart)) {
                seen++
                assertTrue("$style ${s.id}", !TutoPlay.undone(s, target.blank(), target))
            }
        }
        assertTrue("only $seen", seen > 500)
    }
}
