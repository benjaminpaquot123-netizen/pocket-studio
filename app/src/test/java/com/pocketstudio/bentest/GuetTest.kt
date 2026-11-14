package com.pocketstudio.bentest

import com.pocketstudio.bentest.SonsFabriques.RATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/** « Écoute au micro » (T1b): nothing of the room is kept, but the 2 s around a hole or a saturated microphone. */
class GuetTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun ecouter(g: Guet, x: FloatArray) {
        var i = 0
        while (i < x.size) {
            val n = minOf(960, x.size - i)
            g.ajouter(x.copyOfRange(i, i + n), n)
            i += n
        }
        g.finir()
    }

    /** The samples of a 16-bit mono WAV written by the robot. */
    private fun wav(f: File): FloatArray {
        val bytes = f.readBytes()
        assertEquals("RIFF", String(bytes, 0, 4))
        val data = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return FloatArray(data.remaining()) { data.get(it) / 32768f }
    }

    private fun rms(x: FloatArray, from: Double, to: Double): Double {
        val a = (from * RATE).toInt(); val b = (to * RATE).toInt()
        return sqrt((a until b).sumOf { x[it].toDouble() * x[it] } / (b - a))
    }

    @Test
    fun aHoleIsFoundAndOnlyTheTwoSecondsAroundItAreKept() {
        val dir = tmp.newFolder("sons")
        val song = SonsFabriques.rose(8 * RATE, rms = 0.1)
        // A dropout heard in the room: 250 ms of nothing at 4 s.
        for (i in (4.0 * RATE).toInt() until (4.25 * RATE).toInt()) song[i] = 0f
        val g = Guet(RATE, dir, "micro_trap")
        ecouter(g, song)
        assertEquals(g.anomalies.toString(), 1, g.anomalies.size)
        assertEquals("trou", g.anomalies[0].kind)
        assertEquals(4.0, g.anomalies[0].seconde, 0.1)
        assertEquals("one extract, the only file", listOf(g.extraits.single().name), dir.list()!!.toList())
        val x = wav(g.extraits.single())
        assertEquals("2 s kept", 2.0, x.size.toDouble() / RATE, 0.05)
        // The hole in the middle of the extract, the song around it.
        assertTrue("the hole at the middle", rms(x, 1.0, 1.2) < 0.001)
        assertTrue("the song before it", rms(x, 0.2, 0.8) > 0.03)
        assertTrue("the song after it", rms(x, 1.3, 1.8) > 0.03)
    }

    /** Trap and techno cut their sounds: 125 ms of silence on every beat is the music, not a dropout. */
    @Test
    fun theSilencesOfARhythmAreNotHoles() {
        val dir = tmp.newFolder("sons")
        val song = SonsFabriques.rose(8 * RATE, rms = 0.1)
        val beat = RATE / 2
        for (i in song.indices) if (i % beat >= beat * 3 / 4) song[i] = 0f
        val g = Guet(RATE, dir, "micro_techno")
        ecouter(g, song)
        assertTrue(g.anomalies.toString(), g.anomalies.isEmpty())
        assertTrue("nothing written", dir.list()!!.isEmpty())
    }

    @Test
    fun aSteadySongKeepsNothing() {
        val dir = tmp.newFolder("sons")
        val g = Guet(RATE, dir, "micro_house")
        ecouter(g, SonsFabriques.rose(8 * RATE, rms = 0.1))
        assertTrue(g.anomalies.toString(), g.anomalies.isEmpty())
        assertTrue("nothing written", dir.list()!!.isEmpty())
    }

    @Test
    fun aSaturatedMicrophoneIsSaidAndOneExtractAtMost() {
        val dir = tmp.newFolder("sons")
        val song = SonsFabriques.rose(10 * RATE, rms = 0.1)
        for (i in (3 * RATE) until (3 * RATE + 40)) song[i] = if (i % 2 == 0) 1f else -1f
        for (i in (7.0 * RATE).toInt() until (7.3 * RATE).toInt()) song[i] = 0f
        val g = Guet(RATE, dir, "micro_jump_up", maxExtraits = 1)
        ecouter(g, song)
        assertEquals(listOf("saturation", "trou"), g.anomalies.map { it.kind })
        assertEquals(3.0, g.anomalies[0].seconde, 0.05)
        assertEquals("one extract at most", 1, dir.list()!!.size)
        assertTrue("its name says the step and the second", g.extraits.single().name.startsWith("micro_jump_up-3"))
        assertTrue(abs(wav(g.extraits.single()).size - 2 * RATE) < RATE / 20)
    }
}
