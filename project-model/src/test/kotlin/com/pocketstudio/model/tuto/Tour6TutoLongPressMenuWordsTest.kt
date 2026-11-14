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
        val steps = listOf("techno" to "clap_do", "drill" to "hat_rolls", "drill" to "hat_triplet", "phonk" to "hat_rolls", "trap" to "hat_rolls", "trap" to "hat_triplet")
        for ((style, id) in steps) {
            val s = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.first { it.id == id }
            for (fr in listOfNotNull(s.text, s.textPro).filter { it.isNotEmpty() }) {
                seen++
                if ("appui long sur" !in fr) bad += "fr $style $id: the long press not on its cell: $fr"
                TWO_GESTURES.find(fr)?.let { bad += "fr $style $id: « ${it.value} », a touch before the long press: $fr" }
                for (lang in Lang.entries - Lang.FR) if (I18n.table(lang)[fr].isNullOrBlank()) bad += "${lang.code} $style $id: no translation"
            }
        }
        assertTrue("steps met: $seen", seen >= 7)
        assertTrue("${bad.size} steps that say two gestures:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    companion object {
        val TWO_GESTURES = Regex("""si elles? (est|sont) vides?|une fois (le coup )?posé|touche la dernière case|touche-les d'abord""")
    }
}
