package com.pocketstudio.model.i18n

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit stage 5 (« chasse au jargon », one word per idea, DESIGN rule 10): the texts say the
 * interface's own words. The Mix space is « Mix » (no « Mixeur » tab any more), a timeline box is a
 * « case » (not a « clip »), a setting that moves draws a « courbe » (the word « automation » stays
 * in the Lexicon only, to explain it), a separated song gives « pistes » (a « partie » is a moment
 * of the song). Every language follows its French text.
 */
class LexiconWordsTest {
    private val french = I18n.table(Lang.EN).keys

    private val banned = listOf(
        Regex("""\bau Mixeur\b"""),
        Regex("""\bclips?\b""", RegexOption.IGNORE_CASE),
        Regex("""courbes? d'automation"""),
        Regex("""› Automation"""),
        Regex("""l'automation et"""),
        Regex("""coupe chaque partie, même en pleine lecture"""),
    )

    @Test
    fun theFrenchTextsSayTheInterfacesWords() {
        val found = french.filter { k -> banned.any { it.containsMatchIn(k) } }
        assertTrue("old words:\n" + found.joinToString("\n"), found.isEmpty())
    }

    /** The tutos' « À toi : dans Mix, … » name the Mix space in each language (never its old « Mixer »). */
    @Test
    fun theTutosNameTheMixSpaceInEveryLanguage() {
        val mixWord = mapOf(
            Lang.EN to "in Mix", Lang.DE to "in Mix", Lang.ES to "en Mix", Lang.IT to "in Mix", Lang.NL to "in Mix",
            Lang.PT_BR to "no Mix", Lang.PL to "w Miksie", Lang.TR to "Mix'te", Lang.ID to "di Mix", Lang.JA to "ミックスで",
            Lang.KO to "믹스에서", Lang.ZH_CN to "在混音里", Lang.HI to "मिक्स में", Lang.AR to "«المكس»",
        )
        val keys = french.filter { it.startsWith("À toi : dans Mix,") }
        assertTrue("the tutos send to Mix", keys.size >= 90)
        val wrong = mixWord.flatMap { (lang, word) ->
            val table = I18n.table(lang)
            keys.mapNotNull { k -> table[k]?.takeUnless { word in it }?.let { "${lang.code}: $it" } }
        }
        assertTrue("not « Mix »:\n" + wrong.take(20).joinToString("\n"), wrong.isEmpty())
    }

    // --- Tour 5, chantier 3 (« C'est pas faux » : un mot par chose), audit de prise en main § 5 ---

    private val oldWords = listOf(
        Regex("""l'assistant""", RegexOption.IGNORE_CASE), Regex("""\bguidé\b""", RegexOption.IGNORE_CASE), Regex("""Nouveau morceau, pas à pas"""), Regex("""^Pas à pas$""", RegexOption.IGNORE_CASE),
        Regex("""pas à pas avec""", RegexOption.IGNORE_CASE), Regex("""premiers pas"""), Regex("""Un petit guide"""), Regex("""[Cc]e guide se retrouve"""),
        Regex("""Envoyer les bouts"""), Regex("""Envoyer le projet"""), Regex("""Envoyer le fichier brut"""), Regex("""effets de scène""", RegexOption.IGNORE_CASE), Regex("""Mes exports""", RegexOption.IGNORE_CASE),
        Regex("""^Exporter( |$)"""), Regex("""Remettre à 0 dB"""),
    )

    /**
     * One name for the assistant (« Fais-moi un morceau », its name in the app: no « assistant », « guidé » or « pas à pas »
     * for it), « Visite rapide » for the five-page window (the Guide is the bar of cards alone), « Partager » for a file out
     * (a button of the Mix says « Créer le fichier… », never « Exporter »). Red on the texts as they were.
     */
    @Test
    fun oneWordPerThingInFrench() {
        val found = french.filter { k -> oldWords.any { it.containsMatchIn(k) } }
        assertTrue("old words:\n" + found.joinToString("\n"), found.isEmpty())
    }

