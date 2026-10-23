package com.pocketstudio.engine

import java.io.File

/** Order must match OfflineRenderer::Result. */
enum class ExportResult { Ok, IoError, Cancelled }

/**
 * A private engine rendered faster than real time into a stereo WAV file (24-bit/48 kHz unless
 * asked otherwise). Describe the project with [NativeEngine.captureInto], call [render] on a
 * worker thread, read [report], then [close]. [progress] and [cancel] are safe from any thread
 * while rendering.
 */
class OfflineExport : AutoCloseable {
    // Tests and the song bench: a sink stands in for the native engine (see CommandSink).
    private val sink = NativeEngine.sink
    internal val handle: Long = sink?.exportCreate() ?: NativeEngine.exportCreate()
    @Volatile private var closed = false

    /**
     * [fadeOutFrames]: the file's last frames fade to silence (a song's ending); 0: none.
     * [autoTail]: [tailFrames] is a cap, the tail ends once the sound has died away (under −90 dBFS).
     * [floatWav]: 32-bit float samples (a mix without limiter keeps its peaks above 0 dBFS).
     * [sampleRate] 48000 or 44100 (resampled after the master), [bits] 24 or 16 (dithered); ignored with [floatWav].
     */
    fun render(
        file: File, frames: Long, tailFrames: Long, fadeOutFrames: Long = 0, autoTail: Boolean = false, floatWav: Boolean = false,
        sampleRate: Int = SAMPLE_RATE, bits: Int = 24,
    ): ExportResult {
        sink?.let {
            return it.exportRender(handle, file.absolutePath, frames, tailFrames, fadeOutFrames, autoTail, floatWav, sampleRate, if (floatWav) 32 else bits)
        }
        NativeEngine.exportSetFadeOut(handle, fadeOutFrames)
        NativeEngine.exportSetOptions(handle, autoTail, floatWav)
        NativeEngine.exportSetFormat(handle, sampleRate, if (floatWav) 32 else bits)
        return ExportResult.entries[NativeEngine.exportRender(handle, file.absolutePath, frames, tailFrames)]
    }

    /** What the last [render] wrote, measured on the file's own samples (fades and resampling included). */
    fun report(): ExportReport? = if (closed) null else ExportReport.of(NativeEngine.exportReport(handle))

    /** Interleaved stereo at 48 kHz; empty if cancelled. Worker thread. */
    fun renderToArray(frames: Long, tailFrames: Long): FloatArray =
        sink?.exportRenderFloat(handle, frames, tailFrames) ?: NativeEngine.exportRenderFloat(handle, frames, tailFrames)

    val progress: Float get() = if (closed || sink != null) 1f else NativeEngine.exportProgress(handle)

    fun cancel() {
        if (!closed && sink == null) NativeEngine.exportCancel(handle)
    }

    /** Only once [render] has returned. */
    override fun close() {
        if (closed) return
        closed = true
        sink?.let { it.exportDestroy(handle); return }
        NativeEngine.exportDestroy(handle)
    }

    companion object {
        const val SAMPLE_RATE = 48000
        const val CD_RATE = 44100

        /** Frames for [bars] bars of [beatsPerBar] beats at [bpm] (swing keeps the bar length). */
        fun framesFor(bars: Int, bpm: Double, beatsPerBar: Int = 4): Long = Math.round(bars * beatsPerBar * 60.0 / bpm * SAMPLE_RATE)
    }
}

/**
 * The end-of-export report: integrated loudness in LUFS (null for silence), true peak in dBTP,
 * length in seconds, sample rate, and the gain the loudness target added (dB, 0 without one).
 */
data class ExportReport(val lufs: Double?, val truePeakDb: Double, val seconds: Double, val sampleRate: Int, val driveDb: Double) {
    companion object {
        fun of(values: FloatArray): ExportReport? = if (values.size < 5) null else ExportReport(
            lufs = values[NativeEngine.REPORT_LUFS].toDouble().takeIf { it > -100.0 },
            truePeakDb = values[NativeEngine.REPORT_TRUE_PEAK].toDouble(),
            seconds = values[NativeEngine.REPORT_SECONDS].toDouble(),
            sampleRate = values[NativeEngine.REPORT_RATE].toInt(),
            driveDb = values[NativeEngine.REPORT_DRIVE].toDouble(),
        )
    }
}
