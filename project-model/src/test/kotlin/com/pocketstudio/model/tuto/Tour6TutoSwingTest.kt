package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tour 6, T5 / T17 / N27: the swing slider shows swing / [Project.MAX_SWING] in %, the check compared the raw value.
 * « mets le swing à 25 » (house) was 0.1875, never ticked, and « Montre-moi » showed 33 %. Now: the number the step
 * writes is the one the slider shows once the step is ticked, « Montre-moi » puts that very number, and a finger a few
 * points off still ticks it.
 */
class Tour6TutoSwingTest {
    private fun shown(swing: Double): Int = (swing / Project.MAX_SWING * 100).roundToInt()
    private fun raw(percent: Int): Double = percent / 100.0 * Project.MAX_SWING

    /** The slider's number a consigne asks for: « N % », or « swing à N ». */
    private fun asked(text: String): Int? =
        (Regex("""(\d+)\s?%""").find(text) ?: Regex("""swing à (\d+)""").find(text))?.groupValues?.get(1)?.toInt()

    @Test
    fun theNumberWrittenIsTheNumberShownAndTicks() {
        val checked = mutableListOf<String>()
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }.filter { it.check == "swing" }) {
                val want = shown(target.style.swing)
                for (text in listOfNotNull(s.text, s.textPro)) {
                    val n = asked(text)
                    assertEquals("$style/${s.id}: the slider shows $want % when ticked, the step says: $text", want, n)
                }
                val blank = target.blank()
                // The value written, and a finger 3 points off either way, tick it.
                for (d in -3..3) assertTrue("$style/${s.id}: ${want + d} % on the slider is not ticked", TutoChecks.done(s, blank.copy(swing = raw(want + d)), target))
                assertFalse("$style/${s.id}: no swing ticked", TutoChecks.done(s, blank.copy(swing = 0.0), target))
                assertFalse("$style/${s.id}: ${want + 8} % ticked", TutoChecks.done(s, blank.copy(swing = raw(want + 8)), target))
                // « Montre-moi » puts the very number the step writes.
                val demo = TutoChecks.show(s, blank, target)!!
                assertEquals("$style/${s.id}: « Montre-moi » shows another number", want, shown(demo.swing))
                assertTrue(TutoChecks.done(s, demo, target))
                checked += "$style/${s.id}"
            }
        }
        assertTrue("no swing step checked", checked.size >= 15)
    }
}
