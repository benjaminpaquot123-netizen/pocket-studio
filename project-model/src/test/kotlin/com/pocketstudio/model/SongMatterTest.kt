package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Step 5d: the drums' hits told apart and the bass line followed, on sounds made here. */
class SongMatterTest {
    private val rate = 48_000
    private val random = java.util.Random(7)

    private fun add(x: FloatArray, at: Double, seconds: Double, sample: (Double) -> Double) {
        val from = (at * rate).toInt()
        for (i in 0 until (seconds * rate).toInt()) if (from + i < x.size) x[from + i] += sample(i.toDouble() / rate).toFloat()
    }

    private fun kick(t: Double) = 0.8 * sin(2 * PI * (55 + 60 * exp(-t * 40)) * t) * exp(-t * 9)
    private var lp = 0.0
    private fun snare(t: Double): Double {
        lp = 0.7 * lp + 0.3 * (random.nextDouble() * 2 - 1)  // noise, its top rolled off (a snare's body)
        return (0.4 * sin(2 * PI * 190 * t) + 0.6 * lp) * exp(-t * 18)
    }
    private var last = 0.0
    private fun hat(t: Double): Double {
        val n = random.nextDouble() * 2 - 1
        val hp = n - last  // white noise, its lows taken out
        last = n
        return 0.3 * hp * exp(-t * 60)
    }

    @Test
    fun kicksSnaresAndHatsAreFoundAndToldApart() {
        val x = FloatArray(2 * rate)
        listOf(0.2, 1.2).forEach { add(x, it, 0.4, ::kick) }
        listOf(0.7, 1.7).forEach { add(x, it, 0.25, ::snare) }
        listOf(0.45, 0.95, 1.45).forEach { add(x, it, 0.1, ::hat) }
        val hits = SongMatter.drumHits(x, rate)
        val expected = listOf(0.2 to SongMatter.Drum.Kick, 0.45 to SongMatter.Drum.Hat, 0.7 to SongMatter.Drum.Snare, 0.95 to SongMatter.Drum.Hat,
            1.2 to SongMatter.Drum.Kick, 1.45 to SongMatter.Drum.Hat, 1.7 to SongMatter.Drum.Snare)
        assertEquals(hits.map { "%.2f %s %.2f %.2f".format(it.at.toDouble() / rate, it.kind, it.low, it.high) }.toString(), expected.size, hits.size)
        hits.zip(expected).forEach { (h, e) ->
            assertEquals(e.first, h.at.toDouble() / rate, 0.012)
            assertEquals("at ${e.first}: " + hits.map { "%.2f %s %.2f %.2f".format(it.at.toDouble() / rate, it.kind, it.low, it.high) }, e.second, h.kind)
        }
        val best = SongMatter.bestHits(hits)
        assertEquals(setOf(SongMatter.Drum.Kick, SongMatter.Drum.Snare, SongMatter.Drum.Hat), best.keys)
        assertTrue(best.values.all { it.size in 1..2 })
    }

    @Test
    fun aBassLineBecomesNotesOnTheSixteenths() {
        val step = 6000.0  // a sixteenth at 120 BPM
        val x = FloatArray((12 * step).toInt())
        add(x, 0.0, 4 * step / rate) { t -> 0.5 * sin(2 * PI * 55.0 * t) }            // A1 (33), 4 steps
        add(x, 4 * step / rate, 4 * step / rate) { t -> 0.5 * sin(2 * PI * 65.406 * t) } // C2 (36), 4 steps
        val notes = SongMatter.bassNotes(x, rate, step, 10)
        val t = NoteClip.TICKS_PER_STEP
        assertEquals(listOf(Note(0, 4 * t, 33), Note(4 * t, 4 * t, 36)), notes)
    }

    @Test
    fun theBassLineOnTheAcid303() {
        val t = NoteClip.TICKS_PER_STEP
        val steps = SongMatter.acidSteps(listOf(Note(0, 2 * t, 33), Note(4 * t, t, 48)), 16, 36)
        assertEquals(AcidStep(note = 9, octave = -1, gate = true, slide = true), steps[0])
        assertEquals(AcidStep(note = 9, octave = -1, gate = true, slide = false), steps[1])
        assertEquals(AcidStep(), steps[2])
        assertEquals(AcidStep(note = 0, octave = 1, gate = true), steps[4])
    }
}
