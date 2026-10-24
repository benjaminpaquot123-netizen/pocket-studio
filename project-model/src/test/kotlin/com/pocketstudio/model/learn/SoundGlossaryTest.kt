package com.pocketstudio.model.learn

import com.pocketstudio.model.currentSceneContent
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundGlossaryTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    @Test
    fun aboutThirtyWordsEachWithAKeyATextAndAnExample() {
        val words = SoundGlossary.ALL
        assertTrue("${words.size} words", words.size >= 30)
        assertEquals("keys are unique", words.size, words.map { it.key }.toSet().size)
        assertEquals("terms are unique", words.size, words.map { it.term }.toSet().size)
        // The words Benjamin listed are all there.
        val wanted = listOf(
            "kick", "snare", "hat", "clap", "808", "bass", "pad", "lead", "arp", "chord", "scale", "tonic", "tempo", "swing", "quantize",
            "bar", "part", "intro", "verse", "chorus", "buildup", "drop", "break", "fill", "sidechain", "reverb", "echo", "filter",
            "compressor", "saturation", "automation", "mix", "master",
        )
        assertEquals(emptyList<String>(), wanted - words.map { it.key }.toSet())
        // Every word is under one heading, in the list's order.
        assertEquals(words.map { it.key }, SoundGlossary.GROUPS.flatMap { it.second })
        words.forEach { w ->
            assertTrue(w.key, w.term.isNotBlank() && w.text.length in 20..260)
            // One or two plain sentences.
            assertTrue("${w.key}: ${w.text}", w.text.count { it == '.' || it == '!' || it == '?' } in 1..3)
        }
    }

    @Test
    fun everyExampleHasSomethingToHearAndItsTakesAreNamed() {
        SoundGlossary.ALL.forEach { w ->
            val ex = w.example()
            assertTrue(w.key, ex.takes.isNotEmpty() && ex.takes.size <= 2 && ex.bars in 1..4)
            ex.takes.forEach { p ->
                assertTrue("${w.key}: nothing to hear", !p.currentSceneContent().isEmpty)
                assertTrue("${w.key}: tempo ${p.tempoBpm}", p.tempoBpm in 60.0..200.0)
                // Each take is a project the engine can take as it is.
                assertEquals(w.key, p.sanitized().tempoBpm, p.tempoBpm, 0.0)
            }
            if (ex.takes.size > 1) {
                assertEquals("${w.key}: one label a take", ex.takes.size, ex.labels.size)
                assertNotEquals("${w.key}: « sans » and « avec » must differ", ex.takes[0], ex.takes[1])
            }
            // The same example every time (it is built, not drawn at random).
            assertEquals(w.key, ex, w.example())
        }
    }

    @Test
    fun everyWordHasItsEnglishAndDutchText() {
        I18n.lang = Lang.FR
        val french = SoundGlossary.ALL.flatMap { w -> listOf(w.term, w.text) + w.example().labels }.toSet()
        listOf(Lang.EN, Lang.NL).forEach { lang ->
            val table = I18n.table(lang)
            val missing = french.filter { table[it].isNullOrBlank() }
            assertTrue("${lang.code}: $missing", missing.isEmpty())
        }
        I18n.lang = Lang.NL
        val nl = SoundGlossary.ALL.first { it.key == "reverb" }
        assertNotEquals("Réverbe", nl.term)
    }

    @Test
    fun searchIgnoresAccentsAndCaseAndPutsTermsFirst() {
        assertEquals("reverb", SoundGlossary.search("reverbe").first().key)
        assertEquals("echo", SoundGlossary.search("ÉCHO").first().key)
        assertEquals(SoundGlossary.ALL.size, SoundGlossary.search("  ").size)
        // « kick » is a word, and other words talk about it after it.
        val kick = SoundGlossary.search("kick")
        assertEquals("kick", kick.first().key)
        assertTrue(kick.size > 2)
        assertTrue(SoundGlossary.search("zzzz").isEmpty())
    }

    @Test
    fun aHelpOfTheAppFindsItsWord() {
        assertEquals("reverb", SoundGlossary.forHelp("Réverbe")?.key)
        assertEquals("swing", SoundGlossary.forHelp("Swing")?.key)
        assertNotNull(SoundGlossary.forHelp("Tempo (BPM)"))
        assertEquals(null, SoundGlossary.forHelp("Latence"))
    }
}
