package com.pocketstudio.model

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * An empty cell of the Live grid filled from the Live page (Benjamin's list n° 9, choice 1A):
 * played and recorded there, a sound of the library, or a copy of another cell. Only sounds go
 * into a cell: the live effects stay a stage tool (choice 3B), the performance take keeps them.
 */
object LiveCells {
    /** The kinds of track a cell belongs to: a cell copies only onto one of the same kind. */
    enum class Kind { DRUMS, ACID, NOTES, AUDIO, REMIX }

    fun kindOf(key: String): Kind? = when {
        key == SongPart.DRUMS -> Kind.DRUMS
        key == SongPart.ACID -> Kind.ACID
        key == SongPart.REMIX -> Kind.REMIX
        key.startsWith("inst:") -> Kind.NOTES
        key.startsWith("audio:") -> Kind.AUDIO
        else -> null
    }

    /** A cell of track [key] can be played into: the drums (their pads) and instrument tracks (Live › Notes). */
    fun canRecord(key: String): Boolean = kindOf(key) == Kind.DRUMS || kindOf(key) == Kind.NOTES

    /** Cell of [from] can be copied onto a cell of [to]: the same kind of track (the remix has no cells of its own). */
    fun canCopy(from: String, to: String): Boolean = kindOf(from) != null && kindOf(from) == kindOf(to) && kindOf(to) != Kind.REMIX

    /**
     * The whole bars a recording from [origin] to [end] (in sixteenths of the transport) keeps: the
     * nearest number, so the loop stays in time whether the cell was touched a hair early or late;
     * one at least. There is no maximum (choice « pas de limite »): see [keptBars].
     */
    fun bars(origin: Double, end: Double, stepsPerBar: Int): Int {
        val bar = stepsPerBar.coerceAtLeast(1).toDouble()
        val n = (end - origin) / bar
        if (!n.isFinite() || n < 1.0) return 1
        return n.roundToInt().coerceAtLeast(1)
    }

    /** The bars a cell of [key] can loop over at most (its track's longest loop), whole bars. */
    fun maxBars(key: String, stepsPerBar: Int): Int {
        val bar = stepsPerBar.coerceAtLeast(1)
        val longest = if (kindOf(key) == Kind.DRUMS) DrumPattern.MAX_STEPS else NoteClip.MAX_STEPS
        return (longest / bar).coerceAtLeast(1)
    }

    /** Of a recording [bars] long, the bars the cell keeps: all of them, or the last ones its loop holds. */
    fun keptBars(key: String, bars: Int, stepsPerBar: Int): Int = bars.coerceIn(1, maxBars(key, stepsPerBar))

    /**
     * A sound of the library on a cell: a loop (half a bar or more at [bpm]) is fitted to the
     * nearest power-of-two count of whole bars (1, 2, 4, 8 ... up to 64) so it follows the tempo;
     * a short sound (a hit) gets null: it plays once per bar, as it is.
     */
    fun loopBars(seconds: Double, bpm: Double, beatsPerBar: Int): Int? {
        val bars = seconds * bpm / 60.0 / beatsPerBar.coerceAtLeast(1)
        if (!bars.isFinite() || bars < 0.5) return null
        return LOOP_BARS.minBy { abs(ln(it / bars)) }
    }

    private val LOOP_BARS = listOf(1, 2, 4, 8, 16, 32, 64)
}

/**
 * The notes and pad hits played while cell ([key], [scene]) recorded, written into it as its whole
 * content: they start on the bar line [origin] (sixteenths of the transport, the bar of the first
 * thing played) and the recording ended at [end]. Its loop is [LiveCells.bars] whole bars (only
 * the last ones its track can loop over when longer); what was played past its last bar line comes
 * round to its start, as it is heard when the loop goes round. Notes of other tracks are left out.
 * Null: nothing of that track was played, or the cell cannot be recorded into.
 */
fun Project.withRecordedCell(
    key: String,
    scene: Int,
    notes: List<PlayedMemory.KeptNote>,
    hits: List<PlayedMemory.KeptHit>,
    origin: Double,
    end: Double,
): Project? {
    if (scene !in allScenes.indices || !LiveCells.canRecord(key)) return null
    val bar = stepsPerBar.coerceAtLeast(1)
    val all = LiveCells.bars(origin, end, bar)
    val kept = LiveCells.keptBars(key, all, bar)
    // Longer than the loop can hold: the last bars played.
    val from = origin + (all - kept) * bar - PlayedMemory.EARLY_STEPS
    val steps = kept * bar
    val start = origin + (all - kept) * bar
    return when (LiveCells.kindOf(key)) {
        LiveCells.Kind.NOTES -> {
            val id = key.removePrefix("inst:").toIntOrNull() ?: return null
            if (instrumentTracks.none { it.id == id }) return null
            val mine = notes.filter { it.track == id && it.start >= from }
            if (mine.isEmpty()) return null
            val ticks = steps * NoteClip.TICKS_PER_STEP
            val written = mine.take(NoteClip.MAX_NOTES).map { n ->
                // Played a hair before the first bar line: on it.
                val at = Math.floorMod(((n.start - start).coerceAtLeast(0.0) * NoteClip.TICKS_PER_STEP).roundToInt(), ticks)
                Note(at, (n.length * NoteClip.TICKS_PER_STEP).roundToInt().coerceIn(1, NoteClip.MAX_TICKS), n.pitch, n.velocity.coerceIn(0f, 1f))
            }
            editScene(scene) { s ->
                val old = if (id == 0) s.synthNotes else s.trackNotes[id] ?: NoteClip()
                val clip = old.cleared().withSteps(steps).let { it.copy(notes = written) }.sanitized()
                if (id == 0) s.copy(synthNotes = clip) else s.copy(trackNotes = s.trackNotes + (id to clip))
            }
        }
        LiveCells.Kind.DRUMS -> {
            val mine = hits.filter { it.at >= from && it.lane in 0 until DrumPattern.PAD_COUNT }
            if (mine.isEmpty()) return null
            editScene(scene) { s ->
                val lanes = s.drums.lanes.map { it.copy(hits = emptyMap()) }.toMutableList()
                mine.forEach { h ->
                    val step = Math.floorMod((h.at - start).coerceAtLeast(0.0).roundToInt(), steps)
                    val lane = lanes[h.lane]
                    if (lane.hits[step] == null) lanes[h.lane] = lane.copy(hits = lane.hits + (step to Hit(h.velocity.coerceIn(0f, 1f))))
                }
                s.copy(drums = s.drums.copy(steps = steps, lanes = lanes).sanitized())
            }
        }
        else -> null
    }
}

/**
 * Cell ([fromKey], [fromScene]) copied onto cell ([toKey], [toScene]): the same kind of track
 * ([LiveCells.canCopy]); a drum or note cell gets the other's loop and its hits or notes (the
 * sounds stay the target track's), a 303 cell its pattern, an audio cell a copy of the other's
 * clip that belongs to [toScene] (on an audio track with no clip yet). Null when it cannot be done.
 */
fun Project.withCopiedCell(fromKey: String, fromScene: Int, toKey: String, toScene: Int): Project? {
    val scenes = allScenes
    val from = scenes.getOrNull(fromScene) ?: return null
    if (toScene !in scenes.indices || (fromKey == toKey && fromScene == toScene)) return null
    if (!LiveCells.canCopy(fromKey, toKey) || !cellHasContent(fromKey, fromScene)) return null
    return when (LiveCells.kindOf(toKey)) {
        LiveCells.Kind.DRUMS -> editScene(toScene) { s ->
            s.copy(drums = s.drums.copy(steps = from.drums.steps, lanes = s.drums.lanes.mapIndexed { i, l -> l.copy(hits = from.drums.lanes.getOrNull(i)?.hits ?: emptyMap()) }))
        }
        LiveCells.Kind.ACID -> editScene(toScene) { s -> s.copy(acid = from.acid) }
        LiveCells.Kind.NOTES -> {
            val src = fromKey.removePrefix("inst:").toIntOrNull() ?: return null
            val dst = toKey.removePrefix("inst:").toIntOrNull() ?: return null
            if (instrumentTracks.none { it.id == dst }) return null
            val notes = (if (src == 0) from.synthNotes else from.trackNotes[src]) ?: return null
            editScene(toScene) { s -> if (dst == 0) s.copy(synthNotes = notes) else s.copy(trackNotes = s.trackNotes + (dst to notes)) }
        }
        LiveCells.Kind.AUDIO -> {
            val clip = audioTracks.firstOrNull { SongPart.audio(it.id) == fromKey }?.clip ?: return null
            withAudioInCell(toKey, toScene, clip)
        }
        else -> null
    }
}

/**
 * [clip] (its sound file already in the project) as the content of cell ([key], [scene]): on that
 * audio track when it has no clip yet, else on the first audio track without one, else on a new
 * audio track (the cell of another kind of track gets its sound on an audio track of its own, in
 * the same brick). The clip belongs to [scene] and loops there. Null: eight audio tracks all taken.
 */
fun Project.withAudioInCell(key: String, scene: Int, clip: AudioClip): Project? {
    if (scene !in allScenes.indices) return null
    val placed = clip.copy(scene = scene, song = false, once = false, start = 0).sanitized()
    val own = audioTracks.firstOrNull { SongPart.audio(it.id) == key && it.clip == null }
        ?: audioTracks.firstOrNull { it.clip == null }
    if (own != null) return copy(audioTracks = audioTracks.map { if (it.id == own.id) it.copy(clip = placed) else it })
    val grown = addAudioTrack() ?: return null
    val id = grown.audioTracks.first { t -> audioTracks.none { it.id == t.id } }.id
    return grown.copy(audioTracks = grown.audioTracks.map { if (it.id == id) it.copy(clip = placed) else it })
}
