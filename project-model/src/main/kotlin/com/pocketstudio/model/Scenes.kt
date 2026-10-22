package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * One scene of the session: the drum pattern, the acid line's notes and the piano roll.
 * Pad sounds and the acid sound and root stay global (the same kit and 303 play every scene).
 */
@Serializable
data class Scene(
    val name: String = "A",
    val drums: DrumPattern = DrumPattern(),
    val acid: AcidPattern = AcidPattern(),
    val synthNotes: NoteClip = NoteClip(),
    /** The notes of the added instrument tracks in this scene, by track id. */
    val trackNotes: Map<Int, NoteClip> = emptyMap(),
    /** The song section this scene stands for, named by the user ("Refrain"); empty: just its letter. */
    val label: String = "",
) {
    /** Steps the scene lasts before it loops in 4/4: its longest part, rounded up to whole bars. */
    val steps: Int get() = stepsFor(16)

    /** The same with bars of [stepsPerBar] sixteenths (time signature). */
    fun stepsFor(stepsPerBar: Int): Int {
        val longest = maxOf(drums.steps, acid.length, synthNotes.steps, trackNotes.values.maxOfOrNull { it.steps } ?: 0)
        return (longest + stepsPerBar - 1) / stepsPerBar * stepsPerBar
    }

    val isEmpty: Boolean get() = drums.lanes.all { lane -> lane.hits.keys.none { it < drums.steps } } &&
        acid.steps.take(acid.length).none { it.gate } && synthNotes.notes.isEmpty() && trackNotes.values.all { it.notes.isEmpty() }

    companion object {
        const val MAX = 24
        val NAMES = List(MAX) { ('A' + it).toString() }

        /** How many scene slots the session grid can show. */
        val GRID_SIZES = listOf(8, 16, 24)

        /** Section names offered when naming a scene. */
        val SECTIONS = listOf("Intro", "Couplet", "Pré-refrain", "Refrain", "Pont", "Groove", "Montée", "Break", "Drop", "Fin")

        /**
         * A scene label as shown: a section name ("Refrain", or a copy's "Refrain 2") in the
         * interface's language; any other name the user typed stays as it is. The label is
         * saved in French so a project reads the same in every language.
         */
        fun shownLabel(label: String): String {
            if (label in SECTIONS) return tr(label)
            val numbered = Regex("""^(.+) (\d+)$""").matchEntire(label) ?: return label
            val (section, n) = numbered.destructured
            return if (section in SECTIONS) "${tr(section)} $n" else label
        }

        const val MAX_LABEL = 20
    }
}

/** The section name of scene [index] ("Refrain"), or "" when it has none. */
fun Project.sceneLabel(index: Int): String = allScenes.getOrNull(index)?.label.orEmpty()

/** How a scene is shown: "A · Refrain", or just "A" without a section name. */
fun Project.sceneTitle(index: Int): String {
    val letter = Scene.NAMES.getOrElse(index) { "?" }
    return sceneLabel(index).takeIf { it.isNotBlank() }?.let { "$letter · ${Scene.shownLabel(it)}" } ?: letter
}

/** Names scene [index] after a song section (blank: back to its letter alone). */
fun Project.renameScene(index: Int, label: String): Project {
    val synced = syncScenes()
    val all = synced.allScenes
    if (index !in all.indices) return synced
    val clean = label.replace(Regex("\\s+"), " ").trim().take(Scene.MAX_LABEL)
    return synced.copy(scenes = all.mapIndexed { i, s -> if (i == index) s.copy(label = clean) else s }).syncScenes()
}

