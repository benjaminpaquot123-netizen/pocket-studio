package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, P7 (plan § 4, replaces Tour7TutoTwoGesturesTest): one moment per Facile consigne. For every Facile step,
 * (a) the text names no engine button (the panel shows the need's button as its own moment) and (b) it asks for one
 * series of gestures at most (the tour 7 counter, reading verbs out). The steps not yet rewritten (blocs 1 to 4) are in
 * [TutoMoments.EXCEPTIONS]; they still keep the former ceiling of two series. Part (c), the phrase on screen, is the
 * app's TutoOneMomentShownTest.
 */
class TutoOneMomentTest {
    @Test fun theCounterCountsSeriesNotReadingNorTheEnginesButton() {
        assertEquals(1, TutoMoments.series("À toi : touche « Créer la piste Basse », puis les 5 notes en pointillé de la mesure 1. « Copier sur les mesures suivantes » la fait suivre les accords.").size)
        assertEquals(2, TutoMoments.series("À toi : touche « Créer la piste Accords ». Allume « Accords » : toucher la note du bas pose l'accord. Fais les 5 coups de la mesure 1.").size)
        assertEquals(1, TutoMoments.series("À toi : écris les 4 notes de la mesure 2, puis écoute.").size)
        assertEquals(0, TutoMoments.series("Écoute la Montée : le son s'ouvre, puis la tension monte jusqu'au Drop.").size)
        assertEquals(listOf("Créer la piste Accords"), TutoMoments.engineButtons("À toi : touche « Créer la piste Accords ». Allume « Accords »."))
    }

    @Test fun everyFacileConsigneIsOneMoment() {
        val bad = facile().flatMap { (where, text) ->
            TutoMoments.problems(text).filter { "$where ${it.first}" !in TutoMoments.EXCEPTIONS }.map { "$where ${it.first}: ${it.second} | $text" }
        }
        assertTrue("${bad.size} Facile consignes break « one moment » (docs/tuto/AUTHORING.md):\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    /**
     * Tour 8, garde-fous: an entry of [TutoMoments.EXCEPTIONS_FILE] the step no longer needs (rewritten, or gone) makes
     * this red with its name, and the list only shrinks ([CEILING], lowered to the count after each cleanup).
     */
    @Test fun everyExceptionStillBreaksTheRuleItExcuses() {
        val now = facile().flatMap { (where, text) -> TutoMoments.problems(text).map { "$where ${it.first}" } }.toSet()
        val stale = TutoMoments.EXCEPTIONS.filter { it !in now }.sorted()
        assertTrue("${stale.size} entries of ${TutoMoments.EXCEPTIONS_FILE} excuse nothing any more, delete them:\n" + stale.joinToString("\n"), stale.isEmpty())
        assertTrue("${TutoMoments.EXCEPTIONS.size} exceptions: the list only shrinks (ceiling $CEILING)", TutoMoments.EXCEPTIONS.size <= CEILING)
    }

    @Test fun theExceptionsStillAskForTwoSeriesAtMost() {
        val heavy = facile().map { (where, text) -> Triple(where, text, TutoMoments.series(text)) }.filter { it.third.size > 2 }
        assertTrue(heavy.joinToString("\n") { "${it.first}: ${it.third} | ${it.second}" }, heavy.isEmpty())
    }

    private fun facile(): List<Pair<String, String>> = TutoCourse.STYLES.flatMap { style ->
        TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.map { "$style/${it.id}" to it.textFor(TutoLevel.Facile) }
    }

    companion object {
        /** The entries of [TutoMoments.EXCEPTIONS_FILE]: lower it to the count when a lot deletes some. */
        const val CEILING = 52
    }
}
