package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, F034 (t9-24, drill Pro, « Les transitions »): the step says « appui long sur le dernier coup, puis
 * « Roulement » ×4 » and « Montre-moi » laid four hits louder and louder, no roll. The rule: « Montre-moi » shows the
 * gesture the step's words ask for, and a step asking for a roll ticks on a roll.
 */
class T9L17DemoShowsTheGestureTest {
    /** Every Pro course step whose « fill » asks for « Roulement », with its style. */
    private fun rollFills(): List<Pair<String, TutoStep>> = TutoCourse.STYLES.flatMap { style ->
        TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }
            .filter { s -> s.check == "fill" && listOfNotNull(s.text, s.textPro).any { "« Roulement »" in it } }.map { style to it }
    }

    /** [style]'s Pro course walked up to step [id] (each earlier step done the way the course helps). */
    private fun upTo(style: String, id: String): Project {
        val target = TutoTarget.of(style)!!
        var p = target.blank()
        for (s in TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }) {
            if (s.id == id) return p
            if (s.listening) continue
            var tries = 0
            while (!TutoChecks.done(s, p, target, null) && tries++ < 8) {
                val need = TutoChecks.need(s, p, target, null)
                val q = need?.let { TutoChecks.fulfil(it, s, p, target)?.first } ?: TutoChecks.show(s, p, target) ?: break
                if (q == p) break
                p = q
            }
        }
        error("no step $id")
    }

    @Test
    fun aFillAskingForARollShowsARoll() {
        val fills = rollFills()
        assertTrue("drill's fill among them: $fills", fills.any { it.first == "drill" })
        for ((style, s) in fills) {
            val target = TutoTarget.of(style)!!
            val p = upTo(style, s.id)
            val shown = TutoChecks.show(s, p, target) ?: error("$style/${s.id}: nothing shown")
            val i = TutoChecks.sceneLabelled(shown, TutoChecks.partsOf(s).first())
            val rolled = shown.allScenes[i].drums.lanes.any { l -> l.hits.values.any { it.repeats >= 3 } }
            assertTrue("$style/${s.id}: « Montre-moi » rolls", rolled)
            assertTrue("$style/${s.id}: shown is done", TutoChecks.done(s, shown, target, null))
        }
    }
}
