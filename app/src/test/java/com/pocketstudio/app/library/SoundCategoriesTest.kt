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

    @Test
    fun aNamedSoundOfAnotherKindDoesNotCrashTheList() {
        // The style names, for its hat, a file of a pack that has no hat (a loop): the list still opens, kit and packs.
        val loops = PackManifest("loops", "Loops", sounds = listOf(PackSound("groove.wav", "loop", "Groove")))
        val hat = afro.lines.first { it.role.id == "hat" }.role
        val style = afro.copy(kitSounds = mapOf(hat to "loops:groove.wav"))
        val list = soundCategoriesOf(hat, style, listOf(loops, packFor(afro)))
        assertEquals(true, list.any { it.id == KIT_CATEGORY })
    }
}
