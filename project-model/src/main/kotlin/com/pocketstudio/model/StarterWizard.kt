package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr

/**
 * The beginner's assistant for a new song: a style, then one step per drum line (what it does,
 * which sound), then a project with those sounds on the pads and a basic groove to start from.
 * It never writes the music: the grooves are plain textbook patterns the user then changes.
 */
object StarterWizard {
    /** A line of the beat: which pad it lands on, what it is for, which kit voices can play it. */
    enum class Role(val id: String, val lane: Int, val title: String, val explanation: String, val builtIn: List<Int>) {
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
    }

    /**
     * [pattern]: one character per step (16 = one bar of 4/4): 'X' strong, 'x' medium, 'o' soft,
     * '2' '3' '4' a medium roll of that many hits in the step, '.' nothing. [tip]: why the line sits
     * where it does in this style. [voice]: the built-in kit voice that plays it best (a shaker for
     * the hats, a cowbell or a conga for the percussion), null: the pad's own voice. [build]: the
     * line in the second section (Groove, Couplet), where it builds up ("" = [pattern] there too).
     */
    data class Line(val role: Role, val pattern: String, val tip: String, val voice: Int? = null, val build: String = "")

    /**
     * How the song is laid out. [Song]: Intro, Couplet, Refrain (rap, pop). [Club]: Intro, Groove,
     * Drop and a Break without kick nor bass before the last drop (house, techno, bass music), with
     * longer sections for the DJ. [Rave]: the 303 alone in the Intro (its filter opening), the kick
     * and the hats coming in on the Groove, then Drop, Break, Drop, a second Break, Drop and the
     * Intro again as the outro (acidcore). [labels]: the sections' names (saved in French), [roles]
     * what each is for.
     */
    enum class SongForm(val labels: List<String>, val roles: List<String>) {
        Song(listOf("Intro", "Couplet", "Refrain"), listOf("le début, en douceur", "le groove, avec la basse", "tout le monde joue")),
        Club(listOf("Intro", "Groove", "Drop"), listOf("la batterie pour démarrer", "le groove, avec la basse", "tout le monde joue")),
        Rave(listOf("Intro", "Groove", "Drop"), listOf("la 303 seule, son filtre s'ouvre", "le kick arrive, les charleys montent", "tout le monde joue")),
        ;

