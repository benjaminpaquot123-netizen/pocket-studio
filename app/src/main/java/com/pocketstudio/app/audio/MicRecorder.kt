package com.pocketstudio.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * Records the microphone (mono, 48 kHz float) for a pad. [record] blocks until [stop] or the
 * time limit; call it from a background dispatcher. [level] is the latest peak (0..1).
 */
class MicRecorder(private val maxSeconds: Int = 30) {
    @Volatile var level = 0f
        private set
    @Volatile private var stopRequested = false

    fun stop() {
        stopRequested = true
    }

    @SuppressLint("MissingPermission")  // checked by the screen before recording
    suspend fun record(onProgress: (seconds: Float) -> Unit): Pcm {
        val rate = 48_000
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val bufferBytes = maxOf(minBuf, 4096 * 4)
        // Raw input first (no automatic gain or noise gate), plain microphone otherwise.
        val rec = listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC).firstNotNullOfOrNull { source ->
            runCatching { AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, bufferBytes) }
                .getOrNull()
                ?.takeIf { it.state == AudioRecord.STATE_INITIALIZED || run { it.release(); false } }
        } ?: error("micro indisponible")
        val out = FloatArray(rate * maxSeconds)
        var size = 0
        val chunk = FloatArray(1024)
        try {
            rec.startRecording()
            while (!stopRequested && size < out.size && currentCoroutineContext().isActive) {
                val n = rec.read(chunk, 0, minOf(chunk.size, out.size - size), AudioRecord.READ_BLOCKING)
                if (n < 0) break
                var peak = 0f
                for (i in 0 until n) peak = maxOf(peak, abs(chunk[i]))
                level = peak
                chunk.copyInto(out, size, 0, n)
                size += n
                onProgress(size.toFloat() / rate)
            }
        } finally {
            runCatching { rec.stop() }
            rec.release()
        }
        return Pcm(trimSilence(out.copyOf(size)), 1, rate)
    }

    companion object {
        /** Cuts silence before the first sound so the hit starts right on the step. */
        fun trimSilence(data: FloatArray, threshold: Float = 0.02f): FloatArray {
            val start = data.indexOfFirst { abs(it) > threshold }
            if (start < 0) return data
            val from = maxOf(0, start - 48)  // keep 1 ms before the attack
            return data.copyOfRange(from, data.size)
        }
    }
}
