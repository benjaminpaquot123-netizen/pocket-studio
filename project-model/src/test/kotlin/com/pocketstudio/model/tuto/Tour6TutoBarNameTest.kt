package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, N28: chapter 12 said « « Takt 4 » du Build-up » (fr « « Mesure 4 » ») and no such button exists: the drum grid
 * shows « Mesure » then the numbers 1, 2, 3, 4 (RhythmScreen's bar pages), and nothing at all on a part of one bar.
 * A consigne says « dernière mesure » (or the bar's number), never a « Mesure N » button.
 */
class Tour6TutoBarNameTest {
    @Test
    fun noConsigneNamesABarButtonThatDoesNotExist() {
        val bad = TutoCourse.STYLES.flatMap { style ->
            TutoCourse.of(style)!!.chapters.flatMap { it.steps }.flatMap { s -> listOfNotNull(s.text, s.textPro).filter { Regex("""« Mesure \d+ »""").containsMatchIn(it) }.map { "$style/${s.id}: $it" } }
        }
        assertTrue("${bad.size} consignes name a « Mesure N » button:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
