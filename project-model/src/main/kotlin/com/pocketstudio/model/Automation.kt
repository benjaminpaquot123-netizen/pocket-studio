package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * What an automation lane moves: [kind] in [AutoTarget] order (same numbers as the engine),
 * the channel [ch] (4 = the master, for insert parameters), the insert [slot] or the bus,
 * and the effect [param]. [ACID]: a knob of the 303 ([param] in [AcidSound.values] order, one of
 * [ACID_PARAMS]) on the acid line's strip ([ACID_CHANNEL]). [MACRO]: one of a strip's four macros
 * ([param] 0..3: colour, dirt, motion, space), its chopper ([MACRO_CHOP], the Live page's
 * « Hachoir de voie »), its kills ([MACRO_KILL_LOW]..[MACRO_KILL_HIGH]) or throws
 * ([MACRO_THROW_REVERB], [MACRO_THROW_ECHO]). [LIVE]: the Live page's whole-mix effects ([param]
 * in [LIVE_PARAMS]: the DJ filter, the held effects, the kills, the freeze, the throws, the riser,
 * the build-up, and the reverb / echo returns), on the master ([ch] 4).
 */
@Serializable
data class AutoTarget(val kind: Int, val ch: Int = 0, val slot: Int = 0, val param: Int = 0) {
    /** Engine.h Automation::pack: the channel's low 4 bits at 12, its bits 4..5 at 24 (audio tracks 5..8). */
    fun pack(): Int = ((ch shr 4) shl 24) or (kind shl 16) or ((ch and 0xF) shl 12) or (slot shl 8) or param

    /**
     * The pads' own strips (16..31) cannot be automated; the audio tracks 5..8 (32..35) can. An
     * effect's knob: one of the strip's [Mixer.INSERTS] slots (4 bits of slot in the engine too).
     */
    val isValid: Boolean get() = kind in VOLUME..PLAITS && ch in 0..MAX_CH && !Mixer.isPad(ch) && (kind != INSERT_PARAM || slot in 0 until Mixer.INSERTS) &&
        (kind != ACID || (ch == ACID_CHANNEL && slot == 0 && param in ACID_PARAMS)) &&
        (kind != MACRO || (ch != Mixer.MASTER && slot == 0 && param in 0..MACRO_THROW_ECHO)) &&
        (kind != LIVE || (ch == Mixer.MASTER && slot == 0 && param in LIVE_PARAMS)) &&
        (kind != SYNTH || (trackOfChannel(ch) != null && slot == 0 && param in SYNTH_PARAMS)) &&
        (kind != PLAITS || (trackOfChannel(ch) != null && slot == 0 && param in PLAITS_PARAMS))

    /** [SYNTH], [PLAITS]: the instrument track whose knob it is (its mixer channel is [ch]). */
    val track: Int? get() = if (kind == SYNTH || kind == PLAITS) trackOfChannel(ch) else null

    /** An on/off control (held buttons, the choppers): its song curve steps rather than slides. */
    val isOnOff: Boolean get() = (kind == LIVE && param in LIVE_ON_OFF) || (kind == MACRO && param in MACRO_CHOP..MACRO_THROW_ECHO)

    /** A 303 knob: its index in [AcidSound.values], else null. */
    val acidParam: Int? get() = param.takeIf { kind == ACID && isValid }

    /**
     * The whole mix's volume (« Sortie générale »), 0..1 on top of the master's gain: what the
     * part sheet's fades write, only ever as a song curve. A scene played alone keeps its level
     * (see [songCurvesAt]). The engine applies it on the master fader (Mixer::setVolume on the
     * master's index, kMaster: after the master chain, at most unity), back at unity when the lane
     * goes or the song is left.
     */
    val isMasterVolume: Boolean get() = kind == VOLUME && ch == Mixer.MASTER

