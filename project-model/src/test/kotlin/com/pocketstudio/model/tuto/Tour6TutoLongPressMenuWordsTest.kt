package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, seats Bb-7/Bb-8 (H3, decided): a long press on a cell, empty or not, puts its hit and opens its menu at once
 * (RhythmScreen's cell). The steps say that one gesture, « appui long sur la case », then the menu's choice; never
 * a touch first « si elle est vide » nor « une fois posé », in every language.
 */
class Tour6TutoLongPressMenuWordsTest {
    @Test
    fun aLongPressOnTheCellIsTheOneGesture() {
        val bad = ArrayList<String>()
        var seen = 0
        val steps = listOf("techno" to "clap_do", "techno" to "clap_2", "drill" to "hat_rolls", "drill" to "hat_triplet", "phonk" to "hat_rolls", "trap" to "hat_rolls", "trap" to "hat_triplet")
        for ((style, id) in steps) {
            val s = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.first { it.id == id }
            // D4: a Facile step cut in two (the hits, then the menu) says the long press in the piece that opens the menu.
            for (fr in listOfNotNull(s.text, s.textPro).filter { it.isNotEmpty() && MENU.containsMatchIn(it) }) {
                seen++
                // What the user reads at this step: its words and its « why » (Tour 7 P6 moved the gesture's detail there).
                val read = listOfNotNull(fr, s.why).joinToString("\n")
                if ("appui long sur" !in read) bad += "fr $style $id: the long press not on its cell: $fr"
                TWO_GESTURES.find(read)?.let { bad += "fr $style $id: « ${it.value} », a touch before the long press: $fr" }
                for (lang in Lang.entries - Lang.FR) if (I18n.table(lang)[fr].isNullOrBlank()) bad += "${lang.code} $style $id: no translation"
            }
        }
        assertTrue("steps met: $seen", seen >= 7)
        assertTrue("${bad.size} steps that say two gestures:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    companion object {
        val MENU = Regex("« (Doux|Moyen|Fort|Roulement|Triolet) »")
        val TWO_GESTURES = Regex("""si elles? (est|sont) vides?|une fois (le coup )?posé|touche la dernière case|touche-les d'abord""")
    }
}
