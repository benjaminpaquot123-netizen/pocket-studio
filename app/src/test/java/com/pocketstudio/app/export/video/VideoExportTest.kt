package com.pocketstudio.app.export.video

import android.media.MediaCodec
import android.media.MediaFormat
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.export.WavPcm
import com.pocketstudio.model.video.VideoLevels
import com.pocketstudio.model.video.VideoPlan
import com.pocketstudio.model.video.VideoSection
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowMediaCodec
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.math.PI
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VideoExportTest {
    /** Copies what fits: the shadow codecs stand in for the phone's encoders. */
    private val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
        val n = minOf(input.remaining(), output.remaining())
        val slice = input.duplicate().also { it.limit(it.position() + n) }
        output.put(slice)
        input.position(input.position() + n)
    }

    @Before
    fun codecs() {
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_VIDEO_AVC, ShadowMediaCodec.CodecConfig(64 * 96 * 2, 64 * 96 * 2, passThrough))
    }

    @After
    fun clear() = ShadowMediaCodec.clearCodecs()

    /** What the encoders hand to the MP4 writer, in order. */
    private class Recorder : MuxSink {
        val formats = ArrayList<MediaFormat>()
        val samples = ArrayList<Triple<Int, Long, Int>>()  // track, pts, size
        var started = false
        var stopped = false
        override fun addTrack(format: MediaFormat): Int { check(!started); formats += format; return formats.size - 1 }
        override fun start() { started = true }
        override fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) {
            check(started && !stopped)
            assertEquals("no end-of-stream flag in a sample", 0, info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            samples += Triple(track, info.presentationTimeUs, info.size)
        }
        override fun stop() { stopped = true }
        override fun release() = Unit
    }

    private val plan = VideoPlan("Test", startFrame = 0, frames = 48_000, sections = listOf(VideoSection("Intro", 0.0, 0.5), VideoSection("Drop", 0.5, 1.0)))

    @Test
    fun soundAndPicturesGoIntoOneFileInTimeOrder() {
        val pcm = FloatArray(2 * 48_000) { i -> (0.5 * sin(2 * PI * 220 * (i / 2) / 48_000)).toFloat() }
        val levels = VideoLevels.of(plan, pcm)
        val renderer = VideoFrameRenderer(plan, levels, width = 64, height = 96)
        val sink = Recorder()
        var progress = 0f
        VideoEncoder.encode(sink, plan, pcm, renderer::draw, width = 64, height = 96, progress = { progress = it })
        assertTrue(sink.stopped)
        assertEquals(listOf("video/avc", "audio/mp4a-latm"), sink.formats.map { it.getString(MediaFormat.KEY_MIME) })
        val video = sink.samples.filter { it.first == 0 }
        val audio = sink.samples.filter { it.first == 1 }
        // One picture each 1/30 s, all of them.
        assertEquals(plan.videoFrames, video.size)
        assertEquals((0 until plan.videoFrames).map { plan.ptsUs(it) }, video.map { it.second })
        assertEquals(1f, progress, 0f)
        // The sound: every frame of it (4 bytes a stereo frame in, one sample per buffer here), in order.
        assertEquals(48_000L * 4, audio.sumOf { it.third.toLong() })
        assertEquals(audio.map { it.second }.sorted(), audio.map { it.second })
        assertTrue(audio.last().second < 1_000_000L)
        // Interleaved: no picture is written before the sound that plays with it.
        sink.samples.forEachIndexed { i, (track, pts, _) ->
            if (track == 0) assertTrue("audio before $pts", audio.none { a -> a.second <= pts && sink.samples.indexOf(a) > i })
        }
    }

    @Test
    fun aCancelStopsTheRun() {
        val pcm = FloatArray(2 * 48_000)
        val renderer = VideoFrameRenderer(plan, VideoLevels.of(plan, pcm), width = 64, height = 96)
        var frames = 0
        val sink = Recorder()
        val stopped = runCatching {
            VideoEncoder.encode(sink, plan, pcm, { c, i -> frames = i; renderer.draw(c, i) }, width = 64, height = 96, isCancelled = { frames >= 5 })
        }.exceptionOrNull()
        assertTrue(stopped is kotlinx.coroutines.CancellationException)
        assertTrue(!sink.stopped)
    }

    @Test
    fun theWindowIsCutWhereThePlanSaysAndFaded() {
        // Each left sample is its frame number (scaled), the right one its negative.
        val frames = 20_000
        val data = FloatArray(2 * frames) { i -> (if (i % 2 == 0) 1f else -1f) * (i / 2) / 1e6f }
        val wav = Files.createTempFile("song", ".wav").toFile()
        FloatWav.write(wav, data, 0, frames, 2, 48_000)
        val p = VideoPlan("x", startFrame = 4_800, frames = 9_600, sections = emptyList())
        val window = WavPcm.open(wav).use { windowOf(it, p) }
        wav.delete()
        assertEquals(2 * 9_600, window.size)
        // Frame 0 of the window is song frame 4800, faded in from silence.
        assertEquals(0f, window[0], 0f)
        val mid = 4_800
        assertEquals((4_800 + mid) / 1e6f, window[2 * mid], 1e-7f)
        assertEquals(-(4_800 + mid) / 1e6f, window[2 * mid + 1], 1e-7f)
        // The end fades to silence.
        assertTrue(kotlin.math.abs(window[2 * 9_599]) < 1e-4f)
    }

    @Test
    fun theFrameShowsThePlayheadAndThePart() {
        val pcm = FloatArray(2 * 48_000) { 0.4f }
        val renderer = VideoFrameRenderer(plan, VideoLevels.of(plan, pcm))
        val bmp = renderer.frame(25)
        assertEquals(720, bmp.width)
        assertEquals(1280, bmp.height)
        // The bar under the playhead (the middle one) is lit in the « playing » green.
        val colors = VideoColors.of()
        val x = 360
        assertEquals(colors.playing, bmp.getPixel(x, VideoFrameRenderer.WAVE_Y.toInt()))
        // Something is written where the part's name goes.
        val rows = (950..1000).flatMap { y -> (0 until 720).map { bmp.getPixel(it, y) } }
        assertTrue(rows.count { Yuv420.lumaOf(it) > 200 } > 500)
    }

    @Test
    fun picturesBecomeYuvAsEncodersExpect() {
        val yuv = Yuv420(4, 2)
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        yuv.fill(intArrayOf(white, white, black, black, white, white, black, black))
        assertEquals(235, yuv.y[0].toInt() and 0xFF)
        assertEquals(16, yuv.y[2].toInt() and 0xFF)
        assertEquals(128, yuv.u[0].toInt() and 0xFF)
        assertEquals(128, yuv.v[1].toInt() and 0xFF)
        val buf = ByteBuffer.allocate(12)
        yuv.putSemiPlanar(buf)
        assertEquals(12, buf.position())
    }
}
