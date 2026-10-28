package com.pocketstudio.app.human

import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.OfflineExport
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * The phone's engine as far as the « testeur humain » can tell, for « Partager » (lot « tuto 4 »,
 * É0): Robolectric has no native engine, so the share said « le moteur audio n'est pas prêt »
 * where a phone exports. While the bench sets it as [com.pocketstudio.engine.NativeEngine.sink],
 * each render writes [SECONDS] s of a soft tone (a WAV the app then compresses, checks and shares
 * as it would a song), and the live engine's commands are let go. Never in the app.
 */
internal class BenchEngine : CommandSink {
    private var next = 1L

    /** Renders asked for since this engine was made (tests). */
    @Volatile var renders = 0
        private set

    override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
    override fun scheduleFrom(target: Long, frame: Long) = Unit
    override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
    @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
    override fun sampleFrames(handle: Long): Long = OfflineExport.SAMPLE_RATE.toLong()
    override fun sampleRelease(handle: Long) = Unit
    @Synchronized override fun exportCreate(): Long = next++
    override fun exportDestroy(target: Long) = Unit

    override fun exportRender(
        target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
        sampleRate: Int, bits: Int,
    ): ExportResult {
        renders++
        val n = SECONDS * sampleRate
        FloatWav.write(File(path), tone(n, sampleRate), 0, n, 2, sampleRate)
        return ExportResult.Ok
    }

    override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray =
        tone(minOf(frames + tailFrames, (SECONDS * OfflineExport.SAMPLE_RATE).toLong()).toInt().coerceAtLeast(1), OfflineExport.SAMPLE_RATE)

    companion object {
        const val SECONDS = 3

        /** [frames] of a quiet 220 Hz tone, stereo interleaved (−12 dBFS: no limiter needed). */
        fun tone(frames: Int, rate: Int): FloatArray = FloatArray(2 * frames) { i -> (0.25 * sin(2 * PI * 220.0 * (i / 2) / rate)).toFloat() }
    }
}
