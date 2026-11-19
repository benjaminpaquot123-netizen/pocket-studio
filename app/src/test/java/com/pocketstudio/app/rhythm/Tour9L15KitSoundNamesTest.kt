package com.pocketstudio.app.rhythm

import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.DrumLane
import com.pocketstudio.model.Project
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 9, lot L15, F132 (seat t9-39): « Les noms de sons d'un même kit suivent la même forme. » The pads of the
 * Drum & bass kit showed « Drum & bass · Caisse FX » next to « Claquement de mains FX »: a pad holding one of the kit's
 * own sounds now says it as the kit's other pads do, without the kit's name (the « Kit : … » chip says it).
 */
class Tour9L15KitSoundNamesTest {
    @After
    fun tidy() { I18n.everyday = false }

    private fun withSample(lane: Int, name: String): Project {
        val start = Project()
        val lanes = start.drums.lanes.toMutableList()
        lanes[lane] = (lanes.getOrNull(lane) ?: DrumLane()).copy(sample = PadSample(file = "x.wav", name = name))
        return start.copy(drums = start.drums.copy(lanes = lanes))
    }

    private fun sameForm(everyday: Boolean) {
        I18n.everyday = everyday
        DefaultNames.KIT_PADS.forEachIndexed { pad, word ->
            val name = DefaultNames.kitPad("Drum & bass", pad)
            assertEquals("pad screen, $name", tr(word), padName(withSample(pad, name), pad))
            assertEquals("pad name elsewhere, $name", tr(word), DefaultNames.shownPad(pad, name, null))
        }
    }

    @Test fun f132KitSoundsSayTheSameForm() = sameForm(everyday = false)
    @Test fun f132KitSoundsSayTheSameFormInEverydayWords() = sameForm(everyday = true)

    @Test
    fun f132OwnSoundsKeepTheirName() {
        assertEquals("Ma caisse · prise 2", padName(withSample(2, "Ma caisse · prise 2"), 2))
        assertEquals("Mon son", DefaultNames.shownPad(2, "Mon son", null))
    }
}
