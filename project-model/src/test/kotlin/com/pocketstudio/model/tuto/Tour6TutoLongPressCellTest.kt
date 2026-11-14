package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, seat Bb-7 (H3) everywhere: a long press on an EMPTY cell of the grid puts a strong hit and opens no menu
 * (RhythmScreen's cell: `onLongClick` without a hit calls `cycleVelocity`); the menu (« Roulement », « Triolet »)
 * opens on a hit already there. Every hip-hop step that sends a long press to a numbered cell (« case 13 », « cases
 * 15 et 16 ») says to touch the cell first when it is empty, before the long press, in every language.
 */
class Tour6TutoLongPressCellTest {
    @Test
    fun aLongPressOnACellSaysToPutItsHitFirst() {
        val bad = ArrayList<String>()
        var seen = 0
        for (style in listOf("drill", "phonk", "trap")) {
            val course = TutoCourse.of(style) ?: error("no $style course")
            for (s in course.chapters.flatMap { it.steps }) for (fr in listOfNotNull(s.text, s.textPro).filter { it.isNotEmpty() }) {
                // Each sentence part that long-presses a numbered cell.
                for (part in fr.split(';', '.')) {
                    val press = part.indexOf("appui long")
                    if (press < 0 || !CELL.containsMatchIn(part)) continue
                    seen++
                    val first = EMPTY.find(part)?.range?.first
                    if (first == null || first > press) bad += "fr $style ${s.id}: a long press on a cell maybe empty, not touched first: $part"
                }
                if ("appui long" in fr && CELL.containsMatchIn(fr)) for (lang in Lang.entries - Lang.FR) {
                    if (I18n.table(lang)[fr].isNullOrBlank()) bad += "${lang.code} $style ${s.id}: no translation of « $fr »"
                }
            }
        }
        assertTrue("no long press on a cell met", seen > 0)
        assertTrue("${bad.size} long presses sent to a cell maybe empty:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    companion object {
        val CELL = Regex("""\bcases? \d+""")
        val EMPTY = Regex("""si elles? (est|sont) vides?""")
    }
}
