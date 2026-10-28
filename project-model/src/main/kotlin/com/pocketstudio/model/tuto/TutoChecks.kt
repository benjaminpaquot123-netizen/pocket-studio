package com.pocketstudio.model.tuto

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Channel
import com.pocketstudio.model.DrumLane
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Note
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.NewTracks
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.currentSceneContent
import com.pocketstudio.model.firstEmptyScene
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import com.pocketstudio.model.withFx
import com.pocketstudio.model.withTrack
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * What the course aims at in a style: the recipe's lines, notes and whole song, as the assistant
 * makes them (the targets of the steps, never written into the user's project but by « Montre-moi »).
 */
class TutoTarget private constructor(val style: StarterWizard.Style) {
    val mood = style.melody.mood
    val harmony = StarterWizard.harmonyOf(style, mood)

    /** The recipe's drum line of [role] ("kick", "snare", "clap", "hat", "perc"), null when it has none. */
    fun line(role: String): StarterWizard.Line? = style.lines.firstOrNull { it.role.id == role }

    /** The kind of track that plays [part] (« + Piste »'s), its sound and level as the assistant gives them. */
    fun kind(part: Part) = when (part) {
        Part.Bass -> TrackKind.Bass
        Part.Pad -> TrackKind.Chords
        else -> TrackKind.Melody
    }

    private val notesCache = ConcurrentHashMap<Part, NoteClip>()

    /**
     * The notes of [part] in the style (the assistant's first notes, in the course's key), as a
     * phrase of its first half played twice (testers, lot « tuto 4 »: 28 bass notes, 24 of chords
     * touched one by one; the user writes the first 4 bars, « Copier sur les mesures suivantes »
     * repeats them). The jump up bass's one change in its 7th bar goes; the chords and the lead
     * already repeat.
     */
    fun notes(part: Part): NoteClip = notesCache.getOrPut(part) {
        val c = StarterWizard.startingNotes(part, style, mood, harmony, NewTracks.sound(kind(part), style))
        val half = c.ticks / 2
        if (c.steps < 2 * MOTIF_MIN_STEPS) c
        else c.copy(notes = c.notes.filter { it.start < half }.let { first -> first + first.map { it.copy(start = it.start + half) } }).sanitized()
    }

    /** The style's whole song, as the assistant makes it with its advised sounds (the examples' parts and the map). */
    val song: Project by lazy {
        val parts = StarterWizard.defaultParts(style).associateWith { NewTracks.sound(kind(it), style) }
        StarterWizard.build(style, style.lines.associate { it.role to null }, mood, parts).sanitized()
    }

    /** The scene of [song] named [label], -1 when none. */
    fun sceneOf(label: String): Int = song.allScenes.indexOfFirst { it.label == label }

    /**
     * The tutorial's project at its start: EMPTY (no hit, no note, no part, the first track left
     * blank and hidden until a step makes it a track of the song), but already with the style's
     * drum sounds as the assistant sets them (the kit, its voices, their levels and their effects:
     * the punch on the kick and the snare, not their reverb, a step's), and for jump up the drum &
     * bass kit built in ([StudioDrums.withDnbKit]: « Kit : Drum & bass », a short dry kick; owner,
     * 2026-10-28: « ça doit sonner jump up dès le chapitre 2 »): what the user builds sounds like
     * the examples. Tempo and key stay the new project's, steps set them.
     */
    fun blank(): Project {
        val s = song
        val fresh = Project(name = "")
        val drumChannels = setOf(0) + (0 until DrumPattern.PAD_COUNT).map { Mixer.padChannel(it) }
        val made = fresh.copy(
            kitVersion = s.kitVersion, soundVersion = s.soundVersion, mixVersion = s.mixVersion, padVoices = s.padVoices, drumSeed = s.drumSeed,
            drums = s.drums.copy(steps = fresh.drums.steps, lanes = s.drums.lanes.map { it.copy(hits = emptyMap()) }),
            mixer = fresh.mixer.copy(channels = fresh.mixer.channels.mapIndexed { i, c ->
                val theirs = s.mixer.channels.getOrNull(i)
                if (i in drumChannels && theirs != null) c.copy(volume = theirs.volume, pan = theirs.pan, inserts = theirs.inserts) else c
            }),
        )
        return (if (style.id in DNB_KIT_STYLES) StudioDrums.withDnbKit(made.copy(harmony = harmony)).copy(harmony = made.harmony) else made).sanitized()
    }

    companion object {
        /** The styles whose tutorial starts on the drum & bass kit. */
        val DNB_KIT_STYLES = setOf("jump_up")
        /** A line this long (4 bars of 16ths) or longer is a phrase written once, then copied. */
        const val MOTIF_MIN_STEPS = 64
        private val cache = ConcurrentHashMap<String, TutoTarget>()
        fun of(style: String): TutoTarget? = StarterWizard.style(style)?.let { s -> cache.getOrPut(style) { TutoTarget(s) } }
    }
}

/** « Écouter l'exemple »: [after] heard, after [before] when the step compares two parts. */
class TutoExample(val before: Project?, val after: Project)

/**
 * A block of the song's map: a part's [label], its [bars] and its [energy] (0..1: how many of its
 * bricks play) at its start, [energyEnd] at its end (the Montée climbs to its Drop).
 */
data class TutoMapBlock(val label: String, val bars: Int, val energy: Float, val energyEnd: Float = energy)

/** What a step needs before the user can do it ([TutoChecks.need]); the panel's main button meets it. */
sealed class TutoNeed {
    /** The drum loop at least [bars] bars long (« Passer à 2 mesures »). */
    data class Bars(val bars: Int) : TutoNeed()
    /** A track for [role] ("bass", "pad", "lead", "sub"): « Créer la piste Basse ». */
    data class Track(val role: String) : TutoNeed()
    /** The part [label], made from the Groove as the step says: « Créer la partie « Intro » ». */
    data class Part(val label: String) : TutoNeed()
    /** The main loop named [label]. */
    data class Name(val label: String) : TutoNeed()
    /** Each part of the song its advised length. */
    data object Lengths : TutoNeed()
    /** The part [label] shown (the screens edit it). */
    data class Scene(val label: String) : TutoNeed()
    /**
     * « Copier sur les mesures suivantes »: what the user wrote of [role]'s line (its first 4 bars,
     * the hats' first bar) repeated over the next bars (testers, lot « tuto 4 »: the creative
     * gesture is the user's, the copying a button).
     */
    data class Copy(val role: String) : TutoNeed()
}

/** What a frame of « Montre-moi » sounds: pad [lane], or [pitches] on track [track]. */
data class TutoSound(val lane: Int? = null, val track: Int? = null, val pitches: List<Int> = emptyList())

/** « Montre-moi »: [frames] shown one after the other, each with its [sounds] entry (null: silent). */
class TutoDemo(val frames: List<Project>, val sounds: List<TutoSound?>)

/**
 * The detectors of the steps ([done], tolerant: a pattern close enough passes), « Montre-moi »
 * ([show]: the step's target written into the project, null when the step has none) and the
 * examples ([example]: sandboxes made from the target, the user's project untouched).
 */
object TutoChecks {
    /** The pads of each drum role ([StarterWizard.Role]'s lanes; the open hat counts as a hat). */
    private val ROLE_LANES = mapOf("kick" to setOf(0, 1), "snare" to setOf(2, 3), "clap" to setOf(4), "hat" to setOf(5, 6))
    private val DRUM_ROLES = listOf("kick", "snare", "clap", "hat", "perc")
    const val CRASH_LANE = 12
    private const val LOW = 52
    /** The Montée's last cells left silent before the Drop (the gap step's). */
    private const val GAP_CELLS = 2
    /** How much a drawn curve must climb to « monter ». */
    private const val RISE = 0.2f
    /** The Guide's card the app marks done once the song is exported and its share sheet opened. */
    const val SHARED_CARD = "share.ready"
    /** The open hat's pad (« Charley ouvert »). */
    private const val OPEN_HAT_LANE = 6

    fun laneOf(role: String): Int = StarterWizard.Role.entries.firstOrNull { it.id == role }?.lane ?: 0

    fun roleOfLane(lane: Int): String = ROLE_LANES.entries.firstOrNull { lane in it.value }?.key ?: "perc"

    /** A signature of what a "changed:…" step watches (its value when the step began is the baseline). */
    fun signature(what: String, p: Project): Any? = when {
        what == "volumes" || what.startsWith("volumes:") -> p.mixer.channels.map { it.volume }
        what == "kit" -> p.drums.lanes.getOrNull(0)?.sample to p.padVoices[0]
        what.startsWith("synth:") -> trackFor(p, what.removePrefix("synth:").substringBefore(':'))?.let { id -> p.track(id).let { listOf(it.synth, it.plaits, it.engine) } }
        // The hits of every pad in the part named so (« change un détail » in the Drop 2).
        what.startsWith("drums:") -> sceneLabelled(p, what.removePrefix("drums:")).takeIf { it >= 0 }?.let { i -> sceneAt(p, i).drums.lanes.map { it.hits } }
        else -> null
    }

    /** The synth knobs a "changed:synth:<role>:<KNOB>+<KNOB>" step turns up, by their [SynthPatch] names. */
    private val KNOBS = mapOf(
        "CUTOFF" to SynthPatch.CUTOFF, "RESONANCE" to SynthPatch.RESONANCE, "OSC2_DETUNE" to SynthPatch.OSC2_DETUNE,
        "GLIDE" to SynthPatch.GLIDE, "A_ATTACK" to SynthPatch.A_ATTACK, "UNISON_DETUNE" to SynthPatch.UNISON_DETUNE,
    )

    /** How much a knob must go up to count (or be near its top already). */
    private const val KNOB_STEP = 0.05f

    /** The knobs of a "changed:synth:…" check (empty: any change counts) and its role. */
    private fun knobsOf(check: String): Pair<String, List<Int>> {
        val spec = check.removePrefix("changed:synth:")
        return spec.substringBefore(':') to spec.substringAfter(':', "").split('+').mapNotNull { KNOBS[it.trim()] }
    }

    /** The tracks' strips a "changed:volumes:pad,lead" step lowers (the roles that have a track). */
    private fun volumeChannels(p: Project, check: String): List<Int> =
        check.removePrefix("changed:volumes").removePrefix(":").split(',').filter { it.isNotBlank() }
            .mapNotNull { trackFor(p, it.trim())?.let(InstrumentTrack::mixerChannel) }

    /** The strips a "send:<bus>" step sends: the reverb's the « Accords » track and the snare's pad, the echo's the lead. */
    private fun sendChannels(p: Project, bus: Int): List<Int> =
        if (bus == 0) listOfNotNull(trackFor(p, "pad")?.let(InstrumentTrack::mixerChannel), Mixer.FIRST_PAD + laneOf("snare"))
        else listOfNotNull(trackFor(p, "lead")?.let(InstrumentTrack::mixerChannel))

    /** The tracks' strips an "insert:<EFFECT>" step puts its effect on: each role of [TutoStep.target] ("sub,bass") that has a track. */
    private fun insertChannels(p: Project, step: TutoStep): List<Int> =
        step.target.ifEmpty { "bass" }.split(',').mapNotNull { trackFor(p, it.trim())?.let(InstrumentTrack::mixerChannel) }

    private fun hasInsert(p: Project, ch: Int, effect: String): Boolean =
        p.mixer.channels.getOrNull(ch)?.inserts?.any { !it.bypass && it.effect.name == effect } == true

    private fun sends(p: Project, ch: Int, bus: Int): Boolean = (p.mixer.channels.getOrNull(ch)?.sends?.getOrNull(bus) ?: 0f) > 0.05f

    /**
     * The strips a send or an effect step still has to do, in the order its text names them (testers,
     * lot « tuto 4 »: the reverb's « Accords » done, « Je le fais » went back there, never to the
     * snare); empty when done or for another kind of step. « Je le fais » goes to the first.
     */
    fun channelsToDo(step: TutoStep, p: Project): List<Int> {
        val c = step.check
        val arg = c.substringAfter(':', "")
        return when {
            c.startsWith("send:") -> arg.toIntOrNull()?.let { bus -> sendChannels(p, bus).filterNot { sends(p, it, bus) } }.orEmpty()
            c.startsWith("insert:") -> insertChannels(p, step).filterNot { hasInsert(p, it, arg) }
            else -> emptyList()
        }
    }

    // ---- what the user's project holds ----

    /** Scene [i] as it is now (the one shown: its live content). */
    private fun sceneAt(p: Project, i: Int): Scene = if (i == p.scene) p.currentSceneContent() else p.allScenes[i]

    /** Track [id]'s notes in scene [i] only. */
    private fun sceneNotes(p: Project, i: Int, id: Int): List<Note> =
        if (i == p.scene) p.track(id).notes.notes else p.allScenes.getOrNull(i)?.let { s -> (if (id == 0) s.synthNotes else s.trackNotes[id])?.notes }.orEmpty()

    private fun notesOf(p: Project, id: Int): List<Note> =
        p.allScenes.flatMap { s -> (if (id == 0) s.synthNotes else s.trackNotes[id])?.notes.orEmpty() }
            .ifEmpty { p.track(id).notes.notes }

    private fun median(notes: List<Note>): Int = notes.map { it.pitch }.sorted().let { it[it.size / 2] }

    /** What track [id] plays: "bass", "sub", "pad", "lead" by its name, else by its notes; null with neither. */
    fun roleOf(p: Project, id: Int): String? {
        val t = p.track(id)
        val name = t.name.lowercase()
        when {
            "sub" in name -> return "sub"
            listOf("bass", "basse", "808").any { it in name } -> return "bass"
            listOf("accord", "nappe", "pad", "chord").any { it in name } -> return "pad"
            listOf("mélodie", "melod", "lead", "hoover").any { it in name } -> return "lead"
        }
        val notes = notesOf(p, id).ifEmpty { return null }
        val chords = notes.groupBy { it.start }.values.any { it.size >= 2 }
        return when {
            median(notes) < LOW -> "bass"
            chords -> "pad"
            else -> "lead"
        }
    }

    /** The track playing [role] (one with notes first), null when none does. */
    fun trackFor(p: Project, role: String): Int? {
        val ids = p.instrumentTracks.map { it.id }.filter { roleOf(p, it) == role }
        return ids.firstOrNull { notesOf(p, it).isNotEmpty() } ?: ids.firstOrNull()
    }

    /** What plays in scene [s]: its pads with hits, its tracks with notes, the 303. */
    fun bricks(s: Scene): Set<String> = buildSet {
        s.drums.lanes.forEachIndexed { i, l -> if (l.hits.keys.any { it < s.drums.steps }) add("pad:$i") }
        if (s.synthNotes.notes.isNotEmpty()) add("inst:0")
        s.trackNotes.forEach { (id, c) -> if (c.notes.isNotEmpty()) add("inst:$id") }
        if (s.acid.steps.take(s.acid.length.coerceAtLeast(0)).any { it.gate }) add("acid")
    }

    private fun labelIs(label: String, wanted: String): Boolean {
        val l = label.trim()
        return l.equals(wanted, ignoreCase = true) || l.equals(Scene.shownLabel(wanted), ignoreCase = true) ||
            (wanted == "Fin" && l.equals("Outro", ignoreCase = true))
    }

    private fun sceneLabelled(p: Project, label: String): Int = p.allScenes.indexOfFirst { labelIs(it.label, label) && !it.isEmpty }

    /** The user's main loop: the scene named Groove, else the one where the most plays. */
    private fun grooveOf(p: Project): Int {
        val all = p.allScenes
        return sceneLabelled(p, "Groove").takeIf { it >= 0 } ?: all.indices.maxByOrNull { bricks(all[it]).size } ?: 0
    }

    /**
     * The hits of a drum role in [d]: EVERY dotted cell (the target's line over the whole loop,
     * as « Je le fais » shows it) holds a hit, a few more tolerated (testers, 2026-10-28: the hats
     * ticked at 7 cells of 9, the last two never made). The loop must be as long as the line first
     * (« Passer à 2 mesures »), in Facile as in Pro: its second bar's cells are cells to fill too.
     */
    private fun laneMatches(d: DrumPattern, lanes: Set<Int>, line: StarterWizard.Line, perBar: Int): Boolean {
        if (d.steps < line.pattern.length.coerceAtMost(4 * perBar.coerceAtLeast(1))) return false
        val wanted = StarterWizard.hits(line, d.steps).keys.ifEmpty { return false }
        val mine = lanes.flatMap { l -> d.lanes.getOrNull(l)?.hits?.keys.orEmpty().filter { it < d.steps } }.toSet()
        return mine.containsAll(wanted) && (mine - wanted).size <= maxOf(1, wanted.size / 2)
    }

    /** The notes [mine] hold every one of [wanted] (its pitch, its start): the dotted notes all written. */
    private fun notesMatch(mine: List<Note>, wanted: List<Note>): Boolean {
        if (wanted.isEmpty()) return false
        val have = mine.map { it.pitch to it.start }.toSet()
        return wanted.all { (it.pitch to it.start) in have }
    }

    /**
     * Each bar of a pattern whose 4 hats on the beats are all softer (by 0.15) than the 4 between
     * them: true, false (a bar of hats that does not swing), in bar order.
     */
    private fun hatBars(d: DrumPattern, perBar: Int): List<Boolean> {
        val beat = (perBar / 4).coerceAtLeast(2)
        val v = HashMap<Int, Float>()
        lanesOf("hat").forEach { l -> d.lanes.getOrNull(l)?.hits?.forEach { (s, h) -> if (s < d.steps) v[s] = maxOf(v[s] ?: 0f, h.velocity) } }
        return (0 until d.steps step (4 * beat)).map { bar ->
            val on = (0 until 4).map { bar + it * beat }
            val off = on.map { it + beat / 2 }
            on.all { it in v } && off.all { it in v } && on.maxOf { v.getValue(it) } + 0.15f <= off.minOf { v.getValue(it) }
        }
    }

    /** Every bar of the pattern swings (testers, lot « tuto 4 »: the first bar alone was ticked, the second kept its loud beats). */
    private fun hatsSwing(d: DrumPattern, perBar: Int): Boolean = hatBars(d, perBar).let { it.isNotEmpty() && it.all { b -> b } }

    private fun lanesOf(role: String) = ROLE_LANES[role] ?: setOf(laneOf(role))

    /** Whether [step] is done in [p] ([baseline]: the [signature] when it began, for "changed:…"). */
    fun done(step: TutoStep, p: Project, target: TutoTarget, baseline: Any? = null): Boolean {
        val c = step.check
        val arg = c.substringAfter(':', "")
        return when {
            step.manual || step.listening -> false
            c == "tempo" -> kotlin.math.abs(p.tempoBpm - target.style.bpm) <= 2.0
            c == "key" -> p.harmony.root == target.harmony.root && p.harmony.scale == target.harmony.scale
            c.startsWith("lane:") -> target.line(arg)?.let { line -> p.allScenes.any { laneMatches(it.drums, lanesOf(arg), line, p.stepsPerBar) } } ?: false
            // The 4 hats on the beats softer than those between them (one hit made « Doux » is not the groove).
            c == "velocity:hat" -> p.allScenes.any { hatsSwing(it.drums, p.stepsPerBar) }
            // Every dotted note written (not 3 of 12: the testers' bass and chords ticked with the screen's first notes).
            c.startsWith("notes:") -> Part.entries.firstOrNull { it.id == arg }?.let { part ->
                trackFor(p, arg)?.let { id -> notesMatch(notesOf(p, id), target.notes(part).notes) }
            } ?: false
            c == "track:sub" -> trackFor(p, "bass")?.let { bass ->
                val wanted = StarterWizard.subNotes(notesOf(p, bass))
                p.instrumentTracks.any { t -> t.id != bass && roleOf(p, t.id) == "sub" && notesMatch(notesOf(p, t.id), wanted) }
            } ?: false
            // On each track the step names (the pump on the Sub and the Basse), not anywhere in the mix.
            c.startsWith("insert:") -> insertChannels(p, step).let { chans -> chans.isNotEmpty() && chans.all { hasInsert(p, it, arg) } }
            // Every strip the step names sends (the « Accords » track AND the snare), not any strip.
            c.startsWith("send:") -> arg.toIntOrNull()?.let { bus -> sendChannels(p, bus).let { chans -> chans.isNotEmpty() && chans.all { sends(p, it, bus) } } } ?: false
            // A curve drawn that rises over the Montée (b/280: « Par partie » touched, no curve, ticked).
            c == "automation" -> riserDrawn(p, step.target.ifEmpty { "pad" })
            c == "master" -> p.mixer.master.any { it.effect != EffectType.NONE && !it.bypass }
            // The master's « Volume visé » (its limiter, always last, brings the song there).
            c.startsWith("loudness:") -> p.mixer.target.name == arg
            // The song exported and its share sheet opened (the app marks the Guide's « Prêt à partager ? » then), not « C'est fait ».
            c == "shared" -> p.guide.statusOf(SHARED_CARD) == GuideStatus.DONE
            // Each track the step names a little lower than when the step began (the testers' mix ticked with one of two).
            c.startsWith("changed:volumes:") -> (baseline as? List<*>)?.let { base ->
                volumeChannels(p, c).let { chans ->
                    chans.isNotEmpty() && chans.all { ch -> (base.getOrNull(ch) as? Float)?.let { p.mixer.channels.getOrNull(ch)?.volume?.let { v -> v < it - 0.005f } } == true }
                }
            } ?: false
            // The knobs the step names turned up since it began (not the distortion the synth has none of).
            c.startsWith("changed:synth:") && knobsOf(c).second.isNotEmpty() -> {
                val (role, knobs) = knobsOf(c)
                val before = (baseline as? List<*>)?.firstOrNull() as? SynthPatch
                val id = trackFor(p, role)
                before != null && id != null && knobs.all { k -> p.track(id).synth.get(k).let { it >= before.get(k) + KNOB_STEP || it >= 0.95f } }
            }
            c.startsWith("changed:") -> baseline != null && signature(arg, p) != baseline
            c.startsWith("label:") -> sceneLabelled(p, arg) >= 0
            c.startsWith("part:") -> if (writesBass(step)) newBassDone(p, step, target) else partDone(p, c.removePrefix("part:"))
            c == "song:order" -> songOrdered(p, step.song)
            // Each part its advised length (all at 8 bars, as « Ajouter » makes them, is not the step done).
            c == "song:phrases" -> phrasesDone(p, step.song)
            // A roll on the snare in the Montée's last bar (a Drop 2 copied from the example rolls too: it does not count),
            // before its last two cells: those are the silence of the next step (testers, lot « tuto 4 »: the roll made, then erased).
            c == "fill" -> sceneLabelled(p, "Montée").takeIf { it >= 0 }?.let { i ->
                val d = sceneAt(p, i).drums
                val per = p.stepsPerBar.coerceAtLeast(1)
                lanesOf("snare").any { l -> d.lanes.getOrNull(l)?.hits?.any { (s, h) -> s in (d.steps - per) until d.steps - GAP_CELLS && h.repeats > 1 } == true }
            } ?: false
            c == "crash" -> p.allScenes.any { s -> (labelIs(s.label, "Drop") || labelIs(s.label, "Drop 2")) && s.drums.lanes.getOrNull(CRASH_LANE)?.hits?.containsKey(0) == true }
            c == "gap" -> sceneLabelled(p, "Montée").takeIf { it >= 0 }?.let { i ->
                val d = sceneAt(p, i).drums
                d.lanes.none { l -> l.hits.keys.any { it in d.steps - GAP_CELLS until d.steps } }
            } ?: false
            else -> false
        }
    }

    /** "Intro:less", "Montée:nokick", "Drop:full", "Drop 2:differs". */
    private fun partDone(p: Project, spec: String): Boolean {
        val label = spec.substringBeforeLast(':')
        val mode = spec.substringAfterLast(':')
        val i = sceneLabelled(p, label).takeIf { it >= 0 } ?: return false
        val all = p.allScenes
        val mine = bricks(all[i])
        val most = all.filterIndexed { j, _ -> j != i }.maxOfOrNull { bricks(it).size } ?: 0
        return when (mode) {
            "less" -> mine.isNotEmpty() && mine.size < most
            "nokick" -> mine.isNotEmpty() && lanesOf("kick").none { "pad:$it" in mine }
            "full" -> mine.size >= most && "pad:0" in mine
            // Yanis (lot « tuto 4 »): a jump up Drop empties the top for the bass, the « Accords » silent.
            "drop" -> "pad:0" in mine && listOf("bass").all { r -> trackFor(p, r)?.let { "inst:$it" in mine } == true } &&
                trackFor(p, "pad")?.let { "inst:$it" !in mine } != false
            "differs" -> sceneLabelled(p, "Drop").let { d -> d >= 0 && d != i && all[d].copy(name = "", label = "") != all[i].copy(name = "", label = "") }
            else -> false
        }
    }

    /**
     * The Drop 2's own bass, written by the user (Yanis, lot « tuto 4 »: the jump up's first creative
     * gesture was the button's): in the part, every note of the course's bass moved by
     * [TutoStep.transpose], and the Sub an octave under it when there is one.
     */
    private fun newBassDone(p: Project, step: TutoStep, target: TutoTarget): Boolean {
        val i = sceneLabelled(p, partLabel(step)).takeIf { it >= 0 } ?: return false
        val wanted = movedBass(step, target)
        val bass = trackFor(p, "bass") ?: return false
        if (!notesMatch(sceneNotes(p, i, bass), wanted)) return false
        val sub = p.instrumentTracks.firstOrNull { it.id != bass && roleOf(p, it.id) == "sub" }?.id ?: return true
        return notesMatch(sceneNotes(p, i, sub), StarterWizard.subNotes(wanted))
    }

    /** The course's bass moved by the step's [TutoStep.transpose] (the Drop 2's). */
    private fun movedBass(step: TutoStep, target: TutoTarget): List<Note> =
        target.notes(Part.Bass).notes.map { it.copy(pitch = (it.pitch + step.transpose).coerceIn(0, 127)) }

    private fun partLabel(step: TutoStep): String = step.check.removePrefix("part:").substringBeforeLast(':')

    /** A part step whose bass the user writes ([TutoStep.transpose]: the Drop 2's new bass). */
    private fun writesBass(step: TutoStep): Boolean = step.check.startsWith("part:") && step.transpose != 0

    /** A curve on [role]'s filter cutoff that rises over a Montée (its part's own curve, or the song's over its bars). */
    private fun riserDrawn(p: Project, role: String): Boolean {
        val ch = trackFor(p, role)?.let(InstrumentTrack::mixerChannel) ?: return false
        val montee = sceneLabelled(p, "Montée")
        val per = p.stepsPerBar.coerceAtLeast(1)
        var bar = 0
        val spans = p.song.mapNotNull { part ->
            val bars = p.partSteps(part) / per
            val at = bar
            bar += bars
            (at to bars).takeIf { labelIs(p.allScenes.getOrNull(part.scene)?.label.orEmpty(), "Montée") }
        }
        fun rises(points: List<Float?>): Boolean = points.filterNotNull().let { it.size >= 2 && it.last() >= it.first() + RISE }
        return p.automation.any { l ->
            val t = l.target
            val cutoff = t.ch == ch && ((t.kind == AutoTarget.INSERT_PARAM && t.param == 0 &&
                p.mixer.channels.getOrNull(ch)?.inserts?.getOrNull(t.slot)?.effect == EffectType.FILTER) ||
                (t.kind == AutoTarget.SYNTH && t.param == SynthPatch.CUTOFF))
            cutoff && when (l.mode) {
                AutomationLane.PER_SCENE -> montee >= 0 && rises(l.sceneValues[montee].orEmpty())
                AutomationLane.SONG -> spans.any { (at, bars) -> rises(l.songValues.drop(at).take(bars + 1)) }
                else -> false
            }
        }
    }

    /** The bars [spec] ("Intro:32,Montée:8,…") gives each label. */
    private fun barsOf(spec: String): Map<String, Int> =
        spec.split(',').filter { ':' in it }.associate { e -> e.substringBeforeLast(':').trim() to (e.substringAfterLast(':').toIntOrNull() ?: 0) }

    /** Each part of the song as long as [spec] advises for its label, the others in phrases of 8 bars. */
    private fun phrasesDone(p: Project, spec: String): Boolean {
        if (p.song.size < 3) return false
        val bars = barsOf(spec)
        val per = p.stepsPerBar.coerceAtLeast(1)
        return p.song.all { part ->
            val label = p.allScenes.getOrNull(part.scene)?.label.orEmpty()
            val b = p.partSteps(part) / per
            bars.entries.firstOrNull { labelIs(label, it.key) }?.let { b == it.value } ?: (b % 8 == 0)
        }
    }

    /**
     * Every part [spec] names, in its order (testers, lot « tuto 4 »: ticked at 5 parts of 7, without
     * the Drop 2 nor the Fin); a part it does not name (the Groove) may sit in between.
     */
    private fun songOrdered(p: Project, spec: String): Boolean {
        val wanted = barsOfList(spec).map { it.first }
        if (wanted.size < 3) return false
        val labels = p.song.map { p.allScenes.getOrNull(it.scene)?.label.orEmpty() }.filter { l -> wanted.any { labelIs(l, it) } }
        return labels.size == wanted.size && labels.zip(wanted).all { (l, w) -> labelIs(l, w) }
    }

    /** The parts [spec] ("Intro:32,Montée:8,…") lays out, in order, with their bars. */
    private fun barsOfList(spec: String): List<Pair<String, Int>> =
        spec.split(',').filter { ':' in it }.map { e -> e.substringBeforeLast(':').trim() to (e.substringAfterLast(':').toIntOrNull() ?: 0) }

    // ---- « Montre-moi » ----

    /** [p] with [step]'s target in it (one undo step in the app), null when the step has none. */
    fun show(step: TutoStep, p: Project, target: TutoTarget): Project? {
        val c = step.check
        val arg = c.substringAfter(':', "")
        // « Montre-moi » of the kick's step: the other kick (a long one when the drum & bass kick is on, else that one).
        if (step.demo == "kick") {
            val other = if (StudioDrums.isDnbKick(p.padVoices[0])) p.copy(padVoices = p.padVoices - 0) else StudioDrums.withDnbKit(p)
            return other.sanitized().takeIf { it != p.sanitized() }
        }
        val shown: Project = when {
            c == "tempo" -> p.copy(tempoBpm = target.style.bpm)
            c == "key" -> p.copy(harmony = target.harmony)
            c.startsWith("lane:") -> {
                val line = target.line(arg) ?: return null
                val wide = withSteps(p, line.pattern.length)
                val lane = laneOf(arg)
                wide.copy(drums = wide.drums.copy(lanes = wide.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = StarterWizard.hits(line, wide.drums.steps)) else l }))
            }
            c == "velocity:hat" -> {
                val line = target.line("hat") ?: return null
                val v = StarterWizard.hits(line.pattern)
                val lane = laneOf("hat")
                val hits = p.drums.lanes[lane].hits.ifEmpty { StarterWizard.hits(line, p.drums.steps) }
                p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
                    if (i != lane) l else l.copy(hits = hits.mapValues { (s, h) -> h.copy(velocity = v[s % line.pattern.length]?.velocity ?: 0.35f) })
                }))
            }
            c.startsWith("notes:") -> {
                val part = Part.entries.firstOrNull { it.id == arg } ?: return null
                val (q, id) = ensureTrack(p, part, target) ?: return null
                q.withTrack(q.track(id).copy(notes = target.notes(part)))
            }
            c == "track:sub" -> withSub(p, target) ?: return null
            c.startsWith("insert:") -> {
                val type = EffectType.entries.firstOrNull { it.name == arg } ?: return null
                channelsToDo(step, p).fold(p) { q, ch -> withInsert(q, ch, Insert(type.ordinal, type.defaults)) ?: q }
            }
            c.startsWith("loudness:") -> LoudnessTarget.entries.firstOrNull { it.name == arg }?.let { p.copy(mixer = p.mixer.copy(target = it)) } ?: return null
            c.startsWith("changed:drums:") -> inScene(p, c.removePrefix("changed:drums:")) { q ->
                // A variation: an open hat on the second to last cell.
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == OPEN_HAT_LANE) l.copy(hits = l.hits + ((q.drums.steps - 2) to Hit(0.8f))) else l }))
            } ?: return null
            c.startsWith("send:") -> {
                val bus = arg.toIntOrNull() ?: return null
                val chans = sendChannels(p, bus)
                if (chans.isEmpty()) return null
                withChannels(p, chans) { it.copy(sends = it.sends.mapIndexed { b, v -> if (b == bus) maxOf(v, if (bus == 0) 0.3f else 0.25f) else v }) }
            }
            c == "automation" -> riser(p, trackFor(p, "pad") ?: trackFor(p, "lead") ?: return null) ?: return null
            c == "master" -> p.copy(mixer = p.mixer.copy(master = target.song.mixer.master))
            c == "changed:volumes" -> withChannels(p, listOfNotNull(trackFor(p, "pad"), trackFor(p, "lead")).map(InstrumentTrack::mixerChannel)) { it.copy(volume = it.volume * 0.8f) }
            c.startsWith("changed:volumes:") -> volumeChannels(p, c).takeIf { it.isNotEmpty() }?.let { chans -> withChannels(p, chans) { it.copy(volume = it.volume * 0.8f) } } ?: return null
            // The knobs the step names, turned up a third.
            c.startsWith("changed:synth:") && knobsOf(c).second.isNotEmpty() -> {
                val (role, knobs) = knobsOf(c)
                val id = trackFor(p, role) ?: return null
                val t = p.track(id)
                p.withTrack(t.copy(synth = knobs.fold(t.synth) { s, k -> s.with(k, (s.get(k) + 0.3f).coerceAtMost(1f)) }))
            }
            c.startsWith("label:") -> p.renameScene(grooveOf(p), arg)
            c.startsWith("part:") -> makePart(p, step, target)?.let { q ->
                // The Drop 2's own bass (the user's to write), then its Sub, shown in the part.
                if (!writesBass(step)) q
                else trackFor(q, "bass")?.let { b -> q.withTrack(q.track(b).copy(notes = q.track(b).notes.copy(notes = movedBass(step, target)))) }?.let { withSub(it, target) ?: it } ?: q
            } ?: return null
            c.startsWith("song:") -> song(p, step.song) ?: return null
            c == "fill" -> inScene(p, "Montée") { q ->
                val lane = laneOf("snare")
                val n = q.drums.steps
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l ->
                    if (i != lane) l else l.copy(hits = l.hits.filterKeys { it < n - 8 } + (n - 8 until n - 2).associateWith { Hit(0.6f + 0.06f * (it - n + 8), repeats = if (it >= n - 4) 3 else 2) })
                }))
            } ?: return null
            c == "crash" -> inScene(p, "Drop") { q ->
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == CRASH_LANE) l.copy(hits = l.hits + (0 to Hit(1f))) else l }))
            } ?: return null
            c == "gap" -> inScene(p, "Montée") { q ->
                val n = q.drums.steps
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.map { l -> l.copy(hits = l.hits.filterKeys { it < n - 2 }) }))
            } ?: return null
            else -> return null
        }
        return shown.sanitized().takeIf { it != p.sanitized() }
    }

    /**
     * The Sub an octave under the bass of the part shown (« Copier la basse une octave plus bas »):
     * the Sub track made by the step's button, else a new one; null without a bass to copy.
     */
    private fun withSub(p: Project, target: TutoTarget): Project? {
        val bass = trackFor(p, "bass") ?: return null
        val notes = p.track(bass).notes.takeIf { it.notes.isNotEmpty() } ?: return null
        val (q, id) = p.instrumentTracks.firstOrNull { it.id != bass && roleOf(p, it.id) == "sub" }?.let { p to it.id } ?: partTrack(p, "sub", target) ?: return null
        return q.withTrack(q.track(id).copy(name = "Sub", notes = q.track(id).notes.withSteps(notes.steps).copy(notes = StarterWizard.subNotes(notes.notes))))
    }

    /**
     * « Copier sur les mesures suivantes » of [role]: the line's first half (4 bars) written again
     * over its second half (the notes there kept), or the hats' first bar's forces on every bar.
     */
    private fun copied(p: Project, role: String): Project? {
        if (role == "hat") {
            val per = p.stepsPerBar.coerceAtLeast(1)
            val lanes = lanesOf("hat")
            return p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
                if (i !in lanes) l else l.copy(hits = l.hits.mapValues { (s, h) -> l.hits[s % per]?.let { h.copy(velocity = it.velocity) } ?: h })
            })).takeIf { it != p }
        }
        val id = trackFor(p, role) ?: return null
        val clip = p.track(id).notes
        val half = clip.ticks / 2
        val first = clip.notes.filter { it.start < half }.ifEmpty { return null }
        val have = clip.notes.map { it.pitch to it.start }.toSet()
        val copies = first.map { it.copy(start = it.start + half, played = null) }.filter { (it.pitch to it.start) !in have }
        return p.withTrack(p.track(id).copy(notes = clip.copy(notes = clip.notes + copies).sanitized())).takeIf { copies.isNotEmpty() }
    }

    /** [p] with its drum loop at least [steps] long, each pad's hits repeated over the new bars. */
    private fun withSteps(p: Project, steps: Int): Project {
        val d = p.drums
        if (d.steps >= steps) return p
        val n = d.steps.coerceAtLeast(1)
        return p.copy(drums = d.copy(steps = steps, lanes = d.lanes.map { l ->
            l.copy(hits = (0 until steps).mapNotNull { s -> l.hits[s % n]?.let { s to it } }.toMap())
        }))
    }

    /** [p] with track [part] (an existing one, else a new one: [partTrack]), and its id. */
    private fun ensureTrack(p: Project, part: Part, target: TutoTarget): Pair<Project, Int>? =
        trackFor(p, part.id)?.let { p to it } ?: partTrack(p, part.id, target)

    /**
     * The first track still as a new project has it (its default name, no note anywhere): the
     * tutorial hides it until a step makes it the song's first instrument track.
     */
    fun blankFirst(p: Project): Boolean = p.track(0).name == Project().synthTrackName && notesOf(p, 0).isEmpty()

    /**
     * A new, empty track for [role] ("bass", "pad", "lead", "sub") with the style's sound and level
     * as the assistant gives them, its notes as long as the target's (the blank first track becomes
     * it when it is still blank); null when no track is free.
     */
    fun partTrack(p: Project, role: String, target: TutoTarget): Pair<Project, Int>? {
        val part = if (role == "sub") Part.Bass else Part.entries.firstOrNull { it.id == role } ?: return null
        val kind = target.kind(part)
        val added = NewTracks.add(p, kind, target.style) ?: return null
        val new = added.tracks.first { n -> p.tracks.none { it.id == n.id } }
        val steps = if (role == "sub") trackFor(p, "bass")?.let { p.track(it).notes.steps } ?: 16 else target.notes(part).steps
        val made = new.copy(name = if (role == "sub") "Sub" else new.name, notes = new.notes.withSteps(steps).copy(notes = emptyList()))
        // The bass's own effect, as the assistant's song has it (jump up: « Growl », its distortion and its
        // three-band squash; measured: without it the growl synth alone is dull, 0.9 % of its energy over 2 kHz, 6.9 % with it).
        val fx = target.style.effects.firstOrNull { it.target == role && it.preset.isNotEmpty() }?.let { FxPresets.byId(it.preset) }
        val withFx = if (fx == null) added else withChannels(added, listOf(InstrumentTrack.mixerChannel(made.id))) { it.withFx(fx).sanitized() }
        if (!blankFirst(p)) return withFx.withTrack(made) to made.id
        // The blank first track takes its place (same sound, same level), the slot of the new one stays free.
        val level = withFx.mixer.channels[InstrumentTrack.mixerChannel(made.id)]
        val first = p.withTrack(made.copy(id = 0, color = p.track(0).color))
        return withChannels(first, listOf(InstrumentTrack.mixerChannel(0))) { level } to 0
    }

    private fun withChannels(p: Project, chans: List<Int>, f: (Channel) -> Channel): Project =
        p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, ch -> if (i in chans) f(ch) else ch }))

    /** [insert] in the first free slot of channel [ch] (null when it already has one of its kind or no slot is free). */
    private fun withInsert(p: Project, ch: Int, insert: Insert): Project? {
        val inserts = p.mixer.channels.getOrNull(ch)?.inserts ?: return null
        if (inserts.any { it.type == insert.type }) return null
        val slot = inserts.indexOfFirst { it.effect == EffectType.NONE }.takeIf { it >= 0 } ?: return null
        return withChannels(p, listOf(ch)) { it.copy(inserts = inserts.toMutableList().also { l -> l[slot] = insert }) }
    }

    /** A filter on track [id] that opens over each Montée of the song (its first 8 bars without a song). */
    private fun riser(p: Project, id: Int): Project? {
        val ch = InstrumentTrack.mixerChannel(id)
        val withFilter = p.mixer.channels.getOrNull(ch)?.inserts?.indexOfFirst { it.effect == EffectType.FILTER }?.takeIf { it >= 0 }?.let { p to it }
            ?: withInsert(p, ch, Insert(EffectType.FILTER.ordinal, listOf(1f, 0.25f, 0f, 1f)))?.let { q -> q to q.mixer.channels[ch].inserts.indexOfFirst { it.effect == EffectType.FILTER } }
            ?: return null
        val (q, slot) = withFilter
        var lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, 0), mode = AutomationLane.SONG)
        var bar = 0
        val per = q.stepsPerBar.coerceAtLeast(1)
        val spans = q.song.mapNotNull { part ->
            val bars = q.partSteps(part) / per
            val at = bar
            bar += bars
            (at to bars).takeIf { labelIs(q.allScenes.getOrNull(part.scene)?.label.orEmpty(), "Montée") }
        }.ifEmpty { listOf(0 to 8) }
        for ((at, bars) in spans) lane = lane.withSongPoint(at, 0.15f).withSongPoint(at + bars - 1, 1f)
        val kept = q.automation.filterNot { it.target == lane.target }
        return q.copy(automation = (kept + lane).take(AutomationLane.MAX_LANES))
    }

    /** [edit] made in the scene named [label] (shown, then back on the scene shown before). */
    private fun inScene(p: Project, label: String, edit: (Project) -> Project): Project? {
        val i = sceneLabelled(p, label).takeIf { it >= 0 } ?: return null
        return edit(p.switchScene(i)).switchScene(p.scene)
    }

    /**
     * A part [step] makes: the Groove copied into the scene named after it (a new one, or its own
     * when it exists), only the bricks of [TutoStep.keep] kept ("*": all), the drums of the
     * target's part [TutoStep.lanesFrom] on the pads it keeps.
     */
    private fun makePart(p: Project, step: TutoStep, target: TutoTarget): Project? {
        val label = step.check.removePrefix("part:").substringBeforeLast(':')
        val groove = grooveOf(p)
        if (p.allScenes.getOrNull(groove)?.isEmpty != false) return null
        val dest = p.allScenes.indexOfFirst { labelIs(it.label, label) }.takeIf { it >= 0 && it != groove } ?: p.switchScene(groove).firstEmptyScene() ?: return null
        var q = p.switchScene(groove).copySceneTo(dest).renameScene(dest, label).switchScene(dest)
        val keep = step.keep.split(',').map { it.trim() }.toSet()
        val all = "*" in keep
        val from = step.lanesFrom.takeIf { it.isNotEmpty() }?.let { target.sceneOf(it) }?.takeIf { it >= 0 }?.let { target.song.allScenes[it].drums }
        if (from != null) q = withSteps(q, from.steps)
        val steps = q.drums.steps
        q = q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l ->
            val kept = all || roleOfLane(i) in keep
            val theirs = from?.lanes?.getOrNull(i)?.hits?.filterKeys { it < steps }.orEmpty()
            when {
                !kept -> l.copy(hits = emptyMap())
                theirs.isNotEmpty() -> l.copy(hits = theirs)
                else -> l
            }
        }))
        // A brick not kept is silent in the part; the Drop 2's bass ([TutoStep.transpose]) is left for the user to write.
        q.instrumentTracks.forEach { t ->
            val role = roleOf(q, t.id) ?: "lead"
            val kept = all || role in keep || (role == "sub" && "bass" in keep)
            q = q.withTrack(t.copy(notes = if (kept) t.notes else t.notes.copy(notes = emptyList())))
        }
        if (!all && "acid" !in keep) q = q.copy(acid = q.acid.copy(steps = q.acid.steps.map { it.copy(gate = false) }))
        return q
    }

    /** The song laid out from [spec] ("Intro:32,Montée:8,…"): each part named so, its bars; null when none is found. */
    private fun song(p: Project, spec: String): Project? {
        val parts = spec.split(',').mapNotNull { e ->
            val label = e.substringBeforeLast(':').trim()
            val bars = e.substringAfterLast(':').toIntOrNull() ?: return@mapNotNull null
            val i = sceneLabelled(p, label).takeIf { it >= 0 } ?: (if (label == "Fin") sceneLabelled(p, "Intro") else -1).takeIf { it >= 0 } ?: return@mapNotNull null
            SongPart(i, 1, bars = bars)
        }
        if (parts.size < 3) return null
        return p.copy(song = parts.take(SongPart.MAX_PARTS))
    }

    // ---- « Écouter l'exemple » ----

    /** What [step]'s example plays, made from [target] (never from the user's project). */
    fun example(step: TutoStep, target: TutoTarget): TutoExample? {
        val l = step.listen
        return when {
            l.isEmpty() -> null
            // An excerpt of the finished song: its Montée, then its Drop (the whole song's first bars are its Intro).
            l == "song" -> part(target, "Drop")?.let { TutoExample(part(target, "Montée"), it) } ?: TutoExample(null, target.song)
            l.startsWith("drums:") || l.startsWith("parts:") -> TutoExample(null, loop(target, l.substringAfter(':').split(',').toSet()))
            // The snare and the « Accords » track without, then with the reverb (Facile: the Mode simple has no send to touch).
            l == "fx:reverb" -> loop(target, setOf("kick", "snare", "pad")).let { dry ->
                TutoExample(dry, show(TutoStep("x", "", check = "send:0"), dry, target) ?: return null)
            }
            l.startsWith("part:") -> part(target, l.removePrefix("part:"))?.let { TutoExample(null, it) }
            l.startsWith("vs:") -> {
                val (a, b) = l.removePrefix("vs:").split(':').let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
                val after = part(target, b) ?: return null
                TutoExample(part(target, a), after)
            }
            else -> null
        }
    }

    /** The target's part named [label] alone, in a loop. */
    private fun part(target: TutoTarget, label: String): Project? {
        val i = target.sceneOf(label).takeIf { it >= 0 } ?: return null
        return target.song.copy(song = emptyList(), automation = emptyList()).switchScene(i).sanitized()
    }

    /** A loop of the target's [bricks] alone: drum roles and parts ("bass", "pad", "lead"). */
    fun loop(target: TutoTarget, bricks: Set<String>): Project {
        val steps = DRUM_ROLES.filter { it in bricks }.mapNotNull { target.line(it)?.pattern?.length }.maxOrNull() ?: 16
        val lanes = MutableList(DrumPattern.PAD_COUNT) { DrumLane() }
        for (role in DRUM_ROLES) if (role in bricks) target.line(role)?.let { lanes[laneOf(role)] = DrumLane(hits = StarterWizard.hits(it, steps)) }
        var p = Project(name = "", tempoBpm = target.style.bpm, harmony = target.harmony, drums = DrumPattern(steps = steps, lanes = lanes), kitVersion = Project.KIT_V3)
        for (part in listOf(Part.Bass, Part.Pad, Part.Lead)) if (part.id in bricks) p = show(TutoStep("x", "", check = "notes:${part.id}"), p, target) ?: p
        return p.sanitized()
    }

    /**
     * The map of the song: the parts the course has the user lay out (its "song:…" step: Intro 32,
     * Montée 8, Drop 32, Break 16, Montée 8, Drop 2 32, Fin 16), each with the energy of the target's
     * part of that name (testers: the map showed the example's ten parts, « Drop 2 » twice, a Fin of
     * 32 the course never asks for). Without such a step: the target's song, its parts in order.
     */
    fun map(target: TutoTarget): List<TutoMapBlock> {
        val song = target.song
        val all = song.allScenes
        val most = all.maxOfOrNull { bricks(it).size }?.coerceAtLeast(1) ?: 1
        val spec = TutoCourse.of(target.style.id)?.chapters?.flatMap { it.steps }?.firstOrNull { it.song.isNotEmpty() }?.song.orEmpty()
        val laid = spec.split(',').filter { ':' in it }.map { it.substringBeforeLast(':').trim() to (it.substringAfterLast(':').toIntOrNull() ?: 0) }
        if (laid.size >= 3) {
            val energy = laid.map { (label, _) ->
                all.firstOrNull { labelIs(it.label, if (label == "Fin") "Intro" else label) }?.let { bricks(it).size / most.toFloat() } ?: 0f
            }
            val drop = laid.indices.filter { labelIs(laid[it].first, "Drop") }.maxOfOrNull { energy[it] } ?: 1f
            // Yanis (lot « tuto 4 »): the curve fell in the Montée and the Drop 2 sat under the Drop. The Montée
            // climbs from the part before it towards its Drop; the Drop 2 has the Drop's energy, as its text says.
            return laid.mapIndexed { k, (label, bars) ->
                when {
                    labelIs(label, "Montée") -> TutoMapBlock(label, bars, energy.getOrNull(k - 1) ?: energy[k], drop * MONTEE_TOP)
                    labelIs(label, "Drop 2") -> TutoMapBlock(label, bars, drop)
                    else -> TutoMapBlock(label, bars, energy[k])
                }
            }
        }
        val sections = StarterWizard.songSections(song)
        var drops = 0
        return sections.mapIndexed { k, s ->
            // The later Drops play the second bass (« Drop 2 »'s, through the song's parts): named so on the map.
            val label = all.getOrNull(s.scene)?.label.orEmpty().let { if (k > 0 && k == sections.lastIndex && s.scene == sections.first().scene) "Fin" else it }
                .let { if (it == "Drop" && drops++ > 0) "Drop 2" else it }
            TutoMapBlock(label, s.bars, bricks(all[s.scene]).size / most.toFloat())
        }
    }

    /** Where the Montée's curve ends, under its Drop's energy. */
    private const val MONTEE_TOP = 0.85f

    // ---- what a step needs first, its demonstration, its ghost notes ----

    /** The bars a drum role's line lasts (the loop must be at least that long to hold it). */
    private fun barsOf(p: Project, line: StarterWizard.Line): Int = ceil(line.pattern.length / p.stepsPerBar.coerceAtLeast(1).toDouble()).toInt().coerceIn(1, 4)

    /** The part a step works in ("Montée" for the fill and the gap, "Drop" for the crash), null when none. */
    private fun sceneFor(step: TutoStep): String? = when {
        step.check == "fill" || step.check == "gap" || step.check == "automation" -> "Montée"
        step.check == "crash" -> "Drop"
        step.check.startsWith("changed:drums:") -> step.check.removePrefix("changed:drums:")
        writesBass(step) -> partLabel(step)
        else -> null
    }

    /** The notes [step] asks for first (the line's first half while it is not all written), null for a step with none. */
    private fun wantedNotes(step: TutoStep, p: Project, target: TutoTarget): List<Note>? {
        val c = step.check
        val all = when {
            c.startsWith("notes:") -> Part.entries.firstOrNull { it.id == c.removePrefix("notes:") }?.let { target.notes(it).notes } ?: return null
            writesBass(step) -> movedBass(step, target)
            else -> return null
        }
        val mine = mineFor(step, p) ?: return all
        val first = all.filter { it.start < halfOf(step, target) }
        return if (all.size > first.size && !notesMatch(mine, first)) first else all
    }

    /** Where the second half of [step]'s line starts, in ticks (its notes before it are written by hand). */
    private fun halfOf(step: TutoStep, target: TutoTarget): Int =
        target.notes(Part.entries.firstOrNull { "notes:${it.id}" == step.check } ?: Part.Bass).let { it.ticks / 2 }

    /** The notes the user has where [step] asks for them (its track; the Drop 2's bass in its part), null when no track. */
    private fun mineFor(step: TutoStep, p: Project): List<Note>? = when {
        step.check.startsWith("notes:") -> trackFor(p, step.check.removePrefix("notes:"))?.let { notesOf(p, it) }
        writesBass(step) -> sceneLabelled(p, partLabel(step)).takeIf { it >= 0 }?.let { i -> trackFor(p, "bass")?.let { sceneNotes(p, i, it) } }
        else -> null
    }

    /**
     * What [step] needs before the user can do it, null when nothing (or when it is done): the loop
     * long enough for its line, its track, its part, the part it works in. The panel offers it as
     * its main button ([fulfil] on a touch); nothing is made without that touch.
     */
    fun need(step: TutoStep, p: Project, target: TutoTarget, baseline: Any? = null): TutoNeed? {
        if (done(step, p, target, baseline)) return null
        val c = step.check
        val arg = c.substringAfter(':', "")
        return when {
            c.startsWith("lane:") -> target.line(arg)?.let { line ->
                barsOf(p, line).takeIf { p.drums.steps < line.pattern.length.coerceAtMost(4 * p.stepsPerBar) }?.let { TutoNeed.Bars(it) }
            }
            // Its track first; its first 4 bars written: « Copier sur les mesures suivantes ».
            c.startsWith("notes:") -> if (trackFor(p, arg) == null) TutoNeed.Track(arg) else copyNeed(step, p, target, arg)
            // Velocities: the first bar swinging, the others not yet.
            c == "velocity:hat" -> target.line("hat")?.let { line ->
                barsOf(p, line).takeIf { p.drums.steps < line.pattern.length.coerceAtMost(4 * p.stepsPerBar) }?.let { TutoNeed.Bars(it) }
            } ?: hatBars(p.drums, p.stepsPerBar).let { b -> TutoNeed.Copy("hat").takeIf { b.firstOrNull() == true && b.any { !it } } }
            // « Copier la basse une octave plus bas »: the Sub track and its notes in one touch.
            c == "track:sub" -> TutoNeed.Track("sub").takeIf { trackFor(p, "bass") != null }
            // The Drop 2: its part made (without its bass), shown, its bass's first 4 bars written, copied, then its Sub.
            writesBass(step) -> {
                val label = partLabel(step)
                val i = sceneLabelled(p, label)
                when {
                    i < 0 -> TutoNeed.Part(label).takeIf { p.allScenes.getOrNull(grooveOf(p))?.isEmpty == false }
                    i != p.scene -> TutoNeed.Scene(label)
                    else -> copyNeed(step, p, target, "bass") ?: TutoNeed.Track("sub").takeIf {
                        mineFor(step, p)?.let { notesMatch(it, movedBass(step, target)) } == true && p.instrumentTracks.any { roleOf(p, it.id) == "sub" }
                    }
                }
            }
            c.startsWith("part:") -> TutoNeed.Part(c.removePrefix("part:").substringBeforeLast(':')).takeIf { p.allScenes.getOrNull(grooveOf(p))?.isEmpty == false }
            c.startsWith("label:") -> TutoNeed.Name(arg).takeIf { p.allScenes.any { !it.isEmpty } }
            c == "song:phrases" -> TutoNeed.Lengths.takeIf { p.song.size >= 3 }
            else -> sceneFor(step)?.let { label ->
                sceneLabelled(p, label).takeIf { it >= 0 && it != p.scene }?.let { TutoNeed.Scene(label) }
            }
        }
    }

    /** « Copier sur les mesures suivantes » when the first half of [step]'s notes is written and the rest is not. */
    private fun copyNeed(step: TutoStep, p: Project, target: TutoTarget, role: String): TutoNeed? {
        val mine = mineFor(step, p) ?: return null
        val all = wantedNotes(step, p, target) ?: return null
        val first = all.filter { it.start < halfOf(step, target) }
        return TutoNeed.Copy(role).takeIf { first.size < all.size && notesMatch(mine, first) && !notesMatch(mine, all) }
    }

    /** [p] with [need] met (one touch of the user), and the instrument track it made (null: none); null when it cannot. */
    fun fulfil(need: TutoNeed, step: TutoStep, p: Project, target: TutoTarget): Pair<Project, Int?>? = when (need) {
        is TutoNeed.Bars -> p.copy(drums = p.drums.withSteps(need.bars * p.stepsPerBar)) to null
        // The Sub: made (when it is not) and written an octave under the bass of the part shown.
        is TutoNeed.Track -> if (need.role == "sub") withSub(p, target)?.let { q -> q to q.instrumentTracks.firstOrNull { roleOf(q, it.id) == "sub" }?.id }
            else partTrack(p, need.role, target)?.let { (q, id) -> q to id }
        is TutoNeed.Copy -> copied(p, need.role)?.let { it to null }
        is TutoNeed.Part -> makePart(p, step, target)?.let { it to null }
        is TutoNeed.Name -> p.renameScene(grooveOf(p), need.label) to null
        is TutoNeed.Lengths -> lengths(p, step.song)?.let { it to null }
        is TutoNeed.Scene -> sceneLabelled(p, need.label).takeIf { it >= 0 }?.let { p.switchScene(it) to null }
    }

    /** Each part of the song given the bars [spec] names for its label (the order kept); null when nothing changes. */
    private fun lengths(p: Project, spec: String): Project? {
        val bars = spec.split(',').associate { e -> e.substringBeforeLast(':').trim() to (e.substringAfterLast(':').toIntOrNull() ?: 0) }
        val song = p.song.map { part ->
            val label = p.allScenes.getOrNull(part.scene)?.label.orEmpty()
            val b = bars.entries.firstOrNull { labelIs(label, it.key) }?.value ?: 8
            part.copy(repeats = 1, bars = b)
        }
        return p.copy(song = song).takeIf { it.song != p.song }
    }

    /** The notes [step] asks for on its track, shown in dotted outline: (track, notes); null when none. */
    fun ghostNotes(step: TutoStep, p: Project, target: TutoTarget): Pair<Int, List<Note>>? {
        val c = step.check
        return when {
            // The first 4 bars while they are not all written, then the whole line (testers, lot « tuto 4 »).
            c.startsWith("notes:") -> {
                val id = trackFor(p, c.removePrefix("notes:")) ?: return null
                id to (wantedNotes(step, p, target) ?: return null)
            }
            // The Drop 2's new bass, in its part only.
            writesBass(step) -> {
                val i = sceneLabelled(p, partLabel(step)).takeIf { it >= 0 && it == p.scene } ?: return null
                (trackFor(p, "bass") ?: return null) to (wantedNotes(step, p, target) ?: return null)
            }
            c == "track:sub" -> {
                val bass = trackFor(p, "bass") ?: return null
                val sub = p.instrumentTracks.firstOrNull { roleOf(p, it.id) == "sub" }?.id ?: return null
                sub to StarterWizard.subNotes(notesOf(p, bass))
            }
            else -> null
        }
    }

    /**
     * « Montre-moi »: the step's target put in step by step ([TutoDemo.frames], from what the user
     * has, a cell or a note more each time, each with what it sounds: [TutoDemo.sounds]), ending on
     * [show]'s result. Null when the step has none.
     */
    fun demo(step: TutoStep, p: Project, target: TutoTarget): TutoDemo? {
        val end = show(step, p, target) ?: return null
        val c = step.check
        val arg = c.substringAfter(':', "")
        val frames = ArrayList<Project>()
        val sounds = ArrayList<TutoSound?>()
        if (step.demo == "kick") {
            // A long kick, then the short dry one, each heard and left a moment (it ends on the drum & bass kick).
            val dnb = StudioDrums.isDnbKick(p.padVoices[0])
            val (long, short) = if (dnb) end to p else p to end
            for ((q, lane) in listOf(long to StarterWizard.Role.Kick.lane, short to StarterWizard.Role.Kick.lane)) {
                repeat(KICK_FRAMES) { k -> frames += q; sounds += if (k == 0) TutoSound(lane = lane) else null }
            }
            return TutoDemo(frames, sounds)
        }
        when {
            // Yanis (lot « tuto 4 »): the key was set at once; now its two buttons one after the other, the scale, then the root.
            c == "key" -> {
                frames += p; sounds += null
                val scaled = p.copy(harmony = p.harmony.copy(scale = target.harmony.scale))
                repeat(KEY_FRAMES) { frames += scaled; sounds += null }
                repeat(KEY_FRAMES) { frames += end; sounds += null }
            }
            c.startsWith("lane:") -> {
                val lane = laneOf(arg)
                val wanted = end.drums.lanes[lane].hits
                val start = need(step, p, target)?.let { n -> fulfil(n, step, p, target)?.first } ?: p
                var hits = start.drums.lanes[lane].hits.filterKeys { it in wanted }
                fun put(h: Map<Int, Hit>) = start.copy(drums = start.drums.copy(lanes = start.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = h) else l }))
                frames += put(hits); sounds += null
                for ((at, hit) in wanted.entries.sortedBy { it.key }) {
                    if (at in hits) continue
                    hits = hits + (at to hit)
                    frames += put(hits); sounds += TutoSound(lane = lane)
                }
            }
            c.startsWith("notes:") || c == "track:sub" -> {
                val role = if (c == "track:sub") "sub" else arg
                val (start, id) = (if (c == "track:sub") partTrack(p, "sub", target) else ensureTrack(p, Part.entries.first { it.id == arg }, target)) ?: return null
                val endTrack = (if (c == "track:sub") end.instrumentTracks.firstOrNull { roleOf(end, it.id) == "sub" }?.id else trackFor(end, role))?.let { end.track(it) }
                val wanted = endTrack?.notes?.notes.orEmpty()
                var clip = start.track(id).notes.withSteps(endTrack?.notes?.steps ?: start.track(id).notes.steps).copy(notes = emptyList())
                frames += start.withTrack(start.track(id).copy(notes = clip)); sounds += null
                for ((_, chord) in wanted.groupBy { it.start }.entries.sortedBy { it.key }) {
                    clip = clip.copy(notes = clip.notes + chord)
                    frames += start.withTrack(start.track(id).copy(notes = clip)); sounds += TutoSound(track = id, pitches = chord.map { it.pitch })
                }
            }
        }
        if (frames.isEmpty() || frames.last().sanitized() != end) { frames += end; sounds += null }
        // A long line (two bars of hats) goes two by two: the show stays short.
        if (frames.size > MAX_FRAMES) {
            val every = ceil(frames.size / MAX_FRAMES.toDouble()).toInt()
            val keep = frames.indices.filter { it % every == 0 || it == frames.lastIndex }
            return TutoDemo(keep.map { frames[it] }, keep.map { sounds[it] })
        }
        return TutoDemo(frames, sounds)
    }

    const val MAX_FRAMES = 24

    /** The kick's comparison: each kick shown this many frames (about 0.8 s at the panel's pace). */
    private const val KICK_FRAMES = 4

    /** The key's two buttons, each held this many frames. */
    private const val KEY_FRAMES = 4

    /** The cells « Je le fais » shows dotted on a pad: (pad, steps of [steps]) of a "lane:…" step. */
    fun hint(step: TutoStep, target: TutoTarget, steps: Int): Pair<Int, Set<Int>>? {
        if (!step.check.startsWith("lane:")) return null
        val role = step.check.removePrefix("lane:")
        val line = target.line(role) ?: return null
        return laneOf(role) to StarterWizard.hits(line, steps).keys
    }

    /**
     * The hits a step asks to take away, by pad, in the part shown ([p]'s current scene): the gap's
     * last two cells of the Montée (testers: « efface les coups des 2 dernières cases », but they
     * were in its 4th bar, out of sight). Empty when nothing is left to take away; null for a step
     * that takes nothing away. For the screen to mark them (and show their bar).
     */
    fun toErase(step: TutoStep, p: Project): Map<Int, Set<Int>>? {
        if (step.check != "gap") return null
        val d = p.drums
        val last = (d.steps - 2).coerceAtLeast(0) until d.steps
        return d.lanes.mapIndexedNotNull { i, l -> l.hits.keys.filter { it in last }.toSet().takeIf { it.isNotEmpty() }?.let { i to it } }.toMap()
    }
}
