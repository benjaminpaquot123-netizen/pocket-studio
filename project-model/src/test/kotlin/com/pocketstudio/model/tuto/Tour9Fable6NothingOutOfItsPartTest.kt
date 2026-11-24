package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot « fable6 » (the « cible-autre-partie » alert of the tutorial: tour 7, seats tuto2 d and fable a V4), every
 * way a step can point out of its part. Acceptance, seen by the user: « Quand je regarde une autre partie que celle de
 * l'étape, rien de l'étape n'y est dessiné : ni case en pointillé, ni note en pointillé, ni pas de la 303, ni croix. »
 * Every style, both levels, every step that works in a named part (its own « part », or the crash, the silence, the
 * roll of the kind), before and after it is done, with every other part of the project shown and a new empty one.
 */
class Tour9Fable6NothingOutOfItsPartTest {
    @Test
    fun aStepOfANamedPartDrawsNothingInAnotherPart() {
        var met = 0
        var looks = 0
        var own = 0
        val faults = ArrayList<String>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            val steps = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
            var p = target.blank()
            for (s in steps) {
                if (s.manual || s.listening || s.check == "shared") continue
                val before = p
                p = TutoPlay.play(s, p, target, level).project
                val named = s.part.isNotBlank() || s.check == "crash" || s.check == "gap" || s.check == "fill"
                if (!named) continue
                met++
                val parts = TutoChecks.partsOf(s)
                for ((moment, q) in listOf("before" to before, "after" to p)) {
                    // Its own part, the step not done yet: something of it is drawn there, or the test would pass on nothing.
                    if (moment == "before") q.allScenes.indices.firstOrNull { i -> parts.any { TutoChecks.isLabel(q.allScenes[i].label, it) } }?.let { i ->
                        val r = q.switchScene(i)
                        if (drawn(s, r, target).isNotEmpty()) own++
                    }
                    for (i in 0..q.allScenes.size) {
                        val label = q.allScenes.getOrNull(i)?.label
                        if (label != null && parts.any { TutoChecks.isLabel(label, it) }) continue
                        val r = q.switchScene(i)
                        // A new part is born with the name the app gives it: not one of the step's.
                        if (parts.any { TutoChecks.isLabel(r.allScenes[r.scene].label, it) }) continue
                        looks++
                        val d = drawn(s, r, target)
                        if (d.isNotEmpty()) faults += "$style $level ${s.id} (${s.check}, part ${parts}), $moment, in « ${r.allScenes[r.scene].label} »: $d"
                    }
                }
            }
        }
        assertTrue("${faults.size} of $looks looks ($met steps):\n" + faults.take(60).joinToString("\n"), faults.isEmpty())
        assertTrue("only $met steps of a named part, $looks looks, $own drawn in their own part: the test checks too little", met >= 150 && looks >= 600 && own >= 60)
    }

    /** What the screens draw of [s] in the part shown of [p]: the four hints the controller hands them. */
    private fun drawn(s: TutoStep, p: com.pocketstudio.model.Project, target: TutoTarget): List<String> = buildList {
        for (ticked in listOf(false, true)) TutoPlay.hint(s, p, target, p.drums.steps, null, ticked)?.takeIf { it.second.isNotEmpty() }?.let { add("cells $it") }
        TutoPlay.notes(s, p, target)?.second?.takeIf { it.isNotEmpty() }?.let { add("${it.size} dotted notes") }
        TutoPlay.acid(s, p, target)?.takeIf { it.isNotEmpty() }?.let { add("${it.size} steps of the 303") }
        TutoPlay.toErase(s, p, target, null)?.filterValues { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }?.let { add("crosses $it") }
    }.distinct()
}
