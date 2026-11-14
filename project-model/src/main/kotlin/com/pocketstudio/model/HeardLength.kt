package com.pocketstudio.model

/**
 * Tour 6, lot « coquille2 » (P6, seat A b): the length one hears of scene [scene] played in a loop, in steps: its notes
 * and hits ([sceneSteps]) or, when longer, its audio bricks (takes put in the Live, each looping on its own
 * [AudioClip.loopSteps]), rounded up to whole bars. Only what the transport and the screen say; the engine's loop and
 * the exports keep their own length. Counted as `SongLength.of` counts a loop (lot padlive2) for a take in a brick: the
 * same number (the two to be aligned at the entry of the second, chef's note).
 */
fun Project.heardSceneSteps(scene: Int): Int {
    val spb = stepsPerBar.coerceAtLeast(1)
    val notes = sceneSteps(scene)
    // As the engine plays them (fresh review): a clip of every part too, a « once » clip on its whole passes; a track
    // muted or at zero is not heard.
    val bricks = audioTracks.maxOfOrNull { t ->
        val clip = t.clip ?: return@maxOfOrNull 0
        val strip = mixer.channels.getOrNull(AudioTrack.mixerChannel(t.id))
        if (clip.song || !clip.belongsTo(scene) || clip.gain <= 0f || strip?.mute == true || (strip?.volume ?: 1f) <= 0f) 0
        else clip.engineLoopSteps(notes, tempoBpm)
    } ?: 0
    return maxOf(notes, (bricks + spb - 1) / spb * spb)
}
