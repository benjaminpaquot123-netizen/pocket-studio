package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** A ready-made or saved sound: [category] groups it in the browser. */
@Serializable
data class NamedSound<T>(val name: String, val category: String, val sound: T)

/** Families shown in the browsers, in this order ("Favoris" holds the user's own). */
object SoundCategories {
    const val FAVORITES = "Favoris"
    val SYNTH = listOf("Basses", "Leads", "Nappes", "Pluck", "Claviers", "Effets")
    val PLAITS = listOf("Basses", "Leads", "Nappes", "Pluck", "Batterie", "Effets")
    val ACID = listOf("Classiques", "Sales", "Doux")
}

/** Built-in sounds of each instrument, and musical randomisation. */
object SoundLibrary {
    private fun synth(vararg values: Pair<Int, Float>): SynthPatch =
        values.fold(SynthPatch(SynthPatch.DEFAULTS)) { p, (i, v) -> p.with(i, v) }

    /**
     * The loudness calibration of each sound, in dB ([SynthPatch.GAIN], [PlaitsPatch.gain]): each one
     * alone (a bass on Do2, a held chord, a lead on Do4, one note a beat) measured by the engine and
     * brought to about the same loudness as the others of its use, -16 LUFS for basses, leads and
     * plucks, -14 for pads and keys, its peak kept under -1 dBFS (scratchpad p29corps/levels.sh).
     * Without it the library spread over 33 dB (a pluck 20 dB under a pad). The two subs stay as they
     * are: a sine reads quiet but weighs a lot.
     */
    val SYNTH_GAIN_DB: Map<String, Float> = mapOf(
        "Basse ronde" to 5f, "Basse reese" to 4.5f, "Reese moderne" to 5f, "Basse wobble" to 2f, "Basse qui parle" to 4.5f,
        "Basse growl" to 3f, "Reese DnB" to 3f, "Basse riddim" to 1.5f, "Basse neuro" to 5f, "Lead" to 2.5f, "Lead carré" to 1f, "Lead acide" to 6.5f, "Supersaw" to -2f, "Lead trance" to 1.5f,
        "Hoover" to 2.5f, "Nappe" to -3f, "Nappe chaude" to -4f, "Accords future bass" to -5f, "Cordes" to -4f, "Pluck" to 7f,
        "Pluck future" to 6f, "Arpège" to 7.5f, "Cloche" to 8.5f, "Orgue" to -2.5f, "Piano électrique" to 1.5f,
        // « Des basses qui tapent » (docs/hits/basses-qui-tapent.md, P5): measured the same way against
        // « Basse ronde » (scratchpad basses-calib/calib.py); « Sub chaud » stays as « Sub pur ».
        "Basse roulante" to 4f, "Basse pluck" to 8f, "808 synthé" to 2f, "Donk" to 6f, "Sub chaud" to 0f,
    )

    /** [SYNTH_GAIN_DB] of the Plaits sounds. */
    val PLAITS_GAIN_DB: Map<String, Float> = mapOf(
        "Basse analogique" to 5f, "Basse pincée" to 26f, "Log drum" to 8.5f, "Lead filtré" to -2f, "Lead chiptune" to -5.5f,
        "Voix robot" to 3.5f, "Nappe d'accords" to -5.5f, "Machine à cordes" to 2f, "Essaim" to 0.5f, "Piano FM" to -3f,
        "Rhodes FM" to -2f, "Vibraphone FM" to 5.7f, "Cloche FM" to 2.7f, "Stab métallique" to -3.5f,
        "Marimba" to 7f, "Cloche" to 5f,
    )

