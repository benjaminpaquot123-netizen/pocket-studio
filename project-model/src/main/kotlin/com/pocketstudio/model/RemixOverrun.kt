package com.pocketstudio.model

/*
 * Chantier 10 of the hands-on audit (G16, round 5 T5-Ec-28, recette tel-15): a remix's song that runs past the
 * end of the project's song is cut there in the shared file (the file lasts as long as the song's parts).
 * « Prêt à partager ? » says so, with its two fixes.
 */

/**
 * Steps (sixteenths) of the remix's song heard past the end of the project's song; 0 when it ends in time,
 * when nothing of it is heard ([remixEndBars]), or when there is no song yet (a loop export lets it ring:
 * « Longueur de la boucle » offers its end). A single step over is let go: rounding, not a cut one hears.
 */
fun Project.remixOverrunSteps(seconds: Double = remix.seconds): Long {
    if (song.isEmpty() || remixEndBars(seconds) == 0) return 0
    val songSteps = song.sumOf { partSteps(it).toLong() }
    val over = remix.atStep.toLong() + remixSteps(seconds) - songSteps
    return if (over > 1) over else 0
}

/** [remixOverrunSteps] in seconds, rounded up (« de 0:12 »); 0 when the song ends in time. */
fun Project.remixOverrunSeconds(seconds: Double = remix.seconds): Int {
    val over = remixOverrunSteps(seconds)
    if (over <= 0 || tempoBpm <= 0) return 0
    // A sixteenth lasts 15/bpm seconds.
    return kotlin.math.ceil(over * 15.0 / tempoBpm - 1e-6).toInt().coerceAtLeast(1)
}

/** The fade at the cut of « Couper la chanson à la fin » (seconds): no click where the song stops. */
const val REMIX_CUT_FADE_SECONDS = 1.0

/**
 * « Allonger le morceau »: parts added after the last one, replaying it (same brick, same tracks, the song switched on), as many bars as
 * the remix's song still lasts ([SongPart.MAX_BARS] each, up to [SongPart.MAX_PARTS] parts); unchanged when the song
 * already ends in time.
 */
fun Project.withSongLengthenedForRemix(seconds: Double = remix.seconds): Project {
    val missing = remixOverrunSteps(seconds)
    if (missing <= 0) return this
    val last = song.last()
    val parts = song.toMutableList()
    var bars = ((missing + stepsPerBar - 1) / stepsPerBar.coerceAtLeast(1)).toInt()
    while (bars > 0 && parts.size < SongPart.MAX_PARTS) {
        val n = bars.coerceAtMost(SongPart.MAX_BARS)
        // The song itself heard there (fresh review: it may have been switched off in the last part); the rest as it was.
        parts += last.copy(bars = n, repeats = 1, muted = last.muted.filterNot(::isRemixKey).toSet())
        bars -= n
    }
    return copy(song = parts)
}

/**
 * « Couper la chanson à la fin »: every stem's pieces end where the song ends (a fade of [REMIX_CUT_FADE_SECONDS] at
 * the cut), those after it taken away; the stems' files are never changed. Unchanged when the song ends in time.
 */
fun Project.withRemixCutAtSongEnd(seconds: Double = remix.seconds): Project {
    if (remixOverrunSteps(seconds) <= 0) return this
    val end = remixTimeAtStep(song.sumOf { partSteps(it) })
    if (!(end > 0)) return this
    val stems = remix.stems.mapIndexed { i, s ->
        val pieces = remix.piecesOf(i, seconds)
        if (pieces.all { it.end <= end }) s
        else s.copy(
            clips = pieces.mapNotNull { c ->
                when {
                    c.end <= end -> c
                    c.at >= end -> null
                    else -> (end - c.at).let { len -> c.copy(length = len, fadeIn = c.fadeIn.coerceAtMost(len), fadeOut = REMIX_CUT_FADE_SECONDS.coerceAtMost(len)) }
                }
            }.mapNotNull(StemClip::sanitized).take(StemClip.MAX),
        )
    }
    return copy(remix = remix.copy(stems = stems))
}

/** The song's track or one of its stems' (a [SongPart.muted] key). */
private fun isRemixKey(key: String) = key == SongPart.REMIX || Remix.stemOfKey(key) != null
