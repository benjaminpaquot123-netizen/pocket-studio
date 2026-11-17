package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, control of « fable-repar » (Pro, cymbal step, German seat): the words said « un seul coup » (« ein einziger
 * Schlag ») while the pad « Cymbale » already played the ride of the Pro course in that part: the step only adds the
 * first cell's hit, so the pad ends with many hits, not one. Every style, both levels, the course played up to its
 * cymbal step, its part made: words that promise a single hit meet an empty cymbal pad.
 */
class Tour7TutoCrashWordsMatchPadTest {
    @Test
    fun theCymbalStepPromisesOneHitOnlyOnAnEmptyPad() {
        var met = 0
        var single = 0
        val faults = ArrayList<String>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                if (s.manual || s.listening || s.check == "shared") continue
                if (s.check == "crash") {
                    // The user's path: the step's buttons (« Créer la partie », « Ouvrir la partie ») until the pad shows.
                    var q = p
                    repeat(6) {
                        val need = TutoPlay.button(s, q, target, TutoPlay.arrive(s, q), level) ?: return@repeat
                        q = TutoPlay.press(need, s, q, target, level)?.first ?: return@repeat
                    }
                    val parts = TutoChecks.partsOf(s)
                    val there = q.allScenes.filter { sc -> parts.any { TutoChecks.isLabel(sc.label, it) } }
                        .sumOf { it.drums.lanes.getOrNull(TutoChecks.CRASH_LANE)?.hits?.size ?: 0 }
                    val words = s.textFor(level)
                    met++
                    if ("un seul coup" in words) {
                        single++
                        if (there > 0) faults += "$style $level: the words say « un seul coup », the cymbal pad already plays $there hits in $parts"
                    }
                }
                p = TutoPlay.play(s, p, target, level).project
            }
        }
        assertTrue("${faults.size} faults over $met cymbal steps ($single promising a single hit):\n" + faults.joinToString("\n"), faults.isEmpty())
        assertTrue("only $met cymbal steps, $single promising a single hit: the test checks too little", met >= 30 && single >= 2)
    }
}
