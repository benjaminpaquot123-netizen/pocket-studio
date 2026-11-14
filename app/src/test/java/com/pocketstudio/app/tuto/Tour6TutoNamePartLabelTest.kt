package com.pocketstudio.app.tuto

import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoNeed
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, Japanese seat: the button « Nommer la partie « Groove » » read 「Groove」 in Latin letters, while the part
 * is shown 「グルーヴ」 everywhere else. The button names the part as the app shows it, in every language.
 */
class Tour6TutoNamePartLabelTest {
    @After
    fun end() { I18n.lang = Lang.FR }

    @Test
    fun theNameButtonSaysThePartAsShown() {
        val bad = ArrayList<String>()
        for (lang in listOf(Lang.JA, Lang.AR, Lang.HI)) {
            I18n.lang = lang
            for (label in listOf("Groove", "Refrain", "Drop")) {
                val text = needLabel(TutoNeed.Name(label))
                if (text.any { it in 'A'..'Z' || it in 'a'..'z' }) bad += "${lang.code} « $label »: Latin letters in $text"
                if (Scene.shownLabel(label) !in text) bad += "${lang.code} « $label »: not « ${Scene.shownLabel(label)} »: $text"
            }
        }
        assertTrue("${bad.size} buttons that name the part in French:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
