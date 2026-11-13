package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, T8: techno's « bass_do » chained five gestures in one step (create the track, bar 1, « Copier », bar 4,
 * « Copier » again): the beginner lost the thread. A consigne asks for 3 gestures at most (imperative verbs and
 * buttons pressed on their own, [Tour6TutoWords.actions]); a longer one is split into two steps.
 */
class Tour6TutoActionsTest {
    @Test
    fun theCounterCountsGestures() {
        assertEquals(4, Tour6TutoWords.actions("À toi : touche « Créer la piste Basse », puis les notes de la mesure 1. « Copier sur les mesures suivantes » la répète. Touche ensuite les notes de la mesure 4, puis « Copier » encore.").size)
        assertEquals(1, Tour6TutoWords.actions("À toi : règle le tempo. « Je le fais » ouvre la fiche Tempo.").size)
        assertEquals(1, Tour6TutoWords.actions("Écoute la Montée : le son s'ouvre petit à petit, la tension monte jusqu'au Drop.").size)
    }

    @Test
    fun noConsigneAsksForMoreThanThreeGestures() {
        val heavy = Tour6TutoWords.consignes().map { (where, text) -> Triple(where, text, Tour6TutoWords.actions(text)) }.filter { it.third.size > MAX }
        val new = heavy.filter { it.first !in PENDING }
        assertTrue("${new.size} consignes ask for more than $MAX gestures:\n" + new.joinToString("\n") { "${it.first}: ${it.third}" }, new.isEmpty())
        // The list of the courses still to split only shrinks: one split already is taken out of it.
        val fixed = PENDING - heavy.map { it.first }.toSet()
        assertTrue("split already, take out of PENDING: $fixed", fixed.isEmpty())
    }

    companion object {
        const val MAX = 3

        /**
         * Consignes still to split (tour 6). A split notes step (a first step `length: 16`, the bar 1; a second one, « Copier »
         * and the rest) is ready in TutoChecks, but TutoJourneyTest expects each notes step to create its own track: to change
         * there first. Until then this list only shrinks.
         */
        val PENDING = setOf<String>(
            "afrobeats/pad_do/f",
            "boom_bap/bass_do/f",
            "boom_bap/pad_do/f",
            "deep_house/pad_do/f",
            "dnb/pad_do/f",
            "drill/bass_do/f",
            "drill/pad_do/f",
            "dubstep/hat_do/f",
            "dubstep/pad_do/f",
            "grime/pad_do/f",
            "house/pad_do/f",
            "jump_up/bass_do/f",
            "jump_up/bass_do/p",
            "jump_up/make_drop2/p",
            "jump_up/pad_do/f",
            "jungle/pad_do/f",
            "liquid_dnb/pad_do/f",
            "lofi/lead_do/f",
            "lofi/pad_do/f",
            "minimal/pad_do/f",
            "neurofunk/pad_do/f",
            "phonk/pad_do/f",
            "pop/pad_do/f",
            "reggaeton/pad_do/f",
            "riddim/pad_do/f",
            "synthwave/pad_do/f",
            "tech_house/pad_do/f",
            "techno/bass_do/f",
            "techno/pad_do/f",
            "trance/pad_do/f",
            "trap/pad_do/f",
            "uk_garage/pad_do/f",
        )
    }
}
