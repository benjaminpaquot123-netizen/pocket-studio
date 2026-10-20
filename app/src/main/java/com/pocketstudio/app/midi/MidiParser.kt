package com.pocketstudio.app.midi

/** What a MIDI byte stream asks for. Channels are 0..15 (channel 10 of the drums is 9). */
interface MidiListener {
    fun noteOn(channel: Int, note: Int, velocity: Int) {}
    fun noteOff(channel: Int, note: Int) {}
    fun controlChange(channel: Int, controller: Int, value: Int) {}
    /** -8192..8191 (0 = centre). */
    fun pitchBend(channel: Int, value: Int) {}
    fun clock() {}
    fun start() {}
    fun continuePlayback() {}
    fun stop() {}
}

/**
 * Turns MIDI bytes into events: running status, note-on at velocity 0 as note-off, real-time
 * bytes anywhere (even inside another message), System Exclusive skipped. One instance per
 * input port; not thread-safe.
 */
class MidiParser(private val listener: MidiListener) {
    private var status = 0
    private val data = IntArray(2)
    private var count = 0
    private var inSysex = false

    fun feed(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        for (i in offset until offset + length) byte(bytes[i].toInt() and 0xFF)
    }

    private fun byte(b: Int) {
        if (b >= 0xF8) {  // real-time: never interrupts the message around it
            when (b) {
                0xF8 -> listener.clock()
                0xFA -> listener.start()
                0xFB -> listener.continuePlayback()
                0xFC -> listener.stop()
            }
            return
        }
        if (b >= 0x80) {
            when {
                b == 0xF0 -> { inSysex = true; status = 0 }
                b == 0xF7 -> inSysex = false
                b >= 0xF0 -> { status = 0; inSysex = false }  // other system common: ignored, cancels running status
                else -> { status = b; inSysex = false }
            }
            count = 0
            return
        }
        if (inSysex || status == 0) return
        data[count++] = b
        val needed = when (status and 0xF0) {
            0xC0, 0xD0 -> 1
            else -> 2
        }
        if (count < needed) return
        count = 0  // running status: the next data bytes reuse `status`
        val channel = status and 0x0F
        when (status and 0xF0) {
            0x90 -> if (data[1] == 0) listener.noteOff(channel, data[0]) else listener.noteOn(channel, data[0], data[1])
            0x80 -> listener.noteOff(channel, data[0])
            0xB0 -> listener.controlChange(channel, data[0], data[1])
            0xE0 -> listener.pitchBend(channel, (data[0] or (data[1] shl 7)) - 8192)
        }
    }
}

/** Tempo from MIDI clock: 24 ticks per quarter note, averaged over the last beat. */
class ClockTempo {
    private val times = LongArray(25)
    private var n = 0

    /** A tick at [nanos]; returns the tempo once a full beat was seen, else null. */
    fun tick(nanos: Long): Double? {
        if (n < times.size) {
            times[n++] = nanos
        } else {
            System.arraycopy(times, 1, times, 0, times.size - 1)
            times[times.size - 1] = nanos
        }
        if (n < times.size) return null
        val beat = times.last() - times.first()
        return if (beat > 0) 60e9 / beat else null
    }

    fun reset() { n = 0 }
}
