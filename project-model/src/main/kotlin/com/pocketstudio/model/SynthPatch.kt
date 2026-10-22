package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * Subtractive synth settings, 0..1 each, in the engine's Synth::Param order.
 * Stored as a list so new parameters can be appended without breaking old projects.
 */
@Serializable
data class SynthPatch(val values: List<Float> = DEFAULTS) {
    fun get(index: Int): Float = values.getOrElse(index) { DEFAULTS[index] }

    fun with(index: Int, value: Float): SynthPatch =
        copy(values = sanitized().values.toMutableList().also { it[index] = value.coerceIn(0f, 1f) })

    /** This sound with [other]'s loudness calibration ([GAIN]): the same sound, whatever its calibration. */
    fun withGainOf(other: SynthPatch): SynthPatch = with(GAIN, other.get(GAIN))

    fun sanitized(): SynthPatch =
        SynthPatch(List(PARAM_COUNT) { i -> values.getOrElse(i) { DEFAULTS[i] }.finite(DEFAULTS[i]).coerceIn(0f, 1f) })

    companion object {
        const val OSC1_WAVE = 0; const val OSC2_WAVE = 1; const val OSC2_DETUNE = 2; const val OSC2_OCTAVE = 3
        const val OSC_MIX = 4; const val SUB = 5; const val NOISE = 6; const val FILTER_TYPE = 7
        const val CUTOFF = 8; const val RESONANCE = 9; const val FILTER_ENV = 10
        const val F_ATTACK = 11; const val F_DECAY = 12; const val F_SUSTAIN = 13; const val F_RELEASE = 14
        const val A_ATTACK = 15; const val A_DECAY = 16; const val A_SUSTAIN = 17; const val A_RELEASE = 18
        const val LFO1_RATE = 19; const val VIBRATO = 20; const val LFO2_RATE = 21; const val SWEEP = 22; const val LEVEL = 23
        /** Copies of oscillator 1 (0 = none .. 1 = six, a supersaw) and how far apart they are tuned. */
        const val UNISON = 24; const val UNISON_DETUNE = 25
        /**
         * The filter sweep locked to the tempo (0 = free, at [LFO2_RATE]; then a bar, 1/2, 1/4, 1/8,
         * 1/16, 1/4 and 1/8 triplets: value = index / 7, see [syncValue]), restarted on each attack: the wobble.
         */
        const val LFO2_SYNC = 26
        /** Portamento (0 = none, up to 0.5 s): a note that starts while another is held slides from it. */
        const val GLIDE = 27
        /**
         * The sound's loudness calibration (0.5 = 0 dB, 0..1 = -30..+30 dB, engine Synth::Gain): set by
         * the library so that every sound plays about as loud as the others; not a knob.
         */
        const val GAIN = 28
        const val PARAM_COUNT = 29

        /** Decibels of [GAIN] at 0 and 1 (engine Synth::kGainRangeDb). */
        const val GAIN_RANGE_DB = 30f

        /** The [GAIN] (or [PlaitsPatch.gain]) value of [db] decibels. */
        fun gainValue(db: Float): Float = (0.5f + db / (2 * GAIN_RANGE_DB)).coerceIn(0f, 1f)

        /** The decibels of a [GAIN] (or [PlaitsPatch.gain]) value. */
        fun gainDb(value: Float): Float = (value - 0.5f) * 2 * GAIN_RANGE_DB

        /** Tempo divisions of [LFO2_SYNC] (engine Synth::kSyncDivisions), free included. */
        const val SYNC_DIVISIONS = 8
        const val SYNC_BAR = 1; const val SYNC_HALF = 2; const val SYNC_QUARTER = 3; const val SYNC_EIGHTH = 4
        const val SYNC_SIXTEENTH = 5; const val SYNC_QUARTER_TRIPLET = 6; const val SYNC_EIGHTH_TRIPLET = 7

        /** The [LFO2_SYNC] value for tempo division [index] (0 = free, [SYNC_BAR]…[SYNC_EIGHTH_TRIPLET]). */
        fun syncValue(index: Int): Float = index.coerceIn(0, SYNC_DIVISIONS - 1) / (SYNC_DIVISIONS - 1f)

        /** Same values as Synth::prepare() in the engine. */
        val DEFAULTS: List<Float> = listOf(
            0f, 0f, 0.3f, 0.5f, 0.5f, 0.2f, 0f,
            0f, 0.55f, 0.3f, 0.4f,
            0f, 0.3f, 0.3f, 0.3f,
            0.05f, 0.3f, 0.8f, 0.35f,
            0.5f, 0f, 0.3f, 0f, 0.6f,
            0f, 0.3f,
            0f, 0f,
            0.5f,
        )

        /** Ready-made sounds: name -> patch. Waves: 0 = saw, 0.5 = square, 1 = triangle. */
        val PRESETS: List<Pair<String, SynthPatch>> = listOf(
            "Basse" to SynthPatch(DEFAULTS).with(OSC2_OCTAVE, 0.1f).with(SUB, 0.6f).with(CUTOFF, 0.38f)
                .with(RESONANCE, 0.35f).with(FILTER_ENV, 0.5f).with(F_DECAY, 0.25f).with(F_SUSTAIN, 0.1f)
                .with(A_ATTACK, 0f).with(A_RELEASE, 0.2f),
            "Lead" to SynthPatch(DEFAULTS).with(OSC2_WAVE, 0.5f).with(OSC2_DETUNE, 0.5f).with(CUTOFF, 0.7f)
                .with(RESONANCE, 0.45f).with(VIBRATO, 0.15f).with(LFO1_RATE, 0.55f).with(A_RELEASE, 0.3f),
            "Nappe" to SynthPatch(DEFAULTS).with(OSC2_DETUNE, 0.8f).with(CUTOFF, 0.5f).with(FILTER_ENV, 0.2f)
                .with(A_ATTACK, 0.65f).with(A_SUSTAIN, 0.9f).with(A_RELEASE, 0.75f).with(SWEEP, 0.3f).with(LFO2_RATE, 0.2f),
            // Decays of about 50 ms (filter) and 140 ms (level): a pluck, not a click.
            "Pluck" to SynthPatch(DEFAULTS).with(OSC1_WAVE, 0.5f).with(CUTOFF, 0.35f).with(RESONANCE, 0.5f)
                .with(FILTER_ENV, 0.7f).with(F_DECAY, 0.45f).with(F_SUSTAIN, 0f).with(A_DECAY, 0.58f)
                .with(A_SUSTAIN, 0f).with(A_RELEASE, 0.25f),
        )
    }
}
