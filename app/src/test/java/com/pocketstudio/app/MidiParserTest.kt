package com.pocketstudio.app

import com.pocketstudio.app.midi.ClockTempo
import com.pocketstudio.app.midi.MidiListener
import com.pocketstudio.app.midi.MidiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MidiParserTest {
    private val events = mutableListOf<String>()
    private val parser = MidiParser(object : MidiListener {
        override fun noteOn(channel: Int, note: Int, velocity: Int) { events += "on $channel $note $velocity" }
        override fun noteOff(channel: Int, note: Int) { events += "off $channel $note" }
        override fun controlChange(channel: Int, controller: Int, value: Int) { events += "cc $channel $controller $value" }
        override fun clock() { events += "clock" }
        override fun start() { events += "start" }
        override fun stop() { events += "stop" }
    })

    private fun feed(vararg b: Int) = parser.feed(ByteArray(b.size) { b[it].toByte() })

    @Test
    fun notesWithRunningStatusAndZeroVelocity() {
        feed(0x90, 60, 100, 64, 90, 60, 0)  // running status; velocity 0 = off
        feed(0x89, 36, 64)                  // channel 10 note-off
        assertEquals(listOf("on 0 60 100", "on 0 64 90", "off 0 60", "off 9 36"), events)
    }

    @Test
    fun realTimeInsideAMessageAndSysexSkipped() {
        feed(0x90, 60, 0xF8, 100)                      // a clock between the two data bytes
        feed(0xF0, 0x7E, 0x01, 0x90, 0xF7)              // SysEx: never read as a note
        feed(0xB1, 74, 127, 0xFA, 0xFC)
        feed(0x91, 50)                                  // split across two packets
        feed(70)
        assertEquals(listOf("clock", "on 0 60 100", "cc 1 74 127", "start", "stop", "on 1 50 70"), events)
    }

    @Test
    fun programChangeTakesOneDataByte() {
        feed(0xC0, 5, 0x90, 60, 100)
        assertEquals(listOf("on 0 60 100"), events)
    }

    @Test
    fun clockGivesTheTempo() {
        val clock = ClockTempo()
        val tick = 60e9 / 120.0 / 24  // 120 BPM
        for (i in 0 until 24) assertNull(clock.tick((i * tick).toLong()))
        assertEquals(120.0, clock.tick((24 * tick).toLong())!!, 0.01)
        assertEquals(120.0, clock.tick((25 * tick).toLong())!!, 0.01)
    }
}
