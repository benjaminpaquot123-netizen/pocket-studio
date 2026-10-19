package com.pocketstudio.engine

/**
 * Audio held by the native engine (float, 48 kHz after resampling). The engine keeps its own
 * reference while a pad uses it, so [close] is safe at any time; the memory goes when nobody uses it.
 */
class NativeSample private constructor(internal val handle: Long) : AutoCloseable {
    @Volatile private var closed = false

    /** Length in frames at 48 kHz. */
    val frames: Long = NativeEngine.sampleFrames(handle)

    /**
     * Where the attacks are, as fractions 0..1 of the sound (the first is 0). Blocking: worker
     * thread. [sensitivity] 0 keeps only big hits, 1 every small attack.
     */
    @Synchronized  // close() waits: the native data is never freed mid-analysis
    fun onsets(sensitivity: Float, maxSlices: Int = 16): FloatArray {
        check(!closed)
        return NativeEngine.sampleOnsets(handle, sensitivity, maxSlices)
    }

    /**
     * A new sound: the region [start]..[end] (fractions) lasting [outFrames] at 48 kHz, its pitch
     * moved by [semitones] without changing the length. Blocking: worker thread. Null if too short.
     */
    @Synchronized
    fun stretched(start: Float, end: Float, outFrames: Long, semitones: Float): NativeSample? {
        check(!closed)
        val h = NativeEngine.sampleStretched(handle, start, end, outFrames, semitones)
        return if (h == 0L) null else NativeSample(h)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        NativeEngine.sampleRelease(handle)
    }

    companion object {
        /** Resamples to 48 kHz off the main thread (it can take a moment). Null if the data is unusable. */
        fun create(interleaved: FloatArray, channels: Int, sampleRate: Int): NativeSample? {
            val handle = NativeEngine.sampleCreate(interleaved, channels, sampleRate)
            return if (handle == 0L) null else NativeSample(handle)
        }
    }
}