/**
 * One line of the song: play [scene] [repeats] times. [muted]: tracks silent in this part (the
 * timeline): "drums", "acid", "inst:<id>" (instrument track), "audio:<id>" (audio track).
 *
 * A part written from the live grid (« Enregistrer la performance ») can mix scenes: [play] gives
 * the tracks that play another scene than [scene] (track key to scene), [from] the bars each
 * track had already played of its part when this one begins (it carries on rather than starting
 * over), and [bars] the part's own length (then [repeats] is 1). All three are left out of the
 * file when unused: songs made by hand are saved as before.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SongPart(
    val scene: Int = 0,
    val repeats: Int = 1,
    val muted: Set<String> = emptySet(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val play: Map<String, Int> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val from: Map<String, Int> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val bars: Int? = null,
) {
    fun plays(key: String) = key !in muted

    /** The scene track [key] plays in this part. */
    fun sceneFor(key: String): Int = play[key] ?: scene

    /** Bars track [key] had already played of its part when this one begins (0: from its start). */
    fun fromBars(key: String): Int = from[key] ?: 0

    /** Some track plays another scene, or carries on (« mélange » on the timeline). */
    val mixed: Boolean get() = play.isNotEmpty() || from.isNotEmpty()

    /** In range for a song of [scenes] scenes whose parts can mute [keys] (and the remix, when [remix] is loaded). */
    internal fun sanitized(keys: List<String>, scenes: Int, remix: Boolean = false): SongPart {
        val length = bars?.coerceIn(1, MAX_BARS)
        return copy(
            repeats = if (length != null) 1 else repeats.coerceIn(1, MAX_REPEATS),
            muted = muted.filter { it in keys || (remix && it == REMIX) }.toSet(),
            play = play.filter { (k, s) -> k in keys && s in 0 until scenes && s != scene },
            from = from.filter { (k, b) -> k in keys && b > 0 }.mapValues { it.value.coerceAtMost(MAX_FROM) },
            bars = length,
        )
    }

    companion object {
        const val DRUMS = "drums"
        const val ACID = "acid"
        /** The remix (a separated song played along): a column of the live grid. */
        const val REMIX = "remix"
        fun inst(id: Int) = "inst:$id"
        fun audio(id: Int) = "audio:$id"

        const val MAX_REPEATS = 16
        const val MAX_PARTS = 128
        /** The longest part with its own length, in bars. */
        const val MAX_BARS = 256
        /** Bars a track can have played already (whole loops are taken off before: it stays small). */
        const val MAX_FROM = 1024
    }
}

/** The scene being edited and played is mirrored in the project's own drums / acid / notes. */
fun Project.currentSceneContent(): Scene =
    Scene(Scene.NAMES.getOrElse(scene) { "?" }, drums, acid, synthNotes, tracks.associate { it.id to it.notes })

/** All scenes, the current one included (a project that never used scenes has just one). */
val Project.allScenes: List<Scene> get() = scenes.ifEmpty { listOf(currentSceneContent()) }

/**
 * Keeps [Project.scenes] in step with the current content (called by [Project.sanitized]).
 * A project with a single scene keeps an empty list: older files stay byte-for-byte the same.
 */
internal fun Project.syncScenes(): Project {
    if (scenes.isEmpty()) return copy(scene = 0, song = song.filter { it.scene == 0 }.take(SongPart.MAX_PARTS).map { it.sanitized(partKeys, 1, remix.setId != null) })
    val all = scenes.take(Scene.MAX)
    val index = scene.coerceIn(0, all.lastIndex)
    // Empty scenes at the end go when nothing uses them (a tap on a far slot leaves no trail of them).
    val used = song.flatMap { listOf(it.scene) + it.play.values }.toSet() +
        automation.flatMap { lane -> lane.sceneValues.filterValues { v -> v.any { it != null } }.keys } +
        scenesWithTakes() + mixer.insertScenes
    var size = all.size
    while (size - 1 > index && all[size - 1].isEmpty && all[size - 1].label.isBlank() && size - 1 !in used) size--
    val list = all.take(size)
    return copy(
        scene = index,
        scenes = list.mapIndexed { i, s ->
            if (i == index) currentSceneContent().copy(name = Scene.NAMES[i], label = s.label)
            else s.copy(name = Scene.NAMES[i], trackNotes = s.trackNotes.filterKeys { id -> tracks.any { it.id == id } }.mapValues { it.value.sanitized() })
        },
        song = song.filter { it.scene in list.indices }.take(SongPart.MAX_PARTS).map { it.sanitized(partKeys, list.size, remix.setId != null) },
    )
}

/** Makes [index] the current scene (adding empty scenes up to it if needed). */
fun Project.switchScene(index: Int): Project {
    val synced = syncScenes()
    if (index !in 0 until Scene.MAX) return synced
    val existing = synced.allScenes
    return synced.showing(existing + List((index + 1 - existing.size).coerceAtLeast(0)) { Scene() }, index)
}

/**
 * The project with the scenes [list] (already in step: no current content to save first),
 * scene [index] shown and mirrored in drums / acid / notes.
 */
