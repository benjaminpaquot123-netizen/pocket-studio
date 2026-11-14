package com.pocketstudio.app.tuto

import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tour 6, N6 (the app's part): « ajoute quelques coups … dans le Drop 2 » (a "changed:drums:<part>" step) showed no
 * pad: [TutoUi.pad] was null, so « Je le fais » left the grid on whatever pad was chosen. The pad the step names
 * ([com.pocketstudio.model.tuto.TutoStep.target], its first role), else the open hat the course's own example uses.
 */
class Tour6TutoPanelVariationPadTest {
    @Test
    fun aDrumVariationStepHasItsPad() {
        val style = "afro_house"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val ch = chapters.indexOfFirst { c -> c.steps.any { it.check.startsWith("changed:drums:") } }
        val at = chapters[ch].steps.indexOfFirst { it.check.startsWith("changed:drums:") }
        val step = chapters[ch].steps[at]
        fun ui(target: String): TutoUi {
            val list = chapters.mapIndexed { i, c -> if (i == ch) c.copy(steps = c.steps.mapIndexed { j, s -> if (j == at) s.copy(target = target) else s }) else c }
            return TutoUi(TutoProgress(style, TutoLevel.Facile, current = step.id), list, ch, at)
        }
        assertEquals("the pad its target names", TutoChecks.laneOf("perc"), ui("perc,open_hat").pad)
        assertNotNull("no target: still a pad (the open hat)", ui("").pad)
        assertEquals(TutoChecks.laneOf("open_hat"), ui("").pad)
    }
}
