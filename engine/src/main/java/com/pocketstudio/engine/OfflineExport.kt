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

    /**
     * [fadeOutFrames]: the file's last frames fade to silence (a song's ending); 0: none.
     * [autoTail]: [tailFrames] is a cap, the tail ends once the sound has died away (under −90 dBFS).
     * [floatWav]: 32-bit float samples (a mix without limiter keeps its peaks above 0 dBFS).
     */
    fun render(file: File, frames: Long, tailFrames: Long, fadeOutFrames: Long = 0, autoTail: Boolean = false, floatWav: Boolean = false): ExportResult {
        NativeEngine.exportSetFadeOut(handle, fadeOutFrames)
        NativeEngine.exportSetOptions(handle, autoTail, floatWav)
        return ExportResult.entries[NativeEngine.exportRender(handle, file.absolutePath, frames, tailFrames)]
    }

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

        /** Frames for [bars] bars of [beatsPerBar] beats at [bpm] (swing keeps the bar length). */
        fun framesFor(bars: Int, bpm: Double, beatsPerBar: Int = 4): Long = Math.round(bars * beatsPerBar * 60.0 / bpm * SAMPLE_RATE)
    }
}
