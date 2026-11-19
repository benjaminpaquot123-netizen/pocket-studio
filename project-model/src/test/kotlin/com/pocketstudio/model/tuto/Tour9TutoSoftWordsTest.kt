package com.pocketstudio.model.tuto

import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, F031: a drum step whose words ask for soft hits (« un fort, un doux », « deux petits coups doux », « ses
 * fantômes ») ticked with every hit at 100 %: only the steps naming the « Doux » button were checked. Now every step
 * whose words, in every level, ask for soft hits wants its soft cells soft, unless a later step of its course sets
 * the strength (deep house's « velocity:hat »).
 */
class Tour9TutoSoftWordsTest {
    private val words = Regex("(?i)« Doux »|\\bdoux\\b|\\bdouces?\\b|fantômes?|moins fort|discret|deux forces")

    @Test
    fun stepsThatAskForSoftHitsCheckTheirStrength() {
        val checked = mutableListOf<String>()
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            val all = TutoCourse.of(style)!!.chapters.flatMap { it.steps }
            for ((i, s) in all.withIndex()) {
                if (!s.check.startsWith("lane:") || !listOfNotNull(s.text, s.textPro).all { words.containsMatchIn(it) }) continue
                val role = TutoChecks.argOf(s.check)
                if (all.drop(i + 1).any { it.check == "velocity:$role" }) continue
                val line = target.line(role, TutoChecks.partOf(s)) ?: continue
                if (StarterWizard.hits(line, line.pattern.length).values.none { it.velocity < 0.5f }) continue
                val shown = TutoChecks.show(s, target.blank(), target)!!
                assertTrue("$style/${s.id}: « Montre-moi » does not tick it", TutoChecks.done(s, shown, target))
                val lane = TutoChecks.laneOf(role)
                val soft = shown.drums.lanes[lane].hits.filterValues { it.velocity < 0.5f }.keys
                assertTrue("$style/${s.id}: no soft cell shown", soft.isNotEmpty())
                val loud = shown.copy(drums = shown.drums.copy(lanes = shown.drums.lanes.mapIndexed { n, l ->
                    if (n != lane) l else l.copy(hits = l.hits.mapValues { (k, h) -> if (k in soft) h.copy(velocity = 1f) else h })
                }))
                assertFalse("$style/${s.id}: its soft hits played at 100 % tick it", TutoChecks.done(s, loud, target))
                assertTrue("$style/${s.id}: only the softness missing is not said", TutoChecks.onlySoftnessMissing(s, loud, target))
                checked += "$style/${s.id}"
            }
        }
        assertTrue("disco's snare is not checked: $checked", "disco/snare_do" in checked)
        assertTrue("detroit's hats are not checked: $checked", "detroit/hat_do" in checked)
    }

    @Test
    fun aStepWhoseStrengthComesLaterTicksLoud() {
        // Deep house: « hat_vel » sets the hats' strength; « hat_do » before it ticks with the hits at any strength.
        val s = TutoCourse.of("deep_house")!!.chapters.flatMap { it.steps }.first { it.id == "hat_do" }
        assertFalse(TutoChecks.asksSoft(s))
    }
}
