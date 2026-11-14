package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ln

/** Step 4e: the sound the project's tempo was set on (« Tempo calé sur Boys : 113 »), and its tempo. */
@Serializable
data class TempoLead(val name: String, val bpm: Double) {
    fun sanitized(): TempoLead? =
        if (!bpm.isFinite() || bpm !in AutoTempo.MIN_BPM..AutoTempo.MAX_BPM) null else copy(name = name.take(40))
}

/**
 * Step 4e (Benjamin's list n° 16): the first sound with a tempo placed in a project that has none
 * yet (a loop on the Live grid or a live pad, a song from the Remix) sets the project's tempo; a
 * short sound (a hit, a shout) never does. The sounds placed after it, and that one when the tempo
 * moves, are stretched to the project's tempo (their pitch kept) and stay on the beat.
 */
object AutoTempo {
    const val MIN_BPM = Project.MIN_TEMPO
    const val MAX_BPM = Project.MAX_TEMPO

    /** Shorter than this, a sound is a hit: it has no tempo of its own. */
    const val MIN_LOOP_SECONDS = 1.0

    /** How far a detected tempo may be moved to make the sound a whole number of beats. */
    private const val SNAP = 0.06

    /** Beats a loop may last (a half bar of 4/4 to 64 bars). */
    private val LOOP_BEATS = listOf(2, 4, 8, 16, 32, 64, 128, 256)

    /**
     * The tempo of a sound [seconds] long about to be placed: [known] (the library's, the song's
     * analysis) or else [detected] (the engine's beat tracker), moved onto the tempo at which the
     * sound lasts a whole number of beats when that is close; 0 when it has none ([oneShot], too
     * short, nothing found).
     */
    fun tempoOf(seconds: Double, known: Double = 0.0, detected: Double = 0.0, oneShot: Boolean = false): Double {
        if (oneShot || !seconds.isFinite() || seconds < MIN_LOOP_SECONDS) return 0.0
        if (known.isFinite() && known in MIN_BPM..MAX_BPM) return known
        if (!detected.isFinite() || detected !in MIN_BPM..MAX_BPM) return 0.0
        val beats = LOOP_BEATS.minBy { abs(ln(it / (seconds * detected / 60.0))) }
        val whole = beats * 60.0 / seconds
        return if (abs(whole / detected - 1.0) <= SNAP && whole in MIN_BPM..MAX_BPM) whole else detected
    }

    /** Library kinds that are hits (never a tempo), whatever their length. */
    private val HIT_KINDS = setOf("kick", "snare", "clap", "hat", "open_hat", "perc", "tom", "ride", "foley")

    /** A sound of the library that has no tempo: a kit's hit, a party sound (a horn, a shout), a hit [kind]. */
    fun isHit(hitSource: Boolean, kind: String?): Boolean = hitSource || kind in HIT_KINDS

    /**
     * The project already has a tempo of its own: a sound set it, or it has music (bricks filled, a
     * take, a sound on an audio track) or a song heard along. Its tempo then stays.
     */
    fun hasTempo(p: Project): Boolean =
        p.tempoLead != null || p.hasMusicOfItsOwn() ||
            (p.remix.setId != null && p.remix.songBpm > 0 && p.remix.stems.any { !it.mute })

    /** The lead a sound [name] at [bpm] becomes when placed in [p] (before it is placed); null: the tempo stays. */
    fun leadFor(p: Project, name: String, bpm: Double): TempoLead? =
        if (bpm <= 0.0 || hasTempo(p)) null else TempoLead(name, bpm).sanitized()

    /** [p] at the tempo of [lead], remembering it. */
    fun led(p: Project, lead: TempoLead): Project = p.copy(tempoBpm = lead.bpm.coerceIn(MIN_BPM, MAX_BPM), tempoLead = lead)

    /**
     * « Annuler » on the banner: [p] back at [previousBpm] in one step, the sound kept; its lead
     * stays, so the next sound does not set the tempo again.
     */
    fun undone(p: Project, previousBpm: Double): Project = p.copy(tempoBpm = previousBpm.coerceIn(MIN_BPM, MAX_BPM))

    /** Frames a loop of [frames] made at [fromBpm] lasts at [toBpm]. */
    fun fittedFrames(frames: Long, fromBpm: Double, toBpm: Double): Long =
        if (fromBpm <= 0 || toBpm <= 0) frames else Math.round(frames * fromBpm / toBpm)

    /** The stretch keeps a loop clean from half to twice its speed; beyond, it keeps its own. */
    fun canFit(fromBpm: Double, toBpm: Double): Boolean =
        fromBpm > 0 && toBpm > 0 && (fromBpm / toBpm) in 0.5..2.0

    /** It needs a stretched copy at [toBpm] (the tempos differ by more than a hair). */
    fun needsFit(fromBpm: Double, toBpm: Double): Boolean = canFit(fromBpm, toBpm) && abs(fromBpm - toBpm) >= 0.05
}
