package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * Step 5b: a piece of one of the song's stems laid out on the song's timeline: seconds [at] to
 * [at] + [length] of the timeline play the stem from its second [from] on, with fades (seconds).
 * Seconds of the song as separated (its own tempo): the timeline second t is heard where the
 * unedited song plays its second t, so the edits follow the song wherever it is placed and
 * whatever the tempo. The stem's file is never changed.
 */
@Serializable
data class StemClip(val at: Double, val from: Double, val length: Double, val fadeIn: Double = 0.0, val fadeOut: Double = 0.0) {
    val end: Double get() = at + length
    operator fun contains(t: Double) = t >= at && t < end

    fun sanitized(): StemClip? {
        fun ok(v: Double) = v.takeIf { it.isFinite() }?.coerceIn(0.0, Remix.MAX_SECONDS * 2) ?: 0.0
        val len = ok(length)
        if (len <= 0.0) return null
        return StemClip(ok(at), ok(from), len, ok(fadeIn).coerceAtMost(len), ok(fadeOut).coerceAtMost(len))
    }

    companion object {
        /** Pieces a stem holds at most (the engine's StemPlayer::kMaxClips). */
        const val MAX = 32
    }
}

/** Stem [stem]'s pieces: its edits, or the whole stem as separated ([seconds] long). */
fun Remix.piecesOf(stem: Int, seconds: Double = this.seconds): List<StemClip> =
    stems.getOrNull(stem)?.clips ?: listOf(StemClip(0.0, 0.0, seconds.coerceAtLeast(0.0)))

/** The piece of stem [stem] heard at timeline second [t] (the later one where two overlap), or -1. */
fun Remix.pieceAt(stem: Int, t: Double, seconds: Double = this.seconds): Int = piecesOf(stem, seconds).indexOfLast { t in it }

private fun Remix.withPieces(stem: Int, pieces: List<StemClip>): Remix =
    withStem(stem) { it.copy(clips = pieces.mapNotNull(StemClip::sanitized).take(StemClip.MAX)) }

/** Stem [stem] cut in two at timeline second [t] (nothing when no piece is there, or at its edge, or full). */
fun Remix.cutStem(stem: Int, t: Double, seconds: Double = this.seconds): Remix {
    val pieces = piecesOf(stem, seconds)
    val i = pieces.indexOfLast { t in it }
    if (i < 0 || pieces.size >= StemClip.MAX) return this
    val c = pieces[i]
    val left = t - c.at
    if (left < 0.01 || c.end - t < 0.01) return this
    val a = c.copy(length = left, fadeOut = 0.0, fadeIn = c.fadeIn.coerceAtMost(left))
    val b = StemClip(t, c.from + left, c.length - left, fadeOut = c.fadeOut.coerceAtMost(c.length - left))
    return withPieces(stem, pieces.toMutableList().apply { set(i, a); add(i + 1, b) })
}

/** The piece of stem [stem] at timeline second [t] moved by [by] seconds (not before the timeline's start). */
fun Remix.moveStemPiece(stem: Int, t: Double, by: Double, seconds: Double = this.seconds): Remix {
    val pieces = piecesOf(stem, seconds)
    val i = pieces.indexOfLast { t in it }
    if (i < 0 || !by.isFinite()) return this
    val c = pieces[i]
    // Moved on top: the piece moved is the one heard where it lands.
    return withPieces(stem, pieces.toMutableList().apply { removeAt(i); add(c.copy(at = (c.at + by).coerceAtLeast(0.0))) })
}

/** The fades of the piece of stem [stem] at timeline second [t] (null: kept). */
fun Remix.fadeStemPiece(stem: Int, t: Double, fadeIn: Double? = null, fadeOut: Double? = null, seconds: Double = this.seconds): Remix {
    val pieces = piecesOf(stem, seconds)
    val i = pieces.indexOfLast { t in it }
    if (i < 0) return this
    val c = pieces[i]
    return withPieces(stem, pieces.toMutableList().apply { set(i, c.copy(fadeIn = fadeIn ?: c.fadeIn, fadeOut = fadeOut ?: c.fadeOut)) })
}

/** The piece of stem [stem] at timeline second [t] taken away (silence there). */
fun Remix.removeStemPiece(stem: Int, t: Double, seconds: Double = this.seconds): Remix {
    val pieces = piecesOf(stem, seconds)
    val i = pieces.indexOfLast { t in it }
    if (i < 0) return this
    return withPieces(stem, pieces.toMutableList().apply { removeAt(i) })
}

/** Stem [stem] back as separated (its edits forgotten). */
fun Remix.resetStem(stem: Int): Remix = withStem(stem) { it.copy(clips = null) }

/** The timeline second the song's last heard piece ends on: its length when no stem is edited. */
fun Remix.heardEnd(seconds: Double = this.seconds): Double =
    stems.maxOfOrNull { s -> s.clips?.maxOfOrNull { it.end } ?: seconds }?.coerceAtMost(Remix.MAX_SECONDS * 2) ?: seconds

/**
 * Stem [stem]'s edits for the engine: 5 numbers a piece (timeline frame, source frame, length,
 * fade in, fade out; frames of the song at [rate]); null when it has none.
 */
fun Remix.stemClipFrames(stem: Int, rate: Int): LongArray? {
    val clips = stems.getOrNull(stem)?.clips ?: return null
    fun f(s: Double) = Math.round(s * rate).coerceAtLeast(0L)
    return LongArray(clips.size * 5).also { a ->
        clips.forEachIndexed { i, c ->
            a[i * 5] = f(c.at); a[i * 5 + 1] = f(c.from); a[i * 5 + 2] = f(c.length); a[i * 5 + 3] = f(c.fadeIn); a[i * 5 + 4] = f(c.fadeOut)
        }
    }
}

/** Seconds of the song's timeline one project sixteenth lasts (shorter when the song follows a slower tempo). */
fun Project.remixSecondsPerStep(): Double =
    if (tempoBpm > 0) 15.0 / tempoBpm / remix.stretchFor(tempoBpm) else 0.0

/** The song's timeline second heard on the project's song step [step] (as placed by [Remix.atStep]). */
fun Project.remixTimeAtStep(step: Int): Double =
    remix.startFrame(48_000) / 48_000.0 + (step - remix.atStep) * remixSecondsPerStep()

/** The project's song step (fractional) where the song's timeline second [t] is heard. */
fun Project.remixStepAtTime(t: Double): Double {
    val per = remixSecondsPerStep()
    return if (per <= 0) 0.0 else remix.atStep + (t - remix.startFrame(48_000) / 48_000.0) / per
}

/**
 * Where stem [stem] is heard in each part of the song, as fractions of the part (its line in the
 * Structure): its pieces placed by the song, nothing in a part that leaves the song out.
 */
fun Project.remixStemSpans(stem: Int, seconds: Double = remix.seconds): List<List<ClosedFloatingPointRange<Float>>> {
    if (remix.stems.getOrNull(stem)?.clips == null) return remixSpans(seconds)
    val ranges = remix.piecesOf(stem, seconds).map { remixStepAtTime(it.at) to remixStepAtTime(it.end) }
    var at = 0
    return song.map { part ->
        val steps = partSteps(part)
        val partStart = at
        at += steps
        if (steps <= 0 || !part.plays(SongPart.REMIX)) return@map emptyList()
        ranges.mapNotNull { (a, b) ->
            val from = (a - partStart).coerceIn(0.0, steps.toDouble())
            val to = (b - partStart).coerceIn(0.0, steps.toDouble())
            if (to > from) (from / steps).toFloat()..(to / steps).toFloat() else null
        }
    }
}
