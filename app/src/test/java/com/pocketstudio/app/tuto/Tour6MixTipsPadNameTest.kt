package com.pocketstudio.app.tuto

import com.pocketstudio.app.rhythm.padName
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.MixTips
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, Japanese seat: the Mix said « 「Rim」が他を覆っています。 », the pad's French name, while the pad reads « リム ».
 * The Mix's tip names a pad as the pad shows it, in every language and in the Mode simple; a sound the user put on
 * the pad keeps its own name, even when it is a word the app translates.
 */
class Tour6MixTipsPadNameTest {
    @After
    fun end() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    /** A project whose pad [lane] plays and has its fader at the top (the « couvre les autres » tip). */
    private fun hot(lane: Int, sample: PadSample? = null, voice: PadVoice? = null): Project {
        val d = DrumPattern()
        val drums = d.copy(lanes = d.lanes.toMutableList().also { it[lane] = it[lane].copy(hits = mapOf(0 to Hit(), 8 to Hit()), sample = sample) })
        val p = Project(drums = drums, padVoices = voice?.let { mapOf(lane to it) } ?: emptyMap())
        val ch = Mixer.padChannel(lane)
        return p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(volume = 1f) else c }))
    }

    private fun tipOf(p: Project, lane: Int): String =
        MixTips.of(p).firstOrNull { it.ch == Mixer.padChannel(lane) }?.text ?: error("no tip for pad $lane")

    @Test
    fun theMixNamesAPadAsThePadShowsIt() {
        val bad = ArrayList<String>()
        for (lang in listOf(Lang.JA, Lang.EN, Lang.AR, Lang.FR)) for (everyday in listOf(false, true)) {
            I18n.lang = lang
            I18n.everyday = everyday
            for (lane in 0 until 16) {
                val p = hot(lane)
                val shown = padName(p, lane)
                val text = tipOf(p, lane)
                if (shown !in text) bad += "${lang.code}${if (everyday) " simple" else ""} pad $lane shows « $shown »: $text"
            }
        }
        assertTrue("${bad.size} tips that do not name the pad as it shows:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun aStudioVoiceAndTheUsersSoundAreNamedAsOnThePad() {
        I18n.lang = Lang.JA
        // A studio voice (« Rumble » on its pad), and a file of the user's called « Rim »: never translated.
        val rumble = hot(1, voice = PadVoice(PadVoice.RUMBLE))
        val mine = hot(10, sample = PadSample(file = "rim.wav", name = "Rim"))
        val text = tipOf(rumble, 1)
        assertTrue("rumble: « ${padName(rumble, 1)} »: $text", padName(rumble, 1) in text)
        val own = tipOf(mine, 10)
        assertTrue("the user's « Rim » stays as it is: $own", "「Rim」" in own && padName(mine, 10) == "Rim")
    }
}