    companion object {
        const val MAX_CH = Mixer.CHANNELS - 1

        /** The whole mix's volume ([isMasterVolume]). */
        fun masterVolume() = AutoTarget(VOLUME, Mixer.MASTER)
        const val VOLUME = 0
        const val PAN = 1
        const val SEND = 2
        const val INSERT_PARAM = 3
        const val BUS_PARAM = 4
        const val ACID = 5
        const val MACRO = 6
        const val LIVE = 7
        /**
         * Phase 34 (S4, the « phase 37 » automation targets): a knob of an instrument track's synth
         * ([param] in [SynthPatch] order, one of [SYNTH_PARAMS]) or of its Plaits ([param] in
         * [PlaitsPatch.engineValues] order, one of [PLAITS_PARAMS]); [ch] is the track's mixer channel.
         */
        const val SYNTH = 8
        const val PLAITS = 9

        /** The synth knobs a lane can move: every one but the loudness calibration ([SynthPatch.GAIN]). */
        val SYNTH_PARAMS: Set<Int> = (0 until SynthPatch.PARAM_COUNT).toSet() - SynthPatch.GAIN
        /** Plaits' knobs a lane can move: harmonics, timbre, morph, decay, colour, release, level (not the model nor the calibration). */
        val PLAITS_PARAMS: Set<Int> = (1..7).toSet()

        /** The instrument track on mixer channel [ch] (0 on 2, 1..7 on 5..11), or null. */
        fun trackOfChannel(ch: Int): Int? = when (ch) {
            2 -> 0
            in 5 until 4 + InstrumentTrack.MAX -> ch - 4
            else -> null
        }

        fun synth(track: Int, param: Int) = AutoTarget(SYNTH, InstrumentTrack.mixerChannel(track), 0, param)
        fun plaits(track: Int, param: Int) = AutoTarget(PLAITS, InstrumentTrack.mixerChannel(track), 0, param)

        /** [MACRO]: the strip's chopper (« Hachoir de voie »), on above one half. */
        const val MACRO_CHOP = 4

        /** [MACRO]: the strip's bass, mids or highs cut while on (on the grid, both ways). */
        const val MACRO_KILL_LOW = 5
        const val MACRO_KILL_MID = 6
        const val MACRO_KILL_HIGH = 7

        /** [MACRO]: a rise sends the strip into the reverb or the echo for one beat (a « throw »). */
        const val MACRO_THROW_REVERB = 8
        const val MACRO_THROW_ECHO = 9

        // [LIVE] parameters: the engine's PerformanceFx numbers (filter, resonance, repeat, its
        // division, tape stop, chopper, ...), 6 and 7 being the reverb and echo returns (0.5 = as
        // mixed). The engine's numbers in between (18..26) are the project's settings
        // ([LiveFxSettings]), never automated.
        const val LIVE_FILTER = 0
        const val LIVE_RESONANCE = 1
        const val LIVE_REPEAT = 2
        const val LIVE_DIVISION = 3
        const val LIVE_TAPE_STOP = 4
        const val LIVE_CHOP = 5
        const val LIVE_REVERB_RETURN = 6
        const val LIVE_ECHO_RETURN = 7
        const val LIVE_BACKSPIN = 8
        const val LIVE_REVERSE = 9
        const val LIVE_HALF_TIME = 10
        const val LIVE_ECHO_OUT = 11
        const val LIVE_KILL_LOW = 12
        const val LIVE_KILL_MID = 13
        const val LIVE_KILL_HIGH = 14
        const val LIVE_FREEZE = 15
        const val LIVE_THROW_REVERB = 16
        const val LIVE_THROW_ECHO = 17
        const val LIVE_RISER = 27
        const val LIVE_DOWNLIFTER = 28
        const val LIVE_BUILD_UP = 29
        /** « Drop »: everything back at rest on the next bar line (a press). */
        const val LIVE_DROP = 30
        /** « Tout remettre »: everything back at rest at once (a press). */
        const val LIVE_RESET_ALL = 31
        val LIVE_PARAMS: Set<Int> = (LIVE_FILTER..LIVE_THROW_ECHO).toSet() + (LIVE_RISER..LIVE_RESET_ALL)

        /** The whole-mix effects a gesture moves (every [LIVE_PARAMS] but the two returns). */
        val LIVE_GESTURES: List<Int> = LIVE_PARAMS.filter { it != LIVE_REVERB_RETURN && it != LIVE_ECHO_RETURN }.sorted()
        val LIVE_ON_OFF: Set<Int> = setOf(LIVE_REPEAT, LIVE_DIVISION, LIVE_TAPE_STOP, LIVE_CHOP) + (LIVE_BACKSPIN..LIVE_THROW_ECHO) +
            setOf(LIVE_RISER, LIVE_DOWNLIFTER, LIVE_DROP, LIVE_RESET_ALL)

        /** The kills: bass, mids, highs ([band] 0..2). */
        fun liveKill(band: Int) = LIVE_KILL_LOW + band.coerceIn(0, 2)

        /** A one-shot (it acts from a press: the throws, the riser, the downlifter, Drop, Tout remettre). */
        fun isTrigger(param: Int) = param == LIVE_THROW_REVERB || param == LIVE_THROW_ECHO || param in LIVE_RISER..LIVE_RESET_ALL && param != LIVE_BUILD_UP

        /** Where each [LIVE] setting rests when nobody touches it (the filter open, nothing held, returns as mixed). */
        fun liveRest(param: Int): Float = when (param) {
            LIVE_FILTER -> 0.5f
            LIVE_RESONANCE -> 0.3f
            LIVE_DIVISION -> 0.5f
            LIVE_REVERB_RETURN, LIVE_ECHO_RETURN -> 0.5f
            else -> 0f
        }

        /** The acid line's mixer strip. */
        const val ACID_CHANNEL = 1

        /**
         * The 303 knobs a lane (or a live gesture) can move, in [AcidSound.values] order: cutoff,
         * resonance, envelope, decay, accent, saturation (tuning, waveform and volume stay put).
         */
        val ACID_PARAMS = listOf(0, 1, 2, 3, 4, 7)

        fun acid(param: Int) = AutoTarget(ACID, ACID_CHANNEL, 0, param)
        fun macro(ch: Int, param: Int) = AutoTarget(MACRO, ch, 0, param)
        fun live(param: Int) = AutoTarget(LIVE, Mixer.MASTER, 0, param)
    }
}

