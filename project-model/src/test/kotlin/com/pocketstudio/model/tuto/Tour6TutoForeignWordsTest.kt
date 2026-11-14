package com.pocketstudio.model.tuto

import com.pocketstudio.model.Scene
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, T16: « NEW », « Groove », « Perc », English words or cut words in the tutorial's texts; in Hindi 22
 * chapter titles left in English (« Hi-hats », « Stab », « Snare »), in Chinese 11 (« Lead », « Hoover Riff »).
 */
class Tour6TutoForeignWordsTest {
    private val areas = I18n.AREAS.filter { it.startsWith("tuto") }
    private val translated = Lang.entries - Lang.FR

    /** The tutorial's texts of [lang], by French key. */
    private fun tuto(lang: Lang): Map<String, String> = areas.flatMap { I18n.area(lang, it).entries }.associate { it.key to it.value }

    @Test
    fun noUntranslatedWordInTheTutorial() {
        val bad = mutableListOf<String>()
        for (lang in translated) {
            // « Groove » is a part's name: its own word where the app shows one (グルーヴ), else « Groove » as on screen.
            val groove = I18n.table(lang)["Groove"] ?: "Groove"
            val words = listOf("NEW", "Perc") + if (groove != "Groove") listOf("Groove") else emptyList()
            for ((fr, text) in tuto(lang)) for (w in words) {
                if (Regex("""(?<![\p{L}])$w(?![\p{L}])""").containsMatchIn(text)) bad += "${lang.code}: « $w » in $text"
            }
        }
        assertTrue("${bad.size} untranslated words:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }

    /** In the languages not written in Latin letters, a chapter title has no Latin word but a style's name, a proper name or a sign (BPM, 808). */
    @Test
    fun chapterTitlesAreInTheLanguagesScript() {
        val titles = TutoCourse.STYLES.flatMap { s -> TutoCourse.of(s)!!.chapters.flatMap { listOfNotNull(it.title, it.titlePro) } }.toSet()
        val styles = StarterWizard.STYLES.flatMap { it.name.split(' ', '-') }.map { it.lowercase() }.toSet()
        val latin = Regex("""[A-Za-z][A-Za-z'\-]*""")
        val bad = mutableListOf<String>()
        for (lang in listOf(Lang.HI, Lang.AR, Lang.JA, Lang.KO, Lang.ZH_CN)) {
            val table = I18n.table(lang)
            // The parts' names as the app shows them (« Drop » in Hindi Pro; the Mode simple writes them in the script).
            val parts = Scene.SECTIONS.mapNotNull { table[it] }.flatMap { latin.findAll(it).map { m -> m.value }.toList() }.toSet()
            for (t in titles) {
                val v = table[t] ?: continue
                val words = latin.findAll(v).map { it.value }.filter { w ->
                    !(w.all { it.isUpperCase() || it == '-' } || w.lowercase() in styles || w.split('-').all { it.lowercase() in styles } ||
                        w in parts || w in PROPER || v.contains(Regex("""\d-?$w""")))
                }.toList()
                if (words.isNotEmpty()) bad += "${lang.code}: « $t » → « $v » $words"
            }
        }
        assertTrue("${bad.size} chapter titles with Latin words:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    companion object {
        /** Proper names a title may keep as they are. */
        val PROPER = setOf("Amen", "Kerri", "Chandler")
    }
}
