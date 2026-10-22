package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr

/**
 * The beginner's assistant for a new song: a style, then one step per drum line (what it does,
 * which sound), then a project with those sounds on the pads and a basic groove to start from.
 * It never writes the music: the grooves are plain textbook patterns the user then changes.
 */
object StarterWizard {
    /**
     * A line of the beat: which pad it lands on, what it is for, which kit voices can play it.
     * [packKind]: the kind of pack sound that can play it (the rumble is a kick, the second
     * percussion a percussion; no pack has a ride).
     */
    enum class Role(val id: String, val lane: Int, val title: String, val explanation: String, val builtIn: List<Int>, val packKind: String = id) {
        Kick(
            "kick", 0, "Kick (grosse caisse)",
            "La fondation du morceau : le battement de cœur grave que tout le monde suit en tapant du pied.",
            listOf(0, 1),
        ),
        Snare(
            "snare", 2, "Caisse claire",
            "Elle répond au kick, souvent sur les temps 2 et 4 : c'est elle qui donne l'élan.",
            listOf(2, 3, 10),
        ),
        Clap(
            "clap", 4, "Clap",
            "Un battement de mains : il renforce ou remplace la caisse claire, très courant en house et en trap.",
            listOf(4),
        ),
        Hat(
            "hat", 5, "Charley fermé (hi-hat)",
            "Le « tic-tic » régulier et léger : il donne la vitesse et le groove.",
            listOf(5, 13),
        ),
        OpenHat(
            "open_hat", 6, "Charley ouvert",
            "Un « tsss » plus long : placé entre les temps, il fait respirer le rythme.",
            listOf(6, 12),
        ),
        Perc(
            "perc", 10, "Percussion",
            "Des petits coups (rim, cloche, conga, shaker) glissés entre les autres : ils donnent du caractère.",
            listOf(10, 11, 14, 13, 7, 8, 9, 15),
        ),
        Bass808(
            "808", 1, "Basse 808",
            "Une basse très grave et ronde qui suit le kick : c'est elle qui fait vibrer les enceintes.",
            listOf(0, 1),
        ),
        Tom(
            "tom", 8, "Toms",
            "Des tambours graves ou aigus qui roulent entre les coups : ils font avancer le rythme.",
            listOf(8, 7, 9),
        ),
        Ride(
            "ride", 12, "Ride (cymbale)",
            "Une cymbale qui tinte en rythme, plus brillante que le charley : elle fait briller le Drop.",
            listOf(12), packKind = "ride",
        ),
        Perc2(
            "perc2", 13, "Percussion 2",
            "Une deuxième percussion (shaker, conga, cloche…) qui répond à la première : le groove devient plus riche.",
            listOf(13, 14, 11, 10, 15), packKind = "perc",
        ),
        Rumble(
            "rumble", 3, "Rumble (grondement)",
            "Le kick rejoué dans une grande réverbe, saturé et filtré : un grondement grave qui roule entre les coups, la basse de la techno dure.",
            listOf(1, 0), packKind = "kick",
        ),
    }

    /**
     * [pattern]: one character per step (16 = one bar of 4/4): 'X' strong, 'x' medium, 'o' soft,
     * '2' '3' '4' a medium roll of that many hits in the step, '.' nothing. [tip]: why the line sits
     * where it does in this style. [voice]: the built-in kit voice that plays it best (a shaker for
     * the hats, a cowbell or a conga for the percussion), null: the pad's own voice. [build]: the
     * line in the second section (Groove, Couplet), where it builds up ("" = [pattern] there too).
     * [off]: the line is offered switched off (the techno rumble, an alternative to the bass line).
     */
    data class Line(val role: Role, val pattern: String, val tip: String, val voice: Int? = null, val build: String = "", val off: Boolean = false)

    /**
     * How the song is laid out. [Song]: Intro, Couplet, Refrain (rap, pop). [Club]: Intro, Groove,
     * Drop and a Break without kick nor bass before the last drop (house, techno, bass music), with
     * longer sections for the DJ. [Rave]: the 303 alone in the Intro (its filter opening), the kick
     * and the hats coming in on the Groove, then Drop, Break, Drop, a second Break, Drop and the
     * Intro again as the outro (acidcore). [Build]: club music whose drop really drops (bass music,
     * gabber): Intro, a Montée without kick nor bass where the snare rolls faster and faster, the
     * Drop where the kick and the bass land, the Break, the Montée again and the last Drop.
     * [labels]: the sections' names (saved in French), [roles] what each is for.
     */
    enum class SongForm(val labels: List<String>, val roles: List<String>) {
        Song(listOf("Intro", "Couplet", "Refrain"), listOf("le début, en douceur", "le groove, avec la basse", "tout le monde joue")),
        Club(listOf("Intro", "Groove", "Drop"), listOf("la batterie pour démarrer", "le groove, avec la basse", "tout le monde joue")),
        Rave(listOf("Intro", "Groove", "Drop"), listOf("la 303 seule, son filtre s'ouvre", "le kick arrive, les charleys montent", "tout le monde joue")),
        Build(
            listOf("Intro", "Montée", "Drop"),
            listOf("la batterie et l'ambiance pour démarrer", "sans kick ni basse, la caisse claire roule de plus en plus vite", "tout tombe d'un coup : le kick et la basse"),
        ),
        ;

        companion object {
            const val BREAK_LABEL = "Break"
            const val BREAK_ROLE = "une pause sans kick ni basse"
            const val BREAK_ROLE_WITH_BASS = "une pause sans kick, la basse continue"
            const val GROOVE_ROLE_NO_BASS = "le groove, la basse attend le Drop"

            /** The layout of a family's songs: club music for House, Techno and Bass music. */
            fun of(family: String): SongForm = if (family in setOf("House", "Techno", "Bass music")) Club else Song
        }
    }

    /**
     * An effect the style puts on one of its layers when the song is made. [target]: a part's id
     * ("bass", "pad", "lead"), a drum line's ("kick", "snare"…), "acid" or "drums" (the whole
     * beat) or "master" (its inserts only). [preset]: a ready-made treatment of [FxPresets] ("pump",
     * "dub_echo"…), [insert] an effect added with its defaults ([EffectType] name: "VINYL") or with
     * [params] (the first of its knobs), [reverb] and [delay] the sends.
     */
    /** Drum lines and melodic parts: what a section of a style holds back or keeps. */
    data class Layers(val lines: Set<Role> = emptySet(), val parts: Set<Part> = emptySet()) {
        val isEmpty: Boolean get() = lines.isEmpty() && parts.isEmpty()
    }

    /**
     * A low-pass filter put on [target] (a part's id, "acid" or "master") whose cutoff (0..1, the
     * Filtre effect's knob) moves along the song: for the sections of scene 0 (Intro), 1 (Groove or
     * Montée), 2 (Drop), 3 (Break), its value at their start and at their end ([sections]); a
     * section left out plays it open. The outro (the Intro again, last) runs the Intro backwards.
     */
    data class Sweep(val target: String, val sections: Map<Int, Pair<Float, Float>>, val resonance: Float = 0.25f)

    data class StyleFx(
        val target: String,
        val preset: String = "",
        val insert: String = "",
        val reverb: Float = 0f,
        val delay: Float = 0f,
        val params: List<Float> = emptyList(),
    )

    data class Style(
        val id: String,
        val name: String,
        val family: String,
        val bpm: Double,
        val description: String,
        val lines: List<Line>,
        val swing: Double = 0.0,
        val melody: Melody = Melody(),
        /** Landmark tracks, "Artist – Title (year)", to give the idea. */
        val references: List<String> = emptyList(),
        /** The pack each drum line is taken from when it is on the phone (a machine such as tr909, or the style's own). */
        val kit: Map<Role, String> = emptyMap(),
        /** How its songs are laid out (club music has a Break and longer sections). */
        val form: SongForm = SongForm.of(family),
        /** Effects put on its layers ("Vieux vinyle" on lo-fi chords, the pump on a house bass…). */
        val effects: List<StyleFx> = emptyList(),
        /** Club music: the layers held back until the Drop (the open hat of techno, the bass of a garage drop). */
        val drop: Layers = Layers(),
        /** Club music: what its Break plays (tech house keeps the bass and the percussion), null: the chords and the lead. */
        val breakWith: Layers? = null,
        /** Club music: a noise riser before each Drop and a noise impact on its first beat (the « Montée » track). */
        val riser: Boolean = false,
        /** Filters that open and close along the song on some layers (the techno stab, the pad of a Montée). */
        val sweeps: List<Sweep> = emptyList(),
    ) {
        /** Packs that make this style sound right: its machines, its melodic sources, its own pack. */
        val packs: List<String> get() =
            (kit.values + melody.packSounds.values.filter { !it.startsWith(KIT_SOUND) }.map { it.substringBefore(':') } + id).distinct()
    }

    /** A melodic part played by a voice of the built-in kit across the keys: "kit:<voice>" in [Melody.packSounds]. */
    const val KIT_SOUND = "kit:"

