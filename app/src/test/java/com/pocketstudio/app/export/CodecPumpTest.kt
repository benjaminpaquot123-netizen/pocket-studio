package com.pocketstudio.app.export

import android.media.MediaCodec
import android.media.MediaFormat
import com.pocketstudio.app.audio.AudioDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lot 1 (APK 181: « Je prépare ton morceau… · Compression M4A », the bar full, for good): the
 * codec loops on a stand-in codec that behaves like a phone's (a few buffers, outputs to take
 * before inputs come back, a clock that only moves while the loop waits on the codec), a codec
 * that never ends and one that never answers.
 */
@RunWith(RobolectricTestRunner::class)
class CodecPumpTest {
    /**
     * An AAC encoder as a phone's: [inputSlots] input buffers, [outputSlots] output buffers; an
     * input of 4096 frames becomes four 1024-frame outputs, and is only taken in once there is
     * room for them; a wait on the codec moves [clockMs] by its timeout (nothing else does).
     * [ends]: false, the end of stream never comes out; [answers]: false, nothing ever comes out.
     */
    private class PhoneLikeEncoder(
        private val inputSlots: Int = 4, private val outputSlots: Int = 8,
        private val ends: Boolean = true, private val answers: Boolean = true,
    ) : CodecPort {
        var clockMs = 0L
        var waitedMs = 0L
        var outputsTaken = 0
        private val outBytes = 1024 * 4
        private val freeInputs = ArrayDeque((0 until inputSlots).toList())
        private val pending = ArrayDeque<Pair<Int, Pair<Int, Boolean>>>()  // slot, bytes, end
        private val ready = ArrayDeque<Triple<Int, Int, Int>>()  // index, size, flags
        private var held = 0
        private var carry = 0
        private var formatShown = false
        private var next = 0

        private fun idle(timeoutUs: Long) {
            clockMs += timeoutUs / 1000
            waitedMs += timeoutUs / 1000
        }

        private fun process() {
            if (!answers) return
            while (pending.isNotEmpty()) {
                val (slot, what) = pending.first()
                val (bytes, end) = what
                val outs = (carry + bytes) / outBytes + if (end) 1 else 0
                if (ready.size + held + outs > outputSlots) return  // no room: the input waits, its slot taken
                pending.removeFirst()
                val total = carry + bytes
                repeat(total / outBytes) { ready += Triple(next++, 600, 0) }
                carry = total % outBytes
                if (end && ends) ready += Triple(next++, if (carry > 0) 300 else 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                freeInputs += slot
            }
        }

        override fun dequeueInputBuffer(timeoutUs: Long): Int {
            process()
            if (freeInputs.isNotEmpty()) return freeInputs.removeFirst()
            idle(timeoutUs)
            return MediaCodec.INFO_TRY_AGAIN_LATER
        }

        override fun queueInputBuffer(index: Int, offset: Int, size: Int, timeUs: Long, flags: Int) {
            pending += index to (size to (flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0))
            process()
        }

        override fun dequeueOutputBuffer(info: MediaCodec.BufferInfo, timeoutUs: Long): Int {
            process()
            if (answers && !formatShown) {
                formatShown = true
                return MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
            }
            val o = ready.removeFirstOrNull()
            if (o == null) {
                idle(timeoutUs)
                return MediaCodec.INFO_TRY_AGAIN_LATER
            }
            held++
            info.set(0, o.second, 0L, o.third)
            return o.first
        }

        override fun getOutputBuffer(index: Int): ByteBuffer = ByteBuffer.allocate(600)

        override fun releaseOutputBuffer(index: Int) {
            held--
            outputsTaken++
            process()
        }

        override val outputFormat: MediaFormat get() = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48_000, 2)
    }

