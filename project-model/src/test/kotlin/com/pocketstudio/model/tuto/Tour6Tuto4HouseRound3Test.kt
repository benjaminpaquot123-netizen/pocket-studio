package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 », round trip 3, seat Fa-1 (French beginner, House, Mode simple, tuto Facile): what the course
 * shows a Facile reader, as it shows it (the song's map too), and what its texts say against what the steps make.
 */
class Tour6Tuto4HouseRound3Test {
    private val course = TutoCourse.of("house")!!
    private val target = TutoTarget.of("house")!!
    private val translated = Lang.entries - Lang.FR

    private fun step(id: String) = course.chapters.flatMap { it.steps }.first { it.id == id }

    private fun tr(lang: Lang, fr: String): String? = I18n.table(lang)[fr]?.takeIf { it.isNotBlank() }

    private fun word(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: fr

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())

    /**
     * M2 (screens 170, 177): « Break » was first seen on the song's map, shown from the first step of « Comprendre les
     * parties », and explained three steps later. The step where a Facile reader first sees it (its title, its text,
     * the map under it) says what it is, in every language.
     */
    @Test
    fun theBreakIsExplainedWhereTheMapFirstShowsIt() {
        val bad = mutableListOf<String>()
        val labels = TutoChecks.map(target).map { it.label }
        val first = course.chaptersFor(TutoLevel.Facile).flatMap { c ->
            c.steps.map { s -> s to (listOf(c.title, s.text) + if (c.map) labels else emptyList()) }
        }.firstOrNull { (_, seen) -> seen.any { Regex("""\bBreak\b""").containsMatchIn(it) } }?.first
        if (first == null) bad += "fr: « Break » never seen" else {
            if (!Regex("""Break[^.]*pause""").containsMatchIn(first.text)) bad += "fr: « Break » seen at « ${first.id} », not explained there: ${first.text}"
            for (lang in translated) {
                val v = tr(lang, first.text)
                if (v == null) bad += "${lang.code}: no translation of « ${first.text} »"
                else if (word(lang, "Break") !in v) bad += "${lang.code}: no « ${word(lang, "Break")} »: $v"
            }
        }
        report("places where the Break is met unexplained", bad)
    }

    companion object {
        /** How the house course names each drum role in its texts. */
        val WORDS = mapOf("hat" to "charleys", "open_hat" to "charleys", "perc" to "« Rim »", "clap" to "clap", "kick" to "kick")
    }
}
