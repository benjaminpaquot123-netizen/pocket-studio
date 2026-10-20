package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * A song project. Immutable: every edit produces a new value, which makes undo/redo,
 * autosave and handing snapshots to the audio engine trivial.
 *
 * Format rules: every field has a default, unknown keys are ignored. Adding a field never
 * breaks old files; renaming or changing a field's meaning requires bumping [FORMAT_VERSION]
 * and a migration in [ProjectJson].
 */
@Serializable
data class Project(
    val formatVersion: Int = FORMAT_VERSION,
    val name: String = "Nouveau projet",
    val tempoBpm: Double = 120.0,
    /** 0 = straight, 0.5 = heavy shuffle (delay of every second 16th, as a fraction of a 16th). */
    val swing: Double = 0.0,
    val drums: DrumPattern = DrumPattern(),
    val mixer: Mixer = Mixer(),
    val acid: AcidPattern = AcidPattern(),
    val synth: SynthPatch = SynthPatch(),
    val synthNotes: NoteClip = NoteClip(),
    val harmony: Harmony = Harmony(),
    /** The synth track's sound: null = the subtractive synth, else a General MIDI instrument. */
    val instrument: InstrumentChoice? = null,
    /** A separated song played along (Remix tab). */
    val remix: Remix = Remix(),
    /** Mixer settings that move with the loop, step by step. */
    val automation: List<AutomationLane> = emptyList(),
    /** Session: up to 24 scenes; [scene] is the one shown and played (mirrored in drums/acid/notes). */
    val scenes: List<Scene> = emptyList(),
    val scene: Int = 0,
    /** The song: scenes in order with repeat counts. */
    val song: List<SongPart> = emptyList(),
    /** The synth track's own sound: 0 = subtractive synth, 1 = Plaits, 2 = [sampled] (an [instrument] still wins). */
    val synthEngine: Int = 0,
    val plaits: PlaitsPatch = PlaitsPatch(),
    /** The first instrument track's sample played across the keys (see [SampledPatch]). */
    val sampled: SampledPatch = SampledPatch(),
    /** Live pads: sounds switched on and off by a tap, in time. */
    val live: LivePads = LivePads(),
    /** The first instrument track's name and colour (its sound is [synth] / [plaits] / [instrument]). */
    val synthTrackName: String = "Synthé",
    val synthTrackColor: Int = 0,
    /** Instrument tracks added after the first one (see [InstrumentTrack]). */
    val tracks: List<InstrumentTrack> = emptyList(),
    /** Audio tracks: recorded or imported clips (see [AudioTrack]). */
    val audioTracks: List<AudioTrack> = emptyList(),
    /** Time signature [beatsPerBar]/4 (2 to 7). */
    val beatsPerBar: Int = 4,
    /** MIDI learn: controller knobs mapped to settings. */
    val midiMap: List<MidiMapping> = emptyList(),
) {
    /** Sixteenths in a bar. */
    val stepsPerBar: Int get() = beatsPerBar * 4

    /** Returns a copy with every value inside its allowed range. */
    fun sanitized(): Project = copy(
        tempoBpm = tempoBpm.coerceIn(MIN_TEMPO, MAX_TEMPO),
        swing = swing.coerceIn(0.0, MAX_SWING),
        drums = drums.sanitized(),
        acid = acid.sanitized(),
        synth = synth.sanitized(),
        synthNotes = synthNotes.sanitized(),
        harmony = harmony.sanitized(),
        instrument = instrument?.takeIf { it.bank in 0..128 && it.program in 0..127 },
        mixer = mixer.sanitized(),
        remix = remix.sanitized(),
        automation = AutomationLane.sanitize(automation),
        synthEngine = synthEngine.coerceIn(0, 2),
        plaits = plaits.sanitized(),
        sampled = sampled.sanitized(),
        live = live.sanitized(),
        synthTrackName = synthTrackName.take(InstrumentTrack.MAX_NAME).ifBlank { "Synthé" },
        synthTrackColor = synthTrackColor.coerceIn(0, InstrumentTrack.COLORS - 1),
        tracks = tracks.filter { it.id in 1 until InstrumentTrack.MAX }.distinctBy { it.id }.sortedBy { it.id }.map { it.sanitized() },
        beatsPerBar = beatsPerBar.coerceIn(MIN_BEATS, MAX_BEATS),
        midiMap = MidiMapping.sanitize(midiMap),
        audioTracks = audioTracks.filter { it.id in 0 until AudioTrack.MAX }.distinctBy { it.id }.sortedBy { it.id }.map { it.sanitized() },
    ).syncScenes().withFittedSampleRoots()

    companion object {
        const val FORMAT_VERSION = 1
        const val MIN_TEMPO = 40.0
        const val MAX_TEMPO = 300.0
        const val MAX_SWING = 0.75
        const val MIN_BEATS = 2
        const val MAX_BEATS = 7
    }
}

