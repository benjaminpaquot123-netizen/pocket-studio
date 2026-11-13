package com.pocketstudio.model

/**
 * Tour 6, lot « coquille2 » (P6, seat A b): the length one hears of scene [scene] played in a loop, in steps: its notes
 * and hits ([sceneSteps]) or, when longer, its audio bricks (takes put in the Live, each looping on its own
 * [AudioClip.loopSteps]), rounded up to whole bars. Only what the transport and the screen say; the engine's loop and
 * the exports keep their own length. Counted as `SongLength.of` counts a loop (lot padlive2): the same number.
 */
fun Project.heardSceneSteps(scene: Int): Int {
    val spb = stepsPerBar.coerceAtLeast(1)
    val bricks = audioTracks.mapNotNull { it.clip }.filter { !it.song && it.scene == scene }.maxOfOrNull { it.loopSteps } ?: 0
    return maxOf(sceneSteps(scene), (bricks + spb - 1) / spb * spb)
}
