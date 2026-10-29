package com.pocketstudio.app.onboarding

import android.media.AudioManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The opening's timeline and its felt heartbeat (owner, 2026-10-29: no sound, two taps of the
 * vibrator; formerly a sound file made by
 * scripts/brand/render_heartbeat.py): the length asked for, the hand-over from the splash
 * without a jump, the two drawn beats on the sound's two beats to the frame, a clean, mastered
 * file, and when the phone hears it, feels it, or neither.
 */
class IntroSoundTest {

    @Test
    fun atMostOneSecondLongerThanBefore() {
        // Owner (2026-10-29): « légèrement plus longue », « 1 seconde maximum de plus ».
        val before = 3800f
        assertEquals(4700f, IntroTimeline.TOTAL)
        assertTrue(IntroTimeline.TOTAL > before && IntroTimeline.TOTAL - before <= 1000f)
        assertTrue(IntroTimeline.FADE_START < IntroTimeline.TOTAL)
    }

    @Test
    fun theSplashHandsOverWithoutAJump() {
        // Everything the splash cannot draw starts after it has gone: the beats, the light, the relief.
        val handOver = IntroTimeline.SPLASH_MS.toFloat()
        for (t in listOf(0f, 300f, 700f, handOver)) {
            assertEquals(0f, IntroTimeline.relief(t))
            assertEquals(0f, IntroTimeline.ambient(t))
            assertEquals(0f, IntroTimeline.beatGlow(t))
            assertEquals(-1f, IntroTimeline.sheen(t))
            for (k in 0 until IntroTimeline.BEAT_COUNT) {
                assertEquals(0f, IntroTimeline.ringProgress(k, t))
                assertEquals(0f, IntroTimeline.shockProgress(k, t))
                for (j in 0 until IntroTimeline.Sparks.count(k)) assertEquals(0f, IntroTimeline.Sparks.progress(k, j, t))
            }
            for (i in 0 until BrandMark.PADS) assertEquals(0f, IntroTimeline.flash(i, t))
        }
        // What the splash does draw (the pads in, the breath in) goes on smoothly from there.
        assertEquals(1f, IntroTimeline.pop(3, handOver))
        val s = IntroTimeline.heartbeat(handOver)
        assertTrue("breathing in: $s", s < 1f && s > 1f - IntroTimeline.INHALE_DEPTH)
        assertTrue(abs(IntroTimeline.heartbeat(handOver + 8f) - s) < 0.003f)
        assertEquals(1f, IntroTimeline.heartbeat(IntroTimeline.INHALE_START), 1e-6f)
    }

    @Test
    fun theTwoDrawnBeatsAreTheSoundsTwoBeats() {
        val beats = IntArray(IntroTimeline.BEAT_COUNT) { IntroTimeline.beatFrame(it) }
        assertArrayEquals(intArrayOf(4800, 16320), beats)
        // And the drawing beats there: the mark swells from that frame on, the pads light up.
        for (k in 0 until IntroTimeline.BEAT_COUNT) {
            val at = IntroTimeline.beatStart(k)
            assertEquals(at, IntroTimeline.SOUND_START + IntroTimeline.beatFrame(k) / 48f)
            assertTrue(IntroTimeline.heartbeat(at + IntroTimeline.BEAT_UP) > IntroTimeline.heartbeat(at) + 0.05f)
            assertTrue(IntroTimeline.flash(0, at + IntroTimeline.FLASH_UP) > 0.3f)
            assertEquals(0f, IntroTimeline.ringProgress(k, at))
            assertTrue(IntroTimeline.ringProgress(k, at + 1f) > 0f)
        }
        assertEquals(0f, IntroTimeline.flash(0, IntroTimeline.beatStart(0) - 1f))
        // The first beat is the stronger, as « lub » before « dub ».
        val lub = IntroTimeline.heartbeat(IntroTimeline.beatStart(0) + IntroTimeline.BEAT_UP)
        val dub = IntroTimeline.heartbeat(IntroTimeline.beatStart(1) + IntroTimeline.BEAT_UP)
        assertTrue("$lub > $dub", lub > dub)
        // The spring settles: still again long before the name is there.
        assertEquals(1f, IntroTimeline.heartbeat(IntroTimeline.BREATH_START), 0.002f)
    }

    @Test
    fun theSoundStartsAtTheFrameThatPlaysNow() {
        assertTrue(IntroTimeline.SOUND_START >= IntroTimeline.SPLASH_MS)
        assertTrue(IntroTimeline.SOUND_LATEST < IntroTimeline.beatStart(0))
        assertEquals(0, IntroTimeline.soundFrame(IntroTimeline.SOUND_START))
        assertEquals(480, IntroTimeline.soundFrame(IntroTimeline.SOUND_START + 10f))
        assertEquals(IntroTimeline.beatFrame(0) - 48, IntroTimeline.soundFrame(IntroTimeline.beatStart(0) - 1f))
    }

    @Test
    fun thePhoneDecidesWhetherItIsHeardOrFelt() {
        val normal = AudioManager.RINGER_MODE_NORMAL
        val vibrate = AudioManager.RINGER_MODE_VIBRATE
        val silent = AudioManager.RINGER_MODE_SILENT
        fun mode(reduced: Boolean = false, on: Boolean = true, haptics: Boolean = true, ringer: Int = normal) = introCueMode(reduced, on, haptics, ringer)
        // Owner (2026-10-29): the heartbeat is felt, never heard: two taps, whatever the media volume.
        assertEquals(IntroCueMode.Taps, mode())
        assertEquals(IntroCueMode.Taps, mode(ringer = vibrate))
        // Silent phone, the animations off, « Battement à l'ouverture » or the vibrations off in Réglages: nothing.
        assertEquals(IntroCueMode.Quiet, mode(ringer = silent))
        assertEquals(IntroCueMode.Quiet, mode(reduced = true))
        assertEquals(IntroCueMode.Quiet, mode(on = false))
        assertEquals(IntroCueMode.Quiet, mode(haptics = false))
    }

    @Test
    fun theSparksAreLaidOutOnceAroundTheMark() {
        for (k in 0 until IntroTimeline.BEAT_COUNT) {
            var sx = 0f
            for (j in 0 until IntroTimeline.Sparks.count(k)) {
                val d = IntroTimeline.Sparks.dx(k, j) * IntroTimeline.Sparks.dx(k, j) + IntroTimeline.Sparks.dy(k, j) * IntroTimeline.Sparks.dy(k, j)
                assertEquals(1f, d, 1e-4f)
                sx += IntroTimeline.Sparks.dx(k, j)
                // Gone once the mark has risen.
                assertEquals(1f, IntroTimeline.Sparks.progress(k, j, IntroTimeline.RISE_END))
            }
            // All around, not to one side.
            assertTrue(abs(sx) < IntroTimeline.Sparks.count(k) * 0.3f)
        }
        assertTrue(IntroTimeline.Sparks.count(1) < IntroTimeline.Sparks.count(0))
    }
}
