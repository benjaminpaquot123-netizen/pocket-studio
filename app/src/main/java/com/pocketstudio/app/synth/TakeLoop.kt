package com.pocketstudio.app.synth

import com.pocketstudio.model.NoteClip

/**
 * Before a live take on the keyboard: a phrase played over a one-bar loop folds onto that bar (each
 * new note over the one placed a bar earlier), so the loop first grows to [TAKE_BARS] bars.
 */
object TakeLoop {
    const val TAKE_BARS = 4

    /**
     * The clip to record over, or null when [clip] is already longer than a bar. Notes the loop
     * hid when it was shortened come back; otherwise the bar is repeated, so it sounds the same.
     */
    fun grown(clip: NoteClip): NoteClip? {
        if (clip.steps > 16) return null
        val steps = TAKE_BARS * 16
        if (clip.hidden.isNotEmpty() || clip.notes.isEmpty()) return clip.copy(steps = steps).sanitized()
        val repeated = (0 until TAKE_BARS).flatMap { bar -> clip.notes.map { it.copy(start = it.start + bar * clip.ticks, played = it.played?.plus(bar * clip.ticks)) } }
        return clip.copy(steps = steps, notes = repeated).sanitized()
    }
}