    /** The tour's own noun in each language (« Quick tour », « Kurztour », « Visita rápida »…), in lower case. */
    private val TOUR_STEM = mapOf(
        Lang.EN to "tour", Lang.DE to "tour", Lang.ES to "visita", Lang.IT to "tour", Lang.NL to "rondleiding", Lang.PT_BR to "tour",
        Lang.PL to "wprowadzeni", Lang.TR to "tur", Lang.ID to "tur", Lang.JA to "ツアー", Lang.KO to "둘러보기", Lang.ZH_CN to "导览",
        Lang.HI to "टूर", Lang.AR to "جولة",
    )

    /** The assistant's name and the tour's name are the same text in every text of a language. */
    @Test
    fun theAssistantAndTheTourHaveOneNameInEveryLanguage() {
        val name = "Fais-moi un morceau"
        val tour = "La visite rapide, le Guide, le lexique"
        val wrong = (Lang.entries - Lang.FR).flatMap { lang ->
            val t = I18n.table(lang)
            val nameWord = t[name]!!
            val tourWord = TOUR_STEM.getValue(lang)
            val named = french.filter { name in it && it != name }.mapNotNull { k -> t[k]?.takeUnless { nameWord in it }?.let { "${lang.code} name: $it" } }
            val toured = french.filter { Regex("""visite rapide|Cette visite""").containsMatchIn(it) && it != tour }
                .mapNotNull { k -> t[k]?.takeUnless { tourWord in it.lowercase() }?.let { "${lang.code} tour « $tourWord »: $it" } }
            named + toured
        }
        assertTrue("not the same word:\n" + wrong.take(20).joinToString("\n"), wrong.isEmpty())
        assertTrue(french.count { "Fais-moi un morceau" in it } >= 10)
    }

    /** The Guide (the bar of cards) has one word per language wherever a text says « le Guide ». */
    @Test
    fun theGuideHasOneWordInEveryLanguage() {
        val keys = french.filter { Regex("""\bGuide\b""").containsMatchIn(it) && it != "Guide" }
        assertTrue(keys.size >= 15)
        val wrong = (Lang.entries - Lang.FR).flatMap { lang ->
            val t = I18n.table(lang)
            // Arabic builds « مرشد أغنيتي » (annexed form) from « المرشد ».
            val word = t["Guide"]!!.removePrefix("ال").lowercase()
            keys.mapNotNull { k -> t[k]?.takeUnless { word in it.lowercase() }?.let { "${lang.code} « $word »: $it" } }
        }
        assertTrue("another word for the Guide:\n" + wrong.take(20).joinToString("\n"), wrong.isEmpty())
    }

    /** The Mode simple's tutorial (« Facile ») says a level in words: no « dB » in its chapters and steps. */
    @Test
    fun theFacileTutosSayNoDecibel() {
        val bad = com.pocketstudio.model.tuto.TutoCourse.STYLES.mapNotNull { com.pocketstudio.model.tuto.TutoCourse.of(it) }.flatMap { course ->
            course.chaptersFor(com.pocketstudio.model.tuto.TutoLevel.Facile).flatMap { c ->
                listOf(c.title, c.why) + c.steps.flatMap { listOf(it.textFor(com.pocketstudio.model.tuto.TutoLevel.Facile), it.why.orEmpty()) }
            }
        }.filter { Regex("""\bdB""").containsMatchIn(it) }.distinct()
        assertTrue("« dB » in a Facile tutorial:\n" + bad.joinToString("\n") { it.take(160) }, bad.isEmpty())
    }

    /** « Partager » is the one verb for a file out: no « envoyer un projet », « prêt à envoyer », « Export terminé », « Partager envoie ». */
    @Test
    fun noSendOrExportForAFileLeftInFrench() {
        val sense = listOf(
            Regex("""envoyer (un|tes) projets?""", RegexOption.IGNORE_CASE), Regex("""prêt à envoyer""", RegexOption.IGNORE_CASE), Regex("""Partager envoie"""),
            Regex("""et l'export\b"""), Regex("""^Export( |$)"""), Regex("""^L'export a échoué"""), Regex("""^Export terminé"""),
        )
        val found = french.filter { k -> sense.any { it.containsMatchIn(k) } }
        assertTrue("send/export for a file:\n" + found.joinToString("\n") { it.take(100) }, found.isEmpty())
    }
}
