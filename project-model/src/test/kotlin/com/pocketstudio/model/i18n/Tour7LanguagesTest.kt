package com.pocketstudio.model.i18n

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, lot 6 « langues »: words the seats still read in English (or as a technical name) in their language,
 * outside the tutorial's own files (those are lot 1's).
 */
class Tour7LanguagesTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    /** Every text of [lang] outside the tutorial's files: (area, French key, text). */
    private fun texts(lang: Lang): List<Triple<String, String, String>> =
        I18n.AREAS.filterNot { it.startsWith("tuto") }.flatMap { a -> I18n.area(lang, a).map { Triple(a, it.key, it.value) } }

    private fun report(what: String, wrong: List<String>) = assertTrue("$what (${wrong.size}):\n" + wrong.take(40).joinToString("\n"), wrong.isEmpty())

    /** D1 R15 (seat 3a #27, hi): « bar » in Latin letters next to the buttons 1 to 4 of the Rhythm, where hi writes « बार » elsewhere. */
    @Test
    fun hindiNeverWritesBarInLatinLetters() {
        val bar = Regex("""(?i)(?<![A-Za-z])bars?(?![A-Za-z])""")
        report("hi texts with « bar » in Latin letters", texts(Lang.HI).filter { bar.containsMatchIn(it.third) }.map { "${it.first}: « ${it.second} » → « ${it.third} »" })
    }

    /** D2 (seat 3a #62, hi): the « और » menu showed « Live » and « Remix » in English. Its entries (AppSpaces, PlusEntry) read in Hindi. */
    @Test
    fun hindiMoreMenuHasNoEnglishEntries() {
        I18n.lang = Lang.HI
        val entries = listOf(
            "Accueil", "Continuer, nouveau morceau, Remix…", "Mes projets", "Live : jouer en direct",
            "Lancer des boucles en rythme, comme sur scène", "Remix", "Séparer une chanson en voix, batterie, basse…",
            "Enregistrer ma voix", "Bibliothèque de sons",
        )
        val latin = Regex("[A-Za-z]")
        report("hi entries of the « और » menu with Latin letters", entries.map { it to tr(it) }.filter { latin.containsMatchIn(it.second) }.map { "« ${it.first} » → « ${it.second} »" })
    }
}