    /**
     * The synth's sounds as they were until the leads chantier (lot 1): every project saved before
     * keeps playing them (its patches are in its file) and [synthNameOf] still names them. [SYNTH]
     * is what the library plays now.
     */
    val SYNTH_V2: List<NamedSound<SynthPatch>> = with(SynthPatch) {
        listOf(
            NamedSound("Basse ronde", "Basses", SynthPatch.PRESETS[0].second),
            NamedSound("Basse sub", "Basses", synth(OSC1_WAVE to 1f, OSC_MIX to 0f, SUB to 0.9f, CUTOFF to 0.3f, RESONANCE to 0.1f, FILTER_ENV to 0.2f, A_RELEASE to 0.15f)),
            NamedSound("Basse reese", "Basses", synth(OSC2_DETUNE to 0.75f, CUTOFF to 0.42f, RESONANCE to 0.25f, SUB to 0.4f, SWEEP to 0.2f, LFO2_RATE to 0.15f, A_RELEASE to 0.2f)),
            NamedSound("Reese moderne", "Basses", synth(UNISON to 0.67f, UNISON_DETUNE to 0.35f, OSC_MIX to 0.3f, OSC2_DETUNE to 0.7f, CUTOFF to 0.36f, RESONANCE to 0.2f, SUB to 0.5f, SWEEP to 0.25f, LFO2_RATE to 0.12f, A_RELEASE to 0.2f)),
            // The dubstep wobble: a deep filter sweep locked to the tempo (eighth notes), restarted on each note.
            NamedSound("Basse wobble", "Basses", synth(OSC2_WAVE to 0.5f, OSC2_DETUNE to 0.55f, OSC_MIX to 0.45f, SUB to 0.7f, CUTOFF to 0.37f, RESONANCE to 0.45f, FILTER_ENV to 0f, SWEEP to 0.9f, LFO2_RATE to 0.64f, LFO2_SYNC to syncValue(SYNC_EIGHTH), A_ATTACK to 0f, A_SUSTAIN to 1f, A_RELEASE to 0.15f)),
            // The speed garage bass that "talks": a reese whose filter opens and closes every half note, sliding between notes.
            NamedSound("Basse qui parle", "Basses", synth(OSC2_DETUNE to 0.7f, OSC_MIX to 0.5f, SUB to 0.6f, CUTOFF to 0.33f, RESONANCE to 0.45f, FILTER_ENV to 0.2f, SWEEP to 0.75f, LFO2_RATE to 0.45f, LFO2_SYNC to syncValue(SYNC_HALF), GLIDE to 0.4f, A_RELEASE to 0.2f)),
            // The jump up growl: a gritty mid bass (saw and square, three copies) whose resonant filter
            // opens and closes every sixteenth, restarted on each note; no sub of its own (« Sub pur » plays it).
            NamedSound("Basse growl", "Basses", synth(OSC2_WAVE to 0.5f, OSC2_DETUNE to 0.6f, OSC_MIX to 0.5f, UNISON to 0.33f, UNISON_DETUNE to 0.2f, SUB to 0f, CUTOFF to 0.42f, RESONANCE to 0.6f, FILTER_ENV to 0.3f, F_DECAY to 0.2f, F_SUSTAIN to 0.2f, SWEEP to 0.8f, LFO2_RATE to 0.7f, LFO2_SYNC to syncValue(SYNC_SIXTEENTH), A_ATTACK to 0f, A_SUSTAIN to 1f, A_RELEASE to 0.12f)),
            // S4, the bass music voices (each one a sub mono under 250 Hz and a wide, driven mid layer):
            // the drum and bass reese (two saws +-27 cents, Saunderson's), the riddim's square wobble
            // (a gate on 1/8 triplets) and the neurofunk bass that talks (a vowel filter on the sweep).
            NamedSound("Reese DnB", "Basses", WideMix.reese(synth(OSC2_DETUNE to 0.75f, OSC_MIX to 0.35f, CUTOFF to 0.42f, RESONANCE to 0.25f, SUB to 0.45f, SWEEP to 0.2f, LFO2_RATE to 0.15f, A_RELEASE to 0.2f))),
            NamedSound("Basse riddim", "Basses", WideMix.riddim(synth(OSC1_WAVE to 0.5f, OSC2_WAVE to 0.5f, OSC2_DETUNE to 0.55f, OSC_MIX to 0.45f, SUB to 0.7f, CUTOFF to 0.35f, RESONANCE to 0.55f, FILTER_ENV to 0f, SWEEP to 0.9f, LFO2_RATE to 0.64f, LFO2_SYNC to syncValue(SYNC_EIGHTH_TRIPLET), A_ATTACK to 0f, A_SUSTAIN to 1f, A_RELEASE to 0.12f))),
            NamedSound("Basse neuro", "Basses", WideMix.neuro(synth(OSC2_WAVE to 0.5f, OSC2_DETUNE to 0.7f, OSC_MIX to 0.5f, UNISON to 0.33f, UNISON_DETUNE to 0.25f, SUB to 0.4f, CUTOFF to 0.48f, RESONANCE to 0.5f, FILTER_ENV to 0.25f, F_DECAY to 0.25f, F_SUSTAIN to 0.3f, SWEEP to 0.6f, LFO2_RATE to 0.6f, LFO2_SYNC to syncValue(SYNC_EIGHTH), A_ATTACK to 0f, A_SUSTAIN to 1f, A_RELEASE to 0.12f))),
            // A clean sub under a growl: a triangle filtered into an almost pure sine, no movement.
            NamedSound("Sub pur", "Basses", synth(OSC1_WAVE to 1f, OSC_MIX to 0f, SUB to 0f, CUTOFF to 0.27f, RESONANCE to 0f, FILTER_ENV to 0f, A_ATTACK to 0f, A_SUSTAIN to 1f, A_RELEASE to 0.1f)),
            // « Des basses qui tapent » (docs/hits/basses-qui-tapent.md § 5): the basses that hit. Each one
            // restarts its oscillators at each hit (every attack alike) and its sub, a sine, never goes
            // under 30 Hz. The rolling bass (a saw whose filter snaps shut in 36 ms, the velocity opening
            // it: the « gallop » of techno and tech house), the plucked bass (no sustain, a 3-semitone
            // « tok » at the attack), the synth 808 (a sine dropping 24 semitones over 25 ms, driven
            // hard), the donk (a bouncing « boing », 18 semitones over 30 ms) and the warm sub (« Sub
            // pur » saturated a little: its third harmonic, and every hit alike).
            NamedSound("Basse roulante", "Basses", synth(OSC1_WAVE to 0f, OSC_MIX to 0f, SUB to 0.5f, SUB_SHAPE to 1f, SUB_FLOOR to 1f, CUTOFF to 0.34f, RESONANCE to 0.3f, FILTER_ENV to 0.55f, F_ATTACK to 0f, F_DECAY to 0.42f, F_SUSTAIN to 0f, A_ATTACK to 0f, A_DECAY to 0.5f, A_SUSTAIN to 0.6f, A_RELEASE to 0.2f, DRIVE to 0.35f, VELOCITY_CUTOFF to 0.5f, PHASE_RESET to 1f)),
            NamedSound("Basse pluck", "Basses", synth(OSC1_WAVE to 0f, OSC_MIX to 0f, SUB to 0.4f, SUB_SHAPE to 1f, SUB_FLOOR to 1f, CUTOFF to 0.33f, RESONANCE to 0.35f, FILTER_ENV to 0.55f, F_DECAY to 0.45f, F_SUSTAIN to 0f, A_ATTACK to 0f, A_DECAY to 0.6f, A_SUSTAIN to 0f, A_RELEASE to 0.35f, PITCH_ENV to pitchEnvValue(3f), PITCH_DECAY to pitchDecayValue(10f), DRIVE to 0.3f, PHASE_RESET to 1f)),
            NamedSound("808 synthé", "Basses", synth(OSC1_WAVE to 1f, OSC_MIX to 0f, SUB to 0f, CUTOFF to 0.4f, RESONANCE to 0f, FILTER_ENV to 0.5f, F_DECAY to 0.4f, F_SUSTAIN to 0f, A_ATTACK to 0f, A_DECAY to 0.8f, A_SUSTAIN to 0.35f, A_RELEASE to 0.4f, PITCH_ENV to pitchEnvValue(24f), PITCH_DECAY to pitchDecayValue(25f), DRIVE to 0.9f, PHASE_RESET to 1f)),
            NamedSound("Donk", "Basses", synth(OSC1_WAVE to 1f, OSC_MIX to 0f, SUB to 0f, CUTOFF to 0.5f, RESONANCE to 0.1f, FILTER_ENV to 0.2f, F_DECAY to 0.4f, F_SUSTAIN to 0f, A_ATTACK to 0f, A_DECAY to 0.62f, A_SUSTAIN to 0f, A_RELEASE to 0.3f, PITCH_ENV to pitchEnvValue(18f), PITCH_DECAY to pitchDecayValue(30f), DRIVE to 0.5f, PHASE_RESET to 1f)),
            NamedSound("Sub chaud", "Basses", synth(OSC1_WAVE to 1f, OSC_MIX to 0f, SUB to 0f, CUTOFF to 0.27f, RESONANCE to 0f, FILTER_ENV to 0f, A_ATTACK to 0f, A_SUSTAIN to 1f, A_RELEASE to 0.1f, DRIVE to 0.3f, PHASE_RESET to 1f)),
            NamedSound("Lead", "Leads", SynthPatch.PRESETS[1].second),
            NamedSound("Lead carré", "Leads", synth(OSC1_WAVE to 0.5f, OSC2_WAVE to 0.5f, OSC2_DETUNE to 0.52f, CUTOFF to 0.75f, RESONANCE to 0.2f, VIBRATO to 0.1f, LFO1_RATE to 0.5f)),
            NamedSound("Lead acide", "Leads", synth(CUTOFF to 0.28f, RESONANCE to 0.8f, FILTER_ENV to 0.8f, F_DECAY to 0.2f, F_SUSTAIN to 0f, A_RELEASE to 0.15f)),
            NamedSound("Supersaw", "Leads", synth(UNISON to 1f, UNISON_DETUNE to 0.45f, OSC_MIX to 0f, SUB to 0.1f, CUTOFF to 0.8f, RESONANCE to 0.1f, FILTER_ENV to 0.1f, A_ATTACK to 0.05f, A_SUSTAIN to 0.9f, A_RELEASE to 0.45f)),
            NamedSound("Lead trance", "Leads", synth(UNISON to 0.5f, UNISON_DETUNE to 0.3f, OSC2_WAVE to 0.5f, OSC_MIX to 0.3f, CUTOFF to 0.72f, RESONANCE to 0.25f, FILTER_ENV to 0.3f, F_DECAY to 0.3f, A_RELEASE to 0.35f)),
            NamedSound("Hoover", "Leads", synth(UNISON to 1f, UNISON_DETUNE to 0.8f, OSC2_OCTAVE to 0.1f, OSC2_WAVE to 0.5f, OSC_MIX to 0.4f, CUTOFF to 0.7f, VIBRATO to 0.1f, LFO1_RATE to 0.4f, A_ATTACK to 0.05f, A_RELEASE to 0.3f)),
            NamedSound("Nappe", "Nappes", SynthPatch.PRESETS[2].second),
            NamedSound("Nappe chaude", "Nappes", synth(OSC1_WAVE to 1f, OSC2_DETUNE to 0.7f, CUTOFF to 0.4f, FILTER_ENV to 0.1f, A_ATTACK to 0.8f, A_SUSTAIN to 1f, A_RELEASE to 0.85f)),
            NamedSound("Accords future bass", "Nappes", synth(UNISON to 1f, UNISON_DETUNE to 0.55f, OSC_MIX to 0f, CUTOFF to 0.62f, FILTER_ENV to 0.3f, F_DECAY to 0.35f, A_ATTACK to 0.2f, A_SUSTAIN to 0.85f, A_RELEASE to 0.55f)),
            NamedSound("Cordes", "Nappes", synth(OSC2_DETUNE to 0.85f, CUTOFF to 0.6f, A_ATTACK to 0.5f, A_SUSTAIN to 1f, A_RELEASE to 0.6f, VIBRATO to 0.05f, LFO1_RATE to 0.45f)),
            NamedSound("Pluck", "Pluck", SynthPatch.PRESETS[3].second),
            NamedSound("Pluck future", "Pluck", synth(UNISON to 0.5f, UNISON_DETUNE to 0.4f, OSC_MIX to 0f, CUTOFF to 0.35f, RESONANCE to 0.3f, FILTER_ENV to 0.7f, F_DECAY to 0.45f, F_SUSTAIN to 0f, A_DECAY to 0.56f, A_SUSTAIN to 0f, A_RELEASE to 0.2f)),
            NamedSound("Arpège", "Pluck", synth(OSC1_WAVE to 0.5f, CUTOFF to 0.45f, RESONANCE to 0.4f, FILTER_ENV to 0.6f, F_DECAY to 0.4f, F_SUSTAIN to 0f, A_DECAY to 0.5f, A_SUSTAIN to 0f, A_RELEASE to 0.2f)),
            NamedSound("Cloche", "Pluck", synth(OSC1_WAVE to 1f, OSC2_WAVE to 1f, OSC2_OCTAVE to 0.9f, OSC_MIX to 0.4f, CUTOFF to 0.8f, A_DECAY to 0.5f, A_SUSTAIN to 0f, A_RELEASE to 0.5f)),
            NamedSound("Orgue", "Claviers", synth(OSC1_WAVE to 0.5f, OSC2_WAVE to 0.5f, OSC2_OCTAVE to 0.9f, OSC2_DETUNE to 0.5f, SUB to 0.3f, CUTOFF to 0.7f, A_ATTACK to 0f, A_SUSTAIN to 1f, A_RELEASE to 0.1f)),
            NamedSound("Piano électrique", "Claviers", synth(OSC1_WAVE to 1f, OSC2_OCTAVE to 0.9f, OSC_MIX to 0.3f, CUTOFF to 0.55f, A_DECAY to 0.55f, A_SUSTAIN to 0.2f, A_RELEASE to 0.35f, VIBRATO to 0.05f, LFO1_RATE to 0.6f)),
            NamedSound("Sirène", "Effets", synth(SWEEP to 0.9f, LFO2_RATE to 0.5f, RESONANCE to 0.7f, CUTOFF to 0.5f, A_SUSTAIN to 1f)),
            NamedSound("Souffle", "Effets", synth(NOISE to 0.9f, OSC_MIX to 0.5f, CUTOFF to 0.6f, A_ATTACK to 0.3f, A_SUSTAIN to 1f, A_RELEASE to 0.6f, SWEEP to 0.4f, LFO2_RATE to 0.1f)),
        ).map { it.copy(sound = it.sound.with(GAIN, gainValue(SYNTH_GAIN_DB[it.name] ?: 0f))) }
            // A bass's sub never under 30 Hz (« des basses qui tapent », P4: an octave-1 line's square sub
            // fell to 23-31 Hz, which nothing plays and the limiter still hears): an octave up there only.
            .map { if (it.category == "Basses" && it.sound.get(SUB) > 0f) it.copy(sound = it.sound.with(SUB_FLOOR, 1f)) else it }
    }

