package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 8, lot T0c « tuto-mots » (docs/audits/ultra/tour-8-fiches/fiche-T0-touche-ici.md § T0c): the words of the
 * course say what the screen shows, in the 14 languages.
 * - The roll is the user's (c-V3): no step calls it « la dernière rafale », a burst already there, in any language.
 * - The Montée's snare does not « roll faster and faster » by itself, and its creation words do not say its last beat
 *   is silent (bar 4 holds 16 claps).
 * - Korean: the part « Fin » is the button's word, 끝, never 엔딩.
 * - Techno Facile: the Intro 2 leaves out the riff only (no Facile step lays the cymbal in the Groove).
 */
class Tour8TutoWordsTest {
    private val langs = Lang.entries - Lang.FR

    private fun tr(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: "(no ${lang.code} translation of « $fr »)"

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /** Every French text a step shows (consigne at both levels, and its « why »), with where it is. */
    private fun texts(): List<Pair<String, String>> = TutoCourse.STYLES.flatMap { style ->
        TutoCourse.of(style)!!.chapters.flatMap { it.steps }.flatMap { s ->
            listOfNotNull(s.text, s.textPro, s.why?.takeIf { it.isNotBlank() }).distinct().map { "$style/${s.id}" to it }
        }
    }

    @Test
    fun noStepCallsTheRollTheLastBurstInAnyLanguage() {
        val lastBurst = mapOf(
            Lang.FR to Regex("derni[eè]re rafale", RegexOption.IGNORE_CASE),
            Lang.EN to Regex("last burst", RegexOption.IGNORE_CASE),
            Lang.DE to Regex("letzte Salve", RegexOption.IGNORE_CASE),
            Lang.ES to Regex("última ráfaga", RegexOption.IGNORE_CASE),
            Lang.IT to Regex("ultima raffica", RegexOption.IGNORE_CASE),
            Lang.PT_BR to Regex("última rajada", RegexOption.IGNORE_CASE),
            Lang.NL to Regex("laatste salvo", RegexOption.IGNORE_CASE),
            Lang.PL to Regex("ostatni[aą] seri", RegexOption.IGNORE_CASE),
            Lang.TR to Regex("son (bir )?seri", RegexOption.IGNORE_CASE),
            Lang.ID to Regex("semburan terakhir", RegexOption.IGNORE_CASE),
            Lang.HI to Regex("आख़िरी बौछार|आखिरी बौछार"),
            Lang.JA to Regex("最後の連打"),
            Lang.KO to Regex("마지막 연타"),
            Lang.ZH_CN to Regex("最后一阵"),
            Lang.AR to Regex("رشقة أخيرة|الرشقة الأخيرة"),
        )
        assertTrue("every language has its word", lastBurst.keys == Lang.entries.toSet())
        val bad = ArrayList<String>()
        for ((where, fr) in texts()) for (lang in Lang.entries) {
            val t = if (lang == Lang.FR) fr else tr(lang, fr)
            if (lastBurst.getValue(lang).containsMatchIn(t)) bad += "$where ${lang.code}: $t"
        }
        report("steps call the roll « the last burst »", bad.distinct())
    }

    @Test
    fun theMonteeIsNotSaidToRollFasterOrToFallSilentOnItsLastBeat() {
        val bad = ArrayList<String>()
        for ((where, fr) in texts()) if ("roule de plus en plus vite" in fr) bad += "$where: $fr"
        // The creation words of a part that builds up (TutoController.partMade): no silent last beat.
        val old = listOf(
            "Sa caisse claire accélère mesure après mesure, et son dernier temps se tait.",
            "Son pad « %s » accélère mesure après mesure, et son dernier temps se tait.",
        )
        val now = listOf("Sa caisse claire accélère mesure après mesure.", "Son pad « %s » accélère mesure après mesure.")
        for (lang in langs) {
            for (k in old) if (I18n.table(lang)[k] != null) bad += "${lang.code}: still « $k »"
            for (k in now) if (I18n.table(lang)[k] == null) bad += "${lang.code}: no translation of « $k »"
            for (k in now) I18n.table(lang)[k]?.let { if (k.contains("%s") != it.contains("%s")) bad += "${lang.code}: « $it » lost its %s" }
        }
        report("words say the Montée rolls by itself or falls silent", bad)
    }

    @Test
    fun koreanCallsThePartFinByItsButtonsWord() {
        val ko = I18n.table(Lang.KO)
        val bad = ko.filterValues { "엔딩" in it }.map { (k, v) -> "« $k »: $v" }.toMutableList()
        if (ko["Fin"] != "끝") bad += "Fin: ${ko["Fin"]}"
        report("Korean texts say 엔딩 where the button says 끝", bad)
    }

    @Test
    fun theFacileIntro2LeavesOutOnlyWhatTheFacileGrooveHas() {
        val steps = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
        val cymbal = steps.any { it.check == "lane:ride" }
        val intro2 = steps.first { it.id == "make_intro2" }.textFor(TutoLevel.Facile)
        val bad = ArrayList<String>()
        if (!cymbal && "cymbale" in intro2) bad += "fr: « $intro2 » (no Facile step lays the cymbal)"
        for (lang in langs) if (I18n.table(lang)[intro2] == null) bad += "${lang.code}: no translation of « $intro2 »"
        report("Facile Intro 2 words name a pad the Facile Groove does not have", bad)
    }
}
