package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * A version of the song kept aside: what a Live take replaced (the song never goes without a way
 * back), or the one a restore put away. Its music: the parts, the song curves and every scene's
 * content (notes played live during a take go into the scenes). The sounds, the mixer and the
 * tracks stay the project's.
 */
@Serializable
data class SongVersion(
    /** Why it was put aside: [BEFORE_TAKE] or [RESTORE] (the screen names it, with [at]'s time). */
    val kind: Int = BEFORE_TAKE,
    /** When it was put aside (ms since 1970). */
    val at: Long = 0L,
    val song: List<SongPart> = emptyList(),
    val automation: List<AutomationLane> = emptyList(),
    val scenes: List<Scene> = emptyList(),
    val scene: Int = 0,
) {
    /** Its length in bars, as the project [p] would play it. */
    fun bars(p: Project): Int {
        val steps = p.copy(song = song, scenes = scenes).let { q -> song.sumOf { q.partSteps(it) } }
        return (steps + p.stepsPerBar - 1) / p.stepsPerBar.coerceAtLeast(1)
    }

    fun sanitized(): SongVersion {
        val kept = scenes.take(Scene.MAX)
        return copy(
            kind = kind.coerceIn(BEFORE_TAKE, RESTORE),
            song = song.take(SongPart.MAX_PARTS),
            automation = AutomationLane.sanitize(automation),
            scenes = kept,
            scene = scene.coerceIn(0, (kept.size - 1).coerceAtLeast(0)),
        )
    }

    /**
     * In range for project [p] (its tracks and the keys its parts can mute): every scene cleaned as
     * the project's own (a loop of 0 steps would hang [bars]), the parts on scenes it holds.
     */
    internal fun sanitized(p: Project): SongVersion {
        val base = sanitized()
        val trackIds = p.tracks.mapTo(HashSet()) { it.id }
        val cleaned = base.scenes.map { it.cleanedFor(trackIds) }
        val count = cleaned.size.coerceAtLeast(1)
        return base.copy(
            scenes = cleaned,
            song = base.song.filter { it.scene in 0 until count }.map { it.sanitized(p.partKeys, count, p.remix.setId != null) },
        )
    }

    /** The same music as [other] (its time aside): keeping both would only take room. */
    fun sameMusicAs(other: SongVersion): Boolean =
        song == other.song && automation == other.automation && scenes == other.scenes && scene == other.scene

    companion object {
        /** Versions kept at most (the oldest go first). */
        const val MAX = 8
        /** The song a Live take replaced (or wrote after). */
        const val BEFORE_TAKE = 0
        /** The song a restore of another version replaced. */
        const val RESTORE = 1
    }
}

/**
 * The sound files the versions kept aside name (a pad's sound in one of their scenes): kept on
 * disk and in a .zip with the project, not loaded (a restore loads them, see [restoreVersion]).
 */
val Project.versionSampleFiles: Set<String> get() =
    songVersions.flatMap { v -> v.scenes.flatMap { s -> s.drums.lanes.mapNotNull { it.sample?.file } } }.toSet()

/** The song as it is now, as a version of [kind] put aside at [at]. */
fun Project.currentVersion(kind: Int, at: Long): SongVersion {
    val synced = sanitized()
    return SongVersion(kind, at, synced.song, synced.automation, synced.allScenes, synced.scene)
}

/**
 * The project with its song as it is now kept aside as a version ([kind], [at]), newest first,
 * [SongVersion.MAX] at most. Nothing is kept when there is no song yet (nothing to lose).
 */
fun Project.withVersionKept(kind: Int, at: Long): Project {
    if (song.isEmpty()) return this
    return copy(songVersions = songVersions.keeping(currentVersion(kind, at)))
}

/** [kept] first, then these, [SongVersion.MAX] at most; not again when the newest is the same music. */
private fun List<SongVersion>.keeping(kept: SongVersion): List<SongVersion> =
    if (firstOrNull()?.sameMusicAs(kept) == true) this else (listOf(kept) + this).take(SongVersion.MAX)

/**
 * [w], a take written into this project, with this project's song kept aside as the version
 * before it: no Live take replaces the song without a way back (nor the scenes the notes played
 * live went into, even before there was a song). Nothing written: unchanged.
 */
fun Project.keepingVersion(w: TakeWritten, at: Long): TakeWritten {
    if (w.parts == 0 || (song.isEmpty() && allScenes.all { it.isEmpty })) return w
    val kept = currentVersion(SongVersion.BEFORE_TAKE, at)
    return w.copy(project = w.project.copy(songVersions = w.project.songVersions.keeping(kept)))
}

/**
 * Version [index] becomes the song (its parts, curves and scenes); the song it replaces is kept
 * aside in its place (put aside at [at]), so a second restore brings it back. Unchanged when
 * there is no such version.
 */
fun Project.restoreVersion(index: Int, at: Long): Project {
    val v = songVersions.getOrNull(index) ?: return this
    val now = currentVersion(SongVersion.RESTORE, at)
    val others = songVersions.filterIndexed { i, _ -> i != index }
    val scenes = v.scenes.ifEmpty { allScenes }
    val restored = copy(song = v.song, automation = withSongCurvesOf(v.automation))
        .showing(scenes, v.scene.coerceIn(0, scenes.lastIndex))
    return restored.copy(songVersions = others.keeping(now)).sanitized()
}

/**
 * This project's automation with the song curves of [kept] (a version's lanes): only what belongs
 * to the song (its points, jumps, a point per beat, « song only », a lane playing its song curve)
 * comes back; the loops and per-scene curves drawn since stay, a lane of a track deleted since
 * stays gone. A lane left with nothing goes.
 */
internal fun Project.withSongCurvesOf(kept: List<AutomationLane>): List<AutomationLane> {
    fun exists(t: AutoTarget): Boolean {
        val inst = AutoTarget.trackOfChannel(t.ch)
        val audio = AudioTrack.ofChannel(t.ch)
        return (inst == null || instrumentTracks.any { it.id == inst }) && (audio == null || audioTracks.any { it.id == audio })
    }
    val byTarget = kept.filter { exists(it.target) }.associateBy { it.target }
    fun songOf(lane: AutomationLane, from: AutomationLane?): AutomationLane = lane.copy(
        songValues = from?.songValues.orEmpty(),
        songJumps = from?.songJumps.orEmpty(),
        songPerBeat = from?.songPerBeat ?: false,
        songOnly = from?.songOnly ?: false,
        mode = when {
            from != null && (from.mode == AutomationLane.SONG || lane.mode == AutomationLane.SONG) -> from.mode
            from == null && lane.mode == AutomationLane.SONG -> AutomationLane.LOOP
            else -> lane.mode
        },
    )
    fun AutomationLane.blank(): Boolean =
        songValues.all { it == null } && values.all { it == null } && sceneValues.values.all { v -> v.all { it == null } }
    val updated = automation.map { songOf(it, byTarget[it.target]) }.filterNot { it.blank() }
    val added = byTarget.values.filter { k -> automation.none { it.target == k.target } }
        .map { k -> songOf(AutomationLane(k.target, hold = k.hold), k) }
        .filterNot { it.blank() }
    return updated + added
}

/** Version [index] forgotten. */
fun Project.dropVersion(index: Int): Project =
    if (index !in songVersions.indices) this else copy(songVersions = songVersions.filterIndexed { i, _ -> i != index })
