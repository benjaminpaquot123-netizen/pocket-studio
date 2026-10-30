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
    /** [play]: the transport plays a scene on its own, outside the song. */
    const val OUTSIDE_SONG = -1
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
    private const val CMD_MASTER = 67
    private const val CMD_CLEAR_INSERTS = 68
    private const val CMD_INSERT_ON = 69
    private const val CMD_AUTOMATION_HOLD = 70
    private const val CMD_ARM_LAUNCH = 71
    private const val CMD_CANCEL_LAUNCH = 72
    private const val CMD_SLOT_OFFSET = 73
    private const val CMD_BUS_RETURN = 74
    private const val CMD_MARK_SONG = 75
    private const val CMD_AUTOMATION_SONG_RUN = 76
    private const val CMD_REMIX_ON = 77
    private const val CMD_AUTOMATION_SONG_RAMP = 78
    private const val CMD_AUTOMATION_SONG_FLAT = 79
    // Phase 34 (S3): the drums' micro-timing, rolls, glides and studio voices (Engine.h, numbered from 80).
    private const val CMD_DRUM_STEP_OFFSET = 80
    private const val CMD_DRUM_STEP_ROLL = 81
    private const val CMD_DRUM_STEP_GLIDE = 82
    private const val CMD_PAD_VOICE = 83
    private const val CMD_PAD_VOICE_PARAM = 84
    // Numbered apart in Engine.h (phase 33): commands appended after 79 never take its number.
    private const val CMD_KIT_VERSION = 100
    private const val CMD_SOUND_VERSION = 101
    private const val CMD_SIDECHAIN = 102
    private const val CMD_MIX_VERSION = 103
    // Phase 37 (Engine.h, numbered from 120): the drums' polyrhythm and chances.
    private const val CMD_DRUM_LANE_STEPS = 120
    private const val CMD_DRUM_STEP_CHANCE = 121
    private const val CMD_DRUM_SEED = 122
    // Phase 38: multizone instruments and the 808 on instrument tracks (Engine.h, numbered from 130).
    private const val CMD_TRACK_ZONE = 130
    private const val CMD_TRACK_808 = 131
    // G1b: a track's layers (Engine.h, numbered from 140).
    private const val CMD_LAYER = 140
    private const val CMD_LAYER_LISTEN = 141
    // Final audit, voice (Engine.h, from 142): monitoring and the punch-in's mute.
    private const val CMD_MONITOR = 142
    private const val CMD_RECORD_MUTE_AT = 143

    // Rolls' velocity ramps (Engine.h SetDrumStepRoll).
    const val ROLL_RAMP_LEGACY = 0
    const val ROLL_RAMP_FLAT = 1
    const val ROLL_RAMP_UP = 2
    const val ROLL_RAMP_DOWN = 3
    // A pad's studio voice (DrumKit::Kind).
    const val PAD_VOICE_KIT = 0
    const val PAD_VOICE_808 = 1
    const val PAD_VOICE_KICK_STUDIO = 2
    const val PAD_VOICE_KICK_HARD = 3
    const val PAD_VOICE_RUMBLE = 4
    // A studio voice's settings (DrumKit::VoiceParam), with their units.
    const val VOICE_TUNE = 0   // 808: MIDI note 12..72 (36 = C1); kicks and rumble: Hz 30..120
    const val VOICE_DECAY = 1  // seconds to -40 dB (808 up to 2.5, kicks up to 1.2, rumble up to 2)
    const val VOICE_DRIVE = 2  // 0..1
    const val VOICE_CLICK = 3  // 0..1
    const val VOICE_GLIDE = 4  // seconds 0..0.5 (the 808's default slide; a step's setDrumStepGlide wins)
    const val VOICE_SUB = 5    // 0..1, the kicks' sub layer
    const val VOICE_TONE = 6   // 0..1: 808 sine -> triangle; rumble low-pass 60 -> 240 Hz

    // Live grid slots (Engine.h Slot): one per track.
    const val SLOT_DRUMS = 0
    const val SLOT_ACID = 1
    const val SLOT_INST = 2  // + instrument track 0..7
    const val SLOT_AUDIO = 10  // + audio track 0..3
    const val SLOT_REMIX = 14
    const val SLOT_AUDIO2 = 15  // + audio track 4..7 - 4
    const val SLOTS = 19
    const val ALL_SLOTS = (1 shl SLOTS) - 1
    /** [slotStates]: the tag of a slot playing the row's scene (a scene swap), and of no launch waiting. */
    const val ROW_TAG = -2
    const val NO_LAUNCH = -3

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
    const val INSERTS = 8

    /**
     * Mixer channels: 0 drums, 1 acid bass, 2 synth (instrument track 0), 3 remix stems, 5..11
     * instrument tracks 1..7, 12..15 audio tracks 0..3, 16..31 the drum pads' own strips, 32..35
     * audio tracks 4..7. Index 4 is the master (inserts, automation, meters).
     */
    const val MIXER_CHANNELS = 36
    const val MASTER = 4
    /** [mixerMeters]: after one peak per channel, the limiter's lowest gain since the last reading… */
    const val METER_LIMITER = MIXER_CHANNELS
    /** …then the master's peak going into the limiter (above −1 dBFS, the limiter works). */
    const val METER_MASTER_IN = MIXER_CHANNELS + 1
    /** Size of the array [mixerMeters] fills. */
    const val METERS = MIXER_CHANNELS + 2

    /** Size of the array [insertReductions] fills: one value per strip and insert slot (channel * [INSERTS] + slot). */
    const val INSERT_METERS = MIXER_CHANNELS * INSERTS

    /** [loudnessMeters]: the master's momentary, short-term and integrated (since ▶) loudness in LUFS ([LOUDNESS_NONE]: none yet)… */
    const val LOUDNESS_MOMENTARY = 0
    const val LOUDNESS_SHORT_TERM = 1
    const val LOUDNESS_INTEGRATED = 2
    /** …its true peak since ▶ and since the last reading (dBTP)… */
    const val LOUDNESS_TRUE_PEAK = 3
    const val LOUDNESS_RECENT_PEAK = 4
    /** …and the gain the loudness target adds before the clipper and the limiter (dB)… */
    const val LOUDNESS_DRIVE = 5
    /** …and (S5) what the clipper and the limiter take off for it (LU; with mix version 2, at the loudest part). */
    const val LOUDNESS_LOSS = 6
    /** Size of the array [loudnessMeters] fills. */
    const val LOUDNESS = 7
    /** Size of [healthMeters]' output. */
    const val HEALTH = 4
    /** No value yet (silence, or under 400 ms of sound). */
    const val LOUDNESS_NONE = -100f

    /** [OfflineExport.report]: integrated loudness (LUFS), true peak (dBTP), length (s), sample rate, target's drive (dB). */
    const val REPORT_LUFS = 0
    const val REPORT_TRUE_PEAK = 1
    const val REPORT_SECONDS = 2
    const val REPORT_RATE = 3
    const val REPORT_DRIVE = 4
    /** S5: what the clipper and the limiter took off (LU), the loudest part's loudness (LUFS, -1000: none), the target's mode. */
    const val REPORT_LOSS = 5
    const val REPORT_LOUD = 6
    const val REPORT_MODE = 7

    /** [setLiveNoises] modes (the engine's PerformanceFx::Noises). */
    const val LIVE_NOISES_MIXED = 0
    const val LIVE_NOISES_OFF = 1
    const val LIVE_NOISES_ONLY = 2

    /** Instrument tracks (the synth track is track 0). */
    const val TRACKS = 8

    /**
     * G1b: layers over each track's own sound (InstrumentTrack::kLayers). Layer [layer] of track
     * [track] is sound [layerSound] for the sound commands ([setSynthEngine], [setSynthParam],
     * [setPlaitsParam], [setInstrument], [setTrackSample], [setTrackSampleParam], [setTrackZone],
     * [setTrackZoneParam], [setTrackMultiParam], [setTrack808Param]: their `track`), and mixer strip
     * [layerChannel] for the mixer ones ([setChannelPan], [setChannelMute], [setSend], [setInsertType],
     * [setInsertParam], [clearInserts]); [setLayer] says how it plays the track's notes.
     */
    const val LAYERS = 3
    /** Every sound the sound commands address: the tracks' own, then their layers'. */
    const val SOUNDS = TRACKS + TRACKS * LAYERS
    fun layerSound(track: Int, layer: Int) = TRACKS + track.coerceIn(0, TRACKS - 1) * LAYERS + layer.coerceIn(0, LAYERS - 1)
    /** A layer's own strip, after the project's [MIXER_CHANNELS] (never in its saved channel numbers). */
    fun layerChannel(track: Int, layer: Int) = MIXER_CHANNELS + track.coerceIn(0, TRACKS - 1) * LAYERS + layer.coerceIn(0, LAYERS - 1)
    // [setLayer]'s settings (InstrumentTrack::LayerParam, then Engine.h LayerMix).
    const val LAYER_ON = 0         // 1: it plays the track's notes, 0: off (its notes let go, its release rings out)
    const val LAYER_LOW = 1        // the lowest note of the track it plays (MIDI)
    const val LAYER_HIGH = 2       // the highest
    const val LAYER_TRANSPOSE = 3  // semitones -48..48
    const val LAYER_FINE = 4       // -1..1 semitone
    const val LAYER_GAIN = 5       // linear 0..4, after its effects
    const val LAYER_THROUGH = 6    // 1: through the track's effects, 0: joining the track after them
    /** The most notes a layer's synth plays at once (Engine.h kLayerVoices): a seventh steals one. */
    const val LAYER_VOICES = 6
    /** [setLayerListen]: every sound of the track heard (as mixed). Bit 0: its own sound, bit 1 + k: layer k. */
    const val LISTEN_ALL = 15

    /** Audio tracks (AudioTracks::kTracks). */
    const val AUDIO_TRACKS = 8

    /** The mixer channel of audio track [track] (Engine.h audioChannel): 12..15, then 32..35. */
    fun audioChannel(track: Int) = if (track < 4) 12 + track else 32 + track - 4

    /** The live slot of audio track [track] (Engine.h audioSlot): 10..13, then 15..18. */
    fun audioSlot(track: Int) = if (track < 4) SLOT_AUDIO + track else SLOT_AUDIO2 + track - 4

    /** The mixer channel of instrument track [track]. */
    fun trackChannel(track: Int) = if (track == 0) 2 else 4 + track

    // Synth commands name their track in bits 16..23 of `a` (see Engine.h).
    private fun onTrack(track: Int, a: Int) = (a and 0xFFFF) or (track.coerceIn(0, TRACKS - 1) shl 16)
    // The sound commands: a track's own sound or a layer's ([layerSound]).
    private fun onSound(sound: Int, a: Int) = (a and 0xFFFF) or (sound.coerceIn(0, SOUNDS - 1) shl 16)

    /** Layer [layer] of track [track]: a [LAYER_ON]..[LAYER_THROUGH] setting. */
    fun setLayer(track: Int, layer: Int, param: Int, value: Float) =
        post(CMD_LAYER, value, (param and 0xFF) or (layer.coerceIn(0, LAYERS - 1) shl 8) or (track.coerceIn(0, TRACKS - 1) shl 16))
    /**
     * Which of track [track]'s sounds are heard, live (« écouter cette couche seule », « sans elle »):
     * bit 0 its own sound, bit 1 + k its layer k; [LISTEN_ALL] as mixed. Each glides in or out.
     */
    fun setLayerListen(track: Int, mask: Int) = post(CMD_LAYER_LISTEN, (mask and LISTEN_ALL).toFloat(), track.coerceIn(0, TRACKS - 1))

    const val LANES = 16
    /** MultiSampler::kZones: the zones of one multizone instrument. */
    const val MULTI_ZONES = 512

    private val loaded: Boolean = try {
        System.loadLibrary("pocketstudio_engine")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    /**
     * Tests and the song bench only (null in the app): the calls go to this sink instead of the
     * native library, which then need not be loaded (see [CommandSink]).
     */
    @Volatile var sink: CommandSink? = null

    /** The engine can be used: the native library is loaded (or a test's [sink] stands in for it). */
    val isAvailable: Boolean get() = loaded || sink != null

    /** The live engine is the native one (not a test's [sink]): [collectGarbage] has something to free. */
    val isNative: Boolean get() = loaded && sink == null

    /**
     * Main thread (the one that installs sounds): frees the sounds the audio thread let go of. The
     * installs do it before each new sound; an instrument's zones cleared (a track back to its
     * synth) would otherwise wait for the next install with their memory.
     */
    fun collectGarbage() {
        if (isNative) nativeCollectGarbage()
    }
    private external fun nativeCollectGarbage()

    fun setPlaying(playing: Boolean) = post(CMD_SET_PLAYING, if (playing) 1f else 0f)
    fun setFrequency(hz: Float) = post(CMD_SET_FREQUENCY, hz)
    fun setGain(gain: Float) = post(CMD_SET_GAIN, gain)

    /**
     * [songStep]: the song position of the first step (the song started from a later part);
     * [OUTSIDE_SONG]: a scene played on its own (song curves and song clips wait for the song).
     */
    fun play(countInBars: Int = 0, songStep: Int = 0) = post(CMD_SET_TRANSPORT, 1f, countInBars, songStep.coerceAtLeast(OUTSIDE_SONG))
    fun stopTransport() = post(CMD_SET_TRANSPORT, 0f)
    fun setTempo(bpm: Double) = post(CMD_SET_TEMPO, bpm.toFloat())
    /**
     * The built-in kit's levels: 1 (the kit as it was, projects saved before phase 33), 2 (kit v2) or 3
     * (kit v3, phase 34: the short studio kick, the hard kick on pad 2, bright hats, a four-burst clap).
     */
    fun setKitVersion(version: Int) = post(CMD_KIT_VERSION, version.toFloat())
    /**
     * The instrument tracks' sound version: 1 (projects saved before phase 34 S4: every track mono, as
     * always) or 2 (S4: General MIDI instruments and stereo samples play in stereo).
     */
    fun setSoundVersion(version: Int) = post(CMD_SOUND_VERSION, version.toFloat())
    /**
     * The mix version: 1 (projects saved before phase 34 S5: the loudness target aims at the whole
     * song, the master chain may take 6 LU) or 2 (S5: it aims at the loudest part, the Drop, and
     * takes 3 LU at most there).
     */
    fun setMixVersion(version: Int) = post(CMD_MIX_VERSION, version.toFloat())
    /**
     * S5: the sound the sidechain listens to (the audio-keyed pumps, the « Musique » bus): mixer
     * channel [channel] ([padChannel] for a drum pad, the kick's), or -1 for none.
     */
    fun setSidechainKey(channel: Int) = post(CMD_SIDECHAIN, channel.toFloat(), 0)
    /** S5: the « Musique » bus's duck under the key: [depth], [lowDepth] (under 150 Hz), [release] (0..1 = 50..600 ms). */
    fun setMusicBusDuck(depth: Float, lowDepth: Float, release: Float) {
        post(CMD_SIDECHAIN, depth, 1)
        post(CMD_SIDECHAIN, lowDepth, 2)
        post(CMD_SIDECHAIN, release, 3)
    }
    /** S5: mixer channel [channel] plays through the « Musique » bus (or straight into the mix). */
    fun setMusicBusMember(channel: Int, on: Boolean) = post(CMD_SIDECHAIN, if (on) 1f else 0f, 4, channel)
    /** S5: send bus [bus]'s return (0 reverb, 1 echo) through the « Musique » bus. */
    fun setMusicBusReturn(bus: Int, on: Boolean) = post(CMD_SIDECHAIN, if (on) 1f else 0f, 5, bus)
    /** The mixer channel of drum pad [pad]'s own strip (the sidechain key of a kick). */
    fun padChannel(pad: Int) = 16 + pad
    fun setSwing(swing: Double) = post(CMD_SET_SWING, swing.toFloat())
    fun setMetronome(on: Boolean) = post(CMD_SET_METRONOME, if (on) 1f else 0f)
    fun setPatternSteps(steps: Int) = post(CMD_SET_PATTERN_STEPS, 0f, steps)
    /** velocity 0 removes the hit. */
    /**
     * [repeats]: hits in the step (1 = plain, 2..8 = a roll), carried in one command with the hit, and the
     * roll's [ramp] (Hit.ramp: 0 each hit a little softer, as always; 1 rising from half the velocity, a
     * snare build; -1 falling to half, a bounce). [setDrumStepRoll] after it reshapes the roll further.
     */
    fun setDrumHit(lane: Int, step: Int, velocity: Float, repeats: Int = 1, ramp: Int = 0) =
        post(CMD_SET_DRUM_HIT, velocity, lane, step or (repeats.coerceIn(1, 8) shl 16) or (hitRampBits(ramp) shl 24))

    /** Engine.h SetDrumHit bits 24..25: 0 as before, 1 rising, 2 falling (3 even is [setDrumStepRoll]'s flat). */
    private fun hitRampBits(ramp: Int) = when {
        ramp > 0 -> 1
        ramp < 0 -> 2
        else -> 0
    }
    /**
     * Phase 37: the steps pad [lane]'s lane loops over on its own (1..64, a polyrhythm); 0 = the
     * pattern's length, as always. Follows [setStaging] (a scene's own).
     */
    fun setDrumLaneSteps(lane: Int, steps: Int) = post(CMD_DRUM_LANE_STEPS, steps.coerceIn(0, 64).toFloat(), lane)
    /**
     * After [setDrumHit] (which resets it to always): the chance (0..1) the hit and its roll play on
     * each pass, drawn from the seed ([setDrumSeed]), the pad and the step's place in the song, the
     * same way every time (DrumChance).
     */
    fun setDrumStepChance(lane: Int, step: Int, chance: Float) = post(CMD_DRUM_STEP_CHANCE, chance.coerceIn(0f, 1f), lane, step)
    /** The chances' seed (0..2^24): another seed, another draw of the same chances. */
    fun setDrumSeed(seed: Int) = post(CMD_DRUM_SEED, seed.coerceIn(0, 1 shl 24).toFloat())
    /** A pad's place in the stereo field, -1 (left) .. 1 (right). */
    fun setPadPan(lane: Int, pan: Float) = post(CMD_PAD_PAN, pan, lane)
    /**
     * After [setDrumHit] (which resets them): this hit's pan, added to the pad's, and [pitch]
     * semitones (-24..24, sampled sounds only).
     */
    fun setDrumStepDetail(lane: Int, step: Int, pan: Float, pitch: Int) =
        post(CMD_DRUM_STEP_DETAIL, pan, lane, step or ((pitch.coerceIn(-24, 24) + 64) shl 16))
    /** Hits in the step: 1 = plain, 2..16 = a roll (at most 8 a step; more over a [setDrumStepRoll] span). */
    fun setDrumRepeat(lane: Int, step: Int, repeats: Int) = post(CMD_DRUM_REPEAT, repeats.coerceIn(1, 16).toFloat(), lane, step)
    /**
     * After [setDrumHit] (which resets it): this hit, and its roll, plays [ms] milliseconds early (< 0,
     * -50..) or late (..50). An early hit is queued from the step before it; the first step after play
     * lands on the grid.
     */
    fun setDrumStepOffset(lane: Int, step: Int, ms: Float) = post(CMD_DRUM_STEP_OFFSET, ms.coerceIn(-50f, 50f), lane, step)
    /**
     * After [setDrumHit] (which resets it): the step's roll. [hits] (1..16, at most 8 a step) spread
     * evenly over [span] steps (1..4: 8 hits over 4 steps = a 1/32 burst over a beat), their velocity
     * shaped by [ramp] ([ROLL_RAMP_LEGACY]: each hit 0.85 x the one before, as before phase 34;
     * [ROLL_RAMP_FLAT]; [ROLL_RAMP_UP]: the first hit at velocity x (1 - [amount]), the last at the
     * velocity; [ROLL_RAMP_DOWN]: the reverse), [triplet] = a shuffle (every second gap longer, 2:1).
     */
    fun setDrumStepRoll(lane: Int, step: Int, hits: Int, span: Int = 1, ramp: Int = ROLL_RAMP_FLAT, amount: Float = 0f, triplet: Boolean = false) =
        post(
            CMD_DRUM_STEP_ROLL, amount.coerceIn(0f, 1f), lane,
            step or (span.coerceIn(1, 4) shl 8) or (ramp.coerceIn(0, 3) shl 12) or ((if (triplet) 1 else 0) shl 15) or (hits.coerceIn(1, 16) shl 16),
        )
    /**
     * After [setDrumHit] (which resets it): the 808 ([PAD_VOICE_808]) slides from the pitch it plays to
     * this hit's over [ms] milliseconds (0..500; 0: a new note). The hit's pitch: [setDrumStepDetail].
     */
    fun setDrumStepGlide(lane: Int, step: Int, ms: Float) = post(CMD_DRUM_STEP_GLIDE, ms.coerceIn(0f, 500f), lane, step)
    /**
     * What the pad plays instead of its own voice of the kit: [PAD_VOICE_KIT] (its own), [PAD_VOICE_808],
     * [PAD_VOICE_KICK_STUDIO], [PAD_VOICE_KICK_HARD] or [PAD_VOICE_RUMBLE]. The voice's settings go back
     * to its defaults: send [setPadVoiceParam] after it.
     */
    fun setPadVoice(lane: Int, kind: Int) = post(CMD_PAD_VOICE, kind.coerceIn(0, 4).toFloat(), lane)
    /** A studio voice's setting ([VOICE_TUNE] .. [VOICE_TONE]), in its unit (see the constants). */
    fun setPadVoiceParam(lane: Int, param: Int, value: Float) = post(CMD_PAD_VOICE_PARAM, value, lane, param)
    /** [packed] = AcidStep.pack() from project-model (same layout as the engine). */
    fun setAcidStep(index: Int, packed: Int) = post(CMD_SET_ACID_STEP, 0f, index, packed)
    fun setAcidLength(steps: Int) = post(CMD_SET_ACID_LENGTH, 0f, steps)
    fun setAcidRoot(midiNote: Int) = post(CMD_SET_ACID_ROOT, 0f, midiNote)
    /** [param] in AcidTrack::Param order, [value] 0..1. */
    fun setAcidParam(param: Int, value: Float) = post(CMD_SET_ACID_PARAM, value, param)

    fun synthNoteOn(note: Int, velocity: Float, track: Int = 0) = post(CMD_SYNTH_NOTE_ON, velocity, onTrack(track, note))
    fun synthNoteOff(note: Int, track: Int = 0) = post(CMD_SYNTH_NOTE_OFF, 0f, onTrack(track, note))
    /** [param] in Synth::Param order, [value] 0..1. */
    fun setSynthParam(param: Int, value: Float, track: Int = 0) = post(CMD_SYNTH_PARAM, value, onSound(track, param))
    /** Instrument track [track] (1..7) in use or not: a track not in use is silent and costs nothing. */
    fun setTrackOn(track: Int, on: Boolean) = post(CMD_TRACK_ON, if (on) 1f else 0f, track)
    fun synthAllOff() = post(CMD_SYNTH_ALL_OFF, 0f)

    fun setChannelVolume(ch: Int, v: Float) = post(CMD_CHANNEL_VOLUME, v, ch)
    fun setChannelPan(ch: Int, pan: Float) = post(CMD_CHANNEL_PAN, pan, ch)
    fun setChannelMute(ch: Int, on: Boolean) = post(CMD_CHANNEL_MUTE, if (on) 1f else 0f, ch)
    fun setChannelSolo(ch: Int, on: Boolean) = post(CMD_CHANNEL_SOLO, if (on) 1f else 0f, ch)
    /** [ch] 0..MIXER_CHANNELS (the last is the master); [type] in EffectType order. */
    fun setInsertType(ch: Int, slot: Int, type: Int) = post(CMD_INSERT_TYPE, 0f, ch * INSERTS + slot, type)
    /** Every insert slot of [ch] emptied in one command (a strip no longer used gives back its echo and reverb memory). */
    fun clearInserts(ch: Int) = post(CMD_CLEAR_INSERTS, 0f, ch)
    fun setInsertParam(ch: Int, slot: Int, param: Int, value: Float) = post(CMD_INSERT_PARAM, value, ch * INSERTS + slot, param)
    /**
     * « Joue dans »: effect [slot] of [ch] plays in this scene ([on]) or lets the sound through
     * untouched. Follows [setStaging]: the waiting scene's, switched on the swap's frame.
     */
    fun setInsertOn(ch: Int, slot: Int, on: Boolean) = post(CMD_INSERT_ON, if (on) 1f else 0f, ch * INSERTS + slot)
    /** Post-fader send of [ch] to [bus] (0 reverb, 1 delay), 0..1. */
    fun setSend(ch: Int, bus: Int, level: Float) = post(CMD_SEND, level, ch, bus)
    /** A track's quick control (Macros: colour, dirt, motion, space), 0..1. */
    fun setMacro(ch: Int, macro: Int, value: Float) = post(CMD_MACRO, value, ch, macro)
    fun setBusParam(bus: Int, param: Int, value: Float) = post(CMD_BUS_PARAM, value, bus, param)

    /** The send reverb's type ([setBusParam] on bus 0): [REVERB_HALL] (as always), [REVERB_PLATE] or [REVERB_ROOM]. */
    const val BUS_REVERB_TYPE = 4
    const val REVERB_HALL = 0f
    const val REVERB_PLATE = 0.5f
    const val REVERB_ROOM = 1f
    /** A send bus's return equaliser: low cut (0 = off, else 40 x 25^v Hz) and high cut (1 = off, else 1 x 20^v kHz). */
    const val BUS_LOW_CUT = 8
    const val BUS_HIGH_CUT = 9
    /** Lane 0..7 follows [packedTarget] (AutoTarget.pack(), -1 = off); clears its points. */
    fun setAutomationTarget(lane: Int, packedTarget: Int) = post(CMD_AUTOMATION_TARGET, 0f, lane, packedTarget)
    /**
     * Touch take-over: while [held], no automation lane writes the setting [packedTarget]
     * (AutoTarget.pack(): a 303 knob, a mixer setting), so the finger on it is heard; let go, the
     * lanes drive it again from the next step.
     */
    fun setAutomationHold(packedTarget: Int, held: Boolean) = post(CMD_AUTOMATION_HOLD, if (held) 1f else 0f, packedTarget)
    /** [value] 0..1 on [step], or null for no point. */
    fun setAutomationPoint(lane: Int, step: Int, value: Float?) = post(CMD_AUTOMATION_POINT, value ?: -1f, lane, step)
    /** While on, pattern commands (drums, acid, piano roll) fill the waiting scene. */
    fun setStaging(on: Boolean) = post(CMD_STAGING, if (on) 1f else 0f)
    /** The waiting scene takes over after [steps] steps of the playing one (0 = next bar, -1 = cancel). */
    /**
     * [restartsSong]: the waiting scene starts the song (again): song automation and song clips
     * count from there, from song step [songStep] when it is a later part than the first.
     */
    fun armSceneSwap(steps: Int, restartsSong: Boolean = false, songStep: Int = 0, leavesSong: Boolean = false) =
        post(CMD_ARM_SCENE_SWAP, songStep.coerceAtLeast(0).toFloat(), steps, if (restartsSong) 1 else if (leavesSong) 3 else 0)
    /** The swap armed is cancelled and the scene playing goes on by itself, outside the song. */
    fun leaveSong() = post(CMD_ARM_SCENE_SWAP, 0f, -1, 3)
    /** The song ends after [steps] steps of the playing scene: the transport stops on that bar line (counted in [sceneSwaps]). */
    fun armSongEnd(steps: Int) = post(CMD_ARM_SCENE_SWAP, 0f, steps, 2)
    /** Steps since the song (re)started, as song automation reads them; -1 when stopped. */
    fun songStep(): Int = sink?.songStep() ?: nativeSongStep()
    private external fun nativeSongStep(): Int
    /** Live effects on the master: [param] 0 filter (0.5 = off), 1 resonance, 2 repeat on, 3 repeat division, 4 tape stop, 5 chop. */
    fun setPerformance(param: Int, value: Float) = post(CMD_PERFORMANCE, value, param)
    /**
     * The synth track's own voice: 0 subtractive, 1 Plaits, 2 a sample played across the keys ([setTrackSample]),
     * 3 a multizone instrument ([setTrackZone]; the synth while it has none), 4 the 808 ([setTrack808Param]).
     */
    fun setSynthEngine(engine: Int, track: Int = 0) = post(CMD_SYNTH_ENGINE, 0f, onSound(track, engine))
    /** [param] in PlaitsSynth::Param order (0 model, 1 harmonics … 7 level), [value] 0..1. */
    fun setPlaitsParam(param: Int, value: Float, track: Int = 0) = post(CMD_PLAITS_PARAM, value, onSound(track, param))
    /** How many times a waiting scene took over since the engine started. */
    fun sceneSwaps(): Int = sink?.sceneSwaps() ?: nativeSceneSwaps()
    private external fun nativeSceneSwaps(): Int
    /** Writes into [out] (size [METERS]): channel peaks (the master's output at [MASTER]), the limiter gain, the master's level into the limiter. */
    external fun mixerMeters(out: FloatArray)
    /**
     * For each strip and insert slot ([INSERT_METERS] values, channel * [INSERTS] + slot): the deepest
     * gain reduction of its compressor since the last call, in dB (≤ 0; 0: none or not a compressor).
     */
    external fun insertReductions(out: FloatArray)

    /** Writes into [out] (size [LOUDNESS]) the master's loudness and true peak (see [LOUDNESS_MOMENTARY]). */
    external fun loudnessMeters(out: FloatArray)

    /**
     * Writes into [out] (size [HEALTH]) what the bug report's health journal samples, taking nothing
     * from the other meters: momentary and short-term loudness (LUFS, [LOUDNESS_NONE] none yet), the
     * limiter's gain now (linear, 1 = untouched) and the audio thread's load ([cpuLoad]).
     */
    external fun healthMeters(out: FloatArray)

    /** Plays a pad right now (live). */
    fun triggerPad(lane: Int, velocity: Float) = post(CMD_TRIGGER_PAD, velocity, lane)

    // Live input (MIDI thread): always the live engine, never an export being captured.
    // The native side serialises producers, so any thread may call these.
    /** The instrument track a MIDI keyboard plays (the one shown in the synth screen). */
    @Volatile var liveTrack = 0
    fun liveNoteOn(note: Int, velocity: Float, track: Int = liveTrack) = livePost(CMD_SYNTH_NOTE_ON, velocity, onTrack(track, note), 0)
    fun liveNoteOff(note: Int, track: Int = liveTrack) = livePost(CMD_SYNTH_NOTE_OFF, 0f, onTrack(track, note), 0)
    fun liveTriggerPad(lane: Int, velocity: Float) = livePost(CMD_TRIGGER_PAD, velocity, lane, 0)
    /** Pitch bend (semitones), mod wheel (0..1), sustain (0/1) or arpeggiator setting of a track. */
    fun liveExpression(kind: Int, value: Float, track: Int = liveTrack) = livePost(CMD_EXPRESSION, value, onTrack(track, kind), 0)

    private fun livePost(type: Int, value: Float, a: Int, b: Int): Boolean {
        sink?.let { it.post(0L, type, value, a, b); return true }
        return nativePost(type, value, a, b)
    }

    /** Current pattern step, or -1 when stopped or counting in. */
    fun currentStep(): Int = sink?.currentStep() ?: nativeCurrentStep()
    private external fun nativeCurrentStep(): Int

    /**
     * How busy the audio thread is: its render time over the time its sound lasts, smoothed over
     * about half a second. Near 1 the sound starts to crackle (too many heavy effects at once).
     */
    external fun cpuLoad(): Float
    /** Steps since play without wrapping (-1 stopped): the piano roll loops on its own length. */
    fun absoluteStep(): Int = sink?.absoluteStep() ?: nativeAbsoluteStep()
    private external fun nativeAbsoluteStep(): Int

    /**
     * Position in the playing scene in sixteenths, with the fraction (live recording). While the
     * count-in clicks, negative: the sixteenths left before the first step. -1 when stopped.
     */
    fun playPosition(): Double = sink?.playPosition() ?: nativePlayPosition()
    private external fun nativePlayPosition(): Double

    /** Click volume 0..1 (0.5 = default). */
    fun setMetronomeLevel(level: Float) = post(CMD_METRONOME_LEVEL, level.coerceIn(0f, 1f))

    /** Output buffer in bursts: 2 = fast (lowest latency), 4 = safe, 6 = very safe. Applies at once. */
    external fun setBufferBursts(bursts: Int)

    /**
     * Piano-roll note [index] (ticks: 24 per sixteenth). Packing matches NoteTrack::pack and, past
     * 4095 ticks (16-bar loops), NoteTrack::packIndex.
     */
    fun setSynthNote(index: Int, start: Int, length: Int, pitch: Int, velocity: Float, track: Int = 0) =
        post(CMD_SYNTH_NOTE, velocity, onTrack(track, synthNoteIndex(index, start, length)), (start and 0xFFF) or ((length and 0xFFF) shl 12) or ((pitch and 0x7F) shl 24))

    /** NoteTrack::packIndex: the index (10 bits), then the start's and the length's bits 12..14. */
    fun synthNoteIndex(index: Int, start: Int, length: Int): Int =
        (index and 0x3FF) or (((start shr 12) and 0x7) shl 10) or (((length shr 12) and 0x7) shl 13)
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
    /**
     * An output device appeared or went (headphones, Bluetooth): when the sound was cut and could
     * not come back yet, the engine tries again at once instead of at its next delay. Never blocks.
     */
    fun retryOutput() {
        if (isNative) nativeRetryOutput()
    }
    private external fun nativeRetryOutput()
    /** Microphone configuration used by the last latency test, in French, for reports. */
    external fun inputDescription(): String
    private external fun nativeStartLatencyTest(): Int
    // When non-zero, commands go to an offline export engine instead of the live one.
    private var captureTarget = 0L

    private fun post(type: Int, value: Float, a: Int = 0, b: Int = 0): Boolean {
        val target = captureTarget
        sink?.let { it.post(target, type, value, a, b); return true }
        if (target != 0L) {
            exportPost(target, type, value, a, b)
            return true
        }
        if (nativePost(type, value, a, b)) return true
        // The queue stayed full (the stream stalled for 100 ms): the command is lost. Callers that
        // send a lot (a whole project, long curves) check [droppedCommands] and send again.
        droppedCommands++
        return false
    }

    /** Commands the live engine could not take since the app started (main thread). */
    var droppedCommands = 0
        private set

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
        sink?.let { it.scheduleFrom(target, frame); return }
        exportScheduleFrom(target, frame)
    }

    /** Gives pad [pad] the sample (or back to its drum sound with null). Main thread, like every command. */
    fun setPadSample(pad: Int, sample: NativeSample?) {
        val target = captureTarget
        sink?.let { it.install(target, CommandSink.Install.PadSample, pad, sample?.handle ?: 0L); return }
        if (target != 0L) exportInstallSample(target, pad, sample?.handle ?: 0L)
        else nativeInstallSample(pad, sample?.handle ?: 0L)
    }

    /**
     * Sampled kits: pad [pad]'s layer [layer] (0..7) plays [sample] for hit velocities [velocityLow]..[velocityHigh]
     * (0..127); layers sharing a range take turns (round robin, from the start of the transport). Null removes
     * the layer. A pad with layers plays them instead of its sample; [setPadSample] removes them, so a kit's pad
     * is sent as [setPadSample] (its base sound) then its layers. Main thread, like every command.
     */
    fun setPadLayer(pad: Int, layer: Int, velocityLow: Int, velocityHigh: Int, sample: NativeSample?) {
        // Sampler::layerArg: the pad index carries the layer and its range (the engine reads them from it).
        val packed = (pad and 0xFF) or ((layer.coerceIn(0, 7) + 1) shl 8) or (velocityLow.coerceIn(0, 127) shl 16) or
            (velocityHigh.coerceIn(0, 127) shl 24)
        setPadSample(packed, sample)
    }

    /** The remix track plays [stems] from step 0 (null: no song). Main thread, like every command. */
    fun setStems(stems: NativeStems?) {
        val target = captureTarget
        sink?.let { it.install(target, CommandSink.Install.Stems, 0, stems?.handle ?: 0L); return }
        if (target != 0L) exportInstallStems(target, stems?.handle ?: 0L)
        else nativeInstallStems(stems?.handle ?: 0L)
    }

    /** [param]: 0 gain (0..1, 0.8 = unity), 1 mute, 2 solo. */
    fun setStemParam(stem: Int, param: Int, value: Float) = post(CMD_STEM_PARAM, value, stem, param)

    /** Song frame (48 kHz) heard on step 0; negative: the song comes in that many frames after step 0 (silence first). */
    fun setStemStart(frame: Long) = post(CMD_STEM_START, 0f, frame.coerceIn(-Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())

    /** Song position in frames while the stems play, else -1. */
    external fun stemPosition(): Long

    /** [stemPosition], from a test's [sink] when it plays the stems (-1 with no engine at all). */
    fun stemPositionNow(): Long = sink?.stemPosition() ?: if (loaded) stemPosition() else -1L

    /** The remix screen's own listening: [stems] play apart from the transport (null: none). Main thread. */
    fun setPreviewStems(stems: NativeStems?): Boolean {
        sink?.let { it.install(0L, CommandSink.Install.PreviewStems, 0, stems?.handle ?: 0L); return true }
        return nativeInstallPreviewStems(stems?.handle ?: 0L)
    }
    fun playPreview(on: Boolean) = post(CMD_PLAY_PREVIEW, if (on) 1f else 0f)
    /** As [setStemParam], for the preview. */
    fun setPreviewParam(stem: Int, param: Int, value: Float) = post(CMD_PREVIEW_PARAM, value, stem, param)
    /** Preview position in frames while it plays, else -1. */
    external fun previewPosition(): Long

    /** [previewPosition], from a test's [sink] when it plays the preview (-1 with no engine at all). */
    fun previewPositionNow(): Long = sink?.previewPosition() ?: if (loaded) previewPosition() else -1L

    /** Live pad [pad] plays [sample] (null: empty). Main thread, like every command. */
    fun setClip(pad: Int, sample: NativeSample?, refit: Boolean = false): Boolean {
        val index = if (refit) pad or REFIT else pad
        sink?.let { it.install(0L, CommandSink.Install.LiveClip, index, sample?.handle ?: 0L); return true }
        return nativeInstallClip(index, sample?.handle ?: 0L)
    }

    /** Frames [readScope] fills (Engine::kScopeFrames). */
    const val SCOPE_FRAMES = 4096
    /** The last [SCOPE_FRAMES] of the output (mono) or of the microphone, oldest first. */
    external fun readScope(microphone: Boolean, out: FloatArray)
    /**
     * « Mode retours »: the output (stereo interleaved, 48 kHz) copied for the screen recording's
     * sound, from [tapStart] to [tapStop]; [tapRead] fills [out] with what came (whole frames) and
     * says how many floats. One reader thread, never the main one for long.
     */
    external fun tapStart()
    external fun tapStop()
    external fun tapRead(out: FloatArray): Int
    /** Microphone on for the tuner (needs the permission); false when unavailable. Blocking (up to 1 s). */
    external fun micOpen(): Boolean
    external fun micClose()

    /** A song part switches audio track [track] on or off (follows [setStaging]). */
    fun setAudioOn(track: Int, on: Boolean) = post(CMD_AUDIO_ON, if (on) 1f else 0f, track)

    /**
     * « M'entendre dans le casque »: while a take is armed or running, the microphone is heard on
     * audio track [track]'s strip (its volume and effects); -1: off.
     */
    fun setMonitor(track: Int, gain: Float = 1f) = post(CMD_MONITOR, gain.coerceIn(0f, 2f), track.coerceIn(-1, 7))

    /**
     * The audio track [setMonitor] sends the microphone to as the engine has it now (-1: none): the
     * engine cuts it when the output reopens (headphones unplugged). Null: not known (no engine).
     */
    fun monitorTrack(): Int? = sink.let { if (it != null) it.monitorTrack() else if (loaded) nativeMonitorTrack() else null }
    private external fun nativeMonitorTrack(): Int

    /**
     * Punch-in: audio track [track]'s clip plays until step [step] (of the song when [song], else of
     * the track's part), then stays silent until the recording ends. Posted after [recordStart].
     */
    fun recordMuteAt(track: Int, step: Int, song: Boolean) = post(CMD_RECORD_MUTE_AT, if (song) 1f else 0f, track, step)

    /**
     * Automation lane [lane]: 0 loop, 1 per scene, 2 song ([songPoints] values sent with
     * [setAutomationSong]: a point per bar, or per beat with [perBeat]; [held]: each point holds
     * until the next instead of sliding).
     */
    /** [loopSteps]: a loop or per-scene lane's own loop from the scene's start (0: the drum loop's). */
    fun setAutomationMode(lane: Int, mode: Int, songPoints: Int, perBeat: Boolean = false, held: Boolean = false, loopSteps: Int = 0) =
        post(CMD_AUTOMATION_MODE, songPoints.toFloat(), lane, mode or (if (perBeat) 16 else 0) or (if (held) 32 else 0) or (loopSteps.coerceIn(0, 64) shl 8))
    fun setAutomationSong(lane: Int, point: Int, value: Float) = post(CMD_AUTOMATION_SONG, value, lane, point)
    /** [count] song points of lane [lane] from [first] at [value] (a run of equal values, one command). */
    /**
     * Song points [first] .. [first] + [count] - 1 of [lane] on a straight line from [from] toward
     * [to] (the value of the point after them, reached there): a slide in one command.
     */
    fun setAutomationSongRamp(lane: Int, first: Int, count: Int, from: Float, to: Float) =
        post(CMD_AUTOMATION_SONG_RAMP, from, (lane and 0xFF) or (Math.round(to.coerceIn(0f, 1f) * 65535f) shl 8), (first and 0xFFFF) or ((count.coerceIn(0, 0xFFFF)) shl 16))
    /**
     * Song points [first] .. [first] + [count] - 1 of [lane] hold their value instead of sliding
     * toward the next point (the next one is a jump: a take's Drop, a let-go). Sent after
     * [setAutomationMode], which clears them.
     */
    fun setAutomationSongFlat(lane: Int, first: Int, count: Int) =
        post(CMD_AUTOMATION_SONG_FLAT, 1f, lane, (first and 0xFFFF) or ((count.coerceIn(0, 0xFFFF)) shl 16))
    /** The remix plays (or is silent, keeping its place): a song part's (follows [setStaging]). */
    fun setRemixOn(on: Boolean) = post(CMD_REMIX_ON, if (on) 1f else 0f)
    fun setAutomationSongRun(lane: Int, first: Int, count: Int, value: Float) =
        post(CMD_AUTOMATION_SONG_RUN, value, lane, (first and 0xFFFF) or ((count.coerceIn(0, 0xFFFF)) shl 16))

    /** Time signature [beats]/4 (2..7): count-in, bar lines, metronome accent, live pads grid. */
    fun setBeatsPerBar(beats: Int) = post(CMD_BEATS_PER_BAR, 0f, beats)

    /**
     * Audio track [track] plays [sample] (null: nothing). [refit]: the same loop stretched to a new
     * tempo; a pass sounding carries on at its place in the new copy (Engine kRefit).
     */
    fun setAudioClip(track: Int, sample: NativeSample?, refit: Boolean = false) {
        val target = captureTarget
        val index = if (refit) track or REFIT else track
        sink?.let { it.install(target, CommandSink.Install.AudioClip, index, sample?.handle ?: 0L); return }
        if (target != 0L) exportInstallAudioClip(target, index, sample?.handle ?: 0L)
        else nativeInstallAudioClip(index, sample?.handle ?: 0L)
    }

    /** Engine kRefit: the sound installed is the same loop stretched to a new tempo. */
    const val REFIT = 0x100

    /**
     * Where and how the clip plays: from step [start] (of the scene, or of the song when [song]),
     * again every [loopSteps] (0: once, until its scene starts again; song clips play once),
     * fades and [offsetFrames] (later > 0, earlier < 0) in frames at 48 kHz.
     */
    fun setAudioClipParams(
        track: Int, start: Int, loopSteps: Int, gain: Float, fadeInFrames: Int, fadeOutFrames: Int,
        song: Boolean = false, offsetFrames: Int = 0,
    ) {
        post(CMD_AUDIO_CLIP_PARAM, if (song) 1f else 0f, track, 5)
        post(CMD_AUDIO_CLIP_PARAM, start.toFloat(), track, 0)
        post(CMD_AUDIO_CLIP_PARAM, loopSteps.toFloat(), track, 1)
        post(CMD_AUDIO_CLIP_PARAM, gain, track, 2)
        post(CMD_AUDIO_CLIP_PARAM, fadeInFrames.toFloat(), track, 3)
        post(CMD_AUDIO_CLIP_PARAM, fadeOutFrames.toFloat(), track, 4)
        post(CMD_AUDIO_CLIP_PARAM, offsetFrames.toFloat(), track, 6)
    }

    /** A song part's own loop for audio track [track] (follows [setStaging]): "once" clips play once per part. */
    fun setAudioLoop(track: Int, loopSteps: Int) = post(CMD_AUDIO_CLIP_PARAM, loopSteps.toFloat(), track, 1)

    /**
     * Opens the microphone and arms a recording of at most [seconds] (0 = microphone unavailable).
     * [onStart]: it begins exactly on the transport's first step (after the count-in), and the last
     * [prerollFrames] heard before it are kept in front of the take (what [recordFinish] returns
     * starts with them; zeros where the count-in was shorter).
     */
    external fun recordStart(onStart: Boolean, seconds: Int, prerollFrames: Int): Long
    /** [state 0 waiting, 1 recording, 2 finished; frames recorded (the pre-roll left out); input peak]. */
    external fun recordStatus(handle: Long): FloatArray
    /** Stops, frees the recording and returns it (mono, 48 kHz); null if the engine did not let go. */
    external fun recordFinish(handle: Long): FloatArray?
    private external fun nativeInstallAudioClip(track: Int, handle: Long): Boolean

    /** Instrument track [track]'s sampled voice plays [sample] across the keys (null: none). */
    fun setTrackSample(track: Int, sample: NativeSample?) {
        val target = captureTarget
        sink?.let { it.install(target, CommandSink.Install.TrackSample, track, sample?.handle ?: 0L); return }
        if (target != 0L) exportInstallTrackSample(target, track, sample?.handle ?: 0L)
        else nativeInstallTrackSample(track, sample?.handle ?: 0L)
    }

    /** [param] in SampleInstrument::Param order (0 root note … 9 gain), in that parameter's unit. */
    fun setTrackSampleParam(param: Int, value: Float, track: Int = 0) = post(CMD_TRACK_SAMPLE_PARAM, value, onSound(track, param))

    /**
     * Zone [zone] (0..511) of instrument track [track]'s multizone instrument plays [sample] (a
     * [NativeSample.createCompact] one; null: none). The zone's keys and the rest: [setTrackZoneParam].
     */
    fun setTrackZone(track: Int, zone: Int, sample: NativeSample?) {
        val target = captureTarget
        // The JNI's track argument: the track, and the zone + 1 above it (0 would be the sampled source).
        val packed = track.coerceIn(0, SOUNDS - 1) or ((zone.coerceIn(0, MULTI_ZONES - 1) + 1) shl 8)
        sink?.let { it.install(target, CommandSink.Install.TrackSample, packed, sample?.handle ?: 0L); return }
        if (target != 0L) exportInstallTrackSample(target, packed, sample?.handle ?: 0L)
        else nativeInstallTrackSample(packed, sample?.handle ?: 0L)
    }

    /** A zone's setting in MultiSampler::ZoneParam order (keys, velocities, root, gain, round robin, trigger, loop), in its unit. */
    fun setTrackZoneParam(track: Int, zone: Int, param: Int, value: Float) = post(CMD_TRACK_ZONE, value, onSound(track, param), zone.coerceIn(0, MULTI_ZONES - 1))

    /** The multizone instrument's own settings, in MultiSampler::Param order (0 the zones in use … 5 release samples' decay). */
    fun setTrackMultiParam(track: Int, param: Int, value: Float) = post(CMD_TRACK_ZONE, value, onSound(track, param), -1)

    /** The track's 808 ([setSynthEngine] 4): a setting in DrumKit::VoiceParam order ([VOICE_DECAY] … [VOICE_TONE]; the tune is its notes'). */
    fun setTrack808Param(track: Int, param: Int, value: Float) = post(CMD_TRACK_808, value, onSound(track, param))

    /** Drum pad [pad] plays through its own mixer strip (its own effects) or straight into the drums'. */
    fun setPadStrip(pad: Int, on: Boolean) = post(CMD_PAD_STRIP, if (on) 1f else 0f, pad)

    /** The master's gain before the limiter (linear, 1 = unity). */
    fun setMasterGain(gain: Float) = post(CMD_MASTER, gain, 0)
    /** The master's limiter (off only for a pre-master export: the mix keeps its headroom). */
    fun setLimiter(on: Boolean) = post(CMD_MASTER, if (on) 1f else 0f, 1)
    /** The master's inserts (off for stems, which then add back up to the mix). */
    fun setMasterFx(on: Boolean) = post(CMD_MASTER, if (on) 1f else 0f, 2)
    /**
     * The Live page's own sounds (riser, downlifter, the build-up's noise): [LIVE_NOISES_MIXED] in
     * the mix, [LIVE_NOISES_OFF] left out (a track's stem), [LIVE_NOISES_ONLY] alone (the stem « Effets live »).
     */
    fun setLiveNoises(mode: Int) = post(CMD_MASTER, mode.toFloat(), 3)
    /**
     * The master's loudness target in LUFS (« Cible »), or null for none: the engine moves a gain
     * before the clipper and the limiter to bring the music there (slowly, from what it hears; an
     * export masters the whole song at once).
     */
    fun setLoudnessTarget(lufs: Float?) = post(CMD_MASTER, lufs ?: 0f, 4)
    /** The master limiter's ceiling in dBTP (−1 by default; −2 for an M4A). */
    fun setMasterCeiling(dbtp: Float) = post(CMD_MASTER, dbtp, 5)
    /** Another project: the target's estimate of the music's loudness starts over. */
    fun resetLoudnessEstimate() = post(CMD_MASTER, 0f, 6)
    /** S4: the whole mix's lows in mono under [hz] (its side high-passed, 24 dB / octave); 0 = off, as before. */
    fun setMonoBass(hz: Float) = post(CMD_MASTER, hz, 7)
    /** S5 (mix version 2): the drive an export of this song found (dB): the live target settles there. */
    fun setKnownDrive(db: Float) = post(CMD_MASTER, db, 8)
    /**
     * S7 son (mix version 4): the level (LUFS, before the drive) of the loud part [setKnownDrive]'s drive
     * was found on; sent before it. A loss past the cap on a part over 1 LU quieter does not move it. NaN: none.
     */
    fun setKnownLevel(lufs: Float) = post(CMD_MASTER, lufs, 10)
    /**
     * S6 (an export engine only): the loudness target's drive searched as at this ceiling (dBTP; the
     * WAV's −1), the music then limited at [setMasterCeiling]'s: an M4A as loud as its WAV. NaN: none.
     */
    fun setDriveCeiling(dbtp: Float) = post(CMD_MASTER, dbtp, 9)
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

    /**
     * The live grid: the slots of [mask] (1 shl slot) take over their part of [scene] (staged with
     * [setStaging] just before) or stop ([scene] < 0) on the first transport step that is a
     * multiple of [quantumSteps]. The whole mask is a row (the scene changes, [sceneSwaps]).
     * [graceSteps]: a command that comes less than that after a line launches on that line (from
     * the next step, counted from the line): a tap on the « one » heard is not a quantum late.
     */
    fun armLaunch(mask: Int, quantumSteps: Int, scene: Int, graceSteps: Double = 0.0): Boolean {
        val q = quantumSteps.coerceIn(1, 1 shl 16)
        val grace = (graceSteps * 64).toInt().coerceIn(0, 0x3FFF)
        return post(CMD_ARM_LAUNCH, scene.toFloat(), mask and ALL_SLOTS, q or (grace shl 17))
    }
    fun cancelLaunch(mask: Int) = post(CMD_CANCEL_LAUNCH, 0f, mask and ALL_SLOTS)
    /** Slot [slot]'s next launch (or scene swap) starts [steps] into its part. */
    fun setSlotOffset(slot: Int, steps: Int) = post(CMD_SLOT_OFFSET, 0f, slot, steps.coerceAtLeast(0))
    /** The return of send bus [bus] (0 reverb, 1 echo) times [gain] (0..2, 1 = as mixed): a live gesture, not saved. */
    fun setBusReturn(bus: Int, gain: Float) = post(CMD_BUS_RETURN, gain.coerceIn(0f, 2f), bus)
    /** Transport steps since play (-1 stopped or counting in): the live grid's clock. */
    fun transportStep(): Int = sink?.transportStep() ?: nativeTransportStep()
    private external fun nativeTransportStep(): Int
    /** The transport's place in sixteenths since play, with the fraction (-1 stopped or counting in). */
    fun transportPosition(): Double = sink?.transportPosition() ?: nativeTransportPosition()
    private external fun nativeTransportPosition(): Double
    /**
     * A recorded performance starts on the next transport step that is a multiple of [quantumSteps]:
     * the song position there becomes [songStep] ([quantumSteps] 0: cancelled).
     */
    fun markSong(quantumSteps: Int, songStep: Int) = post(CMD_MARK_SONG, songStep.toFloat(), quantumSteps.coerceAtLeast(0))
    /** The transport step the last [markSong] took effect on (-1: not yet). */
    fun songMarkStep(): Int = sink?.songMarkStep() ?: nativeSongMarkStep()
    private external fun nativeSongMarkStep(): Int
    /** Per slot, 3 values: tag playing (scene, -1 stopped, [ROW_TAG]), tag waiting ([NO_LAUNCH], -1 stop, scene), start step. */
    fun slotStates(out: IntArray) {
        sink?.let { it.slotStates(out); return }
        nativeSlotStates(out)
    }
    private external fun nativeSlotStates(out: IntArray)
    /** The launches that happened since the last call into [out] (slot, tag, step each); returns how many. */
    fun drainLaunchEvents(out: IntArray): Int = sink?.drainLaunchEvents(out) ?: nativeDrainLaunchEvents(out)
    private external fun nativeDrainLaunchEvents(out: IntArray): Int

    /** [values] in Sampler::Param order (see PadSample.engineParams). */
    fun setPadParams(pad: Int, values: FloatArray) {
        values.forEachIndexed { param, v -> post(CMD_PAD_PARAM, v, pad, param) }
    }

    /** Instrument track [track] plays [font]'s preset [presetIndex] (null: back to its synth). Main thread. */
    /** False when the live engine did not take it (queue full, stream not running). */
    fun setInstrument(font: SoundFont?, presetIndex: Int, track: Int = 0): Boolean {
        val target = captureTarget
        val a = onSound(track, presetIndex)
        sink?.let { s ->
            // Banks are not recorded: a sink only ever hears the track back to its synth.
            if (font != null) return false
            s.install(target, CommandSink.Install.Instrument, a, 0L)
            return true
        }
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
    external fun sampleCreateCompact(data: FloatArray, channels: Int, sampleRate: Int): Long
    external fun sampleRelease(handle: Long)
    external fun sampleFrames(handle: Long): Long
    external fun sampleOnsets(handle: Long, sensitivity: Float, maxSlices: Int): FloatArray
    external fun sampleStretched(handle: Long, start: Float, end: Float, outFrames: Long, semitones: Float): Long
    /** Worker thread: [data] (interleaved, [channels]) lasting [outFrames], pitch kept; null when it cannot be made. */
    external fun stretchPcm(data: FloatArray, channels: Int, outFrames: Long, sampleRate: Int): FloatArray?
    private external fun nativeInstallSample(pad: Int, handle: Long): Boolean
    external fun stemsLoad(paths: Array<String>): Long
    external fun stemsRelease(handle: Long)
    /** A bundle playing [data] (interleaved stereo at 48 kHz) on its first stem, the others silent; 0 when refused. */
    external fun stemsFromPcm(data: FloatArray): Long
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
    external fun exportSetOptions(handle: Long, autoTail: Boolean, floatWav: Boolean)
    external fun exportSetFormat(handle: Long, sampleRate: Int, bits: Int)
    external fun exportReport(handle: Long): FloatArray
    external fun exportProgress(handle: Long): Float
    external fun exportRenderFloat(handle: Long, frames: Long, tail: Long): FloatArray
    external fun exportRenderFloatMono(handle: Long, frames: Long, tail: Long): FloatArray
    external fun exportRenderReport(handle: Long, frames: Long, tail: Long): Int
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
    /** Times the output stream was reopened after Android closed it (headphones, Bluetooth, reroute). */
    val restarts: Int = 0,
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
            restarts = v.getOrElse(14) { 0.0 }.toInt(),
        )
    }
}
