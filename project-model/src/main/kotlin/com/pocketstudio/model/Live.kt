package com.pocketstudio.model

/**
 * The live grid (« Live » page): one column per track, one row per scene. A pad launches its
 * track's part of that scene on the next quantum ([Project.liveQuantum] bars), each track counting
 * from its own launch; a row launches the whole scene. The engine names each column a "slot".
 */
object LiveSlots {
    const val DRUMS = 0
    const val ACID = 1
    /** + instrument track id (0..7). */
    const val INST = 2
    /** + audio track id (0..3). */
    const val AUDIO = 10
    const val REMIX = 14
    const val COUNT = 15
    /** Every slot: a whole row. */
    const val ALL = (1 shl COUNT) - 1

    /** Launch quantum choices, in bars. */
    val QUANTA = listOf(1, 2, 4)

    /** The slot of a track key (see [SongPart]), or null. */
    fun of(key: String): Int? = when {
        key == SongPart.DRUMS -> DRUMS
        key == SongPart.ACID -> ACID
        key == SongPart.REMIX -> REMIX
        key.startsWith("inst:") -> key.removePrefix("inst:").toIntOrNull()?.takeIf { it in 0 until InstrumentTrack.MAX }?.let { INST + it }
        key.startsWith("audio:") -> key.removePrefix("audio:").toIntOrNull()?.takeIf { it in 0 until AudioTrack.MAX }?.let { AUDIO + it }
        else -> null
    }

    /** The track key of [slot]. */
    fun key(slot: Int): String = when {
        slot == DRUMS -> SongPart.DRUMS
        slot == ACID -> SongPart.ACID
        slot == REMIX -> SongPart.REMIX
        slot in INST until INST + InstrumentTrack.MAX -> SongPart.inst(slot - INST)
        slot in AUDIO until AUDIO + AudioTrack.MAX -> SongPart.audio(slot - AUDIO)
        else -> ""
    }

    fun mask(slot: Int): Int = 1 shl slot

    /** The engine's slot states (NativeEngine.slotStates): a slot playing the row's scene (a scene swap), no launch waiting. */
    const val ROW_TAG = -2
    const val NO_LAUNCH = -3
}

/**
 * One column as the engine plays it: [playing] the scene heard (-1: stopped), [pending] the scene
 * armed (-1: a stop; null: nothing waits), [start] the transport step its part started on.
 */
data class LiveSlot(val playing: Int = 0, val pending: Int? = null, val start: Int = 0)

/**
 * What the grid plays: [row] the scene last launched as a whole (the scene shown and edited),
 * each slot its own scene; [pendingRow] a whole row armed.
 */
data class LiveState(
    val row: Int = 0,
    val slots: List<LiveSlot> = List(LiveSlots.COUNT) { LiveSlot(row) },
    val pendingRow: Int? = null,
) {
    /** True when some track plays another scene than the row, or is stopped. */
    val diverged: Boolean get() = slots.any { it.playing != row }
    val hasPending: Boolean get() = pendingRow != null || slots.any { it.pending != null }
    fun slot(k: Int): LiveSlot = slots.getOrElse(k) { LiveSlot(row) }

    /** [mask]'s slots wait for [scene] (-1: a stop); the last choice for a slot wins. */
    fun armed(mask: Int, scene: Int): LiveState = copy(
        slots = slots.mapIndexed { k, s -> if (mask and (1 shl k) != 0) s.copy(pending = scene.coerceAtLeast(-1)) else s },
        // A slot armed on its own breaks a row that waited (the engine launches it as tracks then).
        pendingRow = if (mask == LiveSlots.ALL && scene >= 0) scene else null,
    )

    /** What [mask]'s slots waited for is dropped. */
    fun cancelled(mask: Int): LiveState = copy(
        slots = slots.mapIndexed { k, s -> if (mask and (1 shl k) != 0) s.copy(pending = null) else s },
        pendingRow = pendingRow.takeIf { mask == 0 },
    )

    /**
     * The engine launched [slot] on [scene] (-1: stopped) at transport [step]. Every slot on one
     * scene again: that scene is the row (the grid is a plain scene once more).
     */
    fun launched(slot: Int, scene: Int, step: Int): LiveState {
        if (slot !in slots.indices) return this
        val next = slots.mapIndexed { k, s -> if (k == slot) LiveSlot(scene.coerceAtLeast(-1), null, step) else s }
        val whole = next.first().playing.takeIf { p -> p >= 0 && next.all { it.playing == p } }
        val stillWaiting = next.any { it.pending != null }
        return copy(
            row = whole ?: row,
            slots = next,
            pendingRow = if (whole != null && whole == pendingRow && !stillWaiting) null else pendingRow,
        )
    }

    companion object {
        /** Every slot plays [scene] from transport step [start] (play, a scene swap of the song). */
        fun on(scene: Int, start: Int = 0) = LiveState(scene, List(LiveSlots.COUNT) { LiveSlot(scene, null, start) })
    }
}

