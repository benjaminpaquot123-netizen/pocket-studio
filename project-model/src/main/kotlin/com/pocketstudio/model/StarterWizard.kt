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
    )

    /**
     * The style's melodic side: its usual key ([root] 0..11, 0 = Do), the mood proposed first,
     * the bass rhythm (one character per step, like the drum lines) and the synth sounds of
     * [SoundLibrary.SYNTH] proposed for each part.
     */
    data class Melody(
        val root: Int = 9,
        val mood: Mood = Mood.Dark,
        val bassPattern: String = "X.......X.......",
        val bass: String = "Basse ronde",
        val pad: String = "Nappe",
        val lead: String = "Pluck",
    ) {
        fun sound(part: Part) = when (part) {
            Part.Bass -> bass
            Part.Pad -> pad
            Part.Lead -> lead
        }
    }

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
        val preferred = all.filter { it.name == style.melody.sound(part) }.take(1)
        val kinds = packKinds(part)
        val packs = installed.sortedBy { if (it.id == style.id) 0 else 1 }.flatMap { m ->
            m.sounds.filter { it.kind in kinds }.take(if (m.id == style.id) Int.MAX_VALUE else 4).map { sound ->
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
        return preferred + (all - preferred.toSet()) + packs
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

    val STYLES: List<Style> = listOf(
        Style(
            "boom_bap", "Boom bap", "Hip-hop", 90.0, "Le hip-hop new-yorkais des années 90 : lourd, posé, qui balance.",
            listOf(
                Line(Role.Kick, "X......x..X.....", "Un kick sur le 1 et quelques coups décalés : le « boom »."),
                Line(Role.Snare, "....X.......X...", "La caisse claire sur 2 et 4 : le « bap »."),
                Line(Role.Hat, "x.o.x.o.x.o.x.o.", "Des croches régulières, une sur deux plus douce."),
                Line(Role.Perc, "..........o....o", "Un petit rim de temps en temps."),
            ),
            swing = 0.25,
            melody = Melody(root = 9, mood = Mood.Dark, bassPattern = "X......X..X.....", bass = "Basse ronde", pad = "Piano électrique", lead = "Pluck"),
        ),
        Style(
            "trap", "Trap", "Hip-hop", 140.0, "Le rap d'Atlanta : rythme à demi-temps, charleys en rafales, 808 énorme.",
            listOf(
                Line(Role.Kick, "X......X..X.....", "Peu de kicks, bien placés : la 808 fait le reste."),
                Line(Role.Bass808, "X......X..X.....", "La 808 joue avec le kick pour un grave qui dure."),
                Line(Role.Clap, "........X.......", "Un seul clap au milieu de la mesure : c'est le demi-temps."),
                Line(Role.Hat, "xoxoxoxoxoxoxoxo", "Des doubles croches serrées (tu pourras ajouter des roulements)."),
                Line(Role.OpenHat, "...............x", "Une ouverture à la fin pour relancer."),
            ),
            melody = Melody(root = 0, mood = Mood.Dark, bassPattern = "X......X..X.....", bass = "Basse sub", pad = "Nappe chaude", lead = "Cloche"),
        ),
        Style(
            "drill", "Drill", "Hip-hop", 142.0, "La drill UK : caisses claires décalées, ambiance sombre, glisses de 808.",
            listOf(
                Line(Role.Kick, "X.....x...X.....", "Kicks espacés, souvent décalés."),
                Line(Role.Bass808, "X.....x...X.....", "La 808 suit le kick (en drill, elle glisse d'une note à l'autre)."),
                Line(Role.Snare, "........X.....x.", "La caisse claire sur le 3 et une deuxième en avance : la signature drill."),
                Line(Role.Hat, "x.xox.x.x.xox.x.", "Des charleys en triolets et petites rafales."),
            ),
            melody = Melody(root = 1, mood = Mood.Dark, bassPattern = "X.....x...X.....", bass = "Basse sub", pad = "Cordes", lead = "Piano FM"),
        ),
        Style(
            "lofi", "Lo-fi hip-hop", "Hip-hop", 84.0, "Le hip-hop pour étudier : doux, chaloupé, un peu poussiéreux.",
            listOf(
                Line(Role.Kick, "X.....x...X.....", "Un kick souple, pas trop fort."),
                Line(Role.Snare, "....X.......X...", "La caisse claire sur 2 et 4, comme en boom bap."),
                Line(Role.Hat, "x.oxx.o.x.oxx.o.", "Des charleys irréguliers et doux pour le côté chaloupé."),
                Line(Role.Perc, "..o.......o.....", "Un shaker ou un rim très discret."),
            ),
            swing = 0.35,
            melody = Melody(root = 3, mood = Mood.Dreamy, bassPattern = "X.....x...X.....", bass = "Basse ronde", pad = "Piano électrique", lead = "Marimba"),
        ),
        Style(
            "house", "House", "House", 124.0, "La house de Chicago : un kick sur chaque temps, ça fait danser.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Un kick sur chaque temps : le « four on the floor »."),
                Line(Role.Clap, "....X.......X...", "Le clap sur 2 et 4, avec le kick."),
                Line(Role.Hat, "o.o.o.o.o.o.o.o.", "Des croches légères en fond."),
                Line(Role.OpenHat, "..X...X...X...X.", "Le charley ouvert entre les temps : le « tss » qui fait sautiller."),
                Line(Role.Perc, "......x.......x.", "Une petite percussion pour relancer."),
            ),
            melody = Melody(root = 9, mood = Mood.Happy, bassPattern = "..X...X...X...X.", bass = "Basse ronde", pad = "Nappe d'accords", lead = "Pluck"),
        ),
        Style(
            "deep_house", "Deep house", "House", 122.0, "La house douce et profonde : chaude, ronde, un peu jazzy.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Le même kick sur chaque temps, plus rond."),
                Line(Role.Clap, "....x.......x...", "Un clap plus discret."),
                Line(Role.Hat, "..x...x...x...x.", "Le charley entre les temps."),
                Line(Role.Perc, "oo.ooo.ooo.ooo.o", "Un shaker continu tout doux qui fait avancer."),
            ),
            swing = 0.15,
            melody = Melody(root = 5, mood = Mood.Dreamy, bassPattern = "..X..X....X..X..", bass = "Basse analogique", pad = "Nappe chaude", lead = "Piano électrique"),
        ),
        Style(
            "tech_house", "Tech house", "House", 126.0, "Entre house et techno : groove serré, percussions qui claquent.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Kick sur chaque temps, bien sec."),
                Line(Role.Clap, "....X.......X...", "Clap sur 2 et 4."),
                Line(Role.OpenHat, "..X...X...X...X.", "Charley ouvert entre les temps."),
                Line(Role.Hat, "oxoxoxoxoxoxoxox", "Doubles croches en fond pour l'énergie."),
                Line(Role.Perc, "...x..x....x..x.", "Des percussions qui rebondissent."),
            ),
            melody = Melody(root = 7, mood = Mood.Future, bassPattern = "..X.X.X...X.X.X.", bass = "Basse FM", pad = "Nappe", lead = "Lead filtré"),
        ),
        Style(
            "uk_garage", "UK Garage", "House", 132.0, "Le 2-step anglais : rythme cassé et sautillant, gros swing.",
            listOf(
                Line(Role.Kick, "X.........X.....", "Pas un kick par temps : deux coups décalés, c'est le « 2-step »."),
                Line(Role.Snare, "....X.......X...", "Caisse claire sur 2 et 4."),
                Line(Role.Clap, "....x.......x..x", "Un clap qui double la caisse et une petite touche à la fin."),
                Line(Role.Hat, "x.xxx.x.x.xxx.x.", "Des charleys bien swingués : c'est tout le groove du garage."),
                Line(Role.Perc, "......o.......o.", "Un rim pour sautiller."),
            ),
            swing = 0.4,
            melody = Melody(root = 2, mood = Mood.Happy, bassPattern = "X..X..X...X..X..", bass = "Basse FM", pad = "Nappe d'accords", lead = "Orgue"),
        ),
        Style(
            "techno", "Techno", "Techno", 130.0, "La techno de Detroit et Berlin : répétitive, hypnotique, puissante.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Un kick sur chaque temps, lourd et constant."),
                Line(Role.Clap, "....x.......x...", "Un clap discret sur 2 et 4."),
                Line(Role.Hat, "oooooooooooooooo", "Des doubles croches régulières en fond."),
                Line(Role.OpenHat, "..X...X...X...X.", "Le charley ouvert entre les temps pour la propulsion."),
                Line(Role.Perc, "...x.......x....", "Une percussion métallique qui revient."),
            ),
            melody = Melody(root = 2, mood = Mood.Future, bassPattern = "..X...X...X...X.", bass = "Basse analogique", pad = "Essaim", lead = "Lead acide"),
        ),
        Style(
            "retro_techno", "Rétro techno", "Techno", 132.0,
            "La techno belge et hollandaise des années 90 (Boccaccio, Montigny, R&S, Rotterdam) : kick 909 qui pompe, stabs et « hoovers ».",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Le kick 909 qui pompe sur chaque temps, comme dans les grandes boîtes belges."),
                Line(Role.Clap, "....X.......X...", "Le clap sec sur 2 et 4."),
                Line(Role.Hat, "xoxoxoxoxoxoxoxo", "Des doubles croches qui roulent sans s'arrêter."),
                Line(Role.OpenHat, "..X...X...X...X.", "Le charley ouvert entre les temps : le « tss » qui fait sauter la salle."),
                Line(Role.Perc, "...x..x....x..x.", "Un rimshot ou une cloche qui revient, très années 90."),
            ),
            melody = Melody(root = 9, mood = Mood.Dark, bassPattern = "..X...X...X...X.", bass = "Basse analogique", pad = "Machine à cordes", lead = "Lead carré"),
        ),
        Style(
            "detroit", "Techno de Detroit", "Techno", 135.0,
            "La techno de Jeff Mills et Underground Resistance : boîte à rythmes 909, percussions qui se croisent, cordes futuristes.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Un kick sec sur chaque temps, la colonne vertébrale."),
                Line(Role.Clap, "....x.......x...", "Un clap discret sur 2 et 4."),
                Line(Role.Hat, "x.xxx.xxx.xxx.xx", "Des charleys serrés qui tournent en boucle, façon 909."),
                Line(Role.OpenHat, "..X...X...X...X.", "Le charley ouvert entre les temps."),
                Line(Role.Perc, "x..x..x..x..x..x", "Une percussion tous les trois pas : elle se décale du kick et fait tourner la tête."),
            ),
            melody = Melody(root = 0, mood = Mood.Future, bassPattern = "X..X..X...X..X..", bass = "Basse analogique", pad = "Cordes", lead = "Lead filtré"),
        ),
        Style(
            "industrial_techno", "Techno industrielle", "Techno", 140.0,
            "Sombre, métallique et saturée : la techno des entrepôts de Brooklyn, Birmingham et Berlin.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Un kick lourd et sale sur chaque temps."),
                Line(Role.Clap, "....X.......X...", "Un clap ou un bruit métallique sur 2 et 4."),
                Line(Role.Hat, "oxoxoxoxoxoxoxox", "Des charleys qui martèlent les doubles croches."),
                Line(Role.Perc, "..x...x.x...x..x", "Des chocs métalliques, comme des machines d'usine."),
            ),
            melody = Melody(root = 1, mood = Mood.Dark, bassPattern = "..X...X...X...X.", bass = "Basse reese", pad = "Essaim", lead = "Voix robot"),
        ),
        Style(
            "gabber", "Gabber", "Techno", 170.0,
            "Le hardcore de Rotterdam (Thunderdome, Energiehal) : un kick distordu énorme et très rapide.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Le kick distordu sur chaque temps : c'est lui toute la musique."),
                Line(Role.Clap, "....x.......x...", "Un clap pour marquer 2 et 4."),
                Line(Role.OpenHat, "..X...X...X...X.", "Le charley ouvert entre les kicks."),
                Line(Role.Hat, "x.x.x.x.x.x.x.x.", "Des croches pour la vitesse."),
            ),
            melody = Melody(root = 9, mood = Mood.Dark, bassPattern = "X...X...X...X...", bass = "Basse sub", pad = "Nappe", lead = "Lead carré"),
        ),
        Style(
            "minimal", "Minimal", "Techno", 128.0, "La techno épurée : peu de sons, chaque petit détail compte.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Le kick sur chaque temps, simple."),
                Line(Role.Hat, "..x...x...x...x.", "Un charley entre les temps."),
                Line(Role.Perc, "x..x..x...x..x..", "Des petits clics et rims qui dessinent le groove."),
                Line(Role.Clap, "............o...", "Un clap très rare."),
            ),
            swing = 0.1,
            melody = Melody(root = 10, mood = Mood.Future, bassPattern = "X..X....X..X....", bass = "Basse pincée", pad = "Nappe", lead = "Arpège"),
        ),
        Style(
            "dnb", "Drum & bass", "Bass music", 174.0, "Le rythme cassé très rapide de Londres, basse énorme.",
            listOf(
                Line(Role.Kick, "X.........X.....", "Kick sur le 1 et juste après le 3 : le « two-step » de la drum & bass."),
                Line(Role.Snare, "....X.......X...", "Caisse claire sur 2 et 4 : à ce tempo, c'est très rapide."),
                Line(Role.Hat, "x.x.x.x.x.x.x.x.", "Des croches pour la vitesse."),
                Line(Role.OpenHat, "..............x.", "Une ouverture en fin de mesure."),
                Line(Role.Perc, "...o...o...o...o", "Un shaker qui remplit les trous."),
            ),
            melody = Melody(root = 5, mood = Mood.Dark, bassPattern = "X.........X.....", bass = "Basse reese", pad = "Nappe chaude", lead = "Lead filtré"),
        ),
        Style(
            "jungle", "Jungle", "Bass music", 165.0, "L'ancêtre de la drum & bass : breaks découpés, esprit reggae.",
            listOf(
                Line(Role.Kick, "X.........X.X...", "Des kicks qui sautent comme un break de funk."),
                Line(Role.Snare, "....X..o.o..X..o", "Caisse claire sur 2 et 4 plus des petites notes fantômes."),
                Line(Role.Hat, "x.x.x.x.x.x.x.x.", "Des croches régulières."),
                Line(Role.Perc, "..o.....o.....o.", "Quelques percussions."),
            ),
            melody = Melody(root = 7, mood = Mood.Dark, bassPattern = "X.......X.X.....", bass = "Basse sub", pad = "Cordes", lead = "Piano FM"),
        ),
        Style(
            "dubstep", "Dubstep", "Bass music", 140.0, "La bass music anglaise : demi-temps, gros sub et basses qui grondent.",
            listOf(
                Line(Role.Kick, "X.........x.....", "Un kick sur le 1, un autre avant le dernier temps."),
                Line(Role.Snare, "........X.......", "Une seule grosse caisse claire au milieu : c'est le demi-temps."),
                Line(Role.Hat, "x.x.x.x.x.x.x.x.", "Des croches régulières."),
                Line(Role.Bass808, "X...............", "Un sub qui tient toute la mesure."),
                Line(Role.Perc, "......o.......o.", "Une petite percussion de relance."),
            ),
            melody = Melody(root = 5, mood = Mood.Dark, bassPattern = "X...............", bass = "Basse reese", pad = "Nappe", lead = "Lead carré"),
        ),
        Style(
            "reggaeton", "Reggaeton", "Autres", 95.0, "Le rythme « dembow » de Porto Rico : impossible de ne pas bouger.",
            listOf(
                Line(Role.Kick, "X...X...X...X...", "Un kick sur chaque temps."),
                Line(Role.Snare, "...X..X....X..X.", "La caisse claire décalée : c'est le dembow."),
                Line(Role.Hat, "x.x.x.x.x.x.x.x.", "Des croches régulières."),
                Line(Role.Perc, "..o...o...o...o.", "Une percussion légère."),
            ),
            melody = Melody(root = 9, mood = Mood.Happy, bassPattern = "X..X....X..X....", bass = "Basse sub", pad = "Piano électrique", lead = "Marimba"),
        ),
        Style(
            "afrobeats", "Afrobeats", "Autres", 105.0, "La pop d'Afrique de l'Ouest : percussions qui roulent, groove solaire.",
            listOf(
                Line(Role.Kick, "X..x..x...X..x..", "Un kick qui danse autour des temps."),
                Line(Role.Snare, "....x.......x...", "Caisse claire ou rim sur 2 et 4, léger."),
                Line(Role.Hat, "oooooooooooooooo", "Un shaker continu."),
                Line(Role.Perc, "..x..x.x..x..x.x", "Des congas qui répondent au kick."),
            ),
            swing = 0.2,
            melody = Melody(root = 7, mood = Mood.Happy, bassPattern = "X..X..X...X..X..", bass = "Basse ronde", pad = "Piano électrique", lead = "Marimba"),
        ),
        Style(
            "pop", "Pop", "Autres", 110.0, "Un rythme pop simple et efficace, pour chanter dessus.",
            listOf(
                Line(Role.Kick, "X.......X.X.....", "Un kick sur le 1 et le 3, plus une petite relance."),
                Line(Role.Snare, "....X.......X...", "Caisse claire sur 2 et 4."),
                Line(Role.Clap, "....x.......x...", "Un clap qui double la caisse claire."),
                Line(Role.Hat, "x.x.x.x.x.x.x.x.", "Des croches régulières."),
            ),
            melody = Melody(root = 0, mood = Mood.Happy, bassPattern = "X.X.X.X.X.X.X.X.", bass = "Basse ronde", pad = "Cordes", lead = "Pluck"),
        ),
    )

    fun style(id: String): Style? = STYLES.firstOrNull { it.id == id }

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
        val steps = style.lines.maxOf { it.pattern.length }.coerceIn(DrumPattern.MIN_STEPS, DrumPattern.MAX_STEPS)
        val lanes = MutableList(DrumPattern.PAD_COUNT) { DrumLane() }
        for (line in style.lines) {
            if (line.role !in sounds) continue
            lanes[line.role.lane] = DrumLane(hits = hits(line.pattern).filterKeys { it < steps }, sample = sounds[line.role])
        }
        var project = Project(
            name = style.name,
            tempoBpm = style.bpm,
            swing = style.swing,
            drums = DrumPattern(steps = steps, lanes = lanes),
        )
        if (mood != null) {
            val harmony = Harmony(style.melody.root, mood.scale, lock = true)
            project = project.copy(harmony = harmony)
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

    /**
     * Four bars to start from, one chord of the mood per bar: the bass plays each chord's root on
     * the style's bass rhythm, the pad holds the chords, the lead arpeggiates them an octave up.
     */
    fun startingNotes(part: Part, style: Style, mood: Mood, harmony: Harmony): NoteClip = when (part) {
        Part.Pad -> NoteClip().withChords(mood.degrees, harmony, octaveRoot = 48, velocity = 0.7f)
        Part.Lead -> NoteClip(performance = Performance(arp = ArpMode.UpDown, arpRate = NoteClip.TICKS_PER_STEP))
            .withChords(mood.degrees, harmony, octaveRoot = 72, velocity = 0.65f)
        Part.Bass -> NoteClip().withBassLine(mood.degrees, harmony, style.melody.bassPattern)
    }
}
