package com.pocketstudio.model

import kotlin.math.ln

/**
 * The leads chantier, lot 1 (docs/hits/leads-pros.md § 4 and § 8 « Lot A »): the library's leads,
 * pads, plucks and keys set as producers set them, from what was measured as « vieux synthé
 * amateur ». No square sub an octave under a voice that is not a bass (it put 150-500 Hz up to
 * +20 dB over 1-4 kHz); the state-variable filter whenever copies or two detuned oscillators play
 * (the ladder's inner saturation left -32 to -35 dB of intermodulation on a stack of saws, the SVF
 * -67 dB); open cut-offs per role; releases of 100 to 400 ms instead of notes cut in 3 to 46 ms;
 * a vibrato at 5 Hz and +-6 to 8 cents instead of a slow, wide tape wobble.
 *
 * Each library sound is its sound before the chantier ([SoundLibrary.SYNTH_V2], which projects saved
 * earlier keep: their patches are in their files) with [SYNTH_EDITS] applied, its loudness kept by
 * [GAIN_DB] (measured: each sound previewed alone as ▶ plays it, the same short-term loudness as before).
 */
object ProVoices {
    /** [SynthPatch.FILTER_TYPE] of the state-variable low-pass (the engine's type 1). */
    const val SVF = 0.3f

    /** [SynthPatch.LFO1_RATE] of 5 Hz (0.05 x 400^v) and [SynthPatch.VIBRATO] of +-8 cents (v x 200). */
    const val VIBRATO_RATE = 0.77f
    const val VIBRATO_8_CENTS = 0.04f