private fun Project.showing(list: List<Scene>, index: Int): Project {
    val target = list[index]
    return copy(
        scenes = list,
        scene = index,
        // Pad sounds and the 303's sound and root are shared by all scenes: only the notes change.
        drums = target.drums.copy(lanes = target.drums.lanes.mapIndexed { i, l -> l.copy(sample = drums.lanes.getOrNull(i)?.sample, pan = drums.lanes.getOrNull(i)?.pan ?: 0f) }),
        acid = acid.copy(length = target.acid.length, steps = target.acid.steps, locked = target.acid.locked),
        synthNotes = target.synthNotes,
        tracks = tracks.map { t -> t.copy(notes = target.trackNotes[t.id] ?: NoteClip(steps = t.notes.steps)) },
    ).syncScenes()
}

/**
 * Where scene [index] is once scene [from] was dropped on slot [to]: [from] goes to [to]; with
 * [replace] the scene at [to] is overwritten (what pointed at [to] stays there and now plays the
 * scene moved), else the two swap places (dropping on an empty slot is a swap with nothing).
 */
fun sceneIndexAfterMove(index: Int, from: Int, to: Int, replace: Boolean): Int = when (index) {
    from -> to
    to -> if (replace) to else from
    else -> index
}

/**
 * The way back of [sceneIndexAfterMove] (undoing the move): what is at [index] after the move came
 * from there. A swap is its own way back; after a replace, slot [to] holds the scene from [from].
 */
fun sceneIndexBeforeMove(index: Int, from: Int, to: Int, replace: Boolean): Int = when (index) {
    to -> from
    from -> if (replace) from else to
    else -> index
}

/** A scene move, as an undo step's tag: undoing or redoing it, what plays follows its scene. */
data class SceneMove(val from: Int, val to: Int, val replace: Boolean) {
    fun forward(index: Int) = sceneIndexAfterMove(index, from, to, replace)
    fun back(index: Int) = sceneIndexBeforeMove(index, from, to, replace)
}

/**
 * Scene [from] dragged onto slot [to] (one edit, see [sceneIndexAfterMove]). Everything that
 * names a scene by its place follows: the song's parts (the song sounds the same after a swap),
 * per-scene automation, scene audio clips, the effects that play in some scenes only, and the
 * scene shown (it follows its content). With [replace], the scene at [to] and what was its own
 * are gone: its automation and its audio takes (they would otherwise play over the scene moved
 * there); Annuler brings them back.
 */
fun Project.moveScene(from: Int, to: Int, replace: Boolean = false): Project {
    if (from == to || from !in 0 until Scene.MAX || to !in 0 until Scene.MAX) return this
    val synced = syncScenes()
    val existing = synced.allScenes
    if (from !in existing.indices) return synced
    val list = (existing + List((to + 1 - existing.size).coerceAtLeast(0)) { Scene() }).toMutableList()
    val moving = list[from]
    list[from] = if (replace) Scene() else list[to]
    list[to] = moving
    val map = { i: Int -> sceneIndexAfterMove(i, from, to, replace) }
    fun AudioClip.moved() = copy(scene = scene?.let(map))
    // With replace, the takes of the scene replaced go with it.
    fun AudioClip?.kept() = this?.takeIf { !(replace && it.scene == to) }?.moved()
    return synced.copy(
        song = synced.song.map { part -> part.copy(scene = map(part.scene), play = part.play.mapValues { map(it.value) }) },
        automation = synced.automation.map { lane ->
            lane.copy(sceneValues = lane.sceneValues.filterKeys { !(replace && it == to) }.mapKeys { map(it.key) })
        },
        audioTracks = synced.audioTracks.map { t -> t.copy(clip = t.clip.kept(), previous = t.previous.kept()) },
        // With replace, slot [to] now is scene [from]: an effect plays there exactly when it played in
        // [from]. One that played only in the scene replaced plays nowhere (kept, and the box says so).
        mixer = synced.mixer.mapInserts { ins ->
            ins.scenes?.let { set -> ins.copy(scenes = set.filter { !(replace && it == to) }.map(map).sorted().toSet()) } ?: ins
        },
    ).showing(list, map(synced.scene))
}

/**
 * Copies the current scene into [index] (to start a variation from it), its per-scene automation
 * too, and the effects that play in some scenes only play in the copy as in the scene copied.
 */
