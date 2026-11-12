package com.pocketstudio.app.ui

import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Audit ultra tour 0 (seat b): what the words say matches what is there, in the person's language. */
class WordsThatMatchTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    /** « Simple » said « 4 onglets » while its bar shows five. */
    @Test
    fun simpleModeCountsItsOwnTabs() {
        for (lang in listOf(Lang.FR, Lang.EN, Lang.DE, Lang.JA)) {
            I18n.lang = lang
            val said = Regex("\\d+").findAll(simpleModeBlurb()).map { it.value.toInt() }.toList()
            assertEquals("$lang: ${simpleModeBlurb()}", listOf(SimpleTab.entries.size), said)
        }
    }

    /** An English person shared « Remix-morceau-….m4a ». */
    @Test
    fun anExportsFileNameIsInTheAppsLanguage() {
        I18n.lang = Lang.EN
        val name = WavExporter.fileName("Remix-" + RhythmViewModel.fileWord("morceau"), java.time.LocalDateTime.of(2026, 11, 4, 16, 54, 32))
        assertFalse(name, "morceau" in name)
        assertTrue(name, name.startsWith("Remix-song-"))
        assertEquals("loop", RhythmViewModel.fileWord("boucle"))
        // A track's own name stays as it is; in French the files keep their words.
        assertEquals("piste2-basse", RhythmViewModel.fileWord("piste2-basse"))
        I18n.lang = Lang.FR
        assertEquals("morceau", RhythmViewModel.fileWord("morceau"))
    }
}
