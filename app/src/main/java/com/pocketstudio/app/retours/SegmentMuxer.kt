package com.pocketstudio.app.retours

import android.media.MediaCodec
import android.media.MediaFormat
import com.pocketstudio.app.export.video.MuxSink
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** The screen recordings' file names: BenStudio-retours-<date>-<n>.mp4. */
object SegmentNames {
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    fun stamp(at: LocalDateTime): String = at.format(stamp)
    fun name(stamp: String, n: Int): String = "BenStudio-retours-$stamp-$n.mp4"
}

/**
 * Writes the encoded screen (H.264) and sound (AAC) into consecutive MP4 files of at most
 * [maxBytes] each: once a file nears its limit, a key picture is asked for ([requestKeyFrame]) and
 * the next file starts on it, its times from 0. [open] gives segment n's file (1, 2…); [onSegment]
 * says when one starts (its first picture's time, µs). Not thread-safe: one caller at a time.
 */
class SegmentMuxer(
    private val open: (Int) -> MuxSink,
    private val maxBytes: Long = MAX_BYTES,
    private val requestKeyFrame: () -> Unit = {},
    private val onSegment: (n: Int, firstPtsUs: Long) -> Unit = { _, _ -> },
) {
    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null
    private var expectAudio = true
    private var sink: MuxSink? = null
    private var videoTrack = -1
    private var audioTrack = -1
    private var basePtsUs = 0L
    private var lastAudioUs = -1L
    private var lastVideoUs = -1L
    private var asked = false

    /** Segments started so far. */
    var segment = 0
        private set
    /** Bytes written in the current file (samples; the index adds a little at the end). */
    var bytes = 0L
        private set

    fun videoFormat(format: MediaFormat) { videoFormat = format }
    fun audioFormat(format: MediaFormat) { audioFormat = format }
    /** No sound track (the sound encoder failed): files start with the video alone. */
    fun withoutAudio() { expectAudio = false }

    private fun ready() = videoFormat != null && (!expectAudio || audioFormat != null)

    /** An encoded picture; the first file starts on a key picture once both formats are known. */
    fun writeVideo(data: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (info.size <= 0 || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
        val key = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
        when {
            sink == null -> {
                if (!key || !ready()) { if (ready()) askKey(); return }
                start(info.presentationTimeUs)
            }
            bytes + info.size > maxBytes - HEADROOM -> {
                if (key) start(info.presentationTimeUs)
                else {
                    askKey()
                    // Never past the limit: a picture that does not fit waits for the key one.
                    if (bytes + info.size > maxBytes - INDEX_ROOM) return
                }
            }
        }
        if (info.presentationTimeUs <= lastVideoUs) return
        lastVideoUs = info.presentationTimeUs
        put(videoTrack, data, info)
    }

    /** Encoded sound; dropped before the first file and when it would not fit. */
    fun writeAudio(data: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (info.size <= 0 || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
        if (sink == null || audioTrack < 0 || info.presentationTimeUs < basePtsUs || info.presentationTimeUs <= lastAudioUs) return
        if (bytes + info.size > maxBytes - INDEX_ROOM) return
        lastAudioUs = info.presentationTimeUs
        put(audioTrack, data, info)
    }

    private fun askKey() {
        if (!asked) { asked = true; requestKeyFrame() }
    }

    private fun put(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) {
        val shifted = MediaCodec.BufferInfo().apply { set(info.offset, info.size, info.presentationTimeUs - basePtsUs, info.flags) }
        sink?.write(track, data, shifted)
        bytes += info.size
    }

    private fun start(ptsUs: Long) {
        close()
        segment++
        val s = open(segment)
        videoTrack = s.addTrack(videoFormat!!)
        audioTrack = audioFormat?.let { s.addTrack(it) } ?: -1
        s.start()
        sink = s
        bytes = 0L
        basePtsUs = ptsUs
        lastAudioUs = -1L
        lastVideoUs = -1L
        asked = false
        onSegment(segment, ptsUs)
    }

    /** Ends the current file (it stays playable). */
    fun finish() = close()

    private fun close() {
        val s = sink ?: return
        sink = null
        runCatching { s.stop() }
        runCatching { s.release() }
    }

    companion object {
        /** 20 MB per file (what a message can carry). */
        const val MAX_BYTES = 20_000_000L
        /** Room kept for the last pictures before the next key one, and the file's index. */
        const val HEADROOM = 1_500_000L
        const val INDEX_ROOM = 400_000L
    }
}
