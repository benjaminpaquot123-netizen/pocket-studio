package com.pocketstudio.app.midi

import com.pocketstudio.model.midi.Controllers
import com.pocketstudio.model.midi.DeviceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit stage 2 (AUDIT §4, carte des fonctions, MiniLab line; Benjamin 2026-11-03): each known
 * profile ([Controllers.PROFILES]) is recognised under the names phones give it, an unknown keyboard
 * plays as a generic one, and the name the app shows is always the real one (the model's, else the
 * device's own; never « Clavier MIDI » for a keyboard that has a name).
 */
class ControllerNamesTest {
    /** How Android names them (USB descriptors, as seen in the field), for each profile id. */
    private val seen = mapOf(
        "minilab3" to listOf("Minilab3 MIDI", "Arturia MiniLab 3"),
        "minilab2" to listOf("Arturia MiniLab mkII", "MiniLab mkII"),
        "mpkmini" to listOf("MPK mini 3", "MPK Mini Mk II", "Akai MPK mini"),
        "launchkeymini" to listOf("Launchkey Mini MK3 MIDI", "Launchkey Mini"),
        "launchkey" to listOf("Launchkey 49 MK3", "Launchkey MK2 25"),
        "nanokontrol2" to listOf("nanoKONTROL2", "nanoKONTROL2 SLIDER/KNOB"),
        "nanokontrol" to listOf("nanoKONTROL", "nanoKONTROL Studio"),
        "lpd8" to listOf("LPD8", "LPD8 mk2"),
        "vmini" to listOf("V-Mini", "Alesis V-Mini"),
    )

    @Test
    fun everyKnownProfileIsRecognisedUnderItsRealNames() {
        assertEquals("a profile without names seen in the field", Controllers.PROFILES.map { it.id }.toSet(), seen.keys)
        seen.forEach { (id, names) ->
            names.forEach { n -> assertEquals("« $n »", id, Controllers.detect(DeviceId(n))?.id) }
            // Its own name in the app's list finds it too.
            assertEquals(id, Controllers.detect(DeviceId(Controllers.byId(id)!!.name))?.id)
        }
        // The MiniLab 3 by its USB ids, whatever the phone calls it.
        assertEquals("minilab3", Controllers.detect(DeviceId("USB-Audio", vendorId = Controllers.ARTURIA, productId = Controllers.MINILAB3_USB))?.id)
    }

    @Test
    fun anUnknownKeyboardIsGenericAndShownByItsOwnName() {
        listOf("Roland A-49", "Keystation 49 MK3", "Clavier de Benjamin", "Arturia KeyStep").forEach { n ->
            assertNull("« $n » is generic", Controllers.detect(DeviceId(n)))
            assertEquals("$n connecté — prêt à jouer", MidiViewModel.greeting(null, n))
        }
        // No name from the phone: the plain words.
        assertEquals("Clavier MIDI connecté — prêt à jouer", MidiViewModel.greeting(null, null))
        assertEquals("Clavier MIDI connecté — prêt à jouer", MidiViewModel.greeting(null, "  "))
    }

    @Test
    fun aKnownControllerIsGreetedByItsModelsName() {
        Controllers.PROFILES.forEach { p ->
            val shown = MidiViewModel.greeting(p, "anything")
            assertTrue("« $shown » names ${p.name}", shown.startsWith(p.name.removePrefix("Arturia ")))
        }
        assertEquals("MiniLab 3 connecté — prêt à jouer", MidiViewModel.greeting(Controllers.MINILAB3, "Minilab3 MIDI"))
    }
}
