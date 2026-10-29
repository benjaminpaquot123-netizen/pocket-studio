package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import kotlin.math.pow

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
            "Un grondement grave, saturé et accordé au kick, qui roule entre les coups : la basse de la techno dure.",
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
        /**
         * A rap song (hits/hiphop.md G4): the Refrain comes early, the Couplet leaves room for the voice
         * (no chords), its kick and its 808 stop 2 bars before each Refrain.
         */
        Rap(
            listOf("Intro", "Couplet", "Refrain"),
            listOf("le début, en douceur", "la place de la voix : kick, basse et mélodie, sans accords", "tout le monde joue"),
        ),
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

            /** The section a song (not club music) has in place of the Break: the Pont, without kick nor bass. */
            const val BRIDGE_LABEL = "Pont"

            /** The layout of a family's songs: club music for House, Techno and Bass music, rap for hip-hop. */
            fun of(family: String): SongForm = when (family) {
                in setOf("House", "Techno", "Bass music") -> Club
                "Hip-hop" -> Rap
                else -> Song
            }
        }

        /** A song with Couplets and Refrains (a pop or a rap song), not club music. */
        val sung: Boolean get() = this == Song || this == Rap
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
        /** Club music: a fake drop before the first Drop (riddim): the Drop's beat without its bass, a beat of silence, then the real one. */
        val fakeDrop: Boolean = false,
        /** Club music: the record pulled back (a tape stop) on the last beat before the second Drop (jungle's rewind). */
        val rewind: Boolean = false,
        /** A vocal layer of the built-in voice ([VOCAL_SOUND]): chops that answer the hook, stabs, shouts, a crushed hit. */
        val vocal: Vocal? = null,
        /**
         * Club music that does not build like EDM (deep house, minimal, Detroit, disco; final audit, report 11): its
         * Montée has no riser, no sub drop and no silent beat, the kick leaves for its last bar only under a one-bar
         * drum fill, and the whole mix's low-pass closes over it, open again on the Drop ([SongShape], [LIGHT_SWEEP_LOW]).
         */
        val lightBuild: Boolean = false,
    ) {
        /** Packs that make this style sound right: its machines, its melodic sources, its own pack. */
        val packs: List<String> get() =
            (kit.values + melody.packSounds.values.filter { !it.startsWith(KIT_SOUND) }.map { it.substringBefore(':') } + id).distinct()
    }

    /**
     * The vocal layer of a style ([Style.vocal]), sung by the built-in voice [VOCAL_SOUND] on a
     * [VOCAL_TRACK] of its own (no vocal sample is built in: the packs' chops replace it later).
     */
    enum class Vocal {
        /** Chops that answer the hook in its rests, pitched down (house, UK garage, jersey club). */
        Answer,
        /** A short vocal stab on the 4th beat of the first bar of every 4 (trap). */
        Stab,
        /** An MC's shout at the start of the Drop's second half (drum & bass). */
        Shout,
        /** A vocal hit through a bitcrusher, every 2 bars (phonk's Memphis voice). */
        Crush,
    }

    /** The vocal track's name (saved in French) and its sound, a Plaits speech voice of the library. */
    const val VOCAL_TRACK = "Voix"
    const val VOCAL_SOUND = "Voix robot"

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
         * A counter-melody in the Drop (or the Refrain) with this sound of the library ([COUNTER_TRACK]):
         * the lead's motif answered in the lead's own rests (a hocket, [MelodyBuilder.counter]) when
         * the song has a written lead, else the chords in short hits an octave above them on
         * [counterRhythm] ("" = the chords' own rhythm when they are stabs, else [LEAD_STABS]).
         * Null: the style wants none.
         */
        val counter: String? = null,
        val counterRhythm: String = "",
        /**
         * A pad of this library sound behind the chords (the leads chantier: liquid's Rhodes over a lush
         * pad, docs/hits/leads-pros.md § 4.6): the chords' notes on a track of their own
         * ([PAD_LAYER_TRACK]), [PAD_LAYER_DB] under them, playing wherever they play. Null: none.
         */
        val padLayer: String? = null,
        /** What the lead is: a singable hook, a riff, vocal chops or an arpeggio ([MelodyRules] judges each its own way). */
        val leadKind: LeadKind = LeadKind.Hook,
        /** The lead's tokens are degrees of the key, not of each bar's chord: the riff stays put while the chords move. */
        val leadOnKey: Boolean = false,
        /** The answer to [leadPattern]'s call (16 or 32 steps), played in bars 3–4 of the phrase ("" = the builder writes one). */
        val leadAnswer: String = "",
        /** A lead arpeggio ([LeadKind.Arp]): its fixed pattern over the chord's two octaves, gate and accents. */
        val leadArp: ArpSpec? = null,
        /** Bars 5–8 of the bass (64 steps; "" = bars 1–4 again with their last bar varied and a rest on its last two beats). */
        val bassVariation: String = "",
        /** A second bass sound for the second Drop (a library sound; "" = none): see [MelodyBuilder]. */
        val bassSwitch: String = "",
        /** The chords arpeggiated (synthwave, trance): the pad plays them one note at a time on this pattern. */
        val chordArp: ArpSpec? = null,
        /**
         * « Motifs de basse » (docs/hits/basses-qui-tapent.md § 4): the [BassPatterns] grid the bass plays
         * instead of the recipe's line ([BassPatterns.styled]: [bassPattern] is then the grid's); "" = the recipe's.
         */
        val bassGrid: String = "",
        /** The chords inverted so that each moves as little as possible from the one before ([withChordRhythm]). */
        val chordVoiceLead: Boolean = false,
    ) {
        fun sound(part: Part) = when (part) {
            Part.Bass -> bass
            Part.Pad -> pad
            Part.Lead -> lead
        }
    }

    /** What a style's lead is, which sets the rules it is judged by ([MelodyRules]). */
    enum class LeadKind {
        /** A singable hook: phrases, a motif that comes back, one peak, mostly stepwise. */
        Hook,
        /** A short riff that loops (techno, minimal, acid): repetition is the point, leaps and a single note allowed. */
        Riff,
        /** Vocal chops: a call, a pitched answer, long echoes between. */
        Chops,
        /** An arpeggio of the chords on a fixed pattern over two octaves. */
        Arp,
    }

    /**
     * A fixed arpeggio: [pattern] indices into the chord's notes stacked over two octaves (0 = its
     * lowest note, n = that note an octave up for n notes), [gate] the part of each step the note
     * sounds (a pluck), the first of every [accentEvery] notes louder, [rate] the step in ticks
     * (24 = a sixteenth, 48 = an eighth).
     */
    data class ArpSpec(val pattern: List<Int>, val gate: Float = 0.6f, val accentEvery: Int = 4, val rate: Int = NoteClip.TICKS_PER_STEP) {
        fun performance(): Performance = Performance(arp = ArpMode.Pattern, arpRate = rate, pattern = pattern, gate = gate, accentEvery = accentEvery).sanitized()
    }

    /**
     * The version of the hook a section plays ([MelodyBuilder]): the Intro hears its first phrase
     * alone (a preview), the Couplet a thinned-out version (at most 60 % of its notes), the
     * Refrain all of it, the second Refrain all of it doubled an octave up (or an octave up when
     * the sound glides and must stay a single line).
     */
    enum class Version { Intro, Verse, Chorus, Chorus2 }

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
        /** A downloaded multizone instrument (phase 38): the track plays it, [synth] while it is not on the phone. */
        val multi: MultiPatch? = null,
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
        // The multizone instruments on the phone (phase 38): the style's own first when it has one
        // for this part (a lo-fi's piano), the others offered after the library's sounds.
        val instruments = installed.filter { it.instrument != null && MultiInstruments.suits(it.instrument.category, part) }
            .mapNotNull { MultiInstruments.partSound(it) }
        val styleInstrument = MultiInstruments.forStyle(style.id, part)?.let { id -> instruments.firstOrNull { it.multi?.pack == id } }
        val packs = installed.filter { it.instrument == null }.sortedBy { if (it.id == chosenPack?.id) 0 else if (it.id == style.id) 1 else 2 }.flatMap { m ->
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
        // « Des basses qui tapent » (P2): in the 808 styles the engine's 808 (the style's synth sound
        // plays it, StudioDrums.bassOn808) comes first, the pack's 808 (a pure sine a phone barely
        // plays) second.
        if (part == Part.Bass && style.id in StudioDrums.BASS_808 && kit == null && preferredSynth.isNotEmpty()) {
            val first = preferredSynth + preferredPack
            return first + (all - first.toSet()) + (instruments - first.toSet()) + (packs - first.toSet())
        }
        val preferred = listOfNotNull(kit).ifEmpty { listOfNotNull(styleInstrument).ifEmpty { preferredPack.ifEmpty { preferredSynth } } }
        return preferred + (all - preferred.toSet()) + (instruments - preferred.toSet()) + (packs - preferred.toSet()) +
            (preferredSynth - preferred.toSet() - all.toSet())
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
     * Phase 36, Mode simple: « Calme (Chill, Lo-fi) », the softer styles gathered first in the
     * assistant (each one stays in its own family too): lo-fi and boom bap, the chill house of deep
     * house and amapiano, synthwave, liquid drum & bass.
     */
    const val CALM_FAMILY = "Calme (Chill, Lo-fi)"
    val CALM_STYLES = listOf("lofi", "deep_house", "amapiano", "synthwave", "boom_bap", "liquid_dnb")

    /** The calm family's styles, in its order. */
    fun calmStyles(): List<Style> = CALM_STYLES.mapNotNull(::style)

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

    /**
     * A pattern string as hits (steps without a hit are left out; '2' to '4': a roll of that many
     * hits; 'T' strong, 't' medium: a triplet, 3 hits over this step and the next, which is '.').
     */
    fun hits(pattern: String): Map<Int, Hit> = pattern.mapIndexedNotNull { i, c ->
        when (c) {
            'X' -> i to Hit(1f)
            'x' -> i to Hit(0.6f)
            'o' -> i to Hit(0.35f)
            in '2'..'4' -> i to Hit(0.6f, repeats = c - '0')
            'T' -> i to Hit(1f, repeats = Hit.TRIPLET.first, span = Hit.TRIPLET.second)
            't' -> i to Hit(0.6f, repeats = Hit.TRIPLET.first, span = Hit.TRIPLET.second)
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
    fun build(style: Style, sounds: Map<Role, PadSample?>, mood: Mood? = null, parts: Map<Part, PartSound> = emptyMap(), acid: Boolean = true, chosenAcid: AcidPattern? = null): Project =
        build(style, sounds, mood, parts, acid, chosenAcid, audibleBass = true).let { it.copy(mixer = it.mixer.withStartVolumes()) }

    /** [build], with [audibleBass] false: the song as it is before [WizardMix.withAudibleBass] (BassesTest compares the two). */
    internal fun build(style: Style, sounds: Map<Role, PadSample?>, mood: Mood?, parts: Map<Part, PartSound>, acid: Boolean, chosenAcid: AcidPattern?, audibleBass: Boolean): Project {
        val steps = drumSteps(style)
        val lanes = MutableList(DrumPattern.PAD_COUNT) { DrumLane() }
        for (line in style.lines) {
            if (line.role !in sounds) continue
            lanes[line.role.lane] = DrumLane(hits = hits(line, steps), sample = sounds[line.role])
        }
        val partOfTrack = HashMap<Int, Part>()
        val feel = DrumHumanizer.feel(style)
        // The 808 plays the bass of trap, drill, phonk and jersey club: on its pad, not on a track (S3).
        val on808 = mood != null && StudioDrums.bassOn808(style, parts[Part.Bass])
        var bass808: NoteClip? = null
        var project = Project(
            name = style.name,
            tempoBpm = style.bpm,
            // Drill swings its hats alone (their off 16ths later, DrumHumanizer): the rest plays straight.
            swing = if (feel.hatsOnlySwing) 0.0 else style.swing,
            drums = DrumPattern(steps = steps, lanes = lanes),
            // Kit v3 (S3): the short studio kick, bright hats, the four-burst clap; each voice kept at
            // kit v2's level for its role, which the mix (WizardMix) is made for.
            kitVersion = Project.KIT_V3,
            // The master brings the song to its style's loudness (club, streaming…: the « Cible »).
            mixer = Mixer(target = LoudnessTarget.AUTO),
            style = style.id,
        )
        val withAcid = mood != null && acid && style.melody.acid != null && chosenAcid?.let(WizardAcid::plays) != false
        var leadVersions: Map<Version, NoteClip> = emptyMap()
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
                // The hook in three versions: a preview in the Intro, thinned out in the Couplet, whole in the Refrain.
                if (part == Part.Lead) leadVersions = Version.entries.associateWith { startingNotes(part, style, mood, harmony, sound, it) }
                // The 808's line: the bass's notes, played by the pad (no track of its own).
                if (part == Part.Bass && on808) {
                    bass808 = notes
                    continue
                }
                // The bass (or the lead) glides where its line says '/' (its notes overlap there): the synth's portamento.
                val glides = glides(part, style)
                val name = trackName(part, sound, style)
                fun InstrumentTrack.withSound() = copy(
                    name = name, notes = notes, synth = if (glides) sound.synth.with(SynthPatch.GLIDE, BASS_GLIDE) else sound.synth,
                    engine = when {
                        sound.multi != null -> InstrumentTrack.ENGINE_MULTI
                        sound.sampled != null -> 2
                        sound.plaits != null -> 1
                        else -> 0
                    },
                    plaits = sound.plaits ?: plaits,
                    // A pack sound plays in the octave of the part's notes (and glides only on a single line).
                    sampled = sound.sampled?.let { s ->
                        s.copy(root = SampledPatch.fitRoot(s.root, notes.notes), glide = if (isLine(notes.notes)) s.glide else 0f)
                    } ?: sampled,
                    multi = sound.multi ?: multi,
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
        val mainBass = partOfTrack.entries.firstOrNull { it.value == Part.Bass }?.key
        // The sub layer: the bass's notes an octave down, clean, on their own track (played where the bass plays).
        val subTrack = if (!style.melody.subBass) null else mainBass
            ?.let { bass -> withSubTrack(project, bass, glides(Part.Bass, style)) }?.let { (p, id) -> project = p; id }
        // The second bass sound of the second Drop: the same line on its own track, faded in there while the first fades out.
        val switchTrack = if (style.melody.bassSwitch.isBlank() || mainBass == null) null
        else withBassSwitchTrack(project, mainBass, style.melody.bassSwitch, glides(Part.Bass, style))?.let { (p, id) -> project = p; id }
        // The pad behind the chords (liquid): their notes on a track of their own, playing where they play.
        val chordsTrack = partOfTrack.entries.firstOrNull { it.value == Part.Pad }?.key
        val padLayerTrack = style.melody.padLayer?.let { name -> chordsTrack?.let { withPadLayerTrack(project, it, name) } }?.let { (p, id) -> project = p; id }
        val plan = planFor(style, style.lines.map { it.role }.filter { it in sounds }.toSet(), partOfTrack.values.toSet() + listOfNotNull(Part.Bass.takeIf { bass808 != null }), withAcid)
        // The counter-melody: the lead's motif answered in its rests (or the chords in short hits), in the Drop (or the Refrain).
        val counterTrack = if (!plan.counter || mood == null) null
        else withCounterTrack(project, style, mood, leadVersions[Version.Chorus])?.let { (p, id) -> project = p; id }
        project = withEffects(project, style, sounds.keys, partOfTrack, withAcid, bass808 = bass808 != null)
        if (switchTrack != null && mainBass != null) project = withSameStrip(project, mainBass, switchTrack)
        if (padLayerTrack != null && chordsTrack != null) project = withSameStrip(project, chordsTrack, padLayerTrack)
        if (subTrack != null) for (bass in listOfNotNull(mainBass, switchTrack)) project = withBassOverSub(project, bass)
        // The default mix (each line and part at its place), then what glues it (audit « corps »).
        project = WizardMix.withDrumMix(project, style, sounds)
        // The studio voices on their pads (the 808, the tuned kicks, the rumble), with their levels.
        project = withStudioVoices(project, style, sounds, bass808 != null)
        project = WizardMix.withPartMix(project, style, parts, partOfTrack, counterTrack)
        if (padLayerTrack != null) project = withLayerLevel(project, padLayerTrack)
        project = WizardMix.withGlue(project, style, partOfTrack, sounds.isNotEmpty())
        // The « Montée » track: the noise that rises before each Drop and hits on its first beat.
        val riserTrack = if (!plan.riser) null else withRiserTrack(project, partOfTrack.isEmpty())?.let { (p, id) -> project = p; id }
        val shape = SongShape.shape(style, plan)
        // The vocal (chops that answer the hook, stabs, shouts), then the sub drop under each Drop's first beat.
        val vocal = if (mood == null || partOfTrack.isEmpty()) null else style.vocal?.let { kind ->
            vocalNotes(style, mood, project.harmony, leadVersions[Version.Chorus], PHRASE_STEPS)?.let { notes ->
                withVocalTrack(project, kind)?.let { (p, id) -> project = p; id to notes }
            }
        }
        val subDropTrack = if (partOfTrack.isEmpty() || shape.scenes.none { it.accent && it.subDrop } || plan.form.sung) null
        else withSubDropTrack(project)?.let { (p, id) -> project = p; id }
        // The crash on each Drop's first beat (the kit's cymbal pad, when no line takes it).
        val crash = withCrash(project, sounds.keys)
        project = crash.first
        // The lines that build up in the second section play their build pattern there.
        val builds = style.lines.filter { it.build.isNotEmpty() && it.role in sounds }.associate { it.role to hits(it.copy(pattern = it.build), steps) }
        // A Montée: its own 4 bars, the lines over them and the snare rolling faster and faster.
        val montee = if (plan.form != SongForm.Build) null else style.lines.filter { it.role in sounds }.associate { line ->
            line.role to if (line.role == plan.rollLine) rollHalf(0, rollTop(hits(line, steps), steps)) else hits(line.copy(pattern = line.build.ifEmpty { line.pattern }), BUILD_STEPS)
        }
        val withSub = partOfTrack + listOfNotNull(subTrack, switchTrack).associateWith { Part.Bass } + listOfNotNull(padLayerTrack).associateWith { Part.Pad }
        val leadTrack = partOfTrack.entries.firstOrNull { it.value == Part.Lead }?.key
        val tracks = SongTracks(
            withSub, riser = riserTrack, counter = counterTrack, subDrop = subDropTrack, vocal = vocal?.first, vocalNotes = vocal?.second,
            lead = leadTrack?.let { it to leadVersions }, crashLane = crash.second,
            bass808 = bass808, feel = feel, seed = DrumHumanizer.seed(style, mood), acid = withAcid,
        )
        project = inScenes(project, plan, shape, style, tracks, builds, montee)
        // The style's filters, and the chords' own in the Break (it breathes, then opens into the Drop).
        val sweeps = withSweepFilters(project, style.sweeps + WizardMix.breakSweeps(style, plan, partOfTrack) + WizardMix.grooveSweeps(style, plan, partOfTrack), partOfTrack, withAcid)
        project = sweeps.first
        val sections = songSections(project)
        // The variants that are stretches of their own (the Montée, the Pré-refrain…) ramp from what is before them to what is after.
        fun own(scene: Int) = scene >= plan.count
        // The Drop and what plays at its level: its variants, a song's Post-refrain.
        val dropKeys = sections.map { it.scene }.filter { it == 2 || (own(it) && shape.scenes.getOrNull(it)?.base == 2 && shape.scenes[it].bass) }.toSet()
        val rewind = if (style.rewind) withRewind(project, shape, project.beatsPerBar).also { project = it.first }.second else null
        // The song's curves: the 303's knobs first, then the riser, then the bass, then the filters that sweep.
        val curves = (if (withAcid) acidLanes(sections, style.melody.acid?.moves.orEmpty()) { own(it) && project.allScenes.getOrNull(it)?.label == SongShape.MONTEE } else emptyList()) +
            listOfNotNull(riserTrack?.let { riserLane(shape, it, project.beatsPerBar) }, dropTwoTrack(plan, partOfTrack, counterTrack)?.let { dropTwoLane(sections, it, project) }, rewind, silenceLane(shape, project.beatsPerBar)) +
            bassLanes(sections, dropKeys, listOfNotNull(mainBass, subTrack, switchTrack), mainBass, switchTrack, project) +
            sweeps.second.mapNotNull { (target, sweep) -> sectionLane(target, sections) { if (own(it)) null else sweep.sections[it] ?: (OPEN to OPEN) } }
        project = withSongLanes(project, curves)
        // A light Montée (Style.lightBuild): the whole mix's low-pass closing over it, open again on the Drop.
        if (style.lightBuild && plan.form != SongForm.Build) project = withLightSweep(project)
        // The drummer's fills at the 8 and the 16, the crash on the 1 of each 16 (S3): one-bar drum parts.
        project = DrumArrangement.withFillsAndCrashes(project, shape, style, feel, DrumHumanizer.seed(style, mood), crash.second)
        // S5: an honest mix and master (WizardMix.honest).
        project = WizardMix.honest(project, style, partOfTrack)
        // S4: wide and moving synths, their effects and curves (WideMix), each track's role chain (RoleMix).
        project = WideMix.apply(project, style, partOfTrack)
        // « Des basses qui tapent »: the basses heard on a phone (the bass exciter), after RoleMix's low cuts.
        if (audibleBass) project = WizardMix.withAudibleBass(project, style, partOfTrack)
        return project.sanitized()
    }

    /**
     * The pads that play a studio voice of the engine (S3, [StudioDrums]): the 808 on its pad when it
     * plays the bass ([bass808]: its level, the style's bass effects already on its strip), the kick
     * and the rumble of the styles that tune them, when their line plays the kit's voice (a sound of
     * a pack keeps its pad); the rumble's strip without the reverb a kick played again needed.
     */
    private fun withStudioVoices(project: Project, style: Style, sounds: Map<Role, PadSample?>, bass808: Boolean): Project {
        val harmony = project.harmony.takeIf { it.scale != Scale.Off } ?: Harmony(style.melody.root)
        val voices = HashMap<Int, PadVoice>()
        var channels = project.mixer.channels
        fun strip(lane: Int, f: (Channel) -> Channel) {
            val ch = Mixer.padChannel(lane)
            channels = channels.toMutableList().also { it[ch] = f(it[ch]).sanitized() }
        }
        for (role in listOf(Role.Kick, Role.Rumble)) {
            if (role !in sounds || sounds[role] != null) continue
            val voice = StudioDrums.voiceFor(style, role, harmony) ?: continue
            voices[role.lane] = voice
            if (role == Role.Rumble) strip(role.lane) { c ->
                c.copy(inserts = Mixer.sanitizeInserts(StudioDrums.RUMBLE_STRIP), volume = WizardMix.fader(StudioDrums.RUMBLE_DB))
            }
        }
        if (bass808) {
            voices[StudioDrums.LANE_808] = StudioDrums.voice808(style, harmony)
            strip(StudioDrums.LANE_808) { c ->
                // Ducked under the kick (WizardMix.honest keys the pump to the kick's sound), in a free slot.
                val free = c.inserts.indexOfFirst { it.type == 0 }
                val inserts = if (free >= 0 && Role.Kick in sounds && c.inserts.none { it.type == EffectType.PUMP.ordinal }) {
                    c.inserts.toMutableList().also { it[free] = StudioDrums.PUMP_808 }
                } else c.inserts
                c.copy(volume = WizardMix.fader(StudioDrums.level808Db(style)), inserts = inserts)
            }
        }
        return project.copy(padVoices = voices, mixer = project.mixer.copy(channels = channels))
    }

    /** A song section: consecutive parts of one scene, with its first bar and its length in bars. */
    data class Section(val scene: Int, val start: Int, val bars: Int)

    /**
     * The sections of [project]'s song, in order: consecutive parts of one section, a variant
     * counted with its section ("Drop 2", "Montée 2", "Intro 3" with the scene of "Drop", "Montée",
     * "Intro"; [Section.scene] that section's first scene).
     */
    fun songSections(project: Project): List<Section> {
        val labels = project.allScenes.map { it.label }
        val stem = Regex("""^(.+) \d+$""")
        fun key(scene: Int): Int {
            val label = labels.getOrNull(scene)?.takeIf { it.isNotBlank() } ?: return scene
            val section = stem.matchEntire(label)?.groupValues?.get(1) ?: return scene
            return labels.indexOf(section).takeIf { it in 0 until scene } ?: scene
        }
        val sections = ArrayList<Section>()
        var bar = 0
        for (part in project.song) {
            val bars = project.partSteps(part) / project.stepsPerBar
            val scene = key(part.scene)
            val last = sections.lastOrNull()
            if (last != null && last.scene == scene) sections[sections.size - 1] = last.copy(bars = last.bars + bars)
            else sections += Section(scene, bar, bars)
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

    /**
     * The 303's knobs along the song ([moves]): each section from its start value to its end value
     * ([sectionLane]'s way), and since the final audit (lot son, report 11) a 303 that plays: in the
     * Groove and the Drop each 8-bar phrase is a gesture ([ACID_PHRASE_SWING] over the section's line,
     * back down on the next phrase's first beat); a Montée ([montee], a stretch of its own the moves do
     * not name) opens from where the section before it ended to over the next Drop's start
     * ([ACID_MONTEE_LIFT]), and the Drop lands back on its own value at once.
     */
    private fun acidLanes(sections: List<Section>, moves: List<AcidMove>, montee: (Int) -> Boolean = { false }): List<AutomationLane> =
        moves.filter { it.param in AutoTarget.ACID_PARAMS }.distinctBy { it.param }
            .mapNotNull { move -> acidLane(AutoTarget.acid(move.param), sections, move, montee) }

    private fun acidLane(target: AutoTarget, sections: List<Section>, move: AcidMove, montee: (Int) -> Boolean): AutomationLane? {
        var lane = AutomationLane(target, mode = AutomationLane.SONG)
        var last: Float? = null
        var jumpNext = false
        sections.forEachIndexed { i, sec ->
            val end = sec.start + sec.bars - 1
            if (montee(sec.scene) && move.sections[sec.scene] == null) {
                val from = last ?: return@forEachIndexed
                val next = sections.getOrNull(i + 1)?.let { move.sections[it.scene] }?.first ?: from
                val top = (maxOf(from, next) + ACID_MONTEE_LIFT).coerceAtMost(ACID_MONTEE_TOP).coerceAtLeast(from)
                lane = lane.withSongPoint(sec.start, from).withSongPoint(end, top)
                last = top
                jumpNext = true
                return@forEachIndexed
            }
            val (from, to) = move.sections[sec.scene] ?: run { jumpNext = false; return@forEachIndexed }
            val outro = i > 0 && i == sections.lastIndex && sec.scene == sections.first().scene
            val (a, b) = if (outro) to to from else from to to
            lane = lane.withSongPoint(sec.start, a)
            if (jumpNext) lane = lane.withSongJump(sec.start, true)
            jumpNext = false
            // The gestures: each phrase climbs a swing over the line, the next one starts a swing under it.
            if (sec.scene == 1 || sec.scene == 2) {
                fun line(bar: Int) = a + (b - a) * (bar - sec.start) / (sec.bars - 1).coerceAtLeast(1).toFloat()
                for (q in sec.start + ACID_PHRASE_BARS until end step ACID_PHRASE_BARS) {
                    lane = lane.withSongPoint(q - 1, (line(q - 1) + ACID_PHRASE_SWING).coerceIn(0f, 1f))
                        .withSongPoint(q, (line(q) - ACID_PHRASE_SWING).coerceIn(0f, 1f)).withSongJump(q, true)
                }
            }
            lane = lane.withSongPoint(end, b)
            last = b
        }
        return lane.takeIf { it.songValues.isNotEmpty() }
    }

    /** The 303's phrase (bars) and how far over (then under) the section's line each phrase's gesture goes. */
    const val ACID_PHRASE_BARS = 8
    const val ACID_PHRASE_SWING = 0.08f

    /** A Montée opens the 303 this much over the next Drop's start value (and never past [ACID_MONTEE_TOP]). */
    const val ACID_MONTEE_LIFT = 0.12f
    const val ACID_MONTEE_TOP = 0.85f

    /**
     * [moves] as song automation curves of the 303's knobs: each section of the song from its start
     * value to its end value (a section the move leaves out holds the value before it), the outro
     * (the Intro's scene again, last) from the Intro's end value back to its start.
     */
    internal fun withAcidMoves(project: Project, moves: List<AcidMove>): Project {
        if (moves.isEmpty() || project.song.isEmpty()) return project
        return withSongLanes(project, acidLanes(songSections(project), moves) { project.allScenes.getOrNull(it)?.label == SongShape.MONTEE })
    }

    /** A filter knob wide open: the sound as it is. */
    private const val OPEN = 1f

    /** Where a light Montée's low-pass on the whole mix closes to on its last bar (about 1.5 kHz: the kick and the bass stay). */
    const val LIGHT_SWEEP_LOW = 0.6f

    /**
     * Final audit (report 11): the house and techno that do not build like EDM ([Style.lightBuild]) take
     * the Montée in a filter sweep, the way a DJ turns the mixer's filter: a low-pass on the master (a
     * free slot, open: the sound as it is), closing over each Montée down to [LIGHT_SWEEP_LOW] on its last
     * bar, wide open again on the Drop's first beat. A song curve (whole song only). Unchanged without a
     * Montée or a free slot.
     */
    internal fun withLightSweep(project: Project): Project {
        val runs = WizardMix.monteeRuns(project)
        if (runs.isEmpty() || project.automation.size >= AutomationLane.MAX_LANES) return project
        val slot = project.mixer.master.indexOfFirst { it.type == 0 }.takeIf { it >= 0 } ?: return project
        val master = project.mixer.master.toMutableList().also { it[slot] = Insert(EffectType.FILTER.ordinal, listOf(OPEN, 0.2f, 0f, 1f)).sanitized() }
        var lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, Mixer.MASTER, slot, 0), mode = AutomationLane.SONG, songOnly = true)
            .withSongPoint(0, OPEN)
        for ((first, end) in runs) {
            if (end - first < 2) continue
            lane = lane.withSongPoint(first, OPEN).withSongPoint(end - 1, LIGHT_SWEEP_LOW).withSongPoint(end, OPEN).withSongJump(end, true)
        }
        return project.copy(mixer = project.mixer.copy(master = master), automation = project.automation + lane)
    }

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

    /**
     * The riser's notes in a scene of [steps] steps ([SongShape.SceneSpec.riser]): held all along
     * (its filter opens it before a Drop, and it falls at the Break's start), ending before the
     * last beat when that beat is silent; or a short hit on a Drop's first step (the impact).
     */
    private fun riserNotes(spec: SongShape.SceneSpec, steps: Int): NoteClip = NoteClip(
        steps = steps,
        notes = when (spec.riser) {
            SongShape.Riser.Held -> listOf(Note(0, ((if (spec.silence) steps - 4 else steps) * NoteClip.TICKS_PER_STEP).coerceAtMost(NoteClip.MAX_TICKS), RISER_PITCH, if (spec.capped) RISER_HELD_CAPPED else 0.9f))
            SongShape.Riser.Hit -> listOf(Note(0, 2 * NoteClip.TICKS_PER_STEP, RISER_PITCH, 1f))
            SongShape.Riser.None -> emptyList()
        },
    )

    /**
     * The riser's filter at the top of a Montée (about 1.6 kHz): it rises, but under the Drop's impact
     * (the bench found Montées louder than their Drop; S5b: 2.1 kHz before, the bright noise the
     * limiter leaves whole carried the Montée's end over the Drop).
     */
    const val RISER_TOP = 0.6f

    /** The riser's note in a Montée: softer than its impact (the bench found the noise made Montées louder than their Drop; S5b: 0.6 before). */
    const val RISER_HELD_CAPPED = 0.5f

    /** Its filter at the start of a rise, and where the downlifter starts at a Break. */
    const val RISER_LOW = 0.25f
    const val DOWNLIFTER = 0.6f

    /**
     * The riser's filter along the song, a point per beat ([shape]'s blocks, bars of [beatsPerBar]
     * beats): shut; opening all through each Montée (or Pré-refrain, or the end of a rap Couplet)
     * up to [RISER_TOP], shut on the silent beat before the Drop; wide open on the Drop's first beat
     * (the impact), shut again through its bar (the tail darkens and dies away). A Drop without a
     * Montée before it (a song's Refrain after the Intro, the real drop after a fake one): the impact
     * alone. At a Break's start the noise falls from [DOWNLIFTER] over 2 bars (a downlifter).
     */
    private fun riserLane(shape: SongShape.Shape, track: Int, beatsPerBar: Int): AutomationLane? {
        val b = beatsPerBar.coerceAtLeast(1)
        // Song only: a section played in a loop keeps the riser shut (its value at the song's start).
        var lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, InstrumentTrack.mixerChannel(track), 0, 0), mode = AutomationLane.SONG, songOnly = true, songPerBeat = true)
            .withSongPoint(0, 0f)
        val starts = shape.blocks.runningFold(0) { acc, blk -> acc + blk.bars }
        fun dropLike(i: Int) = shape.blocks.getOrNull(i)?.sec.let { it == SongShape.Sec.Drop || it == SongShape.Sec.Fake }
        for (i in shape.blocks.indices) {
            val blk = shape.blocks[i]
            val at = starts[i] * b
            val prev = shape.blocks.getOrNull(i - 1)
            if (dropLike(i) && prev != null && !dropLike(i - 1) || (dropLike(i) && prev?.sec == SongShape.Sec.Fake && blk.sec == SongShape.Sec.Drop)) {
                // The stretch of lead-in blocks right before it.
                var first = i
                while (first > 0 && shape.blocks[first - 1].sec == SongShape.Sec.Lead) first--
                if (first < i) {
                    val from = starts[first] * b
                    if (from > 0) lane = lane.withSongPoint(from - 1, 0f)
                    lane = lane.withSongPoint(from, RISER_LOW).withSongPoint(at - 2, RISER_TOP)
                        .withSongPoint(at - 1, 0f).withSongJump(at - 1, true)
                } else {
                    lane = lane.withSongPoint(at - 1, 0f)
                }
                lane = lane.withSongPoint(at, 1f).withSongJump(at, true).withSongPoint(at + b, 0f)
            } else if (blk.sec == SongShape.Sec.Break && prev?.sec != SongShape.Sec.Break && at > 0) {
                lane = lane.withSongPoint(at - 1, 0f).withSongPoint(at, DOWNLIFTER).withSongJump(at, true).withSongPoint(at + 2 * b, 0f)
            }
        }
        return lane.takeIf { it.songValues.count { v -> v != null } > 1 }
    }

    /**
     * The beat of silence before each Drop (or Refrain, or fake drop) made total: the whole mix's
     * volume ([AutoTarget.masterVolume], song only, a point per beat) at 0 on that beat, so that no
     * reverb or echo tail sounds there; full everywhere else. Null without such a beat.
     */
    private fun silenceLane(shape: SongShape.Shape, beatsPerBar: Int): AutomationLane? {
        val b = beatsPerBar.coerceAtLeast(1)
        val starts = shape.blocks.runningFold(0) { acc, blk -> acc + blk.bars }
        var lane = AutomationLane(AutoTarget.masterVolume(), mode = AutomationLane.SONG, songOnly = true, songPerBeat = true).withSongPoint(0, 1f)
        var any = false
        for (i in shape.blocks.indices) {
            if (!shape.scenes[shape.blocks[i].scene].silence) continue
            val end = starts[i + 1] * b
            lane = lane.withSongPoint(end - 2, 1f).withSongPoint(end - 1, 0f).withSongJump(end - 1, true).withSongPoint(end, 1f).withSongJump(end, true)
            any = true
        }
        return lane.takeIf { any }
    }

    /** The sub drop's track (saved in French): a sine that falls under each Drop's first beat. */
    const val SUB_DROP_TRACK = "Sub drop"

    /**
     * The sub drop's sound: « Sub pur » held, its portamento at its longest (half a second), a
     * long release: a key pressed under a held one slides down to it.
     */
    val SUB_DROP_SOUND: SynthPatch get() = SUB_SOUND.with(SynthPatch.GLIDE, 1f).with(SynthPatch.A_RELEASE, 0.6f)

    /**
     * A sub drop on the first beat of a Drop ([SongShape.SceneSpec.subDrop]): the key's root two
     * octaves up for a tick, then held two octaves lower, so that the synth slides down between them.
     */
    private fun subDropNotes(spec: SongShape.SceneSpec, steps: Int, harmony: Harmony): NoteClip {
        if (!spec.accent || !spec.subDrop) return NoteClip(steps = steps)
        val low = 24 + Math.floorMod(harmony.root, 12)
        val t = NoteClip.TICKS_PER_STEP
        return NoteClip(steps = steps, notes = listOf(Note(0, t, low + 24, 1f), Note(t / 2, 8 * t, low, 1f)))
    }

    /** A new track [SUB_DROP_TRACK] with [SUB_DROP_SOUND], a little under the bass. Null when every track is taken. */
    private fun withSubDropTrack(project: Project): Pair<Project, Int>? {
        val added = project.addTrack(SUB_DROP_TRACK) ?: return null
        val id = added.tracks.last().id
        return added.withTrack(added.track(id).copy(name = SUB_DROP_TRACK, engine = 0, instrument = null, notes = NoteClip(), synth = SUB_DROP_SOUND)) to id
    }

    /**
     * The crash of the Drop's first beat: the kit's own cymbal (a long washy crash) on its pad when
     * no line of the style takes that pad (the ride shapes it short), a little louder, with an echo
     * and some reverb. Null: the style plays a ride there.
     */
    private fun withCrash(project: Project, lines: Set<Role>): Pair<Project, Int?> {
        if (Role.Ride in lines || lines.isEmpty()) return project to null
        val lane = Role.Ride.lane
        val ch = Mixer.padChannel(lane)
        val c = project.mixer.channels[ch].copy(volume = WizardMix.fader(CRASH_DB), sends = listOf(0.2f, 0.25f)).sanitized()
        return project.copy(mixer = project.mixer.copy(channels = project.mixer.channels.toMutableList().also { it[ch] = c })) to lane
    }

    /** The crash pad's fader: the kit's cymbal is about 6 dB under where a crash sits. */
    private const val CRASH_DB = 3f

    /** The bass under the Drop's level outside it (the Drop lifts it by as much): the research's +1.5 dB at the Drop. */
    const val BASS_LIFT_DB = 1.5f

    /**
     * The bass tracks along the song (song only: a section played in a loop hears them at their
     * fader): [BASS_LIFT_DB] under their fader outside the Drops, at it in the Drops; with a second
     * bass ([second], [Melody.bassSwitch]) the first plays up to the second Drop and the second from there on.
     */
    private fun bassLanes(sections: List<Section>, dropKeys: Set<Int>, tracks: List<Int>, first: Int?, second: Int?, project: Project): List<AutomationLane> {
        val switchAt = sections.filter { it.scene == 2 }.getOrNull(1)?.start?.takeIf { second != null && first != null }
        val under = 10f.pow(-BASS_LIFT_DB / 40f)
        return tracks.distinct().map { track ->
            val ch = InstrumentTrack.mixerChannel(track)
            val level = project.mixer.channels[ch].volume
            var lane = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch), mode = AutomationLane.SONG, songOnly = true)
            for (sec in sections) {
                fun value(bar: Int): Float {
                    val on = when {
                        switchAt == null -> 1f
                        track == first -> if (bar < switchAt) 1f else 0f
                        track == second -> if (bar < switchAt) 0f else 1f
                        else -> 1f
                    }
                    return level * on * (if (sec.scene in dropKeys) 1f else under)
                }
                lane = lane.withSongPoint(sec.start, value(sec.start)).withSongJump(sec.start, true)
                if (switchAt != null && switchAt in sec.start + 1 until sec.start + sec.bars) {
                    lane = lane.withSongPoint(switchAt - 1, value(switchAt - 1)).withSongPoint(switchAt, value(switchAt)).withSongJump(switchAt, true)
                }
                lane = lane.withSongPoint(sec.start + sec.bars - 1, value(sec.start + sec.bars - 1))
            }
            lane
        }
    }

    /**
     * The jungle's rewind: a tape stop on the master over the two beats before the silent beat that
     * opens the second Drop (the record pulled back), a song curve of its « Arrêt » knob. Null
     * without a free slot on the master or a second Drop.
     */
    private fun withRewind(project: Project, shape: SongShape.Shape, beatsPerBar: Int): Pair<Project, AutomationLane?> {
        val starts = shape.blocks.runningFold(0) { acc, blk -> acc + blk.bars }
        val drops = shape.blocks.indices.filter { i -> shape.blocks[i].sec == SongShape.Sec.Drop && shape.blocks.getOrNull(i - 1)?.sec == SongShape.Sec.Lead }
        val at = drops.getOrNull(1)?.let { starts[it] * beatsPerBar } ?: return project to null
        val master = project.mixer.master
        // In the last free slot before the limiter (the master's last).
        val slot = master.indices.lastOrNull { it < master.size - 1 && master[it].type == 0 } ?: return project to null
        val insert = Insert(EffectType.TAPE_STOP.ordinal, listOf(0f, 0.35f, 0.1f, 0f))
        val p = project.copy(mixer = project.mixer.copy(master = master.toMutableList().also { it[slot] = insert }))
        val lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, Mixer.MASTER, slot, 0), mode = AutomationLane.SONG, songOnly = true, songPerBeat = true)
            .withSongPoint(0, 0f).withSongPoint(at - 4, 0f).withSongPoint(at - 3, 1f).withSongJump(at - 3, true).withSongPoint(at - 1, 1f)
            .withSongPoint(at, 0f).withSongJump(at, true)
        return p to lane
    }

    /**
     * The vocal of [style] ([Style.vocal]) over the song's phrase of [steps] steps, in the key of
     * [harmony]: the chops' answer in the hook's rests ([lead], an octave under its middle), a stab
     * on the 4th beat of the first bar of every 4, an MC's shout, a crushed hit every 2 bars.
     */
    private fun vocalNotes(style: Style, mood: Mood, harmony: Harmony, lead: NoteClip?, steps: Int): NoteClip? {
        val kind = style.vocal ?: return null
        val t = NoteClip.TICKS_PER_STEP
        val root = 12 * 5 + Math.floorMod(harmony.root, 12)
        val fifth = harmony.degreeRoot(4, root).let { if (it - root > 12) it - 12 else it }
        val bars = steps / 16
        val notes = when (kind) {
            Vocal.Answer -> return lead?.takeIf { it.notes.isNotEmpty() }?.let { MelodyBuilder.counter(it, MelodyBuilder.keyOf(style, mood, harmony), MelodyBuilder.seed(style, mood, "vocal")) }
            Vocal.Stab -> (0 until bars step 4).map { bar -> Note((bar * 16 + 12) * t, 2 * t, root, 0.9f) }
            Vocal.Shout -> listOf(Note(0, 3 * t, fifth, 1f), Note(4 * t, 4 * t, root, 0.9f))
            Vocal.Crush -> (0 until bars step 2).map { bar -> Note((bar * 16 + 6) * t, 2 * t, fifth, 0.85f) }
        }
        return NoteClip(steps = steps, notes = notes)
    }

    /**
     * A new track [VOCAL_TRACK] with [VOCAL_SOUND] (a Plaits speech voice): a little echo and
     * reverb, under the lead; through a bitcrusher for the phonk's hit. Null when every track is
     * taken or the sound is not in the library.
     */
    private fun withVocalTrack(project: Project, kind: Vocal): Pair<Project, Int>? {
        val sound = SoundLibrary.PLAITS.firstOrNull { it.name == VOCAL_SOUND }?.sound ?: return null
        val added = project.addTrack(VOCAL_TRACK) ?: return null
        val id = added.tracks.last().id
        var p = added.withTrack(added.track(id).copy(name = VOCAL_TRACK, engine = 1, instrument = null, notes = NoteClip(), plaits = sound.copy(gain = SynthPatch.gainValue(SynthPatch.gainDb(sound.gain) + VOCAL_DB))))
        val ch = InstrumentTrack.mixerChannel(id)
        val inserts = if (kind == Vocal.Crush) listOf(Insert(EffectType.BITCRUSHER.ordinal, listOf(0.6f, 0.45f, 1f, 0.7f))) else emptyList()
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = (inserts + List(Mixer.INSERTS) { Insert() }).take(Mixer.INSERTS), sends = listOf(0.2f, if (kind == Vocal.Crush) 0.1f else 0.3f)).sanitized() else c
        }))
        return p to id
    }

    /** The vocal's level against its calibrated sound: under the lead, it answers. */
    private const val VOCAL_DB = -4f

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
        val notes = added.track(bass).notes.let { clip -> clip.copy(notes = subNotes(clip.notes)) }
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

    /** A bass note's sub: an octave down, never under Do1 (32.7 Hz; « des basses qui tapent »: nothing under 30 Hz). */
    internal fun subPitch(bass: Int): Int = (bass - 12).let { if (it < 24) it + 12 else it }.coerceAtLeast(0)

    /**
     * The [SUB_TRACK]'s notes for a bass playing [bass]: one note at a time (a power chord's fifth and
     * octave, « Motifs de basse », stay over the sub: the lowest note of each onset), each at [subPitch],
     * in the order of their starts. [SubLayers] compares a sub with the same transformation.
     */
    fun subNotes(bass: List<Note>): List<Note> =
        bass.groupBy { it.start }.values.map { same -> same.minBy { it.pitch } }.sortedBy { it.start }.map { it.copy(pitch = subPitch(it.pitch)) }

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

    /** The second bass's track (saved in French; [Melody.bassSwitch]): the sound the second Drop switches to. */
    const val BASS_SWITCH_TRACK = "Basse 2"

    /**
     * A new track [BASS_SWITCH_TRACK] playing the bass track [bass]'s notes with the library sound
     * [name] (gliding where the bass glides): silent until the second Drop, where it takes over
     * from the first bass ([bassLanes]). Null when every track is taken or the sound is unknown.
     */
    private fun withBassSwitchTrack(project: Project, bass: Int, name: String, glide: Boolean): Pair<Project, Int>? {
        val synth = SoundLibrary.SYNTH.firstOrNull { it.name == name }?.sound
        val plaits = SoundLibrary.PLAITS.firstOrNull { it.name == name }?.sound
        if (synth == null && plaits == null) return null
        val added = project.addTrack(BASS_SWITCH_TRACK) ?: return null
        val id = added.tracks.last().id
        val p = added.withTrack(added.track(id).copy(
            name = BASS_SWITCH_TRACK, engine = if (synth != null) 0 else 1, instrument = null, notes = added.track(bass).notes,
            synth = (synth ?: added.track(id).synth).let { if (glide && synth != null) it.with(SynthPatch.GLIDE, BASS_GLIDE) else it },
            plaits = plaits ?: added.track(id).plaits,
        ))
        return p to id
    }

    /** The pad behind the chords ([Melody.padLayer]; saved in French, a default name). */
    const val PAD_LAYER_TRACK = "Nappe"

    /** The pad layer under the chords (dB): heard as air behind them, not as a second part. */
    const val PAD_LAYER_DB = -12.5f

    /** A new track [PAD_LAYER_TRACK] with the chords track [chords]'s notes and the library sound [name]; null when full or unknown. */
    private fun withPadLayerTrack(project: Project, chords: Int, name: String): Pair<Project, Int>? {
        val synth = SoundLibrary.SYNTH.firstOrNull { it.name == name }?.sound
        val plaits = SoundLibrary.PLAITS.firstOrNull { it.name == name }?.sound
        if (synth == null && plaits == null) return null
        val added = project.addTrack(PAD_LAYER_TRACK) ?: return null
        val id = added.tracks.last().id
        val p = added.withTrack(added.track(id).copy(
            name = PAD_LAYER_TRACK, engine = if (synth != null) 0 else 1, instrument = null, notes = added.track(chords).notes,
            synth = synth ?: added.track(id).synth, plaits = plaits ?: added.track(id).plaits,
        ))
        return p to id
    }

    /** The pad layer [layer] [PAD_LAYER_DB] under the chords, as a held pad (the part mix's -4 dB) would sit. */
    private fun withLayerLevel(project: Project, layer: Int): Project {
        val t = project.track(layer)
        val db = WizardMix.HELD_PAD_DB + PAD_LAYER_DB
        return project.withTrack(when (t.engine) {
            0 -> t.copy(synth = t.synth.with(SynthPatch.GAIN, SynthPatch.gainValue(SynthPatch.gainDb(t.synth.get(SynthPatch.GAIN)) + db)))
            else -> t.copy(plaits = t.plaits.copy(gain = SynthPatch.gainValue(SynthPatch.gainDb(t.plaits.gain) + db)))
        })
    }

    /** Track [to]'s strip as track [from]'s (its effects and sends: the second bass goes through the first's chain). */
    private fun withSameStrip(project: Project, from: Int, to: Int): Project {
        val src = project.mixer.channels[InstrumentTrack.mixerChannel(from)]
        val ch = InstrumentTrack.mixerChannel(to)
        return project.copy(mixer = project.mixer.copy(channels = project.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(inserts = src.inserts, sends = src.sends).sanitized() else c }))
    }

    /** The counter-melody's track (saved in French; [Melody.counter]). */
    const val COUNTER_TRACK = "Contre-chant"

    /**
     * A new track [COUNTER_TRACK] with the sound [Melody.counter] of the library: the lead's motif
     * answered in the lead's rests ([MelodyBuilder.counter]) when the song has a written lead
     * ([lead]), else the style's chords in short hits an octave above them ([Melody.counterRhythm],
     * the chords' rhythm when they are stabs, else [LEAD_STABS]). Null when every track is taken or
     * the sound is unknown.
     */
    private fun withCounterTrack(project: Project, style: Style, mood: Mood, lead: NoteClip?): Pair<Project, Int>? {
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
        val melodic = lead != null && m.leadPattern.isNotBlank() && m.leadKind != LeadKind.Arp && lead.notes.isNotEmpty()
        val notes = if (melodic) MelodyBuilder.counter(lead!!, MelodyBuilder.keyOf(style, mood, harmony), MelodyBuilder.seed(style, mood, "counter"))
        else inLeadRange(NoteClip().withChordRhythm(degrees + degrees, harmony, rhythm, Voicing.Triad, 12 * (m.chordOctave + 2), velocity = 0.7f))
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
            !plan.form.sung && 2 in scenes && 1 !in scenes && 0 !in scenes
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
        // A « Motifs de basse » grid has no slide ('/'): its notes never overlap, but a power chord's
        // three notes start together and a gliding synth would slide root -> octave instead of a chord.
        Part.Bass -> style.melody.bassGlide && style.melody.bassGrid.isBlank()
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
    internal fun withEffects(project: Project, style: Style, lines: Set<Role>, partOfTrack: Map<Int, Part>, acid: Boolean, bass808: Boolean = false): Project {
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
                // The 808 playing the bass takes the bass's treatment on its pad.
                ?: Mixer.padChannel(StudioDrums.LANE_808).takeIf { bass808 && fx.target == Part.Bass.id }
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
        /**
         * The scenes the song adds to these ([SongShape]): the Montée before each Drop, the Drop's
         * second scene, the layers coming in every 8 bars, a song's Pré-refrain… (after the [count] first).
         */
        val variants: List<SongShape.SceneSpec> = emptyList(),
    ) {
        /** How many sections: three, or four with the Break (or a song's Pont). */
        val count: Int get() = if (hasBreak) 4 else 3

        /** How many scenes the song has: the sections and their [variants]. */
        val sceneCount: Int get() = count + variants.size

        /** The sections a Drop follows, where the riser rises: the Groove (or the Montée) and the Break. */
        val riserScenes: Set<Int> get() = if (!riser || form.sung) emptySet() else setOfNotNull(1, 3.takeIf { hasBreak })

        /** The sections' names, saved in French ("Intro", "Couplet", "Refrain" or "Intro", "Groove", "Drop", "Break"). */
        val labels: List<String> get() = form.labels + listOfNotNull((if (form.sung) SongForm.BRIDGE_LABEL else SongForm.BREAK_LABEL).takeIf { hasBreak })

        /** Every scene's name, saved in French: the sections', then the variants' ("Drop 2", "Montée"…). */
        val sceneLabels: List<String> get() = labels + variants.map { it.label }

        /** The section scene [index] stands for, in the interface's language ("Drop 2" for a variant). */
        fun title(index: Int): String = sceneLabels.getOrNull(index)?.let { label -> Scene.shownLabel(label).takeIf { it != label } ?: tr(label) } ?: NAMES.getOrElse(index) { "?" }

        /** What the section is for, in a few words (translated): whether the bass plays there tells the Groove and the Break apart. */
        fun role(index: Int): String = tr(roleKey(index))

        /** [role] in French: one of the form's roles, [SongForm.BREAK_ROLE] or the variants with or without the bass (a variant: its section's). */
        fun roleKey(index: Int): String {
            if (index >= count) return variants.getOrNull(index - count)?.let { roleKey(it.base.coerceIn(0, count - 1)) } ?: form.roles[0]
            fun bassIn(i: Int) = i in parts[Part.Bass].orEmpty() || i in lines[Role.Bass808].orEmpty()
            val bass = Part.Bass in parts || Role.Bass808 in lines
            return when {
                index == 3 -> if (bass && bassIn(3)) SongForm.BREAK_ROLE_WITH_BASS else SongForm.BREAK_ROLE
                form == SongForm.Club && index == 1 && bass && !bassIn(1) -> SongForm.GROOVE_ROLE_NO_BASS
                else -> form.roles[index]
            }
        }

        /** The names of what scene [index] plays, in the order of the assistant (translated). */
        fun contents(index: Int): List<String> = variants.getOrNull(index - count)?.takeIf { index >= count }?.let { v ->
            v.content.lines.sortedBy { it.ordinal }.map { tr(it.title) } + v.content.parts.sortedBy { it.ordinal }.map { tr(it.title) } +
                listOfNotNull(tr(ACID_TITLE).takeIf { v.content.acid }, tr(RISER_TITLE).takeIf { riser && v.riser == SongShape.Riser.Held && v.half >= 0 })
        } ?: lines.filterValues { index in it }.keys.sortedBy { it.ordinal }.map { tr(it.title) } +
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
        scenePlan(lines, parts, acid, style.melody.signature, style.form, style.drop, style.breakWith, acidInBreak = acidBreaks(style), bridge = style.form.sung)
            .let { if (style.riser && (lines.isNotEmpty() || parts.isNotEmpty() || acid)) it.copy(riser = true) else it }
            .let { if (style.melody.counter != null && parts.isNotEmpty()) it.copy(counter = true) else it }
            .let { plan -> plan.copy(variants = SongShape.shape(style, plan).scenes.drop(plan.count)) }

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
        drop: Layers = Layers(), breakWith: Layers? = null, acidInBreak: Boolean = false, bridge: Boolean = false,
    ): ScenePlan {
        val plan = when (form) {
            SongForm.Rave -> ravePlan(lines, parts, acid, signature)
            SongForm.Build -> buildPlan(lines, parts, acid, signature, breakWith)
            else -> layeredPlan(lines, parts, acid, signature, form, drop, breakWith, acidInBreak, bridge)
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
        acidInBreak: Boolean = false, bridge: Boolean = false,
    ): ScenePlan {
        val bassLines = setOf(Role.Bass808, Role.Rumble)
        val sig = signature?.takeIf { it in parts }
        // A song's hook is heard everywhere (previewed in the Intro, thinned out in the Couplet): the chords lift its Refrain,
        // or the open hat and the percussion when the chords are the signature.
        val chorusParts = (if (form.sung) listOf(Part.Pad) else listOf(Part.Lead, Part.Pad)).firstOrNull { it in parts && it != sig }?.let { setOf(it) }.orEmpty()
        val chorusLines = when {
            form.sung -> (lines intersect CHORUS_LINES).takeIf { (lines - it).isNotEmpty() }.orEmpty()
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
        // Never a layer the next section drops (the chords of a song without a lead stay for the Refrain); a song's hook is previewed.
        // A rap song's Intro leads straight into its Refrain: it opens on the loop, the hook and its chords over the hats (S5b:
        // the hook alone left those intros 10 to 15 LU under their Refrain).
        val introParts = (if (form.sung) setOf(Part.Pad, Part.Lead) else emptySet<Part>()).plus(listOfNotNull(sig)).minus(Part.Bass) intersect
            (if (form == SongForm.Rap) parts else verseParts)
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
        } else if (bridge) {
            // A song's Pont: the chords and the hook (or the signature) over the hats, without kick nor bass.
            val bridgeParts = parts intersect (setOf(Part.Pad, Part.Lead) + listOfNotNull(sig) - Part.Bass)
            if (bridgeParts.isNotEmpty()) scenes += (lines intersect BREAK_KEEPS) to bridgeParts
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
     * The song of [form]'s sections, each one (bars, scene), as [SongShape.layout] lays them out
     * (consecutive stretches of one scene, such as a Groove and the Montée built on it, make one):
     * a song is Intro, Couplet, Pré-refrain, Refrain, Post-refrain, Couplet, Refrain, Pont, Refrain
     * and the Intro again; a rap song puts its Refrain first; club music is Intro, Groove, Montée,
     * Drop, Break, Montée, Drop and the Intro again; [long] songs (techno, bass music) last 5 to 6
     * minutes, the others at most [MAX_SONG_SECONDS].
     */
    fun sectionsOf(bpm: Double, form: SongForm = SongForm.Song, hasBreak: Boolean = false, beatsPerBar: Int = 4, long: Boolean = false): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        for (span in SongShape.layout(form, bpm, hasBreak, long, beatsPerBar = beatsPerBar)) {
            val last = out.lastOrNull()
            if (last != null && last.second == span.sec.base) out[out.size - 1] = (last.first + span.bars) to last.second
            else out += span.bars to span.sec.base
        }
        return out
    }

    /**
     * The song as parts of the plan's scenes only (no variant), each scene of [loopBars] bars
     * repeated to fill its section (a section longer than [SongPart.MAX_REPEATS] loops is cut in
     * two parts of the same scene).
     */
    fun songOf(loopBars: Int, bpm: Double, beatsPerBar: Int = 4, form: SongForm = SongForm.Song, hasBreak: Boolean = false, buildBars: Int = loopBars, introBars: Int = loopBars, long: Boolean = false): List<SongPart> {
        return sectionsOf(bpm, form, hasBreak, beatsPerBar, long).flatMap { (length, scene) ->
            // The Montée's scene has its own length (a 4-bar roll); a song's Intro may be cut short.
            val bars = (if (form == SongForm.Build && scene == 1) buildBars else if (scene == 0) introBars else loopBars).coerceAtLeast(1)
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

    /** Bars each scene of [style]'s song lasts with [plan]: the beat, or the eight bars of the melodic parts. */
    fun loopBars(style: Style, plan: ScenePlan): Int {
        val acidSteps = if ((style.melody.acid?.pattern?.length ?: 0) > 16) 32 else 16
        val steps = maxOf(drumSteps(style), if (plan.parts.isNotEmpty()) PHRASE_STEPS else 0, if (plan.acid.isNotEmpty()) acidSteps else 0)
        return (steps + 15) / 16
    }

    /** Steps of the melodic parts' clips: an eight-bar phrase. */
    const val PHRASE_STEPS = 128

    /**
     * Bars scene [scene] of [style]'s song lasts with [plan]: the Montée its 4-bar roll; a song's
     * Intro the bars [sectionsOf] gives it when that is shorter than the loop (its clips are cut);
     * a variant ([ScenePlan.variants]) 4 bars when it is half a phrase, else as its section.
     */
    fun sceneBars(style: Style, plan: ScenePlan, scene: Int): Int {
        val variant = plan.variants.getOrNull(scene - plan.count)?.takeIf { scene >= plan.count }
        if (variant != null) return if (variant.half >= 0) SongShape.HALF_BARS else sceneBars(style, plan, variant.base)
        return when {
            plan.form == SongForm.Build && scene == 1 -> BUILD_STEPS / 16
            plan.form.sung && scene == 0 -> minOf(loopBars(style, plan), introBarsOf(style, plan))
            else -> loopBars(style, plan)
        }
    }

    /** The bars of the first section of [style]'s song with [plan] (a song's Intro: 4 or 8). */
    private fun introBarsOf(style: Style, plan: ScenePlan): Int =
        SongShape.layout(plan.form, style.bpm, plan.hasBreak, SongShape.isLong(style), style.fakeDrop).first().bars

    /** [plan] with the variants of [style]'s song (a plan made without its style has none). */
    private fun withVariants(style: Style, plan: ScenePlan): ScenePlan =
        if (plan.variants.isNotEmpty() || plan.isEmpty) plan else plan.copy(variants = SongShape.shape(style, plan).scenes.drop(plan.count))

    /** The song the assistant makes for [style] with [plan]: its sections and their variants ([SongShape]). */
    fun songOf(style: Style, plan: ScenePlan): List<SongPart> {
        val full = withVariants(style, plan)
        return SongShape.shape(style, full).parts({ sceneBars(style, full, it) })
    }

    /** How long [style]'s song with [plan] lasts, in seconds. */
    fun songSeconds(style: Style, plan: ScenePlan, beatsPerBar: Int = 4): Double {
        val full = withVariants(style, plan)
        return songOf(style, full).sumOf { it.repeats * sceneBars(style, full, it.scene) } * beatsPerBar * 60.0 / style.bpm
    }

    /** The longest song the assistant makes for [style]: 6 minutes in techno and bass music, else [MAX_SONG_SECONDS]. */
    fun maxSongSeconds(style: Style): Double = SongShape.maxSeconds(SongShape.isLong(style))

    /** The names of the song's sections in order ("Intro", "Groove", "Montée", "Drop"…), translated: a variant shows as its section. */
    fun songSectionTitles(style: Style, plan: ScenePlan): List<String> {
        val full = withVariants(style, plan)
        val stem = Regex("""^(.+) \d+$""")
        val labels = full.sceneLabels
        val out = ArrayList<String>()
        var last: String? = null
        for (part in songOf(style, full)) {
            val label = labels.getOrNull(part.scene) ?: continue
            val section = stem.matchEntire(label)?.groupValues?.get(1)?.takeIf { it in labels } ?: label
            if (section != last) out += full.title(labels.indexOf(section))
            last = section
        }
        return out
    }

    /** How long [song] lasts, in seconds, with scenes of [loopBars] bars. */
    fun seconds(song: List<SongPart>, loopBars: Int, bpm: Double, beatsPerBar: Int = 4): Double =
        song.sumOf { it.repeats } * loopBars * beatsPerBar * 60.0 / bpm

    /** Steps of half a phrase: a Montée scene, a fake drop, a Post-refrain. */
    private const val HALF_STEPS = SongShape.HALF_BARS * 16

    /** The tracks a scene's content is shared out to: each part's, and the song's own layers. */
    private class SongTracks(
        val partOfTrack: Map<Int, Part>,
        val riser: Int? = null,
        val counter: Int? = null,
        val subDrop: Int? = null,
        val vocal: Int? = null,
        val vocalNotes: NoteClip? = null,
        val lead: Pair<Int, Map<Version, NoteClip>>? = null,
        /** The crash on the Drop's first beat: the kit's cymbal pad, when no line takes it (null: none). */
        val crashLane: Int? = null,
        /** The bass line the 808 plays on its pad (S3), null when the bass has a track of its own. */
        val bass808: NoteClip? = null,
        /** How the style's drummer plays ([DrumHumanizer]), from [seed]. */
        val feel: DrumHumanizer.Feel = DrumHumanizer.Feel(),
        val seed: Long = 0L,
        /** The 303 plays: its beat is written out over the 4-bar phrase too (a fill at its end). */
        val acid: Boolean = false,
    )

    /**
     * [clip] as scene [spec] plays it: half of its phrase (written out over 4 bars), the bass out of
     * its last [SongShape.SceneSpec.cutBars] bars ([bass]), nothing past its last beat
     * ([SongShape.SceneSpec.silence]), the chords softer in a Montée ([chords]).
     */
    private fun windowed(clip: NoteClip, spec: SongShape.SceneSpec, bass: Boolean, chords: Boolean): NoteClip {
        val t = NoteClip.TICKS_PER_STEP
        var notes = clip.notes
        var steps = clip.steps
        if (spec.half >= 0) {
            // Written out over the phrase, then its first or second half.
            val loop = clip.steps.coerceAtLeast(1) * t
            val tiled = if (clip.steps >= PHRASE_STEPS) notes
            else (0 until PHRASE_STEPS * t step loop).flatMap { off -> notes.map { it.copy(start = it.start + off) } }
            val from = spec.half * HALF_STEPS * t
            notes = tiled.filter { it.start in from until from + HALF_STEPS * t }.map { it.copy(start = it.start - from) }
            steps = HALF_STEPS
        }
        val end = steps * t
        fun cutAt(list: List<Note>, at: Int) = list.filter { it.start < at }.map { if (it.end > at) it.copy(length = (at - it.start).coerceAtLeast(1)) else it }
        if (bass && spec.cutBars > 0) notes = cutAt(notes, end - spec.cutBars * 16 * t)
        if (spec.silence) notes = cutAt(notes, end - 4 * t)
        if (chords && spec.capped) notes = notes.map { it.copy(velocity = (it.velocity * MONTEE_CHORDS).coerceAtLeast(0.05f)) }
        return clip.copy(steps = steps, notes = notes, hidden = emptyList())
    }

    /**
     * The hook's version a scene plays: its own ([SongShape.SceneSpec.version]), except a rap song's
     * Intro without its kick (trap, drill, phonk): the whole hook there, as the hits open on their loop
     * (Mask Off, FE!N), not its first phrase then 4 bars of hats alone (S5b: the bench found those
     * intros 10 to 15 LU under their Refrain).
     */
    private fun leadVersion(spec: SongShape.SceneSpec, form: SongForm): Version =
        if (spec.version == Version.Intro && form == SongForm.Rap && Role.Kick !in spec.content.lines) Version.Chorus else spec.version

    /** The chords' velocity in a Montée: held under the Drop (the bench found Montées louder than their Drop). */
    private const val MONTEE_CHORDS = 0.75f

    /** [a] as scene [spec] plays it: half of it written out over 4 bars, its last bars cut with the bass, its last beat silent. */
    private fun acidWindowed(a: AcidPattern, spec: SongShape.SceneSpec): AcidPattern {
        if (spec.half < 0 && spec.cutBars == 0 && !spec.silence) return a
        val len = a.length.coerceAtLeast(1)
        val steps = if (spec.half >= 0) HALF_STEPS else maxOf(len, HALF_STEPS).coerceAtMost(AcidPattern.MAX_STEPS)
        val from = spec.half.coerceAtLeast(0) * HALF_STEPS
        // The 303 carries an acid build: it plays on with the kick and the bass out (SongShape.ACID_CUT_BARS).
        val cut = steps - minOf(spec.cutBars, SongShape.ACID_CUT_BARS) * 16
        val out = List(AcidPattern.MAX_STEPS) { i ->
            if (i >= steps) AcidStep()
            else a.steps[(from + i) % len].let { st -> if (i >= cut || (spec.silence && i >= steps - 4)) st.copy(gate = false) else st }
        }
        return a.copy(length = steps, steps = out)
    }

    /**
     * The 8-bar roll of a Montée, in two halves of 4 bars ([half] 0 or 1): quarter notes for 2 bars,
     * eighths for 2, sixteenths for 2, rolls of 2 hits for a bar, then 3 and 4 hits; louder and
     * louder but held under the Drop (at most [ROLL_TOP]); the last beat silent before the Drop.
     * A step of several hits is played lighter, so that it weighs what a single hit of its strength
     * would ([rollPower]): the roll's last bars get faster, not louder (S5b: its 3 and 4 hits a step
     * at full strength made the Montée's end louder than the Drop, the limiter leaving them whole).
     * [top]: its loudest hit ([rollTop] of the style's backbeat), the rest scaled with it.
     */
    fun rollHalf(half: Int, top: Float = ROLL_TOP): Map<Int, Hit> {
        val scale = (top / ROLL_TOP).coerceIn(0f, 1f)
        val out = LinkedHashMap<Int, Hit>()
        for (i in 0 until HALF_STEPS) {
            val bar = half * 4 + i / 16
            val every = when {
                bar < 2 -> 4
                bar < 4 -> 2
                else -> 1
            }
            if (i % every != 0 || (half == 1 && i >= HALF_STEPS - 4)) continue
            val repeats = when {
                bar < 6 -> 1
                bar == 6 -> 2
                i % 16 < 8 -> 3
                else -> 4
            }
            val at = (bar * 16 + i % 16) / (2f * HALF_STEPS - 1)
            // A roll of several hits rises inside its step too (the engine's ramp).
            val velocity = (ROLL_LOW + (ROLL_TOP - ROLL_LOW) * at) * scale / kotlin.math.sqrt(rollPower(repeats))
            out[i] = Hit(velocity, repeats = repeats, ramp = if (repeats > 1) 1 else 0)
        }
        return out
    }

    /**
     * The roll's loudest hit for a roll line playing [hits] over [steps] in the Drop (its backbeat):
     * at its top a bar of the roll (sixteenths) weighs [ROLL_WEIGHT] times a bar of that backbeat, at
     * most [ROLL_TOP], at least [ROLL_TOP_MIN]. A half-time snare (riddim: one a bar) gets a lighter
     * roll than a clap on 2 and 4: S5b, the roll is the Montée's tension, not louder than the Drop.
     */
    fun rollTop(hits: Map<Int, Hit>, steps: Int): Float {
        if (hits.isEmpty() || steps <= 0) return ROLL_TOP
        val perBar = hits.values.sumOf { (it.velocity * it.velocity * rollPower(it.repeats)).toDouble() } * 16.0 / steps
        return kotlin.math.sqrt(ROLL_WEIGHT * perBar / 16.0).toFloat().coerceIn(ROLL_TOP_MIN, ROLL_TOP)
    }

    /** A bar of the roll at its top against a bar of the Drop's backbeat (power): +3 dB. */
    const val ROLL_WEIGHT = 2.0
    const val ROLL_TOP_MIN = 0.3f

    /**
     * The power of a step of [repeats] hits against a single one of the same velocity: the engine
     * plays them rising from half the velocity to all of it (its ramp, Hit.ramp 1).
     */
    fun rollPower(repeats: Int): Float {
        if (repeats <= 1) return 1f
        var sum = 0.0
        for (k in 0 until repeats) {
            val v = 1.0 - 0.5 * (1.0 - k.toDouble() / (repeats - 1))
            sum += v * v
        }
        return sum.toFloat()
    }

    /**
     * The roll's first and loudest hits: it builds (faster and faster), but the Drop's backbeat
     * stays louder (S5b: 0.7 before; its 3 and 4 hits a step at that strength made the Montée's end
     * louder than the Drop).
     */
    const val ROLL_LOW = 0.25f
    const val ROLL_TOP = 0.6f

    /** A song's one-bar roll before its Refrain (on [steps] of 4 bars): sixteenths, then rolls of 2, crescendo, the last beat silent. */
    private fun lastBarRoll(steps: Int): Map<Int, Hit> = (steps - 16 until steps - 4).associateWith { i ->
        val k = i - (steps - 16)
        Hit(0.4f + 0.3f * k / 11f, repeats = if (k >= 8) 2 else 1, ramp = if (k >= 8) 1 else 0)
    }

    /** [full] cut in the scenes of [shape] (the plan's sections, then their variants; Intro current), named, and a song that chains them. */
    private fun inScenes(
        full: Project, plan: ScenePlan, shape: SongShape.Shape, style: Style, tracks: SongTracks,
        builds: Map<Role, Map<Int, Hit>> = emptyMap(), montee: Map<Role, Map<Int, Hit>>? = null,
    ): Project {
        val partOfTrack = tracks.partOfTrack
        // A scene's length when nothing else sets it: the beat, or the eight bars of the melodic parts.
        val loopSteps = maxOf(full.drums.steps, partOfTrack.keys.maxOfOrNull { full.track(it).notes.steps } ?: 0)
        // The beat is written out over 4 bars (a fill at the end of each phrase) where the melody already lasts them.
        val phrases = (loopSteps >= WizardMix.PHRASE_STEPS || tracks.bass808 != null) && full.drums.lanes.any { it.hits.isNotEmpty() }
        val introBars = if (plan.form.sung) minOf(loopSteps / 16, introBarsOf(style, plan)) else loopSteps / 16
        val bassTracks = partOfTrack.filterValues { it == Part.Bass }.keys
        val club = !plan.form.sung
        fun view(index: Int, spec: SongShape.SceneSpec): Project {
            val c = spec.content
            // The Montée of a Build song has its own lines over its 4 bars, the roll included.
            val built = montee?.takeIf { plan.form == SongForm.Build && spec.base == 1 }
            var steps = if (built != null) BUILD_STEPS else full.drums.steps
            var lanes = full.drums.lanes.mapIndexed { lane, l ->
                val role = Role.entries.firstOrNull { it.lane == lane }
                when {
                    role == null || role !in c.lines -> l.copy(hits = emptyMap())
                    built != null -> l.copy(hits = built[role].orEmpty())
                    // The second section: a line that builds up plays its build pattern there.
                    spec.base == 1 && role in builds -> l.copy(hits = builds.getValue(role))
                    else -> l
                }
            }
            // A section of a song with a melody (and every half scene): its beat written out over the 4-bar phrase, ending on a fill.
            val with808 = tracks.bass808 != null && Part.Bass in c.parts
            if (built == null && (phrases || spec.half >= 0 || with808) && (lanes.any { it.hits.isNotEmpty() } || with808)) {
                lanes = WizardMix.phrase(lanes, steps, plan.form)
                steps = WizardMix.PHRASE_STEPS
            }
            fun set(role: Role, hits: Map<Int, Hit>) {
                lanes = lanes.toMutableList().also { it[role.lane] = it[role.lane].copy(hits = hits) }
            }
            // The 808 plays the bass line of the section on its pad (cut and silenced with the bass), lifted in the Drop.
            if (with808) tracks.bass808?.let { clip ->
                val voice = full.padVoices[StudioDrums.LANE_808]
                val tune = voice?.param(PadVoice.TUNE)?.toInt() ?: StudioDrums.tune808(full.harmony.root)
                val glide = voice?.param(PadVoice.GLIDE)?.let { Math.round(it * 1000f) } ?: StudioDrums.glideMs(style)
                val level = if (spec.base == 2) 1f else StudioDrums.OUTSIDE_DROP
                set(Role.Bass808, StudioDrums.hits808(windowed(clip, spec, bass = true, chords = false), steps, tune, glide, level, style.bpm.toFloat()))
            }
            // The roll: the snare (or the clap) over the Montée's 8 bars; a song's on its last bar.
            val rollRole = listOf(Role.Snare, Role.Clap).firstOrNull { it in c.lines } ?: Role.Hat.takeIf { it in c.lines && !club }
            // Its top follows the style's backbeat (the line as the Drop plays it).
            val top by lazy { rollRole?.let { rollTop(full.drums.lanes[it.lane].hits, full.drums.steps) } ?: ROLL_TOP }
            if (rollRole != null) when (spec.roll) {
                1 -> if (built == null) set(rollRole, rollHalf(0, top))
                2 -> set(rollRole, rollHalf(1, top))
                3 -> set(rollRole, lanes[rollRole.lane].hits.filterKeys { it < steps - 16 } + lastBarRoll(steps))
            }
            // The kick and the bass of the beat out of the last bars before a Drop; the last beat silent.
            if (spec.cutBars > 0) for (r in SongShape.BASS_LINES) set(r, lanes[r.lane].hits.filterKeys { it < steps - spec.cutBars * 16 })
            if (spec.silence) lanes = lanes.map { l -> l.copy(hits = l.hits.filterKeys { it < steps - 4 }) }
            // G0: the last Drop's scene has a drum layer more (an open hat, a ride, a percussion or claps).
            if (spec.extra && built == null) lanes = withDropLayer(lanes, steps, full.stepsPerBar)
            // The drummer: a « B » beat in the Drop's second scene, the phrase's own ending (3 or 4 of them
            // turn through the scenes), each hit played by hand. A Montée's roll and its silence stay as written.
            val seed = tracks.seed + 7919L * (index + 1)
            if (built == null && steps >= WizardMix.PHRASE_STEPS && lanes.any { it.hits.isNotEmpty() }) {
                if (spec.version == Version.Chorus2) lanes = DrumHumanizer.variantB(lanes, steps, seed)
                if (spec.roll == 0 && !spec.silence && spec.cutBars == 0) {
                    lanes = DrumHumanizer.ending(lanes, steps, DrumHumanizer.endingOf(tracks.feel, tracks.seed, index), sung = plan.form.sung)
                }
            }
            val rolling = setOfNotNull(rollRole?.lane?.takeIf { spec.roll != 0 })
            lanes = DrumHumanizer.humanize(lanes, steps, tracks.feel, seed, full.tempoBpm, style.swing, keep = rolling, until = if (spec.silence) steps - 4 else steps)
            // The crash on the 1 of each 16 is the song's (DrumArrangement): one bar of its own, never every loop.
            val drums = full.drums.copy(steps = steps, lanes = lanes)
            val acid = if (c.acid) acidWindowed(full.acid, spec) else full.acid.copy(steps = full.acid.steps.map { it.copy(gate = false) })
            var p = full.copy(drums = drums, acid = acid)
            for ((id, part) in partOfTrack) {
                var clip = p.track(id).notes
                if (part !in c.parts) clip = clip.copy(notes = emptyList())
                // The hook's version for the section: a preview, a thinned-out line, the whole thing, doubled in the second Drop.
                else if (tracks.lead != null && id == tracks.lead.first) tracks.lead.second[leadVersion(spec, plan.form)]?.let { clip = it }
                clip = windowed(clip, spec, bass = id in bassTracks, chords = part == Part.Pad)
                p = p.withTrack(p.track(id).copy(notes = clip))
            }
            val sceneSteps = if (built != null || spec.half >= 0) HALF_STEPS else loopSteps
            tracks.counter?.let { id ->
                val clip = windowed(p.track(id).notes, spec, false, false).let { if (spec.counter) it else it.copy(notes = emptyList()) }
                p = p.withTrack(p.track(id).copy(notes = clip))
            }
            tracks.vocal?.let { id ->
                val clip = tracks.vocalNotes?.takeIf { spec.vocal }?.let { windowed(it, spec, false, false) } ?: NoteClip(steps = minOf(sceneSteps, PHRASE_STEPS))
                p = p.withTrack(p.track(id).copy(notes = if (spec.vocal) clip else clip.copy(notes = emptyList())))
            }
            tracks.riser?.let { id -> p = p.withTrack(p.track(id).copy(notes = riserNotes(spec, sceneSteps))) }
            tracks.subDrop?.let { id -> p = p.withTrack(p.track(id).copy(notes = subDropNotes(spec, sceneSteps, full.harmony))) }
            // A song's short Intro (4 bars when the song would run long) and the Montée (its 4-bar roll): their clips cut to them.
            val bars = if (spec.base == 0 && spec.half < 0) introBars else if (built != null) BUILD_STEPS / 16 else sceneSteps / 16
            if (bars * 16 < loopSteps && spec.half < 0) for (id in partOfTrack.keys + listOfNotNull(tracks.counter, tracks.vocal, tracks.riser, tracks.subDrop)) {
                val t = p.track(id)
                val ticks = bars * 16 * NoteClip.TICKS_PER_STEP
                p = p.withTrack(t.copy(notes = t.notes.copy(steps = bars * 16, notes = t.notes.notes.filter { it.start < ticks }, hidden = emptyList())))
            }
            return p
        }
        val views = shape.scenes.mapIndexed { i, spec -> view(i, spec) }
        var project = views[0].copy(
            scenes = views.mapIndexed { i, v -> v.currentSceneContent().copy(name = Scene.NAMES[i]) },
            scene = 0,
        )
        val bars = project.allScenes.map { (it.stepsFor(project.stepsPerBar) / project.stepsPerBar).coerceAtLeast(1) }
        // A block's muted layers: the tracks that play them (G0: the lean opening of a Drop, a Drop that breathes).
        fun keysOf(layer: SongShape.Layer): Set<String> = when (layer) {
            SongShape.Layer.Pad -> partOfTrack.filterValues { it == Part.Pad }.keys
            SongShape.Layer.Lead -> partOfTrack.filterValues { it == Part.Lead }.keys
            SongShape.Layer.Counter -> setOfNotNull(tracks.counter)
            SongShape.Layer.Vocal -> setOfNotNull(tracks.vocal)
        }.map { SongPart.inst(it) }.toSet()
        project = project.copy(song = shape.parts({ bars.getOrElse(it) { 1 } }, ::keysOf))
        // Section names are saved in French and shown in the interface's language.
        shape.scenes.forEachIndexed { i, spec -> project = project.renameScene(i, spec.label) }
        return project
    }

    /**
     * G0: the drum layer the last Drop adds ([SongShape.SceneSpec.extra]), on the first of these lines
     * the scene leaves empty: the open hat on the off-beats, a percussion on the last sixteenth of each
     * beat, else Énergie 4's hats and claps. Never the cymbal's pad (the crash of the kits without a
     * ride: the song's, once every 16). Nothing over the phrase's last beat (its fill). [lanes]: a
     * scene's beat of [steps] steps, bars of [stepsPerBar].
     */
    internal fun withDropLayer(lanes: List<DrumLane>, steps: Int, stepsPerBar: Int = 16): List<DrumLane> {
        if (lanes.all { it.hits.isEmpty() }) return lanes
        val until = if (steps >= WizardMix.PHRASE_STEPS) steps - 4 else steps
        val options = listOf(
            Role.OpenHat to (0 until until).filter { it % 4 == 2 }.associateWith { Hit(0.5f) },
            Role.Perc2 to (0 until until).filter { it % 4 == 3 }.associateWith { Hit(0.45f) },
        )
        val (role, hits) = options.firstOrNull { (r, _) -> lanes.getOrNull(r.lane)?.hits?.isEmpty() == true } ?: return PartIntents.withExtraLayer(DrumPattern(steps = steps, lanes = lanes), stepsPerBar).lanes
        return lanes.toMutableList().also { it[role.lane] = it[role.lane].copy(hits = hits) }
    }

    /** The crash's strength on the Drop's first beat. */
    internal const val CRASH_VELOCITY = 0.9f

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
                "piano" in name || "rhodes" in name -> "Piano"
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
    fun startingNotes(part: Part, style: Style, mood: Mood, harmony: Harmony, sound: PartSound? = null, version: Version = Version.Chorus): NoteClip {
        val m = style.melody
        val degrees = progressionOf(style, mood).second.let { it + it }
        return when (part) {
            Part.Pad -> {
                val chord = sound?.chord == true
                val voicing = if (chord) Voicing.Single else m.voicing
                val arp = m.chordArp?.takeIf { !chord }
                NoteClip(performance = arp?.performance() ?: Performance()).withChordRhythm(
                    degrees, harmony, if (arp != null) "X---------------" else m.chordRhythm, if (arp != null && voicing == Voicing.Triad) Voicing.Seventh else voicing,
                    12 * (m.chordOctave + 1), velocity = 0.75f, voiceLead = m.chordVoiceLead && !chord, seed = MelodyBuilder.seed(style, mood, "pad"),
                )
            }
            // A written lead: the builder's phrase, already in range (its versions are cut from the same notes).
            Part.Lead -> if (m.leadPattern.isNotBlank() && m.leadKind != LeadKind.Arp) MelodyBuilder.lead(style, mood, harmony, version, mono = sound?.chord == true) else inLeadRange(
                when {
                    // A sound recorded as a chord (a stab) arpeggiated would play chords of chords: it hits the chords' roots instead.
                    sound?.chord == true -> NoteClip().withChordRhythm(
                        degrees, harmony, if (isStabs(m.chordRhythm)) m.chordRhythm else LEAD_STABS, Voicing.Single, 12 * (m.leadOctave + 1), velocity = 0.7f,
                    )
                    // An arpeggio: a fixed pattern over two octaves with accents and a gate, never a triad going up and down.
                    else -> NoteClip(performance = (m.leadArp ?: ArpSpec(DEFAULT_ARP, rate = m.arpRate.coerceIn(12, 48))).performance())
                        .withChordRhythm(degrees, harmony, "X---------------", Voicing.Seventh, 12 * (m.leadOctave + 1), velocity = 0.65f)
                },
            )
            Part.Bass -> MelodyBuilder.bass(style, mood, harmony).let { clip -> BassPatterns.of(style)?.let { BassPatterns.played(clip, it) } ?: clip }
        }
    }

    /** The arpeggio a style without a written lead gets when its lead is ticked: root, fifth, octave, third, up and back. */
    val DEFAULT_ARP = listOf(0, 2, 4, 5, 6, 5, 4, 2)

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
