package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * An audio track: one recorded or imported clip that plays in time with the song. A scene clip
 * starts on step [AudioClip.start] of the scene and comes back every [AudioClip.loopSteps] (or
 * once per part); a song clip ([AudioClip.song]) starts on a step of the song and plays once.
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
    /** First step, in sixteenths from the scene start (from the song start for a [song] clip). */
    val start: Int = 0,
    /** It comes back every this many sixteenths (whole bars of the time signature). */
    val loopSteps: Int = 16,
    val gain: Float = 1f,
    /** Fade in and out, in seconds. */
    val fadeIn: Float = 0f,
    val fadeOut: Float = 0.01f,
    /** A song clip: [start] is a step of the song; it plays once, straight across the parts. */
    val song: Boolean = false,
    /** A scene clip played once per part (and once per pass of its scene played alone), not looped. */
    val once: Boolean = false,
    /** « Décaler la prise »: later (> 0) or earlier (< 0), in milliseconds. */
    val offsetMs: Float = 0f,
    /** The tempo it was recorded at (0: unknown, an imported file). */
    val bpm: Double = 0.0,
) {
    fun sanitized(): AudioClip = copy(
        name = name.take(40),
        seconds = seconds.takeIf { it.isFinite() }?.coerceIn(0.0, 600.0) ?: 0.0,
        start = start.coerceIn(0, if (song) MAX_SONG_START else MAX_START),
        loopSteps = ((loopSteps.coerceIn(8, MAX_LOOP) + 3) / 4) * 4,
        gain = gain.takeIf { it.isFinite() }?.coerceIn(0f, 2f) ?: 1f,
        fadeIn = fadeIn.takeIf { it.isFinite() }?.coerceIn(0f, 10f) ?: 0f,
        fadeOut = fadeOut.takeIf { it.isFinite() }?.coerceIn(0f, 10f) ?: 0f,
        offsetMs = offsetMs.takeIf { it.isFinite() }?.coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS) ?: 0f,
        bpm = bpm.takeIf { it.isFinite() && it in 20.0..400.0 } ?: 0.0,
    )

    /** Sixteenths it lasts at [bpm] (rounded up). */
    fun steps(bpm: Double): Int = kotlin.math.ceil(seconds * bpm / 15.0 - 1e-6).toInt().coerceAtLeast(1)

    /**
     * The loop the engine plays it on. [sceneSteps]: the scene plays alone, that long (null: a
     * song part, where a "once" clip waits for the next part). A "once" clip played alone comes
     * back with its scene, after as many whole passes as it needs to be heard to its end.
     */
    fun engineLoopSteps(sceneSteps: Int?, tempo: Double): Int = when {
        song -> 0
        !once -> loopSteps
        sceneSteps == null -> 0
        else -> {
            val pass = sceneSteps.coerceAtLeast(1)
            ((start + steps(tempo) + pass - 1) / pass * pass).coerceIn(pass, ENGINE_MAX_LOOP)
        }
    }

    /** [offsetMs] in frames at [sampleRate]. */
    fun offsetFrames(sampleRate: Int): Int = Math.round(offsetMs / 1000.0 * sampleRate).toInt()

    /** True when it was recorded at another tempo than [projectBpm] (it no longer falls on the beat). */
    fun tempoDiffers(projectBpm: Double): Boolean = bpm > 0.0 && kotlin.math.abs(bpm - projectBpm) >= 0.5

    /** The bar (1 = the first) it starts on, in [stepsPerBar]. */
    fun startBar(stepsPerBar: Int): Int = start / stepsPerBar.coerceAtLeast(1) + 1

    companion object {
        const val MAX_START = 1023
        /** A song clip may start anywhere in the longest song (256 bars of 7/4). */
        const val MAX_SONG_START = 256 * 28
        /** 64 bars. */
        const val MAX_LOOP = 1024
        /** What the engine loops on at most (AudioTracks::LoopSteps). */
        const val ENGINE_MAX_LOOP = 4096
        const val MAX_OFFSET_MS = 200f
        /** « Décaler la prise » moves by this much per touch. */
        const val OFFSET_STEP_MS = 5f

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

fun Project.removeAudioTrack(id: Int): Project {
    val ch = AudioTrack.mixerChannel(id)
    return copy(
        audioTracks = audioTracks.filterNot { it.id == id },
        automation = automation.filterNot { it.target.kind != AutoTarget.BUS_PARAM && it.target.ch == ch },
        // Its strip back to neutral: a solo left on it would silence every other one.
        mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, c -> if (i == ch) Channel() else c }),
    )
}

fun Project.withAudioTrack(t: AudioTrack): Project = copy(audioTracks = audioTracks.map { if (it.id == t.id) t.sanitized() else it })

/**
 * The bar (counted from the start, rounded up) where the last audio clip heard in a loop export
 * ends: clips on tracks muted in the mixer or at zero volume, and song clips, do not count; 0 when none.
 */
fun Project.audioEndBars(): Int = audioTracks.maxOfOrNull { t ->
    val clip = t.clip ?: return@maxOfOrNull 0
    val strip = mixer.channels.getOrNull(AudioTrack.mixerChannel(t.id))
    // A song clip is silent in a loop export (see [audioPlays]).
    if (clip.song || clip.gain <= 0f || strip?.mute == true || (strip?.volume ?: 1f) <= 0f) return@maxOfOrNull 0
    val end = clip.start + clip.steps(tempoBpm) + kotlin.math.max(0, Math.round(clip.offsetMs / 1000.0 * tempoBpm / 15.0).toInt())
    (end + stepsPerBar - 1) / stepsPerBar
} ?: 0

/**
 * Audio track [id] sounds in song [part]; null: a scene played on its own (live or a loop export),
 * where a song clip stays silent (it belongs to the song: it plays in song playback and its export).
 */
fun Project.audioPlays(id: Int, part: SongPart?): Boolean =
    part?.plays(SongPart.audio(id)) ?: (audioTracks.firstOrNull { it.id == id }?.clip?.song != true)
