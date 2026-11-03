package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** One stem's strip in the remix: same fader law as the mixer (0.8 = unity). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StemMix(
    val volume: Float = 0.8f, val mute: Boolean = false, val solo: Boolean = false,
    /** Step 5b: its edits (pieces cut, moved, faded: [StemClip]); null = played as separated. Left out of the file while null. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val clips: List<StemClip>? = null,
)

/**
 * A separated song played along with the project: [setId] names the stems folder on the phone,
 * [songBpm] and [firstBeat] (seconds) place its beat grid, [startBar] is the song's bar heard
 * first, on the project's song step [atStep] (where the song comes in, 0 = the very start).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Remix(
    val setId: String? = null,
    val name: String = "",
    val songBpm: Double = 0.0,
    val firstBeat: Double = 0.0,
    val startBar: Int = 0,
    val stems: List<StemMix> = List(STEMS) { StemMix() },
    /** The song follows the project's tempo (time-stretched, pitch kept) instead of its own. */
    val followTempo: Boolean = false,
    /** The song's length in seconds (0: unknown, as in projects saved before it was kept). */
    val seconds: Double = 0.0,
    /**
     * Where the song comes in: the project's song step (a sixteenth, counted from the song's first
     * part) on which [startBar] is heard. Before it the song is silent. Left out of the file at 0:
     * older projects read the same.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val atStep: Int = 0,
    /**
     * Step 5a: the song is a track of the project (« Chanson : Boys », its four stems in Sons,
     * Structure, Mix and Live) and was put in the song's structure once ([withRemixPlaced]): a part
     * long enough for it when the song had none. False in projects saved before (they are placed
     * when opened, once). Left out of the file while false.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val placed: Boolean = false,
    /**
     * Lot D2 (« case Remix du Live »): the passages of the song put in cells of the Live grid's
     * remix column, one per brick at most. A brick without one plays the whole song, as before.
     * Left out of the file while empty.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val cells: List<RemixCell> = emptyList(),
) {
    fun sanitized(): Remix = copy(
        songBpm = songBpm.takeIf { it.isFinite() }?.coerceIn(0.0, Project.MAX_TEMPO) ?: 0.0,
        firstBeat = firstBeat.takeIf { it.isFinite() }?.coerceIn(0.0, 10.0) ?: 0.0,
        startBar = startBar.coerceIn(0, MAX_START_BAR),
        seconds = seconds.takeIf { it.isFinite() }?.coerceIn(0.0, MAX_SECONDS) ?: 0.0,
        atStep = atStep.coerceIn(0, MAX_AT_STEP),
        stems = (stems + List((STEMS - stems.size).coerceAtLeast(0)) { StemMix() }).take(STEMS)
            .map {
                it.copy(
                    volume = it.volume.takeIf { v -> v.isFinite() }?.coerceIn(0f, 1f) ?: 0.8f,
                    clips = it.clips?.mapNotNull(StemClip::sanitized)?.take(StemClip.MAX),
                )
            },
        cells = cells.mapNotNull { it.sanitized() }.distinctBy { it.scene }.sortedBy { it.scene },
    )

    /** Lot D2: the passage of brick [scene]'s remix cell, or null (the whole song plays there). */
    fun cell(scene: Int): RemixCell? = cells.firstOrNull { it.scene == scene }

    /** Lot D2: brick [scene]'s remix cell holding [cell] (null: back to the whole song). */
    fun withCell(scene: Int, cell: RemixCell?): Remix =
        copy(cells = (cells.filter { it.scene != scene } + listOfNotNull(cell?.copy(scene = scene))).sortedBy { it.scene })

    /** Frames of one bar of the song (4/4) at [rate]; 0 when the tempo is unknown. */
    fun barFrames(rate: Int): Long = if (songBpm > 0) Math.round(4 * 60.0 / songBpm * rate) else 0L

    /**
     * How much longer the song lasts when it follows [projectBpm] (1 = played as is): songBpm / projectBpm,
     * or 1 when it does not follow, its tempo is unknown, or the two tempos already match.
     */
    fun stretchFor(projectBpm: Double): Double =
        if (!followTempo || songBpm <= 0 || projectBpm <= 0 || kotlin.math.abs(songBpm - projectBpm) < 0.05) 1.0 else songBpm / projectBpm

    /** Song frame heard on the project's first step. */
    fun startFrame(rate: Int): Long = Math.round(firstBeat * rate) + startBar * barFrames(rate)

    fun withStem(i: Int, transform: (StemMix) -> StemMix) =
        copy(stems = stems.mapIndexed { k, s -> if (k == i) transform(s) else s })

    companion object {
        const val STEMS = 4
        const val MAX_START_BAR = 999
        /** The latest song step the song may come in on: past any song (128 parts of 256 bars of 16 steps). */
        const val MAX_AT_STEP = 1 shl 19
        /** Longer than any song the separation takes (7 minutes): a bad value cannot make an endless export. */
        const val MAX_SECONDS = 3600.0
        /** The mixer strip the remix plays through: the song's group fader (its four stems under it). */
        const val STRIP = 3

        /** The stems in the order they are shown (Voix, Batterie, Basse, Reste); stem indices as the engine has them. */
        val SHOWN_ORDER = listOf(3, 0, 1, 2)

        /** The timeline key of stem [stem] (a line under the song's folder); [SongPart.REMIX] is the folder. */
        fun stemKey(stem: Int) = "${SongPart.REMIX}:$stem"

        /** The stem of a timeline key made by [stemKey], or null. */
        fun stemOfKey(key: String): Int? =
            key.takeIf { it.startsWith("${SongPart.REMIX}:") }?.substringAfter(':')?.toIntOrNull()?.takeIf { it in 0 until STEMS }

        /**
         * The stems' strips for a song put in the project after listening with [heard] on (« Voix »
         * alone): the parts switched off come in muted. All on, all off or not known: all heard.
         */
        fun stemsHeard(heard: List<Boolean>?): List<StemMix> =
            if (heard == null || heard.size != STEMS || heard.all { it } || heard.none { it }) List(STEMS) { StemMix() }
            else heard.map { StemMix(mute = !it) }
    }
}

