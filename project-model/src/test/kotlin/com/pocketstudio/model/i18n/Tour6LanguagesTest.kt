package com.pocketstudio.model.i18n

import com.pocketstudio.model.Scene
import kotlinx.serialization.json.Json
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

    /**
     * Tutorials' sweep n° 6: in Arabic, the song's plan named its parts « Intro », « Drop », « Break », « Build-up » in Latin
     * letters, while Japanese and Korean write them in their script (« ドロップ », « 드롭 »). In the five languages whose script
     * is not Latin, every part's name (and its copy's, « Drop 2 ») and every text naming a part say it in that script.
     */
    @Test
    fun thePartsOfTheSongAreNamedInTheLanguagesOwnScript() {
        val part = Regex("^(Intro|Outro|Drops?|drop|Break|Build-ups?|Bridge|Pre-chorus|Post-chorus|Groove|groove|Fake drop|Mini-break|Fills?)$")
        // A Latin word, not a letter: the Japanese verse is « Aメロ » (the A melody), its own word.
        val latin = Regex("[A-Za-z]{2,}")
        val wrong = ArrayList<String>()
        for (lang in listOf(Lang.AR, Lang.HI, Lang.ZH_CN, Lang.JA, Lang.KO)) {
            I18n.lang = lang
            Scene.SECTIONS.flatMap { listOf(it, "$it 2") }.forEach { label ->
                Scene.shownLabel(label).let { if (latin.containsMatchIn(it)) wrong += "${lang.code}: part « $label » shows « $it »" }
            }
            texts(lang).forEach { (area, _, v) -> runs(v).filter { part.matches(it) }.forEach { wrong += "${lang.code}/$area: « $it » in « ${v.take(70)} »" } }
        }
        report("parts named in Latin letters", wrong)
    }

    /** The trade's words of the Arabic catalogs, as the Mode simple's list writes them (`i18n/ar/mots_simples.json`). */
    private fun arabicTradeWords(): Set<String> {
        val stream = I18n::class.java.classLoader.getResourceAsStream("i18n/ar/mots_simples.json")!!
        val words = stream.bufferedReader(Charsets.UTF_8).use { Json.decodeFromString<Map<String, String>>(it.readText()) }.keys
        return (words + listOf("Ghost bass", "Pre-chorus", "Post-chorus", "Fake drop")).map { it.lowercase() }.toSet()
    }

    /**
     * R15 (seat B c, Arabic, Mode pro: « Ghost bass », « Loop », « كل الـPads », Kick, Snare, Clap): the Arabic catalogs
     * write the trade's words in Arabic letters, in both modes (the Mode simple did it alone, tour 5). Names stay as written
     * (« Pad Live », the « → Pad » button, « Drum & bass », an artist's name, what one types in the sound search).
     */
    @Test
    fun theArabicCatalogsWriteTheTradesWordsInArabic() {
        val trade = arabicTradeWords()
        val names = listOf("Drum & bass", "Pad Live", "Pad المباشر", "→ Pad", "Master KG", "Think break", "dark techno kick", "«kick»", "«echo»", "«reverb»")
        val wrong = ArrayList<String>()
        texts(Lang.AR).forEach { (area, _, v) ->
            runs(names.fold(v) { acc, n -> acc.replace(n, " ") }).filter { it.lowercase() in trade }.forEach { wrong += "ar/$area: « $it » in « ${v.take(70)} »" }
        }
        I18n.lang = Lang.AR
        listOf("Basse en fantôme", "Boucler", "Boucle", "Tous les pads", "Kick", "Clap", "Caisse claire", "Pré-refrain", "Post-refrain").forEach { k ->
            tr(k).let { if (Regex("[A-Za-z]").containsMatchIn(it)) wrong += "ar: « $k » shows « $it »" }
        }
        report("Arabic texts with the trade's words in Latin letters", wrong)
    }

    /**
     * R15 (seat B c, Arabic, Mode pro): the drum pads « Cowbell », « Rim » and « Zap » stayed in Latin letters among
     * « سنير », « شيكر », « كونغا », and so did the kit's names and the styles' texts that name them. Every pad of the
     * drum grid and of a kit, and every text naming a rim or a cowbell, in Arabic letters (a style's name, « Phonk »,
     * « Drift », stays as written).
     */
    @Test
    fun theArabicDrumPadsAreNamedInArabicLetters() {
        val wrong = ArrayList<String>()
        I18n.lang = Lang.AR
        val pads = com.pocketstudio.model.DefaultNames.KIT_PADS + listOf("Cymbale", "Shaker", "Zap", "Cowbell 808", "Cloche phonk")
        pads.forEach { k -> tr(k).replace("Phonk", "").let { if (Regex("[A-Za-z]").containsMatchIn(it)) wrong += "ar: pad « $k » shows « $it »" } }
        val pad = Regex("(?i)(?<![A-Za-z])(rim|cowbell)(?![A-Za-z])")
        texts(Lang.AR).forEach { (area, _, v) -> pad.findAll(v).forEach { wrong += "ar/$area: « ${it.value} » in « ${v.take(70)} »" } }
        report("Arabic drum pads in Latin letters", wrong)
    }

    /**
     * R15 (seats F a in Italian, F b in Polish, G b in Indonesian, A a in Hindi): « kick », « step », « timeline », « 5 tab »,
     * « Squelchy »; « Synth » under a track; « bar », « take-mu »; « Post-chorus », « Fade in / Fade out / Ending », where the
     * language has its word (« cassa », « passo », « schede »; « syntezator »; « birama », « rekaman »; Devanagari). Wherever
     * they occur outside the tutorial. A pad's own name (« Kick », « Kick studio ») and a quoted name stay.
     */
    @Test
    fun theSeatsEnglishWordsAreSaidInTheirLanguage() {
        val padNames = setOf(
            "Kick", "Kicks", "Kick 2", "Kick studio", "Kick dur", "Kick analogique", "Kick (grosse caisse)",
            "Kick : la grosse caisse, le « boum » grave de la batterie.",
        )
        val unquoted = Regex("[“”«»„\"‘’][^“”«»„\"‘’]*[“”«»„\"‘’]")
        val words = mapOf(
            Lang.IT to Regex("""(?i)(?<![\w-])(kicks?|steps?|timeline|tab|squelchy)(?![\w-])"""),
            Lang.PL to Regex("""(?i)(?<![\w-])synth(?![\w-])"""),
            Lang.ID to Regex("""(?i)(?<![\w-])(bar|take)(?![\w])"""),
            Lang.HI to Regex("""(?i)(?<![\w-])(fades?|fade-in|fade-out|fade in|fade out|ending|post-chorus)(?![\w])"""),
        )
        val wrong = ArrayList<String>()
        words.forEach { (lang, word) ->
            texts(lang).filter { (_, k, _) -> lang != Lang.IT || k !in padNames }.forEach { (area, _, v) ->
                word.findAll(unquoted.replace(v, " ")).forEach { wrong += "${lang.code}/$area: « ${it.value} » in « ${v.take(70)} »" }
            }
        }
        report("English words left", wrong)
    }

    /**
     * T16 (seats D a, H a, B b: « NEW », « Groove », « Plaits »), outside the tutorial's texts: Pad Live's « NOUVEAU » in
     * the language's word; « Plaits » (a module's name, kept) says it is a synth where it stands alone (the Lexicon's entry,
     * a MIDI knob's target); « Groove », the part, in the script of the languages that are not written in Latin letters.
     */
    @Test
    fun newGrooveAndPlaitsAreSaidOrExplainedOutsideTheTutorial() {
        val wrong = ArrayList<String>()
        for (lang in Lang.entries - Lang.FR) {
            val t = I18n.table(lang)
            if (lang != Lang.EN && t["NOUVEAU"] == "NEW") wrong += "${lang.code}: « NOUVEAU » is « NEW »"
            val synth = t.getValue("Synthé").lowercase(lang.locale).take(4)
            listOf("Plaits", "Plaits : %s (piste choisie)").forEach { k ->
                val v = t.getValue(k)
                if ("Plaits" !in v || synth !in v.lowercase(lang.locale)) wrong += "${lang.code}: « $v » does not say what Plaits is (« $synth »)"
            }
        }
        val groove = Regex("(?<![A-Za-z])[Gg]roove(?![A-Za-z])")
        for (lang in listOf(Lang.AR, Lang.HI, Lang.ZH_CN, Lang.JA, Lang.KO)) {
            texts(lang).filter { (_, _, v) -> groove.containsMatchIn(v) }.forEach { (area, _, v) -> wrong += "${lang.code}/$area: « ${v.take(70)} »" }
        }
        report("NEW, Groove, Plaits", wrong)
    }
}
