package com.pocketstudio.app.tuto

import com.pocketstudio.app.rhythm.padName
import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, French seat (House, Mode simple): the Mix's drum strip said « Rim » and « Crash » while the pads read
 * « Bord de caisse » and « Cymbale ». Each pad under the Batterie's strip is named as the pad shows it, in the
 * 15 languages, in the Mode simple's words too.
 */
class Tour6SimpleMixPadNameTest {
    @After
    fun end() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    @Test
    fun theSimpleMixNamesEachPadAsThePadShowsIt() {
        val d = DrumPattern()
        val p = Project(drums = d.copy(lanes = d.lanes.map { it.copy(hits = mapOf(0 to Hit())) }))
        val bad = ArrayList<String>()
        for (lang in Lang.entries) for (everyday in listOf(false, true)) {
            I18n.lang = lang
            I18n.everyday = everyday
            val pads = simpleTracks(p).first { it.key == SongPart.DRUMS }.pads
            assertTrue("16 pads under the drums", pads.size == 16)
            for (pad in pads) {
                val shown = padName(p, pad.lane)
                if (pad.name != shown) bad += "${lang.code}${if (everyday) " simple" else ""} pad ${pad.lane}: Mix « ${pad.name} », pad « $shown »"
            }
        }
        assertTrue("${bad.size} pads named otherwise at the Mix:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
