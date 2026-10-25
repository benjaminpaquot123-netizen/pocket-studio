package com.pocketstudio.model

/**
 * Everything that acts on the sound (« Ce qui joue »): each track's effects, its sends to the
 * shared reverb and echo, its quick controls moved away from rest, every automation lane (the
 * assistant's sweeps and risers, curves written from the Live page), the master's effects and
 * its trim. Listed for one scene, or for the whole song (scene null), so nothing heard is hidden.
 */
sealed class Acting {
    /** The mixer channel it belongs to ([Mixer.MASTER] for the master, [BUSES] for the shared buses). */
    abstract val ch: Int

    /** An effect in slot [slot] of channel [ch]'s chain (the master's when [ch] is [Mixer.MASTER]). */
    data class Effect(override val ch: Int, val slot: Int, val insert: Insert) : Acting()

    /** A part of channel [ch] sent to the shared [bus] (0 reverb, 1 echo). */
    data class Send(override val ch: Int, val bus: Int, val level: Float) : Acting()

    /** A quick control ([Macros]) away from where it leaves the sound untouched. */
    data class Macro(override val ch: Int, val macro: Int, val value: Float) : Acting()

    /** Automation lane [lane] of the project and the shape it draws in the scope asked. */
    data class Moves(override val ch: Int, val lane: Int, val automation: AutomationLane, val shape: CurveShape) : Acting()

    /**
     * G1b: layer [index] of instrument track [track] (on its strip [ch]): one more sound playing the
     * track's notes, with its own effects and sends ([layer]).
     */
    data class Layered(override val ch: Int, val track: Int, val index: Int, val layer: TrackLayer) : Acting()

    /** The master's gain before the limiter, away from 0 dB. */
    data class MasterTrim(val db: Float) : Acting() {
        override val ch: Int get() = Mixer.MASTER
    }

    companion object {
        /** [ch] of what belongs to the shared reverb and echo themselves. */
        const val BUSES = -1
    }
}

/** What a curve does, in a few numbers: from where to where, its lowest and highest, its kind. */
data class CurveShape(val kind: Kind, val first: Float = 0f, val last: Float = 0f, val min: Float = 0f, val max: Float = 0f, val bars: Int = 0) {
    enum class Kind { EMPTY, STEADY, RISES, FALLS, UP_DOWN, DOWN_UP, MOVES }

    companion object {
        /** The shape of [values] (the points heard, in order); [bars]: how long they last. */
        fun of(values: List<Float>, bars: Int = 0): CurveShape {
            if (values.isEmpty()) return CurveShape(Kind.EMPTY, bars = bars)
            val min = values.min()
            val max = values.max()
            val first = values.first()
            val last = values.last()
            val span = max - min
            val kind = when {
                span < 0.03f -> Kind.STEADY
                last - first > span * 0.6f -> Kind.RISES
                first - last > span * 0.6f -> Kind.FALLS
                max - maxOf(first, last) > span * 0.4f -> Kind.UP_DOWN
                minOf(first, last) - min > span * 0.4f -> Kind.DOWN_UP
                else -> Kind.MOVES
            }
            return CurveShape(kind, first, last, min, max, bars)
        }
    }
}

/** The automation lanes acting on channel [ch] (to badge a track that moves). */
fun Project.automatedLanes(ch: Int): List<Int> = automation.indices.filter { automation[it].target.ch == ch && automation[it].target.kind != AutoTarget.BUS_PARAM && automation[it].target.kind != AutoTarget.LIVE }

/** The song's bars where scene [scene] plays: (first bar, bar count) for each part playing it. */
fun Project.barsOfScene(scene: Int): List<IntRange> = song.indices.filter { song[it].scene == scene }.map { i ->
    val start = songStepOf(i) / stepsPerBar
    val bars = (partSteps(song[i]) + stepsPerBar - 1) / stepsPerBar
    start until start + bars.coerceAtLeast(1)
}

/**
 * Everything acting on the sound in scene [scene] (null: the whole song), in the order the sound
 * goes through it: per channel its effects, quick controls and sends, then the lanes moving it;
 * the master's effects and trim; the shared buses' lanes and the Live page's ones last.
 */
fun Project.whatActs(scene: Int? = null): List<Acting> = buildList {
    val lanes = automation.mapIndexedNotNull { i, lane -> lane.shapeIn(this@whatActs, scene)?.let { Acting.Moves(laneChannel(lane.target), i, lane, it) } }
    mixer.channels.forEachIndexed { ch, c ->
        if (ch == Mixer.MASTER) return@forEachIndexed
        c.inserts.forEachIndexed { slot, ins ->
            if (ins.effect != EffectType.NONE && (scene == null || ins.playsIn(scene))) add(Acting.Effect(ch, slot, ins))
        }
        c.macros.forEachIndexed { m, v -> if (kotlin.math.abs(v - Macros.DEFAULTS.getOrElse(m) { 0f }) > 0.005f) add(Acting.Macro(ch, m, v)) }
        c.sends.forEachIndexed { bus, level -> if (level > 0.01f) add(Acting.Send(ch, bus, level)) }
        // A track's layers, after its own chain (each with its own effects and sends).
        instrumentTracks.firstOrNull { InstrumentTrack.mixerChannel(it.id) == ch }?.let { t ->
            t.layers.forEachIndexed { k, l -> add(Acting.Layered(ch, t.id, k, l)) }
        }
        addAll(lanes.filter { it.ch == ch })
    }
    mixer.master.forEachIndexed { slot, ins -> if (ins.effect != EffectType.NONE && (scene == null || ins.playsIn(scene))) add(Acting.Effect(Mixer.MASTER, slot, ins)) }
    if (kotlin.math.abs(mixer.masterDb) > 0.05f) add(Acting.MasterTrim(mixer.masterDb))
    addAll(lanes.filter { it.ch == Mixer.MASTER })
    addAll(lanes.filter { it.ch == Acting.BUSES })
}

