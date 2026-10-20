package com.pocketstudio.model

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
    }
}

/**
 * One line of the song: play [scene] [repeats] times. [muted]: tracks silent in this part (the
 * timeline): "drums", "acid", "inst:<id>" (instrument track), "audio:<id>" (audio track).
 */
@Serializable
data class SongPart(val scene: Int = 0, val repeats: Int = 1, val muted: Set<String> = emptySet()) {
    fun plays(key: String) = key !in muted

    companion object {
        const val DRUMS = "drums"
        const val ACID = "acid"
        fun inst(id: Int) = "inst:$id"
        fun audio(id: Int) = "audio:$id"

        const val MAX_REPEATS = 16
        const val MAX_PARTS = 32
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
    if (scenes.isEmpty()) return copy(scene = 0, song = song.filter { it.scene == 0 }.take(SongPart.MAX_PARTS)
        .map { it.copy(repeats = it.repeats.coerceIn(1, SongPart.MAX_REPEATS), muted = it.muted.filter { k -> k in partKeys }.toSet()) })
    val all = scenes.take(Scene.MAX)
    val index = scene.coerceIn(0, all.lastIndex)
    // Empty scenes at the end go when nothing uses them (a tap on a far slot leaves no trail of them).
    val used = song.map { it.scene }.toSet() +
        automation.flatMap { lane -> lane.sceneValues.filterValues { v -> v.any { it != null } }.keys }
    var size = all.size
    while (size - 1 > index && all[size - 1].isEmpty && size - 1 !in used) size--
    val list = all.take(size)
    return copy(
        scene = index,
        scenes = list.mapIndexed { i, s ->
            if (i == index) currentSceneContent().copy(name = Scene.NAMES[i])
            else s.copy(name = Scene.NAMES[i], trackNotes = s.trackNotes.filterKeys { id -> tracks.any { it.id == id } }.mapValues { it.value.sanitized() })
        },
        song = song.filter { it.scene in list.indices }.take(SongPart.MAX_PARTS)
            .map { it.copy(repeats = it.repeats.coerceIn(1, SongPart.MAX_REPEATS), muted = it.muted.filter { k -> k in partKeys }.toSet()) },
    )
}

/** Makes [index] the current scene (adding empty scenes up to it if needed). */
fun Project.switchScene(index: Int): Project {
    val synced = syncScenes()
    if (index !in 0 until Scene.MAX) return synced
    val existing = synced.allScenes
    val list = existing + List((index + 1 - existing.size).coerceAtLeast(0)) { Scene() }
    val target = list[index]
    return synced.copy(
        scenes = list,
        scene = index,
        // Pad sounds and the 303's sound and root are shared by all scenes: only the notes change.
        drums = target.drums.copy(lanes = target.drums.lanes.mapIndexed { i, l -> l.copy(sample = drums.lanes.getOrNull(i)?.sample, pan = drums.lanes.getOrNull(i)?.pan ?: 0f) }),
        acid = acid.copy(length = target.acid.length, steps = target.acid.steps, locked = target.acid.locked),
        synthNotes = target.synthNotes,
        tracks = tracks.map { t -> t.copy(notes = target.trackNotes[t.id] ?: NoteClip(steps = t.notes.steps)) },
    ).syncScenes()
}

/** Copies the current scene into [index] (to start a variation from it), its per-scene automation too. */
fun Project.copySceneTo(index: Int): Project {
    if (index !in 0 until Scene.MAX) return this
    val synced = syncScenes()
    val existing = synced.allScenes
    val list = (existing + List((index + 1 - existing.size).coerceAtLeast(0)) { Scene() }).toMutableList()
    list[index] = synced.currentSceneContent()
    val automation = synced.automation.map { lane ->
        val own = lane.sceneValues[synced.scene]
        lane.copy(sceneValues = if (own != null) lane.sceneValues + (index to own) else lane.sceneValues - index)
    }
    return synced.copy(scenes = list, automation = automation).syncScenes()
}

/**
 * Empties the scene shown (its drum hits, its acid line, the notes of every instrument track and
 * its own automation values); sounds, lengths and the other scenes stay.
 */
fun Project.clearCurrentScene(): Project = copy(
    drums = drums.copy(lanes = drums.lanes.map { it.copy(hits = emptyMap()) }),
    acid = acid.copy(steps = acid.steps.map { it.copy(gate = false) }),
    synthNotes = synthNotes.copy(notes = emptyList()),
    tracks = tracks.map { it.copy(notes = it.notes.copy(notes = emptyList())) },
    automation = automation.map { it.copy(sceneValues = it.sceneValues - scene) },
).syncScenes()

/** Another scene that holds exactly what scene [index] holds (it was copied and not changed yet), or null. */
fun Project.copyOf(index: Int): Int? {
    val all = allScenes
    val s = all.getOrNull(index)?.takeUnless { it.isEmpty } ?: return null
    return all.indices.firstOrNull { it != index && all[it].copy(name = "") == s.copy(name = "") }
}

/** The first empty scene slot other than the one shown (a new scene to fill), or null when all 24 are taken. */
fun Project.firstEmptyScene(): Int? {
    val all = allScenes
    return all.indices.firstOrNull { it != scene && all[it].isEmpty } ?: all.size.takeIf { it < Scene.MAX }
}

/** A project shaped as if [index] were current, for sending a scene to the engine ahead of time. */
fun Project.sceneView(index: Int): Project = if (index == scene) this else switchScene(index)

/** Steps scene [index] lasts in this project's time signature. */
fun Project.sceneSteps(index: Int): Int = allScenes.getOrNull(index)?.stepsFor(stepsPerBar) ?: stepsPerBar

/** Every track key a song part can mute, in timeline order. */
val Project.partKeys: List<String> get() =
    listOf(SongPart.DRUMS, SongPart.ACID) + instrumentTracks.map { SongPart.inst(it.id) } + audioTracks.map { SongPart.audio(it.id) }

/** Song part [part] as played: its scene with the muted tracks emptied (audio tracks: see [SongPart.plays]). */
fun Project.partView(part: SongPart): Project {
    var v = sceneView(part.scene)
    if (part.muted.isEmpty()) return v
    if (!part.plays(SongPart.DRUMS)) v = v.copy(drums = v.drums.copy(lanes = v.drums.lanes.map { it.copy(hits = emptyMap()) }))
    if (!part.plays(SongPart.ACID)) v = v.copy(acid = v.acid.copy(steps = v.acid.steps.map { it.copy(gate = false) }))
    v.instrumentTracks.filterNot { part.plays(SongPart.inst(it.id)) }.forEach { t -> v = v.withTrack(t.copy(notes = t.notes.copy(notes = emptyList()))) }
    return v
}

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
            start = (c.start / from * to + (c.start % from).coerceAtMost(to - 1)).coerceAtMost(AudioClip.MAX_START / to * to),
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

/** Bars the whole song lasts (each part's scene times its repeats). */
val Project.songBars: Int get() = (song.sumOf { sceneSteps(it.scene) * it.repeats } + stepsPerBar - 1) / stepsPerBar
