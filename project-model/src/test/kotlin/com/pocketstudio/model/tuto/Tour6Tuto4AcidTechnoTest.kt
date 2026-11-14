package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 », the leftovers of the techno fixes said again in the acid techno course and in the Mode simple's
 * words: what the techno course now says right (the filter's « Par partie » curve, a pad named in the singular) is said
 * right here too, in every language.
 */
class Tour6Tuto4AcidTechnoTest {
    private val course = TutoCourse.of("acid_techno")!!
    private val langs = Lang.entries - Lang.FR
    private val steps = course.chapters.flatMap { it.steps }

    private fun step(id: String): TutoStep = steps.first { it.id == id }

    /** [fr] in [lang], never the French shown in its place. */
    private fun tr(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: "(no ${lang.code} translation of « $fr »)"

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * As the techno's (screen 222): the 303's « Par partie » curve is drawn on the Drop's loop (its drum loop, one bar),
     * so it opens and closes the filter each bar; « jusqu'au milieu du Drop » promised a curve as long as the Drop.
     */
    @Test
    fun theFilterCurveIsSaidAsLongAsTheDropsLoop() {
        val bad = ArrayList<String>()
        val target = TutoTarget.of("acid_techno")!!
        var p = target.blank()
        for (s in steps.takeWhile { it.id != "filter" }.filter { s -> listOf("lane:", "notes:", "label:", "part:").any { s.check.startsWith(it) } }) {
            if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: error("${s.id}: no Montre-moi")
        }
        val drop = p.switchScene(p.allScenes.indexOfFirst { it.label == "Drop" })
        val bars = drop.drums.steps / drop.stepsPerBar
        val filter = step("filter").text
        if ("milieu du Drop" in filter) bad += "fr filter: « milieu du Drop » while the curve « Par partie » is the Drop's loop: $filter"
        if (!Regex("boucle du Drop \\($bars mesures?\\)").containsMatchIn(filter)) bad += "fr filter: not « boucle du Drop ($bars mesure) »: $filter"
        for (l in langs) tr(l, filter).let { t -> if (!Regex("(?<!\\d)$bars(?!\\d)").containsMatchIn(t)) bad += "${l.code} filter: no $bars: $t" }
        report("curves said otherwise than drawn", bad)
    }

    /**
     * As the pads' (commit of « Claps » / « Kicks »): when « Lead » and « Leads » give the same word in a language
     * (Italian « Lead »), the Mode simple used to say a quoted « Lead » by the plural's everyday word (« Melodie »);
     * a quoted name is one thing, said in the singular, in every language.
     */
    @Test
    fun theSimpleModeSaysAQuotedLeadInTheSingular() {
        val bad = ArrayList<String>()
        try {
            for (lang in Lang.entries) {
                I18n.lang = lang
                I18n.everyday = false
                val one = I18n.t("Lead")
                if (one != I18n.t("Leads")) continue
                I18n.everyday = true
                val melody = I18n.t("Lead")
                val said = I18n.t("« $one »")
                if (said != "« $melody »") bad += "${lang.code}: « $one » said $said, not the singular's « $melody »"
                // Nor the other way round: the singular's word never leads back to the plural (a track named so is a melody's).
                if (I18n.frenchOf(melody) == "Leads") bad += "${lang.code}: « $melody » leads back to « Leads »"
            }
        } finally {
            I18n.everyday = false
            I18n.lang = Lang.FR
        }
        report("languages saying one lead by a plural", bad)
    }

    /**
     * Seat D a (Japanese, Mode simple): the kick pad read 「バスドラム 1」 while the step said 「バスドラム」. In Japanese the
     * Mode simple's kick pad is named as every course's kick steps quote it.
     */
    @Test
    fun theJapaneseKickPadIsNamedAsTheStepsQuoteIt() {
        val bad = ArrayList<String>()
        try {
            I18n.lang = Lang.JA
            I18n.everyday = true
            val pad = I18n.t("Kick")
            val pads = listOf("Kick", "Kick 2", "Kick studio", "Kick dur").map(I18n::t).toSet()
            val quoted = Regex("「([^」]+)」")
            // The pad's word leads back to « Kick », not to the plural « Kicks » (the same word in Japanese).
            if (I18n.frenchOf(pad) != "Kick") bad += "「$pad」 leads back to « ${I18n.frenchOf(pad)} », not « Kick »"
            for (style in TutoCourse.STYLES) {
                val kicks = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.filter { it.check == "lane:kick" }
                for (s in kicks) for (fr in listOfNotNull(s.text, s.textPro)) {
                    val t = I18n.t(fr)
                    val names = quoted.findAll(t).map { it.groupValues[1].trim() }.filter { "バスドラム" in it || "キック" in it }.toList()
                    // A step may quote another kick pad by its own name (« Kick studio »…), never a name no pad shows.
                    names.filter { it !in pads }.forEach { bad += "$style ${s.id}: pad 「$pad」, step quotes 「$it」: $t" }
                }
            }
        } finally {
            I18n.everyday = false
            I18n.lang = Lang.FR
        }
        report("Japanese kick steps quoting a name the pad does not show", bad)
    }
}
