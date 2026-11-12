package com.pocketstudio.app.human

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.AudioDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tour 5 « sons-essai »: the trial packs are WAV files, and the bench must hear them as they are (the live bench plays the
 * songs made with them; Robolectric has no decoder, and a pack's FLAC is given to it as a soft tone, [BenchFlac]). A WAV
 * of a pack is read with its own samples, its rate and its channels.
 */
@RunWith(RobolectricTestRunner::class)
class BenchWavPackTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /** A 16-bit PCM WAV of [frames] frames of [channels] channels, each channel a sine of its own. */
    private fun wav(file: File, frames: Int, channels: Int, rate: Int): ShortArray {
        val samples = ShortArray(frames * channels) { i ->
            val c = i % channels
            (sin((i / channels) * 2.0 * Math.PI * (330.0 + 110.0 * c) / rate) * (20_000 - 7_000 * c)).toInt().toShort()
        }
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN).also { b -> samples.forEach { b.putShort(it) } }.array()
        val head = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(channels.toShort()); putInt(rate)
            putInt(rate * channels * 2); putShort((channels * 2).toShort()); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }.array()
        file.parentFile!!.mkdirs()
        file.writeBytes(head + data)
        return samples
    }

    @Test
    fun aWavOfAPackIsReadWithItsOwnSamples() {
        val dir = File(app.filesDir, "bench-packs")
        for ((name, channels) in listOf("kick_01.wav" to 1, "clap_01.wav" to 2)) {
            val file = File(File(dir, "essai_techno"), name)
            val written = wav(file, 4_800 + 700 * channels, channels, 48_000)
            BenchFlac(app, dir).register()
            val pcm = runCatching { AudioDecoder.decode(app, Uri.fromFile(file)) }.getOrNull()
            assertNotNull("$name not read on the bench", pcm)
            pcm!!
            assertEquals(name, channels, pcm.channels)
            assertEquals(name, 48_000, pcm.sampleRate)
            assertEquals(name, written.size, pcm.data.size)
            val worst = written.indices.maxOf { abs(pcm.data[it] - written[it] / 32768f) }
            assertEquals("$name: its own samples, not a stand-in tone", 0f, worst, 1f / 32768f)
        }
    }
}
