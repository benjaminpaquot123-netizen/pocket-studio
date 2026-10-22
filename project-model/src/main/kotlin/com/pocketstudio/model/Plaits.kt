package com.pocketstudio.model

import kotlinx.serialization.Serializable

/** Settings of the Plaits voice (Mutable Instruments): [model] 0..23 in Plaits 1.2 order, the rest 0..1. */
@Serializable
data class PlaitsPatch(
    val model: Int = 8,
    val harmonics: Float = 0.5f,
    val timbre: Float = 0.5f,
    val morph: Float = 0.5f,
    val decay: Float = 0.5f,
    val colour: Float = 0.5f,
    val release: Float = 0.3f,
    val level: Float = 0.8f,
    /** The sound's loudness calibration, as [SynthPatch.GAIN] (0.5 = 0 dB): set by the library, not a knob. */
    val gain: Float = 0.5f,
) {
    /** Values in the engine's PlaitsSynth::Param order. */
    fun engineValues(): List<Float> =
        listOf(model / (MODELS.size - 1f), harmonics, timbre, morph, decay, colour, release, level, gain)

    fun with(param: Int, value: Float): PlaitsPatch {
        val v = value.coerceIn(0f, 1f)
        return when (param) {
            1 -> copy(harmonics = v)
            2 -> copy(timbre = v)
            3 -> copy(morph = v)
            4 -> copy(decay = v)
            5 -> copy(colour = v)
            6 -> copy(release = v)
            7 -> copy(level = v)
            GAIN -> copy(gain = v)
            else -> this
        }
    }

    fun sanitized(): PlaitsPatch {
        fun f(v: Float, d: Float) = v.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: d
        return PlaitsPatch(
            model.coerceIn(0, MODELS.lastIndex), f(harmonics, 0.5f), f(timbre, 0.5f), f(morph, 0.5f),
            f(decay, 0.5f), f(colour, 0.5f), f(release, 0.3f), f(level, 0.8f), f(gain, 0.5f),
        )
    }

    companion object {
        /** [engineValues] index of [gain] (after the knobs of [PARAMS]). */
        const val GAIN = 8

        /** French names, grouped as on the module: the 1.2 additions first, then the classics. */
        val MODELS = listOf(
            "Analogique + filtre", "Distorsion de phase", "FM 6 op. · banque 1", "FM 6 op. · banque 2", "FM 6 op. · banque 3",
            "Terrain d'onde", "Machine à cordes", "Chiptune",
            "Analogique", "Waveshaper", "FM 2 op.", "Formants", "Additif", "Table d'ondes", "Accords", "Voix parlée",
            "Essaim", "Bruit filtré", "Particules", "Corde pincée", "Modal (percussion)", "Grosse caisse", "Caisse claire", "Charley",
        )
        val FAMILIES = listOf(
            "Analogiques" to listOf(8, 0, 9, 1, 7),
            "FM" to listOf(10, 2, 3, 4),
            "Textures" to listOf(11, 12, 13, 5, 6, 14, 16),
            "Physiques" to listOf(19, 20, 18, 17, 15),
            "Batterie" to listOf(21, 22, 23),
        )
        /** Labels of the knobs, in engine order after the model. */
        val PARAMS = listOf("Harmoniques", "Timbre", "Morph", "Déclin", "Couleur", "Relâche", "Volume")
    }
}
