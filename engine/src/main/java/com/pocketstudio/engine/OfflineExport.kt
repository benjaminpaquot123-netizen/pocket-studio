package com.pocketstudio.engine

import java.io.File

/** Order must match OfflineRenderer::Result. */
enum class ExportResult { Ok, IoError, Cancelled }

/**
 * A private engine rendered faster than real time into a 24-bit/48 kHz stereo WAV file.
 * Describe the project with [NativeEngine.captureInto], call [render] on a worker thread,
 * then [close]. [progress] and [cancel] are safe from any thread while rendering.
 */
class OfflineExport : AutoCloseable {
    internal val handle: Long = NativeEngine.exportCreate()
    @Volatile private var closed = false

    fun render(file: File, frames: Long, tailFrames: Long): ExportResult =
        ExportResult.entries[NativeEngine.exportRender(handle, file.absolutePath, frames, tailFrames)]

    /** Interleaved stereo at 48 kHz; empty if cancelled. Worker thread. */
    fun renderToArray(frames: Long, tailFrames: Long): FloatArray = NativeEngine.exportRenderFloat(handle, frames, tailFrames)

    val progress: Float get() = if (closed) 1f else NativeEngine.exportProgress(handle)

    fun cancel() {
        if (!closed) NativeEngine.exportCancel(handle)
    }

    /** Only once [render] has returned. */
    override fun close() {
        if (closed) return
        closed = true
        NativeEngine.exportDestroy(handle)
    }

    companion object {
        const val SAMPLE_RATE = 48000

        /** Frames for [bars] bars of 4/4 at [bpm] (swing keeps the bar length). */
        fun framesFor(bars: Int, bpm: Double): Long = Math.round(bars * 4 * 60.0 / bpm * SAMPLE_RATE)
    }
}
