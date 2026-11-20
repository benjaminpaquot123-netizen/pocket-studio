package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a, F063: a note named in a step is spelt as the keys spell it. The Acid 303 keyboard writes sharps
 * (« La# », not « Si♭ », t9-03), and the Arabic keyboard writes solfège (« ري », not « D », t9-31, t9-62).
 */
class Tour9TutoL5aNotesTest {
    private fun texts(style: String): List<String> {
        val course = TutoCourse.of(style) ?: return emptyList()
        return TutoLevel.entries.flatMap { level ->
            course.chaptersFor(level).flatMap { ch ->
                listOf(ch.titleFor(level), ch.why) + ch.steps.flatMap { s -> listOfNotNull(s.textFor(level), s.why, s.skip) }
            }
        }.filter { it.isNotBlank() }.distinct()
    }

    @Test
    fun acidTechnoNamesTheNotesAsTheAcidKeyboardDoes() {
        val bad = ArrayList<String>()
        for (fr in texts("acid_techno")) for (lang in Lang.entries) {
            val shown = if (lang == Lang.FR) fr else I18n.table(lang)[fr] ?: continue
            if ('♭' in shown) bad += "${lang.code}: $shown"
        }
        assertTrue("a flat the 303 keyboard never shows:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    private val solfege = mapOf('C' to "Do", 'D' to "Ré", 'E' to "Mi", 'F' to "Fa", 'G' to "Sol", 'A' to "La", 'B' to "Si")
    private val latinNote = Regex("(?<![A-Za-z0-9À-ɏ])([A-G])([♯♭#]?)(?![A-Za-z0-9À-ɏ])")

    @Test
    fun arabicCourseTextsNameNotesInSolfege() {
        val table = I18n.table(Lang.AR)
        val bad = ArrayList<String>()
        for (style in TutoCourse.STYLES) for (fr in texts(style)) {
            val ar = table[fr] ?: continue
            latinNote.findAll(ar).forEach { m ->
                val name = solfege.getValue(m.groupValues[1][0])
                // Only where the French text names that note too: « B » is also a part's letter (« + B · Intro »).
                if (Regex("(?<![A-Za-zÀ-ÿ])$name(?![A-Za-zÀ-ÿ])").containsMatchIn(fr)) bad += "$style: « ${m.value} » in $ar"
            }
        }
        assertTrue("Latin note names on an Arabic (solfège) screen:\n" + bad.take(20).joinToString("\n"), bad.isEmpty())
    }
}