@Serializable
data class DrumPattern(
    val steps: Int = 16,
    val lanes: List<DrumLane> = List(PAD_COUNT) { DrumLane() },
) {
    fun sanitized(): DrumPattern {
        val s = steps.coerceIn(MIN_STEPS, MAX_STEPS) / 4 * 4
        val fixedLanes = (lanes + List((PAD_COUNT - lanes.size).coerceAtLeast(0)) { DrumLane() }).take(PAD_COUNT)
        return copy(
            steps = s,
            lanes = fixedLanes.map { lane ->
                lane.copy(
                    // Hits past the loop's end stay, unheard: a time signature tried and undone gives them back.
                    hits = lane.hits.filterKeys { it in 0 until MAX_STEPS }.mapValues { (_, h) -> h.sanitized() },
                    sample = lane.sample?.takeIf { it.isSafe }?.sanitized(),
                    pan = lane.pan.finite(0f).coerceIn(-1f, 1f),
                )
            },
        )
    }

    /**
     * The loop at [newSteps] (whole bars, 16 to 64): a longer loop repeats the current bars so the
     * groove keeps going; a shorter one drops the hits past its end (undo brings them back).
     */
    fun withSteps(newSteps: Int): DrumPattern {
        val n = (newSteps.coerceIn(MIN_STEPS, MAX_STEPS) / 4) * 4
        if (n == steps) return this
        return copy(
            steps = n,
            lanes = lanes.map { lane ->
                val hits = if (n > steps) (0 until n).mapNotNull { st -> lane.hits[st % steps]?.let { st to it } }.toMap()
                else lane.hits.filterKeys { it < n }
                lane.copy(hits = hits)
            },
        )
    }

    private fun withHits(lane: Int, hits: Map<Int, Hit>): DrumPattern =
        copy(lanes = lanes.toMutableList().also { it[lane] = it[lane].copy(hits = hits) })

    /** A hit every [every] steps over the whole loop (replacing the lane's hits). */
    fun fill(lane: Int, every: Int, velocity: Float = 1f): DrumPattern =
        withHits(lane, (0 until steps step every.coerceAtLeast(1)).associateWith { Hit(velocity) })

    /** The lane's hits moved by [by] steps, wrapping around the loop (those past its end stay put). */
    fun rotate(lane: Int, by: Int): DrumPattern =
        withHits(lane, lanes[lane].hits.mapKeys { (step, _) -> if (step < steps) Math.floorMod(step + by, steps) else step })

    /** Next roll for a hit: 1 → 2 → 3 → 4 → 1. No hit: nothing changes. */
    fun cycleRepeats(lane: Int, step: Int): DrumPattern {
        val hit = lanes[lane].hits[step] ?: return this
        return withHits(lane, lanes[lane].hits + (step to hit.copy(repeats = hit.repeats % MAX_REPEATS + 1)))
    }

    /** The pad's place in the stereo field, -1 (left) .. 1 (right). */
    fun withPadPan(lane: Int, pan: Float): DrumPattern =
        copy(lanes = lanes.toMutableList().also { it[lane] = it[lane].copy(pan = pan.coerceIn(-1f, 1f)) })

    /** One hit's own pan and pitch (no hit: nothing changes). */
    fun withHitDetail(lane: Int, step: Int, pan: Float, pitch: Int): DrumPattern {
        val hit = lanes[lane].hits[step] ?: return this
        return withHits(lane, lanes[lane].hits + (step to hit.copy(pan = pan, pitch = pitch).sanitized()))
    }

    fun withSample(lane: Int, sample: PadSample?): DrumPattern =
        copy(lanes = lanes.toMutableList().also { it[lane] = it[lane].copy(sample = sample) })

    fun toggle(lane: Int, step: Int, velocity: Float = 1f): DrumPattern {
        val current = lanes[lane]
        val hits = if (step in current.hits) current.hits - step else current.hits + (step to Hit(velocity))
        return copy(lanes = lanes.toMutableList().also { it[lane] = current.copy(hits = hits) })
    }

    companion object {
        const val PAD_COUNT = 16
        const val MAX_REPEATS = 4
        const val MIN_STEPS = 8
        const val MAX_STEPS = 64
    }
}

