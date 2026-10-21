package com.pocketstudio.app.live

/**
 * What the Live page's effects are doing ([com.pocketstudio.app.rhythm.RhythmViewModel.liveFx]):
 * [values]: each whole-mix effect away from its rest, by [com.pocketstudio.model.AutoTarget] LIVE
 * number (a held effect at 1, the DJ filter where the finger is, the Montée knob); [locks]: those
 * that stay on when let go; [trackKills]: the bands cut on each track strip (0 bass, 1 mids, 2
 * highs); [dropArmed]: a Drop waiting for its bar line.
 */
data class LiveFxUi(
    val values: Map<Int, Float> = emptyMap(),
    val locks: Set<Int> = emptySet(),
    val trackKills: Map<Int, Set<Int>> = emptyMap(),
    val dropArmed: Boolean = false,
) {
    fun value(param: Int, rest: Float = 0f): Float = values[param] ?: rest
    fun on(param: Int): Boolean = (values[param] ?: 0f) > 0.5f
}
