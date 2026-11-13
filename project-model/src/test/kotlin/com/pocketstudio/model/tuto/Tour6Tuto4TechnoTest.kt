package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 » (seats D a: Japanese, phone, Mode simple, tuto Facile; B b: German, tablet, 130 %, Mode pro,
 * tuto Pro), the techno course: a big beginner, or the musician of the Pro tutorial, reads the step and knows what to do
 * without guessing. Each test reads the course in French and in the languages concerned, from the catalogs.
 */
class Tour6Tuto4TechnoTest {
    private val course = TutoCourse.of("techno")!!
    private val langs = Lang.entries - Lang.FR
    private val steps = course.chapters.flatMap { it.steps }

    private fun step(id: String): TutoStep = steps.first { it.id == id }
    private fun chapter(id: String): TutoChapter = course.chapters.first { it.id == id }

    /** A step's words at both levels (the Pro's own when it has some). */
    private fun texts(s: TutoStep): List<String> = listOfNotNull(s.text, s.textPro)

    /** [fr] in [lang], never the French shown in its place. */
    private fun tr(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: "(no ${lang.code} translation of « $fr »)"

    /** [text] names [name] between quotes, in any language's quotes, as a pad or a button is named. */
    private fun quotes(text: String, name: String): Boolean =
        Regex("[$OPEN][\\s  ]?${Regex.escape(name.trim())}[\\s  ]?[$CLOSE]").containsMatchIn(text)

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * M1 (seats D a, B b): ch. 2 to 4 name the pad by the name written on it, in every language and at both levels
     * (« pose la ride » while the pad says « Becken »; « le « toc » » while no pad has that name).
     */
    @Test
    fun padsNamedAsWritten() {
        val pads = mapOf("kick" to "Kick", "rumble" to "Rumble", "clap" to "Clap", "open_hat" to "Charley ouvert", "hat" to "Charley", "perc" to "Rim", "ride" to "Cymbale")
        val bad = mutableListOf<String>()
        for (s in steps) {
            val role = when {
                s.check.startsWith("lane:") -> s.check.removePrefix("lane:")
                s.check == "crash" -> "ride"
                else -> continue
            }
            val name = pads.getValue(role)
            for (fr in texts(s)) {
                if (!quotes(fr, name)) bad += "fr ${s.id}: no « $name »: $fr"
                for (l in langs) tr(l, fr).let { t -> if (!quotes(t, tr(l, name))) bad += "${l.code} ${s.id}: no « ${tr(l, name)} »: $t" }
            }
        }
        steps.flatMap(::texts).filter { "« toc »" in it }.forEach { bad += "fr: « toc », no pad's name: $it" }
        report("drum steps that do not name their pad as written", bad)
    }

    /**
     * M2 (seats D a, B b): each musician's word is explained in simple words where it first shows (kick, bass, Drop,
     * bar, stab, Groove, Break, mix, transition, BPM; the hit menu's « Force », « Roulement », « Triolet »), in Japanese too.
     */
    @Test
    fun tradeWordsExplainedWhereTheyFirstShow() {
        val bad = mutableListOf<String>()
        fun need(where: String, text: String, vararg parts: String) = parts.filter { it !in text }.forEach { bad += "$where: no « $it »: $text" }
        need("ref", step("ref").text, "kick (la grosse caisse)", "basse (les notes graves)", "Drop (le moment où tout joue)")
        need("kick chapter", chapter("kick").why, "font une mesure")
        need("clap_do", step("clap_do").text, "« Force » (", "« Roulement » (", "« Triolet » (")
        need("pad_do", step("pad_do").text, "stabs (")
        need("groove_hear", step("groove_hear").text, "« Groove » (")
        need("gap", step("gap").text, "transition (")
        need("map_break", step("map_break").text, "Break (la pause)")
        need("levels", step("levels").text, "le mix, c'est")
        need("tempo", step("tempo").text, "battements par minute")
        // In the Facile course, the first step that says the word (with its chapter's title) is the one that explains it.
        val firsts = mapOf("kick" to "ref", "basse" to "ref", "Drop" to "ref", "stab" to "pad_do", "Groove" to "groove_hear", "Break" to "map_break", "transition" to "gap", "mix" to "levels", "BPM" to "tempo")
        val facile = course.chaptersFor(TutoLevel.Facile).flatMap { c -> c.steps.mapIndexed { i, s -> s.id to ((if (i == 0) c.title + "\n" else "") + s.text) } }
        for ((word, id) in firsts) {
            val first = facile.firstOrNull { (_, t) -> Regex("(?i)\\b${word}s?\\b").containsMatchIn(t) }?.first
            if (first != id) bad += "« $word » first shows in $first, explained in $id"
        }
        // Japanese: ベース, the menu's words, スタブ, グルーヴ, トランジション explained there; German: the bar (Takt).
        val ja = Lang.JA
        need("ja ref", tr(ja, step("ref").text), "ベース（")
        need("ja clap_do", tr(ja, step("clap_do").text), *listOf("Force", "Roulement", "Triolet").map { tr(ja, it) + "」（" }.toTypedArray())
        need("ja pad_do", tr(ja, step("pad_do").text), "スタブ（")
        need("ja groove_hear", tr(ja, step("groove_hear").text), "グルーヴ」（")
        need("ja gap", tr(ja, step("gap").text), "トランジション（")
        need("de kick chapter", tr(Lang.DE, chapter("kick").why), "Takt")
        // Every language keeps the explanations: as many brackets as the French.
        for (id in listOf("ref", "clap_do", "pad_do", "groove_hear", "gap")) {
            val fr = step(id).text
            val n = fr.count { it == '(' }
            for (l in langs) tr(l, fr).let { t -> if (t.count { it == '(' || it == '（' } < n) bad += "${l.code} $id: an explanation lost: $t" }
        }
        report("words of the trade not explained where they first show", bad)
    }

    /**
     * M4 (seats D a, B b): no contradiction: the Break keeps the hats and the « Rim »; the Montée « Créer la partie » makes
     * still plays its last beat (its silence is the transitions' step); ch. 10 says which button makes each part.
     */
    @Test
    fun noContradiction() {
        val bad = mutableListOf<String>()
        for (fr in texts(step("make_montee"))) if ("se tait" in fr) bad += "make_montee: says the end is silent, the gap step makes it: $fr"
        for (id in listOf("map_break", "make_break")) step(id).text.let { fr ->
            listOf("charleys", "« Rim »", "stab").filter { it !in fr }.forEach { bad += "$id: says nothing of the $it it keeps: $fr" }
        }
        for (s in chapter("parts").steps.filter { it.check.startsWith("part:") }) {
            val label = s.check.removePrefix("part:").substringBefore(':')
            for (fr in texts(s)) {
                if ("« Créer la partie « $label » »" !in fr) bad += "fr ${s.id}: not « Créer la partie « $label » »: $fr"
                for (l in langs) {
                    val button = tr(l, "Créer la partie « %s »")
                    val shown = Regex(Regex.escape(button.substringBefore("%s")) + ".+?" + Regex.escape(button.substringAfter("%s")))
                    tr(l, fr).let { t -> if (!shown.containsMatchIn(t)) bad += "${l.code} ${s.id}: not its button « $button »: $t" }
                }
            }
        }
        report("steps that contradict the screen", bad)
    }

    companion object {
        private const val OPEN = "«“„「『\"‘"
        private const val CLOSE = "»”“」』\"’"
    }
}
