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
    fun sixToSevenSecondsFromTheIcon() {
        // Owner (2026-11-04): « 6 à 7 secondes » from the tap on the icon, hold and slow-down included.
        assertEquals(5000f, IntroTimeline.TOTAL)
        val fromIcon = IntroTimeline.SPLASH_MS + IntroTimeline.HOLD_MS + (IntroTimeline.TOTAL - IntroTimeline.SPLASH_MS) * IntroTimeline.SLOW
        assertTrue("$fromIcon", fromIcon in 6000f..7000f)
        assertTrue(IntroTimeline.FADE_START < IntroTimeline.TOTAL && IntroTimeline.TOTAL - IntroTimeline.FADE_START >= 240f)
    }

    @Test
    fun theSplashHandsOverWithoutAJump() {
        // Everything the splash cannot draw starts after it has gone: the beats, the light, the relief,
        // the shadows, the ring, the dust, the vignette, the rise, the fade.
        val handOver = IntroTimeline.SPLASH_MS.toFloat()
        for (t in listOf(0f, 300f, 700f, handOver)) {
            assertEquals(0f, IntroTimeline.relief(t))
            assertEquals(0f, IntroTimeline.ambient(t))
            assertEquals(0f, IntroTimeline.beatGlow(t))
            assertEquals(-1f, IntroTimeline.sheen(t))
            assertEquals(0f, IntroTimeline.ringAlpha(t))
            assertEquals(0f, IntroTimeline.ringRadius(t))
            for (j in 0 until IntroTimeline.Dust.COUNT) assertEquals(0f, IntroTimeline.Dust.alpha(j, t))
            assertEquals(0f, IntroTimeline.vignette(t))
            assertEquals(1f, IntroTimeline.shadowLift(t))
            assertEquals(0f, IntroTimeline.shadowAlpha(t))
            assertEquals(0f, IntroTimeline.rise(t))
            assertEquals(0f, IntroTimeline.fade(t))
            assertEquals(0f, IntroTimeline.punch(t))
            for (i in 0 until BrandMark.PADS) assertEquals(1f, IntroTimeline.press(i, t))
            for (k in 0 until IntroTimeline.BEAT_COUNT) assertEquals(0f, IntroTimeline.shockProgress(k, t))
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
        }
        assertEquals(0f, IntroTimeline.flash(0, IntroTimeline.beatStart(0) - 1f))
        // The first beat strikes the ring (bright at once), the second plucks it (a flare on the taut, faint ring).
        assertEquals(0f, IntroTimeline.ringAlpha(IntroTimeline.beatStart(0)))
        assertTrue(IntroTimeline.ringAlpha(IntroTimeline.beatStart(0) + 1f) > 0.5f)
        assertTrue("plucked", IntroTimeline.ringAlpha(IntroTimeline.beatStart(1) + 30f) > IntroTimeline.ringAlpha(IntroTimeline.beatStart(1) - 1f) + 0.2f)
        // The struck pad sinks (the app's own gesture), from the beat on, not before.
        assertTrue(IntroTimeline.press(0, IntroTimeline.beatStart(0) + IntroTimeline.PRESS_UP) < 0.97f)
        assertEquals(1f, IntroTimeline.press(0, IntroTimeline.beatStart(0) - 1f))
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
    fun theDustIsLaidOutOnceOverTheMark() {
        var sx = 0f
        val nameTopU = 108f * IntroTimeline.NAME_TOP   // the name's top, in icon units under the screen's centre
        for (j in 0 until IntroTimeline.Dust.COUNT) {
            val d = IntroTimeline.Dust.dx(j) * IntroTimeline.Dust.dx(j) + IntroTimeline.Dust.dy(j) * IntroTimeline.Dust.dy(j)
            assertEquals(1f, d, 1e-4f)
            assertTrue("upper arc", IntroTimeline.Dust.dy(j) <= 0.18f)          // sin(10°)
            sx += IntroTimeline.Dust.dx(j)
            assertTrue(IntroTimeline.Dust.radius(j, 0f) in 33f..50f)              // outside the pads, in the halo
            assertTrue(IntroTimeline.Dust.size(j) in 0.6f..1f)
            assertEquals(0f, IntroTimeline.Dust.alpha(j, IntroTimeline.beatStart(0)))
            assertTrue(IntroTimeline.Dust.alpha(j, IntroTimeline.beatStart(0) + IntroTimeline.DUST_IGNITE_MS) > 0f)
            for (t in listOf(1150f, 1380f, 2900f, 3980f)) assertTrue(IntroTimeline.Dust.alpha(j, t) <= IntroTimeline.DUST_MAX)
            // Never under the name once the mark has risen (the dust follows HALO_RISE of the lift, the name stays put).
            for (t in listOf(IntroTimeline.RISE_END, 2900f, IntroTimeline.DUST_DIM_START, IntroTimeline.FADE_START)) {
                val y = IntroTimeline.Dust.dy(j) * IntroTimeline.Dust.radius(j, t) - IntroTimeline.Dust.lift(t) -
                    IntroTimeline.HALO_RISE * IntroTimeline.LIFT * 108f * IntroTimeline.rise(t)
                assertTrue("grain $j at $t: $y", y < nameTopU - 4f)
            }
        }
        // Over the mark and to both sides, not to one side.
        assertTrue(abs(sx) < IntroTimeline.Dust.COUNT * 0.3f)
    }
}
