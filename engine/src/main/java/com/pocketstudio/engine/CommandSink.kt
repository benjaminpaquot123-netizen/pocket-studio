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

    /** [OfflineExport.renderForReport]: rendered for its [exportReport] only (true: done). */
    fun exportRenderReportOnly(target: Long, frames: Long, tailFrames: Long): Boolean = exportRenderFloat(target, frames, tailFrames).isNotEmpty()
    fun exportDestroy(target: Long)

    /** [OfflineExport.report] of [target]'s last render, as the JNI lays it out (null: none). */
    fun exportReport(target: Long): FloatArray? = null

    // The live engine's state, as the app polls it while playing (a test moves it on by hand).
    fun sceneSwaps(): Int = 0
    fun songStep(): Int = -1
    /** [NativeEngine.monitorTrack] (null: the sink does not say). */
    fun monitorTrack(): Int? = null
    fun currentStep(): Int = -1
    fun absoluteStep(): Int = -1
    fun playPosition(): Double = -1.0
    fun transportStep(): Int = -1
    fun transportPosition(): Double = -1.0
    fun songMarkStep(): Int = -1
    /** Per slot: tag playing, tag waiting, start step (here: each playing the whole row, nothing waiting). */
    fun slotStates(out: IntArray) {
        for (k in 0 until out.size / 3) {
            out[k * 3] = NativeEngine.ROW_TAG
            out[k * 3 + 1] = NativeEngine.NO_LAUNCH
            out[k * 3 + 2] = 0
        }
    }
    fun drainLaunchEvents(out: IntArray): Int = 0

    // A separated song's stems ([NativeStems]), for the « testeur humain » bench. A null answer
    // means « not mine »: the native library is asked, as before these existed.
    /** [NativeStems.load]: the sink's handle for the four WAVs (0: unreadable), or null. */
    fun stemsLoad(paths: Array<String>): Long? = null
    fun stemsFromPcm(data: FloatArray): Long? = null
    fun stemsFrames(handle: Long): Long? = null
    fun stemsExtract(handle: Long, stem: Int, start: Long, count: Long): FloatArray? = null
    fun stemsPeaks(handle: Long, stem: Int, buckets: Int): FloatArray? = null
    fun stemsStretched(handle: Long, ratio: Double): Long? = null
    /** True when the sink let go of its bundle (else the native library is asked). */
    fun stemsRelease(handle: Long): Boolean = false
    /** [NativeEngine.stemPositionNow] / [NativeEngine.previewPositionNow], or null. */
    fun stemPosition(): Long? = null
    fun previewPosition(): Long? = null

    enum class Install { PadSample, TrackSample, AudioClip, Stems, Instrument, LiveClip, PreviewStems }
}
