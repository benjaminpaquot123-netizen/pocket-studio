package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 », round trip 3, seat Bb-7 (German, tablet 8", font 130 %, Mode pro, tuto Pro, Techno): the steps
 * say the gesture that really works and where the button really is, in every layout and every language.
 */
class Tour6Tuto4TechnoRound3Test {
    private val course = TutoCourse.of("techno")!!
    private val langs = Lang.entries - Lang.FR
    private val steps = course.chapters.flatMap { it.steps }

    private fun step(id: String): TutoStep = steps.first { it.id == id }

    private fun tr(lang: Lang, fr: String): String? = I18n.table(lang)[fr]?.takeIf { it.isNotBlank() }

    private fun word(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: fr

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    // H3 (ch. 3): decided since (seat Bb-8): one long press on the cell puts the hit and opens its menu; the wording
    // is Tour6TutoLongPressMenuWordsTest's (the « touch first, then long press » of this round trip is superseded).

    /**
     * H5 and H7 (ch. 5 and 7, screens 111 and 154): « Régler le son de la piste ↓ » sits at the top of the notes page,
     * under « Clavier » and « Notes » (SynthScreen: TrackBar, ModeSwitch, then the button, then the piano roll), on a
     * phone and on a tablet alike; once the page has scrolled to the grid it is out of sight above. The step says where
     * it is without « above the notes » and how to bring it into sight, in every language.
     */
    @Test
    fun theSoundButtonIsSaidWhereItIsInEveryLayout() {
        val bad = ArrayList<String>()
        for (id in listOf("bass_cutoff", "lead_knobs")) {
            val fr = step(id).text
            if ("au-dessus des notes" in fr) bad += "fr $id: « au-dessus des notes », the grid may be over it: $fr"
            for (need in listOf("tout en haut", "« Clavier »", "« Notes »", "vers le haut")) if (need !in fr) bad += "fr $id: no « $need »: $fr"
            for (l in langs) {
                val t = tr(l, fr)
                if (t == null) { bad += "${l.code} $id: no translation"; continue }
                for (w in listOf("Clavier", "Notes", "Régler le son de la piste")) if (word(l, w) !in t) bad += "${l.code} $id: no « ${word(l, w)} »: $t"
            }
        }
        report("sound steps that put the button elsewhere", bad)
    }
}
