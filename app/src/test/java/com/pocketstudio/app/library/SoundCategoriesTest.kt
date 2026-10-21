package com.pocketstudio.app.library

import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundCategoriesTest {
    private val afro = StarterWizard.style("afro_house")!!

    private fun packFor(style: StarterWizard.Style) =
        PackManifest(style.id, "Pack", sounds = listOf(PackSound("hat.wav", "hat", "Hat"), PackSound("perc.wav", "perc", "Perc"), PackSound("kick.wav", "kick", "Kick")))

    @Test
    fun aLineThatAsksForAKitVoiceKeepsItWithThePackDownloaded() {
        val installed = listOf(packFor(afro))
        afro.lines.filter { it.voice != null }.forEach { line ->
            val first = soundCategories(line.role, afro.id, installed).first()
            assertEquals(line.role.toString(), KIT_CATEGORY, first.id)
            assertEquals(line.voice, (first.options.first().ref as SoundRef.Kit).voice)
        }
    }

    @Test
    fun aLineWithoutAKitVoiceStillTakesThePackFirst() {
        val installed = listOf(packFor(afro))
        val kick = afro.lines.first { it.voice == null && it.role.id == "kick" }
        assertEquals("pack:" + afro.id, soundCategories(kick.role, afro.id, installed).first().id)
    }
}
