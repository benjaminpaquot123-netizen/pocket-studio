package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * « Enregistrer la performance »: what is played on the live grid, written into the song.
 *
 * A take starts on a launch line ([start], a transport step) with the grid as it plays there
 * ([initial]); the launches the engine makes (each track taking a scene, or stopping) cut it into
 * song parts, and the gestures on the Live page's effects and 303 become song automation, a point
 * per beat. [write] turns it into the project's song in one go (one undo step for the app).
 */
class PerformanceTake(
    start: Int,
    initial: LiveState,
    private val columns: Int = LiveSlots.ALL,
    /** This take's own id (kept with its journal and in the project it is written into: never written twice). */
    val id: String = "",
    /** Moves that change nothing in what [writeTake] writes are not kept (see [gesture]); false only in tests. */
    private val compact: Boolean = true,
) {
    /** The transport step the take starts on (moved by [startsAt] when the engine marked it later). */
    var start: Int = start
        private set

    /** The grid as it plays at [start] (launches up to there change it). */
    var initial: LiveState = initial
        private set
    private val launches = ArrayList<TakeLaunch>()
    private val gestures = ArrayList<TakeGesture>()

    /**
     * Per setting, the moves kept in its current half beat ([HALF_BEAT_STEPS], from an even step):
     * the first one, the first one with another value ([mid]) and the latest one ([last]), as
     * indexes into [gestures] (-1: none yet).
     */
    private class Window(val half: Long, val first: Int, var mid: Int = -1, var last: Int = -1)
    private val windows = HashMap<AutoTarget, Window>()

    /** The engine launched [slot] on [scene] (-1: stopped) at transport [step]. */
    fun launched(slot: Int, scene: Int, step: Int) {
        if (step <= start) initial = initial.launched(slot, scene, step, columns)
        else launches += TakeLaunch(slot, scene, step)
    }

    /**
     * The engine started the take's song position on [step], later than planned (its mark came
     * after that launch line): the take starts there, with what was launched up to it.
     */
    fun startsAt(step: Int) {
        if (step <= start) return
        start = step
        launches.filter { it.step <= step }.sortedBy { it.step }.forEach { initial = initial.launched(it.slot, it.scene, it.step, columns) }
        launches.removeAll { it.step <= step }
    }

    /**
     * [target] moved to [value] at transport position [at] (steps, the output latency already
     * taken off); [before]: where it was before (only the first gesture of a target uses it).
     * [jump]: it got there at once (a Drop, « Tout remettre », a let-go back to rest), written as
     * a jump rather than a slide.
     */
    fun gesture(target: AutoTarget, value: Float, at: Double, before: Float, jump: Boolean = false) {
        if (!target.isValid || !value.isFinite() || !at.isFinite()) return
        val g = TakeGesture(target, value.coerceIn(0f, 1f), at, before.coerceIn(0f, 1f), jump)
        if (compact && !target.isOnOff) {
            // A finger dragging a knob makes ~60 moves a second; [writeTake] keeps one point per
            // beat. Within a half beat (from an even step: the take's start, its end and its beats'
            // limits are even steps, so a half beat never straddles one), only three moves count:
            // the first, the first with another value (the first may change nothing) and the last
            // (the value written, and the time the next move's « still, then moved » is counted
            // from). The moves between them only slide inside one beat: same song, far less to
            // keep (and to write in the journal every 5 s) on a long set.
            val half = kotlin.math.floor(at / HALF_BEAT_STEPS).toLong()
            val w = windows[target]
            val latest = w?.let { gestures[if (it.last >= 0) it.last else if (it.mid >= 0) it.mid else it.first] }
            if (w != null && latest != null && !jump && w.half == half && at >= latest.at) {
                when {
                    w.mid < 0 && g.value != gestures[w.first].value -> if (w.last >= 0) {
                        gestures[w.last] = g  // the one replaced had the first's value: it changed nothing
                        w.mid = w.last
                        w.last = -1
                    } else {
                        gestures += g
                        w.mid = gestures.lastIndex
                    }
                    w.last >= 0 -> gestures[w.last] = g
                    else -> {
                        gestures += g
                        w.last = gestures.lastIndex
                    }
                }
                return
            }
            windows[target] = Window(half, gestures.size)
        }
        gestures += g
    }

    /**
     * A press of [target] waiting for its grid line [at] was let go before it: the engine never
     * played it, so the take forgets it (nothing is written for it, its let-go neither).
     */
    fun forget(target: AutoTarget, at: Double) {
        windows.clear()  // indexes move
        gestures.removeAll { it.target == target && it.at == at }
    }

    /**
     * A Drop waiting for its bar line [at] was cancelled at [now] (« Tout remettre »: the engine
     * puts everything at rest at once, the Drop never comes): the Drop's press goes, and what it
     * put at rest on its bar line is at rest from [now].
     */
    fun dropCancelled(at: Double, now: Double) {
        val drop = AutoTarget.live(AutoTarget.LIVE_DROP)
        // Moved after what was played meanwhile (gestures of a setting are written in the order made).
        val moved = gestures.filter { it.at == at && it.target != drop }.map { it.copy(at = minOf(now, at)) }
        windows.clear()  // indexes move
        gestures.removeAll { it.at == at }
        gestures += moved
    }

    val launchCount: Int get() = launches.size
    val gestureCount: Int get() = gestures.size
    /** The settings the take's gestures moved. */
    val gestureTargets: Set<AutoTarget> get() = gestures.mapTo(HashSet()) { it.target }

    /**
     * The take so far as a journal ([TakeJournal]) of project [projectId]: kept on disk while it
     * records, so that a take the app could not finish (closed, killed) is written at the next
     * opening; [end]: the transport step it ends on (a bar line), [append]: after the song.
     */
    fun journal(projectId: String, end: Int, append: Boolean): TakeJournal = TakeJournal(
        projectId = projectId, takeId = id, start = start, row = initial.row,
        playing = initial.slots.map { it.playing }, starts = initial.slots.map { it.start },
        columns = columns, launches = launches.toList(), gestures = gestures.toList(), end = end, append = append,
    )

    internal fun restore(launched: List<TakeLaunch>, moved: List<TakeGesture>) {
        windows.clear()
        launches += launched
        gestures += moved.filter { it.target.isValid && it.value.isFinite() && it.at.isFinite() && it.before.isFinite() }
    }

    internal fun launchesUpTo(end: Int): List<TakeLaunch> = launches.filter { it.step < end }.sortedBy { it.step }
    internal fun gesturesUpTo(end: Int): List<TakeGesture> = gestures.filter { it.at < end }
}