/**
 * One automated setting, as a clip. [LOOP]: a value 0..1 (or nothing) on each step of the drum
 * loop, the same in every scene. [PER_SCENE]: each scene has its own steps ([sceneValues]).
 * [SONG]: one curve over the whole song, a point per bar ([songValues]), sliding between points;
 * a curve written from the live grid has a point per beat ([songPerBeat]), and an on/off control
 * steps from one point to the next instead of sliding ([hold]); a continuous one jumps to the
 * points listed in [songJumps] (a Drop, a let-go, a setting moved after staying still).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AutomationLane(
    val target: AutoTarget,
    val values: List<Float?> = List(STEPS) { null },
    val mode: Int = LOOP,
    val sceneValues: Map<Int, List<Float?>> = emptyMap(),
    val songValues: List<Float?> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val songPerBeat: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val hold: Boolean = false,
    /**
     * A song curve heard only with the whole song (the riser before a Drop): a scene played on its
     * own holds it at the song's start (its rest), not where that scene first plays.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val songOnly: Boolean = false,
    /**
     * Song points reached by a jump, not a slide: the point drawn before holds until this one
     * (what a take heard: the Montée up to the Drop's bar line, then nothing). Points drawn only.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val songJumps: List<Int> = emptyList(),
) {
    fun sanitized(): AutomationLane {
        val song = clean(songValues, songValues.size.coerceAtMost(maxSongPoints)).dropLastWhile { it == null }
        return copy(
            values = clean(values, STEPS),
            mode = mode.coerceIn(LOOP, SONG),
            sceneValues = sceneValues.filterKeys { it in 0 until Scene.MAX }.mapValues { clean(it.value, STEPS) },
            songValues = song,
            songJumps = songJumps.filter { it in song.indices && song[it] != null }.distinct().sorted(),
        )
    }

    /** Points the song curve can hold: a bar each (256), or a beat each (4096). */
    val maxSongPoints: Int get() = if (songPerBeat) MAX_SONG_BEATS else MAX_SONG_BARS

    /** Song points in a bar of [beatsPerBar] beats: one, or one per beat. */
    fun pointsPerBar(beatsPerBar: Int): Int = if (songPerBeat) beatsPerBar else 1

    /** The steps playing in scene [scene]. */
    fun valuesFor(scene: Int): List<Float?> = if (mode == PER_SCENE) sceneValues[scene] ?: List(STEPS) { null } else values

    fun withPoint(step: Int, value: Float?, scene: Int = 0): AutomationLane {
        if (step !in 0 until STEPS) return this
        val updated = valuesFor(scene).mapIndexed { i, v -> if (i == step) value?.coerceIn(0f, 1f) else v }
        return if (mode == PER_SCENE) copy(sceneValues = sceneValues + (scene to updated)) else copy(values = updated)
    }

    /** The song curve's point [index] (a bar, or a beat when [songPerBeat]; null erases, its jump too). */
    fun withSongPoint(index: Int, value: Float?): AutomationLane {
        if (index !in 0 until maxSongPoints) return this
        val list = (songValues + List((index + 1 - songValues.size).coerceAtLeast(0)) { null }).toMutableList()
        list[index] = value?.coerceIn(0f, 1f)
        return copy(songValues = list.dropLastWhile { it == null }, songJumps = if (value == null) songJumps - index else songJumps)
    }

    /** The song curve jumps to its point [index] ([jump]) or slides into it. */
    fun withSongJump(index: Int, jump: Boolean): AutomationLane = when {
        jump == (index in songJumps) || index !in 0 until maxSongPoints -> this
        jump -> copy(songJumps = (songJumps + index).sorted())
        else -> copy(songJumps = songJumps - index)
    }

    /**
     * The points (of [points]) over which the engine does not slide toward the next one: each
     * point before a jump ([songJumps]); every point of an on/off control ([hold]: nothing listed,
     * the whole lane holds).
     */
    fun songFlatPoints(points: Int): List<Int> =
        if (hold) emptyList() else songJumps.map { it - 1 }.filter { it in 0 until points - 1 }.distinct().sorted()

    /**
     * The value the engine sets on transport step [step] of the song from this song lane, its curve
     * sent over [points] points ([songCurve]); [stepsPerBar] steps a bar (a point per bar, or per
     * beat of 4 steps): from a point's value toward the next one's, or holding it ([hold], before a
     * jump). Null: no curve.
     */
    fun songValueAtStep(step: Int, points: Int, stepsPerBar: Int): Float? {
        val curve = songCurve(points)
        if (curve.isEmpty() || step < 0) return null
        val unit = if (songPerBeat) 4 else stepsPerBar.coerceAtLeast(1)
        val b = minOf(step / unit, curve.size - 1)
        val a = curve[b]
        val c = curve[minOf(b + 1, curve.size - 1)]
        val flat = hold || step / unit >= curve.size - 1 || b in songFlatPoints(curve.size)
        return if (flat) a else a + (c - a) * (step % unit) / unit
    }

    /**
     * The song curve as the engine plays it: a value for each of [points] points (bars, or beats
     * when [songPerBeat]), straight lines between the points drawn ([hold]: each point's value
     * until the next one), held before the first and after the last. Empty when none.
     */
    fun songCurve(points: Int): List<Float> {
        val values = songValues
        fun drawnFrom(i: Int): Int {
            var j = i
            while (j < values.size && values[j] == null) j++
            return if (j < values.size) j else -1
        }
        var next = drawnFrom(0)
        if (next < 0) return emptyList()
        val jumps = songJumps.toHashSet()
        // One walk: [before] the last point drawn at or before b, [next] the first one after it.
        var before = -1
        val n = points.coerceIn(1, maxSongPoints)
        val out = ArrayList<Float>(n)
        for (b in 0 until n) {
            while (next in 0..b) {
                before = next
                next = drawnFrom(next + 1)
            }
            out += when {
                before < 0 -> values[next]!!
                next < 0 || before == b || hold || next in jumps -> values[before]!!
                else -> values[before]!! + (values[next]!! - values[before]!!) * (b - before) / (next - before)
            }
        }
        return out
    }

    /** This song lane with a point per beat ([beatsPerBar] beats a bar): the curve sounds the same. */
    fun perBeat(beatsPerBar: Int): AutomationLane {
        if (songPerBeat) return this
        val beats = beatsPerBar.coerceAtLeast(1)
        val out = arrayOfNulls<Float>((songValues.size * beats).coerceAtMost(MAX_SONG_BEATS))
        songValues.forEachIndexed { bar, v -> if (bar * beats < out.size) out[bar * beats] = v }
        return copy(songPerBeat = true, songValues = out.toList().dropLastWhile { it == null }, songJumps = songJumps.map { it * beats }.filter { it < out.size })
    }

    companion object {
        const val STEPS = 64
        const val MAX_LANES = 16
        const val LOOP = 0
        const val PER_SCENE = 1
        const val SONG = 2
        const val MAX_SONG_BARS = 256
        /**
         * A curve written from the live grid: a point per beat (1024 bars of 4/4: a set of half an
         * hour at 128 BPM). The engine holds as many (Automation::kSongPoints).
         */
        const val MAX_SONG_BEATS = 4096

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
        val total = partSteps(part)
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

/**
 * The value of each song lane (by index) on the bar where scene [scene] first plays in the song:
 * the scene played on its own holds them there (song curves only move with the song). Lanes
 * without points, or a scene not in the song, are not listed (the mixer's own setting holds).
 * A [AutomationLane.songOnly] lane holds its value at the song's start instead (the riser stays
 * shut while a section loops: no noise held through a Montée, no burst on every Drop loop).
 */
fun Project.songCurvesAt(scene: Int): Map<Int, Float> {
    val part = song.indexOfFirst { it.scene == scene }
    if (part < 0) return emptyMap()
    val step = songStepOf(part)
    val out = HashMap<Int, Float>()
    automation.forEachIndexed { i, lane ->
        if (lane.mode != AutomationLane.SONG) return@forEachIndexed
        // The whole mix's fades are the song's: a part looped on its own plays at its level (an
        // Intro that fades in would otherwise loop in silence).
        if (lane.target.isMasterVolume) return@forEachIndexed
        val at = if (lane.songOnly) 0 else if (lane.songPerBeat) step / 4 else step / stepsPerBar
        lane.songCurve(at + 1).getOrNull(at)?.let { out[i] = it }
    }
    return out
}

/** The lane writes something: a point on a step (loop), in some scene (per scene), or on the song curve. */
val AutomationLane.hasPoints: Boolean get() = when (mode) {
    AutomationLane.SONG -> songValues.any { it != null }
    AutomationLane.PER_SCENE -> sceneValues.values.any { v -> v.any { it != null } }
    else -> values.any { it != null }
}

/**
 * The 303 knobs a lane drives while scene [scene] plays ([inSong]: as a part of the song, else on
 * its own): a loop or per-scene lane with a point in that scene, a song curve with points while
 * the song plays (or on a scene of the song alone, where [songCurvesAt] holds it). The others
 * go back to the project's setting when the scene is sent.
 */
fun Project.acidKnobsDriven(scene: Int, inSong: Boolean): Set<Int> = automation.filter { lane ->
    when (lane.mode) {
        AutomationLane.SONG -> lane.hasPoints && (inSong || song.any { it.scene == scene })
        else -> lane.valuesFor(scene).any { it != null }
    }
}.mapNotNull { it.target.acidParam }.toSet()

/** The 303 knobs some lane moves (somewhere in the project): the Acide tab says so under them. */
val Project.automatedAcidKnobs: Set<Int> get() = automation.filter { it.hasPoints }.mapNotNull { it.target.acidParam }.toSet()

/**
 * A song curve ([AutomationLane.songCurve]) as the few commands that set it in the engine: runs
 * of equal points, straight slides ([Ramp]: from a point toward the next one's value), single
 * points. A drawn curve (a point a bar spread over beats, a take's held settings) takes about a
 * command per point drawn, not one per beat, so a whole song's curves fit in the engine's queue.
 */
sealed class CurveCommand {
    data class Run(val first: Int, val count: Int, val value: Float) : CurveCommand()
    /** Points [first] .. [first] + [count] - 1 on a line from [from] toward [to] (the next point's value). */
    data class Ramp(val first: Int, val count: Int, val from: Float, val to: Float) : CurveCommand()
}

fun songCurveCommands(curve: List<Float>): List<CurveCommand> {
    val out = ArrayList<CurveCommand>()
    val eps = 1e-5f
    var first = 0
    while (first < curve.size) {
        val v = curve[first]
        var end = first + 1
        while (end < curve.size && curve[end] == v) end++
        if (end - first > 1 || end >= curve.size) {
            out += CurveCommand.Run(first, end - first, v)
            first = end
            continue
        }
        // A slope: as far as the points stay on one line (the last one on it starts what follows).
        val slope = curve[first + 1] - v
        var last = first + 1
        while (last + 1 < curve.size && kotlin.math.abs(curve[last + 1] - (v + slope * (last + 1 - first))) < eps) last++
        out += CurveCommand.Ramp(first, last - first, v, curve[last])
        first = last
    }
    return out
}

/**
 * Setting [t] as the project holds it when no curve moves it, 0..1 as a lane writes it (the pan's
 * centre is 0.5). A Live page effect: where it rests.
 */
fun Project.settingOf(t: AutoTarget): Float {
    val c = mixer.channels.getOrNull(t.ch)
    return when (t.kind) {
        // The whole mix: at its full level (the master's gain itself is in dB, not a lane's 0..1).
        AutoTarget.VOLUME -> if (t.isMasterVolume) 1f else c?.volume ?: 0.8f
        AutoTarget.PAN -> ((c?.pan ?: 0f) + 1f) / 2f
        AutoTarget.SEND -> c?.sends?.getOrNull(t.slot) ?: 0f
        AutoTarget.INSERT_PARAM -> mixer.insertAt(t.ch, t.slot)?.params?.getOrNull(t.param) ?: 0.5f
        AutoTarget.BUS_PARAM -> mixer.buses.getOrNull(t.slot)?.getOrNull(t.param) ?: 0.5f
        AutoTarget.ACID -> acid.sound.values().getOrNull(t.param) ?: 0.5f
        AutoTarget.MACRO -> if (t.param < Macros.COUNT) c?.macros?.getOrNull(t.param) ?: 0f else 0f
        AutoTarget.LIVE -> AutoTarget.liveRest(t.param)
        AutoTarget.SYNTH -> t.track?.let { track(it).synth.get(t.param.coerceIn(0, SynthPatch.PARAM_COUNT - 1)) } ?: 0.5f
        AutoTarget.PLAITS -> t.track?.let { track(it).plaits.engineValues().getOrNull(t.param) } ?: 0.5f
        else -> 0.5f
    }.coerceIn(0f, 1f)
}

/**
 * The project with setting [t] held at [v] (0..1, as a lane writes it): what the mixer, the 303 or
 * an effect is set to. The Live page's effects and the momentary controls are not settings: unchanged.
 */
fun Project.withSetting(t: AutoTarget, v: Float): Project {
    val x = v.coerceIn(0f, 1f)
    fun channel(edit: (Channel) -> Channel): Project =
        if (t.ch !in mixer.channels.indices) this
        else copy(mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, c -> if (i == t.ch) edit(c) else c }))
    fun List<Float>.with(i: Int, value: Float) = mapIndexed { k, old -> if (k == i) value else old }
    return when (t.kind) {
        // The whole mix's volume is no fader: a lane on it only moves the song.
        AutoTarget.VOLUME -> if (t.isMasterVolume) this else channel { it.copy(volume = x) }
        AutoTarget.PAN -> channel { it.copy(pan = x * 2f - 1f) }
        AutoTarget.SEND -> channel { it.copy(sends = it.sends.with(t.slot, x)) }
        AutoTarget.INSERT_PARAM -> {
            fun List<Insert>.edited() = mapIndexed { i, ins -> if (i == t.slot) ins.copy(params = ins.params.with(t.param, x)) else ins }
            if (t.ch == Mixer.MASTER) copy(mixer = mixer.copy(master = mixer.master.edited()))
            else channel { it.copy(inserts = it.inserts.edited()) }
        }
        AutoTarget.BUS_PARAM -> copy(mixer = mixer.copy(buses = mixer.buses.mapIndexed { b, ps -> if (b == t.slot) ps.with(t.param, x) else ps }))
        AutoTarget.ACID -> copy(acid = acid.copy(sound = acid.sound.with(t.param, x)))
        AutoTarget.MACRO -> if (t.param < Macros.COUNT) channel { it.copy(macros = it.macros.with(t.param, x)) } else this
        AutoTarget.SYNTH -> t.track?.takeIf { t.param in AutoTarget.SYNTH_PARAMS }?.let { id -> withTrack(track(id).let { it.copy(synth = it.synth.with(t.param, x)) }) } ?: this
        AutoTarget.PLAITS -> t.track?.takeIf { t.param in AutoTarget.PLAITS_PARAMS }?.let { id -> withTrack(track(id).let { it.copy(plaits = it.plaits.with(t.param, x)) }) } ?: this
        else -> this
    }
}

