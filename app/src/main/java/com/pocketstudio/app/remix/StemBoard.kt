package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.Pcm

/*
 * « Tableau des pistes » (owner, 2026-10-28): each stem's sound drawn as a curve, a stretch chosen
 * by sliding a finger from left to right (two handles to adjust it), trimmed with short fades and
 * sent wherever the user wants: a pad, an instrument played on the keyboard, a new track of the
 * song, a live pad; the voice cut into its phrases in one touch. The shared contract of the three
 * teams building it: the board on screen (B1), the edits (B2), where a stretch goes (B3).
 */

/** A stretch of stem [stem] of song [setId], in frames of the stem, with its fades. */
data class StemSelection(
    val setId: String,
    val stem: Int,
    val startFrame: Long,
    val endFrame: Long,
    val fadeInMs: Int = DEFAULT_FADE_IN_MS,
    val fadeOutMs: Int = DEFAULT_FADE_OUT_MS,
) {
    val frames: Long get() = (endFrame - startFrame).coerceAtLeast(0)

    companion object {
        /** Short enough not to soften an attack, long enough not to click. */
        const val DEFAULT_FADE_IN_MS = 4
        const val DEFAULT_FADE_OUT_MS = 12
    }
}

/** Where a stretch goes. */
sealed interface StemDestination {
    /** A drum pad: [pad], or the first free one when null (never one that has a sound of its own without asking). */
    data class Pad(val pad: Int? = null) : StemDestination

    /** A new instrument track that plays the stretch on the keyboard (its pitch follows the keys). */
    data object Instrument : StemDestination

    /** A new audio track of the song, the stretch starting at song step [atStep] (null: where the play head is). */
    data class Track(val atStep: Int? = null) : StemDestination

    /** A free live pad (Live › Pads). */
    data object Live : StemDestination
}

/** The edits (B2): pure work on samples, off the main thread, never on the audio thread. */
interface StemEdits {
    /** [sel] cut out of [stem] with its fades (the same channels and rate). */
    fun cut(stem: Pcm, sel: StemSelection): Pcm

    /**
     * The phrases of [stem] (a voice): the stretches between its silences, in frames, at most
     * [max], each snapped to the nearest beat when [beatFrames] is given. Each range is
     * `start until end` (so a selection's `endFrame` is `range.last + 1`), in time order.
     */
    fun phrases(stem: Pcm, beatFrames: Long? = null, max: Int = 8): List<LongRange>

    /**
     * As [phrases], the beat grid starting at frame [firstBeatFrame] of the stem (the song's first
     * beat, `Remix.firstBeat` × rate) instead of frame 0. Added by B2: a song rarely starts on a
     * beat. The default ignores the offset, so simple fakes need not implement it.
     */
    fun phrases(stem: Pcm, beatFrames: Long?, max: Int, firstBeatFrame: Long): List<LongRange> =
        phrases(stem, beatFrames, max)
}

/** What the board asks of the app (B3). */
interface StemBoardActions {
    /** Plays [sel] alone (again: it stops). */
    fun play(sel: StemSelection)

    fun stop()

    /** Sends [sel] to [dest], one undo step; says what it did (« Voix → pad Zap ») or why not. */
    fun send(sel: StemSelection, dest: StemDestination)

    /** Cuts stem [stem] of [setId] into its phrases, one a pad (free pads only), at most [max]. */
    fun chop(setId: String, stem: Int, max: Int = 8)
}