    /**
     * The synth's sounds: [SYNTH_V2] with its leads, pads, plucks and keys set as producers set them
     * (the leads chantier, lot 1: [ProVoices.synth]; lots B to D, the touch, the attack and the unison:
     * [ProVoices.touch]), the basses and effects unchanged.
     */
    val SYNTH: List<NamedSound<SynthPatch>> = SYNTH_V2.map(ProVoices::synth).map(ProVoices::touch)

    /** The library between the leads chantier's lot 1 and its lots B to D: projects saved then keep their names. */
    private val SYNTH_V3: List<NamedSound<SynthPatch>> = SYNTH_V2.map(ProVoices::synth)

    /** [PlaitsPatch.harmonics] of patch [index] (0..31) of a six-operator FM bank (Plaits' own mapping). */
    fun dx(index: Int): Float = (index.coerceIn(0, 31) + 0.5f) / 32f / 1.02f

    val PLAITS: List<NamedSound<PlaitsPatch>> = listOf(
        NamedSound("Basse analogique", "Basses", PlaitsPatch(model = 8, harmonics = 0.3f, timbre = 0.35f, morph = 0.2f, decay = 0.35f)),
        NamedSound("Basse FM", "Basses", PlaitsPatch(model = 10, harmonics = 0.25f, timbre = 0.45f, morph = 0.3f, decay = 0.3f)),
        NamedSound("Basse pincée", "Basses", PlaitsPatch(model = 19, harmonics = 0.3f, timbre = 0.3f, morph = 0.4f, decay = 0.4f)),
        // The amapiano log drum: Plaits' bass drum played on the notes, a tuned thud whose pitch drops.
        NamedSound("Log drum", "Basses", PlaitsPatch(model = 21, harmonics = 0.6f, timbre = 0.45f, morph = 0.55f, decay = 0.6f)),
        // The leads chantier: its filter half open (morph 0.7; at 0.5 the techno lead sat at 296 Hz, -19.9 dB of brightness).
        NamedSound("Lead filtré", "Leads", PlaitsPatch(model = 0, harmonics = 0.6f, timbre = 0.6f, morph = 0.7f, decay = 0.7f)),
        NamedSound("Lead chiptune", "Leads", PlaitsPatch(model = 7, harmonics = 0.5f, timbre = 0.5f, morph = 0.3f, decay = 0.6f)),
        NamedSound("Voix robot", "Leads", PlaitsPatch(model = 11, harmonics = 0.4f, timbre = 0.6f, morph = 0.5f, decay = 0.7f)),
        NamedSound("Nappe d'accords", "Nappes", PlaitsPatch(model = 14, harmonics = 0.45f, timbre = 0.4f, morph = 0.5f, decay = 0.9f, release = 0.7f)),
        NamedSound("Machine à cordes", "Nappes", PlaitsPatch(model = 6, harmonics = 0.5f, timbre = 0.5f, morph = 0.6f, decay = 0.9f, release = 0.75f)),
        NamedSound("Essaim", "Nappes", PlaitsPatch(model = 16, harmonics = 0.5f, timbre = 0.4f, morph = 0.4f, decay = 0.9f, release = 0.8f)),
        // The DX7 voices of Plaits' six-operator banks (models 2 to 4, a patch every 1/32 of harmonics:
        // [dx]). « Piano FM » played the bank's « BASS 1 » until the leads chantier: now « E.PIANO 1 ».
        NamedSound("Piano FM", "Pluck", PlaitsPatch(model = 3, harmonics = dx(0), timbre = 0.5f, morph = 0.5f, decay = 0.5f)),
        NamedSound("Rhodes FM", "Pluck", PlaitsPatch(model = 3, harmonics = dx(2), timbre = 0.5f, morph = 0.5f, decay = 0.5f)),
        NamedSound("Vibraphone FM", "Pluck", PlaitsPatch(model = 3, harmonics = dx(18), timbre = 0.5f, morph = 0.5f, decay = 0.5f)),
        NamedSound("Cloche FM", "Pluck", PlaitsPatch(model = 3, harmonics = dx(20), timbre = 0.5f, morph = 0.5f, decay = 0.5f)),
        // Neurofunk's metallic answer: two operators at a ratio that is not a whole number, a short ping.
        NamedSound("Stab métallique", "Leads", PlaitsPatch(model = 10, harmonics = 0.62f, timbre = 0.55f, morph = 0.35f, decay = 0.4f)),
        NamedSound("Marimba", "Pluck", PlaitsPatch(model = 20, harmonics = 0.3f, timbre = 0.35f, morph = 0.3f, decay = 0.45f)),
        NamedSound("Cloche", "Pluck", PlaitsPatch(model = 20, harmonics = 0.75f, timbre = 0.7f, morph = 0.7f, decay = 0.7f)),
        NamedSound("Grosse caisse", "Batterie", PlaitsPatch(model = 21, harmonics = 0.4f, timbre = 0.5f, morph = 0.4f, decay = 0.5f)),
        NamedSound("Caisse claire", "Batterie", PlaitsPatch(model = 22, harmonics = 0.5f, timbre = 0.5f, morph = 0.5f, decay = 0.4f)),
        NamedSound("Charley", "Batterie", PlaitsPatch(model = 23, harmonics = 0.5f, timbre = 0.6f, morph = 0.3f, decay = 0.2f)),
        NamedSound("Pluie", "Effets", PlaitsPatch(model = 18, harmonics = 0.5f, timbre = 0.6f, morph = 0.5f, decay = 0.8f)),
        NamedSound("Vent", "Effets", PlaitsPatch(model = 17, harmonics = 0.3f, timbre = 0.4f, morph = 0.6f, decay = 0.9f, release = 0.8f)),
    ).map { it.copy(sound = it.sound.copy(gain = SynthPatch.gainValue(PLAITS_GAIN_DB[it.name] ?: 0f))) }

