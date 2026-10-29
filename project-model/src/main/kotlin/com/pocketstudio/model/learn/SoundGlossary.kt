package com.pocketstudio.model.learn

import com.pocketstudio.model.AcidGenerator
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.ArpMode
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scale
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.tr
import java.text.Normalizer
import kotlin.random.Random

/**
 * What a glossary word sounds like: one or more tiny projects ([takes]) the engine renders, each
 * for [bars] bars, one after the other with a short silence between them (« sans », then « avec »).
 * [labels]: what each take is, shown while it plays (empty: a single take, nothing to say).
 * No audio file: every sound is made by the engine from these projects.
 */
data class SoundExample(val takes: List<Project>, val bars: Int = 2, val labels: List<String> = emptyList())

/**
 * A word of music the app uses, in plain words, with a sound example. [term] and [text] are in the
 * interface's language; [key] stays the same in every language. [aliases]: the French names of the
 * same idea in the app's « ? » helps ([com.pocketstudio.model.i18n.I18n.frenchOf] of a help's term),
 * so a « ? » can offer the example too.
 */
class SoundWord(
    val key: String,
    val term: String,
    val text: String,
    val aliases: List<String> = emptyList(),
    val example: () -> SoundExample,
)

/**
 * The glossary « Les mots de la musique, à écouter » (phase 39): about thirty words, each one or two
 * plain sentences and an example made by the engine. Grouped as a beginner meets them: the drums,
 * the instruments, notes and time, the parts of a song, then the effects and the mix.
 */
object SoundGlossary {
    /** Bars per take: 2 bars at 120 BPM is 4 seconds. */
    const val BARS = 2

