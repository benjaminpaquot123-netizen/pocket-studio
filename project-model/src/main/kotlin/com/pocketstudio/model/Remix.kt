package com.pocketstudio.model

import kotlinx.serialization.Serializable

/** One stem's strip in the remix: same fader law as the mixer (0.8 = unity). */
@Serializable
data class StemMix(val volume: Float = 0.8f, val mute: Boolean = false, val solo: Boolean = false)

/**
 * A separated song played along with the project: [setId] names the stems folder on the phone,
 * [songBpm] and [firstBeat] (seconds) place its beat grid, [startBar] is the song's bar heard on
 * the project's first step.
 */
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
) {
    fun sanitized(): Remix = copy(
        songBpm = songBpm.takeIf { it.isFinite() }?.coerceIn(0.0, Project.MAX_TEMPO) ?: 0.0,
        firstBeat = firstBeat.takeIf { it.isFinite() }?.coerceIn(0.0, 10.0) ?: 0.0,
        startBar = startBar.coerceIn(0, MAX_START_BAR),
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
    }
}
