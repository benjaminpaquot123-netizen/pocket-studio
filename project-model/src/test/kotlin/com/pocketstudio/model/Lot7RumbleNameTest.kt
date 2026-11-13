package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 6 lot 7 (T2 outside French, English, German): the tutorial's kit puts the pad's own voice name on the rumble's
 * pad (« Rumble », PadVoice.NAMES); the step says it in the person's language (럼블, ランブル), the pad showed « Rumble ».
 * A pad named after a built-in voice is an app name: shown in the interface's language, as the step says it.
 */
class Lot7RumbleNameTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    @Test
    fun theVoicesNamesAreShownInTheInterfacesLanguage() {
        for (lang in Lang.entries) {
            I18n.lang = lang
            for (name in PadVoice.NAMES.filter { it.isNotBlank() }) assertEquals("$lang: $name", tr(name), DefaultNames.shown(name))
        }
        I18n.lang = Lang.KO
        assertEquals("럼블", DefaultNames.shown("Rumble"))
        // Saved in another language's words, it is still recognised as ours.
        I18n.lang = Lang.JA
        assertEquals(tr("Rumble"), DefaultNames.shown("럼블"))
    }
}