    val ALL: List<SoundWord> get() = listOf(
        // --- The drums ---
        SoundWord("kick", tr("Kick"), tr("La grosse caisse : le « boum » grave qui marque les temps. En house et en techno, il tape sur chaque temps."),
            listOf("Kick")) { one(drumsOnly(0 to "X...X...X...X...")) },
        SoundWord("snare", tr("Caisse claire"), tr("Le « tchak » sec sur les temps 2 et 4. Avec le kick, c'est le squelette du rythme."),
            listOf("Caisse", "Caisse claire")) { one(drumsOnly(0 to "X.......X.......", 2 to "....X.......X...")) },
        SoundWord("hat", tr("Charley"), tr("Le petit « tss » aigu qui tient le rythme entre les coups. Fermé il est court, ouvert il sonne plus longtemps."),
            listOf("Charley")) { one(drumsOnly(5 to "X.x.X.x.X.x.X...", 6 to "..............X.")) },
        SoundWord("clap", tr("Clap"), tr("Un claquement de mains, souvent sur les temps 2 et 4 à la place de la caisse claire ou avec elle."),
            listOf("Clap")) { one(drumsOnly(0 to "X...X...X...X...", 4 to "....X.......X...")) },
        SoundWord("808", tr("808"), tr("Une basse très grave qui sonne comme un kick long, venue de la boîte à rythmes TR-808. Le cœur du trap et du rap."),
            listOf("808")) { one(bass808()) },
        // --- The instruments ---
        SoundWord("bass", tr("Basse"), tr("L'instrument le plus grave après le kick : il joue les notes de base des accords et fait bouger le corps."),
            listOf("Basse")) { one(withBass(groove(), "Basse ronde")) },
        SoundWord("acid", tr("Acid 303"), tr("Le son de la Roland TB-303 : une basse qui couine et qui « parle » quand son filtre s'ouvre. Né avec l'acid house."),
            listOf("Acid 303", "Basse acide", "Acide")) { one(acid()) },
        SoundWord("pad", tr("Nappe"), tr("Un son doux et long qui tient les accords en fond, comme un tapis sous le morceau."),
            listOf("Nappe")) { one(withChords(Project(tempoBpm = 110.0).asStart(), "Nappe")) },
        SoundWord("lead", tr("Lead"), tr("La mélodie principale, celle qu'on retient et qu'on chante. Elle passe devant tout le reste."),
            listOf("Lead")) { one(withLead(groove(), "Lead")) },
        SoundWord("arp", tr("Arpège"), tr("Les notes d'un accord jouées l'une après l'autre, en montant ou en descendant, au rythme du morceau."),
            listOf("Arpège")) { one(arpeggio()) },
        // --- Notes and time ---
        SoundWord("chord", tr("Accord"), tr("Plusieurs notes jouées ensemble. Ici, un accord majeur (joyeux) puis un accord mineur (plus triste)."),
            listOf("Accord")) { one(majorMinor()) },
        SoundWord("scale", tr("Gamme"), tr("Les notes qui vont bien ensemble dans un morceau, rangées de la plus grave à la plus aiguë. Ici, La mineur."),
            listOf("Gamme", "Gamme et accords")) { one(scaleRun()) },
        SoundWord("tonic", tr("Note tonique"), tr("La note « maison » de la gamme : une mélodie qui finit dessus sonne terminée. Écoute : la première phrase reste en l'air, la deuxième rentre à la maison."),
            listOf("Note d'origine")) { tonic() },
        SoundWord("tempo", tr("Tempo (BPM)"), tr("La vitesse du morceau, en battements par minute. Le même rythme à 90, puis à 140."),
            listOf("Tempo (BPM)", "Tempo")) {
            SoundExample(listOf(groove().copy(tempoBpm = 90.0), groove().copy(tempoBpm = 140.0)), labels = listOf(tr("90 BPM"), tr("140 BPM")))
        },
        SoundWord("swing", tr("Swing"), tr("Un pas sur deux arrive un peu en retard : le rythme se met à chalouper au lieu d'être carré. D'abord carré, puis avec du swing."),
            listOf("Swing")) { before(hats(), hats().copy(swing = 0.45)) },
        SoundWord("quantize", tr("Remettre en rythme"), tr("Recale sur la grille les notes jouées un peu à côté (on dit aussi « quantifier »). D'abord joué à la main, puis remis en rythme."),
            listOf("Quantifier", "Remettre en rythme")) { before(humanised(), groove(), first = tr("Joué à la main"), second = tr("Remis en rythme")) },
        SoundWord("bar", tr("Mesure"), tr("Un groupe de 4 temps : « 1, 2, 3, 4 ». La cymbale marque le début de chaque mesure."),
            listOf("Mesure", "Temps par mesure")) { one(bars()) },
        // --- The parts of a song ---
        SoundWord("part", tr("Partie"), tr("Un bout de morceau qui tourne en boucle (souvent 4 ou 8 mesures). Le morceau enchaîne les parties : une partie, puis une autre."),
            listOf("Partie")) { SoundExample(listOf(verse(), chorus()), labels = listOf(tr("Partie A"), tr("Partie B"))) },
        SoundWord("intro", tr("Intro"), tr("Le début du morceau, plus léger : peu d'instruments, pour donner envie de la suite."),
            listOf("Intro")) { one(intro()) },
        SoundWord("verse", tr("Couplet"), tr("La partie qui raconte : la batterie et la basse posent le groove, on garde des forces pour le refrain."),
            listOf("Couplet")) { one(verse()) },
        SoundWord("chorus", tr("Refrain"), tr("La partie qu'on retient et qui revient : tout le monde joue, avec la mélodie principale."),
            listOf("Refrain")) { one(chorus()) },
        SoundWord("buildup", tr("Montée"), tr("La tension monte avant le grand moment : la caisse claire accélère, le kick s'efface, un souffle grimpe."),
            listOf("Montée")) { one(buildUp()) },
        SoundWord("drop", tr("Drop"), tr("Le moment où tout retombe d'un coup après la montée, plus fort que le reste : le cœur des morceaux électro."),
            listOf("Drop")) { SoundExample(listOf(buildUp(), drop()), bars = BARS, labels = listOf(tr("Montée"), tr("Drop"))) },
        SoundWord("break", tr("Break"), tr("Une pause dans le morceau : le kick et la basse s'arrêtent, il reste les accords et l'espace."),
            listOf("Break")) { one(breakdown()) },
        SoundWord("fill", tr("Fill"), tr("Un petit roulement de batterie à la fin d'une partie, pour annoncer la suivante."),
            listOf("Fill")) { one(fill()) },
        // --- Effects and the mix ---
        SoundWord("sidechain", tr("Sidechain (pompe)"), tr("Les accords baissent à chaque coup de kick puis remontent : le son « respire » et pompe. D'abord sans, puis avec."),
            listOf("Pompe", "Sidechain")) { before(padOverKick(pump = false), padOverKick(pump = true)) },
        SoundWord("reverb", tr("Réverbe"), tr("L'écho naturel d'une pièce : le son paraît plus loin, dans une salle. D'abord sec, puis avec de la réverbe."),
            listOf("Réverbe", "Réverbe et écho")) { before(clapLead(), clapLead().send(bus = 0, amount = 0.7f)) },
        SoundWord("echo", tr("Écho"), tr("Le son se répète en rythme, de plus en plus doucement. D'abord sans, puis avec l'écho."),
            listOf("Écho")) { before(clapLead(), clapLead().send(bus = 1, amount = 0.6f)) },
        SoundWord("filter", tr("Filtre"), tr("Rend le son plus sourd (fermé) ou plus brillant (ouvert). D'abord fermé, comme derrière un mur, puis ouvert."),
            listOf("Filtre")) { before(groove().withInsert(Mixer.MASTER, filter(0.3f)), groove(), first = tr("Fermé"), second = tr("Ouvert")) },
        SoundWord("compressor", tr("Compresseur"), tr("Rapproche les passages forts et faibles : la batterie devient plus serrée et plus présente. D'abord sans, puis avec."),
            listOf("Compresseur")) { before(groove(), groove().withInsert(0, compressor())) },
        // Audit: the Guide's full mode says « OTT », « LUFS » and « pan »: each has its word.
        SoundWord("ott", tr("OTT (compresseur trois bandes)"), tr("Un compresseur qui travaille sur le grave, le médium et l'aigu à la fois : le son devient plus dense et plus gros. D'abord sans, puis avec."),
            listOf("OTT", "OTT multibande")) { before(withBass(groove(), "Basse ronde"), withBass(groove(), "Basse ronde").withInsert(2, Insert.of(EffectType.OTT))) },
        SoundWord("saturation", tr("Saturation"), tr("Fait chauffer le son jusqu'à le salir un peu : plus de grain, plus d'énergie. D'abord propre, puis saturé."),
            listOf("Saturation")) { before(withBass(groove(), "Basse ronde"), withBass(groove(), "Basse ronde").withInsert(2, saturation())) },
        SoundWord("automation", tr("Automation (courbe)"), tr("Un réglage qui bouge tout seul pendant la lecture, en suivant une courbe qu'on a dessinée. Ici le filtre s'ouvre peu à peu."),
            listOf("Automation", "Courbes (automation)", "Courbes")) { one(automated()) },
        SoundWord("mix", tr("Mixage"), tr("Régler le volume et la place de chaque piste pour que tout s'entende bien. D'abord tout au même volume, puis mixé."),
            listOf("Mixeur", "Mixage")) { before(unmixed(), chorus(), first = tr("Pas mixé"), second = tr("Mixé")) },
        SoundWord("pan", tr("Place (panoramique)"), tr("Où un son se trouve entre la gauche et la droite (on dit aussi « pan »). D'abord tout au centre, puis le clap à gauche et la mélodie à droite."),
            listOf("Place", "Place (pan)", "Pan", "Panoramique")) { before(clapLead(), clapLead().panned(0, -0.6f).panned(2, 0.6f), first = tr("Au centre"), second = tr("Placés")) },
        SoundWord("master", tr("Master"), tr("La dernière étape, sur tout le morceau : on le rend plus fort et plus uni, prêt à partager. D'abord sans, puis masterisé."),
            listOf("Sortie générale", "Limiteur", "Master")) { before(chorus(), chorus().let { it.copy(mixer = it.mixer.copy(target = LoudnessTarget.CLUB)) }) },
        SoundWord("loudness", tr("Volume final (LUFS)"), tr("Le volume moyen du morceau entier, mesuré en LUFS : vers −14 pour le streaming, vers −8 en club. Plus le chiffre est proche de 0, plus c'est fort."),
            listOf("LUFS", "Volume final", "Cible de volume")) {
            fun at(t: LoudnessTarget) = chorus().let { it.copy(mixer = it.mixer.copy(target = t)) }
            before(at(LoudnessTarget.STREAMING), at(LoudnessTarget.CLUB), first = tr("Streaming"), second = tr("Club"))
        },
    )

