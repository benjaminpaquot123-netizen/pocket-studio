package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 », seat H a (house, French, phone, Mode simple, tuto Facile): a beginner reads each step of the
 * house tutorial and knows what to do without guessing, in French and in every language (no text falls back to French).
 */
class Tour6Tuto4HouseTest {
    private val course = TutoCourse.of("house")!!
    private val target = TutoTarget.of("house")!!
    private val translated = Lang.entries - Lang.FR

    private fun chapter(id: String) = course.chapters.first { it.id == id }
    private fun step(id: String) = course.chapters.flatMap { it.steps }.first { it.id == id }

    /** Every French text of the house course (titles, « Pourquoi ? », consignes Facile and Pro, « Suivant »'s warning). */
    private fun allTexts(): List<String> = course.chapters.flatMap { c ->
        listOfNotNull(c.title, c.titlePro, c.why) + c.steps.flatMap { s -> listOfNotNull(s.text, s.textPro, s.why, s.skip) }
    }.filter { it.isNotEmpty() }.distinct()

    /** What a Facile reader sees, in order: each chapter's title, then each of its steps' consigne. */
    private fun facileReading(): List<String> = course.chaptersFor(TutoLevel.Facile).flatMap { c -> listOf(c.title) + c.steps.map { it.text } }

    /** [fr] in [lang], null when that language has none (it would show the French). */
    private fun tr(lang: Lang, fr: String): String? = I18n.table(lang)[fr]?.takeIf { it.isNotBlank() }

    private fun word(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: fr

    /** [texts] said in every language (a missing one is shown in French). */
    private fun everyLanguage(texts: List<String>, bad: MutableList<String>) {
        for (fr in texts) for (lang in translated) if (tr(lang, fr) == null) bad += "${lang.code}: no translation of « $fr »"
    }

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())

    /** M1: chapters 2 and 4 name the pad by what is written on it (« Rim », shown « Bord de caisse »), never « tambourin ». */
    @Test
    fun thePadIsNamedAsWrittenOnIt() {
        val bad = mutableListOf<String>()
        allTexts().filter { "tambourin" in it.lowercase() }.forEach { bad += "fr: « $it »" }
        val rim = allTexts().filter { "« Rim »" in it }
        listOf(step("perc_do").text, step("ready").text).forEach { if ("« Rim »" !in it) bad += "fr: the pad not named « Rim »: $it" }
        if ("tambourin" in chapter("hats").title.lowercase()) bad += "fr: title ${chapter("hats").title}"
        for (lang in translated) {
            val name = word(lang, "Rim")
            for (fr in rim) tr(lang, fr)?.let { v -> if (name !in v) bad += "${lang.code}: no « $name »: $v" }
            val all = allTexts().mapNotNull { tr(lang, it) }
            all.filter { v -> TAMBOURINE.any { it.containsMatchIn(v) } }.forEach { bad += "${lang.code}: tambourine in « $it »" }
        }
        everyLanguage(allTexts(), bad)
        report("texts that do not name the pad as written", bad)
    }

    companion object {
        /** The word for a tambourine in each language's former texts. */
        val TAMBOURINE = listOf(
            "tambourine", "tamburin", "pandereta", "खंजरी", "tamborin", "tamburello", "タンバリン", "탬버린", "tamboerijn",
            "tamburyn", "pandeiro", "(?<!\\p{L})tef(?!\\p{L}{2})", "铃鼓", "الدف(?!\\p{L})",
        ).map { Regex(it, RegexOption.IGNORE_CASE) }

        /** Each word (as written) → what has to stand by it where a Facile reader first meets it. */
        val FIRST_SIGHT: List<Pair<Regex, Regex>> = listOf(
            Regex("""(?i)\bkick\b""") to Regex("""grosse caisse"""),
            Regex("""\bGroove\b""") to Regex("""boucle de base"""),
            Regex("""\brelances?\b""") to Regex("""petits coups"""),
            Regex("""\bBreak\b""") to Regex("""pause"""),
            Regex("""« Mix »""") to Regex("""volume de chaque son"""),
            Regex("""\bsolo\b""") to Regex("""toute seule"""),
            Regex("""\bTonique\b""") to Regex("""note de départ"""),
            Regex("""\bBPM\b""") to Regex("""battements par minute"""),
        )
    }
}