        companion object {
            const val BREAK_LABEL = "Break"
            const val BREAK_ROLE = "une pause sans kick ni basse"

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
        /** The 303's sound for this riff: [sound] with [knobs], keeping [base]'s tuning and volume. */
        fun soundFrom(base: AcidSound): AcidSound {
            val preset = SoundLibrary.ACID.firstOrNull { it.name == sound }?.sound?.copy(tuning = base.tuning, level = base.level) ?: base
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
     * project is written, then the copy in the project's folder).
     */
    data class PartSound(val name: String, val synth: SynthPatch = SynthPatch(), val plaits: PlaitsPatch? = null, val sampled: SampledPatch? = null) {
        /** The pack and file of a pack sound, or null. */
        val packFile: Pair<String, String>? get() = sampled?.file?.takeIf { it.startsWith(PACK_FILE) }
            ?.removePrefix(PACK_FILE)?.split('/')?.takeIf { it.size == 2 }?.let { it[0] to it[1] }

        /** The built-in kit voice played across the keys ("kit:<voice>" until the project is written), or null. */
        val kitVoice: Int? get() = sampled?.file?.takeIf { it.startsWith(KIT_SOUND) }?.removePrefix(KIT_SOUND)?.toIntOrNull()
            ?.takeIf { it in 0 until DrumPattern.PAD_COUNT }
    }

    const val PACK_FILE = "pack:"

    /** Kit voices that make a melodic sound, and their names (the 808 cowbell of phonk and Memphis rap). */
    val KIT_MELODIC = mapOf(11 to "Cowbell 808")

    /** The sound of kit voice [voice] played across the keys by [part]. */
    fun kitPartSound(voice: Int, part: Part) = PartSound(
        KIT_MELODIC[voice] ?: "Kit $voice",
        sampled = SampledPatch(file = KIT_SOUND + voice, name = KIT_MELODIC[voice] ?: "Kit $voice", release = if (part == Part.Pad) 0.4f else 0.15f),
    )

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
        val synth = SoundLibrary.SYNTH.filter { it.category in part.categories }.map { PartSound(it.name, synth = it.sound) }
        val plaitsNames = synth.map { it.name }.toSet()
        val plaits = SoundLibrary.PLAITS.filter { it.category in part.categories }
            .map { PartSound(if (it.name in plaitsNames) "${it.name} (Plaits)" else it.name, plaits = it.sound) }
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
            SoundLibrary.SYNTH.filter { it.name == wanted }.map { PartSound(it.name, synth = it.sound) } +
                SoundLibrary.PLAITS.filter { it.name == wanted }.map { PartSound(it.name, plaits = it.sound) }
        }.take(1)
        val kinds = packKinds(part)
        val packs = installed.sortedBy { if (it.id == chosenPack?.id) 0 else if (it.id == style.id) 1 else 2 }.flatMap { m ->
            val own = m.id == style.id || m.id == chosenPack?.id
            // The chosen kind first in the chosen pack (its stabs for a rave lead).
            val packKinds = if (m.id == chosenPack?.id) listOf(packChoice[1]) + (kinds - packChoice[1]) else kinds
            packKinds.flatMap { m.ofKind(it) }.take(if (own) Int.MAX_VALUE else 4).map { sound ->
                val slide = sound.kind == "808" || (part == Part.Bass && style.melody.bassGlide)
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

    val FAMILIES = listOf("Hip-hop", "House", "Techno", "Bass music", "Autres")

    /**
     * The styles, most known first (a beginner finds techno, house or trap at once, riddim and
     * jersey club further down), from the researched recipes (StyleRecipes.kt, scripts/packs/recipes/).
     */
    val STYLES: List<Style> by lazy { STYLE_RECIPES.sortedBy { s -> POPULAR.indexOf(s.id).let { if (it < 0) POPULAR.size else it } } }

    /** How well known each style is, from the most (charts, festivals, streaming) to the least. */
    val POPULAR = listOf(
        "techno", "house", "trap", "drill", "pop", "phonk", "dnb", "reggaeton", "afrobeats", "lofi",
        "dubstep", "disco", "trance", "deep_house", "tech_house", "boom_bap", "synthwave", "acid_house", "hard_techno",
        "amapiano", "uk_garage", "minimal", "afro_house", "jungle", "retro_techno", "acid_techno",
        "gabber", "acidcore", "liquid_dnb", "detroit", "grime", "industrial_techno", "neurofunk", "jersey_club",
        "speed_garage", "riddim",
    )

    /** The first styles of [STYLES], shown apart as the best known. */
    const val BEST_KNOWN = 10

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
                val notes = startingNotes(part, style, mood, harmony)
                // The bass glides where its line says '/' (its notes overlap there): the synth's portamento.
                val glides = part == Part.Bass && style.melody.bassGlide
                fun InstrumentTrack.withSound() = copy(
                    name = part.trackName, notes = notes, synth = if (glides) sound.synth.with(SynthPatch.GLIDE, BASS_GLIDE) else sound.synth,
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
                    val added = project.addTrack(part.trackName) ?: continue
                    project = added.withTrack(added.tracks.last().withSound())
                    partOfTrack[added.tracks.last().id] = part
                }
            }
        }
        val plan = planFor(style, style.lines.map { it.role }.filter { it in sounds }.toSet(), partOfTrack.values.toSet(), withAcid)
        project = withEffects(project, style, sounds.keys, partOfTrack, withAcid)
        // The lines that build up in the second section play their build pattern there.
        val builds = style.lines.filter { it.build.isNotEmpty() && it.role in sounds }.associate { it.role to hits(it.copy(pattern = it.build), steps) }
        project = inScenes(project, plan, partOfTrack, style.bpm, builds)
        if (withAcid) project = withAcidMoves(project, style.melody.acid?.moves.orEmpty())
        return project.sanitized()
    }