    /** The headings the list is shown under, each with the keys of its words (in [ALL]'s order). */
    val GROUPS: List<Pair<String, List<String>>> get() = listOf(
        tr("La batterie") to listOf("kick", "snare", "hat", "clap", "808"),
        tr("Les instruments") to listOf("bass", "acid", "pad", "lead", "arp"),
        tr("Les notes et le temps") to listOf("chord", "scale", "tonic", "tempo", "swing", "quantize", "bar"),
        tr("Les parties d'un morceau") to listOf("part", "intro", "verse", "chorus", "buildup", "drop", "break", "fill"),
        tr("Les effets et le mix") to listOf("sidechain", "reverb", "echo", "filter", "compressor", "ott", "saturation", "automation", "mix", "pan", "master", "loudness"),
    )

    /**
     * The word whose example a « ? » help named [term] can play, if any: [term] is its French term or
     * the term as shown (a shown word can translate several French ones: Dutch « Grondtoon » is
     * « Note d'origine » and « Note tonique », so [I18n.frenchOf] alone may give the other one).
     */
    fun forHelp(term: String): SoundWord? =
        ALL.firstOrNull { w -> w.aliases.any { it.equals(term, ignoreCase = true) || tr(it).equals(term, ignoreCase = true) } }

    /**
     * The words matching [query] in their term or explanation, accents and case aside (all of them
     * for a blank query): the word itself first, then the words whose name starts with it or holds
     * it, then the ones that only talk about it.
     */
    fun search(query: String, words: List<SoundWord> = ALL): List<SoundWord> {
        val q = fold(query.trim())
        if (q.isEmpty()) return words
        fun rank(w: SoundWord): Int {
            val names = (listOf(w.term, I18n.frenchOf(w.term)) + w.aliases).map(::fold)
            return when {
                names.any { it == q } -> 0
                names.any { it.startsWith(q) } -> 1
                names.any { it.contains(q) } -> 2
                fold(w.text).contains(q) -> 3
                else -> 4
            }
        }
        return words.map { it to rank(it) }.filter { it.second < 4 }.sortedBy { it.second }.map { it.first }
    }

