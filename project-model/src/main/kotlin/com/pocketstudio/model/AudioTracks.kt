package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * An audio track: one recorded or imported clip that plays in time with the song. The clip
 * starts on step [AudioClip.start] of the scene and comes back every [AudioClip.loopSteps].
 */
@Serializable
data class AudioTrack(
    /** 0..[MAX]-1: its engine track and mixer channel (see [mixerChannel]). */
    val id: Int,
    val name: String,
    val color: Int = 0,
    val clip: AudioClip? = null,
) {
    fun sanitized(): AudioTrack = copy(
        name = name.take(InstrumentTrack.MAX_NAME).ifBlank { "Audio ${id + 1}" },
        color = color.coerceIn(0, InstrumentTrack.COLORS - 1),
        clip = clip?.takeIf { PadSample(it.file, "").isSafe }?.sanitized(),
    )

    companion object {
        const val MAX = 4

        /** The mixer channel of audio track [id] (NativeEngine.audioChannel). */
        fun mixerChannel(id: Int) = 12 + id
    }
}

/** A clip of an audio track: a sound file of the project and how it plays. */
@Serializable
data class AudioClip(
    val file: String,
    val name: String,
    /** Seconds of sound (for the screen and the loop length). */
    val seconds: Double = 0.0,
    /** First step, in sixteenths from the scene start. */
    val start: Int = 0,
    /** It comes back every this many sixteenths (whole bars of the time signature). */
    val loopSteps: Int = 16,
    val gain: Float = 1f,
    /** Fade in and out, in seconds. */
    val fadeIn: Float = 0f,
    val fadeOut: Float = 0.01f,
) {
    fun sanitized(): AudioClip = copy(
        name = name.take(40),
        seconds = seconds.takeIf { it.isFinite() }?.coerceIn(0.0, 600.0) ?: 0.0,
        start = start.coerceIn(0, MAX_START),
        loopSteps = ((loopSteps.coerceIn(8, MAX_LOOP) + 3) / 4) * 4,
        gain = gain.takeIf { it.isFinite() }?.coerceIn(0f, 2f) ?: 1f,
        fadeIn = fadeIn.takeIf { it.isFinite() }?.coerceIn(0f, 10f) ?: 0f,
        fadeOut = fadeOut.takeIf { it.isFinite() }?.coerceIn(0f, 10f) ?: 0f,
    )

    companion object {
        const val MAX_START = 1023
        /** 64 bars. */
        const val MAX_LOOP = 1024

        /** The whole bars of [beatsPerBar] beats a clip of [seconds] lasts at [bpm] (at least one). */
        fun barsFor(seconds: Double, bpm: Double, beatsPerBar: Int = 4): Int {
            val bars = seconds * bpm / 60.0 / beatsPerBar
            if (!bars.isFinite() || bars <= 1.0) return 1
            return kotlin.math.ceil(bars - 0.02).toInt().coerceIn(1, MAX_LOOP / 16)
        }
    }
}

/** A new audio track (or null when all four exist), with a free id, a fresh mixer channel and a colour. */
fun Project.addAudioTrack(name: String? = null): Project? {
    val id = (0 until AudioTrack.MAX).firstOrNull { free -> audioTracks.none { it.id == free } } ?: return null
    val used = instrumentTracks.map { it.color } + audioTracks.map { it.color }
    val color = (0 until InstrumentTrack.COLORS).firstOrNull { it !in used } ?: (id % InstrumentTrack.COLORS)
    val ch = AudioTrack.mixerChannel(id)
    return copy(
        audioTracks = (audioTracks + AudioTrack(id, name ?: "Audio ${audioTracks.size + 1}", color)).sortedBy { it.id },
        mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, c -> if (i == ch) Channel() else c }),
    )
}

fun Project.removeAudioTrack(id: Int): Project = copy(
    audioTracks = audioTracks.filterNot { it.id == id },
    automation = automation.filterNot { it.target.kind != AutoTarget.BUS_PARAM && it.target.ch == AudioTrack.mixerChannel(id) },
)

fun Project.withAudioTrack(t: AudioTrack): Project = copy(audioTracks = audioTracks.map { if (it.id == t.id) t.sanitized() else it })
