package com.pocketstudio.app.synth

import com.pocketstudio.model.NoteClip

/**
 * Before a live take on the keyboard: a phrase played over a one-bar loop folds onto that bar (each
 * new note over the one placed a bar earlier), so the loop first grows to [TAKE_BARS] bars.
 */
object TakeLoop {
    const val TAKE_BARS = 4

    /**
     * The clip to record over, or null when [clip] is already longer than a bar of [stepsPerBar]. Notes the loop
     * hid when it was shortened come back; otherwise the bar is repeated, so it sounds the same.
     */
    fun grown(clip: NoteClip, stepsPerBar: Int = 16): NoteClip? {
        val bar = stepsPerBar.coerceIn(1, NoteClip.MAX_STEPS / TAKE_BARS)
        if (clip.steps > bar) return null
        // Bars of the time signature (12 steps in 3/4): the take stays on the drums' bars.
        val steps = TAKE_BARS * bar
        if (clip.hidden.isNotEmpty() || clip.notes.isEmpty()) return clip.copy(steps = steps).sanitized()
        val repeated = (0 until TAKE_BARS).flatMap { bar -> clip.notes.map { it.copy(start = it.start + bar * clip.ticks, played = it.played?.plus(bar * clip.ticks)) } }
        return clip.copy(steps = steps, notes = repeated).sanitized()
    }
}
