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
        // The electronic music classics (phase 25).
        FxPreset("scream_lead", "Lead qui crie", "Saturé, il perce le mix ; un écho à la croche pointée le suit.", "Saleté",
            listOf(fx(EffectType.DISTORTION, 0.6f, 0.65f, 0f, 1f), fx(EffectType.ECHO, 0.42f, 0.35f, 0.5f, 0.3f))),
        FxPreset("edm_pump", "Pompe EDM", "Énorme et serré (OTT), et qui respire sur chaque kick.", "Rythme",
            listOf(fx(EffectType.OTT, 0.7f, 0.5f, 0.7f, 0.7f), fx(EffectType.PUMP, 0.8f, 0.4f, 0f))),
        FxPreset("dub_deep", "Dub profond", "Un écho sombre qui tourne longtemps, noyé dans la réverbe.", "Ambiance",
            listOf(fx(EffectType.ECHO, 0.42f, 0.6f, 0.3f, 0.4f), fx(EffectType.REVERB, 0.75f, 0.6f, 0.1f, 0.25f))),
        FxPreset("wide_alive", "Large et vivant", "Il bouge de gauche à droite et s'épaissit : parfait pour une nappe ou des accords.", "Mouvement",
            listOf(fx(EffectType.AUTO_PAN, 0.42f, 0.5f, 0f), fx(EffectType.CHORUS, 0.3f, 0.5f, 0.4f, 0.5f))),
        FxPreset("fat_bass", "Basse grasse", "Une basse fuzz épaisse, compressée en trois bandes : elle remplit tout.", "Saleté",
            listOf(fx(EffectType.DISTORTION, 0.4f, 0.35f, 0.3f, 0.6f), fx(EffectType.OTT, 0.5f, 0.5f, 0.6f, 0.6f))),
        FxPreset("pluck_pingpong", "Pluck ping-pong", "Chaque note rebondit à gauche et à droite dans une petite réverbe.", "Ambiance",
            listOf(fx(EffectType.PING_PONG, 0.25f, 0.45f, 0.6f, 0.35f), fx(EffectType.REVERB, 0.5f, 0.5f, 0.1f, 0.2f))),
        // The Berlin rumble (phase 27): on a copy of the kick, only the reverb's tail, dirtied, kept
        // under 150 Hz, ducked by the kick and in mono, so it rolls between the kicks.
        FxPreset("rumble", "Grondement", "Pour un kick rejoué : seule sa longue réverbe, saturée, gardée dans le grave et creusée par le kick. Elle gronde entre les coups.", "Saleté",
            listOf(
                fx(EffectType.REVERB, 0.85f, 0.4f, 0f, 1f), fx(EffectType.DISTORTION, 0.55f, 0.3f, 0f, 1f),
                fx(EffectType.FILTER, 0.22f, 0.15f, 0f, 1f), fx(EffectType.PUMP, 0.9f, 0.35f, 0f), fx(EffectType.WIDENER, 0f, 0f),
            )),
        // The bass music movements (phase 27): a filter opening and closing in time.
        FxPreset("wob_dubstep", "Wob dubstep", "La basse qui parle : un filtre résonant s'ouvre et se ferme à la croche, sur un son un peu sale.", "Mouvement",
            listOf(fx(EffectType.DISTORTION, 0.35f, 0.75f, 0f, 0.7f), fx(EffectType.WOBBLE, 0.5f, 0.78f, 0.62f, 0f), fx(EffectType.OTT, 0.35f, 0.5f, 0.5f, 0.6f))),
        FxPreset("reese_dnb", "Reese DnB", "Une basse épaisse qui bouge lentement : doublée, elle respire sur une demi-mesure, comme en drum & bass.", "Mouvement",
            listOf(fx(EffectType.CHORUS, 0.15f, 0.7f, 0.5f, 0.55f), fx(EffectType.WOBBLE, 0.12f, 0.5f, 0.3f, 0.3f), fx(EffectType.DISTORTION, 0.2f, 0.6f, 0f, 0.5f))),
        // The jump up growl (phase 29): folded back for a metallic grit, then squashed in three bands.
        FxPreset("growl", "Growl", "Une basse qui grogne : saturée jusqu'au métal et écrasée en trois bandes, grasse comme en dubstep.", "Saleté",
            listOf(fx(EffectType.DISTORTION, 0.55f, 0.6f, 0.8f, 0.75f), fx(EffectType.OTT, 0.75f, 0.5f, 0.75f, 0.7f))),
        // A recorded voice (final audit, 03 G2/G3): the guides' « Voix claire », and the rap one.
        FxPreset("voice_clear", "Voix claire", "Coupe-bas à 120 Hz et un compresseur doux : la voix reste nette et ne se noie pas.", "Ambiance",
            listOf(fx(EffectType.PARAM_EQ, 0.5f, 0.5f, 0.4f, 0.5566f), fx(EffectType.COMPRESSOR, 0.55f, 0.3f, 0.35f, 0.35f, 0f, 1f, 1f, 0.3f, 1f))),
        // Low cut 100 Hz and +3 dB of presence at 3.5 kHz, 4:1 from -20 dB (5 ms, 120 ms, its own
        // make-up), a small room with 30 ms of pre-delay: every word stays clear in front of the beat.
        FxPreset("voice_rap", "Voix rap", "Devant la prod : coupe-bas, compresseur qui tient chaque mot, un peu de présence et une petite pièce.", "Ambiance",
            listOf(
                fx(EffectType.PARAM_EQ, 0.7477f, 0.5833f, 0.343f, 0.5f),
                fx(EffectType.COMPRESSOR, 0.5f, 0.4628f, 0.5852f, 0.4857f, 0f, 1f, 1f, 0.3f, 1f),
                fx(EffectType.REVERB, 0.3f, 0.6f, 0.15f, 0.12f),
            )),
    )

    fun byId(id: String) = (ALL + NONE).firstOrNull { it.id == id }

    /** The ready-made treatments that suit [kind] best, in order (shown first for that sound). */
    fun suggestedFor(kind: SoundKind): List<FxPreset> = when (kind) {
        SoundKind.LEAD -> listOf("scream_lead", "wide_alive", "dub_deep", "dream", "distortion", "far_away")
        SoundKind.PLUCK -> listOf("pluck_pingpong", "dub_deep", "wide_alive", "small_room", "lofi_tape")
        SoundKind.BASS -> listOf("fat_bass", "growl", "wob_dubstep", "reese_dnb", "edm_pump", "pump", "crunch", "distortion")
        SoundKind.PAD -> listOf("wide_alive", "space", "cathedral", "edm_pump", "underwater", "dream")
        SoundKind.KEYS -> listOf("small_room", "warm_vinyl", "lofi_tape", "wide_alive", "dub_deep")
        SoundKind.DRUMS -> listOf("punch", "edm_pump", "crunch", "lofi_tape", "small_room", "rumble")
        SoundKind.ACID -> listOf("dub_deep", "fat_bass", "wob_dubstep", "crunch", "jet", "pump")
        SoundKind.AUDIO -> listOf(VOICE_RAP, VOICE_CLEAR, "small_room", "dub_deep", "radio", "phone", "cathedral")
    }.mapNotNull { byId(it) }

    /**
     * The suggestions for mixer strip [ch] holding [kind]: on the whole drum strip, « Grondement »
     * is left out (its reverb at full mix would turn the whole kit into a rumble); it stays on
     * a pad's own strip (a kick played again).
     */
    fun suggestedFor(kind: SoundKind, ch: Int): List<FxPreset> =
        suggestedFor(kind).filter { it.id != RUMBLE || Mixer.isPad(ch) }

    /** The rumble's preset (see [suggestedFor]). */
    const val RUMBLE = "rumble"
    /** The recorded voice's presets; « Voix claire » goes on a voice track's first take (see Project.withTake callers). */
    const val VOICE_CLEAR = "voice_clear"
    const val VOICE_RAP = "voice_rap"
}

