package com.pocketstudio.app.guide

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tour 7, lot 6, D5 T16 (seat 1c #27, ja): the track sheet wrote « Plaits » as is, in every language and every mode. */
class TrackSheetNamesTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    @Test
    fun thePlaitsChipSpeaksTheInterfaceLanguage() {
        I18n.lang = Lang.JA
        val japanese = Regex("[\\u3040-\\u30FF\\u4E00-\\u9FFF]")
        listOf(true, false).forEach { simple ->
            val shown = plaitsLabel(simple)
            assertTrue("simple=$simple: « $shown » has no Japanese", japanese.containsMatchIn(shown))
        }
        I18n.lang = Lang.FR
        assertEquals("Plaits", plaitsLabel(simple = false))
    }
}
