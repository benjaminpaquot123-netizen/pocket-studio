package com.pocketstudio.model.tuto

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Channel
import com.pocketstudio.model.DrumLane
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.NewTracks
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.firstEmptyScene
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
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

    /** The notes of [part] in the style (the assistant's first notes, in the course's key). */
    fun notes(part: Part): NoteClip = StarterWizard.startingNotes(part, style, mood, harmony, NewTracks.sound(kind(part), style))

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
     * drum sounds as the assistant sets them (the kit, its voices and their levels): what the user
     * builds sounds like the examples. Tempo and key stay the new project's, steps set them.
     */
    fun blank(): Project {
        val s = song
        val fresh = Project(name = "")
        val drumChannels = setOf(0) + (0 until DrumPattern.PAD_COUNT).map { Mixer.padChannel(it) }
        return fresh.copy(
            kitVersion = s.kitVersion, soundVersion = s.soundVersion, mixVersion = s.mixVersion, padVoices = s.padVoices, drumSeed = s.drumSeed,
            drums = s.drums.copy(steps = fresh.drums.steps, lanes = s.drums.lanes.map { it.copy(hits = emptyMap()) }),
            mixer = fresh.mixer.copy(channels = fresh.mixer.channels.mapIndexed { i, c ->
                val theirs = s.mixer.channels.getOrNull(i)
                if (i in drumChannels && theirs != null) c.copy(volume = theirs.volume, pan = theirs.pan) else c
            }),
        ).sanitized()
    }

    companion object {
        private val cache = ConcurrentHashMap<String, TutoTarget>()
        fun of(style: String): TutoTarget? = StarterWizard.style(style)?.let { s -> cache.getOrPut(style) { TutoTarget(s) } }
    }
}

/** « Écouter l'exemple »: [after] heard, after [before] when the step compares two parts. */
class TutoExample(val before: Project?, val after: Project)

