package com.pocketstudio.model

/**
 * The song as a production timeline (« Ligne du temps »): each song part is a column of clips,
 * one per track. A clip is the track's part of the scene that track plays there
 * ([SongPart.sceneFor]); what it holds is drawn small ([ClipPreview]). Everything here is pure: the
 * screen reads it, the ViewModel's edits go through the project's history.
 */

/** One mark of a clip's preview: [start] and [length] in steps of its loop, at height [y] (0 top, 1 bottom), [strength] 0..1. */
data class PreviewMark(val start: Int, val length: Int, val y: Float, val strength: Float = 1f)

/** What a track holds in a scene, drawn small: its own loop of [steps] steps and its [marks]. */
data class ClipPreview(val steps: Int, val marks: List<PreviewMark>) {
    val isEmpty: Boolean get() = marks.isEmpty()

    companion object {
        val EMPTY = ClipPreview(16, emptyList())

        /** Marks kept at most (a busy hi-hat line over 64 steps and 16 pads stays well under). */
        const val MAX_MARKS = 512
    }
}

/** Scene [index]'s content, the one shown fresh from the project's own fields. */
private fun Project.sceneContent(index: Int): Scene? =
    if (index == scene) currentSceneContent() else allScenes.getOrNull(index)

/**
 * What track [key] holds in scene [sceneIndex], to draw on its clip: drum hits (a row per pad
 * used, louder hits stronger), the 303's gated steps or the piano roll's notes (higher notes
 * higher). Audio tracks and unknown keys: [ClipPreview.EMPTY] (their takes are drawn as spans).
 */
fun Project.clipPreview(sceneIndex: Int, key: String): ClipPreview {
    val s = sceneContent(sceneIndex) ?: return ClipPreview.EMPTY
    return when {
        key == SongPart.DRUMS -> drumPreview(s.drums)
        key == SongPart.ACID -> acidPreview(s.acid)
        key.startsWith("inst:") -> {
            val id = key.removePrefix("inst:").toIntOrNull() ?: return ClipPreview.EMPTY
            if (instrumentTracks.none { it.id == id }) return ClipPreview.EMPTY
            notesPreview(if (id == 0) s.synthNotes else s.trackNotes[id] ?: NoteClip())
        }
        else -> ClipPreview.EMPTY
    }
}

internal fun drumPreview(d: DrumPattern): ClipPreview {
    val steps = d.steps.coerceAtLeast(1)
    val used = d.lanes.indices.filter { lane -> d.lanes[lane].hits.keys.any { it < steps } }
    if (used.isEmpty()) return ClipPreview(steps, emptyList())
    val marks = ArrayList<PreviewMark>()
    // The kick (pad 1) at the bottom, as drum lanes read in most apps.
    used.forEachIndexed { row, lane ->
        val y = 1f - (row + 0.5f) / used.size
        d.lanes[lane].hits.entries.sortedBy { it.key }.forEach { (step, hit) ->
            if (step < steps && marks.size < ClipPreview.MAX_MARKS) marks += PreviewMark(step, 1, y, hit.velocity.coerceIn(0.2f, 1f))
        }
    }
    return ClipPreview(steps, marks)
}

internal fun acidPreview(a: AcidPattern): ClipPreview {
    val steps = a.length.coerceIn(1, a.steps.size.coerceAtLeast(1))
    val gated = (0 until minOf(steps, a.steps.size)).filter { a.steps[it].gate }
    if (gated.isEmpty()) return ClipPreview(steps, emptyList())
    fun pitch(i: Int) = a.steps[i].note + 12 * a.steps[i].octave
    val low = gated.minOf(::pitch)
    val high = gated.maxOf(::pitch)
    val marks = gated.map { i ->
        // A slide holds into the next step: a longer bar.
        val length = if (a.steps[i].slide && i + 1 < steps) 2 else 1
        PreviewMark(i, length, heightOf(pitch(i), low, high), if (a.steps[i].accent) 1f else 0.7f)
    }
    return ClipPreview(steps, marks)
}

internal fun notesPreview(c: NoteClip): ClipPreview {
    val steps = c.steps.coerceAtLeast(1)
    val notes = c.notes.filter { it.start < steps }.take(ClipPreview.MAX_MARKS)
    if (notes.isEmpty()) return ClipPreview(steps, emptyList())
    val low = notes.minOf { it.pitch }
    val high = notes.maxOf { it.pitch }
    return ClipPreview(steps, notes.map { n -> PreviewMark(n.start, n.length.coerceIn(1, steps), heightOf(n.pitch, low, high), n.velocity.coerceIn(0.2f, 1f)) })
}

/** [pitch] between [low] and [high] as a height, high notes near the top (a single pitch: the middle). */
private fun heightOf(pitch: Int, low: Int, high: Int): Float {
    if (high <= low) return 0.5f
    // Kept off the edges so a mark is never cut by the clip's border.
    return 0.85f - 0.7f * (pitch - low) / (high - low)
}

/** One song part on the timeline: its [index], first song step [start] and length in [steps]. */
data class PartSpan(val index: Int, val start: Int, val steps: Int) {
    val end: Int get() = start + steps
}

/** Every song part with where it starts and how long it lasts, in song steps. */
val Project.partSpans: List<PartSpan> get() {
    var at = 0
    return song.mapIndexed { i, p ->
        val n = partSteps(p).coerceAtLeast(1)
        PartSpan(i, at, n).also { at += n }
    }
}

/** The song part playing at song step [step] (-1 before the song or past its end). */
fun Project.partAtStep(step: Int): Int {
    if (step < 0) return -1
    return partSpans.firstOrNull { step < it.end }?.index ?: -1
}

