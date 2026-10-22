package com.pocketstudio.model

/**
 * Song automation curves and song clips follow the arrangement.
 *
 * Both are placed on the song's timeline (a point per bar or per beat, a clip on a song step). When the
 * arrangement changes — a part inserted, moved, removed or duplicated, its repeats or its scene's
 * length changed, another time signature — what was placed inside a part moves with that part
 * and keeps its place inside it, so the export still plays the song that was made.
 *
 * [this]: the project after the edit of [before]; [origin]: for each part of this song, the
 * index of the part of [before]'s song it is (null: a new part). Rules:
 * - a point keeps its bar inside its part; points past a part's new end go (Annuler gives them
 *   back); a duplicated part gets its original's points;
 * - a new part, or the bars added to a part, hold the value the curve had just before them;
 * - where parts now meet that did not before, the curve keeps its values on both sides (a point
 *   is written there), so the ramps that remain sound as they did;
 * - a song clip keeps its step inside the part it starts in; one starting in a removed part
 *   keeps its offset from where that part was; points and clips past the song's end keep their
 *   distance from the end.
 * Nothing changes when the new song is empty.
 */
fun Project.followArrangement(before: Project, origin: List<Int?>): Project {
    if (before.song.isEmpty() || song.isEmpty() || origin.size != song.size) return this
    val oldBar = before.stepsPerBar
    val newBar = stepsPerBar
    val oldLen = before.song.map { before.partSteps(it) }
    val newLen = song.map { partSteps(it) }
    if (origin == before.song.indices.toList() && oldLen == newLen && oldBar == newBar) return this
    val oldStart = oldLen.runningFold(0, Int::plus)
    val newStart = newLen.runningFold(0, Int::plus)

    // A step inside a part, from the old bars to the new (the bar and the step in it).
    fun steps(o: Int): Int = if (oldBar == newBar) o else o / oldBar * newBar + (o % oldBar).coerceAtMost(newBar - 1)

    // --- Song clips ------------------------------------------------------------------------------
    val tracks = audioTracks.map { t ->
        val clip = t.clip
        val was = before.audioTracks.firstOrNull { it.id == t.id }?.clip
        if (clip == null || !clip.song || was == null || !was.song || was.file != clip.file) return@map t
        val s = was.start
        val i = (0 until oldLen.size).firstOrNull { s < oldStart[it + 1] }
        val moved = if (i == null) {
            newStart.last() + steps(s - oldStart.last())
        } else {
            val j = origin.indexOf(i)
            val place = if (j >= 0) newStart[j]
            else origin.indices.firstOrNull { origin[it] != null && origin[it]!! > i }?.let { newStart[it] } ?: newStart.last()
            place + steps(s - oldStart[i])
        }
        t.copy(clip = clip.copy(start = moved.coerceIn(0, AudioClip.MAX_SONG_START)))
    }

    // --- Song curves (a value per bar, or per beat) -------------------------------------------------
    val lanes = automation.map { lane ->
        val was = before.automation.firstOrNull { it.target == lane.target } ?: return@map lane
        val old = was.songValues
        if (old.none { it != null }) return@map lane
        // Steps a point stands for, before and after (a bar, or a beat: four sixteenths).
        val oldUnit = if (was.songPerBeat) 4 else oldBar
        val newUnit = if (was.songPerBeat) 4 else newBar
        val oldEnd = oldStart.last() / oldUnit
        val newEnd = newStart.last() / newUnit
        val size = (newEnd + (old.size - oldEnd).coerceAtLeast(0)).coerceAtMost(was.maxSongPoints)
        // A point inside a part, from the old bars to the new: per beat, its bar and its beat in it
        // (a beat past the new bar's end is gone: -1).
        fun inPart(o: Int): Int {
            if (!was.songPerBeat || oldBar == newBar) return o
            val oldBeats = oldBar / 4
            val newBeats = newBar / 4
            return if (o % oldBeats < newBeats) o / oldBeats * newBeats + o % oldBeats else -1
        }
        // The old point each new point shows (-1: a new one).
        val from = IntArray(size) { -1 }
        song.indices.forEach { j ->
            val i = origin[j] ?: return@forEach
            val first = newStart[j] / newUnit
            val oldFirst = oldStart[i] / oldUnit
            val oldPoints = oldLen[i] / oldUnit
            for (o in 0 until oldPoints) {
                val b = inPart(o)
                if (b < 0 || b >= newLen[j] / newUnit || first + b >= size) continue
                from[first + b] = oldFirst + o
            }
        }
        for (b in newEnd until size) from[b] = oldEnd + (b - newEnd)
        val curve = was.songCurve(maxOf(oldEnd, old.size))
        val out = arrayOfNulls<Float>(size)
        for (b in 0 until size) if (from[b] >= 0) out[b] = old.getOrNull(from[b])
        val last = curve.size - 1
        for (b in 0 until size) {
            val ob = from[b]
            if (ob >= 0) {
                // The edges of what moved keep their value (a point where none was).
                val runStart = if (b == 0) ob != 0 else from[b - 1] < 0 || from[b - 1] != ob - 1
                val runEnd = if (b == size - 1) ob != last else from[b + 1] < 0 || from[b + 1] != ob + 1
                if ((runStart || runEnd) && out[b] == null) out[b] = curve.getOrNull(ob)
            } else if (b == 0 || from[b - 1] >= 0) {
                // New points hold the value just before them (just after, at the very start).
                var end = b
                while (end + 1 < size && from[end + 1] < 0) end++
                val hold = (if (b > 0) from[b - 1] else from.getOrElse(end + 1) { -1 }).takeIf { it >= 0 }?.let { curve.getOrNull(it) }
                if (hold != null) {
                    out[b] = hold
                    out[end] = hold
                }
            }
        }
        lane.copy(songValues = out.toList().dropLastWhile { it == null })
    }
    return copy(audioTracks = tracks, automation = lanes)
}

/**
 * After an edit that left the song's parts as they were but changed how long they last (a scene's
 * length, the time signature): song curves and song clips follow their parts. [before]: the
 * project before the edit. Unchanged when the parts last as long as before.
 */
fun Project.followPartLengths(before: Project): Project {
    if (song.isEmpty() || song != before.song) return this
    if (stepsPerBar == before.stepsPerBar && song.indices.all { partSteps(song[it]) == before.partSteps(song[it]) }) return this
    return followArrangement(before, song.indices.toList())
}