@Serializable
data class DrumLane(
    /** step index -> hit. A map keeps sparse patterns small on disk. */
    val hits: Map<Int, Hit> = emptyMap(),
    /** A sound of the user's that replaces the synthesized drum (null = the kit sound). */
    val sample: PadSample? = null,
    /** -1 (left) .. 1 (right); like the sound, the same in every scene. */
    val pan: Float = 0f,
)

/**
 * A pad's own sound. [file] lives in the project folder under `samples/`; [name] is what the
 * user sees. The rest shapes playback (units match the engine's Sampler::Param).
 */
@Serializable
data class PadSample(
    val file: String,
    val name: String,
    val start: Float = 0f,
    val end: Float = 1f,
    val pitch: Float = 0f,
    val reverse: Boolean = false,
    val loop: Boolean = false,
    val attack: Float = 0f,
    val decay: Float = 0f,
    val sustain: Float = 1f,
    val release: Float = 0.003f,
    val choke: Int = 0,
    val gain: Float = 1f,
    /** 0 = plays at its own speed; otherwise lasts this many beats at the project tempo (pitch kept). */
    val fitBeats: Int = 0,
) {
    /** Only a plain file name may come from disk: never a path. */
    val isSafe: Boolean get() = file.isNotEmpty() && file.length <= 80 && file.matches(Regex("[A-Za-z0-9._-]+")) && !file.startsWith(".")

    fun sanitized(): PadSample {
        val s = start.finite(0f).coerceIn(0f, 0.999f)
        val e = end.finite(1f).coerceIn(0.001f, 1f)
        return copy(
            name = name.take(40),
            start = minOf(s, e - 0.001f).coerceAtLeast(0f),
            end = maxOf(e, s + 0.001f).coerceAtMost(1f),
            pitch = pitch.finite(0f).coerceIn(-24f, 24f),
            attack = attack.finite(0f).coerceIn(0f, 2f),
            decay = decay.finite(0f).coerceIn(0f, 2f),
            sustain = sustain.finite(1f).coerceIn(0f, 1f),
            release = release.finite(0.003f).coerceIn(0f, 2f),
            choke = choke.coerceIn(0, CHOKE_GROUPS),
            gain = gain.finite(1f).coerceIn(0f, 2f),
            fitBeats = fitBeats.takeIf { it in FIT_BEATS } ?: 0,
        )
    }

    /**
     * Values in engine order (Sampler::Param). [stretched]: the engine plays a copy already cut
     * to the region, fitted to the tempo and transposed, so region and pitch are neutral.
     */
    fun engineParams(stretched: Boolean = false): FloatArray = floatArrayOf(
        if (stretched) 0f else start, if (stretched) 1f else end, if (stretched) 0f else pitch,
        if (reverse) 1f else 0f, if (loop) 1f else 0f,
        attack, decay, sustain, release, choke.toFloat(), gain,
    )

    /** Frames at 48 kHz this pad lasts when fitted to [bpm]. */
    fun fittedFrames(bpm: Double): Long = Math.round(fitBeats * 60.0 / bpm * 48_000)

    companion object {
        const val CHOKE_GROUPS = 4
        val FIT_BEATS = listOf(0, 1, 2, 4, 8, 16, 32)

        /** The number of beats (from [FIT_BEATS]) closest to [seconds] at [bpm]. */
        fun suggestBeats(seconds: Double, bpm: Double): Int {
            val beats = seconds * bpm / 60.0
            return FIT_BEATS.drop(1).minBy { kotlin.math.abs(kotlin.math.ln(it / beats)) }
        }
    }
}

