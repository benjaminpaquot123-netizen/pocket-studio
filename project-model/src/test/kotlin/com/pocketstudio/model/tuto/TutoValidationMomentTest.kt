package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, P7 (plan § 2.1, point 4): [TutoValidation] refuses a Facile text that names an engine button (the panel shows
 * it as the step's first moment) or asks for more than one series of gestures; a Facile piece with one series passes.
 * The steps known to break it are listed in [TutoMoments.EXCEPTIONS]: blocs 1 to 4 empty the list, it only shrinks.
 */
class TutoValidationMomentTest {
    private val course = TutoCourse.of("techno")!!

    /** [course] with the first Facile step outside the exceptions given [text] and [levels]. */
    private fun withText(text: String, levels: String = "fp"): Pair<String, List<String>> {
        val step = course.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
            .first { s -> TutoMoments.EXCEPTIONS.none { it.startsWith("techno/${s.id} ") } && s.levels == "fp" && s.check.startsWith("lane:") }
        val changed = course.copy(chapters = course.chapters.map { c ->
            c.copy(steps = c.steps.map { if (it.id == step.id) it.copy(text = text, levels = levels) else it })
        })
        return step.id to TutoValidation.problems(changed, "techno.json")
    }

    @Test fun theCourseAsItIsPasses() {
        assertEquals(emptyList<String>(), TutoValidation.problems(course, "techno.json"))
    }

    @Test fun aFacileTextNamingTheEnginesButtonIsRefused() {
        val (id, problems) = withText("À toi : touche « Créer la piste Basse », puis les notes en pointillé de la mesure 1.")
        assertTrue(problems.toString(), problems.any { it.startsWith("$id: Facile engine") && "Créer la piste Basse" in it })
        for (button in listOf("« Passer à 2 mesures »", "« Copier sur les mesures suivantes »"))
            assertTrue(button, withText("À toi : touche $button.").second.any { it.startsWith("$id: Facile engine") })
    }

    @Test fun aFacileTextWithTwoSeriesIsRefused() {
        val (id, problems) = withText("À toi : écris les 4 notes de la mesure 1, puis efface la dernière note.")
        assertTrue(problems.toString(), problems.any { it.startsWith("$id: Facile series") })
    }

    @Test fun aFacilePieceWithOneSeriesPasses() {
        val (_, problems) = withText("À toi : touche les notes en pointillé de la mesure 1 : elles tombent avec le kick.", levels = "f")
        assertEquals(emptyList<String>(), problems.filter { "Facile" in it })
    }

    @Test fun readingAndTheEnginesButtonAreNoSeries() {
        assertEquals(1, TutoMoments.series("À toi : écoute la boucle, puis touche les notes de la mesure 1.").size)
        assertEquals(0, TutoMoments.series("À toi : touche « Créer la piste Basse ».").size)
        assertEquals(1, TutoMoments.series("À toi : touche « Créer la piste Basse », puis les 5 notes en pointillé de la mesure 1.").size)
        assertEquals(listOf("Créer la piste Basse"), TutoMoments.engineButtons("À toi : touche « Créer la piste Basse ». Touche « Kick »."))
        assertEquals(0, TutoMoments.series("Écoute la Montée : le son s'ouvre, puis la tension monte jusqu'au Drop.").size)
    }

    @Test fun everyExceptionStillBreaksTheRuleAndTheListOnlyShrinks() {
        val now = TutoCourse.STYLES.flatMap { style ->
            TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
                .flatMap { s -> TutoMoments.problems(s.textFor(TutoLevel.Facile)).map { "$style/${s.id} ${it.first}" } }
        }.toSet()
        val stale = TutoMoments.EXCEPTIONS - now
        assertTrue("exceptions fixed, delete their lines in ${TutoMoments.EXCEPTIONS_FILE}:\n${stale.sorted().joinToString("\n")}", stale.isEmpty())
        assertTrue("${TutoMoments.EXCEPTIONS.size} exceptions: the list only shrinks (frozen at $FROZEN)", TutoMoments.EXCEPTIONS.size <= FROZEN)
    }

    companion object {
        /** The exceptions left after blocs 1 to 4 and T0c (tour 8 garde-fous: lowered from 368 to the count, CI 387). */
        const val FROZEN = 52
    }
}
