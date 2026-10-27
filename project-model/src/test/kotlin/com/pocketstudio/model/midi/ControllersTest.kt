package com.pocketstudio.model.midi

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllersTest {
    private val none = KnobSettings()

    @Test
    fun theMiniLab3IsFoundByItsUsbIdsOrItsName() {
        // As Android may name it: product only, maker + product, or nothing but the USB ids.
        listOf("Minilab3", "Minilab3 MIDI", "Arturia MiniLab 3", "ARTURIA MINILAB-3 MIDI").forEach {
            assertEquals(it, "minilab3", Controllers.detect(DeviceId(it))?.id)
        }
        assertEquals("minilab3", Controllers.detect(DeviceId("USB MIDI", vendorId = 0x1C75, productId = 0x220B))?.id)
        assertEquals("minilab3", Controllers.detect(DeviceId("Port 1", manufacturer = "Arturia", product = "Minilab3"))?.id)
        // The others the app knows, and a keyboard it does not.
        assertEquals("mpkmini", Controllers.detect("MPK mini 3")?.id)
        assertEquals("mpkmini", Controllers.detect("MPK Mini IV")?.id)
        assertEquals("launchkeymini", Controllers.detect("Launchkey Mini MK3 MIDI")?.id)
        assertEquals("launchkeymini", Controllers.detect("Launchkey Mini MK4 37")?.id)
        assertEquals("nanokontrol2", Controllers.detect("nanoKONTROL2")?.id)
        assertEquals("minilab2", Controllers.detect("Arturia MiniLab mkII")?.id)
        assertNull(Controllers.detect("Keystation Mini 32 MK3"))
        assertNull(Controllers.detect(DeviceId("USB MIDI", vendorId = 0x1C75, productId = 0x0001)))
    }

    @Test
    fun miniLab3ControlsInBothItsPrograms() {
        val ml = Controllers.MINILAB3
        fun of(cc: Int, channel: Int = 0) = Controllers.controlOf(cc, channel, "Minilab3", ml, none)?.first
        // Arturia program: absolute knobs, faders 82 83 85 17.
        assertEquals(Control.Knob(0, KnobMode.ABSOLUTE), of(74))
        assertEquals(Control.Knob(7, KnobMode.ABSOLUTE), of(16))
        assertEquals(Control.Fader(0), of(82))
        assertEquals(Control.Fader(3), of(17))
        // DAW program: relative knobs 86.., faders 14 15 30 31.
        assertEquals(Control.Knob(0, KnobMode.RELATIVE_OFFSET), of(86))
        assertEquals(Control.Knob(7, KnobMode.RELATIVE_OFFSET), of(117))
        assertEquals(Control.Fader(2), of(30))
        // Main encoder, its click, with Shift.
        assertEquals(ControlButton.ENCODER, (of(28) as Control.Button).button)
        assertEquals(ControlButton.ENCODER_SHIFT, (of(29) as Control.Button).button)
        assertEquals(ControlButton.CLICK, (of(118) as Control.Button).button)
        assertEquals(ControlButton.SHIFT, (of(27) as Control.Button).button)
        // Mod strip, sustain, anything else: not the controller's (the engine plays them).
        assertNull(of(1))
        assertNull(of(64))
        assertNull(of(50))
    }

    @Test
    fun padsOfAProfileOfTheGenericPathAndLearnt() {
        val ml = Controllers.MINILAB3
        // Bank A 36..43, bank B 44..51, channel 10.
        assertEquals(0, Controllers.padOf(9, 36, ml, null))
        assertEquals(8, Controllers.padOf(9, 44, ml, null))
        assertEquals(15, Controllers.padOf(9, 51, ml, null))
        assertNull(Controllers.padOf(0, 36, ml, null))  // a key on channel 1
        // Generic: channel 10 notes 36..51.
        assertEquals(3, Controllers.padOf(9, 39, null, null))
        assertNull(Controllers.padOf(9, 60, null, null))
        assertNull(Controllers.padOf(0, 39, null, null))
        // Learnt (« Apprendre mes pads »): pads on channel 1 notes 60, 62, 64; nothing else is a pad.
        val learnt = listOf(Controllers.packPad(0, 60), Controllers.packPad(0, 62), Controllers.packPad(0, 64))
        assertEquals(1, Controllers.padOf(0, 62, null, learnt))
        assertNull(Controllers.padOf(9, 36, null, learnt))
        assertNull(Controllers.padOf(1, 60, null, learnt))
    }

    @Test
    fun aGenericControllerTakesItsKnobsInTheOrderTurnedAndLearntControlsWin() {
        var s = none
        fun turn(cc: Int): Control? = Controllers.controlOf(cc, 0, "Mystère", null, s)?.also { s = it.second }?.first
        assertEquals(Control.Knob(0, KnobMode.ABSOLUTE), turn(50))
        assertEquals(Control.Knob(1, KnobMode.ABSOLUTE), turn(52))
        assertEquals(Control.Knob(0, KnobMode.ABSOLUTE), turn(50))
        // A common block: knob 3 of 21..28.
        assertEquals(Control.Knob(2, KnobMode.ABSOLUTE), turn(23))
        // The mod wheel and the pedals are never knobs.
        assertNull(turn(1))
        assertNull(turn(64))
        // « Apprendre »: CC 52 is fader 1 now (it leaves knob 2), CC 99 the main encoder.
        s = s.withFader("Mystère", 0, 52).withButton("Mystère", ControlButton.ENCODER, 99)
        assertEquals(Control.Fader(0), turn(52))
        assertEquals(ControlButton.ENCODER, (turn(99) as Control.Button).button)
        assertNull(s.learnt("Mystère", 1))
        assertTrue(s.customized("Mystère"))
        // Forgotten: back to the app's choices.
        s = s.forget("Mystère")
        assertFalse(s.customized("Mystère"))
        // A learnt button frees the profile's: CC 118 is no click of this MiniLab any more.
        val mine = none.withButton("Minilab3", ControlButton.CLICK, 20)
        assertEquals(ControlButton.CLICK, (Controllers.controlOf(20, 0, "Minilab3", Controllers.MINILAB3, mine)!!.first as Control.Button).button)
        assertNull(Controllers.controlOf(118, 0, "Minilab3", Controllers.MINILAB3, mine))
    }

    @Test
    fun relativeKnobsSendSteps() {
        assertEquals(1, KnobMode.RELATIVE_OFFSET.delta(65))
        assertEquals(-3, KnobMode.RELATIVE_OFFSET.delta(61))
        assertEquals(0, KnobMode.RELATIVE_OFFSET.delta(64))
        assertEquals(2, KnobMode.RELATIVE_TWOS.delta(2))
        assertEquals(-1, KnobMode.RELATIVE_TWOS.delta(127))
        assertEquals(-1, KnobMode.RELATIVE_SIGNED.delta(65))
        assertNull(KnobMode.ABSOLUTE.delta(65))
    }

    @Test
    fun theGuessTellsEndlessKnobsFromAbsoluteOnes() {
        // An endless knob turned up then down (binary offset): the same step again and again.
        val g = KnobModeGuess()
        val offset = listOf(65, 65, 66, 65, 63, 63).map { g.feed(20, it) }
        assertEquals(KnobMode.RELATIVE_OFFSET, offset.last())
        // Two's complement, one way only.
        assertEquals(KnobMode.RELATIVE_TWOS, listOf(1, 1, 1, 2, 1, 1).map { g.feed(21, it) }.last())
        // An absolute knob slowly through the middle: never the same value twice in a row.
        assertEquals(KnobMode.ABSOLUTE, listOf(60, 61, 62, 63, 65, 66).map { g.feed(22, it) }.last())
        // Far from 64: absolute at once.
        assertEquals(KnobMode.ABSOLUTE, g.feed(23, 30))
    }

    @Test
    fun oldSettingsStillReadAndNewOnesAreKept() {
        val json = Json { ignoreUnknownKeys = true }
        // Saved before this lot: knobs and overrides only.
        val old = json.decodeFromString(KnobSettings.serializer(), """{"enabled":true,"devices":{"X":{"0":21}},"overrides":{}}""").sanitized()
        assertEquals(21, old.learnt("X", 0))
        assertTrue(old.legend)
        assertTrue(old.pads.isEmpty())
        val s = old.withPads("X", listOf(Controllers.packPad(0, 60))).withMode("X", KnobMode.RELATIVE_TWOS).withFader("X", 1, 40)
        val back = json.decodeFromString(KnobSettings.serializer(), json.encodeToString(KnobSettings.serializer(), s)).sanitized()
        assertEquals(s, back)
        assertEquals(KnobMode.RELATIVE_TWOS, back.modeOf("X"))
        // Damaged values are dropped, not crashed on.
        val bad = KnobSettings(faders = mapOf("X" to mapOf(99 to 3)), buttons = mapOf("X" to mapOf("NOPE" to 3)), modes = mapOf("X" to "?")).sanitized()
        assertTrue(bad.faders.isEmpty() && bad.buttons.isEmpty() && bad.modes.isEmpty())
    }
}
