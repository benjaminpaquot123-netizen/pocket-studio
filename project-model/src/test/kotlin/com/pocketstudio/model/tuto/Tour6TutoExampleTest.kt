package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 6, T19: a part heard before it is written is said to be the example's. */
class Tour6TutoExampleTest {
    private val steps: List<Triple<String, TutoStep, TutoLevel>> = TutoCourse.STYLES.flatMap { style ->
        TutoLevel.entries.flatMap { level -> TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.map { Triple(style, it, level) } }
    }

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * T19: techno's « Écoute le riff avec la basse » while the track lit was « Accords »: a listening step heard
     * before the user writes that part says it is the example's.
     */
    @Test
    fun aPartHeardBeforeItIsWrittenIsTheExamples() {
        val bad = mutableListOf<String>()
        for (style in TutoCourse.STYLES) {
            val written = mutableSetOf<String>()
            for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) {
                if (s.check.startsWith("notes:")) written += s.check.removePrefix("notes:")
                if (!s.listening || s.hearMine || !s.listen.startsWith("parts:")) continue
                val roles = s.listen.removePrefix("parts:").split(',').toSet() intersect TutoTarget.TRACK_ROLES.toSet()
                if ((roles - written).isEmpty()) continue
                listOfNotNull(s.text, s.textPro).filter { "exemple" !in it }.forEach { bad += "$style/${s.id}" }
            }
        }
        report("parts heard before written, not said to be the example's", bad.distinct() - PENDING_EXAMPLE)
        assertTrue("done already, take out of PENDING_EXAMPLE: ${PENDING_EXAMPLE - bad.toSet()}", (PENDING_EXAMPLE - bad.toSet()).isEmpty())
    }

    companion object {
        /** Courses whose listening steps still say « Écoute la basse » before it is written (techno and house done). */
        val PENDING_EXAMPLE = setOf<String>(
            "afro_house/bass_hear",
            "afrobeats/bass_hear",
            "afrobeats/lead_hear",
            "afrobeats/pad_hear",
            "amapiano/bass_hear",
            "amapiano/pad_hear",
            "boom_bap/pad_hear",
            "deep_house/bass_hear",
            "deep_house/pad_hear",
            "detroit/pad_hear",
            "disco/pad_hear",
            "dnb/bass_hear",
            "drill/bass_hear",
            "drill/lead_hear",
            "dubstep/bass_hear",
            "dubstep/lead_hear",
            "hard_techno/pad_hear",
            "jersey_club/lead_hear",
            "jump_up/lead_hear",
            "jungle/bass_hear",
            "jungle/lead_hear",
            "liquid_dnb/bass_hear",
            "liquid_dnb/lead_hear",
            "lofi/pad_hear",
            "neurofunk/bass_hear",
            "neurofunk/lead_hear",
            "phonk/bass_hear",
            "phonk/lead_hear",
            "pop/bass_hear",
            "pop/lead_hear",
            "pop/pad_hear",
            "reggaeton/bass_hear",
            "reggaeton/lead_hear",
            "reggaeton/pad_hear",
            "retro_techno/lead_hear",
            "riddim/bass_hear",
            "speed_garage/bass_hear",
            "synthwave/bass_hear",
            "synthwave/lead_hear",
            "tech_house/bass_hear",
            "trance/lead_hear",
            "trance/pad_hear",
            "trap/bass_hear",
            "trap/lead_hear",
            "uk_garage/bass_hear",
            "uk_garage/pad_hear",
        )
    }
}