@Serializable
data class TakeLaunch(val slot: Int, val scene: Int, val step: Int)

@Serializable
data class TakeGesture(val target: AutoTarget, val value: Float, val at: Double, val before: Float, val jump: Boolean = false)

/**
 * A take in progress as kept on disk ([PerformanceTake.journal]): written while it records, it is
 * written into project [projectId]'s song at the next opening when the app could not finish it
 * (closed or killed during the take). [end]: the transport step it ends on.
 */
@Serializable
data class TakeJournal(
    val projectId: String,
    /** The take's id ([PerformanceTake.id]; empty in journals of older versions). */
    val takeId: String = "",
    val start: Int,
    val row: Int,
    val playing: List<Int>,
    val starts: List<Int>,
    val columns: Int = LiveSlots.ALL,
    val launches: List<TakeLaunch> = emptyList(),
    val gestures: List<TakeGesture> = emptyList(),
    val end: Int,
    val append: Boolean = false,
) {
    /** The take again (its grid at the start, its launches and gestures). */
    fun take(): PerformanceTake {
        val slots = List(LiveSlots.COUNT) { k -> LiveSlot(playing.getOrElse(k) { row }.coerceAtLeast(-1), null, starts.getOrElse(k) { start }) }
        return PerformanceTake(start.coerceAtLeast(0), LiveState(row.coerceAtLeast(0), slots), columns, takeId).also {
            it.restore(launches.filter { l -> l.slot in 0 until LiveSlots.COUNT }, gestures)
        }
    }

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        /** Null when unreadable. */
        fun decode(text: String): TakeJournal? = runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
    }
}

/**
 * Take [journal] written into this project's song (see [writeTake]) when it was this project's
 * (else unchanged, with 0 parts).
 */
