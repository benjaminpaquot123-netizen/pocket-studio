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

        fun sanitize(list: List<MidiMapping>): List<MidiMapping> =
            list.filter { it.controller in 0..119 && (it.target.kind in 0..4 || it.target.kind in SYNTH..TEMPO) }
                .distinctBy { it.controller }.take(MAX)
    }
}
