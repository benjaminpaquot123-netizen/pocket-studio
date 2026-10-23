package com.pocketstudio.engine

/**
 * Where [NativeEngine]'s calls go instead of the native library while [NativeEngine.sink] is set:
 * tests and the song bench (`SongBenchExport` records a real export as a `.pscmd` file that
 * `engine/bench/ps_render` replays on Linux). Never set in the app. Handles are the sink's own
 * numbers (never 0); [target] 0 is the live engine, else an [OfflineExport]'s handle.
 */
interface CommandSink {
    /** [NativeEngine]'s commands (type, value, a, b), as the JNI posts them. */
    fun post(target: Long, type: Int, value: Float, a: Int, b: Int)

    /** [NativeEngine.captureAt]: later commands to [target] take effect at [frame] (-1: at once). */
    fun scheduleFrom(target: Long, frame: Long)

    /** A sound installed on pad / track / audio track [index] ([sample] 0: none), see [Install]. */
    fun install(target: Long, kind: Install, index: Int, sample: Long)

    /** [NativeSample.create]: the sink keeps the data; returns its handle (0: refused). */
    fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long
    fun sampleFrames(handle: Long): Long
    fun sampleRelease(handle: Long)

    /** A private export engine ([OfflineExport]). */
    fun exportCreate(): Long

    /**
     * [OfflineExport.render]: into a file (the bench writes the commands there). [sampleRate] and
     * [bits] are the file's format ([OfflineExport.render]'s, 32 bits for a float WAV).
     */
    fun exportRender(
        target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
        sampleRate: Int, bits: Int,
    ): ExportResult

    /** [OfflineExport.renderToArray]: interleaved stereo at 48 kHz (empty: cannot render). */
    fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray
    fun exportDestroy(target: Long)

    enum class Install { PadSample, TrackSample, AudioClip, Stems, Instrument, LiveClip, PreviewStems }
}