    /** Lower case, no accents: « réverbe » finds « Réverbe » and « reverbe ». */
    fun fold(s: String): String = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    // --- Building the examples ------------------------------------------------------------------

    private fun one(p: Project) = SoundExample(listOf(p))

    private fun before(a: Project, b: Project, first: String = tr("Sans"), second: String = tr("Avec")) = SoundExample(listOf(a, b), labels = listOf(first, second))

    /** A blank project as the app starts one now (kit v3, the master leaving room). */
    private fun Project.asStart(): Project = copy(kitVersion = Project.KIT_V3, mixer = mixer.copy(masterDb = Mixer.NEW_PROJECT_DB))

    /** One character a step: 'X' strong, 'x' medium, 'o' soft, '.' nothing; a pattern of 16 repeats over the loop. */
    private fun drums(vararg lanes: Pair<Int, String>, steps: Int = 16): DrumPattern {
        var d = DrumPattern(steps = steps)
        lanes.forEach { (lane, pattern) ->
            for (step in 0 until steps) {
                val v = when (pattern[step % pattern.length]) { 'X' -> 1f; 'x' -> 0.7f; 'o' -> 0.45f; else -> 0f }
                if (v > 0f) d = d.toggle(lane, step, v)
            }
        }
        return d
    }

    private fun drumsOnly(vararg lanes: Pair<Int, String>) = Project(tempoBpm = 120.0, drums = drums(*lanes)).asStart()

    private val MINOR = Harmony(9, Scale.Minor)  // A minor

    /** Kick, snare, hats: the groove most examples play over. */
    private fun groove(bpm: Double = 120.0) = Project(
        tempoBpm = bpm, harmony = MINOR,
        drums = drums(0 to "X...X...X...X...", 2 to "....X.......X...", 5 to "..x...x...x...x."),
    ).asStart()

    private fun hats() = Project(tempoBpm = 100.0, drums = drums(0 to "X......X..X.....", 2 to "....X.......X...", 5 to "xoxoxoxoxoxoxoxo")).asStart()

