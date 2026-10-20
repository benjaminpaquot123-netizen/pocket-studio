package com.pocketstudio.model

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
            "Elle marque le contre-temps (les temps 2 et 4) : elle répond au kick et donne l'élan.",
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
     * '.' nothing. [tip]: why the line sits where it does in this style.
     */
    data class Line(val role: Role, val pattern: String, val tip: String)

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
    ) {
        /** Packs that make this style sound right: its machines, its melodic sources, its own pack. */
        val packs: List<String> get() = (kit.values + melody.packSounds.values.map { it.substringBefore(':') } + id).distinct()
    }

    /**
     * The style's melodic side: its usual key ([root] 0..11, 0 = Do), the mood proposed first,
     * the bass rhythm (one character per step, like the drum lines) and the synth sounds of
     * [SoundLibrary.SYNTH] proposed for each part.
     */
    data class Melody(
        val root: Int = 9,
        val mood: Mood = Mood.Dark,
        /** The bass line, one character per step on the bar's chord (see [withLine]: R root, 5 fifth, O octave…). */
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
        /** A pack sound per part when that pack is on the phone: "pack id:kind" (e.g. "retro_techno:stab"). */
        val packSounds: Map<Part, String> = emptyMap(),
        /** The parts a beginner gets switched on. */
        val parts: Set<Part> = setOf(Part.Bass, Part.Pad),
        /** A TB-303 line on the acid track, where it is the style's signature. */
        val acid: AcidRecipe? = null,
    ) {
        fun sound(part: Part) = when (part) {
            Part.Bass -> bass
            Part.Pad -> pad
            Part.Lead -> lead
        }
    }

    /** A 303 riff: [pattern] as in [withLine], [accents] and [slides] one 'x' per step that has one. */
    data class AcidRecipe(val pattern: String, val accents: String = "", val slides: String = "")

    /** The feeling of the song: it sets the scale and the chords, so every note sounds right. */
    enum class Mood(val id: String, val label: String, val description: String, val scale: Scale, val degrees: List<Int>, val chordsLabel: String) {
        Dark(
            "dark", "Mélancolique / sombre", "Des accords mineurs, un peu tristes ou mystérieux.",
            Scale.Minor, listOf(0, 5, 2, 6), "i – VI – III – VII",
        ),
        Happy(
            "happy", "Joyeux / énergique", "Des accords majeurs lumineux qui donnent la pêche.",
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
            "Des accords tenus qui remplissent l'espace et posent l'ambiance. Ils changent à chaque mesure.",
            listOf("Nappes", "Claviers"), true,
        ),
        Lead(
            "lead", "Lead / mélodie",
            "Un synthé plus aigu qui joue les notes des accords une par une, en rythme (un arpège) : un premier bout de mélodie.",
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
    }

    const val PACK_FILE = "pack:"

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
        // The style's own pack sound first when its pack is on the phone, else its synth sound.
        val packChoice = style.melody.packSounds[part]?.split(':')?.takeIf { it.size == 2 }
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
                val slide = sound.kind == "808"
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
        val preferred = preferredPack.ifEmpty { preferredSynth }
        return preferred + (all - preferred.toSet()) + (packs - preferred.toSet())
    }

    /** The assistant's steps for a style: style, mood, each drum line, each melodic part, summary. */
    sealed interface Step {
        data object Style : Step
        data object Mood : Step
        data class Drum(val line: Line) : Step
        data class Melodic(val part: Part) : Step
        data object Summary : Step
    }

    fun steps(style: Style?): List<Step> =
        if (style == null) listOf(Step.Style)
        else listOf(Step.Style, Step.Mood) + style.lines.map { Step.Drum(it) } + Part.entries.map { Step.Melodic(it) } + Step.Summary

    val FAMILIES = listOf("Hip-hop", "House", "Techno", "Bass music", "Autres")

    /** The styles, from the researched recipes (see StyleRecipes.kt and scripts/packs/recipes/). */
    val STYLES: List<Style> get() = STYLE_RECIPES

    fun style(id: String): Style? = STYLES.firstOrNull { it.id == id }

    /** The drum pattern's length for [style]: its longest line (two bars for breakbeats and rolls). */
    fun drumSteps(style: Style): Int = style.lines.maxOf { it.pattern.length }.coerceIn(DrumPattern.MIN_STEPS, DrumPattern.MAX_STEPS)

    /** [line]'s hits over [steps]: a one-bar line repeats under a two-bar one. */
    fun hits(line: Line, steps: Int): Map<Int, Hit> {
        val bar = hits(line.pattern)
        return (0 until steps step line.pattern.length).flatMap { start -> bar.map { (i, h) -> start + i to h } }
            .filter { it.first < steps }.toMap()
    }

    /** A pattern string as hits (steps without a hit are left out). */
    fun hits(pattern: String): Map<Int, Hit> = pattern.mapIndexedNotNull { i, c ->
        when (c) {
            'X' -> i to Hit(1f)
            'x' -> i to Hit(0.6f)
            'o' -> i to Hit(0.35f)
            else -> null
        }
    }.toMap()

    /**
     * The new project: the style's tempo and swing, each chosen line's sound on its pad
     * ([sounds]: role → the pad's own sound, or null for the kit voice already on that pad) and
     * its groove in the grid. Lines left out of [sounds] stay empty. With [mood], the key is set
     * and locked, and each part of [parts] becomes a track with its sound and first notes.
     */
    fun build(style: Style, sounds: Map<Role, PadSample?>, mood: Mood? = null, parts: Map<Part, PartSound> = emptyMap()): Project {
        val steps = drumSteps(style)
        val lanes = MutableList(DrumPattern.PAD_COUNT) { DrumLane() }
        for (line in style.lines) {
            if (line.role !in sounds) continue
            lanes[line.role.lane] = DrumLane(hits = hits(line, steps), sample = sounds[line.role])
        }
        var project = Project(
            name = style.name,
            tempoBpm = style.bpm,
            swing = style.swing,
            drums = DrumPattern(steps = steps, lanes = lanes),
        )
        if (mood != null) {
            val harmony = harmonyOf(style, mood)
            project = project.copy(harmony = harmony)
            style.melody.acid?.let { a -> project = project.copy(acid = acidLine(harmony, a.pattern, a.accents, a.slides, project.acid)) }
            var first = true
            for (part in Part.entries) {
                val sound = parts[part] ?: continue
                val notes = startingNotes(part, style, mood, harmony)
                fun InstrumentTrack.withSound() = copy(
                    name = part.trackName, notes = notes, synth = sound.synth,
                    engine = when {
                        sound.sampled != null -> 2
                        sound.plaits != null -> 1
                        else -> 0
                    },
                    plaits = sound.plaits ?: plaits, sampled = sound.sampled ?: sampled,
                )
                if (first) {
                    project = project.withTrack(project.track(0).withSound())
                    first = false
                } else {
                    val added = project.addTrack(part.trackName) ?: continue
                    project = added.withTrack(added.tracks.last().withSound())
                }
            }
        }
        return project.sanitized()
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
