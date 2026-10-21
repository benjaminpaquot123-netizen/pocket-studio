package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class RootNoteTest {
    private val rate = 44100

    private fun tone(hz: Double, seconds: Double = 1.0, harmonics: Int = 1, decay: Double = 1.5): FloatArray {
        var phase = 0.0
        return FloatArray((seconds * rate).toInt()) { i ->
            val t = i.toDouble() / rate
            phase += 2 * PI * hz / rate
            var v = 0.0
            for (h in 1..harmonics) v += sin(phase * h) / h
            (0.6 * v * exp(-t * decay)).toFloat()
        }
    }

    @Test
    fun findsTheNoteOfPlainAndRichTones() {
        assertEquals(69f, RootNote.detect(tone(440.0), rate)!!, 0.1f)
        assertEquals(36f, RootNote.detect(tone(65.406), rate)!!, 0.1f)          // C2
        assertEquals(45f, RootNote.detect(tone(110.0, harmonics = 8), rate)!!, 0.1f)  // saw-like A2
        assertEquals(60.3f, RootNote.detect(tone(261.63 * 1.0175), rate)!!, 0.1f)  // a slightly sharp C4
    }

    @Test
    fun findsAn808UnderItsPitchDrop() {
        // The typical 808: a fast drop from 120 Hz to its note (C1, 32.7 Hz), then a long sub.
        var phase = 0.0
        val x = FloatArray(rate * 2) { i ->
            val t = i.toDouble() / rate
            val hz = 32.703 + (120.0 - 32.703) * exp(-t * 60)
            phase += 2 * PI * hz / rate
            (0.8 * sin(phase) * exp(-t * 0.8)).toFloat()
        }
        val note = RootNote.detect(x, rate)
        assertNotNull(note)
        assertEquals(24f, note!!, 0.25f)
    }

    @Test
    fun noiseAndSilenceHaveNoNote() {
        val rng = java.util.Random(1)
        assertNull(RootNote.detect(FloatArray(rate) { (rng.nextFloat() - 0.5f) * exp(-it / 20000.0).toFloat() }, rate))
        assertNull(RootNote.detect(FloatArray(rate), rate))
        assertNull(RootNote.detect(FloatArray(100) { 0.5f }, rate))
    }

    @Test
    fun namesNotesTheFrenchWay() {
        assertEquals("Do3", RootNote.name(60f))
        assertEquals("La3", RootNote.name(69.2f))
        assertEquals("Do0", RootNote.name(24f))
    }

    @Test
    fun namesNotesTheEnglishWayInEnglishNotation() {
        // Middle C is C4 in English notation, not "Do3" nor "C3".
        assertEquals("C4", RootNote.name(60f, english = true))
        assertEquals("A4", RootNote.name(69.2f, english = true))
        assertEquals("C#1", RootNote.name(25f, english = true))
    }

    @Test
    fun sampledPatchesAreSanitised() {
        val bad = SampledPatch(file = "../x.wav", root = Float.NaN, glide = 5f, release = -1f)
        val ok = bad.sanitized()
        assertNull(ok.file)
        assertEquals(60f, ok.root)
        assertEquals(1f, ok.glide)
        assertEquals(0f, ok.release)
        assertEquals(10, ok.engineValues().size)
    }
}