/** What a sound is for, to offer the right ready-made effects first. */
enum class SoundKind(val label: String) {
    LEAD("Lead"), PLUCK("Pluck"), BASS("Basse"), PAD("Nappe"), KEYS("Clavier"), DRUMS("Batterie"), ACID("Basse acide"), AUDIO("Audio");

    companion object {
        /** From a sound library family ("Basses", "Nappes"…), if it says. */
        fun ofCategory(category: String?): SoundKind? = when (category) {
            "Basses" -> BASS
            "Leads" -> LEAD
            "Nappes" -> PAD
            "Pluck" -> PLUCK
            "Claviers" -> KEYS
            "Batterie" -> DRUMS
            else -> null
        }

        /** From a track's name (« Basse », « Nappe / accords », « Lead »…), if it says. */
        fun ofName(name: String): SoundKind? {
            val n = name.lowercase()
            return when {
                listOf("bass", "basse", "808", "sub").any { it in n } -> BASS
                listOf("nappe", "pad", "accord", "chord").any { it in n } -> PAD
                "pluck" in n -> PLUCK
                listOf("piano", "clavier", "keys", "orgue").any { it in n } -> KEYS
                listOf("lead", "mélodie", "melod").any { it in n } -> LEAD
                else -> null
            }
        }

        /** A General MIDI program's family. */
        fun ofProgram(program: Int): SoundKind = when (program) {
            in 0..23 -> KEYS
            in 24..31, in 104..111 -> PLUCK
            in 32..39 -> BASS
            in 48..55, in 88..95 -> PAD
            else -> LEAD
        }
    }
}