    /**
     * The 303 sounds, each [AcidSound.level] calibrated like the synth's ([SYNTH_GAIN_DB]): a
     * one-bar riff (accents, slides) at 130 BPM alone measured about -16 LUFS, as the basses and
     * leads (phase 30; before, 0.7 everywhere spread them from -8.7 to -16.8 LUFS, the 303 4 to 9 dB
     * over a pad).
     */
    val ACID: List<NamedSound<AcidSound>> = listOf(
        NamedSound("Acide de base", "Classiques", AcidSound()),
        NamedSound("Squelchy", "Classiques", AcidSound(cutoff = 0.25f, resonance = 0.85f, envMod = 0.75f, decay = 0.25f, accent = 0.8f, level = 0.46f)),
        NamedSound("Da Funk", "Classiques", AcidSound(cutoff = 0.4f, resonance = 0.55f, envMod = 0.45f, decay = 0.45f, accent = 0.5f, waveform = 1f, drive = 0.45f, level = 0.43f)),
        NamedSound("Hurleur", "Sales", AcidSound(cutoff = 0.45f, resonance = 0.9f, envMod = 0.8f, decay = 0.35f, accent = 0.9f, drive = 0.8f, level = 0.3f)),
        NamedSound("Carré saturé", "Sales", AcidSound(cutoff = 0.35f, resonance = 0.7f, envMod = 0.6f, decay = 0.3f, waveform = 1f, drive = 0.9f, level = 0.32f)),
        NamedSound("Basse ronde", "Doux", AcidSound(cutoff = 0.2f, resonance = 0.3f, envMod = 0.25f, decay = 0.5f, accent = 0.3f, drive = 0.1f, level = 0.77f)),
        NamedSound("Bulle", "Doux", AcidSound(cutoff = 0.3f, resonance = 0.6f, envMod = 0.35f, decay = 0.15f, accent = 0.4f, drive = 0.15f, level = 0.7f)),
    )

