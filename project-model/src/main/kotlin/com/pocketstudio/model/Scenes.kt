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

    val isEmpty: Boolean get() = drums.lanes.all { lane -> lane.hits.all { it == null } } &&
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
    val list = scenes.take(Scene.MAX)
    val index = scene.coerceIn(0, list.lastIndex)
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
        drums = target.drums.copy(lanes = target.drums.lanes.mapIndexed { i, l -> l.copy(sample = drums.lanes.getOrNull(i)?.sample) }),
        acid = acid.copy(length = target.acid.length, steps = target.acid.steps, locked = target.acid.locked),
        synthNotes = target.synthNotes,
        tracks = tracks.map { t -> t.copy(notes = target.trackNotes[t.id] ?: NoteClip(steps = t.notes.steps)) },
    ).syncScenes()
}

/** Copies the current scene into [index] (to start a variation from it). */
fun Project.copySceneTo(index: Int): Project {
    if (index !in 0 until Scene.MAX) return this
    val synced = syncScenes()
    val existing = synced.allScenes
    val list = (existing + List((index + 1 - existing.size).coerceAtLeast(0)) { Scene() }).toMutableList()
    list[index] = synced.currentSceneContent()
    return synced.copy(scenes = list).syncScenes()
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
 * bars (64 steps at most), so its grid stays whole bars.
 */
fun Project.withBeatsPerBar(beats: Int): Project {
    val from = stepsPerBar
    val to = beats.coerceIn(Project.MIN_BEATS, Project.MAX_BEATS) * 4
    fun convert(d: DrumPattern): DrumPattern {
        val bars = (d.steps / from).coerceAtLeast(1)
        return d.withSteps((bars * to).coerceAtMost(DrumPattern.MAX_STEPS / to * to))
    }
    return copy(
        beatsPerBar = to / 4,
        drums = convert(drums),
        scenes = scenes.map { it.copy(drums = convert(it.drums)) },
    ).sanitized()
}

/** Bars the whole song lasts (each part's scene times its repeats). */
val Project.songBars: Int get() = song.sumOf { sceneSteps(it.scene) / stepsPerBar * it.repeats }
