package com.pocketstudio.app.guide

import com.pocketstudio.app.rhythm.padName
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.guideTracks
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, seat Fa-1 (« Rim » at the Mix, « Bord de caisse » on the pad), everywhere a pad is named: the Guide's track
 * sheet names a pad, in its title and in « Copier cette chaîne sur… », as the pad shows it, in the 15 languages and
 * in the Mode simple's words.
 */
class Tour6TrackSheetPadNameTest {
    @After
    fun end() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    @Test
    fun theTrackSheetNamesAPadAsThePadShowsIt() {
        val d = DrumPattern()
        val p = Project(drums = d.copy(lanes = d.lanes.map { it.copy(hits = mapOf(0 to Hit())) }))
        val bad = ArrayList<String>()
        for (lang in Lang.entries) for (everyday in listOf(false, true)) {
            I18n.lang = lang
            I18n.everyday = everyday
            val tracks = p.guideTracks()
            val pads = tracks.filter { it.key.startsWith("pad:") }
            assertTrue("16 pads", pads.size == 16)
            fun shown(key: String) = padName(p, key.removePrefix("pad:").toInt())
            val where = "${lang.code}${if (everyday) " simple" else ""}"
            for (t in pads) if (sheetName(p, t) != shown(t.key)) bad += "$where title ${t.key}: « ${sheetName(p, t)} », pad « ${shown(t.key)} »"
            for (s in sheetTargets(p, tracks, "pad:0")) if (s.name != shown(s.key)) bad += "$where copy to ${s.key}: « ${s.name} », pad « ${shown(s.key)} »"
        }
        assertTrue("${bad.size} pads named otherwise in the track sheet:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
