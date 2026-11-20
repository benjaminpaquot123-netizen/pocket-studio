package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoProgress
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 9 lot L5a: the panel's own words around a step (F058 « À toi : touche « Créer la partie « Intro » ». »). */
class Tour9TutoL5aPanelWordsTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    /** A quote opened again before its own closing mark (« a « b » c »), in the marks the text uses. */
    private fun nested(t: String): Boolean {
        val families = buildList {
            add(Regex("«[^»]*«"))
            add(Regex("「[^」]*「"))
            if ('„' in t) add(Regex("„[^“”]*„")) else add(Regex("“[^”]*“"))
            add(Regex("‘[^’]*‘"))
            add(Regex("\"[^\"]*\"[^\"]*\"[^\"]*\""))
        }
        return families.any { it.containsMatchIn(t) }
    }

    @Test
    fun aButtonNamedInTheMomentLineHasNoQuotesInsideItsQuotes() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        val ui = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[0].steps[0].id), chapters, 0, 0)
        val needs = listOf(TutoNeed.Part("Intro"), TutoNeed.Name("Groove"), TutoNeed.Scene("Drop B"), TutoNeed.Track("bass"), TutoNeed.Bars(2))
        val bad = ArrayList<String>()
        for (lang in Lang.entries) {
            I18n.lang = lang
            needs.forEach { n ->
                val words = momentWords(ui.copy(need = n))!!
                if (nested(words)) bad += "${lang.code} $n: $words"
            }
        }
        assertTrue("quotes inside the same quotes (F058):\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
