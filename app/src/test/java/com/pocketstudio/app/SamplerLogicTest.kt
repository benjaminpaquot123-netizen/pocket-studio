package com.pocketstudio.app

import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.MicRecorder
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.rhythm.PadSamples
import com.pocketstudio.app.rhythm.WaveWindow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class SamplerLogicTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun floatWavRoundTripIsLossless() {
        val data = FloatArray(2001) { (it % 37 - 18) / 18.5f }.copyOf(2000)
        val pcm = Pcm(data, 2, 44100)
        val file = File(tmp.root, "a.wav")
        FloatWav.write(file, pcm)
        val back = FloatWav.read(file)
        assertEquals(2, back.channels)
        assertEquals(44100, back.sampleRate)
        assertArrayEquals(data, back.data, 0f)
        assertTrue("no temporary file left", tmp.root.listFiles()!!.all { !it.name.endsWith(".tmp") })
    }

    @Test(expected = IOException::class)
    fun garbageIsNotAWav() {
        val file = File(tmp.root, "b.wav").apply { writeText("pas un son du tout, vraiment pas") }
        FloatWav.read(file)
    }

    @Test
    fun peaksKeepTheExtremes() {
        val data = FloatArray(1000)
        data[10] = 0.9f
        data[990] = -0.7f
        val peaks = PadSamples.peaks(Pcm(data, 1, 48000), buckets = 10)
        assertEquals(0.9f, peaks[1], 0f)        // bucket 0 max
        assertEquals(-0.7f, peaks[9 * 2], 0f)   // bucket 9 min
        assertEquals(0f, peaks[5 * 2 + 1], 0f)
    }

    @Test
    fun zoomStaysInsideTheSound() {
        val zoomed = WaveWindow().transform(zoom = 4f, focus = 0.5f, pan = 0f)
        assertEquals(0.25f, zoomed.width, 1e-6f)
        assertEquals(0.375f, zoomed.start, 1e-6f)  // centred on the middle
        val panned = zoomed.transform(zoom = 1f, focus = 0.5f, pan = -10f)
        assertEquals(1f - 0.25f, panned.start, 1e-6f)  // clamped at the end
        val maxed = WaveWindow().transform(zoom = 1000f, focus = 0f, pan = 0f)
        assertEquals(WaveWindow.MIN_WIDTH, maxed.width, 1e-6f)
        assertEquals(1f, WaveWindow(0.3f, 0.2f).transform(zoom = 0.01f, focus = 0.5f, pan = 0f).width, 0f)
    }

    @Test
    fun recordingStartsAtTheFirstSound() {
        val data = FloatArray(10_000)
        data[5000] = 0.5f
        val trimmed = MicRecorder.trimSilence(data)
        assertEquals(10_000 - 5000 + 48, trimmed.size)
        assertEquals(0.5f, trimmed[48], 0f)
        assertEquals(3, MicRecorder.trimSilence(FloatArray(3)).size)  // all silence: untouched
    }

    @Test
    fun importedNamesAreTidy() {
        assertEquals("Kick lourd", PadSamples.nameFrom("Kick lourd.wav"))
        assertEquals("Son importé", PadSamples.nameFrom(null))
        assertEquals("Son importé", PadSamples.nameFrom(".mp3"))
    }
}

class SlicerTest {
    @Test
    fun gridCutsEvenly() {
        org.junit.Assert.assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f), com.pocketstudio.app.rhythm.Slicer.grid(4))
    }

    @Test
    fun tapAddsOrRemovesACutButNeverTheStart() {
        val s = com.pocketstudio.app.rhythm.Slicer
        val added = s.toggle(listOf(0f, 0.5f), 0.3f)
        org.junit.Assert.assertEquals(listOf(0f, 0.3f, 0.5f), added)
        org.junit.Assert.assertEquals(listOf(0f, 0.3f), s.toggle(added, 0.505f))
        org.junit.Assert.assertEquals(listOf(0f, 0.3f), s.toggle(listOf(0f, 0.3f), 0.005f))
    }

    @Test
    fun slicesFillThePadsFromTheSelectedOne() {
        val src = com.pocketstudio.model.PadSample("a.wav", "Breakbeat", pitch = 5f, gain = 0.8f)
        val pads = com.pocketstudio.app.rhythm.Slicer.toPads(src, listOf(0f, 0.25f, 0.5f, 0.75f), firstPad = 13)
        org.junit.Assert.assertEquals(listOf(13, 14, 15), pads.map { it.first })  // 4th slice has no pad
        val first = pads[0].second
        org.junit.Assert.assertEquals("Breakbeat 1", first.name)
        org.junit.Assert.assertEquals(0f, first.start)
        org.junit.Assert.assertEquals(0.25f, first.end)
        org.junit.Assert.assertEquals(0f, first.pitch)  // fresh settings, same level
        org.junit.Assert.assertEquals(0.8f, first.gain)
        val drums = com.pocketstudio.app.rhythm.Slicer.apply(com.pocketstudio.model.DrumPattern(), pads)
        org.junit.Assert.assertEquals("Breakbeat 3", drums.lanes[15].sample!!.name)
        org.junit.Assert.assertEquals(0.75f, drums.lanes[15].sample!!.end)  // 3rd slice ends at the 4th cut
    }
}
