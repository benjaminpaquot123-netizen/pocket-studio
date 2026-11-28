package com.pocketstudio.model.tuto

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, F138: the tuto's curve (« Je le fais », « Montre-moi », « Poser ce qui manque ») replaced the user's own
 * curve on the same knob, and with every curve lane taken `.take(MAX_LANES)` threw the new one away: the step never
 * ticked. Acceptance: « Je le fais » garde mes courbes et l'étape se coche même quand toutes les pistes de courbe
 * sont prises.
 */
class Tour9TutoCurveKeepsMineTest {
    private class Case(val style: String, val step: TutoStep, val target: TutoTarget, val base: Project, val knob: AutoTarget)

    /** Every curve step of every course, on its example song with no curve, and the knob its curve moves. */
    private fun cases(): List<Case> = TutoCourse.STYLES.flatMap { style ->
        val target = TutoTarget.of(style)!!
        val base = target.song.copy(automation = emptyList())
        TutoCourse.of(style)!!.chapters.flatMap { it.steps }.filter { it.check.startsWith("automation") }.mapNotNull { s ->
            val shown = TutoChecks.show(s, base, target) ?: return@mapNotNull null
            Case(style, s, target, shown.copy(automation = emptyList()), shown.automation.single().target)
        }
    }

    /** A drawn curve on another knob: the reverb send of strip [n] (no curve step moves a send). */
    private fun other(n: Int) = AutomationLane(AutoTarget(AutoTarget.SEND, n, 0, 0), values = List(AutomationLane.STEPS) { if (it == 2) 0.3f + n / 100f else null })

    @Test
    fun theStepTicksWithEveryCurveLaneTaken() {
        val cs = cases()
        assertTrue("curve steps: ${cs.size}", cs.size >= 20)
        for (c in cs) {
            // Fifteen curves drawn, one lane made and left empty: all the lanes there are.
            val mine = List(AutomationLane.MAX_LANES - 1) { other(it) }
            val p = c.base.copy(automation = mine + AutomationLane(AutoTarget(AutoTarget.SEND, 15, 1, 0)))
            val shown = TutoChecks.show(c.step, p, c.target)
            assertTrue("${c.style}/${c.step.id}: nothing done with every lane taken", shown != null)
            assertTrue("${c.style}/${c.step.id}: does not tick with every lane taken", TutoChecks.done(c.step, shown!!, c.target))
            for (l in mine) assertTrue("${c.style}/${c.step.id}: my curve ${l.target} lost", l in shown.automation)
        }
    }

    @Test
    fun myOwnCurveOnTheSameKnobIsKept() {
        for (c in cases()) {
            // My loop curve on that very knob (a point at step 3), drawn before the step.
            val loop = AutomationLane(c.knob, values = List(AutomationLane.STEPS) { if (it == 3) 0.42f else null })
            val shown = TutoChecks.show(c.step, c.base.copy(automation = listOf(loop)), c.target)!!
            assertTrue("${c.style}/${c.step.id}: does not tick over my loop curve", TutoChecks.done(c.step, shown, c.target))
            val lane = shown.automation.single { it.target == c.knob }
            assertTrue(
                "${c.style}/${c.step.id}: my loop curve is gone from the other parts",
                lane.mode == AutomationLane.PER_SCENE && lane.sceneValues.values.any { it.getOrNull(3) == 0.42f },
            )
            // My song curve on that knob, far after the step's part (its last bar and the one after it).
            val spans = TutoChecks.spansOf(c.base, TutoChecks.partsOf(c.step).first()).ifEmpty { listOf(0 to 8) }
            val far = spans.maxOf { (at, bars) -> at + bars } + 3
            val song = AutomationLane(c.knob, mode = AutomationLane.SONG).withSongPoint(far, 0.42f)
            val over = TutoChecks.show(c.step, c.base.copy(automation = listOf(song)), c.target)!!
            assertTrue("${c.style}/${c.step.id}: does not tick over my song curve", TutoChecks.done(c.step, over, c.target))
            val sl = over.automation.single { it.target == c.knob }
            if (sl.mode == AutomationLane.SONG) assertEquals("${c.style}/${c.step.id}: my song point lost", 0.42f, sl.songValues.getOrNull(far))
        }
    }
}
