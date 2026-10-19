package com.pocketstudio.engine

/**
 * Audio held by the native engine (float, 48 kHz after resampling). The engine keeps its own
 * reference while a pad uses it, so [close] is safe at any time; the memory goes when nobody uses it.
 */
class NativeSample private constructor(internal val handle: Long) : AutoCloseable {
    @Volatile private var closed = false

    /** Length in frames at 48 kHz. */
    val frames: Long = NativeEngine.sampleFrames(handle)

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