fun Project.copySceneTo(index: Int): Project {
    if (index !in 0 until Scene.MAX) return this
    val synced = syncScenes()
    val existing = synced.allScenes
    val list = (existing + List((index + 1 - existing.size).coerceAtLeast(0)) { Scene() }).toMutableList()
    // A named slot keeps its name (copying the Couplet onto « Couplet 2 »).
    list[index] = synced.currentSceneContent().copy(label = list[index].label)
    val automation = synced.automation.map { lane ->
        val own = lane.sceneValues[synced.scene]
        lane.copy(sceneValues = if (own != null) lane.sceneValues + (index to own) else lane.sceneValues - index)
    }
    val mixer = synced.mixer.mapInserts { ins ->
        ins.scenes?.let { set -> ins.copy(scenes = (if (synced.scene in set) set + index else set - index).sorted().toSet()) } ?: ins
    }
    return synced.copy(scenes = list, automation = automation, mixer = mixer).syncScenes()
}

/**
 * Empties the scene shown (its drum hits, its acid line, the notes of every instrument track and
 * its own automation values); sounds, lengths and the other scenes stay.
 */
fun Project.clearCurrentScene(): Project = copy(
    drums = drums.copy(lanes = drums.lanes.map { it.copy(hits = emptyMap()) }),
    acid = acid.copy(steps = acid.steps.map { it.copy(gate = false) }),
    synthNotes = synthNotes.cleared(),
    tracks = tracks.map { it.copy(notes = it.notes.cleared()) },
    automation = automation.map { it.copy(sceneValues = it.sceneValues - scene) },
).syncScenes()

/** Another scene that holds exactly what scene [index] holds (it was copied and not changed yet), or null. */
fun Project.copyOf(index: Int): Int? {
    val all = allScenes
    val s = all.getOrNull(index)?.takeUnless { it.isEmpty } ?: return null
    return all.indices.firstOrNull { it != index && all[it].copy(name = "", label = "") == s.copy(name = "", label = "") }
}

/** The first empty scene slot other than the one shown (a new scene to fill), or null when all 24 are taken. */
fun Project.firstEmptyScene(): Int? {
    val all = allScenes
    val takes = scenesWithTakes()
    return all.indices.firstOrNull { it != scene && all[it].isEmpty && it !in takes } ?: all.size.takeIf { it < Scene.MAX }
}

/** Scenes an audio take belongs to (its clip, or the previous take kept to bring back). */
internal fun Project.scenesWithTakes(): Set<Int> =
    audioTracks.flatMap { t -> listOfNotNull(t.clip, t.previous) }.filter { !it.song }.mapNotNull { it.scene }.toSet()

/** A project shaped as if [index] were current, for sending a scene to the engine ahead of time. */
fun Project.sceneView(index: Int): Project = if (index == scene) this else switchScene(index)

/** Steps scene [index] lasts in this project's time signature. */
fun Project.sceneSteps(index: Int): Int = allScenes.getOrNull(index)?.stepsFor(stepsPerBar) ?: stepsPerBar

/** Every track key a song part can mute, in timeline order. */
val Project.partKeys: List<String> get() =
    listOf(SongPart.DRUMS, SongPart.ACID) + instrumentTracks.map { SongPart.inst(it.id) } + audioTracks.map { SongPart.audio(it.id) }

/**
 * Song part [part] as played: its scene, each track that plays another scene ([SongPart.play])
 * with that scene's notes, and the muted tracks emptied (audio tracks: see [SongPart.plays]).
 */
fun Project.partView(part: SongPart): Project {
    var v = sceneView(part.scene)
    if (part.play.isNotEmpty()) {
        val scenes = allScenes
        fun sceneOf(key: String): Scene? = part.play[key]?.let { scenes.getOrNull(it) }
        sceneOf(SongPart.DRUMS)?.let { s ->
            // Pad sounds stay the project's (as when a scene is shown): only the hits and the loop change.
            v = v.copy(drums = s.drums.copy(lanes = s.drums.lanes.mapIndexed { i, l -> l.copy(sample = v.drums.lanes.getOrNull(i)?.sample, pan = v.drums.lanes.getOrNull(i)?.pan ?: 0f) }))
        }
        sceneOf(SongPart.ACID)?.let { s -> v = v.copy(acid = v.acid.copy(length = s.acid.length, steps = s.acid.steps, locked = s.acid.locked)) }
        v.instrumentTracks.forEach { t ->
            val s = sceneOf(SongPart.inst(t.id)) ?: return@forEach
            val notes = if (t.id == 0) s.synthNotes else s.trackNotes[t.id] ?: NoteClip(steps = t.notes.steps)
            v = v.withTrack(t.copy(notes = notes))
        }
    }
    if (part.muted.isEmpty()) return v
    if (!part.plays(SongPart.DRUMS)) v = v.copy(drums = v.drums.copy(lanes = v.drums.lanes.map { it.copy(hits = emptyMap()) }))
    if (!part.plays(SongPart.ACID)) v = v.copy(acid = v.acid.copy(steps = v.acid.steps.map { it.copy(gate = false) }))
    v.instrumentTracks.filterNot { part.plays(SongPart.inst(it.id)) }.forEach { t -> v = v.withTrack(t.copy(notes = t.notes.copy(notes = emptyList()))) }
    return v
}

