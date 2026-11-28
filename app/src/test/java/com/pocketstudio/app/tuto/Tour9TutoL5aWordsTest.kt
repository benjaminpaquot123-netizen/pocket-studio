package com.pocketstudio.app.tuto

import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a: the course's words against what the screen shows.
 * F066 (ja, acid techno): the step said « open the アシッド tab », the tab reads « Acid 303 ».
 * F067 (ja, acid techno, ch. 2): the intro promised a « crunch » 909 kick; the pad reads « Kick studio » (スタジオキック).
 */
class Tour9TutoL5aWordsTest {
    private fun texts(): List<Pair<String, String>> = buildList {
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            for (level in TutoLevel.entries) course.chaptersFor(level).forEach { ch ->
                add("$style/${ch.id}/why" to ch.why)
                ch.steps.forEach { s ->
                    add("$style/${s.id}/text/$level" to s.textFor(level))
                    s.why?.let { add("$style/${s.id}/why" to it) }
                }
            }
        }
    }.filter { it.second.isNotBlank() }.distinct()

    @Test
    fun aStepNamesTheAcidTabAsTheTabShowsIt() {
        val bad = ArrayList<String>()
        val tab = Regex("""onglet\s+«?\s*Acid""")
        val acid = texts().filter { tab.containsMatchIn(it.second) }
        assertTrue("no step names the acid tab any more", acid.isNotEmpty())
        for (lang in Lang.entries) {
            val table = I18n.table(lang)
            val label = (if (lang == Lang.FR) DefaultNames.ACID else table[DefaultNames.ACID]) ?: DefaultNames.ACID
            acid.forEach { (where, fr) ->
                val shown = if (lang == Lang.FR) fr else table[fr]?.takeIf { it.isNotBlank() } ?: return@forEach
                if (label !in shown) bad += "${lang.code} $where: $shown"
            }
        }
        assertTrue("the acid tab is called « ${DefaultNames.ACID} » on screen (F066):\n" + bad.take(20).joinToString("\n") { it.take(160) }, bad.isEmpty())
    }

    @Test
    fun theAcidTechnoIntroNamesTheKickThePadShows() {
        val style = StarterWizard.style("acid_techno")!!
        val kick = StudioDrums.kickVoice(style, Harmony(style.melody.root))!!
        val pad = PadVoice.NAMES[kick.kind]
        val course = TutoCourse.of("acid_techno")!!
        val bad = ArrayList<String>()
        for (level in TutoLevel.entries) {
            val fr = course.chaptersFor(level).flatMap { it.steps }.first { it.id == "ready" }.textFor(level)
            for (lang in Lang.entries) {
                val table = I18n.table(lang)
                val shown = if (lang == Lang.FR) fr else table[fr]?.takeIf { it.isNotBlank() } ?: continue
                val name = if (lang == Lang.FR) pad else table[pad] ?: pad
                if (name !in shown || "crunch" in shown.lowercase()) bad += "${lang.code} $level: $shown"
            }
        }
        assertTrue("the intro names the kick as its pad shows it, « $pad » (F067):\n" + bad.take(20).joinToString("\n") { it.take(160) }, bad.isEmpty())
    }
}
