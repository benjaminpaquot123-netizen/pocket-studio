package com.pocketstudio.engine

/**
 * A separated song held by the native engine: four stereo stems (drums, bass, other, vocals) at
 * 48 kHz in 16 bits. The engine keeps its own reference while it plays them, so [close] is safe
 * at any time.
 */
class NativeStems private constructor(internal val handle: Long, private val sink: CommandSink? = null) : AutoCloseable {
    @Volatile private var closed = false
    // [sink]: the test sink that made this bundle (as NativeSample), asked even once it is unset.

    /** Length in frames at 48 kHz. */
    val frames: Long = sink?.stemsFrames(handle) ?: NativeEngine.stemsFrames(handle)

    /** Interleaved stereo float of one stem's region (for a pad). Empty when out of range. */
    @Synchronized
    fun extract(stem: Int, start: Long, count: Long): FloatArray {
        check(!closed)
        return sink?.stemsExtract(handle, stem, start, count) ?: NativeEngine.stemsExtract(handle, stem, start, count)
    }

    /** Waveform overview of one stem: the peak (0..1) of each of [buckets] slices. Worker thread. */
    @Synchronized
    fun peaks(stem: Int, buckets: Int): FloatArray {
        check(!closed)
        return sink?.stemsPeaks(handle, stem, buckets) ?: NativeEngine.stemsPeaks(handle, stem, buckets)
    }

    /** A copy lasting [ratio] times as long, pitch kept. Blocking (seconds): worker thread. Null if it failed. */
    @Synchronized
    fun stretched(ratio: Double): NativeStems? {
        check(!closed)
        val h = sink?.stemsStretched(handle, ratio) ?: NativeEngine.stemsStretched(handle, ratio)
        return if (h == 0L) null else NativeStems(h, sink)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        if (sink?.stemsRelease(handle) == true) return
        NativeEngine.stemsRelease(handle)
    }

    companion object {
        /** Reads and resamples the four WAVs. Blocking (seconds): worker thread. Null if unreadable. */
        /** [data] (interleaved stereo at 48 kHz) as a bundle for the preview player ([NativeEngine.setPreviewStems]). */
        fun fromPcm(data: FloatArray): NativeStems? {
            val sink = NativeEngine.sink
            val mine = sink?.stemsFromPcm(data)
            val handle = mine ?: NativeEngine.stemsFromPcm(data)
            return if (handle == 0L) null else NativeStems(handle, sink.takeIf { mine != null })
        }

        fun load(drums: String, bass: String, other: String, vocals: String): NativeStems? {
            val paths = arrayOf(drums, bass, other, vocals)
            val sink = NativeEngine.sink
            val mine = sink?.stemsLoad(paths)
            val handle = mine ?: NativeEngine.stemsLoad(paths)
            return if (handle == 0L) null else NativeStems(handle, sink.takeIf { mine != null })
        }
    }
}