/**
 * Sixteenths of the project the remix's song lasts as the project plays it, from its [Remix.startBar]
 * (stretched when it follows the tempo); 0 when no song or its length ([seconds]) is unknown, or
 * when it starts past its end.
 */
fun Project.remixSteps(seconds: Double = remix.seconds): Int {
    val r = remix
    if (r.setId == null || !(seconds > 0) || !seconds.isFinite() || tempoBpm <= 0) return 0
    val rate = 48_000
    // Step 5b: as long as its last piece, when one was moved past the song's end (or the end cut away).
    val left = (r.heardEnd(seconds).coerceAtMost(Remix.MAX_SECONDS * 2) - r.startFrame(rate).toDouble() / rate) * r.stretchFor(tempoBpm)
    if (left <= 0) return 0
    return kotlin.math.ceil(left * tempoBpm / 15.0 - 1e-6).toInt()  // a sixteenth = 15/bpm s
}

/**
 * The stems' frame (at [rate], of the copy the engine plays: [playRatio] times the song's length)
 * heard on the transport's first step when playback starts on song step [fromStep]: the song comes in
 * on [Remix.atStep], so it is negative (silence first) when playback starts before that step.
 */
fun Project.remixStemStart(rate: Int, playRatio: Double, fromStep: Int): Long {
    val sixteenth = if (tempoBpm > 0) 15.0 / tempoBpm * rate else 0.0
    return Math.round(remix.startFrame(rate) * playRatio + (fromStep.toLong() - remix.atStep) * sixteenth)
}

/**
 * The song step a playback starting outside the song stands for, as the remix places its song: a
 * scene played on its own is heard where it first plays in the song (its song comes in there
 * as in the export); a scene the song never plays, or a project without a song, hears the song
 * from its start right away ([Remix.atStep]).
 */
fun Project.remixFromStepAlone(scene: Int): Int =
    song.indexOfFirst { it.scene == scene }.takeIf { it >= 0 }?.let { songStepOf(it) } ?: remix.atStep

/**
 * The bar (counted from the project's first step, rounded up) where the remix's song ends as the
 * project plays it: from [Remix.atStep] and [Remix.startFrame], stretched when it follows the tempo.
 * 0 when no song is heard (none, its strip or all its stems silent) or its length is unknown.
 * [seconds]: the song's length, when the project does not know it yet (the stems loaded tell it).
 */
fun Project.remixEndBars(seconds: Double = remix.seconds): Int {
    val r = remix
    if (r.setId == null) return 0
    val strip = mixer.channels.getOrNull(Remix.STRIP)
    if (strip?.mute == true || (strip?.volume ?: 1f) <= 0f) return 0
    val anySolo = r.stems.any { it.solo }
    if (r.stems.none { !it.mute && it.volume > 0f && (!anySolo || it.solo) }) return 0
    val steps = remixSteps(seconds)
    if (steps <= 0) return 0
    val end = r.atStep.toLong() + steps
    return ((end + stepsPerBar - 1) / stepsPerBar).toInt()
}

/**
 * The bar where everything a loop export must let ring ends: the audio clips heard
 * ([audioEndBars]) and the remix's song ([remixEndBars]); 0 when neither.
 */
fun Project.exportEndBars(remixSeconds: Double = remix.seconds): Int = maxOf(audioEndBars(), remixEndBars(remixSeconds))


/**
 * The project has music of its own (a beat, notes, a recording): adding a song to play along then
 * leaves its tempo and key as they are, the song follows them (remix lot 1). An empty project (« Partir
 * de zéro ») takes the song's.
 */
fun Project.hasMusicOfItsOwn(): Boolean = allScenes.any { !it.isEmpty } || audioTracks.any { it.clip != null }


/**
 * Step 5a: where the remix's song is heard in each part of the song, as fractions of the part (the
 * timeline's lines under the song's folder): from [Remix.atStep], as long as it lasts ([remixSteps]);
 * nothing in a part that leaves the remix out (the Live's « remix » column off). Empty lists when
 * the song or its length is not known.
 */