    /**
     * The name of the sound of [among] that [patch] plays, whatever its loudness calibration and
     * glide (the assistant raises or lowers a part's [SynthPatch.GAIN] and makes its bass slide):
     * the same sound, shown as chosen in « Sons prêts à jouer ».
     */
    fun synthNameOf(patch: SynthPatch, among: List<NamedSound<SynthPatch>> = SYNTH): String? {
        // The assistant's width and movement (S4: [SynthPatch.S4_PARAMS]) are its production, not the sound.
        // The sub's 30 Hz floor too (a sound chosen before it had none), and the touch and attack of the
        // leads chantier's lots B to D ([SynthPatch.LEAD_PARAMS]: a sound chosen before them is the same sound).
        // The mid layer's drive too (the final audit's bass music Drop, WideMix.phoneMids: its layer is S4's).
        fun SynthPatch.identity() = (SynthPatch.S4_PARAMS + SynthPatch.SUB_FLOOR + SynthPatch.MID_LAYER_DRIVE + SynthPatch.LEAD_PARAMS).fold(sanitized().with(SynthPatch.GAIN, 0.5f).with(SynthPatch.GLIDE, 0f)) { p, i ->
            p.with(i, SynthPatch.DEFAULTS[i])
        }
        val wanted = patch.identity()
        // A sound of the library before the leads chantier (a project saved earlier) keeps its name.
        // Since that chantier the assistant also moves a lead's or a pad's cut-off with its notes
        // (key tracking) and along the song: the same sound at another cut-off is still that sound.
        fun SynthPatch.toneless() = with(SynthPatch.CUTOFF, 0f)
        return among.firstOrNull { it.sound.identity() == wanted }?.name
            ?: SYNTH_V3.firstOrNull { it.sound.identity() == wanted }?.name?.takeIf { name -> among.any { it.name == name } }
            ?: SYNTH_V2.firstOrNull { it.sound.identity() == wanted }?.name?.takeIf { name -> among.any { it.name == name } }
            ?: among.firstOrNull { it.sound.identity().toneless() == wanted.toneless() }?.name
    }

