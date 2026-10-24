package com.pocketstudio.app.export.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.pocketstudio.model.video.VideoPlan
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Where the encoded samples go: an MP4 file ([MuxerSink]), or a recorder in the tests. */
interface MuxSink {
    fun addTrack(format: MediaFormat): Int
    fun start()
    fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo)
    fun stop()
    fun release()
}

/** Android's MP4 writer. */
class MuxerSink(file: File) : MuxSink {
    private val muxer = MediaMuxer(file.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    override fun addTrack(format: MediaFormat) = muxer.addTrack(format)
    override fun start() = muxer.start()
    override fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) = muxer.writeSampleData(track, data, info)
    override fun stop() = muxer.stop()
    override fun release() = muxer.release()
}

/**
 * The « vidéo 9:16 » written with Android's own encoders (no ffmpeg): H.264 pictures drawn on a
 * Canvas and handed over as YUV 4:2:0, AAC-LC sound, both in one MP4. The sound is encoded first
 * (30 s of AAC is under a megabyte, kept in memory), then the pictures; the two are written
 * interleaved by time. Blocking: call off the main thread.
 */
object VideoEncoder {
    const val VIDEO_BIT_RATE = 6_000_000
    const val AUDIO_BIT_RATE = 192_000

    /** One encoded AAC frame, kept until the pictures catch up with it. */
    class AudioSample(val data: ByteArray, val ptsUs: Long, val flags: Int)

    /**
     * Writes [plan]'s video into [sink]: [pcm] (interleaved stereo at the plan's rate, the window
     * already faded) as AAC, and [plan.videoFrames] pictures drawn by [draw] on a [width] ×
     * [height] canvas. [progress] goes from 0 to 1 over the pictures; [isCancelled] stops the run
     * (it throws [kotlinx.coroutines.CancellationException]).
     */
    fun encode(
        sink: MuxSink, plan: VideoPlan, pcm: FloatArray, draw: (Canvas, Int) -> Unit,
        width: Int = VideoPlan.WIDTH, height: Int = VideoPlan.HEIGHT,
        progress: (Float) -> Unit = {}, isCancelled: () -> Boolean = { false },
    ) {
        val (audioFormat, audio) = encodeAudio(pcm, plan.sampleRate, isCancelled)
        encodeVideo(sink, plan, audioFormat, audio, draw, width, height, progress, isCancelled)
    }

