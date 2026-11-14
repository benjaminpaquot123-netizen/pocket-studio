package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
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

    /**
     * M4 (screens 182, 183, 207): the Montée's step said its silence was for later, its success said « son dernier temps
     * se tait », and chapter « Les transitions » still had hits to cross out. The step says what the part really is
     * once made: its accelerating pad silent on the last beat, and every pad still playing there named (to silence later).
     */
    @Test
    fun theMonteeSaysWhatItsLastBeatReallyHolds() {
        val bad = mutableListOf<String>()
        val make = step("make_montee")
        val gap = step("gap")
        var p = target.blank()
        for (s in course.chaptersFor(TutoLevel.Facile).flatMap { it.steps }) {
            if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
            if (s.id == make.id) break
        }
        val montee = p.allScenes.firstOrNull { it.label == "Montée" } ?: error("no Montée made")
        val d = montee.drums
        val last = d.steps - TutoChecks.gapCells(gap, target) until d.steps
        val playing = TutoChecks.gapLanes(gap, target).filter { k -> d.lanes.getOrNull(k)?.hits?.keys?.any { it in last } == true }
        val build = TutoChecks.laneOf(make.build)
        if (build in playing) bad += "the built pad plays on the last beat"
        if ("se tait" !in make.text) bad += "fr: the built pad's silence on the last beat not said: ${make.text}"
        for (k in playing) {
            val w = WORDS[TutoChecks.roleOfLane(k, target)] ?: "pad ${k + 1}"
            if (w !in make.text) bad += "fr: pad ${k + 1} still plays on the last beat, not said (« $w »): ${make.text}"
        }
        if (playing.isEmpty()) bad += "nothing left for « ${gap.id} » to cross out: its step would be done already"
        for (lang in translated) if (tr(lang, make.text) == null) bad += "${lang.code}: no translation of « ${make.text} »"
        report("Montée texts that do not say what plays", bad)
    }

    companion object {
        /** How the house course names each drum role in its texts. */
        val WORDS = mapOf("hat" to "charleys", "open_hat" to "charleys", "perc" to "« Rim »", "clap" to "clap", "kick" to "kick")
    }
}