    /** The groove played by hand: every hit a little early or late (seeded, the same each time). */
    private fun humanised(): Project {
        val g = groove()
        val r = Random(39)
        val lanes = g.drums.lanes.map { lane -> lane.copy(hits = lane.hits.mapValues { (_, h) -> h.copy(offset = (r.nextFloat() * 2f - 1f) * 38f) }) }
        return g.copy(drums = g.drums.copy(lanes = lanes))
    }

    private fun sound(name: String): SynthPatch = SoundLibrary.SYNTH.first { it.name == name }.sound

    /** The first instrument track playing [notes] with the library's sound [name]. */
    private fun Project.firstTrack(name: String, notes: NoteClip): Project = copy(synth = sound(name), synthNotes = notes, synthTrackName = name)

    /** An added instrument track [id] (1..) playing [notes] with the library's sound [name]. */
    private fun Project.addTrack(id: Int, name: String, notes: NoteClip, volume: Float = 0.8f): Project {
        val ch = InstrumentTrack.mixerChannel(id)
        return copy(
            tracks = tracks + InstrumentTrack(id = id, name = name, color = id, synth = sound(name), notes = notes),
            mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(volume = volume) else c }),
        )
    }

    private val PROGRESSION = listOf(0, 5)  // i–VI over the two bars

    private fun bassLine(pattern: String = "X..x..X.X..x..x.") = NoteClip().withBassLine(PROGRESSION, MINOR, pattern, octaveRoot = 33)
    private fun chordClip() = NoteClip().withChords(PROGRESSION, MINOR, octaveRoot = 57, velocity = 0.7f)

    /** A short hook in A minor, two bars. */
    private fun hook(): NoteClip {
        val t = NoteClip.TICKS_PER_STEP
        val line = listOf(0 to 76, 3 to 74, 6 to 72, 8 to 74, 12 to 69, 16 to 76, 19 to 77, 22 to 76, 24 to 74, 28 to 72)
        return NoteClip(steps = 32, notes = line.mapIndexed { i, (step, pitch) ->
            val next = line.getOrNull(i + 1)?.first ?: 32
            Note(step * t, (next - step).coerceAtMost(4) * t, pitch, 0.8f)
        }).sanitized()
    }

    private fun withBass(p: Project, name: String) = p.firstTrack(name, bassLine())
    private fun withChords(p: Project, name: String) = p.firstTrack(name, chordClip())
    private fun withLead(p: Project, name: String) = p.firstTrack(name, hook())

    private fun bass808(): Project {
        val base = Project(tempoBpm = 140.0, drums = drums(0 to "X.........X.....", 2 to "....X.......X...", 5 to "x.x.x.x.x.xxx.x.")).asStart()
        // Pad 2 plays the 808 voice; its hits carry the notes (a slide into the last one).
        val notes = mapOf(0 to Hit(1f), 7 to Hit(0.9f, pitch = 3), 10 to Hit(0.9f), 14 to Hit(0.9f, pitch = -2, glide = 120))
        val lanes = base.drums.lanes.toMutableList().also { it[1] = it[1].copy(hits = notes) }
        return base.copy(drums = base.drums.copy(lanes = lanes), padVoices = mapOf(1 to PadVoice(PadVoice.BASS_808)))
    }

    private fun acid(): Project {
        val line = AcidGenerator(Random(303)).generate(AcidPattern(root = 33), AcidGenerator.NOTE_SETS[0])
        val squelchy = SoundLibrary.ACID.first { it.name == "Squelchy" }.sound
        return groove(126.0).copy(acid = line.copy(sound = squelchy))
    }

    private fun arpeggio(): Project {
        val clip = chordClip().let { it.copy(performance = it.performance.copy(arp = ArpMode.UpDown, arpRate = 24)) }
        return Project(tempoBpm = 118.0, harmony = MINOR, drums = drums(0 to "X...X...X...X...")).asStart().firstTrack("Arpège", clip)
    }

    private fun majorMinor(): Project {
        val bar = 16 * NoteClip.TICKS_PER_STEP
        val c = listOf(60, 64, 67).map { Note(0, bar, it, 0.75f) }
        val cm = listOf(60, 63, 67).map { Note(bar, bar, it, 0.75f) }
        return Project(tempoBpm = 100.0).asStart().firstTrack("Piano électrique", NoteClip(steps = 32, notes = c + cm).sanitized())
    }

    private fun scaleRun(): Project {
        val t = NoteClip.TICKS_PER_STEP
        val pitches = listOf(57, 59, 60, 62, 64, 65, 67, 69)
        val notes = pitches.mapIndexed { i, p -> Note(i * 4 * t, 3 * t, p, 0.8f) }
        return Project(tempoBpm = 120.0, harmony = MINOR).asStart().firstTrack("Piano électrique", NoteClip(steps = 32, notes = notes).sanitized())
    }

    /** The same phrase twice: ending on the second (up in the air), then on the tonic (home). */
    private fun tonic(): SoundExample {
        val t = NoteClip.TICKS_PER_STEP
        fun phrase(last: Int): Project {
            val line = listOf(64, 62, 60, 62, 64, 64, last)
            val notes = line.mapIndexed { i, p -> Note(i * 4 * t, if (i == line.lastIndex) 8 * t else 3 * t, p, 0.8f) }
            return Project(tempoBpm = 110.0, harmony = Harmony(0, Scale.Major)).asStart().firstTrack("Piano électrique", NoteClip(steps = 32, notes = notes).sanitized())
        }
        return SoundExample(listOf(phrase(62), phrase(60)), labels = listOf(tr("En l'air"), tr("À la maison")))
    }

    private fun bars(): Project = Project(tempoBpm = 120.0, drums = drums(0 to "X...X...X...X...", 5 to "..x...x...x...x.", 12 to "X...............")).asStart()

    private fun intro(): Project = Project(tempoBpm = 124.0, harmony = MINOR, drums = drums(0 to "X...X...X...X...", 5 to "..o...o...o...o.")).asStart()
        .withInsert(Mixer.MASTER, filter(0.45f))

    private fun verse(): Project = withBass(groove(124.0), "Basse ronde")

    private fun chorus(): Project = verse()
        .addTrack(1, "Nappe", chordClip(), volume = 0.55f)
        .addTrack(2, "Lead", hook(), volume = 0.6f)
        .let { it.copy(drums = drums(0 to "X...X...X...X...", 2 to "....X.......X...", 4 to "....X.......X...", 5 to "x.x.x.x.x.x.x.x.", 6 to "..X...X...X...X.")) }
        .send(ch = InstrumentTrack.mixerChannel(2), bus = 0, amount = 0.35f)

    private fun buildUp(): Project {
        val roll = drums(2 to "x...x...x.x.xxxx", steps = 32).let { d ->
            // The second bar rolls faster and louder.
            val lane = d.lanes[2]
            val hits = lane.hits.mapValues { (step, h) -> if (step >= 24) h.copy(repeats = 2, velocity = 0.6f + (step - 24) * 0.05f, ramp = 1) else h }
            d.copy(lanes = d.lanes.toMutableList().also { it[2] = lane.copy(hits = hits) })
        }
        val riser = NoteClip(steps = 32, notes = listOf(Note(0, 32 * NoteClip.TICKS_PER_STEP, 57, 0.7f)))
        return Project(tempoBpm = 124.0, harmony = MINOR, drums = roll).asStart().firstTrack("Souffle", riser).automate(
            AutoTarget.synth(0, SynthPatch.CUTOFF), List(32) { 0.15f + 0.8f * it / 31f },
        )
    }

    private fun drop(): Project = chorus().copy(drums = drums(0 to "X...X...X...X...", 2 to "....X.......X...", 4 to "....X.......X...", 5 to "xxxxxxxxxxxxxxxx", 6 to "..X...X...X...X.", 12 to "X..............."))
        .withInsert(InstrumentTrack.mixerChannel(1), Insert.of(EffectType.PUMP))

    private fun breakdown(): Project = Project(tempoBpm = 124.0, harmony = MINOR).asStart().firstTrack("Nappe chaude", chordClip()).send(ch = 2, bus = 0, amount = 0.6f)

    private fun fill(): Project {
        val d = drums(0 to "X...X...X...X...", 2 to "....X.......X...", 5 to "..x...x...x...x.", steps = 32)
        // The last beat of the second bar: snare and toms down the kit.
        val toms = mapOf(2 to listOf(28, 29), 9 to listOf(30), 8 to listOf(31))
        val lanes = d.lanes.mapIndexed { i, lane ->
            val extra = toms[i].orEmpty().associateWith { Hit(0.9f, repeats = if (i == 2) 2 else 1) }
            lane.copy(hits = lane.hits.filterKeys { it < 28 || i == 0 && it == 28 } + extra)
        }
        return Project(tempoBpm = 120.0, drums = d.copy(lanes = lanes)).asStart()
    }

    private fun padOverKick(pump: Boolean): Project {
        val p = Project(tempoBpm = 124.0, harmony = MINOR, drums = drums(0 to "X...X...X...X...")).asStart().firstTrack("Nappe", chordClip())
        return if (pump) p.withInsert(2, Insert.of(EffectType.PUMP)) else p
    }

    /** A clap and a short lead with room around them (the reverb and the echo are heard in the gaps). */
    private fun clapLead(): Project {
        val t = NoteClip.TICKS_PER_STEP
        val notes = listOf(0 to 69, 6 to 72, 16 to 76, 22 to 74).map { (s, p) -> Note(s * t, 2 * t, p, 0.8f) }
        return Project(tempoBpm = 110.0, harmony = MINOR, drums = drums(4 to "....X.......X...")).asStart()
            .firstTrack("Pluck", NoteClip(steps = 32, notes = notes).sanitized())
    }

    private fun automated(): Project = withChords(Project(tempoBpm = 120.0, harmony = MINOR, drums = drums(0 to "X...X...X...X...", steps = 32)).asStart(), "Lead")
        .withInsert(2, filter(0.2f))
        .automate(AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0), List(32) { 0.15f + 0.75f * it / 31f })

    /** Every track at full volume, nothing placed: the bass and the hats cover the rest. */
    private fun unmixed(): Project {
        val c = chorus()
        return c.copy(mixer = c.mixer.copy(channels = c.mixer.channels.map { it.copy(volume = 1f, sends = it.sends.map { 0f }) }))
    }

    private fun filter(cutoff: Float) = Insert(EffectType.FILTER.ordinal, listOf(cutoff, 0.25f, 0f, 1f))
    private fun compressor() = Insert(EffectType.COMPRESSOR.ordinal, listOf(0.25f, 0.6f, 0.1f, 0.25f, 0.45f, 0f, 0f, 0f, 1f))
    private fun saturation() = Insert.of(EffectType.SATURATION).let { it.copy(params = it.params.toMutableList().also { p -> p[0] = 0.85f }) }

    /** [insert] in the first free slot of strip [ch] (the master: [Mixer.MASTER]). */
    private fun Project.withInsert(ch: Int, insert: Insert): Project {
        val m = mixer
        return if (ch == Mixer.MASTER) {
            val i = m.master.indexOfFirst { it.type == 0 }.coerceAtLeast(0)
            copy(mixer = m.copy(master = m.master.mapIndexed { k, x -> if (k == i) insert else x }))
        } else {
            copy(mixer = m.copy(channels = m.channels.mapIndexed { k, c ->
                if (k != ch) c else {
                    val i = c.inserts.indexOfFirst { it.type == 0 }.coerceAtLeast(0)
                    c.copy(inserts = c.inserts.mapIndexed { j, x -> if (j == i) insert else x })
                }
            }))
        }
    }

    /** Strip [ch] sends [amount] to bus [bus] (0 reverb, 1 echo); a bus out of range changes nothing. */
    private fun Project.send(ch: Int = 2, bus: Int, amount: Float): Project {
        if (bus !in 0 until com.pocketstudio.model.Buses.COUNT) return this
        return copy(mixer = mixer.copy(channels = mixer.channels.mapIndexed { k, c ->
            if (k != ch) c else c.copy(sends = c.sends.mapIndexed { b, s -> if (b == bus) amount else s })
        }))
    }

    /** Strip [ch] placed at [pan] (−1 left .. 1 right). */
    private fun Project.panned(ch: Int, pan: Float): Project =
        copy(mixer = mixer.copy(channels = mixer.channels.mapIndexed { k, c -> if (k == ch) c.copy(pan = pan) else c }))

    /** A loop lane on [target]: [values] over the steps of the drum loop (one value a sixteenth). */
    private fun Project.automate(target: AutoTarget, values: List<Float>): Project =
        copy(automation = automation + AutomationLane(target, values = values.map { it.coerceIn(0f, 1f) }))
}
