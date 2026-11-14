package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** « Annuler » of a Live take: the take goes, what was set meanwhile and is no part of it stays. */
class TakeUndoTargetTest {
    @Test
    fun theSongGoesBackTheTempoAndMixerStayAndTheGesturesGoBack() {
        val before = Project(tempoBpm = 120.0, swing = 0.0).sanitized()
        val song = listOf(SongPart(scene = 0, bars = 4))
        val vol = AutoTarget(AutoTarget.VOLUME, 1)
        val macro = AutoTarget.macro(0, 1)
        val after = before.copy(song = song, tempoBpm = 128.0, swing = 0.2, liveTake = "t")
            .withSetting(vol, 0.3f).withSetting(macro, 0.9f)
        val back = after.takeUndoTarget(before, setOf(macro))
        assertEquals(before.song, back.song)
        assertEquals("", back.liveTake)
        assertEquals(128.0, back.tempoBpm, 1e-9)
        assertEquals(0.2, back.swing, 1e-9)
        assertEquals(0.3f, back.settingOf(vol), 1e-6f)  // turned by hand during the take: kept
        assertEquals(before.settingOf(macro), back.settingOf(macro), 1e-6f)  // the take's gesture: back
    }
}