/** Steps song part [part] lasts: its own length when it has one, else its scene's length times its repeats. */
fun Project.partSteps(part: SongPart): Int = part.bars?.let { it * stepsPerBar } ?: (sceneSteps(part.scene) * part.repeats)

/** This part with track [key] playing [scene] from its start. */
fun SongPart.withScene(key: String, scene: Int): SongPart =
    copy(play = if (scene == this.scene) play - key else play + (key to scene), from = from - key)

/** Switches [key] on or off in song part [index]. */
fun Project.togglePartTrack(index: Int, key: String): Project = copy(
    song = song.mapIndexed { i, p -> if (i != index) p else p.copy(muted = if (key in p.muted) p.muted - key else p.muted + key) },
)

/**
 * Time signature [beats]/4 for the whole project: every scene's drum loop keeps its number of
 * bars (64 steps at most), so its grid stays whole bars. Only the loop's length changes: hits
 * past its new end stay (unheard), so trying 3/4 and going back to 4/4 gives the beat back. An
 * audio clip keeps its bar and its number of bars.
 */
fun Project.withBeatsPerBar(beats: Int): Project {
    val from = stepsPerBar
    val to = beats.coerceIn(Project.MIN_BEATS, Project.MAX_BEATS) * 4
    fun convert(d: DrumPattern): DrumPattern {
        val bars = (d.steps / from).coerceAtLeast(1)
        val n = (bars * to).coerceIn(DrumPattern.MIN_STEPS, DrumPattern.MAX_STEPS / to * to)
        // Hits kept past the end come back as they were; with none, a longer bar repeats the
        // groove instead of leaving silent beats.
        val restoring = d.lanes.any { lane -> lane.hits.keys.any { it >= d.steps } }
        if (n <= d.steps || restoring) return d.copy(steps = n)
        return d.copy(
            steps = n,
            lanes = d.lanes.map { lane ->
                lane.copy(hits = lane.hits + (d.steps until n).mapNotNull { st -> lane.hits[st % d.steps]?.let { st to it } })
            },
        )
    }
    fun convert(c: AudioClip): AudioClip {
        val bars = ((c.loopSteps + from - 1) / from).coerceAtLeast(1)
        return c.copy(
            start = (c.start / from * to + (c.start % from).coerceAtMost(to - 1)).coerceAtMost((if (c.song) AudioClip.MAX_SONG_START else AudioClip.MAX_START) / to * to),
            loopSteps = (bars * to).coerceAtMost(AudioClip.MAX_LOOP / to * to),
        )
    }
    return copy(
        beatsPerBar = to / 4,
        drums = convert(drums),
        scenes = scenes.map { it.copy(drums = convert(it.drums)) },
        audioTracks = audioTracks.map { t -> t.copy(clip = t.clip?.let(::convert)) },
    ).sanitized()
}

/**
 * Song playback: the index that leads to the right next part once part [removed] is taken out
 * while part [playing] plays (the song now has [sizeAfter] parts). A part before the playing one
 * shifts it down; the playing part itself leaves the part before it (wrapping round), so the
 * one that followed it still comes next.
 */
fun songIndexAfterRemoval(playing: Int, removed: Int, sizeAfter: Int): Int = when {
    sizeAfter <= 0 -> -1
    removed < playing -> playing - 1
    removed == playing -> Math.floorMod(playing - 1, sizeAfter)
    else -> playing.coerceAtMost(sizeAfter - 1)
}

/**
 * Song playback after the song changed under it (undo, redo): part [index] if it still plays
 * [scene], else the first part from it on (wrapping round) that does, else -1 (no part plays the
 * scene heard now: the song stops following, the scene goes on as a loop).
 */
