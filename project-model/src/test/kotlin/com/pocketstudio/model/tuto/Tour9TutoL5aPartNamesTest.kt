package com.pocketstudio.model.tuto

import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a, F074 (t9-36, t9-42): the Korean consigne says « 드롭 B », the part, its button and the map's
 * legend said « Drop B » in English. A part the course names « <section> B » is shown in the user's language,
 * as the consigne writes it.
 */
class Tour9TutoL5aPartNamesTest {
    @After fun french() { I18n.lang = Lang.FR }

    @Test
    fun aLetteredPartIsShownAsTheConsigneNamesIt() {
        val bad = ArrayList<String>()
        val quoted = Regex("« ([^»]+?) »")
        for (style in TutoCourse.STYLES) {
            val c = TutoCourse.of(style) ?: continue
            val texts = c.chapters.flatMap { it.steps }.flatMap { s -> TutoLevel.entries.map { s.textFor(it) } }.distinct()
            val parts = texts.flatMap { t -> quoted.findAll(t).map { it.groupValues[1] }.toList() }
                .filter { Regex("""^(.+) [A-Z]$""").matchEntire(it)?.groupValues?.get(1) in Scene.SECTIONS }.distinct()
            for (lang in Lang.entries.filter { it != Lang.FR }) {
                I18n.lang = lang
                for (part in parts) {
                    val shown = Scene.shownLabel(part)
                    texts.filter { "« $part »" in it }.map { tr(it) }.filter { it != "" && shown !in it }
                        .forEach { bad += "$style/${lang.code}: « $part » shown « $shown », consigne: $it" }
                }
            }
            I18n.lang = Lang.FR
        }
        assertTrue("part names unlike the consigne's:\n" + bad.take(20).joinToString("\n") + "\n(${bad.size})", bad.isEmpty())
    }
}
