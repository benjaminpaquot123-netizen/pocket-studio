package com.pocketstudio.app.audio

import com.pocketstudio.app.rhythm.TakeVault
import com.pocketstudio.model.AudioClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** Final audit, voice lot: the punch-in keeps « Décaler la prise », the take's level, the anacrusis. */
class VoiceTakeTest {
    private val rate = 48_000

    @Test
    fun aPunchKeepsTheManualOffsetButNotWhatACutLeft() {
        val perStep = 15.0 / 120.0 * rate  // 6000 frames a sixteenth
        // « Décaler la prise » +50 ms: the new bars are 50 ms late too, so the joins fall in place (03 M2).
        val nudged = AudioClip("a.wav", "Prise", start = 16, song = true).withOffset(50f)
        assertEquals(16 * 6000L + 2400, AudioEdits.punchOldAt(nudged, perStep, rate))
        assertEquals(32 * 6000L + 2400, AudioEdits.punchNewAt(nudged, 32, perStep, rate))
        // A cut's rest only (« Garder la sélection »): the old sound moves, the new take does not.
        val cut = AudioClip("a.wav", "Prise", start = 16, song = true).afterCutStart(0.04, 120.0)
        assertEquals(40f, cut.offsetMs, 1e-3f)
        assertEquals(0f, cut.manualOffsetMs, 0f)
        assertEquals(16 * 6000L + 1920, AudioEdits.punchOldAt(cut, perStep, rate))
        assertEquals(32 * 6000L, AudioEdits.punchNewAt(cut, 32, perStep, rate))
        // Both: only the manual part follows the new take.
        val both = nudged.afterCutStart(0.04, 120.0)
        assertEquals(90f, both.offsetMs, 1e-3f)
        assertEquals(50f, both.manualOffsetMs, 1e-3f)
        assertEquals(32 * 6000L + 2400, AudioEdits.punchNewAt(both, 32, perStep, rate))
        // And the stretch then lands where it was sung: a new take of -1 replaces 1s exactly in its bars.
        val old = Pcm(FloatArray(64 * 6000) { 1f }, 1, rate)
        val new = Pcm(FloatArray(40 * 6000) { -1f }, 1, rate)
        val out = AudioEdits.punch(old, new, AudioEdits.punchOldAt(nudged, perStep, rate), AudioEdits.punchNewAt(nudged, 32, perStep, rate),
            from = 48 * 6000L, until = 56 * 6000L)
        val inside = (52 * 6000L - AudioEdits.punchOldAt(nudged, perStep, rate)).toInt()
        assertEquals(-1f, out.data[inside], 0f)
    }

    private fun voice(seconds: Double, level: Float, peakEvery: Int = 0): Pcm {
        val n = (seconds * rate).toInt()
        val data = FloatArray(n) { i -> level * sin(2 * PI * 220.0 * i / rate).toFloat() }
        if (peakEvery > 0) for (i in 0 until n step peakEvery) data[i] = 0.9f
        return Pcm(data, 1, rate)
    }

    @Test
    fun aQuietTakeIsBroughtUpAndALoudOneLeftAlone() {
        // A voice at about -27 dBFS RMS: brought up as far as the clip allows (×4, +12 dB).
        assertEquals(4f, AudioEdits.voiceLevelGain(voice(3.0, 0.064f), AudioClip.MAX_GAIN), 0f)
        // About -21 dBFS: up to about -14 dBFS.
        val g = AudioEdits.voiceLevelGain(voice(3.0, 0.125f), AudioClip.MAX_GAIN)
        assertTrue("gain $g", g in 2.1f..2.4f)
        // A hot take is never made quieter; silence stays at 1.
        assertEquals(1f, AudioEdits.voiceLevelGain(voice(3.0, 0.7f), AudioClip.MAX_GAIN), 0f)
        assertEquals(1f, AudioEdits.voiceLevelGain(Pcm(FloatArray(rate), 1, rate), AudioClip.MAX_GAIN), 0f)
        // Its loudest point stops it at +3 dBFS.
        val plosive = AudioEdits.voiceLevelGain(voice(3.0, 0.064f, peakEvery = rate / 2), AudioClip.MAX_GAIN)
        assertEquals(1.55f, plosive, 0.03f)
    }

    @Test
    fun theCountInIsKeptInFrontAndLeftOutOfTheTake() {
        // 1 s of pre-roll, 10 ms of latency: the take starts after both.
        val range = TakeVault.keptRange(rate * 3, latencyFrames = 480, cutFrames = 0, sampleRate = rate, prerollFrames = rate)
        assertEquals(rate + 480, range!!.first)
        // Stopped during the count-in: the pre-roll alone is not a take.
        assertNull(TakeVault.keptRange(rate, 480, 0, rate, prerollFrames = rate))
        assertNotNull(TakeVault.keptRange(rate + rate / 5, 0, 0, rate, prerollFrames = rate))
    }

    @Test
    fun aWordSungBeforeTheOneIsKeptInWholeEighths() {
        val eighth = 30.0 / 120.0 * rate  // 12000 frames
        val one = rate  // the « 1 » as heard
        val audio = FloatArray(rate * 3)
        // The take itself.
        for (i in one until audio.size) audio[i] = 0.5f * sin(2 * PI * 200.0 * i / rate).toFloat()
        val take = one until audio.size
        // Nothing before it (a click of the count-in half a second before does not count).
        for (i in one - 24000 until one - 23700) audio[i] = 0.8f
        assertEquals(0, TakeVault.leadInEighths(audio, one, 24000, eighth, take, rate))
        // « Yeah » sung 150 ms before the « 1 », into it: one eighth (250 ms) is kept.
        for (i in one - 7200 until one) audio[i] = 0.4f * sin(2 * PI * 180.0 * i / rate).toFloat()
        assertEquals(1, TakeVault.leadInEighths(audio, one, 24000, eighth, take, rate))
        // 300 ms before: two eighths, never more than the room allowed.
        for (i in one - 14400 until one) audio[i] = 0.4f * sin(2 * PI * 180.0 * i / rate).toFloat()
        assertEquals(2, TakeVault.leadInEighths(audio, one, 24000, eighth, take, rate))
        assertEquals(1, TakeVault.leadInEighths(audio, one, 12000, eighth, take, rate))
        // A consonant just ahead of the beat (20 ms) is not an anacrusis.
        val tight = FloatArray(rate * 3)
        for (i in one - 960 until tight.size) tight[i] = 0.5f * sin(2 * PI * 200.0 * i / rate).toFloat()
        assertEquals(0, TakeVault.leadInEighths(tight, one, 24000, eighth, take, rate))
    }
}