private fun Float.finite(default: Float) = if (isFinite()) this else default

@Serializable
data class Hit(
    val velocity: Float = 1f,
    /** Hits in the step: 1 = plain, 2 to 4 = a roll (the step is split evenly). */
    val repeats: Int = 1,
    /** Added to the pad's pan for this hit, -1..1. */
    val pan: Float = 0f,
    /** Semitones for this hit, -24..24 (the pad's own sounds; the kit's synthesized drums ignore it). */
    val pitch: Int = 0,
) {
    val isPlain: Boolean get() = pan == 0f && pitch == 0

    fun sanitized() = copy(
        repeats = repeats.coerceIn(1, DrumPattern.MAX_REPEATS),
        pan = pan.finite(0f).coerceIn(-1f, 1f),
        pitch = pitch.coerceIn(-MAX_PITCH, MAX_PITCH),
    )

    companion object {
        const val MAX_PITCH = 24
    }
}

@Serializable
data class Mixer(
    val channels: List<Channel> = List(CHANNELS) { Channel() },
    /** Inserts on the master, after the send returns and before the limiter. */
    val master: List<Insert> = List(INSERTS) { Insert() },
    /** Parameters of the send buses (see [Buses]). */
    val buses: List<List<Float>> = Buses.DEFAULTS,
) {
    fun sanitized(): Mixer = copy(
        channels = (channels + List((CHANNELS - channels.size).coerceAtLeast(0)) { Channel() })
            .take(CHANNELS)
            .map { it.sanitized() },
        master = sanitizeInserts(master),
        buses = Buses.sanitized(buses),
    )

    companion object {
        /** 0..15 the tracks (4 is the master's index), 16..31 the drum pads' own strips. */
        const val CHANNELS = 32
        const val FIRST_PAD = 16
        const val INSERTS = 4
        /** The master's index where a channel number is expected (automation, engine). */
        const val MASTER = 4

        /** The strip of drum pad [lane] (its own effects, before the drums' strip). */
        fun padChannel(lane: Int) = FIRST_PAD + lane

        fun sanitizeInserts(inserts: List<Insert>): List<Insert> =
            (inserts + List((INSERTS - inserts.size).coerceAtLeast(0)) { Insert() }).take(INSERTS).map { it.sanitized() }
    }
}

/** Something on the strip changes the sound (a pad's strip is only used then). */
fun Channel.hasOwnFx(): Boolean =
    inserts.any { it.type != 0 } || macros != Macros.DEFAULTS || sends.any { it > 0f } || volume != 0.8f || pan != 0f

@Serializable
data class Channel(
    val volume: Float = 0.8f,
    val pan: Float = 0f,
    val mute: Boolean = false,
    val solo: Boolean = false,
    val inserts: List<Insert> = List(Mixer.INSERTS) { Insert() },
    /** Post-fader sends to the buses (0 = reverb, 1 = delay), 0..1. */
    val sends: List<Float> = List(Buses.COUNT) { 0f },
    /** The quick controls ([Macros]: colour, dirt, motion, space), after the inserts. */
    val macros: List<Float> = Macros.DEFAULTS,
) {
    fun sanitized(): Channel = copy(
        volume = volume.coerceIn(0f, 1f),
        pan = pan.coerceIn(-1f, 1f),
        inserts = Mixer.sanitizeInserts(inserts),
        sends = (sends + List((Buses.COUNT - sends.size).coerceAtLeast(0)) { 0f }).take(Buses.COUNT)
            .map { v -> v.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f },
        macros = Macros.sanitized(macros),
    )
}