    /** The library family (« Leads », « Nappes », « Pluck »…) of the synth sound [patch] plays, null when it is none of them. */
    fun synthCategoryOf(patch: SynthPatch): String? = synthNameOf(patch)?.let { name -> SYNTH.firstOrNull { it.name == name }?.category }

    /**
     * The 303 [preset] taken by a 303 now sounding [current]: its tuning kept; its volume the
     * preset's calibrated one, unless the user set their own (a level no preset has, nor the old 0.7).
     */
    fun withAcidPreset(preset: AcidSound, current: AcidSound): AcidSound {
        val untouched = current.level == AcidSound.OLD_LEVEL || current.level == 0.6f || ACID.any { it.sound.level == current.level }
        return preset.copy(tuning = current.tuning, level = if (untouched) preset.level else current.level)
    }

    /** A random synth sound that stays musical: one of the families, then nudged. */
    fun randomSynth(rng: Random = Random.Default): SynthPatch {
        val base = SYNTH.random(rng).sound
        var p = base
        // Nudge the character, not the level: oscillators, filter and envelopes.
        val nudged = listOf(
            SynthPatch.OSC2_DETUNE, SynthPatch.OSC_MIX, SynthPatch.SUB, SynthPatch.CUTOFF, SynthPatch.RESONANCE,
            SynthPatch.FILTER_ENV, SynthPatch.F_DECAY, SynthPatch.A_DECAY, SynthPatch.A_RELEASE, SynthPatch.VIBRATO,
        )
        nudged.forEach { i -> p = p.with(i, p.get(i) + (rng.nextFloat() - 0.5f) * 0.3f) }
        p = p.with(SynthPatch.OSC1_WAVE, listOf(0f, 0.5f, 1f).random(rng)).with(SynthPatch.OSC2_WAVE, listOf(0f, 0.5f, 1f).random(rng))
        // Never too dark to hear, never screaming.
        return p.with(SynthPatch.CUTOFF, p.get(SynthPatch.CUTOFF).coerceIn(0.25f, 0.85f))
            .with(SynthPatch.RESONANCE, p.get(SynthPatch.RESONANCE).coerceAtMost(0.75f))
    }

