package com.pocketstudio.app.audio

import com.pocketstudio.app.audiotracks.clipBarMarks
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePadTake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Phase 35: punch-in, the pads of a Live take as one sound, the bar lines over a take. */
class TakeAudioTest {
    private fun ones(frames: Int, v: Float = 1f) = Pcm(FloatArray(frames) { v }, 1, 1000)

    @Test
    fun aPunchReplacesOnlyItsStretchWithCrossfades() {
        // Old at song frame 100, 1000 frames of 1; new from frame 0, 2000 frames of -1; punch 400..700.
        val out = AudioEdits.punch(ones(1000), ones(2000, -1f), oldAt = 100, newAt = 0, from = 400, until = 700, fadeFrames = 20)
        assertEquals(1000, out.frames)
        assertEquals(1f, out.data[100], 0f)  // song frame 200: the old take
        assertEquals(-1f, out.data[450], 0f)  // song frame 550: the new one
        assertEquals(1f, out.data[700], 0f)  // after the stretch: old again
        // Across an edge the sound slides from one to the other, never a jump.
        val edge = out.data.slice(285..315)
        assertTrue(edge.zipWithNext().all { (a, b) -> abs(a - b) < 0.2f })
    }

    @Test
    fun aPunchPastTheOldTakesEndMakesItLonger() {
        val out = AudioEdits.punch(ones(100), ones(500, 0.5f), oldAt = 0, newAt = 0, from = 50, until = 300, fadeFrames = 10)
        assertTrue(out.frames >= 300)
        assertEquals(0.5f, out.data[200], 0f)
    }

    @Test
    fun padsBounceLoopsEachPassAndPlaysAOneShotOnce() {
        val clips = listOf(LiveClip("a.wav", "Batterie", beats = 1), LiveClip("b.wav", "Impact", beats = 1, oneShot = true, gain = 0.5f))
        // 120 BPM at 1000 frames a second: a sixteenth is 125 frames, a beat 500.
        val loop = Pcm(FloatArray(800) { if (it < 10) 1f else 0f }, 1, 1000)
        val shot = Pcm(FloatArray(100) { 1f }, 1, 1000)
        val spans = listOf(LivePadTake.Span(0, 0.0, 8.0), LivePadTake.Span(1, 4.0, Double.POSITIVE_INFINITY, oneShot = true))
        val out = PadBounce.render(spans, clips, mapOf(0 to loop, 1 to shot), start = 0, end = 16, bpm = 120.0, rate = 1000)!!
        assertEquals(2000, out.frames)
        // The loop starts again on each beat (0, 500), cut at the span's end (two beats).
        assertEquals(1f, out.data[0], 0f)
        assertEquals(1f + 0.5f, out.data[505], 1e-6f)  // the second pass and the one-shot (from step 4 = frame 500)
        assertEquals(0f, out.data[1000], 0f)  // the loop stopped at step 8
        assertNull(PadBounce.render(emptyList(), clips, emptyMap(), 0, 16, 120.0, 1000))
    }

    @Test
    fun aPadStartedSoftlyOnAControllerIsBouncedAsSoftAsItPlayed() {
        val clips = listOf(LiveClip("a.wav", "Batterie", beats = 1))
        val loop = Pcm(FloatArray(800) { if (it < 10) 1f else 0f }, 1, 1000)
        val out = PadBounce.render(listOf(LivePadTake.Span(0, 0.0, 8.0, velocity = 0.25f)), clips, mapOf(0 to loop), start = 0, end = 16, bpm = 120.0, rate = 1000)!!
        assertEquals(0.25f, out.data[0], 1e-6f)
        assertEquals(0.25f, out.data[500], 1e-6f)
    }

    @Test
    fun barLinesFallWhereTheBarsAreInTheTake() {
        // A song clip from step 24 (bar 2, half way), 8 s at 120 BPM: 64 sixteenths, 4 bars.
        val marks = clipBarMarks(AudioClip("v.wav", "Prise", seconds = 8.0, start = 24, song = true), 120.0, 16)
        assertEquals(listOf(3, 4, 5, 6), marks.map { it.second })
        assertEquals(8f / 64f, marks.first().first, 1e-6f)
    }
}
