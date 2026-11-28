package com.pocketstudio.model

import com.pocketstudio.model.guide.GuideSheets
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, F133: « Le nom d'une nouvelle piste ne répète jamais un mot » (seats read « 808 808 », « Baixo Baixo pluck »
 * on the track's chip: its name, then its sound). Every language, every kind under « + Piste », with no style and in
 * each style.
 */
class TrackNameNoRepeatTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    private fun repeated(text: String): List<String> =
        text.split(Regex("""\s+""")).filter { it.isNotBlank() }.map { it.lowercase() }.groupBy { it }.filter { it.value.size > 1 }.keys.toList()

    @Test
    fun aNewTracksChipNeverSaysAWordTwice() {
        val styles = listOf<StarterWizard.Style?>(null) + StarterWizard.STYLES
        val faults = mutableListOf<String>()
        var before = 0
        Lang.entries.forEach { lang ->
            I18n.lang = lang
            styles.forEach { style ->
                TrackKind.entries.forEach { kind ->
                    val p = Project()
                    val t = NewTracks.add(p, kind, style)!!.tracks.first { n -> p.tracks.none { it.id == n.id } }
                    val name = DefaultNames.shown(t.name)
                    val sound = GuideSheets.soundName(t)
                    if (repeated("$name $sound").isNotEmpty()) before++
                    val chip = "$name ${DefaultNames.soundBeside(name, sound)}"
                    if (repeated(chip).isNotEmpty()) faults += "${lang.code} ${style?.id} ${kind.id}: « $chip »"
                }
            }
        }
        assertTrue("the case the seats saw is still met (name, then the raw sound)", before > 0)
        assertTrue(faults.take(10).joinToString("\n"), faults.isEmpty())
    }

    @Test
    fun theSoundKeepsWhatTheNameDoesNotSay() {
        assertEquals("pluck", DefaultNames.soundBeside("Baixo", "Baixo pluck"))
        assertEquals("", DefaultNames.soundBeside("808", "808"))
        assertEquals("Basse sub", DefaultNames.soundBeside("Mélodie", "Basse sub"))
        assertEquals("sub", DefaultNames.soundBeside("Basse", "basse sub"))
    }
}
