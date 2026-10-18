package com.pocketstudio.engine

/**
 * Thin JNI facade over the C++ audio engine. All calls are cheap and may be made
 * from the main thread; heavy work (latency analysis) should run on a background dispatcher.
 */
object NativeEngine {
    private const val CMD_SET_PLAYING = 0
    private const val CMD_SET_FREQUENCY = 1
    private const val CMD_SET_GAIN = 2

    val isAvailable: Boolean = try {
        System.loadLibrary("pocketstudio_engine")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    fun setPlaying(playing: Boolean) = post(CMD_SET_PLAYING, if (playing) 1f else 0f)
    fun setFrequency(hz: Float) = post(CMD_SET_FREQUENCY, hz)
    fun setGain(gain: Float) = post(CMD_SET_GAIN, gain)

    fun streamInfo(): StreamInfo = StreamInfo.fromArray(nativeStreamInfo())

    fun latencyState(): LatencyState = LatencyState.entries[latencyTestState()]

    fun startLatencyTest(): LatencyStart = LatencyStart.entries[nativeStartLatencyTest()]

    fun latencyResult(): LatencyResult {
        val v = finishLatencyTest()
        return LatencyResult(
            valid = v[0] > 0.5,
            latencyMs = v[1],
            confidence = v[2],
            inputPeak = v[3].toFloat(),
            inputFrames = v[4].toInt(),
            failure = LatencyFailure.entries[v[5].toInt()],
        )
    }

    external fun start(): Boolean
    external fun stop()
    /** Microphone configuration used by the last latency test, in French, for reports. */
    external fun inputDescription(): String
    private external fun nativeStartLatencyTest(): Int
    private external fun post(type: Int, value: Float): Boolean
    private external fun nativeStreamInfo(): DoubleArray
    private external fun latencyTestState(): Int
    private external fun finishLatencyTest(): DoubleArray
}

enum class LatencyState { Idle, Running, Done }

/** Order must match AudioHost::LatencyStart. */
enum class LatencyStart { Ok, MicUnavailable, OutputFailed, NotForeground }

/** Order must match RoundTripMeter::Failure. */
enum class LatencyFailure { None, NoInput, SilentInput, LowConfidence }

data class LatencyResult(
    val valid: Boolean,
    val latencyMs: Double,
    val confidence: Double,
    val inputPeak: Float = 0f,
    val inputFrames: Int = 0,
    val failure: LatencyFailure = LatencyFailure.None,
)

data class StreamInfo(
    val sampleRate: Int = 0,
    val framesPerBurst: Int = 0,
    val bufferSize: Int = 0,
    val bufferCapacity: Int = 0,
    val exclusive: Boolean = false,
    val aaudio: Boolean = false,
    val mmap: Boolean = false,
    val lowLatency: Boolean = false,
    val xruns: Int = 0,
    val outputLatencyMs: Double = -1.0,
    val running: Boolean = false,
    val callbacks: Long = 0,
    val peak: Float = 0f,
    val nonFinite: Boolean = false,
) {
    companion object {
        fun fromArray(v: DoubleArray) = StreamInfo(
            sampleRate = v[0].toInt(),
            framesPerBurst = v[1].toInt(),
            bufferSize = v[2].toInt(),
            bufferCapacity = v[3].toInt(),
            exclusive = v[4] > 0.5,
            aaudio = v[5] > 0.5,
            mmap = v[6] > 0.5,
            lowLatency = v[7] > 0.5,
            xruns = v[8].toInt(),
            outputLatencyMs = v[9],
            running = v[10] > 0.5,
            callbacks = v[11].toLong(),
            peak = v[12].toFloat(),
            nonFinite = v[13] > 0.5,
        )
    }
}
