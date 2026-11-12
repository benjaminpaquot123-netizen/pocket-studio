package com.pocketstudio.app.library

import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.SoundTrial
import com.pocketstudio.model.StarterWizard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 5 « sons-essai »: with the hidden trial switch on, each trial line of jump up, tech house and techno takes its trial
 * pack first, over the machine packs (tr909) and the style's own pack the recipe names; off, those stay first.
 */
class SoundTrialPacksTest {
    @After
    fun off() {
        System.clearProperty(SoundTrial.PROPERTY)
    }

    private val kinds = listOf("kick", "snare", "clap", "hat", "open_hat", "perc", "ride")

    private fun pack(id: String) = PackManifest(id, id, sounds = kinds.map { PackSound("${it}_01.wav", it, it) })

    /** Every pack a phone could hold for the three styles: their own, the machines, the trial ones. */
    private val installed = listOf("jump_up", "dnb", "tech_house", "techno", "tr909", "tr707") .map(::pack) +
        SoundTrial.STYLES.map { pack(SoundTrial.packOf(it)) }

    @Test
    fun withTheSwitchEachTrialLineTakesItsTrialPack() {
        System.setProperty(SoundTrial.PROPERTY, "1")
        for (id in SoundTrial.STYLES) {
            val style = StarterWizard.style(id)!!
            for (role in style.kit.keys) assertEquals("$id $role", "pack:essai_$id", soundCategories(role, id, installed).first().id)
        }
    }

    @Test
    fun withoutTheSwitchTheRecipesPacksStayFirst() {
        assertEquals("pack:tr909", soundCategories(StarterWizard.Role.OpenHat, "techno", installed).first().id)
        assertEquals("pack:dnb", soundCategories(StarterWizard.Role.Kick, "jump_up", installed).first().id)
        assertEquals("pack:tech_house", soundCategories(StarterWizard.Role.Kick, "tech_house", installed).first().id)
        // Tech house's percussion names its kit voice: the kit first.
        assertEquals(KIT_CATEGORY, soundCategories(StarterWizard.Role.Perc, "tech_house", installed).first().id)
    }
}
