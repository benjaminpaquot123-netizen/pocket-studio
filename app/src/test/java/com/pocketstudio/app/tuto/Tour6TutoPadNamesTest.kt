package com.pocketstudio.app.tuto

import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, T2: a consigne named a pad the screen does not have (« Perc », « Tambourin », « Bord de caisse », ja
 * 「バスドラム類」). Each pad a consigne names between quotes is a pad's shown name (the kit's, [DrumKitInfo.names],
 * or a studio voice's, RhythmScreen.padVoiceName), and each translation names it as that language's pad does.
 */
class Tour6TutoPadNamesTest {
    @After
    fun end() { I18n.lang = Lang.FR }

    @Test
    fun aPadNamedIsAPadShown() {
        I18n.lang = Lang.FR
        val kit = DrumKitInfo.names.toSet() + VOICES
        val named = Regex("""\bpads? (?:du |de la |des )?« ([^»]+) »""")
        val bad = mutableListOf<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) {
            for (text in listOfNotNull(s.text, s.textPro, s.why)) {
                for (m in named.findAll(text)) {
                    val pad = m.groupValues[1]
                    checked++
                    if (pad !in kit) { bad += "$style/${s.id}: « $pad » is no pad ($kit)"; continue }
                    for (lang in Lang.entries - Lang.FR) {
                        val table = I18n.table(lang)
                        val shown = table[pad] ?: pad
                        val v = table[text] ?: continue
                        if (shown !in v) bad += "${lang.code} $style/${s.id}: the pad shows « $shown »: $v"
                    }
                }
            }
        }
        assertTrue("${bad.size} pads named that the screen does not show:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
        assertTrue(checked > 100)
    }

    companion object {
        /** The studio voices' names on their pad (RhythmScreen.padVoiceName). */
        val VOICES = setOf("Kick studio", "Kick dur", "Rumble", "808")
    }
}
