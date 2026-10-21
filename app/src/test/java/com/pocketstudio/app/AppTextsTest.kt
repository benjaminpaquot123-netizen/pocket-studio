package com.pocketstudio.app

import com.pocketstudio.app.instruments.InstrumentLibrary
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.PROJECT_STARTERS
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's own data texts shown through `tr(value)` (the model's are checked by ModelTextsTest):
 * instrument families, the "Nouveau projet" choices, the parts of a separated song. Each one has
 * an English and a Dutch translation, so a new entry without them fails here.
 */
class AppTextsTest {
    private fun texts(): Set<String> = buildSet {
        addAll(InstrumentLibrary.FAMILIES)
        addAll(RhythmViewModel.STEM_LABELS)
        PROJECT_STARTERS.forEach { (s, _) -> add(s.title); add(s.description) }
    }

    @Test
    fun everyDataTextHasItsTranslations() {
        val texts = texts()
        assertTrue(texts.size > 20)
        val problems = listOf(Lang.EN, Lang.NL).flatMap { lang ->
            val table = I18n.table(lang)
            texts.filter { table[it].isNullOrBlank() }.map { "no ${lang.code} for \"$it\"" }
        }
        assertTrue("${problems.size} problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