/** The channel a lane belongs to: its track's, the master's (the Live page's), or the buses'. */
fun laneChannel(t: AutoTarget): Int = when (t.kind) {
    AutoTarget.BUS_PARAM -> Acting.BUSES
    AutoTarget.LIVE -> Mixer.MASTER
    else -> t.ch
}

/**
 * The shape this lane draws in scene [scene] (null: the whole song), or null when it plays no
 * part there (a per-scene lane with nothing in that scene, a song curve over other bars).
 */
fun AutomationLane.shapeIn(p: Project, scene: Int?): CurveShape? {
    fun held(steps: List<Float?>): List<Float> {
        var last = steps.lastOrNull { it != null } ?: return emptyList()
        return steps.map { v -> (v ?: last).also { last = it } }
    }
    return when (mode) {
        AutomationLane.LOOP -> CurveShape.of(held(values))
        AutomationLane.PER_SCENE -> if (scene != null) {
            sceneValues[scene]?.takeIf { s -> s.any { it != null } }?.let { CurveShape.of(held(it)) }
        } else {
            // The whole song: every scene's steps one after the other.
            val all = sceneValues.toSortedMap().values.filter { s -> s.any { it != null } }
            if (all.isEmpty()) CurveShape.of(emptyList()) else CurveShape.of(all.flatMap { held(it) })
        }
        else -> {
            val bars = maxOf(p.songBars, 1)
            val per = pointsPerBar(p.beatsPerBar)
            val curve = songCurve(bars * per)
            if (scene == null || p.song.isEmpty()) return CurveShape.of(curve, if (curve.isEmpty()) 0 else bars)
            // A curve heard only with the whole song still shows under each scene it covers.
            val ranges = p.barsOfScene(scene)
            if (ranges.isEmpty() || curve.isEmpty()) return null
            val heard = ranges.flatMap { r -> r.flatMap { b -> (0 until per).mapNotNull { curve.getOrNull(b * per + it) } } }
            if (heard.isEmpty()) null else CurveShape.of(heard, ranges.sumOf { it.count() })
        }
    }
}

/** A straight line from ([from], [a]) to ([to], [b]) over a list of points (others kept). */
fun List<Float?>.withLine(from: Int, a: Float, to: Int, b: Float, size: Int = this.size): List<Float?> {
    val out = (this + List((size - this.size).coerceAtLeast(0)) { null }).toMutableList()
    if (out.isEmpty()) return out
    val forward = from <= to
    val start = (if (forward) from else to).coerceIn(0, out.size - 1)
    val end = (if (forward) to else from).coerceIn(0, out.size - 1)
    val v0 = if (forward) a else b
    val v1 = if (forward) b else a
    for (i in start..end) {
        val t = if (end == start) 1f else (i - start).toFloat() / (end - start)
        out[i] = (v0 + (v1 - v0) * t).coerceIn(0f, 1f)
    }
    return out
}

/** Each point drawn averaged with its drawn neighbours (a shaky finger line made smooth); empty steps stay empty. */
fun List<Float?>.smoothed(): List<Float?> = mapIndexed { i, v ->
    if (v == null) null else {
        val around = listOfNotNull(getOrNull(i - 1), v, v, getOrNull(i + 1))
        around.sum() / around.size
    }
}

/** A line between two steps of the loop (or of scene [scene]'s steps): one edit. */
fun AutomationLane.withStepLine(from: Int, a: Float, to: Int, b: Float, scene: Int = 0): AutomationLane {
    val line = valuesFor(scene).withLine(from, a, to, b, AutomationLane.STEPS).take(AutomationLane.STEPS)
    return if (mode == AutomationLane.PER_SCENE) copy(sceneValues = sceneValues + (scene to line)) else copy(values = line)
}

/** The loop's (or scene [scene]'s) steps smoothed. */
fun AutomationLane.withStepsSmoothed(scene: Int = 0): AutomationLane {
    val s = valuesFor(scene).smoothed()
    return if (mode == AutomationLane.PER_SCENE) copy(sceneValues = sceneValues + (scene to s)) else copy(values = s)
}

/** A line between two points of the song curve (point indexes: bars, or beats when [AutomationLane.songPerBeat]). */
fun AutomationLane.withSongLine(from: Int, a: Float, to: Int, b: Float): AutomationLane {
    val end = maxOf(from, to).coerceAtMost(maxSongPoints - 1)
    val line = songValues.withLine(from.coerceAtMost(end), a, to.coerceAtMost(end), b, end + 1).dropLastWhile { it == null }
    val lo = minOf(from, to)
    // The points in between now slide: no jump left inside the line.
    return copy(songValues = line, songJumps = songJumps.filter { it <= lo || it > end })
}

/** The song curve's points smoothed (jumps kept). */
fun AutomationLane.withSongSmoothed(): AutomationLane = copy(songValues = songValues.smoothed())