    /**
     * The style's melodic side: its usual key ([root] 0..11, 0 = Do), the mood proposed first,
     * the bass rhythm (one character per step, like the drum lines) and the synth sounds of
     * [SoundLibrary.SYNTH] proposed for each part.
     */
    data class Melody(
        val root: Int = 9,
        val mood: Mood = Mood.Dark,
        /** The bass line, one character per step on the bar's chord (see [withLine]: R root, 5 fifth, O octave, '/' slide…). */
        val bassPattern: String = "R.......R.......",
        val bass: String = "Basse ronde",
        val pad: String = "Nappe",
        val lead: String = "Pluck",
        /** Per mood: the scale and the chord of each of the four bars (scale degrees), as the style uses them. */
        val progressions: Map<Mood, Pair<Scale, List<Int>>> = emptyMap(),
        /** How the chords are played: "X---------------" held all bar, "..X...X...X...X." offbeat stabs… */
        val chordRhythm: String = "X---------------",
        val voicing: Voicing = Voicing.Triad,
        val chordOctave: Int = 3,
        val bassOctave: Int = 2,
        /** The lead's riff on the chords ("" = an arpeggio of them at [arpRate]). */
        val leadPattern: String = "",
        val arpRate: Int = NoteClip.TICKS_PER_STEP,
        val leadOctave: Int = 5,
        /**
         * A pack sound per part when that pack is on the phone: "pack id:kind" (e.g. "retro_techno:stab");
         * or a voice of the built-in kit played across the keys, "kit:<voice>" (the 808 cowbell).
         */
        val packSounds: Map<Part, String> = emptyMap(),
        /** The parts a beginner gets switched on (the others stay off until ticked). */
        val parts: Set<Part> = setOf(Part.Bass, Part.Pad),
        /** A TB-303 line on the acid track, where it is the style's signature. */
        val acid: AcidRecipe? = null,
        /** The part that makes the style (the boom bap's piano loop, the phonk's cowbell): it plays in every section. */
        val signature: Part? = null,
        /** What a part does in this style, shown on its step (empty: the general explanation alone). */
        val tips: Map<Part, String> = emptyMap(),
        /** The bass slides from note to note (808, "talking" bass): its synth or sample glides where the line says '/'. */
        val bassGlide: Boolean = false,
        /** The lead slides where its riff says '/' (the rave hoover's dive): its synth or sample glides. */
        val leadGlide: Boolean = false,
        /**
         * A clean sub layer under the bass (jump up: the growl in the middle, a sine an octave below for
         * the weight): the bass's notes an octave down on a track of their own ([SUB_TRACK]), playing
         * wherever the bass plays.
         */
        val subBass: Boolean = false,
        /**
         * A counter-melody in the Drop (or the Refrain): the chords answered in short hits an octave
         * above them with this sound of the library ([COUNTER_TRACK]), on [counterRhythm] ("" = the
         * chords' own rhythm when they are stabs, else [LEAD_STABS]). Null: the style wants none.
         */
        val counter: String? = null,
        val counterRhythm: String = "",
    ) {
        fun sound(part: Part) = when (part) {
            Part.Bass -> bass
            Part.Pad -> pad
            Part.Lead -> lead
        }
    }

    /**
     * A 303 riff: [pattern] as in [withLine] (one bar, or a two-bar riff of 32 steps), [accents] and
     * [slides] one 'x' per step that has one. [sound]: a 303 sound of [SoundLibrary.ACID] ("" = the
     * default one), [knobs] set on top of it ([AcidSound.values] index → 0..1), [moves] the knobs
     * turned along the song.
     */
    data class AcidRecipe(
        val pattern: String,
        val accents: String = "",
        val slides: String = "",
        val sound: String = "",
        val knobs: Map<Int, Float> = emptyMap(),
        val moves: List<AcidMove> = emptyList(),
    ) {
        /** The 303's sound for this riff: [sound] (at its calibrated level) with [knobs], keeping [base]'s tuning. */
        fun soundFrom(base: AcidSound): AcidSound {
            val preset = SoundLibrary.ACID.firstOrNull { it.name == sound }?.sound?.copy(tuning = base.tuning) ?: base
            return knobs.entries.fold(preset) { acc, (i, v) -> acc.with(i, v) }
        }
    }

    /**
     * A 303 knob ([param]: [AcidSound.values] index, one of [AutoTarget.ACID_PARAMS]) turned along the
     * song: for the sections of scene 0 (Intro), 1 (Groove), 2 (Drop), 3 (Break), its value at
     * their start and at their end ([sections]). The last section, when it is the Intro again (the
     * outro), runs the Intro's values backwards. Written as a song automation curve.
     */
    data class AcidMove(val param: Int, val sections: Map<Int, Pair<Float, Float>>)

    /** The feeling of the song: it sets the scale and the chords, so every note sounds right. */
    enum class Mood(val id: String, val label: String, val description: String, val scale: Scale, val degrees: List<Int>, val chordsLabel: String) {
        Dark(
            "dark", "Sombre / épique", "Des accords mineurs : mystérieux, intenses ou un peu tristes.",
            Scale.Minor, listOf(0, 5, 2, 6), "i – VI – III – VII",
        ),
        Happy(
            "happy", "Joyeux / énergique", "Des accords lumineux qui donnent la pêche.",
            Scale.Major, listOf(0, 4, 5, 3), "I – V – vi – IV",
        ),
        Future(
            "future", "Futuriste / électro", "Une couleur mineure froide et hypnotique, taillée pour l'électro.",
            Scale.Dorian, listOf(0, 3, 0, 6), "i – IV – i – VII",
        ),
        Dreamy(
            "dreamy", "Rêveur / planant", "Des accords doux qui flottent : on se laisse porter.",
            Scale.Major, listOf(3, 0, 4, 5), "IV – I – V – vi",
        ),
    }

    /** The melodic tracks the assistant can add next to the beat. */
    enum class Part(val id: String, val title: String, val explanation: String, val categories: List<String>, val byDefault: Boolean) {
        Bass(
            "bass", "Basse",
            "Elle joue la note principale de chaque accord, dans le grave : elle fait le lien entre la batterie et l'harmonie.",
            listOf("Basses"), true,
        ),
        Pad(
            "pad", "Nappe / accords",
            "Des accords qui posent l'ambiance : tenus ou joués en petits coups, selon le style.",
            listOf("Nappes", "Claviers"), true,
        ),
        Lead(
            "lead", "Lead / mélodie",
            "Un son plus aigu qui joue une petite mélodie sur les accords : le bout qu'on retient.",
            listOf("Leads", "Pluck", "Claviers"), false,
        ),
    }

    /**
     * A melodic sound of the library: a subtractive synth preset, a Plaits one ([plaits] set), or a
     * pack sound played across the keys ([sampled] set; its file is "pack:<pack>/<file>" until the
     * project is written, then the copy in the project's folder). [chord]: a pack sound recorded as a
     * whole chord (a stab, a pad, keys): the chords play it as one note each, not a chord of chords.
     * [family]: its library category ("Nappes", "Claviers"…) or its pack kind ("pad", "keys", "stab"…),
     * which names the chords' track ([trackName]).
     */
    data class PartSound(
        val name: String,
        val synth: SynthPatch = SynthPatch(),
        val plaits: PlaitsPatch? = null,
        val sampled: SampledPatch? = null,
        val chord: Boolean = false,
        val family: String = "",
    ) {
        /** The pack and file of a pack sound, or null. */
        val packFile: Pair<String, String>? get() = sampled?.file?.takeIf { it.startsWith(PACK_FILE) }
            ?.removePrefix(PACK_FILE)?.split('/')?.takeIf { it.size == 2 }?.let { it[0] to it[1] }

        /** The built-in kit voice played across the keys ("kit:<voice>" until the project is written), or null. */
        val kitVoice: Int? get() = sampled?.file?.takeIf { it.startsWith(KIT_SOUND) }?.removePrefix(KIT_SOUND)?.toIntOrNull()
            ?.takeIf { it in 0 until DrumPattern.PAD_COUNT }
    }

    const val PACK_FILE = "pack:"

    /**
     * The kit's own voice for [role] needs shaping into a sample on its pad (the kit's cymbal is a
     * long washy crash: as a ride it must ring short). The others play the kit voice as it is.
     */
    fun shapesKit(role: Role): Boolean = role == Role.Ride

    /**
     * [sample], a kit voice rendered for [role], shaped as that line wants it: the ride is the
     * cymbal a fifth higher (brighter, shorter), dying away in about a third of a second, and
     * softer (the research: about 6 dB under the open hat it doubles). Others: unchanged.
     */
    fun kitShaped(role: Role, sample: PadSample): PadSample = if (!shapesKit(role)) sample
    else sample.copy(name = RIDE_NAME, pitch = 7f, decay = 0.3f, sustain = 0f, release = 0.12f, gain = 0.55f)

    /** The shaped ride's name on its pad (saved in French). */
    const val RIDE_NAME = "Ride"

    /** Kit voices that make a melodic sound, and their names (the 808 cowbell of phonk and Memphis rap). */
    val KIT_MELODIC = mapOf(11 to "Cowbell 808")

    /** The sound of kit voice [voice] played across the keys by [part]. */
    fun kitPartSound(voice: Int, part: Part) = PartSound(
        KIT_MELODIC[voice] ?: "Kit $voice",
        sampled = SampledPatch(file = KIT_SOUND + voice, name = KIT_MELODIC[voice] ?: "Kit $voice", release = if (part == Part.Pad) 0.4f else 0.15f),
    )

    /** Pack sound kinds recorded as a chord ("synth pad chord, note C"): one of them per chord is enough. */
    val CHORD_KINDS = setOf("stab", "pad", "keys")

    /** Pack sounds that suit a part: 808s and basses for the bass, pads and keys for the chords, leads for the melody. */
    private fun packKinds(part: Part) = when (part) {
        Part.Bass -> listOf("808", "bass")
        Part.Pad -> listOf("pad", "keys")
        Part.Lead -> listOf("lead", "pluck", "stab")
    }

    /**
     * The sounds a part can take (its categories of the synth and Plaits libraries), the style's
     * one first, then the [installed] packs' sounds of the right kinds (the style's own pack first,
     * a few of each other pack).
     */
    fun soundsFor(part: Part, style: Style, installed: List<PackManifest> = emptyList()): List<PartSound> {
        val synth = SoundLibrary.SYNTH.filter { it.category in part.categories }.map { PartSound(it.name, synth = it.sound, family = it.category) }
        val plaitsNames = synth.map { it.name }.toSet()
        val plaits = SoundLibrary.PLAITS.filter { it.category in part.categories }
            .map { PartSound(if (it.name in plaitsNames) "${it.name} (Plaits)" else it.name, plaits = it.sound, family = it.category) }
        val all = synth + plaits
        // A kit voice played across the keys (the phonk's cowbell) is always there: it comes first.
        val kit = style.melody.packSounds[part]?.takeIf { it.startsWith(KIT_SOUND) }?.removePrefix(KIT_SOUND)?.toIntOrNull()
            ?.let { kitPartSound(it, part) }
        // The style's own pack sound first when its pack is on the phone, else its synth sound.
        val packChoice = style.melody.packSounds[part]?.takeIf { !it.startsWith(KIT_SOUND) }?.split(':')?.takeIf { it.size == 2 }
        val chosenPack = packChoice?.let { (id, _) -> installed.firstOrNull { it.id == id } }
        // The style's synth sound may come from another family (a hoover as chord stabs).
        val wanted = style.melody.sound(part)
        val preferredSynth = all.filter { it.name == wanted }.ifEmpty {
            SoundLibrary.SYNTH.filter { it.name == wanted }.map { PartSound(it.name, synth = it.sound, family = it.category) } +
                SoundLibrary.PLAITS.filter { it.name == wanted }.map { PartSound(it.name, plaits = it.sound, family = it.category) }
        }.take(1)
        val kinds = packKinds(part)
        val packs = installed.sortedBy { if (it.id == chosenPack?.id) 0 else if (it.id == style.id) 1 else 2 }.flatMap { m ->
            val own = m.id == style.id || m.id == chosenPack?.id
            // The chosen kind first in the chosen pack (its stabs for a rave lead).
            val packKinds = if (m.id == chosenPack?.id) listOf(packChoice[1]) + (kinds - packChoice[1]) else kinds
            packKinds.flatMap { m.ofKind(it) }.take(if (own) Int.MAX_VALUE else 4).map { sound ->
                val slide = sound.kind == "808" || glides(part, style)
                PartSound(
                    "${sound.name} · ${m.name}",
                    sampled = SampledPatch(
                        file = PACK_FILE + m.id + "/" + sound.file, name = "${sound.name} · ${m.name}".take(40),
                        glide = if (slide) SampledPatch.SLIDE else 0f,
                        release = when (part) {
                            Part.Bass -> 0.08f
                            Part.Pad -> 0.4f
                            Part.Lead -> 0.15f
                        },
                    ),
                    chord = sound.kind in CHORD_KINDS,
                    family = sound.kind,
                )
            }
        }
        val preferredPack = if (chosenPack != null) packs.take(1) else emptyList()
        val preferred = listOfNotNull(kit).ifEmpty { preferredPack.ifEmpty { preferredSynth } }
        return preferred + (all - preferred.toSet()) + (packs - preferred.toSet()) + (preferredSynth - preferred.toSet() - all.toSet())
    }