fun Project.writeJournal(journal: TakeJournal, projectId: String, at: Long = System.currentTimeMillis()): TakeWritten = when {
    journal.projectId != projectId -> TakeWritten(this, 0, 0)
    // Already in the song (written at ■ Fin, or at an opening the app was killed after): never twice.
    journal.takeId.isNotEmpty() && journal.takeId == liveTake -> TakeWritten(this, 0, 0, already = true)
    // The song it replaces is kept as a version (Versions du morceau).
    else -> keepingVersion(writeTake(journal.take(), journal.end, journal.append && song.isNotEmpty()), at).let { w ->
        if (w.parts > 0 && journal.takeId.isNotEmpty()) w.copy(project = w.project.copy(liveTake = journal.takeId)) else w
    }
}

/**
 * What writing a take did: the project, how many song parts and settings (automation lanes) it
 * wrote; [cut]: the song was full ([SongPart.MAX_PARTS]) and the take's end was left out;
 * [skipped]: settings moved that had no lane left ([AutomationLane.MAX_LANES]); [late]: moves
 * past the song curves' end ([AutomationLane.MAX_SONG_BEATS]) left out (the parts go on);
 * [already]: the take was already in the song ([Project.liveTake]), nothing written.
 */
data class TakeWritten(val project: Project, val parts: Int, val settings: Int, val cut: Boolean = false, val skipped: Int = 0, val late: Int = 0, val already: Boolean = false)

/**
 * The song parts take [take] makes up to transport step [end] (none when it is too short), each
 * with the transport step it starts on: each stretch between two launches is a part; parts that
 * only carry on from the one before are merged; a stretch longer than [SongPart.MAX_BARS] goes on
 * in a next part.
 */
fun Project.takeParts(take: PerformanceTake, end: Int): List<Pair<SongPart, Int>> {
    val bar = stepsPerBar
    val stop = maxOf(end, take.start)
    if (stop <= take.start) return emptyList()
    val keys = partKeys
    val columns = liveColumns
    val sceneCount = allScenes.size

    class Stretch(val from: Int, val to: Int, val state: LiveState)
    val stretches = ArrayList<Stretch>()
    var state = take.initial
    var at = take.start
    val launches = take.launchesUpTo(stop)
    var i = 0
    while (i < launches.size) {
        val step = launches[i].step
        if (step > at) stretches += Stretch(at, step, state)
        while (i < launches.size && launches[i].step == step) {
            val l = launches[i]
            state = state.launched(l.slot, l.scene, l.step, liveColumnsMask)
            i++
        }
        at = maxOf(at, step)
    }
    if (stop > at) stretches += Stretch(at, stop, state)

    // Each column: the scene it plays (-1 stopped); the remix has no scenes: on (the row) or off.
    fun scenesOf(s: LiveState): Map<String, Int> = columns.associateWith { k ->
        val p = LiveSlots.of(k)?.let { s.slot(it).playing } ?: s.row
        when {
            p < 0 -> -1
            k == SongPart.REMIX || p >= sceneCount -> s.row.coerceIn(0, sceneCount - 1)
            else -> p
        }
    }
    fun rowOf(s: LiveState) = s.row.coerceIn(0, sceneCount - 1)
    // A track's loop, rounded up to whole bars (the steps it has played only matter that far).
    fun period(key: String, scene: Int): Int = lcm(slotLoopSteps(key, scene).coerceAtLeast(1), bar)
    fun elapsed(s: LiveState, key: String, step: Int): Int = step - (LiveSlots.of(key)?.let { s.slot(it).start } ?: step)

    // Merge a stretch into the one before when every track just carries on.
    val merged = ArrayList<Stretch>()
    for (s in stretches) {
        val last = merged.lastOrNull()
        val same = last != null && rowOf(last.state) == rowOf(s.state) && scenesOf(last.state) == scenesOf(s.state) &&
            scenesOf(s.state).all { (k, scene) ->
                scene < 0 || k !in keys || Math.floorMod(elapsed(last!!.state, k, last.from) + (last.to - last.from), period(k, scene)) ==
                    Math.floorMod(elapsed(s.state, k, s.from), period(k, scene))
            }
        if (same) merged[merged.lastIndex] = Stretch(last!!.from, s.to, last.state) else merged += s
    }
    // A stretch longer than a part can be goes on in the next one (carrying on).
    val longest = SongPart.MAX_BARS * bar
    val pieces = merged.flatMap { s ->
        if (s.to - s.from <= longest) listOf(s)
        else (s.from until s.to step longest).map { a -> Stretch(a, minOf(a + longest, s.to), s.state) }
    }
    return pieces.map { s ->
        val row = rowOf(s.state)
        val scenes = scenesOf(s.state)
        val muted = scenes.filterValues { it < 0 }.keys
        val play = scenes.filter { (k, it) -> k in keys && it >= 0 && it != row }
        // Stopped drums too: loop curves count with the drums (stopped, from their stop), so the
        // song's part keeps that count (with the row's loop) rather than starting it over.
        val from = scenes.filter { (k, it) -> k in keys && (it >= 0 || k == SongPart.DRUMS) }.mapValues { (k, scene) ->
            Math.floorMod(elapsed(s.state, k, s.from), period(k, if (scene >= 0) scene else row)) / bar
        }.filterValues { it > 0 }
        val length = s.to - s.from
        val sceneLength = sceneSteps(row)
        val repeats = length / sceneLength
        val part = if (play.isEmpty() && from.isEmpty() && length % sceneLength == 0 && repeats in 1..SongPart.MAX_REPEATS) {
            SongPart(row, repeats, muted)
        } else {
            SongPart(row, 1, muted, play, from, ((length + bar - 1) / bar).coerceIn(1, SongPart.MAX_BARS))
        }
        part to s.from
    }
}

