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