    /**
     * The assistant's steps for a style: style, mood, each drum line, each melodic part (not
     * without a melody, [melody] false: "Pas de mélodie"), summary.
     */
    sealed interface Step {
        data object Style : Step
        data object Mood : Step
        data class Drum(val line: Line) : Step
        /** The style's 303 line (acid house, acid techno): what it is, heard, kept or not. */
        data object Acid : Step
        data class Melodic(val part: Part) : Step
        data object Summary : Step
    }

    fun steps(style: Style?, melody: Boolean = true): List<Step> =
        if (style == null) listOf(Step.Style)
        else listOf(Step.Style, Step.Mood) + style.lines.map { Step.Drum(it) } +
            (if (melody && style.melody.acid != null) listOf(Step.Acid) else emptyList()) +
            (if (melody) Part.entries.map { Step.Melodic(it) } else emptyList()) + Step.Summary

    /** The style families, in the order shown (Benjamin's, 2026-10-22): each opens and closes. */
    val FAMILIES = listOf("Techno", "House", "Bass music", "Hip-hop", "Autres")

    /** The families open at first (those of Benjamin's favourite styles); the others wait folded. */
    val OPEN_FAMILIES = setOf("Techno", "House", "Bass music")

    /**
     * The styles in Benjamin's order (2026-10-22): his favourites first, family by family, then the
     * others in the same families (StyleRecipes.kt, scripts/packs/recipes/).
     */
    val STYLES: List<Style> by lazy { STYLE_RECIPES.sortedBy { s -> ORDER.indexOf(s.id).let { if (it < 0) ORDER.size else it } } }

    /** The order of [STYLES]: the [FAVORITES] first, then the others. */
    val ORDER = listOf(
        "techno", "detroit", "acid_techno", "hard_techno", "industrial_techno", "retro_techno", "acidcore", "trance",
        "house", "deep_house", "tech_house", "acid_house", "uk_garage",
        "dnb", "jump_up", "dubstep",
        "minimal", "gabber",
        "afro_house", "speed_garage", "amapiano", "disco",
        "liquid_dnb", "neurofunk", "jungle", "riddim",
        "trap", "drill", "phonk", "boom_bap", "grime", "jersey_club", "lofi",
        "reggaeton", "afrobeats", "synthwave", "pop",
    )

    /** How many of [STYLES] are Benjamin's favourites (they come first). */
    const val FAVORITES = 16

    /** [family]'s styles, in the order of [STYLES]. */
    fun stylesOf(family: String): List<Style> = STYLES.filter { it.family == family }

    fun style(id: String): Style? = STYLES.firstOrNull { it.id == id }

    /** The drum pattern's length for [style]: its longest line (two bars for breakbeats and rolls). */
    fun drumSteps(style: Style): Int = style.lines.maxOf { it.pattern.length }.coerceIn(DrumPattern.MIN_STEPS, DrumPattern.MAX_STEPS)

    /** [line]'s hits over [steps]: a one-bar line repeats under a two-bar one. */
    fun hits(line: Line, steps: Int): Map<Int, Hit> {
        val bar = hits(line.pattern)
        return (0 until steps step line.pattern.length).flatMap { start -> bar.map { (i, h) -> start + i to h } }
            .filter { it.first < steps }.toMap()
    }

    /** A pattern string as hits (steps without a hit are left out; '2' to '4': a roll of that many hits). */
    fun hits(pattern: String): Map<Int, Hit> = pattern.mapIndexedNotNull { i, c ->
        when (c) {
            'X' -> i to Hit(1f)
            'x' -> i to Hit(0.6f)
            'o' -> i to Hit(0.35f)
            in '2'..'4' -> i to Hit(0.6f, repeats = c - '0')
            else -> null
        }
    }.toMap()

    /**
     * The new project: the style's tempo and swing, each chosen line's sound on its pad
     * ([sounds]: role → the pad's own sound, or null for the kit voice already on that pad) and
     * its groove in the grid. Lines left out of [sounds] stay empty. With [mood], the key is set
     * and locked, each part of [parts] becomes a track with its sound and first notes, and the
     * style's 303 line plays unless [acid] is false ([chosenAcid]: the line and sound written in
     * the assistant instead of the style's; a line without a note is left out). The style's
     * effects go on what is used.
     */
    fun build(style: Style, sounds: Map<Role, PadSample?>, mood: Mood? = null, parts: Map<Part, PartSound> = emptyMap(), acid: Boolean = true, chosenAcid: AcidPattern? = null): Project {
        val steps = drumSteps(style)
        val lanes = MutableList(DrumPattern.PAD_COUNT) { DrumLane() }
        for (line in style.lines) {
            if (line.role !in sounds) continue
            lanes[line.role.lane] = DrumLane(hits = hits(line, steps), sample = sounds[line.role])
        }
        val partOfTrack = HashMap<Int, Part>()
        var project = Project(
            name = style.name,
            tempoBpm = style.bpm,
            swing = style.swing,
            drums = DrumPattern(steps = steps, lanes = lanes),
        )
        val withAcid = mood != null && acid && style.melody.acid != null && chosenAcid?.let(WizardAcid::plays) != false
        if (mood != null) {
            val harmony = harmonyOf(style, mood)
            project = project.copy(harmony = harmony)
            if (withAcid) style.melody.acid?.let { a ->
                val line = chosenAcid?.sanitized() ?: acidLine(harmony, a.pattern, a.accents, a.slides, project.acid).let { it.copy(sound = a.soundFrom(it.sound)) }
                project = project.copy(acid = line)
            }
            var first = true
            for (part in Part.entries) {
                val sound = parts[part] ?: continue
                val notes = startingNotes(part, style, mood, harmony, sound)
                // The bass (or the lead) glides where its line says '/' (its notes overlap there): the synth's portamento.
                val glides = glides(part, style)
                val name = trackName(part, sound, style)
                fun InstrumentTrack.withSound() = copy(
                    name = name, notes = notes, synth = if (glides) sound.synth.with(SynthPatch.GLIDE, BASS_GLIDE) else sound.synth,
                    engine = when {
                        sound.sampled != null -> 2
                        sound.plaits != null -> 1
                        else -> 0
                    },
                    plaits = sound.plaits ?: plaits,
                    // A pack sound plays in the octave of the part's notes (and glides only on a single line).
                    sampled = sound.sampled?.let { s ->
                        s.copy(root = SampledPatch.fitRoot(s.root, notes.notes), glide = if (isLine(notes.notes)) s.glide else 0f)
                    } ?: sampled,
                )
                if (first) {
                    project = project.withTrack(project.track(0).withSound())
                    partOfTrack[0] = part
                    first = false
                } else {
                    val added = project.addTrack(name) ?: continue
                    project = added.withTrack(added.tracks.last().withSound())
                    partOfTrack[added.tracks.last().id] = part
                }
            }
        }
        // The sub layer: the bass's notes an octave down, clean, on their own track (played where the bass plays).
        val subTrack = if (!style.melody.subBass) null else partOfTrack.entries.firstOrNull { it.value == Part.Bass }?.key
            ?.let { bass -> withSubTrack(project, bass, glides(Part.Bass, style)) }?.let { (p, id) -> project = p; id }
        val plan = planFor(style, style.lines.map { it.role }.filter { it in sounds }.toSet(), partOfTrack.values.toSet(), withAcid)
        // The counter-melody: the chords answered in short hits, in the Drop (or the Refrain).
        val counterTrack = if (!plan.counter || mood == null) null
        else withCounterTrack(project, style, mood)?.let { (p, id) -> project = p; id }
        project = withEffects(project, style, sounds.keys, partOfTrack, withAcid)
        if (subTrack != null) partOfTrack.entries.firstOrNull { it.value == Part.Bass }?.let { project = withBassOverSub(project, it.key) }
        // The default mix (each line and part at its place), then what glues it (audit « corps »).
        project = WizardMix.withDrumMix(project, style, sounds)
        project = WizardMix.withPartMix(project, style, parts, partOfTrack, counterTrack)
        project = WizardMix.withGlue(project, style, partOfTrack, sounds.isNotEmpty())
        // The « Montée » track: the noise that rises before each Drop and hits on its first beat.
        val riserTrack = if (!plan.riser) null else withRiserTrack(project, partOfTrack.isEmpty())?.let { (p, id) -> project = p; id }
        // The lines that build up in the second section play their build pattern there.
        val builds = style.lines.filter { it.build.isNotEmpty() && it.role in sounds }.associate { it.role to hits(it.copy(pattern = it.build), steps) }
        // A Montée: its own 4 bars, the lines over them and the snare rolling faster and faster.
        val montee = if (plan.form != SongForm.Build) null else style.lines.filter { it.role in sounds }.associate { line ->
            line.role to if (line.role == plan.rollLine) rollHits(BUILD_STEPS) else hits(line.copy(pattern = line.build.ifEmpty { line.pattern }), BUILD_STEPS)
        }
        val withSub = if (subTrack == null) partOfTrack else partOfTrack + (subTrack to Part.Bass)
        project = inScenes(project, plan, withSub, style.bpm, builds, montee, riserTrack, counterTrack)
        // The style's filters, and the chords' own in the Break (it breathes, then opens into the Drop).
        val sweeps = withSweepFilters(project, style.sweeps + WizardMix.breakSweeps(style, plan, partOfTrack), partOfTrack, withAcid)
        project = sweeps.first
        val sections = songSections(project)
        // The song's curves: the 303's knobs first, then the riser, then the filters that sweep.
        val curves = (if (withAcid) acidLanes(sections, style.melody.acid?.moves.orEmpty()) else emptyList()) +
            listOfNotNull(riserTrack?.let { riserLane(sections, it, plan.form) }, dropTwoTrack(plan, partOfTrack, counterTrack)?.let { dropTwoLane(sections, it, project) }) +
            sweeps.second.mapNotNull { (target, sweep) -> sectionLane(target, sections) { sweep.sections[it] ?: (OPEN to OPEN) } }
        project = withSongLanes(project, curves)
        return project.sanitized()
    }

