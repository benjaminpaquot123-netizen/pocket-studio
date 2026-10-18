package com.pocketstudio.engine

/**
 * Thin JNI facade over the C++ audio engine. All calls are cheap and may be made
 * from the main thread; heavy work (latency analysis) should run on a background dispatcher.
 */
object NativeEngine {
    private const val CMD_SET_PLAYING = 0
    private const val CMD_SET_FREQUENCY = 1
    private const val CMD_SET_GAIN = 2
    // 3, 4: latency test (native only)
    private const val CMD_SET_TRANSPORT = 5
    private const val CMD_SET_TEMPO = 6
    private const val CMD_SET_SWING = 7
    private const val CMD_SET_METRONOME = 8
    private const val CMD_SET_PATTERN_STEPS = 9
    private const val CMD_SET_DRUM_HIT = 10
    private const val CMD_TRIGGER_PAD = 11
    private const val CMD_SET_ACID_STEP = 12
    private const val CMD_SET_ACID_LENGTH = 13
    private const val CMD_SET_ACID_ROOT = 14
    private const val CMD_SET_ACID_PARAM = 15
    private const val CMD_SYNTH_NOTE_ON = 16
    private const val CMD_SYNTH_NOTE_OFF = 17
    private const val CMD_SYNTH_PARAM = 18
    private const val CMD_SYNTH_ALL_OFF = 19
    private const val CMD_CHANNEL_VOLUME = 20
    private const val CMD_CHANNEL_PAN = 21
    private const val CMD_CHANNEL_MUTE = 22
    private const val CMD_CHANNEL_SOLO = 23

    /** Mixer channels: 0 drums, 1 acid bass, 2 synth. */
    const val MIXER_CHANNELS = 3

    const val LANES = 16

    val isAvailable: Boolean = try {
        System.loadLibrary("pocketstudio_engine")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    fun setPlaying(playing: Boolean) = post(CMD_SET_PLAYING, if (playing) 1f else 0f)
    fun setFrequency(hz: Float) = post(CMD_SET_FREQUENCY, hz)
    fun setGain(gain: Float) = post(CMD_SET_GAIN, gain)

    fun play(countInBars: Int = 0) = post(CMD_SET_TRANSPORT, 1f, countInBars)
    fun stopTransport() = post(CMD_SET_TRANSPORT, 0f)
    fun setTempo(bpm: Double) = post(CMD_SET_TEMPO, bpm.toFloat())
    fun setSwing(swing: Double) = post(CMD_SET_SWING, swing.toFloat())
    fun setMetronome(on: Boolean) = post(CMD_SET_METRONOME, if (on) 1f else 0f)
    fun setPatternSteps(steps: Int) = post(CMD_SET_PATTERN_STEPS, 0f, steps)
    /** velocity 0 removes the hit. */
    fun setDrumHit(lane: Int, step: Int, velocity: Float) = post(CMD_SET_DRUM_HIT, velocity, lane, step)
    /** [packed] = AcidStep.pack() from project-model (same layout as the engine). */
    fun setAcidStep(index: Int, packed: Int) = post(CMD_SET_ACID_STEP, 0f, index, packed)
    fun setAcidLength(steps: Int) = post(CMD_SET_ACID_LENGTH, 0f, steps)
    fun setAcidRoot(midiNote: Int) = post(CMD_SET_ACID_ROOT, 0f, midiNote)
    /** [param] in AcidTrack::Param order, [value] 0..1. */
    fun setAcidParam(param: Int, value: Float) = post(CMD_SET_ACID_PARAM, value, param)

    fun synthNoteOn(note: Int, velocity: Float) = post(CMD_SYNTH_NOTE_ON, velocity, note)
    fun synthNoteOff(note: Int) = post(CMD_SYNTH_NOTE_OFF, 0f, note)
    /** [param] in Synth::Param order, [value] 0..1. */
    fun setSynthParam(param: Int, value: Float) = post(CMD_SYNTH_PARAM, value, param)
    fun synthAllOff() = post(CMD_SYNTH_ALL_OFF, 0f)

    fun setChannelVolume(ch: Int, v: Float) = post(CMD_CHANNEL_VOLUME, v, ch)
    fun setChannelPan(ch: Int, pan: Float) = post(CMD_CHANNEL_PAN, pan, ch)
    fun setChannelMute(ch: Int, on: Boolean) = post(CMD_CHANNEL_MUTE, if (on) 1f else 0f, ch)
    fun setChannelSolo(ch: Int, on: Boolean) = post(CMD_CHANNEL_SOLO, if (on) 1f else 0f, ch)
    /** Writes into [out] (size 5): peaks of drums, acid, synth, master, then limiter gain. */
    external fun mixerMeters(out: FloatArray)

    /** Plays a pad right now (live). */
    fun triggerPad(lane: Int, velocity: Float) = post(CMD_TRIGGER_PAD, velocity, lane)

    /** Current pattern step, or -1 when stopped or counting in. */
    external fun currentStep(): Int
    /** Writes into [out] (size [LANES]) how many times each lane has fired. */
    external fun laneTriggers(out: IntArray)

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
    // When non-zero, commands go to an offline export engine instead of the live one.
    private var captureTarget = 0L

    private fun post(type: Int, value: Float, a: Int = 0, b: Int = 0): Boolean {
        val target = captureTarget
        if (target != 0L) {
            exportPost(target, type, value, a, b)
            return true
        }
        return nativePost(type, value, a, b)
    }

    /**
     * Runs [block] (on the main thread, like every other command) with all commands sent
     * to [export]'s private engine, so the same sync code describes the project to both.
     */
    fun captureInto(export: OfflineExport, block: () -> Unit) {
        check(captureTarget == 0L)
        captureTarget = export.handle
        try {
            block()
        } finally {
            captureTarget = 0L
        }
    }

    internal external fun exportCreate(): Long
    private external fun exportPost(handle: Long, type: Int, value: Float, a: Int, b: Int)
    internal external fun exportRender(handle: Long, path: String, frames: Long, tail: Long): Int
    internal external fun exportProgress(handle: Long): Float
    internal external fun exportCancel(handle: Long)
    internal external fun exportDestroy(handle: Long)
    private external fun nativePost(type: Int, value: Float, a: Int, b: Int): Boolean
    private external fun nativeStreamInfo(): DoubleArray
    private external fun latencyTestState(): Int
    private external fun finishLatencyTest(): DoubleArray
}

enum class LatencyState { Idle, Running, Done }

/** Order must match AudioHost::LatencyStart. */
enum class LatencyStart { Ok, MicUnavailable, OutputFailed, NotForeground, MicSilent }

/** Order must match RoundTripMeter::Failure. */
enum class LatencyFailure { None, NoInput, SilentInput, LowConfidence, Interrupted }

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
