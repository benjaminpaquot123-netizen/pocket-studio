package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * What an automation lane moves: [kind] in [AutoTarget] order (same numbers as the engine),
 * the channel [ch] (4 = the master, for insert parameters), the insert [slot] or the bus,
 * and the effect [param].
 */
@Serializable
data class AutoTarget(val kind: Int, val ch: Int = 0, val slot: Int = 0, val param: Int = 0) {
    fun pack(): Int = (kind shl 16) or (ch shl 12) or (slot shl 8) or param

    /** The engine reads 4 bits of channel: the pads' own strips (16..31) cannot be automated. */
    val isValid: Boolean get() = kind in VOLUME..BUS_PARAM && ch in 0..MAX_CH

    companion object {
        const val MAX_CH = 15
        const val VOLUME = 0
        const val PAN = 1
        const val SEND = 2
        const val INSERT_PARAM = 3
        const val BUS_PARAM = 4
    }
}

/**
 * One automated setting, as a clip. [LOOP]: a value 0..1 (or nothing) on each step of the drum
 * loop, the same in every scene. [PER_SCENE]: each scene has its own steps ([sceneValues]).
 * [SONG]: one curve over the whole song, a point per bar ([songValues]), sliding between points.
 */
@Serializable
data class AutomationLane(
    val target: AutoTarget,
    val values: List<Float?> = List(STEPS) { null },
    val mode: Int = LOOP,
    val sceneValues: Map<Int, List<Float?>> = emptyMap(),
    val songValues: List<Float?> = emptyList(),
) {
    fun sanitized(): AutomationLane = copy(
        values = clean(values, STEPS),
        mode = mode.coerceIn(LOOP, SONG),
        sceneValues = sceneValues.filterKeys { it in 0 until Scene.MAX }.mapValues { clean(it.value, STEPS) },
        songValues = clean(songValues, songValues.size.coerceAtMost(MAX_SONG_BARS)),
    )

    /** The steps playing in scene [scene]. */
    fun valuesFor(scene: Int): List<Float?> = if (mode == PER_SCENE) sceneValues[scene] ?: List(STEPS) { null } else values

    fun withPoint(step: Int, value: Float?, scene: Int = 0): AutomationLane {
        if (step !in 0 until STEPS) return this
        val updated = valuesFor(scene).mapIndexed { i, v -> if (i == step) value?.coerceIn(0f, 1f) else v }
        return if (mode == PER_SCENE) copy(sceneValues = sceneValues + (scene to updated)) else copy(values = updated)
    }

    /** The song curve's point on bar [bar] (null erases). */
    fun withSongPoint(bar: Int, value: Float?): AutomationLane {
        if (bar !in 0 until MAX_SONG_BARS) return this
        val list = (songValues + List((bar + 1 - songValues.size).coerceAtLeast(0)) { null }).toMutableList()
        list[bar] = value?.coerceIn(0f, 1f)
        return copy(songValues = list.dropLastWhile { it == null })
    }

    /**
     * The song curve as the engine plays it: a value for each of [bars] bars, straight lines
     * between the points drawn, held before the first and after the last. Empty when none.
     */
    fun songCurve(bars: Int): List<Float> {
        val points = songValues.withIndex().filter { it.value != null }.map { it.index to it.value!! }
        if (points.isEmpty()) return emptyList()
        return List(bars.coerceIn(1, MAX_SONG_BARS)) { b ->
            val after = points.firstOrNull { it.first >= b } ?: return@List points.last().second
            val before = points.lastOrNull { it.first <= b } ?: return@List after.second
            if (after.first == before.first) before.second
            else before.second + (after.second - before.second) * (b - before.first) / (after.first - before.first)
        }
    }

    companion object {
        const val STEPS = 64
        const val MAX_LANES = 8
        const val LOOP = 0
        const val PER_SCENE = 1
        const val SONG = 2
        const val MAX_SONG_BARS = 256

        private fun clean(list: List<Float?>, size: Int) = (list + List((size - list.size).coerceAtLeast(0)) { null }).take(size)
            .map { v -> v?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) }

        fun sanitize(lanes: List<AutomationLane>): List<AutomationLane> =
            lanes.filter { it.target.isValid }.distinctBy { it.target }.take(MAX_LANES).map { it.sanitized() }
    }
}

/** The insert in [slot] of strip [ch] ([Mixer.MASTER] = the master's), if any. */
fun Mixer.insertAt(ch: Int, slot: Int): Insert? =
    (if (ch == Mixer.MASTER) master else channels.getOrNull(ch)?.inserts)?.getOrNull(slot)

/**
 * An insert parameter lane only means something for the effect it was drawn on: lanes whose slot
 * holds another effect type than in [before] are dropped (else they would drive the new effect's
 * knobs with the old curve).
 */
fun Project.withoutStaleInsertLanes(before: Project): Project {
    val kept = automation.filter { l ->
        val t = l.target
        t.kind != AutoTarget.INSERT_PARAM || mixer.insertAt(t.ch, t.slot)?.type == before.mixer.insertAt(t.ch, t.slot)?.type
    }
    return if (kept.size == automation.size) this else copy(automation = kept)
}

/**
 * Where each loop or per-scene lane (by index) has left its setting when song part [index]
 * begins, as the song played from its start leaves it: a step without a point keeps the last
 * value written (the engine's rule), so the part before decides. Lanes that wrote nothing yet
 * are not listed (the mixer's own setting holds); song lanes set theirs on every step.
 */
fun Project.automationBefore(index: Int): Map<Int, Float> {
    val out = HashMap<Int, Float>()
    val scenes = allScenes
    song.take(index.coerceIn(0, song.size)).forEach { part ->
        // The engine reads the lanes on the drum loop's step (8..64 steps).
        val drumSteps = (scenes.getOrNull(part.scene)?.drums?.steps ?: DrumPattern().steps).coerceIn(8, AutomationLane.STEPS)
        val total = sceneSteps(part.scene) * part.repeats
        automation.forEachIndexed { i, lane ->
            if (lane.mode == AutomationLane.SONG) return@forEachIndexed
            val values = lane.valuesFor(part.scene)
            // The last step of this part that writes a value.
            for (local in total - 1 downTo maxOf(0, total - drumSteps)) {
                val v = values.getOrNull(local % drumSteps) ?: continue
                out[i] = v
                break
            }
        }
    }
    return out
}
