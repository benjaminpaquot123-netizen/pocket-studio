package com.pocketstudio.separation

import java.io.File

/** One transcribed note: times in seconds, MIDI [pitch] (21..108), [amplitude] 0..1. */
data class PitchNote(val start: Double, val end: Double, val pitch: Int, val amplitude: Float)

/**
 * Audio to MIDI notes with Spotify's Basic Pitch (ONNX, polyphonic, any instrument). Takes a few
 * seconds per minute of audio: call from a worker thread. The model ships in the app assets
 * (`basic-pitch/nmp.onnx`) and must be copied to a file first; the native side checks its
 * SHA-256 against [MODEL_SHA256] before loading it.
 */
object BasicPitch {
    const val MODEL_ASSET = "basic-pitch/nmp.onnx"
    const val MODEL_SHA256 = "2c3c1d144bfa61ad236e92e169c13535c880469a12a047d4e73451f2c059a0ec"

    /** Same native library as [Demucs] (loads ONNX Runtime first). */
    val isAvailable: Boolean get() = Demucs.isAvailable

    /**
     * [mono] at any [sampleRate] (resampled to 22.05 kHz natively). Returns the notes sorted by
     * start then pitch, possibly empty. Throws [IllegalStateException] with a French message
     * when the model is missing or corrupt, the audio is unusable, or memory runs out.
     */
    fun transcribe(model: File, mono: FloatArray, sampleRate: Int): List<PitchNote> {
        check(isAvailable) { "moteur de transcription absent" }
        val flat = nativeTranscribe(model.absolutePath, mono, sampleRate)
            ?: throw IllegalStateException("transcription impossible")
        return List(flat.size / 4) { i ->
            PitchNote(flat[i * 4].toDouble(), flat[i * 4 + 1].toDouble(), flat[i * 4 + 2].toInt(), flat[i * 4 + 3])
        }
    }

    private external fun nativeTranscribe(model: String, mono: FloatArray, sampleRate: Int): FloatArray?
}