    /** A 16-bit stereo WAV of [seconds] at 48 kHz (a quiet sine). */
    private fun wav(seconds: Int): File {
        val f = File.createTempFile("pump", ".wav")
        val frames = seconds * 48_000
        RandomAccessFile(f, "rw").use { out ->
            val head = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            head.put("RIFF".toByteArray()).putInt(36 + frames * 4).put("WAVE".toByteArray())
            head.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(2).putInt(48_000).putInt(48_000 * 4).putShort(4).putShort(16)
            head.put("data".toByteArray()).putInt(frames * 4)
            out.write(head.array())
            val block = ByteBuffer.allocate(4800 * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (start in 0 until frames step 4800) {
                block.clear()
                for (i in 0 until 4800) {
                    val v = (3000 * Math.sin(2 * Math.PI * 220 * (start + i) / 48_000.0)).toInt().toShort()
                    block.putShort(v).putShort(v)
                }
                out.write(block.array())
            }
        }
        return f
    }

    private class Recorder : AacEncoder.MuxSink {
        var started = false
        var written = 0
        override fun addTrack(format: MediaFormat): Int = 0
        override fun start() { started = true }
        override fun write(data: ByteBuffer, info: MediaCodec.BufferInfo) { written++ }
    }

    @Test(timeout = 60_000)
    fun theEncoderTakesEveryOutputAndNeverWaitsMinutesOnTheCodec() {
        // 30 s of a song: 1407 AAC frames. The loops of APK 181 took one output a round and waited
        // 10 ms for an input buffer the codec could only free once its outputs were taken: about
        // 7 ms of waiting per 21 ms frame, ten seconds here, two minutes for a 5-minute song.
        val file = wav(30)
        try {
            val codec = PhoneLikeEncoder()
            val sink = Recorder()
            val progress = mutableListOf<Float>()
            WavPcm.open(file).use { pcm ->
                val bufs = HashMap<Int, ByteBuffer>()
                AacEncoder.pump(codec, pcm, 1f, { false }, { progress += it }, { i -> bufs.getOrPut(i) { ByteBuffer.allocateDirect(16 * 1024) } }, sink, now = { codec.clockMs })
            }
            assertTrue(sink.started)
            // Every frame out (the last one with the end of stream holds no data here).
            assertEquals((30 * 48_000 + 1023) / 1024, sink.written)
            // Waiting on the codec: under 1 % of the audio's length (a phone's codec works on its own meanwhile).
            assertTrue("waited ${codec.waitedMs} ms on the codec for 30 s of audio", codec.waitedMs < 300)
            // An honest bar: from the start to the end, never backwards.
            assertEquals(1f, progress.last(), 0f)
            assertTrue(progress.zipWithNext().all { (a, b) -> b >= a })
        } finally {
            file.delete()
        }
    }

    @Test(timeout = 60_000)
    fun anEncoderThatNeverEndsFailsInsteadOfHanging() {
        val file = wav(2)
        try {
            val codec = PhoneLikeEncoder(ends = false)
            try {
                WavPcm.open(file).use { pcm ->
                    AacEncoder.pump(codec, pcm, 1f, { false }, {}, { ByteBuffer.allocate(16 * 1024) }, Recorder(), now = { codec.clockMs })
                }
                fail("an encoder without end of stream must fail the step")
            } catch (e: CodecStalledException) {
                // Given up a few seconds (of the codec's clock) after the last input.
                assertTrue("gave up after ${codec.clockMs} ms", codec.clockMs <= CodecPump.END_MS + 1_000)
            }
        } finally {
            file.delete()
        }
    }

    @Test(timeout = 60_000)
    fun anEncoderThatNeverAnswersFailsInsteadOfHanging() {
        val file = wav(2)
        try {
            val codec = PhoneLikeEncoder(answers = false)
            try {
                WavPcm.open(file).use { pcm ->
                    AacEncoder.pump(codec, pcm, 1f, { false }, {}, { ByteBuffer.allocate(16 * 1024) }, Recorder(), now = { codec.clockMs })
                }
                fail("a codec that never answers must fail the step")
            } catch (e: CodecStalledException) {
                assertTrue("gave up after ${codec.clockMs} ms", codec.clockMs <= CodecPump.STALL_MS + 1_000)
            }
        } finally {
            file.delete()
        }
    }

    @Test(timeout = 60_000)
    fun cancelStopsTheEncoderAtOnce() {
        val file = wav(10)
        try {
            val codec = PhoneLikeEncoder()
            // « Annuler » touched at a third of the song.
            var cancelled = false
            var last = 0f
            try {
                WavPcm.open(file).use { pcm ->
                    AacEncoder.pump(codec, pcm, 1f, { cancelled }, { f -> last = f; if (f >= 0.3f) cancelled = true },
                        { ByteBuffer.allocate(16 * 1024) }, Recorder(), now = { codec.clockMs })
                }
                fail("cancelled")
            } catch (e: kotlinx.coroutines.CancellationException) {
                assertTrue("stopped at $last", last < 0.4f)
            }
        } finally {
            file.delete()
        }
    }

    /** The decoded samples, counted. */
    private class Counting : AudioDecoder.FoldingSink() {
        override fun add(v: Float) { size++ }
    }

    /**
     * A decoder: one 1024-frame output for each input; [ends] false: the end of stream never
     * comes out. Its outputs are 16-bit stereo silence.
     */
    private class FakeDecoder(private val ends: Boolean) : CodecPort {
        var clockMs = 0L
        private var inputs = 0
        private var outputs = 0
        private var endQueued = false
        private var endShown = false
        override fun dequeueInputBuffer(timeoutUs: Long): Int = if (inputs - outputs < 4 && !endQueued) inputs else { clockMs += timeoutUs / 1000; -1 }
        override fun queueInputBuffer(index: Int, offset: Int, size: Int, timeUs: Long, flags: Int) {
            if (flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) endQueued = true else inputs++
        }
        override fun dequeueOutputBuffer(info: MediaCodec.BufferInfo, timeoutUs: Long): Int {
            if (outputs < inputs) {
                info.set(0, 4096, 0L, 0)
                return outputs++ % 4
            }
            if (endQueued && ends && !endShown) {
                endShown = true
                info.set(0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                return 0
            }
            clockMs += timeoutUs / 1000
            return MediaCodec.INFO_TRY_AGAIN_LATER
        }
        override fun getOutputBuffer(index: Int): ByteBuffer = ByteBuffer.allocate(4096)
        override fun releaseOutputBuffer(index: Int) = Unit
        override val outputFormat: MediaFormat get() = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, 48_000, 2)
    }

    @Test(timeout = 60_000)
    fun theCheckDecodesToTheEndOrFailsButNeverHangs() {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48_000, 2)
        // 100 packets: all of them decoded, the bar to its end.
        var fed = 0
        val sink = Counting()
        val progress = mutableListOf<Float>()
        val ok = FakeDecoder(ends = true)
        AudioDecoder.drain(ok, format, sink, 3600.0, cut = false, isCancelled = { false }, progress = { progress += it },
            durationUs = 100 * 1024 * 1_000_000L / 48_000, read = { if (fed < 100) { fed++; CodecPump.Fed(400, 0, end = false) } else CodecPump.Fed(0, 0, end = true) },
            now = { ok.clockMs })
        assertEquals(100L * 1024 * 2, sink.size)
        assertEquals(1f, progress.last(), 0f)
        // No end of stream: given up, an error the export reports (never « Vérification » for good).
        fed = 0
        val stuck = FakeDecoder(ends = false)
        try {
            AudioDecoder.drain(stuck, format, Counting(), 3600.0, cut = false, isCancelled = { false }, progress = {},
                durationUs = 0, read = { if (fed < 100) { fed++; CodecPump.Fed(400, 0, end = false) } else CodecPump.Fed(0, 0, end = true) },
                now = { stuck.clockMs })
            fail("a decoder without end of stream must fail")
        } catch (e: CodecStalledException) {
            assertTrue(stuck.clockMs <= CodecPump.END_MS + 1_000)
        }
        // Cancelled: the decoder's own « annulé ».
        try {
            AudioDecoder.drain(FakeDecoder(ends = true), format, Counting(), 3600.0, cut = false, isCancelled = { true }, progress = {},
                durationUs = 0, read = { CodecPump.Fed(0, 0, end = true) })
            fail("cancelled")
        } catch (e: AudioDecoder.CancelledException) {
            // expected
        }
    }

