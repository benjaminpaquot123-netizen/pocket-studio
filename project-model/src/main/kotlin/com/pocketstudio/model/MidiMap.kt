package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * MIDI learn: a knob or fader of a MIDI controller (control change [controller]) moves [target].
 * Mixer targets reuse [AutoTarget]'s kinds; the others are [SYNTH] / [PLAITS] (a parameter of the
 * selected instrument track), [PERFORMANCE] (live master effects) and [TEMPO].
 */
@Serializable
data class MidiMapping(val controller: Int, val target: AutoTarget) {
    companion object {
        const val SYNTH = 10
        const val PLAITS = 11
        const val PERFORMANCE = 12
        const val TEMPO = 13
        const val MAX = 32

        /** Only targets the app can move (a damaged file must not crash a knob turn). */
        private fun valid(t: AutoTarget): Boolean = when (t.kind) {
            AutoTarget.VOLUME, AutoTarget.PAN -> t.ch in 0 until Mixer.CHANNELS && t.ch != 4
            AutoTarget.SEND -> t.ch in 0 until Mixer.CHANNELS && t.ch != 4 && t.slot in 0 until Buses.COUNT
            AutoTarget.INSERT_PARAM -> t.ch in 0 until Mixer.CHANNELS && t.slot in 0 until Mixer.INSERTS && t.param in 0 until EffectType.PARAMS
            AutoTarget.BUS_PARAM -> t.slot in 0 until Buses.COUNT && t.param in 0 until EffectType.PARAMS
            AutoTarget.ACID -> t.isValid
            SYNTH -> t.param in 0 until SynthPatch.GAIN
            PLAITS -> t.param in 1..PlaitsPatch.PARAMS.size
            PERFORMANCE -> t.param in 0..5
            TEMPO -> true
            else -> false
        }

        fun sanitize(list: List<MidiMapping>): List<MidiMapping> =
            list.filter { it.controller in 0..119 && valid(it.target) }
                .distinctBy { it.controller }.take(MAX)
    }
}
