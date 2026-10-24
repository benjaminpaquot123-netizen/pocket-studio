package com.pocketstudio.model.midi

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoKnobsTest {
    private val none = KnobSettings()

    @Test
    fun knownControllersAndCommonBlocksGiveTheirKnobsInOrder() {
        // Arturia MiniLab 3: its own CCs.
        assertEquals(0, KnobLayout.slotOf(74, "Arturia MiniLab 3", none)?.first)
        assertEquals(5, KnobLayout.slotOf(18, "Arturia MiniLab 3 MIDI", none)?.first)
        assertEquals(7, KnobLayout.slotOf(16, "Minilab3", none)?.first)
        // Its other controls (the faders) are no knobs.
        assertNull(KnobLayout.slotOf(82, "Arturia MiniLab 3", none))
        // Akai MPK Mini: 70–77; Launchkey: 21–28.
        assertEquals(0, KnobLayout.slotOf(70, "MPK mini 3", none)?.first)
        assertEquals(7, KnobLayout.slotOf(77, "MPK mini 3", none)?.first)
        assertEquals(3, KnobLayout.slotOf(24, "Launchkey Mini MK3", none)?.first)
        // Nothing remembered for them: the profile or the block is enough.
        assertEquals(none, KnobLayout.slotOf(72, "MPK mini 3", none)?.second)
    }

    @Test
    fun anUnknownControllerGetsItsKnobsInTheOrderTheyAreTurned() {
        var s = none
        fun turn(cc: Int): Int? = KnobLayout.slotOf(cc, "Mystère", s)?.also { s = it.second }?.first
        assertEquals(0, turn(40))
        assertEquals(1, turn(13))
        assertEquals(0, turn(40))  // the same knob again
        assertNull(turn(1))  // the mod wheel is no knob
        assertNull(turn(64))  // nor the sustain pedal
        assertEquals(2, turn(41))
        assertEquals(mapOf(0 to 40, 1 to 13, 2 to 41), s.devices["Mystère"])
        listOf(50, 51, 52, 53, 54).forEach { turn(it) }
        assertNull("eight knobs at most", turn(55))
    }

    @Test
    fun aKnobLearntTakesItsPlace() {
        // « Apprendre » on knob 3: CC 90 becomes knob 3 of the MiniLab 3, whose CC 77 no longer is.
        val s = none.withKnob("Arturia MiniLab 3", 3, 90)
        assertEquals(3, KnobLayout.slotOf(90, "Arturia MiniLab 3", s)?.first)
        assertNull(KnobLayout.slotOf(77, "Arturia MiniLab 3", s))
        assertEquals(90, KnobLayout.ccOf(3, "Arturia MiniLab 3", s))
        assertEquals(74, KnobLayout.ccOf(0, "Arturia MiniLab 3", s))
        // Learnt again elsewhere: it leaves its old place.
        val moved = s.withKnob("Arturia MiniLab 3", 5, 90)
        assertEquals(mapOf(5 to 90), moved.devices["Arturia MiniLab 3"])
        assertEquals(none.copy(devices = emptyMap()), moved.reset("acid", "Arturia MiniLab 3"))
    }

    @Test
    fun theSettingsAreSavedAndReadBack() {
        val s = none.withKnob("MPK mini 3", 2, 30).withOverride("Acide", 4, 123456789L).copy(enabled = false)
        val json = Json { ignoreUnknownKeys = true }
        val back = json.decodeFromString<KnobSettings>(json.encodeToString(KnobSettings.serializer(), s)).sanitized()
        assertEquals(s, back)
        assertEquals(30, back.learnt("MPK mini 3", 2))
        // Removing an override leaves nothing behind.
        assertEquals(emptyMap<String, Map<Int, Long>>(), back.withOverride("Acide", 4, null).overrides)
        // A damaged file: knobs out of range are dropped.
        val damaged = KnobSettings(devices = mapOf("x" to mapOf(9 to 3, 1 to 200, 2 to 5)), overrides = mapOf("y" to mapOf(12 to 1L)))
        assertEquals(KnobSettings(devices = mapOf("x" to mapOf(2 to 5))), damaged.sanitized())
    }

    @Test
    fun theKnobPicksUpTheSettingBeforeMovingIt() {
        val t = SoftTakeover()
        t.follow(1)
        // The setting is at 0.6; the knob starts at 0.1 and goes up: nothing moves until it gets there.
        assertNull(t.move(0.10f, 0.6f))
        assertEquals(1, t.direction(0.6f))
        assertNull(t.move(0.40f, 0.6f))
        assertFalse(t.pickedUp)
        assertEquals(0.62f, t.move(0.62f, 0.6f)!!, 0f)  // crossed it
        assertTrue(t.pickedUp)
        assertEquals(0.7f, t.move(0.7f, 0.62f)!!, 0f)
        // A jump of the knob after pickup goes through.
        assertEquals(0.2f, t.move(0.2f, 0.7f)!!, 0f)
        // Moved on screen meanwhile: the knob waits for it again.
        assertNull(t.move(0.25f, 0.9f))
        assertEquals(1, t.direction(0.9f))
        assertEquals(0.9f, t.move(0.9f, 0.9f)!!, 0f)  // reached it
    }

    @Test
    fun aKnobCloseToTheSettingTakesItAtOnceAndANewSettingWaitsAgain() {
        val t = SoftTakeover()
        t.follow(7)
        assertEquals(0.505f, t.move(0.505f, 0.5f)!!, 0f)
        t.follow(7)  // the same setting: still picked
        assertEquals(0.51f, t.move(0.51f, 0.505f)!!, 0f)
        t.follow(8)  // another screen
        assertNull(t.move(0.9f, 0.1f))
        assertEquals(-1, t.direction(0.1f))
        // Coming down past it picks up.
        assertEquals(0.05f, t.move(0.05f, 0.1f)!!, 0f)
    }
}
