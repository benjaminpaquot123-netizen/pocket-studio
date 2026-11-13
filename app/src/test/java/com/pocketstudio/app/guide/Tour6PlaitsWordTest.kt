package com.pocketstudio.app.guide

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tour 6, lot 2 « tuto-consignes », T16 (seat D a, Japanese, Mode simple: « Plaits » on the layer's sound picker, untranslated and
 * meaningless to a beginner): in the Mode simple the chip says what it is, in the person's language; the Pro keeps the module's name.
 */
class Tour6PlaitsWordTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    @Test
    fun theModeSimpleSaysOtherSynthsInItsLanguage() {
        for (lang in listOf(Lang.FR, Lang.JA, Lang.DE)) {
            I18n.lang = lang
            assertFalse("${lang.code}: ${plaitsLabel(simple = true)}", plaitsLabel(simple = true).contains("Plaits"))
        }
        I18n.lang = Lang.JA
        assertEquals("ほかのシンセ", plaitsLabel(simple = true))
        I18n.lang = Lang.FR
        assertEquals("Plaits", plaitsLabel(simple = false))
    }
}