/**
 * What a clip says beside its colour: « ↪ » when track [key] carries on from where it was (a part
 * written from the live grid), then the letter of the scene it plays when not its part's own.
 */
fun SongPart.trackBadge(key: String): String =
    (if (fromBars(key) > 0) "↪" else "") + (play[key]?.let { Scene.NAMES.getOrElse(it) { "?" } } ?: "")

/** The mixer channel of track [key] (its M and S), or null (the remix, an unknown key). */
fun channelOfKey(key: String): Int? = when {
    key == SongPart.DRUMS -> 0
    key == SongPart.ACID -> 1
    key.startsWith("inst:") -> key.removePrefix("inst:").toIntOrNull()?.takeIf { it in 0 until InstrumentTrack.MAX }?.let(InstrumentTrack::mixerChannel)
    key.startsWith("audio:") -> key.removePrefix("audio:").toIntOrNull()?.takeIf { it in 0 until AudioTrack.MAX }?.let(AudioTrack::mixerChannel)
    else -> null
}

/** The track key of mixer channel [ch] (0..15), or null: the remix, the master, a pad's own strip. */
fun keyOfChannel(ch: Int): String? = when {
    ch == 0 -> SongPart.DRUMS
    ch == 1 -> SongPart.ACID
    ch == 2 -> SongPart.inst(0)
    ch in 5..11 -> SongPart.inst(ch - 4)
    ch in 12..15 -> SongPart.audio(ch - 12)
    else -> null
}

/** The track an automation lane's [target] moves (its key), or null for the whole mix (master, buses, live effects). */
fun laneTrackKey(target: AutoTarget): String? = when (target.kind) {
    AutoTarget.ACID -> SongPart.ACID
    AutoTarget.BUS_PARAM, AutoTarget.LIVE -> null
    else -> keyOfChannel(target.ch)
}

/**
 * The song curves (« Morceau » mode, with points) drawn under track [key] on the timeline, as
 * indexes into [Project.automation]; [key] null: the whole mix's, and those of a track no
 * longer in the song (shown with the mix rather than lost).
 */
fun Project.songLanesOf(key: String?): List<Int> {
    val keys = partKeys.toSet()
    return automation.indices.filter { i ->
        val lane = automation[i]
        if (lane.mode != AutomationLane.SONG || lane.songValues.none { it != null }) return@filter false
        val k = laneTrackKey(lane.target)?.takeIf { it in keys }
        k == key
    }
}

/**
 * Song part [index] cut in two at [atBar] bars from its start (one part becomes two that play
 * the same, back to back). A part that repeats its scene is cut between two repeats ([atBar] is
 * moved to the nearest one, never to either end); a part with its own length (written from the
 * live grid) at any bar, its second half carrying each track on from where it was. Song curves
 * and clips stay where they are: the song lasts as long and plays the same. Unchanged when the
 * part cannot be cut there (a single pass of its scene) or the song is full.
 */
fun Project.splitSongPart(index: Int, atBar: Int): Project {
    if (index !in song.indices || song.size >= SongPart.MAX_PARTS) return this
    val part = song[index]
    val bars = part.bars
    val (first, second) = if (bars != null) {
        if (atBar !in 1 until bars) return this
        val carried = partKeys.associateWith { part.fromBars(it) + atBar }.mapValues { it.value.coerceAtMost(SongPart.MAX_FROM) }
        part.copy(bars = atBar) to part.copy(bars = bars - atBar, from = carried)
    } else {
        if (part.repeats < 2) return this
        val sceneBars = (sceneSteps(part.scene) / stepsPerBar).coerceAtLeast(1)
        val cut = Math.round(atBar.toDouble() / sceneBars).toInt().coerceIn(1, part.repeats - 1)
        part.copy(repeats = cut) to part.copy(repeats = part.repeats - cut)
    }
    return copy(song = song.take(index) + first + second + song.drop(index + 1))
}

/** The bar [atBar] of part [index] where [splitSongPart] would really cut it, or null when it cannot. */
fun Project.splitBar(index: Int, atBar: Int): Int? {
    val part = song.getOrNull(index) ?: return null
    if (song.size >= SongPart.MAX_PARTS) return null
    part.bars?.let { bars -> return atBar.takeIf { it in 1 until bars } }
    if (part.repeats < 2) return null
    val sceneBars = (sceneSteps(part.scene) / stepsPerBar).coerceAtLeast(1)
    return Math.round(atBar.toDouble() / sceneBars).toInt().coerceIn(1, part.repeats - 1) * sceneBars
}

/**
 * Song playback with a loop over parts [loop] (the timeline's « Boucle »): the part that follows
 * [playing]. The last part of the loop leads back to its first one unless a part was chosen to
 * come next ([queued]); otherwise as [songNextIndex]. A loop out of the song's [size] is ignored.
 */
fun songNextIndexLooped(playing: Int, queued: Int, size: Int, loops: Boolean, loop: IntRange?): Int {
    val l = loop?.takeIf { !it.isEmpty() && it.first >= 0 && it.last < size }
    if (l != null && queued < 0 && playing == l.last) return l.first
    return songNextIndex(playing, queued, size, loops)
}

/** The parts from [a] to [b] (either order) as a loop for a song of [size] parts, or null when out of it. */
fun songLoopOf(a: Int, b: Int, size: Int): IntRange? {
    val lo = minOf(a, b)
    val hi = maxOf(a, b)
    return if (lo < 0 || hi >= size) null else lo..hi
}
