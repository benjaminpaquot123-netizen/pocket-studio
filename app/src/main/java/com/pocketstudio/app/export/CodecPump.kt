package com.pocketstudio.app.export

import android.media.MediaCodec
import android.media.MediaFormat
import com.pocketstudio.model.i18n.tr
import java.io.IOException
import java.nio.ByteBuffer

/**
 * The few calls of [MediaCodec] a feed-and-drain loop makes: the phone's codec ([of]), or a
 * stand-in in the tests (a slow codec, one that never ends).
 */
interface CodecPort {
    fun dequeueInputBuffer(timeoutUs: Long): Int
    fun queueInputBuffer(index: Int, offset: Int, size: Int, timeUs: Long, flags: Int)
    fun dequeueOutputBuffer(info: MediaCodec.BufferInfo, timeoutUs: Long): Int
    fun getOutputBuffer(index: Int): ByteBuffer
    fun releaseOutputBuffer(index: Int)
    val outputFormat: MediaFormat

    companion object {
        fun of(codec: MediaCodec): CodecPort = object : CodecPort {
            override fun dequeueInputBuffer(timeoutUs: Long) = codec.dequeueInputBuffer(timeoutUs)
            override fun queueInputBuffer(index: Int, offset: Int, size: Int, timeUs: Long, flags: Int) = codec.queueInputBuffer(index, offset, size, timeUs, flags)
            override fun dequeueOutputBuffer(info: MediaCodec.BufferInfo, timeoutUs: Long) = codec.dequeueOutputBuffer(info, timeoutUs)
            override fun getOutputBuffer(index: Int): ByteBuffer = codec.getOutputBuffer(index)!!
            override fun releaseOutputBuffer(index: Int) = codec.releaseOutputBuffer(index, false)
            override val outputFormat: MediaFormat get() = codec.outputFormat
        }
    }
}

/** The phone's codec stopped answering (nothing in, nothing out for too long): the step fails rather than wait forever. */
class CodecStalledException(message: String) : IOException(message)

/**
 * Feeds a codec and drains it until its end of stream, never waiting forever (lot 1, the M4A
 * stuck on « Compression M4A »). Each round feeds every free input buffer, then takes EVERY output
 * ready: the old loops took one output a round and waited 10 ms for an input buffer the codec
 * could not free until its outputs were taken, so an AAC encoder (four outputs for each input)
 * ran at about 2 to 3 times real time, minutes for a 5-minute song, the bar full all along. It
 * waits for the codec only when there is nothing to do, a few milliseconds at a time.
 *
 * Fails with [CodecStalledException] when nothing goes in or comes out for [stallMs], or when
 * the end of stream is still missing [endMs] after the last input; with
 * [kotlinx.coroutines.CancellationException] as soon as [isCancelled].
 */
object CodecPump {
    /** Nothing in nor out for this long: the codec is stuck. */
    const val STALL_MS = 15_000L
    /** The end of stream this long after the last input at most. */
    const val END_MS = 10_000L
    /** The longest a round waits for the codec (it returns as soon as an output is ready). */
    const val WAIT_US = 5_000L

    /** Milliseconds of a steady clock. */
    fun clockMs(): Long = System.nanoTime() / 1_000_000

    /** What [run]'s feed put in input buffer [index]: [bytes] at [timeUs], [end] for the last one (it may hold data). */
    class Fed(val bytes: Int, val timeUs: Long, val end: Boolean)

    /**
     * [feed] fills input buffer `index` (the codec's own buffer or image, asked by the caller)
     * and says what it put; [format] gets the output format; [output] gets each output buffer
     * (positioned on its data) and its info, and returns true to stop there (enough read).
     */
    fun run(
        codec: CodecPort,
        feed: (index: Int) -> Fed,
        format: (MediaFormat) -> Unit = {},
        output: (ByteBuffer, MediaCodec.BufferInfo) -> Boolean,
        isCancelled: () -> Boolean = { false },
        stallMs: Long = STALL_MS,
        endMs: Long = END_MS,
        now: () -> Long = ::clockMs,
    ) {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var lastProgress = now()
        var inputEnded = 0L
        while (true) {
            if (isCancelled()) throw kotlinx.coroutines.CancellationException("annulé")
            var progressed = false
            // Every free input buffer, none waited for: the outputs are taken in between.
            while (!inputDone) {
                val i = codec.dequeueInputBuffer(0)
                if (i < 0) break
                val fed = feed(i)
                codec.queueInputBuffer(i, 0, fed.bytes, fed.timeUs, if (fed.end) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                progressed = true
                if (fed.end) {
                    inputDone = true
                    inputEnded = now()
                }
            }
            // Every output ready; a short wait only when this round did nothing else.
            var wait = if (progressed) 0L else WAIT_US
            while (true) {
                val o = codec.dequeueOutputBuffer(info, wait)
                wait = 0L
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    format(codec.outputFormat)
                    progressed = true
                } else if (o >= 0) {
                    // Read before [output], which may clear the flag on its way to a muxer.
                    val end = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    val data = codec.getOutputBuffer(o)
                    data.position(info.offset)
                    data.limit(info.offset + info.size)
                    val stop = try {
                        output(data, info)
                    } finally {
                        codec.releaseOutputBuffer(o)
                    }
                    progressed = true
                    if (stop || end) return
                } else {
                    // INFO_TRY_AGAIN_LATER (nothing yet), or the old « buffers changed »: next round.
                    break
                }
            }
            val t = now()
            if (progressed) lastProgress = t
            if (t - lastProgress > stallMs) throw CodecStalledException(tr("le codec du téléphone ne répond plus"))
            if (inputDone && t - inputEnded > endMs) throw CodecStalledException(tr("le codec du téléphone ne finit pas"))
        }
    }
}