    /** A song section: consecutive parts of one scene, with its first bar and its length in bars. */
    data class Section(val scene: Int, val start: Int, val bars: Int)

    /** The sections of [project]'s song, in order. */
    fun songSections(project: Project): List<Section> {
        val sections = ArrayList<Section>()
        var bar = 0
        for (part in project.song) {
            val bars = project.sceneSteps(part.scene) * part.repeats / project.stepsPerBar
            val last = sections.lastOrNull()
            if (last != null && last.scene == part.scene) sections[sections.size - 1] = last.copy(bars = last.bars + bars)
            else sections += Section(part.scene, bar, bars)
            bar += bars
        }
        return sections
    }

    /** [lanes] added to [project]'s automation (each replaces a lane of the same target), as many as fit. */
    private fun withSongLanes(project: Project, lanes: List<AutomationLane>): Project {
        if (lanes.isEmpty()) return project
        val kept = project.automation.filterNot { l -> lanes.any { it.target == l.target } }
        return project.copy(automation = (kept + lanes).take(AutomationLane.MAX_LANES))
    }

    /**
     * A song curve of [target] over [sections]: each section from its start value to its end value
     * ([values] of its scene; null: the section holds the value before it), the outro (the Intro's
     * scene again, last) from the Intro's end value back to its start. Null without a point.
     */
    private fun sectionLane(target: AutoTarget, sections: List<Section>, values: (Int) -> Pair<Float, Float>?): AutomationLane? {
        var lane = AutomationLane(target, mode = AutomationLane.SONG)
        sections.forEachIndexed { i, sec ->
            val (from, to) = values(sec.scene) ?: return@forEachIndexed
            val outro = i > 0 && i == sections.lastIndex && sec.scene == sections.first().scene
            val (a, b) = if (outro) to to from else from to to
            lane = lane.withSongPoint(sec.start, a).withSongPoint(sec.start + sec.bars - 1, b)
        }
        return lane.takeIf { it.songValues.isNotEmpty() }
    }

    private fun acidLanes(sections: List<Section>, moves: List<AcidMove>): List<AutomationLane> =
        moves.filter { it.param in AutoTarget.ACID_PARAMS }.distinctBy { it.param }
            .mapNotNull { move -> sectionLane(AutoTarget.acid(move.param), sections) { move.sections[it] } }

    /**
     * [moves] as song automation curves of the 303's knobs: each section of the song from its start
     * value to its end value (a section the move leaves out holds the value before it), the outro
     * (the Intro's scene again, last) from the Intro's end value back to its start.
     */
    internal fun withAcidMoves(project: Project, moves: List<AcidMove>): Project {
        if (moves.isEmpty() || project.song.isEmpty()) return project
        return withSongLanes(project, acidLanes(songSections(project), moves))
    }

    /** A filter knob wide open: the sound as it is. */
    private const val OPEN = 1f