/** The kind of the sound on an instrument track: its library family, its name, else a lead. */
fun InstrumentTrack.soundKind(): SoundKind {
    instrument?.let { return SoundKind.ofName(name) ?: SoundKind.ofProgram(it.program) }
    val category = when (engine) {
        1 -> SoundLibrary.PLAITS.firstOrNull { it.sound == plaits.copy(level = it.sound.level, gain = it.sound.gain) }?.category
        0 -> SoundLibrary.SYNTH.firstOrNull { it.sound.sanitized() == synth.withGainOf(it.sound) }?.category
        else -> null
    }
    return SoundKind.ofCategory(category) ?: SoundKind.ofName(name) ?: SoundKind.LEAD
}

/** The kind of sound on mixer channel [ch], for the effects offered first. */
fun Project.soundKindOf(ch: Int): SoundKind = when {
    ch == 0 || Mixer.isPad(ch) -> SoundKind.DRUMS
    ch == 1 -> SoundKind.ACID
    else -> instrumentTracks.firstOrNull { InstrumentTrack.mixerChannel(it.id) == ch }?.soundKind() ?: SoundKind.AUDIO
}

/** [preset] on this track: its inserts (the others emptied), sends and macros; the fader stays. */
fun Channel.withFx(preset: FxPreset): Channel = copy(
    inserts = (preset.inserts + List(Mixer.INSERTS) { Insert() }).take(Mixer.INSERTS),
    sends = (preset.sends + List(Buses.COUNT) { 0f }).take(Buses.COUNT),
    macros = Macros.sanitized(preset.macros),
).sanitized()

/** The preset this track's effects match exactly, if any (to highlight it). */
fun Channel.matchingFx(): FxPreset? {
    val clean = sanitized()
    return (FxPresets.ALL + FxPresets.NONE).firstOrNull { p ->
        val made = clean.withFx(p)
        // An effect of the preset kept to some scenes only is still that preset.
        made.inserts == clean.inserts.map { it.copy(scenes = null) } && made.sends == clean.sends && made.macros == clean.macros
    }
}
