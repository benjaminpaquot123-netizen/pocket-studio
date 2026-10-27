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

        // Phase 34, S4 (engine Synth.h): width and movement. At their defaults the sound is as before
        // them (every project saved earlier keeps its sound: its shorter list takes these defaults).
        /** The unison copies (without copies, oscillators 1 and 2) spread left and right; the mono sum unchanged. */
        const val WIDTH = 29
        /** Level of a copy of oscillator 1's wave an octave below the note (the automatic octave doubling). */
        const val OCTAVE_DOWN = 30
        /** Saturation after the filter (4x oversampled), its level kept. */
        const val DRIVE = 31
        /** The stereo folded to mono under 60 x 5^v Hz ([monoBelowValue]): 120 Hz by default, 250 Hz for a bass. */
        const val MONO_BELOW = 32
        /** The sweep's shape: [SHAPE_TRIANGLE] (as before), [SHAPE_SQUARE] (a riddim gate), [SHAPE_SAW], [SHAPE_SINE]. */
        const val LFO2_SHAPE = 33
        /** A slow filter sweep locked to the song's bars: 0 off, then 1, 2, 4, 8, 16 bars ([cycleValue]). */
        const val CYCLE_BARS = 34
        /** Its depth: 0..1 = 0..+-2 octaves (closed at the cycle's start, open half-way). */
        const val CYCLE_DEPTH = 35
        /** The vowel filter's share (0 = off) and its vowel (A, E, I, O, U over 0..1; the sweep moves it). */
        const val FORMANT = 36
        const val VOWEL = 37
        /** A layer of the sound above 250 Hz, driven and wide (the bass music « mid » over a mono sub). */
        const val MID_LAYER = 38
        /** Above 0.5: the oscillators oversampled (aliasing under -60 dB). */
        const val OVERSAMPLE = 39

        // « Des basses qui tapent » (docs/hits/basses-qui-tapent.md, P3; engine Synth.h), appended: at
        // their defaults the sound is as before them (a project saved earlier takes these defaults).
        /** The attack starts 0..48 semitones over the note and falls to it ([PITCH_DECAY]): the 808, the donk, the « wub ». */
        const val PITCH_ENV = 40
        /** That fall's time constant: 1..500 ms (500^v ms; 0.5: 22 ms). */
        const val PITCH_DECAY = 41
        /** Above 0.5: a note on a silent voice starts its oscillators at phase 0 (every hit attacks alike). */
        const val PHASE_RESET = 42
        /** The velocity opens the filter: ±2 octaves × this at velocity 1 / 0.4, none at 0.7. */
        const val VELOCITY_CUTOFF = 43
        /** The sub's wave: 0 square (as before), 0.5 triangle, 1 sine. */
        const val SUB_SHAPE = 44
        /** Above 0.5: the sub (and the octave-down copy) never under 30 Hz (an octave up there). */
        const val SUB_FLOOR = 45
        /** The mid layer's high-pass, 156..400 Hz (0.5: 250 Hz, as before), and its drive (0.5: x3, as before). */
        const val MID_LAYER_HZ = 46
        const val MID_LAYER_DRIVE = 47

        // The leads chantier, lots B to D (docs/hits/leads-pros.md § 6; engine Synth.h), appended: at their
        // defaults the sound is as before them (a project saved earlier takes these defaults).
        /** The cut-off follows the note: 0..1 octave per octave around C4. */
        const val KEY_TRACK = 48
        /** The velocity scales the filter envelope: ×(1 ± this) at velocity 1 / 0.4, as is at 0.7. */
        const val VELOCITY_ENV = 49
        /** A noise click of about 4 ms, high-passed at 2 kHz, on each attack (over the filter). */
        const val CLICK = 50
        /** Each note's vibrato waits 0..1 s, then fades in over as long. */
        const val VIBRATO_DELAY = 51
        /** The engine's [CYCLE_BARS] cycle shifted by this part of a cycle (0.5: open at its start). */
        const val CYCLE_OFFSET = 52
        /** Above 0.5: the unison as a JP-8000 supersaw (Szabo's ratios, free phases, drift, centre/side mix, high-pass at f0). */
        const val SUPERSAW = 53
        /** Above 0.5: the ladder filter linear (no inner saturation: clean on dense stacks). */
        const val CLEAN_LADDER = 54
        /** Saturation before the filter, on each voice. */
        const val PRE_DRIVE = 55
        /** The square waves' pulse width swept by a 0.7 Hz triangle (the hoover). */
        const val PWM = 56
        /** A 2-operator FM pair taking over oscillator 1 (its index falling with the filter envelope), and its ratio ([fmRatioValue]). */
        const val FM = 57
        const val FM_RATIO = 58
        const val PARAM_COUNT = 59

        /** The leads chantier's lots B to D ([KEY_TRACK]..[FM_RATIO]). */
        val LEAD_PARAMS: IntRange = KEY_TRACK..FM_RATIO

        /** [FM_RATIO]'s choices (engine Synth::fmRatioOf). */
        val FM_RATIOS = listOf(0.5f, 1f, 1.41f, 2f, 2.76f, 3f, 3.5f, 4f, 7f)

        /** The [FM_RATIO] value of the ratio nearest [ratio]. */
        fun fmRatioValue(ratio: Float): Float = FM_RATIOS.indices.minByOrNull { kotlin.math.abs(FM_RATIOS[it] - ratio) }!! / (FM_RATIOS.size - 1f)

        /** [VIBRATO_DELAY] for a delay of [ms] (0..1000). */
        fun vibratoDelayValue(ms: Float): Float = (ms / 1000f).coerceIn(0f, 1f)

        /** The parameters « des basses qui tapent » appended ([PITCH_ENV]..[MID_LAYER_DRIVE]). */
        val BASS_PARAMS: IntRange = PITCH_ENV..MID_LAYER_DRIVE

        /**
         * [patch] as it was written before [BASS_PARAMS] when they are all at their defaults (its first
         * [PITCH_ENV] values): what a frozen track's signature reads, so that it stays up to date.
         */
        fun beforeBassParams(patch: SynthPatch): SynthPatch {
            val p = beforeLeadParams(patch)
            return if (p.values.size > PITCH_ENV && BASS_PARAMS.all { p.get(it) == DEFAULTS[it] }) SynthPatch(p.values.take(PITCH_ENV)) else p
        }

        /** [patch] as written before [LEAD_PARAMS] when they are all at their defaults (its first [KEY_TRACK] values). */
        fun beforeLeadParams(patch: SynthPatch): SynthPatch =
            if (patch.values.size > KEY_TRACK && LEAD_PARAMS.all { patch.get(it) == DEFAULTS[it] }) SynthPatch(patch.values.take(KEY_TRACK)) else patch

        /** [PITCH_DECAY] for a fall of [ms] milliseconds (1..500). */
        fun pitchDecayValue(ms: Float): Float = (kotlin.math.ln(ms.coerceIn(1f, 500f)) / kotlin.math.ln(500f))

        /** [PITCH_ENV] for a drop of [semitones] (0..48). */
        fun pitchEnvValue(semitones: Float): Float = (semitones / 48f).coerceIn(0f, 1f)

        /** The parameters S4 added ([WIDTH]..[OVERSAMPLE]): production settings, not a sound's identity. */
        val S4_PARAMS: IntRange = WIDTH..OVERSAMPLE

        const val SHAPE_TRIANGLE = 0f
        const val SHAPE_SQUARE = 1f / 3f
        const val SHAPE_SAW = 2f / 3f
        const val SHAPE_SINE = 1f

        /** [MONO_BELOW] for [hz] (60..300 Hz). */
        fun monoBelowValue(hz: Float): Float = (kotlin.math.ln(hz.coerceIn(60f, 300f) / 60f) / kotlin.math.ln(5f))
        const val MONO_BELOW_DEFAULT = 0.43f

        /** [CYCLE_BARS] for a cycle of [bars] (1, 2, 4, 8 or 16; 0 = off). */
        fun cycleValue(bars: Int): Float = when (bars) {
            1 -> 0.2f; 2 -> 0.4f; 4 -> 0.6f; 8 -> 0.8f; 16 -> 1f
            else -> 0f
        }

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
            // S4: width, octave down, drive, mono below (120 Hz), sweep shape, cycle and depth,
            // formant and vowel, mid layer, oversampling: all off.
            0f, 0f, 0f, MONO_BELOW_DEFAULT, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            // « Des basses qui tapent »: no pitch envelope (its fall 22 ms), no phase reset, no velocity on
            // the cutoff, the square sub without a floor, the mid layer at 250 Hz driven x3: as before.
            0f, 0.5f, 0f, 0f, 0f, 0f, 0.5f, 0.5f,
            // The leads chantier, lots B to D: no key tracking, no velocity on the envelope, no click, the
            // vibrato at once, the cycle from its start, the linear unison, the ladder as before, no drive
            // before the filter, no pulse sweep, no FM (its ratio 1): as before.
            0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.125f,
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
