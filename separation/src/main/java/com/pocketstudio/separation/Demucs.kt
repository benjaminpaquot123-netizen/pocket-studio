package com.pocketstudio.separation

import java.io.File

/** Order must match ps::SeparationResult::Code. */
enum class SeparationResult { Ok, ModelError, Cancelled, IoError, BadInput }

/**
 * HT-Demucs v4 on the phone: a song in, four stems out (drums, bass, other, vocals) as 32-bit
 * float WAV files at 44.1 kHz. Takes minutes: call from a worker thread (see the service).
 */
object Demucs {
    const val SAMPLE_RATE = 44_100
    val STEMS = listOf("drums", "bass", "other", "vocals")

    val isAvailable: Boolean = try {
        System.loadLibrary("onnxruntime")
        System.loadLibrary("pocketstudio_separation")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    fun interface Progress {
        /** 0..1; return false to cancel. */
        fun onProgress(fraction: Float): Boolean
    }

    /** [stereo44k] interleaved stereo at 44.1 kHz. */
    fun separate(model: File, stereo44k: FloatArray, outDir: File, progress: Progress): SeparationResult {
        outDir.mkdirs()
        return SeparationResult.entries[nativeSeparate(model.absolutePath, stereo44k, outDir.absolutePath, progress)]
    }

    /** [raw]: interleaved 32-bit float, [channels] 1 or 2, any [sampleRate] (resampled to 44.1 kHz). */
    fun separateFile(model: File, raw: File, channels: Int, sampleRate: Int, outDir: File, progress: Progress): SeparationResult {
        outDir.mkdirs()
        return SeparationResult.entries[nativeSeparateFile(model.absolutePath, raw.absolutePath, channels, sampleRate, outDir.absolutePath, progress)]
    }

    private external fun nativeSeparateFile(model: String, raw: String, channels: Int, sampleRate: Int, outDir: String, listener: Progress): Int
    private external fun nativeSeparate(model: String, audio: FloatArray, outDir: String, listener: Progress): Int
}
