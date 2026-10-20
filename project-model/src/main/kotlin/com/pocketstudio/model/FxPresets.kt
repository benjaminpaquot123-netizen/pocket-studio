package com.pocketstudio.model

/** The four quick controls of every track (engine TrackMacros order), 0..1. */
object Macros {
    const val COLOR = 0
    const val DIRT = 1
    const val MOTION = 2
    const val SPACE = 3
    const val COUNT = 4

    val NAMES = listOf("Couleur", "Saleté", "Mouvement", "Espace")
    /** What each end means, for the knobs and the pad XY. */
    val ENDS = listOf("Sombre" to "Brillant", "Propre" to "Sale", "Immobile" to "Ondulant", "Sec" to "Lointain")
    /** At rest the macros leave the sound untouched (colour sits in the middle). */
    val DEFAULTS = listOf(0.5f, 0f, 0f, 0f)

    fun sanitized(values: List<Float>): List<Float> =
        (values + DEFAULTS.drop(values.size)).take(COUNT).mapIndexed { i, v -> v.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: DEFAULTS[i] }
}

/**
 * A ready-made sound treatment for one track: named after what it sounds like, it sets the
 * track's inserts, sends and macros in one tap (volume, pan, mute and solo stay).
 */
data class FxPreset(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val inserts: List<Insert> = emptyList(),
    val sends: List<Float> = listOf(0f, 0f),
    val macros: List<Float> = Macros.DEFAULTS,
)

object FxPresets {
    val CATEGORIES = listOf("Ambiance", "Couleur", "Saleté", "Mouvement", "Rythme")

    private fun fx(type: EffectType, vararg p: Float) = Insert(type.ordinal, p.toList() + type.defaults.drop(p.size))

    /** Colour, dirt, motion, space. */
    private fun m(color: Float = 0.5f, dirt: Float = 0f, motion: Float = 0f, space: Float = 0f) = listOf(color, dirt, motion, space)

    val NONE = FxPreset("none", "Sans effet", "Le son tel quel : plus d'effets, macros au repos.", "")