    fun randomPlaits(rng: Random = Random.Default): PlaitsPatch {
        // Drum models are left out: "au hasard" is for a playable voice.
        val models = PlaitsPatch.FAMILIES.filter { it.first != "Batterie" }.flatMap { it.second }
        fun r(lo: Float, hi: Float) = lo + rng.nextFloat() * (hi - lo)
        return PlaitsPatch(model = models.random(rng), harmonics = r(0.1f, 0.9f), timbre = r(0.2f, 0.8f), morph = r(0.1f, 0.9f),
            decay = r(0.3f, 0.9f), colour = r(0.3f, 0.7f), release = r(0.2f, 0.6f))
    }

    fun randomAcid(rng: Random = Random.Default, keep: AcidSound = AcidSound()): AcidSound {
        fun r(lo: Float, hi: Float) = lo + rng.nextFloat() * (hi - lo)
        return keep.copy(cutoff = r(0.15f, 0.55f), resonance = r(0.4f, 0.9f), envMod = r(0.3f, 0.85f), decay = r(0.15f, 0.6f),
            accent = r(0.3f, 0.9f), waveform = if (rng.nextBoolean()) 0f else 1f, drive = r(0.1f, 0.7f))
    }
}

/** The user's own sounds, kept for every project (not inside one). */
@Serializable
data class Favorites(
    val synth: List<NamedSound<SynthPatch>> = emptyList(),
    val plaits: List<NamedSound<PlaitsPatch>> = emptyList(),
    val acid: List<NamedSound<AcidSound>> = emptyList(),
) {
    fun sanitized() = Favorites(
        synth.take(MAX).map { it.copy(name = it.name.take(30), category = SoundCategories.FAVORITES, sound = it.sound.sanitized()) },
        plaits.take(MAX).map { it.copy(name = it.name.take(30), category = SoundCategories.FAVORITES, sound = it.sound.sanitized()) },
        acid.take(MAX).map { it.copy(name = it.name.take(30), category = SoundCategories.FAVORITES, sound = it.sound.sanitized()) },
    )

    companion object {
        const val MAX = 48

        /** "Mon son 3": the first number not taken in [names]. */
        fun nextName(names: List<String>, prefix: String = tr("Mon son")): String =
            generateSequence(1) { it + 1 }.map { "$prefix $it" }.first { it !in names }
    }
}