fun followSongIndex(song: List<SongPart>, index: Int, scene: Int): Int {
    if (song.isEmpty()) return -1
    val start = index.coerceIn(0, song.size - 1)
    return (song.indices).map { (start + it) % song.size }.firstOrNull { song[it].scene == scene } ?: -1
}

/** A song part playing scene [scene] once is added at [at] (0 = first, the song's size = last). */
fun Project.insertSongPart(at: Int, scene: Int): Project {
    if (song.size >= SongPart.MAX_PARTS || scene !in allScenes.indices) return this
    val i = at.coerceIn(0, song.size)
    return copy(song = song.take(i) + SongPart(scene, 1) + song.drop(i))
        .followArrangement(this, song.indices.toList().let { it.take(i) + null + it.drop(i) })
}

/** Song part [index] played again right after itself (same scene, repeats and silenced tracks). */
fun Project.duplicateSongPart(index: Int): Project {
    if (song.size >= SongPart.MAX_PARTS || index !in song.indices) return this
    return copy(song = song.take(index + 1) + song[index] + song.drop(index + 1))
        .followArrangement(this, song.indices.toList().let { it.take(index + 1) + index + it.drop(index + 1) })
}

/** Song part [from] moved to place [to] (the others close up around it). */
fun Project.moveSongPart(from: Int, to: Int): Project {
    if (from !in song.indices || to !in song.indices || from == to) return this
    val list = song.toMutableList()
    list.add(to, list.removeAt(from))
    val origin = song.indices.toMutableList<Int?>()
    origin.add(to, origin.removeAt(from))
    return copy(song = list).followArrangement(this, origin)
}

/** Song part [index] taken out of the song. */
fun Project.removeSongPart(index: Int): Project =
    if (index !in song.indices) this
    else copy(song = song.filterIndexed { i, _ -> i != index }).followArrangement(this, song.indices.filter { it != index })

/** Song part [index] plays its scene [repeats] times (1..[SongPart.MAX_REPEATS]); what is placed after it follows. */
fun Project.withSongRepeats(index: Int, repeats: Int): Project {
    if (index !in song.indices) return this
    val n = repeats.coerceIn(1, SongPart.MAX_REPEATS)
    if (song[index].repeats == n) return this
    return copy(song = song.mapIndexed { i, part -> if (i == index) part.copy(repeats = n) else part })
        .followArrangement(this, song.indices.toList())
}

/** Where song part [index] is once part [from] moved to [to]. */
fun songIndexAfterMove(index: Int, from: Int, to: Int): Int = when {
    index == from -> to
    from < index && index <= to -> index - 1
    to <= index && index < from -> index + 1
    else -> index
}

/** Where song part [index] is once a part was inserted at [at] (a part inserted at its place pushes it on). */
fun songIndexAfterInsert(index: Int, at: Int): Int = if (at <= index) index + 1 else index

/**
 * Song playback: the part that follows part [playing] when [queued] (a part chosen to come next,
 * or -1 for none) is taken into account, or -1 when the song ends there ([loops] false) — the
 * song then wraps round to its first part when it [loops]. [size]: parts in the song.
 */
fun songNextIndex(playing: Int, queued: Int, size: Int, loops: Boolean): Int {
    if (size <= 0) return -1
    val next = if (queued >= 0) queued else playing + 1
    return when {
        next < size -> next
        loops -> 0
        else -> -1
    }
}

/** Bars the whole song lasts (each part's scene times its repeats, or its own length). */
val Project.songBars: Int get() = (song.sumOf { partSteps(it) } + stepsPerBar - 1) / stepsPerBar

/** The song step part [index] starts on: its place in the song (each part as long as [partSteps]). */
fun Project.songStepOf(index: Int): Int = song.take(index.coerceIn(0, song.size)).sumOf { partSteps(it) }

/** Song part [index]'s track [key] plays scene [scene] (the timeline's long press); one undo step. */
fun Project.withPartScene(index: Int, key: String, scene: Int): Project {
    if (index !in song.indices || key !in partKeys || scene !in allScenes.indices) return this
    val before = song[index]
    val after = before.withScene(key, scene)
    if (after == before) return this
    return copy(song = song.mapIndexed { i, p -> if (i == index) after else p }).followArrangement(this, song.indices.toList())
}
