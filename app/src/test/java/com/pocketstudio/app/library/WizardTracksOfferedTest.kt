package com.pocketstudio.app.library

import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.app.ui.timelineRows
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit ultra round 1, n° 32 (seats A c, A d): the assistant's Drum & Bass and Boom bap opened on a
 * timeline with an « Acid 303 » track the assistant never offered. A track is shown only when it plays.
 */
class WizardTracksOfferedTest {
    @Test
    fun aSongWithoutTheAcidStepShowsNoAcidTrack() {
        for (id in listOf("boom_bap", "dnb", "lofi", "trap")) {
            val style = StarterWizard.style(id)!!
            assertTrue(id, StarterWizard.steps(style).none { it == StarterWizard.Step.Acid })
            val p = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, StarterWizard.Mood.entries.first(), emptyMap())
            assertFalse("$id: timeline", timelineRows(p).any { it.key == SongPart.ACID })
            assertFalse("$id: Mes pistes", simpleTracks(p).any { it.key == SongPart.ACID })
        }
    }

    @Test
    fun anAcidStyleStillShowsItsAcidTrack() {
        val style = StarterWizard.style("acid_house")!!
        val p = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood, emptyMap())
        assertTrue(timelineRows(p).any { it.key == SongPart.ACID })
    }
}