/**
 * Where « Annuler » of a take written into this project goes back to, [before] being the project
 * as the take began: its song, curves, scenes and song versions, without the take; but what was
 * set during the take and is no part of it (tempo, swing, the mixer and its effects, the
 * pads, the Live settings) stays as it is now. The settings the take's gestures moved
 * ([gestured]) go back to where they were: the take moved them, the take takes them back.
 */
fun Project.takeUndoTarget(before: Project, gestured: Set<AutoTarget>): Project {
    var back = before.copy(
        name = name, tempoBpm = tempoBpm, swing = swing, mixer = mixer,
        live = live, midiMap = midiMap, songLoops = songLoops, loopPart = loopPart, liveQuantum = liveQuantum, liveFx = liveFx,
    )
    for (t in gestured) back = back.withSetting(t, before.settingOf(t))
    return back
}

/**
 * The project with [take] written into its song, the take ending on transport step [end] (a bar
 * line): it replaces the song, or comes after it ([append]). Each stretch between two launches is
 * a part ([takeParts]): its scene is the grid's row, the tracks on another scene play it
 * ([SongPart.play]), the stopped ones are muted (the remix too), and those that carried on start
 * where they were ([SongPart.from]). When the song cannot hold every part, the take ends where the
 * first part left out begins ([TakeWritten.cut]).
 *
 * Gestures become song curves with a point per beat; on/off controls step ([AutomationLane.hold]).
 * What the take heard is what is written: before a setting's first gesture, its curve as it
 * played (a song curve kept; a loop or per-scene lane baked beat by beat, its own steps kept for
 * a switch back), then the gestures, held to the take's end (the app holds the setting in the
 * engine from its first gesture on). Song positions count from the take's start (the engine's
 * song position during the take, see the app's MarkSong), after the song when appending. Replacing
 * the song, the song curves keep what the take heard on its bars and nothing after its end (the
 * bars of the song before it are gone).
 */