    /**
     * [moves] as song automation curves of the 303's knobs: each section of the song from its start
     * value to its end value (a section the move leaves out holds the value before it), the outro
     * (the Intro's scene again, last) from the Intro's end value back to its start.
     */
    internal fun withAcidMoves(project: Project, moves: List<AcidMove>): Project {
        if (moves.isEmpty() || project.song.isEmpty()) return project
        // The sections: consecutive parts of one scene, with their first bar and their length.
        data class Section(val scene: Int, val start: Int, val bars: Int)
        val sections = ArrayList<Section>()
        var bar = 0
        for (part in project.song) {
            val bars = project.sceneSteps(part.scene) * part.repeats / project.stepsPerBar
            val last = sections.lastOrNull()
            if (last != null && last.scene == part.scene) sections[sections.size - 1] = last.copy(bars = last.bars + bars)
            else sections += Section(part.scene, bar, bars)
            bar += bars
        }
        val lanes = moves.filter { it.param in AutoTarget.ACID_PARAMS }.distinctBy { it.param }.map { move ->
            var lane = AutomationLane(AutoTarget.acid(move.param), mode = AutomationLane.SONG)
            sections.forEachIndexed { i, sec ->
                val (from, to) = move.sections[sec.scene] ?: return@forEachIndexed
                val outro = i > 0 && i == sections.lastIndex && sec.scene == sections.first().scene
                val (a, b) = if (outro) to to from else from to to
                lane = lane.withSongPoint(sec.start, a).withSongPoint(sec.start + sec.bars - 1, b)
            }
            lane
        }.filter { it.songValues.isNotEmpty() }
        val kept = project.automation.filterNot { l -> lanes.any { it.target == l.target } }
        return project.copy(automation = (kept + lanes).take(AutomationLane.MAX_LANES))
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
    ) {
        /** How many sections: three, or four with the Break. */
        val count: Int get() = if (hasBreak) 4 else 3

        /** The sections' names, saved in French ("Intro", "Couplet", "Refrain" or "Intro", "Groove", "Drop", "Break"). */
        val labels: List<String> get() = form.labels + listOfNotNull(SongForm.BREAK_LABEL.takeIf { hasBreak })

        /** The section scene [index] stands for, in the interface's language. */
        fun title(index: Int): String = tr(labels[index])

        /** What the section is for, in a few words (translated). */
        fun role(index: Int): String = tr((form.roles + SongForm.BREAK_ROLE)[index])

        /** The names of what scene [index] plays, in the order of the assistant (translated). */
        fun contents(index: Int): List<String> =
            lines.filterValues { index in it }.keys.sortedBy { it.ordinal }.map { tr(it.title) } +
                parts.filterValues { index in it }.keys.sortedBy { it.ordinal }.map { tr(it.title) } +
                listOfNotNull(tr(ACID_TITLE).takeIf { index in acid })

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
        }
    }

    /** Light drum lines: the intro starts with them alone. */
    private val INTRO_LINES = setOf(Role.Kick, Role.Hat)

    /** A club intro: the beat a DJ mixes on, kick, hats and percussion. */
    private val CLUB_INTRO_LINES = setOf(Role.Kick, Role.Hat, Role.Perc)

    /** Drum lines kept for the chorus (they lift it). */
    private val CHORUS_LINES = setOf(Role.OpenHat, Role.Perc)

    /** The parts a beginner finds switched on for [style]: the recipe's (the riddim's bass alone, the acid styles' 303 alone). */
    fun defaultParts(style: Style): Set<Part> = style.melody.parts

    /** The plan of [style]'s song with these lines, parts and acid line. */
    fun planFor(style: Style, lines: Set<Role>, parts: Set<Part>, acid: Boolean): ScenePlan =
        scenePlan(lines, parts, acid, style.melody.signature, style.form)

