package com.pocketstudio.separation

import java.io.File

/**
 * Stable Audio Open Small on the phone: a text prompt (already tokenized with T5) in, up to
 * [MAX_SECONDS] of interleaved stereo at 44.1 kHz out. Powered by Stability AI.
 * Takes seconds to a minute: call from a worker thread.
 */
object StableAudio {
    const val SAMPLE_RATE = 44_100
    const val MAX_SECONDS = 11f
    val FILES = listOf("cond_q8.onnx", "dit_l8.onnx", "dec.onnx")

    val isAvailable: Boolean get() = Demucs.isAvailable

    /** Null when cancelled or when the model failed (bad files, out of memory). */
    fun generate(modelDir: File, tokens: IntArray, seconds: Float, seed: Long, progress: Demucs.Progress): FloatArray? =
        nativeGenerate(modelDir.absolutePath, tokens, seconds.coerceIn(1f, MAX_SECONDS), seed, progress)

    private external fun nativeGenerate(dir: String, tokens: IntArray, seconds: Float, seed: Long, listener: Demucs.Progress): FloatArray?
}