/**
 * The song curve at [value] over its points [from]..[to] (a zone of the song: « plus fort au
 * début »), and as before around it: the point just before and just after the zone keep the old
 * curve's values there ([rest], the setting itself, where the lane had none), and the curve jumps
 * into and out of the zone instead of sliding, so the level holds over the zone and only there.
 * Points drawn inside the zone give way to it. [points]: the points the curve spans (the song).
 */
fun AutomationLane.withSongZone(from: Int, to: Int, value: Float, rest: Float, points: Int): AutomationLane {
    val last = maxSongPoints - 1
    val a = minOf(from, to).coerceIn(0, last)
    val b = maxOf(from, to).coerceIn(0, last)
    val v = value.coerceIn(0f, 1f)
    val old = songCurve(maxOf(points, b + 2).coerceAtMost(maxSongPoints))
    fun was(i: Int): Float = old.getOrNull(i) ?: old.lastOrNull() ?: rest.coerceIn(0f, 1f)
    val before = if (a > 0) was(a - 1) else null
    val after = if (b < last) was(b + 1) else null
    val size = maxOf(songValues.size, minOf(b + 2, maxSongPoints))
    val list = (songValues + List(size - songValues.size) { null }).toMutableList()
    for (i in a..b) list[i] = null
    if (before != null && list[a - 1] == null) list[a - 1] = before
    list[a] = v
    list[b] = v
    if (after != null && list[b + 1] == null) list[b + 1] = after
    val jumps = songJumps.filter { it !in a..b } + listOfNotNull(a.takeIf { before != null }, (b + 1).takeIf { after != null })
    return copy(songValues = list.dropLastWhile { it == null }, songJumps = jumps.distinct().sorted())
}