fun Project.writeTake(take: PerformanceTake, end: Int, append: Boolean): TakeWritten {
    val bar = stepsPerBar
    val before = if (append) song else emptyList()
    val base = before.sumOf { partSteps(it) }
    val room = (SongPart.MAX_PARTS - before.size).coerceAtLeast(0)
    val all = takeParts(take, end)
    val cut = all.size > room
    val kept = all.take(room)
    if (kept.isEmpty()) return TakeWritten(this, 0, 0, cut)
    val stop = if (cut) all[room].second else maxOf(end, take.start)
    val newSong = before + kept.map { it.first }

    // --- Gestures: song curves, a point per beat ---------------------------------------------------
    val beatsPerBar = bar / 4
    val firstBeat = base / 4
    val lastBeat = (base + stop - take.start) / 4 - 1
    val written = copy(song = newSong)
    var lanes = automation
    if (!append) lanes = lanes.map { it.songCutAt(lastBeat + 1, beatsPerBar) }
    var settings = 0
    var skipped = 0
    var late = 0
    take.gesturesUpTo(stop).groupBy { it.target }.forEach { (target, moves) ->
        // The beat each move lands on (nearest), in the song, and whether the setting jumped there:
        // pressed at once ([TakeGesture.jump]), or moved after staying still (half a beat or more,
        // the take's first move included). A finger dragging it slides from beat to beat.
        class Change(val beat: Int, val value: Float, val jump: Boolean)
        val changes = ArrayList<Change>()
        val rest = moves.first().before
        var current = rest
        var lastAt = Double.NEGATIVE_INFINITY
        for (m in moves) {
            var beat = (firstBeat + ((m.at - take.start) / 4.0).roundToInt()).coerceIn(firstBeat, lastBeat)
            val jumped = m.jump || m.at - lastAt >= STILL_STEPS
            lastAt = m.at
            val last = changes.lastOrNull()
            if (target.isOnOff && last != null && beat <= last.beat && m.value != last.value) {
                // An on/off pressed and let go within a beat still lasts one beat.
                val previous = changes.getOrNull(changes.size - 2)?.value ?: rest
                if (last.value != previous) beat = last.beat + 1
            }
            if (beat > lastBeat) continue
            // Past the song curves' last point: not written (the take's parts go on).
            if (beat >= AutomationLane.MAX_SONG_BEATS) {
                late++
                continue
            }
            if (last != null && beat <= last.beat) changes[changes.lastIndex] = Change(last.beat, m.value, last.jump || jumped)
            else if (m.value != current || changes.isEmpty()) changes += Change(beat, m.value, jumped)
            current = changes.last().value
        }
        if (changes.isEmpty()) return@forEach
        val index = lanes.indexOfFirst { it.target == target }
        if (index < 0 && lanes.size >= AutomationLane.MAX_LANES) {
            skipped++
            return@forEach
        }
        val first = changes.first().beat
        val old = lanes.getOrNull(index)
        // The setting as the take heard it on the beat before its first gesture.
        var heard = rest
        var lane = when {
            old != null && old.mode == AutomationLane.SONG && old.hasPoints -> {
                // A song curve: kept up to the first gesture.
                val was = old.perBeat(beatsPerBar)
                if (first > 0) heard = was.songCurve(first)[first - 1]
                was.copy(hold = target.isOnOff, songValues = was.songValues.mapIndexed { b, v -> if (b >= first) null else v }, songJumps = was.songJumps.filter { it < first })
            }
            old != null && old.hasPoints -> {
                // A loop or per-scene lane: what it played, beat by beat (its steps stay for a switch back).
                val baked = written.bakedBeats(old, first, beatsPerBar)
                var l = old.copy(mode = AutomationLane.SONG, songPerBeat = true, hold = target.isOnOff, songValues = emptyList(), songJumps = emptyList())
                // Before the lane wrote anything: the mixer's own setting.
                if (first > 0 && baked.first() == null) l = l.withSongPoint(0, rest)
                baked.forEachIndexed { b, v -> if (v != null && (b == 0 || v != baked[b - 1])) l = l.withSongPoint(b, v) }
                baked.lastOrNull()?.let { heard = it }
                l.flatBetweenPoints()
            }
            else -> (old ?: AutomationLane(target)).copy(mode = AutomationLane.SONG, songPerBeat = true, hold = target.isOnOff, songValues = emptyList(), songJumps = emptyList())
        }
        // The beat before the first gesture as heard (the song before a take appended keeps its sound).
        if (first > 0) lane = lane.withSongPoint(first - 1, heard)
        var previous = (first - 1) to heard
        for (c in changes) {
            // A setting left still and then moved: flat until the beat before (no slow ramp), and
            // a jump on its beat (the engine holds the beat before instead of sliding into it).
            if (!lane.hold && c.beat - previous.first > 1) lane = lane.withSongPoint(c.beat - 1, previous.second)
            lane = lane.withSongPoint(c.beat, c.value).withSongJump(c.beat, !lane.hold && c.jump && c.beat > 0)
            previous = c.beat to c.value
        }
        // Held to the take's end (the engine held it there), whatever the curve does after.
        if (previous.first < lastBeat && lane.songValues.drop(lastBeat + 1).any { it != null }) lane = lane.withSongPoint(lastBeat, previous.second)
        lanes = if (index >= 0) lanes.mapIndexed { k, l -> if (k == index) lane else l } else lanes + lane
        settings++
    }
    return TakeWritten(copy(song = newSong, automation = lanes).sanitized(), kept.size, settings, cut, skipped, late)
}

