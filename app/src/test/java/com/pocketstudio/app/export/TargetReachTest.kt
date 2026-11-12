package com.pocketstudio.app.export

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Decision 1A (Benjamin, 2026-11-03), on the reference projects' renders by the real engine
 * (`ps_render` on the .pscmd of `app/src/test/resources/reference-projects`, docs/lots/AUDIT-4.md and
 * docs/lots/AIDE.md): « Club » on `enorme` stops at −10.6 LUFS for −9 because the chain's cap on what it
 * takes off (6 LU) is reached: said, with the loudness reached. A target met (Spotify −14.2 for −14, the
 * Club of `brouillon-jumpup` −9.1) or missed without the cap (a quiet song with no room) says nothing.
 */
class TargetReachTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    @Test
    fun enormesClubSaysTheLoudnessItReachedAndWhy() {
        // enorme-club.render.json: lufs −10.565, loss_db 6.117, mode 0 (whole song), target −9.
        val reached = TargetReach.short(-9.0, -10.565, null, 6.117)
        assertEquals(-10.565, reached!!, 1e-9)
        assertEquals("−10,6 LUFS au lieu de −9 : plus fort, le son s'écraserait.", TargetReach.words(reached, -9.0))
        I18n.lang = Lang.EN
        assertEquals("−10.6 LUFS instead of −9: any louder, the sound would get crushed.", TargetReach.words(reached, -9.0))
    }

    @Test
    fun enormesDropAimedExportIsJudgedOnItsDrop() {
        // enorme.report.json (« Comme le style »): the Drop at −9.5 for −7.5, loss 2.97 (cap 3).
        assertEquals(-9.507, TargetReach.short(-7.5, -12.389, -9.507, 2.971)!!, 1e-9)
    }

    @Test
    fun aTargetMetOrMissedWithoutTheCapSaysNothing() {
        assertNull(TargetReach.short(-14.0, -14.2, null, 1.0))      // enorme, Spotify
        assertNull(TargetReach.short(-9.0, -9.1, null, 4.2))        // brouillon-jumpup, Club
        assertNull(TargetReach.short(-9.0, -12.0, null, 1.5))       // under, but the chain did not push it to its cap
        assertNull(TargetReach.short(null, -10.6, null, 6.1))       // no target
        assertNull(TargetReach.short(-9.0, null, null, 6.1))        // silence
    }
}
