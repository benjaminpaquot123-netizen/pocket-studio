package com.pocketstudio.app

import com.pocketstudio.app.audio.Pcm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PcmTailTest {
    private val rate = 48000

    @Test
    fun aOneShotLosesItsSilentTailButKeepsTenMilliseconds() {
        // 0.1 s of sound, then 0.9 s of silence.
        val data = FloatArray(rate) { i -> if (i < rate / 10) 0.5f else 0f }
        val cut = Pcm(data, 1, rate).withoutTail()
        assertEquals(rate / 10 + rate / 100, cut.frames)
        assertEquals(0.5f, cut.data[rate / 10 - 1], 0f)
    }

    @Test
    fun aQuietEndOfTheSoundIsKept() {
        // A decay down to -50 dB stays: only what is under -60 dB of the peak goes.
        val data = FloatArray(rate) { i -> if (i == 0) 1f else if (i < rate / 2) 0.004f else 0f }
        assertEquals(rate / 2 + rate / 100, Pcm(data, 1, rate).withoutTail().frames)
    }

    @Test
    fun stereoKeepsAFrameWhereOnlyOneSideSounds() {
        val frames = rate / 2
        val data = FloatArray(frames * 2)
        data[0] = 0.8f
        data[(frames / 4) * 2 + 1] = 0.5f  // the right channel, a quarter of the way
        val cut = Pcm(data, 2, rate).withoutTail()
        assertEquals(frames / 4 + 1 + rate / 100, cut.frames)
        assertEquals(0.5f, cut.data[(frames / 4) * 2 + 1], 0f)
    }

    @Test
    fun silenceASingleFrameAndLongSoundsAreLeftAlone() {
        val silent = Pcm(FloatArray(rate), 1, rate)
        assertSame(silent, silent.withoutTail())
        val one = Pcm(floatArrayOf(0.3f), 1, rate)
        assertSame(one, one.withoutTail())
        // Longer than three seconds: a loop or a phrase, never cut.
        val long = Pcm(FloatArray(rate * 4) { i -> if (i < rate) 0.5f else 0f }, 1, rate)
        assertSame(long, long.withoutTail())
        // Nothing after the sound: the same buffer.
        val full = Pcm(FloatArray(rate) { 0.2f }, 1, rate)
        assertSame(full, full.withoutTail())
    }
}