    /**
     * The low-pass filters of [sweeps] on their strips (a part's track, the acid line, the master),
     * in a free slot, set as they sound in the Drop; and the knob each one's curve moves.
     */
    private fun withSweepFilters(project: Project, sweeps: List<Sweep>, partOfTrack: Map<Int, Part>, acid: Boolean): Pair<Project, List<Pair<AutoTarget, Sweep>>> {
        var p = project
        val targets = ArrayList<Pair<AutoTarget, Sweep>>()
        for (sweep in sweeps) {
            val ch = when (sweep.target) {
                "master" -> Mixer.MASTER
                "acid" -> 1.takeIf { acid }
                else -> Part.entries.firstOrNull { it.id == sweep.target }?.let { part ->
                    partOfTrack.entries.firstOrNull { it.value == part }?.key?.let { InstrumentTrack.mixerChannel(it) }
                }
            } ?: continue
            val inserts = p.insertsOf(ch)
            val slot = inserts.indexOfFirst { it.effect == EffectType.NONE }.takeIf { it >= 0 } ?: continue
            // Heard as in the Drop when the song is not playing (a section in a loop).
            val filter = Insert(EffectType.FILTER.ordinal, listOf(sweep.sections[2]?.first ?: OPEN, sweep.resonance, 0f, 1f))
            val edited = inserts.toMutableList().also { it[slot] = filter }
            p = p.copy(mixer = if (ch == Mixer.MASTER) p.mixer.copy(master = edited) else p.mixer.copy(
                channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(inserts = edited) else c },
            ))
            targets += AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, 0) to sweep
        }
        return p to targets
    }

    /** [project] has a filter moving along the song other than the riser's (a style's [Style.sweeps]). */
    fun hasSweeps(project: Project): Boolean {
        val riser = project.instrumentTracks.filter { it.name == RISER_TRACK || it.name == OLD_RISER_TRACK }.map { InstrumentTrack.mixerChannel(it.id) }.toSet()
        return project.automation.any { it.mode == AutomationLane.SONG && it.target.kind == AutoTarget.INSERT_PARAM && it.target.ch !in riser }
    }

    /**
     * The name of the riser's track (saved in French): not « Montée », the section's name (the
     * Live grid showed a « Montée » column next to a « Montée » row).
     */
    const val RISER_TRACK = "Souffle montant"

    /** Its name in the projects made before (phase 27 previews). */
    const val OLD_RISER_TRACK = "Montée"

    /**
     * The riser's sound: white noise (the oscillators play far below hearing and the high-pass
     * filter takes them out), no attack, a long release for the impact's tail.
     */
    val RISER_SOUND: SynthPatch = SynthPatch().with(SynthPatch.NOISE, 1f).with(SynthPatch.SUB, 0f).with(SynthPatch.FILTER_TYPE, 0.6f)
        .with(SynthPatch.CUTOFF, 0.45f).with(SynthPatch.RESONANCE, 0.15f).with(SynthPatch.FILTER_ENV, 0f).with(SynthPatch.SWEEP, 0f)
        .with(SynthPatch.A_ATTACK, 0f).with(SynthPatch.A_SUSTAIN, 1f).with(SynthPatch.A_RELEASE, 0.86f).with(SynthPatch.LEVEL, 0.4f)

    /** The riser's note: 16 Hz, so that only its noise is heard. */
    const val RISER_PITCH = 12

    /** Bars the riser rises over before a Drop (the Montée: all of it). */
    const val RISER_BARS = 4

    /**
     * The riser's track: the first track when no part took it, else a new one, with [RISER_SOUND],
     * a low-pass filter shut (silent until the song's curve opens it: a section played in a loop
     * stays quiet) and some reverb. Null when every track is taken.
     */
    private fun withRiserTrack(project: Project, firstFree: Boolean): Pair<Project, Int>? {
        val base = if (firstFree) project else project.addTrack(RISER_TRACK) ?: return null
        val id = if (firstFree) 0 else base.tracks.last().id
        var p = base.withTrack(base.track(id).copy(name = RISER_TRACK, engine = 0, synth = RISER_SOUND, instrument = null, notes = NoteClip()))
        val ch = InstrumentTrack.mixerChannel(id)
        val filter = Insert(EffectType.FILTER.ordinal, listOf(0f, 0.2f, 0f, 1f))
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = (listOf(filter) + List(Mixer.INSERTS) { Insert() }).take(Mixer.INSERTS), sends = listOf(0.25f, 0f)).sanitized() else c
        }))
        return p to id
    }

    /** The riser's notes in scene [scene] of [steps] steps: held all along before a Drop, a short hit on the Drop's first step. */
    private fun riserNotes(plan: ScenePlan, scene: Int, steps: Int): NoteClip = NoteClip(
        steps = steps,
        notes = when {
            scene in plan.riserScenes -> listOf(Note(0, (steps * NoteClip.TICKS_PER_STEP).coerceAtMost(NoteClip.MAX_TICKS), RISER_PITCH, 0.9f))
            scene == 2 -> listOf(Note(0, 2 * NoteClip.TICKS_PER_STEP, RISER_PITCH, 1f))
            else -> emptyList()
        },
    )

    /**
     * The riser's filter along the song: shut, then opening over the last [RISER_BARS] bars before
     * each Drop (all the Montée), wide open on the Drop's first bar (the impact) and shut again
     * through that bar (its tail darkens and dies away).
     */
    private fun riserLane(sections: List<Section>, track: Int, form: SongForm): AutomationLane? {
        // Song only: a section played in a loop keeps the riser shut (its value at the song's start).
        var lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, InstrumentTrack.mixerChannel(track), 0, 0), mode = AutomationLane.SONG, songOnly = true)
            .withSongPoint(0, 0f)
        sections.zipWithNext().forEach { (sec, next) ->
            if (sec.scene == 2 || next.scene != 2) return@forEach
            if (form == SongForm.Song) {
                // A song: no rise, only the impact on the Refrain's first beat.
                lane = lane.withSongPoint(next.start - 1, 0f).withSongPoint(next.start, 1f).withSongJump(next.start, true).withSongPoint(next.start + 1, 0f)
                return@forEach
            }
            val rise = if (form == SongForm.Build && sec.scene == 1) sec.bars else minOf(RISER_BARS, sec.bars)
            val from = sec.start + sec.bars - rise
            if (from > 0) lane = lane.withSongPoint(from - 1, 0f)
            lane = lane.withSongPoint(from, 0.3f).withSongPoint(sec.start + sec.bars - 1, 0.9f)
                .withSongPoint(next.start, 1f).withSongPoint(next.start + 1, 0f)
        }
        return lane.takeIf { it.songValues.count { v -> v != null } > 1 }
    }

    /** The sub layer's track (saved in French; [Melody.subBass]). */
    const val SUB_TRACK = "Sub"

    /** The Filtre knob of the bass's high-pass over a sub layer: about 120 Hz (40 Hz · 450^v). */
    const val SUB_HIGH_PASS = 0.18f

    /** The sub layer's sound: « Sub pur », an almost pure sine. */
    val SUB_SOUND: SynthPatch get() = SoundLibrary.SYNTH.first { it.name == "Sub pur" }.sound

    /**
     * A new track [SUB_TRACK] playing the notes of the bass track [bass] an octave down with
     * [SUB_SOUND] (gliding where the bass glides), ducked by the kick (the pump) so that the two
     * lows never pile up. Null when every track is taken.
     */
    private fun withSubTrack(project: Project, bass: Int, glide: Boolean): Pair<Project, Int>? {
        val added = project.addTrack(SUB_TRACK) ?: return null
        val id = added.tracks.last().id
        val notes = added.track(bass).notes.let { clip -> clip.copy(notes = clip.notes.map { it.copy(pitch = (it.pitch - 12).coerceAtLeast(0)) }) }
        var p = added.withTrack(added.track(id).copy(
            name = SUB_TRACK, engine = 0, instrument = null, notes = notes,
            synth = if (glide) SUB_SOUND.with(SynthPatch.GLIDE, BASS_GLIDE) else SUB_SOUND,
        ))
        val ch = InstrumentTrack.mixerChannel(id)
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = (listOf(Insert.of(EffectType.PUMP)) + List(Mixer.INSERTS) { Insert() }).take(Mixer.INSERTS)).sanitized() else c
        }))
        return p to id
    }

    /**
     * The bass above a sub layer: a high-pass at about 120 Hz in a free slot of its strip (after the
     * style's effects) takes its own low end out, so that the two lows never pile up.
     */
    private fun withBassOverSub(project: Project, bass: Int): Project {
        val ch = InstrumentTrack.mixerChannel(bass)
        val c = project.mixer.channels[ch]
        val slot = c.inserts.indexOfFirst { it.type == 0 }.takeIf { it >= 0 } ?: return project
        val edited = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = Insert(EffectType.FILTER.ordinal, listOf(SUB_HIGH_PASS, 0.1f, 1f, 1f)) }).sanitized()
        return project.copy(mixer = project.mixer.copy(channels = project.mixer.channels.toMutableList().also { it[ch] = edited }))
    }

    /** The counter-melody's track (saved in French; [Melody.counter]). */
    const val COUNTER_TRACK = "Contre-chant"

    /**
     * A new track [COUNTER_TRACK]: the style's chords answered in short hits an octave above them
     * ([Melody.counterRhythm], the chords' rhythm when they are stabs, else [LEAD_STABS]) with the
     * sound [Melody.counter] of the library. Null when every track is taken or the sound is unknown.
     */
    private fun withCounterTrack(project: Project, style: Style, mood: Mood): Pair<Project, Int>? {
        val m = style.melody
        val name = m.counter ?: return null
        val synth = SoundLibrary.SYNTH.firstOrNull { it.name == name }?.sound
        val plaits = SoundLibrary.PLAITS.firstOrNull { it.name == name }?.sound
        if (synth == null && plaits == null) return null
        val added = project.addTrack(COUNTER_TRACK) ?: return null
        val id = added.tracks.last().id
        val rhythm = m.counterRhythm.ifEmpty { if (WizardMix.isShort(m.chordRhythm)) m.chordRhythm else LEAD_STABS }
        val harmony = project.harmony
        val degrees = progressionOf(style, mood).second
        val notes = inLeadRange(NoteClip().withChordRhythm(degrees, harmony, rhythm, Voicing.Triad, 12 * (m.chordOctave + 2), velocity = 0.7f))
        val p = added.withTrack(added.track(id).copy(
            name = COUNTER_TRACK, engine = if (synth != null) 0 else 1, instrument = null, notes = notes,
            synth = synth ?: added.track(id).synth, plaits = plaits ?: added.track(id).plaits,
        ))
        return p to id
    }

    /**
     * The layer the first Drop holds back for its second half (the second Drop is fuller): the
     * counter-melody, else in club music a lead that only the Drop plays (never a song's hook).
     */
    private fun dropTwoTrack(plan: ScenePlan, partOfTrack: Map<Int, Part>, counter: Int?): Int? = counter
        ?: partOfTrack.entries.firstOrNull { it.value == Part.Lead }?.key?.takeIf {
            val scenes = plan.parts[Part.Lead].orEmpty()
            plan.form != SongForm.Song && 2 in scenes && 1 !in scenes && 0 !in scenes
        }

    /**
     * That layer along the song (its fader, song only: a Drop played in a loop has it): out of the
     * first half of the first Drop (or Refrain), in from its second half, there all through the next ones.
     */
    private fun dropTwoLane(sections: List<Section>, track: Int, project: Project): AutomationLane? {
        val ch = InstrumentTrack.mixerChannel(track)
        val level = project.mixer.channels[ch].volume
        val first = sections.firstOrNull { it.scene == 2 } ?: return null
        if (first.bars < 4) return null
        val half = first.start + first.bars / 2
        val lane = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch), mode = AutomationLane.SONG, songOnly = true)
            .withSongPoint(0, level).withSongPoint(first.start, 0f).withSongJump(first.start, true)
            .withSongPoint(half - 1, 0f).withSongPoint(half, level).withSongJump(half, true)
        return lane
    }

    /** [part] slides where its line says '/' in [style] (its synth's portamento, its sample's glide). */
    fun glides(part: Part, style: Style): Boolean = when (part) {
        Part.Bass -> style.melody.bassGlide
        Part.Lead -> style.melody.leadGlide
        Part.Pad -> false
    }

    /** The bass synth's portamento where its line slides (0.4 = 80 ms, an 808 slide). */
    const val BASS_GLIDE = 0.4f

    /** Ticks a note sliding into the next one ('/') still sounds after the next one starts. */
    const val SLIDE_OVERLAP = NoteClip.TICKS_PER_STEP / 2

    /** A single line (no chord): notes may overlap only by a slide's few ticks. */
    fun isLine(notes: List<Note>): Boolean =
        notes.sortedBy { it.start }.zipWithNext().none { (a, b) -> b.start == a.start || b.start < a.end - SLIDE_OVERLAP }

    /**
     * The style's effects on the strips of what is used: a part's track, a drum line's pad, the
     * acid line, the whole beat. A preset replaces the strip's effects; an insert goes in the first
     * free slot; the sends only go up.
     */
    internal fun withEffects(project: Project, style: Style, lines: Set<Role>, partOfTrack: Map<Int, Part>, acid: Boolean): Project {
        if (style.effects.isEmpty()) return project
        var channels = project.mixer.channels
        var master = project.mixer.master
        for (fx in style.effects) {
            if (fx.target == "master") {
                // The master takes inserts only, in its free slots (its limiter stays last).
                val inserts = FxPresets.byId(fx.preset)?.inserts.orEmpty() + listOfNotNull(insertOf(fx))
                for (ins in inserts) {
                    val slot = master.indexOfFirst { it.type == 0 }
                    if (slot >= 0) master = master.toMutableList().also { it[slot] = ins }
                }
                continue
            }
            val channel = Part.entries.firstOrNull { it.id == fx.target }?.let { part ->
                partOfTrack.entries.firstOrNull { it.value == part }?.key?.let { InstrumentTrack.mixerChannel(it) }
            } ?: Role.entries.firstOrNull { it.id == fx.target && it in lines }?.let { Mixer.padChannel(it.lane) }
                ?: when (fx.target) {
                    "acid" -> 1.takeIf { acid }
                    "drums" -> 0.takeIf { lines.isNotEmpty() }
                    else -> null
                } ?: continue
            var c = channels[channel]
            FxPresets.byId(fx.preset)?.let { c = c.withFx(it) }
            insertOf(fx)?.let { ins ->
                val slot = c.inserts.indexOfFirst { it.type == 0 }
                if (slot >= 0) c = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = ins })
            }
            c = c.copy(sends = c.sends.mapIndexed { i, v -> maxOf(v, listOf(fx.reverb, fx.delay).getOrElse(i) { 0f }) })
            channels = channels.toMutableList().also { it[channel] = c.sanitized() }
        }
        return project.copy(mixer = project.mixer.copy(channels = channels, master = master).sanitized())
    }

    /**
     * The mixer [style]'s effects give a song with the 303 ([acid]) and the drum [lines]: what the
     * assistant's 303 heard with the beat goes through (the acid strip, the drums' and the
     * master's effects, as in the song it makes).
     */
    fun effectsMixer(style: Style, lines: Set<Role>, acid: Boolean): Mixer = withEffects(Project(), style, lines, emptyMap(), acid).mixer

    /** The insert [fx] adds: its effect with its [StyleFx.params] (the others its defaults), or null. */
    private fun insertOf(fx: StyleFx): Insert? = EffectType.entries.firstOrNull { it.name == fx.insert && it != EffectType.NONE }?.let { type ->
        Insert(type.ordinal, (fx.params + type.defaults.drop(fx.params.size)).take(type.defaults.size))
    }

    /**
     * Where the assistant puts each line and part: the scenes are the sections of the song
     * ([form]: Intro, Couplet, Refrain; or Intro, Groove, Drop, and a Break as scene D), the first
     * three each adding to the one before (a beginner hears the layers pile up). Each line or part
     * maps to the scenes it plays in.
     */
    data class ScenePlan(
        val lines: Map<Role, Set<Int>>,
        val parts: Map<Part, Set<Int>>,
        val acid: Set<Int>,
        val form: SongForm = SongForm.Song,
        /** The Break (scene D) is there: club music with something to play without kick nor bass. */
        val hasBreak: Boolean = false,
        /** [SongForm.Build]: the line that rolls faster and faster in the Montée (the snare, or the clap). */
        val rollLine: Role? = null,
        /**
         * The « Montée » track: a noise riser in the sections before a Drop ([riserScenes]), its impact
         * on the Drop; in a song, the impact alone on the Refrain's first beat.
         */
        val riser: Boolean = false,
        /** The counter-melody ([Melody.counter]) plays in the Drop (or the Refrain), from the second half of the first one. */
        val counter: Boolean = false,
    ) {
        /** How many sections: three, or four with the Break. */
        val count: Int get() = if (hasBreak) 4 else 3

        /** The sections a Drop follows, where the riser rises: the Groove (or the Montée) and the Break. */
        val riserScenes: Set<Int> get() = if (!riser || form == SongForm.Song) emptySet() else setOfNotNull(1, 3.takeIf { hasBreak })

        /** The sections' names, saved in French ("Intro", "Couplet", "Refrain" or "Intro", "Groove", "Drop", "Break"). */
        val labels: List<String> get() = form.labels + listOfNotNull(SongForm.BREAK_LABEL.takeIf { hasBreak })

        /** The section scene [index] stands for, in the interface's language. */
        fun title(index: Int): String = tr(labels[index])

        /** What the section is for, in a few words (translated): whether the bass plays there tells the Groove and the Break apart. */
        fun role(index: Int): String = tr(roleKey(index))

        /** [role] in French: one of the form's roles, [SongForm.BREAK_ROLE] or the variants with or without the bass. */
        fun roleKey(index: Int): String {
            fun bassIn(i: Int) = i in parts[Part.Bass].orEmpty() || i in lines[Role.Bass808].orEmpty()
            val bass = Part.Bass in parts || Role.Bass808 in lines
            return when {
                index == 3 -> if (bass && bassIn(3)) SongForm.BREAK_ROLE_WITH_BASS else SongForm.BREAK_ROLE
                form == SongForm.Club && index == 1 && bass && !bassIn(1) -> SongForm.GROOVE_ROLE_NO_BASS
                else -> form.roles[index]
            }
        }

        /** The names of what scene [index] plays, in the order of the assistant (translated). */
        fun contents(index: Int): List<String> =
            lines.filterValues { index in it }.keys.sortedBy { it.ordinal }.map { tr(it.title) } +
                parts.filterValues { index in it }.keys.sortedBy { it.ordinal }.map { tr(it.title) } +
                listOfNotNull(
                    tr(ACID_TITLE).takeIf { index in acid },
                    tr(RISER_TITLE).takeIf { index in riserScenes },
                    tr(IMPACT_TITLE).takeIf { riser && index == 2 },
                    tr(COUNTER_TITLE).takeIf { counter && index == 2 },
                )

        val isEmpty: Boolean get() = (0 until count).all { contents(it).isEmpty() }

        /** "Intro", "Couplet et Refrain", "Intro, Couplet et Refrain": the sections of [scenes], translated. */
        fun sections(scenes: Set<Int>): String {
            val names = scenes.sorted().map { title(it) }
            return if (names.size <= 1) names.joinToString() else tr("%s et %s", names.dropLast(1).joinToString(", "), names.last())
        }

        companion object {
            /** The scenes' letters. */
            val NAMES = listOf("A", "B", "C", "D")
            const val ACID_TITLE = "Ligne acide"
            const val RISER_TITLE = "Montée de souffle"
            const val IMPACT_TITLE = "Impact"
            const val COUNTER_TITLE = "Contre-chant"
        }
    }

    /** Light drum lines: the intro starts with them alone. */
    private val INTRO_LINES = setOf(Role.Kick, Role.Hat)

    /** A club intro: the beat a DJ mixes on, kick, hats and percussion. */
    private val CLUB_INTRO_LINES = setOf(Role.Kick, Role.Hat, Role.Perc, Role.Perc2, Role.Tom)

    /** Drum lines kept for the chorus (they lift it). */
    private val CHORUS_LINES = setOf(Role.OpenHat, Role.Perc, Role.Perc2, Role.Ride)

    /** What a Break plays without chords nor lead: the hats and the percussion. */
    private val BREAK_LINES = setOf(Role.Hat, Role.OpenHat, Role.Perc, Role.Perc2)

    /** What a Break keeps under its chords and lead: the closed hats and the percussion. */
    private val BREAK_KEEPS = setOf(Role.Hat, Role.Perc, Role.Perc2)

    /** The parts a beginner finds switched on for [style]: the recipe's (the riddim's bass alone, the acid styles' 303 alone). */
    fun defaultParts(style: Style): Set<Part> = style.melody.parts

    /** The plan of [style]'s song with these lines, parts and acid line. */
    fun planFor(style: Style, lines: Set<Role>, parts: Set<Part>, acid: Boolean): ScenePlan =
        scenePlan(lines, parts, acid, style.melody.signature, style.form, style.drop, style.breakWith, acidInBreak = acidBreaks(style))
            .let { if (style.riser && (lines.isNotEmpty() || parts.isNotEmpty() || acid)) it.copy(riser = true) else it }
            .let { if (style.melody.counter != null && parts.isNotEmpty()) it.copy(counter = true) else it }

    /** [style]'s 303 turns its knobs in the Break (the acid house filter opening): it keeps playing there, chords or not. */
    fun acidBreaks(style: Style): Boolean = style.melody.acid?.moves?.any { 3 in it.sections } == true

    /**
     * Shares the [lines] and [parts] used (and the acid line, when there is one) between the
     * sections. The last of the three (Refrain, Drop): everything. The middle one (Couplet,
     * Groove): everything but the chorus's own layer, the lead (or the chords without a lead, never
     * the [signature]), and in a song the open hat and the percussion. The first (Intro): in a song
     * the [signature] and the hats, or the kick, the hats and the chords; in club music the kick,
     * the hats, the percussion and the signature; never the bass. The [signature] plays in every
     * section. A section that would be silent plays what the next one plays. Club music also gets a
     * Break (scene D): the chords, the lead and the signature, without kick nor bass (the acid line,
     * or the hats and the percussion, when there is no chord nor lead; with [acidInBreak], the acid
     * line whatever else plays there). Last, a section left silent (only the rumble kept, only the
     * chords in acidcore) plays what the next one plays.
     */
    fun scenePlan(
        lines: Set<Role>, parts: Set<Part>, acid: Boolean, signature: Part? = null, form: SongForm = SongForm.Song,
        drop: Layers = Layers(), breakWith: Layers? = null, acidInBreak: Boolean = false,
    ): ScenePlan {
        val plan = when (form) {
            SongForm.Rave -> ravePlan(lines, parts, acid, signature)
            SongForm.Build -> buildPlan(lines, parts, acid, signature, breakWith)
            else -> layeredPlan(lines, parts, acid, signature, form, drop, breakWith, acidInBreak)
        }
        // The rumble is the kick's own tail: it plays wherever the kick plays (else in the last section, like a bass),
        // and in the Break when the style keeps it there without the kick (industrial techno).
        if (Role.Rumble !in lines) return withoutSilence(plan)
        val inBreak = breakWith?.lines?.contains(Role.Rumble) == true && plan.hasBreak
        val rumble = (plan.lines[Role.Kick] ?: setOf(2)) + if (inBreak) setOf(3) else emptySet()
        return withoutSilence(plan.copy(lines = plan.lines + (Role.Rumble to rumble)))
    }

    /** [plan] where a section with no line, no part and no acid line plays what the next one plays (the last ones first). */
    private fun withoutSilence(plan: ScenePlan): ScenePlan {
        var p = plan
        for (i in plan.count - 2 downTo 0) {
            if (p.lines.values.any { i in it } || p.parts.values.any { i in it } || i in p.acid) continue
            fun Set<Int>.joined() = if (i + 1 in this) this + i else this
            p = p.copy(lines = p.lines.mapValues { it.value.joined() }, parts = p.parts.mapValues { it.value.joined() }, acid = p.acid.joined())
        }
        return p
    }

    /** [scenePlan] of a song or of club music: the layers pile up, section after section. */
    private fun layeredPlan(
        lines: Set<Role>, parts: Set<Part>, acid: Boolean, signature: Part?, form: SongForm, drop: Layers, breakWith: Layers?,
        acidInBreak: Boolean = false,
    ): ScenePlan {
        val bassLines = setOf(Role.Bass808, Role.Rumble)
        val sig = signature?.takeIf { it in parts }
        val chorusParts = listOf(Part.Lead, Part.Pad).firstOrNull { it in parts && it != sig }?.let { setOf(it) }.orEmpty()
        val chorusLines = when {
            form == SongForm.Song -> (lines intersect CHORUS_LINES).takeIf { (lines - it).isNotEmpty() }.orEmpty()
            chorusParts.isEmpty() -> lines intersect CHORUS_LINES
            else -> emptySet()
        }
        val verseLines = lines - chorusLines
        val verseParts = parts - chorusParts
        val introLines = when {
            form == SongForm.Club -> (lines intersect CLUB_INTRO_LINES).ifEmpty { verseLines - bassLines }
            sig != null && sig != Part.Bass -> (lines intersect setOf(Role.Hat))
            else -> (lines intersect INTRO_LINES).ifEmpty { verseLines - bassLines }
        } intersect verseLines
        // Never a layer the next section drops (the chords of a song without a lead stay for the Refrain).
        val introParts = (if (form == SongForm.Song) setOf(Part.Pad) else emptySet<Part>()).plus(listOfNotNull(sig)).minus(Part.Bass) intersect verseParts
        // A silent section plays what the next one plays (the acid line counts in the couplet).
        val verseSilent = verseLines.isEmpty() && verseParts.isEmpty() && !acid
        val couplet = if (verseSilent) lines to parts else verseLines to verseParts
        val introSilent = introLines.isEmpty() && introParts.isEmpty()
        val intro = if (introSilent) couplet else introLines to introParts
        val scenes = mutableListOf(intro, couplet, lines to parts)
        if (form == SongForm.Club && !drop.isEmpty) {
            // The layers the Drop brings are held back before it (a section never left silent by it).
            for (i in 0..1) {
                val (l, p) = scenes[i]
                val held = (l - drop.lines) to (p - drop.parts)
                if (held.first.isNotEmpty() || held.second.isNotEmpty()) scenes[i] = held
            }
        }
        var acidScenes = if (!acid) emptySet() else if (introSilent) setOf(0, 1, 2) else setOf(1, 2)
        if (form == SongForm.Club) {
            val own = breakWith?.let { (lines intersect it.lines - Role.Kick) to (parts intersect it.parts) }
                ?.takeIf { it.first.isNotEmpty() || it.second.isNotEmpty() }
            val breakParts = own?.second ?: (parts intersect (setOf(Part.Pad, Part.Lead) + listOfNotNull(sig) - Part.Bass))
            val breakAcid = acid && (breakParts.isEmpty() || acidInBreak)
            // The Break keeps the hats and the percussion under the chords: it breathes, it does not fall into a hole.
            val breakLines = own?.first ?: if (breakParts.isEmpty() && !breakAcid) lines intersect BREAK_LINES else lines intersect BREAK_KEEPS
            // A break with nothing but what the drop also has without its kick: worth a section of its own.
            if (breakParts.isNotEmpty() || breakAcid || (breakLines.isNotEmpty() && breakLines != lines)) {
                scenes += breakLines to breakParts
                if (breakAcid) acidScenes = acidScenes + 3
            }
        }
        return ScenePlan(
            lines = lines.associateWith { r -> scenes.indices.filter { r in scenes[it].first }.toSet() },
            parts = parts.associateWith { p -> scenes.indices.filter { p in scenes[it].second }.toSet() },
            acid = acidScenes,
            form = form,
            hasBreak = scenes.size == 4,
        )
    }

    /**
     * The plan of a [SongForm.Build] song: the Intro plays the kick, the hats, the percussion and the
     * [signature] (never the bass); the Montée the hats, the chords and the signature with the snare
     * (or the clap) rolling faster and faster ([ScenePlan.rollLine]), without kick nor bass; the Drop
     * everything; the Break [breakWith] (by default the chords, the lead and the signature, or the
     * hats and the percussion without them), never the kick.
     */
    private fun buildPlan(lines: Set<Role>, parts: Set<Part>, acid: Boolean, signature: Part?, breakWith: Layers?): ScenePlan {
        val sig = signature?.takeIf { it in parts && it != Part.Bass }
        val roll = listOf(Role.Snare, Role.Clap).firstOrNull { it in lines }
        val introLines = (lines intersect CLUB_INTRO_LINES).ifEmpty { lines - Role.Bass808 - Role.Kick }
        val intro = introLines to setOfNotNull(sig)
        val buildLines = (lines intersect setOf(Role.Hat)) + listOfNotNull(roll)
        val buildParts = parts intersect (setOf(Part.Pad) + listOfNotNull(sig))
        val montee = if (buildLines.isEmpty() && buildParts.isEmpty()) (lines - Role.Kick - Role.Bass808) to (parts - Part.Bass) else buildLines to buildParts
        val scenes = mutableListOf(intro, montee, lines to parts)
        val own = breakWith?.let { (lines intersect it.lines - Role.Kick) to (parts intersect it.parts) }
            ?.takeIf { it.first.isNotEmpty() || it.second.isNotEmpty() }
        val breakParts = own?.second ?: (parts intersect (setOf(Part.Pad, Part.Lead) + listOfNotNull(sig) - Part.Bass))
        val breakLines = own?.first ?: if (breakParts.isEmpty()) lines intersect BREAK_LINES else lines intersect BREAK_KEEPS
        val hasBreak = breakParts.isNotEmpty() || (breakLines.isNotEmpty() && breakLines != lines)
        if (hasBreak) scenes += breakLines to breakParts
        // A silent section (nothing chosen for it) plays the Drop without its kick and its bass, or the Drop.
        for (i in scenes.indices) if (scenes[i].first.isEmpty() && scenes[i].second.isEmpty()) {
            scenes[i] = ((lines - Role.Kick - Role.Bass808) to (parts - Part.Bass)).takeIf { it.first.isNotEmpty() || it.second.isNotEmpty() } ?: (lines to parts)
        }
        return ScenePlan(
            lines = lines.associateWith { r -> scenes.indices.filter { r in scenes[it].first }.toSet() },
            parts = parts.associateWith { p -> scenes.indices.filter { p in scenes[it].second }.toSet() },
            acid = if (acid) setOf(2) else emptySet(),
            form = SongForm.Build,
            hasBreak = hasBreak,
            rollLine = roll?.takeIf { 1 in scenes.indices && it in scenes[1].first },
        )
    }

    /**
     * The plan of a [SongForm.Rave] song (acidcore): the Intro is the acid line alone (with the
     * [signature] part), the Groove brings the kick and the hats (their build pattern) and the parts
     * but the chorus's own layer, the Drop everything, the Break the acid line with the chords and
     * the lead, without kick nor bass. Without the acid line, the Intro plays the hats and the Break
     * the hats and the percussion (or what the next section plays, rather than nothing).
     */
    private fun ravePlan(lines: Set<Role>, parts: Set<Part>, acid: Boolean, signature: Part?): ScenePlan {
        val sig = signature?.takeIf { it in parts && it != Part.Bass }
        val chorusLines = (lines intersect CHORUS_LINES).takeIf { (lines - it).isNotEmpty() }.orEmpty()
        val chorusParts = listOf(Part.Lead, Part.Pad).firstOrNull { it in parts && it != sig }?.let { setOf(it) }.orEmpty()
        val groove = (lines - chorusLines) to (parts - chorusParts)
        val introParts = setOfNotNull(sig)
        val introLines = if (acid || introParts.isNotEmpty()) emptySet() else lines intersect setOf(Role.Hat)
        val intro = if (!acid && introParts.isEmpty() && introLines.isEmpty()) groove else introLines to introParts
        val breakParts = parts intersect (setOf(Part.Pad, Part.Lead) + listOfNotNull(sig))
        val breakLines = if (acid || breakParts.isNotEmpty()) emptySet() else lines intersect BREAK_LINES
        val scenes = mutableListOf(intro, groove, lines to parts)
        val hasBreak = acid || breakParts.isNotEmpty() || (breakLines.isNotEmpty() && breakLines != lines)
        if (hasBreak) scenes += breakLines to breakParts
        return ScenePlan(
            lines = lines.associateWith { r -> scenes.indices.filter { r in scenes[it].first }.toSet() },
            parts = parts.associateWith { p -> scenes.indices.filter { p in scenes[it].second }.toSet() },
            acid = if (acid) scenes.indices.toSet() else emptySet(),
            form = SongForm.Rave,
            hasBreak = hasBreak,
        )
    }

    /** Bars of the main sections (Couplet, Refrain, Groove, Drop), of the Break, of a long intro. */
    const val SECTION_BARS = 16
    const val BREAK_BARS = 8

    /** The Montée of a [SongForm.Build] song: 8 bars, its scene a 4-bar loop where the snare rolls faster and faster. */
    const val BUILD_BARS = 8
    const val BUILD_STEPS = 64

    /** Longest song the assistant makes, with its intro and ending shortened if need be: 3 min 30. */
    const val MAX_SONG_SECONDS = 210.0

    /**
     * The song of [form]'s sections, each one (bars, scene):
     * a song is Intro, Couplet, Refrain, Couplet, Refrain and the Intro again as the ending, the
     * Couplets and Refrains 16 bars, the Intro 8 (4 when that would last over [MAX_SONG_SECONDS], and
     * then the second Couplet 8);
     * club music is Intro, Groove, Drop, Break (8 bars; a Groove without one), Drop and the Intro
     * again, the Intro 16 bars (8 when too long) and the Drops 32 when a fast tempo leaves room.
     */
    fun sectionsOf(bpm: Double, form: SongForm = SongForm.Song, hasBreak: Boolean = false, beatsPerBar: Int = 4): List<Pair<Int, Int>> {
        fun seconds(list: List<Pair<Int, Int>>) = list.sumOf { it.first } * beatsPerBar * 60.0 / bpm
        val s = SECTION_BARS
        // The rave's second Break (8 bars), or a Groove when there is no Break.
        val brk = if (hasBreak) 3 else 1
        val candidates = if (form == SongForm.Song) listOf(8, 4).map { intro -> listOf(intro to 0, s to 1, s to 2, s to 1, s to 2, intro to 0) } +
            // A slow song (lo-fi): the second Couplet half as long.
            listOf(listOf(4 to 0, s to 1, s to 2, s / 2 to 1, s to 2, 4 to 0))
        else if (form == SongForm.Rave) listOf(
            // 303 alone, kick and hats, Drop, Break with its filter sweep, Drop, a short Break, Drop, outro.
            listOf(s to 0, s to 1, 2 * s to 2, s to brk, 2 * s to 2, BREAK_BARS to brk, 3 * s / 2 to 2, s to 0),
            listOf(BREAK_BARS to 0, s to 1, s to 2, BREAK_BARS to brk, s to 2, BREAK_BARS to brk, s to 2, BREAK_BARS to 0),
        )
        else if (form == SongForm.Build) listOf(2 * s to s, s to s, s to s / 2).map { (drop, intro) ->
            // Intro, Montée, Drop, Break, Montée again, the last Drop, the Intro to end.
            listOf(intro to 0, BUILD_BARS to 1, drop to 2) + (if (hasBreak) listOf(BREAK_BARS to 3) else emptyList()) +
                listOf(BUILD_BARS to 1, drop to 2, intro to 0)
        }
        else listOf(2 * s to s, s to s, s to s / 2).map { (drop, intro) ->
            listOf(intro to 0, s to 1, drop to 2, (if (hasBreak) BREAK_BARS to 3 else s to 1), drop to 2, intro to 0)
        }
        return candidates.firstOrNull { seconds(it) <= MAX_SONG_SECONDS } ?: candidates.last()
    }

    /**
     * The song as parts of the scenes, each scene of [loopBars] bars repeated to fill its section
     * (a section longer than [SongPart.MAX_REPEATS] loops is cut in two parts of the same scene).
     */
    fun songOf(loopBars: Int, bpm: Double, beatsPerBar: Int = 4, form: SongForm = SongForm.Song, hasBreak: Boolean = false, buildBars: Int = loopBars): List<SongPart> {
        return sectionsOf(bpm, form, hasBreak, beatsPerBar).flatMap { (length, scene) ->
            // The Montée's scene has its own length (a 4-bar roll).
            val bars = (if (form == SongForm.Build && scene == 1) buildBars else loopBars).coerceAtLeast(1)
            var left = (length / bars).coerceAtLeast(1)
            val parts = ArrayList<SongPart>()
            while (left > 0) {
                val n = left.coerceAtMost(SongPart.MAX_REPEATS)
                parts += SongPart(scene, n)
                left -= n
            }
            parts
        }
    }

    /** Bars each scene of [style]'s song lasts with [plan]: the beat, or the four bars of the melodic parts. */
    fun loopBars(style: Style, plan: ScenePlan): Int {
        val acidSteps = if ((style.melody.acid?.pattern?.length ?: 0) > 16) 32 else 16
        val steps = maxOf(drumSteps(style), if (plan.parts.isNotEmpty()) 64 else 0, if (plan.acid.isNotEmpty()) acidSteps else 0)
        return (steps + 15) / 16
    }

    /** Bars scene [scene] of [style]'s song lasts with [plan] (the Montée: its 4-bar roll). */
    fun sceneBars(style: Style, plan: ScenePlan, scene: Int): Int =
        if (plan.form == SongForm.Build && scene == 1) maxOf(BUILD_STEPS / 16, loopBars(style, plan)) else loopBars(style, plan)

    /** The song the assistant makes for [style] with [plan]. */
    fun songOf(style: Style, plan: ScenePlan): List<SongPart> =
        songOf(loopBars(style, plan), style.bpm, form = plan.form, hasBreak = plan.hasBreak, buildBars = sceneBars(style, plan, 1))

    /** How long [style]'s song with [plan] lasts, in seconds. */
    fun songSeconds(style: Style, plan: ScenePlan, beatsPerBar: Int = 4): Double =
        songOf(style, plan).sumOf { it.repeats * sceneBars(style, plan, it.scene) } * beatsPerBar * 60.0 / style.bpm

    /** How long [song] lasts, in seconds, with scenes of [loopBars] bars. */
    fun seconds(song: List<SongPart>, loopBars: Int, bpm: Double, beatsPerBar: Int = 4): Double =
        song.sumOf { it.repeats } * loopBars * beatsPerBar * 60.0 / bpm

    /** [full] cut in the sections of [plan] (Intro current), named, and a song that chains them. */
    private fun inScenes(
        full: Project, plan: ScenePlan, partOfTrack: Map<Int, Part>, bpm: Double, builds: Map<Role, Map<Int, Hit>> = emptyMap(),
        montee: Map<Role, Map<Int, Hit>>? = null, riserTrack: Int? = null, counterTrack: Int? = null,
    ): Project {
        // A scene's length when nothing else sets it: the beat, or the four bars of the melodic parts.
        val loopSteps = maxOf(full.drums.steps, partOfTrack.keys.maxOfOrNull { full.track(it).notes.steps } ?: 0)
        // The beat is written out over 4 bars (a fill at the end of each phrase) where the melody already lasts them.
        val phrases = loopSteps >= WizardMix.PHRASE_STEPS && full.drums.lanes.any { it.hits.isNotEmpty() }
        fun only(index: Int): Project {
            // The Montée has its own length: its lines over its 4 bars, the roll included.
            val built = montee?.takeIf { index == 1 }
            val drums = full.drums.copy(steps = if (built != null) BUILD_STEPS else full.drums.steps, lanes = full.drums.lanes.mapIndexed { lane, l ->
                val role = Role.entries.firstOrNull { it.lane == lane }
                when {
                    role == null || index !in plan.lines[role].orEmpty() -> l.copy(hits = emptyMap())
                    built != null -> l.copy(hits = built[role].orEmpty())
                    // The second section: a line that builds up plays its build pattern there.
                    index == 1 && role in builds -> l.copy(hits = builds.getValue(role))
                    else -> l
                }
            })
            val acid = if (index in plan.acid) full.acid else full.acid.copy(steps = full.acid.steps.map { it.copy(gate = false) })
            // A section of a song with a melody: its beat written out over the 4-bar phrase, ending on a fill.
            val phrased = if (built == null && phrases) drums.copy(steps = WizardMix.PHRASE_STEPS, lanes = WizardMix.phrase(drums.lanes, drums.steps, plan.form)) else drums
            var p = full.copy(drums = phrased, acid = acid)
            for ((id, part) in partOfTrack) {
                if (index in plan.parts[part].orEmpty()) continue
                val t = p.track(id)
                p = p.withTrack(t.copy(notes = t.notes.copy(notes = emptyList())))
            }
            if (riserTrack != null) p = p.withTrack(p.track(riserTrack).copy(notes = riserNotes(plan, index, if (built != null) BUILD_STEPS else loopSteps)))
            if (counterTrack != null && index != 2) p = p.withTrack(p.track(counterTrack).let { it.copy(notes = it.notes.copy(notes = emptyList())) })
            return p
        }
        val views = (0 until plan.count).map { only(it) }
        var project = views[0].copy(
            scenes = views.mapIndexed { i, v -> v.currentSceneContent().copy(name = ScenePlan.NAMES[i]) },
            scene = 0,
        )
        val bars = project.allScenes.map { it.stepsFor(project.stepsPerBar) / project.stepsPerBar }
        val loopBars = bars.filterIndexed { i, _ -> plan.form != SongForm.Build || i != 1 }.maxOrNull() ?: bars.max()
        project = project.copy(song = songOf(loopBars, bpm, project.beatsPerBar, plan.form, plan.hasBreak, buildBars = bars.getOrElse(1) { loopBars }))
        // Section names are saved in French and shown in the interface's language.
        plan.labels.forEachIndexed { i, label -> project = project.renameScene(i, label) }
        return project
    }

    /**
     * The Montée's roll over [steps] (4 bars): quarter notes, then eighths, sixteenths, and rolls of
     * 2, 3 and 4 hits in the last bar, louder and louder, the very last step left silent before the Drop.
     */
    fun rollHits(steps: Int = BUILD_STEPS): Map<Int, Hit> = (0 until steps - 1).mapNotNull { i ->
        val bar = i * 4 / steps
        val pos = i % (steps / 4)
        val every = when (bar) {
            0 -> 4
            1 -> 2
            else -> 1
        }
        if (pos % every != 0) return@mapNotNull null
        val repeats = if (bar < 3) 1 else when {
            pos < steps / 8 -> 2
            pos < steps * 3 / 16 -> 3
            else -> 4
        }
        i to Hit((0.3f + 0.7f * i / (steps - 1)).coerceAtMost(1f), repeats = repeats)
    }.toMap()

    /**
     * The name of [part]'s track playing [sound] in [style] (saved in French, translated at
     * display): the chords are named after what is heard, so that a beginner finds « le piano »:
     * « Nappe » for a pad, « Piano », « Orgue » or « Clavier » for keys, « Stabs » for a stab
     * sample or short hits, « Accords » for another sound.
     */
    fun trackName(part: Part, sound: PartSound?, style: Style): String = when (part) {
        Part.Bass -> "Basse"
        Part.Lead -> "Lead"
        Part.Pad -> {
            val name = sound?.name.orEmpty().lowercase()
            val family = sound?.family.orEmpty()
            when {
                family == "Nappes" || family == "pad" -> "Nappe"
                "piano" in name -> "Piano"
                "orgue" in name || "organ" in name -> "Orgue"
                family == "Claviers" || family == "keys" -> "Clavier"
                family == "stab" || isStabs(style.melody.chordRhythm) -> "Stabs"
                else -> "Accords"
            }
        }
    }

    /** The names [trackName] gives the chords' track (each one saved in French, translated at display). */
    val CHORD_TRACK_NAMES = listOf("Nappe", "Piano", "Orgue", "Clavier", "Stabs", "Accords")

    /** A chord rhythm of short hits only (none longer than an eighth): stabs, not held chords. */
    private fun isStabs(rhythm: String): Boolean = StepLine.parse(rhythm).let { e -> e.isNotEmpty() && e.all { it.length <= 2 } }

    /** The scale and the four chords (scale degrees) of [mood] in [style]: the style's own, or the mood's. */
    fun progressionOf(style: Style, mood: Mood): Pair<Scale, List<Int>> = style.melody.progressions[mood] ?: (mood.scale to mood.degrees)

    /** The song's key for [mood] in [style], locked so that the keyboard plays no wrong note. */
    fun harmonyOf(style: Style, mood: Mood): Harmony = Harmony(style.melody.root, progressionOf(style, mood).first, lock = true)

    /** "i – VI – III – VII": the chords of [mood] in [style], minor in lower case. */
    fun chordsLabel(style: Style, mood: Mood): String {
        val harmony = harmonyOf(style, mood)
        val pads = harmony.chordPads(48)
        return progressionOf(style, mood).second.joinToString(" – ") { d -> pads[Math.floorMod(d, 7)].roman }
    }

    /**
     * Four bars to start from, one chord of the mood per bar, played as the style plays them: the
     * bass line on the chords' roots, the chords on the style's rhythm and voicing, the lead's riff
     * (or an arpeggio of the chords an octave up). A [sound] recorded as a chord ([PartSound.chord])
     * plays the chords as their root alone.
     */
    fun startingNotes(part: Part, style: Style, mood: Mood, harmony: Harmony, sound: PartSound? = null): NoteClip {
        val m = style.melody
        val degrees = progressionOf(style, mood).second
        return when (part) {
            Part.Pad -> {
                val voicing = if (sound?.chord == true) Voicing.Single else m.voicing
                NoteClip().withChordRhythm(degrees, harmony, m.chordRhythm, voicing, 12 * (m.chordOctave + 1), velocity = 0.75f)
            }
            Part.Lead -> inLeadRange(
                when {
                    m.leadPattern.isNotBlank() -> NoteClip().withLine(degrees, harmony, m.leadPattern, 12 * (m.leadOctave + 1), velocity = 0.8f)
                    // A sound recorded as a chord (a stab) arpeggiated would play chords of chords: it hits the chords' roots instead.
                    sound?.chord == true -> NoteClip().withChordRhythm(
                        degrees, harmony, if (isStabs(m.chordRhythm)) m.chordRhythm else LEAD_STABS, Voicing.Single, 12 * (m.leadOctave + 1), velocity = 0.7f,
                    )
                    else -> NoteClip(performance = Performance(arp = ArpMode.UpDown, arpRate = m.arpRate))
                        .withChordRhythm(degrees, harmony, "X---------------", Voicing.Triad, 12 * (m.leadOctave + 1), velocity = 0.65f)
                },
            )
            Part.Bass -> NoteClip().withLine(degrees, harmony, m.bassPattern, 12 * (m.bassOctave + 1), bass = true)
        }
    }

    /** The stabs a lead recorded as a chord plays when the style's chords are held: 3-3-2, twice a bar. */
    const val LEAD_STABS = "X..x..X.X..x..X."

    /** The highest note the assistant gives a lead (Do7 in English notation). */
    const val LEAD_TOP = 96

    /** The highest middle note of a lead's line (Mi6 in English notation): above it, a lead squeals. */
    const val LEAD_MIDDLE = 88

    /**
     * [clip] taken down by octaves while its top note is over [LEAD_TOP] or its middle note over
     * [LEAD_MIDDLE] (a riff written from the octave of a key high in the octave, a V a fifth up), as
     * long as it stays above Do3 (48, English notation): a lead stays in a playable range.
     */
    fun inLeadRange(clip: NoteClip): NoteClip {
        val pitches = clip.notes.map { it.pitch }.sorted()
        if (pitches.isEmpty()) return clip
        var shift = 0
        while ((pitches.last() + shift > LEAD_TOP || pitches[pitches.size / 2] + shift > LEAD_MIDDLE) && pitches.first() + shift - 12 >= 48) shift -= 12
        return if (shift == 0) clip else clip.copy(notes = clip.notes.map { it.copy(pitch = it.pitch + shift) })
    }
}
