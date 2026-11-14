package com.pocketstudio.app.library

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot 2 « tuto-consignes », T6 (seats D a, D b, H a: « mesure, temps 2 et 4, stab, accord, riff » ; 小節, キー, スタブ,
 * コード ; 마디, 노트, 루프, 퍼크 ; « 909 », « pad », « effet » ; « Tonalité, Dorienne »): the tutorial's plain lines (PlainWordsLines,
 * read on the step's French text) explained only BPM, two-step, hoover, sub, octave, kick, charley, triolet, roulement, Drop, penta,
 * phrygien, contretemps, 808. Every word of the trade a step uses gets its plain line.
 */
class Tour6PlainWordsTest {
    /** A step's sentence using the word, and the word the plain line starts with. */
    private val used = listOf(
        "À toi : remplis la mesure 1." to "Mesure",
        "Le clap tombe sur les temps 2 et 4." to "Temps",
        "Joue un accord de trois notes." to "Accord",
        "Écoute le riff avec la basse." to "Riff",
        "Pose un stab sur chaque contretemps." to "Stab",
        "Touche le pad « Clap »." to "Pad",
        "Ajoute un effet sur la piste." to "Effet",
        "Choisis la gamme et la tonalité." to "Gamme",
        "Choisis la gamme et la tonalité." to "Tonalité",
        "Règle la tonalité sur Ré dorien." to "Dorien",
        "Une petite boucle de deux notes." to "Boucle",
        "Le son d'une 909." to "909",
        "Le son d'une 707." to "707",
        "Monte le swing." to "Swing",
        "Place le break." to "Break",
    )

    @Test
    fun everyWordOfTheTradeInATutoStepHasItsPlainLine() {
        val missing = used.filter { (text, word) -> PlainWords.of(text).none { it.startsWith("$word :") || it.startsWith("$word :") } }
        assertTrue("no plain line for: " + missing.joinToString { it.second }, missing.isEmpty())
    }

    @Test
    fun aCommonWordAloneIsNotExplained() {
        // « en même temps », « longtemps », « de temps en temps » are not the beat.
        assertTrue(PlainWords.of("Écoute en même temps, depuis longtemps.").none { it.startsWith("Temps") })
    }
    /**
     * Tour 6, lot « tuto4 », round trip 1 after Da-5, M4 (screen 134: ブレイク « ドラムが止まります » while the Break keeps
     * the hats, the « Rim » and the stab): the Break's plain line says what stops, the kick and the bass, in every language.
     */
    @Test
    fun theBreaksPlainLineSaysTheKickAndTheBassStop() {
        val line = PlainWords.of("Écoute le Break.").single { it.startsWith("Break") }
        val bad = ArrayList<String>()
        if ("batterie" in line || "kick" !in line || "basse" !in line) bad += "fr: $line"
        for (lang in com.pocketstudio.model.i18n.Lang.entries - com.pocketstudio.model.i18n.Lang.FR) {
            val t = com.pocketstudio.model.i18n.I18n.table(lang)[line]
            if (t == null) bad += "${lang.code}: no translation of « $line »"
        }
        com.pocketstudio.model.i18n.I18n.table(com.pocketstudio.model.i18n.Lang.JA)[line]?.let { if ("ドラムが止" in it) bad += "ja: $it" }
        assertTrue("Break lines that say the drums stop:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
