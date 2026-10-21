package com.pocketstudio.app.remix

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.separation.Demucs

/**
 * What the separation needs in memory, checked before the model is offered (174 MB for nothing on
 * a device that can never run it) and again by the service once the song's length is known.
 */
object RemixMemory {
    /** The model while it runs (measured about 2.6 GB), with a margin. */
    const val MODEL_BYTES = 2_800_000_000L

    /** The shortest song worth separating, for the check made before any song is chosen. */
    const val SHORTEST_SECONDS = 60.0

    /** The song and its stems in memory, per second of music. */
    private const val BYTES_PER_SECOND = Demucs.SAMPLE_RATE * 2L * 4 * 7

    /**
     * Total memory under which the separation never fits: Android itself and the apps that always
     * run keep 2 GB or more, so a 4 GB device (about 3.7 GB reported) never has 3 GB free.
     */
    const val SMALLEST_DEVICE_BYTES = 5_000_000_000L

    /** True when [problem] can never go away on this device (no use checking again). */
    fun never(totalBytes: Long): Boolean = totalBytes < SMALLEST_DEVICE_BYTES

    /** Memory the separation of [seconds] of music needs free. */
    fun needed(seconds: Double): Long = MODEL_BYTES + (seconds * BYTES_PER_SECOND).toLong()

    /**
     * Why this device cannot separate a song now, in plain words (null: it can): [totalBytes] and
     * [availableBytes] as ActivityManager.MemoryInfo gives them.
     */
    fun problem(totalBytes: Long, availableBytes: Long): String? {
        val least = needed(SHORTEST_SECONDS)
        return when {
            // Said at once, plainly: closing apps would never be enough (no « check again » loop).
            never(totalBytes) -> tr(
                "Cet appareil n'a pas assez de mémoire pour le Remix : il a %.1f Go en tout, il en faut au moins 6. Fermer des applis n'y changera rien. Le reste de l'appli marche normalement.",
                totalBytes / 1e9,
            )
            availableBytes < least -> tr(
                "Pas assez de mémoire libre pour le Remix en ce moment (%.1f Go, il en faut au moins %.1f). Ferme les autres applis, puis touche « Vérifier à nouveau ».",
                availableBytes / 1e9, least / 1e9,
            )
            else -> null
        }
    }
}
