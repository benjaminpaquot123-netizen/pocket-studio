package com.pocketstudio.app.library

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a. F079 (t9-31, t9-63): under a step the plain lines must be the step's own words with the same meaning; « accordé »
 * (the kit is tuned) gave the « Accord » line, and the Arabic Break line said « الاستراحة » while the part is called « فاصل ».
 * F065 (t9-01, t9-42, t9-52, t9-62): « 303 », « accents », « slides », « glide » were never explained.
 */
class Tour9PlainWordsL5aTest {
    @Test
    fun aLongerWordIsNotTheWordOfTheTrade() {
        val bad = listOf(
            "Le kick est accordé sur la tonalité." to "Accord",
            "Un son bouclé, bien mesuré." to "Boucle",
            "Un son bouclé, bien mesuré." to "Mesure",
            "Une note accentuée." to "Accent",
        ).filter { (text, word) -> PlainWords.of(text).any { it.startsWith(word) } }
        val lines = PlainWords.of("Écoute le Break.")
        val break_ = lines.single { it.startsWith("Break") }
        val wrong = ArrayList<String>()
        for (lang in Lang.entries - Lang.FR) {
            val part = I18n.table(lang)["Break"] ?: continue
            val line = I18n.table(lang)[break_] ?: continue
            if (part !in line) wrong += "${lang.code}: « $line » does not name the part « $part »"
        }
        assertTrue("plain lines for another word: $bad\n" + wrong.joinToString("\n"), bad.isEmpty() && wrong.isEmpty())
    }

    @Test
    fun theAcidWordsAreExplained() {
        val used = listOf("Écoute la 303." to "303", "Ajoute deux accents." to "Accent", "Pose des slides." to "Slide", "Le GLIDE lie les notes." to "Glide")
        val missing = used.filter { (text, word) -> PlainWords.of(text).none { it.startsWith("$word :") } }.map { it.second }
        val untranslated = PlainWords.of("303 accent slide glide").flatMap { line -> (Lang.entries - Lang.FR).filter { I18n.table(it)[line] == null }.map { "${it.code}: $line" } }
        assertTrue("no plain line for: $missing\n" + untranslated.joinToString("\n"), missing.isEmpty() && untranslated.isEmpty())
    }
}