    val ALL: List<FxPreset> = listOf(
        FxPreset("underwater", "Sous l'eau", "Étouffé et ondulant, comme entendu depuis le fond d'une piscine.", "Ambiance",
            macros = m(color = 0.16f, motion = 0.55f, space = 0.4f)),
        FxPreset("cathedral", "Grande cathédrale", "Une immense réverbe : chaque note résonne longtemps.", "Ambiance",
            listOf(fx(EffectType.EQ, 0.45f, 0.5f, 0.55f)), macros = m(motion = 0.15f, space = 0.95f)),
        FxPreset("small_room", "Petite pièce", "Juste un peu d'air autour du son, comme dans une chambre.", "Ambiance",
            macros = m(space = 0.25f)),
        FxPreset("far_away", "Lointain", "Le son s'éloigne : moins de graves et d'aigus, beaucoup d'écho.", "Ambiance",
            listOf(fx(EffectType.EQ, 0.2f, 0.5f, 0.35f)), macros = m(color = 0.42f, space = 0.75f)),
        FxPreset("space", "Espace", "Large, flottant, stéréo : idéal pour une nappe.", "Ambiance",
            listOf(fx(EffectType.WIDENER, 0.9f, 0.35f)), macros = m(motion = 0.3f, space = 0.55f)),
        FxPreset("dub_echo", "Écho dub", "Un écho qui se répète en rythme et s'éteint doucement.", "Ambiance",
            listOf(fx(EffectType.FILTER, 0.3f, 0.2f, 0.9f, 1f)), sends = listOf(0.15f, 0.75f), macros = m(space = 0.2f)),
        FxPreset("lofi_tape", "Lo-Fi Tape", "Vieille cassette : moins d'aigus, grain, léger pleurage.", "Couleur",
            listOf(fx(EffectType.BITCRUSHER, 0.45f, 0.2f, 0.5f, 0.71f), fx(EffectType.EQ, 0.55f, 0.5f, 0.35f)),
            macros = m(color = 0.38f, dirt = 0.35f, motion = 0.3f)),
        FxPreset("warm_vinyl", "Vinyle chaud", "Rond et chaleureux, un peu feutré.", "Couleur",
            listOf(fx(EffectType.EQ, 0.62f, 0.5f, 0.4f)), macros = m(color = 0.44f, dirt = 0.2f)),
        FxPreset("radio", "Vieille radio", "Fin et nasillard, comme un poste des années 50.", "Couleur",
            listOf(fx(EffectType.FILTER, 0.6f, 0.3f, 0.5f, 1f), fx(EffectType.EQ, 0.1f, 0.65f, 0.3f)), macros = m(dirt = 0.4f)),
        FxPreset("phone", "Téléphone", "Tout petit et grésillant, comme au bout du fil.", "Couleur",
            listOf(fx(EffectType.FILTER, 0.68f, 0.45f, 0.5f, 1f), fx(EffectType.BITCRUSHER, 0.55f, 0.35f, 0.6f, 0.71f)), macros = m(dirt = 0.3f)),
        FxPreset("bright", "Brillant", "Plus clair et plus présent, pour sortir du mix.", "Couleur",
            listOf(fx(EffectType.EQ, 0.45f, 0.55f, 0.72f)), macros = m(color = 0.58f)),
        FxPreset("distortion", "Distorsion", "Saturé et agressif, façon ampli poussé à fond.", "Saleté",
            macros = m(color = 0.42f, dirt = 0.9f)),
        FxPreset("crunch", "Croustillant", "Juste un peu de grain pour donner du mordant.", "Saleté",
            macros = m(dirt = 0.4f)),
        FxPreset("robot", "Robot", "Numérique, métallique et grinçant.", "Saleté",
            listOf(fx(EffectType.BITCRUSHER, 0.8f, 0.55f, 0.85f, 0.71f), fx(EffectType.FLANGER, 0.7f, 0.8f, 0.75f, 0.5f))),
        FxPreset("8bit", "Jeu vidéo 8 bits", "Le son des vieilles consoles.", "Saleté",
            listOf(fx(EffectType.BITCRUSHER, 0.9f, 0.7f, 1f, 0.71f))),
        FxPreset("dream", "Rêve", "Doux, tourbillonnant et voilé.", "Mouvement",
            listOf(fx(EffectType.PHASER, 0.15f, 0.7f, 0.4f, 0.5f)), macros = m(color = 0.4f, motion = 0.4f, space = 0.6f)),
        FxPreset("jet", "Avion à réaction", "Un balayage qui monte et descend, très années 70.", "Mouvement",
            listOf(fx(EffectType.FLANGER, 0.2f, 0.9f, 0.8f, 0.6f))),
        FxPreset("wobble", "Ondulation", "Le volume qui respire lentement de gauche à droite.", "Mouvement",
            listOf(fx(EffectType.TREMOLO, 0.25f, 0.6f, 0f, 1f))),
        FxPreset("chopper", "Hachoir", "Le son coupé en tranches rapides et régulières.", "Rythme",
            listOf(fx(EffectType.TREMOLO, 0.62f, 1f, 1f, 0f))),
        FxPreset("pump", "Pompe", "Le son se baisse à chaque kick puis remonte : l'effet « house » qui respire.", "Rythme",
            listOf(fx(EffectType.PUMP, 0.8f, 0.4f, 0f))),
        FxPreset("punch", "Punch", "Plus serré et plus fort, pour une batterie qui claque.", "Rythme",
            listOf(fx(EffectType.COMPRESSOR, 0.45f, 0.45f, 0.45f, 0.3f), fx(EffectType.SATURATION, 0.25f, 0.8f, 0.6f, 0.7f))),
    )

    fun byId(id: String) = (ALL + NONE).firstOrNull { it.id == id }
}

/** [preset] on this track: its inserts (the others emptied), sends and macros; the fader stays. */
fun Channel.withFx(preset: FxPreset): Channel = copy(
    inserts = (preset.inserts + List(Mixer.INSERTS) { Insert() }).take(Mixer.INSERTS),
    sends = (preset.sends + List(Buses.COUNT) { 0f }).take(Buses.COUNT),
    macros = Macros.sanitized(preset.macros),
).sanitized()

/** The preset this track's effects match exactly, if any (to highlight it). */
fun Channel.matchingFx(): FxPreset? = (FxPresets.ALL + FxPresets.NONE).firstOrNull { p ->
    val made = copy().withFx(p)
    made.inserts == sanitized().inserts && made.sends == sanitized().sends && made.macros == sanitized().macros
}