    /**
     * Shares the [lines] and [parts] used (and the acid line, when there is one) between the
     * sections. The last of the three (Refrain, Drop): everything. The middle one (Couplet,
     * Groove): everything but the chorus's own layer, the lead (or the chords without a lead, never
     * the [signature]), and in a song the open hat and the percussion. The first (Intro): in a song
     * the [signature] and the hats, or the kick, the hats and the chords; in club music the kick,
     * the hats, the percussion and the signature; never the bass. The [signature] plays in every
     * section. A section that would be silent plays what the next one plays. Club music also gets a
     * Break (scene D): the chords, the lead and the signature, without kick nor bass (the acid line,
     * or the hats and the percussion, when there is no chord nor lead).
     */
    fun scenePlan(lines: Set<Role>, parts: Set<Part>, acid: Boolean, signature: Part? = null, form: SongForm = SongForm.Song): ScenePlan {
        if (form == SongForm.Rave) return ravePlan(lines, parts, acid, signature)
        val bassLines = setOf(Role.Bass808)
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
        var acidScenes = if (!acid) emptySet() else if (introSilent) setOf(0, 1, 2) else setOf(1, 2)
        if (form == SongForm.Club) {
            val breakParts = parts intersect (setOf(Part.Pad, Part.Lead) + listOfNotNull(sig) - Part.Bass)
            val breakAcid = acid && breakParts.isEmpty()
            val breakLines = if (breakParts.isEmpty() && !breakAcid) lines intersect setOf(Role.Hat, Role.OpenHat, Role.Perc) else emptySet()
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
        val breakLines = if (acid || breakParts.isNotEmpty()) emptySet() else lines intersect setOf(Role.Hat, Role.OpenHat, Role.Perc)
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

    /** Longest song the assistant makes, with its intro and ending shortened if need be: 3 min 30. */
    const val MAX_SONG_SECONDS = 210.0

    /**
     * The song of [form]'s sections, each one (bars, scene):
     * a song is Intro, Couplet, Refrain, Couplet, Refrain and the Intro again as the ending, the
     * Couplets and Refrains 16 bars, the Intro 8 (4 when that would last over [MAX_SONG_SECONDS]);
     * club music is Intro, Groove, Drop, Break (8 bars; a Groove without one), Drop and the Intro
     * again, the Intro 16 bars (8 when too long) and the Drops 32 when a fast tempo leaves room.
     */
    fun sectionsOf(bpm: Double, form: SongForm = SongForm.Song, hasBreak: Boolean = false, beatsPerBar: Int = 4): List<Pair<Int, Int>> {
        fun seconds(list: List<Pair<Int, Int>>) = list.sumOf { it.first } * beatsPerBar * 60.0 / bpm
        val s = SECTION_BARS
        // The rave's second Break (8 bars), or a Groove when there is no Break.
        val brk = if (hasBreak) 3 else 1
        val candidates = if (form == SongForm.Song) listOf(8, 4).map { intro -> listOf(intro to 0, s to 1, s to 2, s to 1, s to 2, intro to 0) }
        else if (form == SongForm.Rave) listOf(
            // 303 alone, kick and hats, Drop, Break with its filter sweep, Drop, a short Break, Drop, outro.
            listOf(s to 0, s to 1, 2 * s to 2, s to brk, 2 * s to 2, BREAK_BARS to brk, 3 * s / 2 to 2, s to 0),
            listOf(BREAK_BARS to 0, s to 1, s to 2, BREAK_BARS to brk, s to 2, BREAK_BARS to brk, s to 2, BREAK_BARS to 0),
        )
        else listOf(2 * s to s, s to s, s to s / 2).map { (drop, intro) ->
            listOf(intro to 0, s to 1, drop to 2, (if (hasBreak) BREAK_BARS to 3 else s to 1), drop to 2, intro to 0)
        }
        return candidates.firstOrNull { seconds(it) <= MAX_SONG_SECONDS } ?: candidates.last()
    }

    /**
     * The song as parts of the scenes, each scene of [loopBars] bars repeated to fill its section
     * (a section longer than [SongPart.MAX_REPEATS] loops is cut in two parts of the same scene).
     */
    fun songOf(loopBars: Int, bpm: Double, beatsPerBar: Int = 4, form: SongForm = SongForm.Song, hasBreak: Boolean = false): List<SongPart> {
        val bars = loopBars.coerceAtLeast(1)
        return sectionsOf(bpm, form, hasBreak, beatsPerBar).flatMap { (length, scene) ->
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

    /** The song the assistant makes for [style] with [plan]. */
    fun songOf(style: Style, plan: ScenePlan): List<SongPart> = songOf(loopBars(style, plan), style.bpm, form = plan.form, hasBreak = plan.hasBreak)

    /** How long [song] lasts, in seconds, with scenes of [loopBars] bars. */
    fun seconds(song: List<SongPart>, loopBars: Int, bpm: Double, beatsPerBar: Int = 4): Double =
        song.sumOf { it.repeats } * loopBars * beatsPerBar * 60.0 / bpm

    /** [full] cut in the sections of [plan] (Intro current), named, and a song that chains them. */
    private fun inScenes(full: Project, plan: ScenePlan, partOfTrack: Map<Int, Part>, bpm: Double, builds: Map<Role, Map<Int, Hit>> = emptyMap()): Project {
        fun only(index: Int): Project {
            val drums = full.drums.copy(lanes = full.drums.lanes.mapIndexed { lane, l ->
                val role = Role.entries.firstOrNull { it.lane == lane }
                when {
                    role == null || index !in plan.lines[role].orEmpty() -> l.copy(hits = emptyMap())
                    // The second section: a line that builds up plays its build pattern there.
                    index == 1 && role in builds -> l.copy(hits = builds.getValue(role))
                    else -> l
                }
            })
            val acid = if (index in plan.acid) full.acid else full.acid.copy(steps = full.acid.steps.map { it.copy(gate = false) })
            var p = full.copy(drums = drums, acid = acid)
            for ((id, part) in partOfTrack) {
                if (index in plan.parts[part].orEmpty()) continue
                val t = p.track(id)
                p = p.withTrack(t.copy(notes = t.notes.copy(notes = emptyList())))
            }
            return p
        }
        val views = (0 until plan.count).map { only(it) }
        var project = views[0].copy(
            scenes = views.mapIndexed { i, v -> v.currentSceneContent().copy(name = ScenePlan.NAMES[i]) },
            scene = 0,
        )
        val loopBars = project.allScenes.maxOf { it.stepsFor(project.stepsPerBar) } / project.stepsPerBar
        project = project.copy(song = songOf(loopBars, bpm, project.beatsPerBar, plan.form, plan.hasBreak))
        // Section names are saved in French and shown in the interface's language.
        plan.labels.forEachIndexed { i, label -> project = project.renameScene(i, label) }
        return project
    }

    private val Part.trackName get() = when (this) {
        Part.Bass -> "Basse"
        Part.Pad -> "Nappe"
        Part.Lead -> "Lead"
    }

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
     * (or an arpeggio of the chords an octave up).
     */
    fun startingNotes(part: Part, style: Style, mood: Mood, harmony: Harmony): NoteClip {
        val m = style.melody
        val degrees = progressionOf(style, mood).second
        return when (part) {
            Part.Pad -> NoteClip().withChordRhythm(degrees, harmony, m.chordRhythm, m.voicing, 12 * (m.chordOctave + 1), velocity = 0.75f)
            Part.Lead -> if (m.leadPattern.isBlank()) {
                NoteClip(performance = Performance(arp = ArpMode.UpDown, arpRate = m.arpRate))
                    .withChordRhythm(degrees, harmony, "X---------------", Voicing.Triad, 12 * (m.leadOctave + 1), velocity = 0.65f)
            } else {
                NoteClip().withLine(degrees, harmony, m.leadPattern, 12 * (m.leadOctave + 1), velocity = 0.8f)
            }
            Part.Bass -> NoteClip().withLine(degrees, harmony, m.bassPattern, 12 * (m.bassOctave + 1))
        }
    }
}
