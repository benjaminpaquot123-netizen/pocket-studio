package com.pocketstudio.app.tuto

import com.pocketstudio.app.tuto.TutoReadability.PanelState.GRID
import com.pocketstudio.app.tuto.TutoReadability.PanelState.OPEN
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoStep
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tour 7, P6 (docs/audits/ultra/tour-7-plan-p6-p7.md § 1.4): every Facile consigne, in French and the 13 other
 * languages, fits the panel in the three cases (360 dp at 100 % in its own state, 360 dp at 130 % OPEN, 320 dp at
 * 100 % OPEN). The ones that do not yet are frozen in `tuto/readability-facile-exceptions.txt`: the list only shrinks
 * (blocs 1 to 4 empty it). Pro is a ratchet: the count of consignes out of budget only goes down.
 */
class TutoReadabilityBudgetTest {
    @After fun french() { I18n.lang = Lang.FR }

    /** The plan's state of a step: cells, crosses or buttons to touch in sight (GRID), else OPEN. */
    private fun state(s: TutoStep) =
        if (s.check.substringBefore(':') in GRID_CHECKS || s.check.startsWith("changed:")) GRID else OPEN

    /** « style/step lang case lines/budget » for each case [text] does not fit. */
    private fun misses(style: String, s: TutoStep, text: String, lang: Lang): List<String> {
        I18n.lang = lang
        val shown = tr(text)
        val cases = listOf(Triple(state(s), 1f, 360), Triple(OPEN, 1.3f, 360), Triple(OPEN, 1f, 320))
        return cases.mapNotNull { (st, scale, w) ->
            val b = TutoReadability.budget(st, lang, scale, w)
            val n = TutoReadability.lines(shown, b.charsPerLine, lang)
            if (n <= b.maxLines) null else "$style/${s.id} ${lang.code} $st ${(scale * 100).toInt()}% ${w}dp $n/${b.maxLines}"
        }
    }

    private fun all(level: TutoLevel): List<String> = TutoCourse.STYLES.flatMap { style ->
        TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.filter { !it.manual }.flatMap { s ->
            Lang.entries.flatMap { lang -> misses(style, s, s.textFor(level), lang) }
        }
    }

    private fun frozen(): Set<String> =
        javaClass.classLoader!!.getResourceAsStream(FACILE)!!.bufferedReader().readLines()
            .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.map(::case).toSet()

    /** An entry without its « lines/budget »: a text shortened but still too long stays the same known case. */
    private fun case(entry: String) = entry.substringBeforeLast(' ')

    @Test fun everyFacileConsigneFitsOrIsAKnownException() {
        val now = all(TutoLevel.Facile)
        File("build/tuto-readability-facile.txt").writeText(now.joinToString("\n", postfix = "\n"))
        val known = frozen()
        val fresh = now.filter { case(it) !in known }
        assertTrue("${fresh.size} Facile consignes out of the panel's budget (style/step lang state scale width lines/budget):\n" +
            fresh.take(60).joinToString("\n"), fresh.isEmpty())
        assertTrue("${known.size} exceptions: the list only shrinks (frozen at $FACILE_FROZEN)", known.size <= FACILE_FROZEN)
    }

    /** Tour 8, garde-fous: an exception whose consigne now fits (shortened, or gone) makes this red with its name. */
    @Test fun everyFacileExceptionStillMisses() {
        val now = all(TutoLevel.Facile).map(::case).toSet()
        val stale = frozen().filter { it !in now }.sorted()
        assertTrue("${stale.size} entries of $FACILE now fit, delete them (style/step lang state scale width):\n" + stale.joinToString("\n"), stale.isEmpty())
    }

    @Test fun proOutOfBudgetOnlyGoesDown() {
        val n = all(TutoLevel.Pro).size
        File("build/tuto-readability-pro-count.txt").writeText("$n\n")
        assertTrue("$n Pro consignes out of budget: the ratchet is $PRO_RATCHET (lower it when it goes down)", n <= PRO_RATCHET)
    }

    companion object {
        const val FACILE = "tuto/readability-facile-exceptions.txt"
        const val FACILE_FROZEN = 409
        const val PRO_RATCHET = 3725
        val GRID_CHECKS = setOf("lane", "gap", "fill", "crash", "velocity", "triplet", "offset", "swing")
    }
}