/** A block of the song's map: a part's [label], its [bars] and its [energy] (0..1: how many of its bricks play). */
data class TutoMapBlock(val label: String, val bars: Int, val energy: Float)

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

    fun laneOf(role: String): Int = StarterWizard.Role.entries.firstOrNull { it.id == role }?.lane ?: 0

    fun roleOfLane(lane: Int): String = ROLE_LANES.entries.firstOrNull { lane in it.value }?.key ?: "perc"

    /** A signature of what a "changed:…" step watches (its value when the step began is the baseline). */
    fun signature(what: String, p: Project): Any? = when {
        what == "volumes" -> p.mixer.channels.map { it.volume }
        what == "kit" -> p.drums.lanes.getOrNull(0)?.sample to p.padVoices[0]
        what.startsWith("synth:") -> trackFor(p, what.removePrefix("synth:"))?.let { id -> p.track(id).let { listOf(it.synth, it.plaits, it.engine) } }
        else -> null
    }

    // ---- what the user's project holds ----

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

    /** The hits of drum role [role] in [d], laid on the target's line length. */
    private fun laneMatches(d: DrumPattern, lanes: Set<Int>, line: StarterWizard.Line): Boolean {
        val target = StarterWizard.hits(line.pattern).keys
        val window = if (d.steps >= line.pattern.length) line.pattern.length else d.steps
        val wanted = target.filter { it < window }.toSet().ifEmpty { return false }
        val mine = lanes.flatMap { l -> d.lanes.getOrNull(l)?.hits?.keys.orEmpty().filter { it < d.steps } }.map { it % window }.toSet()
        val hit = (wanted intersect mine).size
        val extra = (mine - wanted).size
        return hit >= ceil(wanted.size * 0.75).toInt() && extra <= maxOf(1, wanted.size / 2)
    }

    private fun lanesOf(role: String) = ROLE_LANES[role] ?: setOf(laneOf(role))

    /** Whether [step] is done in [p] ([baseline]: the [signature] when it began, for "changed:…"). */
    fun done(step: TutoStep, p: Project, target: TutoTarget, baseline: Any? = null): Boolean {
        val c = step.check
        val arg = c.substringAfter(':', "")
        return when {
            step.manual || step.listening -> false
            c == "tempo" -> kotlin.math.abs(p.tempoBpm - target.style.bpm) <= 2.0
            c == "key" -> p.harmony.root == target.harmony.root && p.harmony.scale == target.harmony.scale
            c.startsWith("lane:") -> target.line(arg)?.let { line -> p.allScenes.any { laneMatches(it.drums, lanesOf(arg), line) } } ?: false
            c == "velocity:hat" -> p.allScenes.any { s -> lanesOf("hat").flatMap { s.drums.lanes.getOrNull(it)?.hits?.values.orEmpty() }.map { it.velocity }.let { v -> v.size >= 4 && v.max() - v.min() >= 0.15f } }
            c.startsWith("notes:") -> trackFor(p, arg)?.let { id ->
                val n = notesOf(p, id)
                when (arg) {
                    "bass" -> n.size >= 4 && median(n) < LOW + 3
                    "lead" -> n.size >= 3 && median(n) >= LOW + 3
                    else -> n.size >= 3
                }
            } ?: false
            c == "track:sub" -> p.instrumentTracks.count { t -> notesOf(p, t.id).let { it.size >= 2 && median(it) < LOW } } >= 2
            c.startsWith("insert:") -> (p.mixer.channels.flatMap { it.inserts } + p.mixer.master).any { !it.bypass && it.effect.name == arg }
            c.startsWith("send:") -> arg.toIntOrNull()?.let { bus -> p.mixer.channels.any { (it.sends.getOrNull(bus) ?: 0f) > 0.05f } } ?: false
            c == "automation" -> p.automation.any { l -> l.songValues.any { it != null } || l.sceneValues.isNotEmpty() || l.values.any { it != null } }
            c == "master" -> p.mixer.master.any { it.effect != EffectType.NONE && !it.bypass }
            c.startsWith("changed:") -> baseline != null && signature(arg, p) != baseline
            c.startsWith("label:") -> sceneLabelled(p, arg) >= 0
            c.startsWith("part:") -> partDone(p, c.removePrefix("part:"))
            c == "song:order" -> songOrdered(p)
            c == "song:phrases" -> p.song.size >= 3 && p.song.all { (p.partSteps(it) / p.stepsPerBar.coerceAtLeast(1)) % 8 == 0 }
            c == "fill" -> p.allScenes.any { s -> s.drums.lanes.any { l -> l.hits.values.any { it.repeats > 1 } } }
            c == "crash" -> p.allScenes.any { s -> (labelIs(s.label, "Drop") || labelIs(s.label, "Drop 2")) && s.drums.lanes.getOrNull(CRASH_LANE)?.hits?.containsKey(0) == true }
            c == "gap" -> sceneLabelled(p, "Montée").takeIf { it >= 0 }?.let { i ->
                val d = p.allScenes[i].drums
                d.lanes.none { l -> l.hits.keys.any { it in d.steps - 2 until d.steps } }
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
            "differs" -> sceneLabelled(p, "Drop").let { d -> d >= 0 && d != i && all[d].copy(name = "", label = "") != all[i].copy(name = "", label = "") }
            else -> false
        }
    }

    private fun songOrdered(p: Project): Boolean {
        if (p.song.size < 5) return false
        val labels = p.song.map { p.allScenes.getOrNull(it.scene)?.label.orEmpty() }
        val montee = labels.indices.any { k -> labelIs(labels[k], "Montée") && labels.getOrNull(k + 1)?.let { labelIs(it, "Drop") || labelIs(it, "Drop 2") } == true }
        return labelIs(labels.first(), "Intro") && montee && labels.any { labelIs(it, "Break") }
    }

    // ---- « Montre-moi » ----

    /** [p] with [step]'s target in it (one undo step in the app), null when the step has none. */
    fun show(step: TutoStep, p: Project, target: TutoTarget): Project? {
        val c = step.check
        val arg = c.substringAfter(':', "")
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
            c == "track:sub" -> {
                val bass = trackFor(p, "bass") ?: return null
                val notes = p.track(bass).notes.takeIf { it.notes.isNotEmpty() } ?: return null
                // The Sub track made by the step's button, else a new one.
                val (q, id) = p.instrumentTracks.firstOrNull { it.id != bass && roleOf(p, it.id) == "sub" }?.let { p to it.id } ?: partTrack(p, "sub", target) ?: return null
                q.withTrack(q.track(id).copy(name = "Sub", notes = notes.copy(notes = StarterWizard.subNotes(notes.notes))))
            }
            c.startsWith("insert:") -> {
                val type = EffectType.entries.firstOrNull { it.name == arg } ?: return null
                val ch = InstrumentTrack.mixerChannel(trackFor(p, step.target.ifEmpty { "bass" }) ?: 0)
                withInsert(p, ch, Insert(type.ordinal, type.defaults)) ?: return null
            }
            c.startsWith("send:") -> {
                val bus = arg.toIntOrNull() ?: return null
                val chans = if (bus == 0) listOfNotNull(trackFor(p, "pad")?.let(InstrumentTrack::mixerChannel), Mixer.FIRST_PAD + laneOf("snare"))
                else listOfNotNull(trackFor(p, "lead")?.let(InstrumentTrack::mixerChannel))
                if (chans.isEmpty()) return null
                withChannels(p, chans) { it.copy(sends = it.sends.mapIndexed { b, v -> if (b == bus) (if (bus == 0) 0.3f else 0.25f) else v }) }
            }
            c == "automation" -> riser(p, trackFor(p, "pad") ?: trackFor(p, "lead") ?: return null) ?: return null
            c == "master" -> p.copy(mixer = p.mixer.copy(master = target.song.mixer.master))
            c == "changed:volumes" -> withChannels(p, listOfNotNull(trackFor(p, "pad"), trackFor(p, "lead")).map(InstrumentTrack::mixerChannel)) { it.copy(volume = it.volume * 0.8f) }
            c.startsWith("label:") -> p.renameScene(grooveOf(p), arg)
            c.startsWith("part:") -> makePart(p, step, target) ?: return null
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
        if (!blankFirst(p)) return added.withTrack(made) to made.id
        // The blank first track takes its place (same sound, same level), the slot of the new one stays free.
        val level = added.mixer.channels[InstrumentTrack.mixerChannel(made.id)]
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
     * target's part [TutoStep.lanesFrom] on the pads it keeps, the bass moved by [TutoStep.transpose].
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
        q.instrumentTracks.forEach { t ->
            val role = roleOf(q, t.id) ?: "lead"
            val kept = all || role in keep || (role == "sub" && "bass" in keep)
            val moved = if (kept && step.transpose != 0 && role in setOf("bass", "sub")) t.notes.copy(notes = t.notes.notes.map { it.copy(pitch = (it.pitch + step.transpose).coerceIn(0, 127)) }) else t.notes
            q = q.withTrack(t.copy(notes = if (kept) moved else moved.copy(notes = emptyList())))
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

    /** The map of the target's song: its parts in order, their bars and energy (the outro named Fin). */
    fun map(target: TutoTarget): List<TutoMapBlock> {
        val song = target.song
        val all = song.allScenes
        val sections = StarterWizard.songSections(song)
        val most = all.maxOfOrNull { bricks(it).size }?.coerceAtLeast(1) ?: 1
        var drops = 0
        return sections.mapIndexed { k, s ->
            // The later Drops play the second bass (« Drop 2 »'s, through the song's parts): named so on the map.
            val label = all.getOrNull(s.scene)?.label.orEmpty().let { if (k > 0 && k == sections.lastIndex && s.scene == sections.first().scene) "Fin" else it }
                .let { if (it == "Drop" && drops++ > 0) "Drop 2" else it }
            TutoMapBlock(label, s.bars, bricks(all[s.scene]).size / most.toFloat())
        }
    }

    // ---- what a step needs first, its demonstration, its ghost notes ----

    /** The bars a drum role's line lasts (the loop must be at least that long to hold it). */
    private fun barsOf(p: Project, line: StarterWizard.Line): Int = ceil(line.pattern.length / p.stepsPerBar.coerceAtLeast(1).toDouble()).toInt().coerceIn(1, 4)

    /** The part a step works in ("Montée" for the fill and the gap, "Drop" for the crash), null when none. */
    private fun sceneFor(step: TutoStep): String? = when (step.check) {
        "fill", "gap" -> "Montée"
        "crash" -> "Drop"
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
            c.startsWith("lane:") || c == "velocity:hat" -> target.line(if (c.startsWith("lane:")) arg else "hat")?.let { line ->
                barsOf(p, line).takeIf { p.drums.steps < line.pattern.length.coerceAtMost(4 * p.stepsPerBar) }?.let { TutoNeed.Bars(it) }
            }
            c.startsWith("notes:") -> TutoNeed.Track(arg).takeIf { trackFor(p, arg) == null }
            c == "track:sub" -> TutoNeed.Track("sub").takeIf { trackFor(p, "bass") != null && p.instrumentTracks.none { roleOf(p, it.id) == "sub" } }
            c.startsWith("part:") -> TutoNeed.Part(c.removePrefix("part:").substringBeforeLast(':')).takeIf { p.allScenes.getOrNull(grooveOf(p))?.isEmpty == false }
            c.startsWith("label:") -> TutoNeed.Name(arg).takeIf { p.allScenes.any { !it.isEmpty } }
            c == "song:phrases" -> TutoNeed.Lengths.takeIf { p.song.size >= 3 }
            else -> sceneFor(step)?.let { label ->
                sceneLabelled(p, label).takeIf { it >= 0 && it != p.scene }?.let { TutoNeed.Scene(label) }
            }
        }
    }

    /** [p] with [need] met (one touch of the user), and the instrument track it made (null: none); null when it cannot. */
    fun fulfil(need: TutoNeed, step: TutoStep, p: Project, target: TutoTarget): Pair<Project, Int?>? = when (need) {
        is TutoNeed.Bars -> p.copy(drums = p.drums.withSteps(need.bars * p.stepsPerBar)) to null
        is TutoNeed.Track -> partTrack(p, need.role, target)?.let { (q, id) -> q to id }
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
            c.startsWith("notes:") -> {
                val part = Part.entries.firstOrNull { it.id == c.removePrefix("notes:") } ?: return null
                val id = trackFor(p, part.id) ?: return null
                id to target.notes(part).notes
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
        when {
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

    /** The cells « Je le fais » shows dotted on a pad: (pad, steps of [steps]) of a "lane:…" step. */
    fun hint(step: TutoStep, target: TutoTarget, steps: Int): Pair<Int, Set<Int>>? {
        if (!step.check.startsWith("lane:")) return null
        val role = step.check.removePrefix("lane:")
        val line = target.line(role) ?: return null
        return laneOf(role) to StarterWizard.hits(line, steps).keys
    }
}