/**
 * Replacing the song: this lane's song curve over the first [beats] beats of the new song (as the
 * take heard it there), nothing after (those bars of the song before are gone); a point just past
 * the end keeps the slide of the last bar (or beat) as it was.
 */
internal fun AutomationLane.songCutAt(beats: Int, beatsPerBar: Int): AutomationLane {
    val points = if (songPerBeat) beats else (beats + beatsPerBar - 1) / beatsPerBar.coerceAtLeast(1)
    if (songValues.size <= points + 1) return this
    val end = songCurve(points + 1).getOrNull(points)
    return copy(songValues = songValues.take(points) + end, songJumps = songJumps.filter { it <= points })
}

/** Each point keeps its value up to the beat before the next one (a loop's steps do not slide). */
private fun AutomationLane.flatBetweenPoints(): AutomationLane {
    var lane = this
    val drawn = songValues.withIndex().filter { it.value != null }
    for (k in 0 until drawn.size - 1) {
        val a = drawn[k].index
        val b = drawn[k + 1].index
        if (b - a > 1) lane = lane.withSongPoint(b - 1, drawn[k].value)
    }
    return lane
}

/**
 * Loop or per-scene lane [lane] as this project's song plays it, for its first [beats] beats: the
 * value it has set on each beat's first step (null: nothing set yet). It runs with the drums'
 * loop of each part (their scene, from where they carry on); per-scene steps are those of the
 * scene the lane's track plays in the part.
 */
internal fun Project.bakedBeats(lane: AutomationLane, beats: Int, beatsPerBar: Int): List<Float?> {
    val out = ArrayList<Float?>(beats.coerceAtLeast(0))
    if (beats <= 0) return out
    val bar = beatsPerBar * 4
    val scenes = allScenes
    val key = laneKey(lane.target)
    var value: Float? = null
    var partStart = 0
    var step = 0
    for (part in song) {
        val length = partSteps(part)
        val drumSteps = (scenes.getOrNull(part.sceneFor(SongPart.DRUMS))?.drums?.steps ?: DrumPattern().steps).coerceIn(8, AutomationLane.STEPS)
        val values = lane.valuesFor(key?.let { part.sceneFor(it) } ?: part.scene)
        val from = part.fromBars(SongPart.DRUMS) * bar
        while (step < partStart + length && out.size < beats) {
            values.getOrNull(Math.floorMod(step - partStart + from, drumSteps))?.let { value = it }
            if (step % 4 == 0) out += value
            step++
        }
        partStart += length
        if (out.size >= beats) break
    }
    while (out.size < beats) out += value
    return out
}

/** The song part key of the track whose setting [target] moves (null: the master, the buses, the whole mix). */
private fun laneKey(target: AutoTarget): String? = when (target.kind) {
    AutoTarget.ACID -> SongPart.ACID
    AutoTarget.BUS_PARAM, AutoTarget.LIVE -> null
    else -> when (val ch = target.ch) {
        0 -> SongPart.DRUMS
        1 -> SongPart.ACID
        2 -> SongPart.inst(0)
        in 5..11 -> SongPart.inst(ch - 4)
        else -> AudioTrack.ofChannel(ch)?.let(SongPart::audio)
    }
}

/** A setting still for this many steps (half a beat) and then moved jumps to its new value in a take. */
private const val STILL_STEPS = 2.0

/** [PerformanceTake.gesture] keeps at most three moves of a setting per this many steps (half a beat, at most [STILL_STEPS]). */
private const val HALF_BEAT_STEPS = 2.0

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
private fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b
