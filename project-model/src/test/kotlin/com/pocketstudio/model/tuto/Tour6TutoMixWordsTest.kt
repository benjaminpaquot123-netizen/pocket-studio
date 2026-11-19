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
        assertTrue("no volume step checked", checked >= 18)
    }

    /**
     * Tour 8, garde-fous: the volume steps without « sous le curseur » were not read at all. Every volume step, in
     * French and in each of the 14 other languages, is translated and names each track its check asks to lower (the
     * name as the Mix shows it in that language).
     */
    @Test
    fun everyVolumeStepNamesItsTracksInEveryLanguage() {
        val bad = mutableListOf<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) {
            for (s in TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.filter { it.check.startsWith("changed:volumes") }) {
                checked++
                val text = s.textFor(TutoLevel.Facile)
                val names = s.check.removePrefix("changed:volumes").removePrefix(":").split(',').filter { it.isNotBlank() }.map { role ->
                    TRACK[role.trim()] ?: run { bad += "$style/${s.id}: role « $role » has no name here"; role }
                }
                if (names.isEmpty()) bad += "$style/${s.id}: no track to lower in « ${s.check} »"
                names.filter { it !in text }.forEach { bad += "fr $style/${s.id}: no « $it »: $text" }
                for (lang in Lang.entries - Lang.FR) {
                    val t = I18n.table(lang)
                    val v = t[text]?.takeIf { it.isNotEmpty() }
                    if (v == null) { bad += "${lang.code} $style/${s.id}: not translated"; continue }
                    names.map { t[it]?.takeIf { n -> n.isNotEmpty() } ?: it }.filter { it !in v }.forEach { bad += "${lang.code} $style/${s.id}: no « $it »: $v" }
                }
            }
        }
        assertTrue("${bad.size} volume steps:\n" + bad.take(30).joinToString("\n"), bad.isEmpty())
        assertTrue("$checked volume steps checked", checked >= 36)
    }

    private companion object {
        /** The track a volume check's role names, as the Mix and the consignes write it in French. */
        val TRACK = mapOf("pad" to "Accords", "lead" to "Mélodie", "bass" to "Basse", "clap" to "Clap", "acid" to "303")
    }
}