    /** What changes in each sound (parameter to value), by its name in the library. */
    val SYNTH_EDITS: Map<String, List<Pair<Int, Float>>> = with(SynthPatch) {
        mapOf(
            // Leads: ~4 kHz open in the SVF (two detuned oscillators through the ladder: -42 dB of
            // intermodulation at C6, the SVF -53), the vibrato at 5 Hz, a 200 ms release.
            "Lead" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.78f, RESONANCE to 0.3f, VIBRATO to VIBRATO_8_CENTS, LFO1_RATE to VIBRATO_RATE, A_RELEASE to 0.62f),
            "Lead carré" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.78f, VIBRATO to VIBRATO_8_CENTS, LFO1_RATE to VIBRATO_RATE, A_RELEASE to 0.62f),
            "Lead acide" to listOf(SUB to 0f, A_RELEASE to 0.55f),
            // The supersaw open at 10 kHz in the SVF (§ 4.1), its envelope +1 octave over 300 ms, a 250 ms release.
            "Supersaw" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.92f, FILTER_ENV to 0.2f, F_DECAY to 0.67f, A_ATTACK to 0.15f,
                A_DECAY to 0.7f, A_SUSTAIN to 0.8f, A_RELEASE to 0.65f),
            "Lead trance" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.82f, A_RELEASE to 0.62f),
            // The hoover keeps its square an octave down (its identity, § 4.2), SVF at 3 kHz, no slow vibrato.
            "Hoover" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.74f, VIBRATO to 0f, A_RELEASE to 0.62f),
            // Pads at 2-3 kHz (§ 4.7).
            "Nappe" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.72f),
            "Nappe chaude" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.68f),
            "Accords future bass" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.75f),
            "Cordes" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.72f, VIBRATO to 0.03f, LFO1_RATE to VIBRATO_RATE),
            // Plucks (§ 4.8): opened by a 170 ms envelope over a 600 Hz base (SVF: -56 dB at C6, the ladder -40),
            // a 390 ms decay, a 250 ms release.
            "Pluck" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.5f, F_DECAY to 0.6f, A_DECAY to 0.7f, A_RELEASE to 0.65f),
            "Pluck future" to listOf(SUB to 0f, FILTER_TYPE to SVF, CUTOFF to 0.5f, A_DECAY to 0.7f, A_RELEASE to 0.65f),
            "Arpège" to listOf(SUB to 0f, CUTOFF to 0.55f, A_RELEASE to 0.55f),
            // A bell that rings (460 ms, not a 70 ms toy), the organ and the piano let go over 100-200 ms.
            "Cloche" to listOf(SUB to 0f, A_DECAY to 0.72f, A_RELEASE to 0.68f),
            "Orgue" to listOf(SUB to 0f, A_RELEASE to 0.55f),
            "Piano électrique" to listOf(SUB to 0f, CUTOFF to 0.7f, VIBRATO to 0f, A_RELEASE to 0.6f),
        )
    }

    /**
     * Each edited sound's loudness calibration change (dB, added to its [SoundLibrary.SYNTH_GAIN_DB]):
     * without the sub and through another filter a sound plays softer or louder; this brings it back
     * to the loudness it had (the loudest 3 s, K-weighted, of a line or a held chord played by the
     * engine's synth alone, before and after: scratchpad leads1/py/loud.py).
     */
    val GAIN_DB: Map<String, Float> = mapOf(
        "Lead" to -2.7f, "Lead carré" to -2.1f, "Lead acide" to 1.5f, "Supersaw" to -2.2f, "Lead trance" to -1.8f, "Hoover" to -2.5f,
        "Nappe" to -1.7f, "Nappe chaude" to -3.3f, "Accords future bass" to -1.9f, "Cordes" to -1.3f,
        "Pluck" to -7.1f, "Pluck future" to -7.5f, "Cloche" to -7f, "Orgue" to 0.5f, "Piano électrique" to 0.5f,
    )

    /** [sound] of the library before the chantier as it plays now. */
    fun synth(sound: NamedSound<SynthPatch>): NamedSound<SynthPatch> {
        val edits = SYNTH_EDITS[sound.name] ?: return sound
        val patch = edits.fold(sound.sound) { p, (i, v) -> p.with(i, v) }
        val db = SynthPatch.gainDb(sound.sound.get(SynthPatch.GAIN)) + (GAIN_DB[sound.name] ?: 0f)
        return sound.copy(sound = patch.with(SynthPatch.GAIN, SynthPatch.gainValue(db)))
    }

    /**
     * The leads chantier, lots B to D (docs/hits/leads-pros.md § 6 and § 8): what the engine gained,
     * set on each sound by its name. The cut-off follows the note in the engine ([SynthPatch.KEY_TRACK]:
     * the recipe's once-a-part [keyTracked] is then left out; plucks 0.7: at 1 their state-variable
     * filter opened so high on C6 that it aliased, −56 → −50 dB) and opens with the velocity; plucks and
     * keys « bark » when struck hard ([SynthPatch.VELOCITY_ENV]) and start with a 4 ms click and a
     * 12-semitone tick; the vibratos wait 250 ms; the supersaws are JP-8000s (free phases, drift,
     * Szabo's ratios, the high-pass at f0); the hoover's square breathes (PWM) and its note dives
     * 3 semitones; the ladder sounds are linear (no intermodulation). At C4 and velocity 0.7 each
     * sound keeps its loudness (none of these moves it there by more than a fraction of a dB).
     */
    val TOUCH_EDITS: Map<String, List<Pair<Int, Float>>> = with(SynthPatch) {
        val vibratoLate = VIBRATO_DELAY to vibratoDelayValue(250f)
        val tick = listOf(PITCH_ENV to pitchEnvValue(12f), PITCH_DECAY to pitchDecayValue(4f))
        mapOf(
            "Lead" to listOf(KEY_TRACK to 0.5f, VELOCITY_CUTOFF to 0.45f, VELOCITY_ENV to 0.6f, vibratoLate, CLICK to 0.12f),
            "Lead carré" to listOf(KEY_TRACK to 0.5f, VELOCITY_CUTOFF to 0.45f, VELOCITY_ENV to 0.6f, vibratoLate, CLICK to 0.12f),
            "Supersaw" to listOf(SUPERSAW to 1f, KEY_TRACK to 0.5f, VELOCITY_CUTOFF to 0.4f, VELOCITY_ENV to 1f, CLICK to 0.15f),
            "Lead trance" to listOf(SUPERSAW to 1f, KEY_TRACK to 0.5f, VELOCITY_CUTOFF to 0.45f, VELOCITY_ENV to 0.6f, CLICK to 0.1f),
            "Hoover" to listOf(SUPERSAW to 1f, PWM to 0.6f, PITCH_ENV to pitchEnvValue(3f), PITCH_DECAY to pitchDecayValue(150f), KEY_TRACK to 0.3f),
            "Nappe" to listOf(SUPERSAW to 1f, KEY_TRACK to 0.3f),
            "Nappe chaude" to listOf(SUPERSAW to 1f, KEY_TRACK to 0.3f),
            "Accords future bass" to listOf(SUPERSAW to 1f, KEY_TRACK to 0.3f, VELOCITY_CUTOFF to 0.3f),
            "Cordes" to listOf(SUPERSAW to 1f, KEY_TRACK to 0.3f, vibratoLate),
            "Pluck" to listOf(KEY_TRACK to 0.7f, VELOCITY_CUTOFF to 0.25f, VELOCITY_ENV to 0.5f, CLICK to 0.3f) + tick,
            "Pluck future" to listOf(KEY_TRACK to 0.7f, VELOCITY_CUTOFF to 0.25f, VELOCITY_ENV to 0.5f, CLICK to 0.3f) + tick,
            "Arpège" to listOf(KEY_TRACK to 0.8f, VELOCITY_CUTOFF to 0.2f, CLICK to 0.2f, CLEAN_LADDER to 1f),
            "Cloche" to listOf(KEY_TRACK to 0.5f, VELOCITY_CUTOFF to 0.25f, CLEAN_LADDER to 1f),
            "Orgue" to listOf(CLEAN_LADDER to 1f),
            "Piano électrique" to listOf(KEY_TRACK to 0.5f, VELOCITY_CUTOFF to 0.2f, VELOCITY_ENV to 0.7f, CLEAN_LADDER to 1f),
        )
    }

    /**
     * The loudness the JP-8000 unison takes off (its high-pass at the note, its centre and copies
     * mixed), given back (dB, the loudest 3 s of a held chord by the engine's synth alone, before and
     * after: scratchpad son8/sp/chord.cpp); the other sounds move by under 0.5 dB.
     */
    val TOUCH_GAIN_DB: Map<String, Float> = mapOf("Supersaw" to 1.4f, "Lead trance" to 1.3f, "Hoover" to 2.1f, "Accords future bass" to 1.3f)

    /** [sound] (lot 1 applied) with [TOUCH_EDITS]: the library's sound since the lots B to D. */
    fun touch(sound: NamedSound<SynthPatch>): NamedSound<SynthPatch> {
        val edits = TOUCH_EDITS[sound.name] ?: return sound
        val patch = edits.fold(sound.sound) { p, (i, v) -> p.with(i, v) }
        val db = SynthPatch.gainDb(patch.get(SynthPatch.GAIN)) + (TOUCH_GAIN_DB[sound.name] ?: 0f)
        return sound.copy(sound = patch.with(SynthPatch.GAIN, SynthPatch.gainValue(db)))
    }

    /**
     * [patch], the library's [name] since [touch], as it was before it ([TOUCH_EDITS] taken back to the
     * lot 1 values, its gain too): a lead's sound playing a bass (the grime square) keeps the low end
     * it had; its cut-off following the note around C4 would darken a line two octaves down.
     */
    fun untouched(patch: SynthPatch, name: String): SynthPatch {
        val edits = TOUCH_EDITS[name] ?: return patch
        val lot1 = SoundLibrary.SYNTH_V2.firstOrNull { it.name == name }?.let { synth(it).sound } ?: return patch
        val p = edits.fold(patch) { q, (i, _) -> q.with(i, lot1.get(i)) }
        val db = SynthPatch.gainDb(p.get(SynthPatch.GAIN)) - (TOUCH_GAIN_DB[name] ?: 0f)
        return p.with(SynthPatch.GAIN, SynthPatch.gainValue(db))
    }

    /** Octaves of [SynthPatch.CUTOFF] per unit (20 x 900^v Hz). */
    private val OCTAVES_PER_UNIT = (ln(900.0) / ln(2.0)).toFloat()

    /** [SynthPatch.CUTOFF] moved by [octaves] (kept under 0.95, about 13 kHz). */
    fun cutoffMoved(value: Float, octaves: Float): Float = (value + octaves / OCTAVES_PER_UNIT).coerceIn(0.05f, 0.95f)

    /** [octaves] as a change of [SynthPatch.CUTOFF]. */
    fun cutoffUnits(octaves: Float): Float = octaves / OCTAVES_PER_UNIT

    /**
     * The filter's key tracking, done once in the recipe (the engine's cut-off does not follow the
     * note yet: lot B): [patch]'s cut-off moved by [amount] octave per octave between C4 (60) and the
     * part's median note [median], so that a pluck played two octaves up is not twice as dark.
     */
    fun keyTracked(patch: SynthPatch, median: Int?, amount: Float): SynthPatch {
        if (median == null || amount == 0f) return patch
        return patch.with(SynthPatch.CUTOFF, cutoffMoved(patch.get(SynthPatch.CUTOFF), amount * (median - 60) / 12f))
    }

    /** How much each library family's cut-off follows the part's notes ([keyTracked]). */
    fun keyTracking(category: String?): Float = when (category) {
        "Pluck" -> 0.8f
        "Leads", "Claviers" -> 0.5f
        "Nappes" -> 0.3f
        else -> 0f
    }
}