/**
 * The grid as the engine plays song part [part] (null: scene [scene] on its own) after a scene swap
 * or ▶: each track on the scene the part gives it ([SongPart.sceneFor]), stopped (-1) when the part
 * mutes it. [states]: the engine's slots (per slot: tag playing, tag waiting, start step, as
 * NativeEngine.slotStates gives them): exact starts, and a slot launched on its own since keeps
 * its scene. Without them, each slot starts on [start] less the bars it carries on ([SongPart.from]).
 */
fun Project.liveStateOf(part: SongPart?, scene: Int, start: Int, states: IntArray? = null): LiveState {
    val row = part?.scene ?: scene
    val columns = liveColumns.toSet()
    val slots = List(LiveSlots.COUNT) { k ->
        val key = LiveSlots.key(k)
        val ofPart = when {
            part == null || key !in columns -> row
            !part.plays(key) -> -1
            key == SongPart.REMIX -> row
            else -> part.sceneFor(key)
        }
        val tag = states?.getOrNull(k * 3)
        LiveSlot(
            playing = if (tag == null || tag == LiveSlots.ROW_TAG || tag == LiveSlots.NO_LAUNCH) ofPart else tag,
            pending = states?.getOrNull(k * 3 + 1)?.takeIf { it != LiveSlots.NO_LAUNCH },
            start = states?.getOrNull(k * 3 + 2) ?: (start - (part?.fromBars(key) ?: 0) * stepsPerBar),
        )
    }
    return LiveState(row, slots)
}

/** The live grid's columns in order: drums, acid, each instrument track, each audio track, and the remix when one is loaded. */
val Project.liveColumns: List<String> get() = partKeys + if (remix.setId != null) listOf(SongPart.REMIX) else emptyList()

/** The launch quantum in steps. */
val Project.liveQuantumSteps: Int get() = liveQuantum.coerceIn(1, 4) * stepsPerBar

/** Track [key] has something to play in scene [scene] (the pad shows it). */
fun Project.cellHasContent(key: String, scene: Int): Boolean {
    val s = allScenes.getOrNull(scene) ?: return false
    return when {
        key == SongPart.DRUMS -> s.drums.lanes.any { lane -> lane.hits.keys.any { it < s.drums.steps } }
        key == SongPart.ACID -> s.acid.steps.take(s.acid.length).any { it.gate }
        key == SongPart.REMIX -> remix.setId != null
        key.startsWith("inst:") -> notesOf(s, key.removePrefix("inst:").toIntOrNull() ?: -1)?.notes?.isNotEmpty() == true
        key.startsWith("audio:") -> {
            val clip = audioTracks.firstOrNull { "audio:${it.id}" == key }?.clip
            clip != null && !clip.song && clip.belongsTo(scene)
        }
        else -> false
    }
}

private fun Project.notesOf(s: Scene, id: Int): NoteClip? = when {
    id == 0 -> s.synthNotes
    tracks.any { it.id == id } -> s.trackNotes[id] ?: NoteClip()
    else -> null
}

/** Steps track [key]'s part of scene [scene] lasts before it loops (the pad's position ring). */
fun Project.slotLoopSteps(key: String, scene: Int): Int {
    val s = allScenes.getOrNull(scene) ?: return stepsPerBar
    val steps = when {
        key == SongPart.DRUMS -> s.drums.steps
        key == SongPart.ACID -> s.acid.length
        key.startsWith("inst:") -> notesOf(s, key.removePrefix("inst:").toIntOrNull() ?: -1)?.steps ?: 0
        key.startsWith("audio:") -> audioTracks.firstOrNull { "audio:${it.id}" == key }?.clip?.loopSteps?.takeIf { it > 0 } ?: 0
        else -> 0
    }
    return steps.takeIf { it > 0 } ?: s.stepsFor(stepsPerBar)
}

/** The transport step the next launch fires on when armed during [transportStep] (-1: stopped, the first step). */
fun nextLaunchStep(transportStep: Int, quantumSteps: Int): Int {
    if (transportStep < 0) return 0
    val q = quantumSteps.coerceAtLeast(1)
    return (transportStep / q + 1) * q
}

/** Where a part that started on [start] and loops every [loop] steps is at [transportStep], 0..1. */
fun livePosition(transportStep: Int, start: Int, loop: Int): Float {
    if (transportStep < 0 || loop <= 0) return 0f
    return Math.floorMod(transportStep - start, loop) / loop.toFloat()
}
