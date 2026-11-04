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
}
