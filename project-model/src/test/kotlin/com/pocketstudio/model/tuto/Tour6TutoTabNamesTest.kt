package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 6, N12: a tab is named as it shows. */
class Tour6TutoTabNamesTest {
    private val steps: List<Triple<String, TutoStep, TutoLevel>> = TutoCourse.STYLES.flatMap { style ->
        TutoLevel.entries.flatMap { level -> TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.map { Triple(style, it, level) } }
    }

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * N12: « dans « Le morceau » » (37 courses) while the tab says « Mon morceau » (Mode simple) or « Structure » (Pro):
     * a tab a consigne names is the tab shown at its level (SimpleTab and Space labels, app/ui/Spaces.kt).
     */
    @Test
    fun aTabIsNamedAsShown() {
        val tab = Regex("""(?:onglet|[Dd]ans) « ([^»]+) »""")
        val bad = steps.flatMap { (st, s, l) ->
            val shown = if (l == TutoLevel.Facile) SIMPLE_TABS else PRO_TABS
            tab.findAll(s.textFor(l)).map { it.groupValues[1] }.filter { it in TAB_LIKE && it !in shown }.map { "$st/${s.id}/${l.code}: « $it »" }.toList()
        }.distinct()
        report("tabs named by another name", bad)
    }

    companion object {
        /** The Mode simple's tabs (SimpleTab) and the full app's spaces (Space), French. */
        val SIMPLE_TABS = setOf("Mon morceau", "Rythme", "Clavier", "Mix", "Partager")
        val PRO_TABS = setOf("Structure", "Sons", "Mix", "Live", "Plus")
        /** Names that are, or were, a tab. */
        val TAB_LIKE = SIMPLE_TABS + PRO_TABS + setOf("Le morceau", "Morceau", "Mixeur", "Synthé", "Outils", "Envoyer", "Acide")
    }
}
