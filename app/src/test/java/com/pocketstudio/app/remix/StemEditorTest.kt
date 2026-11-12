package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.Pcm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** The stem board's edits (B2) on synthetic sounds: cuts without clicks, a voice's phrases, peaks. */
class StemEditorTest {
    private val rate = 48_000

    private fun f(seconds: Double) = Math.round(seconds * rate)

    /**
     * A sung voice on two channels: a 220 Hz tone with harmonics and syllables (the level moving
     * 4 times a second), sung during [phrases] (seconds), each with optional [gaps] of silence
     * inside it; under it all, the separation's bleed at about -66 dB.
     */
    private fun voice(seconds: Double, phrases: List<Pair<Double, Double>>, gaps: List<Pair<Double, Double>> = emptyList(), seed: Int = 1): Pcm {
        val n = f(seconds).toInt()
        val data = FloatArray(n * 2)
        val noise = Random(seed)
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val sung = phrases.any { t >= it.first && t < it.second } && gaps.none { t >= it.first && t < it.second }
            var x = (noise.nextDouble() - 0.5) * 0.002
            if (sung) {
                val syllables = 0.65 + 0.35 * sin(2 * PI * 4 * t)
                val tone = sin(2 * PI * 220 * t) + 0.5 * sin(2 * PI * 440 * t) + 0.25 * sin(2 * PI * 660 * t)
                x += 0.3 * syllables * tone
            }
            data[i * 2] = x.toFloat()
            data[i * 2 + 1] = (x * 0.8).toFloat()
        }
        return Pcm(data, 2, rate)
    }

    /** The largest step between two samples of one channel, the silence before and after included. */
    private fun largestStep(pcm: Pcm): Float {
        var worst = 0f
        for (c in 0 until pcm.channels) {
            var prev = 0f
            for (i in 0 until pcm.frames) {
                val x = pcm.data[i * pcm.channels + c]
                worst = max(worst, abs(x - prev))
                prev = x
            }
            worst = max(worst, abs(prev))
        }
        return worst
    }

    private fun sine(seconds: Double, hz: Double, amp: Float = 0.9f): Pcm {
        val n = f(seconds).toInt()
        return Pcm(FloatArray(n * 2) { i -> (amp * sin(2 * PI * hz * (i / 2) / rate)).toFloat() * (if (i % 2 == 0) 1f else -0.5f) }, 2, rate)
    }

    private fun sel(start: Long, end: Long, fadeIn: Int = StemSelection.DEFAULT_FADE_IN_MS, fadeOut: Int = StemSelection.DEFAULT_FADE_OUT_MS) =
        StemSelection("set", 3, start, end, fadeIn, fadeOut)

    // --- Cut ---------------------------------------------------------------------------------

    @Test
    fun theCutIsExactAndKeepsChannelsAndRate() {
        val src = sine(2.0, 330.0)
        val out = StemEditor.cut(src, sel(12_345, 60_000))
        assertEquals(2, out.channels)
        assertEquals(rate, out.sampleRate)
        assertEquals(60_000 - 12_345, out.frames)
        // Past the fades, every sample is the source's, both channels (left and right differ).
        val skip = f(0.004).toInt() + 1
        for (i in skip until out.frames - f(0.012).toInt() - 1) {
            assertEquals(src.data[(12_345 + i) * 2], out.data[i * 2], 0f)
            assertEquals(src.data[(12_345 + i) * 2 + 1], out.data[i * 2 + 1], 0f)
        }
    }

    @Test
    fun aCutAtTheTopOfAWaveDoesNotClick() {
        val hz = 1000.0
        val src = sine(1.0, hz)
        val start = f(0.25 / hz) + 4800  // a crest of the wave: a bare cut jumps from 0 to 0.9
        val end = start + 9_600
        val natural = (0.9 * 2 * PI * hz / rate).toFloat()  // the steepest step of the wave itself
        val bare = Pcm(src.data.copyOfRange((start * 2).toInt(), (end * 2).toInt()), 2, rate)
        assertTrue("a bare cut would click (test can fail)", largestStep(bare) > 0.5f)
        for ((fi, fo) in listOf(4 to 12, 50 to 50, 0 to 0)) {
            val out = StemEditor.cut(src, sel(start, end, fi, fo))
            assertEquals(0f, out.data[0], 0f)
            assertEquals(0f, out.data[out.data.size - 1], 0f)
            assertTrue("fades $fi/$fo: step ${largestStep(out)} over $natural", largestStep(out) <= natural * 1.05f)
        }
    }

    @Test
    fun aSteadyLevelFadesSmoothly() {
        // A level that does not move (the worst case: no wave to hide in) cut with « Fondu : aucun ».
        val src = Pcm(FloatArray(48_000 * 2) { 0.8f }, 2, rate)
        val out = StemEditor.cut(src, sel(1_000, 20_000, 0, 0))
        assertTrue("step ${largestStep(out)}", largestStep(out) < 0.05f)
        // The fade-in rises without going back, and is over after its length.
        val fadeIn = f(StemEditor.DECLICK_MS / 1000).toInt()
        for (i in 1 until fadeIn) assertTrue(out.data[i * 2] >= out.data[(i - 1) * 2])
        assertEquals(0.8f, out.data[fadeIn * 2], 0f)
        // The 12 ms default fade-out: the level is halved around its middle.
        val dflt = StemEditor.cut(src, sel(1_000, 20_000))
        val mid = dflt.frames - 1 - f(0.006).toInt()
        assertEquals(0.4f, dflt.data[mid * 2], 0.02f)
    }

    @Test
    fun aCutOutsideTheSoundIsClampedAndATinyOneStaysSilentAtItsEdges() {
        val src = sine(0.5, 440.0)
        assertEquals(src.frames - 20_000, StemEditor.cut(src, sel(20_000, 10_000_000)).frames)
        assertEquals(0, StemEditor.cut(src, sel(30_000, 10_000)).frames)
        assertEquals(0, StemEditor.cut(src, sel(-50, -1)).frames)
        val tiny = StemEditor.cut(src, sel(1_000, 1_010, 50, 50))
        assertEquals(10, tiny.frames)
        assertEquals(0f, tiny.data[0], 0f)
        assertEquals(0f, tiny.data[tiny.data.size - 1], 0f)
    }

    // --- Phrases -----------------------------------------------------------------------------

    private val three = listOf(1.13 to 2.6, 3.55 to 5.1, 6.2 to 7.45)

    @Test
    fun aVoiceInThreePhrasesGivesThreePhrases() {
        // A 150 ms breath inside the second phrase, and a 100 ms click alone at 8.3 s.
        val v = voice(9.0, three + listOf(8.3 to 8.4), gaps = listOf(4.2 to 4.35))
        val found = StemEditor.phrases(v)
        assertEquals(found.toString(), 3, found.size)
        found.zip(three).forEach { (r, p) ->
            assertTrue("$r starts near ${p.first}", r.first in f(p.first - 0.04)..f(p.first + 0.01))
            assertTrue("$r ends near ${p.second}", r.last + 1 in f(p.second - 0.01)..f(p.second + 0.06))
        }
    }

    @Test
    fun aLongerSilenceSplitsAPhraseAndAShortOneDoesNot() {
        val short = StemEditor.phrases(voice(4.0, listOf(0.5 to 3.0), gaps = listOf(1.5 to 1.7)))
        assertEquals(1, short.size)
        val long = StemEditor.phrases(voice(4.0, listOf(0.5 to 3.0), gaps = listOf(1.5 to 1.9)))
        assertEquals(2, long.size)
    }

    @Test
    fun silenceAndBleedHoldNoPhraseAndAVoiceThatNeverStopsIsOne() {
        assertEquals(emptyList<LongRange>(), StemEditor.phrases(Pcm(FloatArray(96_000 * 2), 2, rate)))
        assertEquals(emptyList<LongRange>(), StemEditor.phrases(voice(5.0, emptyList())))
        assertEquals(emptyList<LongRange>(), StemEditor.phrases(Pcm(FloatArray(0), 2, rate)))
        val all = StemEditor.phrases(voice(5.0, listOf(0.0 to 5.0)))
        assertEquals(listOf(0L until f(5.0)), all)
        assertEquals(emptyList<LongRange>(), StemEditor.phrases(voice(9.0, three), max = 0))
    }

    @Test
    fun theEdgesFallOnTheBeatsWithoutCuttingTheVoice() {
        val beat = f(0.5)  // 120 BPM
        // Sung well after a beat (1.38 s: the nearest beat, 1.5 s, would cut it) and ending well
        // after one (7.12 s: the nearest, 7 s, would cut it).
        val sung = listOf(1.38 to 2.6, 3.55 to 5.1, 6.2 to 7.12)
        val found = StemEditor.phrases(voice(9.0, sung), beatFrames = beat)
        assertEquals(3, found.size)
        found.zip(sung).forEach { (r, p) ->
            assertEquals("$r start on a beat", 0L, r.first % beat)
            assertEquals("$r end on a beat", 0L, (r.last + 1) % beat)
            assertTrue("$r holds the voice from ${p.first}", r.first <= f(p.first))
            assertTrue("$r holds the voice until ${p.second}", r.last + 1 >= f(p.second))
            assertTrue("$r at most a beat around it", r.first > f(p.first) - beat && r.last + 1 < f(p.second) + beat)
        }
        assertEquals(f(1.0), found[0].first)
        assertEquals(f(3.0), found[0].last + 1)
        assertEquals(f(7.5), found[2].last + 1)
    }

    @Test
    fun theGridFollowsTheSongsFirstBeatAndASingerSlightlyEarlyStillStartsOnTheBeat() {
        val beat = f(0.5)
        val first = f(0.2)  // the song's first beat at 0.2 s: beats at 0.2, 0.7, 1.2…
        // Sung 20 ms before the beat of 1.2 s and 40 ms before the one of 3.7 s.
        val v = voice(6.0, listOf(1.18 to 2.4, 3.66 to 5.0))
        val found = StemEditor.phrases(v, beat, 8, first)
        assertEquals(listOf(f(1.2) until f(2.7), f(3.7) until f(5.2)), found)
        // The contract's default for a fake ignores the offset.
        val fake = object : StemEdits {
            override fun cut(stem: Pcm, sel: StemSelection) = stem
            override fun phrases(stem: Pcm, beatFrames: Long?, max: Int) = listOf(0L until 1L)
        }
        assertEquals(listOf(0L until 1L), fake.phrases(v, beat, 8, first))
    }

    @Test
    fun twoPhrasesWithNoBeatBetweenThemBecomeOne() {
        // 300 ms apart, no beat of a slow song (1 s) in between: one phrase, both voices kept.
        val found = StemEditor.phrases(voice(6.0, listOf(0.3 to 1.2, 1.5 to 2.6)), beatFrames = f(1.0))
        assertEquals(listOf(0L until f(3.0)), found)
        // Without the grid, they are two.
        assertEquals(2, StemEditor.phrases(voice(6.0, listOf(0.3 to 1.2, 1.5 to 2.6))).size)
    }

    @Test
    fun atMostMaxPhrasesTheLongestInTimeOrder() {
        val five = listOf(0.5 to 1.0, 1.6 to 3.6, 4.2 to 4.7, 5.3 to 6.8, 7.4 to 8.4)  // 0.5 2 0.5 1.5 1
        val found = StemEditor.phrases(voice(9.0, five), max = 3)
        assertEquals(3, found.size)
        assertTrue(found.zipWithNext().all { (a, b) -> a.last < b.first })
        listOf(1.6, 5.3, 7.4).zip(found).forEach { (s, r) -> assertTrue("$r near $s", abs(r.first - f(s)) < f(0.05)) }
    }

    // --- Peaks -------------------------------------------------------------------------------

    @Test
    fun peaksKeepTheLoudestChannelOfEachSlice() {
        val data = FloatArray(1000 * 2)
        data[10 * 2 + 1] = -0.9f   // right channel, slice 0
        data[555 * 2] = 0.4f        // left, slice 5
        data[990 * 2] = 1.7f        // too loud: drawn full
        val peaks = StemEditor.peaks(Pcm(data, 2, rate), 10)
        assertArrayEquals(floatArrayOf(0.9f, 0f, 0f, 0f, 0f, 0.4f, 0f, 0f, 0f, 1f), peaks, 0f)
        // A window of the sound, part of it past the end.
        assertArrayEquals(floatArrayOf(0.4f, 0f, 1f, 0f), StemEditor.peaks(Pcm(data, 2, rate), 500L, 1300L, 4), 0f)
        assertEquals(0, StemEditor.peaks(Pcm(data, 2, rate), 0).size)
    }

    @Test
    fun thePeaksAtEveryZoomFindEveryCrestInItsSlice() {
        val frames = 3 * 60 * rate / 10  // 18 s
        val data = FloatArray(frames * 2)
        val spikes = listOf(777L to 0.3f, 100_000L to 0.9f, 400_001L to 0.6f, 863_999L to 0.5f)
        spikes.forEach { (at, v) -> data[(at * 2 + 1).toInt()] = -v }
        val pcm = Pcm(data, 2, rate)
        val pyramid = StemPeaks.of(pcm)
        assertEquals(frames.toLong(), pyramid.frames)
        for (slots in listOf(10, 333, 1080, 50_000)) {
            val out = FloatArray(slots)
            pyramid.fill(0, frames.toLong(), out)
            val exact = StemEditor.peaks(pcm, slots)
            for ((at, v) in spikes) {
                val slot = (at * slots / frames).toInt()
                assertTrue("zoom $slots, crest at $at", out[slot] >= v)
            }
            // Never quieter than the sound, never a crest from far away.
            for (b in 0 until slots) {
                assertTrue(out[b] >= exact[b])
                // A slot may read up to its own width (at least StemPeaks.BASE) of its neighbours.
                val from = frames.toLong() * b / slots
                val to = frames.toLong() * (b + 1) / slots
                val reach = max(StemPeaks.BASE.toLong(), to - from)
                if (out[b] > 0f) assertTrue("zoom $slots slot $b", spikes.any { it.first >= from - reach && it.first < to + reach })
            }
        }
        // Zoomed in on a quarter second around a crest, past the start, past the end.
        val out = FloatArray(200)
        pyramid.fill(400_001L - 6_000, 400_001L + 6_000, out)
        assertEquals(0.6f, out[100], 0f)
        assertTrue(out.count { it > 0f } <= 2)
        pyramid.fill(-1_000_000L, 0L, out)
        assertTrue(out.all { it == 0f })
        pyramid.fill(frames.toLong(), frames + 1_000L, out)
        assertTrue(out.all { it == 0f })
    }

    @Test
    fun thePeaksBuiltPieceByPieceAreTheSame() {
        val v = voice(12.0, three)
        val whole = StemPeaks.of(v)
        var reads = 0
        val pieces = StemPeaks.of(v.frames.toLong(), 2) { start, count ->
            reads++
            v.data.copyOfRange((start * 2).toInt(), ((start + count) * 2).toInt())
        }
        assertTrue("read in pieces", reads > 1)
        val a = FloatArray(777)
        val b = FloatArray(777)
        whole.fill(0, v.frames.toLong(), a)
        pieces.fill(0, v.frames.toLong(), b)
        assertArrayEquals(a, b, 0f)
        assertEquals(0L, StemPeaks.of(Pcm(FloatArray(0), 2, rate)).frames)
        StemPeaks.of(Pcm(FloatArray(0), 2, rate)).fill(0, 100, a)
        assertTrue(a.all { it == 0f })
    }
}
