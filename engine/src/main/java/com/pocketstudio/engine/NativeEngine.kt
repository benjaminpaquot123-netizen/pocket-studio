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
    // 24: InstallSample (native only)
    private const val CMD_PAD_PARAM = 25
    private const val CMD_SYNTH_NOTE = 26
    private const val CMD_SYNTH_NOTE_COUNT = 27
    private const val CMD_SYNTH_STEPS = 28
    // 29: SetInstrument, 30: InstallStems (native only)
    private const val CMD_STEM_PARAM = 31
    private const val CMD_STEM_START = 32
    private const val CMD_INSERT_TYPE = 33
    private const val CMD_INSERT_PARAM = 34
    private const val CMD_SEND = 35
    private const val CMD_BUS_PARAM = 36
    private const val CMD_AUTOMATION_TARGET = 37
    private const val CMD_AUTOMATION_POINT = 38
    private const val CMD_STAGING = 39
    private const val CMD_ARM_SCENE_SWAP = 40
    private const val CMD_PERFORMANCE = 41
    private const val CMD_SYNTH_ENGINE = 42
    private const val CMD_PLAITS_PARAM = 43
    // 44: InstallClip (native only)
    private const val CMD_CLIP_PARAM = 45
    private const val CMD_TOGGLE_CLIP = 46
    private const val CMD_PLAY_PREVIEW = 47
    private const val CMD_PREVIEW_PARAM = 48
    private const val CMD_METRONOME_LEVEL = 49
    private const val CMD_DRUM_REPEAT = 50
    private const val CMD_TRACK_ON = 51
    // 52: InstallAudioClip (native only)
    private const val CMD_AUDIO_CLIP_PARAM = 53
    // 54: RecordArm, 55: RecordStop (native only)
    private const val CMD_AUDIO_ON = 56
    private const val CMD_BEATS_PER_BAR = 57
    private const val CMD_AUTOMATION_MODE = 58
    private const val CMD_AUTOMATION_SONG = 59
    private const val CMD_EXPRESSION = 60
    private const val CMD_PAD_PAN = 61
    private const val CMD_DRUM_STEP_DETAIL = 62
    private const val CMD_MACRO = 63
    // 64: InstallTrackSample (native only)
    private const val CMD_TRACK_SAMPLE_PARAM = 65
    private const val CMD_PAD_STRIP = 66

    // Kinds of [setExpression] (InstrumentTrack::Expression).
    const val EXPR_BEND = 0
    const val EXPR_MOD = 1
    const val EXPR_SUSTAIN = 2
    const val EXPR_ARP_ON = 3
    const val EXPR_ARP_RATE = 4
    const val EXPR_ARP_MODE = 5
    const val EXPR_ARP_OCTAVES = 6
    const val EXPR_ARP_GATE = 7

    /** Live pads (ClipLauncher::kSlots). */
    const val LIVE_PADS = 16

    /** Insert slots per channel (and on the master, whose channel index is [MASTER]). */
    const val INSERTS = 4

    /**
     * Mixer channels: 0 drums, 1 acid bass, 2 synth (instrument track 0), 3 remix stems, 5..11
     * instrument tracks 1..7, 12..15 audio tracks 0..3. Index 4 is the master (inserts, automation, meters).
     */
    /** 16 track strips (4 = the master's index) and the 16 drum pads' own strips. */
    const val MIXER_CHANNELS = 32
    const val MASTER = 4

    /** Instrument tracks (the synth track is track 0). */
    const val TRACKS = 8

    /** Audio tracks (AudioTracks::kTracks). */
    const val AUDIO_TRACKS = 4

    fun audioChannel(track: Int) = 12 + track

    /** The mixer channel of instrument track [track]. */
    fun trackChannel(track: Int) = if (track == 0) 2 else 4 + track

    // Synth commands name their track in bits 16..23 of `a` (see Engine.h).
    private fun onTrack(track: Int, a: Int) = (a and 0xFFFF) or (track.coerceIn(0, TRACKS - 1) shl 16)

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
    /** [repeats]: hits in the step (1 = plain, 2..4 = a roll), carried in one command with the hit. */
    fun setDrumHit(lane: Int, step: Int, velocity: Float, repeats: Int = 1) =
        post(CMD_SET_DRUM_HIT, velocity, lane, step or (repeats.coerceIn(1, 4) shl 16))
    /** A pad's place in the stereo field, -1 (left) .. 1 (right). */
    fun setPadPan(lane: Int, pan: Float) = post(CMD_PAD_PAN, pan, lane)
    /**
     * After [setDrumHit] (which resets them): this hit's pan, added to the pad's, and [pitch]
     * semitones (-24..24, sampled sounds only).
     */
    fun setDrumStepDetail(lane: Int, step: Int, pan: Float, pitch: Int) =
        post(CMD_DRUM_STEP_DETAIL, pan, lane, step or ((pitch.coerceIn(-24, 24) + 64) shl 16))
    /** Hits in the step: 1 = plain, 2..4 = a roll. */
    fun setDrumRepeat(lane: Int, step: Int, repeats: Int) = post(CMD_DRUM_REPEAT, repeats.toFloat(), lane, step)
    /** [packed] = AcidStep.pack() from project-model (same layout as the engine). */
    fun setAcidStep(index: Int, packed: Int) = post(CMD_SET_ACID_STEP, 0f, index, packed)
    fun setAcidLength(steps: Int) = post(CMD_SET_ACID_LENGTH, 0f, steps)
    fun setAcidRoot(midiNote: Int) = post(CMD_SET_ACID_ROOT, 0f, midiNote)
    /** [param] in AcidTrack::Param order, [value] 0..1. */
    fun setAcidParam(param: Int, value: Float) = post(CMD_SET_ACID_PARAM, value, param)

    fun synthNoteOn(note: Int, velocity: Float, track: Int = 0) = post(CMD_SYNTH_NOTE_ON, velocity, onTrack(track, note))
    fun synthNoteOff(note: Int, track: Int = 0) = post(CMD_SYNTH_NOTE_OFF, 0f, onTrack(track, note))
    /** [param] in Synth::Param order, [value] 0..1. */
    fun setSynthParam(param: Int, value: Float, track: Int = 0) = post(CMD_SYNTH_PARAM, value, onTrack(track, param))
    /** Instrument track [track] (1..7) in use or not: a track not in use is silent and costs nothing. */
    fun setTrackOn(track: Int, on: Boolean) = post(CMD_TRACK_ON, if (on) 1f else 0f, track)
    fun synthAllOff() = post(CMD_SYNTH_ALL_OFF, 0f)

    fun setChannelVolume(ch: Int, v: Float) = post(CMD_CHANNEL_VOLUME, v, ch)
    fun setChannelPan(ch: Int, pan: Float) = post(CMD_CHANNEL_PAN, pan, ch)
    fun setChannelMute(ch: Int, on: Boolean) = post(CMD_CHANNEL_MUTE, if (on) 1f else 0f, ch)
    fun setChannelSolo(ch: Int, on: Boolean) = post(CMD_CHANNEL_SOLO, if (on) 1f else 0f, ch)
    /** [ch] 0..MIXER_CHANNELS (the last is the master); [type] in EffectType order. */
    fun setInsertType(ch: Int, slot: Int, type: Int) = post(CMD_INSERT_TYPE, 0f, ch * INSERTS + slot, type)
    fun setInsertParam(ch: Int, slot: Int, param: Int, value: Float) = post(CMD_INSERT_PARAM, value, ch * INSERTS + slot, param)
    /** Post-fader send of [ch] to [bus] (0 reverb, 1 delay), 0..1. */
    fun setSend(ch: Int, bus: Int, level: Float) = post(CMD_SEND, level, ch, bus)
    /** A track's quick control (Macros: colour, dirt, motion, space), 0..1. */
    fun setMacro(ch: Int, macro: Int, value: Float) = post(CMD_MACRO, value, ch, macro)
    fun setBusParam(bus: Int, param: Int, value: Float) = post(CMD_BUS_PARAM, value, bus, param)
    /** Lane 0..7 follows [packedTarget] (AutoTarget.pack(), -1 = off); clears its points. */
    fun setAutomationTarget(lane: Int, packedTarget: Int) = post(CMD_AUTOMATION_TARGET, 0f, lane, packedTarget)
    /** [value] 0..1 on [step], or null for no point. */
    fun setAutomationPoint(lane: Int, step: Int, value: Float?) = post(CMD_AUTOMATION_POINT, value ?: -1f, lane, step)
    /** While on, pattern commands (drums, acid, piano roll) fill the waiting scene. */
    fun setStaging(on: Boolean) = post(CMD_STAGING, if (on) 1f else 0f)
    /** The waiting scene takes over after [steps] steps of the playing one (0 = next bar, -1 = cancel). */
    /** [restartsSong]: the waiting scene is the song's first part again (song automation restarts). */
    fun armSceneSwap(steps: Int, restartsSong: Boolean = false) = post(CMD_ARM_SCENE_SWAP, 0f, steps, if (restartsSong) 1 else 0)
    /** Steps since the song (re)started, as song automation reads them; -1 when stopped. */
    external fun songStep(): Int
    /** Live effects on the master: [param] 0 filter (0.5 = off), 1 resonance, 2 repeat on, 3 repeat division, 4 tape stop, 5 chop. */
    fun setPerformance(param: Int, value: Float) = post(CMD_PERFORMANCE, value, param)
    /** The synth track's own voice: 0 subtractive, 1 Plaits, 2 a sample played across the keys ([setTrackSample]). */
    fun setSynthEngine(engine: Int, track: Int = 0) = post(CMD_SYNTH_ENGINE, 0f, onTrack(track, engine))
    /** [param] in PlaitsSynth::Param order (0 model, 1 harmonics … 7 level), [value] 0..1. */
    fun setPlaitsParam(param: Int, value: Float, track: Int = 0) = post(CMD_PLAITS_PARAM, value, onTrack(track, param))
    /** How many times a waiting scene took over since the engine started. */
    external fun sceneSwaps(): Int
    /** Writes into [out] (size MIXER_CHANNELS + 1): channel peaks (the master's at [MASTER]), then the limiter gain. */
    external fun mixerMeters(out: FloatArray)

    /** Plays a pad right now (live). */
    fun triggerPad(lane: Int, velocity: Float) = post(CMD_TRIGGER_PAD, velocity, lane)

    // Live input (MIDI thread): always the live engine, never an export being captured.
    // The native side serialises producers, so any thread may call these.
    /** The instrument track a MIDI keyboard plays (the one shown in the synth screen). */
    @Volatile var liveTrack = 0
    fun liveNoteOn(note: Int, velocity: Float, track: Int = liveTrack) = nativePost(CMD_SYNTH_NOTE_ON, velocity, onTrack(track, note), 0)
    fun liveNoteOff(note: Int, track: Int = liveTrack) = nativePost(CMD_SYNTH_NOTE_OFF, 0f, onTrack(track, note), 0)
    fun liveTriggerPad(lane: Int, velocity: Float) = nativePost(CMD_TRIGGER_PAD, velocity, lane, 0)
    /** Pitch bend (semitones), mod wheel (0..1), sustain (0/1) or arpeggiator setting of a track. */
    fun liveExpression(kind: Int, value: Float, track: Int = liveTrack) = nativePost(CMD_EXPRESSION, value, onTrack(track, kind), 0)

    /** Current pattern step, or -1 when stopped or counting in. */
    external fun currentStep(): Int
    /** Steps since play without wrapping (-1 stopped): the piano roll loops on its own length. */
    external fun absoluteStep(): Int

    /**
     * Position in the playing scene in sixteenths, with the fraction (live recording). While the
     * count-in clicks, negative: the sixteenths left before the first step. -1 when stopped.
     */
    external fun playPosition(): Double

    /** Click volume 0..1 (0.5 = default). */
    fun setMetronomeLevel(level: Float) = post(CMD_METRONOME_LEVEL, level.coerceIn(0f, 1f))

    /** Output buffer in bursts: 2 = fast (lowest latency), 4 = safe, 6 = very safe. Applies at once. */
    external fun setBufferBursts(bursts: Int)

    /** Piano-roll note [index] (ticks: 24 per sixteenth). Packing matches NoteTrack::pack. */
    fun setSynthNote(index: Int, start: Int, length: Int, pitch: Int, velocity: Float, track: Int = 0) =
        post(CMD_SYNTH_NOTE, velocity, onTrack(track, index), (start and 0xFFF) or ((length and 0xFFF) shl 12) or ((pitch and 0x7F) shl 24))
    fun setSynthNoteCount(count: Int, track: Int = 0) = post(CMD_SYNTH_NOTE_COUNT, 0f, onTrack(track, count))
    fun setSynthSteps(steps: Int, track: Int = 0) = post(CMD_SYNTH_STEPS, 0f, onTrack(track, steps))
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

    /** True inside [captureInto]: commands go to an export engine, not the live one. */
    val isCapturing: Boolean get() = captureTarget != 0L

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

    /**
     * Inside [captureInto] only: the commands that follow take effect at [frame] of the rendered
     * song (-1: at once). A song export stages each next scene while the previous one plays.
     */
    fun captureAt(frame: Long) {
        val target = captureTarget
        check(target != 0L) { "captureAt outside captureInto" }
        exportScheduleFrom(target, frame)
    }

    /** Gives pad [pad] the sample (or back to its drum sound with null). Main thread, like every command. */
    fun setPadSample(pad: Int, sample: NativeSample?) {
        val target = captureTarget
        if (target != 0L) exportInstallSample(target, pad, sample?.handle ?: 0L)
        else nativeInstallSample(pad, sample?.handle ?: 0L)
    }

    /** The remix track plays [stems] from step 0 (null: no song). Main thread, like every command. */
    fun setStems(stems: NativeStems?) {
        val target = captureTarget
        if (target != 0L) exportInstallStems(target, stems?.handle ?: 0L)
        else nativeInstallStems(stems?.handle ?: 0L)
    }

    /** [param]: 0 gain (0..1, 0.8 = unity), 1 mute, 2 solo. */
    fun setStemParam(stem: Int, param: Int, value: Float) = post(CMD_STEM_PARAM, value, stem, param)

    /** Song frame (48 kHz) heard on step 0. */
    fun setStemStart(frame: Long) = post(CMD_STEM_START, 0f, frame.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())

    /** Song position in frames while the stems play, else -1. */
    external fun stemPosition(): Long

    /** The remix screen's own listening: [stems] play apart from the transport (null: none). Main thread. */
    fun setPreviewStems(stems: NativeStems?) = nativeInstallPreviewStems(stems?.handle ?: 0L)
    fun playPreview(on: Boolean) = post(CMD_PLAY_PREVIEW, if (on) 1f else 0f)
    /** As [setStemParam], for the preview. */
    fun setPreviewParam(stem: Int, param: Int, value: Float) = post(CMD_PREVIEW_PARAM, value, stem, param)
    /** Preview position in frames while it plays, else -1. */
    external fun previewPosition(): Long

    /** Live pad [pad] plays [sample] (null: empty). Main thread, like every command. */
    fun setClip(pad: Int, sample: NativeSample?) = nativeInstallClip(pad, sample?.handle ?: 0L)

    /** Frames [readScope] fills (Engine::kScopeFrames). */
    const val SCOPE_FRAMES = 4096
    /** The last [SCOPE_FRAMES] of the output (mono) or of the microphone, oldest first. */
    external fun readScope(microphone: Boolean, out: FloatArray)
    /** Microphone on for the tuner (needs the permission); false when unavailable. Blocking (up to 1 s). */
    external fun micOpen(): Boolean
    external fun micClose()

    /** A song part switches audio track [track] on or off (follows [setStaging]). */
    fun setAudioOn(track: Int, on: Boolean) = post(CMD_AUDIO_ON, if (on) 1f else 0f, track)

    /** Automation lane [lane]: 0 loop, 1 per scene, 2 song ([songBars] values sent with [setAutomationSong]). */
    fun setAutomationMode(lane: Int, mode: Int, songBars: Int) = post(CMD_AUTOMATION_MODE, songBars.toFloat(), lane, mode)
    fun setAutomationSong(lane: Int, bar: Int, value: Float) = post(CMD_AUTOMATION_SONG, value, lane, bar)

    /** Time signature [beats]/4 (2..7): count-in, bar lines, metronome accent, live pads grid. */
    fun setBeatsPerBar(beats: Int) = post(CMD_BEATS_PER_BAR, 0f, beats)

    /** Audio track [track] plays [sample] (null: nothing). */
    fun setAudioClip(track: Int, sample: NativeSample?) {
        val target = captureTarget
        if (target != 0L) exportInstallAudioClip(target, track, sample?.handle ?: 0L)
        else nativeInstallAudioClip(track, sample?.handle ?: 0L)
    }

    /** Where and how the clip plays: from step [start], again every [loopSteps], fades in frames at 48 kHz. */
    fun setAudioClipParams(track: Int, start: Int, loopSteps: Int, gain: Float, fadeInFrames: Int, fadeOutFrames: Int) {
        post(CMD_AUDIO_CLIP_PARAM, start.toFloat(), track, 0)
        post(CMD_AUDIO_CLIP_PARAM, loopSteps.toFloat(), track, 1)
        post(CMD_AUDIO_CLIP_PARAM, gain, track, 2)
        post(CMD_AUDIO_CLIP_PARAM, fadeInFrames.toFloat(), track, 3)
        post(CMD_AUDIO_CLIP_PARAM, fadeOutFrames.toFloat(), track, 4)
    }

    /**
     * Opens the microphone and arms a recording of at most [seconds] (0 = microphone unavailable).
     * [onStart]: it begins exactly on the transport's first step (after the count-in).
     */
    external fun recordStart(onStart: Boolean, seconds: Int): Long
    /** [state 0 waiting, 1 recording, 2 finished; frames recorded; input peak]. */
    external fun recordStatus(handle: Long): FloatArray
    /** Stops, frees the recording and returns it (mono, 48 kHz); null if the engine did not let go. */
    external fun recordFinish(handle: Long): FloatArray?
    private external fun nativeInstallAudioClip(track: Int, handle: Long): Boolean

    /** Instrument track [track]'s sampled voice plays [sample] across the keys (null: none). */
    fun setTrackSample(track: Int, sample: NativeSample?) {
        val target = captureTarget
        if (target != 0L) exportInstallTrackSample(target, track, sample?.handle ?: 0L)
        else nativeInstallTrackSample(track, sample?.handle ?: 0L)
    }

    /** [param] in SampleInstrument::Param order (0 root note … 9 gain), in that parameter's unit. */
    fun setTrackSampleParam(param: Int, value: Float, track: Int = 0) = post(CMD_TRACK_SAMPLE_PARAM, value, onTrack(track, param))

    /** Drum pad [pad] plays through its own mixer strip (its own effects) or straight into the drums'. */
    fun setPadStrip(pad: Int, on: Boolean) = post(CMD_PAD_STRIP, if (on) 1f else 0f, pad)
    private external fun nativeInstallTrackSample(track: Int, handle: Long): Boolean
    private external fun exportInstallTrackSample(exportHandle: Long, track: Int, handle: Long)
    private external fun exportInstallAudioClip(exportHandle: Long, track: Int, handle: Long)
    /** [beats]: loop length; [oneShot]: plays once per tap; [gain]: 0..2. */
    fun setClipParams(pad: Int, beats: Int, oneShot: Boolean, gain: Float) {
        post(CMD_CLIP_PARAM, beats.toFloat(), pad, 0)
        post(CMD_CLIP_PARAM, if (oneShot) 1f else 0f, pad, 1)
        post(CMD_CLIP_PARAM, gain, pad, 2)
    }
    /** On at the next grid line, or off; [pad] < 0: every pad off. */
    fun toggleClip(pad: Int) = post(CMD_TOGGLE_CLIP, 0f, pad)
    /** Per pad: 0 off, 1 waiting for the grid, 2 playing ([LIVE_PADS] values). */
    external fun clipStates(out: IntArray)

    /** [values] in Sampler::Param order (see PadSample.engineParams). */
    fun setPadParams(pad: Int, values: FloatArray) {
        values.forEachIndexed { param, v -> post(CMD_PAD_PARAM, v, pad, param) }
    }

    /** Instrument track [track] plays [font]'s preset [presetIndex] (null: back to its synth). Main thread. */
    /** False when the live engine did not take it (queue full, stream not running). */
    fun setInstrument(font: SoundFont?, presetIndex: Int, track: Int = 0): Boolean {
        val target = captureTarget
        val a = onTrack(track, presetIndex)
        if (target != 0L) {
            exportSetInstrument(target, font?.handle ?: 0L, a)
            return true
        }
        return nativeSetInstrument(font?.handle ?: 0L, a)
    }

    external fun soundFontLoad(path: String): Long
    external fun soundFontClose(handle: Long)
    external fun soundFontPresets(handle: Long): Array<String>
    /** Worker thread (seconds for a song). [bpm, confidence, firstBeatSeconds]; bpm 0 = not found. */
    external fun analyzeTempo(wavPath: String): FloatArray

    /** Worker thread. [tonic 0=C..11=B or -1, minor 0/1, confidence], chroma summed over every file. */
    external fun analyzeKey(wavPaths: Array<String>): FloatArray

    external fun soundFontRenderNote(handle: Long, preset: Int, note: Int, velocity: Float, holdFrames: Int, frames: Int): FloatArray
    private external fun nativeSetInstrument(font: Long, preset: Int): Boolean
    private external fun exportSetInstrument(exportHandle: Long, font: Long, preset: Int)

    external fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long
    external fun sampleRelease(handle: Long)
    external fun sampleFrames(handle: Long): Long
    external fun sampleOnsets(handle: Long, sensitivity: Float, maxSlices: Int): FloatArray
    external fun sampleStretched(handle: Long, start: Float, end: Float, outFrames: Long, semitones: Float): Long
    private external fun nativeInstallSample(pad: Int, handle: Long): Boolean
    external fun stemsLoad(paths: Array<String>): Long
    external fun stemsRelease(handle: Long)
    external fun stemsFrames(handle: Long): Long
    external fun stemsExtract(handle: Long, stem: Int, start: Long, count: Long): FloatArray
    external fun stemsPeaks(handle: Long, stem: Int, buckets: Int): FloatArray
    external fun stemsStretched(handle: Long, ratio: Double): Long
    private external fun nativeInstallStems(handle: Long): Boolean
    private external fun nativeInstallPreviewStems(handle: Long): Boolean
    private external fun nativeInstallClip(pad: Int, handle: Long): Boolean
    private external fun exportInstallStems(exportHandle: Long, handle: Long)
    private external fun exportInstallSample(exportHandle: Long, pad: Int, handle: Long)

    // Export natives stay public: `internal` would mangle their JNI names. Use OfflineExport.
    external fun exportCreate(): Long
    private external fun exportPost(handle: Long, type: Int, value: Float, a: Int, b: Int)
    private external fun exportScheduleFrom(handle: Long, frame: Long)
    external fun exportRender(handle: Long, path: String, frames: Long, tail: Long): Int
    external fun exportSetFadeOut(handle: Long, frames: Long)
    external fun exportProgress(handle: Long): Float
    external fun exportRenderFloat(handle: Long, frames: Long, tail: Long): FloatArray
    external fun exportCancel(handle: Long)
    external fun exportDestroy(handle: Long)
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