fun Project.remixSpans(seconds: Double = remix.seconds): List<List<ClosedFloatingPointRange<Float>>> {
    val length = remixSteps(seconds)
    var at = 0
    return song.map { part ->
        val steps = partSteps(part)
        val partStart = at
        at += steps
        if (steps <= 0 || length <= 0 || !part.plays(SongPart.REMIX)) return@map emptyList()
        val from = (remix.atStep - partStart).coerceIn(0, steps)
        val to = (remix.atStep + length - partStart).coerceIn(0, steps)
        if (to > from) listOf(from.toFloat() / steps..to.toFloat() / steps) else emptyList()
    }
}

/**
 * Step 5a: the remix's song put in the song's structure, once: when the song has no part yet
 * (Benjamin's « Ton morceau est vide » with a song in the project), one part on brique A as long
 * as the song ([remixSteps], whole bars, at most [SongPart.MAX_BARS]) so the Structure shows its
 * four lines on the bars (« Toute la chanson, à partir de la brique A »). Then [Remix.placed]:
 * parts taken away afterwards stay so. Projects saved before step 5a get it when opened; their
 * remix strip, stems and placing are kept as they were (nothing heard changes but the song now
 * running through the part).
 */
fun Project.withRemixPlaced(): Project {
    if (remix.setId == null || remix.placed) return this
    val steps = remixSteps()
    // The length not known yet (a song saved before it was kept): placed once it is known.
    if (song.isNotEmpty()) return copy(remix = remix.copy(placed = true))
    if (steps <= 0) return this
    val bars = ((remix.atStep + steps + stepsPerBar - 1) / stepsPerBar.coerceAtLeast(1)).coerceIn(1, SongPart.MAX_BARS)
    return copy(song = listOf(SongPart(scene = 0, bars = bars)), remix = remix.copy(placed = true))
}


/**
 * Lot D2 (« case Remix du Live »): a passage of the separated song in brick [scene]'s cell of the
 * Live grid's remix column. [file] (in the project's `samples/`, like a pad's sound) holds [bars]
 * whole bars of it cut at [bpm] (the song's tempo, so it follows the project's: stretched off the
 * audio thread); [stem] the stem it was taken from ([WHOLE]: the four together), [from] where in
 * the song it starts (seconds), [name] what the cell shows (« Voix », French, shown translated).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RemixCell(
    val scene: Int,
    val file: String,
    val name: String,
    val bars: Int = 1,
    val bpm: Double = 0.0,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val stem: Int = WHOLE,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val from: Double = 0.0,
) {
    /** Steps the passage loops over in a project of [stepsPerBar] steps a bar. */
    fun loopSteps(stepsPerBar: Int): Int = bars * stepsPerBar.coerceAtLeast(1)

    /** In range; null when it cannot be played (a brick past the last, a file name that is not safe). */
    fun sanitized(): RemixCell? {
        if (scene !in 0 until Scene.MAX || !PadSample(file, name).isSafe) return null
        return copy(
            name = name.take(40),
            bars = bars.coerceIn(1, MAX_BARS),
            bpm = bpm.takeIf { it.isFinite() && it in AutoTempo.MIN_BPM..AutoTempo.MAX_BPM } ?: 0.0,
            stem = stem.coerceIn(WHOLE, Remix.STEMS - 1),
            from = from.takeIf { it.isFinite() }?.coerceIn(0.0, Remix.MAX_SECONDS) ?: 0.0,
        )
    }

    companion object {
        /** [stem] for the whole song (its four stems together). */
        const val WHOLE = -1
        /** The longest passage, in bars (a cell is a loop, not the song). */
        const val MAX_BARS = 32
        /** Lengths offered in the cell's menu, in bars. */
        val LENGTHS = listOf(1, 2, 4, 8, 16)

        /**
         * The whole bars a passage of [seconds] at [bpm] ([beatsPerBar] a bar) loops over: the
         * nearest number, one at least, [MAX_BARS] at most. 1 when the tempo is not known.
         */
        fun barsFor(seconds: Double, bpm: Double, beatsPerBar: Int): Int {
            val bars = seconds * bpm / 60.0 / beatsPerBar.coerceAtLeast(1)
            if (!bars.isFinite() || bars < 1.0) return 1
            return kotlin.math.round(bars).toInt().coerceIn(1, MAX_BARS)
        }

        /** Frames of [bars] bars at [bpm] at [rate] (the passage cut to whole bars). */
        fun framesFor(bars: Int, bpm: Double, beatsPerBar: Int, rate: Int): Long =
            if (bpm <= 0.0) 0L else Math.round(bars * beatsPerBar.coerceAtLeast(1) * 60.0 / bpm * rate)
    }
}

/** Lot D2: the first brick whose remix cell holds no passage yet ([Scene.MAX] bricks), or null. */
fun Project.firstFreeRemixCell(): Int? = (0 until Scene.MAX).firstOrNull { remix.cell(it) == null }

/** Lot D2: the engine's remix cell for brick [scene] (the brick itself), or -1 when it plays the whole song. */
fun Project.remixCellOf(scene: Int): Int = if (remix.setId != null && remix.cell(scene) != null) scene else -1
