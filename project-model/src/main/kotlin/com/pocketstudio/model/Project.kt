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
) {
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
    )

    companion object {
        const val FORMAT_VERSION = 1
        const val MIN_TEMPO = 40.0
        const val MAX_TEMPO = 300.0
        const val MAX_SWING = 0.75
    }
}

@Serializable
data class DrumPattern(
    val steps: Int = 16,
    val lanes: List<DrumLane> = List(PAD_COUNT) { DrumLane() },
) {
    fun sanitized(): DrumPattern {
        val s = steps.coerceIn(MIN_STEPS, MAX_STEPS)
        val fixedLanes = (lanes + List((PAD_COUNT - lanes.size).coerceAtLeast(0)) { DrumLane() }).take(PAD_COUNT)
        return copy(
            steps = s,
            lanes = fixedLanes.map { lane ->
                lane.copy(hits = lane.hits.filterKeys { it in 0 until s }, sample = lane.sample?.takeIf { it.isSafe }?.sanitized())
            },
        )
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
        const val MIN_STEPS = 16
        const val MAX_STEPS = 64
    }
}

@Serializable
data class DrumLane(
    /** step index -> hit. A map keeps sparse patterns small on disk. */
    val hits: Map<Int, Hit> = emptyMap(),
    /** A sound of the user's that replaces the synthesized drum (null = the kit sound). */
    val sample: PadSample? = null,
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
data class Hit(val velocity: Float = 1f)

@Serializable
data class Mixer(val channels: List<Channel> = List(CHANNELS) { Channel() }) {
    fun sanitized(): Mixer = copy(
        channels = (channels + List((CHANNELS - channels.size).coerceAtLeast(0)) { Channel() })
            .take(CHANNELS)
            .map { it.copy(volume = it.volume.coerceIn(0f, 1f), pan = it.pan.coerceIn(-1f, 1f)) },
    )

    companion object {
        const val CHANNELS = 16
    }
}

@Serializable
data class Channel(
    val volume: Float = 0.8f,
    val pan: Float = 0f,
    val mute: Boolean = false,
    val solo: Boolean = false,
)
