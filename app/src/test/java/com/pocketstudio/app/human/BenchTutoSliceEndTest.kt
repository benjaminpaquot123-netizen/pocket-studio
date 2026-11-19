package com.pocketstudio.app.human

import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 (seats t9-07, t9-09, t9-11): every step of the slice done, the screen on « Chapitre 5 · Fait », and the journal
 * still at `step=<the chapter's last step>`: by design a step ticked stays shown with its « Bravo » until « Chapitre
 * suivant », a touch the mission never asks for. The round's oracle (`step=<the next chapter's first step>`) could not
 * be reached. The journal says which chapters are over, whatever step is shown: each course, each level, each slice.
 */
class BenchTutoSliceEndTest {
    @Test
    fun theJournalSaysTheChaptersOverWhileTheLastStepStaysShown() {
        val missed = mutableListOf<String>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.values()) {
            val chapters = TutoCourse.of(style)!!.chaptersFor(level)
            for (n in 1..chapters.size) {
                val played = chapters.take(n).flatMap { it.steps }
                val line = HumanTester.tutoState(TutoProgress(style, level, done = played.map { it.id }.toSet(), current = played.last().id))
                val want = "chapters=" + (1..n).joinToString(",")
                if (" $want " !in "$line ") missed += "$style/$level 1-$n: « $line » without « $want »"
            }
        }
        assertTrue("${missed.size} slices whose end the journal cannot say:\n" + missed.take(10).joinToString("\n"), missed.isEmpty())
    }

    /** A chapter with a step left is not over; in Pro an example not listened to holds nothing back (the app's own rule). */
    @Test
    fun aChapterWithAStepLeftIsNotOver() {
        for (style in TutoCourse.STYLES) for (level in TutoLevel.values()) {
            val chapters = TutoCourse.of(style)!!.chaptersFor(level)
            val held = chapters.first().steps.firstOrNull { level == TutoLevel.Facile || !it.listening } ?: continue
            val done = chapters.flatMap { it.steps }.map { it.id }.toSet() - held.id
            val line = HumanTester.tutoState(TutoProgress(style, level, done = done, current = held.id))
            assertTrue("$style/$level: « $line »", " chapters=" + (2..chapters.size).joinToString(",") in line && " chapters=1" !in line)
        }
    }
}
