package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** One stem's strip in the remix: same fader law as the mixer (0.8 = unity). */
@Serializable
data class StemMix(val volume: Float = 0.8f, val mute: Boolean = false, val solo: Boolean = false)

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
) {
    fun sanitized(): Remix = copy(
        songBpm = songBpm.takeIf { it.isFinite() }?.coerceIn(0.0, Project.MAX_TEMPO) ?: 0.0,
        firstBeat = firstBeat.takeIf { it.isFinite() }?.coerceIn(0.0, 10.0) ?: 0.0,
        startBar = startBar.coerceIn(0, MAX_START_BAR),
        seconds = seconds.takeIf { it.isFinite() }?.coerceIn(0.0, MAX_SECONDS) ?: 0.0,
        atStep = atStep.coerceIn(0, MAX_AT_STEP),
        stems = (stems + List((STEMS - stems.size).coerceAtLeast(0)) { StemMix() }).take(STEMS)
            .map { it.copy(volume = it.volume.takeIf { v -> v.isFinite() }?.coerceIn(0f, 1f) ?: 0.8f) },
    )

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
        /** The mixer strip the remix plays through. */
        const val STRIP = 3

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
    val left = (seconds.coerceAtMost(Remix.MAX_SECONDS) - r.startFrame(rate).toDouble() / rate) * r.stretchFor(tempoBpm)
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