    @Test(timeout = 60_000)
    fun aSongTooLongByItsHeaderIsRefusedBeforeDecodingWithItsTrueLength() {
        // Audit 02 M1: a 9-minute song (7 at most) said « 9:00 » at once, not « 7 min » after 7 minutes of decoding.
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48_000, 2)
        var fed = 0
        val sink = Counting()
        try {
            AudioDecoder.drain(FakeDecoder(ends = true), format, sink, 420.0, cut = false, isCancelled = { false }, progress = {},
                durationUs = 540L * 1_000_000, read = { fed++; CodecPump.Fed(400, 0, end = fed > 100) })
            fail("too long")
        } catch (e: AudioDecoder.TooLongException) {
            assertEquals(540.0, e.seconds, 0.01)
        }
        assertEquals(0, fed)
        assertEquals(0L, sink.size)
        assertEquals("Morceau trop long (9:00) : 7 minutes au maximum.", com.pocketstudio.app.remix.SeparationService.tooLong(540.0))
        // Cut to its first minutes (a pad's sample): the header never refuses it.
        fed = 0
        AudioDecoder.drain(FakeDecoder(ends = true), format, Counting(), 420.0, cut = true, isCancelled = { false }, progress = {},
            durationUs = 540L * 1_000_000, read = { if (fed < 10) { fed++; CodecPump.Fed(400, 0, end = false) } else CodecPump.Fed(0, 0, end = true) })
        assertEquals(10, fed)
    }
}
