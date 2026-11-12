package com.pocketstudio.model.i18n

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot 5 « langues » (texts): what the seats read in their language outside the tutorial's own texts (the
 * tutorial's files are lot 2's). R16: mistakes of the catalogs; n° 6 of the tutorials' sweep: the parts of the song
 * in Latin letters in Arabic while Japanese writes them in its own script; R15: English words left in a translated
 * interface; T16: « Plaits », « Groove », « NEW ».
 */
class Tour6LanguagesTest {
    @After
    fun back() {
        I18n.everyday = false
        I18n.lang = Lang.FR
    }

    /** Every text of [lang] outside the tutorial's files: (area, French key, text). */
    private fun texts(lang: Lang): List<Triple<String, String, String>> =
        I18n.AREAS.filterNot { it.startsWith("tuto") }.flatMap { a -> I18n.area(lang, a).map { Triple(a, it.key, it.value) } }

    /** The Latin « words » of a text: letters joined by a space or a hyphen (« Fake drop », « Build-up »), placeholders left out. */
    private val run = Regex("(?<![A-Za-z0-9_])[A-Za-z][A-Za-z'’]*(?:(?: |-)[A-Za-z][A-Za-z'’]*)*")

    private fun runs(text: String): List<String> = run.findAll(text.replace(Regex("%\\d*\\$?[sdf]"), " ")).map { it.value }.toList()

    private fun report(what: String, wrong: List<String>) = assertTrue("$what (${wrong.size}):\n" + wrong.take(40).joinToString("\n"), wrong.isEmpty())

    /**
     * R16 (seats F a, F b, B a, G a): « su il pad 3 » in Italian (su + il is « sul »), « 5 zakładki » in Polish (5 takes
     * « zakładek »), « COMP. 2 A 5 » in Spanish, and the Turkish title « Önce ayırma modeli » (« First separation model »)
     * cut short, while the sentence under it says « Önce ayrıştırma modeli gerekli ».
     */
    @Test
    fun theSeatsCatalogMistakesAreFixed() {
        val wrong = ArrayList<String>()
        I18n.lang = Lang.IT
        val notContracted = Regex("""(?i)\b(su|di|a|da|in) (il|lo|la|i|gli|le)\b|\b(su|di|a|da|in) l'""")
        // What the sound library fills (AppRoot, libraryTarget) and the take's place (AudioTracksScreen).
        val targets = listOf(
            tr("le pad %1\$d (%2\$s)", 3, tr("Caisse claire")), tr("le pad live %d", 3), tr("le pad %s", "B1"),
            tr("la piste « %s »", "Basse"), tr("la brique %s", "A"),
        )
        val shown = targets.map { tr("Touche un son pour l'écouter, « Poser » pour le mettre sur %s.", it) } +
            listOf(tr("la partie %s", "A"), tr("tout le morceau")).map { tr("sur %s", it) }
        shown.filter { notContracted.containsMatchIn(it) }.forEach { wrong += "it: « $it »" }
        I18n.lang = Lang.PL
        tr("Simple : %d onglets", 5).let { if (it != "Prosty: 5 zakładek") wrong += "pl: « $it »" }
        I18n.lang = Lang.ES
        mapOf(
            tr("mes. %1\$d à %2\$d", 2, 5).upperShown() to "COMPASES 2 A 5", tr("Mes. %1\$d à %2\$d", 2, 5) to "Compases 2 a 5",
            tr("Refaire mes. %1\$d-%2\$d", 5, 8) to "Rehacer compases 5-8", tr("Refaire la mes. %d", 5) to "Rehacer el compás 5",
            tr("Coller mes. %d", 3) to "Pegar compás 3",
        ).forEach { (got, want) -> if (got != want) wrong += "es: « $got », not « $want »" }
        I18n.lang = Lang.TR
        val title = tr("D'abord le modèle de séparation")
        val sentence = tr("Il faut d'abord le modèle de séparation (%s, une seule fois, Wi-Fi conseillé).", "200 MB").substringBefore(" (")
        if (title != sentence) wrong += "tr: title « $title », sentence « $sentence »"
        report("catalog mistakes", wrong)
    }
}
