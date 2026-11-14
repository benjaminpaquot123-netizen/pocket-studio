package com.pocketstudio.app.audit3

import android.media.MediaCodec
import android.media.MediaFormat
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.CodecPort
import com.pocketstudio.app.export.CodecPump
import com.pocketstudio.app.rhythm.ZipPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Audit stage 3, « fichiers piégés »: sounds whose headers lie (no channel, an impossible rate, a
 * data chunk longer than the file), a sound pack .zip with « ../ » paths or a bomb: each fails with
 * a message (or is cut), never hangs, never crashes, never writes outside its folder.
 */
@RunWith(RobolectricTestRunner::class)
class TrappedSoundFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun wav(rate: Int, channels: Int, floats: Int, claimedData: Int = floats * 4, format: Int = 3, bits: Int = 32): ByteArray {
        val b = ByteBuffer.allocate(44 + floats * 4).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + floats * 4).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(format.toShort()).putShort(channels.toShort()).putInt(rate)
            .putInt(rate * channels * 4).putShort((channels * 4).toShort()).putShort(bits.toShort())
        b.put("data".toByteArray()).putInt(claimedData)
        repeat(floats) { b.putFloat(if (it % 3 == 0) Float.NaN else 0.25f) }
        return b.array()
    }

    private fun fails(what: String, block: () -> Unit) {
        val e = runCatching(block).exceptionOrNull()
        assertTrue("$what: a clean IOException, got $e", e is IOException)
    }

    @Test
    fun lyingFloatWavsAreRefused() {
        val f = tmp.newFile("x.wav")
        fun read(bytes: ByteArray): Pcm { f.writeBytes(bytes); return FloatWav.read(f) }
        // A plain one reads (its NaN are the engine's to clean: Sample::create zeroes them).
        assertEquals(1000, read(wav(48_000, 2, 1000)).data.size)
        fails("a rate of 100 Hz (gigabytes once resampled)") { read(wav(100, 2, 1000)) }
        fails("a rate of zero") { read(wav(0, 2, 1000)) }
        fails("negative rate") { read(wav(-48_000, 2, 1000)) }
        fails("no channel") { read(wav(48_000, 0, 1000)) }
        fails("nine channels") { read(wav(48_000, 9, 1000)) }
        fails("16-bit claimed as float") { read(wav(48_000, 2, 1000, format = 1, bits = 16)) }
        fails("data longer than the file") { read(wav(48_000, 2, 1000, claimedData = 1 shl 30)) }
        fails("negative data size") { read(wav(48_000, 2, 1000, claimedData = -8)) }
        fails("truncated") { read(wav(48_000, 2, 1000).copyOf(60)) }
        fails("empty") { read(ByteArray(0)) }
        fails("not a wav") { read(ByteArray(4000) { 7 }) }
    }

    /** A codec nobody should reach: the format is refused before it is fed. */
    private object Unreachable : CodecPort {
        override fun dequeueInputBuffer(timeoutUs: Long): Int = error("fed")
        override fun queueInputBuffer(index: Int, offset: Int, size: Int, timeUs: Long, flags: Int) = error("fed")
        override fun dequeueOutputBuffer(info: MediaCodec.BufferInfo, timeoutUs: Long): Int = error("fed")
        override fun getOutputBuffer(index: Int): ByteBuffer = error("fed")
        override fun releaseOutputBuffer(index: Int) = error("fed")
        override val outputFormat: MediaFormat get() = error("fed")
    }

    private class Sink : AudioDecoder.FoldingSink() {
        override fun add(v: Float) { size++ }
    }

    @Test(timeout = 20_000)
    fun aDecoderFormatWithNoChannelOrAnImpossibleRateFailsAtOnce() {
        for ((rate, channels) in listOf(48_000 to 0, 48_000 to -2, 0 to 2, 1 to 2, 100_000_000 to 2, 44_100 to 1000)) {
            val format = MediaFormat.createAudioFormat("audio/mpeg", rate, channels)
            fails("rate $rate, $channels channels") {
                AudioDecoder.drain(Unreachable, format, Sink(), 120.0, cut = true, isCancelled = { false }, progress = {}, durationUs = 0L, read = { CodecPump.Fed(0, 0, true) })
            }
        }
        // A codec changing its mind mid-way (output format with no channel): before, an endless loop.
        fails("folded with no channel") { Sink().appendFolded(ByteBuffer.allocate(64), android.media.AudioFormat.ENCODING_PCM_16BIT, 0) }
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> for ((n, d) in entries) { z.putNextEntry(ZipEntry(n)); z.write(d); z.closeEntry() } }
        return out.toByteArray()
    }

    @Test
    fun aSoundPackZipWritesOnlyInItsFolder() {
        val phone = tmp.newFolder("phone")
        val dir = File(phone, "pack")
        val files = ZipPack.extractAudio(
            zip("../../evil.wav" to ByteArray(10), "a/../../b.wav" to ByteArray(10), "/abs.wav" to ByteArray(10), "c\\..\\..\\d.wav" to ByteArray(10), ".hidden.wav" to ByteArray(10)).inputStream(),
            dir, 16, setOf("wav"),
        )
        assertEquals(listOf("abs.wav", "b.wav", "d.wav", "evil.wav"), files.map { it.second })
        assertTrue(files.all { it.first.parentFile == dir })
        assertEquals(setOf("pack"), phone.list()!!.toSet())
    }

    @Test
    fun aSoundPackBombStopsAndLeavesNothing() {
        val phone = tmp.newFolder("phone2")
        val dir = File(phone, "pack")
        // Streamed: an entry of zeros past the per-file limit (never all in memory).
        val pipeIn = java.io.PipedInputStream(1 shl 16)
        val pipeOut = java.io.PipedOutputStream(pipeIn)
        Thread {
            runCatching {
                ZipOutputStream(pipeOut).use { z ->
                    z.putNextEntry(ZipEntry("bomb.wav"))
                    val zeros = ByteArray(1 shl 20)
                    repeat(((ZipPack.MAX_ENTRY_BYTES shr 20) + 2).toInt()) { z.write(zeros) }
                    z.closeEntry()
                }
            }
        }.also { it.isDaemon = true }.start()
        val e = runCatching { ZipPack.extractAudio(pipeIn, dir, 16, setOf("wav")) }.exceptionOrNull()
        assertTrue("$e", e is ZipPack.TooBigException)
        assertTrue("nothing left: ${dir.list()?.toList()}", !dir.exists() || dir.list().isNullOrEmpty())
    }
}
