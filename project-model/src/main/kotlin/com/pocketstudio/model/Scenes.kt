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
) {
    /** Steps the scene lasts before it loops: its longest part, rounded up to whole bars. */
    val steps: Int get() {
        val longest = maxOf(drums.steps, acid.length, synthNotes.steps)
        return (longest + 15) / 16 * 16
    }

    val isEmpty: Boolean get() = drums.lanes.all { lane -> lane.hits.all { it == null } } &&
        acid.steps.take(acid.length).none { it.gate } && synthNotes.notes.isEmpty()

    companion object {
        const val MAX = 8
        val NAMES = listOf("A", "B", "C", "D", "E", "F", "G", "H")
    }
}

/** One line of the song: play [scene] [repeats] times. */
@Serializable
data class SongPart(val scene: Int = 0, val repeats: Int = 1) {
    companion object {
        const val MAX_REPEATS = 16
        const val MAX_PARTS = 32
    }
}

/** The scene being edited and played is mirrored in the project's own drums / acid / notes. */
fun Project.currentSceneContent(): Scene =
    Scene(Scene.NAMES.getOrElse(scene) { "?" }, drums, acid, synthNotes)

/** All scenes, the current one included (a project that never used scenes has just one). */
val Project.allScenes: List<Scene> get() = scenes.ifEmpty { listOf(currentSceneContent()) }

/**
 * Keeps [Project.scenes] in step with the current content (called by [Project.sanitized]).
 * A project with a single scene keeps an empty list: older files stay byte-for-byte the same.
 */
internal fun Project.syncScenes(): Project {
    if (scenes.isEmpty()) return copy(scene = 0, song = song.filter { it.scene == 0 }.take(SongPart.MAX_PARTS)
        .map { it.copy(repeats = it.repeats.coerceIn(1, SongPart.MAX_REPEATS)) })
    val list = scenes.take(Scene.MAX)
    val index = scene.coerceIn(0, list.lastIndex)
    return copy(
        scene = index,
        scenes = list.mapIndexed { i, s -> if (i == index) currentSceneContent().copy(name = Scene.NAMES[i]) else s.copy(name = Scene.NAMES[i]) },
        song = song.filter { it.scene in list.indices }.take(SongPart.MAX_PARTS)
            .map { it.copy(repeats = it.repeats.coerceIn(1, SongPart.MAX_REPEATS)) },
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
