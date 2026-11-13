package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, N24: the volumes' step (Facile) said « le nombre sous le curseur » (ja « フェーダーの下の数字 ») while the
 * Mode simple's fader shows a word, « Normal » (ふつう), then « −3 », « −6 »… (MixerScreen.levelWords). The step
 * says what is written there, in every language with that language's « Normal ».
 */
class Tour6TutoMixWordsTest {
    @Test
    fun theVolumeStepSaysTheWordUnderTheFader() {
        val bad = mutableListOf<String>()
        var checked = 0
        // jump up: TutoLot4Test (C3, C4) pins « nombre sous le curseur » there, until its owner updates it.
        for (style in TutoCourse.STYLES) {
            for (s in TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.filter { it.check.startsWith("changed:volumes") }) {
                val text = s.text
                if ("nombre sous le curseur" in text) bad += "$style/${s.id}: $text"
                if (!text.contains("sous le curseur", ignoreCase = true)) continue
                checked++
                if ("« Normal »" !in text) bad += "$style/${s.id}: no « Normal »: $text"
                for (lang in Lang.entries - Lang.FR) {
                    val t = I18n.table(lang)
                    val normal = t["Normal"] ?: "Normal"
                    val v = t[text] ?: continue
                    if (normal !in v) bad += "${lang.code} $style/${s.id}: no « $normal »: $v"
                }
            }
        }
        assertTrue("${bad.size} volume steps:\n" + bad.take(30).joinToString("\n"), bad.isEmpty())
        assertTrue("no volume step checked", checked >= 20)
    }
}
