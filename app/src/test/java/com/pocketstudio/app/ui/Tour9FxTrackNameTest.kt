package com.pocketstudio.app.ui

import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F136 on the phone: « Les effets rapides proposés nomment la piste ouverte. » The effects opened from the
 * Synthé / Acide cards and from the Rythme pads (AppRoot) get the strip's own name, not the sound's family.
 */
class Tour9FxTrackNameTest {
    @After
    fun french() { I18n.lang = Lang.FR }

    private val project = Project(tracks = listOf(InstrumentTrack(0, "Basse"), InstrumentTrack(1, "Rhodes")))

    @Test
    fun f136_theEffectsOfATrackSayItsName() {
        I18n.lang = Lang.FR
        assertEquals("Rhodes", fxTrackName(project, InstrumentTrack.mixerChannel(1)))
        assertEquals("Basse", fxTrackName(project, InstrumentTrack.mixerChannel(0)))
    }

    @Test
    fun f136_theEffectsOfAPadSayItsSound() {
        I18n.lang = Lang.FR
        val name = fxTrackName(project, Mixer.FIRST_PAD)
        assertTrue("a pad's effects say its sound, got « $name »", !name.isNullOrBlank())
    }

    @Test
    fun f136_theAcidStripSaysItsName() {
        I18n.lang = Lang.FR
        assertTrue(!fxTrackName(project, 1).isNullOrBlank())
    }
}