    /** [pcm] as AAC-LC, in memory: the encoder's output format and its frames in order. */
    fun encodeAudio(pcm: FloatArray, sampleRate: Int, isCancelled: () -> Boolean = { false }): Pair<MediaFormat, List<AudioSample>> {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val out = ArrayList<AudioSample>()
        var outFormat: MediaFormat? = null
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val frames = pcm.size / 2
            var at = 0
            val dither = Dither()
            // Never stuck (CodecPump): every output taken each round, a codec that stops answering fails.
            com.pocketstudio.app.export.CodecPump.run(
                com.pocketstudio.app.export.CodecPort.of(codec),
                feed = { i ->
                    val buf = codec.getInputBuffer(i)!!.order(ByteOrder.LITTLE_ENDIAN)
                    buf.clear()
                    val n = minOf(buf.capacity() / 4, frames - at)
                    val timeUs = at.toLong() * 1_000_000L / sampleRate
                    if (n <= 0) {
                        com.pocketstudio.app.export.CodecPump.Fed(0, timeUs, end = true)
                    } else {
                        for (s in 2 * at until 2 * (at + n)) buf.putShort(dither.to16(pcm[s]))
                        at += n
                        com.pocketstudio.app.export.CodecPump.Fed(n * 4, timeUs, end = false)
                    }
                },
                format = { outFormat = it },
                output = { data, info ->
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                        val bytes = ByteArray(info.size).also { data.get(it) }
                        out += AudioSample(bytes, info.presentationTimeUs, info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv())
                    }
                    false
                },
                isCancelled = isCancelled,
            )
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
        return (outFormat ?: format) to out
    }

    /** The colour layout the H.264 encoder takes: « flexible » (an Image per picture) when it says so, else planar or semi-planar. */
    private fun colorFormat(codec: MediaCodec): Int {
        val formats = runCatching { codec.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).colorFormats.toList() }.getOrNull()
            ?: return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        return listOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
        ).firstOrNull { it in formats } ?: MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
    }

    private fun encodeVideo(
        sink: MuxSink, plan: VideoPlan, audioFormat: MediaFormat, audio: List<AudioSample>, draw: (Canvas, Int) -> Unit,
        width: Int, height: Int, progress: (Float) -> Unit, isCancelled: () -> Boolean,
    ) {
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val color = colorFormat(codec)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, color)
            setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, plan.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val pixels = IntArray(width * height)
        val yuv = Yuv420(width, height)
        val total = plan.videoFrames
        var videoTrack = -1
        var audioTrack = -1
        var muxing = false
        var nextAudio = 0
        val audioInfo = MediaCodec.BufferInfo()
        fun writeAudioUpTo(ptsUs: Long) {
            while (muxing && nextAudio < audio.size && audio[nextAudio].ptsUs <= ptsUs) {
                val s = audio[nextAudio++]
                audioInfo.set(0, s.data.size, s.ptsUs, s.flags)
                sink.write(audioTrack, ByteBuffer.wrap(s.data), audioInfo)
            }
        }
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            var frame = 0
            // Never stuck (CodecPump): every output taken each round, a codec that stops answering fails.
            com.pocketstudio.app.export.CodecPump.run(
                com.pocketstudio.app.export.CodecPort.of(codec),
                feed = { i ->
                    if (frame >= total) {
                        com.pocketstudio.app.export.CodecPump.Fed(0, plan.ptsUs(frame), end = true)
                    } else {
                        draw(canvas, frame)
                        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                        yuv.fill(pixels)
                        // The buffer's own size first (an Image's rows can be wider than the picture:
                        // its data then runs past width × height × 1.5); asked before the Image, which it hands over to.
                        val capacity = runCatching { codec.getInputBuffer(i)?.capacity() }.getOrNull() ?: 0
                        val image = runCatching { codec.getInputImage(i) }.getOrNull()
                        val size = if (image != null) maxOf(width * height * 3 / 2, capacity) else width * height * 3 / 2
                        if (image != null) {
                            yuv.putImage(image)
                        } else {
                            val buf = codec.getInputBuffer(i)!!
                            buf.clear()
                            if (color == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) yuv.putSemiPlanar(buf) else yuv.putPlanar(buf)
                        }
                        val pts = plan.ptsUs(frame)
                        frame++
                        progress(frame.toFloat() / total)
                        com.pocketstudio.app.export.CodecPump.Fed(size, pts, end = false)
                    }
                },
                format = { f ->
                    if (!muxing) {
                        videoTrack = sink.addTrack(f)
                        audioTrack = sink.addTrack(audioFormat)
                        sink.start()
                        muxing = true
                    }
                },
                output = { data, info ->
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0  // already in the format
                    if (info.size > 0) {
                        if (!muxing) throw IOException("H.264 sans format")
                        writeAudioUpTo(info.presentationTimeUs)
                        info.flags = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv()
                        sink.write(videoTrack, data, info)
                    }
                    false
                },
                isCancelled = isCancelled,
            )
            writeAudioUpTo(Long.MAX_VALUE)
            if (!muxing) throw IOException("H.264 sans image")
            sink.stop()
        } finally {
            runCatching { codec.stop() }
            codec.release()
            bitmap.recycle()
        }
    }

    /** Float to 16 bits with a triangular dither of one step (no grain in the fade-out). */
    private class Dither {
        private var seed = 0x2545F491
        private fun uniform(): Float {
            seed = seed xor (seed shl 13); seed = seed xor (seed ushr 17); seed = seed xor (seed shl 5)
            return (seed.toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
        }
        fun to16(x: Float): Short = Math.round(x * 32767f + uniform() - uniform()).coerceIn(-32768, 32767).toShort()
    }
}
